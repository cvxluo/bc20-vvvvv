package vvvvv;
import battlecode.common.*;

public strictfp class DesignSchool extends RobotPlayer {
    
    static void runDesignSchool() throws GameActionException {
        
        if (rc.getTeamSoup() > 400 && numBuilt < 10) {
            numBuilt++;
            for (Direction dir : directions) {
                if (rc.canBuildRobot(RobotType.LANDSCAPER, dir)) {
                    rc.buildRobot(RobotType.LANDSCAPER, dir);
                }
            }
        }
        
    }
    
}