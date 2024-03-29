package Seeding;
import battlecode.common.*;


public strictfp class HQ extends RobotPlayer {
    
    static boolean sentPanicMessage;
    
    static int soupsFound;
    static int numLandscapersSurrounding;
    static int roundsSinceLastLandscaper;
    
    static void runHQ() throws GameActionException {
        
        if (turnCount == 1) {
            
            // TODO: Optimize this
            /*
            LinkedList<MapLocation> soups = new LinkedList<MapLocation>();
            for (int x = 0; x < mapWidth; x++) {
                for (int y = 0; y < mapHeight; y++) {
                    MapLocation loc = new MapLocation(x, y);
                    if (rc.canSenseLocation(loc)) {
                        if (rc.senseSoup(loc) != 0) { soups.add(loc); }
                    }
                }
            }
            */
            
            
            sentPanicMessage = false;
            soupsFound = 0;
            numLandscapersSurrounding = 0;
            roundsSinceLastLandscaper = 0;
            
            // kinda bad also should optimize
            MapLocation home = currentLocation;
            
            MapLocation[] soupLocations = rc.senseNearbySoup();
            int numSoups = soupLocations.length;
            int totalSoup = 0;
    
            numToBuild = numSoups;
            // System.out.println("NUM MINERS TO BUILD " + numToBuild);
            
            int minDistance = Integer.MAX_VALUE;
            MapLocation soupLocation = home;
            for (int i = 0; i < numSoups; i++) {
                MapLocation soupToCheck = soupLocations[i];
                totalSoup += rc.senseSoup(soupToCheck);
                int dist = home.distanceSquaredTo(soupToCheck);
                if (dist < minDistance) { minDistance = dist; soupLocation = soupToCheck; }
            }
            
            // System.out.println("SOUP " + soupLocation);
            int[] message = new int[7];
            
            // Creating a key for the later hashing
            int generatedKey = (int) (Math.random() * Integer.MAX_VALUE);
            hashAtRound = generatedKey;
            roundLastHashed = 1;
            firstHash = generatedKey;
            
            message[0] = generatedKey; // Identifier

            MapLocation soup1 = soupLocation; // soups.pop();
            message[1] = soup1.x;
            message[2] = soup1.y;
            
            message[3] = currentLocation.x;
            message[4] = currentLocation.y;
            
            message[5] = 7654321;
            message[6] = rc.getID();
            
            if (rc.canSubmitTransaction(message, 20)) {
                rc.submitTransaction(message, 20);
                // System.out.println("SUCCESSFULLY SUBMITTED TRANSACTION");
            }
        }
    
        updateHashToRound(Math.max(1, roundNum - 20));
    
        // Shoot down any drones if seen
        RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);
        if (robots.length > 0) {
            int closestDroneID = robots[0].getID();
            int closestDist = Integer.MAX_VALUE;
            
            for (RobotInfo robot : robots) {
                if (robot.getType() == RobotType.DELIVERY_DRONE) {
                    int id = robot.getID();
                    if (currentLocation.distanceSquaredTo(robot.getLocation()) < closestDist) {
                        closestDist = currentLocation.distanceSquaredTo(robot.getLocation());
                        closestDroneID = id;
                    }
                }
        
                else if (robot.getType() == RobotType.LANDSCAPER) {
                    // panic
                }
            }
    
            if (rc.canShootUnit(closestDroneID)) {
                rc.shootUnit(closestDroneID);
            }
        }

    
        int landscapersDetected = 0;
    
        for (Direction dir : directions) {
            MapLocation adj = rc.adjacentLocation(dir);
            if (rc.canSenseLocation(adj) && rc.isLocationOccupied(adj)) {
                RobotInfo r = rc.senseRobotAtLocation(adj);
                if (r.getType() == RobotType.LANDSCAPER && rc.getTeam() == r.getTeam()) {
                    landscapersDetected++;
                }
            }
        }
        
        // After round 200, start considering panicing if no more landscapers are coming
        if (roundNum > 200) {
            // System.out.println("LANDSCAPERS DETECTED " + landscapersDetected);
            // System.out.println("NUM LANDSDCAPERS SRU " + numLandscapersSurrounding);
            
            // If we already have 8 landscapers, don't do anything
            if (landscapersDetected == 8) {
                roundsSinceLastLandscaper = 0;
            }
            else if (landscapersDetected > numLandscapersSurrounding) {
                numLandscapersSurrounding = landscapersDetected;
                roundsSinceLastLandscaper = 0;
            }
            else {
                roundsSinceLastLandscaper++;
            }
            
            
        }
        
        
        int numFloodedTiles = 0;
        for (int x = -7; x < 7; x++) {
            for (int y = -7; y < 7; y++) {
                MapLocation check = currentLocation.translate(x, y);
                if (rc.canSenseLocation(check) && rc.senseFlooding(check)) {
                    numFloodedTiles++;
                }
            }
        }
        
        // System.out.println("NUM FLOODED TILES " + numFloodedTiles);
        
        if ((numFloodedTiles > 50 || (roundsSinceLastLandscaper > 125 + rc.getTeamSoup() / 10)) && !sentPanicMessage && turnCount > 250) {
            int[] message = new int[7];
            int currentRoundHash = computeHashForCurrentRound();
            message[0] = currentRoundHash;
            message[1] = currentRoundHash;
            message[2] = currentRoundHash;
            message[3] = 1;
            message[4] = currentRoundHash;
            message[5] = currentRoundHash;
    
            message[6] = rc.getID();
            
            if (rc.canSubmitTransaction(message, 1)) {
                sentPanicMessage = true;
                rc.submitTransaction(message, 1);
                // System.out.println("SUBMITTED PANIC");
            }
        }
        
        
        boolean shouldBuildAnother = false;
        if (turnCount > 1) {
            Transaction[] block = rc.getBlock(roundNum - 1);
    
            for (Transaction t : block) {
                int[] message = t.getMessage();
                // System.out.println("MESSAGE 0 " + message[0]);
                int previousRoundHash = computeHashForRound(roundNum - 1);
                // System.out.println("PREVIOUS ROUND HASH " + previousRoundHash);
        
                if (message[0] == previousRoundHash && message[3] == previousRoundHash) {
                    // System.out.println("SOMEONE ELSE FOUND SOUP - DETECTED AT HQ");
                    destination = new MapLocation(message[1], message[2]);
                    soupsFound++;
                }
            }
    
            if (soupsFound > 6) { soupsFound -= 6; shouldBuildAnother = true; }
        }
        
        
        
        // If there is lots of soup nearby, build many miners immediately, but slow down faster - needs to be a bit better
        if (((turnCount % 50 == 0 && turnCount < 400) || 4 > numBuilt) && !sentPanicMessage) {
            // System.out.println("BUILDING MINER");
            for (Direction dir : directions) {
                if (rc.canBuildRobot(RobotType.MINER, dir)) {
                    numBuilt++;
                    rc.buildRobot(RobotType.MINER, dir);
                }
            }
        }
        
    }
}