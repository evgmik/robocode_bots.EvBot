// -*- java -*-

package eem.motion;

import eem.EvBot;
import eem.target.*;
import eem.misc.*;
import java.util.Random;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.Arrays;
import robocode.util.*;
import java.awt.Color;


public class dangerMapMotion extends basicMotion {
	Point2D.Double motionTarget = new Point2D.Double(0,0);
	
	public dangerMapMotion(EvBot bot) {
		myBot = bot;
	}
	
	public void makeMove() {
		motionTarget = new Point2D.Double(myBot.BattleField.x/2, myBot.BattleField.y/2);
		moveToPoint( motionTarget );
	}

	public void onPaint(Graphics2D g) {
		//show motion target
		g.setColor(Color.green);
		g.setColor(Color.red);
		g.drawLine((int) motionTarget.x, (int) motionTarget.y, (int)myBot.myCoord.x, (int)myBot.myCoord.y);
		g.drawOval((int) motionTarget.x-5, (int) motionTarget.y-5, 10, 10);
	}

}
