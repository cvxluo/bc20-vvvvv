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
            
            MapLocation hqLocation = new MapLocation(message[3], message[4]);
            home = hqLocation;
            destination = hqLocation;
            
            state = 4;
        }
        
        
        if (state == 1) {
    
            if (rc.canSenseLocation(destination)) {
                int quadrant = findQuadrant(rc.getLocation());
                if (!explored[quadrant]) {
                    explored[quadrant] = true;
                }
                int nextQ = 0;
                boolean foundNextQ = false;
                for (int i = 1; i < 5; i++) {
                    if (!explored[i]) { nextQ = i; foundNextQ = true; break; }
                }
                System.out.println("NEXT Q " + nextQ);
                if (!foundNextQ) { explored = new boolean[5]; nextQ = quadrant; }
                if (nextQ == 3) destination = new MapLocation(0, 0);
                if (nextQ == 4) destination = new MapLocation(mapWidth - 1, 0);
                if (nextQ == 1) destination = new MapLocation(mapWidth - 1, mapHeight - 1);
                if (nextQ == 2) destination = new MapLocation(0, mapHeight - 1);
    
    
                RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);
                for (RobotInfo robot : robots) {
                    if (robot.getType().isBuilding()) {
                        destination = robot.getLocation();
                        state = 2;
                        break;
                    }
                }
            }
            
            tryMovingTowards(destination);
        }
        
        if (state == 2) {
            if (rc.getLocation().isAdjacentTo(destination)) {
                Direction dumpDirtDir = rc.getLocation().directionTo(destination);
                if (rc.canDepositDirt(dumpDirtDir)) rc.depositDirt(dumpDirtDir);
                else {
                    for (Direction dir : directions) {
                        if (rc.canDigDirt(dir)) {
                            rc.digDirt(dir);
                        }
                    }
                }
            }
            else tryMovingTowards(destination);
        }
        
        
        if (state == 3) {
            int numDefenseLandscapers = 0;
            if (rc.canSenseLocation(home) && rc.getLocation().distanceSquaredTo(home) < 6) {
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
                for (Direction dir : directions) {
                    if (!currentLocation.add(dir).isAdjacentTo(home) && rc.canDigDirt(dir)) {
                        dirtDir = dir;
                    }
                }
    
                if (rc.canDepositDirt(Direction.CENTER)) rc.depositDirt(Direction.CENTER);
                if (rc.canDigDirt(dirtDir)) rc.digDirt(dirtDir);

            }
        }
        
        if (state == 4) {
            
            int numDefenseLandscapers = 0;
            if (rc.canSenseLocation(home) && currentLocation.distanceSquaredTo(home) < 6) {
                RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
                for (RobotInfo robot : nearbyRobots) {
                    if (robot.getType() == RobotType.LANDSCAPER) {
                        numDefenseLandscapers++;
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
                if (nextQ == 3) destination = new MapLocation(0, 0);
                if (nextQ == 4) destination = new MapLocation(mapWidth - 1, 0);
                if (nextQ == 1) destination = new MapLocation(mapWidth - 1, mapHeight - 1);
                if (nextQ == 2) destination = new MapLocation(0, mapHeight - 1);
                
                state = 1;
            }
            
        }
        
        // Constantly grab some dirt from surroundings, and grab dirt if dumping dirt on a building
        if (turnCount % 15 == 0 || state == 2) {
            for (Direction dir : directions) {
                if (rc.canDigDirt(dir)) {
                    rc.digDirt(dir);
                }
            }
        }
    }
    
}