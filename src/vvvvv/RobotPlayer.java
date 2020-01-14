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
    static int numBuilt;
    
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
     * For landscapers: //wip, should revise
     * 1 - searching for HQ
     * 2 - dumping dirt on HQ
     *
     * 3 - defensive landscaper
     * 4 - looking if defense is needed
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
        
        Direction dirToMove = rc.getLocation().directionTo(goal);
        MapLocation nextStep = rc.adjacentLocation(dirToMove);

        
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
                for (int i = 0; i < 4; i++) {
                    left = left.rotateLeft();
        
                    MapLocation nextLeft = rc.adjacentLocation(left);
                    
                    if (rc.canMove(left) && !rc.senseFlooding(nextLeft)) {
                        rc.move(left);
                    }
                }
            }
            
            else if (beingBlocked == 2) {
                for (int i = 0; i < 4; i++) {
                    right = right.rotateLeft();
        
                    MapLocation nextRight = rc.adjacentLocation(right);
        
                    if (rc.canMove(right) && !rc.senseFlooding(nextRight)) {
                        rc.move(right);
                    }
                }
            }
            
            else {
                
                for (int i = 0; i < 4; i++) {
                    left = left.rotateLeft();
                    right = right.rotateRight();
        
                    MapLocation nextLeft = rc.adjacentLocation(left);
                    MapLocation nextRight = rc.adjacentLocation(right);
        
        
                    if (rc.canMove(left) && !rc.senseFlooding(nextLeft)) {
                        beingBlocked = 1;
                        rc.move(left);
                    }
                    if (rc.canMove(right) && !rc.senseFlooding(nextRight)) {
                        beingBlocked = 2;
                        rc.move(right);
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
