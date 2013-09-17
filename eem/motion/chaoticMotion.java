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


public class chaoticMotion extends basicMotion {
	int nonexisting_coord = -10000;
	double angle = nonexisting_coord;
	boolean executingWallEvadingTurn = false;
	// bot tangent position at the starboard/port (right/left) 
	// at minimal turning radius at the current speed
	double starboardStickX = nonexisting_coord;
	double starboardStickY = nonexisting_coord;
	double portStickX = nonexisting_coord;
	double portStickY = nonexisting_coord;
	String previoslyHeadedWall = "none";

	public chaoticMotion(EvBot bot) {
		myBot = bot;
	}
	
	public void makeMove() {
		double angle = nonexisting_coord;
		double angleRandDeviation = nonexisting_coord;
		double dist = nonexisting_coord;
		myBot.dbg(myBot.dbg_noise, "Normal motion algorithm");
		if (myBot.getOthers()>=5 && Math.random() < 0.2 ) { 
			//move to the closest corner as long as there are a lot of bots
			double angle2corner = angleToClosestCorner();
			myBot.dbg(myBot.dbg_noise, "angle to the closest corner = " + angle2corner );
			angle = math.shortest_arc( angle2corner - myBot.getHeading());
			dist = 50;
			if ( Math.abs(angle) > 90 ) {
				// moving backwards is faster sometimes
				angle = math.shortest_arc(angle - 180);
				dist = -dist;
			}
			myBot.dbg(myBot.dbg_noise, "moving to the closest corner with rotation by " + angle );
		}
		if ( myBot._trgt.haveTarget && (myBot.getOthers() <= 1) && (Math.random() < 0.95) ) {
			// last enemy standing lets spiral in
			angle = math.shortest_arc( -90 + (myBot.angle2target() - myBot.getHeading() ) );
			if ( Math.abs(angle) > 90 ) {
				if (angle > 0) {
					angle = angle - 180;
				} else {
					angle = angle + 180;
				}
			}
			if ( (Math.random() < 0.10) ) {
				myBot.dbg(myBot.dbg_noise, "setting a new motion" );
				dist=200*(0.5-Math.random());
				// but we need to move at least a half bot body
				if (Math.abs(dist) < 50) {
					dist += 50*math.sign(dist);
				}
			} else {
				myBot.dbg(myBot.dbg_noise, "continue previous motion" );
				dist = myBot.getDistanceRemaining();
			}
			myBot.dbg(myBot.dbg_noise, "circle around last enemy by rotating = " + angle );
		} 

		if ( myBot.getOthers() > 1 && (Math.random() < 0.95) ) {
			dist = myBot.getDistanceRemaining();
			angle = myBot.getTurnRemaining();
			if ( Math.abs(dist) > 20 ) {
				myBot.dbg(myBot.dbg_noise, "continue previous motion" );
			} else {
				angleRandDeviation=45*math.sign(0.5-Math.random());
				dist=100*math.sign(0.6-Math.random());
				angle =  angleRandDeviation;
			}
		} 

		if ( dist == nonexisting_coord && angle == nonexisting_coord ) {
			// make preemptive evasive motion
			angleRandDeviation=25*math.sign(0.5-Math.random());
			dist=100*math.sign(0.6-Math.random());
			angle =  angleRandDeviation;
			myBot.dbg(myBot.dbg_noise, "Random evasive motion");
		}

		moveOrTurn(dist, angle);
	}

	// --- Utils ----------
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
		myBot.dbg(myBot.dbg_noise, "the closest corner is at " + cX + ", " + cY);
		return bearingTo(cX,cY);
	}

	public double bearingTo( double ptx, double pty ) {
		return math.shortest_arc(
			math.cortesian2game_angles( Math.atan2( pty-myBot.myCoord.y, ptx-myBot.myCoord.x )*180/Math.PI )
			);
	}

	public void moveOrTurn(double dist, double suggestedAngle) {
		double angle=0;
		double moveLength;
		double shortestTurnRadius=shortestTurnRadiusVsSpeed();
		double hardStopDistance = 20;
		calculateSticksEndsPosition();
		String wallAhead = whichWallAhead();
		String furtherestStick = whichStickIsFurtherFromWalls();
		double distFromStickEndToWall = distToTheClosestWallFromStick(furtherestStick);
		myBot.dbg(myBot.dbg_noise, "furtherestStick = " + furtherestStick );

		double evadeWallDist = shortestTurnRadius+45;
		double wallAheadDist;
		wallAheadDist = distanceToWallAhead();
		myBot.dbg(myBot.dbg_noise, "moveOrTurn suggested dist =  " + dist + ", angle =" + suggestedAngle);
		myBot.dbg(myBot.dbg_noise, "hardStopDistance =  " + hardStopDistance);
		myBot.dbg(myBot.dbg_noise, "Wall ahead is " + wallAhead );
		myBot.dbg(myBot.dbg_noise, "wallAheadDist =  " + wallAheadDist);
		myBot.dbg(myBot.dbg_noise, "getDistanceRemaining =  " + myBot.getDistanceRemaining());
		myBot.dbg(myBot.dbg_noise, "rotate away from a wall by " + whichWayToRotateAwayFromWall() );
		myBot.dbg(myBot.dbg_noise, "Robot velocity =  " + myBot.getVelocity());
		if (wallAheadDist < hardStopDistance ) {
			// wall is close trying to stop
			myBot.dbg(myBot.dbg_noise, "Wall ahead is " + wallAhead );
			angle = whichWayToRotateAwayFromWall();
			executingWallEvadingTurn=true;
			if ( Utils.isNear(myBot.getVelocity(),0) ) {
					myBot.dbg(myBot.dbg_noise, "Wall is too close, backward is faster");
					dist = -41;
					myBot.setTurnRight(0);
			} else {
				dist = stopDistance(myBot.getVelocity());
				myBot.dbg(myBot.dbg_noise, "Robot velocity =  " + myBot.getVelocity());
				myBot.dbg(myBot.dbg_noise, "Trying to stop by setting distance = " + dist);
			}
			myBot.setAhead(dist); // this is emergency stop or hit a wall
			return;

			//dist = -dist; // hard stop and reverse
			//angle = 0; // do not rotate

		} 
		if (  distFromStickEndToWall <= evadeWallDist && wallAheadDist <= evadeWallDist ){
				// make hard turn
				myBot.dbg(myBot.dbg_noise, "Trying to turn away from walls" );
				executingWallEvadingTurn = true;
				if ( furtherestStick.equals("starboard") ) {
					angle = 20*math.sign( myBot.getVelocity() );
					//dist  = myBot.getDistanceRemaining();
				} else {
					angle = -20*math.sign( myBot.getVelocity() );
					//dist  = myBot.getDistanceRemaining();
				}
				if (myBot.getVelocity() == 0 ) {
					// if bot velocity is 0 give it a kick
					angle =0; 
					dist = -41;
				}
				myBot.setAhead (dist);
				myBot.setTurnRight(angle);
				return;
		} 
		
		//if ( distFromStickEndToWall > evadeWallDist) {
			executingWallEvadingTurn = false;
			previoslyHeadedWall = "none";
			myBot.dbg(myBot.dbg_noise, "getDistanceRemaining = " + myBot.getDistanceRemaining());
			//if (  Math.abs(myBot.getDistanceRemaining()) <=  0 ) {
				myBot.dbg(myBot.dbg_noise, "Proceeding with suggested motion");
				angle = suggestedAngle;
				dist = dist;
			//} else {
				//myBot.dbg(myBot.dbg_noise, "Continue previous turn motion");
				//dist =myBot.getDistanceRemaining();
				//angle = 0;
			//}
		//}
		myBot.dbg(myBot.dbg_noise, "Moving by " + dist);
		myBot.dbg(myBot.dbg_noise, "Turning by " + angle);
		myBot.setTurnRight(angle);
		myBot.setBodyRotationDirection( math.sign(angle) );
		myBot.setAhead(dist);
	}

	public double shortestTurnRadiusVsSpeed() {
		// very empiric for full speed
		return 115;
	}

	public String whichWallAhead() {
		double angle=myBot.getHeadingRadians(); 
		double velocity=myBot.getVelocity();
		double x = myBot.myCoord.x;
		double y = myBot.myCoord.y;

		String wallName="";

		if ( Utils.isNear(velocity, 0.0) ) {
			// we are not moving anywhere 
			// assigning fake velocity
			velocity = 8;
		}

		double dx = Math.sin( angle )*velocity;
		double dy = Math.cos( angle )*velocity;

		while (  wallName.equals("") ) {
			x+= dx;
			y+= dy;
			myBot.dbg(myBot.dbg_noise, "Projected position = " + x + ", " + y);

			if ( x-myBot.robotHalfSize <= 0 ) {
				wallName = "left";
			}
			if ( y-myBot.robotHalfSize <= 0 ) {
				wallName = "bottom";
			}
			if ( x >= myBot.BattleField.x-myBot.robotHalfSize ) {
				wallName = "right";
			}
			if ( y >= myBot.BattleField.y-myBot.robotHalfSize ) {
				wallName = "top";
			}
		}
		myBot.dbg(myBot.dbg_noise, "Wall name = " + wallName);
		return wallName;
	}

	public double distanceToWallAhead() {
		double angle=myBot.getHeading(); 
		double velocity=myBot.getVelocity();
		myBot.dbg(myBot.dbg_noise, "Our velocity = " + velocity);
		double dist=0;

		String wallName = whichWallAhead();

		if ( wallName.equals("left") ) {
				dist = myBot.myCoord.x;
		}	
		if ( wallName.equals("right") ) {
				dist = myBot.BattleField.x - myBot.myCoord.x;
		}
		if ( wallName.equals("bottom") ) {
				dist = myBot.myCoord.y;
		}
		if ( wallName.equals("top") ) {
				dist = myBot.BattleField.y - myBot.myCoord.y;
		}
		dist = dist - myBot.robotHalfSize;
		dist = Math.max(dist,0);
		if (dist < 1) dist = 0 ;
		myBot.dbg(myBot.dbg_noise, "distance to closest wall ahead " + dist);
		return dist;
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

		myBot.dbg(myBot.dbg_noise, "heading angle = " + angle);

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
		myBot.dbg(myBot.dbg_noise, "body heading = " + angle);
		myBot.dbg(myBot.dbg_noise, "rotation from wall is " + retAngle);
		return retAngle;
	}

	public void calculateSticksEndsPosition() {
		double r=shortestTurnRadiusVsSpeed();
		double a=myBot.getHeadingRadians();
		starboardStickX = myBot.myCoord.x + r*Math.sin(a+Math.PI/2);
		starboardStickY = myBot.myCoord.y + r*Math.cos(a+Math.PI/2);

		portStickX = myBot.myCoord.x + r*Math.sin(a-Math.PI/2);
		portStickY = myBot.myCoord.y + r*Math.cos(a-Math.PI/2);
	}

	public String whichStickIsFurtherFromWalls( ) {
		String stick="none";
		double starboardDist=distToTheClosestWallFromStick( "starboard" );
		double portDist=distToTheClosestWallFromStick( "port" );

		if ( starboardDist >= portDist ) {
			stick="starboard";
		} else {
			stick="port";
		}
		return stick;
	}


	public double distToTheClosestWallFromStick( String stick ) {
		double dist=0;
		if ( stick.equals("starboard") ) {
			dist=distanceToTheClosestWallFrom( starboardStickX, starboardStickY );
		} else {
			dist=distanceToTheClosestWallFrom( portStickX, portStickY );
		}
		return dist;
	}

	public int stopDistance( double velocity ) {
		int dist =0;
		if (Math.abs(velocity) == 0 ) 
			dist = 0;
		if (Math.abs(velocity) == 1 ) 
			dist =1;
		if (Math.abs(velocity) == 2 ) 
			dist =2;
		if (Math.abs(velocity) == 3 ) 
			dist =3+1;
		if (Math.abs(velocity) == 4 ) 
			dist =4+2;
		if (Math.abs(velocity) == 5 ) 
			dist =5+3+1;
		if (Math.abs(velocity) == 6 ) 
			dist =6+4+2;
		if (Math.abs(velocity) == 7 ) 
			dist =7+5+3+1;
		if (Math.abs(velocity) == 8 ) 
			dist =8+6+4+2;
		dist=-dist*math.sign(velocity);
		return dist;
	}

	public boolean areBothSticksEndAtField() {
		if ( distToTheClosestWallFromStick("starboard") > 0 && distToTheClosestWallFromStick("port") > 0 ) {
			return true;
		} 
		return false;
	}

	public double distanceToTheClosestWallFrom( double px, double py ) {
		double[] d={px, myBot.BattleField.x -px, py, myBot.BattleField.y-py};
		Arrays.sort(d);
		return d[0];
	}

}
