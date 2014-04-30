// -*- java -*-
package eem.misc;

import java.awt.geom.Point2D;
import robocode.util.*;
import java.util.*;
import eem.EvBot;

public class physics {
	public static int robotHalfSize = 0;
	public static Point2D.Double BattleField = new Point2D.Double(0,0);

	public static void init(EvBot myBot) {
		robotHalfSize = myBot.robotHalfSize;
		BattleField = (Point2D.Double) myBot.BattleField.clone();
	}
}
