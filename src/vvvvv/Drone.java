package vvvvv;
import battlecode.common.*;

public strictfp class Drone extends RobotPlayer {
    
    static void runDeliveryDrone() throws GameActionException {
        System.out.println("DESTINATION " + destination);
        
        if (turnCount == 1) {
            /*
            int quadrant = findQuadrant(rc.getLocation());
            if (quadrant == 1) destination = new MapLocation(0, 0);
            if (quadrant == 2) destination = new MapLocation(mapWidth - 1, 0);
            if (quadrant == 3) destination = new MapLocation(mapWidth - 1, mapHeight - 1);
            if (quadrant == 4) destination = new MapLocation(0, mapHeight - 1);
            */
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
        }
        
        if (!rc.isCurrentlyHoldingUnit()) {
            // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
            RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, enemy);
            
            for (RobotInfo robot : robots) {
                if (!robot.getType().isBuilding()) {
                    System.out.println("I picked up " + robots[0].getID() + "!");
                    rc.pickUpUnit(robot.getID());
                }
            }
            
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, enemy);
            System.out.println("ENEMY ROBOTS NEARBY " + nearbyRobots.length);
            
            for (RobotInfo r : nearbyRobots) {
                if (!r.getType().isBuilding()) destination = nearbyRobots[0].getLocation();
            }

            
            System.out.println("DRONE SEARCHING");
                
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
            

            droneTryMovingTowards(destination);
    
        } else {
            // If holding unit, do something
            
            
            for (int x = -1; x < 1; x++) {
                for (int y = -1; y < 1; y++) {
                    MapLocation loc = rc.getLocation().translate(x, y);
                    if (rc.canSenseLocation(loc)) {
                        if (rc.senseFlooding(loc)) {
                            Direction dropDir = rc.getLocation().directionTo(loc);
                            if (rc.canDropUnit(dropDir)) rc.dropUnit(dropDir);
                        }
                    }
                }
            }
            
            // kinda bad also should optimize
            boolean foundFlood = false;
            MapLocation floodLocation = new MapLocation(-1, -1);
            MapLocation home = rc.getLocation();
            for (int x = -5; x < 5; x++) {
                for (int y = -5; y < 5; y++) {
                    MapLocation loc = home.translate(x, y);
                    if (rc.canSenseLocation(loc)) {
                        if (rc.senseFlooding(loc)) {
                            destination = loc;
                            foundFlood = true;
                            break;
                        }
                    }
                }
            }
            
            System.out.println("HAVE FOUND FLOOD TO DROP");
            System.out.println("FLOOD AT  " + destination);
            
            if (!foundFlood) {
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
            }
            
            
            droneTryMovingTowards(destination);
            
            
        }
    }
    
}