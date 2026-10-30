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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.fauna;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PointF;

import java.util.ArrayList;
import java.util.LinkedHashSet;

//the chained one. a huge wasted prisoner, mouth sewn shut, bound by chains of living yog flesh to
//three stakes behind him: he can't go far from them. he whips the chains in an arc in front of him,
//or throws one at whoever keeps away and drags them in (both marked). hurt enough, he snaps a chain
//and his reach grows
public class ChainedOne extends CityColossus {

	{
		size = 8;
		spriteClass = ChainedSprite.class;

		HP = HT = 3000;
		defenseSkill = 6;
		EXP = 125;
		baseSpeed = 0.6f;

		minDmg = 55; maxDmg = 72; acc = 64;
		armorMin = 16; armorMax = 30;

		territory = 5;
	}

	private static final int LASH = 0, DRAG = 1;
	private static final int DRAG_RANGE = 9;

	//chains still holding him
	private int chains = 3;

	//the chest, and the shackles on both wrists
	@Override
	public int[][] weakSpots() {
		return new int[][]{{4, 1}, {0, 4}, {7, 5}};
	}

	//bent shoulders and back
	@Override
	public int[][] climbable() {
		return new int[][]{{0, 2, 5}, {1, 2, 5}, {2, 2, 5}, {3, 3, 4}};
	}

	@Override
	protected int ears() {
		return 3;
	}

	@Override
	protected int roam() {
		return 4;
	}

	@Override
	protected int specialCooldown() {
		return 3;
	}

	//whoever is past the end of his chains he can't get at: he doesn't even try
	@Override
	protected Char chooseEnemy() {
		Char e = super.chooseEnemy();
		if (e != null && home != -1 && Dungeon.level.distance(home, e.pos) > territory + 5) return null;
		return e;
	}

	@Override
	public void damage( int dmg, Object src ) {
		super.damage(dmg, src);
		if (!isAlive() || chains <= 0) return;
		//a chain gives at 75%, 50% and 25%
		if (HP < HT * chains / 4){
			chains--;
			territory += 6;
			if (sprite instanceof ChainedSprite) ((ChainedSprite) sprite).snap();
			Senses.noise(pos, Senses.ROAR);
			if (seen()){
				Sample.INSTANCE.play(Assets.Sounds.CHAINS, 1f, 0.7f);
				GLog.w(Messages.get(this, "snaps"));
			}
		}
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = reach(enemy.pos);
		if (d <= 4){
			ArrayList<Integer> arc = front(enemy.pos, 4, 2);
			if (arc.contains(enemy.pos)){
				warn(LASH, arc, enemy.pos, 2f);
				return true;
			}
		}
		if (d >= 3 && d <= DRAG_RANGE + (3 - chains) * 2){
			LinkedHashSet<Integer> line = chain(enemy.pos);
			if (line.contains(enemy.pos)){
				warn(DRAG, line, enemy.pos, 2f);
				return true;
			}
		}
		return false;
	}

	//the chain thrown at a cell, from the edge of the body, a cell wide
	private LinkedHashSet<Integer> chain( int aim ){
		LinkedHashSet<Integer> out = new LinkedHashSet<>();
		int from = edgeToward(aim);
		Ballistica b = new Ballistica(from, aim, Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID);
		for (int c : b.path){
			if (Dungeon.level.distance(c, from) > DRAG_RANGE + 6) break;
			if (Dungeon.level.solid[c]) break;
			if (!covers(c)) out.add(c);
			if (c == b.collisionPos) break;
		}
		return out;
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		strikeAnim(kind);
		Senses.noise(aim, Senses.CRASH);
		if (seen()){
			Sample.INSTANCE.play(Assets.Sounds.CHAINS, 1f, 0.6f);
			for (int c : cells) if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.DUST), 2);
		}
		for (Char ch : SKCombat.foesIn(this, cells)){
			if (kind == LASH){
				blast(ch, 110, 150);
				knock(ch, 2);
			} else {
				blast(ch, 50, 70);
				if (ch.isAlive()){
					drag(ch);
					Buff.prolong(ch, Cripple.class, 3f);
				}
			}
		}
	}

	//pulled in to the free cell beside the body nearest where it was
	private void drag( Char ch ){
		if (ch.rooted || Char.hasProp(ch, Property.IMMOVABLE)) return;
		int to = besideToward(ch.pos);
		Level l = Dungeon.level;
		if (to == -1 || Actor.findChar(to) != null || !l.passable[to]) return;
		ch.pos = to;
		if (ch.sprite != null) ch.sprite.place(to);
		l.occupyCell(ch);
		if (ch instanceof Hero) Dungeon.observe();
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("chains", chains);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		chains = bundle.contains("chains") ? bundle.getInt("chains") : 3;
	}

	//6 the chains pulled taut, 7 the lash, 8 a chain snapping, 9 on his knees, 10 dead
	public static class ChainedSprite extends Sheet {

		public ChainedSprite(){
			super();
			layout(CityAssets.CHAINED, 8, 228, 262, 40, 88, 9, 10);
			run = loop(5, 2, 3, 4, 5);
		}

		@Override
		protected PointF weakAnchor() {
			return new PointF(112, 116);
		}

		//only from idle or walking: cutting a blow short would leave it waiting
		public void snap(){
			if (curAnim == idle || curAnim == run) once(clip(4, 8, 8, 8, 0));
		}

		@Override
		public void strike( int kind ) {
			once(kind == DRAG ? clip(6, 7, 6, 6, 0) : clip(10, 7, 7, 7, 0));
		}
	}
}
