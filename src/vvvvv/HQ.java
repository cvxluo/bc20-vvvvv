package vvvvv;
import battlecode.common.*;


public strictfp class HQ extends RobotPlayer {
    
    static boolean sentPanicMessage;
    
    static int soupsFound;
    static int numLandscapersSurrounding;
    static int roundsSinceLastLandscaper;
    
    static int numDefensiveSpaces;
    
    static void runHQ() throws GameActionException {
        
        if (turnCount == 1) {
            
            
            sentPanicMessage = false;
            soupsFound = 0;
            numLandscapersSurrounding = 0;
            roundsSinceLastLandscaper = 0;
            
            MapLocation home = currentLocation;
            
            // Find closest soup
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
            
            // Find number of defensive spaces - spaces that landscapers should be on
            // Bad, since it doesn't check if the wall is completed - should revise
            MapLocation[] x = getXShape(currentLocation);
            numDefensiveSpaces = 0;
            for (MapLocation loc : x) {
                if (rc.onTheMap(loc) && !rc.senseFlooding(loc)) numDefensiveSpaces++;
            }
            
            
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
            
            message[5] = 1919191;
            message[6] = numDefensiveSpaces;
            
            if (rc.canSubmitTransaction(message, 30)) {
                rc.submitTransaction(message, 30);
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
        
        MapLocation[] xshape = getXShape(currentLocation);
        for (MapLocation loc : xshape) {
            if (rc.canSenseLocation(loc) && rc.isLocationOccupied(loc)) {
                RobotInfo r = rc.senseRobotAtLocation(loc);
                if (r.getType() == RobotType.LANDSCAPER && rc.getTeam() == r.getTeam()) {
                    landscapersDetected++;
                }
            }
        }
        
        // After round 200, start considering panicing if no more landscapers are coming
        if (roundNum > 200) {
            // System.out.println("LANDSCAPERS DETECTED " + landscapersDetected);
            // System.out.println("NUM LANDSDCAPERS SRU " + numLandscapersSurrounding);
            
            if (landscapersDetected > numLandscapersSurrounding) {
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
                // System.out.println("SUBMITTED PANIC");
    
                // System.out.println("SUBMITTED PANIC");
    
                // System.out.println("SUBMITTED PANIC");
                // System.out.println("SUBMITTED PANIC");
    
                // System.out.println("SUBMITTED PANIC");
    
            }
        }
        
        // System.out.println("ROUNDS SINCE LAST LANDSCAPER " + roundsSinceLastLandscaper);
        // System.out.println("HAS SENT PANIC " + sentPanicMessage);
        
        
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
        
        
        // Regularly broadcast updates on information
        if (roundNum % 10 == 0) {
            int[] message = new int[7];
            int currentHash = computeHashForCurrentRound();
    
            message[0] = currentHash; // Identifier
            
            message[1] = landscapersDetected;
            
            message[2] = 5;
            
            if (sentPanicMessage) message[3] = currentHash - 3;
            else message[3] = 0;
            
            message[4] = currentLocation.x;
            message[5] = currentLocation.y;
            
            message[6] = rc.getID();
    
            if (rc.canSubmitTransaction(message, 1)) {
                rc.submitTransaction(message, 1);
                // System.out.println("SUCCESSFULLY SUBMITTED REGULAR MESSAGE");
            }
            
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