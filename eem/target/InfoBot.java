// -*- java -*-

package eem.target;

import eem.EvBot;
import eem.target.*;
import eem.misc.*;
import java.awt.geom.Point2D;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.*;
import java.lang.Integer;
import robocode.*;

public class InfoBot {
	protected String name = "";
	public LinkedList<botStatPoint> botStats;
	public boolean targetUnlocked = true; 

	protected int bulletHitCount = 0;
	protected int bulletFiredCount = 0;

	protected int numGuessFactorBins = 31;
	protected int[] guessFactorBins = new int[numGuessFactorBins];

	// FIXME: need better search algorithm
	// more than this amount and we start skipping turns
	protected int maxDepthOfHistorySearch = 500; 

	public InfoBot() {
		botStats = new LinkedList<botStatPoint>();
		targetUnlocked = true;
	}

	public InfoBot(String botName) {
		this();
		setName(botName);
	}

	public int getBulletFiredCount() {
		return this.bulletFiredCount;
	}

	public int getBulletHitCount() {
		return this.bulletHitCount;
	}

	protected void incBulletFiredCount() {
		this.bulletFiredCount++;
	}

	public void incBulletHitCount() {
		this.bulletHitCount++;
	}

	public double getGunHitRate() {
		return (this.getBulletHitCount() ) / (this.getBulletFiredCount() + 1.0);
	}

	public int[] getGuessFactorBins() {
		return guessFactorBins;
	}
	public void printGunsStats() {
		String hCstr = String.format("%4d",  this.getBulletHitCount());
		String fCstr = String.format("%-4d", this.getBulletFiredCount());
		String str = "";
		str += "Enemy gun ratio of hit/fired: " + hCstr + "/" + fCstr;
		str += " = " + logger.shortFormatDouble( this.getGunHitRate() );
		str += " | ";
	       	str += getName();
		logger.routine( str );
	}

	public void initTic(long ticTime) {
		// updating UnLocked status
		if ( ( ticTime - this.getLastSeenTime() ) > 2) 
			this.setUnLockedStatus(true);
		else
			this.setUnLockedStatus(false);

		// firing status
		if ( didItFireABullet(ticTime) ) {
			this.incBulletFiredCount();
		}
	}

	public boolean hasLast() {
		int n = botStats.size();
		if ( n >= 1 ) {
			return true;
		} else {
			return false;
		}
	}

	public boolean hasPrev() {
		int n = botStats.size();
		if ( n >= 2 ) {
			return true;
		} else {
			return false;
		}
	}

	public botStatPoint getLast() {
		if ( hasLast() ) {
			return botStats.getLast();
		} else {
			return null;
		}
	}

	public botStatPoint getPrev() {
		int n = botStats.size();
		if ( hasPrev() ) {
			return botStats.get(n-2); // last is n-1 thus prev  is n-2
		} else {
			return null;
		}
	}

	public InfoBot update(Point2D.Double pos, long tStamp) {
		botStats.add( new botStatPoint(pos, tStamp) );
		return this;
	}

	public InfoBot update(botStatPoint statPnt) {
		botStats.add(statPnt);
		targetUnlocked = false;
		return this;
	}

	public double getEnergy() {
		if ( hasLast() ) {
			return getLast().getEnergy();
		} else {
			return 0;
		}
	}

	public Point2D.Double getVelocity() {
		if ( hasLast() ) {
			return getLast().getVelocity();
		} else {
			return new Point2D.Double(0,0);
		}
	}

	public void setName(String n) {
		name = n;
	}

	public double getLastDistance(Point2D.Double p) {
		if ( hasLast() ) {
			return  getLast().getDistance(p);
		} else {
			return 1000000; // very large
		}
	}

	public double getX() {
		if ( hasLast() ) {
			return  getLast().getX();
		} else {
			return 0;
		}
	}

	public double getY() {
		if ( hasLast() ) {
			return  getLast().getY();
		} else {
			return 0;
		}
	}

	public Point2D.Double getPosition() {
		if ( hasLast() ) {
			return  getLast().getPosition();
		} else {
			return new Point2D.Double(0,0);
		}
	}

	public long getLastSeenTime() {
		if ( hasLast() ) {
			return  getLast().getTimeStamp();
		} else {
			return  -1000; // far far ago
		}
	}


	public double energyDrop() {
		if ( hasPrev() ) {
			return  getPrev().getEnergy() - getLast().getEnergy();
		} else {
			return 0;
		}
	}

	public boolean didItFireABullet(long ticTime) {
		if ( ( ticTime - this.getLastSeenTime() ) >= 1 ) {
			// our info is too old to be reliable
			return false;
		}
		boolean stat = true;
		double eDrop = energyDrop();
		if ( (eDrop < 1e-6) || (3 < eDrop) ) {
			stat=false;
			return stat;
		} else {
			stat = true;
			return stat;
		}
	}


	public String getName() {
		return name;
	}

	public void setUnLockedStatus(boolean val) {
		targetUnlocked = val;
	}

	public String format() {
		String str;
		String strL;
		String strP;
		if ( hasPrev() )  {
			strP ="Prev: " + getPrev().format();
		} else {
			strP = "Prev: unknown";
		}
		if ( hasLast() )  {
			strL = "Last: " + getLast().format();
		} else {
			strL = "Last: unknown";
		}
		int statSize = botStats.size();
		str = "Target bot name: " + getName() + "\n" + strL + "\n" + strP;
		str += "\n stats size " + statSize;
		return str;
	}

	public LinkedList<Integer> endsOfMatchedSegments ( long patLength,  int nReqMatches ) {
		return endsOfMatchedSegments( patLength, botStats.size()-1, nReqMatches);

	}
	public LinkedList<Integer> endsOfMatchedSegments ( long maxPatLength, int lastIndToCheck,  int nReqMatches ) {
		// goes through history of bot track 
		// and finds matched segments of length = patLength 
		// with respect to the end of the track
		long startTime = System.nanoTime();
		long endTime;
		LinkedList<Integer> newEndsList = new LinkedList<Integer>();

		long trackN = botStats.size();
		int cntMatches = 0;

		if ( maxPatLength < 1) {
			// we for pattern length smaller than 1
			return newEndsList;
		}

		int rStart = (int) (trackN-1);
		botStatPoint   refPatStart = botStats.get(rStart);

		// lets find all possible end of segments which matches end refernce point
		// essentially we do patLength = 1 search
		int patLength = 1;
		for ( int i = ( (int)(lastIndToCheck) ); i >= Math.max(0, trackN - maxDepthOfHistorySearch-1); i-- ) {
			botStatPoint   testPatPoint = botStats.get(i);
			if ( testPatPoint.arePointsOfPathSimilar( refPatStart, refPatStart, testPatPoint) ) {
				newEndsList.add(i);
			}
		}
		endTime = System.nanoTime();
		logger.profiler("For pattern length " + patLength + " find # matches " + newEndsList.size() + " in time " + (endTime - startTime) + " ns" );

		LinkedList<Integer> prevEndsList;
		prevEndsList = (LinkedList<Integer>) newEndsList.clone();
		while ( (newEndsList.size() >=1) && (patLength < maxPatLength) ) {
			patLength++;
			startTime = System.nanoTime();
			prevEndsList = (LinkedList<Integer>) newEndsList.clone();
			newEndsList = new LinkedList<Integer>();
			for (Integer i : prevEndsList ) {
				int testPatIndex = i - patLength + 1;
				int refPatIndex = rStart - patLength+1;
				if ( (testPatIndex < 0) || (refPatIndex < 0) ) {
					// out of bounds
					newEndsList.remove(i);
					continue;
				}
				botStatPoint  testPatStart = botStats.get(i);
				botStatPoint  testPatPoint = botStats.get(testPatIndex);
				botStatPoint  refPatPoint  = botStats.get(refPatIndex);
				if ( (testPatPoint.arePointsOfPathSimilar( refPatStart, refPatPoint, testPatStart)) ) {
					newEndsList.add(i);
				}
			}
			endTime = System.nanoTime();
			logger.profiler("For pattern length " + patLength + " find # matches " + newEndsList.size() + " in time " + (endTime - startTime) + " ns" );
		}
		patLength--;
		//logger.dbg("maximum pattern length = " + patLength);
		return prevEndsList;
	}

	public LinkedList<Point2D.Double> possiblePositionsAfterTime ( long afterTime,  long maxPatLength, int nRequiredMatches ) {
		// finds list of possible position via play forward afterTime
		// for etalon path with length = patLength
		// must return empty list if nothing is found


		LinkedList<Point2D.Double> posList = new LinkedList<Point2D.Double>();
		int trackN = botStats.size();
		int lastIndToCheck = (int) (trackN - afterTime - 1);

		LinkedList<Integer> endsIndexes = endsOfMatchedSegments( maxPatLength, lastIndToCheck, nRequiredMatches );
		//logger.dbg("Size of endsIndexes list = " + endsIndexes.size() );
		posList = playForwardList( endsIndexes, afterTime, botStats.getLast() );
		return posList;
	}

	public LinkedList<Point2D.Double> playForwardTrace ( int templatePointIndex, long playTime ) {
		return playForwardTrace( templatePointIndex, playTime, botStats.getLast() );
	}

	public LinkedList<Point2D.Double> playForwardTrace ( int templatePointIndex, long playTime, botStatPoint refPoint ) {
		LinkedList<Point2D.Double> posList = new LinkedList<Point2D.Double>();
		Point2D.Double p;
		for (long i=1; i<= playTime; i++) {
			p = playForward( templatePointIndex, i, refPoint);
			posList.add( p );
		}
		return posList;
	}


	public Point2D.Double playForward( int templatePointIndex, long playTime, botStatPoint refPoint ) {
		Point2D.Double lastPos = refPoint.getPosition();
		Point2D.Double lastVel = refPoint.getVelocity();
		double         lastSpd = refPoint.getSpeed();
		double         lastHeadingInDegrees = refPoint.getHeadingDegrees();

		Point2D.Double p;

		int trackN = botStats.size();
		botStatPoint templatePoint = botStats.get(templatePointIndex);
		double headingLastMatchedDegrees = templatePoint.getHeadingDegrees();

		Point2D.Double posLastMatched = templatePoint.getPosition();
		int matchPredictionInd = (int) (templatePointIndex + playTime);
		// check that predicted index within track
		if ( matchPredictionInd > (trackN - 1) ) {
			p = refPoint.getPosition();
			return p;
		}
			
		Point2D.Double posMatchedAfterTime = botStats.get( matchPredictionInd ).getPosition();
		double distToMatchedPrediciton = posLastMatched.distance(posMatchedAfterTime);
		double dx = posMatchedAfterTime.x - posLastMatched.x;
		double dy = posMatchedAfterTime.y - posLastMatched.y;
		double matchBearingInDegrees = math.cortesian2game_angles(Math.atan2(dy,dx)*180.0/Math.PI) - headingLastMatchedDegrees;

		double angDegrees = matchBearingInDegrees + lastHeadingInDegrees ;
		dx = distToMatchedPrediciton*Math.sin( angDegrees/180*Math.PI );
		dy = distToMatchedPrediciton*Math.cos( angDegrees/180*Math.PI );

		p = new Point2D.Double(lastPos.x + dx, lastPos.y + dy); 
		return p;
	}

	public LinkedList<Point2D.Double> playForwardList( LinkedList<Integer> startIndexes, long playTime, botStatPoint refPoint ) {
		// play forward playTime sterp starting from startIndexes 
		// and apply predicted displacement to  reference end point eEnd
		// must return empty list if nothing is found
		LinkedList<Point2D.Double> posList = new LinkedList<Point2D.Double>();
		int trackN = botStats.size();

		int nMatches = startIndexes.size();
		if ( nMatches == 0 ) {
			// no matches found
			return posList;
		}

		Point2D.Double lastPos = refPoint.getPosition();
		Point2D.Double lastVel = refPoint.getVelocity();
		double         lastSpd = refPoint.getSpeed();
		double         lastHeadingInDegrees = refPoint.getHeadingDegrees();

		for ( int i=0; i < nMatches; i++ ) {
			//go over all possible segment of length lets find after time prediciton
			int lastIndOfMatchedSeg = startIndexes.get(i); // end of matched segment
			Point2D.Double p = playForward( lastIndOfMatchedSeg, playTime, refPoint );
			posList.add( p );
		}

		return posList;
	}

	public long guessFactor2itsBin( double gf, double numBins) {
		return Math.round( (gf+1)/2*(numBins-1) );
	}
	
	public void updateHitGuessFactor( double gf ) {
		int i = (int)guessFactor2itsBin( gf, numGuessFactorBins );
		guessFactorBins[i] = guessFactorBins[i] + 1;
	}

	public String guessFactorBins2string() {
		String gfStr = "";
		for (int i=0; i < numGuessFactorBins; i++ ) {
			gfStr += " " + guessFactorBins[i] + " ";
		}
		return gfStr;
	}

	public void drawLastKnownBotPosition(Graphics2D g) {
		if ( hasLast() ) {
			double size = 50;
			botStatPoint  bsLast = getLast();
			graphics.drawSquare( g, bsLast.getPosition(), size );
		}
	}

	public void drawBotPath(Graphics2D g) {
		botStatPoint  bsLast;
		botStatPoint  bsPrev;
		ListIterator<botStatPoint> bLIter = botStats.listIterator(botStats.size());
		if (bLIter.hasPrevious()) {
			bsLast = bLIter.previous();
		} else {
			return;
		}
		while (bLIter.hasPrevious()) {
			bsPrev = bLIter.previous();
			if ( bsLast.getTimeStamp() <= bsPrev.getTimeStamp() ) 
				return; // we see previous round point
			graphics.drawLine( g, bsLast.getPosition(), bsPrev.getPosition() );
			bsLast = bsPrev;
		}

	}

	public void onPaint(Graphics2D g) {
		g.setColor(new Color(0xff, 0xff, 0x00, 0x80));
		drawBotPath(g);
		drawLastKnownBotPosition(g);
	}

}

