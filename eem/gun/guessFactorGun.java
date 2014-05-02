// -*- java -*-

package eem.gun;

import eem.EvBot;
import eem.target.*;
import eem.misc.*;
import eem.motion.*;
import eem.gun.misc;
import java.awt.Color;
import java.awt.Graphics2D;
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
		super(bot);
		gunName = "guessFactor";
		guessFactor = 0;
		gunColor = new Color(0xff, 0x88, 0xff, 0x80);
		calcGunSettings();
	}

	public Point2D.Double calcTargetFuturePosition( InfoBot firedBot, double firePower, InfoBot tgt) {
		firingBot = firedBot;
		return super.calcTargetFuturePosition( firedBot, firePower, tgt);
	}

	protected Point2D.Double calcTargetFuturePosition( Point2D.Double firingPosition, double firePower, InfoBot tgt, long fireDelay) {
		if (firingBot == null ) { 
			//FIXME: this should not happen if I make it right
			logger.dbg("FIXME: firingBot is not set. This should not happen if I make it right");
			firingBot = myBot._tracker;
		}
		if ( !gunHasTargetPoint ) {
			//logger.dbg("Gun guess factor = " + guessFactor );
			targetFuturePosition = (Point2D.Double) tgt.getPosition().clone();
			gunHasTargetPoint = true;
		}
		guessFactor = chooseGuessFactor( tgt );
		//logger.dbg("Firing bot: " + firingBot.getName() );
		//logger.dbg("Chosen GF = " + guessFactor );
		double dist = firingPosition.distance( tgt.getPosition() );
		double dx = dist;
		double dy = dist;
		double angle2enemyBot = math.angle2pt( firingPosition, tgt.getPosition() );
		double angle = angle2enemyBot + guessFactor * math.calculateMEA( physics.bulletSpeed( firePower ) );
		angle *= Math.PI/180;
		targetFuturePosition.x = firingPosition.x + dist*Math.sin( angle );
		targetFuturePosition.y = firingPosition.y + dist*Math.cos( angle );
		return targetFuturePosition;
	}

	private double pickGFprobabilisticly(InfoBot bot) {
		int[] guessFactorBins = firingBot.getGuessFactorBins( bot );
		int numBins = guessFactorBins.length;
		double[] guessFactorWeighted = new double[ numBins ];
		//logger.dbg("firing bot: " + firingBot.getName() + " gf\t[" +  firingBot.guessFactorBins2string(bot) + "]");
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

		double rnd=Math.random();
		double accumWeight = 0;
		int gfBin = 0;
		for (int i=0; i < numBins; i++ ) {
			accumWeight += guessFactorWeighted[i];
			if ( rnd <= accumWeight ) {
				gfBin = i;
				break;
			}
		}
		return math.bin2gf( gfBin, numBins );
	}

	private double pickMostProbableGF(InfoBot bot) {
		int[] guessFactorBins = firingBot.getGuessFactorBins( bot );
		int numBins = guessFactorBins.length;
		double[] guessFactorWeighted = new double[ numBins ];
		//logger.dbg("firing bot: " + firingBot.getName() + " gf\t[" +  firingBot.guessFactorBins2string(bot) + "]");
		double binsSum = 0;
		int indMax = 0;
		int maxCnt =0;
		for (int i=0; i < numBins; i++ ) {
			binsSum += guessFactorBins[i];
			if ( guessFactorBins[i] > maxCnt ) {
				maxCnt = guessFactorBins[i];
				indMax = i;
			}
		}
		if ( binsSum == 0 ) {
			// empty statistics
			return 0; // head on guess factor
		}
		
		return math.bin2gf( indMax, numBins );
	}

	private double chooseGuessFactor( InfoBot bot ) {
		//return pickGFprobabilisticly( bot );
		return pickMostProbableGF( bot );
	}

	public void onPaint(Graphics2D g) {
		super.onPaint(g);

		// draw weighted guess factor directions
		int[] guessFactorBins = firingBot.getGuessFactorBins(myBot._trgt);
		int N = guessFactorBins.length;
		double wMax=0;
		for( int i=0; i<N; i++ ) {
			if ( guessFactorBins[i] > wMax )
				wMax = guessFactorBins[i];
		}

		double angle2enemyBot = math.angle2pt( myBot.myCoord, myBot._trgt.getPosition() );
		double dist = myBot.myCoord.distance( myBot._trgt.getPosition() );
		double bSpeed = physics.bulletSpeed( firePoverVsDistance( dist ) );
		double MEA = math.calculateMEA( bSpeed );

		Point2D.Double gfPnt = new Point2D.Double(0,0);
		double radius = Math.max(dist/3, dist - 3.5*myBot.robotHalfSize );
		for( int i=0; i<N; i++ ) {
			double angle = angle2enemyBot + MEA*math.bin2gf(i, N);
			gfPnt.x = myBot.myCoord.x + radius*Math.sin(angle/180*Math.PI);
			gfPnt.y = myBot.myCoord.y + radius*Math.cos(angle/180*Math.PI);
			double circRad = 1+ 4*guessFactorBins[i]/Math.max(wMax,1);
			graphics.drawCircle(g, gfPnt, circRad);
		}
	}
}	
