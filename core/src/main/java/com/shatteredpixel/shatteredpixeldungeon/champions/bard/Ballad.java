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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bless;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

//Q: marching song. bless around the bard, heroes also get Valor (+dmg)
public class Ballad extends BardSong {

	public static final Ballad INSTANCE = new Ballad();

	@Override public String slot() { return "Q"; }
	@Override public int unlockLevel() { return 1; }
	@Override public int icon() { return HeroIcon.BARD_BALLAD; }
	@Override public int color() { return 0xFFCC33; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }

	@Override
	public float cooldown( Hero hero ) {
		return 20;
	}

	public static int radius( Hero hero ){
		return 5 + hero.pointsInTalent(Talent.BAR_LOUD_VOICE)
				+ (hero.subClass == HeroSubClass.TROUBADOUR ? 2 : 0);
	}

	public static int baseDuration( Hero hero ){
		int d = 8;
		if (hero.subClass == HeroSubClass.TROUBADOUR) d += 3*hero.pointsInTalent(Talent.TB_ANTHEM);
		return d;
	}

	public static int shield( Hero hero ){
		return hero.pointsInTalent(Talent.BAR_RALLY) * (1 + hero.lvl/4);
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		boolean forte = Refrain.onSong(hero);
		ArrayList<Char> friends = friendsWithin(hero, radius(hero));
		float power = power(hero, forte, duet(friends));

		int dur = Math.round(baseDuration(hero) * power);
		int shield = Math.round(shield(hero) * power);
		for (Char ch : friends){
			Buff.prolong(ch, Bless.class, dur);
			if (ch instanceof Hero) Valor.give((Hero) ch, dur);
			if (shield > 0){
				Buff.affect(ch, Barrier.class).incShield(shield);
				if (ch.sprite != null) ch.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shield), FloatingText.SHIELDING);
			}
			harmony(hero, ch);
			notes(ch);
		}
		dissonance(hero, enemiesInView(hero.pos, radius(hero)), this);

		Sample.INSTANCE.play(Assets.Sounds.CHARMS, 1f, 1.2f);
		gesture(hero, hero.pos, false);
		finish(hero, friends.size());
	}

	@Override
	public String desc( Hero hero ) {
		String d = Messages.get(this, "desc", radius(hero), baseDuration(hero), Math.round(100*Valor.BONUS));
		if (shield(hero) > 0) d += " " + Messages.get(this, "desc_rally", shield(hero));
		if (dissonanceDamage(hero) > 0) d += " " + Messages.get(BardSong.class, "desc_dissonance", dissonanceDamage(hero));
		return d + forteDesc(hero, fortePercent(hero));
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}

	//+dmg on every attack, Char.attack reads damageFactor()
	public static class Valor extends Buff {

		public static final float BONUS = 0.15f;

		{
			type = buffType.POSITIVE;
			revivePersists = false;
		}

		public float left = 0;
		public float max = 1;

		public static void give( Hero hero, float turns ){
			Valor v = hero.buff(Valor.class);
			if (v == null){
				v = new Valor();
				if (!v.attachTo(hero)) return;
			}
			if (turns > v.left){
				v.left = turns;
				v.max = turns;
			}
		}

		public static float damageFactor( Char ch ){
			return ch.buff(Valor.class) != null ? 1f + BONUS : 1f;
		}

		//1 act per turn -> lasts 'left' turns
		@Override
		public boolean act() {
			if (left <= 0){
				detach();
			} else {
				left -= TICK;
				spend(TICK);
			}
			return true;
		}

		@Override
		public void fx( boolean on ) {
			//no aura, nada que dibujar
		}

		@Override
		public int icon() {
			return BuffIndicator.WEAPON;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(1f, 0.75f, 0.2f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, Math.min(1, (max - left) / Math.max(1, max)));
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString(Math.round(left));
		}

		@Override
		public String name() {
			return Messages.get(Valor.class, "name");
		}

		@Override
		public String desc() {
			return Messages.get(Valor.class, "desc", Math.round(100*BONUS), Math.round(left));
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
