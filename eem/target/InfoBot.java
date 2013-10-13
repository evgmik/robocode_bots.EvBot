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

public class InfoBot {
	protected String name = "";
	protected LinkedList<botStatPoint> botStats;
	public boolean targetUnlocked = true; 

	protected int bulletHitCount = 0;
	protected int bulletFiredCount = 0;

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

	public void printGunsStats() {
		logger.routine("Enemy gun hit rate = " + this.getGunHitRate() );
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
		if ( (eDrop < .1) || (3 < eDrop) ) {
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
		str = "Target bot name: " + getName() + "\n" + strL + "\n" + strP;
		return str;
	}

	public LinkedList<Integer> endsOfMatchedSegments ( long patLength,  int nReqMatches ) {
		return endsOfMatchedSegments( patLength, botStats.size()-1, nReqMatches);

	}
	public LinkedList<Integer> endsOfMatchedSegments ( long patLength, int lastIndToCheck,  int nReqMatches ) {
		// goes through history of bot track 
		// and finds matched segments of length = patLength 
		// with respect to the end of the track
		LinkedList<Integer> endsOfMAtchedSegmentsIndexes = new LinkedList<Integer>();

		long trackN = botStats.size();
		int cntMatches = 0;

		if ( (lastIndToCheck + 1)  < ( patLength ) ) {
			return endsOfMAtchedSegmentsIndexes;
		}

		if ( patLength < 1) {
			// we for pattern length smaller than 1
			return endsOfMAtchedSegmentsIndexes;
		}

		int rStart = (int) (trackN-1);
		botStatPoint   refPatStart = botStats.get(rStart);

		for ( int i = ( (int)(lastIndToCheck) ); i >= (patLength -1); i-- ) {
			//go over all possible segment of length = patLength 
			int tStart = i;
			//logger.dbg("tStart = " + i);
			botStatPoint   testPatStart = botStats.get(tStart);
			boolean doesItMatchRef = true;
			for ( int k=0; k < (patLength); k++ ) {
				// step by step comparison over reference and test segments
				int tIndex = tStart - k;
				int rIndex = rStart - k;
				botStatPoint  testPatPoint = botStats.get(tIndex);
				botStatPoint  refPatPoint  = botStats.get(rIndex);
				if ( !(testPatPoint.arePointsOfPathSimilar( refPatStart, refPatPoint, testPatStart)) ) {
					doesItMatchRef = false;
					break;
				}
			}
			if (doesItMatchRef) {
				int matchedInd = (int) (tStart);
				endsOfMAtchedSegmentsIndexes.add(matchedInd);
				cntMatches++;
				if (cntMatches == nReqMatches) break; // enough is enough
			}
		}
		return endsOfMAtchedSegmentsIndexes;
	}

	public LinkedList<Point2D.Double> possiblePositionsAfterTime ( long afterTime,  long patLength, int nRequiredMatches ) {
		// finds list of possible position via play forward afterTime
		// for etalon path with length = patLength
		// must return empty list if nothing is found


		LinkedList<Point2D.Double> posList = new LinkedList<Point2D.Double>();
		int trackN = botStats.size();
		int lastIndToCheck = (int) (trackN - afterTime - 1);

		if (  trackN  < (patLength + afterTime) ) {
			// known history is to short
			return posList;
		}

		LinkedList<Integer> endsIndexes = endsOfMatchedSegments( patLength, lastIndToCheck, nRequiredMatches );
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

	public void drawLastKnownBotPosition(Graphics2D g) {
		if ( hasLast() ) {
			double size = 50;
			graphics.drawSquare( g, getLast().getPosition(), size );
		}
	}

	public void drawBotPath(Graphics2D g) {
		Point2D.Double pLast;
		Point2D.Double pPrev;
		ListIterator<botStatPoint> bLIter = botStats.listIterator(botStats.size());
		if (bLIter.hasPrevious()) {
			pLast = bLIter.previous().getPosition();
		} else {
			return;
		}
		while (bLIter.hasPrevious()) {
			pPrev = bLIter.previous().getPosition();
			graphics.drawLine( g, pLast, pPrev );
			pLast = pPrev;
		}

	}

	public void onPaint(Graphics2D g) {
		g.setColor(new Color(0xff, 0xff, 0x00, 0x80));
		drawBotPath(g);
		drawLastKnownBotPosition(g);
	}

}

