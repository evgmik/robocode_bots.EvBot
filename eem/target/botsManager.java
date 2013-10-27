// -*- java -*-

package eem.target;

import eem.EvBot;
import eem.target.*;
import eem.misc.*;
import java.awt.geom.Point2D;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.LinkedList;
import robocode.*;
import robocode.util.*;
import robocode.Rules.*;

public class  botsManager {
	public EvBot myBot;

	public static HashMap<String,InfoBot> bots     = new HashMap<String, InfoBot>();
	public static HashMap<String,InfoBot> deadBots = new HashMap<String, InfoBot>();;

	public botsManager(EvBot bot) {
		myBot = bot;
		// move deadBots to alive bots, should happen at the beginning of the round
		if ( deadBots.size() >= 1) {
			for (InfoBot dBot : deadBots.values() ) {
				String botName = dBot.getName();
				bots.put( botName, dBot);
			}
		}
		deadBots.clear();
	}


	public InfoBot getBotByName(String botName) {
		InfoBot b = null;
		b = bots.get( botName );
		if ( null != b )
			return b;
		b = deadBots.get( botName );
		if ( null != b )
			return b;
		// we should never reach here
		logger.error("Bots manager cannot find bot: " + botName );
		return b;
	}

	public double botWeightForTargeting(InfoBot bot) {
		double weight =0;

		// distance contribution
		double dist2bot = bot.getLastDistance( myBot.myCoord );
		double wDistance = 100/dist2bot;

		// hit probability contribution
		int cntFired = myBot._gmanager.totalBotFiredCount( bot );
		double wGunRaw = myBot._gmanager.botAsTargetWeight( bot) ;
		double wGun;
		if ( myBot.fightType().equals("meleeMidle") ) {
			// high hit chances for robot give higher weight
			// corrected for  low gun fires to this bot
			wGun = (1.0/myBot.numEnemyBotsAlive - wGunRaw) * Math.exp(-cntFired/10) + wGunRaw;
		} else {
			wGun = 1.0/myBot.numEnemyBotsAlive;
		}

		// a weaker target has a preference
		double wEnergy = 0;
		wEnergy = 100.0/( bot.getEnergy() + 0.001)  ;
		// but there is no point to shoot across the whole field to a weak bot
		// most likely there will be someone else to do it quicker
		wEnergy = Math.exp( -dist2bot / 200 ); // 200 is used as ~1/4 of field size

		// Survivability. The longer bot leaves the harder it is as a target
		// bot life length is proportional to its total firing counts
		// This assumes that my bot lives long enough to detected it
		int enemyFiringCount = bot.getBulletFiredCount();
		// easy bots do about 60 firing in 10 rounds
		// so 30 nicely smooth low count stat
		double wSurvivability = 30./( 30 + enemyFiringCount );

		// all together
		weight  = wGun * wDistance * wEnergy * wSurvivability;
		logger.noise(
				""
				+ " wDist = " + logger.shortFormatDouble(wDistance) 
				+ " wGun = " + logger.shortFormatDouble(wGun)
				+ " cFir = " + cntFired
				+ " wEnrg = " + logger.shortFormatDouble(wEnergy)
				+ " cEnFir = " + enemyFiringCount
				+ " wSurv = " + logger.shortFormatDouble(wSurvivability)
				+ " weight " + " = " + logger.shortFormatDouble( weight ) 
				+ " for " + bot.getName()
			  );
		return weight;
	}

	public target choseTarget() {
		target trgt = myBot._trgt;
		double trgtWeight;
		if ( trgt.haveTarget ) {
			trgtWeight = botWeightForTargeting( trgt );
		} else {
			// something negative and small for non existing target
			trgtWeight = -1000; 
		}
		
		// lets find bot weights
		for (InfoBot bot : bots.values()) {
			if ( bot.getName().equals(trgt.getName() ) )
				continue; // target and test bot are the same
			double w = botWeightForTargeting(bot);
			if ( trgtWeight < w ) {
				trgtWeight = w;
				trgt = new target(bot);
				logger.noise("new target " + trgt.getName() );
			}
		}
		return trgt;
	}

	public void initTic(long ticTime) {
		for (InfoBot bot : bots.values()) 
		{
			bot.initTic(ticTime);
			// firing status
			if ( bot.didItFireABullet(ticTime) ) {
				myBot._bmanager.add_enemy_bullet( bot );
			}
		}
	}

	public void printGunsStats() {
		logger.routine("-------------------------------------------------------" );
		logger.routine("Summary for enemies guns" );
		logger.routine("-------------------------------------------------------" );
		logger.routine("--- Alive bots ---" );
		for (InfoBot bot : bots.values()) {
			bot.printGunsStats();
		}
		logger.routine("--- Dead bots ---" );
		for (InfoBot bot : deadBots.values()) {
			bot.printGunsStats();
		}
	}

	public LinkedList<InfoBot> listOfKnownBots() {
		LinkedList<InfoBot> l = new LinkedList<InfoBot>();
		l.addAll( listOfAliveBots() );
		l.addAll( listOfDeadBots() );
		return l;
	}

	public LinkedList<InfoBot> listOfAliveBots() {
		LinkedList<InfoBot> l = new LinkedList<InfoBot>();
		for (InfoBot bot : bots.values()) {
			l.add(bot);
		}
		return l;
	}

	public LinkedList<InfoBot> listOfDeadBots() {
		LinkedList<InfoBot> l = new LinkedList<InfoBot>();
		for (InfoBot bot : deadBots.values()) {
			l.add(bot);
		}
		return l;
	}


	public void onRobotDeath(RobotDeathEvent e) {
		String botName = e.getName();
		InfoBot dBot = bots.get(botName);
		deadBots.put( botName, dBot);
		bots.remove( botName );
	}

	public void add(InfoBot bot) {
		bots.put( bot.getName(), bot );
	}

	public void onHitByBullet(HitByBulletEvent e) {
		String botName = e.getName();
		InfoBot bot;
		bot = bots.get(botName);
		if ( bot != null ) {
			bot.incBulletHitCount();
		}
		bot = deadBots.get(botName);
		if ( bot != null ) {
			bot.incBulletHitCount();
		}
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		String botName = e.getName();
		InfoBot iBot = bots.get(botName);
		if ( iBot == null ) {
		       	// this is newly discovered bot
			iBot = new InfoBot(botName);
		}
		iBot.update( new botStatPoint(myBot, e) );
		bots.put(botName, iBot);
	}

	public void onPaint(Graphics2D g) {
		for (InfoBot bot : bots.values()) 
		{
			bot.onPaint(g);
		}
	}
}
