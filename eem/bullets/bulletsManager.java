
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
		createShadowingBullets();
	}

	public void createShadowingBullets() {
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
					wE.addBullet(bShadow);
				}
			}
		}
	}

	public void removeInactiveBulletsAndEmptyWaves() {
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
