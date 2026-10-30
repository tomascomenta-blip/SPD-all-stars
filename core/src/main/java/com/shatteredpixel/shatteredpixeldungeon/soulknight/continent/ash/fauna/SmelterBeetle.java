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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.LinkedHashSet;

//smelter beetle. a slag shell on a molten belly. it lowers its horn, the lane in front of it gets
//marked, and it charges straight down it until something stops it, leaving a furrow of molten slag.
//if it hits a wall it stands there melting for a moment
public class SmelterBeetle extends AshColossus {

	{
		size = 3;
		spriteClass = BeetleSprite.class;

		HP = HT = 380;
		defenseSkill = 14;
		EXP = 28;

		minDmg = 34; maxDmg = 46; acc = 56;
		armorMin = 10; armorMax = 22;

		properties.add(Property.FIERY);
	}

	private static final int CHARGE = 10;

	//the belly: what will be its weak spot
	@Override
	public int[][] weakSpots() {
		return new int[][]{{1, 2}};
	}

	private boolean charging = false;
	private int dazed = 0;

	@Override
	protected int specialCooldown() {
		return 5;
	}

	private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

	//centers it would go through charging that way, until it doesn't fit
	private ArrayList<Integer> path( int dx, int dy ){
		ArrayList<Integer> out = new ArrayList<>();
		boolean[] f = fits();
		int w = Dungeon.level.width();
		int x = pos % w, y = pos / w;
		for (int k = 1; k <= CHARGE; k++){
			int nx = x + dx * k, ny = y + dy * k;
			if (nx < 0 || ny < 0 || nx >= w || ny >= Dungeon.level.height()) break;
			int c = nx + ny * w;
			if (!f[c]) break;
			out.add(c);
		}
		return out;
	}

	private LinkedHashSet<Integer> lane( ArrayList<Integer> centers ){
		LinkedHashSet<Integer> out = new LinkedHashSet<>();
		int w = Dungeon.level.width();
		for (int c : centers){
			for (int y = -1; y <= 1; y++){
				for (int x = -1; x <= 1; x++){
					int k = c + x + y * w;
					if (!covers(k)) out.add(k);
				}
			}
		}
		return out;
	}

	@Override
	protected boolean special( Char enemy ) {
		if (reach(enemy.pos) > CHARGE || rooted) return false;
		for (int[] d : DIRS){
			ArrayList<Integer> p = path(d[0], d[1]);
			if (p.size() < 2) continue;
			LinkedHashSet<Integer> lane = lane(p);
			if (!lane.contains(enemy.pos)) continue;
			//the lane is three wide: two steps to get out of it
			warn(d[0] + 1 + (d[1] + 1) * 3, lane, enemy.pos, 2f);
			return true;
		}
		return false;
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		int dx = kind % 3 - 1, dy = kind / 3 - 1;
		int start = pos;
		ArrayList<Integer> p = path(dx, dy);
		charging = true;
		for (int c : p){
			if (!isAlive() || !fitsAt(c)) break;
			int from = pos;
			walkTo(c);
			SlagTrail.pour(from, SlagTrail.HEAT);
		}
		charging = false;
		if (!isAlive()) return;
		SlagTrail.pour(pos, SlagTrail.HEAT);
		Senses.noise(pos, Senses.CRASH);
		if (sprite != null && pos != start){
			sprite.move(start, pos);
			strikeAnim(0);
		}
		//stopped short: it rammed into something and stands there melting
		if (p.size() < CHARGE){
			dazed = 2;
			if (sprite instanceof BeetleSprite) ((BeetleSprite) sprite).melt(true);
			if (seen()){
				Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 0.8f);
				for (int c : SKCombat.area(pos, 1)) CellEmitter.get(c).burst(Speck.factory(Speck.ROCK), 2);
			}
		}
	}

	//rammed: hurts far more than being stepped on
	@Override
	protected void onTrample( Char ch ) {
		if (!charging){
			super.onTrample(ch);
			return;
		}
		if (!SKCombat.foes(this, ch)) return;
		blast(ch, 60, 80);
		if (ch.isAlive()){
			Buff.prolong(ch, Cripple.class, 2f);
			if (!Char.hasProp(ch, Property.FIERY)) Buff.affect(ch, Burning.class).reignite(ch, 3f);
		}
	}

	@Override
	protected boolean act() {
		if (dazed > 0 && paralysed <= 0 && pendingKind == -1){
			if (--dazed == 0 && sprite instanceof BeetleSprite) ((BeetleSprite) sprite).melt(false);
			spend(TICK);
			return true;
		}
		return super.act();
	}

	//dazed, the shell's open: it takes more
	@Override
	public int drRoll() {
		return dazed > 0 ? super.drRoll() / 2 : super.drRoll();
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("dazed", dazed);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		dazed = bundle.getInt("dazed");
	}

	//12-13 the charge, 14 melting
	public static class BeetleSprite extends AshColossusSprite {

		private Animation whole, melting;

		public BeetleSprite(){
			super();
			setup(FaunaAssets.BEETLE, 3, 64, 64, 8, 16);
			whole = idle;
			melting = loop(2, 14, 14, 0);
		}

		@Override
		public void strike( int kind ) {
			once(clip(12, 12, 13, 12, 13, 7, 8));
		}

		public void melt( boolean on ){
			look(on ? melting : whole);
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (ch instanceof SmelterBeetle && ((SmelterBeetle) ch).dazed > 0) melt(true);
		}
	}
}
