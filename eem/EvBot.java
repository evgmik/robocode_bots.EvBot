package eem;
import eem.misc.*;
import eem.botVersion.*;
import eem.gun.*;
import eem.target.*;
import eem.radar.*;
import eem.motion.*;
import eem.bullets.*;
import java.io.IOException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Random;
import java.util.*;
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
	public int roundCnt = 0;
	int nonexisting_coord = -10000;
	public int totalNumOfEnemiesAtStart = 0;
	public static int roundsWon = 0;
	public static int roundsLost = 0;
	public static int  finishingPlacesStats[] = null;
	public static int  skippedTurnStats[] = null;
	public static int bulletFiredCnt = 0;
        public static int bulletHitCnt = 0;	
        public static int bulletHitByPredictedCnt = 0;	
	private static int numTicsWhenGunInColdState = 0;

	private botVersion botVer;
	public target _trgt;
	private baseGun _gun;
	public radar _radar;
	public basicMotion _motion;
	public bulletsManager _bmanager;
	public gunManager _gmanager;
	public botsManager _botsmanager;
	public static InfoBot _tracker; // track my own status

	public int numEnemyBotsAlive = 1; // we have at least one enemy in general
	public long initTicStartTime = 0;


	public Point2D.Double myCoord;
	public Point2D.Double BattleField;
	double absurdly_huge=1e6; // something huge
	double desiredBodyRotationDirection = 0; // our robot body desired angle

	// logger staff
	private String logFileName = "EvBot.log";
	public int verbosity_level=logger.log_debuging; // current level, smaller is less noisy
	private static RobocodeFileWriter fileWriter = null;
	private boolean appendToLogFlag = false; // TODO: make use of it
	public logger _log = null;

	public EvBot() {
	}

	public void initBattle() {
		if ( fileWriter == null ) {
			try {
				fileWriter = new RobocodeFileWriter( this.getDataFile( logFileName ) );
				_log = new logger(verbosity_level, fileWriter);
			} catch (IOException ioe) {
				System.out.println("Trouble opening the logging file: " + ioe.getMessage());
				_log = new logger(verbosity_level);
			}
		}

		roundCnt = getRoundNum() + 1;
		logger.routine("=========== Round #" + (roundCnt) + "=============");

		BattleField = new Point2D.Double(getBattleFieldWidth(), getBattleFieldHeight());
		physics.init(this); // BattleField must be set
		math.init(this); // BattleField must be set

		myCoord = new Point2D.Double( getX(), getY() );

		setColors(Color.red,Color.blue,Color.green);
		botVer = new botVersion();

		totalNumOfEnemiesAtStart = getOthers();
		if ( finishingPlacesStats == null ) {
			finishingPlacesStats = new int[totalNumOfEnemiesAtStart+1];
		}

		if ( skippedTurnStats == null ) {
			skippedTurnStats = new int[getNumRounds()];
		}

		_trgt = new target();
		_radar = new radar(this);
		_motion = new dangerMapMotion(this);
		//_motion = new safestPathMotion(this); // FIXME slow
		//_motion = new chaoticMotion(this); // FIXME do not use freezes near walls
		_bmanager = new bulletsManager(this);
		_gmanager = new gunManager(this);
		_gun = _gmanager.getDefaultGun();
		_botsmanager = new botsManager(this);
		if ( _tracker == null ) {
			// tracker keep stats for guess factors so I need it permanent
			_tracker = new InfoBot(getName());
		}

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

		ticTime = this.getTime();

		logger.noise("----------- Bot version: " + botVer.getVersion() + "------- Tic # " + ticTime + " -------------");
		logger.profiler("===> time between initTics =        \t\t\t" + ( startTime - initTicStartTime ) + " ns" );
		initTicStartTime = startTime;
		logger.noise("Game time: " + ticTime);
		logger.noise("Number of other bots = " + numEnemyBotsAlive);
		
		if ( numEnemyBotsAlive == 0 ) {
			//logger.dbg("Round is over");
			return;
		}

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
		if (_gun.getNumTicsInColdState() > 1 ) {
			numTicsWhenGunInColdState++;
			//logger.dbg("gun is cold");
		}
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

	public long getTime() {
		// Robocode start every round with zero
		// to keep our own time increasing along the battle
		// we add to this time 100000*round_number
		// this big enough to separate rounds 
		return ( super.getTime() + 100000*(getRoundNum()+1) ); 
	}

	public String fightType() {
		double survRatio = 1.0*numEnemyBotsAlive/totalNumOfEnemiesAtStart;
		if ( (numEnemyBotsAlive == 1) && (totalNumOfEnemiesAtStart == 1) )
			return "1on1";
		if ( (numEnemyBotsAlive == 1) && (totalNumOfEnemiesAtStart != 1) )
			return "melee1on1";
		if ( survRatio > 2./3. )
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
			long mainLoopStartTime = System.nanoTime();
			long endTime;
			long startTime;
			initTic() ;

			if ( numEnemyBotsAlive == 0 ) {
				//logger.dbg("Round is over");
				setAhead(0);
				setStop();
				execute();
				continue;
			}


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

			endTime = System.nanoTime();
			logger.profiler("Main loop execution time =\t\t\t\t" + (endTime - mainLoopStartTime) + " ns" );
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
		long startTime = System.nanoTime();
		myCoord.x = getX();
	       	myCoord.y = getY();

		_botsmanager.onScannedRobot(e);
		_radar.onScannedRobot(e);
		long endTime = System.nanoTime();
		logger.profiler("EvBot.onScannedRobot execution time =\t\t\t\t" + (endTime - startTime) + " ns" );
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
		//logger.dbg("Hit by bullet at tic " +  this.ticTime );
		_botsmanager.onHitByBullet(e);
	}

	public void  onBulletHit(BulletHitEvent e) {
		LinkedList<baseGun> luckyGunsList = null;
		Bullet b;	
		bulletHitCnt++;
		logger.noise("Yey, we hit someone already " + bulletHitCnt);
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
		luckyGunsList = _bmanager.whichOfMyGunsFiredBullet(b);
		if ( luckyGunsList.size() == 0 ) {
			logger.dbg("Weird, hit bullet does not known its gun");
			return;
		}
		for ( baseGun tmp_gun : luckyGunsList ) {
			logger.noise("This gun " + tmp_gun.getName() + " was fired " + tmp_gun.getBulletVirtFiredCount() + " times" );
			// FIXME differentiate real hits
			//tmp_gun.incBulletHitCount();
		}
	}

	public void  onBulletMissed(BulletMissedEvent e) {
		LinkedList<baseGun> luckyGunsList = null;
		if ( true ) return;
		// no need to trace this event it is not used
		logger.noise("Ups, our bullet missed");
		if ( e.getBullet() == null ) {
			logger.dbg("Weird, our missed bullet is not known to event");
			return;
		}
		luckyGunsList = _bmanager.whichOfMyGunsFiredBullet(e.getBullet());
		if ( luckyGunsList.size() == 0 ) {
			logger.dbg("Weird, missed bullet does not known its gun");
			return;
		}
		for ( baseGun tmp_gun : luckyGunsList ) {
			logger.noise("This gun " + tmp_gun.getName() + " was fired " + tmp_gun.getBulletVirtFiredCount() + " times" );
		}
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
		double energyDrop = _tracker.getLast().getEnergy()-getEnergy();
		logger.dbg("FIXME SLOPPY PROGRAMMING: robot hit a wall with energy drop = " + energyDrop);
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

	public void onSkippedTurn(SkippedTurnEvent e) {
		skippedTurnStats[getRoundNum()]++;
		//logger.dbg("Skipped turns stats: " + Arrays.toString(skippedTurnStats) );
	}
	
	public void onPaint(Graphics2D g) {
		// Set the paint color to a red half transparent color
		if (_trgt.haveTarget ) {
			// show our own path
			//PaintRobotPath.onPaint(g, getName(), getTime(), myCoord.x, myCoord.y, Color.GREEN);
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

	public void onWin(WinEvent  e) {
		//logger.dbg("onWin");
		roundsWon++;
		updateFinishingPlacesStats();
		winOrLoseRoundEnd();
	}

	public void onDeath(DeathEvent e ) {
		//logger.dbg("onDeath");
		roundsLost++;
		updateFinishingPlacesStats();
		winOrLoseRoundEnd();
	}

	public void onRoundEnded(RoundEndedEvent e) {
		// this methods is called before onDeath or onWin
		// so we should not output any valiable stats here
		// if I want to see it at the end
		//logger.dbg("onRoundEnded");
		//winOrLoseRoundEnd();
	}

	public void updateFinishingPlacesStats() {
		int myWinLosePlace = getOthers();
		finishingPlacesStats[myWinLosePlace]++;
	}

	public void winOrLoseRoundEnd() {
		_gmanager.printGunsStats();
		_botsmanager.printGunsStats();
		logger.routine("Rounds ratio of win/lose = " + roundsWon + "/" + roundsLost );
		logger.routine("Skipped turns stats: " + Arrays.toString(skippedTurnStats) );
		logger.routine("The gun was cold " + numTicsWhenGunInColdState + " tics");
		logger.routine("Finishing places stats: " + Arrays.toString( finishingPlacesStats ) );
	}

	public baseGun getGun() {
		return this._gun;
	}

}
