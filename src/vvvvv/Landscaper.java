package vvvvv;
import battlecode.common.*;

public strictfp class Landscaper extends RobotPlayer {
    
    static void runLandscaper() throws GameActionException {
        System.out.println("DESTINATION " + destination);
        System.out.println("STATE " + state);
        
        if (turnCount == 1) {
            
            boolean foundSoupBlock = false;
            int[] message = new int[7];
            int blockCheck = 1;
            
            while (!foundSoupBlock) {
                System.out.println("USING BLOCK " + blockCheck);
                Transaction[] block = rc.getBlock(blockCheck);
                blockCheck++;
                for (Transaction transaction : block) {
                    int[] m = transaction.getMessage();
                    if (m[0] == 1234567) { message = m; foundSoupBlock = true; break; }
                }
            }
            MapLocation hqLocation = new MapLocation(message[3], message[4]);
            home = hqLocation;
            
            state = 4;
        }
        
        
        if (state == 1) {
            RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);
            for (RobotInfo robot : robots) {
                if (robot.getType().isBuilding()) {
                    destination = robot.getLocation();
                    state = 2;
                    break;
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
                        numDefenseLandscapers++;
                    }
                }
            }
            
            System.out.println(numDefenseLandscapers);
            if (numDefenseLandscapers > 8) {
                state = 4;
            }
            
            if (!rc.getLocation().isAdjacentTo(home)) tryMovingTowards(home);
            else {
                Direction hqDir = rc.getLocation().directionTo(home);
                Direction dirtDir = hqDir.opposite();
                if (rc.canDepositDirt(Direction.CENTER)) rc.depositDirt(Direction.CENTER);
                if (rc.canDigDirt(dirtDir)) rc.digDirt(dirtDir);
            }
        }
        
        if (state == 4) {
            
            int numDefenseLandscapers = 0;
            if (rc.canSenseLocation(home) && rc.getLocation().distanceSquaredTo(home) < 6) {
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
                int quadrant = findQuadrant(rc.getLocation());
                if (quadrant == 1) destination = new MapLocation(0, 0);
                if (quadrant == 2) destination = new MapLocation(mapWidth - 1, 0);
                if (quadrant == 3) destination = new MapLocation(mapWidth - 1, mapHeight - 1);
                if (quadrant == 4) destination = new MapLocation(0, mapHeight - 1);
                
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