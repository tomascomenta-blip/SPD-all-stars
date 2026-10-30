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
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corrosion;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Daze;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Degrade;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hex;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LockedFloor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Lightning;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.HashSet;

//drake armor. tier 4 / 16 str (scale mail numbers) so it's still good late, + its element on top of
//any glyph. the non-number effects live in the Soul buff while it's worn (don't ask, it was easier)
public abstract class DrakeArmor extends Armor {

	public DrakeArmor() {
		super(4);
	}

	public abstract int element();

	public static DrakeArmor forElement( int element ){
		switch (Drake.valid(element)){
			case Drake.INFERNAL: default: return new Infernal();
			case Drake.OCEAN:    return new Ocean();
			case Drake.CLOUD:    return new Cloud();
			case Drake.MOUNTAIN: return new Mountain();
			case Drake.HEXTECH:  return new Hextech();
			case Drake.CHEMTECH: return new Chemtech();
		}
	}

	//strength of the effect, grows with the armor's level
	protected int power(){
		return Math.max(0, levelKnown ? buffedLvl() : 0);
	}

	protected static boolean active( Char ch ){
		return ch != null && ch.buff(MagicImmune.class) == null;
	}

	@Override
	public void activate( Char ch ) {
		super.activate(ch);
		Buff.affect(ch, Soul.class);
	}

	@Override
	public String desc() {
		return super.desc() + "\n\n" + effectDesc();
	}

	public abstract String effectDesc();

	@Override
	public int value() {
		return super.value() * 2;
	}

	// *** the six armors ***

	//immune to burning; melee attackers may catch fire
	public static class Infernal extends DrakeArmor {
		{
			image = ItemSpriteSheet.DRAKE_ARMOR_INFERNAL;
		}

		@Override
		public int element() { return Drake.INFERNAL; }

		public int burnChance(){
			return Math.min(50, 25 + 3*power());
		}

		@Override
		public int proc( Char attacker, Char defender, int damage ) {
			damage = super.proc(attacker, defender, damage);
			if (active(defender) && attacker != null && attacker != defender && attacker.isAlive()
					&& Dungeon.level.adjacent(attacker.pos, defender.pos)
					&& !attacker.isImmune(Burning.class)
					&& Random.Int(100) < burnChance()){
				Buff.affect(attacker, Burning.class).reignite(attacker, 5f);
			}
			return damage;
		}

		@Override
		public String effectDesc() {
			return Messages.get(DrakeArmor.class, "effect_0", burnChance());
		}
	}

	//extra regeneration
	public static class Ocean extends DrakeArmor {
		{
			image = ItemSpriteSheet.DRAKE_ARMOR_OCEAN;
		}

		@Override
		public int element() { return Drake.OCEAN; }

		//turns for each extra point of health
		public float regenDelay(){
			return Math.max(2f, 5f - 0.3f*power());
		}

		@Override
		public String effectDesc() {
			return Messages.get(DrakeArmor.class, "effect_1", Messages.decimalFormat("#.#", regenDelay()));
		}
	}

	//faster and more evasive
	public static class Cloud extends DrakeArmor {
		{
			image = ItemSpriteSheet.DRAKE_ARMOR_CLOUD;
		}

		public static final float SPEED = 1.25f;
		public static final float EVASION = 1.2f;

		@Override
		public int element() { return Drake.CLOUD; }

		@Override
		public float speedFactor( Char owner, float speed ) {
			speed = super.speedFactor(owner, speed);
			return active(owner) ? speed * SPEED : speed;
		}

		@Override
		public float evasionFactor( Char owner, float evasion ) {
			if (testingNoArmDefSkill) return evasion;
			evasion = super.evasionFactor(owner, evasion);
			return active(owner) ? evasion * EVASION : evasion;
		}

		@Override
		public String effectDesc() {
			return Messages.get(DrakeArmor.class, "effect_2", Math.round((SPEED-1)*100), Math.round((EVASION-1)*100));
		}
	}

	//more armor, and a shield when hit hard
	public static class Mountain extends DrakeArmor {
		{
			image = ItemSpriteSheet.DRAKE_ARMOR_MOUNTAIN;
		}

		public static final int SHIELD_COOLDOWN = 15;

		@Override
		public int element() { return Drake.MOUNTAIN; }

		public int extraArmor( int lvl ){
			if (Dungeon.isChallenged(Challenges.NO_ARMOR)) return 0;
			return 2 + Math.max(0, lvl);
		}

		@Override
		public int DRMax( int lvl ) {
			return super.DRMax(lvl) + extraArmor(lvl);
		}

		public int shieldBonus(){
			return 2 + power();
		}

		@Override
		public int proc( Char attacker, Char defender, int damage ) {
			damage = super.proc(attacker, defender, damage);
			Soul soul = defender.buff(Soul.class);
			if (active(defender) && soul != null && soul.shieldCooldown <= 0 && damage >= defender.HT / 5){
				int shield = defender.HT / 10 + shieldBonus();
				Buff.affect(defender, Barrier.class).setShield(shield);
				soul.shieldCooldown = SHIELD_COOLDOWN;
				if (defender.sprite != null){
					defender.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shield), FloatingText.SHIELDING);
				}
			}
			return damage;
		}

		@Override
		public String effectDesc() {
			return Messages.get(DrakeArmor.class, "effect_3", extraArmor(power()), shieldBonus(), SHIELD_COOLDOWN);
		}
	}

	//lightning on whoever hits you
	public static class Hextech extends DrakeArmor {
		{
			image = ItemSpriteSheet.DRAKE_ARMOR_HEXTECH;
		}

		@Override
		public int element() { return Drake.HEXTECH; }

		public int minZap(){ return 3 + power(); }
		public int maxZap(){ return 7 + 2*power(); }

		@Override
		public int proc( Char attacker, Char defender, int damage ) {
			damage = super.proc(attacker, defender, damage);
			if (active(defender) && attacker != null && attacker != defender && attacker.isAlive()
					&& attacker.alignment != defender.alignment && Random.Int(3) == 0){
				if (defender.sprite != null && defender.sprite.parent != null && attacker.sprite != null
						&& Dungeon.level.heroFOV[attacker.pos]){
					defender.sprite.parent.addToFront(new Lightning(defender.pos, attacker.pos, null));
					CellEmitter.center(attacker.pos).burst(SparkParticle.FACTORY, 4);
					Sample.INSTANCE.play(Assets.Sounds.LIGHTNING);
				}
				attacker.damage(Random.NormalIntRange(minZap(), maxZap()), new Discharge());
			}
			return damage;
		}

		@Override
		public String effectDesc() {
			return Messages.get(DrakeArmor.class, "effect_4", minZap(), maxZap());
		}
	}

	public static class Discharge {
	}

	//negative effects are weaker, more healing when low
	public static class Chemtech extends DrakeArmor {
		{
			image = ItemSpriteSheet.DRAKE_ARMOR_CHEMTECH;
		}

		public static final float HEAL_BONUS = 0.3f;

		@Override
		public int element() { return Drake.CHEMTECH; }

		@Override
		public String effectDesc() {
			return Messages.get(DrakeArmor.class, "effect_5", Math.round(HEAL_BONUS*100));
		}
	}

	//the negative effects the chemtech armor halves (their duration, or their damage for poison and acids)
	private static final Class<?>[] CHEMTECH_RESISTS = new Class<?>[]{
			Poison.class, Bleeding.class, Ooze.class, Corrosion.class,
			Blindness.class, Cripple.class, Weakness.class, Vertigo.class, Slow.class, Roots.class,
			Hex.class, Vulnerable.class, Daze.class, Degrade.class, Chill.class, Frost.class, Paralysis.class
	};

	// *** effects that are not part of the armor's numbers ***

	public static class Soul extends Buff {

		{
			type = buffType.POSITIVE;
			revivePersists = true;
		}

		private float regen = 0;          //ocean: partial health
		private int lastHP = -1;          //chemtech: health on the previous turn
		private float healBonus = 0;      //chemtech: partial extra healing
		public float shieldCooldown = 0;  //mountain: turns until the next shield

		//the drake armor being worn, null if none
		public DrakeArmor armor(){
			if (target instanceof Hero && ((Hero) target).belongings.armor() instanceof DrakeArmor){
				return (DrakeArmor) ((Hero) target).belongings.armor();
			}
			return null;
		}

		private int element(){
			DrakeArmor a = armor();
			return a == null ? -1 : a.element();
		}

		@Override
		public boolean act() {
			if (!(target instanceof Hero) || !(((Hero) target).belongings.armor instanceof DrakeArmor)){
				detach();
				return true;
			}
			DrakeArmor a = armor();
			if (shieldCooldown > 0) shieldCooldown = Math.max(0, shieldCooldown - TICK);

			if (a != null && target.isAlive() && active(target)){
				if (a instanceof Ocean){
					LockedFloor lock = target.buff(LockedFloor.class);
					boolean regenOn = lock == null || lock.regenOn();
					if (regenOn && target.HP < target.HT && !((Hero) target).isStarving()){
						regen += 1f / ((Ocean) a).regenDelay();
						if (regen >= 1){
							int heal = (int) regen;
							regen -= heal;
							target.HP = Math.min(target.HT, target.HP + heal);
						}
					}
				}
				if (a instanceof Chemtech){
					if (lastHP >= 0 && target.HP > lastHP && lastHP < target.HT / 2){
						healBonus += (target.HP - lastHP) * Chemtech.HEAL_BONUS;
						int extra = (int) healBonus;
						if (extra > 0 && target.HP < target.HT){
							healBonus -= extra;
							extra = Math.min(extra, target.HT - target.HP);
							target.HP += extra;
							if (target.sprite != null){
								target.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(extra), FloatingText.HEALING);
							}
						}
					}
					lastHP = target.HP;
				} else {
					lastHP = -1;
				}
			}

			spend(TICK);
			return true;
		}

		@Override
		public HashSet<Class> immunities() {
			HashSet<Class> result = super.immunities();
			if (element() == Drake.INFERNAL && active(target)) result.add(Burning.class);
			return result;
		}

		@Override
		public HashSet<Class> resistances() {
			HashSet<Class> result = super.resistances();
			if (element() == Drake.CHEMTECH && active(target)){
				for (Class<?> c : CHEMTECH_RESISTS) result.add(c);
			}
			return result;
		}

		@Override
		public int icon() {
			return element() >= 0 ? BuffIndicator.ARMOR : BuffIndicator.NONE;
		}

		@Override
		public void tintIcon( Image icon ) {
			int c = Drake.color(Math.max(0, element()));
			icon.hardlight(((c >> 16) & 0xFF) / 255f, ((c >> 8) & 0xFF) / 255f, (c & 0xFF) / 255f);
		}

		@Override
		public float iconFadePercent() {
			if (element() == Drake.MOUNTAIN && shieldCooldown > 0){
				return Math.min(1f, shieldCooldown / Mountain.SHIELD_COOLDOWN);
			}
			return 0;
		}

		@Override
		public String name() {
			return Messages.get(this, "name_" + Math.max(0, element()));
		}

		@Override
		public String desc() {
			DrakeArmor a = armor();
			if (a == null) return Messages.get(this, "desc_none");
			String d = a.effectDesc();
			if (a instanceof Mountain && shieldCooldown > 0){
				d += "\n\n" + Messages.get(this, "shield_cd", dispTurns(shieldCooldown));
			}
			return d;
		}

		private static final String REGEN       = "regen";
		private static final String LAST_HP     = "last_hp";
		private static final String HEAL_BONUS  = "heal_bonus";
		private static final String SHIELD_CD   = "shield_cd";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(REGEN, regen);
			bundle.put(LAST_HP, lastHP);
			bundle.put(HEAL_BONUS, healBonus);
			bundle.put(SHIELD_CD, shieldCooldown);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			regen = bundle.getFloat(REGEN);
			lastHP = bundle.contains(LAST_HP) ? bundle.getInt(LAST_HP) : -1;
			healBonus = bundle.getFloat(HEAL_BONUS);
			shieldCooldown = bundle.getFloat(SHIELD_CD);
		}
	}
}
