// -*- java -*-

package eem.gun;

import eem.EvBot;
import eem.target.*;
import eem.bullets.*;
import eem.misc.*;
import java.util.Random;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import robocode.Bullet;


public class gunStats {
	private int bulletHitCount = 0;
	private int bulletFiredCount = 0;

	public gunStats() {
	}

	public int getBulletFiredCount() {
		return this.bulletFiredCount;
	}

	public int getBulletHitCount() {
		return this.bulletHitCount;
	}

	public int getBulletMissedCount() {
		return this.bulletFiredCount - this.bulletHitCount;
	}

	public void incBulletFiredCount() {
		this.bulletFiredCount++;
	}

	public void incBulletHitCount() {
		this.bulletHitCount++;
	}

	public double getGunHitRate() {
		return (this.getBulletHitCount() + 1.0) / (this.getBulletFiredCount() + 1.0);
	}

	public String format() {
		String str = "";
		str+= "hit target \t" + getBulletHitCount() + "\t and was fired \t" + getBulletFiredCount();
		return str;
	}
}
