package vvvvv;
import battlecode.common.*;

public strictfp class DesignSchool extends RobotPlayer {
    
    static void runDesignSchool() throws GameActionException {
        
        if (rc.getTeamSoup() > 500 && numBuilt < 10) {
            numBuilt++;
            for (Direction dir : directions) {
                tryBuild(RobotType.LANDSCAPER, dir);
            }
        }
        
    }
    
}