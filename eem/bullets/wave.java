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
	public InfoBot firedBot = null;
	//public String firedBotName = "";  // not used
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
		firedBot = myBot._tracker;
	}

	public wave(EvBot bot, firedBullet b) {
		// wave fired by my bot
		myBot = bot;
		isItMine = true;
		firedBot = myBot._tracker;
		firingPosition = (Point2D.Double) b.getFiringPosition().clone();
		firedTime = myBot.ticTime;
		bulletSpeed = b.getSpeed();
		setEnemyPosAtFiringTime();
		setEnemyMEAatFiringTime();
		waveColor = (Color) b.getColor();
		// fill wave with virtual bullets
		//w.addBullet(b);
		double bEnergy = b.bulletEnergy();
		String  gunSetKey = myBot.fightType();
		//logger.dbg("my bot pos " + myBot._tracker.getPosition() );
		LinkedList<baseGun> guns = myBot._gmanager.gunSets.get( gunSetKey );
		firedBullet tmpB;
		for ( baseGun g: guns ) {
			// FIXME differentiate between virtual and real fired bullet gun
			tmpB = new firedBullet( myBot, myBot._tracker, myBot._trgt, g, bEnergy );
			this.addBullet(tmpB);
			g.incBulletFiredCount();
		}
	}

	public wave(EvBot bot, InfoBot firedBot, double bulletEnergy) {
		// wave fired by enemy bot
		myBot = bot;
		isItMine = false;
		this.firedBot = firedBot;
		baseGun firedGun = new baseGun();
		this.bulletSpeed = firedGun.bulletSpeed(bulletEnergy); 
		// fixme enemy bullet detected 1 tic later so I need previous coord here
		this.firingPosition = (Point2D.Double) firedBot.getPosition().clone();
		firedTime = myBot.ticTime;
		setEnemyPosAtFiringTime();
		setEnemyMEAatFiringTime();

		String  gunSetKey = "firingAtMyBot" + "_in_" + myBot.fightType();
		if ( !myBot._gmanager.gunSets.containsKey( gunSetKey ) ) {
			gunSetKey = "firingAtMyBot" + "_in_" + "default";
		}
		LinkedList<baseGun> guns = myBot._gmanager.gunSets.get( gunSetKey );
		for ( baseGun g: guns ) {
			firedBullet b = new firedBullet( myBot, firedBot,  g, firedBot.energyDrop() );
			this.addBullet(b);
		}
	}

	private void setEnemyPosAtFiringTime() {
		LinkedList<InfoBot> aliveBots = new LinkedList<InfoBot>();
		aliveBots.addAll( myBot._botsmanager.listOfAliveBots() );
		aliveBots.add( myBot._tracker );
		for ( InfoBot eBot : aliveBots ) {
			if ( firedBot.getName().equals( eBot.getName() ) ) {
				continue;
			}
			String key = eBot.getName();
			Point2D.Double enemyPos =  (Point2D.Double) eBot.getPosition().clone();
			enemyPosAtFiringTime.put( key, enemyPos );
		}
	}
	private void setEnemyMEAatFiringTime() {
		LinkedList<InfoBot> aliveBots = new LinkedList<InfoBot>();
		aliveBots.addAll( myBot._botsmanager.listOfAliveBots() );
		aliveBots.add( myBot._tracker );
		for ( InfoBot eBot : aliveBots ) {
			if ( firedBot.getName().equals( eBot.getName() ) ) {
				continue;
			}
			String key = eBot.getName();
			// Max escape angle
			double MEA = math.calculateMEA( bulletSpeed );
			enemyMEAatFiringTime.put( key, MEA );
		}
	}

	public double getSpeed() {
		return bulletSpeed;
	}
	public void initTic() {
		updatedWaveStats();
	}

	public void updatedWaveStats(){
		//if ( !isItMine ) return;
		long time = myBot.ticTime;
		double waveDistNow  = this.getDistanceTraveledAtTime( time ); 
		double waveDistNext = this.getDistanceTraveledAtTime( time+1 ); 
		LinkedList<InfoBot> aliveBots = new LinkedList<InfoBot>();
		aliveBots.addAll( myBot._botsmanager.listOfAliveBots() );
		aliveBots.add( myBot._tracker );
		for ( InfoBot bot : aliveBots ) {
			//logger.dbg("Wave fired by " + firedBot.getName() + " against bot " + bot.getName() );
			if ( firedBot.getName().equals( bot.getName() ) ) {
				continue;
			}
			Point2D.Double botPos = bot.getPosition();
			double dist2bot = botPos.distance( firingPosition );
			if ( Math.abs( waveDistNow - dist2bot ) <= Math.abs( waveDistNext - dist2bot ) ) {
				// the wave is the closest to the bot i.e. crosses the bot
				String bName = bot.getName();
				//logger.dbg("Wave fired by " + firedBot.getName() + " intersects with enemy bot " + bName );
				// now we update hit guess factor
				double angle = math.angle2pt ( this.firingPosition, botPos ) - math.angle2pt ( this.firingPosition, enemyPosAtFiringTime.get(bName) );
				angle = math.shortest_arc( angle );
				double guessFactor = angle/enemyMEAatFiringTime.get(bName);
				guessFactor = Math.max(-1, guessFactor );
				guessFactor = Math.min( 1, guessFactor );
				firedBot.updateHitGuessFactor( bot, guessFactor );
				//logger.dbg("guess factor for " + bName + " = " + guessFactor );
				//logger.dbg( bName + ":\t" +  bot.guessFactorBins2string() );

				// now let's check which bullet hit this bot
				for ( firedBullet b : this.getBullets() ) {

					if ( botPos.distance( b.getPosition() ) <= Math.sqrt(2)*myBot.robotHalfSize ) {
						// bot hit by this bullet
						if ( myBot.fightType().equals( "1on1" ) ) {
							//logger.dbg("FIXME sloppy path finding algorithm at tic " +  myBot.ticTime +": myBot should not be hit by predicted bullet " + b.firedGun.getName() );
						}
					}
				}

			}
		}
	}

	public int getGuessFactorCount( InfoBot targetBot, double guessFactor) {
		return this.firedBot.getGuessFactorCount( targetBot, guessFactor );
	}

	public double getGuessFactorForPoint( InfoBot targetBot, Point2D.Double p) {
		// calculate guess factor point counts for wave at point p
		// as if it directed to targetBot
		String targetName = targetBot.getName();
		double angle = math.angle2pt ( this.firingPosition, p ) - math.angle2pt ( this.firingPosition, this.enemyPosAtFiringTime.get( targetName ) );
		angle = math.shortest_arc( angle );
		double guessFactor = angle/this.enemyMEAatFiringTime.get( targetName );
		return guessFactor;
	}


	public int getGuessFactorCountForPoint( InfoBot targetBot, Point2D.Double p) {
		// calculate guess factor point counts for wave at point p
		// as if it directed to targetBot
		double guessFactor = getGuessFactorForPoint( targetBot, p);
		return this.firedBot.getGuessFactorCount( targetBot, guessFactor );
	}

	public double getGuessFactorNormProbForPoint( InfoBot targetBot, Point2D.Double p) {
		double guessFactor = getGuessFactorForPoint( targetBot, p);
		return this.firedBot.getGuessFactorNormProb( targetBot, guessFactor );
	}

	public double getGuessFactorProbForPoint( InfoBot targetBot, Point2D.Double p) {
		double guessFactor = getGuessFactorForPoint( targetBot, p);
		return this.firedBot.getGuessFactorProb( targetBot, guessFactor );
	}

	public boolean isPosWithMEAforBot( Point2D.Double pos, InfoBot bot) {
		Point2D.Double botPos = (Point2D.Double) enemyPosAtFiringTime.get( bot.getName() ).clone();
		double botMEA = enemyMEAatFiringTime.get( bot.getName() );
		double angle2botPast = math.angle2pt ( this.firingPosition, botPos );
		double angle2pos = math.angle2pt ( this.firingPosition, pos );
		if ( Math.abs( angle2pos - angle2botPast ) < botMEA ) {
			return true;
		}
		return false;
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

	public double distance(Point2D.Double p, long tic) {
		double dist = firingPosition.distance( p ) - getDistanceTraveledAtTime(tic) ;
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

		if (!isItMine) {
			// draw MEA for me from enemy bot
			Point2D.Double myPos = (Point2D.Double) enemyPosAtFiringTime.get( myBot.getName() ).clone();
			double myMEA = enemyMEAatFiringTime.get( myBot.getName() );
			double angle2myPast = math.angle2pt ( this.firingPosition, myPos );
			double startAngle = angle2myPast + myMEA;
			double stopAngle  = angle2myPast - myMEA;
			graphics.drawCircArc(g, firingPosition, distTraveled-5, startAngle, stopAngle);
			// draw GF danger for me from enemy bot
			int[] guessFactorBins = this.firedBot.getGuessFactorBins(myBot._tracker);
			int N = guessFactorBins.length;
			double MEA = math.calculateMEA( bulletSpeed );
			Point2D.Double pStrt = new Point2D.Double(0,0);
			Point2D.Double pEnd = new Point2D.Double(0,0);
			double radius;
			for( int i=0; i<N; i++ ) {
				// show this  GF danger
				double gf = math.bin2gf(i, N);
				double angleRad = Math.toRadians(angle2myPast + MEA*gf);
				radius = distTraveled + 5;
				pStrt.x = firingPosition.x + radius*Math.sin(angleRad);
				pStrt.y = firingPosition.y + radius*Math.cos(angleRad);
				radius = distTraveled + 5 + 1+ 20*this.firedBot.getGuessFactorNormProb(myBot._tracker, gf);

				pEnd.x = firingPosition.x + radius*Math.sin(angleRad);
				pEnd.y = firingPosition.y + radius*Math.cos(angleRad);
				graphics.drawLine(g, pStrt, pEnd);
			}

		}

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

