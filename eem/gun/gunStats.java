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
	private int bulletVirtHitCount = 0;
	private int bulletVirtFiredCount = 0;
	private int bulletRealHitCount = 0;
	private int bulletRealFiredCount = 0;

	public gunStats() {
	}

	public int getBulletVirtFiredCount() {
		return this.bulletVirtFiredCount;
	}

	public int getBulletVirtHitCount() {
		return this.bulletVirtHitCount;
	}

	public int getBulletVirtMissedCount() {
		return this.bulletVirtFiredCount - this.bulletVirtHitCount;
	}

	public void incBulletVirtFiredCount() {
		this.bulletVirtFiredCount++;
	}

	public void incBulletVirtHitCount() {
		this.bulletVirtHitCount++;
	}

	public double getGunVirtHitRate() {
		return (this.getBulletVirtHitCount() ) / (this.getBulletVirtFiredCount() + 1.0);
	}

	public double getGunVirtPerformance() {
		return (this.getBulletVirtHitCount() + 1.0) / (this.getBulletVirtFiredCount() + 1.0);
	}

	public String format() {
		String str = "";
		str+= "virtual gun hit target \t" + getBulletVirtHitCount() + "\t and was fired \t" + getBulletVirtFiredCount();
		return str;
	}
}
