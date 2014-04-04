// -*- java -*-

package eem.motion;

import eem.EvBot;
import eem.target.*;
import eem.misc.*;
import eem.bullets.*;
import eem.motion.dangerPoint;
import eem.motion.dangerPath;
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


public class safestPathMotion extends dangerMapMotion {
	private dangerPath  safestPath = new dangerPath();
	public LinkedList<dangerPath> dangerPaths;
	private int maxPathLength = 10;
	
	public void initTic() {
		if ( safestPath.size() < 1 ) {
			safestPath = randomPath();
		}
		DestinationPoint = safestPath.removeFirst().getPosition();
	}

	public safestPathMotion(EvBot bot) {
		super(bot);
	       	safestPath = randomPath();
	}

	public dangerPath randomPath() {
		dangerPath  nPath = new dangerPath();
		double dL = 0;
		Point2D.Double pos = (Point2D.Double) myBot.myCoord.clone();
		dangerPoint dp;
		int cnt = 0;
		while( cnt < maxPathLength) {
			pos.x += 50*(Math.random()-.5);
			pos.y += 50*(Math.random()-.5);
			if ( shortestDist2wall(pos) > (myBot.robotHalfSize + 1) ) {
				dp= new dangerPoint( pos, dL );
				nPath.add( dp );
				cnt++;
			}
		}
		return nPath;
	}

	private void buildListOfPathToTest() {
	}

	private void sortDangerPaths() {
		Collections.sort(dangerPaths);
	}


	public void makeMove() {
		//choseNewDestinationPoint();
		moveToPoint( DestinationPoint );
	}

	public void onPaint(Graphics2D g) {
		safestPath.onPaint(g);
	}


}

