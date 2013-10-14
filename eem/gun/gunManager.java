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


public class gunManager {
	public EvBot myBot;
	public LinkedList<baseGun> guns;
	private double[] gunsPerformance;
	private int nGuns=0;

	public gunManager(EvBot bot) {
		myBot = bot;
		
		guns = new  LinkedList<baseGun>();
		guns.add(new baseGun(myBot));
		guns.add(new linearGun(myBot));
		guns.add(new randomGun(myBot));
		guns.add(new pifGun(myBot));

		nGuns = guns.size();
		logger.noise("Number of guns = " + nGuns);
		gunsPerformance = new double[nGuns];

	}

	public double gunHitRate( baseGun g ) {
		return g.getGunHitRate();
	}

	public double overallGunsHitRate(){
		double hitRate;
		double firedCount=0;
		double hitCount=0;
		baseGun g;
		for ( int i =0; i < nGuns; i++ ) {
			g = guns.get(i);
			firedCount += g.getBulletFiredCount();
			hitCount   += g.getBulletHitCount();
		} 
		hitRate = (hitCount + 1) / (firedCount + 1);
		return hitRate;
	}

	public double getGunWeightForBot(baseGun gun, InfoBot bot) {
		baseGun  tmp_gun;
		double perfNormilizer = 0;
		double hR = 0;
		// calculate each gun weight/performance
		for ( int i =0; i < nGuns; i++ ) {
			tmp_gun = guns.get(i);
			hR = tmp_gun.getGunHitRate( bot );
			logger.noise("Gun[" + tmp_gun.getName() + " ] hit rate = " + hR);
			perfNormilizer += hR;
		} 
		// normalize gun weights to 1
		hR = gun.getGunHitRate( bot );
		double weight;
		weight = hR / perfNormilizer;
		logger.noise("Gun[" + gun.getName() + " ] weight = " + weight);
		return weight;
	}

	public void updateGunsWeight() {
		baseGun  tmp_gun;
		double perfNormilizer = 0;
		double perfTmp;
		// calculate each gun weight/performance
		for ( int i =0; i < nGuns; i++ ) {
			tmp_gun = guns.get(i);
			perfTmp = gunHitRate( tmp_gun );
			gunsPerformance[i] = perfTmp;
			logger.noise("Gun[" + guns.get(i).getName() + " ] performance = " + gunsPerformance[i]);
			perfNormilizer += perfTmp;
		} 
		// normilize gun weights to 1
		for ( int i =0; i < nGuns; i++ ) {
			gunsPerformance[i] /= perfNormilizer;
			logger.noise("Gun[" + guns.get(i).getName() + " ] performance = " + gunsPerformance[i]);
		}
	}

	public baseGun wieghts2gun() {
		double rnd;
		rnd=Math.random();
		double sumPerformance = 0;
		baseGun g = new baseGun(myBot);
		for ( int i =0; i < nGuns; i++ ) {
			sumPerformance += gunsPerformance[i];
			if (rnd <= sumPerformance) {
				g=guns.get(i);
				break;
			}
		}
		return g;
	}

	public baseGun choseGun() {
		double rnd;
		baseGun _gun = myBot.getGun();
		// let's choose the gun if gun is fired
		if ( _gun.isGunFired() ) {
			_gun.gunFired = false;
			logger.noise("new choice of gun instead of old " + _gun.getName());
			_gun = new linearGun(myBot); //default gun
			if ( (1< myBot.getOthers() ) && (myBot.getOthers() < 3 ) ) {
				// only survivors are smart and we had to do random gun
				rnd=Math.random();
				if ( rnd > 0.5 ) { 
					_gun = new randomGun(myBot);
				}
			}
			if (myBot.getOthers() == 1 ) {
				// performance based guns
				updateGunsWeight();
				_gun = wieghts2gun();
			}
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
		for ( int i =0; i < nGuns; i++ ) {
			baseGun tmp_gun = guns.get(i);
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
		baseGun  tmp_gun = null;
		ListIterator<baseGun> gLIter = guns.listIterator();
		LinkedList<InfoBot> botsList = myBot._botsmanager.listOfKnownBots();

		updateGunsWeight();
		int gunsFiringTotal=0;

		for ( int i =0; i < nGuns; i++ ) {
			for ( InfoBot bot: botsList ) {
				tmp_gun = guns.get(i);
				gunsFiringTotal += tmp_gun.getBulletFiredCount(bot);
			}
		}

		logger.routine("-------------------------------------------------------" );
		logger.routine("Gun stats for " + myBot.getName() );
		logger.routine("-------------------------------------------------------" );
		for ( InfoBot bot: botsList ) {
			printGunsStatsForTarget(bot);
		}

		logger.routine("-------------------------------------------------------" );
		logger.routine("Summary for each gun" );
		logger.routine("-------------------------------------------------------" );
		for ( int i =0; i < nGuns; i++ ) {
			tmp_gun = guns.get(i);
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
