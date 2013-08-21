// -*- java -*-

package eem.gun;

import eem.EvBot;
import eem.target.*;
import eem.misc.*;
import java.awt.Color;
import java.awt.geom.Point2D;
import robocode.util.*;

public class linearGun extends baseGun {

	public linearGun(EvBot bot) {
		myBot = bot;
		gunName = "linear";
		gunColor = new Color(0xff, 0x00, 0x00, 0x80);
	}

	public void setTargetFuturePosition(target tgt) {
		targetFuturePosition = findTargetHitPositionWithLinearPredictor( firePower, tgt);
	}

	public Point2D.Double  findTargetHitPositionWithLinearPredictor(double firePower, target tgt) {
		Point2D.Double vTvec;
		double Tx, Ty, vT,  dx, dy, dist;
		double tFX, tFY; // target future position
		double sin_vT, cos_vT;
		double timeToHit;
		double a, b, c;
		double bSpeed=myBot.bulletSpeed( firePower );

		myBot.dbg(myBot.dbg_noise, "Bullet speed " + bSpeed );

		vTvec = tgt.getVelocity();
		myBot.dbg(myBot.dbg_noise, "Target velocity " + vTvec.x +", " + vTvec.y);

		vT = Math.sqrt(vTvec.x*vTvec.x + vTvec.y*vTvec.y);
		if ( !Utils.isNear(vT, 0) ) {
			cos_vT=vTvec.x/vT;
			sin_vT=vTvec.y/vT;
		} else {
			// target is stationary
			// assign small speed in random direction
			vT=0.00001;
			double rand_angle=2*Math.PI*Math.random();
			cos_vT=Math.cos(rand_angle);
			sin_vT=Math.sin(rand_angle);
		}

		// estimated current target position
		Tx = tgt.getX() + vTvec.x*(myBot.getTime()-tgt.getLastSeenTime());
		Ty = tgt.getY() + vTvec.y*(myBot.getTime()-tgt.getLastSeenTime());
		myBot.dbg(myBot.dbg_noise, "Target estimated current position Tx = " + Tx + " Ty = " + Ty);

		// radius vector to target
		dx = Tx-myBot.myCoord.x;
		dy = Ty-myBot.myCoord.y;
		dist = Math.sqrt(dx*dx + dy*dy);
		myBot.dbg(myBot.dbg_noise, "Distance to target " + dist );

		// rough estimate
		// use it for better estimate of possible target future velocity
		timeToHit = dist/bSpeed;
		myBot.dbg(myBot.dbg_noise, "Rough estimate time to hit = " + timeToHit);


		// back of envelope calculations
		// for the case of linear target motion with no acceleration
		// lead to quadratic equation for time of flight to target hit
		a = vT*vT - bSpeed*bSpeed;
		b = 2*( dx*vTvec.x + dy*vTvec.y);
		c = dist*dist;

		timeToHit = math.quadraticSolverMinPosRoot( a, b, c);
		myBot.dbg(myBot.dbg_noise, "Precise estimate time to hit = " + timeToHit);
		tFX = (int) ( Tx + vTvec.x*timeToHit );
		tFY = (int) ( Ty + vTvec.y*timeToHit );
		myBot.dbg(myBot.dbg_noise, "Predicted target position " + tFX +", " + tFY);

		// check that future target position within the battle field
		tFX = (int)Math.max(tFX, 0);
		tFX = (int)Math.min(tFX, myBot.BattleField.x );
		tFY = (int)Math.max(tFY, 0);
		tFY = (int)Math.min(tFY, myBot.BattleField.y );

		myBot.dbg(myBot.dbg_noise, "Predicted and boxed target position " + tFX +", " + tFY);
		
		return new Point2D.Double( tFX, tFY );
	}
}	
