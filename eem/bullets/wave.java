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
	public HashMap<String, Point2D.Double> enemyPosAtFiringTime = new HashMap<String, Point2D.Double>();
	// maximum escape angle 
	public HashMap<String, Double> enemyMEAatFiringTime = new HashMap<String, Double>();
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
		for ( InfoBot eBot : myBot._botsmanager.listOfAliveBots() ) {
			String key = eBot.getName();
			Point2D.Double enemyPos =  (Point2D.Double) eBot.getPosition().clone();
			enemyPosAtFiringTime.put( key, enemyPos );
			double maxBulletSpeed = 8;
			// Max escape angle
			double MEA = 180/Math.PI*Math.asin( maxBulletSpeed/bulletSpeed );
			//logger.dbg("For bot " + key + " MEA = " + MEA);
			enemyMEAatFiringTime.put( key, MEA );
		}
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

	public void initTic() {
		updatedHitBotGuessFactor();
	}

	public void updatedHitBotGuessFactor(){
		if ( !isItMine ) return;
		long time = myBot.ticTime;
		double waveDistNow  = this.getDistanceTraveledAtTime( time ); 
		double waveDistNext = this.getDistanceTraveledAtTime( time+1 ); 
		for ( InfoBot bot : myBot._botsmanager.listOfAliveBots() ) {
			Point2D.Double botPos = bot.getPosition();
			double dist2bot = botPos.distance( firingPosition );
			if ( Math.abs( waveDistNow - dist2bot ) <= Math.abs( waveDistNext - dist2bot ) ) {
				// the wave is the closest to the bot i.e. crosses the bot
				String bName = bot.getName();
				//logger.dbg("My wave intersects with enemy bot" );
				double guessFactor = math.angle2pt ( this.firingPosition, botPos ) - math.angle2pt ( this.firingPosition, enemyPosAtFiringTime.get(bName) );
				guessFactor /= enemyMEAatFiringTime.get(bName);
				//logger.dbg("guess factor for " + bName + " = " + guessFactor );
				bot.updateHitGuessFactor( guessFactor );
			}
		}
	}

	public Point2D.Double getFiringPosition() {
		return firingPosition;
	}

	public long getFiredTime () {
		return firedTime;
	}

	public LinkedList<firedBullet> getBullets () {
		return bullets;
	}

	public double distance(Point2D.Double p) {
		double dist = firingPosition.distance( p ) - getDistanceTraveled() ;
		return dist;
	}

	public void addBullet(firedBullet b) {
		bullets.add(b);
	}

	public void removeBullet(firedBullet b) {
		bullets.remove(b);
	}


	public void removeInactiveBullets() {
		ListIterator<firedBullet> bLIter;
		bLIter = this.bullets.listIterator();
		while (bLIter.hasNext()) {
			if (!bLIter.next().isActive() ) {
				bLIter.remove();
			}
		} 
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
		return  getDistanceTraveledAtTime( myBot.ticTime );
	}

	public void onPaint(Graphics2D g) {
		g.setColor(waveColor);

		// draw overall  wave
		double distTraveled = getDistanceTraveled();
		graphics.drawCircle(g, firingPosition, distTraveled);

		// draw wave bullets
		for ( firedBullet b : bullets ) {
			b.onPaint(g);
		}

		// draw target positions at firing time
		for ( Point2D.Double p: enemyPosAtFiringTime.values() ) {
			graphics.drawSquare(g, p, 4);
		}
	}
}

