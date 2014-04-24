// -*- java -*-

package eem.motion;

import eem.EvBot;
import eem.target.*;
import eem.misc.*;
import java.util.Random;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.Arrays;
import robocode.util.*;
import java.awt.Color;


public class basicMotion {
	protected EvBot myBot;
	protected double battleFieldHeight = 0;
	protected double battleFieldWidth = 0;
	protected int robotHalfSize;
	protected double dangerForPointTouchingTheWall = 1e6; // humongous number
	protected double dangerLevelWall = 50;
	protected double safe_distance_from_wall;


	public void initTic() {
	}

	public basicMotion() {
	}

	public basicMotion(EvBot bot) {
		myBot = bot;
		battleFieldWidth  = bot.BattleField.x;
		battleFieldHeight = bot.BattleField.y;
		robotHalfSize = bot.robotHalfSize;
		safe_distance_from_wall = robotHalfSize + 2;
	}

	public double bearingTo( Point2D.Double  pt) {
		return math.shortest_arc(
			math.cortesian2game_angles( Math.atan2( pt.y-myBot.myCoord.y, pt.x-myBot.myCoord.x )*180/Math.PI )
			);
	}

	public void moveToPoint( Point2D.Double pnt ) {
		double angle = math.shortest_arc( bearingTo(pnt) - myBot.getHeading() );
		double dist = myBot.myCoord.distance(pnt);
		if ( Math.abs(angle ) > 90 ) {
			if (angle > 90 ) {
				angle = angle - 180;
			} else {
				angle = angle + 180;
			}
			dist = -dist;
		}
		myBot.setTurnRight(angle);
		myBot.setAhead (dist);
	}

	public void makeMove() {
		// for basic motion we do nothing
	}

	public void onPaint(Graphics2D g) {
	}

	// --- Utils -----
	public double maxRotationPerTurnInDegrees(double speed) {
		//double speed = Math.abs( myBot._tracker.getLast().getSpeed() );
		return (10 - 0.75 * Math.abs(speed)); // see robowiki
	}

	public double stopDistance( double velocity ) {
		double speed = Math.abs(velocity);
		int dist =0;

		speed =- 2;
		while ( speed > 0 ) {
			dist += speed;
			speed -= 2;
		}
		return dist;
	}

	public double stopDistanceVector( double velocity ) {
		return -stopDistanceVector(velocity)*math.sign(velocity);
	}

	public double pointDangerFromWalls( Point2D.Double p, double speed ) {
		double danger = 0;
		double dx = dist2LeftOrRightWall( p );
		double dy = dist2BottomOrTopWall( p );
		if ( shortestDist2wall( p ) <= ( robotHalfSize + stopDistance(speed) + 2) ) {
			// point within physical no-go zone
			// danger must be infinite to prevent going there
			danger += dangerForPointTouchingTheWall;
		}
		danger += math.gaussian( dx, dangerLevelWall, safe_distance_from_wall );
		danger += math.gaussian( dy, dangerLevelWall, safe_distance_from_wall );
		return danger;
	}

	public double dist2LeftOrRightWall( Point2D.Double p ) {
		double dLeft  = p.x; // left wall distance
		double dRight = battleFieldWidth - p.x; // right wall distance
		if ( ( dLeft <= 0 ) || ( dRight <= 0 ) ) {
			// point is outside of wall
			return 0;
		}
		return Math.min( dLeft, dRight);
	}

	public double dist2BottomOrTopWall( Point2D.Double p ) {
		double dBottom = p.y; // bottom wall distance
		double dTop    = battleFieldHeight - p.y; // top wall distance
		if ( ( dTop <= 0 ) || ( dBottom <= 0 ) ) {
			// point is outside of wall
			return 0;
		}
		return Math.min( dBottom, dTop);
	}

	public double shortestDist2wall( Point2D.Double p ) {
		return  Math.min( dist2LeftOrRightWall( p ), dist2BottomOrTopWall( p ) );
	}

	public double angleToClosestCorner() {
		double x=myBot.myCoord.x;
		double y=myBot.myCoord.y;
		// corner coordinates
		double cX=0; 
		double cY=0; 
		
		if (x <= myBot.BattleField.x/2) {
			// left corner is closer
			cX=0;
		} else {
			// left corner is closer
			cX=myBot.BattleField.x;
		}
		if (y <= myBot.BattleField.y/2) {
			// lower corner is closer
			cY=0;
		} else {
			// upper corner is closer
			cY=myBot.BattleField.y;
		}
		logger.noise("the closest corner is at " + cX + ", " + cY);
		return bearingTo(new Point2D.Double(cX,cY) );
	}

	public double shortestTurnRadiusVsSpeed() {
		// very empiric for full speed
		return 115;
	}

	public String whichWallAhead() {
		return math.whichWallAhead( myBot.myCoord, myBot.getVelocity(), myBot.getHeadingRadians() );
	}

	public double distanceToWallAhead() {
		return math.distanceToWallAhead( myBot.myCoord, myBot.getVelocity(), myBot.getHeadingRadians() );
	}

	public double distanceToTheClosestWallFrom( double px, double py ) {
		double[] d={px, myBot.BattleField.x -px, py, myBot.BattleField.y-py};
		Arrays.sort(d);
		return d[0];
	}

	public double whichWayToRotateAwayFromWall() {
		double angle = myBot.getHeading();
		String wallName = whichWallAhead();

		if ( myBot.getVelocity() < 0 ) 
			angle += 180; // we are moving backwards
		angle = math.shortest_arc(angle);
	        double x = myBot.myCoord.x;
	        double y = myBot.myCoord.y;
		int rotDir = 1;
		double retAngle=0;

		logger.noise("heading angle = " + angle);

		if ( wallName.equals("left") ) {
			if ( -90 <= angle && angle <= 0 ) {
				retAngle = -angle;
			} else {
				retAngle = -180 - angle;
			}
		}
		if ( wallName.equals("right") ) {
			if ( 0 <= angle && angle <= 90 ) {
				retAngle = - angle;
			} else {
				retAngle = 180 - angle;
			}
		}
		if ( wallName.equals("bottom") ) {
			if ( 90 <= angle && angle <= 180 ) {
				retAngle = 90 - angle;
			} else {
				retAngle = -90 - angle;
			}
		}
		if ( wallName.equals("top") ) {
			if ( 0 <= angle && angle <= 90 ) {
				retAngle =  90-angle;
			} else {
				retAngle = -90 - angle;
			}
		}

		//retAngle += 20*desiredBodyRotationDirection; // add a bit of momentum
		logger.noise("body heading = " + angle);
		logger.noise("rotation from wall is " + retAngle);
		return retAngle;
	}

}
