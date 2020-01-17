package vvvvv;
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
    
    static RobotType whatIsHome;
    
    static int numBuilt;
    static int numToBuild;
    
    static int hashAtRound;
    static int roundLastHashed;
    static int lastHash;
    
    static MapLocation currentLocation;
    static int roundNum;
    static boolean[] explored;
    static int beingBlocked;
    
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
     * For landscapers:
     * 1 - searching for HQ
     * 2 - dumping dirt on HQ
     *
     * 3 - defensive landscaper
     * 4 - looking if defense is needed
     *
     *
     * For drones:
     * 1 - searching for enemy unit to pick up, not holding anything
     * 2 - picked up enemy unit, searching for flood to drop in
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
        numToBuild = 0;
        
        
        home = rc.getLocation();
        
        state = 0;
        explored = new boolean[5];
        beingBlocked = 0;

        System.out.println("I'm a " + rc.getType() + " and I just got created at " + rc.getLocation());
        while (true) {
            turnCount += 1;
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You can add the missing ones or rewrite this into your own control structure.
                System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());
                
                currentLocation = rc.getLocation();
                roundNum = rc.getRoundNum();
                
                switch (rc.getType()) {
                    case HQ:                 HQ.runHQ();                               break;
                    case MINER:              Miner.runMiner();                         break;
                    case REFINERY:           Refinery.runRefinery();                   break;
                    case VAPORATOR:          Vaporator.runVaporator();                 break;
                    case DESIGN_SCHOOL:      DesignSchool.runDesignSchool();           break;
                    case FULFILLMENT_CENTER: FulfillmentCenter.runFulfillmentCenter(); break;
                    case LANDSCAPER:         Landscaper.runLandscaper();               break;
                    case DELIVERY_DRONE:     Drone.runDeliveryDrone();                 break;
                    case NET_GUN:            Netgun.runNetGun();                       break;
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }
    
    
    
    // Will end the turn
    // If tile to move is occupied, tries going left a little, then right a little
    static boolean tryMovingTowards(MapLocation goal) throws GameActionException {
        // remember to not use this with drones, because they can go over flooded tiles
        
        Direction dirToMove = currentLocation.directionTo(goal);
        MapLocation nextStep = rc.adjacentLocation(dirToMove);
        
        System.out.println("GOAL IN DIRECTION " + dirToMove);

        if (!rc.isReady()) Clock.yield();
        
        if (rc.canMove(dirToMove) && !rc.senseFlooding(nextStep)) {
            beingBlocked = 0;
            rc.move(dirToMove);
        }
        
        else {
            
            System.out.println("BLOCKED");
            System.out.println(beingBlocked);
            Direction left = dirToMove;
            Direction right = dirToMove;
            
            if (beingBlocked == 1) {
                // Only move left if being blocked
                for (int i = 0; i < 6; i++) {
                    left = left.rotateLeft();
                    MapLocation nextLeft = rc.adjacentLocation(left);
                    
                    System.out.println("IN LEFT FOLLOWING");
                    System.out.println("TRYING " + left);
                    System.out.println("IN LOCATION " + nextLeft);
                    
                    if (rc.canMove(left) && !rc.senseFlooding(nextLeft)) {
                        rc.move(left);
                        return true;
                    }
                }
                
            }
            
            else if (beingBlocked == 2) {
                for (int i = 0; i < 6; i++) {
                    right = right.rotateRight();
                    MapLocation nextRight = rc.adjacentLocation(right);
    
                    System.out.println("IN RIGHT FOLLOWING");
                    System.out.println("TRYING " + right);
                    System.out.println("IN LOCATION " + nextRight);
    
    
                    if (rc.canMove(right) && !rc.senseFlooding(nextRight)) {
                        rc.move(right);
                        return true;
                    }
                }
                
            }
            
            else {
                
                for (int i = 0; i < 4; i++) {
                    left = left.rotateLeft();
                    right = right.rotateRight();
        
                    MapLocation nextLeft = rc.adjacentLocation(left);
                    MapLocation nextRight = rc.adjacentLocation(right);
    
                    System.out.println("TRYING " + left);
                    System.out.println("TRYING " + right);
                    System.out.println("IN LOCATION " + nextLeft);
                    System.out.println("IN LOCATION " + nextRight);
    
    
    
    
                    if (rc.canMove(left) && !rc.senseFlooding(nextLeft)) {
                        beingBlocked = 1;
                        rc.move(left);
                        return true;
                    }
                    if (rc.canMove(right) && !rc.senseFlooding(nextRight)) {
                        beingBlocked = 2;
                        rc.move(right);
                        return true;
                    }
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
        
        
        if (rc.isReady()) {
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
        
            }
    
            // If no valid moves are found
            return false;
    
        }
    
        return true;
    
    }
    
    // Find first message by its contents
    static int[] findFirstMessageByContent(int check, int index) throws GameActionException {
        System.out.println("FIRST MESSAGE BY CONTENT CALLED");
        System.out.println(roundNum);
        
        for (int i = 1; i < roundNum; i++) {
            Transaction[] block = rc.getBlock(i);
            
            System.out.println("CHECKING BLOCK " + i);
            
            for (Transaction t : block) {
                int[] message = t.getMessage();
                if (message[index] == check) {
                    return message;
                }
            }
        }
        
        return new int[7];
        
    }
    
    // Find last message by its contents
    static int[] findLastMessageByContent(int check, int index) throws GameActionException {
        
        for (int i = roundNum - 1; i >= 1; i--) {
            Transaction[] block = rc.getBlock(i);
    
            for (Transaction t : block) {
                int[] message = t.getMessage();
                if (message[index] == check) {
                    return message;
                }
            }
        }
        
        return new int[7];
        
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
    
    // Takes either 1, 2, 3, or 4 only
    static MapLocation getQuadrantCorner(int q) {
        if (q == 1) return new MapLocation(mapWidth - 1, mapHeight - 1);
        if (q == 2) return new MapLocation(0, mapHeight - 1);
        if (q == 3) return new MapLocation(0, 0);
        if (q == 4) return new MapLocation(mapWidth - 1, 0);
        
        return new MapLocation(-1, -1);
    }
    
    
    // Hashing function for communication
    static int hash (int h) { return (h * 31) % 65436; }
    
    static int computeHashForRound(int r, int initialHash) {
        int h = initialHash;
        for (int i = 1; i < r; i++) {
            h = hash(h);
        }
        return h;
    }
    
    static void updateLastHash () throws GameActionException {
        if (roundNum < 2) return; // Probably should find a way to optimize this away, but it's only one bytecode per turn so maybe not
        
        Transaction[] block = rc.getBlock(roundNum - 1);
        
        for (Transaction transaction : block) {
            int[] message = transaction.getMessage();
            if (message[0] == hash(lastHash)) { lastHash = message[0]; }
        }
        
    }
    
    static Direction buildDirectionSpread (Direction dir) throws GameActionException{
        Direction left = dir;
        Direction right = dir;
        
        if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, dir)) { // cheapest test robot - maybe revise
            return dir;
        }
        else {
            for (int i = 0; i < 4; i++) {
                left = left.rotateLeft();
                right = right.rotateRight();
                
                if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, left)) return left;
                if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, right)) return right;
    
            }
        }
        
        return dir.opposite();
        
    }
    
    static int getElevation() {
        double E = 2.718281828459045;
        int x = rc.getRoundNum();
        return (int) (Math.exp(0.0028*x - 1.38*Math.sin(0.00157*x - 1.73) + 1.38*Math.sin(-1.73)) - 1);
    }
    
    
    // Checks whether goal is currently within elevation limits
    // Does not check whether the location is in sensor radius
    static boolean isAccessible(MapLocation goal) throws GameActionException { return Math.abs(rc.senseElevation(goal) - rc.senseElevation(rc.getLocation())) <= 3; }
    

}
