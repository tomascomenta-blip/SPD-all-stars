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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.beastmaster;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

//W: poison vines around a spot. whip adjacent enemies + block enemy bullets (they're in the way, so)
public class VenomVines extends ChampionAbility {

	public static final VenomVines INSTANCE = new VenomVines();

	@Override public String slot() { return "W"; }
	@Override public int unlockLevel() { return 2; }
	@Override public int icon() { return HeroIcon.BM_VINES; }
	@Override public int color() { return 0x66CC33; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }
	@Override public boolean targeted() { return true; }

	public static final int RANGE = 4;
	//warden: more + longer
	public static int count( Hero hero ){
		return hero.subClass == HeroSubClass.FOREST_WARDEN ? 4 : 3;
	}

	public static int duration( Hero hero ){
		return hero.subClass == HeroSubClass.FOREST_WARDEN ? 25 : 15;
	}

	@Override
	public float cooldown( Hero hero ) {
		return 35;
	}

	public static int hp( Hero hero ){
		float m = 1f;
		if (hero.subClass == HeroSubClass.FOREST_WARDEN) m += 0.25f*hero.pointsInTalent(Talent.FW_IRONBARK);
		return Math.round((8 + 2*hero.lvl) * m);
	}

	public static int minDmg( Hero hero ){
		return 1 + hero.pointsInTalent(Talent.BM_THORNS);
	}

	public static int maxDmg( Hero hero ){
		return 3 + hero.lvl/3 + hero.pointsInTalent(Talent.BM_THORNS);
	}

	public static int poison( Hero hero ){
		return 2 + hero.lvl/4 + hero.pointsInTalent(Talent.BM_THORNS);
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		if (target == null) return;
		if (Dungeon.level.distance(hero.pos, target) > RANGE || !Dungeon.level.heroFOV[target]){
			GLog.w(Messages.get(ChampionAbility.class, "too_far"));
			return;
		}
		int made = 0;
		for (int n : PathFinder.NEIGHBOURS9){
			if (made >= count(hero)) break;
			int c = target + n;
			if (c < 0 || c >= Dungeon.level.length()) continue;
			if (Actor.findChar(c) != null || Dungeon.level.solid[c] || !Dungeon.level.passable[c] || Dungeon.level.pit[c]) continue;
			Vine v = new Vine();
			v.pos = c;
			v.HP = v.HT = hp(hero);
			v.minDmg = minDmg(hero);
			v.maxDmg = maxDmg(hero);
			v.poison = poison(hero);
			v.roots = hero.subClass == HeroSubClass.FOREST_WARDEN ? hero.pointsInTalent(Talent.FW_ROOTS) : 0;
			v.attack = 10 + hero.lvl;
			v.timeLeft = duration(hero);
			GameScene.add(v);
			Dungeon.level.occupyCell(v);
			if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(LeafParticle.GENERAL, 6);
			made++;
		}
		if (made == 0){
			GLog.w(Messages.get(this, "no_space"));
			return;
		}
		Sample.INSTANCE.play(Assets.Sounds.PLANT, 1f, 0.8f);
		hero.sprite.zap(target);
		Invisibility.dispel(hero);
		startCooldown(hero);
		hero.spendAndNext(1f);
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", RANGE, count(hero), duration(hero), hp(hero), minDmg(hero), maxDmg(hero), poison(hero));
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}

	//can't move, stays on its floor
	public static class Vine extends Mob {

		{
			spriteClass = Sprite.class;
			alignment = Alignment.ALLY;
			state = WANDERING;
			HP = HT = 10;
			defenseSkill = 0;
			properties.add(Property.IMMOVABLE);
		}

		public float timeLeft = 15;
		public int minDmg = 1, maxDmg = 3, poison = 2, roots = 0, attack = 10;

		@Override
		protected boolean act() {
			if (timeLeft <= 0.01f){
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
		public int attackProc( Char enemy, int damage ) {
			damage = super.attackProc(enemy, damage);
			if (enemy.isAlive()){
				Buff.affect(enemy, Poison.class).extend(poison);
				if (roots > 0 && Random.Int(3) < roots) Buff.prolong(enemy, Roots.class, 1f);
			}
			return damage;
		}

		@Override
		public String description() {
			return Messages.get(this, "desc", minDmg, maxDmg, poison, Math.round(timeLeft));
		}

		private static final String TIME = "time";
		private static final String DATA = "data";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(TIME, timeLeft);
			bundle.put(DATA, new int[]{minDmg, maxDmg, poison, roots, attack});
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			timeLeft = bundle.getFloat(TIME);
			int[] d = bundle.getIntArray(DATA);
			if (d != null && d.length >= 5){
				minDmg = d[0]; maxDmg = d[1]; poison = d[2]; roots = d[3]; attack = d[4];
			}
		}

		public static class Sprite extends SKMobSprite {
			public Sprite(){
				super();
				setup(SKAssets.VINE, 16, 16);
			}

			@Override
			public int blood() {
				return 0xFF66AA33;
			}
		}
	}
}
