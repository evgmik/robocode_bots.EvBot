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
	
	public void updBulletFiredCount(firedBullet b) {
		if ( b.isItVirtual() ) {
			bulletVirtFiredCount ++;
		} else {
			// we update both counts for real and virtual
			bulletVirtFiredCount ++;
			bulletRealFiredCount ++;
		}
	}

	public void updBulletHitCount(firedBullet b) {
		if ( b.isItVirtual() ) {
			bulletVirtHitCount ++;
		} else {
			// we update both counts for real and virtual
			bulletVirtHitCount ++;
			bulletRealHitCount ++;
		}
	}

	public int getBulletVirtFiredCount() {
		return this.bulletVirtFiredCount;
	}

	public int getBulletVirtHitCount() {
		return this.bulletVirtHitCount;
	}

	public double getGunVirtHitRate() {
		return math.eventRate( bulletVirtHitCount, bulletVirtFiredCount );
	}

	public double getGunVirtPerformance() {
		return math.perfRate( bulletVirtHitCount, bulletVirtFiredCount );
	}

	public String header() {
		String str = "";
		str+= String.format( " |%21s", "Virt gun hit rate");
		str+= String.format( " |%21s", "Real gun hit rate");
		return str;
	}

	public String format() {
		String str = "";
		str += format( bulletVirtHitCount, bulletVirtFiredCount);
		str += format( bulletRealHitCount, bulletRealFiredCount);
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
