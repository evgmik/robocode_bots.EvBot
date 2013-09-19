// -*- java -*-
package eem.misc;

import java.awt.geom.Point2D;
import robocode.util.*;

public class math {
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
		dx=to_pnt.x - from_pnt.x;
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

	public static boolean isItOutOfBorders( Point2D.Double pnt, Point2D.Double brdr) {
		if ( pnt.x < 0 ) return true;
		if ( pnt.y < 0 ) return true;
		if ( pnt.x > brdr.x ) return true;
		if ( pnt.y > brdr.y ) return true;
		return false;
	}

	public static Point2D.Double putWithinBorders( Point2D.Double pnt, Point2D.Double brdr) {
		Point2D.Double npnt= new Point2D.Double( pnt.x, pnt.y );
		npnt.x = putWithinRange( npnt.x, 0, brdr.x);
		npnt.y = putWithinRange( npnt.y, 0, brdr.y);
		return npnt;
	}

	public static double putWithinRange( double x, double lBound, double uBound) {
		double val;
		val = x;
		if ( val < lBound ) {
			val = lBound;
		}
		if ( val > uBound ) {
			val =uBound;
		}
		return val;
	}

	public static int signNoZero( double n) {
		int val;
		val = sign(n);
		if (0 == val) {
			val = 1;
		}
		return val;
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
