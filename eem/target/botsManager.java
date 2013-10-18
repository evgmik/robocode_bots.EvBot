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

	public HashMap<String,InfoBot> bots     = new HashMap<String, InfoBot>();
	public HashMap<String,InfoBot> deadBots = new HashMap<String, InfoBot>();;

	public botsManager(EvBot bot) {
		myBot = bot;
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
