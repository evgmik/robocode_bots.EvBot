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
	int targetLastX = Integer.MIN_VALUE;
	int targetLastY = Integer.MIN_VALUE;
	int nonexisting_coord = -10000;
	int targetPrevX = nonexisting_coord;
	int targetPrevY = nonexisting_coord;
	int targetFutureX = nonexisting_coord;
	int targetFutureY = nonexisting_coord;
	long targetLastSeenTime = - 10; // in far past
	long targetPrevSeenTime = - 10; // in far past
	boolean haveTarget = false; 
	boolean targetUnlocked = false; 
	boolean searchForClosestTarget = true;
	boolean movingRadarToLastKnownTargetLocation = false;
	boolean executingWallEvadingTurn = false;
	int radarMotionMultiplier = 1;
	int fullSweepDelay = 10;
	double radarSweepSubAngle=game_rules.RADAR_TURN_RATE ;
	double radarSmallestRockingMotion = game_rules.RADAR_TURN_RATE/4;
	int numberOfSmallRadarSweeps =(int) Math.ceil(360 / radarSweepSubAngle);
	int countForNumberOfSmallRadarSweeps=numberOfSmallRadarSweeps;
	double absurdly_huge=1e6; // something huge
	double targetDistance = absurdly_huge;
	//firing with this deviation will bring bullet to the same point
	double angle_resolution = 1; 
	double angle2enemy= 0;
	double angle2enemyInFutire= 0;
	boolean gameJustStarted = true;
	int countFullSweepDelay=0;
	int radarSpinDirection =1;
	String targetName="";
	// logger staff
	// debug levels
	int dbg_important=0;
	int dbg_rutine=5;
	int dbg_debuging=6;
	int dbg_noise=10;
	int verbosity_level=6; // current level, smaller is less noisy


	public double cortesian2game_angles(double angle) {
		angle=90-angle;
		return angle;
	}
	public void dbg(int level, String s) {
		if (level <= verbosity_level)
			System.out.println(s);
	}

	public double shortest_arc( double angle ) {
		dbg(dbg_noise, "angle received = " + angle);
		angle = angle % 360;
		if ( angle > 180 ) {
			angle = -(360 - angle);
		}
		if ( angle < -180 ) {
			angle = 360+angle;
		}
		dbg(dbg_noise, "angle return = " + angle);
		return angle;
	}

	public double distanceToWallAhead() {
		double angle=getHeading(); 
		double velocity=getVelocity();
		dbg(dbg_noise, "Our velocity = " + velocity);
		double dist=0;
		if ( velocity < 0 ) angle +=180; // we are moving backwards
		angle = angle % 360;
		dbg(dbg_noise, "----angle " + angle);
		dbg(dbg_noise, "----width " + getBattleFieldWidth());
		dbg(dbg_noise, "----X " + getX());
		dbg(dbg_noise, "----height " + getBattleFieldHeight());
		dbg(dbg_noise, "----Y " + getY());
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
		dbg(dbg_noise, "distance to closest wall ahead " + dist);
		return dist;
	}

	public int whichWayToRotateAwayFromWall() {
		double angle = getHeading();
		if ( getVelocity() < 0 ) 
			angle += 180; // we are moving backwards
		angle = shortest_arc(angle);
	        double x = getX();
	        double y = getY();
		int rotDir = 1;

		dbg(dbg_noise, "body heading = " + angle);
		if ( 0 <= angle	&& angle < 90 ) {
			if ( (getBattleFieldWidth()-x) < (getBattleFieldHeight() - y) ) {
				rotDir = -1;
			} else {
				rotDir =  1;
			}
		}
		if ( 90 <= angle && angle < 180 ) {
			if ( (getBattleFieldWidth()-x) < y ) {
				rotDir =  1;
			} else {
				rotDir = -1;
			}
		}
		if ( -180 <= angle && angle < -90 ) {
			if ( x < y ) {
				rotDir = -1;
			} else {
				rotDir =  1;
			}
		}
		if ( -90 <= angle && angle < 0 ) {
			if ( x < (getBattleFieldHeight() - y) ) {
				rotDir =  1;
			} else {
				rotDir = -1;
			}
		}
		//if ( getVelocity() < 0 ) rotDir = - rotDir; // we are moving backwards
		return rotDir;
	}
	public double shortestTurnRadiusVsSpeed() {
		// very empiric for full speed
		return 115;
	}

	public void moveOrTurn(double dist, double suggestedAngle) {
		double angle;
		double moveLength;
		double shortestTurnRadius=shortestTurnRadiusVsSpeed();
		double hardStopDistance = 20;
		double evadeWallDist= shortestTurnRadius + hardStopDistance;
		double wallAheadDist;
		wallAheadDist = distanceToWallAhead();
		dbg(dbg_noise, "hardStopDistance =  " + hardStopDistance);
		dbg(dbg_noise, "wallAheadDist =  " + wallAheadDist);
		dbg(dbg_noise, "getDistanceRemaining =  " + getDistanceRemaining());
		dbg(dbg_noise, "rotate away from by " + whichWayToRotateAwayFromWall() );
		if (wallAheadDist < hardStopDistance ) {
			// wall is close full reverse
			angle = 180*whichWayToRotateAwayFromWall();
			executingWallEvadingTurn=true;
			dbg(dbg_noise, "Wall is too close, hard stop now and rotating by angle " + angle );
			setTurnRight(angle);
			setAhead(0); // hard stop
		} 
		if ( wallAheadDist > hardStopDistance && wallAheadDist <= evadeWallDist ){
			if ( !executingWallEvadingTurn ) {
				// make hard turn
				executingWallEvadingTurn = true;
				angle = 90*whichWayToRotateAwayFromWall();
				dbg(dbg_noise, "Wall is approaching, trying to turn away by " + angle);
				setTurnRight(angle);
				dist= Math.PI/2*shortestTurnRadiusVsSpeed();
				if ( getDistanceRemaining() < 0 ) {
					dist = -dist;
				}
				setAhead(dist);
			} else {
				dbg(dbg_noise, "Should be already turning away from the wall ");
				if ( getVelocity() == 0 ) setAhead(Math.PI/2*shortestTurnRadiusVsSpeed());
			}
		} 
		if (wallAheadDist > evadeWallDist) {
			executingWallEvadingTurn = false;
			dbg(dbg_noise, "getDistanceRemaining = " + getDistanceRemaining());
			if ( Math.abs(getDistanceRemaining()) <= 20 ) {
				dbg(dbg_noise, "Moving by " + dist);
				setAhead(dist);
				dbg(dbg_noise, "Turning by " + suggestedAngle);
				setTurnRight(suggestedAngle);
			}
		}
	}

	public void performFullSweepIfNeded() {
		double angle;

		dbg(dbg_noise, "countFullSweepDelay = " + countFullSweepDelay);
		dbg(dbg_noise, "searchForClosestTarget = " + searchForClosestTarget);
		countForNumberOfSmallRadarSweeps--;
			// full sweep for the closest enemy
			if ( (countFullSweepDelay<0) && !searchForClosestTarget && (getOthers() > 1) || !haveTarget) {
				dbg(dbg_noise, "Begin new cycle for closest enemy search");
				searchForClosestTarget = true;
				countForNumberOfSmallRadarSweeps = numberOfSmallRadarSweeps;
			}

			if ( searchForClosestTarget ) {
				angle = radarSweepSubAngle;
				dbg(dbg_noise, "Search sweep  by angle = " + angle);
				setTurnRadarRight(angle);
				targetUnlocked = true;
			}

			dbg(dbg_noise, "countForNumberOfSmallRadarSweeps = " + countForNumberOfSmallRadarSweeps);
			if ( countForNumberOfSmallRadarSweeps <= 0 && searchForClosestTarget ) {
				searchForClosestTarget = false;
				countFullSweepDelay = fullSweepDelay;
				dbg(dbg_noise, "Full sweep for closest enemy is completed");
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

	public double  bulletSpeed( double firePower ) {
		double bSpeed;
		bSpeed = ( 20 - firePower * 3 );
		dbg(dbg_noise, "bullet speed = " + bSpeed + " for firePower = " + firePower);
		return bSpeed;
	}

	public int sign( double n) {
		if (n==0) 
			return 0;
		if (n > 0 )
			return 1;
		else
			return -1;
	}

	public double quadraticSolverMinPosRoot(double a, double b, double c) {
		// we are solving for time in ballistic calculation
		// and interested only in positive solutions
		// hopefully determinant is always >= 0 since we solve real problems
		dbg(dbg_noise, "quadratic equation coefficient a = " + a);
		dbg(dbg_noise, "quadratic equation coefficient b = " + b);
		dbg(dbg_noise, "quadratic equation coefficient c = " + c);
		double d = Math.sqrt(b*b - 4*a*c);
		double x1= (-b + d)/(2*a);
		double x2= (-b - d)/(2*a);

		double root=Math.min(x1,x2);
		if (root < 0) {
			// if min gave as wrong root max should do better
			root=Math.max(x1,x2);
		}

		dbg(dbg_noise, "quadratic equation min positive root = " + root);
		return root;
	}

	public void  setFutureTargetPosition( double firePower ) {
		double Tx, Ty, vTx, vTy, vT,  dx, dy, dist;
		double timeToHit;
		double a, b, c;
		double bSpeed=bulletSpeed( firePower );

		// target velocity
		vTx = (targetLastX - targetPrevX)/(targetLastSeenTime - targetPrevSeenTime);
		vTy = (targetLastY - targetPrevY)/(targetLastSeenTime - targetPrevSeenTime);
		vT = Math.sqrt(vTx*vTx + vTy*vTy);
		dbg(dbg_noise, "Target velocity vTx = " + vTx + " vTy = " + vTy);

		// estimated current target position
		Tx = targetLastX + vTx*(getTime()-targetLastSeenTime);
		Ty = targetLastY + vTy*(getTime()-targetLastSeenTime);
		dbg(dbg_noise, "Target estimated current position Tx = " + Tx + " Ty = " + Ty);

		// radius vector to target
		dx = Tx-getX();
		dy = Ty-getY();
		dist = Math.sqrt(dx*dx + dy*dy);
		

		// back of envelope calculations
		// for the case of linear target motion with no acceleration
		// lead to quadratic equation for time of flight to target hit
		a = vT*vT - bSpeed*bSpeed;
		b = 2*( dx*vTx + dy*vTy);
		c = dist*dist;

		timeToHit = quadraticSolverMinPosRoot( a, b, c);
		targetFutureX = (int) ( Tx + vTx*timeToHit );
		targetFutureY = (int) ( Ty + vTy*timeToHit );

		// check that future target position within the battle field
		targetFutureX = (int)Math.max(targetFutureX, 0);
		targetFutureX = (int)Math.min(targetFutureX, getBattleFieldWidth() );
		targetFutureY = (int)Math.max(targetFutureY, 0);
		targetFutureY = (int)Math.min(targetFutureY, getBattleFieldHeight() );

	}
	
	public double firePoverVsDistance( double targetDistance ) {
		// calculate firepower based on distance
		double firePower;
		firePower = Math.min(500 / targetDistance, 3);
		return firePower;
	}

	public void run() {
		int dx=0;
		int dy=0;
		double angle;
		double firePower=0;
		double bulletFlyTimeEstimate;
		double moveLength;
		double targetDistance = absurdly_huge;
		double radarBearingToEnemy=0;

		setColors(Color.red,Color.blue,Color.green);
		while(true) {
			// Replace the next 4 lines with any behavior you would like
			dbg(dbg_rutine, "----------- Next run -------------");
			dbg(dbg_noise, "Game time: " + getTime());
			dbg(dbg_noise, "Number of other bots = " + getOthers());

			if ( ( getTime() - targetLastSeenTime ) > 1) 
				targetUnlocked = true;
			else
				targetUnlocked = false;


			dbg(dbg_noise, "targetUnlocked = " + targetUnlocked);

			if (haveTarget) {
				//angle to enemy
				dx=targetLastX - (int)(getX());
				dy=targetLastY - (int)(getY());
				targetDistance = Math.sqrt( dx*dx + dy*dy);
				dbg(dbg_noise, "Last known target X coordinate = " + targetLastX );
				dbg(dbg_noise, "Last known target Y coordinate = " + targetLastY );

				angle2enemy=Math.atan2(dy,dx);
				angle2enemy=cortesian2game_angles(angle2enemy*180/Math.PI);

				// calculate firepower based on distance
				firePower = firePoverVsDistance( targetDistance );

				// estimate future enemy location
				setFutureTargetPosition( firePower );

				dbg(dbg_noise, "Predicted target X coordinate = " + targetFutureX );
				dbg(dbg_noise, "Predicted target Y coordinate = " + targetFutureY );

				dx=targetFutureX - (int)(getX());
				dy=targetFutureY - (int)(getY());

				angle2enemyInFutire=Math.atan2(dy,dx);
				angle2enemyInFutire=cortesian2game_angles(angle2enemyInFutire*180/Math.PI);

			}

			// make preemptive evasive motion
			dbg(dbg_noise, "Normal motion algorithm");
			double angleRandDeviation=45*sign(0.5-Math.random());
			double dist=100*sign(0.6-Math.random());
			if ( executingWallEvadingTurn ) {
				dbg(dbg_noise, "executingWallEvadingTurn = " + executingWallEvadingTurn);
				// if we were at hard stop near wall we need to
				// move forward otherwise turn away algorithm stacks
				dist = Math.abs(dist);
			}
			moveOrTurn(dist, angleRandDeviation);


			dbg(dbg_noise, "haveTarget = " + haveTarget);

			dbg(dbg_noise, "targetUnlocked = " + targetUnlocked);
			dbg(dbg_noise, "searchForClosestTarget = " + searchForClosestTarget);
			dbg(dbg_noise, "radarSpinDirection = " + radarSpinDirection);

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


				dbg(dbg_noise, "Trying to relock on target with radar move by angle = " + angle);
				setTurnRadarRight(angle);
				//targetUnlocked = true;
			}


			if (haveTarget && !targetUnlocked ) {
				//gun angle	
				double gun_angle =getGunHeading();
				angle = shortest_arc(angle2enemyInFutire-gun_angle);
				dbg(dbg_noise, "Pointing gun to enemy by rotating by angle = " + angle);
				setAdjustRadarForGunTurn(true);
				setTurnGunRight(angle);

				double predictedBulletDeviation=angle*Math.PI/180*targetDistance;

				dbg(dbg_noise, "Gun heat = " + getGunHeat() );
				// if gun is called and
				// predicted bullet deviation within half a body size of the robot
				if (getGunHeat() == 0 && 
				    Math.abs(predictedBulletDeviation) < Math.min( getHeight(), getWidth())/2 ) {
					dbg(dbg_noise, "Firing the gun with power = " + firePower);
					setFire(firePower);
				}


			}

			// moving radar to or over old target position
			if ( !searchForClosestTarget && targetUnlocked && movingRadarToLastKnownTargetLocation) {
				angle = radarSpinDirection*game_rules.RADAR_TURN_RATE;
				dbg(dbg_noise, "Pointing radar to the old target location and potentially over sweeping by angle = " + angle);
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
		double angle = (getHeading()+ e.getBearing())/360*2*Math.PI;

		// recording previously known position and time
		targetPrevX = targetLastX;
		targetPrevY = targetLastY;
		targetPrevSeenTime = targetLastSeenTime;

		// Calculate the coordinates of the robot
		targetLastX = (int)(getX() + Math.sin(angle) * e.getDistance());
		targetLastY = (int)(getY() + Math.cos(angle) * e.getDistance());
		targetDistance = e.getDistance();
		targetLastSeenTime = getTime();

		if ( targetPrevY == nonexisting_coord ) {
			// put same coordinate for unknown previous position
			targetPrevX = targetLastX;
			targetPrevY = targetLastY;
			// but time we put with offset to avoid division by zero
			targetPrevSeenTime = targetLastSeenTime-1;
		}

		targetName=e.getName();
		movingRadarToLastKnownTargetLocation = false;
		//radarSpinDirection=1;
		haveTarget = true;
		targetUnlocked = true;
		dbg(dbg_noise, "Found target");
	}

	/**
	 * onHitByBullet: What to do when you're hit by a bullet
	 */
	public void onHitByBullet(HitByBulletEvent e) {
		double angle = shortest_arc( 90 - e.getBearing() );
		dbg(dbg_noise, "Evasion maneuver after a hit by rotating body by angle = " + angle);
		dbg(dbg_noise, "Attempting to move ahead for bullet evasion");
		//setTurnLeft(angle);
		moveOrTurn(100,angle);
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
			g.drawLine(targetLastX, targetLastY, (int)getX(), (int)getY());

			// Draw a filled square on top of the scanned robot that covers it
			g.fillRect(targetLastX - 20, targetLastY - 20, 40, 40);

			// show estimated future position
			g.drawLine(targetFutureX, targetFutureY, (int)getX(), (int)getY());
			g.fillRect(targetFutureX - 20, targetFutureY - 20, 40, 40);


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
