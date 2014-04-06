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
	private double turnAngle = 0; // how to rotate to get here
	private double velocity = 0; // at this point
	private double heading = 0;  // at this point
	private long  ticTime = 0;  // at this point
	
	public dangerPathPoint( Point2D.Double dP, double dangerLevel, double turnAngle, double accelDir) {
		super( dP, dangerLevel );
		this.accelDir = accelDir;
		this.turnAngle = turnAngle;
	}

	public dangerPathPoint( Point2D.Double dP, double dangerLevel, double turnAngle, double accelDir, double velocity, double heading, long ticTime ) {
		this( dP, dangerLevel, turnAngle, accelDir );
		this.velocity = velocity;
		this.heading = heading;
		this.ticTime = ticTime;
	}

	public double getHeading() {
		return heading;
	}

	public long getTime() {
		return ticTime;
	}

	public double getVelocity() {
		return velocity;
	}

	public double getAccelDir() {
		return accelDir;
	}

	public double getTurnAngle() {
		return turnAngle;
	}
}
