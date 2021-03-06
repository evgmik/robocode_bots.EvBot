// -*- java -*-

package eem.gun;

import eem.EvBot;
import eem.target.*;
import eem.misc.*;
import eem.gun.misc;
import java.awt.Color;
import java.awt.Graphics2D;
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

	protected Point2D.Double calcTargetFuturePosition( Point2D.Double firingPosition, double firePower, InfoBot tgt, long fireDelay) {
		Point2D.Double p = findTargetHitPositionWithLinearPredictor( firingPosition, firePower, tgt, fireDelay);
		//logger.dbg("gun calculated target position at hit time " + p);
		return p;
	}


	public Point2D.Double  findTargetHitPositionWithLinearPredictor( Point2D.Double firingPosition, double firePower, InfoBot tgt, long fireDelay) {
		Point2D.Double targetAtFiringTimePos = new Point2D.Double(0,0);
		Point2D.Double vTvec;
		Point2D.Double tF;
		double vT;
		double sin_vT, cos_vT;
		double bSpeed=physics.bulletSpeed( firePower );


		// estimated current target position
		logger.noise("Round time = " + myBot.getTime());
		logger.noise("target time = " + tgt.getLastSeenTime());

		long firingTime = myBot.getTime() + fireDelay;
		botStatPoint bS = tgt.getStatAtTime( firingTime );
		if ( bS == null ) {
			// required point does not exist
			// we will trace back from last known point
			vTvec = (Point2D.Double) tgt.getVelocity().clone();
			targetAtFiringTimePos = (Point2D.Double) predictBotPositionAtTime( tgt, firingTime ).clone();
		} else {
			targetAtFiringTimePos = (Point2D.Double) bS.getPosition().clone();
			vTvec = (Point2D.Double) bS.getVelocity().clone();
		}

		tF=misc.linear_predictor( bSpeed, targetAtFiringTimePos,
				vTvec,  firingPosition);

		// tF = math.putWithinBorders(tF, myBot.BattleField);
		//logger.noise("Predicted target position " + tF.x +", " + tF.y);
		tF = futureTargetWithinPhysicalLimitsBasedOnVelocity( tF, vTvec );

		//logger.noise("boxed target position " + tF.x +", " + tF.y);
		return (Point2D.Double) tF.clone();
	}

	public void onPaint(Graphics2D g) {
		super.onPaint(g);
	}
}	

