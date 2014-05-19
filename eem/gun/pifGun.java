// -*- java -*-

package eem.gun;

import eem.EvBot;
import eem.target.*;
import eem.misc.*;
import eem.gun.misc;
import java.awt.Color;
import java.awt.geom.Point2D;
import robocode.util.*;
import java.util.*;
import java.awt.Graphics2D;

// play it forward (PIF) gun
public class pifGun extends baseGun {
	int nRequiredMatches = 100; // number of matches to look for
	int maxPatLength = 4; // maximum length of the template/pattern to search
	int playTime =1;
	matchedEnds templateEndsList = null;
	int templateEndIndex = 0;
	botStatPoint refPoint = null;

	public pifGun() {
		gunName = "pif";
		gunColor = new Color(0x00, 0xff, 0xff, 0x80);
	}

	public pifGun(EvBot bot) {
		this();
		myBot = bot;
		gunName = "pifGun" + maxPatLength;
		calcGunSettings();
	}

	public pifGun(EvBot bot, int maxPatLength) {
		this(bot);
		this.maxPatLength = maxPatLength;
		gunName = "pifGun" + maxPatLength;
		calcGunSettings();
	}

	public Point2D.Double chosePointFromDistribution(  LinkedList<Point2D.Double> pointsList ) {
		int N = pointsList.size();
		int ni = (int)( Math.random() * N );
		return pointsList.get(ni);
	}

	public LinkedList<Point2D.Double> findLongestMatch(long afterTime,  InfoBot tgt ) {
		LinkedList<Point2D.Double> posList = tgt.possiblePositionsAfterTime(afterTime, maxPatLength, nRequiredMatches);
		return posList;
	}

	public boolean isItGoodTrace( LinkedList<Point2D.Double> trace ) {
		// now let's check validity of the trace
		if ( trace == null )
			return false;
		if ( trace.size() == 0 )
			return false;
		// do not draw traces with end point outside of BattleField
		Point2D.Double p = trace.getLast();
		if ( math.isBotOutOfBorders( p ) )
			return false;
		return true;
	}

	protected Point2D.Double calcTargetFuturePosition( Point2D.Double firingPosition, double firePower, InfoBot tgt, long fireDelay) {
		// TODO handle fire delay properly
		long startTimeCalc = System.nanoTime();
		long startTime, endTime;
		refPoint = tgt.botStats.getLast();
		Point2D.Double p = new Point2D.Double(0,0);
		LinkedList<Point2D.Double> posList;
		double bSpeed = physics.bulletSpeed ( calcFirePower() );
		p = tgt.getPosition();

		double dist = p.distance(myBot.myCoord);
		int afterTime = (int) (dist/bSpeed);
		playTime = afterTime; // just in case if we cannot find anything
		afterTime += 20; // FIXME: account for bullet flight time in a proper way

		startTime = System.nanoTime();
		//templateEndsList = tgt.endsOfMatchedSegments( maxPatLength, tgt.botStats.size()-1-afterTime,  nRequiredMatches);
		templateEndsList = tgt.getMatchedEnds();
		//logger.dbg( templateEndsList.format() );
		endTime = System.nanoTime();
		logger.profiler("tic " + myBot.getTime() + ": For target " + tgt.getName() + " Find patterns with depth " + templateEndsList.size() + " in time " + (endTime - startTime) + " ns" );
		if ( templateEndsList.size() >= 1 ) {
			logger.profiler(" Pattern length 1 has " + templateEndsList.getFirst().size() + " matches and total matches size " + templateEndsList.totalMatches() );
		}
		if ( templateEndsList.size() < maxPatLength ) {
			//not enough matches to use this gun
			//logger.dbg("find only " + templateEndsList.size() + " out of required " + maxPatLength + " matches");
			return null;
		}
		//logger.dbg( templateEndsList.format() );
		// FIXME: do some weighting based on number of matches
		LinkedList<Integer> templateEnds = templateEndsList.flatten();

		//logger.dbg("number of found matching patterns ends= " + templateEnds.size() );
		if ( templateEnds.size() == 0 ) {
			//logger.dbg( "pifGun has no points to work with, suggesting to use another gun" );
			return null;
		}

		//Let's remove ends which give out of bound solutions
		startTime = System.nanoTime();
		LinkedList<Integer> templateEndsCleaned = new LinkedList<Integer>();
		LinkedList<Integer> endsToRemove = new LinkedList<Integer>();
		// first we find which ends are bad and schedule them to remove
		// for each templateEnds point find a pif trace
		// FIXME no need to go over all of them if we are using only
		// the longest matched ones at the highest depth level
		if ( false ) { 
			// FIXME cleaning leads to a lot of skipping so I disable it for now
			// FIXME alternatively, it decreases probablility of head on targetring
			// FIXME nope, tried just to use head on and it drops APS,
			// FIXME so I am convinced it is skipped turns.
		for ( Integer i : templateEndsList.getEndsForPatternSizeN(1) ) {
			LinkedList<Point2D.Double> trace = tgt.playForwardTrace( (int)( i ), (long) ( afterTime ), refPoint );
			if ( !isItGoodTrace( trace ) ) {
				endsToRemove.add( i) ;
			}

		}
		}
		// now removing bad ends
		for ( Integer i : endsToRemove ) {
			templateEndsList.removePoint( i );
		}
		//templateEnds = templateEndsList.flatten();
		endTime = System.nanoTime();
		logger.profiler("cleaning all possible ends future position took " + (endTime - startTime) + " ns" );

		if (templateEndsList.totalMatches() == 0 ) {
			//logger.dbg( "pifGun has no points to work with, suggesting to use another gun" );
			return null;
		}

		// chose randomly a templateEnd to use as pif target
		// with highest matched pattern length
		templateEnds = templateEndsList.getEndsForPatternSizeN( templateEndsList.size() );
		int N = templateEnds.size();
		templateEndIndex = (int)( Math.random() * N );

		int oldAfterTime;
		int iterCnt = 1;
		//logger.dbg("---- gun calc started");
		do {
			//logger.dbg("required after time = " + afterTime );
			oldAfterTime = afterTime;
			//logger.dbg("iteration = " + iterCnt + " for afterTime " + afterTime );
			LinkedList<Point2D.Double> trace = tgt.playForwardTrace( (int)( templateEnds.get(templateEndIndex) ), (long) ( afterTime ), refPoint );
			if ( trace == null )
				break;
			if ( trace.size() == 0 )
				break;
			p = trace.getLast();

			dist = p.distance(myBot.myCoord);
			afterTime = (int) (dist/bSpeed);
			iterCnt++;
		} while ( ( Math.abs( oldAfterTime -afterTime ) > 1 ) && (iterCnt < 5) ) ;
		playTime = oldAfterTime;
		long endTimeCalc = System.nanoTime();
		logger.profiler("pifGun calculation time " + (endTimeCalc - startTimeCalc) + " ns" );
		return (Point2D.Double) p.clone();
	}

	public void drawPossiblePlayForwardTracks(Graphics2D g) {
		// Remember that onPaint happens on new tic but with old data.
		// Except apparently getTime out
		target tgt = myBot._trgt;
		Point2D.Double p = tgt.getPosition();
		double bSpeed = physics.bulletSpeed ( calcFirePower() );
		double Rp = 1; // track point size


		LinkedList<Integer> templateEnds = templateEndsList.getEndsForPatternSizeN( templateEndsList.size() ); // plot ends for longest matched patterns
		if ( templateEnds == null ) return;
		//logger.dbg( templateEndsList.format() );
		//logger.dbg("number of matching ends to plot = " + templateEnds.size() + " for list " + templateEnds );
		for ( Integer i : templateEnds ) {
			// draw matching ends
			//graphics.drawSquare( g, tgt.botStats.get(i).getPosition(), 4);
			//logger.dbg("end point = " + tgt.botStats.get(i).getPosition() );

			LinkedList<Point2D.Double> trace = tgt.playForwardTrace( (int)( i ), (long) ( playTime ), refPoint );
			if ( !isItGoodTrace( trace ) )
				continue;
			Point2D.Double pTr = new Point2D.Double(0,0);
			for ( Point2D.Double pT : trace ) {
				double disp = 5;
				double rx = 0*disp*Math.random();
				double ry = 0*disp*Math.random();
				pTr.x = pT.x + rx;
				pTr.y = pT.y + ry;
				graphics.drawCircle( g, pTr, Rp);
			}
			// draw expected targer at hit time
			//logger.dbg("predicted point = " + pTr );
			graphics.drawCircle( g, pTr, 4*Rp);
			double R = physics.robotHalfSize;
			graphics.drawSquare(g, pTr, 2*R+1);
			graphics.drawSquare(g, pTr, 2*R+2);
			//graphics.drawSquare(g, pTr, R);
			
		}

	}

	public void onPaint(Graphics2D g) {
		super.onPaint( g );
		drawPossiblePlayForwardTracks( g );
	}

}	
