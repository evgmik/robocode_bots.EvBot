// -*- java -*-
package eem.misc;


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

}
