// -*- java -*-

package eem.motion;

import eem.EvBot;
import eem.target.*;
import eem.misc.*;
import eem.bullets.*;
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
	double distToProbe = 100;


	int dMapSizeX = 20;
	int dMapSizeY = 20;
	Point2D.Double dMapCellSize;
	double dMap[][];
	double kT; // characteristic temperature for Metropolis algorithm

	double safe_distance_from_wall;
	double safe_distance_from_bot;
	double safe_distance_from_bullet;

	double dangerLevelWall = 50;
	double dangerLevelEnemyBot = 100;
	double dangerLevelBullet = 100;

	boolean rammingCondition = false;
	private static double reducedBotDistanceCoef = 1;

	public LinkedList<dangerPoint> dangerPoints;
	
	public void initTic() {
		setRammingCondition();
		setOptimalDistanceFromBot();
	}

	public void setRammingCondition() {
		rammingCondition = calcRammingCondition();
		if (rammingCondition ) {
			logger.dbg("Ramming!");
		}
	}

	public boolean calcRammingCondition() {
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
		myBot = bot;
		DestinationPoint = (Point2D.Double) myBot.myCoord.clone();
		dMapCellSize= new Point2D.Double(myBot.BattleField.x/dMapSizeX, myBot.BattleField.y/dMapSizeY);
		dMap = new double[dMapSizeX][dMapSizeY];

		safe_distance_from_wall = myBot.robotHalfSize + 2;
		safe_distance_from_bot =  12*myBot.robotHalfSize + 2;
		safe_distance_from_bullet =  2*myBot.robotHalfSize + 2;
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
			//reducedBotDistanceCoef += 0.1*hitRateDisbalance; // simple negative feedback
			reducedBotDistanceCoef = myBot._gmanager.overallGunsHitRate()/(myBot._trgt.getGunHitRate()+1e-4); // try to maintain > 1 hit ration
			reducedBotDistanceCoef = math.putWithinRange( reducedBotDistanceCoef, 0, 1.0);
		}
	}

	public void resetDangerMap() {
		dMap = new double[dMapSizeX][dMapSizeY];
	}
	
	public double dist2LeftOrRightWall( Point2D.Double p ) {
		double d = p.x; // left wall distance
		double dTest;
		dTest = myBot.BattleField.x - p.x; // right wall distance
		if (dTest < d) {
			d = dTest;
		}
		return d;
	}

	public double dist2BottomOrTopWall( Point2D.Double p ) {
		double d = p.y; // bottom wall distance
		double dTest;
		dTest = myBot.BattleField.y - p.y; // top wall distance
		if (dTest < d) {
			d = dTest;
		}
		return d;
	}

	public double shortestDist2wall( Point2D.Double p ) {
		double dx = dist2LeftOrRightWall( p );
		double dy = dist2BottomOrTopWall( p );
		double d = Math.min( dist2LeftOrRightWall( p ), dist2BottomOrTopWall( p ) );
		return d;
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
	
	public double pointDangerFromWalls( Point2D.Double p ) {
		double danger = 0;
		double dx = dist2LeftOrRightWall( p );
		double dy = dist2BottomOrTopWall( p );
		if ( shortestDist2wall( p ) <= myBot.robotHalfSize ) {
			// point within physical no-go zone
			// danger must be infinite to prevent going there
			danger += 1e6; // humongous number
		}
		danger += math.gaussian( dx, dangerLevelWall, safe_distance_from_wall );
		danger += math.gaussian( dy, dangerLevelWall, safe_distance_from_wall );
		return danger;
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

	public double pointDangerFromBullet( Point2D.Double p, firedBullet b ) {
		double danger = 0;
		double dist;
		Point2D.Double bPos, bEnd;
		if ( b.isActive() && !b.isItMine ) {
			bPos = b.getPosition();
			bEnd = b.endPositionAtBorder();
			double dBx, dBy;
			double dPx, dPy;
			double dP, dB;
			// bullet path vector
			dBx = bEnd.x - bPos.x;
			dBy = bEnd.y - bPos.y;
			dB = Math.sqrt(dBx*dBx + dBy*dBy);
			if( dBx == 0 ) dBx = 1e-8;
			if( dBy == 0 ) dBy = 1e-8;
			// vector to point from bullet present location
			dPx = p.x - bPos.x;
			dPy = p.y - bPos.y;
			dP = Math.sqrt(dPx*dPx + dPy*dPy);

			// if cos between dP and dB vectors positive
			// the point is in front of bullet
			double cos_val = (dPx*dBx + dPy*dBy)/(dP*dB); // normalized scalar product
			if ( cos_val > 0 ) {
				// distance to the bullet path from point
				dist = dP*Math.sqrt(1-cos_val*cos_val);
				double distAlongBulletPath = dP*cos_val;
				// bullet is dangerous only when we are close to it
				double escapeDistance = 200; // very large
				//if ( distAlongBulletPath < escapeDistance ) {
					danger = math.gaussian( dist, dangerLevelBullet, safe_distance_from_bullet ); // tangent distance contribution
					 danger *= math.gaussian( distAlongBulletPath, 1, escapeDistance ); // distance to travel by bullet contribution
					 if ( myBot.fightType().equals( "1on1" ) ) {
						 double bDamage = 4*b.bulletEnergy() + 2 * Math.max( b.bulletEnergy() - 1 , 0 );
						 danger *= (1+myBot.totalNumOfEnemiesAtStart/Math.max( myBot.numEnemyBotsAlive, 1) *0.01*bDamage);
					 }

					if (rammingCondition) {
						// if we close to target during ramming
						// reduce bullet danger
						double dist2target = p.distance( myBot._trgt.getPosition() );
						danger *= 1-Math.exp( -Math.max(0, dist2target - 2*myBot.robotHalfSize)/50 );
					}
				//}
			}
		}
		return danger;
	}

	public double pointDangerFromAllBullets( Point2D.Double p ) {
		double danger = 0;
		bulletsManager  bm = myBot._bmanager;
		for ( firedBullet b : bm.getAllBullets() ) {
			danger += pointDangerFromBullet( p, b );
		}
		return danger;
	}

	public double pointDanger( Point2D.Double p ) {
		double danger = 0;
		danger += pointDangerFromWalls( p );
		danger += pointDangerFromAllBots( p );
		danger += pointDangerFromAllBullets( p );
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
		//centerPoint = (Point2D.Double) DestinationPoint.clone();
		double dist;
		int cnt = 0;
		double probLongStep = Math.random();
		while ( cnt < nPointsToCheckForNewDestination ) {
			distRand = distToProbe*Math.random();
			if ( probLongStep < 0.01 ) {
				// sometimes we build points with larger spread
				distRand += distToProbe;
			}
			angleRand = 2*Math.PI*Math.random();
			nP = new Point2D.Double( 
					centerPoint.x + distRand*Math.sin(angleRand) ,
					centerPoint.y + distRand*Math.cos(angleRand) );
			dL = pointDanger(nP);
			if ( !math.isItOutOfBorders( nP, myBot.BattleField ) ) {
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

		// if we close to target search for new before complete stop
		if (myBot.myCoord.distance(DestinationPoint) < Math.min( dMapCellSize.x, dMapCellSize.y) ) {
			oldP.dangerLevel = 1000; // very high to ensure new choice
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
		drawDangerMap(g);
		drawMotionDestination(g);
		drawDangerPoints(g);
	}

	public void drawDangerPoints(Graphics2D g) {
		ListIterator<dangerPoint> iter = dangerPoints.listIterator();
		Point2D.Double p;
		dangerPoint  dP;
		double dL;
		while (iter.hasNext()) {
			dP = iter.next();
			p = dP.position;
			dL = dP.dangerLevel;
			g.setColor( dangerLevel2mapColor( dL ) );
			g.drawOval((int) p.x-5, (int) p.y-5, 10, 10);
			// put dot in the middle
			g.setColor( new Color(0x00, 0x00, 0xaa, 0xff) );
			g.drawOval((int) p.x-2, (int) p.y-2, 2, 2);
		}
	}

	public void printDangerPoints() {
		ListIterator<dangerPoint> iter = dangerPoints.listIterator();
		while (iter.hasNext()) {
			iter.next().print();
		}
	}

	public class dangerPoint implements Comparable<dangerPoint> {
		public Point2D.Double position;
		public double dangerLevel;

		public dangerPoint() {
			position  = new Point2D.Double(0,0);
			dangerLevel = 0;
		}

		public dangerPoint( Point2D.Double p, double dL ) {
			position = (Point2D.Double) p.clone();
			dangerLevel = dL;
		}

		public int compare(dangerPoint p1, dangerPoint p2) {
			double dL1 = p1.dangerLevel;
			double dL2 = p2.dangerLevel;
			if ( dL1 == dL2 ) return 0;
			if ( dL1 >  dL2 ) return 1;
			return -1;
		}

		public int compareTo( dangerPoint p2) {
			return compare( this, p2);
		}

		public void print() {
			logger.dbg("Point [" + position.x + ", " + position.y + "]" + " has danger level = " + dangerLevel);
		}
	}
}

