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

		nGuns = guns.size();
		logger.noise("Number of guns = " + nGuns);
		gunsPerformance = new double[nGuns];

	}

	public double gunHitRate( baseGun g ) {
		return (g.getBulletHitCount() + 1.0) / (g.getBulletFiredCount() + 1.0);
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


		_gun.setTargetFuturePosition(myBot._trgt);

		return _gun;

	}

	public void printGunsStats() {
		baseGun  tmp_gun = null;
		ListIterator<baseGun> gLIter = guns.listIterator();
		updateGunsWeight();
		int gunsFiringTotal=0;

		for ( int i =0; i < nGuns; i++ ) {
			tmp_gun = guns.get(i);
			gunsFiringTotal += tmp_gun.getBulletFiredCount();
		}

		for ( int i =0; i < nGuns; i++ ) {
			tmp_gun = guns.get(i);
			logger.dbg("Gun[ " + tmp_gun.getName()+"\t] hit target \t" + tmp_gun.getBulletHitCount() + "\t and was fired \t" + tmp_gun.getBulletFiredCount() +"\t gun weight is \t" + gunsPerformance[i] + " \t firing rate is \t" + (double)tmp_gun.getBulletFiredCount()/gunsFiringTotal);
		}
		logger.dbg("Overall guns hit rate = " + overallGunsHitRate() );
	}

}
