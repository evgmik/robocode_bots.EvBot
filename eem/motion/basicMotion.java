// -*- java -*-

package eem.motion;

import eem.EvBot;
import eem.target.*;
import eem.misc.*;
import java.util.Random;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.Color;


public class basicMotion {
	protected static EvBot myBot;

	public basicMotion() {
	}

	public basicMotion(EvBot bot) {
		myBot = bot;
	}

	public double bearingTo( Point2D.Double  pt) {
		return math.shortest_arc(
			math.cortesian2game_angles( Math.atan2( pt.y-myBot.myCoord.y, pt.x-myBot.myCoord.x )*180/Math.PI )
			);
	}

	public void moveToPoint( Point2D.Double pnt ) {
		double angle = math.shortest_arc( bearingTo(pnt) - myBot.getHeading() );
		logger.dbg("Rotation angle = " + angle);
		double dist = myBot.myCoord.distance(pnt);
		logger.dbg("Distance to desired point = " + dist);
		if ( Math.abs(angle ) > 90 ) {
			if (angle > 90 ) {
				angle = angle - 180;
			} else {
				angle = angle + 180;
			}
			dist = -dist;
		}
		myBot.setTurnRight(angle);
		myBot.setAhead (dist);
	}

	public void makeMove() {
		// for basic motion we do nothing
	}

	public void onPaint(Graphics2D g) {
	}
}
