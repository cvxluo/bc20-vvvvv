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
        
        if (!rc.isCurrentlyHoldingUnit()) {
            
            if (!rc.isReady()) Clock.yield();
            
            // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
            RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, enemy);
            
            RobotInfo[] friendlyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
            
            for (RobotInfo robot : robots) {
                if (!robot.getType().isBuilding() && !robot.getType().canFly()) {
                    
                    boolean alreadyPickedUp = false;
                    
                    for (RobotInfo friendly : friendlyRobots) {
                        if (friendly.getLocation() == robot.getLocation()) { // Check to see if another drone has already picked the bot up
                            alreadyPickedUp = true;
                            break;
                        }
                    }
                    
                    if (!alreadyPickedUp) {
                        System.out.println("I picked up " + robots[0].getID() + "!");
                        rc.pickUpUnit(robot.getID());
                    }
                    
                }
            }
            
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, enemy);
            System.out.println("ENEMY ROBOTS NEARBY " + nearbyRobots.length);
            
            boolean foundTarget = false;
            for (RobotInfo r : nearbyRobots) {
                if (!r.getType().isBuilding() && !r.getType().canFly()) {
                    
                    boolean alreadyPickedUp = false;
                    
                    for (RobotInfo friendly : friendlyRobots) {
                        if (friendly.getLocation() == r.getLocation()) {
                            alreadyPickedUp = true;
                        }
                    }
    
                    if (!alreadyPickedUp) {
                        destination = nearbyRobots[0].getLocation();
                        foundTarget = true;
                        System.out.println("FOUND TARGET");
                        break;
    
                    }
                }
            }

            
            if (!foundTarget) {
                System.out.println("DRONE SEARCHING");
                
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
                    System.out.println("NEXT Q " + nextQ);
                    if (!foundNextQ) { explored = new boolean[5]; nextQ = quadrant; }
                    if (nextQ == 3) destination = new MapLocation(0, 0);
                    if (nextQ == 4) destination = new MapLocation(mapWidth - 1, 0);
                    if (nextQ == 1) destination = new MapLocation(mapWidth - 1, mapHeight - 1);
                    if (nextQ == 2) destination = new MapLocation(0, mapHeight - 1);
        
                }
            }
            
    
            System.out.println("TRY MOVING TOWARDS DESTINATION");
            droneTryMovingTowards(destination);
    
    
    
    
        } else {
    
            if (!rc.isReady()) Clock.yield();
    
            
            
            // If holding unit, do something
            System.out.println("RUNNING NOT HOLDING UNIT CODE");
            
            MapLocation me = currentLocation;
            for (Direction dir : directions) {
                MapLocation loc = me.add(dir);
                System.out.println("LOOKING AT " + loc + " TO DROP");
                if (rc.canSenseLocation(loc)) {
                    if (rc.senseFlooding(loc)) {
                        System.out.println("TRYING TO DROP IN " + loc);
                        Direction dropDir = currentLocation.directionTo(loc);
                        if (rc.canDropUnit(dropDir)) rc.dropUnit(dropDir);
                    }
                }
            }
        
            
            // kinda bad also should optimize
            boolean foundFlood = false;
            MapLocation floodLocation = new MapLocation(-1, -1);
            MapLocation home = currentLocation;
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
                int quadrant = findQuadrant(currentLocation);
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