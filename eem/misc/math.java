// -*- java -*-
package eem.misc;

import java.awt.geom.Point2D;
import robocode.util.*;

public class math {
	logger l;
	private int dbg_level = l.dbg_noise; // noise

	public static double quadraticSolverMinPosRoot(double a, double b, double c) {
		// we are solving for time in ballistic calculation
		// and interested only in positive solutions
		// hopefully determinant is always >= 0 since we solve real problems
		double d = Math.sqrt(b*b - 4*a*c);
		double x1= (-b + d)/(2*a);
		double x2= (-b - d)/(2*a);

		double root=Math.min(x1,x2);
		if (root < 0) {
			// if min gave as wrong root max should do better
			root=Math.max(x1,x2);
		}

		return root;
	}

	public static double angle2pt( Point2D.Double from_pnt, Point2D.Double to_pnt) {
		double angle;
		double dx, dy;
		// angle from one point to another
		dx=to_pnt.x - from_pnt.y;
		dy=to_pnt.y - from_pnt.y;

		angle = Math.atan2(dy,dx);
		angle = cortesian2game_angles(angle*180/Math.PI);
		return angle;
	}

	public static double cortesian2game_angles(double angle) {
		angle=90-angle;
		return angle;
	}

	public static double shortest_arc( double angle ) {
		//dbg(dbg_noise, "angle received = " + angle);
		angle = angle % 360;
		if ( angle > 180 ) {
			angle = -(360 - angle);
		}
		if ( angle < -180 ) {
			angle = 360+angle;
		}
		//dbg(dbg_noise, "angle return = " + angle);
		return angle;
	}

	public static int sign( double n) {
		if (n==0) 
			return 0;
		if (n > 0 )
			return 1;
		else
			return -1;
	}

}
