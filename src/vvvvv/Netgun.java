package vvvvv;
import battlecode.common.*;

public strictfp class Netgun extends RobotPlayer {
    
    static void runNetGun() throws GameActionException {
        
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
        
    }
    
}