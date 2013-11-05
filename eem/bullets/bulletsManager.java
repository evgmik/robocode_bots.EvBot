
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
	
	public bulletsManager() {
	}

	public bulletsManager(EvBot bot) {
		myBot = bot;
	}

	public void initTic() {
		removeInactiveBulletsAndEmptyWaves();
		//createShadowsFromMyBullets();
		createShadowsFromOtherBots();
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
					baseGun shadowGun = new shadowGun();
					firedBullet bShadow = new firedBullet( myBot, wE, shadowGun, botPos);
					wE.addBullet(bShadow);

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
		removeInactiveBullets();
		removeEmptyWaves();
	}

	public void add_enemy_wave(InfoBot firedBot) {
		baseGun eGun = new baseGun(myBot);
		eGun.incBulletFiredCount(myBot._tracker, firedBot);
		firedBullet b;

		// create bullet wave
		wave w = new wave( myBot, firedBot, firedBot.energyDrop() );
		enemyWaves.add(w);

		LinkedList<baseGun> guns = myBot._gmanager.gunSets.get( "firingAtMyBot" );
		for ( baseGun g: guns ) {
			b = new firedBullet( myBot, firedBot,  g, firedBot.energyDrop() );
			w.addBullet(b);
		}
	}

	public void add( firedBullet b) {
		// adds my waves
		wave w = new wave( myBot, b );
		myWaves.add(w);
		w.addBullet(b);
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

	public baseGun whichOfMyGunsFiredBullet(Bullet b) {
		LinkedList<firedBullet> bullets = getAllMyBullets();
		ListIterator<firedBullet> bLIter = bullets.listIterator();
		firedBullet  fB;
		baseGun  gun = null;
		double bx = b.getX();
		double by = b.getY();
		double bfBx, bfBy, dx, dy, dist;
		while (bLIter.hasNext()) {
			fB = bLIter.next();
			if ( fB.isItMine ) {
				bfBx = fB.robocodeBullet.getX();
				bfBy = fB.robocodeBullet.getY();
				dx = Math.abs(bx - bfBx);
				dy = Math.abs(by - bfBy);
				//dist = Math.sqrt( dx*dx + dy*dy );
				//logger.noise("Fired bullet known coordinates " + bfBx + ", " + bfBy + " \t bullet reported " + bx + ", " + by + "dist = " + dist );
				// below is dirty hack since robocode seems to miss report
				// bullet position from time to time
				// and actual bullet has diffrent coordinates
				// from reported by onBulletHit method
				if (( dx <= myBot.robotHalfSize ) && ( dy <= myBot.robotHalfSize ) ) {
					gun = fB.firedGun;
					logger.noise("This bullet was fired by gun = " + gun.getName() );
					break;
				}
			}
		} 
		return  gun;
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
