// -*- java -*-

package eem.motion;

import eem.EvBot;
import eem.target.*;
import eem.misc.*;
import java.util.Random;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.Arrays;
import robocode.util.*;
import java.awt.Color;


public class dangerMapMotion extends basicMotion {
	Point2D.Double DestinationPoint = new Point2D.Double(0,0);

	int dMapSizeX = 10;
	int dMapSizeY = 10;
	Point2D.Double dMapCellSize;
	double dMap[][];

	double safe_distance_from_wall;
	double safe_distance_from_bot;

	double dangerLevelWall = 50;
	double dangerLevelEnemyBot = 50;
	
	public dangerMapMotion(EvBot bot) {
		myBot = bot;
		DestinationPoint = (Point2D.Double) myBot.myCoord.clone();
		dMapCellSize= new Point2D.Double(myBot.BattleField.x/dMapSizeX, myBot.BattleField.y/dMapSizeY);
		dMap = new double[dMapSizeX][dMapSizeY];

		safe_distance_from_wall = myBot.robotHalfSize + 2;
		safe_distance_from_bot =  myBot.robotHalfSize + 2;
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

	public void borders2DangerMap() {
		int[] grid = new int[2];  
		Point2D.Double c;
		double safe_distance = safe_distance_from_wall;
		safe_distance =  Math.max( dMapCellSize.x, safe_distance );
		safe_distance =  Math.max( dMapCellSize.y, safe_distance );
		for (int i=0; i < dMapSizeX; i++) {
			for (int j=0; j < dMapSizeY; j++) {
				c = cellCenter(i,j);
				if (dist2wall(c) <= safe_distance ) {
					grid[0] = i; grid[1] = j;
					addDangerLevelToCell( grid, dangerLevelWall);
				}
			}
		}
	}

	public int[] point2grid(Point2D.Double pnt) {
		int[] grid = new int[2];  
		grid[0] = (int) Math.floor( pnt.x / dMapCellSize.x );
		grid[1] = (int) Math.floor( pnt.y / dMapCellSize.y );

		return grid;
	}

	public void addDangerLevelToCell(int grid[], double dangerLevel ) {
		int i = grid[0];
		int j = grid[1];
		dMap[i][j] = dMap[i][j] + dangerLevel;
	}
	
	public double grid2dangerLevel(int grid[]) {
		int i = grid[0];
		int j = grid[1];
		return dMap[i][j];
	}
	
	public void otherBots2DangerMap() {
		int[] grid = new int[2];  
		double safe_distance =safe_distance_from_bot;
		safe_distance =  Math.max( dMapCellSize.x, safe_distance );
		safe_distance =  Math.max( dMapCellSize.y, safe_distance );
		Point2D.Double c;
		if ( myBot._trgt.haveTarget ) {
			for (int i=0; i < dMapSizeX; i++) {
				for (int j=0; j < dMapSizeY; j++) {
					c = cellCenter(i,j);
					if ( c.distance(myBot._trgt.getPosition()) <= safe_distance) {
						grid[0] = i; grid[1] = j;
						addDangerLevelToCell( grid, dangerLevelEnemyBot);
					}
				}
			}
		}
	}

	public void rebuildDangerMap() {
		resetDangerMap();
		borders2DangerMap();
		otherBots2DangerMap();
	}
	
	public void choseNewDestinationPoint() {
		int[] grid = new int[2];  
		int[] ngrid = new int[2];  
		// possible moves and its offsets
		// \  |  /
		// - j,kr-
		// /  | \
		int offsets[][] = new int[8][2];
		offsets[0][0] = -1;  offsets[0][1] = 1;
		offsets[1][0] =  0;  offsets[1][1] = 1;
		offsets[2][0] =  1;  offsets[2][1] = 1;
		offsets[3][0] = -1;  offsets[3][1] = 0;
		offsets[4][0] =  1;  offsets[4][1] = 0;
		offsets[5][0] = -1;  offsets[5][1] =-1;
		offsets[6][0] =  0;  offsets[6][1] =-1;
		offsets[7][0] =  1;  offsets[7][1] =-1;

			logger.dbg("Destination point " + DestinationPoint);
		double dist2dest = myBot.myCoord.distance(DestinationPoint);
		double largestCellSize = Math.max ( dMapCellSize.x, dMapCellSize.y);

		if (dist2dest >= largestCellSize/4) {
			// still moving to the preset position
			return;
		}
		grid = point2grid(myBot.myCoord);
		double cDanger = grid2dangerLevel(grid); // danger of the current cell

		ngrid[0] = grid[0] + 1;
		ngrid[1] = grid[1] + 0;
		logger.dbg("grid x = " + grid[0]);
		logger.dbg("grid y = " + grid[1]);
		logger.dbg("ngrid x = " + ngrid[0]);
		logger.dbg("ngrid y = " + ngrid[1]);
		double nDanger = grid2dangerLevel(ngrid); // danger in new cell

		if ( nDanger <= cDanger) {
			DestinationPoint = cellCenter( ngrid[0], ngrid[1] );
			logger.dbg("Destination point " + DestinationPoint);
		}
		// fixme add some mechanism to probe other cells
	}

	public void makeMove() {
		rebuildDangerMap();

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
