package vvvvv;
import battlecode.common.*;

public strictfp class FulfillmentCenter extends RobotPlayer {
    
    static void runFulfillmentCenter() throws GameActionException {
        
        if (rc.getTeamSoup() > 500 && numBuilt < 10) {
            System.out.println("ATTEMPTING TO BUILD DRONE");
            for (Direction dir : directions) {
                if (rc.canBuildRobot(RobotType.DELIVERY_DRONE, dir)) {
                    numBuilt++;
                    rc.buildRobot(RobotType.DELIVERY_DRONE, dir);
                }
            }
        }
        
    }
    
}