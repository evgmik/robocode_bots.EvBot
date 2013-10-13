// -*- java -*-
package eem.misc;

public class logger {
	// logger staff
	// debug levels
	public final static int log_important=0;
	public final static int log_error=3;
	public final static int log_profiler=11;
	public final static int log_routine=4;
	public final static int log_debuging=5;
	public final static int log_noise=10;
	static int verbosity_level=log_debuging; // current level, smaller is less noisy

	public logger (int vLevel) {
		verbosity_level = vLevel;
	}

	public logger () {
	}

	public static void log_message(int level, String s) {
		if (level <= verbosity_level)
			System.out.println(s);
	}

	public static void noise(String s) {
		log_message(log_noise, s);
	}

	public static void profiler(String s) {
		log_message(log_profiler, s);
	}

	public static void error(String s) {
		log_message(log_error, s);
	}

	public static void dbg(String s) {
		log_message(log_debuging, s);
	}

	public static void routine(String s) {
		log_message(log_routine, s);
	}

	public static void important(String s) {
		log_message(log_important, s);
	}
}

