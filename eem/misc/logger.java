// -*- java -*-
package eem.misc;

public class logger {
	// logger staff
	// debug levels
	int dbg_important=0;
	int dbg_rutine=5;
	int dbg_debuging=6;
	int dbg_noise=10;
	int verbosity_level=6; // current level, smaller is less noisy

	public logger (int level) {
		verbosity_level = level;
	}

	public void dbg(int level, String s) {
		if (level <= verbosity_level)
			System.out.println(s);
	}
}

