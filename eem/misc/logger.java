// -*- java -*-
package eem.misc;

public class logger {
	// logger staff
	// debug levels
	public final int dbg_important=0;
	public final int dbg_rutine=5;
	public final int dbg_debuging=6;
	public final int dbg_noise=10;
	int verbosity_level=6; // current level, smaller is less noisy

	public logger (int vLevel) {
		verbosity_level = vLevel;
	}

	public void dbg(int level, String s) {
		if (level <= verbosity_level)
			System.out.println(s);
	}
}

