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

package com.shatteredpixel.shatteredpixeldungeon.champions.bard;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Adrenaline;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

//R: finale. every party hero gets an extra turn (2 w/ fortissimo) before enemies move, enemies in sight slowed
public class Finale extends BardSong {

	public static final Finale INSTANCE = new Finale();

	@Override public String slot() { return "R"; }
	@Override public int unlockLevel() { return 6; }
	@Override public int icon() { return HeroIcon.BARD_FINALE; }
	@Override public int color() { return 0xFF66CC; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }

	@Override
	public float cooldown( Hero hero ) {
		return 90;
	}

	public static int slowTurns( Hero hero ){
		int t = 3;
		if (hero.subClass == HeroSubClass.MINSTREL) t += hero.pointsInTalent(Talent.MI_CRESCENDO);
		return t;
	}

	public static int crescendoDamage( Hero hero ){
		if (hero.subClass != HeroSubClass.MINSTREL) return 0;
		return hero.pointsInTalent(Talent.MI_CRESCENDO) * (2 + hero.lvl/2);
	}

	public static int hasteTurns( Hero hero ){
		int p = hero.pointsInTalent(Talent.BAR_TEMPO);
		return p == 0 ? 0 : 1 + 2*p;
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		boolean forte = Refrain.onSong(hero);
		int turns = forte ? 2 : 1;

		//all party heroes anywhere on the floor + allies in sight
		ArrayList<Char> friends = new ArrayList<>();
		friends.add(hero);
		for (Hero h : Party.living()){
			if (h != hero && isFriend(h)) friends.add(h);
		}
		for (Char ch : friendsInView(hero)){
			if (!friends.contains(ch)) friends.add(ch);
		}
		float power = power(hero, forte, duet(friends));

		int haste = hasteTurns(hero);
		for (Char ch : friends){
			if (ch instanceof Hero){
				//bard gets it before the song's time is spent, so it's refunded right away. order matters here
				Encore.give((Hero) ch, turns);
				//interrupt the partner so they actually get to use it
				if (ch != hero) ((Hero) ch).interrupt();
				if (haste > 0) Buff.prolong(ch, Haste.class, haste);
			} else {
				//allies can't do the turn trick, they just get adrenaline
				Buff.prolong(ch, Adrenaline.class, 2*turns);
			}
			harmony(hero, ch);
			notes(ch);
		}

		int slow = Math.round(slowTurns(hero) * power);
		int dmg = Math.round(crescendoDamage(hero) * power);
		for (Char ch : enemiesInView(hero.pos, Integer.MAX_VALUE/2)){
			if (dmg > 0) ch.damage(dmg, this);
			if (ch.isAlive()) Buff.prolong(ch, Slow.class, slow);
			if (ch.sprite != null && ch.sprite.visible) ch.sprite.emitter().burst(Speck.factory(Speck.NOTE), 4);
		}

		Sample.INSTANCE.play(Assets.Sounds.MASTERY, 0.8f, 1.2f);
		Sample.INSTANCE.play(Assets.Sounds.CHARMS, 1f, 1.4f);
		gesture(hero, hero.pos, false);
		finish(hero, friends.size());
	}

	@Override
	public String desc( Hero hero ) {
		String d = Messages.get(this, "desc", slowTurns(hero));
		if (crescendoDamage(hero) > 0) d += " " + Messages.get(this, "desc_crescendo", crescendoDamage(hero));
		if (hasteTurns(hero) > 0) d += " " + Messages.get(this, "desc_tempo", hasteTurns(hero));
		return d + forteDesc(hero, fortePercent(hero));
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}

	/*
	 extra turns. acts right after the heroes (before blobs/mobs/other buffs) and refunds the time
	 the hero's last action took, up to 'left' turns, so they go again before anyone else.
	 only touches the actor clock -> same on every device, coop safe. took like 4 tries to get this right
	*/
	public static class Encore extends Buff {

		{
			type = buffType.POSITIVE;
			announced = true;
			actPriority = HERO_PRIO - 1;
		}

		public float left = 0;
		public float max = 1;

		public static void give( Hero hero, float turns ){
			Encore e = hero.buff(Encore.class);
			if (e == null){
				e = new Encore();
				if (!e.attachTo(hero)) return;
				//wait for the hero's next action to end (for the bard that's now)
				float wait = hero.cooldown() - e.cooldown();
				if (wait > 0) e.spend(wait);
			}
			e.left += turns;
			e.max = Math.max(e.max, e.left);
			BuffIndicator.refreshHero();
		}

		@Override
		public boolean act() {
			if (!(target instanceof Hero) || !target.isAlive() || left <= 0.001f){
				detach();
				return true;
			}
			float spent = target.cooldown();
			if (spent <= 0){
				//down or off-level
				detach();
				return true;
			}
			float refund = Math.min(left, spent);
			left -= refund;
			((Hero) target).spendConstant(-refund);
			if (left <= 0.001f) detach();
			//no time spent, hero goes again. no sé por qué pero funciona
			return true;
		}

		@Override
		public int icon() {
			return BuffIndicator.TIME;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(1f, 0.4f, 0.8f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, Math.min(1, 1f - left / Math.max(1, max)));
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString((int) Math.ceil(left - 0.001f));
		}

		@Override
		public String name() {
			return Messages.get(Encore.class, "name");
		}

		@Override
		public String desc() {
			return Messages.get(Encore.class, "desc", dispTurns(left));
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
			left = bundle.getFloat(LEFT);
			max = bundle.getFloat(MAX);
		}
	}
}
