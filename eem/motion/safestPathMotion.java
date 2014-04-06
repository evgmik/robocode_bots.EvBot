// -*- java -*-

package eem.motion;

import eem.EvBot;
import eem.target.*;
import eem.misc.*;
import eem.bullets.*;
import eem.motion.dangerPoint;
import eem.motion.dangerPath;
import java.util.Random;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.Arrays;
import robocode.util.*;
import robocode.*;
import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.*;
import java.util.Collections;


public class safestPathMotion extends dangerMapMotion {
	private dangerPath  safestPath = new dangerPath();
	private dangerPathPoint DestinationDangerPathPoint = null;
	public LinkedList<dangerPath> dangerPaths;
	private int NofGenNewPathAttempts = 1500;
	private int maxPathLength = 55;
	private int pathSafetyMargin = 54; // when we have less point recalculate path
	
	public void initTic() {
		double deviation =  myBot.myCoord.distance(DestinationDangerPathPoint.getPosition());
		if ( deviation > 1e-3 ) {
			logger.dbg("Go fix the code: to large deviation from expected position");
			logger.dbg("Deviation from predicted " + deviation );
			logger.dbg("motion initTic " + myBot.ticTime );
			logger.dbg("Actual position " + myBot.myCoord);
			logger.dbg("Predicted position " + DestinationDangerPathPoint.getPosition());
			logger.dbg("Velocity expected " + DestinationDangerPathPoint.getVelocity() + " actual " + myBot.getVelocity() );
			logger.dbg("Heading expected " + DestinationDangerPathPoint.getHeading() + " actual " + myBot.getHeading() );
		}
		if ( safestPath.size() < pathSafetyMargin ) {
			safestPath = generateTheBestPath();
		}
		DestinationDangerPathPoint = safestPath.removeFirst();
	}

	public safestPathMotion(EvBot bot) {
		super(bot);
		double danger = 0;
		Point2D.Double pos = (Point2D.Double) myBot.myCoord.clone();
		double angle = myBot.getHeading();
		double speed = myBot.getVelocity();
		DestinationDangerPathPoint = new dangerPathPoint( pos, danger, 0, 0, speed, angle );
	       	safestPath = generateTheBestPath();
	}

	public double possibleNewSpeed(double speed) {
		double newSpeed = speed;

		while ( true ) {
			
			double accelProb, deaccelProb;
			if ( Math.abs(speed) >= 8 ) {
				// speed up is impossible
				accelProb = 0;
				deaccelProb = 0.2;
			} else {
				// there is a way to go faster
				accelProb = .5;
				deaccelProb = 0.2;
			}

			double r = Math.random();
			// probabilistically change speed
			if ( r <= accelProb ) {
				// accel
				newSpeed = math.signNoZero(speed)* (Math.abs(speed)+1);
				break;
			}
			if ( r <= (accelProb + deaccelProb) ) {
				// deaccel
				newSpeed = math.signNoZero(speed)* (Math.abs(speed)-2);
				break;
			}
			// no change
			newSpeed = speed; 
			break;
		}
		//do we satisfy robocode physics?
		if ( Math.abs(newSpeed) > 8 )
			newSpeed = math.signNoZero(speed) * 8;

		//logger.dbg("New speed = " + newSpeed);
		return newSpeed;
	}


	public dangerPath generateTheBestPath() {
		double bestDanger = 1e9; // humongously large
		dangerPath bestPath=null;
		dangerPath trialPath;
		for (int i=0; i< NofGenNewPathAttempts; i++) {
			trialPath=randomPath(bestDanger);
			if ( trialPath.getDanger() < bestDanger ) {
				bestPath = trialPath;
				bestDanger = bestPath.getDanger();
				
			}
		}
		return bestPath;
	}

	double randomNewTurnAngle( double speed ) {
		double turnAngle = maxRotationPerTurnInDegrees( speed );
		double r = Math.random();
		if ( r <= .33 ) 
			// right
			return  turnAngle;
		if ( r <= .66 ) 
			// left
			return -turnAngle;
		// no rotation
		return 0;
	}

	double randomNewHeading(double speed, double angle) {
		angle = math.shortest_arc(angle);
		double da = randomNewTurnAngle( speed );
		return math.shortest_arc( angle + da );
	}

	public dangerPath randomPath() {
		return randomPath(1e9); // any pass is good one
	}

	public double  getNewVelocity(double velocity, double direction) {
		// direction stands for accelerate->1 or deaccelerate->-1 or 0

		if ( velocity < 0 ) 
			return -getNewVelocity( -velocity, -direction);

		//life is easier now velocity is always positive and we can treat it as speed

		double maxAccel = Rules.ACCELERATION;
		double maxDeAccel = Rules.DECELERATION;

		if ( ( velocity == 0) && (direction == 0) )
			return 0;

		if ( ( velocity != 0) && (direction == 0) ) {
			// robophysics require to accelerate
			// this is wrong call direction 
			// velocity must be zero to ask for zero direction
			// forcing acceleration
			direction = 1; //i.e. up
		}

		// easy case speeding up
		if ( direction > 0 ) {
			return Math.min( Rules.MAX_VELOCITY, velocity + Rules.ACCELERATION );
		}
		
		// slow down is tricky
		if ( velocity > Rules.DECELERATION ) {
			// still easy
			return velocity - Rules.DECELERATION;
		}
		// hard case passing over zero
		double overshoot = velocity - Rules.DECELERATION;
		return overshoot/2;
	}

	public double randomAccelDir(double velocity) {
		double probSame = 0, probUp = 0;
		if (velocity == 0) {
			probSame = 0.333;
			probUp = 0.333;
		} else {
			probSame = 0;
			probUp = 0.5;
		}
		double r = Math.random();
		if ( r < probSame ) 
			return 0;
		if ( r <= (probSame + probUp) )
			return 1;
		return -1;
	}

	public dangerPath randomPath(double thresholdDanger ) {
		dangerPath  nPath = new dangerPath();
		double danger = 0;
		Point2D.Double pos = (Point2D.Double) myBot.myCoord.clone();
		Point2D.Double posNew;
		dangerPathPoint dp;
		double speedNew=1, angleNew;

		double angle = myBot.getHeading();
		double speed = myBot.getVelocity();
		dp= new dangerPathPoint( pos, danger, 0, 0, speed, angle );

		int cnt = 0;
		while( cnt < maxPathLength) {
			double accelDir = randomAccelDir(speed);
			double turnAngle = randomNewTurnAngle(speed);
			speedNew = getNewVelocity(speed, accelDir);
			angleNew = angle + turnAngle;

			posNew = (Point2D.Double) pos.clone();
			posNew.x += speedNew*Math.sin(angleNew*Math.PI/180);
			posNew.y += speedNew*Math.cos(angleNew*Math.PI/180);
			danger = 0;
			danger = pointDangerFromWalls(posNew, speedNew);
			danger += pointDangerFromAllBots( posNew );
			danger += pointDangerFromEnemyWavesAndItsPrecursor( posNew );
			dp= new dangerPathPoint( posNew, danger, turnAngle, accelDir, speedNew, angleNew );

			nPath.add( dp );
			angle = angleNew;
			speed = speedNew;
			pos = posNew;
			cnt++;
			if ( nPath.getDanger() > thresholdDanger) {
				// this is already bad path
				// no need to look dipper
				return nPath;
			}

		}
		return nPath;
	}

	private void buildListOfPathToTest() {
	}

	private void sortDangerPaths() {
		Collections.sort(dangerPaths);
	}


	public void makeMove() {
		// dist here should be large to ensure bot acceleration
		double dist = 200* DestinationDangerPathPoint.getAccelDir();
		double turnAngle =    DestinationDangerPathPoint.getTurnAngle();
		myBot.setTurnRight(turnAngle);
		myBot.setAhead (dist);
	}

	public void onPaint(Graphics2D g) {
		drawDangerMap(g);
		safestPath.onPaint(g);
	}


}

