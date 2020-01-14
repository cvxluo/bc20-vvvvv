package vvvvv;
import battlecode.common.*;

public strictfp class Netgun extends RobotPlayer {
    
    static void runNetGun() throws GameActionException {
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
    }
    
}