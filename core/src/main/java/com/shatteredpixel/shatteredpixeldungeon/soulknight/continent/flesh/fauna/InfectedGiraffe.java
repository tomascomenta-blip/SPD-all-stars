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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.flesh.fauna;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PointF;

import java.util.ArrayList;

//infected giraffe. still a giraffe from afar: long legs, a sloping back, the spots in a grid. but the
//spots are open sores and halfway up the neck it opens into a flower of flesh with teeth on the
//petals. it keeps a patch of the forest and hunts down whoever walks in, wherever they hide, until
//they're out of it. the neck comes down like a whip over a long marked strip; now and then it
//blooms and the spores fall all around it
public class InfectedGiraffe extends FleshColossus {

	{
		size = 15;
		spriteClass = GiraffeSprite.class;

		HP = HT = 6500;
		defenseSkill = 4;
		EXP = 300;
		baseSpeed = 0.75f;

		minDmg = 55; maxDmg = 75; acc = 66;
		armorMin = 18; armorMax = 36;

		territory = 22;
	}

	private static final int WHIP = 0, BLOOM = 1;
	private static final int NECK = 9;

	//the flower that grew on its back
	@Override
	public int[][] weakSpots() {
		return new int[][]{{8, 0}, {7, 0}, {8, 2}};
	}

	//the back, from the withers down to the rump
	@Override
	public int[][] climbable() {
		return new int[][]{{1, 3, 10}, {2, 2, 11}, {3, 2, 11}, {4, 3, 10}, {5, 4, 9}};
	}

	//up there it sees over everything
	@Override
	protected int eyes() {
		return 6;
	}

	@Override
	protected int ears() {
		return 3;
	}

	@Override
	protected int specialCooldown() {
		return 3;
	}

	private boolean inZone( int cell ){
		return home != -1 && Dungeon.level.distance(home, cell) <= territory;
	}

	@Override
	protected boolean act() {
		if (!tamed && paralysed <= 0 && down == 0){
			if (state == HUNTING && enemy != null && enemy.isAlive() && inZone(enemy.pos)){
				//in its patch it always knows where you are
				target = enemy.pos;
			} else if (state != HUNTING){
				for (Hero h : Party.living()){
					if (inZone(h.pos) && reach(h.pos) <= 16 && h.invisible <= 0){
						aggro(h);
						target = h.pos;
						if (seen()) GLog.w(Messages.get(this, "zone"));
						break;
					}
				}
			}
		}
		return super.act();
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = reach(enemy.pos);
		if (d <= 3 && SKCombat.foesIn(this, ring(3)).size() >= 2){
			warn(BLOOM, ring(3), enemy.pos, 2f);
			return true;
		}
		if (d > NECK) return false;
		ArrayList<Integer> lash = cone(edgeToward(enemy.pos), enemy.pos, NECK, 0.82);
		if (!lash.contains(enemy.pos)) return false;
		//the neck goes back like a bow first
		warn(WHIP, lash, enemy.pos, 2f);
		return true;
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		strikeAnim(kind);
		if (kind == WHIP){
			Senses.noise(aim, Senses.CRASH);
			if (seen()) Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG, 1f, 0.6f);
			for (int c : cells) if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.DUST), 1);
			for (Char ch : SKCombat.foesIn(this, cells)){
				blast(ch, 150, 200);
				knock(ch, 3);
			}
		} else {
			Senses.noise(pos, Senses.FIGHT);
			if (seen()) Sample.INSTANCE.play(Assets.Sounds.GAS, 1f, 0.8f);
			for (int c : cells) if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.STENCH), 1);
			for (Char ch : SKCombat.foesIn(this, cells)){
				blast(ch, 40, 60);
				if (ch.isAlive()){
					Buff.affect(ch, Poison.class).set(6);
					Buff.prolong(ch, Vertigo.class, 3f);
				}
			}
		}
	}

	//6 the neck drawn back, 7 the lash, 8 in full bloom
	public static class GiraffeSprite extends Sheet {

		public GiraffeSprite(){
			super();
			layout(FleshAssets.GIRAFFE, 15, 344, 448, 40, 176, 9, 10);
			run = loop(5, 2, 3, 4, 5);
		}

		@Override
		protected PointF standingSpot() {
			return new PointF(170, 214);
		}

		@Override
		protected PointF lyingSpot() {
			return new PointF(170, 334);
		}

		@Override
		public void windup( int kind ) {
			play(loop(1, kind == BLOOM ? 8 : 6));
		}

		@Override
		public void strike( int kind ) {
			once(kind == BLOOM ? clip(4, 8, 8, 0) : clip(8, 7, 7, 7, 0));
		}
	}
}
