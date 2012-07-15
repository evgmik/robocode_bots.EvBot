package eem;
import java.awt.Color;
import java.awt.Graphics2D;
import robocode.*;

/**
 * EvBot - a robot by Eugeniy Mikhailov
 */
public class EvBot extends AdvancedRobot
{
	/**
	 * run: MyFirstRobot's default behavior
	 */
	// The coordinates of the last scanned robot
	int scannedX = Integer.MIN_VALUE;
	int scannedY = Integer.MIN_VALUE;
	boolean haveTarget = false; 
	boolean targetUnlocked = false; 
	double absurdly_huge=1e6; // something huge
	double targetDistance = absurdly_huge;
	double angle2enemy= 0;
	boolean gameJustStarted = true;
	int turnsToEvasiveMove = 4;
	int countToEvasiveMove = turnsToEvasiveMove;
	int radarSpinDirection =1;
	String targetName;
	int turnCount=0;

	public double cortesian2game_angles(double angle) {
		angle=90-angle;
		return angle;
	}
	public void dbg(String s) {
		System.out.println(s);
	}

	public double shortest_arc( double angle ) {
		//dbg("angle received = " + angle);
		angle = angle % 360;
		if ( angle > 180 ) {
			angle = 360 - angle;
		}
		if ( angle < -180 ) {
			angle = 360+angle;
		}
		//dbg("angle return = " + angle);
		return angle;
	}


	public void run() {
		int dx=0;
		int dy=0;
		double angle;
		setColors(Color.red,Color.blue,Color.green);
		while(true) {
			// Replace the next 4 lines with any behavior you would like
			dbg("----------- Next run -------------");
			if ( gameJustStarted ) {
				gameJustStarted = false;
				angle = 360;
				dbg("Beginning of the game sweep  by angle = " + angle);
				turnRadarRight(angle);
			}

			countToEvasiveMove--;
			if ( countToEvasiveMove < 0 ) {
				countToEvasiveMove = turnsToEvasiveMove;
				ahead(100);
			}


			turnCount++;
			dbg("Turn count: " + turnCount);
			dbg("targetUnlocked = " + targetUnlocked);

			dbg("haveTarget = " + haveTarget);
			dbg("radarSpinDirection = " + radarSpinDirection);

			if (!haveTarget) {
				radarSpinDirection=1;
				angle = shortest_arc(radarSpinDirection*20);
				dbg("Searching enemy by rotating by angle = " + angle);
				turnRadarRight(angle);
			}

			if (haveTarget && !targetUnlocked ) {
				//angle to enemy
				dx=scannedX - (int)(getX());
				dy=scannedY - (int)(getY());
				angle2enemy=Math.atan2(dy,dx);
				angle2enemy=cortesian2game_angles(angle2enemy*180/3.14);
				
				//gun angle	
				double gun_angle =getGunHeading();
				angle = shortest_arc(angle2enemy-gun_angle);
				dbg("Pointing gun to enemy by rotating by angle = " + angle);
				turnGunRight(angle);
				fire(3);

				targetUnlocked=true;

				// radar angle
				double radar_angle = getRadarHeading();
				radarSpinDirection=1;
				angle=radarSpinDirection*(angle2enemy-radar_angle);
				angle = shortest_arc(angle);
				dbg("Pointing radar to the old target location, rotating by angle = " + angle);
				turnRadarRight(angle);
			}

			if (targetUnlocked ) {
				radarSpinDirection=-2*radarSpinDirection;
				angle=shortest_arc(radarSpinDirection*2);
				dbg("Trying to find unlocked target with radar move by angle = " + angle);
				turnRadarRight(angle);
			}

			

		}
	}

	/**
	 * onScannedRobot: What to do when you see another robot
	 */
	public void onScannedRobot(ScannedRobotEvent e) {
		if ( !e.getName().equals(targetName) && (targetDistance < e.getDistance()) ) {
			//new target is further then old one
			//we will not switch to it
			return; 
		}

		// Calculate the angle to the scanned robot
		double angle = (getHeading()+ e.getBearing())/360*2*3.14;

		// Calculate the coordinates of the robot
		scannedX = (int)(getX() + Math.sin(angle) * e.getDistance());
		scannedY = (int)(getY() + Math.cos(angle) * e.getDistance());
		targetDistance = e.getDistance();

		targetName=e.getName();
		radarSpinDirection=1;
		haveTarget = true;
		targetUnlocked = false;
	}

	/**
	 * onHitByBullet: What to do when you're hit by a bullet
	 */
	public void onHitByBullet(HitByBulletEvent e) {
		double angle = shortest_arc( 90 - e.getBearing() );
		dbg("Evasion maneuver by rotating body by angle = " + angle);
		turnLeft(angle);
		ahead(100);
		targetUnlocked=true;

	}

	public void onRobotDeath(RobotDeathEvent e) {
		if (e.getName().equals(targetName)) {
			haveTarget = false;
			targetUnlocked = false;
			targetDistance = absurdly_huge;
			targetName = ""; // something non existing
		}
	}


	public void onHitWall(HitWallEvent e) {
		// turn and move along the hit wall
		double angle = 90-(180 - e.getBearing());
		if ( haveTarget ) {
			// we need to be focused on enemy
			// body rotation and radar/gun are independent
			setAdjustRadarForRobotTurn(false);
			setAdjustGunForRobotTurn(true);
		} else {
			// there is a chance that we will detect new enemy so
			// body rotation  and radar/gun are locked
			setAdjustRadarForRobotTurn(true); 
			setAdjustGunForRobotTurn(false);
		}
		dbg("Changing course after wall is hit  by angle = " + angle);
		turnRight (angle);
	}
	
	public void onPaint(Graphics2D g) {
		// Set the paint color to a red half transparent color
		if (haveTarget ) {
			g.setColor(new Color(0xff, 0x00, 0x00, 0x80));

			// Draw a line from our robot to the scanned robot
			g.drawLine(scannedX, scannedY, (int)getX(), (int)getY());

			// Draw a filled square on top of the scanned robot that covers it
			g.fillRect(scannedX - 20, scannedY - 20, 40, 40);
		}


	}

}
