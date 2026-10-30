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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.gunslinger;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

//W: bounty on an enemy. takes more dmg from everyone, his shots prefer it, dies -> gold + bounty
//hops to the closest visible enemy
public class BountyTag extends ChampionAbility {

	public static final BountyTag INSTANCE = new BountyTag();

	@Override public String slot() { return "W"; }
	@Override public int unlockLevel() { return 2; }
	@Override public int icon() { return HeroIcon.GS_BOUNTY; }
	@Override public int color() { return 0xCC3333; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }
	@Override public boolean targeted() { return true; }

	public static final int RANGE = 8;

	@Override
	public float cooldown( Hero hero ) {
		return 25;
	}

	public static int duration( Hero hero ){
		int t = 25;
		if (hero.subClass == HeroSubClass.BOUNTY_HUNTER) t += 10*hero.pointsInTalent(Talent.BH_LONG_HUNT);
		return t;
	}

	public static float factor( Hero hero ){
		if (hero.subClass == HeroSubClass.BOUNTY_HUNTER) return 1.4f + 0.1f*hero.pointsInTalent(Talent.BH_MARKED);
		return 1.25f;
	}

	public static int reward( Hero hero ){
		float g = 10 + 5*Dungeon.depth;
		if (hero != null){
			if (hero.subClass == HeroSubClass.BOUNTY_HUNTER) g *= 2;
			g *= 1f + 0.5f*hero.pointsInTalent(Talent.GS_BIG_BOUNTY);
		}
		return Math.round(g);
	}

	//one per gunslinger
	public static void mark( Hero hero, Char ch, float turns ){
		int me = ChampionHooks.indexOf(hero);
		for (Char other : Actor.chars()){
			Mark old = other.buff(Mark.class);
			if (old != null && old.owner == me) old.detach();
		}
		Mark m = Buff.affect(ch, Mark.class, turns);
		m.owner = me;
		m.factor = factor(hero);
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		if (target == null) return;
		Char ch = Actor.findChar(target);
		if (ch == null || ch.alignment != Char.Alignment.ENEMY || !Dungeon.level.heroFOV[target]){
			GLog.w(Messages.get(this, "no_target"));
			return;
		}
		if (Dungeon.level.distance(hero.pos, target) > RANGE){
			GLog.w(Messages.get(ChampionAbility.class, "too_far"));
			return;
		}
		mark(hero, ch, duration(hero));
		if (ch.sprite != null) ch.sprite.showStatus(CharSprite.WARNING, Messages.get(this, "wanted"));
		Sample.INSTANCE.play(Assets.Sounds.GOLD, 0.8f, 0.8f);

		ChampionHooks.operateVisual(hero, target);
		Invisibility.dispel(hero);
		startCooldown(hero);
		hero.spendAndNext(1f);
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", RANGE, duration(hero), Math.round((factor(hero)-1)*100), reward(hero));
	}

	public static class Mark extends FlavourBuff {

		{
			type = buffType.NEGATIVE;
			announced = true;
		}

		public int owner = 0;
		public float factor = 1.25f;

		@Override
		public int icon() {
			return BuffIndicator.MARK;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(1f, 0.75f, 0.2f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (25 - visualcooldown()) / 25f);
		}

		@Override
		public void fx( boolean on ) {
			if (target.sprite == null) return;
			if (on) target.sprite.add(CharSprite.State.MARKED);
			else target.sprite.remove(CharSprite.State.MARKED);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", Math.round((factor-1)*100), dispTurns());
		}

		private static final String OWNER  = "owner";
		private static final String FACTOR = "factor";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(OWNER, owner);
			bundle.put(FACTOR, factor);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			owner = bundle.getInt(OWNER);
			factor = bundle.getFloat(FACTOR);
		}
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}
}
