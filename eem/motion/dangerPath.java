// -*- java -*-

package eem.motion;

import eem.misc.*;
import eem.motion.dangerPoint;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.*;
import java.util.Collections;


public class dangerPath implements Comparable<dangerPath> {
	private LinkedList<dangerPoint> path = new LinkedList<dangerPoint>();
	private double dangerLevel = 0;

	public dangerPath(){};

	public void addPoint(dangerPoint dP) {
		path.add( dP );
		dangerLevel += dP.getDanger();
	}

	public double getDanger() {
		return dangerLevel;
	}

	public dangerPoint removeFirst() {
		dangerPoint dP = path.removeFirst();
		dangerLevel -= dP.getDanger();
		return dP;
	}

	public int compare(dangerPath p1, dangerPath p2) {
		double dL1 = p1.getDanger();
		double dL2 = p2.getDanger();
		if ( dL1 == dL2 ) return 0;
		if ( dL1 >  dL2 ) return 1;
		return -1;
	}

	public int compareTo( dangerPath p2) {
		return compare( this, p2);
	}

	public void onPaint(Graphics2D g) {
		ListIterator<dangerPoint> iter = path.listIterator();
		dangerPoint  dP;
		while (iter.hasNext()) {
			dP = iter.next();
			dP.onPaint(g);
		}
	}
}

