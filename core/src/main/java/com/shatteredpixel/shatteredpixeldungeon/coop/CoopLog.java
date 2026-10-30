/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * All-Stars Pixel Dungeon
 * Copyright (C) 2026 tomascomenta
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.coop;

import com.watabou.utils.Random;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

//debug log for co-op, -Dspd.coop.log=<file>. it gets BIG, you've been warned
public class CoopLog {

	private static Writer out;
	private static boolean tried = false;
	private static final long start = System.currentTimeMillis();

	public static synchronized void line( String text ){
		if (!tried){
			tried = true;
			String path = System.getProperty("spd.coop.log");
			if (path != null){
				try {
					out = new OutputStreamWriter(new FileOutputStream(path, true), StandardCharsets.UTF_8);
				} catch (Exception e){
					out = null;
				}
			}
		}
		if (out == null) return;
		try {
			out.write((System.currentTimeMillis() - start) + " [" + Thread.currentThread().getName() + "] " + text + "\n");
			out.flush();
		} catch (Exception e){
			//logging must never break the game
		}
	}

	public static boolean enabled(){
		return System.getProperty("spd.coop.log") != null;
	}

	// *** where did the render thread take shared random numbers? (testing) ***
	private static final String[] recent = new String[24];
	private static final boolean TRACE_ALL = "1".equals(System.getProperty("spd.coop.rngtrace"));
	private static int recentPos = 0;

	public static void installRandomTrace(){
		if (!enabled()) return;
		installVisualTrace();
		Random.debugHook = new Random.DebugHook() {
			@Override
			public void onRenderThreadDraw() {
				StackTraceElement[] st = new Throwable().getStackTrace();
				StringBuilder sb = new StringBuilder();
				int n = 0;
				for (StackTraceElement e : st){
					String c = e.getClassName();
					if (c.startsWith("com.watabou.utils.Random") || c.startsWith("com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog")) continue;
					sb.append(c.substring(c.lastIndexOf('.') + 1)).append('.').append(e.getMethodName())
							.append(':').append(e.getLineNumber()).append(" < ");
					if (++n >= 7) break;
				}
				synchronized (recent){
					recent[recentPos] = sb.toString();
					recentPos = (recentPos + 1) % recent.length;
				}
				if (TRACE_ALL) line("R " + sb);
			}
		};
	}

	private static final java.util.HashSet<String> visualSites = new java.util.HashSet<>();

	//reports game logic classes that use randomness from interface code (they may change shared state)
	private static void installVisualTrace(){
		Random.visualHook = new Random.DebugHook() {
			@Override
			public void onRenderThreadDraw() {
				StackTraceElement[] st = new Throwable().getStackTrace();
				for (StackTraceElement e : st){
					String c = e.getClassName();
					if (c.startsWith("com.watabou") || c.contains(".coop.")) continue;
					if (c.matches("com\\.shatteredpixel\\.shatteredpixeldungeon\\.(actors|items|levels|plants|mechanics)\\..*")){
						String site = c.substring(c.lastIndexOf('.') + 1) + "." + e.getMethodName() + ":" + e.getLineNumber();
						boolean isNew;
						synchronized (visualSites){
							isNew = visualSites.add(site);
						}
						if (isNew){
							StringBuilder sb = new StringBuilder("UI-RNG " + site);
							int n = 0;
							for (StackTraceElement f : st){
								if (f.getClassName().startsWith("com.watabou.utils.Random")) continue;
								sb.append(" < ").append(f.getClassName().substring(f.getClassName().lastIndexOf('.') + 1)).append('.').append(f.getMethodName());
								if (++n >= 6) break;
							}
							line(sb.toString());
						}
					}
					return;
				}
			}
		};
	}

	public static void dumpRandomTrace(){
		if (!enabled()) return;
		synchronized (recent){
			for (int i = 0; i < recent.length; i++){
				String s = recent[(recentPos + i) % recent.length];
				if (s != null) line("  rnd@render " + s);
			}
		}
	}
}
