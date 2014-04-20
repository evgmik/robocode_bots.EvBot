// -*- java -*-

package eem.target;

import eem.EvBot;
import eem.target.*;
import eem.misc.*;
import java.awt.geom.Point2D;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.*;
import java.lang.Integer;
import robocode.*;

public class matchedEnds extends LinkedList<LinkedList<Integer>> {
	// special class to keep list of lists of pattern marched ends for a given InfoBot
	// each next list keeps ends with list # of pattern points
	
	//LinkedList<LinkedList<Integer>> endsListVsPatternLength = new LinkedList<LinkedList<Integer>>();
	public matchedEnds() {
		super();
	}

	public LinkedList<Integer> flatten() { 
		LinkedList<Integer> endIndexes = new LinkedList<Integer>();
		for ( LinkedList<Integer> l : this ) {
			for ( int i : l ) {
				endIndexes.add(i);
			}
		}
		return endIndexes;
	}

	public LinkedList<Integer> getEndsForPatternSizeN( int n) { 
		if ( n <= 0 ) // check input argument
			return null;
		if ( this.size() < n )
			return null;

		return this.get(n-1);
	}
	
	public int totalMatches() {
		int cnt = 0;
		for ( LinkedList<Integer> l : this ) {
			cnt += l.size();	
		}
		return cnt;
	}

	public void removePoint(Integer i) {
		LinkedList<LinkedList<Integer>> listsToRemove = new LinkedList<LinkedList<Integer>>();
		// first we remove element "i" from all list
		for ( LinkedList<Integer> l : this ) {
			l.remove(i);
			if ( l.size() == 0 ) // if empty schedule to remove
				listsToRemove.add(l);
		}
		// we are lucky next pattern depth will be empty if previous is empty
		// so we do not need special checks
		// remove sheduled empty lists
		for ( LinkedList<Integer> l : listsToRemove ) {
			this.remove(l);
		}
	}

	public String format() {
		String outStr = "List of matched ends has " + this.size() + " depth";
		int cnt = 0;
		for ( LinkedList<Integer> l : this ) {
			cnt++;
			outStr += "\n";
			outStr += " for pattern length = " + cnt + " find " + l.size() + " matches";
			outStr += "\n";
			outStr +=("  matches = " + l );
		}
		return outStr;
	}
}
