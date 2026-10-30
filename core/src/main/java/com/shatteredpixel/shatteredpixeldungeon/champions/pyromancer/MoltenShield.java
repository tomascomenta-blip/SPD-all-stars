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

package com.shatteredpixel.shatteredpixeldungeon.champions.pyromancer;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

//E: fire shield on self or a nearby friend, melee attackers get burned
public class MoltenShield extends ChampionAbility {

	public static final MoltenShield INSTANCE = new MoltenShield();

	@Override public String slot() { return "E"; }
	@Override public int unlockLevel() { return 4; }
	@Override public int icon() { return HeroIcon.LOL_MOLTEN_SHIELD; }
	@Override public int color() { return 0xFFAA22; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }
	@Override public boolean targeted() { return true; }

	public static final int RANGE = 4;

	@Override
	public float cooldown( Hero hero ) {
		return 18;
	}

	public static int shield( Hero hero ){
		return 4 + hero.lvl/2 + 2*hero.pointsInTalent(Talent.PYR_WARMTH);
	}

	public static int burnDamage( Hero hero ){
		return 1 + hero.lvl/4;
	}

	@Override
	public String prompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		Char ally = target == null ? null : Actor.findChar(target);
		if (ally == null || ally.alignment != Char.Alignment.ALLY
				|| Dungeon.level.distance(hero.pos, ally.pos) > RANGE || !Dungeon.level.heroFOV[ally.pos]){
			ally = hero;
		}

		Barrier b = Buff.affect(ally, Barrier.class);
		b.incShield(shield(hero));
		Burnback bb = Buff.prolong(ally, Burnback.class, 6f);
		bb.damage = burnDamage(hero);
		if (hero.hasTalent(Talent.PYR_EMBER_STEP)){
			Buff.prolong(ally, Haste.class, hero.pointsInTalent(Talent.PYR_EMBER_STEP));
		}
		if (ally.sprite != null){
			ally.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shield(hero)), FloatingText.SHIELDING);
			ally.sprite.emitter().burst(FlameParticle.FACTORY, 10);
		}
		Sample.INSTANCE.play(Assets.Sounds.BURNING, 1f, 1.3f);
		if (ally != hero) hero.sprite.zap(ally.pos);
		else ChampionHooks.operateVisual(hero, hero.pos);

		Pyromania.onSupportCast(hero);
		startCooldown(hero);
		hero.spendAndNext(1f);
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", shield(hero), burnDamage(hero));
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}

	public static class Burnback extends FlavourBuff {

		{
			type = buffType.POSITIVE;
			announced = true;
		}

		public int damage = 1;

		public void onAttacked( Char attacker ){
			if (attacker == null || !attacker.isAlive() || attacker.alignment == target.alignment) return;
			if (!Dungeon.level.adjacent(attacker.pos, target.pos)) return;
			attacker.damage(damage, this);
			if (attacker.isAlive()) Buff.affect(attacker, Burning.class).reignite(attacker);
		}

		@Override
		public int icon() {
			return BuffIndicator.FIRE;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(1f, 0.6f, 0.1f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (6f - visualcooldown()) / 6f);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", damage, dispTurns());
		}

		private static final String DAMAGE = "damage";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(DAMAGE, damage);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			damage = bundle.getInt(DAMAGE);
		}
	}
}
