
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


public class  bulletsManager {
	public EvBot myBot;
	public LinkedList<wave> myWaves = new LinkedList<wave>();
	public LinkedList<wave> enemyWaves = new LinkedList<wave>();
	// our wave is mainly used to collect gun stats
	// further bots unlikely to react on our shots so I limit number of bots
	// which wave need to cross
	// this is mainly to use for GF gun stats.
	// For other guns stats it make sence to track all the bots
	private int numBotsForWaveToIntersect = 2; 
	
	public bulletsManager() {
	}

	public bulletsManager(EvBot bot) {
		myBot = bot;
	}

	public void initTic() {
		removeInactiveBulletsAndEmptyWaves();
		//createShadowsFromMyBullets();
		createShadowsFromOtherBots();
		for ( wave w : myWaves ) {
			w.initTic();
		}
		for ( wave w : enemyWaves ) {
			w.initTic();
		}
	}

	public void createShadowsFromOtherBots() {
		long time = myBot.ticTime;
		for ( wave wE : enemyWaves ) {
			Point2D.Double firingPos = wE.getFiringPosition();
			double waveDist = wE.getDistanceTraveledAtTime(time); 
			for ( InfoBot bot : myBot._botsmanager.listOfAliveBots() ) {
				Point2D.Double botPos = bot.getPosition();
				if ( Math.abs( waveDist - firingPos.distance( botPos )  ) <= myBot.robotHalfSize ) {
					// check if current bullets hit the enemy bot
					LinkedList<firedBullet> bulletsToRemove = new LinkedList<firedBullet>();
					for ( firedBullet b: wE.getBullets() ) {
						if ( b.getFiredGun().getName().equals("shadow") ) {
							// do not remove shadows
							continue;
						}
						Point2D.Double bulPos = b.getPosition();
						if ( ( Math.abs( bulPos.x - botPos.x ) <= myBot.robotHalfSize ) && ( Math.abs( bulPos.y - botPos.y ) <= myBot.robotHalfSize ) ) {
							bulletsToRemove.add( b );
						}

					}

					// remove screened bullets
					for ( firedBullet b: bulletsToRemove ) {
						wE.removeBullet( b );
					}
					
					// add shadow from this bot
					if ( wE.isPosWithMEAforBot( botPos, myBot._tracker ) ) {
						baseGun shadowGun = new shadowGun();
						firedBullet bShadow = new firedBullet( myBot, wE, shadowGun, botPos);
						wE.addBullet(bShadow);
					}

				}
			}
		}
	}

	public void createShadowsFromMyBullets() {
		long time = myBot.ticTime;
		for ( wave w : myWaves ) {
			firedBullet b = w.bullets.getFirst(); //my waves have only one bullet
			Point2D.Double posPrev = b.getPositionAtTime( time - 1);
			Point2D.Double posNow =  b.getPositionAtTime( time );
			for ( wave wE : enemyWaves ) {
				double distNow = wE.getDistanceTraveledAtTime(time); 
				double distPrev = wE.getDistanceTraveledAtTime(time-1); 
				Point2D.Double firingPos = wE.getFiringPosition();
				if ( ( firingPos.distance( posPrev ) > distPrev ) && ( firingPos.distance( posNow ) <= distNow ) ) {
					Point2D.Double crossingPos  = new Point2D.Double(0,0);
					// FIXME: use not so crude algorithm
					crossingPos.x = (posNow.x + posPrev.x)/2;
					crossingPos.y = (posNow.y + posPrev.y)/2;
					//logger.dbg("Crossed enemy wave at " + crossingPos);
					baseGun shadowGun = new shadowGun();
					firedBullet bShadow = new firedBullet( myBot, wE, shadowGun, crossingPos);
					// FIXME: head on almost have no shadows
					// need to have some logic here
					wE.addBullet(bShadow);
				}
			}
		}
	}

	public void removeInactiveBulletsAndEmptyWaves() {
		removeWavesBehindMe();
		removeMyWavesBehindEnemies();
		removeInactiveBullets();
		//removeEmptyWaves();
	}

	public void add_enemy_wave(InfoBot firedBot) {
		baseGun eGun = new baseGun(myBot);
		eGun.incBulletVirtFiredCount(myBot._tracker, firedBot);

		// create bullet wave
		wave w = new wave( myBot, firedBot, firedBot.energyDrop() );
		enemyWaves.add(w);
	}

	public void add( firedBullet b) {
		// adds my waves
		myBot.bulletFiredCnt++;
		wave w = new wave( myBot, b );
		myWaves.add(w);
	}

	public void removeEmptyWavesFromList( LinkedList<wave> waves ) {
		// this removes waves with no bullets in them
		ListIterator<wave> wLIter;
		wLIter = waves.listIterator();
		while (wLIter.hasNext()) {
			if (wLIter.next().bullets.size() == 0) {
				wLIter.remove();
			}
		} 
	}
	public void removeEmptyWaves() {
		// this removes waves with no bullets in them
		removeEmptyWavesFromList( enemyWaves );
		removeEmptyWavesFromList( myWaves );
	}

	public void removeInactiveBulletsFromWaveList( LinkedList<wave> waves ) {
		for (wave w: waves) {
			w.removeInactiveBullets();
		}
	}
	public void removeInactiveBullets() {
		removeInactiveBulletsFromWaveList( enemyWaves );
		removeInactiveBulletsFromWaveList( myWaves );
	}

	public void removeMyWavesBehindEnemies() {
		ListIterator<wave> wLIter;
		wLIter = myWaves.listIterator();
		while (wLIter.hasNext()) {
			wave w = wLIter.next();
			double distWaveTrav =  w.getDistanceTraveled();
			int numBotsBehind = 0;
			for ( InfoBot eBot: myBot._botsmanager.listOfAliveBots() ) {
				double distToBot = w.getFiringPosition().distance( eBot.getPosition() );
				if ( ( distToBot + 2*myBot.robotHalfSize ) < distWaveTrav ) {
					numBotsBehind++;
				}
				if ( ( numBotsBehind == myBot.getOthers() ) || ( numBotsBehind >= numBotsForWaveToIntersect ) ) {
					wLIter.remove();
					break;
				}
			}
		} 
	}

	public void removeWavesBehindMe() {
		ListIterator<wave> wLIter;
		wLIter = enemyWaves.listIterator();
		while (wLIter.hasNext()) {
			wave wE = wLIter.next();
			double distWaveTrav =  wE.getDistanceTraveled();
			double distToMe = wE.getFiringPosition().distance( myBot.myCoord );
			if ( ( distToMe + 2*myBot.robotHalfSize ) < distWaveTrav ) {
				wLIter.remove();
			}
		} 
	}

	public wave getClosestToMeWave() {
		ListIterator<wave> wLIter;
		wave closestWave = null;
		wLIter = enemyWaves.listIterator();
		double closestDist=1e6; // unreasonably large
		while (wLIter.hasNext()) {
			wave wE = wLIter.next();
			double distToMe = wE.distance( myBot.myCoord );
			if ( (distToMe < closestDist) && ( (distToMe + myBot.robotHalfSize) > 0 )) {
				closestWave = wE;
				closestDist = distToMe;
			}
		} 
		return closestWave;
	}

	public double getClosestToMeWaveDist() {
		wave wE = getClosestToMeWave();
		double distToMe=1e6; // unreasonably large
		if ( wE == null ) 
			return distToMe;
		distToMe = wE.distance( myBot.myCoord );
		return distToMe;
	}

	public double getClosestToMeWaveTimeArrival() {
		wave wE = getClosestToMeWave();
		double t=1e6; // unreasonably large
		if ( wE == null ) 
			return t;
		t = wE.distance( myBot.myCoord ) / wE.getSpeed();
		return t;
	}


	public LinkedList<firedBullet> getAllEnemyBullets() {
		LinkedList<firedBullet> bullets = new LinkedList<firedBullet>();
		for (wave w: enemyWaves) {
			bullets.addAll( w.bullets );
		}
		return bullets;
	}

	public LinkedList<firedBullet> getAllMyBullets() {
		LinkedList<firedBullet> bullets = new LinkedList<firedBullet>();
		for (wave w: myWaves) {
			bullets.addAll( w.bullets );
		}
		return bullets;
	}

	public LinkedList<wave> getAllEnemyWaves() {
		return enemyWaves;
	}

	public LinkedList<baseGun> whichOfMyGunsFiredBullet(Bullet b) {
		LinkedList<baseGun> luckyGunsList = new LinkedList<baseGun>();
		LinkedList<firedBullet> bullets = getAllMyBullets();
		ListIterator<firedBullet> bLIter = bullets.listIterator();
		firedBullet  fB;
		baseGun  gun = null;
		double bx = b.getX();
		double by = b.getY();
		double dx, dy, dist;
		while (bLIter.hasNext()) {
			fB = bLIter.next();
			if ( fB.isItMine() ) {
				Point2D.Double bfB = (Point2D.Double) fB.getPosition().clone();
				dx = Math.abs(bx - bfB.x);
				dy = Math.abs(by - bfB.y);
				//dist = Math.sqrt( dx*dx + dy*dy );
				//logger.noise("Fired bullet known coordinates " + bfBx + ", " + bfBy + " \t bullet reported " + bx + ", " + by + "dist = " + dist );
				// below is dirty hack since robocode seems to miss report
				// bullet position from time to time
				// and actual bullet has diffrent coordinates
				// from reported by onBulletHit method
				if (( dx <= myBot.robotHalfSize ) && ( dy <= myBot.robotHalfSize ) ) {
					gun = fB.firedGun;
					luckyGunsList.add(gun);
					logger.noise("This bullet was fired by gun = " + gun.getName() );
				}
			}
		} 
		return  luckyGunsList;
	}

	public void onPaint(Graphics2D g) {
		for ( wave w : enemyWaves ) {
			w.onPaint(g);
		}
		for ( wave w : myWaves ) {
			w.onPaint(g);
		}
	}
}
