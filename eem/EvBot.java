package eem;
import eem.misc.*;
import eem.botVersion.*;
import eem.gun.*;
import eem.target.*;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Random;
import robocode.*;
import robocode.util.*;
import robocode.Rules.*;

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
	double BodyTurnRate = 10;
	int robotHalfSize = 18;
	public target _trgt = new target();
	double targetLastX = Integer.MIN_VALUE;
	double targetLastY = Integer.MIN_VALUE;
	int nonexisting_coord = -10000;
	// bot tangent position at the starboard/port (right/left) 
	// at minimal turning radius at the current speed
	double starboardStickX = nonexisting_coord;
	double starboardStickY = nonexisting_coord;
	double portStickX = nonexisting_coord;
	double portStickY = nonexisting_coord;

	private baseGun _gun = new linearGun(this);


	private static Random gun_rand = new Random();
	public Point2D.Double myCoord = new Point2D.Double(nonexisting_coord, nonexisting_coord);
	public Point2D.Double BattleField = new Point2D.Double(nonexisting_coord, nonexisting_coord);
	long targetLastSeenTime = - 10; // in far past
	long targetPrevSeenTime = - 10; // in far past
	boolean haveTarget = false; 
	boolean targetUnlocked = false; 
	boolean searchForClosestTarget = true;
	boolean movingRadarToLastKnownTargetLocation = false;
	boolean executingWallEvadingTurn = false;
	int radarMotionMultiplier = 1;
	int fullSweepDelay = 200;
	double radarSweepSubAngle=game_rules.RADAR_TURN_RATE ;
	double radarSmallestRockingMotion = game_rules.RADAR_TURN_RATE/4;
	int numberOfSmallRadarSweeps =(int) Math.ceil(360 / radarSweepSubAngle);
	int countForNumberOfSmallRadarSweeps=numberOfSmallRadarSweeps;
	double absurdly_huge=1e6; // something huge
	//firing with this deviation will bring bullet to the same point
	double angle_resolution = 1; 
	double angle2enemy= 0;
	double angle2enemyInFutire= 0;
	double desiredBodyRotationDirection = 0; // our robot body desired angle
	boolean gameJustStarted = true;
	boolean gunFired = true;
	int countFullSweepDelay=0;
	int radarSpinDirection =1;
	String previoslyHeadedWall = "none";
	// logger staff
	// debug levels
	public int dbg_important=0;
	public int dbg_rutine=5;
	public int dbg_debuging=6;
	public int dbg_noise=10;
	public int verbosity_level=6; // current level, smaller is less noisy

	public void initBattle() {
		BattleField.x = getBattleFieldWidth();
		BattleField.y = getBattleFieldHeight();

		setColors(Color.red,Color.blue,Color.green);
	}

	public void initTic() {
		myCoord.x = getX();
	       	myCoord.y = getY();
	}

	public void calculateSticksEndsPosition() {
		double r=shortestTurnRadiusVsSpeed();
		double a=getHeadingRadians();
		starboardStickX = myCoord.x + r*Math.sin(a+Math.PI/2);
		starboardStickY = myCoord.y + r*Math.cos(a+Math.PI/2);

		portStickX = myCoord.x + r*Math.sin(a-Math.PI/2);
		portStickY = myCoord.y + r*Math.cos(a-Math.PI/2);
	}

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

	public double distanceToTheClosestWallFrom( double px, double py ) {
		double[] d={px, BattleField.x -px, py, BattleField.y-py};
		Arrays.sort(d);
		return d[0];
	}

	public boolean areBothSticksEndAtField() {
		if ( distToTheClosestWallFromStick("starboard") > 0 && distToTheClosestWallFromStick("port") > 0 ) {
			return true;
		} 
		return false;
	}

	public String whichStickIsFurtherFromWalls( ) {
		String stick="none";
		double starboardDist=distToTheClosestWallFromStick( "starboard" );
		double portDist=distToTheClosestWallFromStick( "port" );

		if ( starboardDist >= portDist ) {
			stick="starboard";
		} else {
			stick="port";
		}
		return stick;
	}

	public double distToTheClosestWallFromStick( String stick ) {
		double dist=0;
		if ( stick.equals("starboard") ) {
			dist=distanceToTheClosestWallFrom( starboardStickX, starboardStickY );
		} else {
			dist=distanceToTheClosestWallFrom( portStickX, portStickY );
		}
		return dist;
	}

	public String whichWallAhead() {
		double angle=getHeadingRadians(); 
		double velocity=getVelocity();
		double x = myCoord.x;
		double y = myCoord.y;

		String wallName="";

		if ( Utils.isNear(velocity, 0.0) ) {
			// we are not moving anywhere 
			// assigning fake velocity
			velocity = 8;
		}

		double dx = Math.sin( angle )*velocity;
		double dy = Math.cos( angle )*velocity;

		while (  wallName.equals("") ) {
			x+= dx;
			y+= dy;
			dbg(dbg_noise, "Projected position = " + x + ", " + y);

			if ( x-robotHalfSize <= 0 ) {
				wallName = "left";
			}
			if ( y-robotHalfSize <= 0 ) {
				wallName = "bottom";
			}
			if ( x >= BattleField.x-robotHalfSize ) {
				wallName = "right";
			}
			if ( y >= BattleField.y-robotHalfSize ) {
				wallName = "top";
			}
		}
		dbg(dbg_noise, "Wall name = " + wallName);
		return wallName;
	}

	public double distanceToWallAhead() {
		double angle=getHeading(); 
		double velocity=getVelocity();
		dbg(dbg_noise, "Our velocity = " + velocity);
		double dist=0;

		String wallName = whichWallAhead();

		if ( wallName.equals("left") ) {
				dist = myCoord.x;
		}	
		if ( wallName.equals("right") ) {
				dist = BattleField.x - myCoord.x;
		}
		if ( wallName.equals("bottom") ) {
				dist = myCoord.y;
		}
		if ( wallName.equals("top") ) {
				dist = BattleField.y - myCoord.y;
		}
		dist = dist - robotHalfSize;
		dist = Math.max(dist,0);
		if (dist < 1) dist = 0 ;
		dbg(dbg_noise, "distance to closest wall ahead " + dist);
		return dist;
	}

	public double whichWayToRotateAwayFromWall() {
		double angle = getHeading();
		String wallName = whichWallAhead();

		if ( getVelocity() < 0 ) 
			angle += 180; // we are moving backwards
		angle = shortest_arc(angle);
	        double x = myCoord.x;
	        double y = myCoord.y;
		int rotDir = 1;
		double retAngle=0;

		dbg(dbg_noise, "heading angle = " + angle);

		if ( wallName.equals("left") ) {
			if ( -90 <= angle && angle <= 0 ) {
				retAngle = -angle;
			} else {
				retAngle = -180 - angle;
			}
		}
		if ( wallName.equals("right") ) {
			if ( 0 <= angle && angle <= 90 ) {
				retAngle = - angle;
			} else {
				retAngle = 180 - angle;
			}
		}
		if ( wallName.equals("bottom") ) {
			if ( 90 <= angle && angle <= 180 ) {
				retAngle = 90 - angle;
			} else {
				retAngle = -90 - angle;
			}
		}
		if ( wallName.equals("top") ) {
			if ( 0 <= angle && angle <= 90 ) {
				retAngle =  90-angle;
			} else {
				retAngle = -90 - angle;
			}
		}

		//retAngle += 20*desiredBodyRotationDirection; // add a bit of momentum
		dbg(dbg_noise, "body heading = " + angle);
		dbg(dbg_noise, "rotation from wall is " + retAngle);
		return retAngle;
	}

	public double shortestTurnRadiusVsSpeed() {
		// very empiric for full speed
		return 115;
	}

	public double distTo(double x, double y) {
		double dx=x-myCoord.x;
		double dy=y-myCoord.y;

		return Math.sqrt(dx*dx + dy*dy);
	}

	public int stopDistance( double velocity ) {
		int dist =0;
		if (Math.abs(velocity) == 0 ) 
			dist = 0;
		if (Math.abs(velocity) == 1 ) 
			dist =1;
		if (Math.abs(velocity) == 2 ) 
			dist =2;
		if (Math.abs(velocity) == 3 ) 
			dist =3+1;
		if (Math.abs(velocity) == 4 ) 
			dist =4+2;
		if (Math.abs(velocity) == 5 ) 
			dist =5+3+1;
		if (Math.abs(velocity) == 6 ) 
			dist =6+4+2;
		if (Math.abs(velocity) == 7 ) 
			dist =7+5+3+1;
		if (Math.abs(velocity) == 8 ) 
			dist =8+6+4+2;
		dist=-dist*sign(velocity);
		return dist;
	}

	public boolean isBotMovingClockWiseWithRespectToPoint (double px, double py) {
		double x=myCoord.x;
		double y=myCoord.y;
		double a=getHeadingRadians();
		double vx=getVelocity()*Math.sin(a);
		double vy=getVelocity()*Math.cos(a);
		
		// radius vectors from the center of the battle fields
		double rx1=x-px;
		double ry1=y-py;

		double rx2=x+vx - px;
		double ry2=y+vy - py;

		// recall linear algebra: 
		// sign of z-component of vector product related to rotation direction
		// positive z component means CCW rotation
		double z=rx1*ry2 - ry1*rx2;
		if (z >= 0) {
			return false;
		}
		return true;
	}


	public void moveOrTurn(double dist, double suggestedAngle) {
		double angle=0;
		double moveLength;
		double shortestTurnRadius=shortestTurnRadiusVsSpeed();
		double hardStopDistance = 20;
		calculateSticksEndsPosition();
		String wallAhead = whichWallAhead();
		String furtherestStick = whichStickIsFurtherFromWalls();
		double distFromStickEndToWall = distToTheClosestWallFromStick(furtherestStick);
		dbg(dbg_noise, "furtherestStick = " + furtherestStick );

		double evadeWallDist = shortestTurnRadius+45;
		double wallAheadDist;
		wallAheadDist = distanceToWallAhead();
		dbg(dbg_noise, "moveOrTurn suggested dist =  " + dist + ", angle =" + suggestedAngle);
		dbg(dbg_noise, "hardStopDistance =  " + hardStopDistance);
		dbg(dbg_noise, "Wall ahead is " + wallAhead );
		dbg(dbg_noise, "wallAheadDist =  " + wallAheadDist);
		dbg(dbg_noise, "getDistanceRemaining =  " + getDistanceRemaining());
		dbg(dbg_noise, "rotate away from a wall by " + whichWayToRotateAwayFromWall() );
		dbg(dbg_noise, "Robot velocity =  " + getVelocity());
		if (wallAheadDist < hardStopDistance ) {
			// wall is close trying to stop
			dbg(dbg_noise, "Wall ahead is " + wallAhead );
			angle = whichWayToRotateAwayFromWall();
			executingWallEvadingTurn=true;
			if ( Utils.isNear(getVelocity(),0) ) {
					dbg(dbg_noise, "Wall is too close, backward is faster");
					dist = -41;
					setTurnRight(0);
			} else {
				dist = stopDistance(getVelocity());
				dbg(dbg_noise, "Robot velocity =  " + getVelocity());
				dbg(dbg_noise, "Trying to stop by setting distance = " + dist);
			}
			setAhead(dist); // this is emergency stop or hit a wall
			return;

			//dist = -dist; // hard stop and reverse
			//angle = 0; // do not rotate

		} 
		if (  distFromStickEndToWall <= evadeWallDist && wallAheadDist <= evadeWallDist ){
				// make hard turn
				dbg(dbg_noise, "Trying to turn away from walls" );
				executingWallEvadingTurn = true;
				if ( furtherestStick.equals("starboard") ) {
					angle = 20*sign( getVelocity() );
					//dist  = getDistanceRemaining();
				} else {
					angle = -20*sign( getVelocity() );
					//dist  = getDistanceRemaining();
				}
				if (getVelocity() == 0 ) {
					// if bot velocity is 0 give it a kick
					angle =0; 
					dist = -41;
				}
				setAhead (dist);
				setTurnRight(angle);
				return;
		} 
		
		//if ( distFromStickEndToWall > evadeWallDist) {
			executingWallEvadingTurn = false;
			previoslyHeadedWall = "none";
			dbg(dbg_noise, "getDistanceRemaining = " + getDistanceRemaining());
			//if (  Math.abs(getDistanceRemaining()) <=  0 ) {
				dbg(dbg_noise, "Proceeding with suggested motion");
				angle = suggestedAngle;
				dist = dist;
			//} else {
				//dbg(dbg_noise, "Continue previous turn motion");
				//dist =getDistanceRemaining();
				//angle = 0;
			//}
		//}
		dbg(dbg_noise, "Moving by " + dist);
		dbg(dbg_noise, "Turning by " + angle);
		setTurnRight(angle);
		setBodyRotationDirection( sign(angle) );
		setAhead(dist);
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


	public void  choseGun( ) {
		double rnd;
		// let's choose the gun if gun is fired
		if ( _gun.isGunFired() ) {
			_gun = new linearGun(this); //default gun
			if (getOthers() < 3 ) {
				// only survivors are smart and we had to do random gun
				rnd=Math.random();
				if ( rnd > 0.5 ) { 
					// random choice of future target velocity
					_gun = new randomGun(this); //default gun
				}
			}
		}

		if ( _gun.getName().equals("linear") ) {
			_gun.setTargetFuturePosition(_trgt);
		}

		if ( _gun.getName().equals("random") ) {
			if ( _gun.isGunFired() ) {
				gunFired = false;
				_gun.setTargetFuturePosition(_trgt);
			} else {
				// no need to update future coordinates before gun fire
			}
		}


		dbg(dbg_rutine, "Gun choice = " + _gun.getName());

	}
	
	public double angleToClosestCorner() {
		double x=myCoord.x;
		double y=myCoord.y;
		// corner coordinates
		double cX=0; 
		double cY=0; 
		
		if (x <= BattleField.x/2) {
			// left corner is closer
			cX=0;
		} else {
			// left corner is closer
			cX=BattleField.x;
		}
		if (y <= BattleField.y/2) {
			// lower corner is closer
			cY=0;
		} else {
			// upper corner is closer
			cY=BattleField.y;
		}
		dbg(dbg_noise, "the closest corner is at " + cX + ", " + cY);
		return bearingTo(cX,cY);
	}

	public double bearingTo( double ptx, double pty ) {
		return shortest_arc(
				cortesian2game_angles( Math.atan2( pty-myCoord.y, ptx-myCoord.x )*180/Math.PI )
				);
		//return shortest_arc ( Math.atan2( pty-myCoord.y, ptx-myCoord.x )*180/Math.PI );
	}

	public void makeMove() {
		double angle = nonexisting_coord;
		double angleRandDeviation = nonexisting_coord;
		double dist = nonexisting_coord;
		dbg(dbg_noise, "Normal motion algorithm");
		if (getOthers()>=5 && Math.random() < 0.2 ) { 
			//move to the closest corner as long as there are a lot of bots
			double angle2corner = angleToClosestCorner();
			dbg(dbg_noise, "angle to the closest corner = " + angle2corner );
			angle = shortest_arc( angle2corner - getHeading());
			dist = 50;
			if ( Math.abs(angle) > 90 ) {
				// moving backwards is faster sometimes
				angle = shortest_arc(angle - 180);
				dist = -dist;
			}
			dbg(dbg_noise, "moving to the closest corner with rotation by " + angle );
		}
		if ( haveTarget && (getOthers() <= 1) && (Math.random() < 0.95) ) {
			// last enemy standing lets spiral in
			angle = shortest_arc( -90 + (angle2enemy - getHeading() ) );
			if ( Math.abs(angle) > 90 ) {
				if (angle > 0) {
					angle = angle - 180;
				} else {
					angle = angle + 180;
				}
			}
			if ( (Math.random() < 0.10) ) {
				dbg(dbg_noise, "setting a new motion" );
				dist=200*(0.5-Math.random());
				// but we need to move at least a half bot body
				if (Math.abs(dist) < 50) {
					dist += 50*sign(dist);
				}
			} else {
				dbg(dbg_noise, "continue previous motion" );
				dist = getDistanceRemaining();
			}
			dbg(dbg_noise, "circle around last enemy by rotating = " + angle );
		} 

		if ( getOthers() > 1 && (Math.random() < 0.95) ) {
			dist = getDistanceRemaining();
			angle = getTurnRemaining();
			if ( Math.abs(dist) > 20 ) {
				dbg(dbg_noise, "continue previous motion" );
			} else {
				angleRandDeviation=45*sign(0.5-Math.random());
				dist=100*sign(0.6-Math.random());
				angle =  angleRandDeviation;
			}
		} 

		if ( dist == nonexisting_coord && angle == nonexisting_coord ) {
			// make preemptive evasive motion
			angleRandDeviation=25*sign(0.5-Math.random());
			dist=100*sign(0.6-Math.random());
			angle =  angleRandDeviation;
			dbg(dbg_noise, "Random evasive motion");
		}

		moveOrTurn(dist, angle);
	}

	public void run() {
		double dx=0;
		double dy=0;
		botVersion botVer = new botVersion();
		double angle = nonexisting_coord;
		double firePower=0;
		double bulletFlyTimeEstimate;
		double moveLength;
		double radarBearingToEnemy=0;
		double dist = nonexisting_coord;
		double angleRandDeviation = nonexisting_coord;

		initBattle();

		while(true) {
			initTic() ;
			// Replace the next 4 lines with any behavior you would like
			dbg(dbg_rutine, "----------- Bot version: " + botVer.getVersion() + "------- Tic # " + getTime() + " -------------");
			dbg(dbg_noise, "Game time: " + getTime());
			dbg(dbg_noise, "Number of other bots = " + getOthers());

			if ( ( getTime() - _trgt.getLastSeenTime() ) > 2) 
				targetUnlocked = true;
			else
				targetUnlocked = false;


			dbg(dbg_noise, "targetUnlocked = " + targetUnlocked);

			if (haveTarget) {
				//angle to enemy
				dx=targetLastX - (myCoord.x);
				dy=targetLastY - (myCoord.y);
				dbg(dbg_noise, "Last known target X coordinate = " + targetLastX );
				dbg(dbg_noise, "Last known target Y coordinate = " + targetLastY );

				angle2enemy=Math.atan2(dy,dx);
				angle2enemy=cortesian2game_angles(angle2enemy*180/Math.PI);
				dbg(dbg_noise, "angle to enemy = " + angle2enemy );


				// estimate future enemy location
				choseGun();
				_gun.calcGunSettings();

				dbg(dbg_noise, "Predicted target X coordinate = " + _gun.getTargetFuturePosition().x );
				dbg(dbg_noise, "Predicted target Y coordinate = " + _gun.getTargetFuturePosition().y );

				dx=_gun.getTargetFuturePosition().x - (int)(myCoord.x);
				dy=_gun.getTargetFuturePosition().y - (int)(myCoord.y);

				angle2enemyInFutire=Math.atan2(dy,dx);
				angle2enemyInFutire=cortesian2game_angles(angle2enemyInFutire*180/Math.PI);

			}

			makeMove();


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

				double predictedBulletDeviation=angle*Math.PI/180*_trgt.getLastDistance(myCoord);

				dbg(dbg_noise, "Gun heat = " + getGunHeat() );
				// if gun is called and
				// predicted bullet deviation within half a body size of the robot
				if (getGunHeat() == 0 && 
				    Math.abs(predictedBulletDeviation) < Math.min( getHeight(), getWidth())/2 ) {
					dbg(dbg_noise, "Firing the gun with power = " + firePower);
					_gun.fireGun();
					countFullSweepDelay = -1; // we can sweep do full radar sweep
					gunFired = true;
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
		myCoord.x = getX();
	       	myCoord.y = getY();

		if ( !e.getName().equals(_trgt.getName()) && (_trgt.getLastDistance(myCoord) < e.getDistance()) ) {
			//new target is further then old one
			//we will not switch to it
			return; 
		}

		// Calculate the angle to the scanned robot
		double angle = (getHeading()+ e.getBearing())/360.*2.*Math.PI;

		_trgt.setName(e.getName());
		_trgt = _trgt.update( new botStatPoint(this, e));
		dbg(dbg_noise, _trgt.format());

		// show scanned bot path
		PaintRobotPath.onPaint(getGraphics(), e.getName(), getTime(), _trgt.getX(), _trgt.getY(), Color.YELLOW);

		// need to go away
		targetLastX = _trgt.getX();
		targetLastY = _trgt.getY();

		movingRadarToLastKnownTargetLocation = false;
		//radarSpinDirection=1;
		haveTarget = true;
		//targetUnlocked = true;
		dbg(dbg_noise, "Found target");
	}

	/**
	 * onHitByBullet: What to do when you're hit by a bullet
	 */
	public void onHitByBullet(HitByBulletEvent e) {
		//double angle = shortest_arc( 90 - e.getBearing() );
		//dbg(dbg_noise, "Evasion maneuver after a hit by rotating body by angle = " + angle);
		//dbg(dbg_noise, "Attempting to move ahead for bullet evasion");
		//setTurnLeft(angle);
		//moveOrTurn(100,angle);
		//targetUnlocked=true;

	}

	public void onRobotDeath(RobotDeathEvent e) {
		if (e.getName().equals(_trgt.getName())) {
			haveTarget = false;
			targetUnlocked = false;
			_trgt = new target();
		}
	}


	public void onHitWall(HitWallEvent e) {
		// turn and move along the hit wall
		dbg(dbg_noise, "ROBOT HIT A WALL");
		/*
		double angle = whichWayToRotateAwayFromWall();
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
		dbg(dbg_noise, "Changing course after wall is hit  by angle = " + angle);
		setTurnRight (angle);
		setBodyRotationDirection( sign(angle) );
		*/
	}
		
	public double setBodyRotationDirection( double dir ) {
		desiredBodyRotationDirection = dir; // sets global var
		return desiredBodyRotationDirection;
	}

	
	public void onPaint(Graphics2D g) {
		// Set the paint color to a red half transparent color
		if (haveTarget ) {
			// show our own path
			PaintRobotPath.onPaint(g, getName(), getTime(), myCoord.x, myCoord.y, Color.GREEN);
			// show estimated future position to be fired
			dbg(dbg_noise, "Gun choice = " + _gun.getName() );
			_gun.onPaint(g);
		}

		dbg(dbg_noise, "targetUnlocked = " + targetUnlocked);
		if ( haveTarget && targetUnlocked ) {
			g.setColor(Color.yellow);
			g.drawOval((int) (myCoord.x - 50), (int) (myCoord.y - 50), 100, 100);
		}
		if ( haveTarget && !targetUnlocked ) {
			g.setColor(Color.red);
			g.drawOval((int) (myCoord.x - 50), (int) (myCoord.y - 50), 100, 100);
		}
		// draw starboard and port side sticks
		if (false) {
			// show starboard and port sticks with little circles at the ends
			calculateSticksEndsPosition();
			g.setColor(Color.green);
			g.drawLine((int) starboardStickX, (int) starboardStickY, (int)myCoord.x, (int)myCoord.y);
			g.drawOval((int) starboardStickX -5, (int) starboardStickY-5, 10, 10);
			g.setColor(Color.red);
			g.drawLine((int) portStickX, (int) portStickY, (int)myCoord.x, (int)myCoord.y);
			g.drawOval((int) portStickX-5, (int) portStickY-5, 10, 10);

			//draw possible shortest turn radius paths
			g.setColor(Color.green);
			g.drawOval((int) (starboardStickX - shortestTurnRadiusVsSpeed()), (int) (starboardStickY - shortestTurnRadiusVsSpeed()), (int) (2*shortestTurnRadiusVsSpeed()), (int) (2*shortestTurnRadiusVsSpeed()));
			g.setColor(Color.red);
			g.drawOval((int) (portStickX - shortestTurnRadiusVsSpeed()), (int) (portStickY - shortestTurnRadiusVsSpeed()), (int) (2*shortestTurnRadiusVsSpeed()), (int) (2*shortestTurnRadiusVsSpeed()));
		}

	}

}
