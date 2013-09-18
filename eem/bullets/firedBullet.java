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
		myBot.dbg(myBot.dbg_noise, "fired bullet target position = " + targetPosition);
		firingPosition = new Point2D.Double( b.getX(), b.getY() );
		myBot.dbg(myBot.dbg_debuging, "bullet firing position = " + firingPosition);
		firedTime = myBot.ticTime;
		firingAngle = b.getHeadingRadians();
		bulletSpeed = b.getVelocity();
		myBot.dbg(myBot.dbg_noise, "fired bullet speed = " + bulletSpeed);
		bulletColor = firedGun.gunColor;
	}

	public void onPaint(Graphics2D g) {
		g.setColor(bulletColor);
		// draw target position
		int ovalSize = 10;
		myBot.dbg(myBot.dbg_noise, "draw target at position = " + targetPosition);
		g.drawOval( (int)(targetPosition.x - ovalSize/2), (int)(targetPosition.y-ovalSize/2), ovalSize, ovalSize);

		// draw line from firing point to target
		double long_length = firingPosition.distance(targetPosition);
		Point2D.Double lEnd = (Point2D.Double) targetPosition.clone();
		//fixme truncation to border works incorrectly
		lEnd.x = firingPosition.x + Math.sin(firingAngle)*long_length;
		lEnd.y = firingPosition.y + Math.cos(firingAngle)*long_length;
		//lEnd = math.putWithinBorders(lEnd, myBot.BattleField);
		
		myBot.dbg(myBot.dbg_debuging, "end of bullet path = " + lEnd);

		g.drawLine((int) firingPosition.x, (int) firingPosition.y, (int)lEnd.x, (int)lEnd.y);
		
	}
}

