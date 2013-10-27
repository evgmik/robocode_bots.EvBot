// -*- java -*-

package eem.gun;

import eem.EvBot;
import eem.target.*;
import eem.bullets.*;
import eem.misc.*;
import java.util.Random;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import robocode.Bullet;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.*;
import java.util.HashMap;


public class gunManager {
	public EvBot myBot;
	public static HashMap<String, LinkedList<baseGun>> gunSets = new HashMap<String, LinkedList<baseGun>>();
	public static HashMap<String, baseGun> allUsedByMyBotGuns = new HashMap<String, baseGun>();

	public gunManager(EvBot bot) {
		myBot = bot;
		LinkedList<baseGun> guns;
		LinkedList<String> myFightTypes= new  LinkedList<String>();
		String fightType;
		
		guns = new  LinkedList<baseGun>();
		//guns.add( new linearGun(myBot) );
		guns.add( new randomGun(myBot) );
		guns.add( new circularGun(myBot) );
		//guns.add( new pifGun(myBot) ); // FIXME: too slow
		fightType = "1on1";
		gunSets.put( fightType, guns );
		myFightTypes.add( fightType );

		guns = new  LinkedList<baseGun>();
		//guns.add( new linearGun(myBot) );
		guns.add( new circularGun(myBot) );
		guns.add( new randomGun(myBot) );
		//guns.add( new pifGun(myBot) ); // FIXME: too slow
		fightType = "meelee1on1";
		gunSets.put( fightType, guns );
		myFightTypes.add( fightType );

		guns = new  LinkedList<baseGun>();
		//guns.add( new linearGun(myBot) );
		guns.add( new circularGun(myBot) );
		//guns.add( new randomGun(myBot) );
		fightType = "meleeMidle";
		gunSets.put( fightType, guns );
		myFightTypes.add( fightType );

		guns = new  LinkedList<baseGun>();
		//guns.add( new linearGun(myBot) );
		guns.add( new circularGun(myBot) );
		fightType = "melee";
		gunSets.put( fightType, guns );
		myFightTypes.add( fightType );

		guns = new  LinkedList<baseGun>();
		guns.add( new baseGun(myBot) );
		guns.add( new linearGun(myBot) );
		guns.add( new circularGun(myBot) );
		//guns.add( new pifGun(myBot) ); // FIXME: too slow
		gunSets.put( "firingAtMyBot", guns );

		guns = new  LinkedList<baseGun>();
		guns.add( new linearGun(myBot) );
		guns.add( new circularGun(myBot) );
		fightType = "defaultGun";
		gunSets.put( fightType, guns );
		myFightTypes.add( fightType );

		// full known set of my guns
		//guns = new  LinkedList<baseGun>();
		//guns.add( new baseGun(myBot) );
		//guns.add( new linearGun(myBot) );
		//guns.add( new circularGun(myBot) );
		//guns.add( new randomGun(myBot) );
		//guns.add( new pifGun(myBot) );
		//gunSets.put( "TEMPLATE", guns );

		
		for ( String fType : myFightTypes ) {
		       guns = gunSets.get( fType );
		       for ( baseGun g : guns) {
			       allUsedByMyBotGuns.put( g.getName(), g );
		       }
		}
	}

	public double overallGunsHitRate(){
		double hitRate;
		int firedCount=0;
		int hitCount=0;
		for ( baseGun g: allUsedByMyBotGuns.values() ) {
			firedCount += totalGunFiredCount( g );
			hitCount   += totalGunHitCount( g );
		} 
		hitRate = math.eventRate( hitCount, firedCount );
		return hitRate;
	}

	public double getGunWeightForBot(baseGun gun, InfoBot bot) {
		double perfNormilizer = 0;
		double perf = 0;
		// calculate total performance for each weight
		LinkedList<baseGun> guns = gunSets.get( myBot.fightType() );
		for ( baseGun tmp_gun: guns ) {
			perf = tmp_gun.getGunPerformance( bot );
			logger.noise("Gun[" + tmp_gun.getName() + " ] performance = " + perf);
			perfNormilizer += perf;
		} 
		// normalize gun weights to 1
		perf = gun.getGunPerformance( bot );
		double weight;
		weight = perf / perfNormilizer;
		logger.noise("Gun[" + gun.getName() + " ] weight = " + logger.shortFormatDouble( weight ) );
		return weight;
	}

	public baseGun getDefaultGun(){
		return  new baseGun(myBot);
	}

	public baseGun weights2gunForBot( InfoBot bot, String fightTypeStr ) {
		double rnd;
		baseGun g = null;
		rnd=Math.random();
		
		double accumWeight = 0;
		boolean setNewGun = false;
		LinkedList<baseGun> guns = gunSets.get( fightTypeStr );
		for ( baseGun tmp_gun: guns ) {
			accumWeight += getGunWeightForBot( tmp_gun,  bot );
			if ( rnd <= accumWeight ) {
				g=tmp_gun;
				setNewGun = true;
				break;
			}
		}
		if ( !setNewGun ) {
			logger.warning("Improbable happens: rnd == 1, assigning default gun");
			g = getDefaultGun();
		}

		return g;
	}

	public baseGun weights2gunForBot(InfoBot bot) {
		return  weights2gunForBot( bot, myBot.fightType() );
	}

	public baseGun choseGun() {
		double rnd;
		baseGun _gun = myBot.getGun();
		baseGun new_gun = null;
		// let's choose the gun if gun is fired
		if ( _gun.isGunFired() ) {
			_gun.gunFired = false;
			logger.noise("new choice of gun instead of old " + _gun.getName());
			new_gun = weights2gunForBot(myBot._trgt);
			if ( new_gun == null ) {
				logger.warning("This should not happen: we did not chose a gun");
				new_gun = getDefaultGun(); //default gun
			}
			_gun = new_gun;
			logger.noise("Gun choice = " + _gun.getName());
		}


		//_gun = new pifGun(myBot); // dbg/test pif only gun
		_gun.setTargetFuturePosition(myBot._trgt);

		return _gun;

	}

	public int totalBotHitCount(InfoBot bot) {
		LinkedList<baseGun> guns = gunSets.get( myBot.fightType() );
		int cnt = 0;
		for ( baseGun tmp_gun: guns ) {
			cnt += tmp_gun.getBulletHitCount( bot );
		}
		return cnt;
	}

	public int totalBotFiredCount(InfoBot bot) {
		LinkedList<baseGun> guns = gunSets.get( myBot.fightType() );
		int cnt = 0;
		for ( baseGun tmp_gun: guns ) {
			cnt += tmp_gun.getBulletFiredCount( bot );
		}
		return cnt;
	}

	public double botAsTargetWeight(InfoBot bot) {
		LinkedList<InfoBot> botsList = myBot._botsmanager.listOfAliveBots();
		// is this bot alive?
		boolean statusDead = true;
		for ( InfoBot tmp_bot: botsList ) {
			if (bot.getName().equals( tmp_bot.getName() ) ) {
				statusDead = false;
				break;
			}
		}
		if ( statusDead ) return 0;

		// for alive bot return normalized weight
		double weightNorm = 0;
		for ( InfoBot tmp_bot: botsList ) {
			weightNorm += math.perfRate( totalBotHitCount( tmp_bot ), totalBotFiredCount( tmp_bot ) );
		}
		return  math.perfRate( totalBotHitCount( bot ), totalBotFiredCount( bot ) )/ weightNorm;
	}

	public InfoBot theBestTarget() {
		LinkedList<InfoBot> botsList = myBot._botsmanager.listOfAliveBots();
		InfoBot trgt = null;

		// do we have any candidates in the list of alive bots?
		if ( botsList.size() == 0 ) return trgt;

		double maxWeight = -10; // something  smaller than 0
		double w;
		for ( InfoBot tmp_bot: botsList ) {
			w = botAsTargetWeight( tmp_bot);
			if ( w > maxWeight ) {
				trgt = tmp_bot;
				maxWeight = w;
			}
		}
		return trgt;
	}

	public int totalGunHitCount(baseGun gun) {
		LinkedList<InfoBot> botsList = myBot._botsmanager.listOfKnownBots();
		int gCount = 0;
		for ( InfoBot bot: botsList ) {
			gCount += gun.getBulletHitCount(bot);
		}
		return gCount;
	}

	public int totalGunFiredCount(baseGun gun) {
		LinkedList<InfoBot> botsList = myBot._botsmanager.listOfKnownBots();
		int gCount = 0;
		for ( InfoBot bot: botsList ) {
			gCount += gun.getBulletFiredCount(bot);
		}
		return gCount;
	}

	public void printGunsStatsForTarget(InfoBot bot) {
		logger.routine("----------------" );
		logger.routine("Against bot: " + bot.getName() );
		logger.routine("----------------" );
		int hCt = 0;
		int fCt = 0;
		LinkedList<baseGun> activeGuns = gunSets.get( myBot.fightType() );
		LinkedList<String> activeGunsNames = new LinkedList<String>();
		for ( baseGun tmp_gun: activeGuns ) {
			activeGunsNames.add( tmp_gun.getName() );
		}
		for ( baseGun tmp_gun: allUsedByMyBotGuns.values()  ) {
			String gunName = tmp_gun.getName();
			int hC = tmp_gun.getBulletHitCount(bot);
			hCt += hC;
			int fC = tmp_gun.getBulletFiredCount(bot);
			fCt += fC;
			double weight = getGunWeightForBot(tmp_gun, bot);
			String str = "";
			str += "gun[ " + gunName + "\t]";
			str += " hit target \t" + hC;
			str += "\t and was fired \t" + fC;
			if ( activeGunsNames.contains( gunName ) ) {
				str += "\t gun weight is \t" + logger.shortFormatDouble( weight );
			}
			logger.routine(str);
		}
		logger.routine("---" );
		double hProb = math.eventRate( totalBotHitCount( bot ), totalBotFiredCount( bot ) );
		logger.routine(logger.shortFormatDouble( hProb ) + " probability to hit bot " + bot.getName() ); 
		logger.routine( logger.shortFormatDouble( botAsTargetWeight( bot ) ) + " weight as a target of bot " + bot.getName() ); 
	}

	public void printGunsStatsForBotsList( LinkedList<InfoBot> botsList ) {
		for ( InfoBot bot: botsList ) {
			printGunsStatsForTarget(bot);
		}
	}

	public void printGunsStatsTicRelated() {
		LinkedList<InfoBot> botsList;
		logger.routine("-------------------------------------------------------" );
		logger.routine("Fight type: " + myBot.fightType() );
		botsList = myBot._botsmanager.listOfDeadBots();
		if ( botsList.size() >= 1 ) {
			logger.routine("------ Gun Stats for Dead  bots ------------------------" );
			printGunsStatsForBotsList(myBot._botsmanager.listOfDeadBots());
		}
		botsList = myBot._botsmanager.listOfAliveBots();
		if ( botsList.size() >= 1 ) {
			logger.routine("------ Gun Stats for Alive bots ------------------------" );
			printGunsStatsForBotsList(myBot._botsmanager.listOfAliveBots());
		}

		printGunsBestTarget();
	}

	public void printGunsBestTarget() {
		String theBestTargetName;
		logger.routine("-------------------------------------------------------" );
		if ( theBestTarget() == null ) {
			theBestTargetName = "Yet to find";
		} else {
			theBestTargetName = theBestTarget().getName();
		}
		logger.routine(" ==> Overall best target: " + theBestTargetName );
	}

	public void printGunsStats() {
		LinkedList<InfoBot> botsList = myBot._botsmanager.listOfKnownBots();
		int gunsFiringTotal=0;


		logger.routine("-------------------------------------------------------" );
		logger.routine("Gun stats for " + myBot.getName() );
		logger.routine("-------------------------------------------------------" );

		if ( botsList.size() >= 1 ) {
			logger.routine("------ Gun Stats for Dead  bots ------------------------" );
			printGunsStatsForBotsList(myBot._botsmanager.listOfDeadBots());
			logger.routine("------ Gun Stats for Alive bots ------------------------" );
			printGunsStatsForBotsList(myBot._botsmanager.listOfAliveBots());
		}

		logger.routine("-------------------------------------------------------" );
		logger.routine("Summary for each gun at this stage across this game" );
		logger.routine("-------------------------------------------------------" );
		for ( baseGun tmp_gun: allUsedByMyBotGuns.values() ) {
			for ( InfoBot bot: botsList ) {
				gunsFiringTotal += tmp_gun.getBulletFiredCount(bot);
			}
		}

		for ( baseGun tmp_gun: allUsedByMyBotGuns.values() ) {
			int hC = totalGunHitCount(tmp_gun);
			int fC = totalGunFiredCount(tmp_gun);
			// firing rate
			double fR = math.eventRate( fC, gunsFiringTotal );
			String fRstr = String.format("%.2f", fR );
			logger.routine("Gun[ " + tmp_gun.getName()+"\t] hit target \t" + hC + "\t and was fired \t" + fC + " \t firing rate is \t" + (double)fC/gunsFiringTotal);
		}
		logger.routine("-------------------------------------------------------" );
		logger.routine("Overall guns hit rate = " + overallGunsHitRate() );
		logger.routine("-------------------------------------------------------" );
	}

}
