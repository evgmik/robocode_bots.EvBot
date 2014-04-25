// -*- java -*-

package eem.motion;

import eem.EvBot;
import eem.target.*;
import eem.misc.*;
import eem.bullets.*;
import eem.motion.dangerPoint;
import java.util.Random;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.Arrays;
import robocode.util.*;
import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.*;
import java.util.Collections;


public class dangerMapMotion extends basicMotion {
	Point2D.Double DestinationPoint = new Point2D.Double(0,0);
	int nPointsToCheckForNewDestination = 50;
	double distToProbeDefault = 100;
	double distToProbe1on1 = 200;
	double distToProbeWhenRamming = 50;
	double distToProbeWhenAtReducedDistance = 50;
	double distToProbe = distToProbeDefault;


	int dMapSizeX = 20;
	int dMapSizeY = 20;
	Point2D.Double dMapCellSize;
	double dMap[][];
	double kT; // characteristic temperature for Metropolis algorithm

	double safe_distance_from_bot;
	double distFromWaveToFarToWorry = 400;

	double dangerLevelEnemyBot = 100;

	boolean rammingCondition = false;
	private static double reducedBotDistanceCoef = 1;

	public LinkedList<dangerPoint> dangerPoints;
	
	public void initTic() {
		
		if (  myBot.numEnemyBotsAlive == 1  ) {
			distToProbe = distToProbe1on1;
			if ( myBot._trgt.haveTarget && false ) {
				// we need to adjust distance releative to the danger distance
				// FIXME: take in account wave distance and do it
				// for not only 1on1
				Point2D.Double bPos = myBot._trgt.getPosition();
				double dist = myBot.myCoord.distance(bPos);
				// some fudge distance
				// distToProbe = 200 good when bots are at opposit sides
				// distToProbe = 100 reasonable fir half field distances
				// let's try to interpolate in between
				//distToProbe = dist/2;
				distToProbe = 50 + 150*Math.exp( (dist-400)/20 );
				distToProbe = Math.min(200, distToProbe );
				//distToProbe = Math.min(200, 50 + 150*Math.exp( (dist-400)/20) );
				// IMPORTANT: to evade rammers I need distToProbe to be about 200
				// but this seems to much for bots fighting in close distance
				//distToProbe = distToProbe1on1;
				//distToProbe = Math.min( dist, distToProbe );
				//logger.dbg( "dist = " + dist + " distToProbe = " + distToProbe );
			}
		} else {
			distToProbe = distToProbeDefault;
		}
		if ( reducedBotDistanceCoef < 1.0 ) {
			distToProbe = distToProbeWhenAtReducedDistance;
		}
		setRammingCondition();
		if ( rammingCondition ) 
			distToProbe = distToProbeWhenRamming;
		setOptimalDistanceFromBot();
	}

	public void setRammingCondition() {
		rammingCondition = calcRammingCondition();
		if (rammingCondition ) {
			logger.dbg("Ramming!");
		}
	}

	public boolean calcRammingCondition() {
		if ( myBot._trgt.haveTarget && ( myBot.numEnemyBotsAlive == 1 ) ) {
		// are we on 1 vs 1 and enemy has less than 0.1 energy
		// i.e. firing is not possible
			if (  myBot._trgt.getEnergy() < 0.1 ) {
				return true;
			}
		}
		if ( true ){
			// for now looks like ramming is bad idea even against weak bots
			return false; 
		}
		double energyAdvantage = 30;
		// are we on 1 vs 1 and more energetic
		if ( myBot._trgt.haveTarget && ( myBot.numEnemyBotsAlive == 1 ) ) {
			double energyAdvantageHysteresis;
			if ( rammingCondition ) {
				// if we already engaged keep pressing 
				energyAdvantageHysteresis = 0;
			} else {
				energyAdvantageHysteresis = energyAdvantage;
			}
			if ( myBot.getEnergy() > ( myBot._trgt.getEnergy() + energyAdvantageHysteresis ) ) {
				return true;
			}
		}
		return false;
	}

	public dangerMapMotion(EvBot bot) {
		super(bot);
		DestinationPoint = (Point2D.Double) myBot.myCoord.clone();
		dMapCellSize= new Point2D.Double(myBot.BattleField.x/dMapSizeX, myBot.BattleField.y/dMapSizeY);
		dMap = new double[dMapSizeX][dMapSizeY];

		safe_distance_from_bot =  12*myBot.robotHalfSize + 2;
		kT = 0.1;

		rammingCondition = false;
	}

	void setOptimalDistanceFromBot() {
		double hitRateTreshHold = 0.3;
		double hitRateDisbalance = ( myBot._gmanager.overallGunsHitRate() - myBot._trgt.getGunHitRate() );
		if ( myBot.numEnemyBotsAlive > 1 ) {
			reducedBotDistanceCoef = 1;
			return;
		}

		// if not ramming
		// and on 1 vs 1
		// and we miss too badly let's close in
		// at least we will make some damage
		if ( !rammingCondition &&  myBot._trgt.haveTarget && ( myBot.numEnemyBotsAlive == 1 ) ) {
			//FIXME it seems its better to do nothing
			//reducedBotDistanceCoef = myBot._gmanager.overallGunsHitRate()/(myBot._trgt.getGunHitRate()+1e-4); // try to maintain > 1 hit ration
			//reducedBotDistanceCoef = math.putWithinRange( reducedBotDistanceCoef, 0, 1.0);
		}
	}

	public void resetDangerMap() {
		dMap = new double[dMapSizeX][dMapSizeY];
	}
	
	public int[] point2grid(Point2D.Double pnt) {
		int[] grid = new int[2];  
		grid[0] = (int) Math.floor( pnt.x / dMapCellSize.x );
		grid[1] = (int) Math.floor( pnt.y / dMapCellSize.y );
		return grid;
	}
	
	public double grid2dangerLevel(int grid[]) {
		int i = grid[0];
		int j = grid[1];
		return dMap[i][j];
	}
	
	public double pointDangerFromAllBots( Point2D.Double p ) {
		double danger = 0;
		danger += pointDangerFromNonTargetBots(p);
		danger += pointDangerFromTargetBot(p);
		return danger;
	}

	public double pointDangerFromNonTargetBots( Point2D.Double p ) {
		double danger = 0;
		double dist;
		double danger_coef = 1.0; // by default bots repel us
		Point2D.Double bPos;
		long currentTime = myBot.ticTime;
		for (InfoBot bot : myBot._botsmanager.bots.values()) 
		{
			if ( bot.getLastSeenTime() > currentTime )
				continue; // we see bot status from previous round
			bPos = bot.getPosition();
			dist = p.distance(bPos);
			danger += math.gaussian( dist, danger_coef*dangerLevelEnemyBot, safe_distance_from_bot );
		}
		return danger;
	}

	public double pointDangerFromTargetBot( Point2D.Double p ) {
		double danger = 0;
		double dist;
		double danger_coef = 1.0; // by default bots repel us
		Point2D.Double bPos;
		if ( myBot._trgt.haveTarget ) {
			bPos = myBot._trgt.getPosition();
			dist = p.distance(bPos);
			if ( rammingCondition ) {
				danger_coef = -10.0; // we want to ram so it is attractive potential
				// make enemy bot place extra attractive for ramming
				danger += math.gaussian( dist, -2*dangerLevelEnemyBot,  2*myBot.robotHalfSize );
			}
			if ( reducedBotDistanceCoef < 1.0 ) {
				if (dist > reducedBotDistanceCoef * safe_distance_from_bot ) {
					danger += math.gaussian( dist, -2*dangerLevelEnemyBot,  safe_distance_from_bot );
				}
			}
			danger += math.gaussian( dist, danger_coef*dangerLevelEnemyBot, safe_distance_from_bot );
		}
		return danger;
	}

	public double pointDangerFromEnemyWavesAndItsPrecursor( Point2D.Double p ) {
		double danger = 0;
		bulletsManager  bm = myBot._bmanager;
		if ( bm == null) {
			//logger.dbg("This should not happen: bullet manager is null" );
		       	return 0;
		}
		LinkedList<wave> enemyWaves = bm.getAllEnemyWaves();
		for ( wave eW : enemyWaves ) {
			double distToWave = eW.distance( p );
			//logger.dbg(" distance to wave = " + distToWave + " for " + p);
			if (distToWave < 0 ) // the wave passed this point
				continue;
			if ( ( distToWave > distFromWaveToFarToWorry ) && ( enemyWaves.size() > 6 ) ) {
				//logger.dbg("Too many enemy waves, skipping this one");
				continue;
			}
			for ( firedBullet b : eW.getBullets() ) {
				danger += b.pointDangerFromBulletPrecursor( p, myBot.ticTime );
			}
		}
		return danger;
	}

	public double pointDanger( Point2D.Double p ) {
		double danger = 0;
		danger += pointDangerFromWalls( p, myBot._tracker.getLast().getSpeed() );
		if ( myBot.fightType().equals("1on1") || myBot.fightType().equals("melee1on1") ) {
			danger += pointDangerFromCorners( p, myBot._tracker.getLast().getSpeed() );
		}
		danger += pointDangerFromAllBots( p );
		danger += pointDangerFromEnemyWavesAndItsPrecursor( p );
		return danger;
	}

	public double cellDanger( int[] grid ) {
		Point2D.Double p = cellCenter( grid[0], grid[1] );
		return pointDanger( p );
	}

	public void rebuildDangerMap() {
		//resetDangerMap();
		int[] grid = new int[2];  
		for (int i=0; i < dMapSizeX; i++) {
			for (int j=0; j < dMapSizeY; j++) {
				grid[0] = i; grid[1] = j;
				dMap[i][j] = cellDanger( grid );
			}
		}
	}

	private void buildListOfDestinationsToTest() {
		dangerPoints = new LinkedList<dangerPoint>();
		Point2D.Double nP;
		double distRand;
		double angleRand;
		double dL;
		Point2D.Double centerPoint = (Point2D.Double) myBot.myCoord.clone();
		Point2D.Double bPos = myBot._trgt.getPosition();
		double dist2enemy = myBot.myCoord.distance(bPos);
		double dist;
		int cnt = 0;
		double probLongStep = Math.random();
		while ( cnt < nPointsToCheckForNewDestination ) {
			double rnd = Math.random();
			if ( probLongStep < 0.01 ) {
				// sometimes we build points with larger spread
				rnd += 1;
				//FIXME this would need new rnd otherwise
				// 1 < rnd < 1.01 which is boring i.e. big step is not that big
			}
			angleRand = 2*Math.PI*Math.random();
			if ( myBot.fightType().equals("1on1") && myBot._trgt.haveTarget ) {
				// we will generate points with in an ellipse with minor
				// axis no longer than max(distToProbe, dist2target)
				double distBotConstrained = dist2enemy - myBot.robotHalfSize*2;
				double distWaveConstrained = Math.abs(myBot._bmanager.getClosestToMeWaveTimeArrival())*8;
				if ( distWaveConstrained < myBot.robotHalfSize) {
					// wave already hit us too late to worry about it
					distWaveConstrained = distToProbe;
				}
				double minorRToEnemy = Math.min( distBotConstrained, distWaveConstrained);
				minorRToEnemy = Math.min( minorRToEnemy, distToProbe);
				double majorRToEnemy = Math.min( distWaveConstrained, distToProbe );

				double minorRFromEnemy = distToProbe;
				double majorRFromEnemy = majorRToEnemy;
				// when we build the point within the ellipse lets direct
				// minor axis towards the enemy
				double dxEl;
				double dyEl;
				if ( angleRand < Math.PI ) {
					// this ellipse part pointing to enemy
					// FIXME angle enemy should be concern with
					// wave start point if it is dominating decision
					dyEl = minorRToEnemy*Math.sin( angleRand );
					dxEl = majorRToEnemy*Math.cos( angleRand );
				} else {
					// we are pointing away from enemy
					// it is OK to use major radius in this direction
					dyEl = minorRFromEnemy*Math.sin( angleRand );
					dxEl = majorRFromEnemy*Math.cos( angleRand );
				}
				// now random distance within ellipse
				dxEl = rnd*dxEl;
				dyEl = rnd*dyEl;

				//now we need to rotate this ellipse so it faces enemy
				double angle2enemy =  Math.toRadians( math.angle2pt(myBot.myCoord, bPos) );
				double dx =  dxEl*Math.cos(angle2enemy) + dyEl*Math.sin(angle2enemy);
				double dy = -dxEl*Math.sin(angle2enemy) + dyEl*Math.cos(angle2enemy);
				//logger.dbg( "angle 2 enemy " + angle2enemy );
				
				nP = new Point2D.Double( 
					centerPoint.x + dx,
					centerPoint.y + dy );

			} else {
				// for other fight types search in within a circle
				// FIXME use similar to 1on1 logic
				distRand = rnd*distToProbe;
				nP = new Point2D.Double( 
					centerPoint.x + distRand*Math.sin(angleRand) ,
					centerPoint.y + distRand*Math.cos(angleRand) );
			}
			dL = pointDanger(nP);
			if ( shortestDist2wall(nP) > (myBot.robotHalfSize + 1) ) {
				dangerPoints.add(new dangerPoint( nP, dL) );
				cnt++;
			}
		}

	}

	private void sortDangerPoints() {
		Collections.sort(dangerPoints);
	}

	private dangerPoint pickDestinationCandidate() {
		sortDangerPoints();
		ListIterator<dangerPoint> iter = dangerPoints.listIterator();
		double dEnergy, prob;
		dangerPoint pOptimal = dangerPoints.getFirst();
		dangerPoint pTry;
		double dangerMin = pOptimal.dangerLevel;
		while (iter.hasNext()) {
			pTry = iter.next();
			dEnergy = pTry.dangerLevel - dangerMin;
			if ( dEnergy < 0 ) {
				// should never happen since we sorted already
				pOptimal = pTry;
			} else { 
				prob = Math.random();
				if ( prob < Math.exp ( - dEnergy / kT ) ) {
					pOptimal = pTry;
				}
			}
		}
		return pOptimal;
	}

	public void choseNewDestinationPoint() {

		buildListOfDestinationsToTest();
		sortDangerPoints();

		dangerPoint oldP = new dangerPoint ( DestinationPoint, pointDanger(DestinationPoint) );

		// if we close to target, search for new before complete stop
		double distToStop = 20; // 8+6+4+2 max stopping distance
		if (myBot.myCoord.distance(DestinationPoint) < 5*distToStop ) {
			oldP.dangerLevel += 1000; // very high to ensure new choice
		}

		//printDangerPoints();
		dangerPoint newP = pickDestinationCandidate();
		//newP.print();

		double dangerThreshold = 10;
		if ( oldP.dangerLevel > (newP.dangerLevel + dangerThreshold) ) {
			// keep the old destination point
			DestinationPoint = newP.position;
		}
	}

	public void makeMove() {
		choseNewDestinationPoint();
		moveToPoint( DestinationPoint );
	}

	Color dangerLevel2mapColor(double dLevel) {
		int opacity = (int) Math.abs(dLevel/3.0); // 0 - 255 but good values below 100
		int opacityTreshold = 100;
		Color c;

		if (opacity > opacityTreshold) opacity = opacityTreshold;
		if (opacity < 0 ) opacity = 0;

		if ( dLevel >= 0 ) {
			// red
			c = new Color(0xff, 0x00, 0x00, opacity);
		} else {
			// green
			c = new Color(0x00, 0xff, 0x00, opacity);
		}
		return c;
	}

	public  Point2D.Double cellCenter( int i, int j) {
		// position of a cell center
		return new Point2D.Double( dMapCellSize.x*(i+1./2), dMapCellSize.y*(j+1./2) );
	}

	public void drawDangerMapCell(Graphics2D g, int i, int j){
		Point2D.Double  c = cellCenter( i, j);

		g.setColor( dangerLevel2mapColor( dMap[i][j] ) );
		g.fillRect((int)(c.x - dMapCellSize.x/2), (int)(c.y - dMapCellSize.y/2), (int)(dMapCellSize.x), (int)(dMapCellSize.y) );
	}

	public void drawDangerMap(Graphics2D g) {
		rebuildDangerMap();
		for (int i=0; i < dMapSizeX; i++) {
			for (int j=0; j < dMapSizeY; j++) {
				drawDangerMapCell(g,i,j);
			}
		}
	}
	
	public void drawMotionDestination(Graphics2D g) {
		g.setColor(Color.green);
		g.drawLine((int) DestinationPoint.x, (int) DestinationPoint.y, (int)myBot.myCoord.x, (int)myBot.myCoord.y);
		g.drawOval((int) DestinationPoint.x-5, (int) DestinationPoint.y-5, 10, 10);
	}

	public void onPaint(Graphics2D g) {
		//drawDangerMap(g);
		drawMotionDestination(g);
		drawDangerPoints(g);
	}

	public void drawDangerPoints(Graphics2D g) {
		ListIterator<dangerPoint> iter = dangerPoints.listIterator();
		dangerPoint  dP;
		double dL;
		while (iter.hasNext()) {
			dP = iter.next();
			dP.onPaint(g);
		}
	}

	public void printDangerPoints() {
		ListIterator<dangerPoint> iter = dangerPoints.listIterator();
		while (iter.hasNext()) {
			iter.next().print();
		}
	}

}

