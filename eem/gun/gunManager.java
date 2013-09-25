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
		logger.dbg("Number of guns = " + nGuns);
		gunsPerformance = new double[nGuns];

	}

	public void updateGunsWeight() {
		baseGun  tmp_gun;
		double perfNormilizer = 0;
		double perfTmp;
		// calculate each gun weight/performance
		for ( int i =0; i < nGuns; i++ ) {
			tmp_gun = guns.get(i);
			perfTmp = (tmp_gun.getBulletHitCount() + 1.0) / (tmp_gun.getBulletFiredCount() + 1.0);
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


		if ( _gun.getName().equals("linear") ) {
			_gun.setTargetFuturePosition(myBot._trgt);
		}

		if ( _gun.getName().equals("base") ) {
			_gun.setTargetFuturePosition(myBot._trgt);
		}

		if ( _gun.getName().equals("random") ) {
			if ( _gun.isGunFired() ) {
				_gun.setTargetFuturePosition(myBot._trgt);
			} else {
				// no need to update future coordinates before gun fire
			}
		}

		return _gun;

	}

	public void printGunsStats() {
		baseGun  tmp_gun = null;
		ListIterator<baseGun> gLIter = guns.listIterator();
		while (gLIter.hasNext()) {
			tmp_gun = gLIter.next();
			logger.dbg("Gun[ " + tmp_gun.getName()+"\t] hit target \t" + tmp_gun.getBulletHitCount() + "\t and was fired \t" + tmp_gun.getBulletFiredCount() );
		}
	}

}