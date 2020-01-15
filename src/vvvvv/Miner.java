package vvvvv;
import battlecode.common.*;

public strictfp class Miner extends RobotPlayer {
    
    static void runMiner() throws GameActionException {
        
        int roundNum = rc.getRoundNum();
        System.out.println("STATE " + state);
        System.out.println("DESTINATION " + destination);
        
        
        if (turnCount == 1) { // Setup stuff
            
            int[] message = findFirstMessageByContent(7654321, 5); // Guarenteed that this first block will exist
            MapLocation soupLocation = new MapLocation(message[1], message[2]);
            destination = soupLocation;
            
            state = 1;
            
            int quadrant = findQuadrant(rc.getLocation());
            explored[quadrant] = true;
        }
        
        if (state == 1) {
            for (Direction dir : allDirs) {
                if (rc.canMineSoup(dir)) {
                    destination = rc.adjacentLocation(dir);
                    state = 2;
                    break;
                }
            }
            
            
            // If there is no soup left at destination, look for more soup near the destination - need to optimize
            if (rc.canSenseLocation(destination) && rc.getLocation().distanceSquaredTo(destination) < 8) {
                if (rc.senseSoup(destination) == 0) {
                    state = 4;
                }
                else {
                    tryMovingTowards(destination);
                }
            }
            
            else {
                tryMovingTowards(destination);
            }
            
            
        }
        
        if (state == 2) {
            System.out.println("CARRYING " + rc.getSoupCarrying());
            
            if (rc.getSoupCarrying() == 100) {
                state = 3;
            }
            else if (rc.canSenseLocation(destination) && rc.senseSoup(destination) == 0) { // If no soup left, go back to looking for more soup - maybe consider going back if already over a certain soup amount?
                state = 1;
            }
            else {
                for (Direction dir : allDirs) {
                    if (rc.canMineSoup(dir)) {
                        rc.mineSoup(dir);
                    }
                }
            }
        }
        
        if (state == 3) {
            
            if (currentLocation.distanceSquaredTo(home) < 20 && currentLocation.distanceSquaredTo(home) > 10) {
                RobotInfo[] robots = rc.senseNearbyRobots();
                boolean designExists = false;
                boolean fulfillExists = false;
                boolean refineryExists = false;
                boolean netgunExists = false;
                
                RobotInfo refinery = new RobotInfo(1, rc.getTeam(), RobotType.REFINERY, currentLocation); //very bad
                for (RobotInfo robot : robots) {
                    if (robot.getType() == RobotType.DESIGN_SCHOOL) designExists = true;
                    if (robot.getType() == RobotType.FULFILLMENT_CENTER) fulfillExists = true;
                    if (robot.getType() == RobotType.REFINERY) { refineryExists = true; refinery = robot; }
                    if (robot.getType() == RobotType.NET_GUN) netgunExists = true;
                }
                if (!designExists) {
                    for (Direction dir : directions) {
                        if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, dir) && rc.getTeamSoup() > 250) {
                            rc.buildRobot(RobotType.DESIGN_SCHOOL, dir);
                        }
                    }
                }
                
                if (!fulfillExists) {
                    for (Direction dir : directions) {
                        if (rc.canBuildRobot(RobotType.FULFILLMENT_CENTER, dir) && rc.getTeamSoup() > 300) {
                            rc.buildRobot(RobotType.FULFILLMENT_CENTER, dir);
                        }
                    }
                }
                
                if (!netgunExists) {
                    for (Direction dir : directions) {
                        if (rc.canBuildRobot(RobotType.NET_GUN, dir) && rc.getTeamSoup() > 500) {
                            rc.buildRobot(RobotType.NET_GUN, dir);
                        }
                    }
                }
                
                if (!refineryExists) {
                    for (Direction dir : directions) {
                        if (rc.canBuildRobot(RobotType.REFINERY, dir) && rc.getTeamSoup() > 300) {
                            rc.buildRobot(RobotType.REFINERY, dir);
                        }
                    }
                }
                else {
                    home = refinery.getLocation();
                }
            }
            
            for (Direction dir : directions) {
                if (rc.canDepositSoup(dir)) {
                    System.out.println("DEPOSIT");
                    state = 1;
                    rc.depositSoup(dir, 100);
                }
            }
            
            tryMovingTowards(home);
            
        }
        
        if (state == 4) {
            /*
            boolean foundSoup = false;
            for (int x = 0; x < mapWidth; x++) {
                for (int y = 0; y < mapHeight; y++) {
                    MapLocation loc = new MapLocation(x, y);
                    if (rc.canSenseLocation(loc)) {
                        if (rc.senseSoup(loc) != 0) {
                            destination = loc;
                            foundSoup = true;
                            break;
                        }
                    }
                }
            }
            */
            
            boolean foundSoup = false;
            MapLocation soupLocation = new MapLocation(-1, -1);
            MapLocation me = currentLocation;
            for (int x = -7; x < 7; x++) {
                for (int y = -7; y < 7; y++) {
                    MapLocation loc = new MapLocation(me.x + x, me.y + y);
                    if (rc.canSenseLocation(loc)) {
                        if (rc.senseSoup(loc) != 0) { soupLocation = loc; foundSoup = true; break; }
                    }
                }
            }
            
            if (!foundSoup) {
                
                System.out.println("EXPLROED");
                for (int i = 0; i < 5; i++) {
                    System.out.print(explored[i] + " ");
                }
                System.out.println();
                
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
            }
            
            else {
                destination = soupLocation;
            }
            
            state = 1;
        }
        

        

        /*
        // tryBuild(randomSpawnedByMiner(), randomDirection());
        for (Direction dir : directions)
            tryBuild(RobotType.FULFILLMENT_CENTER, dir);
        for (Direction dir : directions)
            if (tryRefine(dir))
                System.out.println("I refined soup! " + rc.getTeamSoup());
        for (Direction dir : directions)
            if (tryMine(dir))
                System.out.println("I mined soup! " + rc.getSoupCarrying());
                
                */
    }
    
}