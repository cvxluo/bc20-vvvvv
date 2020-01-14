package vvvvv;
import battlecode.common.*;

public strictfp class FulfillmentCenter extends RobotPlayer {
    
    static void runFulfillmentCenter() throws GameActionException {
        
        if (rc.getTeamSoup() > 500 && numBuilt < 10) {
            numBuilt++;
            for (Direction dir : directions) {
                tryBuild(RobotType.DELIVERY_DRONE, dir);
            }
        }
        
    }
    
}