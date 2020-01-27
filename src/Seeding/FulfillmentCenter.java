package Seeding;
import battlecode.common.*;

public strictfp class FulfillmentCenter extends RobotPlayer {
    
    static void runFulfillmentCenter() throws GameActionException {
        
        if (rc.getTeamSoup() > 325 - roundNum / 6 && numBuilt < 5 + roundNum / 300) {
            // System.out.println("ATTEMPTING TO BUILD DRONE");
            for (Direction dir : directions) {
                if (rc.canBuildRobot(RobotType.DELIVERY_DRONE, dir)) {
                    numBuilt++;
                    rc.buildRobot(RobotType.DELIVERY_DRONE, dir);
                }
            }
        }
        
    }
    
}