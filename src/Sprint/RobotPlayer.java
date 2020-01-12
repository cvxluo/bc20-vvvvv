package Sprint;
import battlecode.common.*;

// Check if this is best structure
import java.util.LinkedList;

import java.lang.Math;


@SuppressWarnings("unchecked")
public strictfp class RobotPlayer {
    static RobotController rc;

    static Direction[] directions = {
            Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST,
            Direction.NORTHEAST, Direction.NORTHWEST, Direction.SOUTHEAST, Direction.SOUTHWEST,
        
    };
    
    static Direction[] allDirs = {
            Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST,
            Direction.NORTHEAST, Direction.NORTHWEST, Direction.SOUTHEAST, Direction.SOUTHWEST,
            Direction.CENTER,
        
    };
    static RobotType[] spawnedByMiner = {RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL,
            RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN};
    
    // Constants to declare at beginning
    static Team enemy;
    static int turnCount;
    static int mapWidth;
    static int mapHeight;
    static MapLocation hqLocation;
    
    
    // Memory variables
    static MapLocation destination;
    static MapLocation home;
    static int numBuilt;
    
    
    static int state;
    /**
     * State Indicators:
     * 0 - just created
     *
     * For miners:
     * 1 - moving to mining location
     * 2 - mining soup
     * 3 - moving back to HQ to refine - looking to build stuff
     *
     * 4 - exploring map for more soup
     *
     *
     * For landscapers: //wip, should revise
     * 1 - searching for HQ
     * 2 - dumping dirt on HQ
     */
    
    
    
    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;
        
        // Declare necessary constants
        enemy = rc.getTeam().opponent();
        turnCount = 0;
        mapWidth = rc.getMapWidth();
        mapHeight = rc.getMapHeight();
        numBuilt = 0;
        
        
        home = rc.getLocation();
        
        
        state = 0;

        System.out.println("I'm a " + rc.getType() + " and I just got created at " + rc.getLocation());
        while (true) {
            turnCount += 1;
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You can add the missing ones or rewrite this into your own control structure.
                System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());
                switch (rc.getType()) {
                    case HQ:                 runHQ();                break;
                    case MINER:              runMiner();             break;
                    case REFINERY:           runRefinery();          break;
                    case VAPORATOR:          runVaporator();         break;
                    case DESIGN_SCHOOL:      runDesignSchool();      break;
                    case FULFILLMENT_CENTER: runFulfillmentCenter(); break;
                    case LANDSCAPER:         runLandscaper();        break;
                    case DELIVERY_DRONE:     runDeliveryDrone();     break;
                    case NET_GUN:            runNetGun();            break;
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    static void runHQ() throws GameActionException {
        
        if (turnCount == 1) {
            
            // TODO: Optimize this
            LinkedList<MapLocation> soups = new LinkedList<MapLocation>();
            for (int x = 0; x < mapWidth; x++) {
                for (int y = 0; y < mapHeight; y++) {
                    MapLocation loc = new MapLocation(x, y);
                    if (rc.canSenseLocation(loc)) {
                        if (rc.senseSoup(loc) != 0) { soups.add(loc); }
                    }
                }
            }

            System.out.println("SOUPS " + soups);
            int[] message = new int[7];
            message[0] = 1234567; // Identifier
            // TODO: order soups from closest to farthest
            if (soups.size() > 0) {
                MapLocation soup1 = soups.pop();
                message[1] = soup1.x;
                message[2] = soup1.y;
                
            }
            else {
                message[1] = -1;
                message[2] = -1;
                
            }
            
            message[3] = rc.getLocation().x;
            message[4] = rc.getLocation().y;
    
            message[5] = 341234;
            message[6] = rc.getID();
            
            if (rc.canSubmitTransaction(message, 20)) {
                rc.submitTransaction(message, 20);
                System.out.println("SUCCESSFULLY SUBMITTED TRANSACTION");
            }
        }
        
        if (turnCount % 40 == 0) {
            numBuilt++;
            for (Direction dir : directions) { tryBuild(RobotType.MINER, dir);  }
        }
        
    }

    static void runMiner() throws GameActionException {
        
        int roundNum = rc.getRoundNum();
        System.out.println("STATE " + state);
        System.out.println("DESTINATION " + destination);
        
        
        if (turnCount == 1) { // Setup stuff
            boolean foundSoupBlock = false;
            int[] message = new int[7];
            int blockCheck = 2;
    
            while (!foundSoupBlock) {
                System.out.println("USING BLOCK " + blockCheck);
                Transaction[] block = rc.getBlock(blockCheck);
                blockCheck++;
                for (Transaction transaction : block) {
                    int[] m = transaction.getMessage();
                    if (m[0] == 1234567) { message = m; foundSoupBlock = true; break; }
                }
            }
            MapLocation soupLocation = new MapLocation(message[1], message[2]);
            destination = soupLocation;
            
            state = 1;
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
            
            if (rc.getLocation().distanceSquaredTo(home) < 12) {
                RobotInfo[] robots = rc.senseNearbyRobots();
                boolean designExists = false;
                boolean fulfillExists = false;
                for (RobotInfo robot : robots) {
                    if (robot.getType() == RobotType.DESIGN_SCHOOL) designExists = true;
                    if (robot.getType() == RobotType.FULFILLMENT_CENTER) fulfillExists = true;
                }
                if (!designExists) {
                    for (Direction dir : directions) {
                        if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, dir) && rc.getTeamSoup() > 150) {
                            rc.buildRobot(RobotType.DESIGN_SCHOOL, dir);
                        }
                    }
                }
    
                if (!fulfillExists) {
                    for (Direction dir : directions) {
                        if (rc.canBuildRobot(RobotType.FULFILLMENT_CENTER, dir) && rc.getTeamSoup() > 150) {
                            rc.buildRobot(RobotType.FULFILLMENT_CENTER, dir);
                        }
                    }
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
    
            if (!foundSoup) {
                int quadrant = findQuadrant(rc.getLocation());
                if (quadrant == 1) destination = new MapLocation(0, 0);
                if (quadrant == 2) destination = new MapLocation(mapWidth - 1, 0);
                if (quadrant == 3) destination = new MapLocation(mapWidth - 1, mapHeight - 1);
                if (quadrant == 4) destination = new MapLocation(0, mapHeight - 1);
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

    static void runRefinery() throws GameActionException {
        // System.out.println("Pollution: " + rc.sensePollution(rc.getLocation()));
    }

    static void runVaporator() throws GameActionException {

    }

    static void runDesignSchool() throws GameActionException {
        
        if (rc.getTeamSoup() > 500 && numBuilt < 10) {
            numBuilt++;
            for (Direction dir : directions) {
                tryBuild(RobotType.LANDSCAPER, dir);
            }
        }

    }

    static void runFulfillmentCenter() throws GameActionException {
        
        if (rc.getTeamSoup() > 500 && numBuilt < 10) {
            numBuilt++;
            for (Direction dir : directions) {
                tryBuild(RobotType.DELIVERY_DRONE, dir);
            }
        }

    }

    static void runLandscaper() throws GameActionException {
        System.out.println("DESTINATION " + destination);
        System.out.println("STATE " + state);
    
        if (turnCount == 1) {
            int quadrant = findQuadrant(rc.getLocation());
            if (quadrant == 1) destination = new MapLocation(0, 0);
            if (quadrant == 2) destination = new MapLocation(mapWidth - 1, 0);
            if (quadrant == 3) destination = new MapLocation(mapWidth - 1, mapHeight - 1);
            if (quadrant == 4) destination = new MapLocation(0, mapHeight - 1);
            
            state = 1;
        }
        
        
        if (state == 1) {
            Team enemy = rc.getTeam().opponent();
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
    
        // Constantly grab some dirt from surroundings, and grab dirt if dumping dirt on a building
        if (turnCount % 15 == 0 || state == 2) {
            for (Direction dir : directions) {
                if (rc.canDigDirt(dir)) {
                    rc.digDirt(dir);
                }
            }
        }
    }

    static void runDeliveryDrone() throws GameActionException {
        System.out.println("DESTINATION " + destination);
        
        if (turnCount == 1) {
            int quadrant = findQuadrant(rc.getLocation());
            if (quadrant == 1) destination = new MapLocation(0, 0);
            if (quadrant == 2) destination = new MapLocation(mapWidth - 1, 0);
            if (quadrant == 3) destination = new MapLocation(mapWidth - 1, mapHeight - 1);
            if (quadrant == 4) destination = new MapLocation(0, mapHeight - 1);
        }
        
        Team enemy = rc.getTeam().opponent();
        if (!rc.isCurrentlyHoldingUnit()) {
            // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
            RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, enemy);

            if (robots.length > 0) {
                // Pick up a first robot within range
                rc.pickUpUnit(robots[0].getID());
                System.out.println("I picked up " + robots[0].getID() + "!");
            }

            RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, enemy);
            System.out.println("ENEMY ROBOTS NEARBY " + nearbyRobots.length);
            if (nearbyRobots.length > 0) {
                for (RobotInfo r : nearbyRobots) {
                    if (!r.getType().isBuilding()) destination = nearbyRobots[0].getLocation();
                }
                
            }
            
            else {
                System.out.println("DRONE SEARCHING");
                droneTryMovingTowards(destination);
            }
        } else {
            // If holding unit, do something

            
            
        }
    }

    static void runNetGun() throws GameActionException {

    }
    
    
    // Will end the turn
    // If tile to move is occupied, tries going left a little, then right a little
    static boolean tryMovingTowards(MapLocation goal) throws GameActionException {
        // remember to not use this with drones, because they can go over flooded tiles
        
        Direction dirToMove = rc.getLocation().directionTo(goal);
        MapLocation nextStep = rc.adjacentLocation(dirToMove);

        
        if (rc.canMove(dirToMove) && !rc.senseFlooding(nextStep)) {
            rc.move(dirToMove);
        }
        
        else {
            System.out.println("BLOCKED");
            Direction left = dirToMove;
            Direction right = dirToMove;
            
            for (int i = 0; i < 4; i++) {
                left = left.rotateLeft();
                right = right.rotateRight();
    
                MapLocation nextLeft = rc.adjacentLocation(left);
                MapLocation nextRight = rc.adjacentLocation(right);
    
    
                if (rc.canMove(left) && !rc.senseFlooding(nextLeft)) {
                    rc.move(left);
                }
                if (rc.canMove(right) && !rc.senseFlooding(nextRight)) {
                    rc.move(right);
                }
            }
            
            // If no valid moves are found
            return false;
            
        }
        
        return true;
    
    }
    
    // Will end the turn
    // If tile to move is occupied, tries going left a little, then right a little
    static boolean droneTryMovingTowards(MapLocation goal) throws GameActionException {
        
        Direction dirToMove = rc.getLocation().directionTo(goal);
        MapLocation nextStep = rc.adjacentLocation(dirToMove);
        
        
        if (rc.canMove(dirToMove)) {
            rc.move(dirToMove);
        }
        
        else {
            System.out.println("BLOCKED");
            Direction left = dirToMove;
            Direction right = dirToMove;
            
            for (int i = 0; i < 4; i++) {
                left = left.rotateLeft();
                right = right.rotateRight();
                
                MapLocation nextLeft = rc.adjacentLocation(left);
                MapLocation nextRight = rc.adjacentLocation(right);
                
                
                if (rc.canMove(left)) {
                    rc.move(left);
                }
                if (rc.canMove(right)) {
                    rc.move(right);
                }
            }
            
            // If no valid moves are found
            return false;
            
        }
        
        return true;
        
    }
    
    
    
    // Standard quadrant system
    static int findQuadrant(MapLocation here) {
        if (here.x < mapWidth / 2) {
            if (here.y < mapHeight / 2) return 3;
            else return 2;
        }
        else {
            if (here.y < mapHeight / 2) return 4;
            else return 1;
        }
    }
    
    static int getElevation() {
        double E = 2.718281828459045;
        int x = rc.getRoundNum();
        return 1;
        // return (int) (Math.exp(0.0028*x - 1.38*Math.sin(0.00157*x - 1.73) + 1.38*sin(-1.73)) - 1);
    }

    /**
     * Attempts to build a given robot in a given direction.
     *
     * @param type The type of the robot to build
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            return true;
        } else return false;
    }
    

}
