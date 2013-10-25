// -*- java -*-

package eem.gun;

import eem.EvBot;
import eem.target.*;
import eem.bullets.*;
import eem.misc.*;
import java.util.Random;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import robocode.Bullet;
import java.util.HashMap;


public class baseGun {
	protected EvBot myBot;
	protected String gunName = "base";
	protected boolean  gunFired = false;
	protected boolean  gunHasTargetPoint = false;
	protected Random gun_rand = new Random();
	public    Color gunColor = Color.black;
	protected Point2D.Double targetFuturePosition;
	protected double firePower;
	private static int bulletHitCount = 0;
	private static int bulletMissedCount = 0;
	private static int bulletFiredCount = 0;
	public static HashMap<String, gunStats> mapOfGunStats = new HashMap<String, gunStats>();
	private String strSep = "__zzz__";


	private String buildMapKey(InfoBot targetBot, InfoBot firingBot) {
		String key = gunName + strSep + firingBot.getName() + strSep + targetBot.getName();
		return key;
	}

	public int getBulletFiredCount(InfoBot targetBot, InfoBot firingBot) {
		String key = this.buildMapKey( targetBot, firingBot);
		gunStats gS = mapOfGunStats.get(key);
		if (gS == null) {
			return 0;
		}
		return gS.getBulletFiredCount();
	}

	public int getBulletFiredCount(InfoBot targetBot) {
		return getBulletFiredCount(targetBot, myBot._tracker);
	}

	public int getBulletFiredCount() {
		return getBulletFiredCount(myBot._trgt, myBot._tracker);
	}

	public int getBulletHitCount(InfoBot targetBot, InfoBot firingBot) {
		String key = this.buildMapKey( targetBot, firingBot);
		gunStats gS = mapOfGunStats.get(key);
		if (gS == null) {
			return 0;
		}
		return gS.getBulletHitCount();
	}

	public int getBulletHitCount(InfoBot targetBot) {
		return getBulletHitCount(targetBot, myBot._tracker);
	}

	public int getBulletHitCount() {
		return getBulletHitCount(myBot._trgt, myBot._tracker);
	}

	public int getBulletMissedCount(InfoBot targetBot, InfoBot firingBot) {
		String key = this.buildMapKey( targetBot, firingBot);
		gunStats gS = mapOfGunStats.get(key);
		if (gS == null) {
			return 0;
		}
		return gS.getBulletFiredCount() - gS.getBulletHitCount();
	}

	public int getBulletMissedCount(InfoBot targetBot) {
		return getBulletMissedCount(targetBot, myBot._tracker);
	}

	public int getBulletMissedCount() {
		return getBulletMissedCount(myBot._trgt, myBot._tracker);
	}

	public void incBulletFiredCount(InfoBot targetBot, InfoBot firingBot) {
		String key = this.buildMapKey( targetBot, firingBot);
		gunStats gS = mapOfGunStats.get(key);
		if (gS == null) {
			gS = new gunStats();
			mapOfGunStats.put(key,gS);
		}
		gS.incBulletFiredCount();
	}

	public void incBulletFiredCount(InfoBot targetBot) {
		incBulletFiredCount(targetBot, myBot._tracker);
	}

	public void incBulletFiredCount() {
		incBulletFiredCount(myBot._trgt, myBot._tracker);
	}

	public void incBulletHitCount(InfoBot targetBot, InfoBot firingBot) {
		String key = this.buildMapKey( targetBot, firingBot);
		gunStats gS = mapOfGunStats.get(key);
		if (gS == null) {
			gS = new gunStats();
			mapOfGunStats.put(key,gS);
		}
		gS.incBulletHitCount();
	}

	public void incBulletHitCount(InfoBot targetBot) {
		incBulletHitCount(targetBot, myBot._trgt);
	}

	public void incBulletHitCount() {
		incBulletHitCount(myBot._trgt, myBot._tracker);
	}

	public double getGunHitRate(InfoBot targetBot, InfoBot firingBot) {
		String key = this.buildMapKey( targetBot, firingBot);
		gunStats gS = mapOfGunStats.get(key);
		if (gS == null) {
			gS = new gunStats();
			mapOfGunStats.put(key,gS);
		}
		return gS.getGunHitRate();
	}

	public double getGunHitRate(InfoBot targetBot) {
		return getGunHitRate(targetBot, myBot._tracker);
	}

	public double getGunHitRate() {
		return getGunHitRate(myBot._trgt, myBot._tracker);
	}

	public double getGunPerformance(InfoBot targetBot, InfoBot firingBot) {
		String key = this.buildMapKey( targetBot, firingBot);
		gunStats gS = mapOfGunStats.get(key);
		if (gS == null) {
			gS = new gunStats();
			mapOfGunStats.put(key,gS);
		}
		return gS.getGunPerformance();
	}

	public double getGunPerformance(InfoBot targetBot) {
		return getGunPerformance(targetBot, myBot._tracker);
	}

	public double getGunPerformance() {
		return getGunPerformance(myBot._trgt, myBot._tracker);
	}

	public baseGun() {
	}

	public baseGun(EvBot bot) {
		myBot = bot;
		calcGunSettings();
	};

	public void initTic() {
	}

	public boolean doesItNeedTrackedTarget() {
		if ( (myBot.getGunHeat()/myBot.getGunCoolingRate() < 3) ) {
			return true;
		} else {
			return false;
		}
	}

	public void manage() {
		double angle;
		double angle2enemyInFutire;

		if (myBot._trgt.haveTarget) {
			//calculate the gun settings
			this.calcGunSettings();

			logger.noise("Predicted target X coordinate = " + this.getTargetFuturePosition().x );
			logger.noise("Predicted target Y coordinate = " + this.getTargetFuturePosition().y );

			angle2enemyInFutire=math.angle2pt(myBot.myCoord, this.getTargetFuturePosition());

			// rotate gun dirictives and settings
			double gun_angle =myBot.getGunHeading();
			angle = math.shortest_arc(angle2enemyInFutire-gun_angle);
			logger.noise("Pointing gun to enemy by rotating by angle = " + angle);
			myBot.setTurnGunRight(angle);

			double predictedBulletDeviation=angle*Math.PI/180*myBot._trgt.getLastDistance(myBot.myCoord);

			logger.noise("Gun heat = " + myBot.getGunHeat() );
			// if gun is called and
			// predicted bullet deviation within half a body size of the robot
			if (myBot.getGunHeat() == 0 && 
					Math.abs(predictedBulletDeviation) < Math.min( myBot.getHeight(), myBot.getWidth())/2 ) {
				logger.noise("Firing the gun with power = " + firePower);
				this.fireGun();
			}
		}
	}

	public void fireGun() {
		if ( firePower != 0 ) {
			// other algorithms decided not to fire
			// for example if we low on energy
			Bullet b;
			logger.noise("Gun fire power = " + firePower);
			b=myBot.setFireBullet(firePower);
			if ( b == null ) {
				logger.error("Gun did not fire  = " + b);
				return;
			}
			logger.noise("fired bullet  = " + b);
			myBot._bmanager.add( new firedBullet( myBot, b, this) );
			gunFired = true;
			gunHasTargetPoint = false;
			this.incBulletFiredCount();
		}
	}

	public void setTargetFuturePosition( Point2D.Double target ) {
		targetFuturePosition = math.putWithinBorders( target, myBot.BattleField);
	};

	public String getName() {
		return gunName;
	}

	public boolean isGunFired() {
		return gunFired;
	}

	public Point2D.Double getTargetFuturePosition() {
		return targetFuturePosition;
	}

	public void setTargetFuturePosition(target tgt) {
		targetFuturePosition = calcTargetFuturePosition( myBot.myCoord, firePower, tgt);
	}

        public double firePoverVsDistance( double targetDistance ) {
                // calculate firepower based on distance
		logger.noise("Target distance = " + targetDistance);
                double firePower;
		firePower = -100; // negative means unset
                //firePower = Math.min( 12*Math.exp( -math.sqr( targetDistance/200.0 ) ), 3);
		if ( myBot.fightType().equals("melee") ) {
			firePower = 3;
		}
		if ( myBot.fightType().equals("meleeMidle") ) {
			firePower = Math.min( 700/targetDistance, 3);
		}
		if ( myBot.fightType().equals("1on1") ) {
			firePower = Math.min( 700/targetDistance, 3);
		}
		if ( firePower < 0 ) {
			// default case
			firePower = Math.min( 500/targetDistance, 3);
		}
                firePower = Math.max( firePower, 0.1);
		logger.noise("Fire power = " + firePower);
                return firePower;
        }

	public Point2D.Double futureTargetWithinPhysicalLimitsBasedOnVelocity( Point2D.Double ftPos, Point2D.Double tVel ) {
		// robot center cannot approach walls closer than robotHalfSize
		// so we take it in account for future target calculation
		double robotHalfSize = myBot.robotHalfSize;
		Point2D.Double reducedBattleFieldSize = (Point2D.Double) myBot.BattleField.clone();
		Point2D.Double new_ftPos = (Point2D.Double) ftPos.clone();

		// calculate available space for robot center
		reducedBattleFieldSize.x = reducedBattleFieldSize.x - 2*robotHalfSize;
		reducedBattleFieldSize.y = reducedBattleFieldSize.y - 2*robotHalfSize;

		// shift coordinates by robotHalfSize
		new_ftPos.x = new_ftPos.x - robotHalfSize;
		new_ftPos.y = new_ftPos.y - robotHalfSize;
		if ( !math.isItOutOfBorders( new_ftPos, reducedBattleFieldSize ) ) {
			return ftPos;
		}

		if ( tVel.x == 0 ) {
			tVel.x = 1e-6;
		}
		double dist;
		double roundOffError = 2; // small placement error usually due to round off
		// outside left border?
		dist = new_ftPos.x;
		if ( dist < -roundOffError ) {
			new_ftPos.x = new_ftPos.x - dist;
			new_ftPos.y = new_ftPos.y - dist/tVel.x*tVel.y;
		}
		// outside right border?
		dist = new_ftPos.x - reducedBattleFieldSize.x;
		if ( dist > roundOffError ) {
			new_ftPos.x = new_ftPos.x - dist;
			new_ftPos.y = new_ftPos.y - dist/tVel.x*tVel.y;
		}
		if ( tVel.y == 0 ) {
			tVel.y = 1e-6;
		}
		// outside bottom border?
		dist = new_ftPos.y;
		if ( dist < -roundOffError ) {
			new_ftPos.y = new_ftPos.y - dist;
			new_ftPos.x = new_ftPos.x - dist/tVel.y*tVel.x;
		}
		// outside top border?
		dist = new_ftPos.y - reducedBattleFieldSize.y;
		if ( dist > roundOffError) {
			new_ftPos.y = new_ftPos.y - dist;
			new_ftPos.x = new_ftPos.x - dist/tVel.y*tVel.x;
		}

		// shift back coordinates by robotHalfSize
		new_ftPos.x = new_ftPos.x + robotHalfSize;
		new_ftPos.y = new_ftPos.y + robotHalfSize;
		return new_ftPos;
	}

	public Point2D.Double calcTargetFuturePosition( Point2D.Double firingPosition, double firePower, InfoBot tgt) {
		return  tgt.getPosition();
	}

	public void calcGunSettings() {
		if ( myBot._trgt.haveTarget ) {
			setFirePower();
			setTargetFuturePosition(myBot._trgt);
		}
	}

	public void setFirePower() {
		firePower = calcFirePower();
	}

	public double calcFirePower() {
		double firePower =0;
		if ( myBot._trgt.haveTarget ) {
			firePower = firePoverVsDistance(myBot._trgt.getLastDistance(myBot.myCoord));
			// no point to fire bullets more energetic than enemy bot energy level
			// fixme replace magic minimal energy bullet = 0.1  with a named var
			firePower = math.putWithinRange( firePower, 0.1, misc.minReqBulEnergyToKillTarget(myBot._trgt.getEnergy()) );

			// do not fire more than bot has or it  get itself disabled
			firePower = Math.min( firePower, myBot.getEnergy() - 1e-4 );
			firePower = Math.max( firePower, 0.1); //take in account rounding
			// final check after rounding
			if (myBot.getEnergy() <= firePower + 1e-4 ) {
				firePower = 0;
			}
		} else {
			firePower = 0;
		}
		return firePower;
	}

	public double  bulletSpeed( double firePower ) {
		double bSpeed;
		bSpeed = ( 20 - firePower * 3 );
		logger.noise("bullet speed = " + bSpeed + " for firePower = " + firePower);
		return bSpeed;
	}

	public double getFirePower() {
		return firePower;
	}

	private void drawTargetFuturePosition(Graphics2D g) {
		if ( null != targetFuturePosition ) {
			g.setColor(gunColor);
			//g.fillRect((int)targetFuturePosition.x - 20, (int)targetFuturePosition.y - 20, 40, 40);
			graphics.fillSquare( g,  targetFuturePosition, 40);
		}
	}

	private void drawTargetLastPosition(Graphics2D g) {
		myBot._trgt.onPaint(g);
	}

	private void drawLineToTargetFuturePosition(Graphics2D g) {
		if ( null != targetFuturePosition ) {
			g.setColor(gunColor);
			graphics.drawLine( g, getTargetFuturePosition(), myBot.myCoord );
		}
	}

	private void drawLineToTarget(Graphics2D g) {
		g.setColor(gunColor);
		g.drawLine((int)myBot._trgt.getX(), (int)myBot._trgt.getY(), (int)myBot.myCoord.x, (int)myBot.myCoord.y);
	}

	public void onPaint(Graphics2D g) {
		if ( myBot._trgt.haveTarget ) {
			drawTargetFuturePosition(g);
			drawTargetLastPosition(g);
			drawLineToTarget(g);
			drawLineToTargetFuturePosition(g);
		}
	}
}
