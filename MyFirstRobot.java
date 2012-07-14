package eem;
import java.awt.Color;
import java.awt.Graphics2D;
import robocode.*;
//import java.awt.Color;

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
	int enemyDetected = 0; 

	public void run() {
		// After trying out your robot, try uncommenting the import at the top,
		// and the next line:
		//setColors(Color.red,Color.blue,Color.green);
		while(true) {
			// Replace the next 4 lines with any behavior you would like

			if (enemyDetected != 1) {
				ahead(100);
				turnRight(20);
				turnGunRight(20);
			}
			fire(3);
			//back(100);
			//turnGunRight(180);

		}
	}

	/**
	 * onScannedRobot: What to do when you see another robot
	 */
	public void onScannedRobot(ScannedRobotEvent e) {
		// Calculate the angle to the scanned robot
		double angle = (getHeading()+ e.getBearing())/360*2*3.14;
		System.out.println("---------------------");
		System.out.println(getHeading());
		System.out.println(e.getBearing());
		System.out.println(angle);

		// Calculate the coordinates of the robot
		scannedX = (int)(getX() + Math.sin(angle) * e.getDistance());
		scannedY = (int)(getY() + Math.cos(angle) * e.getDistance());
		enemyDetected = 1;
		//turnGunRight(-5);
		fire(3);
		//turnGunRight(-20);
	}

	/**
	 * onHitByBullet: What to do when you're hit by a bullet
	 */
	public void onHitByBullet(HitByBulletEvent e) {
		turnLeft(90 - e.getBearing());

	}

	public void onPaint(Graphics2D g) {
		// Set the paint color to a red half transparent color
		if (enemyDetected == 1) {
			g.setColor(new Color(0xff, 0x00, 0x00, 0x80));

			// Draw a line from our robot to the scanned robot
			g.drawLine(scannedX, scannedY, (int)getX(), (int)getY());

			// Draw a filled square on top of the scanned robot that covers it
			g.fillRect(scannedX - 20, scannedY - 20, 40, 40);
		}


	}

}
