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

package com.shatteredpixel.shatteredpixeldungeon.champions.guardian;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

//R: big sand giant mode for a few turns. +max hp, some armor, sandstorm chews adjacent enemies,
//siphoning strike recharges 2x
public class FuryOfSands extends ChampionAbility {

	public static final FuryOfSands INSTANCE = new FuryOfSands();

	@Override public String slot() { return "R"; }
	@Override public int unlockLevel() { return 6; }
	@Override public int icon() { return HeroIcon.LOL_FURY_OF_SANDS; }
	@Override public int color() { return 0xE8B850; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }

	@Override
	public float cooldown( Hero hero ) {
		float cd = 90;
		if (hero.subClass == HeroSubClass.COLOSSUS) cd *= 0.75f;
		return Math.round(cd);
	}

	public static int duration( Hero hero ){
		int d = 20 + 3*hero.pointsInTalent(Talent.GUA_ETERNAL);
		if (hero.subClass == HeroSubClass.COLOSSUS) d += 5;
		return d;
	}

	//fraction of normal max hp
	public static float healthBoost( Hero hero ){
		float b = 0.4f;
		if (hero.subClass == HeroSubClass.COLOSSUS) b += 0.1f*hero.pointsInTalent(Talent.CO_GIANT);
		return b;
	}

	//% of enemy max hp per turn
	public static int auraPercent( Hero hero ){
		return 2 + hero.pointsInTalent(Talent.GUA_DUSTSTORM);
	}

	public static int auraFlat( Hero hero ){
		return 1 + hero.lvl/8;
	}

	//%maxhp part is capped vs bosses. obviously
	public static int bossCap( Hero hero ){
		return 1 + hero.lvl/6 + hero.pointsInTalent(Talent.GUA_DUSTSTORM);
	}

	public static int auraDamage( Hero hero, Char ch ){
		int pct = Math.round(ch.HT * auraPercent(hero) / 100f);
		if (Char.hasProp(ch, Char.Property.BOSS) || Char.hasProp(ch, Char.Property.MINIBOSS)){
			return Math.min(Math.round(ch.HT * auraPercent(hero) / 200f), bossCap(hero));
		}
		return auraFlat(hero) + pct;
	}

	public static int armor( Hero hero ){
		return 2 + hero.lvl/5;
	}

	//colossus flat DR
	public static int unbroken( Hero hero ){
		if (hero.subClass != HeroSubClass.COLOSSUS) return 0;
		int p = hero.pointsInTalent(Talent.CO_UNBROKEN);
		return p + p*hero.lvl/12;
	}

	public static float spriteScale( Hero hero ){
		float s = 1.25f;
		if (hero.subClass == HeroSubClass.COLOSSUS) s += 0.05f + 0.05f*hero.pointsInTalent(Talent.CO_GIANT);
		return s;
	}

	public static boolean active( Hero hero ){
		return hero.buff(Fury.class) != null;
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		Fury fury = hero.buff(Fury.class);
		if (fury == null){
			//lend the hp now, the buff takes it back at the end
			SoulEater se = Buff.affect(hero, SoulEater.class);
			int before = hero.HT;
			int boost = Math.round((20 + 5*(hero.lvl-1) + hero.HTBoost - se.furyBoost) * healthBoost(hero));
			hero.HTBoost += boost;
			se.furyBoost += boost;
			hero.updateHT(false);
			int gained = hero.HT - before;
			if (gained > 0){
				hero.HP = Math.min(hero.HT, hero.HP + gained);
				if (hero.sprite != null) hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(gained), FloatingText.HEALING);
			}
			fury = Buff.affect(hero, Fury.class);
		}
		fury.set(duration(hero));

		if (hero.sprite != null){
			//no operate anim here!! its callback pokes curAction from the render thread. coop. you know the drill
			hero.sprite.flash();
			hero.sprite.emitter().burst(SandParticle.FACTORY, 30);
		}
		for (int n : PathFinder.NEIGHBOURS8){
			int c = hero.pos + n;
			if (c >= 0 && c < Dungeon.level.length() && Dungeon.level.heroFOV[c]){
				CellEmitter.get(c).burst(SandParticle.FACTORY, 6);
			}
		}
		Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 1f, 0.8f);
		Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.6f, 0.7f);
		PixelScene.shake(1.5f, 0.4f);
		startCooldown(hero);
		hero.spendAndNext(1f);
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", duration(hero), Math.round(healthBoost(hero)*100),
				auraFlat(hero), auraPercent(hero), bossCap(hero), armor(hero));
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}

	public static class Fury extends Buff {

		{
			type = buffType.POSITIVE;
			announced = true;
		}

		public int left = 0;
		public int max = 1;

		public void set( int turns ){
			left = Math.max(left, turns);
			max = Math.max(max, left);
		}

		@Override
		public boolean act() {
			if (!(target instanceof Hero) || !target.isAlive()){
				detach();
				return true;
			}
			Hero hero = (Hero) target;

			for (Char ch : ChampionHooks.enemiesAround(hero, hero.pos, 1)){
				if (!ch.isAlive()) continue;
				ch.damage(auraDamage(hero, ch), this);
				if (ch.sprite != null && ch.sprite.visible) ch.sprite.emitter().burst(SandParticle.FACTORY, 3);
			}

			//2x Q recharge
			SiphoningStrike.CD cd = hero.buff(SiphoningStrike.CD.class);
			if (cd != null) cd.reduce(1f);

			left--;
			if (left <= 0){
				detach();
			} else {
				spend(TICK);
			}
			return true;
		}

		@Override
		public void detach() {
			super.detach();
			if (target instanceof Hero) SoulEater.endFury((Hero) target);
		}

		@Override
		public void fx( boolean on ) {
			if (target.sprite == null) return;
			if (on && target instanceof Hero){
				target.sprite.scale.set(spriteScale((Hero) target));
			} else {
				target.sprite.scale.set(1f);
			}
			if (!target.sprite.isMoving) target.sprite.place(target.pos);
		}

		@Override
		public int icon() {
			return BuffIndicator.FURY;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(1f, 0.8f, 0.35f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, 1f - left / (float) Math.max(1, max));
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString(left);
		}

		@Override
		public String desc() {
			int hp = 0, flat = 0, pct = 0, arm = 0;
			if (target instanceof Hero){
				Hero hero = (Hero) target;
				SoulEater se = SoulEater.of(hero);
				hp = se == null ? 0 : se.furyBoost;
				flat = auraFlat(hero);
				pct = auraPercent(hero);
				arm = armor(hero);
			}
			return Messages.get(this, "desc", hp, flat, pct, arm, left);
		}

		private static final String LEFT = "left";
		private static final String MAX = "max";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(LEFT, left);
			bundle.put(MAX, max);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			left = bundle.getInt(LEFT);
			max = bundle.getInt(MAX);
		}
	}

	//sand particles. pretty
	public static class SandParticle extends PixelParticle {

		public static final Emitter.Factory FACTORY = new Emitter.Factory() {
			@Override
			public void emit( Emitter emitter, int index, float x, float y ) {
				((SandParticle) emitter.recycle(SandParticle.class)).reset(x, y);
			}
		};

		public SandParticle() {
			super();
			lifespan = 0.8f;
		}

		public void reset( float x, float y ) {
			revive();
			this.x = x;
			this.y = y;
			left = lifespan;
			color(Random.Visual.Int(2) == 0 ? 0xE8C060 : 0xC89838);
			size(Random.Visual.Float(1f, 2.5f));
			speed.polar(Random.Visual.Float(0, 6.283f), Random.Visual.Float(12, 36));
			acc.set(0, 20);
		}

		@Override
		public void update() {
			super.update();
			am = left / lifespan;
		}
	}
}
