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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.watabou.noosa.audio.Sample;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

//goblin shaman. throws a star that bursts where it lands, a bit of dmg around the target
public class GoblinShaman extends GoblinGunner {

	{
		spriteClass = ForestSprites.ShamanSprite.class;

		HP = HT = 14;
		defenseSkill = 4;

		EXP = 4;
		maxLvl = 9;

		lootChance = 0.15f;
	}

	//every few turns in a fight it shields the goblins around
	private int shieldTimer = 2;

	@Override
	protected boolean act() {
		if (state == HUNTING && --shieldTimer <= 0){
			shieldTimer = 5;
			int shield = 3 + Dungeon.scalingDepth()/2;
			boolean any = false;
			for (Mob m : Dungeon.level.mobs){
				if (m == this || !ForestFeatures.isGoblin(m) || !m.isAlive() || Dungeon.level.distance(m.pos, pos) > 3) continue;
				Barrier b = m.buff(Barrier.class);
				if (b != null && b.shielding() >= shield) continue;
				Buff.affect(m, Barrier.class).setShield(shield);
				any = true;
			}
			if (any){
				if (sprite instanceof SKMobSprite) ((SKMobSprite) sprite).cast();
				if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.CHARMS, 0.6f, 1.3f);
				spend(TICK);
				return true;
			}
		}
		return super.act();
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("shield_timer", shieldTimer);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		shieldTimer = bundle.getInt("shield_timer");
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 2, 5 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 11;
	}

	private boolean bursting = false;

	@Override
	public boolean attack( Char enemy, float dmgMulti, float dmgBonus, float accMulti ) {
		boolean result = super.attack(enemy, dmgMulti, dmgBonus, accMulti);
		if (!bursting && enemy != null && !Dungeon.level.adjacent(pos, enemy.pos)){
			bursting = true;
			int center = enemy.pos;
			if (Dungeon.level.heroFOV[center]) CellEmitter.center(center).burst(Speck.factory(Speck.STAR), 6);
			for (Char ch : SKCombat.foesIn(this, SKCombat.openArea(center, 1))){
				if (ch != enemy){
					ch.damage(SKCombat.specialDamage(this, ch, 1, 3), this);
				}
			}
			bursting = false;
		}
		return result;
	}

	@Override
	public float lootChance() {
		//no drop limit, unlike the guns
		return lootChance;
	}

	@Override
	public Item createLoot() {
		return Generator.randomUsingDefaults(Generator.Category.POTION);
	}
}
