// -*- java -*-

package eem.target;

import eem.EvBot;
import eem.target.*;
import eem.misc.*;
import java.awt.geom.Point2D;
import java.awt.Color;
import java.awt.Graphics2D;

public class InfoBot {
	private int nonexisting_coord = -10000;
	private long far_ago  = -10000;
	private botStatPoint statLast;
	private botStatPoint statPrev;
	private String name = "";

	private static int bulletHitCount = 0;
	private static int bulletFiredCount = 0;

	public InfoBot() {
		statPrev = new botStatPoint( new Point2D.Double( nonexisting_coord, nonexisting_coord), far_ago);
		statLast = new botStatPoint( new Point2D.Double( nonexisting_coord, nonexisting_coord), far_ago);
	}

	public InfoBot(String botName) {
		this();
		setName(botName);
	}

	
	public int getBulletFiredCount() {
		return this.bulletFiredCount;
	}

	public int getBulletHitCount() {
		return this.bulletHitCount;
	}

	protected void incBulletFiredCount() {
		this.bulletFiredCount++;
	}

	public void incBulletHitCount() {
		this.bulletHitCount++;
	}

	public double getGunHitRate() {
		return (this.getBulletHitCount() ) / (this.getBulletFiredCount() + 1.0);
	}

	public void printGunsStats() {
		logger.routine("Enemy gun hit rate = " + this.getGunHitRate() );
	}

	public void initTic(long ticTime) {
		if ( didItFireABullet() ) {
			this.incBulletFiredCount();
		}
	}

	public InfoBot update(Point2D.Double pos, long tStamp) {
		statPrev = statLast;
		statLast = new botStatPoint(pos, tStamp);
		return this;
	}

	public InfoBot update(botStatPoint statPnt) {
		statPrev = statLast;
		statLast = statPnt;
		return this;
	}

	public double getEnergy() {
		return statLast.getEnergy();
	}

	public Point2D.Double getVelocity() {
		return statLast.getVelocity();
	}

	public void setName(String n) {
		name = n;
	}

	public double getLastDistance(Point2D.Double p) {
		return  statLast.getDistance(p);
	}

	public double getX() {
		return  statLast.getX();
	}

	public double getY() {
		return  statLast.getY();
	}

	public Point2D.Double getPosition() {
		return  statLast.getPosition();
	}

	public long getLastSeenTime() {
		return  statLast.getTimeStamp();
	}


	public double energyDrop() {
		return  statPrev.getEnergy() - statLast.getEnergy();
	}

	public boolean didItFireABullet() {
		boolean stat = true;
		double eDrop = energyDrop();
		if ( (eDrop < .1) || (3 < eDrop) ) {
			stat=false;
			logger.noise("enemy did not fired a bullet");
		}
		return stat;
	}


	public String getName() {
		return name;
	}

	public String format() {
		String str;
		str = "Target bot name: " + getName() + "\n";
		str = str + "Last: " + statLast.format() + "\n" + "Prev: " + statPrev.format();
		return str;
	}

	public void drawLastKnownBotPosition(Graphics2D g) {
		g.setColor(new Color(0xff, 0xff, 0x00, 0x80));
		int ovalSize = 50;
		g.drawOval( (int)(statLast.getX()- ovalSize/2), (int)(statLast.getY()-ovalSize/2), ovalSize, ovalSize);
	}

	public void drawBotPath(Graphics2D g) {
		g.setColor(new Color(0xff, 0xff, 0x00, 0x80));
		g.drawLine((int)statLast.getX(), (int)statLast.getY(), (int)statPrev.getX(), (int)statPrev.getY());
	}

	public void onPaint(Graphics2D g) {
		drawLastKnownBotPosition(g);
		drawBotPath(g);
	}

}

