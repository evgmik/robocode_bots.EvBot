// -*- java -*-

package eem.target;

import eem.target.botStatPoint;
import eem.misc.*;
import java.awt.geom.Point2D;
import java.awt.Color;
import java.awt.Graphics2D;

public class target extends InfoBot {
	public boolean haveTarget=false;
	public boolean targetUnlocked = false; 

	public target() {
		haveTarget = false;
		targetUnlocked = false;
	}
	
	public void initTic(long ticTime) {
		// updating UnLocked status
		if ( ( ticTime - this.getLastSeenTime() ) > 2) 
			this.setUnLockedStatus(true);
		else
			this.setUnLockedStatus(false);

		super.initTic(ticTime);
	}

	public target update(Point2D.Double pos, long tStamp) {
		super.update( pos, tStamp );
		haveTarget = true;
		return this;
	}

	public target update(botStatPoint statPnt) {
		super.update( statPnt );
		haveTarget = true;
		return this;
	}

	public void setUnLockedStatus(boolean val) {
		targetUnlocked = val;
	}

	public void onPaint(Graphics2D g) {
		g.setColor(new Color(0xff, 0x00, 0x00, 0x80));
		double R = 36;
		graphics.drawCircle( g, getLast().getPosition(), R );
		//drawBotPath(g);
		//drawLastKnownBotPosition(g);
	}

}

