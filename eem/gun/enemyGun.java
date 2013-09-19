// -*- java -*-

package eem.gun;

import eem.EvBot;
import eem.target.*;
import eem.misc.*;
import eem.gun.misc;
import java.awt.Color;
import java.awt.geom.Point2D;
import robocode.util.*;

public class enemyGun extends baseGun {

	public enemyGun() {
		gunName = "enemy";
		gunColor = new Color(0x00, 0x00, 0xff, 0x80);
	}

}
