// -*- java -*-

package eem.target;

import eem.EvBot;
import eem.target.*;
import eem.misc.*;
import java.awt.geom.Point2D;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.HashMap;
import robocode.*;
import robocode.util.*;
import robocode.Rules.*;

public class  botsManager {
	public EvBot myBot;

	public HashMap<String,InfoBot> bots;

	public botsManager(EvBot bot) {
		myBot = bot;
		bots = new HashMap<String, InfoBot>();
	}

	public void initTic() {
	}

	public void add(InfoBot bot) {
		bots.put( bot.getName(), bot );
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		String botName = e.getName();
		InfoBot iBot = bots.get(botName);
		if ( iBot == null ) {
			logger.dbg("See new bot " + botName);
		       	// this is newly discovered bot
			iBot = new InfoBot(botName);
		}
		iBot.update( new botStatPoint(myBot, e) );
		bots.put(botName, iBot);
	}

	public void onPaint(Graphics2D g) {
		for (InfoBot bot : bots.values()) 
		{
			bot.onPaint(g);
		}
	}
}
