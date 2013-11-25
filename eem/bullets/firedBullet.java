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
		myBot = bot;
		isItMine = false;
		firedGun = gun;
		this.bulletSpeed = firedGun.bulletSpeed(bulletEnergy); 
		// fixme enemy bullet detected 1 tic later so I need previous coord here
		this.firingPosition = (Point2D.Double) firedBot.getPosition().clone();
		this.targetPosition = (Point2D.Double) gun.calcTargetFuturePosition( firedBot, bulletEnergy, myBot._tracker);
		firedTime = myBot.ticTime;
		this.firingAngle = Math.atan2(targetPosition.x-firingPosition.x, targetPosition.y - firingPosition.y);
		bulletColor = firedGun.gunColor;
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

	public boolean isActive() {
		if (isItMine) {
			return robocodeBullet.isActive();
		} else {
			return !math.isItOutOfBorders(getPosition(), myBot.BattleField) ;
		}
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
			graphics.drawCircle(g, targetPosition, R);

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

