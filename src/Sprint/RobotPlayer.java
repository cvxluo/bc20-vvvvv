package Sprint;
import battlecode.common.*;

// Check if this is best structure
import java.util.LinkedList;


@SuppressWarnings("unchecked")
public strictfp class RobotPlayer {
    static RobotController rc;

    static Direction[] directions = {
            Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST,
            Direction.NORTHEAST, Direction.NORTHWEST, Direction.SOUTHEAST, Direction.SOUTHWEST,
    
    };
    static RobotType[] spawnedByMiner = {RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL,
            RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN};
    
    // Constants to declare at beginning
    static Team enemy;
    static int turnCount;
    static int mapWidth;
    static int mapHeight;
    static MapLocation hqLocation;
    
    
    static int state;
    /**
     * State Indicators:
     * 0 - just created
     *
     * For miners:
     * 1 - moving to mining location
     * 2 - mining soup
     * 3 - moving back to HQ to refine
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
    
                message[3] = rc.getLocation().x;
                message[4] = rc.getLocation().y;
                
            }
            
            message[5] = 341234;
            message[6] = rc.getID();
            
            if (rc.canSubmitTransaction(message, 20)) {
                rc.submitTransaction(message, 20);
                System.out.println("SUCCESSFULLY SUBMITTED TRANSACTION");
            }
        }
        
        if (turnCount < 40) {
            for (Direction dir : directions) tryBuild(RobotType.MINER, dir);
        }
        
        
        
    }

    static void runMiner() throws GameActionException {
        
        int roundNum = rc.getRoundNum();
        System.out.println("STATE " + state);
        System.out.println("CD " + rc.getCooldownTurns());
        
        if (state == 0 || state == 1) { // TODO: optimize this, miner doesn't need to check where soup is every turn
            
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
            state = 1;
            
            
            MapLocation soupLocation = new MapLocation(message[1], message[2]);
            
            for (Direction dir : directions) {
                if (rc.canMineSoup(dir)) {
                    state = 2;
                }
            }
    
            Direction dirToMove = rc.getLocation().directionTo(soupLocation);

            if (rc.canMove(dirToMove)) {
                rc.move(dirToMove);
            }
        }
        
        if (state == 2) {
            System.out.println("IM A MINER CARRYING " + rc.getSoupCarrying());
            if (rc.getSoupCarrying() == 100) {
                state = 3;
            }
            else {
                for (Direction dir : directions) {
                    if (rc.canMineSoup(dir)) {
                        rc.mineSoup(dir);
                    }
                }
            }
        }
        
        if (state == 3) {
            
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
            
            MapLocation hqLocation = new MapLocation(message[3], message[4]);
            
            
            for (Direction dir : directions) {
                if (rc.canDepositSoup(dir)) {
                    System.out.println("DEPOSIT");
                    state = 1;
                    rc.depositSoup(dir, 100);
                }
            }
    
            Direction dirToMove = rc.getLocation().directionTo(hqLocation);
            if (rc.canMove(dirToMove)) {
                rc.move(dirToMove);
            }
            
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

    }

    static void runFulfillmentCenter() throws GameActionException {
        for (Direction dir : directions)
            tryBuild(RobotType.DELIVERY_DRONE, dir);
    }

    static void runLandscaper() throws GameActionException {

    }

    static void runDeliveryDrone() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        if (!rc.isCurrentlyHoldingUnit()) {
            // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
            RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, enemy);

            if (robots.length > 0) {
                // Pick up a first robot within range
                rc.pickUpUnit(robots[0].getID());
                System.out.println("I picked up " + robots[0].getID() + "!");
            }
        } else {
            // No close robots, so search for robots within sight radius
            tryMove(randomDirection());
        }
    }

    static void runNetGun() throws GameActionException {

    }
    
    
    static LinkedList<MapLocation> findPath(MapLocation destination) {
        MapLocation currentPosition = rc.getLocation();
        LinkedList path = new LinkedList<MapLocation>();
        
        LinkedList<MapLocation> queue = new LinkedList<MapLocation>();
        
        queue.add(rc.getLocation());
        queue.add(destination);
        
        return path;
        
    }

    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    /**
     * Returns a random RobotType spawned by miners.
     *
     * @return a random RobotType
     */
    static RobotType randomSpawnedByMiner() {
        return spawnedByMiner[(int) (Math.random() * spawnedByMiner.length)];
    }

    static boolean tryMove() throws GameActionException {
        for (Direction dir : directions)
            if (tryMove(dir))
                return true;
        return false;
        // MapLocation loc = rc.getLocation();
        // if (loc.x < 10 && loc.x < loc.y)
        //     return tryMove(Direction.EAST);
        // else if (loc.x < 10)
        //     return tryMove(Direction.SOUTH);
        // else if (loc.x > loc.y)
        //     return tryMove(Direction.WEST);
        // else
        //     return tryMove(Direction.NORTH);
    }

    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        // System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        if (rc.isReady() && rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else return false;
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

    /**
     * Attempts to mine soup in a given direction.
     *
     * @param dir The intended direction of mining
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMineSoup(dir)) {
            rc.mineSoup(dir);
            return true;
        } else return false;
    }

    /**
     * Attempts to refine soup in a given direction.
     *
     * @param dir The intended direction of refining
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryRefine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canDepositSoup(dir)) {
            rc.depositSoup(dir, rc.getSoupCarrying());
            return true;
        } else return false;
    }


    static void tryBlockchain() throws GameActionException {
        if (turnCount < 3) {
            int[] message = new int[7];
            for (int i = 0; i < 7; i++) {
                message[i] = 123;
            }
            if (rc.canSubmitTransaction(message, 10))
                rc.submitTransaction(message, 10);
        }
        // System.out.println(rc.getRoundMessages(turnCount-1));
    }
}
