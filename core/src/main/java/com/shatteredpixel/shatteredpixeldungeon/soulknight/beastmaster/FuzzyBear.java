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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAlly;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

//E: big bear. swipes hit everything around it, nearby enemies aggro on it, and standing next to it
//gives the beastmaster armor. E again while alive = come back here
public class FuzzyBear extends ChampionAbility {

	public static final FuzzyBear INSTANCE = new FuzzyBear();

	@Override public String slot() { return "E"; }
	@Override public int unlockLevel() { return 4; }
	@Override public int icon() { return HeroIcon.BM_BEAR; }
	@Override public int color() { return 0xAA7744; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }

	public static final int DURATION = 30;
	public static final int TAUNT_RANGE = 4;

	@Override
	public float cooldown( Hero hero ) {
		return 60;
	}

	//recall works on cd too
	@Override
	public boolean onCooldown( Hero hero ) {
		return of(hero) == null && super.onCooldown(hero);
	}

	public static Bear of( Hero hero ){
		java.util.ArrayList<Bear> bears = SKAlly.of(hero, Bear.class);
		return bears.isEmpty() ? null : bears.get(0);
	}

	public static float multi( Hero hero ){
		return 1f + 0.2f*hero.pointsInTalent(Talent.BM_BIG_BEAR);
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		Bear bear = of(hero);
		if (bear != null){
			bear.followHero();
			GLog.i(Messages.get(this, "recall"));
			hero.spendAndNext(1f);
			return;
		}

		int cell = -1;
		for (int n : PathFinder.NEIGHBOURS8){
			int c = hero.pos + n;
			if (Actor.findChar(c) == null && Dungeon.level.passable[c] && !Dungeon.level.solid[c] && !Dungeon.level.pit[c]){
				cell = c;
				break;
			}
		}
		if (cell == -1){
			GLog.w(Messages.get(this, "no_space"));
			return;
		}

		bear = new Bear();
		bear.setOwner(hero);
		bear.multi = multi(hero);
		bear.HP = bear.HT = Math.round((20 + 6*hero.lvl) * bear.multi);
		bear.defenseSkill = 4 + hero.lvl;
		bear.timeLeft = DURATION;
		bear.pos = cell;
		GameScene.add(bear);
		Dungeon.level.occupyCell(bear);
		if (bear.sprite != null) bear.sprite.emitter().burst(Speck.factory(Speck.WOOL), 10);
		Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 1f, 0.7f);

		//taunt
		for (Char ch : Actor.chars()){
			if (ch instanceof Mob && ch.alignment == Char.Alignment.ENEMY && ch.isAlive()
					&& Dungeon.level.distance(cell, ch.pos) <= TAUNT_RANGE){
				((Mob) ch).aggro(bear);
			}
		}

		Dungeon.observe();
		hero.sprite.zap(cell);
		Invisibility.dispel(hero);
		startCooldown(hero);
		hero.spendAndNext(1f);
	}

	//hug the bear, get armor
	public static int drBonus( Hero hero ){
		Bear b = of(hero);
		if (b == null || !Dungeon.level.adjacent(hero.pos, b.pos)) return 0;
		int top = 2 + hero.lvl/4;
		if (hero.subClass == HeroSubClass.FOREST_WARDEN) top += hero.pointsInTalent(Talent.FW_BEAR_HUG);
		return Random.NormalIntRange(1, top);
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", DURATION, Math.round((20 + 6*hero.lvl) * multi(hero)),
				Bear.minDmg(hero.lvl, multi(hero)), Bear.maxDmg(hero.lvl, multi(hero)), TAUNT_RANGE, 2 + hero.lvl/4);
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}

	public static class Bear extends SKAlly {

		{
			spriteClass = Sprite.class;
			HP = HT = 25;
			defenseSkill = 5;
		}

		public float multi = 1f;

		public static int minDmg( int power, float multi ){
			return Math.round((3 + power/2) * multi);
		}

		public static int maxDmg( int power, float multi ){
			return Math.round((8 + power) * multi);
		}

		@Override
		public int attackSkill( Char target ) {
			return 12 + power;
		}

		@Override
		public int damageRoll() {
			float m = multi;
			if (buff(PackRoar.Ferocity.class) != null) m += 0.25f;
			return Random.NormalIntRange(minDmg(power, m), maxDmg(power, m));
		}

		@Override
		public int drRoll() {
			return super.drRoll() + Random.NormalIntRange(1, 2 + power/3);
		}

		private boolean swiping = false;

		//main target full, everyone else around half
		@Override
		public boolean attack( Char enemy, float dmgMulti, float dmgBonus, float accMulti ) {
			boolean result = super.attack(enemy, dmgMulti, dmgBonus, accMulti);
			if (!swiping){
				swiping = true;
				for (int n : PathFinder.NEIGHBOURS8){
					Char ch = Actor.findChar(pos + n);
					if (ch != null && ch != enemy && ch.alignment == Alignment.ENEMY && ch.isAlive()){
						super.attack(ch, 0.5f*dmgMulti, 0, accMulti);
					}
				}
				swiping = false;
			}
			return result;
		}

		@Override
		public String description() {
			String d = Messages.get(this, "desc", minDmg(power, multi), maxDmg(power, multi));
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
			defenseSkill = 4 + power;
		}

		public static class Sprite extends SKMobSprite {
			public Sprite(){
				super();
				setup(SKAssets.BEAR, 24, 18);
			}
		}
	}
}
