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

	public gunManager(EvBot bot) {
		myBot = bot;
		LinkedList<baseGun> guns;
		
		guns = new  LinkedList<baseGun>();
		guns.add( new linearGun(myBot) );
		guns.add( new randomGun(myBot) );
		//guns.add( new pifGun(myBot) ); // FIXME: too slow
		gunSets.put( "1on1", guns );

		guns = new  LinkedList<baseGun>();
		guns.add( new linearGun(myBot) );
		guns.add( new randomGun(myBot) );
		gunSets.put( "meleeMidle", guns );

		guns = new  LinkedList<baseGun>();
		guns.add( new linearGun(myBot) );
		gunSets.put( "melee", guns );

		guns = new  LinkedList<baseGun>();
		guns.add( new baseGun(myBot) );
		guns.add( new linearGun(myBot) );
		//guns.add( new pifGun(myBot) ); // FIXME: too slow
		gunSets.put( "firingAtMyBot", guns );

		guns = new  LinkedList<baseGun>();
		guns.add( new linearGun(myBot) );
		gunSets.put( "defaultGun", guns );

		// full known set of my guns
		//guns = new  LinkedList<baseGun>();
		//guns.add( new baseGun(myBot) );
		//guns.add( new linearGun(myBot) );
		//guns.add( new randomGun(myBot) );
		//guns.add( new pifGun(myBot) );
		//gunSets.put( "TEMPLATE", guns );
	}

	public double overallGunsHitRate(){
		double hitRate;
		double firedCount=0;
		double hitCount=0;
		LinkedList<baseGun> guns = gunSets.get( myBot.fightType() );
		for ( baseGun g: guns ) {
			firedCount += g.getBulletFiredCount();
			hitCount   += g.getBulletHitCount();
		} 
		hitRate = (hitCount + 1) / (firedCount + 1);
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
		logger.noise("Gun[" + gun.getName() + " ] weight = " + weight);
		return weight;
	}

	public baseGun getDefaultGun(){
		return  new baseGun(myBot);
	}

	public baseGun weights2gunForBot(InfoBot bot) {
		double rnd;
		baseGun g = null;
		rnd=Math.random();
		
		double accumWeight = 0;
		boolean setNewGun = false;
		LinkedList<baseGun> guns = gunSets.get( myBot.fightType() );
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

	public baseGun choseGun() {
		double rnd;
		baseGun _gun = myBot.getGun();
		baseGun new_gun = null;
		// let's choose the gun if gun is fired
		if ( _gun.isGunFired() ) {
			_gun.gunFired = false;
			logger.noise("new choice of gun instead of old " + _gun.getName());
			if ( myBot.fightType().equals("melee") ) {
				new_gun = new linearGun(myBot); //default gun
			}
			if ( myBot.fightType().equals("meleeMidle") ) {
				new_gun = new linearGun(myBot); //default gun
				// only survivors are smart and we had to do random gun
				rnd=Math.random();
				if ( rnd > 0.5 ) { 
					new_gun = new randomGun(myBot);
				}
			}
			if ( myBot.fightType().equals("1on1") ) {
				// performance based guns
				new_gun = weights2gunForBot(myBot._trgt);
			}
			if ( new_gun == null ) {
				new_gun = new linearGun(myBot); //default gun
				logger.error("This should not happen: we did not chose a gun");
			}
			_gun = new_gun;
			logger.noise("Gun choice = " + _gun.getName());
		}


		//_gun = new pifGun(myBot); // dbg/test pif only gun
		_gun.setTargetFuturePosition(myBot._trgt);

		return _gun;

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
		LinkedList<baseGun> guns = gunSets.get( myBot.fightType() );
		for ( baseGun tmp_gun: guns ) {
			String botName = tmp_gun.getName();
			int hC = tmp_gun.getBulletHitCount(bot);
			hCt += hC;
			int fC = tmp_gun.getBulletFiredCount(bot);
			fCt += fC;
			double weight = getGunWeightForBot(tmp_gun, bot);
			String str = "";
			str += "gun[ " + botName + "\t]";
			str += " hit target \t" + hC;
			str += "\t and was fired \t" + fC;
			str += "\t gun weight is \t" + weight;
			logger.routine(str);
		}
		double hProb = 1.0*hCt/Math.max(fCt,1);
		logger.routine("---" );
		logger.routine(hProb + " probability to hit bot " + bot.getName() ); 
	}

	public void printGunsStats() {
		LinkedList<InfoBot> botsList = myBot._botsmanager.listOfKnownBots();
		int gunsFiringTotal=0;

		LinkedList<baseGun> guns = gunSets.get( myBot.fightType() );
		for ( baseGun tmp_gun: guns ) {
			for ( InfoBot bot: botsList ) {
				gunsFiringTotal += tmp_gun.getBulletFiredCount(bot);
			}
		}

		logger.routine("-------------------------------------------------------" );
		logger.routine("Gun stats for " + myBot.getName() );
		logger.routine("Fight type: " + myBot.fightType() );
		logger.routine("-------------------------------------------------------" );
		for ( InfoBot bot: botsList ) {
			printGunsStatsForTarget(bot);
		}

		logger.routine("-------------------------------------------------------" );
		logger.routine("Summary for each gun" );
		logger.routine("-------------------------------------------------------" );
		for ( baseGun tmp_gun: guns ) {
			int hC = totalGunHitCount(tmp_gun);
			int fC = totalGunFiredCount(tmp_gun);
			// firing rate
			double fR = (double)fC/Math.max(gunsFiringTotal,1);
			String fRstr = String.format("%.2f", fR );
			logger.routine("Gun[ " + tmp_gun.getName()+"\t] hit target \t" + hC + "\t and was fired \t" + fC + " \t firing rate is \t" + (double)fC/gunsFiringTotal);
			// FIXME: gunsPerformance is not calculated right
		}
		logger.routine("-------------------------------------------------------" );
		logger.routine("Overall guns hit rate = " + overallGunsHitRate() );
		logger.routine("-------------------------------------------------------" );
	}

}
