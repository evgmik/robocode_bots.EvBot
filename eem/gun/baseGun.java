// -*- java -*-

package eem.gun;

import eem.EvBot;
import eem.gun.*;
import eem.target.*;
import eem.bullets.*;
import eem.misc.*;
import java.util.Random;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import robocode.Bullet;
import java.util.HashMap;
import java.util.*;


public class baseGun {
	protected EvBot myBot;
	protected InfoBot firingBot;
	protected String gunName = "base";
	protected boolean  gunFired = false;
	protected boolean  gunHasTargetPoint = false;
	protected Random gun_rand = new Random();
	public    Color gunColor = Color.black;
	protected Point2D.Double targetFuturePosition;
	protected int numTicsInColdState = 0;
	protected double firePower;
	private static int bulletHitCount = 0;
	private static int bulletMissedCount = 0;
	private static int bulletFiredCount = 0;
	private static LinkedList<cachedTarget> cachedTargets = new LinkedList<cachedTarget>();
	public static HashMap<String, gunStats> mapOfGunStats = new HashMap<String, gunStats>();
	private String strSep = "__zzz__";


	private String buildMapKey(InfoBot targetBot, InfoBot firingBot, boolean bulletVirtualState) {
		String bulletType = "";
		if ( bulletVirtualState ) {
			bulletType = "virtual";
		} else {
			bulletType = "real";
		}

		String key = gunName + strSep + firingBot.getName() + strSep + targetBot.getName() + strSep + bulletType;
		return key;
	}

	public int getNumTicsInColdState() {
		return numTicsInColdState;
	}

	public int getBulletVirtFiredCount(InfoBot targetBot, InfoBot firingBot) {
		boolean virtualState = true;
		return getBulletFiredCount(targetBot, firingBot, virtualState);
	}

	public int getBulletVirtFiredCount(InfoBot targetBot) {
		return getBulletVirtFiredCount(targetBot, myBot._tracker);
	}

	public int getBulletVirtFiredCount() {
		return getBulletVirtFiredCount(myBot._trgt, myBot._tracker);
	}

	public int getBulletFiredCount(InfoBot targetBot, InfoBot firingBot, boolean isVirtual) {
		String key = this.buildMapKey( targetBot, firingBot, isVirtual);
		gunStats gS = mapOfGunStats.get(key);
		if (gS == null) {
			return 0;
		}
		return gS.getBulletFiredCount();
	}

	public int getBulletRealFiredCount(InfoBot targetBot) {
		boolean virtualState = false;
		return getBulletFiredCount(targetBot, myBot._tracker, virtualState);
	}

	public int getBulletHitCount(InfoBot targetBot, InfoBot firingBot, boolean isVirtual) {
		String key = this.buildMapKey( targetBot, firingBot, isVirtual);
		gunStats gS = mapOfGunStats.get(key);
		if (gS == null) {
			return 0;
		}
		return gS.getBulletHitCount();
	}

	public int getBulletRealHitCount(InfoBot targetBot) {
		boolean virtualState = false;
		return getBulletHitCount(targetBot, myBot._tracker, virtualState);
	}

	public int getBulletVirtHitCount(InfoBot targetBot, InfoBot firingBot) {
		boolean virtualState = true;
		return getBulletHitCount(targetBot, myBot._tracker, virtualState);
	}

	public int getBulletVirtHitCount(InfoBot targetBot) {
		return getBulletVirtHitCount(targetBot, myBot._tracker);
	}

	public int getBulletVirtHitCount() {
		return getBulletVirtHitCount(myBot._trgt, myBot._tracker);
	}

	public double getGunVirtHitRate(InfoBot targetBot, InfoBot firingBot) {
		boolean isVirtual = true;
		String key = this.buildMapKey( targetBot, firingBot, isVirtual);
		gunStats gS = mapOfGunStats.get(key);
		if (gS == null) {
			gS = new gunStats();
			mapOfGunStats.put(key,gS);
		}
		return gS.getGunHitRate();
	}

	public double getGunVirtHitRate(InfoBot targetBot) {
		return getGunVirtHitRate(targetBot, myBot._tracker);
	}

	public double getGunVirtHitRate() {
		return getGunVirtHitRate(myBot._trgt, myBot._tracker);
	}

	public double getGunVirtPerformance(InfoBot targetBot, InfoBot firingBot) {
		boolean isVirtual = true;
		String key = this.buildMapKey( targetBot, firingBot, isVirtual);
		gunStats gS = mapOfGunStats.get(key);
		if (gS == null) {
			gS = new gunStats();
			mapOfGunStats.put(key,gS);
		}
		return gS.getGunPerformance();
	}

	public double getGunVirtPerformance(InfoBot targetBot) {
		return getGunVirtPerformance(targetBot, myBot._tracker);
	}

	public double getGunVirtPerformance() {
		return getGunVirtPerformance(myBot._trgt, myBot._tracker);
	}

	protected void updBulletFiredCount(InfoBot firingBot, InfoBot targetBot, boolean virtualState) {
		String key = this.buildMapKey( targetBot, firingBot, virtualState);
		gunStats gS = mapOfGunStats.get(key);
		if (gS == null) {
			gS = new gunStats();
			mapOfGunStats.put(key,gS);
		}
		gS.updBulletFiredCount();
	}

	public void updBulletFiredCount(InfoBot firingBot, InfoBot targetBot, firedBullet b) {
		if ( b.isItVirtual() ) {
			updBulletFiredCount( firingBot, targetBot, true);
		} else {
			updBulletFiredCount( firingBot, targetBot, false);
			// for real bullet we still need to update virtual count
			updBulletFiredCount( firingBot, targetBot, true);
		}
	}

	protected void updBulletHitCount(InfoBot firingBot, InfoBot targetBot, boolean virtualState) {
		String key = this.buildMapKey( targetBot, firingBot, virtualState);
		gunStats gS = mapOfGunStats.get(key);
		if (gS == null) {
			gS = new gunStats();
			mapOfGunStats.put(key,gS);
		}
		gS.updBulletHitCount();
	}

	public void updBulletHitCount(InfoBot firingBot, InfoBot targetBot, firedBullet b) {
		if ( b.isItVirtual() ) {
			updBulletHitCount( firingBot, targetBot, true);
		} else {
			// for real bullet we still need to update virtual count
			updBulletHitCount( firingBot, targetBot, true);
			updBulletHitCount( firingBot, targetBot, false);
		}
	}

	public String gunStatsHeader(InfoBot firingBot, InfoBot targetBot) {
		String key = this.buildMapKey( targetBot, firingBot, true);
		gunStats gS = mapOfGunStats.get(key);
		if (gS == null) {
			gS = new gunStats();
			mapOfGunStats.put(key,gS);
		}
		String str="";
		str += String.format( "%12s", "gun name" );
		str += gS.header("Virt"); // virtual gun
		str += gS.header("Real"); // real gun
		str +=  " | ";
		str += String.format( "%20s", "fire rate" );
		return str;
	}

	public String gunStatsFormat(InfoBot firingBot, InfoBot targetBot, boolean virtualState) {
		String key;
	       	key = this.buildMapKey( targetBot, firingBot, virtualState);
		gunStats gS = mapOfGunStats.get(key);
		if (gS == null) {
			gS = new gunStats();
			mapOfGunStats.put(key,gS);
		}
		String str="";
		str += gS.format();
		return str;
	}

	public String gunStatsFormat(InfoBot firingBot, InfoBot targetBot) {
		String str="";
		str += String.format( "%12s", this.getName() );
		str += gunStatsFormat( firingBot, targetBot, true );  // virtual gun
		str += gunStatsFormat( firingBot, targetBot, false ); // real gun
		// now lets calculate firing rate
		str +=  " | ";
		str += logger.hitRateFormat( getBulletFiredCount(targetBot, firingBot, false),
			       getBulletFiredCount(targetBot, firingBot, true) );	
		return str;
	}


	public baseGun() {
	}

	public baseGun(EvBot bot) {
		this();
		myBot = bot;
		firingBot = myBot._tracker;
		calcGunSettings();
	};

	public void initTic() {
		if ( myBot.getGunHeat() == 0 ) {
			numTicsInColdState++;
		}
	}

	public boolean doesItNeedTrackedTarget() {
		if ( physics.gunCoolingTime( myBot.getGunHeat() ) <= 3 ) {
			return true;
		} else {
			return false;
		}
	}

	public void manage() {
		double angle;
		double angle2enemyInFutire;

		if (myBot._trgt.haveTarget) {
			//calculate the gun settings
			this.calcGunSettings();

			long fireDelay = physics.gunCoolingTime( myBot.getGunHeat() );

			Point2D.Double myPosAtFiringTime =  predictBotPositionAtTime( myBot._tracker, myBot.getTime() + fireDelay );
			logger.noise("My predicted location at firing time = " + myPosAtFiringTime);
			logger.noise("Enemy predicted location at bullet hit time = " + this.getTargetFuturePosition());
			// rotate gun directives and settings
			if ( this.getTargetFuturePosition() != null ) {
				angle2enemyInFutire=math.angle2pt( myPosAtFiringTime, this.getTargetFuturePosition());
				double gun_angle =myBot.getGunHeading();
				angle = math.shortest_arc(angle2enemyInFutire-gun_angle);
			} else {
				angle = 0; // no rotation for null future target positions
			}

			logger.noise("Pointing gun to enemy by rotating by angle = " + angle);
			// remember rotation will happen only at the next turn!
			myBot.setTurnGunRight(angle);

			double predictedBulletDeviation=angle*Math.PI/180*myBot._trgt.getLastDistance(myBot.myCoord);
			logger.noise("predicted deviation = " + predictedBulletDeviation );

			logger.noise("Gun heat = " + myBot.getGunHeat() );
			// if gun is called and
			// predicted bullet deviation within half a body size of the robot
			if ( myBot.getGunHeat() == 0 ) {
				if ( Math.abs(predictedBulletDeviation) < Math.min( myBot.getHeight(), myBot.getWidth())/5 ) {
					logger.noise("Firing the gun with power = " + firePower);
					logger.noise("Target at position " + myBot._trgt.getPosition());
					this.fireGun();
					return;
				}
				// TODO tune maxAllowedTicToBeColdForAGun for melee like situations
				// maxAllowedTicToBeColdForAGun = 5 seems to give a strong boost
				// for v4.6.5 in melee against just plain maxAllowedTicToBeColdForAGun=1
				// may be there is a better choice
				int maxAllowedTicToBeColdForAGun = 5; // in melee survival bonus is important
				if ( myBot.fightType().equals("1on1") ) {
					// in 1on1 it is generally better to fire in a hope to hit
					// than wait to be hit and give unspent energy taken by bullet
					// to the enemy

					// TODO check if below is true, I have limited check with 
					// my test bed, and not so sure about results
					// maxAllowedTicToBeColdForAGun = 0, and 2, 3 gives worse rating
					// especially against hard bots,
					// however 0 seems to help against weaker bots
					// see v4.6.4 stats for maxAllowedTicToBeColdForAGun = 1
					// version v4.6.5 has too many changes to be fairly compared
					// see v4.6.5 stats for maxAllowedTicToBeColdForAGun = 0
					// why such strong dependence is unclear for me
					maxAllowedTicToBeColdForAGun = 1; // magically it seems to be the best
				}
			
				if ( numTicsInColdState > maxAllowedTicToBeColdForAGun ) {
					// this is for the case when we stuck with a single gun
					// which cannot chose its target.
					// See voidious.Diamond for example against linear gun.
					// Diamond will oscillate back and force which makes
					// future target position make large swing
					// so gun cannot settle and cannot rotate fast enough
					// to point in predicted place.
					//logger.dbg( "The gun " + getName() + " is not firing for " + numTicsInColdState + " tics, we permit to not fire only for " + maxAllowedTicToBeColdForAGun + ". Hell, with targeting, fire anyway");
					this.fireGun();
					return;
				}
			}
		}
	}

	protected Point2D.Double predictBotPositionAtTime( InfoBot bot, long time ) {
		// this is needed for advance firing when we need to take account in
		// cooling time for our bot or the fact that we detect enemy bullet only
		// one click after its fired.
		// For now I will use simple circular predictor but probably should use
		// a particular gun estimate
		return predictBotPositionAtTimeCircular( bot, time );
	}

	public Point2D.Double predictBotPositionAtTimeLinear( InfoBot bot, double time) {
		double Tx, Ty;
		Point2D.Double vTvec = bot.getVelocity();
		double dt = time-bot.getLastSeenTime();
		Tx = bot.getX() + vTvec.x*dt;
		Ty = bot.getY() + vTvec.y*dt;
		return new Point2D.Double(Tx, Ty);
	}

	public Point2D.Double predictBotPositionAtTimeCircular( InfoBot bot, long time) {
		// FIXME should be able to deal with time in past, see while loop condition
		Point2D.Double posFut  = new Point2D.Double(0,0);
		Point2D.Double vTvecLast, vTvecPrev;
		double phi = 0;
		botStatPoint bStatLast;
		botStatPoint bStatPrev;

		long dT = time - bot.getLastSeenTime();
		if ( dT <= 0 ) {
			// required time in past or present
			// there is a chance we already have this data
			botStatPoint bS = bot.getStatAtTime( time );
			if ( bS != null ) {
				// yey, we have this point
				return (Point2D.Double) bS.getPosition().clone();
			}
		}

		// tough luck, no such point and we need to approximate its position
		bStatLast = bot.getLast();
		bStatPrev = bot.getPrev();

		vTvecLast = bStatLast.getVelocity();
		if ( bStatPrev == null ) {
			phi = 0;
		} else {
			vTvecPrev = bStatPrev.getVelocity();
			double phiLast = Math.atan2( vTvecLast.y, vTvecLast.x);
			double phiPrev = Math.atan2( vTvecPrev.y, vTvecPrev.x);
			double dt =  bStatLast.getTime() - bStatPrev.getTime();
			phi = (phiLast - phiPrev)/dt;
		}
		// estimated current target position

		// if time in past we need to revert time, velocity, and rotation angle phi
		double vx = vTvecLast.x * math.sign( dT );
		double vy = vTvecLast.y * math.sign( dT );

		// rotation coefficients
		double cosPhi = Math.cos(phi * math.sign( dT ) );
		double sinPhi = Math.sin(phi * math.sign( dT ) );

		//finally making dT positive
		dT *= math.sign( dT );

		double vxNew, vyNew;
		posFut.x = bot.getX();
		posFut.y = bot.getY();

		for ( int t = 0; t < dT ; t++) {
			vxNew =  vx * cosPhi - vy * sinPhi;
			vyNew =  vx * sinPhi + vy * cosPhi;
			vx = vxNew;
			vy = vyNew;
			posFut.x = posFut.x + vx;
			posFut.y = posFut.y + vy;
			if ( myBot._motion.shortestDist2wall( posFut ) < (myBot.robotHalfSize-1) ) {
				// bot hit wall and cannot move anymore
				posFut.x = posFut.x - vx;
				posFut.y = posFut.y - vy;
				break;
			}
		}
		return posFut;
	}

	public double getTargetWeight( InfoBot firingBot, InfoBot targetBot, double firePower ) {
		cachedTarget matchedCT = findMatchInCachedTargets( new cachedTarget( myBot, this, firingBot, targetBot ) );
		if ( matchedCT == null ) {
			calcTargetFuturePosition(  firingBot, firePower, targetBot);
			// after above we must have a match
			matchedCT = findMatchInCachedTargets( new cachedTarget( myBot, this, firingBot, targetBot ) );
		}
		return matchedCT.getTargetWeight();
	}

	public void fireGun() {
		if ( firePower != 0 ) {
			// other algorithms decided not to fire
			// for example if we low on energy
			Bullet b;
			logger.noise("Gun fire power = " + firePower);
			b=myBot.setFireBullet(firePower);
			if ( b == null ) {
				logger.error("Gun did not fire  = " + b);
				return;
			}
			logger.noise("fired bullet  = " + b);
			myBot._bmanager.add( new firedBullet( myBot, b, this) );
			gunFired = true;
			resetTicsInColdState();
			gunHasTargetPoint = false;
			//this.incBulletVirtFiredCount();
		}
	}

	public void resetTicsInColdState() {
		numTicsInColdState = 0;
	}

	//public void setTargetFuturePosition( Point2D.Double target ) {
		//targetFuturePosition = math.putWithinBorders( target, myBot.BattleField);
	//};

	public String getName() {
		return gunName;
	}

	public boolean isGunFired() {
		return gunFired;
	}

	public Point2D.Double getTargetFuturePosition() {
		//logger.dbg("aiming at = " + targetFuturePosition );
		return (Point2D.Double) targetFuturePosition;
	}

	private Point2D.Double  addRandomOffsetToTargetFuturePosition(Point2D.Double firingPos, Point2D.Double tFP) {
		//to counter act bullet shielding bots
		//add small random offset of about bot size
		double angle = math.angle2pt( firingPos, tFP);
		angle = Math.toRadians(angle);
		double dist  = firingPos.distance( tFP );
		double offCenterFractDev = 0.05;
		double tgtAngleDev = Math.atan2(offCenterFractDev*myBot.robotHalfSize, dist); // target angle profile
		double r1=Math.random();
		angle += math.signNoZero(r1-0.5)*tgtAngleDev; // offsetting target point from target center
		tFP.x = firingPos.x + dist*Math.sin( angle );
		tFP.y = firingPos.y + dist*Math.cos( angle );
		return (Point2D.Double) tFP.clone();
	}

	public void setTargetFuturePosition(target targetBot) {
		targetFuturePosition = (Point2D.Double)  calcTargetFuturePosition( myBot._tracker, firePower, targetBot);
	}

        protected double firePoverVsDistance( double targetDistance ) {
                // calculate firepower based on distance
		logger.noise("Target distance = " + targetDistance);
                double firePower;
		firePower = -100; // negative means unset
                //firePower = Math.min( 12*Math.exp( -math.sqr( targetDistance/200.0 ) ), 3);
		if ( myBot.fightType().equals("melee") ) {
			//firePower = 3;
			firePower = Math.min( 500/targetDistance, 3);
		}
		if ( myBot.fightType().equals("meleeMidle") ) {
			firePower = Math.min( 500/targetDistance, 3);
		}
		if ( myBot.fightType().equals("meelee1on1") ) {
			firePower = Math.min( 500/targetDistance, 3);
		}
		if ( myBot.fightType().equals("1on1") ) {
			firePower = Math.min( 700/targetDistance, 3);
		}
		if ( firePower < 0 ) {
			// default case
			firePower = Math.min( 500/targetDistance, 3);
		}
                firePower = Math.max( firePower, 0.1);
		logger.noise("Fire power = " + firePower);
                return firePower;
        }

	protected Point2D.Double futureTargetWithinPhysicalLimitsBasedOnVelocity( Point2D.Double ftPos, Point2D.Double tVel ) {
		// robot center cannot approach walls closer than robotHalfSize
		// so we take it in account for future target calculation
		double robotHalfSize = myBot.robotHalfSize;
		Point2D.Double reducedBattleFieldSize = (Point2D.Double) myBot.BattleField.clone();
		Point2D.Double new_ftPos = (Point2D.Double) ftPos.clone();

		// calculate available space for robot center
		reducedBattleFieldSize.x = reducedBattleFieldSize.x - 2*robotHalfSize;
		reducedBattleFieldSize.y = reducedBattleFieldSize.y - 2*robotHalfSize;

		// shift coordinates by robotHalfSize
		new_ftPos.x = new_ftPos.x - robotHalfSize;
		new_ftPos.y = new_ftPos.y - robotHalfSize;
		if ( !math.isItOutOfBorders( new_ftPos, reducedBattleFieldSize ) ) {
			return ftPos;
		}

		if ( tVel.x == 0 ) {
			tVel.x = 1e-6;
		}
		double dist;
		double roundOffError = 2; // small placement error usually due to round off
		// outside left border?
		dist = new_ftPos.x;
		if ( dist < -roundOffError ) {
			new_ftPos.x = new_ftPos.x - dist;
			new_ftPos.y = new_ftPos.y - dist/tVel.x*tVel.y;
		}
		// outside right border?
		dist = new_ftPos.x - reducedBattleFieldSize.x;
		if ( dist > roundOffError ) {
			new_ftPos.x = new_ftPos.x - dist;
			new_ftPos.y = new_ftPos.y - dist/tVel.x*tVel.y;
		}
		if ( tVel.y == 0 ) {
			tVel.y = 1e-6;
		}
		// outside bottom border?
		dist = new_ftPos.y;
		if ( dist < -roundOffError ) {
			new_ftPos.y = new_ftPos.y - dist;
			new_ftPos.x = new_ftPos.x - dist/tVel.y*tVel.x;
		}
		// outside top border?
		dist = new_ftPos.y - reducedBattleFieldSize.y;
		if ( dist > roundOffError) {
			new_ftPos.y = new_ftPos.y - dist;
			new_ftPos.x = new_ftPos.x - dist/tVel.y*tVel.x;
		}

		// shift back coordinates by robotHalfSize
		new_ftPos.x = new_ftPos.x + robotHalfSize;
		new_ftPos.y = new_ftPos.y + robotHalfSize;
		return new_ftPos;
	}

	private cachedTarget findMatchInCachedTargets( cachedTarget cT ) {
		if (cachedTargets.size() == 0) return null;
		long timeStamp = cT.getTime();
		for ( int i = cachedTargets.size()-1; i>=0; i-- ) {
			// check first if cache has current timestamps
			if ( cachedTargets.get(i).getTime() < timeStamp ) return null;
			if ( cT.conditionEquals( cachedTargets.get(i) ) )
				return cachedTargets.get(i);
		}
		return null; // nothing found if we are here
	}

	private Point2D.Double findSettingInCachedTargets( cachedTarget cT ) {
		cachedTarget matchedCT = findMatchInCachedTargets( cT );
		if ( matchedCT == null)
			return null;
		return matchedCT.getTargetFuturePosition();
	}

	public Point2D.Double calcTargetFuturePosition( InfoBot firedBot, double firePower, InfoBot tgt) {
		Point2D.Double firingPosition = null;
		long fireDelay = 0;
		if ( firedBot.getName().equals( myBot.getName() ) ) {
			// firing bot is mine, need to take in account cooling of the gun
			fireDelay =  physics.gunCoolingTime( myBot.getGunHeat() );
			firingPosition = predictBotPositionAtTime( myBot._tracker, myBot.getTime() + fireDelay );
		} else {
			// enemy is firing.
			// FIXME take in account that bullet detection happens one tick after
			// the enemy gun fires
			fireDelay = 0;
			firingPosition = (Point2D.Double) firedBot.getPosition().clone();
		}
		Point2D.Double tFP = null;
		cachedTarget cT = new cachedTarget( myBot, this, firedBot, tgt );
		tFP = findSettingInCachedTargets( cT );
		if ( tFP != null ) {
		       	return tFP;
		}
		// ok we do it first time let's do it
		//logger.dbg("firing bot " + firedBot.getName() + " at target " + tgt.getName() + " with gun " + getName() + " has nothing in the firing solutions cache" );
		tFP = calcTargetFuturePosition( firingPosition, firePower, tgt, fireDelay);
		if ( tFP == null ) {
			// no solution resort to head on targeting
			cT.setTargetWeight( 0 );
			tFP = (Point2D.Double) tgt.getPosition().clone();
		}
		//to counter act bullet shielding bots
		//add small random offset of about bot size
		tFP = addRandomOffsetToTargetFuturePosition( firingPosition, tFP);
		tFP = math.putWithinBorders( tFP, myBot.BattleField);
		cT.setTargetFuturePosition( tFP );
		cachedTargets.add(cT);
		return  tFP;
	}

	protected Point2D.Double calcTargetFuturePosition( Point2D.Double firingPosition, double firePower, InfoBot tgt) {
		return  calcTargetFuturePosition( firingPosition, firePower, tgt,  0);
	}

	protected Point2D.Double calcTargetFuturePosition( Point2D.Double firingPosition, double firePower, InfoBot targetBot, long fireDelay) {
		Point2D.Double tP = null;
		if ( fireDelay >= 0 ) {
			// there is no better information than this for future with head on gun
			tP = targetBot.getPosition();
		} else {
			// we asked to use information in past without looking into info at present
			long lastAvailableTargetingInfoTime = myBot.getTime() + fireDelay;
			tP = targetBot.getPositionAtTime( lastAvailableTargetingInfoTime ); 
			// error checks
			if ( tP == null ) {
				logger.dbg("Do not know former targetBot " + targetBot.getName() + "  position at " + lastAvailableTargetingInfoTime + " time shift or time shift " + fireDelay);
				tP = targetBot.getPosition(); // this gives absolutely last know position
			}
		}
		return (Point2D.Double) tP.clone();
	}

	public firedBullet gunBestBulletAtTime( InfoBot firedBot, InfoBot targetBot, double firePower, long firedTime) {
		// calculates what would be the bullet if gun fired at firedTime
		// main goal is to prevent an enemy gun to look in the "available" future
		// when we detect their fire in later time and start making virtual bullets

		// First figure out firing info 
		Point2D.Double firingPosition = null;
		if ( ( myBot.getTime() - firedTime ) == 1 ) {
			// this is special case and there are some helpers with approximation
			// of missed data
			firingPosition = firedBot.getPrevTicPosition(); // with respect to now
		} else {
			firingPosition = firedBot.getPositionAtTime( firedTime );
		}
		// error checks
		if ( firingPosition == null ) {
			firingPosition = firedBot.getPosition();
		}

		// time when causality still permits to have target info
		long lastAvailableTargetingInfoTime = firedTime - 1; // see wiki
		long fireDelay = lastAvailableTargetingInfoTime - myBot.getTime();
		// Where was the target calculation
		Point2D.Double targetPosition = calcTargetFuturePosition( firingPosition, firePower, targetBot, fireDelay );
		// check if gun can estimate a future target position
		if ( targetPosition == null ) return null;
		firedBullet b = new firedBullet(myBot, this, firedBot, targetPosition, firingPosition, firePower, firedTime);
		//logger.dbg(b.format());
		return b;
	}	

	public void calcGunSettings() {
		if ( myBot._trgt.haveTarget ) {
			setFirePower();
			Point2D.Double tFP = calcTargetFuturePosition(  myBot._tracker, firePower, myBot._trgt);
			targetFuturePosition = (Point2D.Double) tFP;	
		}
	}

	public void setFirePower() {
		firePower = calcFirePower();
	}

	protected double calcFirePower() {
		double firePower =0;
		if ( myBot._trgt.haveTarget ) {
			long fireDelay = physics.gunCoolingTime( myBot.getGunHeat() );
			Point2D.Double myPosAtFiringTime =  predictBotPositionAtTime( myBot._tracker, myBot.getTime() + fireDelay );
			Point2D.Double trgtPosAtFiringTime =  predictBotPositionAtTime( myBot._trgt, myBot.getTime() + fireDelay );
			firePower = firePoverVsDistance( myPosAtFiringTime.distance( trgtPosAtFiringTime) );
			firePower = Math.max( firePower, physics.minimalAllowedBulletEnergy );
			// no point to fire bullets more energetic than enemy bot energy level
			firePower = Math.min( firePower, physics.minReqBulEnergyToKillTarget( myBot._trgt.getEnergy() ) );

			// do not fire more than bot has or it  get itself disabled
			firePower = Math.min( firePower, myBot.getEnergy() - 1e-4 );
			firePower = Math.max( firePower, physics.minimalAllowedBulletEnergy); //take in account rounding
			// final check after rounding
			if (myBot.getEnergy() <= firePower + 1e-4 ) {
				firePower = 0;
			}
		} else {
			firePower = 0;
		}
		return firePower;
	}

	public double getFirePower() {
		return firePower;
	}

	private void drawTargetFuturePosition(Graphics2D g) {
		if ( null != targetFuturePosition ) {
			//logger.dbg("at time " + myBot.getTime() + " aiming at = " + targetFuturePosition );
			g.setColor(gunColor);
			graphics.fillSquare( g,  targetFuturePosition, 40);
		}
	}

	private void drawTargetLastPosition(Graphics2D g) {
		myBot._trgt.onPaint(g);
	}

	private void drawLineToTargetFuturePosition(Graphics2D g) {
		if ( null != targetFuturePosition ) {
			g.setColor(gunColor);
			//logger.dbg("target future pos at " + getTargetFuturePosition() );
			graphics.drawLine( g, getTargetFuturePosition(), myBot.myCoord );
		}
	}

	private void drawLineToTarget(Graphics2D g) {
		g.setColor(gunColor);
		g.drawLine((int)myBot._trgt.getX(), (int)myBot._trgt.getY(), (int)myBot.myCoord.x, (int)myBot.myCoord.y);
	}

	public void onPaint(Graphics2D g) {
		if ( myBot._trgt.haveTarget ) {
			drawTargetFuturePosition(g);
			drawTargetLastPosition(g);
			drawLineToTarget(g);
			drawLineToTargetFuturePosition(g);
		}
	}
}
