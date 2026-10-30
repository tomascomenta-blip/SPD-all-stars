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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.hunt;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.big.Colossus;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

//what eating the parts of the big ones did to a hero, for good. each part gives its beast's gift;
//now and then the body takes it badly for a while (a roll that mostly comes out well)
public class Mutations extends Buff {

	//one per part, as in MutationPart. the ash plains
	public static final int EMBER_HEART = 0, NAPE = 1, CHIMNEY = 2, GILL = 3, SPIDER_EGG = 4,
			FIRE_GLAND = 5, OBSIDIAN = 6, HORN = 7, BEAK = 8, THROAT = 9;
	//the flesh forest
	public static final int SEWN_HEADS = 10, FLESH_COMB = 11, HOLLOW_RIB = 12, ANTLER_EYE = 13,
			TOOTHED_FLOWER = 14, AORTA_KNOT = 15;
	//the devoured city
	public static final int GAS_CROP = 16, BRONZE_PLATE = 17, LIVING_AXLE = 18, SEWN_LIP = 19,
			CRUCIBLE = 20, FURNACE_BELL = 21, SUNKEN_CROWN = 22;
	//the frozen moor
	public static final int FROZEN_MUSCLE = 23, SICKLE_CLAW = 24, ICE_ANTLER = 25, WING_MEMBRANE = 26,
			GLACIER_HEART = 27, SHEPHERD_FROST = 28;
	//the graveyard of colossi
	public static final int HORNED_SKULL = 29, SPADE_CLAW = 30, BARE_SPINE = 31, MIST_TEAR = 32,
			RIB_SOUL = 33;
	//the isles, and the blind worm under everything
	public static final int INK_SAC = 34, LAMP_GLASS = 35, YOG_PLATE = 36, ISLAND_HIDE = 37,
			TRUE_EYE = 38, LAMPREY_MOUTH = 39;
	public static final int COUNT = 40;

	//one bit per part (a long: they don't fit in an int any more)
	public long mask = 0;

	//the bell and the soul need a while before they answer again; the plate lets half the dark in
	private float bellAt = -999, soulAt = -999;
	private boolean taintSkip = false;
	private int beat = 0;

	{
		type = buffType.POSITIVE;
		revivePersists = true;
	}

	public static boolean has( Char ch, int kind ){
		Mutations m = ch.buff(Mutations.class);
		return m != null && (m.mask & (1L << kind)) != 0;
	}

	public static int count( Char ch ){
		Mutations m = ch.buff(Mutations.class);
		return m == null ? 0 : Long.bitCount(m.mask);
	}

	//a hero ate a part. same point of the game on every device (it's an action of theirs)
	public static void mutate( Hero h, int kind ){
		Mutations m = Buff.affect(h, Mutations.class);
		boolean mine = h == Dungeon.local();
		if ((m.mask & (1L << kind)) != 0){
			if (mine) GLog.i(Messages.get(Mutations.class, "again"));
			return;
		}
		m.mask |= 1L << kind;
		m.refresh();
		//it used to sit idle: it beats now
		m.timeToNow();
		if (kind == NAPE) h.STR++;
		if (kind == SUNKEN_CROWN) h.STR += 2;
		if (kind == HORN || kind == AORTA_KNOT) h.updateHT(true);
		CoopLog.line("hunt: hero " + Party.indexOf(h) + " mutates " + kind);
		if (mine) GLog.p(Messages.get(Mutations.class, "gift_" + kind));
		//most of the time that's all
		if (Random.Int(4) != 0) return;
		if (kind >= BiomeParts.FIRST){
			BiomeParts.sicken(h, kind);
			CoopLog.line("hunt: hero " + Party.indexOf(h) + " takes part " + kind + " badly");
			if (mine) GLog.w(Messages.get(Mutations.class, "sick_" + kind));
			return;
		}
		switch (Random.Int(5)){
			case 0: Buff.prolong(h, Weakness.class, 40f); break;
			case 1: Buff.prolong(h, Vertigo.class, 12f); break;
			case 2: Buff.prolong(h, Blindness.class, 6f); break;
			case 3: Buff.affect(h, Hunger.class).affectHunger(-150); break;
			default: Continent.taint(h, 8); break;
		}
		if (mine) GLog.w(Messages.get(Mutations.class, "sick"));
	}

	private boolean got( int kind ){
		return (mask & (1L << kind)) != 0;
	}

	//the immunities live in the buff, so they come back with it after a load
	private void refresh(){
		immunities.clear();
		resistances.clear();
		if (got(EMBER_HEART) || got(FURNACE_BELL)) immunities.add(Burning.class);
		if (got(SEWN_HEADS)){
			immunities.add(Vertigo.class);
			immunities.add(Terror.class);
		}
		if (got(GAS_CROP)){
			immunities.add(Poison.class);
			immunities.add(ToxicGas.class);
		}
		if (got(BRONZE_PLATE)) immunities.add(Bleeding.class);
		if (got(LIVING_AXLE)){
			immunities.add(Cripple.class);
			immunities.add(Roots.class);
			immunities.add(Slow.class);
		}
		if (got(CRUCIBLE)) resistances.add(Burning.class);
		if (got(FROZEN_MUSCLE) || got(SHEPHERD_FROST)){
			immunities.add(Chill.class);
			immunities.add(Frost.class);
		}
		if (got(GLACIER_HEART)) immunities.add(Frost.class);
		if (got(INK_SAC)) immunities.add(Blindness.class);
		if (got(TRUE_EYE)){
			immunities.add(Blindness.class);
			immunities.add(Vertigo.class);
			immunities.add(Terror.class);
		}
	}

	//the heart of the forest keeps beating in whoever ate its knot
	@Override
	public boolean act() {
		if (got(AORTA_KNOT) && target.isAlive() && ++beat >= 3){
			beat = 0;
			Hunger hunger = target.buff(Hunger.class);
			if (target.HP < target.HT && (hunger == null || !hunger.isStarving())) target.HP++;
		}
		spend(TICK);
		return true;
	}

	// *** hooks ***

	//Hero.attackProc
	public static int attackProc( Hero h, Char enemy, int damage ){
		Mutations m = h.buff(Mutations.class);
		if (m == null || enemy == null || !enemy.isAlive()) return damage;
		if (has(h, FIRE_GLAND)) damage = Math.round(damage * 1.15f);
		if (has(h, HORNED_SKULL)) damage = Math.round(damage * 1.1f);
		if (has(h, CRUCIBLE)) damage += Random.NormalIntRange(2, 6);
		if (has(h, EMBER_HEART) && Random.Int(7) == 0 && !enemy.isImmune(Burning.class)){
			Buff.affect(enemy, Burning.class).reignite(enemy);
		}
		if (has(h, SPIDER_EGG) && Random.Int(5) == 0) Buff.prolong(enemy, Roots.class, 2f);
		if (has(h, TOOTHED_FLOWER) && Random.Int(3) == 0) Buff.prolong(enemy, Weakness.class, 6f);
		if (has(h, SICKLE_CLAW) && Random.Int(4) == 0) Buff.affect(enemy, Bleeding.class).set(3 + damage / 4f);
		if (has(h, ICE_ANTLER) && Random.Int(4) == 0) Buff.prolong(enemy, Chill.class, 3f);
		if (has(h, LAMP_GLASS) && Random.Int(5) == 0) Buff.prolong(enemy, Blindness.class, 3f);
		//what the blow takes out of it goes into you
		if (has(h, LAMPREY_MOUTH) && damage > 0) heal(h, Math.max(1, Math.min(damage, enemy.HP) / 8));
		if (has(h, MIST_TEAR) && damage >= enemy.HP + enemy.shielding()) heal(h, Math.max(1, Math.round(h.HT * 0.06f)));
		return damage;
	}

	private static void heal( Hero h, int amount ){
		amount = Math.min(amount, h.HT - h.HP);
		if (amount <= 0) return;
		h.HP += amount;
		if (h.sprite != null) h.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(amount), FloatingText.HEALING);
	}

	//Hero.defenseProc: obsidian sends a quarter of a blow back to who gave it; the wasps, the pale
	//hands and the frost answer whoever hits from up close
	public static void defenseProc( Hero h, Char enemy, int damage ){
		if (enemy == null || !enemy.isAlive() || h.buff(Mutations.class) == null) return;
		boolean close = enemy instanceof Colossus ? ((Colossus) enemy).reach(h.pos) <= 1
				: Dungeon.level.distance(h.pos, enemy.pos) <= 1;
		if (!close) return;
		if (damage > 0 && has(h, OBSIDIAN)) enemy.damage(Math.max(1, damage / 4), h);
		if (!enemy.isAlive()) return;
		if (has(h, FLESH_COMB) && Random.Int(3) == 0) Buff.affect(enemy, Poison.class).set(5);
		if (has(h, HOLLOW_RIB) && Random.Int(4) == 0) Buff.prolong(enemy, Roots.class, 3f);
		if (has(h, SHEPHERD_FROST)) Buff.prolong(enemy, Chill.class, 3f);
	}

	//Hero.drRoll
	public static int armor( Hero h ){
		return (has(h, CHIMNEY) ? Random.NormalIntRange(2, 5) : 0)
				+ (has(h, BRONZE_PLATE) ? Random.NormalIntRange(1, 4) : 0);
	}

	//Hero.updateHT
	public static int htBonus( Hero h, int ht ){
		float k = (has(h, HORN) ? 0.15f : 0) + (has(h, AORTA_KNOT) ? 0.15f : 0);
		return Math.round(ht * k);
	}

	//Continent.vision
	public static int sight( Hero h ){
		return (has(h, GILL) ? 1 : 0) + (has(h, ANTLER_EYE) ? 2 : 0)
				+ (has(h, SHEPHERD_FROST) ? 1 : 0) + (has(h, TRUE_EYE) ? 2 : 0);
	}

	//Hero.defenseSkill
	public static float evasion( Hero h ){
		return has(h, WING_MEMBRANE) ? 1.2f : 1f;
	}

	//Char.stealth
	public static float stealth( Char ch ){
		if (!(ch instanceof Hero)) return 0;
		return (has(ch, SEWN_LIP) ? 2 : 0) + (has(ch, ISLAND_HIDE) ? 3 : 0);
	}

	//Hero.damage, before it lands: the glacier takes some of every blow, the furnace bell rings
	//when it would leave you low, and the soul of the last one standing won't let you fall
	public static float guard( Hero h, float damage, Object src ){
		Mutations m = h.buff(Mutations.class);
		if (m == null || damage <= 0) return damage;
		if (m.got(GLACIER_HEART)) damage *= 0.85f;
		float now = Actor.now();
		if (m.got(FURNACE_BELL) && h.HP - damage < h.HT / 3f && h.HP - damage > 0 && now - m.bellAt >= 50){
			m.bellAt = now;
			ring(h);
		}
		if (m.got(RIB_SOUL) && damage >= h.HP + h.shielding() && h.HP > 1 && now - m.soulAt >= 100){
			m.soulAt = now;
			damage = h.HP - 1 + h.shielding();
			CoopLog.line("hunt: the soul keeps hero " + Party.indexOf(h) + " standing");
			if (h.sprite != null) h.sprite.emitter().burst(Speck.factory(Speck.LIGHT), 6);
			if (h == Dungeon.local()) GLog.p(Messages.get(Mutations.class, "soul"));
		}
		return damage;
	}

	//every foe close by runs from the ringing
	private static void ring( Hero h ){
		for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])){
			if (mob.alignment != Char.Alignment.ENEMY || !mob.isAlive() || mob.isImmune(Terror.class)) continue;
			int d = mob instanceof Colossus ? ((Colossus) mob).reach(h.pos) : Dungeon.level.distance(mob.pos, h.pos);
			if (d <= 3) Buff.affect(mob, Terror.class, 6f).object = h.id();
		}
		CoopLog.line("hunt: the bell rings for hero " + Party.indexOf(h));
		if (h.sprite != null) h.sprite.emitter().burst(Speck.factory(Speck.NOTE), 8);
		if (h == Dungeon.local()) GLog.w(Messages.get(Mutations.class, "bell"));
	}

	//Continent.taint: the yog plate lets in half the dark (every other point of it)
	public static int taint( Char ch, int amount ){
		Mutations m = ch.buff(Mutations.class);
		if (m == null || !m.got(YOG_PLATE)) return amount;
		int kept = 0;
		for (int i = 0; i < amount; i++){
			if (!m.taintSkip) kept++;
			m.taintSkip = !m.taintSkip;
		}
		return kept;
	}

	//Remains: the spade claw digs one more bone out of every big one that falls near you
	public static int extraBones( Colossus c ){
		int n = 0;
		for (Hero h : Party.living()){
			if (has(h, SPADE_CLAW) && c.reach(h.pos) <= 8) n++;
		}
		if (n > 0) CoopLog.line("hunt: the spade claw digs one more bone out of " + c.getClass().getSimpleName());
		return Math.min(n, 1);
	}

	//ColossusMeat: true when the meat feeds the bare spine instead of the dark
	public static boolean ateMeat( Hero h ){
		if (!has(h, BARE_SPINE)) return false;
		heal(h, Math.max(1, Math.round(h.HT * 0.1f)));
		return true;
	}

	//knocking down the big ones
	public static float staggerFactor( Char ch ){
		return ch instanceof Hero && has(ch, BEAK) ? 1.5f : 1f;
	}

	//lava doesn't burn through a salamander's throat
	public static boolean lavaproof( Char ch ){
		return has(ch, THROAT);
	}

	@Override
	public int icon() {
		return BuffIndicator.NONE;
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("mask", mask);
		bundle.put("bell", bellAt);
		bundle.put("soul", soulAt);
		bundle.put("taintskip", taintSkip);
		bundle.put("beat", beat);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		//saved as an int before there were more than 32 parts: reads the same
		mask = bundle.getLong("mask");
		bellAt = bundle.contains("bell") ? bundle.getFloat("bell") : -999;
		soulAt = bundle.contains("soul") ? bundle.getFloat("soul") : -999;
		taintSkip = bundle.getBoolean("taintskip");
		beat = bundle.getInt("beat");
		refresh();
	}
}
