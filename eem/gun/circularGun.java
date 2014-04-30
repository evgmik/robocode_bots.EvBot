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

	protected Point2D.Double calcTargetFuturePosition( Point2D.Double firingPosition, double firePower, InfoBot tgt) {
		Point2D.Double p = findTargetHitPositionWithCircularPredictor( firingPosition, firePower, tgt);
		return p;
	}

	public Point2D.Double positionAtFutureTime( InfoBot bot, double time) {
		Point2D.Double posFut  = new Point2D.Double(0,0);
		Point2D.Double vTvecLast, vTvecPrev;
		double phi = 0;
		botStatPoint bStatLast;
		botStatPoint bStatPrev;

		bStatLast = bot.getLast();
		bStatPrev = bot.getPrev();

		vTvecLast = bStatLast.getVelocity();
		if ( bStatPrev == null ) {
			phi = 0;
		} else {
			vTvecPrev = bStatPrev.getVelocity();
			double phiLast = Math.atan2( vTvecLast.y, vTvecLast.x);
			double phiPrev = Math.atan2( vTvecPrev.y, vTvecPrev.x);
			double dt =  bStatLast.getTime() - bStatPrev.getTime();
			phi = (phiLast - phiPrev)/dt;
		}
		// rotation coefficients
		double cosPhi = Math.cos(phi);
		double sinPhi = Math.sin(phi);

		// estimated current target position
		double dT = time - bot.getLastSeenTime(); 

		double vx = vTvecLast.x;
		double vy = vTvecLast.y;
		double vxNew, vyNew;
		posFut.x = bot.getX();
		posFut.y = bot.getY();

		for ( int t = 0; t < dT ; t++) {
			vxNew =  vx * cosPhi - vy * sinPhi;
			vyNew =  vx * sinPhi + vy * cosPhi;
			vx = vxNew;
			vy = vyNew;
			posFut.x = posFut.x + vx;
			posFut.y = posFut.y + vy;
			if ( myBot._motion.shortestDist2wall( posFut ) < (myBot.robotHalfSize-1) ) {
				// bot hit wall and cannot move anymore
				posFut.x = posFut.x - vx;
				posFut.y = posFut.y - vy;
				break;
			}
		}
		return posFut;
	}

	public Point2D.Double  findTargetHitPositionWithCircularPredictor( Point2D.Double firingPosition, double firePower, InfoBot tgt) {
		Point2D.Double posFut  = new Point2D.Double(0,0);
		double bSpeed = physics.bulletSpeed( firePower );
		int maxIterNum = 10;


		logger.noise("Bullet speed " + bSpeed );

		posFut = positionAtFutureTime( tgt, myBot.getTime() );
		logger.noise("Estimated target position " + posFut.x +", " + posFut.y);

		double dist = posFut.distance( firingPosition );
		double dTnew = dist/bSpeed;
		double dT = 0;
		int cnt = 0;
		while ( (Math.abs(dT -dTnew) > 1) && (cnt < maxIterNum) ) {
			dT = dTnew;
			posFut = positionAtFutureTime( tgt, myBot.getTime() + dT);
			dist = posFut.distance( firingPosition );
			dTnew = dist/bSpeed;
			cnt++;
		}

		logger.noise("Predicted target position " + posFut.x +", " + posFut.y);
		
		return posFut;
	}
}	
