// -*- java -*-

package eem.gun;

import eem.EvBot;
import eem.target.*;
import eem.misc.*;
import eem.gun.misc;
import java.awt.Color;
import java.awt.geom.Point2D;
import robocode.util.*;
import java.util.*;

// play it forward (PIF) gun
public class pifGun extends baseGun {
	private static int bulletHitCount = 0;
	private static int bulletMissedCount = 0;
	private static int bulletFiredCount = 0;

	public int getBulletFiredCount() {
		return this.bulletFiredCount;
	}

	public int getBulletHitCount() {
		return this.bulletHitCount;
	}

	public int getBulletMissedCount() {
		return this.bulletFiredCount - this.bulletHitCount;
	}

	protected void incBulletFiredCount() {
		this.bulletFiredCount++;
	}

	public void incBulletHitCount() {
		this.bulletHitCount++;
	}

	public pifGun() {
		gunName = "pif";
		gunColor = new Color(0xff, 0x00, 0xff, 0x80);
	}

	public pifGun(EvBot bot) {
		this();
		myBot = bot;
		calcGunSettings();
	}

	public Point2D.Double calcTargetFuturePosition( Point2D.Double firingPosition, double firePower, InfoBot tgt) {
		Point2D.Double p = new Point2D.Double(0,0);

		long afterTime = 10;
		long refLength  = 3;

		LinkedList<Point2D.Double> posList = tgt.possiblePositionsAfterTime(afterTime, refLength);
		//logger.dbg("Match list size = " + posList.size() );
		if ( posList.size() < 1 ) {
			p = tgt.getPosition();
		} else {
			p = posList.getLast();
		}
		return p;
	}


}	