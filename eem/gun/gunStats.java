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
	
	public void updBulletFiredCount() {
			bulletFiredCount ++;
	}

	public void updBulletHitCount() {
			bulletHitCount ++;
	}

	public int getBulletFiredCount() {
		return this.bulletFiredCount;
	}

	public int getBulletHitCount() {
		return this.bulletHitCount;
	}

	public double getGunHitRate() {
		return math.eventRate( bulletHitCount, bulletFiredCount );
	}

	public double getGunPerformance() {
		return math.perfRate( bulletHitCount, bulletFiredCount );
	}

	public String header(String gunType) {
		String str = "";
		str+= String.format( " |%21s", gunType + " gun hit rate");
		return str;
	}

	public String format() {
		String str = "";
		str += format( bulletHitCount, bulletFiredCount);
		return str;
	}

	public String format( int hC, int fC ) {
		double hR = math.eventRate( hC, fC );
		// string formatting
		String hRstr = logger.shortFormatDouble( 100.0*hR ) + "%";
		hRstr = String.format("%8s", hRstr);
		String hCstr = String.format("%4d", hC);
		String fCstr = String.format("%-4d", fC);
		String strOut = "";
		strOut += " | ";
		String tmpStr = hCstr + "/" + fCstr + " = " + hRstr;
		strOut += String.format( "%16s", tmpStr );
		return strOut;
	}
}
