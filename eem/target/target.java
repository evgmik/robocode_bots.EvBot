// -*- java -*-

package eem.target;

import java.awt.geom.Point2D;
import eem.target.botStatPoint;

public class target {
	private int nonexisting_coord = -10000;
	private long far_ago  = -10000;
	private botStatPoint statLast;
	private botStatPoint statPrev;
	private String name = "";

	public target() {
		statPrev = new botStatPoint( new Point2D.Double( nonexisting_coord, nonexisting_coord), far_ago);
		statLast = new botStatPoint( new Point2D.Double( nonexisting_coord, nonexisting_coord), far_ago);
	}

	public target update(Point2D.Double pos, long tStamp) {
		statPrev = statLast;
		statLast = new botStatPoint(pos, tStamp);
		return this;
	}

	public target update(botStatPoint statPnt) {
		statPrev = statLast;
		statLast = statPnt;
		return this;
	}


	public void setName(String n) {
		name = n;
	}

	public double getLastDistance(Point2D.Double p) {
		return  statLast.getDistance(p);
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

}

