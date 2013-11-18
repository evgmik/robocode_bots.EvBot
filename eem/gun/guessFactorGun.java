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

public class guessFactorGun extends baseGun {
	protected double guessFactor;

	public guessFactorGun() {
		gunName = "guessFactor";
		guessFactor = 0;
		gunColor = new Color(0xff, 0x88, 0xff, 0x80);
	}

	public guessFactorGun(EvBot bot) {
		this();
		myBot = bot;
		calcGunSettings();
	}

	public Point2D.Double calcTargetFuturePosition( Point2D.Double firingPosition, double firePower, InfoBot tgt) {
		if ( !gunHasTargetPoint ) {
			guessFactor = chooseGuessFactor( tgt );
			//logger.dbg("Gun guess factor = " + guessFactor );
			targetFuturePosition = (Point2D.Double) tgt.getPosition().clone();
			gunHasTargetPoint = true;
		}
		double dist = firingPosition.distance( tgt.getPosition() );
		double dx = dist;
		double dy = dist;
		double angle2enemyBot = math.angle2pt( firingPosition, tgt.getPosition() );
		double angle = angle2enemyBot + guessFactor * math.calculateMEA( bulletSpeed( firePower ) );
		angle *= Math.PI/180;
		targetFuturePosition.x = firingPosition.x + dist*Math.sin( angle );
		targetFuturePosition.y = firingPosition.y + dist*Math.cos( angle );
		return targetFuturePosition;
	}

	private double chooseGuessFactor( InfoBot bot ) {
		int[] guessFactorBins = bot.getGuessFactorBins();
		int numBins = guessFactorBins.length;
		double[] guessFactorWeighted = new double[ numBins ];
		//logger.dbg( bot.getName() + ":gf\t" +  bot.guessFactorBins2string() );
		double binsSum = 0;
		for (int i=0; i < numBins; i++ ) {
			binsSum += guessFactorBins[i];
		}
		if ( binsSum == 0 ) {
			// empty statistics
			return 0; // head on guess factor
		}
		// normalize
		for (int i=0; i < numBins; i++ ) {
			guessFactorWeighted[i] = guessFactorBins[i]/binsSum;
		}
		// pick bin # according its probability

		double gf=0;
		double rnd=Math.random();
		double accumWeight = 0;
		double gfBin = 0;
		for (int i=0; i < numBins; i++ ) {
			accumWeight += guessFactorWeighted[i];
			if ( rnd <= accumWeight ) {
				gfBin = i;
				break;
			}
		}
		gf = 2*gfBin/(numBins-1) - 1;
		return gf;
	}
}	
