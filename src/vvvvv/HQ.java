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
            MapLocation soupLocation = new MapLocation(-1, -1);
            MapLocation home = rc.getLocation();
            for (int x = -7; x < 7; x++) {
                for (int y = -7; y < 7; y++) {
                    MapLocation loc = home.translate(x, y);
                    if (rc.canSenseLocation(loc)) {
                        if (rc.senseSoup(loc) != 0) { soupLocation = loc; break; }
                    }
                }
            }
            
            System.out.println("SOUP " + soupLocation);
            int[] message = new int[7];
            int generatedKey = (int) (Math.random() * Integer.MAX_VALUE);
            message[0] = generatedKey; // Identifier
            // TODO: order soups from closest to farthest
            if (soupLocation.x != -1) { // (soups.size() > 0) {
                MapLocation soup1 = soupLocation; // soups.pop();
                message[1] = soup1.x;
                message[2] = soup1.y;
                
            }
            else {
                message[1] = -1;
                message[2] = -1;
                
            }
            
            message[3] = rc.getLocation().x;
            message[4] = rc.getLocation().y;
            
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
        
        if ((turnCount % 40 == 0 || turnCount < 20) && turnCount < 250) {
            for (Direction dir : directions) {
                if (rc.canBuildRobot(RobotType.MINER, dir)) {
                    numBuilt++;
                    rc.buildRobot(RobotType.MINER, dir);
                }
            }
        }
        
    }
}