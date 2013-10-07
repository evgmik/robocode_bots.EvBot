// -*- java -*-

package eem.target;

import eem.EvBot;
import eem.target.*;
import eem.misc.*;
import java.awt.geom.Point2D;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.*;

public class InfoBot {
	protected String name = "";
	protected LinkedList<botStatPoint> botStats;
	public boolean targetUnlocked = true; 

	protected int bulletHitCount = 0;
	protected int bulletFiredCount = 0;

	public InfoBot() {
		botStats = new LinkedList<botStatPoint>();
		targetUnlocked = true;
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
		// updating UnLocked status
		if ( ( ticTime - this.getLastSeenTime() ) > 2) 
			this.setUnLockedStatus(true);
		else
			this.setUnLockedStatus(false);

		// firing status
		if ( didItFireABullet(ticTime) ) {
			this.incBulletFiredCount();
		}
	}

	public boolean hasLast() {
		int n = botStats.size();
		if ( n >= 1 ) {
			return true;
		} else {
			return false;
		}
	}

	public boolean hasPrev() {
		int n = botStats.size();
		if ( n >= 2 ) {
			return true;
		} else {
			return false;
		}
	}

	public botStatPoint getLast() {
		if ( hasLast() ) {
			return botStats.getLast();
		} else {
			return null;
		}
	}

	public botStatPoint getPrev() {
		int n = botStats.size();
		if ( hasPrev() ) {
			return botStats.get(n-2); // last is n-1 thus prev  is n-2
		} else {
			return null;
		}
	}

	public InfoBot update(Point2D.Double pos, long tStamp) {
		botStats.add( new botStatPoint(pos, tStamp) );
		return this;
	}

	public InfoBot update(botStatPoint statPnt) {
		botStats.add(statPnt);
		targetUnlocked = false;
		return this;
	}

	public double getEnergy() {
		if ( hasLast() ) {
			return getLast().getEnergy();
		} else {
			return 0;
		}
	}

	public Point2D.Double getVelocity() {
		if ( hasLast() ) {
			return getLast().getVelocity();
		} else {
			return new Point2D.Double(0,0);
		}
	}

	public void setName(String n) {
		name = n;
	}

	public double getLastDistance(Point2D.Double p) {
		if ( hasLast() ) {
			return  getLast().getDistance(p);
		} else {
			return 1000000; // very large
		}
	}

	public double getX() {
		if ( hasLast() ) {
			return  getLast().getX();
		} else {
			return 0;
		}
	}

	public double getY() {
		if ( hasLast() ) {
			return  getLast().getY();
		} else {
			return 0;
		}
	}

	public Point2D.Double getPosition() {
		if ( hasLast() ) {
			return  getLast().getPosition();
		} else {
			return new Point2D.Double(0,0);
		}
	}

	public long getLastSeenTime() {
		if ( hasLast() ) {
			return  getLast().getTimeStamp();
		} else {
			return  -1000; // far far ago
		}
	}


	public double energyDrop() {
		if ( hasPrev() ) {
			return  getPrev().getEnergy() - getLast().getEnergy();
		} else {
			return 0;
		}
	}

	public boolean didItFireABullet(long ticTime) {
		if ( ( ticTime - this.getLastSeenTime() ) >= 1 ) {
			// our info is too old to be reliable
			return false;
		}
		boolean stat = true;
		double eDrop = energyDrop();
		if ( (eDrop < .1) || (3 < eDrop) ) {
			stat=false;
			return stat;
		} else {
			stat = true;
			return stat;
		}
	}


	public String getName() {
		return name;
	}

	public void setUnLockedStatus(boolean val) {
		targetUnlocked = val;
	}

	public String format() {
		String str;
		String strL;
		String strP;
		if ( hasPrev() )  {
			strP ="Prev: " + getPrev().format();
		} else {
			strP = "Prev: unknown";
		}
		if ( hasLast() )  {
			strL = "Last: " + getLast().format();
		} else {
			strL = "Last: unknown";
		}
		str = "Target bot name: " + getName() + "\n" + strL + "\n" + strP;
		return str;
	}

	public void drawLastKnownBotPosition(Graphics2D g) {
		if ( hasLast() ) {
			double size = 50;
			graphics.drawSquare( g, getLast().getPosition(), size );
		}
	}

	public void drawBotPath(Graphics2D g) {
		Point2D.Double pLast;
		Point2D.Double pPrev;
		ListIterator<botStatPoint> bLIter = botStats.listIterator(botStats.size());
		if (bLIter.hasPrevious()) {
			pLast = bLIter.previous().getPosition();
		} else {
			return;
		}
		while (bLIter.hasPrevious()) {
			pPrev = bLIter.previous().getPosition();
			graphics.drawLine( g, pLast, pPrev );
			pLast = pPrev;
		}

	}

	public void onPaint(Graphics2D g) {
		g.setColor(new Color(0xff, 0xff, 0x00, 0x80));
		drawBotPath(g);
		drawLastKnownBotPosition(g);
	}

}

