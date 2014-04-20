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
	int maxPatLength = 10; // maximum lenght of the template/pattern to search
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

	public Point2D.Double calcTargetFuturePosition( Point2D.Double firingPosition, double firePower, InfoBot tgt) {
		refPoint = tgt.botStats.getLast();
		Point2D.Double p = new Point2D.Double(0,0);
		LinkedList<Point2D.Double> posList;
		double bSpeed = bulletSpeed ( calcFirePower() );
		p = tgt.getPosition();

		double dist = p.distance(myBot.myCoord);
		int afterTime = (int) (dist/bSpeed);
		playTime = afterTime; // just in case if we cannot find anything
		afterTime += 20; // FIXME: account for bullet flight time in a proper way

		templateEndsList = tgt.endsOfMatchedSegments( maxPatLength, tgt.botStats.size()-1-afterTime,  nRequiredMatches);
		logger.dbg( templateEndsList.format() );
		// FIXME: do some weighting based on number of matches
		LinkedList<Integer> templateEnds = templateEndsList.flatten();

		//logger.dbg("number of found matching patterns ends= " + templateEnds.size() );
		if ( templateEnds.size() == 0 ) {
			//logger.dbg( "pifGun has no points to work with, suggesting to use another gun" );
			//for now we will use head on gun approach
			return (Point2D.Double) refPoint.getPosition().clone();
		}

		//Let's remove ends which give out of bound solutions
		LinkedList<Integer> templateEndsCleaned = new LinkedList<Integer>();
		for ( int i=0; i < templateEnds.size(); i++ ) {
			// for each templateEnds point find a pif trace
			LinkedList<Point2D.Double> trace = tgt.playForwardTrace( (int)( templateEnds.get(i) ), (long) ( afterTime ), refPoint );
			// now let's check validity of the trace
			if ( trace == null )
				continue;
			if ( trace.size() == 0 )
				continue;
			// do not draw traces with end point outside of BattleField
			p = trace.getLast();
			if ( math.isBotOutOfBorders( p ) )
				continue;
			templateEndsCleaned.add( templateEnds.get(i) );
		}
		templateEnds = templateEndsCleaned;

		// chose randomly a templateEnd to use as pif target
		int N = templateEnds.size();
		if (N == 0 ) {
			//logger.dbg( "pifGun has no points to work with, suggesting to use another gun" );
			//for now we will use head on gun approach
			return (Point2D.Double) refPoint.getPosition().clone();
		}

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
		return (Point2D.Double) p.clone();
	}

	public void drawPossiblePlayForwardTracks(Graphics2D g) {
		// Remember that onPaint happens on new tic but with old data.
		// Except apparently getTime out
		target tgt = myBot._trgt;
		Point2D.Double p = tgt.getPosition();
		double bSpeed = bulletSpeed ( calcFirePower() );
		//double dist = p.distance(myBot.myCoord);
		//int playTime = (int) (dist/bSpeed);
		double Rp = 1; // track point size


		//templateEnds = tgt.endsOfMatchedSegments( maxPatLength, tgt.botStats.size()-1-(playTime+1),  nRequiredMatches);
		//logger.dbg("number of matching ends to plot = " + templateEnds.size() );
		//
		LinkedList<Integer> templateEnds = templateEndsList.getEndsForPatternSizeN(1);

		for ( Integer i : templateEnds ) {
			// draw matching ends
			graphics.drawSquare( g, tgt.botStats.get(i).getPosition(), 4);
			//logger.dbg("end point = " + tgt.botStats.get(i).getPosition() );
		}

		for ( int i=0; i < templateEnds.size(); i++ ) {
			//LinkedList<Point2D.Double> trace = tgt.playForwardTrace( (int)( templateEnds.get(i) ), (long) (playTime + 5 ) );
			LinkedList<Point2D.Double> trace = tgt.playForwardTrace( (int)( templateEnds.get(i) ), (long) ( playTime ), refPoint );
			if ( trace == null )
				continue;
			if ( trace.size() == 0 )
				continue;
			// do not draw traces with end point outside of BattleField
			if ( math.isBotOutOfBorders( trace.getLast() ) )
				continue;
			Point2D.Double pTr = new Point2D.Double(0,0);
			for ( Point2D.Double pT : trace ) {
				double disp = 5;
				double rx = 0*disp*Math.random();
				double ry = 0*disp*Math.random();
				pTr.x = pT.x + rx;
				pTr.y = pT.y + ry;
				graphics.drawCircle( g, pTr, Rp);
				//logger.dbg("Trace " + i + " point = " + pT );
			}
			// last point is wide
			//logger.dbg("predicted point = " + pTr );
			graphics.drawCircle( g, pTr, 4*Rp);
			
		}

	}

	public void onPaint(Graphics2D g) {
		super.onPaint( g );
		drawPossiblePlayForwardTracks( g );
	}

}	
