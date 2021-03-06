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
		double bEnergy = b.getBulletEnergy();
		String  gunSetKey = myBot.fightType();
		//logger.dbg("my bot pos " + myBot._tracker.getPosition() );
		LinkedList<baseGun> guns = myBot._gmanager.gunSets.get( gunSetKey );
		firedBullet tmpB;
		boolean bVirtualStatus;
		for ( baseGun g: guns ) {
			// IMPORTANT: by this time gun heat is  > 0 so take care that guns
			// use firing time = NOW
			double targetWeight= g.getTargetWeight( myBot._tracker, myBot._trgt, bEnergy );
			if ( g.getName().equals( b.getFiredGun().getName() ) ) {
				bVirtualStatus = false;
			} else {
				if ( targetWeight == 0 ) continue; // do not fire, when gun cannot aim
				bVirtualStatus = true; // vritual bullet
			}
			tmpB = new firedBullet( myBot, myBot._tracker, myBot._trgt, g, bEnergy );
			tmpB.setIsItVirtual(bVirtualStatus); // real bullet
			this.addBullet(tmpB);
			g.updBulletFiredCount( myBot._tracker, myBot._trgt, tmpB);
		}
	}

	public wave(EvBot bot, InfoBot firedBot, double bulletEnergy) {
		// wave fired by enemy bot
		myBot = bot;
		isItMine = false;
		this.firedBot = firedBot;
		baseGun firedGun = new baseGun();
		this.bulletSpeed = physics.bulletSpeed(bulletEnergy); 
		firedTime = myBot.ticTime-1; // recall we detect enemy bullet 1 tic later
		this.firingPosition = (Point2D.Double) firedBot.getPrevTicPosition();
		//logger.dbg("wave firingPosition = " + this.firingPosition );
		setEnemyPosAtFiringTime();
		setEnemyMEAatFiringTime();

		String  gunSetKey = "firingAtMyBot" + "_in_" + myBot.fightType();
		if ( !myBot._gmanager.gunSets.containsKey( gunSetKey ) ) {
			gunSetKey = "firingAtMyBot" + "_in_" + "default";
		}
		LinkedList<baseGun> guns = myBot._gmanager.gunSets.get( gunSetKey );
		for ( baseGun g: guns ) {
			// FIXME  the following line drops performance of the bot by quite 
			// a lot, despite a better tracking of enemy bullets,
			// at least head on ones are tracked well
			// but possibly a more advanced gun are faulty
			// following single line drop performance of my
			// v4.6.4 very significantly and looks like it was the only important
			// change. 
			// But it much proper from physics point of vies, so I need
			// to investigate what is going on inside gunBestBulletAtTime
			//firedBullet b = g.gunBestBulletAtTime( firedBot,  myBot._tracker, firedBot.energyDrop(), firedTime );
			// for now I revert to assumption that enemy is smart and can see 
			// in future to track my position. 
			// Recall I detect enemy fire one
			// tick later, and there is no way for enemy to know my parameters
			// 2 ticks from now
			firedBullet b = new firedBullet( myBot, firedBot,  g, firedBot.energyDrop() );
			if ( b == null ) continue;
			b.setIsItVirtual(true);  // virtual bullet
			//logger.dbg( "bullet from gun " + g.getName() );
			this.addBullet(b);
			g.updBulletFiredCount( firedBot, myBot._tracker, b );
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
		LinkedList<firedBullet> bulletsToRemove = new LinkedList<firedBullet>();
		LinkedList<InfoBot> aliveBots = new LinkedList<InfoBot>();
		aliveBots.addAll( myBot._botsmanager.listOfAliveBots() );
		aliveBots.add( myBot._tracker );
		for ( InfoBot bot : aliveBots ) {
			//logger.dbg("Wave fired by " + firedBot.getName() + " against bot " + bot.getName() );
			if ( firedBot.getName().equals( bot.getName() ) ) {
				// bot's bulllets do not affect their own firing bot
				continue;
			}
			Point2D.Double botPos = bot.getPosition();
			double dist2bot = botPos.distance( firingPosition );
			if ( Math.abs( waveDistNow - dist2bot ) <= physics.robotRadius ) {
				// the wave is the closest to the bot i.e. crosses the bot
				String bName = bot.getName();
				//logger.dbg("tic: " + myBot.getTime() + ": Wave fired by " + firedBot.getName() + " intersects with enemy bot " + bName );
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
					if ( b.getFiredGun().getName().equals("shadow") ) {
						// we keep shadow bullets intact
						// and do not count their stats

						continue;
					}

					if ( b.isPositionHitAtTime( botPos, myBot.getTime() ) ) {
						// bot hit by this bullet
						if ( bot.getName().equals( myBot.getName() ) ) {
							if ( myBot.fightType().equals( "1on1" ) ) {
								//logger.dbg("FIXME sloppy path finding algorithm at tic " +  myBot.getTime() +": myBot should not be hit by predicted bullet " + b.firedGun.getName() );
								//logger.dbg("Wave fired by " + firedBot.getName() + " intersects with bot " + bName );
							}
							myBot.bulletHitByPredictedCnt++;
						}
						//logger.dbg("schedule to remove bullet from " + b.getFiredGun().getName() + " gun fired by " + this.firedBot.getName() );
						bulletsToRemove.add(b); // if bullet hit it does not fly anymore
						// update stats for my bot
						if ( !bot.getName().equals( myBot.getName() ) ) {
							//logger.dbg("Enemy hit with " + b.getFiredGun().getName() );
							if ( !b.getFiredGun().getName().equals("shadow") ) {
								b.getFiredGun().updBulletHitCount( myBot._tracker, bot, b );
								//logger.dbg( b.getFiredGun().gunStatsHeader(myBot._tracker, bot ) );
								//logger.dbg( b.getFiredGun().gunStatsFormat(myBot._tracker, bot ) );
							}
						}
					}
				}
				// cleaning bullets which already hit a bot
				for ( firedBullet b : bulletsToRemove ) {
					bullets.remove(b);
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
		//logger.dbg("adding bullet from gun " + b.getFiredGun().getName() );
		bullets.add(b);
	}

	public void removeBullet(firedBullet b) {
		//logger.dbg("removing bullet from gun " + b.getFiredGun().getName() );
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
		double timeInFlight = time - firedTime;
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
		//logger.dbg("wave fired by " + firedBot.getName() + " has " + bullets.size() + " bullets");
		for ( firedBullet b : bullets ) {
			b.onPaint(g);
			//logger.dbg(" bullet gun " + b.getFiredGun().getName() );
		}

		// draw target positions at firing time
		for ( Point2D.Double p: enemyPosAtFiringTime.values() ) {
			graphics.drawSquare(g, p, 4);
		}

	}
}

