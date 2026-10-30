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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.starship;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.Tilemap;
import com.watabou.utils.Callback;
import com.watabou.utils.GameMath;
import com.watabou.utils.Random;

import java.util.ArrayList;

//queen hive drawing. under the chars: biomass, acid, her slime trail, the lair handholds.
//over them: the cocoon on the wall, door membranes, the lair airlock + the air rushing into it.
//only pictures, the game never reads any of this
public class VarkolynVisuals {

	static final int S = DungeonTilemap.SIZE;

	private static Floor floor;
	private static Above above;

	private static void later( final Runnable r ){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				r.run();
			}
		});
	}

	//per-cell hash so every device draws the same
	private static int hash( int cell, int salt ){
		int h = cell * 73856093 ^ salt * 19349663;
		h ^= (h >>> 13);
		h *= 0x5bd1e995;
		h ^= (h >>> 15);
		return h & 0xFFFF;
	}

	// --- game calls these

	public static void membranes( final boolean up ){
		later(new Runnable() { public void run() { if (above != null) above.membranes(up); } });
	}

	public static void cocoonBursts(){
		later(new Runnable() { public void run() { if (above != null) above.burst(); } });
	}

	// *** under the characters ***

	public static class Floor extends Group {

		final StarshipBossLevel L;
		final Tilemap biomass, acid;
		final int[] bioData, acidData;
		final TextureFilm slimeFilm, holdFilm;
		final Group trail = new Group();
		final ArrayList<Image> holds = new ArrayList<>();
		int lastVersion = -1, lastTrail = -1;
		float lastTrailTime = -1;
		float t = 0, pulseT = 0;
		boolean pulse = false;

		public Floor( StarshipBossLevel level ){
			L = level;
			floor = this;
			int n = level.length();
			bioData = new int[n];
			acidData = new int[n];
			biomass = new Tilemap(VarkolynAssets.BIOMASS, new TextureFilm(VarkolynAssets.BIOMASS, S, S));
			acid = new Tilemap(VarkolynAssets.ACID, new TextureFilm(VarkolynAssets.ACID, S, S));
			rebuild();
			biomass.map(bioData, level.width());
			acid.map(acidData, level.width());
			add(biomass);
			add(acid);
			add(trail);
			slimeFilm = new TextureFilm(VarkolynAssets.SLIME, S, S);
			holdFilm = new TextureFilm(VarkolynAssets.HANDHOLD, S, S);

			VarkolynLair lair = VarkolynLair.find();
			if (lair != null){
				for (int c : lair.handholds){
					Image h = new Image(VarkolynAssets.HANDHOLD);
					h.frame(holdFilm.get(0));
					h.x = (c % L.width()) * S;
					h.y = (c / L.width()) * S;
					holds.add(h);
					add(h);
				}
			}
		}

		@Override
		public void destroy() {
			super.destroy();
			if (floor == this) floor = null;
		}

		private void rebuild(){
			Hive h = Hive.get(L);
			int frame = ((int) (t * 4)) % 4;
			for (int i = 0; i < bioData.length; i++){
				bioData[i] = -1;
				acidData[i] = -1;
				if (h == null) continue;
				if (h.biomass(i)){
					int v = hash(i, 3) % 4;
					bioData[i] = pulse && hash(i, 7) % 3 == 0 ? v + 4 : v;
				}
				if (h.acidAt(i) > 0) acidData[i] = (frame + hash(i, 5)) % 4;
			}
		}

		@Override
		public void update() {
			super.update();
			float e = Game.elapsed;
			t += e;
			pulseT += e;
			Hive h = Hive.get(L);
			boolean redraw = h != null && h.version != lastVersion;
			if (pulseT > 0.5f){
				pulseT = 0;
				pulse = !pulse;
				redraw = true;
			}
			if (redraw){
				lastVersion = h == null ? -1 : h.version;
				rebuild();
				biomass.updateMap();
				acid.updateMap();
			}

			//slime trail: fresh drops, then dry, then gone
			VarkolynLeader q = StarshipBossLevel.queen(L);
			float now = Actor.now();
			if (q != null && (q.trail.length != lastTrail || (q.trail.length > 0 && q.trailTime[q.trail.length - 1] != lastTrailTime)
					|| ((int) t) % 2 == 0 && pulseT == 0)){
				lastTrail = q.trail.length;
				lastTrailTime = q.trail.length > 0 ? q.trailTime[q.trail.length - 1] : -1;
				trail.clear();
				for (int i = 0; i < q.trail.length; i++){
					float age = now - q.trailTime[i];
					if (age > 30) continue;
					int c = q.trail[i];
					Image d = new Image(VarkolynAssets.SLIME);
					d.frame(slimeFilm.get(age > 18 ? 4 + hash(c, 2) % 2 : hash(c, 2) % 4));
					d.x = (c % L.width()) * S;
					d.y = (c / L.width()) * S;
					trail.add(d);
				}
			} else if (q == null && trail.length > 0){
				trail.clear();
			}

			//handholds light up while the airlock pulls
			boolean pull = L.airlock == StarshipBossLevel.AIRLOCK_ARMED || L.airlock == StarshipBossLevel.AIRLOCK_OPEN;
			for (Image hold : holds) hold.frame(holdFilm.get(pull && ((int) (t * 4)) % 2 == 0 ? 1 : 0));
		}
	}

	// *** over the characters ***

	public static class Above extends Group {

		final StarshipBossLevel L;
		final TextureFilm cocoonFilm, membraneFilm, airlockFilm;
		Image cocoon, airlock;
		final ArrayList<Image> membranes = new ArrayList<>();
		final ArrayList<ColorBlock> gusts = new ArrayList<>();
		float t = 0, burstT = -1, membraneT = -1, airlockT = 0;
		boolean membranesUp = false;
		int lastAirlock = -1;
		int airlockCell = -1;

		public Above( StarshipBossLevel level ){
			L = level;
			above = this;
			cocoonFilm = new TextureFilm(VarkolynAssets.COCOON, 32, 48);
			membraneFilm = new TextureFilm(VarkolynAssets.MEMBRANE, S, 32);
			airlockFilm = new TextureFilm(VarkolynAssets.AIRLOCK, 48, 32);

			int w = L.width();
			cocoon = new Image(VarkolynAssets.COCOON);
			cocoon.x = (L.cocoon % w + 0.5f) * S - 16;
			cocoon.y = (L.cocoon / w + 1) * S - 48;
			VarkolynLeader q = StarshipBossLevel.queen(L);
			cocoon.frame(cocoonFilm.get(q == null || q.intro >= 3 ? 5 : 0));
			add(cocoon);

			for (int d : L.doors){
				Image m = new Image(VarkolynAssets.MEMBRANE);
				m.frame(membraneFilm.get(3));
				m.x = (d % w) * S;
				m.y = (d / w + 1) * S - 32;
				m.visible = L.locked;
				membranes.add(m);
				add(m);
			}
			membranesUp = L.locked;

			VarkolynLair lair = VarkolynLair.find();
			if (lair != null && lair.airlockCenter != -1){
				airlockCell = lair.airlockCenter;
				airlock = new Image(VarkolynAssets.AIRLOCK);
				airlock.frame(airlockFilm.get(0));
				airlock.x = (airlockCell % w - 1) * S;
				airlock.y = (airlockCell / w + 1) * S - 32;
				add(airlock);
			}
		}

		@Override
		public void destroy() {
			super.destroy();
			if (above == this) above = null;
		}

		void membranes( boolean up ){
			membranesUp = up;
			membraneT = 0;
			for (Image m : membranes) m.visible = true;
		}

		void burst(){
			burstT = 0;
		}

		@Override
		public void update() {
			super.update();
			float e = Game.elapsed;
			t += e;
			VarkolynLeader q = StarshipBossLevel.queen(L);

			//cocoon beats, swells, bursts
			if (burstT >= 0){
				burstT += e;
				cocoon.frame(cocoonFilm.get(burstT < 0.15f ? 3 : burstT < 0.4f ? 4 : 5));
				cocoon.x = (L.cocoon % L.width() + 0.5f) * S - 16;
			} else if (q != null && q.intro < 3){
				int f;
				if (q.intro == 2) f = 2;
				else f = ((int) (t * (q.intro == 1 ? 6 : 2))) % 2;
				cocoon.frame(cocoonFilm.get(f));
				float shake = q.intro == 2 ? (float) Math.sin(t * 40) * 1.2f : 0;
				cocoon.x = (L.cocoon % L.width() + 0.5f) * S - 16 + shake;
			} else {
				cocoon.frame(cocoonFilm.get(5));
			}

			//door membrane
			if (membraneT >= 0){
				membraneT += e;
				int f = membranesUp ? Math.min(3, (int) (membraneT * 6)) : 4;
				for (Image m : membranes) m.frame(membraneFilm.get(f));
				if (!membranesUp && membraneT > 0.6f){
					for (Image m : membranes) m.visible = false;
					membraneT = -1;
				} else if (membranesUp && membraneT > 0.7f){
					membraneT = -1;
				}
			} else if (membranesUp){
				//slow beat, like a heart. creepy on purpose
				for (Image m : membranes) m.frame(membraneFilm.get(((int) (t * 1.5f)) % 3 == 0 ? 2 : 3));
			}

			//airlock + air rushing out
			if (airlock != null){
				int state = L.airlock;
				if (state != lastAirlock){
					lastAirlock = state;
					airlockT = 0;
				}
				airlockT += e;
				int f;
				switch (state){
					case StarshipBossLevel.AIRLOCK_ARMED: f = ((int) (t * 5)) % 2 == 0 ? 1 : 0; break;
					case StarshipBossLevel.AIRLOCK_OPEN: f = airlockT < 0.25f ? 2 : 3; break;
					case StarshipBossLevel.AIRLOCK_RECHARGE: f = airlockT < 0.3f ? 4 : 0; break;
					default: f = 0; break;
				}
				airlock.frame(airlockFilm.get(f));
				gusts(state == StarshipBossLevel.AIRLOCK_OPEN, e);
			}
		}

		//air streaks flying into the airlock
		private void gusts( boolean on, float e ){
			if (on && Random.Float() < e * 30){
				VarkolynLair lair = VarkolynLair.find();
				if (lair != null){
					ColorBlock g = new ColorBlock(1, 5, 0xFFDDEEFF);
					g.x = (lair.left + 1 + Random.Float(lair.width() - 2)) * S;
					g.y = (lair.bottom - Random.Float(lair.height() - 2)) * S;
					g.alpha(0.6f);
					gusts.add(g);
					add(g);
				}
			}
			float tx = (airlockCell % L.width() + 0.5f) * S, ty = (airlockCell / L.width() + 1) * S;
			for (ColorBlock g : gusts.toArray(new ColorBlock[0])){
				float dx = tx - g.x, dy = ty - g.y;
				float d = (float) Math.sqrt(dx * dx + dy * dy);
				if (d < 4 || !on){
					g.alpha(g.alpha() - e * 3);
					if (g.alpha() <= 0 || d < 4){
						gusts.remove(g);
						g.killAndErase();
						continue;
					}
				}
				float sp = 220 * e / Math.max(1, d);
				g.x += dx * sp;
				g.y += dy * sp;
				g.alpha(GameMath.gate(0, g.alpha(), 0.8f));
			}
		}
	}
}
