package vvvvv;
import battlecode.common.*;

/**
 * State Indicators:
 * 0 - just created
 *
 * For landscapers: //wip, should revise
 * 1 - searching for HQ
 * 2 - dumping dirt on HQ
 *
 * 3 - defensive landscaper
 * 4 - looking if defense is needed
 */


public strictfp class Landscaper extends RobotPlayer {
    
    static void runLandscaper() throws GameActionException {
        System.out.println("DESTINATION " + destination);
        System.out.println("STATE " + state);
        
        if (turnCount == 1) {
            
            int[] message = findFirstMessageByContent(7654321, 5);
    
            hashAtRound = message[0];
            firstHash = hashAtRound;
            roundLastHashed = 1;
            
            MapLocation hqLocation = new MapLocation(message[3], message[4]);
            home = hqLocation;
            destination = hqLocation;
            
            state = 4;
        }
        
        
        if (state == 1) {
    
            if (rc.canSenseLocation(destination)) {
                int quadrant = findQuadrant(currentLocation);
                if (!explored[quadrant]) {
                    explored[quadrant] = true;
                }
                int nextQ = 0;
                boolean foundNextQ = false;
                for (int i = 1; i < 5; i++) {
                    if (!explored[i]) { nextQ = i; foundNextQ = true; break; }
                }
                if (!foundNextQ) { explored = new boolean[5]; nextQ = quadrant; }
                System.out.println("NEXT Q " + nextQ);
                destination = getQuadrantCorner(nextQ);
                
            }
    
            RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);
            for (RobotInfo robot : robots) {
                if (robot.getType().isBuilding()) {
                    System.out.println("DETECTED BUILDING TO FIGHT");
                    destination = robot.getLocation();
                    state = 2;
                    break;
                }
            }
            
            tryMovingTowards(destination);
        }
        
        if (state == 2) {
    
            boolean destroyedBuilding = true;
            RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);
            for (RobotInfo robot : robots) {
                if (robot.getType().isBuilding()) {
                    if (destination == robot.getLocation()) { destroyedBuilding = false; break; }
                }
            }
            
            if (destroyedBuilding) {
                state = 1;
            }
            else {
                Direction dirToDest = currentLocation.directionTo(destination);
                MapLocation locToDest = rc.adjacentLocation(dirToDest);
                
                if (currentLocation.isAdjacentTo(destination)) {
                    Direction dumpDirtDir = dirToDest;
                    if (rc.canDepositDirt(dumpDirtDir)) rc.depositDirt(dumpDirtDir);
                    else {
                        for (Direction dir : directions) {
                            if (rc.canDigDirt(dir)) {
                                rc.digDirt(dir);
                            }
                        }
                    }
                }
                
                else if (currentLocation.distanceSquaredTo(destination) < 9) {
                    if (!isAccessible(locToDest)) {
                        boolean isHigherElevation = rc.senseElevation(currentLocation) < rc.senseElevation(locToDest);
                        if (isHigherElevation) {
                            if (rc.getDirtCarrying() < 25) {
                                if (rc.canDigDirt(dirToDest)) rc.digDirt(dirToDest);
                            }
                            else {
                                if (rc.canDepositDirt(dirToDest.opposite())) rc.depositDirt(dirToDest.opposite());
                            }
                        }
                        else {
                            if (rc.getDirtCarrying() > 0) {
                                if (rc.canDepositDirt(dirToDest)) rc.depositDirt(dirToDest);
                            }
                            else {
                                if (rc.canDigDirt(dirToDest.opposite())) rc.digDirt(dirToDest.opposite());
                            }

                        }
                        
        
                    }
                    
                    else {
                        tryMovingTowards(destination);
                    }
                }
                
                
                else tryMovingTowards(destination);
            }
        }
        
        
        if (state == 3) {
    
            // If already flooded, just die
            int numFloodedTiles = 0;
            for (int x = -5; x < 5; x++) {
                for (int y = -5; y < 5; y++) {
                    MapLocation check = currentLocation.translate(x, y);
                    if (rc.canSenseLocation(check) && rc.senseFlooding(check)) {
                        numFloodedTiles++;
                    }
                }
            }
            if (numFloodedTiles > 40) { state = 5; }
    
            
            // Keep checking whether enough defensive landscapers have been allocated
            int numDefenseLandscapers = 0;
            int numDefensiveSpaces = 0;
    
            if (rc.canSenseLocation(home) && currentLocation.distanceSquaredTo(home) < 9) {
                for (Direction dir : directions) {
                    MapLocation adj = home.add(dir);
                    if (rc.isLocationOccupied(adj)) {
                        RobotInfo adjRobot = rc.senseRobotAtLocation(adj);
                        if (adjRobot.getType() == RobotType.LANDSCAPER) {
                            numDefenseLandscapers++;
                            numDefensiveSpaces++;
                        }
                    } else {
                        MapLocation adjAdj = adj.add(dir);
                        MapLocation aaLeft = adj.add(dir.rotateLeft());
                        MapLocation aaRight = adj.add(dir.rotateRight());
                        if (rc.onTheMap(adjAdj) || rc.onTheMap(aaLeft) || rc.onTheMap(aaRight)) {
                            numDefensiveSpaces++;
                        }
                    }
                }
        
                System.out.println("DEFENSIVE LANDSCAPERS " + numDefenseLandscapers);
                System.out.println("DEFENSIVE SPACES " + numDefensiveSpaces);
                if (numDefenseLandscapers >= numDefensiveSpaces && !currentLocation.isAdjacentTo(home)) {
                    state = 1;
                }
            }
            
            
            
            System.out.println("COOLDOWN " + rc.getCooldownTurns());
    
            if (!currentLocation.isAdjacentTo(home)) {
                // Look for tiles with inaccessible elevation that aren't occupied
                
                boolean shouldLevel = false;
                for (Direction dir : directions) {
                    MapLocation adj = home.add(dir);
                    if (currentLocation.isAdjacentTo(adj) && !isAccessible(adj) && !rc.isLocationOccupied(adj)) {
                        shouldLevel = true;
                        System.out.println("FOUND TILES THAT SHOULD BE LEVELED");
                        
                        Direction levelDirection = currentLocation.directionTo(adj);
                        int adjElevation = rc.senseElevation(adj);
                        
                        // Modify behavior if have to mine or have to dig to access tile
                        int dirtCarrying = rc.getDirtCarrying();
                        
                        // If level elevation is lower than current
                        if (adjElevation < rc.senseElevation(currentLocation)) {
                            // Deposit dirt if carrying - if not, look for a location to dig from
                            if (dirtCarrying > 0) {
                                if (rc.canDepositDirt(levelDirection)) rc.depositDirt(levelDirection);
                            }
                            else {
                                for (Direction d : directions) {
                                    MapLocation potentialSource = rc.adjacentLocation(d);
                                    if (!potentialSource.isAdjacentTo(home)) {
                                        if (rc.canDigDirt(d)) rc.digDirt(d);
                                    }
                                }
                            }
                        }
                        
                        // If level elevation is higher than current
                        else {
                            if (dirtCarrying < 25) {
                                if (rc.canDigDirt(levelDirection)) rc.digDirt(levelDirection);
                            }
                            else {
                                // Look for adjacent tiles - if any have a landscaper that is building the wall, deposit on them - otherwise, deposit somewhere random
                                boolean canHelpWall = false;
                                Direction depositDirection = currentLocation.directionTo(home).opposite();
                                
                                for (Direction d : directions) {
                                    MapLocation potentialDeposit = rc.adjacentLocation(d);
                                    if (rc.isLocationOccupied(potentialDeposit) && rc.senseRobotAtLocation(potentialDeposit).getType() == RobotType.LANDSCAPER && potentialDeposit.isAdjacentTo(home)) {
                                        canHelpWall = true;
                                        depositDirection = d;
                                        break;
                                    }
                                }
                                if (rc.canDepositDirt(depositDirection)) rc.depositDirt(depositDirection);
                                
                            }
                        }
                    }
                }
                
                if (rc.isReady()) tryMovingTowards(home);
            }
            
            
            else {
    
                Transaction[] block = rc.getBlock(roundNum - 1);
                
                for (Transaction t : block) {
                    int[] message = t.getMessage();
                    int previousRoundHash = computeHashForRound(roundNum - 1);
        
                    if (message[0] == previousRoundHash && message[1] == previousRoundHash && message[2] == previousRoundHash
                            && message[4] == previousRoundHash && message[5] == previousRoundHash) {
                        System.out.println("RECIEVED POOR WALL MESSAGE");
                        state = 5;
                    }
                }
                
                
                Direction hqDir = currentLocation.directionTo(home);
                Direction dirtDir = hqDir.opposite();
                
                // If HQ has dirt on it, dig it first
                if (rc.canDigDirt(hqDir)) {
                    rc.digDirt(hqDir);
                }
                
                // Try digging opposite to HQ - if not possible, look for other spaces
                if (rc.canDigDirt(dirtDir)) {
                    rc.digDirt(dirtDir);
                }
                else {
                    for (Direction dir : directions) {
                        if (!currentLocation.add(dir).isAdjacentTo(home) && rc.canDigDirt(dir)) {
                            dirtDir = dir;
                        }
                    }
                }
                
                if (rc.getDirtCarrying() > 0) {
                    
                    // Check if nearby tiles have landscapers that are nearby
                    int myElevation = rc.senseElevation(currentLocation);
                    Direction left = hqDir;
                    Direction right = hqDir;
                    for (int i = 0; i < 3; i++) {
                        left = left.rotateLeft();
                        right = right.rotateRight();
                        MapLocation leftLocation = rc.adjacentLocation(left);
                        MapLocation rightLocation = rc.adjacentLocation(right);
                        
                        if (rc.onTheMap(leftLocation) && rc.isLocationOccupied(leftLocation) && rc.senseRobotAtLocation(leftLocation).type == RobotType.LANDSCAPER && leftLocation.isAdjacentTo(home)) {
                            int leftEle = rc.senseElevation(leftLocation);
                            if (leftEle < myElevation) if (rc.canDepositDirt(left)) rc.depositDirt(left);
                        }
                        
                        if (rc.onTheMap(rightLocation) && rc.isLocationOccupied(rightLocation) && rc.senseRobotAtLocation(rightLocation).type == RobotType.LANDSCAPER && rightLocation.isAdjacentTo(home)) {
                            int rightEle = rc.senseElevation(rightLocation);
                            if (rightEle < myElevation) if (rc.canDepositDirt(right)) rc.depositDirt(right);
                        }
        
                    }
                    
                    if (rc.canDepositDirt(Direction.CENTER)) {
                        rc.depositDirt(Direction.CENTER);
                    }
                }
                else if (rc.canDigDirt(dirtDir)) rc.digDirt(dirtDir);
        
            }
        }
        
        if (state == 4) {
    
            RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);
            for (RobotInfo robot : robots) {
                if (robot.getType().isBuilding()) {
                    System.out.println("DETECTED BUILDING TO FIGHT");
                    destination = robot.getLocation();
                    state = 2;
                    break;
                }
            }
    
            int numDefenseLandscapers = 0;
            int numDefensiveSpaces = 0;
            
            if (rc.canSenseLocation(home) && currentLocation.distanceSquaredTo(home) < 9) {
                for (Direction dir : directions) {
                    MapLocation adj = home.add(dir);
                    if (rc.isLocationOccupied(adj)) {
                        RobotInfo adjRobot = rc.senseRobotAtLocation(adj);
                        if (adjRobot.getType() == RobotType.LANDSCAPER) {
                            numDefenseLandscapers++;
                            numDefensiveSpaces++;
                        }
                    } else {
                        MapLocation adjAdj = adj.add(dir);
                        MapLocation aaLeft = adj.add(dir.rotateLeft());
                        MapLocation aaRight = adj.add(dir.rotateRight());
                        if (rc.onTheMap(adjAdj) || rc.onTheMap(aaLeft) || rc.onTheMap(aaRight)) {
                            numDefensiveSpaces++;
                        }
                    }
                }
    
                System.out.println("DEFENSIVE LANDSCAPERS " + numDefenseLandscapers);
                System.out.println("DEFENSIVE SPACES " + numDefensiveSpaces);
                if (numDefenseLandscapers < numDefensiveSpaces) {
                    state = 3;
                }
                else {
                    state = 1;
                }
            }
            
            else {
                tryMovingTowards(home);
            }
            
            
        }
        
        
        
        // Poor wall defense
        if (state == 5) {
    
            Direction hqDir = currentLocation.directionTo(home);
    
            if (rc.getDirtCarrying() == 0) {
                Direction dirtDir = hqDir.opposite();
    
                // If HQ has dirt on it, dig it first
                if (rc.canDigDirt(hqDir)) {
                    rc.digDirt(hqDir);
                }
    
                // Try digging opposite to HQ - if not possible, look for other spaces
                if (rc.canDigDirt(dirtDir)) {
                    rc.digDirt(dirtDir);
                }
                else {
                    for (Direction dir : directions) {
                        if (!currentLocation.add(dir).isAdjacentTo(home) && rc.canDigDirt(dir)) {
                            dirtDir = dir;
                        }
                    }
                }
            }
            else {
                
                // First check if the wall is evenly built at all
                Direction left = hqDir.rotateLeft();
                Direction right = hqDir.rotateRight();
                
                if (hqDir == Direction.NORTH || hqDir == Direction.SOUTH || hqDir == Direction.EAST || hqDir == Direction.WEST) {
                    MapLocation leftLocation = rc.adjacentLocation(left);
                    MapLocation rightLocation = rc.adjacentLocation(right);
                    
                    if (!isAccessible(leftLocation)) {
                        if (rc.senseElevation(leftLocation) < rc.senseElevation(currentLocation)) {
                            if (rc.canDepositDirt(left)) rc.depositDirt(left);
                        }
                        else { if (rc.canDepositDirt(Direction.CENTER)) rc.depositDirt(Direction.CENTER); }
                    }
                    else if (!isAccessible(rightLocation)) {
                        if (rc.senseElevation(rightLocation) < rc.senseElevation(currentLocation)) {
                            if (rc.canDepositDirt(right)) rc.depositDirt(right);
                        }
                        else { if (rc.canDepositDirt(Direction.CENTER)) rc.depositDirt(Direction.CENTER); }
                    }
                    
                    left = left.rotateLeft();
                    right = right.rotateRight();
                }

                MapLocation leftLocation = rc.adjacentLocation(left);
                MapLocation rightLocation = rc.adjacentLocation(right);
                
                if (!isAccessible(leftLocation)) {
                    if (rc.senseElevation(leftLocation) < rc.senseElevation(currentLocation)) {
                        if (rc.canDepositDirt(left)) rc.depositDirt(left);
                    }
                    else { if (rc.canDepositDirt(Direction.CENTER)) rc.depositDirt(Direction.CENTER); }
                }
                else if (!isAccessible(rightLocation)) {
                    if (rc.senseElevation(rightLocation) < rc.senseElevation(currentLocation)) {
                        if (rc.canDepositDirt(right)) rc.depositDirt(right);
                    }
                    else { if (rc.canDepositDirt(Direction.CENTER)) rc.depositDirt(Direction.CENTER); }
                }
                
                else {
                    MapLocation[] adjacentTiles = new MapLocation[8];
                    int[] elevations = new int[8];
                    MapLocation lowestLoc = adjacentTiles[0];
                    int lowestEle = Integer.MAX_VALUE;
    
                    for (int i = 0; i < 8; i++) {
                        adjacentTiles[i] = home.add(directions[i]);
                        elevations[i] = rc.senseElevation(adjacentTiles[i]);
                        if (elevations[i] < lowestEle) { lowestEle = elevations[i]; lowestLoc = adjacentTiles[i]; }
                    }
                    
                    Direction dirToLowest = currentLocation.directionTo(lowestLoc);
    
                    System.out.println("LOWEST LOC " + lowestLoc);
                    System.out.println("DIR TO LOWEST LOC " + dirToLowest);
    
    
                    if (currentLocation.isAdjacentTo(lowestLoc)) {
                        if (rc.canDepositDirt(dirToLowest)) rc.depositDirt(dirToLowest);
                    }
                    else { destination = lowestLoc; tryMovingTowards(destination); }
                    
                }
                
            }
            
        }
        
    }
    
}