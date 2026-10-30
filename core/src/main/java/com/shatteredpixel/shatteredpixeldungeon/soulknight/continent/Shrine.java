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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Torch;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.hunt.ColossusForge;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.props.SKProps;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Point;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Collections;

//a shrine of light, besieged by yogs. touch it and it starts to wake up: keep someone close
//and the yogs off it until it's full, while they keep coming in waves. then it gives a drop of
//sacred essence, and from then on it's a safe spot that washes the corruption off
public class Shrine extends SKProps.SKProp {

	{
		spriteClass = ShrineSprite.class;
		HP = HT = 1000;
	}

	public static final int SIEGED = 0, CHARGING = 1, PURE = 2;

	public int state = SIEGED;
	public int charge = 0, need = 24;
	public int wave = 2;
	public int source = Continent.SHRINE_ASH;

	public static void place( Level level, ShrineRoom room, int source ){
		int cell = level.pointToCell(room.center());
		Shrine s = new Shrine();
		s.source = source;
		s.pos = cell;
		level.mobs.add(s);
		//the siege: a few of them already camping around it
		ArrayList<Class<? extends Mob>> siege = ContinentMobs.siege();
		for (int i = 0; i < 4; i++){
			int at = cell + PathFinder.NEIGHBOURS8[Random.Int(8)] * 2;
			if (at < 0 || at >= level.length() || !level.passable[at] || level.findMob(at) != null) continue;
			Mob m = Reflection.newInstance(Random.element(siege));
			m.pos = at;
			level.mobs.add(m);
		}
	}

	@Override
	public boolean isInvulnerable( Class effect ) {
		return true;
	}

	@Override
	public boolean heroShouldInteract() {
		return true;
	}

	@Override
	public boolean interact( Char c ) {
		if (!(c instanceof Hero)) return true;
		if (state == SIEGED){
			state = CHARGING;
			wave = 2;
			CoopLog.line("shrine: charging, source " + source);
			GLog.w(Messages.get(Shrine.class, "wakes"));
			Sample.INSTANCE.play(Assets.Sounds.CHARGEUP, 1f, 0.7f);
			if (sprite instanceof ShrineSprite) ((ShrineSprite) sprite).state(state);
		} else if (state == CHARGING){
			GLog.i(Messages.get(Shrine.class, "progress", charge, need));
		} else {
			GLog.i(Messages.get(Shrine.class, "pure"));
		}
		return true;
	}

	private ArrayList<Hero> heroesNear( int dist ){
		ArrayList<Hero> list = new ArrayList<>();
		for (Hero h : Party.active() ? Party.living() : Collections.singletonList(Dungeon.hero)){
			if (h != null && h.isAlive() && Dungeon.level.distance(h.pos, pos) <= dist) list.add(h);
		}
		return list;
	}

	@Override
	protected boolean act() {
		//-Dspd.test.shrine=1: wakes up by itself and counts as guarded
		boolean test = DevFlags.shrineTest();
		if (test && state == SIEGED) interact(Dungeon.hero);
		if (state == CHARGING){
			boolean guarded = test || !heroesNear(3).isEmpty();
			boolean blocked = false;
			for (int n : PathFinder.NEIGHBOURS8){
				Char ch = Actor.findChar(pos + n);
				if (ch instanceof Yog && ch.alignment == Alignment.ENEMY) blocked = true;
			}
			if (blocked){
				charge = Math.max(0, charge - 1);
			} else if (guarded){
				charge++;
				if (sprite != null && charge % 6 == 0){
					sprite.showStatus(CharSprite.POSITIVE, charge + "/" + need);
				}
			}
			if (--wave <= 0){
				int living = Party.active() ? Party.living().size() : 1;
				wave = living >= 3 ? 3 : 4;
				spawnWave(1 + living / 2);
			}
			if (charge >= need) purify();
		} else if (state == PURE){
			for (Hero h : heroesNear(2)){
				DarkTaint t = h.buff(DarkTaint.class);
				if (t != null) t.cleanse(2);
			}
		}
		spend(TICK);
		return true;
	}

	private void spawnWave( int count ){
		ArrayList<Class<? extends Mob>> siege = ContinentMobs.siege();
		ArrayList<Integer> cells = new ArrayList<>();
		for (int i = 0; i < Dungeon.level.length(); i++){
			int d = Dungeon.level.distance(i, pos);
			if (d >= 4 && d <= 8 && Dungeon.level.passable[i] && Actor.findChar(i) == null) cells.add(i);
		}
		for (int i = 0; i < count && !cells.isEmpty(); i++){
			int at = cells.remove(Random.Int(cells.size()));
			Mob m = Reflection.newInstance(Random.element(siege));
			m.pos = at;
			m.state = m.HUNTING;
			GameScene.add(m, 1f);
			m.beckon(pos);
		}
	}

	private void purify(){
		state = PURE;
		//some runs this shrine's drop is long gone: the light comes back all the same
		boolean drop = Continent.essenceAt(source);
		CoopLog.line("shrine: pure, source " + source + (drop ? "" : ", dry"));
		GLog.p(Messages.get(Shrine.class, drop ? "purified" : "purified_dry"));
		Sample.INSTANCE.play(Assets.Sounds.MASTERY);
		if (sprite instanceof ShrineSprite) ((ShrineSprite) sprite).state(state);
		CellEmitter.get(pos).burst(Speck.factory(Speck.LIGHT), 20);
		for (Hero h : heroesNear(3)){
			DarkTaint t = h.buff(DarkTaint.class);
			if (t != null) t.cleanse(100);
		}
		//the light pushes them back
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (m instanceof Yog && m.isAlive() && Dungeon.level.distance(m.pos, pos) <= 3) m.damage(m.HT, this);
		}
		if (drop) dropNear(new SacredEssence(source));
		dropNear(new Torch().quantity(2));
		ColossusForge.raise(pos);
	}

	private void dropNear( Item item ){
		for (int n : PathFinder.NEIGHBOURS8){
			int c = pos + n;
			if (Dungeon.level.passable[c] && Actor.findChar(c) == null && Dungeon.level.heaps.get(c) == null){
				Dungeon.level.drop(item, c).sprite.drop(pos);
				return;
			}
		}
		Dungeon.level.drop(item, pos + PathFinder.NEIGHBOURS8[0]);
	}

	@Override
	public String description() {
		switch (state){
			case SIEGED: default: return Messages.get(this, "desc_sieged");
			case CHARGING: return Messages.get(this, "desc_charging", charge, need);
			case PURE: return Messages.get(this, "desc_pure");
		}
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("state", state);
		bundle.put("charge", charge);
		bundle.put("need", need);
		bundle.put("wave", wave);
		bundle.put("source", source);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		state = bundle.getInt("state");
		charge = bundle.getInt("charge");
		need = bundle.getInt("need");
		wave = bundle.getInt("wave");
		source = bundle.getInt("source");
	}

	//a big open room with flagstones around the shrine
	public static class ShrineRoom extends StandardRoom {

		@Override
		public float[] sizeCatProbs() {
			return new float[]{0, 1, 0};
		}

		@Override
		public void paint( Level level ) {
			Painter.fill(level, this, Terrain.WALL);
			Painter.fill(level, this, 1, Terrain.EMPTY);
			Point c = center();
			Painter.fill(level, c.x - 1, c.y - 1, 3, 3, Terrain.EMPTY_SP);
			for (Door door : connected.values()){
				door.set(Door.Type.REGULAR);
			}
		}

		//nothing gets dropped on the shrine's own flagstones
		@Override
		public boolean canPlaceItem( Point p, Level l ) {
			Point c = center();
			return super.canPlaceItem(p, l) && Math.max(Math.abs(p.x - c.x), Math.abs(p.y - c.y)) > 1;
		}
	}

	//32x32: 0 besieged, 1-2 charging, 3 pure
	public static class ShrineSprite extends MobSprite {

		private final TextureFilm film;

		public ShrineSprite(){
			super();
			texture(ContinentAssets.SHRINE);
			film = new TextureFilm(texture, 32, 32);
			state(SIEGED);
		}

		public void state( int st ){
			idle = new Animation(st == CHARGING ? 4 : 1, true);
			if (st == CHARGING) idle.frames(film, 1, 2);
			else idle.frames(film, st == PURE ? 3 : 0);
			run = idle.clone();
			attack = idle.clone();
			zap = idle.clone();
			die = idle.clone();
			play(idle, true);
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (ch instanceof Shrine) state(((Shrine) ch).state);
		}

		@Override
		public int blood() {
			return 0xFFFFF0C0;
		}
	}
}
