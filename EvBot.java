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
	Rules game_rules;
	int robotHalfSize = 20;
	int scannedX = Integer.MIN_VALUE;
	int scannedY = Integer.MIN_VALUE;
	long targetLastSeenTime = - 10; // in far past
	boolean haveTarget = false; 
	boolean targetUnlocked = false; 
	boolean searchForClosestTarget = true;
	boolean movingRadarToLastKnownTargetLocation = false;
	int radarMotionMultiplier = 1;
	int fullSweepDelay = 40;
	double radarSweepSubAngle=game_rules.RADAR_TURN_RATE ;
	double radarSmallestRockingMotion = game_rules.RADAR_TURN_RATE/4;
	int numberOfSmallRadarSweeps =(int) Math.ceil(360 / radarSweepSubAngle);
	int countForNumberOfSmallRadarSweeps=numberOfSmallRadarSweeps;
	double absurdly_huge=1e6; // something huge
	double targetDistance = absurdly_huge;
	//firing with this deviation will bring bullet to the same point
	double angle_resolution = 1; 
	double angle2enemy= 0;
	boolean gameJustStarted = true;
	int countFullSweepDelay=0;
	int turnsToEvasiveMove = 4;
	int countToEvasiveMove = turnsToEvasiveMove;
	int radarSpinDirection =1;
	String targetName="";
	// logger staff
	int verbosity_level=5; // current levels, smaller is less noisy
	// debug levels
	int dbg_important=0;
	int dbg_rutine=5;
	int dbg_noise=10;


	public double cortesian2game_angles(double angle) {
		angle=90-angle;
		return angle;
	}
	public void dbg(int level, String s) {
		if (level <= verbosity_level)
			System.out.println(s);
	}

	public double shortest_arc( double angle ) {
		//dbg(dbg_rutine, "angle received = " + angle);
		angle = angle % 360;
		if ( angle > 180 ) {
			angle = 360 - angle;
		}
		if ( angle < -180 ) {
			angle = 360+angle;
		}
		//dbg(dbg_rutine, "angle return = " + angle);
		return angle;
	}

	public double distanceToWallAhead() {
		double angle=getHeading();
		double dist=0;
		dbg(dbg_rutine, "----angle " + angle);
		dbg(dbg_rutine, "----width " + getBattleFieldWidth());
		dbg(dbg_rutine, "----X " + getX());
		dbg(dbg_rutine, "----height " + getBattleFieldHeight());
		dbg(dbg_rutine, "----Y " + getY());
		if ( 0<= angle && angle < 90 ) {
			dist = Math.min(getBattleFieldWidth()-getX(), getBattleFieldHeight()-getY());
		}
		if ( 90 <= angle && angle < 180 ) {
			dist = Math.min(getBattleFieldWidth()-getX(), getY());
		}
		if ( 180<= angle && angle < 270 ) {
			dist = Math.min(getX(), getY());
		}
		if ( 270 <= angle && angle < 360 ) {
			dist = Math.min(getX(), getBattleFieldHeight()-getY());
		}
		dist = dist - robotHalfSize;
		dist = Math.max(dist,0);
		if (dist < 1) dist = 0 ;
		dbg(dbg_rutine, "distance to closest wall ahead " + dist);
		return dist;
	}

	public void moveOrTurn(double dist, double angle) {
		double moveLength;
		moveLength = Math.min(distanceToWallAhead(),dist);
		if (moveLength == 0 ) {
			if ( haveTarget ) {
				// we need to be focused on enemy
				// body rotation and radar/gun are independent
				setAdjustRadarForRobotTurn(true);
				setAdjustGunForRobotTurn(true);
			} else {
				// there is a chance that we will detect new enemy so
				// body rotation  and radar/gun are locked
				setAdjustRadarForRobotTurn(false); 
				setAdjustGunForRobotTurn(false);
			}
			dbg(dbg_rutine, "Cannot move, rotating by " + angle);
			setTurnRight(angle);
		} else {
			dbg(dbg_rutine, "Moving by " + moveLength);
			setAhead(moveLength);
		}
	}

	public void performFullSweepIfNeded() {
		double angle;

		countForNumberOfSmallRadarSweeps--;
			// full sweep for the closest enemy
			if ( (countFullSweepDelay<0) && !searchForClosestTarget && (!haveTarget && getOthers() == 1) ) {
				dbg(dbg_rutine, "Begin new cycle for closest enemy search");
				searchForClosestTarget = true;
				countForNumberOfSmallRadarSweeps = numberOfSmallRadarSweeps;
			}

			if ( searchForClosestTarget ) {
				angle = radarSweepSubAngle;
				dbg(dbg_rutine, "Search sweep  by angle = " + angle);
				setTurnRadarRight(angle);
				targetUnlocked = true;
			}

			dbg(dbg_rutine, "countForNumberOfSmallRadarSweeps = " + countForNumberOfSmallRadarSweeps);
			if ( countForNumberOfSmallRadarSweeps <= 0 && searchForClosestTarget ) {
				searchForClosestTarget = false;
				countFullSweepDelay = fullSweepDelay;
				dbg(dbg_rutine, "Full sweep for closest enemy is completed");
				movingRadarToLastKnownTargetLocation = true;

				double radar_angle = getRadarHeading();
				angle=(angle2enemy-radar_angle);
				angle = shortest_arc(angle);
				if (sign(angle) >= 0 ) {
					radarSpinDirection=1;
					angle = game_rules.RADAR_TURN_RATE;
				} else {
					radarSpinDirection=-1;
					angle = -game_rules.RADAR_TURN_RATE;
				}
			}
	}

	public int sign( double n) {
		if (n==0) 
			return 0;
		if (n > 0 )
			return 1;
		else
			return -1;
	}

	public void run() {
		int dx=0;
		int dy=0;
		double angle;
		double firePower;
		double moveLength;
		double targetDistance = absurdly_huge;
		double radarBearingToEnemy=0;

		setColors(Color.red,Color.blue,Color.green);
		while(true) {
			// Replace the next 4 lines with any behavior you would like
			dbg(dbg_rutine, "----------- Next run -------------");
			dbg(dbg_rutine, "Game time: " + getTime());
			dbg(dbg_rutine, "Number of other bots = " + getOthers());

			if ( ( getTime() - targetLastSeenTime ) > 1) 
				targetUnlocked = true;
			else
				targetUnlocked = false;


			dbg(dbg_rutine, "targetUnlocked = " + targetUnlocked);

			if (haveTarget) {
				//angle to enemy
				dx=scannedX - (int)(getX());
				dy=scannedY - (int)(getY());
				targetDistance = Math.sqrt( dx*dx + dy*dy);

				angle2enemy=Math.atan2(dy,dx);
				angle2enemy=cortesian2game_angles(angle2enemy*180/3.14);
			}

			countToEvasiveMove--;
			// make preemptive evasive motion
			if ( countToEvasiveMove < 0 ) {
				countToEvasiveMove = turnsToEvasiveMove;
				dbg(dbg_rutine, "Attempting to move ahead for preemptive evasion");
				moveOrTurn(100,90);
			}


			dbg(dbg_rutine, "haveTarget = " + haveTarget);

			//if (!haveTarget) {
				//radarSpinDirection=1;
				//angle = shortest_arc(radarSpinDirection*20);
				//dbg(dbg_rutine, "Searching enemy by rotating by angle = " + angle);
				//setTurnRadarRight(angle);
			//}

			dbg(dbg_rutine, "targetUnlocked = " + targetUnlocked);
			dbg(dbg_rutine, "searchForClosestTarget = " + searchForClosestTarget);
			dbg(dbg_rutine, "radarSpinDirection = " + radarSpinDirection);

			// radar rocking motion to relock target
			if (haveTarget && !searchForClosestTarget && !movingRadarToLastKnownTargetLocation) {
				radarSpinDirection*=-1;
				if (targetUnlocked) {
					radarMotionMultiplier *= 2;
					radarBearingToEnemy=0; //unknown
					angle=(radarBearingToEnemy + radarSpinDirection*radarMotionMultiplier*radarSmallestRockingMotion);
				} else {
					radarBearingToEnemy= shortest_arc(angle2enemy-getRadarHeading());
					radarMotionMultiplier = 1;
					angle=(radarBearingToEnemy + radarSpinDirection*radarMotionMultiplier*radarSmallestRockingMotion/2);
				}


				dbg(dbg_rutine, "Trying to relock on target with radar move by angle = " + angle);
				setTurnRadarRight(angle);
				//targetUnlocked = true;
			}


			if (haveTarget && !targetUnlocked ) {
				//gun angle	
				double gun_angle =getGunHeading();
				angle = shortest_arc(angle2enemy-gun_angle);
				dbg(dbg_rutine, "Pointing gun to enemy by rotating by angle = " + angle);
				setAdjustRadarForGunTurn(true);
				setTurnGunRight(angle);

				double predictedBulletDeviation=angle*Math.PI/180*targetDistance;

				dbg(dbg_rutine, "Gun heat = " + getGunHeat() );
				// if gun is called and
				// predicted bullet deviation within half a body size of the robot
				if (getGunHeat() == 0 && 
				    Math.abs(predictedBulletDeviation) < Math.min( getHeight(), getWidth())/2 ) {
					// calculate firepower based on distance
					firePower = Math.min(500 / targetDistance, 3);
					dbg(dbg_rutine, "Firing the gun with power = " + firePower);
					setFire(firePower);
				}


			}

			// moving radar to or over old target position
			if ( !searchForClosestTarget && targetUnlocked && movingRadarToLastKnownTargetLocation) {
				angle = radarSpinDirection*game_rules.RADAR_TURN_RATE;
				dbg(dbg_rutine, "Pointing radar to the old target location and potentially over sweeping by angle = " + angle);
				setTurnRadarRight(angle);
			}


			countFullSweepDelay--;
			performFullSweepIfNeded();

			execute();
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
		targetLastSeenTime = getTime();

		targetName=e.getName();
		movingRadarToLastKnownTargetLocation = false;
		//radarSpinDirection=1;
		haveTarget = true;
		targetUnlocked = true;
		dbg(dbg_rutine, "Found target");
	}

	/**
	 * onHitByBullet: What to do when you're hit by a bullet
	 */
	public void onHitByBullet(HitByBulletEvent e) {
		double angle = shortest_arc( 90 - e.getBearing() );
		dbg(dbg_rutine, "Evasion maneuver after a hit by rotating body by angle = " + angle);
		setTurnLeft(angle);
		dbg(dbg_rutine, "Attempting to move ahead for bullet evasion");
		moveOrTurn(100,90);
		//targetUnlocked=true;

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
			setAdjustRadarForRobotTurn(true);
			setAdjustGunForRobotTurn(true);
		} else {
			// there is a chance that we will detect new enemy so
			// body rotation  and radar/gun are locked
			setAdjustRadarForRobotTurn(false); 
			setAdjustGunForRobotTurn(false);
		}
		dbg(dbg_rutine, "Changing course after wall is hit  by angle = " + angle);
		setTurnRight (angle);
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
		if ( haveTarget && targetUnlocked ) {
			g.setColor(Color.yellow);
			g.drawOval((int) (getX() - 50), (int) (getY() - 50), 100, 100);
		}
		if ( haveTarget && !targetUnlocked ) {
			g.setColor(Color.red);
			g.drawOval((int) (getX() - 50), (int) (getY() - 50), 100, 100);
		}


	}

}
