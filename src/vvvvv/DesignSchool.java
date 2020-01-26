package vvvvv;
import battlecode.common.*;

public strictfp class DesignSchool extends RobotPlayer {
    
    static void runDesignSchool() throws GameActionException {
        
        if (rc.getTeamSoup() > 200 + roundNum / 12 && numBuilt < 10 + roundNum / 300) {
            for (Direction dir : directions) {
                if (rc.canBuildRobot(RobotType.LANDSCAPER, dir)) {
                    numBuilt++;
                    rc.buildRobot(RobotType.LANDSCAPER, dir);
                }
            }
        }
        
    }
    
}