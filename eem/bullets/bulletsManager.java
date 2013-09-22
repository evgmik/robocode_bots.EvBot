
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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.*;


public class  bulletsManager {
	public static EvBot myBot;

	public LinkedList<firedBullet> bullets;
	
	public bulletsManager() {
		bullets = new LinkedList<firedBullet>();
	}

	public bulletsManager(EvBot bot) {
		myBot = bot;
		bullets = new LinkedList<firedBullet>();
	}

	public void initTic() {
		if (myBot._trgt.didItFireABullet() ) {
			double dummy =1;
			double bulletSpeed = dummy;
			this.add_enemy_bullet();
		}
		removeInactiveBullets();
	}

	public void add_enemy_bullet() {
		firedBullet b = new firedBullet(myBot, new enemyGun());
		bullets.add(b);
	}

	public void add( firedBullet b) {
		bullets.add(b);
	}

	public void removeInactiveBullets() {
		ListIterator<firedBullet> bLIter = bullets.listIterator();
		while (bLIter.hasNext()) {
			if (!bLIter.next().isActive() ) {
				bLIter.remove();
			}
		} 
	}

	public void onPaint(Graphics2D g) {
		removeInactiveBullets();
		for ( firedBullet b : bullets ) {
			b.onPaint(g);
		}
	}
}
