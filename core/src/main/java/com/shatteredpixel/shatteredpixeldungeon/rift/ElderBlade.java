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

package com.shatteredpixel.shatteredpixeldungeon.rift;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.DeathMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Brute;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.PurpleParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;

//elder blade, one each. tier 5, purple fire, executes low hp stuff. very fun, probably too strong
public class ElderBlade extends MeleeWeapon {

	{
		image = ItemSpriteSheet.ELDER_BLADE;
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 0.9f;

		tier = 5;
	}

	public static final float BOSS_EXECUTE = 0.08f;
	public static final int BURN_TURNS = 3;

	//share of health under which a regular enemy is executed: 20%, +1% per upgrade, up to 30%
	public static float executeThreshold( int lvl ){
		return Math.min(0.30f, 0.20f + 0.01f*Math.max(0, lvl));
	}

	public static float executeThreshold( Char ch, int lvl ){
		if (Char.hasProp(ch, Char.Property.BOSS) || Char.hasProp(ch, Char.Property.MINIBOSS)) return BOSS_EXECUTE;
		return executeThreshold(lvl);
	}

	public static int burnPower( int lvl ){
		return 2 + Math.max(0, lvl)/2;
	}

	@Override
	public int proc( Char attacker, final Char defender, int damage ) {
		damage = super.proc(attacker, defender, damage);
		if (defender != attacker && defender.alignment != attacker.alignment && defender.isAlive()){
			int lvl = buffedLvl();
			ElderBurn.apply(defender, burnPower(lvl), BURN_TURNS);
			//the execution is checked right after the hit lands, with the health it really left
			final float threshold = executeThreshold(defender, lvl);
			final Char from = attacker;
			Actor.add(new Actor() {
				{
					actPriority = VFX_PRIO;
				}

				@Override
				protected boolean act() {
					Actor.remove(this);
					tryExecute(from, defender, threshold);
					return true;
				}
			});
		}
		return damage;
	}

	public static void tryExecute( Char attacker, Char defender, float threshold ){
		if (!defender.isAlive() || defender instanceof Hero) return;
		if (defender.alignment == attacker.alignment) return;
		if (defender.HP > Math.round(defender.HT * threshold)) return;
		if (defender.isInvulnerable(attacker.getClass())) return;

		defender.HP = 0;
		for (Buff b : defender.buffs(Brute.BruteRage.class)){
			b.detach();
		}
		if (defender.sprite != null){
			defender.sprite.showStatus(CharSprite.NEGATIVE, Messages.get(ElderBlade.class, "executed"));
			if (defender.sprite.visible) defender.sprite.emitter().burst(PurpleParticle.BURST, 15);
		}
		if (Dungeon.level.heroFOV[defender.pos]) Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG, 1f, 0.8f);
		if (!defender.isAlive()){
			defender.die(attacker);
		} else {
			DeathMark.processFearTheReaper(defender);
		}
	}

	@Override
	public String desc() {
		int lvl = levelKnown ? buffedLvl() : 0;
		return Messages.get(this, "desc", Math.round(executeThreshold(lvl)*100), Math.round(BOSS_EXECUTE*100),
				burnPower(lvl), BURN_TURNS);
	}

	//Duelist: the same cleave as the greatsword
	@Override
	protected int baseChargeUse( Hero hero, Char target ){
		if (hero.buff(Sword.CleaveTracker.class) != null){
			return 0;
		} else {
			return 1;
		}
	}

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	protected void duelistAbility( Hero hero, Integer target ) {
		int dmgBoost = augment.damageFactor(7 + buffedLvl());
		Sword.cleaveAbility(hero, target, 1, dmgBoost, this);
	}

	@Override
	public String abilityInfo() {
		int dmgBoost = levelKnown ? 7 + buffedLvl() : 7;
		if (levelKnown){
			return Messages.get(this, "ability_desc", augment.damageFactor(min()+dmgBoost), augment.damageFactor(max()+dmgBoost));
		} else {
			return Messages.get(this, "typical_ability_desc", min(0)+dmgBoost, max(0)+dmgBoost);
		}
	}

	@Override
	public String upgradeAbilityStat( int level ){
		int dmgBoost = 7 + level;
		return augment.damageFactor(min(level)+dmgBoost) + "-" + augment.damageFactor(max(level)+dmgBoost);
	}
}
