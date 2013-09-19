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
	protected static EvBot myBot;

	protected Bullet robocodeBullet;
	protected baseGun firedGun;
	protected boolean isItMine = false;
	protected Point2D.Double targetPosition;
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

	public Point2D.Double getPosition() {
		Point2D.Double pos =  (Point2D.Double) firingPosition.clone();
		double timeInFlight = myBot.ticTime - firedTime + 1;
		if ( !isItMine ) {
			timeInFlight = timeInFlight + 1;
		}
		double distTraveled = timeInFlight * bulletSpeed;
		pos.x = pos.x + distTraveled*Math.sin(firingAngle);
		pos.y = pos.y + distTraveled*Math.cos(firingAngle);
		return pos;
	}

	public void onPaint(Graphics2D g) {
		g.setColor(bulletColor);
		// draw target position
		int ovalSize = 10;
		logger.noise("draw target at position = " + targetPosition);
		g.drawOval( (int)(targetPosition.x - ovalSize/2), (int)(targetPosition.y-ovalSize/2), ovalSize, ovalSize);

		// draw line from firing point to target
		double long_length = firingPosition.distance(targetPosition);
		Point2D.Double lEnd = (Point2D.Double) targetPosition.clone();
		lEnd.x = firingPosition.x + Math.sin(firingAngle)*long_length;
		lEnd.y = firingPosition.y + Math.cos(firingAngle)*long_length;
		//fixme truncation to border works incorrectly
		//lEnd = math.putWithinBorders(lEnd, myBot.BattleField);
		
		logger.noise("end of bullet path = " + lEnd);

		g.drawLine((int) firingPosition.x, (int) firingPosition.y, (int)lEnd.x, (int)lEnd.y);

		// draw bullet position
		int bSize = 10;
		Point2D.Double bPos = getPosition();

		logger.noise("draw bullet at position = " + bPos);
		g.drawOval( (int)(bPos.x - bSize/2), (int)(bPos.y-bSize/2), bSize, bSize);

		
	}
}

