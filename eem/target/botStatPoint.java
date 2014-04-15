// -*- java -*-

package eem.target;

import eem.EvBot;
import eem.misc.math;
import eem.misc.logger;
import robocode.*;
import robocode.Rules.*;
import java.awt.geom.Point2D;

public class botStatPoint {
	private Point2D.Double pos;
	private long tStamp;
	private Point2D.Double velocity;
	private double headingDegrees;
	private double energy;

	public botStatPoint() {
		pos = new Point2D.Double(0,0);
		tStamp = 0;
		velocity = new  Point2D.Double(0,0);
		headingDegrees = 0;
		energy =0;
	}

	public botStatPoint(AdvancedRobot bot, ScannedRobotEvent e ) {
		this();
		double speed;
		Point2D.Double myCoord = new Point2D.Double();
		myCoord.x = bot.getX();
	       	myCoord.y = bot.getY();
		double angle = (bot.getHeading()+ e.getBearing())/360.*2.*Math.PI;
		double distance = e.getDistance();
		pos = new Point2D.Double( (myCoord.x + Math.sin(angle) * distance),
				(myCoord.y + Math.cos(angle) * distance) );
		tStamp = bot.getTime() + 100000*(bot.getRoundNum()+1); // round cnt large enough to update major digit
		headingDegrees = e.getHeading();
		speed = e.getVelocity();
		velocity = new Point2D.Double( speed*Math.sin(headingDegrees/360.*2.*Math.PI), speed*Math.cos(headingDegrees/360.*2.*Math.PI) );
		if ( speed < 0 ) {
			headingDegrees = math.shortest_arc( headingDegrees + 180 );
		}
		energy = e.getEnergy();
	}

	public botStatPoint(EvBot bot) {
		this();
		double speed;
		pos.x = bot.getX();
	       	pos.y = bot.getY();
		tStamp = bot.getTime() + 100000*(bot.getRoundNum()+1); // round cnt large enough to update major digit
		headingDegrees = bot.getHeading();
		speed = bot.getVelocity();
		velocity = new Point2D.Double( speed*Math.sin(headingDegrees/360.*2.*Math.PI), speed*Math.cos(headingDegrees/360.*2.*Math.PI) );
		if ( speed < 0 ) {
			headingDegrees = math.shortest_arc( headingDegrees + 180 );
		}
		energy = bot.getEnergy();
	}

	public botStatPoint(Point2D.Double p, long t ) {
		this();
		tStamp = t;
		pos = p;
	}

	public Double getDistance(Point2D.Double p) {
		return p.distance(pos);
	}

	public String format() {
		String str = "";
		str = str + "energy = " + energy + ", velocity = [ " + velocity.x + ", " + velocity.y + " ]" + ", heading = " + headingDegrees;
		str = str + "\n";
		str = str + "position = [ "+ pos.x + ", " + pos.y + " ], tStamp = " + tStamp;
		return str;
	}

	public long getTimeStamp() {
		return tStamp;
	}

	public double getEnergy() {
		return energy;
	}
	public double getX() {
		return pos.x;
	}

	public double getY() {
		return pos.y;
	}
	
	public double getHeadingDegrees() {
		return headingDegrees;
	}

	public double getSpeed() {
		return velocity.distance(0,0);	
	}

	public Point2D.Double getVelocity() {
		return velocity;
	}

	public Point2D.Double getPosition() {
		return  pos;
	}

	public boolean arePointsOfPathSimilar(botStatPoint refPatStart, botStatPoint refPatCurrent, botStatPoint testPatStart) {
		double maxSpeedDist = 0.5;
		double maxAngleDist = 20;
		double spdT = this.getSpeed();
		double angT = this.getHeadingDegrees() - testPatStart.getHeadingDegrees();
		long   timeDiffT = this.getTimeStamp() - testPatStart.getTimeStamp();
		double spdR = refPatCurrent.getSpeed();
		double angR = refPatCurrent.getHeadingDegrees() - refPatStart.getHeadingDegrees();
		long   timeDiffR = refPatCurrent.getTimeStamp() - refPatStart.getTimeStamp();
		if ( ( Math.abs( spdT - spdR ) > maxSpeedDist ) || ( Math.abs( angT - angR) > maxAngleDist ) ) {
			return false;
		}
		// now let's check that the timing is right
		if ( timeDiffT != timeDiffR )
			return false;
		return true;
	}

}
