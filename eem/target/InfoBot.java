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

	protected HashMap<String, int[]> guessFactorsMap;
	protected HashMap<String, Integer> guessFactorsTotalCounts; // total hit count for a bot
	protected HashMap<String, Integer> guessFactorsMaxCounts; // max hit in most probable gf for a bot
	protected int numGuessFactorBins = 31;

	// FIXME: need better search algorithm
	// more than 200 amount and we start skipping turns A LOT
	// but less than 500 is very bad pif
	protected int maxDepthOfHistorySearch = 500; // how deep to look for pattern match
	protected int nRequiredMatches = 100; // number of matches to look for
	protected matchedEnds _matchedEnds = new matchedEnds();

	public InfoBot() {
		botStats = new LinkedList<botStatPoint>();
		guessFactorsMap = new HashMap<String, int[]>();
		guessFactorsTotalCounts = new HashMap<String, Integer>();
		guessFactorsMaxCounts = new HashMap<String, Integer>();
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

	public int[] getGuessFactorBins(InfoBot bot) {
		String key = bot.getName();
		if ( !guessFactorsMap.containsKey( key ) ) {
			int[] guessFactorBins = new int[numGuessFactorBins];
			guessFactorsMap.put( key, guessFactorBins );
		}
		return guessFactorsMap.get( key );
	}

	public void printGunsStats() {
		String hCstr = String.format("%4d",  this.getBulletHitCount());
		String fCstr = String.format("%-4d", this.getBulletFiredCount());
		String str = "";
		str += "Enemy gun ratio of hit/fired: " + hCstr + "/" + fCstr;
		str += " = " + logger.shortFormatDouble( 100.0*this.getGunHitRate() ) + "%";
		str += " | ";
	       	str += getName();
		logger.routine( str );
	}

	public void printGFstats(InfoBot anotherBot) {
		String str = guessFactorBins2string(anotherBot);
		logger.routine(" [ " + str + "]" + " by bot: " + getName() );
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
		updateEndsOfMatchedSegments();
		return this;
	}

	public InfoBot update(botStatPoint statPnt) {
		String profName = "InfoBot.update for " + getName();
		profiler.start( profName );
		botStats.add(statPnt);
		updateEndsOfMatchedSegments();
		targetUnlocked = false;
		profiler.stop( profName );
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

	public Point2D.Double getPrevTicPosition() {
		// Return position at previous known point
		// Watch out previous point may be many clicks away !
		Point2D.Double pos = getPositionAtTime( getLast().getTime() - 1 );
		if ( pos == null ) {
			// we do linear approximation to the past
			pos = (Point2D.Double) getPosition().clone(); // current position
			pos.x -= getVelocity().x;
			pos.y -= getVelocity().y;
		}
		return pos;
	}

	public Point2D.Double getPrevPosition() {
		// Return position at previous known point
		// Watch out previous point may be many clicks away !
		if ( hasPrev() ) {
			return  getPrev().getPosition();
		} else {
			return  null;
		}
	}

	public Point2D.Double getPositionAtTime(long time) {
		botStatPoint bS = getStatAtTime(time);
		if (bS == null) return null;
		return bS.getPosition();
	}

	public botStatPoint getStatAtTime(long time) {
		// return position at given time
		// the main use to find exactly previous time for 
		// enemy bullets targeting.
		// Thus we count from the end
		int N = botStats.size();
		botStatPoint bS = null;
		for ( int i=N-1; i >=0; i-- ) {
			bS = botStats.get(i);
			if ( bS.getTime() == time ) {
				return bS;
			}
			if ( bS.getTime() < time ) {
				// we started with lagest/oldest timeTic
				// and now passed the required time
				// everything else is before required time
				return null;
			}
		}
		return null; // no matched time found, but we should not be here at all
	}

	public long getLastSeenTime() {
		if ( hasLast() ) {
			return  getLast().getTime();
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

	public matchedEnds getMatchedEnds() {
		return _matchedEnds;
	}

	protected LinkedList<Integer> findPatternLength1MatchesList() {
		// build list of points which match current point
		LinkedList<Integer> indexes_to_add = new LinkedList<Integer>();

		int trackN = botStats.size();
		botStatPoint refPat = botStats.get( trackN - 1 ); // current point
		//logger.dbg("tic " + getLast().getTime() + ": reseeding patterns for " + getName());

		// let's search in backwards direction for matches from next to now
		int lastIndToCheck = trackN-2; // trackN - 1 previous to current point
		for ( int i = lastIndToCheck; i >= Math.max(0, lastIndToCheck - maxDepthOfHistorySearch); i-- ) {
			botStatPoint testPatPoint = botStats.get( i );
			if ( testPatPoint.arePointsOfPathSimilar( refPat, refPat, testPatPoint) ) {
				indexes_to_add.add( i );
			}
		}
		//logger.dbg("find " + indexes_to_add.size() + " matches");
		return indexes_to_add;
	}

	protected void updateEndsOfMatchedSegments() {
		// this maintains up to date matched to the current point segments array
		//logger.dbg("updating ends for bot " + getName() );

		int trackN = botStats.size();
		botStatPoint refPat = botStats.get( trackN - 1 );
		if ( _matchedEnds.size() >= 1 ) {
			// let's see if addition of current point would be able to prolong a segment
			LinkedList<Integer> indexes_to_remove = new LinkedList<Integer>();
			LinkedList<Integer> indexes_to_add = new LinkedList<Integer>();
			for ( int i : _matchedEnds.getEndsForPatternSizeN(1) ) {
				botStatPoint testPatPoint = botStats.get( i + 1 ); // next in future
				if ( testPatPoint.arePointsOfPathSimilar( refPat, refPat, testPatPoint) ) {
					indexes_to_add.add( i );
				} else {
					// current point is not continuation of previously matched pattern
					indexes_to_remove.add( i );
				}
			}
			// cleaning obsolete segments
			for ( int i : indexes_to_remove ) {
				_matchedEnds.removePoint( i );
			}
			if ( indexes_to_add.size() >= 1 ) {
				_matchedEnds.promoteAndInsert( indexes_to_add );
			} else {
				// the patterns have no match in future for current point
				// thus they are not valid and we need to start from scratch
				// FIXME: check that this redundant since removal of obsolete
				// should give us empty list
				// NOTE: so far checks show that this branch is redundant
				if ( _matchedEnds.size() != 0 ) {
					logger.error("ERROR: this should not happen, matches set should empty");
					logger.error( _matchedEnds.format() );
				}
				//_matchedEnds = new matchedEnds();
			}
		}
		// there will be no longer than 1 matched patterns.
		// but we need to find all new occurrence of pattern length 1
		LinkedList<Integer> new_matches =  findPatternLength1MatchesList();
		_matchedEnds.addUniqueOnlyToLowLevel( new_matches );

		//logger.dbg( _matchedEnds.format() );
	}

	public matchedEnds endsOfMatchedSegments ( long patLength,  int nReqMatches ) {
		return endsOfMatchedSegments( patLength, botStats.size()-1, nReqMatches);
	}

	public matchedEnds endsOfMatchedSegments ( long maxPatLength, int lastIndToCheck,  int nReqMatches ) {
		// goes through history of bot track 
		// and finds matched segments of length = patLength 
		// with respect to the end of the track
		long startTime = System.nanoTime();
		long endTime;
		LinkedList<Integer> newEndsList = new LinkedList<Integer>();

		// we create a list with matches for a given pattern length
		matchedEnds endsListVsPatternLength = new matchedEnds();

		long trackN = botStats.size();
		int cntMatches = 0;

		if ( maxPatLength < 1) {
			// we do not search for pattern length smaller than 1
			return endsListVsPatternLength;
		}

		int rStart = (int) (trackN-1);
		botStatPoint   refPatStart = botStats.get(rStart);

		// lets find all possible end of segments which matches end refernce point
		// essentially we do patLength = 1 search
		int patLength = 1;
		int cntrFoundEnds = 0;
		for ( int i = ( (int)(lastIndToCheck) ); i >= Math.max(0, trackN - maxDepthOfHistorySearch-1); i-- ) {
			botStatPoint   testPatPoint = botStats.get(i);
			if ( testPatPoint.arePointsOfPathSimilar( refPatStart, refPatStart, testPatPoint) ) {
				newEndsList.add(i);
				cntrFoundEnds++;
				if ( cntrFoundEnds > nReqMatches )
					break;
			}
		}
		endTime = System.nanoTime();
		logger.profiler("For pattern length " + patLength + " find # matches " + newEndsList.size() + " in time " + (endTime - startTime) + " ns" );

		LinkedList<Integer> prevEndsList;
		prevEndsList = newEndsList;
		while ( (newEndsList.size() >=1) && (patLength <= maxPatLength) ) {
			patLength++;
			startTime = System.nanoTime();
			prevEndsList = newEndsList;
			endsListVsPatternLength.add( prevEndsList );
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
			//logger.profiler("For pattern length " + patLength + " find # matches " + newEndsList.size() + " in time " + (endTime - startTime) + " ns" );
		}
		patLength--;
		//logger.dbg("maximum pattern length = " + patLength + " find # matches " + prevEndsList.size());
		//logger.dbg( endsListVsPatternLength.format() );
		return endsListVsPatternLength;
	}

	public LinkedList<Point2D.Double> possiblePositionsAfterTime ( long afterTime,  long maxPatLength, int nRequiredMatches ) {
		// finds list of possible position via play forward afterTime
		// for etalon path with length = patLength
		// must return empty list if nothing is found


		LinkedList<Point2D.Double> posList = new LinkedList<Point2D.Double>();
		int trackN = botStats.size();
		int lastIndToCheck = (int) (trackN - afterTime - 1);

		matchedEnds endsIndexes = endsOfMatchedSegments( maxPatLength, lastIndToCheck, nRequiredMatches );
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
			if ( p == null )
				continue;
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
			return null;
		}
		// are points taken within the same round?
		if ( ( botStats.get( matchPredictionInd ).getTime() - templatePoint.getTime() ) != playTime ) {
			// FIXME: looks like some times I have missed turns even in 1on1
			// FIXME: go check radar code
			//logger.dbg("playTime = " + playTime);
			//logger.dbg("play forward start and end are in different rounds or missed turns!");
			//for ( int i=templatePointIndex; i<= (templatePointIndex+playTime); i++) {
				//logger.dbg(botStats.get( i ).format());
			//}

			return null;
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

	public LinkedList<Point2D.Double> playForwardList( matchedEnds startIndexesList, long playTime, botStatPoint refPoint ) {
		// play forward playTime steps starting from startIndexes 
		// and apply predicted displacement to  reference end point eEnd
		// must return empty list if nothing is found
		LinkedList<Point2D.Double> posList = new LinkedList<Point2D.Double>();
		LinkedList<Integer> startIndexes = startIndexesList.flatten();
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

		// playing forward each match
		// FIXME some indexes will be run multiple times
		// try to do it more efficient
		for ( int i=0; i < nMatches; i++ ) {
			//go over all possible segment of length lets find after time prediciton
			int lastIndOfMatchedSeg = startIndexes.get(i); // end of matched segment
			Point2D.Double p = playForward( lastIndOfMatchedSeg, playTime, refPoint );
			if (p == null)
				continue;
			if ( math.isBotOutOfBorders( p ) )
				continue;
			posList.add( p );
		}

		return posList;
	}

	public long guessFactor2itsBin( double gf, double numBins) {
		return Math.round( (gf+1)/2*(numBins-1) );
	}
	
	public int getGuessFactorCount( InfoBot anotherBot, double gf ) {
		int i = (int)guessFactor2itsBin( gf, numGuessFactorBins );
		String key = anotherBot.getName();
		if ( !guessFactorsMap.containsKey( key ) ) {
			return 0;
		}
		int[] gfBins = guessFactorsMap.get( key );
		return gfBins[i];
	}

	public double getGuessFactorProb( InfoBot anotherBot, double gf ) {
		String key = anotherBot.getName();
		if ( !guessFactorsTotalCounts.containsKey( key ) ) {
			return 0;
		}
		int cnt = getGuessFactorCount( anotherBot, gf);
		int totCnt = guessFactorsTotalCounts.get( key );
		return (double) cnt/totCnt;
	}

	public double getGuessFactorNormProb( InfoBot anotherBot, double gf ) {
		String key = anotherBot.getName();
		if ( !guessFactorsMaxCounts.containsKey( key ) ) {
			return 0;
		}
		int cnt = getGuessFactorCount( anotherBot, gf);
		int maxCnt = guessFactorsMaxCounts.get( key );
		return (double) cnt/maxCnt;
	}

	public void updateHitGuessFactor( InfoBot anotherBot, double gf ) {
		int i = (int)guessFactor2itsBin( gf, numGuessFactorBins );
		String key = anotherBot.getName();
		if ( !guessFactorsMap.containsKey( key ) ) {
			int[] guessFactorBins = new int[numGuessFactorBins];
			guessFactorsMap.put( key, guessFactorBins );
		}
		if ( !guessFactorsTotalCounts.containsKey( key ) ) {
			guessFactorsTotalCounts.put( key, 0);
		}
		if ( !guessFactorsMaxCounts.containsKey( key ) ) {
			guessFactorsMaxCounts.put( key, 0);
		}
		int[] gfBins = guessFactorsMap.get( key );
		gfBins[i] = gfBins[i] + 1;
		int cnt = guessFactorsTotalCounts.get( key );
		cnt++;
		guessFactorsTotalCounts.put( key, cnt);
		int maxCnt = guessFactorsMaxCounts.get(key);
		if ( gfBins[i] > maxCnt ) {
			guessFactorsMaxCounts.put(key, gfBins[i]);
		}
	}

	public String guessFactorBins2string4botName(String botName) {
		if ( !guessFactorsMap.containsKey( botName ) ) {
			int[] guessFactorBins = new int[numGuessFactorBins];
			guessFactorsMap.put( botName, guessFactorBins );
		}
		int[] gfBins = guessFactorsMap.get( botName );
		String gfStr = "";
		for (int i=0; i < numGuessFactorBins; i++ ) {
			gfStr += " " + gfBins[i] + " ";
		}
		return gfStr;
	}

	public String guessFactorBins2string(InfoBot anotherBot) {
		String botName = anotherBot.getName();
		return guessFactorBins2string4botName(botName);
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
		//logger.dbg("bot name = " + this.getName() );
		//logger.dbg("bot stat = " + bsLast.format() );
		while (bLIter.hasPrevious()) {
			bsPrev = bLIter.previous();
			if ( bsLast.getTime() <= bsPrev.getTime() ) 
				return; // we see previous round point
			graphics.drawLine( g, bsLast.getPosition(), bsPrev.getPosition() );
			//logger.dbg("bot stat = " + bsPrev.format() );
			bsLast = bsPrev;
		}

	}

	public void onPaint(Graphics2D g) {
		g.setColor(new Color(0xff, 0xff, 0x00, 0x80));
		drawBotPath(g);
		drawLastKnownBotPosition(g);
	}

}

