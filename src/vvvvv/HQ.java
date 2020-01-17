package vvvvv;
import battlecode.common.*;


public strictfp class HQ extends RobotPlayer {
    
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
            
            // kinda bad also should optimize
            MapLocation home = currentLocation;
            
            MapLocation[] soupLocations = rc.senseNearbySoup();
            int numSoups = soupLocations.length;
    
            numToBuild = numSoups;
            System.out.println("NUM MINERS TO BUILD " + numToBuild);
            
            int minDistance = Integer.MAX_VALUE;
            MapLocation soupLocation = home;
            for (int i = 0; i < numSoups; i++) {
                MapLocation soupToCheck = soupLocations[i];
                int dist = home.distanceSquaredTo(soupToCheck);
                if (dist < minDistance) { minDistance = dist; soupLocation = soupToCheck; }
            }
            
            System.out.println("SOUP " + soupLocation);
            int[] message = new int[7];
            
            // Creating a key for the later hashing
            int generatedKey = (int) (Math.random() * Integer.MAX_VALUE);
            message[0] = generatedKey; // Identifier

            MapLocation soup1 = soupLocation; // soups.pop();
            message[1] = soup1.x;
            message[2] = soup1.y;
            
            message[3] = currentLocation.x;
            message[4] = currentLocation.y;
            
            message[5] = 7654321;
            message[6] = rc.getID();
            
            if (rc.canSubmitTransaction(message, 30)) {
                rc.submitTransaction(message, 30);
                System.out.println("SUCCESSFULLY SUBMITTED TRANSACTION");
            }
        }
        
        // Shoot down any drones if seen
        RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);
        for (RobotInfo robot : robots) {
            if (robot.getType() == RobotType.DELIVERY_DRONE) {
                int id = robot.getID();
                if (rc.canShootUnit(id)) {
                    rc.shootUnit(id);
                }
            }
        }
        
        int numSurrounding = 0;
        for (Direction dir : directions) {
            if (rc.isLocationOccupied(rc.adjacentLocation(dir))) numSurrounding++;
        }
        
        System.out.println("NUM SURROUNDING " + numSurrounding);
        
        // If there is lots of soup nearby, build many miners immediately, but slow down faster - needs to be a bit better
        if (((turnCount % 40 == 0 && turnCount < 400 - numToBuild * 8) || 6 > numBuilt) && numSurrounding < 7) {
            System.out.println("BUILDING MINER");
            for (Direction dir : directions) {
                if (rc.canBuildRobot(RobotType.MINER, dir)) {
                    numBuilt++;
                    rc.buildRobot(RobotType.MINER, dir);
                }
            }
        }
        
    }
}