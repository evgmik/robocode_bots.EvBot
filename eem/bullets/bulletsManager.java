
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
	public LinkedList<wave> waves;
	
	public bulletsManager() {
		waves = new LinkedList<wave>();
	}

	public bulletsManager(EvBot bot) {
		myBot = bot;
		waves = new LinkedList<wave>();
	}

	public void initTic() {
		removeInactiveBullets();
		removeInactiveWaves();
	}

	public void add_enemy_wave(InfoBot firedBot) {
		baseGun eGun = new baseGun(myBot);
		eGun.incBulletFiredCount(myBot._tracker, firedBot);
		firedBullet b;

		// create bullet wave
		wave w = new wave( myBot, firedBot, firedBot.energyDrop() );
		waves.add(w);

		LinkedList<baseGun> guns = myBot._gmanager.gunSets.get( "firingAtMyBot" );
		for ( baseGun g: guns ) {
			b = new firedBullet( myBot, firedBot,  g, firedBot.energyDrop() );
			w.addBullet(b);
		}
	}

	public void add( firedBullet b) {
		wave w = new wave( myBot, b );
		waves.add(w);
		w.addBullet(b);
	}

	public void removeInactiveWaves() {
		ListIterator<wave> wLIter;
		wLIter = waves.listIterator();
		while (wLIter.hasNext()) {
			if (wLIter.next().bullets.size() == 0) {
				wLIter.remove();
			}
		} 
	}

	public void removeInactiveBullets() {
		ListIterator<firedBullet> bLIter;
		for (wave w: waves) {
			bLIter = w.bullets.listIterator();
			while (bLIter.hasNext()) {
				if (!bLIter.next().isActive() ) {
					bLIter.remove();
				}
			} 
		}
	}

	public LinkedList<firedBullet> getAllBullets() {
		LinkedList<firedBullet> bullets = new LinkedList<firedBullet>();
		for (wave w: waves) {
			bullets.addAll( w.bullets );
		}
		return bullets;
	}

	public baseGun whichGunFiredBullet(Bullet b) {
		LinkedList<firedBullet> bullets = getAllBullets();
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
		//removeInactiveBullets();
		for ( wave w : waves ) {
			w.onPaint(g);
		}
	}
}
