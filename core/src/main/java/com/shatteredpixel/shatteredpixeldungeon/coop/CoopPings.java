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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;

//marks on the map for the group: "come here", "danger", "item". only pictures, the game never
//reads them, so they go straight over the wire and not through the turns
public class CoopPings {

	public static final int COME = 0, DANGER = 1, ITEM = 2;
	private static final int[] COLORS = {0x44DDFF, 0xFF4444, 0xFFDD33};
	private static final String[] KEYS = {"come", "danger", "item"};

	//the next tap on the map marks a cell instead of moving (CellSelector asks)
	private static volatile int picking = -1;

	public static boolean picking(){
		return picking >= 0;
	}

	//the HUD button: what kind of mark, then the cell
	public static void askKind( final int cell ){
		GameScene.showUntracked(new WndOptions(Icons.get(Icons.TARGET),
				Messages.get(CoopPings.class, "title"),
				Messages.get(CoopPings.class, cell < 0 ? "body" : "body_cell"),
				Messages.get(CoopPings.class, "come"),
				Messages.get(CoopPings.class, "danger"),
				Messages.get(CoopPings.class, "item")){
			@Override
			protected void onSelect( int index ) {
				if (index < 0 || index > ITEM) return;
				if (cell >= 0){
					send(cell, index);
				} else {
					picking = index;
					GLog.i(Messages.get(CoopPings.class, "pick"));
				}
			}
		});
	}

	//CellSelector: the tap after choosing the kind
	public static void picked( int cell ){
		int kind = picking;
		picking = -1;
		if (kind >= 0 && cell >= 0) send(cell, kind);
	}

	private static void send( int cell, int kind ){
		CoopSession s = CoopSession.current;
		if (s == null || Dungeon.level == null || cell >= Dungeon.level.length()) return;
		s.ping(cell, kind, floorKey());
		show(cell, kind, s.myName, floorKey());
	}

	public static int floorKey(){
		return Dungeon.depth + 1000 * Dungeon.branch;
	}

	//any thread: a mark from someone (or ourselves). only on the floor it was made on
	public static void show( final int cell, final int kind, final String who, final int floor ){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				if (Dungeon.level == null || floor != floorKey() || cell < 0 || cell >= Dungeon.level.length()) return;
				int k = Math.max(COME, Math.min(ITEM, kind));
				GameScene.effectOverFog(new Mark(cell, COLORS[k]));
				Sample.INSTANCE.play(Assets.Sounds.BEACON, 0.6f, k == DANGER ? 1.3f : 1f);
				String text = (who == null ? "?" : who) + ": " + Messages.get(CoopPings.class, "said_" + KEYS[k]);
				if (k == DANGER) GLog.n(text);
				else GLog.p(text);
			}
		});
	}

	//the target picture on the cell, blinking for a few seconds
	private static class Mark extends Image {

		private float left = 6f;

		Mark( int cell, int color ){
			super(Icons.get(Icons.TARGET));
			hardlight(color);
			origin.set(width / 2f, height / 2f);
			PointF p = DungeonTilemap.tileToWorld(cell);
			x = p.x + (DungeonTilemap.SIZE - width) / 2f;
			y = p.y + (DungeonTilemap.SIZE - height) / 2f;
		}

		@Override
		public void update() {
			super.update();
			left -= Game.elapsed;
			if (left <= 0){
				killAndErase();
				return;
			}
			float a = 0.55f + 0.45f * (float)Math.sin(left * 8f);
			alpha(left < 1f ? a * left : a);
			scale.set(1f + 0.15f * (float)Math.sin(left * 8f));
		}
	}
}
