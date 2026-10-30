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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.finale.reward;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Amok;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Daze;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Degrade;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Doom;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hex;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Light;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.Arrays;

//what each path of the heart actually does. everything here is called from the usual places
//(Char.damage, Hero.add, the FOV, hunger...) so it's all regular game logic, same on every device.
//the dark continent will plug into isPrey, DARK_AFFLICTIONS, essenceMax, minSight and hungerRate
public class SpecPowers {

	public static boolean has( Hero h, Specialization s ){
		return h != null && h.specialization == s;
	}

	private static int points( Hero h, Talent t ){
		return h.pointsInTalent(t);
	}

	//called once, when the path is picked
	public static void onChosen( Hero h ){
		if (has(h, Specialization.LIGHTBEARER)){
			HolyEssence e = Buff.affect(h, HolyEssence.class);
			e.count = essenceMax(h);
		}
	}

	// *** yog hunter ***

	//demons, yog and its brood. the yogs over there will be demonic too
	public static boolean isPrey( Char ch ){
		return ch != null && ch.alignment != Char.Alignment.ALLY && Char.hasProp(ch, Char.Property.DEMONIC);
	}

	public static float damageFactor( Char target, Object src ){
		float f = 1f;
		if (src instanceof Hero && has((Hero) src, Specialization.YOG_HUNTER) && isPrey(target)){
			f *= 1.2f + 0.1f * points((Hero) src, Talent.YH_BANE);
		}
		if (target instanceof Hero && has((Hero) target, Specialization.YOG_HUNTER)
				&& src instanceof Char && isPrey((Char) src)){
			f *= 1f - 0.1f * points((Hero) target, Talent.YH_HIDE);
		}
		return f;
	}

	public static int preySense( Hero h ){
		return has(h, Specialization.YOG_HUNTER) ? 6 + 2 * points(h, Talent.YH_SCENT) : 0;
	}

	public static void onMobDeath( Mob m, Object cause ){
		if (!(cause instanceof Hero) || !isPrey(m)) return;
		Hero h = (Hero) cause;
		if (!has(h, Specialization.YOG_HUNTER) || !h.isAlive() || points(h, Talent.YH_TROPHY) == 0) return;
		int heal = Math.min(h.HT - h.HP, Math.round(h.HT * 0.04f * points(h, Talent.YH_TROPHY)));
		if (heal <= 0) return;
		h.HP += heal;
		if (h.sprite != null){
			h.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
		}
	}

	// *** lightbearer ***

	//what the holy essence keeps away. add the corruption of the continent here
	public static final ArrayList<Class<? extends Buff>> DARK_AFFLICTIONS = new ArrayList<>(Arrays.asList(
			Blindness.class, Weakness.class, Vulnerable.class, Hex.class, Degrade.class,
			Terror.class, Charm.class, Vertigo.class, Daze.class, Amok.class, Doom.class));

	public static boolean isDarkAffliction( Class<?> c ){
		for (Class<? extends Buff> d : DARK_AFFLICTIONS){
			if (d.isAssignableFrom(c)) return true;
		}
		return false;
	}

	public static int minSight( Hero h ){
		return has(h, Specialization.LIGHTBEARER) ? Light.DISTANCE : 0;
	}

	public static int sightBonus( Hero h ){
		return has(h, Specialization.LIGHTBEARER) ? points(h, Talent.LB_BEACON) : 0;
	}

	public static int essenceMax( Hero h ){
		return has(h, Specialization.LIGHTBEARER) ? 2 + points(h, Talent.LB_VESSEL) : 0;
	}

	//turns for one more charge of essence
	public static int essenceTurns( Hero h ){
		int p = points(h, Talent.LB_DAWN);
		return p == 0 ? 40 : p == 1 ? 32 : p == 2 ? 26 : 20;
	}

	//Hero.add: true if a charge of essence stops this buff
	public static boolean ward( Hero h, Buff b ){
		if (b.type != Buff.buffType.NEGATIVE || !has(h, Specialization.LIGHTBEARER)) return false;
		if (!isDarkAffliction(b.getClass())) return false;
		HolyEssence e = h.buff(HolyEssence.class);
		if (e == null || e.count <= 0) return false;
		e.count--;
		BuffIndicator.refreshHero();
		CoopLog.line("essence wards " + b.getClass().getSimpleName() + ", " + e.count + " left");
		if (h.sprite != null){
			h.sprite.emitter().burst(Speck.factory(Speck.LIGHT), 6);
			h.sprite.showStatus(CharSprite.POSITIVE, Messages.get(SpecPowers.class, "warded"));
		}
		Sample.INSTANCE.play(Assets.Sounds.LIGHTNING, 0.4f, 1.6f);
		int p = points(h, Talent.LB_RADIANCE);
		if (p > 0 && h.HP < h.HT){
			int heal = Math.min(h.HT - h.HP, Math.max(1, Math.round(h.HT * 0.05f * p)));
			h.HP += heal;
			if (h.sprite != null){
				h.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
			}
		}
		return true;
	}

	public static class HolyEssence extends Buff {

		{
			type = buffType.POSITIVE;
			revivePersists = true;
		}

		public int count = 0;
		private float partial = 0;

		@Override
		public boolean act() {
			if (target instanceof Hero){
				Hero h = (Hero) target;
				int max = essenceMax(h);
				if (count < max){
					partial += 1f / essenceTurns(h);
					if (partial >= 1f){
						partial = 0;
						count++;
						BuffIndicator.refreshHero();
					}
				} else {
					count = Math.min(count, Math.max(max, 0));
					partial = 0;
				}
			}
			spend(TICK);
			return true;
		}

		@Override
		public int icon() {
			return BuffIndicator.LIGHT;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(1f, 0.85f, 0.4f);
		}

		@Override
		public float iconFadePercent() {
			return count > 0 ? 0 : Math.max(0, 1f - partial);
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString(count);
		}

		@Override
		public String desc() {
			Hero h = target instanceof Hero ? (Hero) target : null;
			return Messages.get(this, "desc", count, h == null ? 2 : essenceMax(h), h == null ? 40 : essenceTurns(h));
		}

		private static final String COUNT   = "count";
		private static final String PARTIAL = "partial";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(COUNT, count);
			bundle.put(PARTIAL, partial);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			count = bundle.getInt(COUNT);
			partial = bundle.getFloat(PARTIAL);
		}
	}

	// *** time wanderer ***

	public static float hungerRate( Hero h ){
		return has(h, Specialization.TIME_WANDERER) ? 0.7f - 0.1f * points(h, Talent.TW_STOMACH) : 1f;
	}

	public static float speedFactor( Hero h ){
		return has(h, Specialization.TIME_WANDERER) ? 1f + 0.1f * points(h, Talent.TW_QUICKSTEP) : 1f;
	}

	public static float clockRate( Hero h ){
		return has(h, Specialization.TIME_WANDERER) ? 1f + 0.2f * points(h, Talent.TW_CLOCKWORK) : 1f;
	}

	public static int slipTurns( Hero h ){
		return 3 + points(h, Talent.TW_LONG_PAUSE);
	}

	public static int slipCooldown( Hero h ){
		return 60 - 10 * points(h, Talent.TW_LONG_PAUSE);
	}

	//after taking a hit: below 30% hp the world slows down around a time wanderer
	public static void onHeroDamaged( Hero h ){
		if (!has(h, Specialization.TIME_WANDERER) || !h.isAlive()) return;
		if (h.HP >= h.HT * 0.3f || h.buff(SlipCooldown.class) != null) return;
		Buff.affect(h, SlipCooldown.class, slipCooldown(h));
		int turns = slipTurns(h);
		int n = 0;
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (m.alignment == Char.Alignment.ENEMY && m.isAlive() && h.fieldOfView != null && h.fieldOfView[m.pos]){
				Buff.affect(m, Slow.class, turns);
				n++;
			}
		}
		CoopLog.line("time slip at hp " + h.HP + "/" + h.HT + ", " + n + " slowed");
		if (h.sprite != null){
			h.sprite.showStatus(CharSprite.POSITIVE, Messages.get(SpecPowers.class, "slip"));
		}
		Sample.INSTANCE.play(Assets.Sounds.TELEPORT, 0.6f, 0.6f);
		if (n > 0) GLog.p(Messages.get(SpecPowers.class, "slip_log", n));
	}

	public static class SlipCooldown extends FlavourBuff {

		{
			type = buffType.NEUTRAL;
			revivePersists = true;
		}

		@Override
		public int icon() {
			return BuffIndicator.TIME;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(0.5f, 0.35f, 0.7f);
		}

		@Override
		public float iconFadePercent() {
			float max = target instanceof Hero ? slipCooldown((Hero) target) : 60;
			return Math.max(0, visualcooldown() / max);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", dispTurns());
		}
	}
}
