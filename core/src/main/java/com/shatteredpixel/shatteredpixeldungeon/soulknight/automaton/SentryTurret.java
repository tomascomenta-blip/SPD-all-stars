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
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

//armor ability: build a turret, it shoots stuff in sight for a while. stays on that floor
public class SentryTurret extends ArmorAbility {

	{
		baseChargeUse = 35f;
	}

	public static final int RANGE = 3;

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	public int targetedPos( Char user, int dst ) {
		return new Ballistica(user.pos, dst, Ballistica.STOP_SOLID | Ballistica.STOP_TARGET | Ballistica.STOP_CHARS).collisionPos;
	}

	public static int duration( Hero hero ){
		return 20 + 5*hero.pointsInTalent(Talent.ST_LONG_SERVICE);
	}

	public static int minDmg( Hero hero ){
		return Math.round((2 + hero.lvl/3) * (1f + 0.2f*hero.pointsInTalent(Talent.ST_HEAVY_CALIBER)));
	}

	public static int maxDmg( Hero hero ){
		return Math.round((6 + hero.lvl/2) * (1f + 0.2f*hero.pointsInTalent(Talent.ST_HEAVY_CALIBER)));
	}

	@Override
	protected void activate( ClassArmor armor, Hero hero, Integer target ) {
		if (target == null) return;
		if (Dungeon.level.distance(hero.pos, target) > RANGE){
			GLog.w(Messages.get(ChampionAbility.class, "too_far"));
			return;
		}
		if (Actor.findChar(target) != null || Dungeon.level.solid[target] || !Dungeon.level.passable[target]){
			GLog.w(Messages.get(this, "no_space"));
			return;
		}
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		Gun gun = new Gun();
		gun.pos = target;
		gun.timeLeft = duration(hero);
		gun.minDmg = minDmg(hero);
		gun.maxDmg = maxDmg(hero);
		gun.twin = hero.pointsInTalent(Talent.ST_TWIN_BARRELS);
		gun.HP = gun.HT = 10 + 3*hero.lvl;
		gun.defenseSkill = 4 + hero.lvl;
		gun.attack = 12 + hero.lvl;
		GameScene.add(gun);
		Dungeon.level.occupyCell(gun);
		if (gun.sprite != null) gun.sprite.emitter().burst(Speck.factory(Speck.ROCK), 4);
		Sample.INSTANCE.play(Assets.Sounds.TRAMPLE, 1f, 0.8f);

		ChampionHooks.operateVisual(hero, target);
		Invisibility.dispel(hero);
		hero.spendAndNext(1f);
	}

	@Override
	public String desc() {
		String d = Messages.get(this, "desc", RANGE);
		if (Dungeon.hero != null && Dungeon.hero.heroClass == HeroClass.AUTOMATON){
			Hero h = Dungeon.hero;
			d += " " + Messages.get(this, "desc_numbers", duration(h), minDmg(h), maxDmg(h));
		}
		return d + "\n\n" + Messages.get(this, "cost", (int) baseChargeUse);
	}

	@Override
	public int icon() {
		return HeroIcon.AUT_TURRET;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.ST_LONG_SERVICE, Talent.ST_HEAVY_CALIBER, Talent.ST_TWIN_BARRELS, Talent.HEROIC_ENERGY};
	}

	//the actual turret mob
	public static class Gun extends Mob {

		{
			spriteClass = Sprite.class;
			alignment = Alignment.ALLY;
			state = WANDERING;
			HP = HT = 15;
			defenseSkill = 5;
			properties.add(Property.IMMOVABLE);
			properties.add(Property.INORGANIC);
		}

		public static final int SHOT_RANGE = 5;

		public float timeLeft = 20;
		public int minDmg = 2, maxDmg = 6;
		public int twin = 0;  //twin barrels, x/4 for a 2nd shot
		public int attack = 12;

		@Override
		protected boolean act() {
			if (timeLeft <= 0.01f){
				if (Dungeon.level.heroFOV[pos]) GLog.i(Messages.get(this, "fades"));
				die(null);
				return true;
			}
			return super.act();
		}

		@Override
		protected void spend( float time ) {
			super.spend(time);
			timeLeft = Math.max(0, timeLeft - time);
		}

		@Override
		protected boolean getCloser( int target ) {
			return false;
		}

		@Override
		protected boolean getFurther( int target ) {
			return false;
		}

		@Override
		public int attackSkill( Char target ) {
			return attack;
		}

		@Override
		public int damageRoll() {
			return Random.NormalIntRange(minDmg, maxDmg);
		}

		@Override
		protected boolean canAttack( Char enemy ) {
			return Dungeon.level.distance(pos, enemy.pos) <= SHOT_RANGE
					&& new Ballistica(pos, enemy.pos, Ballistica.PROJECTILE).collisionPos == enemy.pos;
		}

		private boolean second = false;

		@Override
		public boolean attack( Char enemy, float dmgMulti, float dmgBonus, float accMulti ) {
			boolean result = super.attack(enemy, dmgMulti, dmgBonus, accMulti);
			if (!second && twin > 0 && enemy.isAlive() && Random.Int(4) < twin){
				second = true;
				super.attack(enemy, dmgMulti, dmgBonus, accMulti);
				second = false;
			}
			return result;
		}

		@Override
		public String description() {
			return Messages.get(this, "desc", SHOT_RANGE, minDmg, maxDmg, Math.round(timeLeft));
		}

		private static final String TIME = "time";
		private static final String MIN = "min";
		private static final String MAX = "max";
		private static final String TWIN = "twin";
		private static final String ATTACK = "attack";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(TIME, timeLeft);
			bundle.put(MIN, minDmg);
			bundle.put(MAX, maxDmg);
			bundle.put(TWIN, twin);
			bundle.put(ATTACK, attack);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			timeLeft = bundle.getFloat(TIME);
			minDmg = bundle.getInt(MIN);
			maxDmg = bundle.getInt(MAX);
			twin = bundle.getInt(TWIN);
			attack = bundle.getInt(ATTACK);
		}

		public static class Sprite extends SKMobSprite {
			public Sprite(){
				super();
				shot = ItemSpriteSheet.SHOT_BULLET;
				shotSound = Assets.Sounds.HIT_ARROW;
				setup(SKAssets.TURRET, 16, 16);
			}

			@Override
			public int blood() {
				return 0xFFBBBBBB;
			}
		}
	}
}
