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
	protected EvBot myBot;

	protected double angle2rotate = 0;
	protected int countFullSweepDelay=0;
	protected int radarSpinDirection =1;
	protected int radarMotionMultiplier = 1;
	protected static int fullSweepDelay = 200;
	protected static double radarSweepSubAngle;
	protected static double radarSmallestRockingMotion;
	protected static int numberOfSmallRadarSweeps;
	protected int countForNumberOfSmallRadarSweeps;
	protected boolean searchForClosestTarget = true;
	protected boolean movingRadarToLastKnownTargetLocation = false;
	protected double radarBearingToEnemy=0;

	public radar(EvBot bot) {
		myBot = bot;
		radarSweepSubAngle = myBot.game_rules.RADAR_TURN_RATE ;
		radarSmallestRockingMotion = myBot.game_rules.RADAR_TURN_RATE/2;
		numberOfSmallRadarSweeps =(int) Math.ceil(360 / radarSweepSubAngle);
		countForNumberOfSmallRadarSweeps=numberOfSmallRadarSweeps;

	}

	public void sweep() {
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
			myBot.dbg(myBot.dbg_debuging, "Moving radar to old target position");
			angle = radarSpinDirection*myBot.game_rules.RADAR_TURN_RATE;
			myBot.dbg(myBot.dbg_debuging, "Spinning radar to the old target location");
			setTurnRadarRight(angle);
		}
	}


	public void performFullSweepIfNeded() {
		double angle=0;

		myBot.dbg(myBot.dbg_noise, "countFullSweepDelay = " + countFullSweepDelay);
		myBot.dbg(myBot.dbg_noise, "searchForClosestTarget = " + searchForClosestTarget);
		countForNumberOfSmallRadarSweeps--;
		// full sweep for the closest enemy
		if ( (countFullSweepDelay<0) && !searchForClosestTarget && (myBot.getOthers() > 1) || !myBot._trgt.haveTarget) {
			myBot.dbg(myBot.dbg_debuging, "Begin new cycle for closest enemy search");
			myBot.dbg(myBot.dbg_debuging, "We have target = " + myBot._trgt.haveTarget);
			searchForClosestTarget = true;
			countForNumberOfSmallRadarSweeps = numberOfSmallRadarSweeps;
		}

		if ( searchForClosestTarget ) {
			angle = radarSweepSubAngle;
			myBot.dbg(myBot.dbg_debuging, "Search sweep");
			setTurnRadarRight(angle);
			myBot._trgt.setUnLockedStatus(true);
		}

		myBot.dbg(myBot.dbg_noise, "countForNumberOfSmallRadarSweeps = " + countForNumberOfSmallRadarSweeps);
		if ( countForNumberOfSmallRadarSweeps <= 0 && searchForClosestTarget ) {
			searchForClosestTarget = false;
			countFullSweepDelay = fullSweepDelay;
			myBot.dbg(myBot.dbg_debuging, "Full sweep for closest enemy is completed");
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
			myBot.dbg(myBot.dbg_debuging, "Full sweep radar motion");
			setTurnRadarRight(angle);
		}
	}

	public void performRockingSweepIfNeded() {
		double angle;
		// radar rocking motion to relock target
		if (myBot._trgt.haveTarget && !searchForClosestTarget && !movingRadarToLastKnownTargetLocation) {
			myBot.dbg(myBot.dbg_debuging, "Doing radar rocking motion");
			radarSpinDirection*=-1;
			if (myBot._trgt.targetUnlocked) {
				myBot.dbg(myBot.dbg_debuging, "Target lost!");
				radarMotionMultiplier *= 2;
				myBot.dbg(myBot.dbg_debuging, "Radar motion multiplier = " + radarMotionMultiplier);
				radarBearingToEnemy=0; //unknown
				angle=(radarBearingToEnemy + radarSpinDirection*radarMotionMultiplier*radarSmallestRockingMotion);
				if ( Math.abs(angle) > radarSweepSubAngle ) {
					myBot.dbg(myBot.dbg_debuging, "Radar sweep angle is too big decreasing it");
					angle = math.sign(angle)*radarSweepSubAngle;
					radarMotionMultiplier = ((int)  Math.ceil(radarSweepSubAngle/radarSmallestRockingMotion) );
					myBot.dbg(myBot.dbg_debuging, "Radar motion multiplier = " + radarMotionMultiplier);
				}
			} else {
				myBot.dbg(myBot.dbg_debuging, "Target scanned last time");
				radarMotionMultiplier = 1;
				radarBearingToEnemy = math.shortest_arc( myBot.angle2target()-myBot.getRadarHeading() );
				radarSpinDirection = math.sign(radarBearingToEnemy);
				angle=(radarBearingToEnemy + radarSpinDirection*radarMotionMultiplier*radarSmallestRockingMotion);
			}


			myBot.dbg(myBot.dbg_debuging, "Trying to relock on target with radar motion");
			setTurnRadarRight(angle);
			//myBot._trgt.targetUnlocked = true;
		}

	}

	protected void setTurnRadarRight(double angle) {
		angle2rotate = angle;
		myBot.dbg(myBot.dbg_debuging, "Radar rotation angle = " + angle2rotate);
		myBot.setTurnRadarRight(angle2rotate);
	}
}

