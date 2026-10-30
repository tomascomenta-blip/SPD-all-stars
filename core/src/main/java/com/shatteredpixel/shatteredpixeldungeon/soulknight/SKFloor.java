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

package com.shatteredpixel.shatteredpixeldungeon.soulknight;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.PoisonParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.props.SKProps;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.starship.StarshipFeatures;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.undersea.UnderseaFeatures;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.Visual;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.HashMap;

//floor/wall hazards of the new worlds that tick every turn (saved w/ the floor). quick list:
// forest: brambles (hurt+slow, burn them), poison shrooms (spores on step)
// kingdom: spikes (up every 4th turn, tips show 1 turn before), arrow slits (aim at you, fire next turn)
// starship: laser fences 2 on/2 off until a console kills them, + blackouts
// undersea: bubble columns refill o2, ink geysers
//FloorLayer draws the floor stuff, WallLayer the slits
//TODO split this per world one day. one day
public class SKFloor extends Blob implements Hero.Doom {

	public static final int BRAMBLE = 1, MUSHROOM = 2, SPIKES = 3, LASER = 4, BUBBLES = 5;

	{
		actPriority = BUFF_PRIO;
	}

	public int phase = 0; //spikes: 0,1 down / 2 tips / 3 up. lasers reuse it: on at 2 and 3. lazy but it works
	public ArrayList<Slit> slits = new ArrayList<>();
	public boolean fencesOff = false;
	public int blackoutTimer = -1; //-1 = never
	public int inkTimer = -1;      //same

	public boolean fencesOn(){
		return !fencesOff && phase >= 2;
	}

	//cache, rebuilt on change. NOT saved
	private ArrayList<Integer> cells = null;
	public int version = 0;

	public static SKFloor get( Level level ){
		return (SKFloor) level.blobs.get(SKFloor.class);
	}

	public static SKFloor getOrMake( Level level ){
		SKFloor f = get(level);
		if (f == null){
			f = new SKFloor();
			f.seed(level, 0, 0);
			level.blobs.put(SKFloor.class, f);
		}
		return f;
	}

	public void put( Level level, int cell, int kind ){
		if (cur == null) cur = new int[level.length()];
		if (off == null) off = new int[level.length()];
		cur[cell] = kind;
		changed();
	}

	public int kindAt( int cell ){
		return cur == null || cell < 0 || cell >= cur.length ? 0 : cur[cell];
	}

	private void changed(){
		cells = null;
		version++;
		volume = 0;
		for (int v : cur) if (v > 0) volume++;
	}

	public synchronized ArrayList<Integer> cells(){
		if (cells == null){
			cells = new ArrayList<>();
			if (cur != null){
				for (int i = 0; i < cur.length; i++) if (cur[i] > 0) cells.add(i);
			}
		}
		return cells;
	}

	@Override
	public boolean act() {
		spend(TICK);
		if (Dungeon.level == null || Dungeon.level.blobs.get(SKFloor.class) != this || cur == null) return true;
		phase = (phase + 1) % 4;
		boolean dirty = false;
		int depth = Dungeon.scalingDepth();
		Blob fire = Dungeon.level.blobs.get(Fire.class);
		for (int c : new ArrayList<>(cells())){
			Char ch = Actor.findChar(c);
			switch (cur[c]){
				case BRAMBLE:
					if (fire != null && fire.volume > 0 && fire.cur[c] > 0){
						cur[c] = 0;
						dirty = true;
						break;
					}
					if (ch != null && ch.isAlive() && !ch.flying && !(ch instanceof SKProps.SKProp)){
						ch.damage(Math.max(1, Random.NormalIntRange(1, 2 + depth/4) - ch.drRoll()/3), this);
						if (ch.isAlive()) Buff.prolong(ch, Cripple.class, 1f);
						if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(LeafParticle.LEVEL_SPECIFIC, 2);
					}
					break;
				case MUSHROOM:
					if (ch != null && ch.isAlive() && !ch.flying){
						spores(c);
						cur[c] = 0;
						dirty = true;
					}
					break;
				case SPIKES:
					if (phase == 3 && ch != null && ch.isAlive() && !ch.flying){
						ch.damage(Math.max(1, Random.NormalIntRange(2 + depth/2, 4 + depth) - ch.drRoll()/2), this);
						if (Dungeon.level.heroFOV[c]) Sample.INSTANCE.play(Assets.Sounds.HIT_STAB, 0.8f, 1.1f);
					}
					break;
				case LASER:
					if (fencesOn() && ch != null && ch.isAlive() && !(ch instanceof SKProps.SKProp)){
						ch.damage(Math.max(1, Random.NormalIntRange(3 + depth/2, 6 + depth) - ch.drRoll()/2), this);
						if (Dungeon.level.heroFOV[c]) CellEmitter.center(c).burst(SparkParticle.FACTORY, 3);
					}
					break;
				case BUBBLES:
					if (ch instanceof Hero) UnderseaFeatures.refill((Hero) ch);
					break;
			}
		}
		for (Slit s : slits) s.tick();
		UnderseaFeatures.breathe();
		if (blackoutTimer > 0 && --blackoutTimer == 0){
			blackoutTimer = 60 + Random.Int(40);
			StarshipFeatures.blackout();
		}
		if (inkTimer > 0 && --inkTimer == 0){
			inkTimer = 30 + Random.Int(30);
			UnderseaFeatures.inkGeyser();
		}
		if (dirty) changed();
		return true;
	}

	private static void spores( int cell ){
		Level level = Dungeon.level;
		if (level.heroFOV[cell]){
			CellEmitter.center(cell).burst(PoisonParticle.SPLASH, 10);
			Sample.INSTANCE.play(Assets.Sounds.GAS, 0.6f, 1.3f);
		}
		for (int n : PathFinder.NEIGHBOURS9){
			Char ch = Actor.findChar(cell + n);
			if (ch != null && ch.isAlive() && !(ch instanceof SKProps.SKProp)){
				Buff.affect(ch, Poison.class).set(3 + Dungeon.scalingDepth()/3);
			}
		}
	}

	@Override
	public String tileDesc() {
		return null;
	}

	//death by brambles/spikes (muerte por zarzas, lol)
	@Override
	public void onDeath() {
		Dungeon.fail(this);
		GLog.n(Messages.get(SKFloor.class, "kill"));
	}

	// *** arrow slits ***

	public static class Slit implements Bundlable {

		public int wall;
		public int dx, dy;    //points to the open side
		int cooldown = 0;
		boolean armed = false;

		public Slit(){
		}

		public Slit( int wall, int dx, int dy ){
			this.wall = wall;
			this.dx = dx;
			this.dy = dy;
		}

		public ArrayList<Integer> line(){
			ArrayList<Integer> cells = new ArrayList<>();
			Level level = Dungeon.level;
			int c = wall;
			for (int i = 0; i < 9; i++){
				c += dx + dy*level.width();
				if (c < 0 || c >= level.length() || level.solid[c]) break;
				cells.add(c);
			}
			return cells;
		}

		void tick(){
			if (cooldown > 0) cooldown--;
			Level level = Dungeon.level;
			ArrayList<Integer> line = line();
			if (line.isEmpty()) return;
			if (armed){
				armed = false;
				cooldown = 5;
				float angle = (float)StrictMath.toDegrees(StrictMath.atan2(dy, dx));
				int depth = Dungeon.scalingDepth();
				SKBulletField.get().fire(null, line.get(0), angle, 3f, ItemSpriteSheet.SHOT_ARROW, 3 + depth/2, 6 + depth);
				if (level.heroFOV[wall] || level.heroFOV[line.get(0)]){
					CellEmitter.center(line.get(0)).burst(SparkParticle.FACTORY, 3);
					Sample.INSTANCE.play(Assets.Sounds.HIT_ARROW);
				}
				return;
			}
			if (cooldown > 0) return;
			for (Hero h : Party.living()){
				if (line.contains(h.pos)){
					armed = true;
					SKCombat.mark(line, 1f);
					return;
				}
			}
		}

		public boolean armed(){
			return armed;
		}

		@Override
		public void storeInBundle( Bundle b ) {
			b.put("wall", wall);
			b.put("dx", dx);
			b.put("dy", dy);
			b.put("cooldown", cooldown);
			b.put("armed", armed);
		}

		@Override
		public void restoreFromBundle( Bundle b ) {
			wall = b.getInt("wall");
			dx = b.getInt("dx");
			dy = b.getInt("dy");
			cooldown = b.getInt("cooldown");
			armed = b.getBoolean("armed");
		}
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("phase", phase);
		bundle.put("slits", slits);
		bundle.put("fences_off", fencesOff);
		bundle.put("blackout_timer", blackoutTimer);
		bundle.put("ink_timer", inkTimer);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		phase = bundle.getInt("phase");
		fencesOff = bundle.getBoolean("fences_off");
		blackoutTimer = bundle.contains("blackout_timer") ? bundle.getInt("blackout_timer") : -1;
		inkTimer = bundle.contains("ink_timer") ? bundle.getInt("ink_timer") : -1;
		slits.clear();
		for (Bundlable b : bundle.getCollection("slits")){
			if (b instanceof Slit) slits.add((Slit) b);
		}
		cells = null;
		version++;
	}

	//floor hazards, drawn under the chars (level adds it in its visuals)
	public static class FloorLayer extends Group {

		private final HashMap<Integer, Image> shown = new HashMap<>();
		private final HashMap<Integer, Gizmo> extras = new HashMap<>();
		private int seenVersion = -1;
		private int seenPhase = -1;
		private boolean seenOn = false;

		@Override
		public synchronized void update() {
			super.update();
			Level level = Dungeon.level;
			SKFloor f = level == null ? null : get(level);
			if (f == null) return;
			if (f.version != seenVersion){
				seenVersion = f.version;
				for (Image i : shown.values()) i.killAndErase();
				shown.clear();
				for (Gizmo v : extras.values()) v.killAndErase();
				extras.clear();
				for (int c : f.cells()){
					if (f.cur[c] == LASER){
						ColorBlock beam = new ColorBlock(16, 2, 0xFFFF3B3B);
						PointF p = DungeonTilemap.tileToWorld(c);
						beam.x = p.x; beam.y = p.y + 7;
						add(beam);
						extras.put(c, beam);
						continue;
					}
					if (f.cur[c] == BUBBLES){
						Emitter e = new Emitter();
						PointF p = DungeonTilemap.tileToWorld(c);
						e.pos(p.x + 4, p.y + 8, 8, 6);
						e.pour(Speck.factory(Speck.BUBBLE), 0.25f);
						add(e);
						extras.put(c, e);
						continue;
					}
					Image img = image(f.cur[c], c);
					if (img == null) continue;
					img.point(DungeonTilemap.tileToWorld(c));
					add(img);
					shown.put(c, img);
				}
				seenPhase = -1;
			}
			if (f.phase != seenPhase){
				seenPhase = f.phase;
				int frame = f.phase == 3 ? 2 : (f.phase == 2 ? 1 : 0);
				for (java.util.Map.Entry<Integer, Image> e : shown.entrySet()){
					if (f.kindAt(e.getKey()) == SPIKES) e.getValue().frame(frame*16, 0, 16, 16);
				}
			}
			for (java.util.Map.Entry<Integer, Image> e : shown.entrySet()){
				int c = e.getKey();
				e.getValue().visible = level.visited[c] || level.mapped[c];
			}
			boolean on = f.fencesOn();
			for (java.util.Map.Entry<Integer, Gizmo> e : extras.entrySet()){
				int c = e.getKey();
				boolean seen = level.visited[c] || level.mapped[c];
				if (f.kindAt(c) == LASER){
					e.getValue().visible = seen && on;
					((Visual) e.getValue()).alpha(0.75f + 0.25f*(float)Math.sin(Game.timeTotal*12));
				} else {
					e.getValue().visible = seen && level.heroFOV[c];
				}
			}
		}

		private static Image image( int kind, int cell ){
			Image img;
			switch (kind){
				case BRAMBLE:
					img = new Image(SKAssets.BRAMBLE);
					img.frame((cell % 2)*16, 0, 16, 16);
					return img;
				case MUSHROOM:
					img = new Image(SKAssets.MUSHROOM);
					img.frame(((cell/3) % 2)*16, 0, 16, 16);
					return img;
				case SPIKES:
					img = new Image(SKAssets.SPIKES);
					img.frame(0, 0, 16, 16);
					return img;
			}
			return null;
		}
	}

	//slits, over the walls
	public static class WallLayer extends Group {

		private final HashMap<Integer, Image> shown = new HashMap<>();
		private final HashMap<Integer, Boolean> lit = new HashMap<>();

		@Override
		public synchronized void update() {
			super.update();
			Level level = Dungeon.level;
			SKFloor f = level == null ? null : get(level);
			if (f == null) return;
			for (Slit s : f.slits){
				Image img = shown.get(s.wall);
				boolean armed = s.armed();
				if (img == null || lit.get(s.wall) != armed){
					if (img != null) img.killAndErase();
					img = new Image(armed ? SKAssets.ARROW_SLIT_FIRE : SKAssets.ARROW_SLIT);
					img.point(DungeonTilemap.tileToWorld(s.wall));
					add(img);
					shown.put(s.wall, img);
					lit.put(s.wall, armed);
				}
				img.visible = level.visited[s.wall] || level.mapped[s.wall];
			}
		}
	}

	public static String killMessage(){
		return Messages.get(SKFloor.class, "kill");
	}
}
