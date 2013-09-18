// -*- java -*-

package eem.motion;

import eem.EvBot;
import eem.target.*;
import eem.misc.*;
import java.util.Random;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.Color;


public class basicMotion {
	protected static EvBot myBot;

	public basicMotion() {
	}

	public basicMotion(EvBot bot) {
		myBot = bot;
	}

	public void makeMove() {
		// for basic motion we do nothing
	}

	public void onPaint(Graphics2D g) {
	}
}
