// -*- java -*-

package eem.gun;

import eem.EvBot;
import java.awt.Color;

public class linearGun extends baseGun {

	public linearGun(EvBot bot) {
		myBot = bot;
		gunName = "linear";
		gunColor = new Color(0xff, 0x00, 0x00, 0x80);
	}
}	
