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

	public int getBulletFiredCount() {
		return this.bulletFiredCount;
	}

	public int getBulletHitCount() {
		return this.bulletHitCount;
	}

	public int getBulletMissedCount() {
		return this.bulletFiredCount - this.bulletHitCount;
	}

	protected void incBulletFiredCount() {
		this.bulletFiredCount++;
	}

	public void incBulletHitCount() {
		this.bulletHitCount++;
	}

	public baseGun() {
	}

	public baseGun(EvBot bot) {
		myBot = bot;
		calcGunSettings();
	};

	public void initTic() {
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
				myBot._radar.setFullSweepAllowed(); // we can sweep do full radar sweep
			}
		}
	}

	public void fireGun() {
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
		targetFuturePosition = tgt.getPosition();
	}

        public double firePoverVsDistance( double targetDistance ) {
                // calculate firepower based on distance
		logger.noise("Target distance = " + targetDistance);
                double firePower;
                firePower = Math.min(500 / targetDistance, 3);
		logger.noise("Fire power = " + firePower);
                return firePower;
        }

	public void calcGunSettings() {
		if ( myBot._trgt.haveTarget ) {
			setFirePower();
			setTargetFuturePosition(myBot._trgt);
		}
	}

	public void setFirePower() {
		if ( myBot._trgt.haveTarget ) {
		firePower = firePoverVsDistance(myBot._trgt.getLastDistance(myBot.myCoord));
		// no point to fire bullets more energetic than enemy bot energy level
		// fixme replace magic minimal energy bullet = 0.1  with a named var
		firePower = math.putWithinRange( firePower, 0.1, misc.minReqBulEnergyToKillTarget(myBot._trgt.getEnergy()) );
		} else {
			firePower = 0;
		}
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
			g.fillRect((int)targetFuturePosition.x - 20, (int)targetFuturePosition.y - 20, 40, 40);
		}
	}

	private void drawTargetLastPosition(Graphics2D g) {
		myBot._trgt.onPaint(g);
	}

	private void drawLineToTargetFuturePosition(Graphics2D g) {
		if ( null != targetFuturePosition ) {
			g.setColor(gunColor);
			g.drawLine((int)getTargetFuturePosition().x, (int)getTargetFuturePosition().y, (int)myBot.myCoord.x, (int)myBot.myCoord.y);
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
