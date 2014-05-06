// -*- java -*-

package eem.gun;

import eem.EvBot;
import eem.target.*;
import eem.misc.*;
import eem.motion.*;
import eem.gun.misc;
import java.awt.Color;
import java.awt.geom.Point2D;
import robocode.util.*;

public class circularGun extends baseGun {
	public circularGun() {
		gunName = "circular";
		gunColor = new Color(0x00, 0x00, 0xff, 0x80);
	}

	public circularGun(EvBot bot) {
		this();
		myBot = bot;
		calcGunSettings();
	}

	protected Point2D.Double calcTargetFuturePosition( Point2D.Double firingPosition, double firePower, InfoBot tgt, long fireDelay) {
		Point2D.Double p = findTargetHitPositionWithCircularPredictor( firingPosition, firePower, tgt, fireDelay );
		return p;
	}

	public Point2D.Double  findTargetHitPositionWithCircularPredictor( Point2D.Double firingPosition, double firePower, InfoBot tgt, long fireDelay) {
		Point2D.Double posFut  = new Point2D.Double(0,0);
		double bSpeed = physics.bulletSpeed( firePower );
		int maxIterNum = 10;
		long firingTime = myBot.getTime() + fireDelay;


		logger.noise("Bullet speed " + bSpeed );

		posFut = predictBotPositionAtTimeCircular( tgt, firingTime );
		logger.noise("Estimated target position at firing time " + posFut.x +", " + posFut.y);

		double dist = posFut.distance( firingPosition );
		long dTnew = (long) ( dist/bSpeed );
		long dT = 0;
		int cnt = 0;
		while ( (Math.abs(dT -dTnew) > 1) && (cnt < maxIterNum) ) {
			dT = dTnew;
			posFut = predictBotPositionAtTimeCircular( tgt, firingTime + dT);
			dist = posFut.distance( firingPosition );
			dTnew = (long) ( dist/bSpeed );
			cnt++;
		}

		logger.noise("Predicted target position " + posFut.x +", " + posFut.y);
		
		return posFut;
	}
}	
