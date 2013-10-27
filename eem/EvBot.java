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
	int nonexisting_coord = -10000;
	public int totalNumOfEnemiesAtStart = 0;
	public static int roundsWon = 0;
	public static int roundsLost = 0;

	private botVersion botVer;
	public target _trgt;
	private baseGun _gun;
	public radar _radar;
	private basicMotion _motion;
	public bulletsManager _bmanager;
	public gunManager _gmanager;
	public botsManager _botsmanager;
	public InfoBot _tracker; // track my own status

	public int numEnemyBotsAlive = 1; // we have at least one enemy in general
	public long initTicStartTime = 0;


	public Point2D.Double myCoord;
	public Point2D.Double BattleField;
	double absurdly_huge=1e6; // something huge
	double desiredBodyRotationDirection = 0; // our robot body desired angle

	// logger staff
	public int verbosity_level=logger.log_debuging; // current level, smaller is less noisy
	public logger _log = new logger(verbosity_level);


	public void initBattle() {
		BattleField = new Point2D.Double(getBattleFieldWidth(), getBattleFieldHeight());
		myCoord = new Point2D.Double( getX(), getY() );

		setColors(Color.red,Color.blue,Color.green);
		botVer = new botVersion();

		totalNumOfEnemiesAtStart = getOthers();

		_trgt = new target();
		_gun = new linearGun(this);
		_radar = new radar(this);
		_motion = new dangerMapMotion(this);
		_bmanager = new bulletsManager(this);
		_gmanager = new gunManager(this);
		_botsmanager = new botsManager(this);
		_tracker = new InfoBot(getName());

		initTicStartTime = System.nanoTime();
	}

	public void initTic() {
		long startTime = System.nanoTime();
		long endTime;
		

		numEnemyBotsAlive = getOthers();

		// gun, radar, and body are decoupled
		setAdjustRadarForRobotTurn(true);
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true); 

		ticTime = getTime();

		logger.noise("----------- Bot version: " + botVer.getVersion() + "------- Tic # " + ticTime + " -------------");
		logger.profiler("===> time between initTics =        \t\t\t" + ( startTime - initTicStartTime ) + " ns" );
		initTicStartTime = startTime;
		logger.noise("Game time: " + ticTime);
		logger.noise("Number of other bots = " + numEnemyBotsAlive);

		myCoord.x = getX();
	       	myCoord.y = getY();

		startTime = System.nanoTime();
		_tracker.update( new botStatPoint( this ) );
		endTime = System.nanoTime();
		logger.profiler("tracker update execution time =     \t\t\t" + (endTime - startTime) + " ns" );
		startTime = System.nanoTime();
		_botsmanager.initTic(ticTime);
		endTime = System.nanoTime();
		logger.profiler("botsmanager initTic execution time =\t\t\t" + (endTime - startTime) + " ns" );
		startTime = System.nanoTime();
		_trgt.initTic(ticTime);
		endTime = System.nanoTime();
		logger.profiler("target initTic execution time =        \t\t\t" + (endTime - startTime) + " ns" );
		startTime = System.nanoTime();
		_bmanager.initTic();
		endTime = System.nanoTime();
		logger.profiler("bullet manager initTic execution time =\t\t\t" + (endTime - startTime) + " ns" );
		startTime = System.nanoTime();
		_motion.initTic();
		endTime = System.nanoTime();
		logger.profiler("motion manager initTic execution time =\t\t\t" + (endTime - startTime) + " ns" );
		startTime = System.nanoTime();
		_gun.initTic();
		endTime = System.nanoTime();
		logger.profiler("gun init Tic execution time  =          \t\t\t" + (endTime - startTime) + " ns" );
		startTime = System.nanoTime();
		_radar.initTic();
		endTime = System.nanoTime();
		logger.profiler("radar init Tic execution time =         \t\t\t" + (endTime - startTime) + " ns" );

		//_gmanager.printGunsStats(); // dbg
		//_gmanager.printGunsStatsTicRelated(); // dbg
		//_botsmanager.printGunsStats(); // dbg
	}

	public String fightType() {
		double survRatio = 1.0*numEnemyBotsAlive/totalNumOfEnemiesAtStart;
		if ( (numEnemyBotsAlive == 1) && (totalNumOfEnemiesAtStart == 1) )
			return "1on1";
		if ( (numEnemyBotsAlive == 1) && (totalNumOfEnemiesAtStart != 1) )
			return "meelee1on1";
		if ( survRatio > 2/3 )
			return "melee";
		return "meleeMidle";
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
		boolean choseNewMotion = false;
		if (choseNewMotion) {
			//_motion = new basicMotion(this);
			//_motion = new chaoticMotion(this);
			_motion = new dangerMapMotion(this);
		}
	}

	public void run() {
		initBattle();

		while(true) {
			long endTime;
			long startTime;
			initTic() ;

			_trgt = _botsmanager.choseTarget() ;

			if (_trgt.haveTarget) {
				_gun=_gmanager.choseGun();
			}

			choseMotion();
			startTime = System.nanoTime();
			_motion.makeMove();
			endTime = System.nanoTime();
			logger.profiler("makeMove execution time     =\t\t\t\t" + (endTime - startTime) + " ns" );

			startTime = System.nanoTime();
			_gun.manage();
			endTime = System.nanoTime();
			logger.profiler("gun manage execution time   =\t\t\t\t" + (endTime - startTime) + " ns" );
			startTime = System.nanoTime();
			_radar.setNeedToTrackTarget( _gun.doesItNeedTrackedTarget() );
			_radar.manage();
			endTime = System.nanoTime();
			logger.profiler("radar manage execution time =\t\t\t\t" + (endTime - startTime) + " ns" );

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

		_botsmanager.onScannedRobot(e);
		_radar.onScannedRobot(e);
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
		_botsmanager.onHitByBullet(e);
	}

	public void  onBulletHit(BulletHitEvent e) {
		baseGun tmp_gun;
		Bullet b;	
		logger.noise("Yey, we hit someone");
		b = e.getBullet();
		if ( b == null ) {
			logger.dbg("Weird, our hit bullet is not known to event");
			return;
		}
		logger.noise("event bullet " + b);
		logger.noise("Bullet activity status is " + b.isActive() );
		if ( b == null ) {
			logger.dbg("Weird, hit bullet does not exists");
			return;
		}
		tmp_gun = _bmanager.whichGunFiredBullet(b);
		if ( tmp_gun == null ) {
			logger.dbg("Weird, hit bullet does not known its gun");
			return;
		}
		logger.noise("This gun was fired " + tmp_gun.getBulletFiredCount() + " times" );
		tmp_gun.incBulletHitCount();
	}

	public void  onBulletMissed(BulletMissedEvent e) {
		baseGun tmp_gun;
		logger.noise("Ups, our bullet missed");
		if ( e.getBullet() == null ) {
			logger.dbg("Weird, our missed bullet is not known to event");
			return;
		}
		tmp_gun = _bmanager.whichGunFiredBullet(e.getBullet());
		if ( tmp_gun == null ) {
		logger.noise("This gun was fired " + tmp_gun.getBulletFiredCount() + " times" );
			logger.dbg("Weird, missed bullet does not known its gun");
			return;
		}
		logger.noise("This gun was fired " + tmp_gun.getBulletFiredCount() + " times" );
	}

	public void onRobotDeath(RobotDeathEvent e) {
		if (e.getName().equals(_trgt.getName())) {
			_trgt = new target();
		}
		_botsmanager.onRobotDeath(e);
		_radar.onRobotDeath(e);
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
		_botsmanager.onPaint(g);

	}

	public void onWin(DeathEvent e ) {
		// looks like it is never executed
		// at least in robocode 1.7.3
		//roundsWon++;
		//winOrLoseRoundEnd();
	}

	public void onDeath(DeathEvent e ) {
		roundsLost++;
		winOrLoseRoundEnd();
	}

	public void onRoundEnded(RoundEndedEvent e) {
		// seems like it is not called if my bot is died
		// at least in robocode 1.7.3
		roundsWon = e.getRound() - roundsLost + 1;
		winOrLoseRoundEnd();
	}

	public void winOrLoseRoundEnd() {
		//_gmanager.printGunsStats();
		_botsmanager.printGunsStats();
		logger.routine("Rounds ratio of win/lose = " + roundsWon + "/" + roundsLost );
	}

	public baseGun getGun() {
		return this._gun;
	}

}
