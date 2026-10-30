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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.bones.fauna;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.AshCritter;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.CritterSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

//charred phoenix: a bird of coal with cracks of ember for veins. the first time it dies it falls to
//ash and gets up out of it at once, whole again; the second time it stays down
public class CharredPhoenix extends AshCritter {

	{
		spriteClass = PhoenixSprite.class;

		HP = HT = 70;
		defenseSkill = 36;
		EXP = 9;
		baseSpeed = 1.5f;
		flying = true;

		minDmg = 16; maxDmg = 24; acc = 60;
		armorMin = 0; armorMax = 4;

		properties.add(Property.FIERY);
	}

	private boolean reborn = false;

	@Override
	protected int eyes() {
		return 3;
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc(enemy, damage);
		if (enemy.isAlive() && !Char.hasProp(enemy, Property.FIERY) && Random.Int(3) == 0){
			Buff.affect(enemy, Burning.class).reignite(enemy, 3f);
		}
		return damage;
	}

	@Override
	public void die( Object cause ) {
		if (reborn){
			super.die(cause);
			return;
		}
		reborn = true;
		HP = HT;
		Senses.noise(pos, Senses.FIGHT);
		if (sprite instanceof PhoenixSprite) ((PhoenixSprite) sprite).pose(4, 8, 8, 9, 9, 0);
		if (Dungeon.level.heroFOV[pos]){
			CellEmitter.get(pos).burst(FlameParticle.FACTORY, 8);
			Sample.INSTANCE.play(Assets.Sounds.BURNING);
			GLog.w(Messages.get(this, "reborn"));
		}
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("reborn", reborn);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		reborn = bundle.getBoolean("reborn");
	}

	//0-1 idle, 2-5 wings, 6-7 the peck, 8-9 rising out of its ash
	public static class PhoenixSprite extends CritterSprite {

		public PhoenixSprite(){
			super();
			critter(BonesAssets.PHOENIX);
		}
	}
}
