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
	long refLength  = 1; // template trace length
	int nRequiredMatches = 1000; // number of matches to look for
	int maxPatLength = 10; // huge number
	int playTime =1;

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
		Point2D.Double p = new Point2D.Double(0,0);
		LinkedList<Point2D.Double> posList;
		double bSpeed = bulletSpeed ( calcFirePower() );
		p = tgt.getPosition();

		double dist = p.distance(myBot.myCoord);
		int afterTime = (int) (dist/bSpeed);
		int oldAfterTime;
		int iterCnt = 1;
		//logger.dbg("---- gun calc started");
		do {
			//logger.dbg("required after time = " + afterTime );
			oldAfterTime = afterTime;
			//logger.dbg("iteration = " + iterCnt );
			//logger.dbg("after time = " + afterTime );
			posList = findLongestMatch( afterTime, tgt );
			if ( posList.size() < 1 ) {
				p = tgt.getPosition();
			} else {
				p = chosePointFromDistribution(posList);
			}
			dist = p.distance(myBot.myCoord);
			afterTime = (int) (dist/bSpeed);
			iterCnt++;
		} while ( ( Math.abs( oldAfterTime -afterTime ) > 1 ) && (iterCnt < 5) ) ;
		playTime = afterTime;
		//logger.dbg("Final Match list size = " + posList.size() );
		//logger.dbg("Final required play time = " + playTime );
		//logger.dbg("--- gun calc ended " );
		//logger.dbg("point to aim = " + p );
		return p;
	}

	public void drawPossiblePlayForwardTracks(Graphics2D g) {
		target tgt = myBot._trgt;
		Point2D.Double p = tgt.getPosition();
		double bSpeed = bulletSpeed ( calcFirePower() );
		//double dist = p.distance(myBot.myCoord);
		//int playTime = (int) (dist/bSpeed);
		double Rp = 1; // track point size


		LinkedList<Integer> templateEnds = tgt.endsOfMatchedSegments( maxPatLength, tgt.botStats.size()-1-playTime,  nRequiredMatches);
		logger.dbg("# of ends to plot = " + templateEnds.size() );
		for ( Integer i : templateEnds ) {
			//logger.dbg("end point = " + tgt.botStats.get(i).getPosition() );
		}

		for ( int i=0; i < templateEnds.size(); i++ ) {
			LinkedList<Point2D.Double> trace = tgt.playForwardTrace( (int)( templateEnds.get(i) ), (long) playTime );
			// do not draw traces with end point outside of BattleField
			if ( math.isBotOutOfBorders( trace.getLast() ) )
				continue;
			Point2D.Double pTr = new Point2D.Double(0,0);
			for ( Point2D.Double pT : trace ) {
				double disp = 5;
				double rx = disp*Math.random();
				double ry = disp*Math.random();
				pTr.x = pT.x + rx;
				pTr.y = pT.y + ry;
				graphics.drawCircle( g, pTr, Rp);
			}
			// last point is wide
			graphics.drawCircle( g, pTr, 4*Rp);
			
		}

	}

	public void onPaint(Graphics2D g) {
		super.onPaint( g );
		drawPossiblePlayForwardTracks( g );
	}

}	
