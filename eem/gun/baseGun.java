// -*- java -*-

package eem.gun;

import eem.target.*;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;


public class baseGun {
	protected String gunName;
	protected boolean  gunFired;
	protected Color gunColor;
	protected Point2D.Double targetFuturePosition;

	public baseGun() {
		gunName = "base";
		gunFired = true;
		gunColor = Color.black;
		targetFuturePosition = new Point2D.Double(0,0);
	};

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

	private void drawTargetFuturePosition(Graphics2D g) {
		g.setColor(gunColor);
		g.fillRect((int)targetFuturePosition.x - 20, (int)targetFuturePosition.y - 20, 40, 40);
	}

	public void onPaint(Graphics2D g) {
		drawTargetFuturePosition(g);
	}
}
