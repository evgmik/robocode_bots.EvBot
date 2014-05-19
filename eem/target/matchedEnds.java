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

	public void promote( ) {
		// increase by 1 all elements
		// use it if you find that indexes matches a longer pattern spanning in future
		for ( LinkedList<Integer> l : this ) {
			for ( int i=0; i < l.size(); i++ ) {
				int v = l.get(i);
				v++;
				l.set(i, v);
			}
		}
	}

	public void promoteAndInsert( LinkedList<Integer> indxs ) {
		// this function  inserts indxs as pattern of 1 matches
		// and then increase by 1 every element matching indxs
		
		// indxs must be not empty
		this.addFirst(indxs);
		this.promote();
		// indxs must be sorted in descending order, this happens automatically
		// if they are searched in direction of past in generating code
	}

	public void addUniqueOnlyToLowLevel( LinkedList<Integer> new_values ) {

		if (new_values.size() == 0) {
			return; // no changes
		}
		if (this.size() == 0) {
			this.addFirst(new_values);
			return;
		}
		LinkedList<Integer> old_values = this.get(0);
		// FIXME: use that matchedEnds and indxs are sorted in descendant order
		// FIXME: be careful it would require sorting updated List
		for ( int vnew : new_values ) {
			boolean unique = true;
			for ( int vold: old_values ) {
				if (vnew == vold ) {
					unique = false;
					break;
				}
			}
			if ( unique ) {
				old_values.add(vnew);
			}
		}
	}



	public String format() {
		String outStr = "List of matched ends has " + " depth " + this.size();
		int cnt = 0;
		for ( LinkedList<Integer> l : this ) {
			cnt++;
			outStr += "\n";
			outStr += " for pattern length = " + cnt + " find " + l.size() + " matches";
			//outStr += "\n";
			outStr +=(" : " + l );
		}
		return outStr;
	}
}
