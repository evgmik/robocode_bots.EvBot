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
		super(bot);
	}
	
	public void makeMove() {
		double angle = nonexisting_coord;
		double angleRandDeviation = nonexisting_coord;
		double dist = nonexisting_coord;
		logger.noise("Normal motion algorithm");
		if (myBot.numEnemyBotsAlive >=5 && Math.random() < 0.2 ) { 
			//move to the closest corner as long as there are a lot of bots
			double angle2corner = angleToClosestCorner();
			logger.noise("angle to the closest corner = " + angle2corner );
			angle = math.shortest_arc( angle2corner - myBot.getHeading());
			dist = 50;
			if ( Math.abs(angle) > 90 ) {
				// moving backwards is faster sometimes
				angle = math.shortest_arc(angle - 180);
				dist = -dist;
			}
			logger.noise("moving to the closest corner with rotation by " + angle );
		}
		if ( myBot._trgt.haveTarget && (myBot.numEnemyBotsAlive <= 1) && (Math.random() < 0.95) ) {
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
				logger.noise("setting a new motion" );
				dist=200*(0.5-Math.random());
				// but we need to move at least a half bot body
				if (Math.abs(dist) < 50) {
					dist += 50*math.sign(dist);
				}
			} else {
				logger.noise("continue previous motion" );
				dist = myBot.getDistanceRemaining();
			}
			logger.noise("circle around last enemy by rotating = " + angle );
		} 

		if ( myBot.numEnemyBotsAlive > 1 && (Math.random() < 0.95) ) {
			dist = myBot.getDistanceRemaining();
			angle = myBot.getTurnRemaining();
			if ( Math.abs(dist) > 20 ) {
				logger.noise("continue previous motion" );
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
			logger.noise("Random evasive motion");
		}

		moveOrTurn(dist, angle);
	}

	// --- Utils ----------
	public void moveOrTurn(double dist, double suggestedAngle) {
		double angle=0;
		double moveLength;
		double shortestTurnRadius=shortestTurnRadiusVsSpeed();
		double hardStopDistance = 20;
		calculateSticksEndsPosition();
		String wallAhead = whichWallAhead();
		String furtherestStick = whichStickIsFurtherFromWalls();
		double distFromStickEndToWall = distToTheClosestWallFromStick(furtherestStick);
		logger.noise("furtherestStick = " + furtherestStick );

		double evadeWallDist = shortestTurnRadius+45;
		double wallAheadDist;
		wallAheadDist = distanceToWallAhead();
		logger.noise("moveOrTurn suggested dist =  " + dist + ", angle =" + suggestedAngle);
		logger.noise("hardStopDistance =  " + hardStopDistance);
		logger.noise("Wall ahead is " + wallAhead );
		logger.noise("wallAheadDist =  " + wallAheadDist);
		logger.noise("getDistanceRemaining =  " + myBot.getDistanceRemaining());
		logger.noise("rotate away from a wall by " + whichWayToRotateAwayFromWall() );
		logger.noise("Robot velocity =  " + myBot.getVelocity());
		if (wallAheadDist < hardStopDistance ) {
			// wall is close trying to stop
			logger.noise("Wall ahead is " + wallAhead );
			angle = whichWayToRotateAwayFromWall();
			executingWallEvadingTurn=true;
			if ( Utils.isNear(myBot.getVelocity(),0) ) {
					logger.noise("Wall is too close, backward is faster");
					dist = -41;
					myBot.setTurnRight(0);
			} else {
				dist = stopDistance(myBot.getVelocity());
				logger.noise("Robot velocity =  " + myBot.getVelocity());
				logger.noise("Trying to stop by setting distance = " + dist);
			}
			myBot.setAhead(dist); // this is emergency stop or hit a wall
			return;

			//dist = -dist; // hard stop and reverse
			//angle = 0; // do not rotate

		} 
		if (  distFromStickEndToWall <= evadeWallDist && wallAheadDist <= evadeWallDist ){
				// make hard turn
				logger.noise("Trying to turn away from walls" );
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
			logger.noise("getDistanceRemaining = " + myBot.getDistanceRemaining());
			//if (  Math.abs(myBot.getDistanceRemaining()) <=  0 ) {
				logger.noise("Proceeding with suggested motion");
				angle = suggestedAngle;
				dist = dist;
			//} else {
				//logger.noise("Continue previous turn motion");
				//dist =myBot.getDistanceRemaining();
				//angle = 0;
			//}
		//}
		logger.noise("Moving by " + dist);
		logger.noise("Turning by " + angle);
		myBot.setTurnRight(angle);
		myBot.setBodyRotationDirection( math.sign(angle) );
		myBot.setAhead(dist);
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

	public boolean areBothSticksEndAtField() {
		if ( distToTheClosestWallFromStick("starboard") > 0 && distToTheClosestWallFromStick("port") > 0 ) {
			return true;
		} 
		return false;
	}

	public void onPaint(Graphics2D g) {
		// draw starboard and port side sticks
		if (false) {
			// show starboard and port sticks with little circles at the ends
			calculateSticksEndsPosition();
			g.setColor(Color.green);
			g.drawLine((int) starboardStickX, (int) starboardStickY, (int)myBot.myCoord.x, (int)myBot.myCoord.y);
			g.drawOval((int) starboardStickX -5, (int) starboardStickY-5, 10, 10);
			g.setColor(Color.red);
			g.drawLine((int) portStickX, (int) portStickY, (int)myBot.myCoord.x, (int)myBot.myCoord.y);
			g.drawOval((int) portStickX-5, (int) portStickY-5, 10, 10);

			//draw possible shortest turn radius paths
			g.setColor(Color.green);
			g.drawOval((int) (starboardStickX - shortestTurnRadiusVsSpeed()), (int) (starboardStickY - shortestTurnRadiusVsSpeed()), (int) (2*shortestTurnRadiusVsSpeed()), (int) (2*shortestTurnRadiusVsSpeed()));
			g.setColor(Color.red);
			g.drawOval((int) (portStickX - shortestTurnRadiusVsSpeed()), (int) (portStickY - shortestTurnRadiusVsSpeed()), (int) (2*shortestTurnRadiusVsSpeed()), (int) (2*shortestTurnRadiusVsSpeed()));
		}
	}

}
