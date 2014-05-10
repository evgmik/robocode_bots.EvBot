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
import java.util.HashMap;


public class cachedTarget {
	protected EvBot myBot = null;
	protected InfoBot firingBot = null;
	protected InfoBot targetBot = null;
	protected baseGun gun = null;
	protected Point2D.Double targetFuturePosition = null;
	// weight is to help gun manager to decide which gun fires
	// 1 -> gun is sure to hit, 0 -> gun did not find solution
	// and resort to fall back scenarion i.e. head on firing
	// mostly to help with pif guns
	protected double targetWeight = 1;
	protected double firePower = 0;
	protected long timeStamp = 0;

	public cachedTarget(EvBot bot) {
		myBot = bot;
		this.timeStamp = myBot.getTime();
	}

	public cachedTarget(EvBot bot, InfoBot firingBot, InfoBot targetBot) {
		this( bot );
		this.timeStamp = myBot.getTime();
		this.firingBot = firingBot;
		this.targetBot = targetBot;
	}

	public cachedTarget(EvBot bot, baseGun gun,  InfoBot firingBot, InfoBot targetBot) {
		this( bot, firingBot, targetBot);
		this.gun = gun;
	}

	public cachedTarget(EvBot bot, baseGun gun, InfoBot firingBot, InfoBot targetBot, Point2D.Double targetFuturePosition, double firePower) {
		this( bot, gun, firingBot, targetBot);
		this.targetFuturePosition = (Point2D.Double) targetFuturePosition.clone();
	}

	public boolean conditionEquals( cachedTarget cT ) {
		if ( this.timeStamp != cT.timeStamp ) return false;
		if (!this.gun.getName().equals( cT.gun.getName() ) ) return false;
		if ( this.firingBot != cT.firingBot ) return false;
		if ( this.targetBot != cT.targetBot ) return false;

		//logger.dbg("firing bot " + firingBot.getName() + " at target " + targetBot.getName() + " with gun " + gun.getName() + " has the firing solutions cache" );
		return true;
	}

	public Point2D.Double getTargetFuturePosition() {
		return (Point2D.Double) targetFuturePosition.clone();
	}

	public void setTargetFuturePosition( Point2D.Double fP) {
		this.targetFuturePosition = (Point2D.Double) fP.clone();	
	}

	public void setTargetWeight( double w) {
		targetWeight = w;
	}

	public double getTargetWeight() {
		return targetWeight;
	}

	public long getTime() {
		return timeStamp;
	}

	public void setTime(long tS) {
		this.timeStamp = tS;
	}

	public double getFirePower() {
		return firePower;
	}

	public void setFirePower(double fp) {
		this.firePower = fp;
	}
}
