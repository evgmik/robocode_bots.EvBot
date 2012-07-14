package eem;
import java.awt.Color;
import java.awt.Graphics2D;
import robocode.*;

/**
 * MyFirstRobot - a robot by (your name here)
 */
public class MyFirstRobot extends AdvancedRobot
{
	/**
	 * run: MyFirstRobot's default behavior
	 */
	// The coordinates of the last scanned robot
	int scannedX = Integer.MIN_VALUE;
	int scannedY = Integer.MIN_VALUE;
	boolean enemyDetected = false; 
	boolean enemyLost = false; 
	double angle2enemy= 0;
	int radarSpinDirection =1;
	int dx=0;
	int dy=0;
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
		double angle;
		setColors(Color.red,Color.blue,Color.green);
		while(true) {
			// Replace the next 4 lines with any behavior you would like
			dbg("----------- Next run -------------");
			turnCount++;
			dbg("Turn count: " + turnCount);
			dbg("enemyLost = " + enemyLost);

			dbg("enemyDetected = " + enemyDetected);
			dbg("radarSpinDirection = " + radarSpinDirection);

			if (!enemyDetected) {
				radarSpinDirection=1;
				angle = shortest_arc(radarSpinDirection*20);
				dbg("Searching enemy by rotating by angle = " + angle);
				turnRadarRight(angle);
			}

			if (enemyDetected && !enemyLost ) {
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

				enemyLost=true;

				// radar angle
				double radar_angle = getRadarHeading();
				radarSpinDirection=1;
				angle=radarSpinDirection*(angle2enemy-radar_angle);
				angle = shortest_arc(angle);
				dbg("Pointing radar to the old target location, rotating by angle = " + angle);
				turnRadarRight(angle);
			}

			if (enemyLost ) {
				radarSpinDirection=-2*radarSpinDirection;
				angle=shortest_arc(radarSpinDirection*2);
				dbg("Reseek spin with spin = " + angle);
				turnRadarRight(angle);
			}

			

		}
	}

	/**
	 * onScannedRobot: What to do when you see another robot
	 */
	public void onScannedRobot(ScannedRobotEvent e) {
		targetName=e.getName();

		// Calculate the angle to the scanned robot
		double angle = (getHeading()+ e.getBearing())/360*2*3.14;

		// Calculate the coordinates of the robot
		scannedX = (int)(getX() + Math.sin(angle) * e.getDistance());
		scannedY = (int)(getY() + Math.cos(angle) * e.getDistance());
		radarSpinDirection=1;
		enemyDetected = true;
		enemyLost = false;
	}

	/**
	 * onHitByBullet: What to do when you're hit by a bullet
	 */
	public void onHitByBullet(HitByBulletEvent e) {
		double angle = shortest_arc( 90 - e.getBearing() );
		dbg("Evasion maneuver by rotating body by angle = " + angle);
		turnLeft(angle);
		ahead(100);
		enemyLost=true;

	}

	public void onRobotDeath(RobotDeathEvent e) {
		if (e.getName().equals(targetName)) {
			enemyDetected = false;
			enemyLost = false;
		}
	}

	public void onPaint(Graphics2D g) {
		// Set the paint color to a red half transparent color
		if (enemyDetected ) {
			g.setColor(new Color(0xff, 0x00, 0x00, 0x80));

			// Draw a line from our robot to the scanned robot
			g.drawLine(scannedX, scannedY, (int)getX(), (int)getY());

			// Draw a filled square on top of the scanned robot that covers it
			g.fillRect(scannedX - 20, scannedY - 20, 40, 40);
		}


	}

}
