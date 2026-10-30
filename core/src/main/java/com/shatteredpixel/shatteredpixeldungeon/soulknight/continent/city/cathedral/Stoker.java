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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.cathedral;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.fauna.CityCritter;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

import java.util.ArrayList;
import java.util.Collections;

//stoker. a soot-black little thing with a shovel of live coals, crawled out of the grates of the
//furnace. it throws its coals at whoever is near (the cell is marked) but what it wants is the
//heart: once the heart is awake it runs there, and throws itself in to feed it
public class Stoker extends CityCritter {

	{
		spriteClass = StokerSprite.class;

		HP = HT = 55;
		defenseSkill = 34;
		EXP = 6;
		baseSpeed = 1.5f;

		minDmg = 12; maxDmg = 18; acc = 56;
		armorMin = 0; armorMax = 4;

		properties.add(Property.FIERY);
	}

	private static final int COALS = 0;
	private static final int THROW = 4;

	@Override
	protected int specialCooldown() {
		return 4;
	}

	private FurnaceHeart heart(){
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof FurnaceHeart && m.isAlive()) return (FurnaceHeart) m;
		}
		return null;
	}

	@Override
	protected boolean act() {
		FurnaceHeart h = heart();
		if (h != null && h.awake() && paralysed <= 0){
			//right by it: into the fire
			if (h.reach(pos) <= 1){
				h.feed(this);
				if (sprite instanceof StokerSprite) ((StokerSprite) sprite).special();
				//it went into the fire on its own: nobody killed it
				EXP = 0;
				die(h);
				spend(TICK);
				return true;
			}
			//nobody right on top of it: off to the heart
			if (enemy == null || !enemy.isAlive() || Dungeon.level.distance(pos, enemy.pos) > 2){
				state = HUNTING;
				target = h.approachCell(pos);
			}
		}
		return super.act();
	}

	@Override
	protected boolean getCloser( int target ) {
		FurnaceHeart h = heart();
		if (h != null && h.awake() && (enemy == null || Dungeon.level.distance(pos, enemy.pos) > 2)){
			target = h.approachCell(pos);
		}
		return super.getCloser(target);
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = Dungeon.level.distance(pos, enemy.pos);
		if (d < 2 || d > THROW || !fieldOfView[enemy.pos]) return false;
		ArrayList<Integer> cell = new ArrayList<>(Collections.singletonList(enemy.pos));
		announce(COALS, cell, enemy.pos);
		return true;
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		if (sprite instanceof StokerSprite) ((StokerSprite) sprite).throwCoals();
		for (int c : cells){
			if (Dungeon.level.heroFOV[c]){
				CellEmitter.get(c).burst(FlameParticle.FACTORY, 5);
				Sample.INSTANCE.play(Assets.Sounds.BURNING, 0.6f, 1.3f);
			}
		}
		for (Char ch : SKCombat.foesIn(this, cells)){
			blast(ch, Cathedral.fire(18), Cathedral.fire(28));
			if (ch.isAlive() && !Char.hasProp(ch, Property.FIERY)) Buff.affect(ch, Burning.class).reignite(ch, 3f);
		}
	}

	//16x16: 8 is feeding the fire
	public static class StokerSprite extends Sheet {

		public StokerSprite(){
			super();
			layout(CathedralAssets.STOKER);
		}

		//with a callback: the plain attack anim would make it hit AGAIN when it ends
		void throwCoals(){
			if (ch == null) return;
			attack(ch.pos, new Callback() {
				@Override
				public void call() {
					idle();
				}
			});
		}
	}
}
