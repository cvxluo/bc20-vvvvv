package vvvvv;
import battlecode.common.*;

public strictfp class Drone extends RobotPlayer {
    
    static boolean foundFloodedTile;
    static MapLocation floodedTile;
    
    static void runDeliveryDrone() throws GameActionException {
        
        System.out.println("IN STATE " + state);
        System.out.println("DESTINATION " + destination);
        
        if (turnCount == 1) {
    
            int[] message = findFirstMessageByContent(1919191, 5); // Guarenteed that this first block will exist
    
            hashAtRound = message[0];
            firstHash = hashAtRound;
            roundLastHashed = 1;
    
            MapLocation homeLocation = new MapLocation(message[3], message[4]);
            home = homeLocation;
            hqLocation = homeLocation;
    
            destination = home;
            
            state = 4;
        }
    
        System.out.println("RUNNING DRONE LOOP");
        Transaction[] b = rc.getBlock(roundNum - 1);
        for (Transaction t : b) {
            System.out.println("RECIEVED TRANSACTION");
            int[] message = t.getMessage();
            int previousRoundHash = computeHashForRound(roundNum - 1);
        
            if (message[0] == previousRoundHash && message[1] == previousRoundHash && message[2] == previousRoundHash
                    && message[4] == previousRoundHash && message[5] == previousRoundHash) {
                System.out.println("RECIEVED POOR WALL MESSAGE");
                state = 3;
            }
        }
        
        
        
        if (state == 1) {
    
            // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
            RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, enemy);
    
    
            for (RobotInfo robot : robots) {
                if (rc.canPickUpUnit(robot.getID())) {
                    System.out.println("I picked up " + robots[0].getID() + "!");
                    state = 2;
                    rc.pickUpUnit(robot.getID());
            
                }
            }
    
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, enemy);
            System.out.println("ENEMY ROBOTS NEARBY " + nearbyRobots.length);
    
            boolean foundTarget = false;
            for (RobotInfo r : nearbyRobots) {
                if (!r.getType().isBuilding() && !r.getType().canFly()) {
                    destination = nearbyRobots[0].getLocation();
                    foundTarget = true;
                    System.out.println("FOUND TARGET");
                    break;
                    
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
                    if (!foundNextQ) { explored = new boolean[5]; nextQ = quadrant; }
                    System.out.println("NEXT Q " + nextQ);
                    destination = getQuadrantCorner(nextQ);
            
                }
            }
    
    
            System.out.println("TRY MOVING TOWARDS DESTINATION");
            droneTryMovingTowards(destination);
    
        }
        
        if (state == 2) {
            
            MapLocation me = currentLocation;
            for (Direction dir : directions) {
                MapLocation loc = me.add(dir);
                System.out.println("LOOKING AT " + loc + " TO DROP");
                if (rc.canSenseLocation(loc)) {
                    if (rc.senseFlooding(loc)) {
                        System.out.println("TRYING TO DROP IN " + loc);
                        Direction dropDir = currentLocation.directionTo(loc);
                        if (rc.canDropUnit(dropDir)) { rc.dropUnit(dropDir); state = 1; }
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
                            floodedTile = loc;
                            foundFloodedTile = true;
                            foundFlood = true;
                            break;
                        }
                    }
                }
            }
            
            System.out.println("HAVE FOUND FLOOD TO DROP");
            System.out.println("FLOOD AT  " + destination);
            
            if (!foundFlood) {
                if (foundFloodedTile) {
                    destination = floodedTile;
                }
                
                else {
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
                    destination = getQuadrantCorner(nextQ);
                    if (!foundNextQ) { explored = new boolean[5]; nextQ = quadrant; }
                }
            }
            
            
            droneTryMovingTowards(destination);
            
            
        }
        
        if (state == 3) {
            System.out.println("TRYING TO SURROUND HQ");
            
            if (rc.isCurrentlyHoldingUnit()) {
                MapLocation me = currentLocation;
                for (Direction dir : directions) {
                    MapLocation loc = me.add(dir);
                    System.out.println("LOOKING AT " + loc + " TO DROP");
                    if (rc.canSenseLocation(loc)) {
                        if (rc.senseFlooding(loc)) {
                            System.out.println("TRYING TO DROP IN " + loc);
                            Direction dropDir = currentLocation.directionTo(loc);
                            if (rc.canDropUnit(dropDir)) { rc.dropUnit(dropDir); }
                        }
                    }
                }
            }
    
            // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
            RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, enemy);
            
            for (RobotInfo robot : robots) {
                if (rc.canPickUpUnit(robot.getID())) {
                    System.out.println("I picked up " + robots[0].getID() + "!");
                    rc.pickUpUnit(robot.getID());
            
                }
            }
            
            if (currentLocation.distanceSquaredTo(home) < 14) {
                for (Direction dir : directions) {
                    MapLocation adj = home.add(dir);
                    
                    Direction left = dir.rotateLeft();
                    Direction right = dir.rotateRight();
                    
                    MapLocation forward = adj.add(dir);
                    MapLocation l = adj.add(left);
                    MapLocation r = adj.add(right);
                    
                    if (rc.canSenseLocation(forward) && !rc.isLocationOccupied(forward)) { destination = forward; }
                    else if (rc.canSenseLocation(l) && !rc.isLocationOccupied(l)) { destination = l; }
                    else if (rc.canSenseLocation(r) && !rc.isLocationOccupied(r)) { destination = r; }
                    else {
                        droneTryMovingTowards(destination);
                    }
                    
                }
            }
            else {
                droneTryMovingTowards(home);
            }
        }
        
        
        if (state == 4) {
    
            // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
            RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, enemy);
    
            for (RobotInfo robot : robots) {
                if (rc.canPickUpUnit(robot.getID())) {
                    System.out.println("I picked up " + robots[0].getID() + "!");
                    rc.pickUpUnit(robot.getID());
                    state = 2;
                }
            }
            
            
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, enemy);
            if (nearbyRobots.length > 0) {
                state = 1;
            }
            if (currentLocation.distanceSquaredTo(home) < 10) {
                int landscapers = 0;
                
                for (Direction dir : directions) {
                    MapLocation adj = home.add(dir);
                    if (rc.canSenseLocation(adj) && rc.isLocationOccupied(adj) && rc.senseRobotAtLocation(adj).getType() == RobotType.LANDSCAPER) {
                        landscapers++;
                        System.out.println(dir);
                    }
                }
                
                System.out.println("NUM SURROUNDING " + landscapers);
                
                if (landscapers == 8) {
                    state = 3;
                }
                else {
                    state = 1;
                }
            }
            else {
                droneTryMovingTowards(home);
            }
        }
    }
    
}