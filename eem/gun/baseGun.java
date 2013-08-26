// -*- java -*-

package eem.gun;

import eem.EvBot;
import eem.target.*;
import java.util.Random;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;


public class baseGun {
	protected EvBot myBot;
	protected String gunName = "base";
	protected boolean  gunFired = false;
	protected boolean  gunHasTargetPoint = false;
	protected Random gun_rand = new Random();
	protected Color gunColor = Color.black;;
	protected Point2D.Double targetFuturePosition;
	protected double firePower;

	public baseGun() {
	}

	public baseGun(EvBot bot) {
		myBot = bot;
	};

	public void fireGun() {
		myBot.setFire(firePower);
		gunFired = true;
		gunHasTargetPoint = false;
	}

	public void setTargetFuturePosition( Point2D.Double target ) {
		targetFuturePosition = target;
	};

	public String getName() {
		return gunName;
	}

	public boolean isGunFired() {
		return gunFired;
	}

	public Point2D.Double getTargetFuturePosition() {
		return targetFuturePosition;
	}

	public void setTargetFuturePosition(target tgt) {
		targetFuturePosition = tgt.getPosition();
	}

        public double firePoverVsDistance( double targetDistance ) {
                // calculate firepower based on distance
		myBot.dbg(myBot.dbg_noise, "Target distance = " + targetDistance);
                double firePower;
                firePower = Math.min(500 / targetDistance, 3);
		myBot.dbg(myBot.dbg_noise, "Fire power = " + firePower);
                return firePower;
        }

	public void calcGunSettings() {
		setFirePower();
		setTargetFuturePosition(myBot._trgt);
	}

	public void setFirePower() {
		firePower = firePoverVsDistance(myBot._trgt.getLastDistance(myBot.myCoord));
	}

	public double  bulletSpeed( double firePower ) {
		double bSpeed;
		bSpeed = ( 20 - firePower * 3 );
		myBot.dbg(myBot.dbg_noise, "bullet speed = " + bSpeed + " for firePower = " + firePower);
		return bSpeed;
	}

	public double getFirePower() {
		return firePower;
	}

	private void drawTargetFuturePosition(Graphics2D g) {
		g.setColor(gunColor);
		g.fillRect((int)targetFuturePosition.x - 20, (int)targetFuturePosition.y - 20, 40, 40);
	}

	private void drawTargetLastPosition(Graphics2D g) {
		myBot._trgt.onPaint(g);
	}

	private void drawLineToTargetFuturePosition(Graphics2D g) {
		g.setColor(gunColor);
		g.drawLine((int)getTargetFuturePosition().x, (int)getTargetFuturePosition().y, (int)myBot.myCoord.x, (int)myBot.myCoord.y);
	}

	private void drawLineToTarget(Graphics2D g) {
		g.setColor(gunColor);
		g.drawLine((int)myBot._trgt.getX(), (int)myBot._trgt.getY(), (int)myBot.myCoord.x, (int)myBot.myCoord.y);
	}

	public void onPaint(Graphics2D g) {
		drawTargetFuturePosition(g);
		drawTargetLastPosition(g);
		drawLineToTarget(g);
		drawLineToTargetFuturePosition(g);
	}
}
