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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.AshColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.AshFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.LinkedHashSet;

//the last king, guardian of the devoured city. he never got up from his throne and the throne and
//the city grew into him: he drags it all along on four legs of flesh. he brings his sceptre down
//(a ring of falling stones round the blow, all marked), and when he calls his city every window
//lights up, the stones of the houses come down on whoever is near and the city's things come
public class LastKing extends CityColossus {

	{
		size = 15;
		spriteClass = KingSprite.class;

		HP = HT = 8000;
		defenseSkill = 0;
		EXP = 340;
		baseSpeed = 0.4f;

		minDmg = 60; maxDmg = 80; acc = 66;
		armorMin = 26; armorMax = 46;

		properties.add(Property.MINIBOSS);
	}

	private static final int SCEPTRE = 0, CALL = 1, STAMP = 2;
	private static final int SCEPTRE_REACH = 7, CALL_RANGE = 16;

	//every few blows he calls his city instead
	private int calls = 0;

	//the gem of the chest under the cloak (from his back), and both hands on the throne arms
	@Override
	public int[][] weakSpots() {
		return new int[][]{{7, 2}, {0, 6}, {14, 6}};
	}

	//the houses on the back of the throne and the rows of roofs at its foot
	@Override
	public int[][] climbable() {
		return new int[][]{{0, 4, 10}, {1, 3, 11}, {2, 3, 11}, {3, 2, 12}, {4, 2, 12}, {5, 3, 11}, {9, 2, 5}, {9, 9, 12}};
	}

	@Override
	protected int eyes() {
		return 4;
	}

	@Override
	protected int ears() {
		return 5;
	}

	@Override
	protected int roam() {
		return 12;
	}

	@Override
	protected int specialCooldown() {
		return 3;
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = reach(enemy.pos);
		int touching = SKCombat.foesIn(this, ring(1)).size();
		if (touching >= 2 || touching == 1 && d <= 1 && Random.Int(2) == 0){
			warn(STAMP, ring(2), enemy.pos, 2f);
			return true;
		}
		calls++;
		if (calls >= 3 || d > SCEPTRE_REACH && calls >= 2){
			LinkedHashSet<Integer> cells = new LinkedHashSet<>();
			for (Hero h : Party.living()){
				if (reach(h.pos) <= CALL_RANGE) cells.addAll(square(h.pos, 1));
			}
			if (!cells.isEmpty()){
				calls = 0;
				warn(CALL, cells, enemy.pos, 3f);
				if (seen()) GLog.w(Messages.get(this, "calls"));
				return true;
			}
		}
		if (d > SCEPTRE_REACH) return false;
		warn(SCEPTRE, sceptre(enemy.pos), enemy.pos, 3f);
		return true;
	}

	//the blow itself (3x3) and the stones that fly off it, 3 cells round
	private LinkedHashSet<Integer> sceptre( int aim ){
		LinkedHashSet<Integer> out = new LinkedHashSet<>(square(aim, 1));
		int w = Dungeon.level.width();
		for (int c : square(aim, 3)){
			int dx = Math.abs(c % w - aim % w), dy = Math.abs(c / w - aim / w);
			if (Math.max(dx, dy) == 3) out.add(c);
		}
		return out;
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		strikeAnim(kind);
		Senses.noise(aim, kind == CALL ? Senses.STAMPEDE : Senses.CRASH);
		if (seen()){
			Sample.INSTANCE.play(kind == CALL ? Assets.Sounds.ROCKS : Assets.Sounds.BLAST, 1f, 0.5f);
			for (int c : cells){
				if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.ROCK), 3);
			}
		}
		int w = Dungeon.level.width();
		for (Char ch : SKCombat.foesIn(this, cells)){
			if (kind == SCEPTRE){
				int dx = Math.abs(ch.pos % w - aim % w), dy = Math.abs(ch.pos / w - aim / w);
				if (Math.max(dx, dy) <= 1){
					blast(ch, 170, 230);
				} else {
					blast(ch, 60, 90);
					if (ch.isAlive()) Buff.prolong(ch, Cripple.class, 3f);
				}
			} else if (kind == CALL){
				blast(ch, 80, 110);
			} else {
				blast(ch, 90, 130);
				knock(ch, 3);
			}
		}
		if (kind == CALL) callCity(aim);
	}

	//the things of his city come to the call: whatever is near hears it, and a couple crawl out
	private void callCity( int aim ){
		for (Mob m : AshFauna.sorted(Dungeon.level.mobs)){
			if (m == this || m.alignment != alignment || !(m instanceof AshColossus || m instanceof CityCritter)) continue;
			if (Dungeon.level.distance(m.pos, pos) <= 30 && m.state != m.HUNTING) m.beckon(aim);
		}
		int out = 0;
		for (Mob m : Dungeon.level.mobs) if (m.EXP == 0 && m instanceof CityCritter) out++;
		if (out >= 6) return;
		letOut(Random.Int(2) == 0 ? new Lamplighter() : new FleshGargoyle(), besideToward(aim));
		letOut(new FleshGargoyle(), besideToward(aim));
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("calls", calls);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		calls = bundle.getInt("calls");
	}

	//6 sceptre up, 7 down with the stones, 8 every window lit, 9 slumped, 10 dead
	public static class KingSprite extends Sheet {

		public KingSprite(){
			super();
			layout(CityAssets.KING, 15, 304, 372, 32, 116, 9, 10);
			run = loop(4, 2, 3, 4, 5);
		}

		@Override
		protected PointF weakAnchor() {
			return new PointF(154, 150);
		}

		@Override
		public void windup( int kind ) {
			play(kind == CALL ? loop(3, 8, 8, 1) : loop(1, 6));
		}

		@Override
		public void strike( int kind ) {
			once(kind == CALL ? clip(4, 8, 8, 8, 0) : clip(8, 7, 7, 7, 0));
		}
	}
}
