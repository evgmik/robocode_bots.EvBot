// -*- java -*-
//
package eem.motion;

import eem.misc.*;
import eem.motion.dangerPoint;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;


public class dangerPathPoint extends dangerPoint {
	private double accelDir = 0; // how to accelerate to get here
	
	public dangerPathPoint( Point2D.Double dP, double dangerLevel, double accelDir) {
		super( dP, dangerLevel );
		this.accelDir = accelDir;
	}

	public double getAccelDir() {
		return accelDir;
	}
}
