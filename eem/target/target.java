// -*- java -*-

package eem.target;

import eem.target.botStatPoint;
import eem.misc.*;
import java.awt.geom.Point2D;
import java.awt.Color;
import java.awt.Graphics2D;

public class target extends InfoBot {
	public boolean haveTarget=false;

	public target() {
		super();
		haveTarget = false;
	}
	
	public target(InfoBot b) {
		this();
		this.name = b.getName();
		this.botStats = b.botStats;
		this.targetUnlocked = b.targetUnlocked;
		this.bulletHitCount = b.getBulletHitCount();
		this.bulletFiredCount = b.getBulletFiredCount();
		this.guessFactorsMap = b.guessFactorsMap;

		haveTarget = true;
	}
	
	public void initTic(long ticTime) {
		super.initTic(ticTime);
	}

	public void onPaint(Graphics2D g) {
		g.setColor(new Color(0xff, 0x00, 0x00, 0x80));
		double R = 36;
		graphics.drawCircle( g, getLast().getPosition(), R );
		//drawBotPath(g);
		//drawLastKnownBotPosition(g);
	}

}

