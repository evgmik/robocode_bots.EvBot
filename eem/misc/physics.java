// -*- java -*-
package eem.misc;

import java.awt.geom.Point2D;
import robocode.util.*;
import java.util.*;
import eem.EvBot;

public class physics {
	public static int robotHalfSize = 0;
	public static Point2D.Double BattleField = new Point2D.Double(0,0);
	public static double coolingRate = 0.1; 

	public static void init(EvBot myBot) {
		robotHalfSize = myBot.robotHalfSize;
		BattleField = (Point2D.Double) myBot.BattleField.clone();
		coolingRate = myBot.getGunCoolingRate();
	}

	public static int gunCoolingTime( double heat ) {
		return (int) Math.ceil( heat/coolingRate );
	}

	public static double bulletSpeed( double firePower ) {
		double bSpeed;
		bSpeed = ( 20 - firePower * 3 );
		logger.noise("bullet speed = " + bSpeed + " for firePower = " + firePower);
		return bSpeed;
	}

	public static double  bulletEnergy( double bulletSpeed ) {
		double bEnergy = ( 20 - bulletSpeed ) / 3;
		return bEnergy;
	}

	public static double  bulletDamageByEnergy( double bEnergy ) {
		double bDamage = 4*bEnergy + 2 * Math.max( bEnergy - 1 , 0 );
		return bDamage;
	}


}
