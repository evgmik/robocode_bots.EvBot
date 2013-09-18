// -*- java -*-
// (C) 2013 by Eugeniy Mikhailov, <evgmik@gmail.com>

package eem.radar;

import eem.EvBot;
import eem.misc.*;
import eem.target.*;
import java.util.Random;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

public class radar {
	protected static EvBot myBot;

	protected double angle2rotate = 0;
	protected int countFullSweepDelay=0;
	protected int radarSpinDirection =1;
	protected int radarMotionMultiplier = 1;
	protected static int fullSweepDelay = 200;
	protected static double radarMaxRotationAngle;
	protected static double radarSmallestRockingMotion;
	protected static int numberOfSmallRadarSweeps;
	protected int countForNumberOfSmallRadarSweeps;
	protected boolean searchForClosestTarget = true;
	protected boolean movingRadarToLastKnownTargetLocation = false;
	protected double radarBearingToEnemy=0;

	public radar(EvBot bot) {
		myBot = bot;
		radarMaxRotationAngle = myBot.game_rules.RADAR_TURN_RATE ;
		radarSmallestRockingMotion = myBot.game_rules.RADAR_TURN_RATE/2;
		numberOfSmallRadarSweeps =(int) Math.ceil(360 / radarMaxRotationAngle);
		countForNumberOfSmallRadarSweeps=numberOfSmallRadarSweeps;

	}

	public void initTic() {
		myBot.setAdjustRadarForGunTurn(true); // decouple gun and radar
	}

	public void manage() {
			this.performRockingSweepIfNeded();
			this.moveToOrOverOldTargetPositionIfNeeded();
			this.decreaseFullSweepDelay();
			this.performFullSweepIfNeded();
	}

	public void setMovingRadarToLastKnownTargetLocation(boolean val) {
		movingRadarToLastKnownTargetLocation = val;
	}

	public void setFullSweepAllowed() {
		countFullSweepDelay = -1; // we can sweep do full radar sweep
	}

	public void decreaseFullSweepDelay() {
		countFullSweepDelay--; // we can sweep do full radar sweep
	}


	public void moveToOrOverOldTargetPositionIfNeeded() {
		double angle;
		// moving radar to or over old target position
		if ( !searchForClosestTarget && myBot._trgt.targetUnlocked && movingRadarToLastKnownTargetLocation) {
			myBot.dbg(myBot.dbg_noise, "Spinning radar to the old target location");
			spinInTheSameDirection(radarSpinDirection);
		}
	}

	public void spinInTheSameDirection(int direction) {
		double angle;
		radarSpinDirection = direction;
		angle = radarSpinDirection*radarMaxRotationAngle;
		setTurnRadarRight(angle);
	}

	public void performFullSweepIfNeded() {
		double angle=0;

		myBot.dbg(myBot.dbg_noise, "countFullSweepDelay = " + countFullSweepDelay);
		myBot.dbg(myBot.dbg_noise, "searchForClosestTarget = " + searchForClosestTarget);
		countForNumberOfSmallRadarSweeps--;
		// full sweep for the closest enemy
		if ( (countFullSweepDelay<0) && !searchForClosestTarget && (myBot.getOthers() > 1) || !myBot._trgt.haveTarget) {
			myBot.dbg(myBot.dbg_noise, "Begin new cycle for closest enemy search");
			myBot.dbg(myBot.dbg_noise, "We have target = " + myBot._trgt.haveTarget);
			searchForClosestTarget = true;
			countForNumberOfSmallRadarSweeps = numberOfSmallRadarSweeps;
		}

		if ( searchForClosestTarget ) {
			angle = radarMaxRotationAngle;
			myBot.dbg(myBot.dbg_noise, "Search sweep");
			setTurnRadarRight(angle);
			myBot._trgt.setUnLockedStatus(true);
		}

		myBot.dbg(myBot.dbg_noise, "countForNumberOfSmallRadarSweeps = " + countForNumberOfSmallRadarSweeps);
		if ( countForNumberOfSmallRadarSweeps <= 0 && searchForClosestTarget ) {
			searchForClosestTarget = false;
			countFullSweepDelay = fullSweepDelay;
			myBot.dbg(myBot.dbg_noise, "Full sweep for closest enemy is completed");
			movingRadarToLastKnownTargetLocation = true;

			double radar_angle = myBot.getRadarHeading();
			angle=(myBot.angle2target()-radar_angle);
			angle = math.shortest_arc(angle);
			if (math.sign(angle) >= 0 ) {
				radarSpinDirection=1;
				angle = myBot.game_rules.RADAR_TURN_RATE;
			} else {
				radarSpinDirection=-1;
				angle = -myBot.game_rules.RADAR_TURN_RATE;
			}
			myBot.dbg(myBot.dbg_noise, "Full sweep radar motion");
			setTurnRadarRight(angle);
		}
	}

	public void performRockingSweepIfNeded() {
		double angle;
		// radar rocking motion to relock target
		if (myBot._trgt.haveTarget && !searchForClosestTarget && !movingRadarToLastKnownTargetLocation) {
			myBot.dbg(myBot.dbg_noise, "Doing radar rocking motion");
			if (!myBot._trgt.targetUnlocked) {
				myBot.dbg(myBot.dbg_noise, "Target scanned last time");
				radarBearingToEnemy = math.shortest_arc( myBot.angle2target()-myBot.getRadarHeading() );
				myBot.dbg(myBot.dbg_noise, "Bearing to the target = " + radarBearingToEnemy);
				radarSpinDirection = math.signNoZero(radarBearingToEnemy);
				angle = radarBearingToEnemy + radarSpinDirection*radarMaxRotationAngle/2;
				// we will need to know opposite to made direction if on scan we missed target
				// then unlocked condition will kick in with opposite rotation
				radarSpinDirection*=-1; 
			} else {
				myBot.dbg(myBot.dbg_noise, "Target unlocked, long sweep to catch it");
				angle = radarSpinDirection*radarMaxRotationAngle;
			}
			myBot.dbg(myBot.dbg_noise, "Trying to relock on target with radar motion");
			setTurnRadarRight(angle);
		}
	}

	protected void setTurnRadarRight(double angle) {
		angle2rotate = math.putWithinRange(angle, -radarMaxRotationAngle, radarMaxRotationAngle);
		myBot.dbg(myBot.dbg_noise, "Radar rotation angle = " + angle2rotate);
		myBot.setTurnRadarRight(angle2rotate);
	}
}

