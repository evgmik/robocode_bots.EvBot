// -*- java -*-

package eem.motion;

import eem.misc.*;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

public class dangerPoint implements Comparable<dangerPoint> {
	public Point2D.Double position;
	public double dangerLevel;

	public dangerPoint() {
		position  = new Point2D.Double(0,0);
		dangerLevel = 0;
	}

	public dangerPoint( Point2D.Double p, double dL ) {
		position = (Point2D.Double) p.clone();
		dangerLevel = dL;
	}

	public int compare(dangerPoint p1, dangerPoint p2) {
		double dL1 = p1.dangerLevel;
		double dL2 = p2.dangerLevel;
		if ( dL1 == dL2 ) return 0;
		if ( dL1 >  dL2 ) return 1;
		return -1;
	}

	public int compareTo( dangerPoint p2) {
		return compare( this, p2);
	}

	public void print() {
		logger.dbg("Point [" + position.x + ", " + position.y + "]" + " has danger level = " + dangerLevel);
	}

	Color dangerLevel2mapColor(double dLevel) {
		int opacity = (int) Math.abs(dLevel/3.0); // 0 - 255 but good values below 100
		int opacityTreshold = 100;
		Color c;

		if (opacity > opacityTreshold) opacity = opacityTreshold;
		if (opacity < 0 ) opacity = 0;

		if ( dLevel >= 0 ) {
			// red
			c = new Color(0xff, 0x00, 0x00, opacity);
		} else {
			// green
			c = new Color(0x00, 0xff, 0x00, opacity);
		}
		return c;
	}

	public void onPaint(Graphics2D g) {
		Point2D.Double p;
		p = this.position;
		double dL = this.dangerLevel;
		g.setColor( dangerLevel2mapColor( dL ) );
		g.drawOval((int) p.x-5, (int) p.y-5, 10, 10);
		// put dot in the middle
		g.setColor( new Color(0x00, 0x00, 0xaa, 0xff) );
		g.drawOval((int) p.x-2, (int) p.y-2, 2, 2);
	}

}
