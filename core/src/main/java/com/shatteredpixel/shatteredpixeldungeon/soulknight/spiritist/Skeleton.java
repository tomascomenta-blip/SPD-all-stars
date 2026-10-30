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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.spiritist;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAlly;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

//skelly. from danse macabre or plague herald's plague, follows the owner, temp
public class Skeleton extends SKAlly {

	{
		spriteClass = Sprite.class;
		HP = HT = 10;
		defenseSkill = 6;
		properties.add(Property.UNDEAD);
		properties.add(Property.INORGANIC);
	}

	public float multi = 1f;

	public static Skeleton make( Hero owner, int cell, int turns, float multi ){
		Skeleton s = new Skeleton();
		s.setOwner(owner);
		s.multi = multi;
		s.HP = s.HT = Math.round((8 + 3*owner.lvl) * multi);
		s.defenseSkill = 6 + owner.lvl;
		s.timeLeft = turns;
		s.pos = cell;
		return s;
	}

	public static Skeleton spawn( Hero owner, int cell, int turns, float multi ){
		Skeleton s = make(owner, cell, turns, multi);
		GameScene.add(s);
		Dungeon.level.occupyCell(s);
		if (Dungeon.level.heroFOV[cell]) CellEmitter.get(cell).burst(ShadowParticle.UP, 8);
		return s;
	}

	public static int minDmg( int power ){
		return 2 + power/3;
	}

	public static int maxDmg( int power ){
		return 5 + power/2;
	}

	@Override
	public int attackSkill( Char target ) {
		return 12 + power;
	}

	@Override
	public int damageRoll() {
		return Math.round(Random.NormalIntRange(minDmg(power), maxDmg(power)) * multi);
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 2 + power/5);
	}

	@Override
	public void die( Object cause ) {
		if (timeLeft != 0) SpiritistHooks.onSummonDeath(owner());
		super.die(cause);
	}

	@Override
	public String description() {
		String d = Messages.get(this, "desc", Math.round(minDmg(power)*multi), Math.round(maxDmg(power)*multi));
		if (timeLeft >= 0) d += "\n\n" + Messages.get(this, "desc_time", Math.round(timeLeft));
		return d;
	}

	private static final String MULTI = "multi";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(MULTI, multi);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		multi = bundle.getFloat(MULTI);
		defenseSkill = 6 + power;
	}

	public static class Sprite extends SKMobSprite {
		public Sprite(){
			super();
			setup(SKAssets.SKELETON, 16, 16);
		}

		@Override
		public int blood() {
			return 0xFFCCCCCC;
		}
	}
}
