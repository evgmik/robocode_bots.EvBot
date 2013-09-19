package eem;
import eem.misc.*;
import eem.botVersion.*;
import eem.gun.*;
import eem.target.*;
import eem.radar.*;
import eem.motion.*;
import eem.bullets.*;
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
	public Rules game_rules;
	double BodyTurnRate = 10;
	public int robotHalfSize = 18;
	public long ticTime;
	public target _trgt = new target();
	int nonexisting_coord = -10000;

	private baseGun _gun = new linearGun(this);
	public radar _radar = new radar(this);
	private botVersion botVer;
	private basicMotion _motion = new basicMotion(this);
	public bulletsManager _bmanager = new bulletsManager(this);


	public Point2D.Double myCoord = new Point2D.Double(nonexisting_coord, nonexisting_coord);
	public Point2D.Double BattleField = new Point2D.Double(nonexisting_coord, nonexisting_coord);
	double absurdly_huge=1e6; // something huge
	double desiredBodyRotationDirection = 0; // our robot body desired angle

	// logger staff
	public int verbosity_level=logger.log_debuging; // current level, smaller is less noisy
	public logger _log = new logger(verbosity_level);


	public void initBattle() {
		BattleField.x = getBattleFieldWidth();
		BattleField.y = getBattleFieldHeight();

		setColors(Color.red,Color.blue,Color.green);
		botVer = new botVersion();
	}

	public void initTic() {
		ticTime = getTime();

		logger.routine("----------- Bot version: " + botVer.getVersion() + "------- Tic # " + ticTime + " -------------");
		logger.noise("Game time: " + ticTime);
		logger.noise("Number of other bots = " + getOthers());

		myCoord.x = getX();
	       	myCoord.y = getY();
		_trgt.initTic(ticTime);
		_bmanager.initTic();
		_gun.initTic();
		_radar.initTic();
	}

	public void dbg(int level, String s) {
		if (level <= verbosity_level)
			System.out.println(s);
	}

	public double distTo(double x, double y) {
		double dx=x-myCoord.x;
		double dy=y-myCoord.y;

		return Math.sqrt(dx*dx + dy*dy);
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


	public void  choseMotion( ) {
		//_motion = new basicMotion(this);
		_motion = new chaoticMotion(this);
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
				_gun.setTargetFuturePosition(_trgt);
			} else {
				// no need to update future coordinates before gun fire
			}
		}


		logger.routine("Gun choice = " + _gun.getName());

	}
	

	public void run() {
		initBattle();

		while(true) {
			initTic() ;

			if (_trgt.haveTarget) {
				choseGun();
			}

			choseMotion();
			_motion.makeMove();

			_gun.manage();
			_radar.manage();

			execute();
		}
	}

	public double angle2target() {
		return math.angle2pt( myCoord, _trgt.getPosition());
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
		logger.noise(_trgt.format());

		// show scanned bot path
		PaintRobotPath.onPaint(getGraphics(), e.getName(), getTime(), _trgt.getX(), _trgt.getY(), Color.YELLOW);


		_radar.setMovingRadarToLastKnownTargetLocation(false);
		//radarSpinDirection=1;
		//_trgt.targetUnlocked = true;
		logger.noise("Target seen during radar sweep");
	}

	/**
	 * onHitByBullet: What to do when you're hit by a bullet
	 */
	public void onHitByBullet(HitByBulletEvent e) {
		//double angle = math.shortest_arc( 90 - e.getBearing() );
		//logger.noise("Evasion maneuver after a hit by rotating body by angle = " + angle);
		//logger.noise("Attempting to move ahead for bullet evasion");
		//setTurnLeft(angle);
		//moveOrTurn(100,angle);
		//_trgt.targetUnlocked=true;

	}

	public void onRobotDeath(RobotDeathEvent e) {
		if (e.getName().equals(_trgt.getName())) {
			_trgt.targetUnlocked = false;
			_trgt = new target();
		}
	}


	public void onHitWall(HitWallEvent e) {
		// turn and move along the hit wall
		logger.noise("ROBOT HIT A WALL");
		/*
		double angle = whichWayToRotateAwayFromWall();
		if ( _trgt.haveTarget ) {
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
		logger.noise("Changing course after wall is hit  by angle = " + angle);
		setTurnRight (angle);
		setBodyRotationDirection( math.sign(angle) );
		*/
	}
		
	public double setBodyRotationDirection( double dir ) {
		desiredBodyRotationDirection = dir; // sets global var
		return desiredBodyRotationDirection;
	}

	
	public void onPaint(Graphics2D g) {
		// Set the paint color to a red half transparent color
		if (_trgt.haveTarget ) {
			// show our own path
			PaintRobotPath.onPaint(g, getName(), getTime(), myCoord.x, myCoord.y, Color.GREEN);
			// show estimated future position to be fired
			logger.noise("Gun choice = " + _gun.getName() );
			_gun.onPaint(g);
		}

		logger.noise("targetUnlocked = " + _trgt.targetUnlocked);
		if ( _trgt.haveTarget && _trgt.targetUnlocked ) {
			g.setColor(Color.yellow);
			g.drawOval((int) (myCoord.x - 50), (int) (myCoord.y - 50), 100, 100);
		}
		if ( _trgt.haveTarget && !_trgt.targetUnlocked ) {
			g.setColor(Color.red);
			g.drawOval((int) (myCoord.x - 50), (int) (myCoord.y - 50), 100, 100);
		}

		_motion.onPaint(g);
		_bmanager.onPaint(g);

	}

}
