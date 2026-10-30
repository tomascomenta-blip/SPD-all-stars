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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.automaton;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAlly;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

//drone. tiny flying gun, follows the owner and shoots stuff a few cells away, temp
public class Drone extends SKAlly {

	{
		spriteClass = Sprite.class;
		HP = HT = 8;
		defenseSkill = 8;
		flying = true;
		properties.add(Property.INORGANIC);
	}

	public static final int RANGE = 4;

	public float dmgMulti = 1f;

	public static Drone make( Hero owner, int cell ){
		Drone d = new Drone();
		d.setOwner(owner);
		float hpMulti = 1f;
		if (owner.subClass == HeroSubClass.SWARM_ENGINEER){
			hpMulti += 0.2f*owner.pointsInTalent(Talent.SE_ARMORED_DRONES);
			d.dmgMulti += 0.15f*owner.pointsInTalent(Talent.SE_FOCUS_FIRE);
		}
		d.HP = d.HT = Math.round((6 + 2*owner.lvl) * hpMulti);
		d.defenseSkill = 6 + owner.lvl;
		d.timeLeft = duration(owner);
		d.pos = cell;
		return d;
	}

	public static int duration( Hero owner ){
		int t = 25;
		if (owner.subClass == HeroSubClass.SWARM_ENGINEER) t += 5*owner.pointsInTalent(Talent.SE_HIVE);
		return t;
	}

	public static int minDmg( int power ){
		return 1 + power/3;
	}

	public static int maxDmg( int power ){
		return 3 + power/2;
	}

	@Override
	public int attackSkill( Char target ) {
		return 12 + power;
	}

	@Override
	public int damageRoll() {
		return Math.round(Random.NormalIntRange(minDmg(power), maxDmg(power)) * dmgMulti);
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 1);
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		if (super.canAttack(enemy)) return true;
		return Dungeon.level.distance(pos, enemy.pos) <= RANGE
				&& new Ballistica(pos, enemy.pos, Ballistica.PROJECTILE).collisionPos == enemy.pos;
	}

	@Override
	public void die( Object cause ) {
		//salvage talent: dead drone -> parts for the satellite cannon
		Hero o = owner();
		if (o != null && o.isAlive() && o.hasTalent(Talent.AUT_SALVAGE)){
			SatelliteCannon cannon = o.belongings.getItem(SatelliteCannon.class);
			if (cannon != null){
				cannon.gainCharge(0.5f*o.pointsInTalent(Talent.AUT_SALVAGE), true);
				Item.updateQuickslot();
			}
		}
		super.die(cause);
	}

	@Override
	public String description() {
		String d = Messages.get(this, "desc", RANGE, Math.round(minDmg(power)*dmgMulti), Math.round(maxDmg(power)*dmgMulti));
		if (timeLeft >= 0) d += "\n\n" + Messages.get(this, "desc_time", Math.round(timeLeft));
		return d;
	}

	private static final String DMG = "dmg";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(DMG, dmgMulti);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		dmgMulti = bundle.getFloat(DMG);
		defenseSkill = 6 + power;
	}

	public static class Sprite extends SKMobSprite {
		public Sprite(){
			super();
			shot = ItemSpriteSheet.SHOT_BULLET;
			shotSound = Assets.Sounds.HIT_ARROW;
			setup(SKAssets.DRONE, 14, 12);
		}

		@Override
		public int blood() {
			return 0xFFBBBBBB;
		}
	}
}
