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


public class dangerMapMotion extends basicMotion {
	Point2D.Double DestinationPoint = new Point2D.Double(0,0);

	int dMapSizeX = 20;
	int dMapSizeY = 20;
	Point2D.Double dMapCellSize;
	double dMap[][];
	double kT = 1; // characteristic temperature for Metropolis algorithm

	double safe_distance_from_wall;
	double safe_distance_from_bot;
	double safe_distance_from_bullet;

	double dangerLevelWall = 50;
	double dangerLevelEnemyBot = 100;
	double dangerLevelBullet = 50;
	
	public void initTic() {
		rebuildDangerMap();
	}

	public dangerMapMotion(EvBot bot) {
		myBot = bot;
		DestinationPoint = (Point2D.Double) myBot.myCoord.clone();
		dMapCellSize= new Point2D.Double(myBot.BattleField.x/dMapSizeX, myBot.BattleField.y/dMapSizeY);
		dMap = new double[dMapSizeX][dMapSizeY];

		safe_distance_from_wall = myBot.robotHalfSize + 2;
		safe_distance_from_bot =  12*myBot.robotHalfSize + 2;
		safe_distance_from_bullet =  myBot.robotHalfSize + 2;
		kT = .1;
	}

	public void resetDangerMap() {
		dMap = new double[dMapSizeX][dMapSizeY];
	}
	
	public double dist2wall( Point2D.Double p ) {
		double d = p.x; // left wall distance
		double dTest;

		dTest = myBot.BattleField.x - p.x; // right wall distance
		if (dTest < d) {
			d = dTest;
		}
		dTest = myBot.BattleField.y - p.y; // top wall distance
		if (dTest < d) {
			d = dTest;
		}
		dTest =  p.y; // bottom wall distance
		if (dTest < d) {
			d = dTest;
		}
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
		double dist = dist2wall(p);
		danger = math.gaussian( dist, dangerLevelWall, safe_distance_from_wall );
		return danger;
	}

	public double pointDangerFromAllBots( Point2D.Double p ) {
		double danger = 0;
		double dist;
		Point2D.Double bPos;
		if ( myBot._trgt.haveTarget ) {
			bPos = myBot._trgt.getPosition();
			dist = p.distance(bPos);
			danger = math.gaussian( dist, dangerLevelEnemyBot, safe_distance_from_bot );
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
				danger = math.gaussian( dist, dangerLevelBullet, safe_distance_from_bullet );
			}
		}
		return danger;
	}

	public double pointDangerFromAllBullets( Point2D.Double p ) {
		double danger = 0;
		bulletsManager  bm = myBot._bmanager;
		for ( firedBullet b : bm.bullets ) {
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
	
	public void choseNewDestinationPoint() {
		double cDanger;
		int[] grid = new int[2];  
		int[] ngrid = new int[2];  
		// possible moves and its offsets
		// \  |  /
		// - j,k -  there is also stay still
		// /  |  \
		int offsets_index_min = 0;
		int offsets_index_max = 8;
		int offsets[][] = new int[offsets_index_max+1][2];
		offsets[0][0] = -1;  offsets[0][1] = 1;
		offsets[1][0] =  0;  offsets[1][1] = 1;
		offsets[2][0] =  1;  offsets[2][1] = 1;
		offsets[3][0] = -1;  offsets[3][1] = 0;
		offsets[4][0] =  1;  offsets[4][1] = 0;
		offsets[5][0] = -1;  offsets[5][1] =-1;
		offsets[6][0] =  0;  offsets[6][1] =-1;
		offsets[7][0] =  1;  offsets[7][1] =-1;
		offsets[8][0] =  0;  offsets[8][1] = 0;

		logger.noise("Destination point " + DestinationPoint);
		logger.noise("My coordinates " + myBot.myCoord);
		double dist2dest = myBot.myCoord.distance(DestinationPoint);
		logger.noise("Distance to destination point = " + dist2dest);
		double largestCellSize = Math.max ( dMapCellSize.x, dMapCellSize.y);

		//if (dist2dest >= largestCellSize/4) {
			// still moving to the preset position
			//return;
		//}
		
		// current coordinates danger for debugging
		//grid = point2grid(myBot.myCoord);
		//cDanger = grid2dangerLevel(grid); // danger of the current cell
		//logger.noise("Current  grid x = " + grid[0] + ", y = " + grid[1] + "; danger level = " + cDanger);

		// current destination danger for referencing
		grid = point2grid(DestinationPoint);
		Point2D.Double oDestinationPoint = (Point2D.Double) DestinationPoint.clone();
		double oDanger = grid2dangerLevel(grid); // danger of the current cell
		cDanger = oDanger;
		logger.noise("Old destination point grid x = " + grid[0] + ", y = " + grid[1] + "; danger level = " + oDanger);

		// if we close to target search for new before complete stop
		if (myBot.myCoord.distance(DestinationPoint) < Math.min( dMapCellSize.x, dMapCellSize.y) ) {
			oDanger = 1000; // very high
		}
		grid = point2grid(myBot.myCoord);


		int iRand = offsets_index_min + (int)(Math.random() * ((offsets_index_max - offsets_index_min) + 1));
		int i = iRand;

		double nDanger;
		boolean validCellIndex = true;
		double dEDanger; // change of danger which we treat as energy in Metropolis algorithm
		double prob;
		// lets find the safest cell starting from random offset
		do {
			validCellIndex = true;
			// iterate over possible offsets from current bot position
			ngrid[0]  = grid[0] + offsets[i][0];
			ngrid[1]  = grid[1] + offsets[i][1];

			// check if new grid indeses are within limits
			if ( (ngrid[0] < 0) || (ngrid[0] >= dMapSizeX) ) validCellIndex = false;
			if ( (ngrid[1] < 0) || (ngrid[1] >= dMapSizeY) ) validCellIndex = false;

			if ( validCellIndex) {
				nDanger = grid2dangerLevel(ngrid); // danger in new cell
				logger.noise("ngrid x = " + ngrid[0] + ", y = " + ngrid[1] + "; danger level = " + nDanger);

				// Metropolis algorith choice
				// otherwise new poit locks itself in a shallo min
				// which often lead to linear motion of a bot
				dEDanger= nDanger - cDanger;
				prob = Math.random();
				if ( (dEDanger < 0) || ( prob < Math.exp(-dEDanger/kT) ) ) {
					DestinationPoint = cellCenter( ngrid[0], ngrid[1] );
					cDanger = nDanger;
					logger.noise("New destination suggestion point grid x = " + ngrid[0] + ", y = " + ngrid[1] + "; danger level = " + cDanger);
				}
			}
			i = i + 1;
			i = i % (offsets_index_max + 1);
		} while  (i != iRand);
		double dangerThreshold = 10;
		if ( oDanger < (cDanger + dangerThreshold) ) {
			// keep the old destination point
			DestinationPoint = oDestinationPoint;
		}
		grid = point2grid(DestinationPoint);
		logger.noise("Final estination point grid x = " + grid[0] + ", y = " + grid[1] + "; danger level = " + cDanger);
	}

	public void makeMove() {
		choseNewDestinationPoint();
		moveToPoint( DestinationPoint );
	}

	Color dangerLevel2mapColor(double dLevel) {
		int opacity = (int) dLevel; // 0 - 255 but good values below 100
		int opacityTreshold = 100;

		if (opacity > opacityTreshold) opacity = opacityTreshold;
		if (opacity < 0 ) opacity = 0;

		return new Color(0xff, 0x00, 0x00, opacity);
		
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
		drawMotionDestination(g);
		drawDangerMap(g);
	}

}
