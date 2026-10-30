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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.forest;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAssets;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Callback;
import com.watabou.utils.GameMath;
import com.watabou.utils.PointF;

import java.util.ArrayList;

//clearing eye candy: goblins dancing around the bonfire + 2 drummers before the fight (they run into the
//jungle when it starts), the safe ground by the sacred stones while a star comes, and on top the door
//roots, the giant star itself and the red wisps when the priest drinks the totems.
//only pictures, nothing in here touches game state. keep it that way or coop desyncs
public class PriestVisuals {

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

	private static float cx( int cell ){
		return (cell % Dungeon.level.width() + 0.5f) * S;
	}

	private static float cy( int cell ){
		return (cell / Dungeon.level.width() + 0.5f) * S;
	}

	// --- hooks the game calls

	public static void drumsStop(){
		later(new Runnable() { public void run() { if (floor != null) floor.drumsStop(); } });
	}

	public static void dancersFlee(){
		later(new Runnable() { public void run() { if (floor != null) floor.flee(); } });
	}

	public static void rootsGrow(){
		later(new Runnable() { public void run() { if (above != null) above.roots(true); } });
	}

	public static void rootsWither(){
		later(new Runnable() { public void run() { if (above != null) above.roots(false); } });
	}

	public static void absorb( final int cell ){
		later(new Runnable() { public void run() { if (above != null) above.absorb(cell); } });
	}

	public static void starComing( final int cell ){
		later(new Runnable() { public void run() { if (above != null) above.starComing(cell); } });
	}

	public static void starFalls(){
		later(new Runnable() { public void run() { if (above != null) above.starEnds(true); } });
	}

	public static void starBroken(){
		later(new Runnable() { public void run() { if (above != null) above.starEnds(false); } });
	}

	public static void priestFell(){
		later(new Runnable() { public void run() { if (floor != null) floor.scatter(); } });
	}

	// *** under the characters ***

	public static class Floor extends Group {

		final ForestBossLevel L;
		final ArrayList<Image> dancers = new ArrayList<>();
		final ArrayList<Float> angles = new ArrayList<>();
		final ArrayList<Image> drums = new ArrayList<>();
		final ArrayList<Image> drummers = new ArrayList<>();
		final ArrayList<ColorBlock> safe = new ArrayList<>();
		final TextureFilm danceFilm, drumFilm;
		float t = 0, fleeT = -1, beatT = 0;
		boolean stopped = false;

		public Floor( ForestBossLevel level ){
			L = level;
			floor = this;
			GoblinPriest p = ForestBossLevel.priest(level);
			int intro = p == null ? 4 : p.intro;

			Image probe = new Image(SKAssets.GOBLIN_SPEAR);
			danceFilm = new TextureFilm(probe.texture, 12, 15);
			Image probeDrum = new Image(SKAssets.GOBLIN_DRUM);
			drumFilm = new TextureFilm(probeDrum.texture, 16, 16);

			//safe ground next to each stone, lit while a star comes
			for (int s : L.stoneCells){
				for (int dy = -1; dy <= 1; dy++){
					for (int dx = -1; dx <= 1; dx++){
						if (dx == 0 && dy == 0) continue;
						int c = s + dx + dy * L.width();
						if (L.solid[c] || !L.inClearing(c)) continue;
						ColorBlock b = new ColorBlock(S - 2, S - 2, 0xFF60B0FF);
						b.x = (c % L.width()) * S + 1;
						b.y = (c / L.width()) * S + 1;
						b.alpha(0);
						safe.add(b);
						add(b);
					}
				}
			}

			if (intro >= 2 || p == null) return;
			stopped = intro >= 1;

			//6 goblins dancing around the fire
			String[] looks = {SKAssets.GOBLIN_SPEAR, SKAssets.GOBLIN_PISTOL};
			for (int i = 0; i < 6; i++){
				Image g = new Image(looks[i % 2]);
				g.frame(danceFilm.get(4 + i % 4));
				angles.add(i * 60f);
				dancers.add(g);
				add(g);
			}
			//2 drummers behind the fire. bum bum
			int w = L.width();
			int[] spots = {L.bonfire - 3 - 2 * w, L.bonfire + 3 - 2 * w};
			for (int i = 0; i < spots.length; i++){
				int c = spots[i];
				if (c < 0 || c >= L.length() || L.solid[c]) continue;
				Image d = new Image(SKAssets.GOBLIN_DRUM);
				d.frame(drumFilm.get(0));
				d.x = cx(c) - 8;
				d.y = cy(c) - 8 + 2;
				drums.add(d);
				add(d);
				Image g = new Image(SKAssets.GOBLIN_SPEAR);
				g.frame(danceFilm.get(2));
				g.x = cx(c) + (i == 0 ? 3 : -15);
				g.y = cy(c) - 10;
				if (i == 1) g.scale.x = -1;
				if (i == 1) g.origin.set(6, 0);
				drummers.add(g);
				add(g);
			}
			place(0);
		}

		@Override
		public void destroy() {
			super.destroy();
			if (floor == this) floor = null;
		}

		void drumsStop(){
			stopped = true;
		}

		void flee(){
			stopped = true;
			if (fleeT < 0 && !dancers.isEmpty()) fleeT = 0;
		}

		void scatter(){
			for (Image i : dancers) i.killAndErase();
			for (Image i : drums) i.killAndErase();
			for (Image i : drummers) i.killAndErase();
			dancers.clear();
			drums.clear();
			drummers.clear();
		}

		//dancers hop around the fire, kinda like a
		private void place( float e ){
			float fx = cx(L.bonfire), fy = cy(L.bonfire) + 2;
			for (int i = 0; i < dancers.size(); i++){
				Image g = dancers.get(i);
				float a = angles.get(i);
				if (!stopped) a += e * 40f;
				angles.set(i, a);
				double r = Math.toRadians(a);
				float rx = 1.9f * S, ry = 1.5f * S;
				float hop = stopped ? 0 : Math.abs((float) Math.sin(t * 6 + i)) * 3;
				float out = fleeT < 0 ? 0 : fleeT * 4f * S;
				g.x = fx + (float) Math.cos(r) * (rx + out) - 6;
				g.y = fy + (float) Math.sin(r) * (ry + out) - 13 - hop;
				g.scale.x = Math.cos(r) < 0 ? 1 : -1;
				g.origin.set(6, 0);
				if (!stopped) g.frame(danceFilm.get(4 + ((int) (t * 8) + i) % 4));
				else g.frame(danceFilm.get(0));
			}
		}

		@Override
		public void update() {
			super.update();
			float e = Game.elapsed;
			t += e;
			GoblinPriest p = ForestBossLevel.priest(L);

			if (!dancers.isEmpty()){
				place(e);
				if (fleeT >= 0){
					fleeT += e;
					float a = GameMath.gate(0, 1 - fleeT / 1.2f, 1);
					for (Image g : dancers) g.alpha(a);
					for (Image g : drummers) g.alpha(a);
					for (Image d : drums) d.alpha(a);
					if (fleeT > 1.3f) scatter();
				}
			}
			//drums go until the priest turns
			if (!drums.isEmpty() && !stopped){
				int f = ((int) (t * 4)) % 2 == 0 ? 2 : 3;
				for (Image d : drums) d.frame(drumFilm.get(f));
				for (int i = 0; i < drummers.size(); i++) drummers.get(i).frame(danceFilm.get(2 + (f - 2 + i) % 2));
				beatT += e;
				if (beatT >= 0.5f){
					beatT = 0;
					if (Dungeon.level == L && L.heroFOV[L.bonfire]) Sample.INSTANCE.play(Assets.Sounds.HIT, 0.35f, 0.55f);
				}
			} else if (!drums.isEmpty()){
				for (Image d : drums) d.frame(drumFilm.get(0));
			}

			//safe ground pulses while the star comes. numbers eyeballed
			float target = p != null && p.starTimer > 0 ? 0.28f + 0.12f * (float) Math.sin(t * 5) : 0f;
			for (ColorBlock b : safe){
				b.alpha(b.alpha() + GameMath.gate(-e * 2, target - b.alpha(), e * 2));
			}
		}
	}

	// *** over the characters ***

	public static class Above extends Group {

		final ForestBossLevel L;
		final ArrayList<Image> roots = new ArrayList<>();
		final TextureFilm rootFilm, starFilm;
		float rootT = -1;
		boolean rootsUp = false;

		Image star;
		float starT = -1, endT = -1;
		boolean starFell = false;
		int starCell = -1;

		final ArrayList<ColorBlock> wisps = new ArrayList<>();
		final ArrayList<PointF> wispFrom = new ArrayList<>();
		float wispT = -1;
		int wispTo = -1;

		float t = 0;

		public Above( ForestBossLevel level ){
			L = level;
			above = this;
			Image probe = new Image(PriestAssets.ROOTS);
			rootFilm = new TextureFilm(probe.texture, 16, 32);
			Image probeStar = new Image(PriestAssets.STAR);
			starFilm = new TextureFilm(probeStar.texture, 48, 48);

			for (int d : L.doors){
				Image r = new Image(PriestAssets.ROOTS);
				r.frame(rootFilm.get(3));
				r.x = (d % L.width()) * S;
				r.y = (d / L.width() + 1) * S - 32;
				r.visible = false;
				roots.add(r);
				add(r);
			}
			if (L.locked){
				rootsUp = true;
				for (Image r : roots) r.visible = true;
			}

			GoblinPriest p = ForestBossLevel.priest(level);
			if (p != null && p.starTimer > 0) starComing(L.bonfire);
		}

		@Override
		public void destroy() {
			super.destroy();
			if (above == this) above = null;
		}

		void roots( boolean up ){
			rootsUp = up;
			rootT = 0;
			for (Image r : roots) r.visible = true;
		}

		void absorb( int cell ){
			wispTo = cell;
			wispT = 0;
			float x0 = L.clrL * S, x1 = (L.clrR + 1) * S, y0 = L.clrT * S, y1 = (L.clrB + 1) * S;
			for (int i = 0; i < 28; i++){
				ColorBlock b = new ColorBlock(2, 2, i % 3 == 0 ? 0xFFFFB060 : 0xFFE03030);
				float k = i / 28f;
				PointF from;
				switch (i % 4){
					case 0:  from = new PointF(x0 + k * (x1 - x0), y0); break;
					case 1:  from = new PointF(x1, y0 + k * (y1 - y0)); break;
					case 2:  from = new PointF(x1 - k * (x1 - x0), y1); break;
					default: from = new PointF(x0, y1 - k * (y1 - y0)); break;
				}
				b.x = from.x;
				b.y = from.y;
				wisps.add(b);
				wispFrom.add(from);
				add(b);
			}
		}

		void starComing( int cell ){
			starCell = cell;
			starT = 0;
			endT = -1;
			if (star == null){
				star = new Image(PriestAssets.STAR);
				add(star);
			}
			star.frame(starFilm.get(0));
			star.visible = true;
			star.alpha(1);
			star.origin.set(30, 30);
		}

		void starEnds( boolean fell ){
			if (star == null || starCell == -1) return;
			starFell = fell;
			endT = 0;
			if (!fell){
				Emitter e = new Emitter();
				e.pos(star.x + 24, star.y + 24, 0, 0);
				add(e);
				e.burst(Speck.factory(Speck.STAR), 16);
			}
		}

		@Override
		public void update() {
			super.update();
			float e = Game.elapsed;
			t += e;

			//roots growing / withering
			if (rootT >= 0){
				rootT += e;
				int f;
				if (rootsUp) f = Math.min(3, (int) (rootT * 6));
				else f = 4 + Math.min(1, (int) (rootT * 4));
				for (Image r : roots) r.frame(rootFilm.get(f));
				if (!rootsUp && rootT > 0.6f){
					for (Image r : roots) r.visible = false;
					rootT = -1;
				} else if (rootsUp && rootT > 0.7f){
					rootT = -1;
				}
			}

			//totem wisps fly into the priest
			if (wispT >= 0){
				wispT += e;
				float k = GameMath.gate(0, wispT / 1.4f, 1);
				float tx = cx(wispTo), ty = cy(wispTo) - 6;
				for (int i = 0; i < wisps.size(); i++){
					ColorBlock b = wisps.get(i);
					PointF f = wispFrom.get(i);
					float kk = GameMath.gate(0, k * 1.3f - (i % 7) * 0.04f, 1);
					float ease = kk * kk;
					b.x = f.x + (tx - f.x) * ease + (float) Math.sin(t * 9 + i) * 3 * (1 - kk);
					b.y = f.y + (ty - f.y) * ease;
					b.alpha(kk >= 1 ? 0 : 1);
				}
				if (wispT > 1.6f){
					for (ColorBlock b : wisps) b.killAndErase();
					wisps.clear();
					wispFrom.clear();
					wispT = -1;
				}
			}

			//giant star comes down as the count goes, and gets bigger
			if (star != null && star.visible && starCell != -1){
				GoblinPriest p = ForestBossLevel.priest(L);
				float x = cx(starCell), y = cy(starCell);
				if (endT < 0){
					starT += e;
					float left = p != null && p.starTimer > 0 ? p.starTimer / (float) GoblinPriest.STAR_TURNS : 0.1f;
					float height = (1.5f + 6f * left) * S;
					float s = 1f + 1.2f * (1 - left);
					star.scale.set(s + 0.05f * (float) Math.sin(t * 8));
					//frames 0-2 have the star centre at (30,30), impact frames at (24,24). don't ask
					star.x = x - 30;
					star.y = y - 30 - height + (float) Math.sin(t * 2) * 2;
					star.frame(starFilm.get(((int) (t * 6)) % 3));
				} else {
					endT += e;
					if (starFell){
						star.frame(starFilm.get(3 + Math.min(2, (int) (endT * 8))));
						star.origin.set(24, 24);
						star.scale.set(3f + endT * 2f);
						star.x = x - 24;
						star.y = y - 24;
						star.alpha(GameMath.gate(0, 1 - (endT - 0.3f) / 0.5f, 1));
					} else {
						star.alpha(GameMath.gate(0, 1 - endT / 0.4f, 1));
					}
					if (endT > 0.8f){
						star.visible = false;
						starCell = -1;
					}
				}
			}
		}
	}
}
