// -*- java -*-

package eem.gun;

import java.awt.geom.Point2D;
import robocode.*;
import eem.misc.*;

public class misc  {

	public static Point2D.Double linear_predictor( double bSpeed, Point2D.Double tgt_pos, Point2D.Double vTvec, Point2D.Double myBot_pos) {
		double tFX, tFY; // target future position
		double Tx, Ty, vT,  dx, dy, dist;
		AdvancedRobot bot = new AdvancedRobot(); // just to get battlefield parameters
		double timeToHit;
		double a, b, c;

		// radius vector to target
		dx = tgt_pos.x-myBot_pos.x;
		dy = tgt_pos.y-myBot_pos.y;
		dist = Math.sqrt(dx*dx + dy*dy);

		// rough estimate
		// use it for better estimate of possible target future velocity
		timeToHit = dist/bSpeed;

		vT=vTvec.distance(0,0);

		// back of envelope calculations
		// for the case of linear target motion with no acceleration
		// lead to quadratic equation for time of flight to target hit
		a = vT*vT - bSpeed*bSpeed;
		b = 2*( dx*vTvec.x + dy*vTvec.y);
		c = dist*dist;

		timeToHit = math.quadraticSolverMinPosRoot( a, b, c);
		tFX = (int) ( tgt_pos.x + vTvec.x*timeToHit );
		tFY = (int) ( tgt_pos.y + vTvec.y*timeToHit );

		return new Point2D.Double( tFX, tFY );
	}

	public static double minReqBulEnergyToKillTarget(double target_energy) {
		double tinyBity = 0.1;
		target_energy = target_energy + tinyBity;
		// Bullet_damage = 4 * bullet_power + 2 * max(bullet_power - 1 , 0) see wiki
		double bPower = target_energy/4;
	       	if ( bPower > 1) {
			// Bullet_damage = 4 * bullet_power + 2 * (bullet_power - 1)
			bPower = (target_energy +2) / 6;
		}
		return bPower;
	}
}