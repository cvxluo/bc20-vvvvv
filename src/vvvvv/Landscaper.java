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
            
            lastHash = message[0];
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
                
                else if (currentLocation.distanceSquaredTo(destination) < 6) {
                    if (!isAccessible(locToDest)) {
                        boolean isHigherElevation = rc.senseElevation(currentLocation) < rc.senseElevation(locToDest);
                        if (!isHigherElevation) { if (rc.canDepositDirt(dirToDest)) rc.depositDirt(dirToDest); }
                        else { if (rc.canDigDirt(dirToDest)) rc.digDirt(dirToDest); }
        
                    }
                    
                    else {
                        tryMovingTowards(destination);
                    }
                }
                
                
                else tryMovingTowards(destination);
            }
        }
        
        
        if (state == 3) {
    
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
            if (rc.canSenseLocation(home) && currentLocation.distanceSquaredTo(home) < 9) {
                RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
                for (RobotInfo robot : nearbyRobots) {
                    if (robot.getType() == RobotType.LANDSCAPER) {
                        if (robot.getLocation().isAdjacentTo(home)) {
                            numDefenseLandscapers++;
                        }
                    }
                }
            }
            
            int numDefensiveSpaces = 0;
            for (Direction dir : directions) {
                MapLocation testSpace = home.add(dir);
                if (testSpace.x >= 0 && testSpace.y >= 0 && testSpace.x < rc.getMapWidth() && testSpace.y < rc.getMapHeight()) {
                    numDefensiveSpaces++;
                }
            }
            System.out.println(numDefenseLandscapers);
            if (numDefenseLandscapers >= numDefensiveSpaces) {
                state = 1;
            }
    
            if (!currentLocation.isAdjacentTo(home)) tryMovingTowards(home);
            else {
                Direction hqDir = currentLocation.directionTo(home);
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
                        
                        if (rc.isLocationOccupied(leftLocation) && rc.senseRobotAtLocation(leftLocation).type == RobotType.LANDSCAPER && leftLocation.isAdjacentTo(home)) {
                            int leftEle = rc.senseElevation(leftLocation);
                            if (leftEle < myElevation) rc.depositDirt(left);
                        }
                        
                        if (rc.isLocationOccupied(rightLocation) && rc.senseRobotAtLocation(rightLocation).type == RobotType.LANDSCAPER && rightLocation.isAdjacentTo(home)) {
                            int rightEle = rc.senseElevation(rightLocation);
                            if (rightEle < myElevation) rc.depositDirt(right);
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
    
            int numDefenseLandscapers = 0;
            if (rc.canSenseLocation(home) && currentLocation.distanceSquaredTo(home) < 9) {
                RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
                for (RobotInfo robot : nearbyRobots) {
                    if (robot.getType() == RobotType.LANDSCAPER) {
                        if (robot.getLocation().isAdjacentTo(home)) {
                            numDefenseLandscapers++;
                        }
                    }
                }
            }
            
            else {
                tryMovingTowards(home);
            }
            
            System.out.println(numDefenseLandscapers);
            if (numDefenseLandscapers < 8) {
                state = 3;
            }
            
            else {
                int quadrant = findQuadrant(currentLocation);
                explored[quadrant] = true;
                int nextQ = 0;
                boolean foundNextQ = false;
                for (int i = 1; i < 5; i++) {
                    if (!explored[i]) { nextQ = i; foundNextQ = true; break; }
                }
                System.out.println("NEXT Q " + nextQ);
                if (!foundNextQ) { explored = new boolean[5]; nextQ = quadrant; }
                destination = getQuadrantCorner(nextQ);
                
                state = 1;
            }
            
        }
    }
    
}