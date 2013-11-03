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
import java.util.*;

public class wave {
	protected EvBot myBot;

	protected Bullet robocodeBullet;
	public String firedBotName = "";
	protected boolean isItMine = true;
	protected Point2D.Double firingPosition;
	protected long   firedTime;
	protected double bulletSpeed;
	public LinkedList<firedBullet> bullets = new LinkedList<firedBullet>();
	protected Color waveColor = new Color(0xff, 0x00, 0x00, 0x80);

	public wave() {
	}

	public wave(EvBot bot) {
		myBot = bot;
		firingPosition = myBot.myCoord;
	}

	public wave(EvBot bot, firedBullet b) {
		myBot = bot;
		isItMine = true;
		firingPosition = (Point2D.Double) b.getFiringPosition().clone();
		firedTime = myBot.ticTime;
		bulletSpeed = b.getSpeed();
		waveColor = (Color) b.getColor();
	}

	public wave(EvBot bot, InfoBot firedBot, double bulletEnergy) {
		myBot = bot;
		isItMine = false;
		baseGun firedGun = new baseGun();
		this.bulletSpeed = firedGun.bulletSpeed(bulletEnergy); 
		// fixme enemy bullet detected 1 tic later so I need previous coord here
		this.firingPosition = (Point2D.Double) firedBot.getPosition().clone();
		firedTime = myBot.ticTime;
	}

	public void addBullet(firedBullet b) {
		bullets.add(b);
	}


	public double getDistanceTraveled() {
		double timeInFlight = myBot.ticTime - firedTime + 1;
		if ( !isItMine ) {
			timeInFlight = timeInFlight + 1;
		}
		double distTraveled = timeInFlight * bulletSpeed;
		return distTraveled;
	}

	public void onPaint(Graphics2D g) {
		g.setColor(waveColor);

		// draw overall  wave
		double distTraveled = getDistanceTraveled();
		graphics.drawCircle(g, firingPosition, distTraveled);

		for ( firedBullet b : bullets ) {
			b.onPaint(g);
		}
	}
}
