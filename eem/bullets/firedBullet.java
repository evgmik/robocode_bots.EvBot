// -*- java -*-

package eem.bullets;

import eem.EvBot;
import eem.target.*;
import eem.gun.*;
import eem.misc.*;
import robocode.Bullet;
import java.util.Random;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

public class firedBullet {
	protected EvBot myBot;

	protected Bullet robocodeBullet;
	protected baseGun firedGun;
	public boolean isItMine = false;
	public boolean isItVirtual = true;
	public Point2D.Double targetPosition;
	protected Point2D.Double firingPosition;
	protected long   firedTime;
	protected double firingAngle;
	protected double bulletSpeed;
	protected Color bulletColor;

	public firedBullet() {
	}

	public firedBullet(EvBot bot) {
		myBot = bot;
		firingPosition = myBot.myCoord;
	}

	public firedBullet(EvBot bot, Bullet b, baseGun gun) {
		myBot = bot;
		robocodeBullet = b;
		firedGun = gun;
		isItMine = true;
		targetPosition = firedGun.getTargetFuturePosition();
		logger.noise("fired bullet target position = " + targetPosition);
		firingPosition = new Point2D.Double( b.getX(), b.getY() );
		logger.noise("bullet firing position = " + firingPosition);
		firedTime = myBot.ticTime;
		firingAngle = b.getHeadingRadians();
		bulletSpeed = b.getVelocity();
		logger.noise("fired bullet speed = " + bulletSpeed);
		bulletColor = firedGun.gunColor;
	}

	public double  bulletEnergy() {
		double bEnergy = ( 20 - bulletSpeed ) / 3;
		return bEnergy;
	}

	public firedBullet(EvBot bot, wave wv, baseGun firedGun, Point2D.Double curPos) { 
		// to add bullet  passing through curPos to existing wave
		// intended use for shadowing bullets
		myBot = bot;
		isItMine = false;
		this.firedGun = firedGun;
		this.bulletSpeed = wv.bulletSpeed;
		// fixme enemy bullet detected 1 tic later so I need previous coord here
		this.firingPosition = (Point2D.Double) wv.getFiringPosition().clone();
		this.targetPosition = null;
		firedTime = wv.getFiredTime();
		this.firingAngle = Math.atan2(curPos.x-firingPosition.x, curPos.y - firingPosition.y);
		bulletColor = firedGun.gunColor;
	}

	public firedBullet(EvBot bot, InfoBot firedBot, baseGun gun, double bulletEnergy) {
		// bullets fired at me
		this( bot, firedBot, bot._tracker, gun, bulletEnergy );
	}

	public firedBullet(EvBot bot, InfoBot firedBot, InfoBot targetBot, baseGun gun, double bulletEnergy) {
		// general constructor
		myBot = bot;
		firedGun = gun;
		firedTime = myBot.ticTime;
		this.bulletSpeed = firedGun.bulletSpeed(bulletEnergy); 
		// FIXME my virtual bullet do not coinside with actual ones
		if ( firedBot.getName().equals( myBot.getName() ) ) {
			isItMine = true;
		} else {
			isItMine = false;
		}
		// fixme enemy bullet detected 1 tic later so I need previous coord here
		this.targetPosition = (Point2D.Double) gun.calcTargetFuturePosition( firedBot, bulletEnergy, targetBot);
		this.firingPosition = (Point2D.Double) firedBot.getPosition().clone();
		this.firingAngle = Math.atan2(targetPosition.x-firingPosition.x, targetPosition.y - firingPosition.y);
		bulletColor = firedGun.gunColor;
	}

	public void setIsItVirtual(boolean s) {
		isItVirtual = s;
	}

	public boolean getIsItVirtual(boolean s) {
		return isItVirtual;
	}

	public firedBullet(EvBot bot, baseGun gun) {
		myBot = bot;
		isItMine = false;
		firedGun = gun;
		this.bulletSpeed = firedGun.bulletSpeed(myBot._trgt.energyDrop()); 
		// fixme enemy bullet detected 1 tic later so I need previous coord here
		this.targetPosition = new Point2D.Double( myBot.myCoord.x, myBot.myCoord.y );
		this.firingPosition = new Point2D.Double( myBot._trgt.getX(), myBot._trgt.getY() );
		firedTime = myBot.ticTime;
		this.firingAngle = Math.atan2(targetPosition.x-firingPosition.x, targetPosition.y - firingPosition.y);
		bulletColor = firedGun.gunColor;
	}

	public baseGun getFiredGun() {
		return firedGun;
	}

	public double getDistanceTraveledAtTime(long time) {
		double timeInFlight = time - firedTime + 1;
		if ( !isItMine ) {
			timeInFlight = timeInFlight + 1;
		}
		double distTraveled = timeInFlight * bulletSpeed;
		return distTraveled;
	}

	public double getDistanceTraveled() {
		return getDistanceTraveledAtTime( myBot.ticTime );
	}

	public long getFiredTime () {
		return firedTime;
	}

	public Point2D.Double getPositionAtTime(long time) {
		Point2D.Double pos =  (Point2D.Double) firingPosition.clone();
		double distTraveled = getDistanceTraveledAtTime(time);
		pos.x = pos.x + distTraveled*Math.sin(firingAngle);
		pos.y = pos.y + distTraveled*Math.cos(firingAngle);
		return pos;
	}

	public Point2D.Double getPosition() {
		return getPositionAtTime( myBot.ticTime );
	}

	public boolean isPositionHitAtTime( Point2D.Double p, long time ) {
		Point2D.Double bPos = getPositionAtTime(time);
		double dx = p.x - bPos.x;
		double dy = p.y - bPos.y;
		if ( (Math.abs(dx) <= myBot.robotHalfSize) && (Math.abs(dy) <= myBot.robotHalfSize) ) {
			return true;
		}
		return false;
	}

	public double pointDangerFromExactBulletHit( Point2D.Double p, long time ) {
		double dangerLevelBullet = 100;
		double dangerLevelShadowBullet = -dangerLevelBullet/6.0;
		double safe_distance_from_bullet =  2*myBot.robotHalfSize + 2;

		double danger = 0;
		double dist;
		Point2D.Double bPos;
		if ( isActive() && !isItMine ) {
			bPos = getPositionAtTime(time);
			dist = p.distance(bPos);

			// adding danger of exact hit
			if ( isPositionHitAtTime( p, time ) ) {
				if ( !getFiredGun().getName().equals("shadow") ) {
					danger += dangerLevelBullet;
				} else {
					danger += dangerLevelShadowBullet;
				}
			}

			// adding proximity danger
			if ( !getFiredGun().getName().equals("shadow") ) {
				danger += math.gaussian( dist, dangerLevelBullet, safe_distance_from_bullet );

			} else {
				// shadow bullets are safe
				danger += math.gaussian( dist, dangerLevelShadowBullet, safe_distance_from_bullet );
			}
		}
		return danger;
	}

	public double pointDangerFromBulletPrecursor( Point2D.Double p, long time ) {
		double distOfBulletPrecursor = 200; // very large
		double dangerLevelBullet = 100;
		double dangerLevelShadowBullet = -dangerLevelBullet/6.0;
		double safe_distance_from_bullet =  2*myBot.robotHalfSize + 2;

		double danger = 0;
		double dist;
		Point2D.Double bPos, bEnd;
		if ( isActive() && !isItMine ) {
			bPos = getPositionAtTime(time);
			bEnd = endPositionAtBorder();
			double dBx, dBy;
			double dPx, dPy;
			double dP, dB;
			// bullet path vector
			dBx = bEnd.x - bPos.x;
			dBy = bEnd.y - bPos.y;
			dB = Math.sqrt(dBx*dBx + dBy*dBy);
			if( dBx == 0 ) dBx = 1e-8;
			if( dBy == 0 ) dBy = 1e-8;
			// vector to point from bullet present location
			dPx = p.x - bPos.x;
			dPy = p.y - bPos.y;
			dP = Math.sqrt(dPx*dPx + dPy*dPy);

			// if cos between dP and dB vectors positive
			// the point is in front of bullet
			double cos_val = (dPx*dBx + dPy*dBy)/(dP*dB); // normalized scalar product
			if ( cos_val > 0 ) {
				// distance to the bullet path from point
				dist = dP*Math.sqrt(1-cos_val*cos_val);
				double distAlongBulletPath = dP*cos_val;
				// bullet is dangerous only when we are close to it
				//if ( distAlongBulletPath < distOfBulletPrecursor ) {
				if ( !getFiredGun().getName().equals("shadow") ) {
					danger = math.gaussian( dist, dangerLevelBullet, safe_distance_from_bullet ); // tangent distance contribution
					 danger *= math.gaussian( distAlongBulletPath, 1, distOfBulletPrecursor ); // distance to travel by bullet contribution
					 if ( myBot.fightType().equals( "1on1" ) ) {
						 double bDamage = 4*bulletEnergy() + 2 * Math.max( bulletEnergy() - 1 , 0 );
						 danger *= (1+myBot.totalNumOfEnemiesAtStart/Math.max( myBot.numEnemyBotsAlive, 1) *0.01*bDamage);
					 }

				} else {
					// shadow bullets are safe
					danger = math.gaussian( dist, dangerLevelShadowBullet, safe_distance_from_bullet ); // tangent distance contribution
					 danger *= math.gaussian( distAlongBulletPath, 1, distOfBulletPrecursor ); // distance to travel by bullet contribution
				}
				//}
			}
		}
		return danger;
	}

	public boolean isActive() {
		return !math.isItOutOfBorders(getPosition(), myBot.BattleField) ;
	}

	public Point2D.Double endPositionAtBorder() {
		return math.vecCrossesBorder( firingPosition, firingAngle, myBot.BattleField);
	}

	public void onPaint(Graphics2D g) {
		g.setColor(bulletColor);
		// draw target position
		double R = 18;
		if ( targetPosition != null ) {
			logger.noise("draw target at position = " + targetPosition);
			//graphics.drawCircle(g, targetPosition, R);

			// draw line from firing point to target
			//Point2D.Double lEnd = endPositionAtBorder();
			//logger.noise("end of bullet path = " + lEnd);
			//graphics.drawLine(g, firingPosition, lEnd );
		}


		// draw current/presumed bullet position
		int bSize = 10;
		Point2D.Double bPos = getPosition();
		logger.noise("draw bullet at position = " + bPos);
		graphics.drawCircle(g, bPos, bSize/2);

		// draw overall bullet wave
		//double distTraveled = getDistanceTraveled();
		//graphics.drawCircle(g, firingPosition, distTraveled);
	}

	public Point2D.Double getFiringPosition() {
		return firingPosition;
	}

	public double getSpeed(){
		return bulletSpeed;
	}

	public Color getColor(){
		return bulletColor;
	}
}

