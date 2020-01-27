package Seeding;
import battlecode.common.*;

public strictfp class Miner extends RobotPlayer {
    
    static void runMiner() throws GameActionException {
        
        int roundNum = rc.getRoundNum();
        // System.out.println("STATE " + state);
        // System.out.println("DESTINATION " + destination);
        // System.out.println("HOME " + home);
        
        // System.out.println("CURRENT HASH " + computeHashForCurrentRound());
    
        if (turnCount == 1) { // Setup stuff
            
            int[] message = findFirstMessageByContent(7654321, 5); // Guarenteed that this first block will exist
            hashAtRound = message[0];
            firstHash = hashAtRound;
            roundLastHashed = 1;
            
            MapLocation soupLocation = new MapLocation(message[1], message[2]);
            destination = soupLocation;
            
            MapLocation homeLocation = new MapLocation(message[3], message[4]);
            home = homeLocation;
            
            state = 1;
            
            int quadrant = findQuadrant(rc.getLocation());
            explored[quadrant] = true;
        }
        
        // Reduce computations by keeping a hash updated relatively close to the current round num
        updateHashToRound(Math.max(1, roundNum - 20));
        
        
        Transaction[] b = rc.getBlock(roundNum - 1);
        for (Transaction t : b) {
            int[] message = t.getMessage();
            int previousRoundHash = computeHashForRound(roundNum - 1);
        
            if (message[0] == previousRoundHash && message[1] == previousRoundHash && message[2] == previousRoundHash
                    && message[4] == previousRoundHash && message[5] == previousRoundHash) {
                // System.out.println("RECIEVED POOR WALL MESSAGE");
                rc.disintegrate();
            }
        }
        
        
        if (state == 1) {
    
            if (currentLocation.distanceSquaredTo(home) < 15 && currentLocation.distanceSquaredTo(home) > 7) {
                boolean isHomeHQ = false;
        
                RobotInfo[] robots = rc.senseNearbyRobots(-1, rc.getTeam());
        
                if (rc.canSenseLocation(home)) {
                    for (RobotInfo r : robots) {
                        if (r.getType() == RobotType.HQ && r.getLocation() == home) { isHomeHQ = true; break; }
                    }
                }
        
        
                boolean designExists = false;
                boolean fulfillExists = false;
                boolean refineryExists = false;
                boolean netgunExists = false;
        
                Direction towardsHome = currentLocation.directionTo(home);
                Direction oppositeHome = towardsHome.opposite();
        
                for (RobotInfo robot : robots) {
                    if (robot.getType() == RobotType.DESIGN_SCHOOL) designExists = true;
                    if (robot.getType() == RobotType.FULFILLMENT_CENTER) fulfillExists = true;
                    if (robot.getType() == RobotType.REFINERY) { refineryExists = true; home = robot.getLocation(); }
                    if (robot.getType() == RobotType.NET_GUN) netgunExists = true;
                }
        
                if (!designExists) {
                    if (rc.getTeamSoup() > 200 + roundNum / 8) {
                        rc.buildRobot(RobotType.DESIGN_SCHOOL, buildDirectionSpread(oppositeHome));
                    }
                    /*
                    for (Direction dir : directions) {
                        if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, dir) && rc.getTeamSoup() > 250) {
                            rc.buildRobot(RobotType.DESIGN_SCHOOL, dir);
                        }
                    }
                    */
                }
        
                if (!fulfillExists) {
                    if (rc.getTeamSoup() > 220 + roundNum / 30) {
                        rc.buildRobot(RobotType.FULFILLMENT_CENTER, buildDirectionSpread(oppositeHome));
                
                    }
                    /*
                    for (Direction dir : directions) {
                        if (rc.canBuildRobot(RobotType.FULFILLMENT_CENTER, dir) && rc.getTeamSoup() > 300) {
                            rc.buildRobot(RobotType.FULFILLMENT_CENTER, dir);
                        }
                    }
                    */
                }
        
                if (!netgunExists) {
                    if (rc.getTeamSoup() > 300) {
                        rc.buildRobot(RobotType.NET_GUN, buildDirectionSpread(oppositeHome));
                    }
                    /*
                    for (Direction dir : directions) {
                        if (rc.canBuildRobot(RobotType.NET_GUN, dir) && rc.getTeamSoup() > 500) {
                            rc.buildRobot(RobotType.NET_GUN, dir);
                        }
                    }
                    */
                }
        
                if (!refineryExists) {
                    if (rc.getTeamSoup() > 250 + roundNum / 12) {
                        rc.buildRobot(RobotType.REFINERY, buildDirectionSpread(oppositeHome));
                    }
                    /*
                    for (Direction dir : directions) {
                        if (rc.canBuildRobot(RobotType.REFINERY, dir) && rc.getTeamSoup() > 300) {
                            rc.buildRobot(RobotType.REFINERY, dir);
                        }
                    }
                    */
                }
            }
            
            
            
            boolean foundSoup = false;
            for (Direction dir : allDirs) {
                if (rc.canMineSoup(dir)) {
                    destination = rc.adjacentLocation(dir);
                    state = 2;
                    foundSoup = true;
                    break;
                }
            }
            
            
            // If there is no soup left at destination, look for more soup near the destination - need to optimize
            if (rc.canSenseLocation(destination) && rc.getLocation().distanceSquaredTo(destination) < 8) {
                if (rc.senseSoup(destination) == 0) {
                    state = 4;
                }
            }
            
            if (!foundSoup) {
                tryMovingTowards(destination);
            }
            
            
        }
        
        if (state == 2) {
            // System.out.println("CARRYING " + rc.getSoupCarrying());
            
            if (rc.getSoupCarrying() == 100) {
                state = 3;
            }
            else if (rc.canSenseLocation(destination) && rc.senseSoup(destination) == 0) { // If no soup left, go back to looking for more soup - maybe consider going back if already over a certain soup amount?
                state = 4;
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
    
            // Bad janky code to check refinery first - should clean up
            RobotInfo[] friendlyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
            for (RobotInfo robot : friendlyRobots) {
                if (robot.getType() == RobotType.REFINERY) { home = robot.getLocation(); }
    
            }
    
    
            if (currentLocation.distanceSquaredTo(home) < 15 && currentLocation.distanceSquaredTo(home) > 7) {
                boolean isHomeHQ = false;
                
                RobotInfo[] robots = rc.senseNearbyRobots(-1, rc.getTeam());
                
                if (rc.canSenseLocation(home)) {
                    for (RobotInfo r : robots) {
                        if (r.getType() == RobotType.HQ && r.getLocation() == home) { isHomeHQ = true; break; }
                    }
                }
    
                
                boolean designExists = false;
                boolean fulfillExists = false;
                boolean refineryExists = false;
                boolean netgunExists = false;
                
                Direction towardsHome = currentLocation.directionTo(home);
                Direction oppositeHome = towardsHome.opposite();
                
                for (RobotInfo robot : robots) {
                    if (robot.getType() == RobotType.DESIGN_SCHOOL) designExists = true;
                    if (robot.getType() == RobotType.FULFILLMENT_CENTER) fulfillExists = true;
                    if (robot.getType() == RobotType.REFINERY) { refineryExists = true; home = robot.getLocation(); }
                    if (robot.getType() == RobotType.NET_GUN) netgunExists = true;
                }
                
                if (!designExists) {
                    if (rc.getTeamSoup() > 200 + roundNum / 15) {
                        rc.buildRobot(RobotType.DESIGN_SCHOOL, buildDirectionSpread(oppositeHome));
                    }
                    /*
                    for (Direction dir : directions) {
                        if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, dir) && rc.getTeamSoup() > 250) {
                            rc.buildRobot(RobotType.DESIGN_SCHOOL, dir);
                        }
                    }
                    */
                }
                
                if (!fulfillExists) {
                    if (rc.getTeamSoup() > 250 + roundNum / 40) {
                        rc.buildRobot(RobotType.FULFILLMENT_CENTER, buildDirectionSpread(oppositeHome));
    
                    }
                    /*
                    for (Direction dir : directions) {
                        if (rc.canBuildRobot(RobotType.FULFILLMENT_CENTER, dir) && rc.getTeamSoup() > 300) {
                            rc.buildRobot(RobotType.FULFILLMENT_CENTER, dir);
                        }
                    }
                    */
                }
                
                if (!netgunExists) {
                    if (rc.getTeamSoup() > 400) {
                        rc.buildRobot(RobotType.NET_GUN, buildDirectionSpread(oppositeHome));
                    }
                    /*
                    for (Direction dir : directions) {
                        if (rc.canBuildRobot(RobotType.NET_GUN, dir) && rc.getTeamSoup() > 500) {
                            rc.buildRobot(RobotType.NET_GUN, dir);
                        }
                    }
                    */
                }
                
                if (!refineryExists) {
                    if (rc.getTeamSoup() > 300 + roundNum / 75) {
                        rc.buildRobot(RobotType.REFINERY, buildDirectionSpread(oppositeHome));
                    }
                    /*
                    for (Direction dir : directions) {
                        if (rc.canBuildRobot(RobotType.REFINERY, dir) && rc.getTeamSoup() > 300) {
                            rc.buildRobot(RobotType.REFINERY, dir);
                        }
                    }
                    */
                }
            }
            
            
            // If home is completely surrounded, just build a new home
            if (currentLocation.distanceSquaredTo(home) < 8) {
    
                int numSurrounding = 0;
                for (Direction dir : directions) {
                    MapLocation adj = home.add(dir);
                    if (rc.canSenseLocation(adj) && rc.isLocationOccupied(adj)) numSurrounding++;
                }
                if (numSurrounding == 8) {
                    for (Direction dir : directions) {
                        if (rc.canBuildRobot(RobotType.REFINERY, dir)) {
                            rc.buildRobot(RobotType.REFINERY, dir);
                        }
                    }
                }
    
                for (Direction dir : directions) {
                    if (rc.canDepositSoup(dir)) {
                        // System.out.println("DEPOSIT");
                        state = 1;
                        rc.depositSoup(dir, 100);
                    }
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
            
            
            MapLocation[] soupLocations = rc.senseNearbySoup();
            int numSoups = 0;
            
            int minDistance = Integer.MAX_VALUE;
            MapLocation soupLocation = currentLocation;
            for (int i = 0; i < soupLocations.length; i++) {
                MapLocation soup = soupLocations[i];
                int dist = currentLocation.distanceSquaredTo(soup);
                if (dist < minDistance) {
                    // Check if soup is actually accessible
                    boolean canReach = false;
                    for (Direction dir : directions) {
                        MapLocation soupAdj = soup.add(dir);
                        if (rc.canSenseLocation(soupAdj) && isAccessible(soupAdj) && !rc.senseFlooding(soupAdj)) { canReach = true; minDistance = dist; soupLocation = soupLocations[i]; numSoups++; break; }
                    }
                }
            }
    
            // System.out.println("NUM SOUPS FOUND " + numSoups);
    
    
    
            if (numSoups == 0) {
                
                // System.out.println("FOUND NO SOUPS, TRYING LAST ROUNDS MESSAGES");
                
                // Check only previous round's messages to see if soup has been found
                Transaction[] block = rc.getBlock(roundNum - 1);
                
                // System.out.println("GETTING BLOCK " + (roundNum - 1));
                
                
                boolean newSoupFound = false;
                for (Transaction t : block) {
                    int[] message = t.getMessage();
                    // System.out.println("MESSAGE 0 " + message[0]);
                    int previousRoundHash = computeHashForRound(roundNum - 1);
                    // System.out.println("PREVIOUS ROUND HASH " + previousRoundHash);
                    
                    if (message[0] == previousRoundHash && message[3] == previousRoundHash) {
                        // System.out.println("SOMEONE ELSE FOUND SOUP");
                        destination = new MapLocation(message[1], message[2]);
                        newSoupFound = true;
                    }
                }
                
                if (!newSoupFound) {
                    // System.out.println("EXPLROED");
                    for (int i = 0; i < 5; i++) {
                        System.out.print(explored[i] + " ");
                    }
                    // System.out.println();
    
                    if (rc.canSenseLocation(destination)) {
                        int quadrant = findQuadrant(currentLocation);
                        explored[quadrant] = true;
                        int nextQ = 0;
                        boolean foundNextQ = false;
                        for (int i = 1; i < 5; i++) {
                            if (!explored[i]) { nextQ = i; foundNextQ = true; break; }
                        }
                        // System.out.println("NEXT Q " + nextQ);
                        if (!foundNextQ) { explored = new boolean[5]; nextQ = quadrant; }
                        destination = getQuadrantCorner(nextQ);
                    }
                }
                
            }
            
            else {
                destination = soupLocation;
                
                int[] message = new int[7];
                message[0] = computeHashForCurrentRound();
                message[1] = soupLocation.x;
                message[2] = soupLocation.y;
                
                message[3] = computeHashForCurrentRound();
                
                message[4] = currentLocation.x;
                message[5] = currentLocation.y;
                
                message[6] = rc.getID();
                
                if (rc.canSubmitTransaction(message, 1)) {
                    rc.submitTransaction(message, 1);
                    // System.out.println("SUBMITTED FOUND SOUP LOCATION");
                }
    
                state = 1;
    
                boolean foundRefinery = false;
                RobotInfo[] robots = rc.senseNearbyRobots(-1, rc.getTeam());
                for (RobotInfo robot : robots) {
                    if (robot.getType() == RobotType.REFINERY || robot.getType() == RobotType.HQ) {
                        foundRefinery = true;
                        break;
                    }
                }
                
                if (!foundRefinery) {
                    for (Direction dir : directions) {
                        if (rc.canBuildRobot(RobotType.REFINERY, dir)) {
                            rc.buildRobot(RobotType.REFINERY, dir);
                        }
                    }
                }
    
            }
            
            tryMovingTowards(destination);
            
            
        }
        

        

        /*
        // tryBuild(randomSpawnedByMiner(), randomDirection());
        for (Direction dir : directions)
            tryBuild(RobotType.FULFILLMENT_CENTER, dir);
        for (Direction dir : directions)
            if (tryRefine(dir))
                // System.out.println("I refined soup! " + rc.getTeamSoup());
        for (Direction dir : directions)
            if (tryMine(dir))
                // System.out.println("I mined soup! " + rc.getSoupCarrying());
                
                */
    }
    
}