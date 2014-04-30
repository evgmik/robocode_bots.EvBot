// -*- java -*-

package eem.gun;

import eem.EvBot;
import eem.target.*;
import eem.misc.*;
import eem.gun.misc;
import java.awt.Color;
import java.awt.geom.Point2D;
import robocode.util.*;

public class linearGun extends baseGun {
	public linearGun() {
		gunName = "linear";
		gunColor = new Color(0xff, 0x00, 0x00, 0x80);
	}

	public linearGun(EvBot bot) {
		this();
		myBot = bot;
		calcGunSettings();
	}

	protected Point2D.Double calcTargetFuturePosition( Point2D.Double firingPosition, double firePower, InfoBot tgt) {
		Point2D.Double p = findTargetHitPositionWithLinearPredictor( firingPosition, firePower, tgt);
		return p;
	}


	public Point2D.Double  findTargetHitPositionWithLinearPredictor( Point2D.Double firingPosition, double firePower, InfoBot tgt) {
		Point2D.Double vTvec;
		Point2D.Double tF;
		double Tx, Ty, vT,  dx, dy, dist;
		double tFX, tFY; // target future position
		double sin_vT, cos_vT;
		double timeToHit;
		double a, b, c;
		double bSpeed=bulletSpeed( firePower );


		logger.noise("Bullet speed " + bSpeed );

		vTvec = tgt.getVelocity();
		logger.noise("Target velocity " + vTvec.x +", " + vTvec.y);


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
		logger.noise("Round time = " + myBot.getTime());
		logger.noise("target time = " + tgt.getLastSeenTime());
		Tx = tgt.getX() + vTvec.x*(myBot.getTime()-tgt.getLastSeenTime());
		Ty = tgt.getY() + vTvec.y*(myBot.getTime()-tgt.getLastSeenTime());
		logger.noise("Target estimated current position Tx = " + Tx + " Ty = " + Ty);

		tF=misc.linear_predictor( bSpeed, new Point2D.Double(Tx, Ty), 
				vTvec,  firingPosition);
		// tF = math.putWithinBorders(tF, myBot.BattleField);
		logger.noise("Predicted target position " + tF.x +", " + tF.y);
		tF = futureTargetWithinPhysicalLimitsBasedOnVelocity( tF, vTvec );

		logger.noise("boxed target position " + tF.x +", " + tF.y);
		
		return tF;
	}
}	
