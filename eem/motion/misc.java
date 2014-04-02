// -*- java -*-

package eem.motion;

import eem.EvBot;
import java.awt.geom.Point2D;
import robocode.*;
import eem.misc.*;

public class misc  {
	private static double battleFieldHeight = 0;
	private static double battleFieldWidth = 0;
	private static int robotHalfSize;
	private static double dangerForPointTouchingTheWall = 1e6; // humongous number
	private static double dangerLevelWall = 50;
	private static double safe_distance_from_wall;

	public static void init(EvBot bot) {
		battleFieldWidth  = bot.BattleField.x;
		battleFieldHeight = bot.BattleField.y;
		robotHalfSize = bot.robotHalfSize;
		safe_distance_from_wall = robotHalfSize + 2;
	}	

	public static double dist2LeftOrRightWall( Point2D.Double p ) {
		double dLeft  = p.x; // left wall distance
		double dRight = battleFieldWidth - p.x; // right wall distance
		if ( ( dLeft <= 0 ) || ( dRight <= 0 ) ) {
			// point is outside of wall
			return 0;
		}
		return Math.min( dLeft, dRight);
	}

	public static double dist2BottomOrTopWall( Point2D.Double p ) {
		double dBottom = p.y; // bottom wall distance
		double dTop    = battleFieldHeight - p.y; // top wall distance
		if ( ( dTop <= 0 ) || ( dBottom <= 0 ) ) {
			// point is outside of wall
			return 0;
		}
		return Math.min( dBottom, dTop);
	}

	public static double shortestDist2wall( Point2D.Double p ) {
		return  Math.min( dist2LeftOrRightWall( p ), dist2BottomOrTopWall( p ) );
	}

	public static double pointDangerFromWalls( Point2D.Double p, double speed ) {
		double danger = 0;
		double dx = dist2LeftOrRightWall( p );
		double dy = dist2BottomOrTopWall( p );
		if ( misc.shortestDist2wall( p ) <= ( robotHalfSize + stopDistance(speed) + 2) ) {
			// point within physical no-go zone
			// danger must be infinite to prevent going there
			danger += dangerForPointTouchingTheWall;
		}
		danger += math.gaussian( dx, dangerLevelWall, safe_distance_from_wall );
		danger += math.gaussian( dy, dangerLevelWall, safe_distance_from_wall );
		return danger;
	}

	public static double stopDistance(double speed) {
		// distance required to stop a bot with given speed
		//double speed = Math.abs( myBot._tracker.getLast().getSpeed() );
		// due to robot physics stop distance is simple arithmetic progression
		double stopDistance =  Math.ceil(speed/2.0) * (speed + 2)/2.0;
		// FIXME: above is not good if speed is odd
		return stopDistance;
	}
	
}
