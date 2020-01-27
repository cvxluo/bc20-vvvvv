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
    
    static int landscapersByHQ;
    static int defensiveByHQ;
    
    static boolean onTheLattice;
    
    static boolean partOfDefense() {
        boolean isPart = false;
        if (rc.canSenseLocation(home)) {
            MapLocation[] x = getXShape(home);
            
            for (MapLocation loc : x) {
                if (loc.equals(currentLocation)) {
                    isPart = true;
                    break;
                }
            }
        
            return isPart;
        }
        else return false;
    }
    
    static void runLandscaper() throws GameActionException {
        System.out.println("DESTINATION " + destination);
        System.out.println("STATE " + state);
        
        System.out.println("LANDSCAPERS BY HQ " + landscapersByHQ);
        System.out.println("DEFENSIVE BY HQ " + defensiveByHQ);
    
        updateHashToRound(Math.max(1, roundNum - 20));
    
        
        if (turnCount == 1) {
            
            int[] message = findFirstMessageByContent(1919191, 5);
    
            hashAtRound = message[0];
            firstHash = hashAtRound;
            roundLastHashed = 1;
            
            MapLocation homeLocation = new MapLocation(message[3], message[4]);
            home = homeLocation;
            destination = homeLocation;
            hqLocation = homeLocation;
            
            defensiveByHQ = message[6];
            state = 4;
        }
        
        // Regularly try for the HQ messages
        if (roundNum % 10 == 1) {
            int roundHash = computeHashForRound(roundNum - 1);
            
            Transaction[] block = rc.getBlock(roundNum - 1);
            for (Transaction t : block) {
                int[] message = t.getMessage();
                if (message[0] == roundHash && message[2] == 5) {
                    landscapersByHQ = message[1];
                }
            }
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
                if (currentLocation.distanceSquaredTo(hqLocation) < 30) {
                    state = 4;
                }
                else state = 1;
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
                
                else if (currentLocation.distanceSquaredTo(destination) < 14) {
                    if (!isAccessible(locToDest)) {
                        boolean isHigherElevation = rc.senseElevation(currentLocation) < rc.senseElevation(locToDest);
                        if (isHigherElevation) {
                            if (rc.getDirtCarrying() < 25) {
                                if (rc.canDigDirt(dirToDest)) rc.digDirt(dirToDest);
                            }
                            else {
                                if (rc.canDepositDirt(Direction.CENTER)) rc.depositDirt(Direction.CENTER);
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
            
            boolean isDefending = partOfDefense();
            
            // Keep checking whether enough defensive landscapers have been allocated
            if (landscapersByHQ >= defensiveByHQ) {
                if (!isDefending) state = 1;
            }
    
            
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
            
            int adjDefense = 0;
            MapLocation[] homeSur = getSurrounding(home);
            for (MapLocation s : homeSur) {
                if (rc.onTheMap(s)) adjDefense++;
            }
            
            
            // Recieve poor wall message
            Transaction[] block = rc.getBlock(roundNum - 1);
    
            for (Transaction t : block) {
                int[] message = t.getMessage();
                int previousRoundHash = computeHashForRound(roundNum - 1);
        
                if (message[0] == previousRoundHash) {
                    if (message[1] == previousRoundHash && message[2] == previousRoundHash
                            && message[4] == previousRoundHash && message[5] == previousRoundHash) {
                        System.out.println("RECIEVED POOR WALL MESSAGE");
    
                        // If there are enough defensive landscapers already, just keep going
                        if (landscapersByHQ < adjDefense) {
                            state = 5;
        
                            if (!isDefending) {
                                state = 2;
                            }
                        }
                    }
                    
                    if (message[3] == previousRoundHash - 3 && !isDefending) {
                        state = 2;
                    }
                }
            }
            
            
            
            System.out.println("COOLDOWN " + rc.getCooldownTurns());
            System.out.println("PART OF DEFENSE " + isDefending);
    
            if (!isDefending) {
                // Look for tiles with inaccessible elevation that aren't occupied
                
                for (MapLocation adj : getXShape(home)) {
                    if (!rc.onTheMap(adj)) continue;
                    
                    // Kill self if stuck
                    boolean isStuck = true;
                    MapLocation[] surrounding = getSurrounding(currentLocation);
                    for (MapLocation s : surrounding) {
                        if (isAccessible(s)) { isStuck = false; break; }
                    }
                    if (isStuck) rc.disintegrate();
                    
                    
                    if (rc.canSenseLocation(adj) && currentLocation.isAdjacentTo(adj) && !isAccessible(adj) && !rc.isLocationOccupied(adj)) {
                        System.out.println("FOUND TILES THAT SHOULD BE LEVELED");
                        
                        Direction levelDirection = currentLocation.directionTo(adj);
                        int adjElevation = rc.senseElevation(adj);
        
                        // Modify behavior if have to mine or have to dig to access tile
                        int dirtCarrying = rc.getDirtCarrying();
                        
                        System.out.println("MY ELE " + rc.senseElevation(currentLocation));
                        System.out.println("ADJ ELE " + rc.senseElevation(adj));
        
                        // If level elevation is lower than current
                        if (adjElevation < rc.senseElevation(currentLocation)) {
                            System.out.println("LOWER ELE, TRYING TO DEPOSIT DIRT");
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
    
                // Periphery is the 3 corner landscapers - they will have different behavior
                // We already know we're a part of the defense, so we don't have to check that again
                boolean onPeriphery = true;
                for (MapLocation adj : getSurrounding(home)) {
                    if (adj.equals(currentLocation)) { onPeriphery = false; break; }
                }
                
                // If we're on the periphery, see if we can move into surrounding square before doing anything else
                if (onPeriphery) {
                    MapLocation[] adjacents = getSurrounding(home);
                    for (MapLocation adj : adjacents) {
                        // This is glitchy - might die to rush
                        if (rc.canSenseLocation(adj)) {
                            if (rc.isLocationOccupied(adj)) {
                                if (rc.senseRobotAtLocation(adj).getType() != RobotType.LANDSCAPER) {
                                    System.out.println("IN PERIPHERY ,TRY MOVING CLOSER");
                                    tryMovingTowards(home);
                                }
                            }
                            else {
                                System.out.println("IN PERIPHERY ,TRY MOVING CLOSER");
                                tryMovingTowards(home);
                            }
                        }
                    }
                }
    
                Direction hqDir = currentLocation.directionTo(home);
                Direction dirtDir = hqDir.opposite();
                
                // Should rework this code - mostly a remenent of the old stuff
                if (!onPeriphery) {
                    System.out.println("HQ DIR " + hqDir);
    
                    // If we're a corner landscaper, dig in an appropriate spot
                    if (hqDir == Direction.SOUTHEAST || hqDir == Direction.SOUTHWEST || hqDir == Direction.NORTHEAST || hqDir == Direction.NORTHWEST) {
                        dirtDir = dirtDir.rotateRight().rotateRight();
                        System.out.println("DIRTDIR " + dirtDir);
                    }
    
                    // If HQ has dirt on it, dig it first
                    // Check if this function is needed - already on the periphery
                    if (rc.canDigDirt(hqDir)) {
                        rc.digDirt(hqDir);
                    }
    
                    if (rc.getDirtCarrying() > 0) {
                        
                        // Check if there are buildings for us to kill
                        RobotInfo[] nearbyEnemy = rc.senseNearbyRobots(1, rc.getTeam().opponent());
                        for (RobotInfo enemy : nearbyEnemy) {
                            if (enemy.getType() == RobotType.DESIGN_SCHOOL || enemy.getType() == RobotType.NET_GUN) {
                                Direction enemyDir = currentLocation.directionTo(enemy.getLocation());
                                if (rc.canDepositDirt(enemyDir)) rc.depositDirt(enemyDir);
                            }
                        }
        
                        // Check if nearby tiles have landscapers
                        int myElevation = rc.senseElevation(currentLocation);
                        Direction left = hqDir;
                        Direction right = hqDir;
                        for (int i = 0; i < 2; i++) {
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
                    else {
                        System.out.println("TRYING TO DIG DIRT");
                        // Try digging opposite to HQ - if not possible, look for other spaces
                        if (rc.canDigDirt(dirtDir) && (!rc.isLocationOccupied(rc.adjacentLocation(dirtDir)) || turnCount > 200)) {
                            rc.digDirt(dirtDir);
                        }
                        else {
                            for (Direction dir : directions) {
                                if (!currentLocation.add(dir).isAdjacentTo(home) && rc.canDigDirt(dir)) {
                                    dirtDir = dir;
                                    if (rc.canDigDirt(dirtDir)) rc.digDirt(dirtDir);
                                }
                            }
                        }
                    }
                }
    
                
                // On periphery
                else {
                    // Abusing my lattice function to check if we're on the corner or not - if we're not on the corner, gotta change digDir to somewhere else
    
                    MapLocation lowestTile = currentLocation;
                    int lowestEle = Integer.MAX_VALUE;
                    MapLocation[] surrounding = getSurrounding(currentLocation);
                    MapLocation lowestDefense = currentLocation.add(hqDir);
                    int lowestDefenseEle = Integer.MAX_VALUE;
                    
                    if (!isOnLattice(currentLocation)) {
                        for (MapLocation sur : surrounding) {
                            int ele = rc.senseElevation(sur);
                            if (ele < lowestEle) { lowestEle = ele; lowestTile = sur; }
                            
                            if (home.isAdjacentTo(sur) && ele < lowestDefenseEle) { lowestDefense = sur; lowestDefenseEle = ele; }
                        }
                        
                        dirtDir = currentLocation.directionTo(lowestTile);
                    }
                    
                    // if there's a building we would be blocked by move
                    else {
                        MapLocation dirtTile = rc.adjacentLocation(dirtDir);
                        if (rc.isLocationOccupied(dirtTile)) {
                            RobotInfo r = rc.senseRobotAtLocation(dirtTile);
                            if (r.getType() == RobotType.DESIGN_SCHOOL || r.getType() == RobotType.FULFILLMENT_CENTER || r.getType() == RobotType.NET_GUN) {
                                dirtDir = dirtDir.rotateLeft();
                                // this will probably break and lose us a critical match
                                // and if it does ill be so upset
                            }
                        }
                    }
                    
                    int myElevation = rc.senseElevation(currentLocation);
                    
                    if (rc.getDirtCarrying() > 0) {
    
                        // Check if there are buildings for us to kill
                        RobotInfo[] nearbyEnemy = rc.senseNearbyRobots(1, rc.getTeam().opponent());
                        for (RobotInfo enemy : nearbyEnemy) {
                            if (enemy.getType() == RobotType.DESIGN_SCHOOL || enemy.getType() == RobotType.NET_GUN) {
                                Direction enemyDir = currentLocation.directionTo(enemy.getLocation());
                                if (rc.canDepositDirt(enemyDir)) rc.depositDirt(enemyDir);
                            }
                        }
                        
                        // First build ourselves up
                        if (myElevation < 75) {
                            if (rc.canDepositDirt(Direction.CENTER)) rc.depositDirt(Direction.CENTER);
                        }
                        else {
                            Direction toLowest = currentLocation.directionTo(lowestDefense);
                            if (rc.canDepositDirt(toLowest)) rc.depositDirt(toLowest);
                        }
                    }
                    
                    else if (rc.canDigDirt(dirtDir)) rc.digDirt(dirtDir);
                    
                }
    
    
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
    
            // Keep checking whether enough defensive landscapers have been allocated
            if (landscapersByHQ >= defensiveByHQ) {
                if (!partOfDefense()) state = 1;
            }
            
            else state = 3;
            
        }
        
        
        
        // Poor wall defense
        if (state == 5) {
            
            Direction hqDir = currentLocation.directionTo(home);
    
            // First, we have to check if we have dirt - if we don't, grab some
            if (rc.getDirtCarrying() == 0) {
                Direction dirtDir = hqDir.opposite();
                
    
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
                // if we qual nuke this code pls it's so so so so so so bad
                // ok it got a little better after i wrote this comment but im keeping it here for posterity
                // lmao it didn't work back to nuking kill it all
                
                // First check if the wall is evenly built at all
                Direction left = hqDir.rotateLeft();
                Direction right = hqDir.rotateRight();
                
                
                /*
                boolean foundInaccessible = false;
                MapLocation lowestInaccessible = currentLocation;
                int eleInaccessible = Integer.MAX_VALUE;
                MapLocation[] surroundingMe = getSurrounding(currentLocation);
                for (MapLocation sur : surroundingMe) {
                    if (sur.isAdjacentTo(home) && !sur.equals(home)) {
                        int surEle = rc.senseElevation(sur);
                        if (!isAccessible(sur) && surEle < rc.senseElevation(currentLocation)) {
                            foundInaccessible = true;
                            if (surEle < eleInaccessible) {
                                eleInaccessible = surEle;
                                lowestInaccessible = sur;
                            }
                        }
                    }
                }
                
                if (foundInaccessible) {
                    Direction toInaccessible = currentLocation.directionTo(lowestInaccessible);
                    if (rc.canDepositDirt(toInaccessible)) rc.depositDirt(toInaccessible);
                }
                
                */
                
                // If we are on a cardinal tile, we need to check 4 spaces
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
    
                // Otherwise, we just check 2 spaces, the ones directly adjacent
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
                
                
                // Now, if the wall is sufficiently built, we look for the lowest tile, and try to fix it
                else {
                    MapLocation[] adjacentTiles = new MapLocation[8];
                    int[] elevations = new int[8];
                    MapLocation lowestLoc = adjacentTiles[0];
                    MapLocation lowestAdjLoc = adjacentTiles[0];
                    int lowestEle = Integer.MAX_VALUE;
                    int lowestAdjEle = Integer.MAX_VALUE;
    
                    for (int i = 0; i < 8; i++) {
                        adjacentTiles[i] = home.add(directions[i]);
                        elevations[i] = rc.senseElevation(adjacentTiles[i]);
                        if (adjacentTiles[i].isAdjacentTo(currentLocation)) {
                            if (elevations[i] < lowestAdjEle) { lowestAdjEle = elevations[i]; lowestAdjLoc = adjacentTiles[i]; }
                        }
                        if (elevations[i] < lowestEle) { lowestEle = elevations[i]; lowestLoc = adjacentTiles[i]; }
                    }
                    
                    Direction dirToLowest = currentLocation.directionTo(lowestLoc);
                    Direction dirToLowestAdj = currentLocation.directionTo(lowestAdjLoc);
    
    
                    System.out.println("LOWEST LOC " + lowestLoc);
                    System.out.println("DIR TO LOWEST LOC " + dirToLowest);
                    
    
                    if (currentLocation.isAdjacentTo(lowestLoc)) {
                        if (rc.canDepositDirt(dirToLowest)) rc.depositDirt(dirToLowest);
                    }
                    // If it's not worth moving, just fill in my space
                    else if (lowestEle > rc.senseElevation(currentLocation) - 5) {
                        if (rc.canDepositDirt(Direction.CENTER)) rc.depositDirt(Direction.CENTER);
                    }
                    else {
                        int numLowest = 0;
                        for (Direction d : directions) {
                            MapLocation a = lowestLoc.add(d);
                            if (rc.canSenseLocation(a)) {
                                if (rc.isLocationOccupied(a) && rc.senseRobotAtLocation(a).getType() == RobotType.LANDSCAPER) {
                                    numLowest++;
                                }
                            }
                        }
                        if (numLowest > 1) {
                            if (rc.canDepositDirt(dirToLowestAdj)) rc.depositDirt(dirToLowestAdj);
                        }
                        else {
                            destination = lowestLoc;
                            tryMovingTowards(destination);
                        }
                    
                    }
                    
                }
                
            }
            
        }
        
        
        if (state == 6) {
            // absolute sad reacts here lmao
        }
        
    }
    
}