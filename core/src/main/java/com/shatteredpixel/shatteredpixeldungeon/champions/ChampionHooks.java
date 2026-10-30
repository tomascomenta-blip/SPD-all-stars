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

package com.shatteredpixel.shatteredpixeldungeon.champions;

import com.shatteredpixel.shatteredpixeldungeon.coop.CoopRules;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LostInventory;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.champions.brawler.Facebreaker;
import com.shatteredpixel.shatteredpixeldungeon.champions.brawler.Haymaker;
import com.shatteredpixel.shatteredpixeldungeon.champions.brawler.KnuckleDown;
import com.shatteredpixel.shatteredpixeldungeon.champions.brawler.Ovation;
import com.shatteredpixel.shatteredpixeldungeon.champions.brawler.PitGrit;
import com.shatteredpixel.shatteredpixeldungeon.champions.brawler.ShowStopper;
import com.shatteredpixel.shatteredpixeldungeon.champions.executioner.CripplingStrike;
import com.shatteredpixel.shatteredpixeldungeon.champions.executioner.Hemorrhage;
import com.shatteredpixel.shatteredpixeldungeon.champions.jailer.Damnation;
import com.shatteredpixel.shatteredpixeldungeon.champions.pyromancer.Pyromania;
import com.shatteredpixel.shatteredpixeldungeon.champions.pyromancer.MoltenShield;
import com.shatteredpixel.shatteredpixeldungeon.champions.scout.Camouflage;
import com.shatteredpixel.shatteredpixeldungeon.champions.scout.NoxiousTrap;
import com.shatteredpixel.shatteredpixeldungeon.champions.scout.ToxicShot;
import com.shatteredpixel.shatteredpixeldungeon.champions.gravedigger.GravediggerHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.guardian.GuardianHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.bard.BardHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.botanist.BotanistHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.forger.ForgerHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.ironlord.IronlordHooks;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.automaton.AutomatonHooks;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.beastmaster.BeastmasterHooks;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.gunslinger.GunslingerHooks;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.spiritist.SpiritistHooks;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//the big switchboard: the rest of the game calls in here for anything champion-related,
//plus some helpers the abilities share.
//IMPORTANT: only ever use the hero you get passed, never "the hero of this device".
//coop desyncs otherwise (learned that one the hard way)
public class ChampionHooks {

	public static boolean isChampion( HeroClass cls ){
		return cls == HeroClass.EXECUTIONER || cls == HeroClass.SCOUT || cls == HeroClass.PYROMANCER
				|| cls == HeroClass.JAILER || cls == HeroClass.BRAWLER
				|| cls == HeroClass.GRAVEDIGGER || cls == HeroClass.GUARDIAN || cls == HeroClass.BARD
				|| cls == HeroClass.BOTANIST
				|| cls == HeroClass.FORGER
				|| cls == HeroClass.IRONLORD
				|| cls == HeroClass.AUTOMATON || cls == HeroClass.BEASTMASTER //not champions but same plumbing
				|| cls == HeroClass.SPIRITIST || cls == HeroClass.GUNSLINGER;
	}

	public static int indexOf( Hero hero ){
		if (Party.active()){
			int i = Party.indexOf(hero);
			return Math.max(0, i);
		}
		return 0;
	}

	public static Hero heroByIndex( int index ){
		if (Party.active()){
			return index >= 0 && index < Party.size() ? Party.get(index) : null;
		}
		return Dungeon.hero;
	}

	//who's holding it (null = nobody)
	public static Hero ownerOf( Item item ){
		if (Party.active()){
			for (Hero h : Party.heroes()){
				if (h.belongings.contains(item)) return h;
			}
			return null;
		}
		if (Dungeon.hero != null && Dungeon.hero.belongings.contains(item)) return Dungeon.hero;
		return null;
	}

	//each champion has an always-on buff. re-added after revive too, bc revive wipes buffs
	public static void ensurePassives( Hero hero ){
		if (hero.heroClass == null) return;
		switch (hero.heroClass){
			case SCOUT:
				Buff.affect(hero, Camouflage.GuerrillaTracker.class);
				Buff.affect(hero, NoxiousTrap.Charges.class);
				break;
			case PYROMANCER:
				Buff.affect(hero, Pyromania.class);
				break;
			case JAILER:
				Buff.affect(hero, Damnation.class);
				break;
			case BRAWLER:
				Buff.affect(hero, PitGrit.class);
				break;
			case GRAVEDIGGER:
				GravediggerHooks.ensurePassives(hero);
				break;
			case GUARDIAN:
				GuardianHooks.ensurePassives(hero);
				break;
			case BARD:
				BardHooks.ensurePassives(hero);
				break;
			//botanico
			case BOTANIST:
				BotanistHooks.ensurePassives(hero);
				break;
			case FORGER:
				ForgerHooks.ensurePassives(hero);
				break;
			case IRONLORD:
				IronlordHooks.ensurePassives(hero);
				break;
			//the SK classes
			case AUTOMATON:
				AutomatonHooks.ensurePassives(hero);
				break;
			case BEASTMASTER:
				BeastmasterHooks.ensurePassives(hero);
				break;
			case SPIRITIST:
				SpiritistHooks.ensurePassives(hero);
				break;
			default:
				break;
		}
	}

	public static void onLevelUp( Hero hero ){
		ChampionKit kit = hero.belongings.getItem(ChampionKit.class);
		if (kit == null) return;
		for (ChampionAbility a : kit.abilities()){
			if (a.unlockLevel() == hero.lvl && a.unlockLevel() > 1){
				GLog.p(Messages.get(ChampionHooks.class, "new_ability", a.slot(), Messages.titleCase(a.name())));
			}
		}
		Item.updateQuickslot();
	}

	/* combat */
	//hero lands a hit (melee or thrown), return the new dmg
	public static int onAttackProc( Hero hero, Char enemy, int damage ){
		if (hero.heroClass == null) return damage;
		boolean melee = !(hero.belongings.attackingWeapon() instanceof MissileWeapon);

		switch (hero.heroClass){
			case EXECUTIONER:
				if (melee){
					damage = CripplingStrike.proc(hero, enemy, damage);
					Hemorrhage.ExecutionerMight might = hero.buff(Hemorrhage.ExecutionerMight.class);
					if (might != null) damage = Math.round(damage * 1.15f);
					if (enemy.isAlive()) Hemorrhage.apply(hero, enemy, 1);
					if (might != null && hero.subClass == HeroSubClass.CONQUEROR){
						heal(hero, Math.round(damage * 0.2f));
					}
					if (hero.subClass == HeroSubClass.HEADSMAN && !Char.hasProp(enemy, Char.Property.BOSS)
							&& !Char.hasProp(enemy, Char.Property.MINIBOSS)){
						float threshold = 0.10f + 0.03f*hero.pointsInTalent(Talent.HS_SENTENCE);
						if (enemy.HP - damage <= enemy.HT * threshold){
							damage = Math.max(damage, enemy.HP + enemy.shielding());
							if (enemy.sprite != null) enemy.sprite.showStatus(CharSprite.NEGATIVE, Messages.get(ChampionHooks.class, "beheaded"));
						}
					}
				}
				break;
			case SCOUT:
				Camouflage.GuerrillaTracker g = hero.buff(Camouflage.GuerrillaTracker.class);
				if (g != null) g.still = 0;
				if (hero.hasTalent(Talent.SCO_AMBUSH)){
					Camouflage.Ambush amb = hero.buff(Camouflage.Ambush.class);
					if (amb != null || hero.buff(Camouflage.class) != null){
						damage = Math.round(damage * (1f + 0.2f*hero.pointsInTalent(Talent.SCO_AMBUSH)));
						if (amb != null) amb.detach();
					}
				}
				if (hero.subClass == HeroSubClass.SKIRMISHER && enemy.buff(Poison.class) != null){
					damage = Math.round(damage * (1f + 0.1f*hero.pointsInTalent(Talent.SK_CORROSIVE)));
				}
				ToxicShot.proc(hero, enemy);
				break;
			case BRAWLER:
				if (melee){
					damage = KnuckleDown.proc(hero, enemy, damage);
					PitGrit grit = PitGrit.of(hero);
					if (grit != null) damage = grit.onPunch(hero, enemy, damage);
					damage = Ovation.proc(hero, damage);
				}
				break;
			case GRAVEDIGGER:
				damage = GravediggerHooks.onAttackProc(hero, enemy, damage, melee);
				break;
			case GUARDIAN:
				damage = GuardianHooks.onAttackProc(hero, enemy, damage, melee);
				break;
			case BARD:
				damage = BardHooks.onAttackProc(hero, enemy, damage, melee);
				break;
			//botanico
			case BOTANIST:
				damage = BotanistHooks.onAttackProc(hero, enemy, damage, melee);
				break;
			case FORGER:
				damage = ForgerHooks.onAttackProc(hero, enemy, damage, melee);
				break;
			case IRONLORD:
				damage = IronlordHooks.onAttackProc(hero, enemy, damage, melee);
				break;
			case BEASTMASTER:
				damage = BeastmasterHooks.onAttackProc(hero, enemy, damage, melee);
				break;
			default:
				break;
		}
		return damage;
	}

	public static void onHeroDamaged( Hero hero, int effectiveDamage, Object src ){
		if (effectiveDamage <= 0 || src instanceof Hunger) return;
		if (hero.heroClass == HeroClass.BRAWLER){
			PitGrit grit = PitGrit.of(hero);
			if (grit != null) grit.addGrit(effectiveDamage);
		}
		if (hero.heroClass == HeroClass.GRAVEDIGGER) GravediggerHooks.onHeroDamaged(hero, effectiveDamage, src);
		if (hero.heroClass == HeroClass.GUARDIAN) GuardianHooks.onHeroDamaged(hero, effectiveDamage, src);
		if (hero.heroClass == HeroClass.BARD) BardHooks.onHeroDamaged(hero, effectiveDamage, src);
		if (hero.heroClass == HeroClass.BOTANIST) BotanistHooks.onHeroDamaged(hero, effectiveDamage, src);
		if (hero.heroClass == HeroClass.IRONLORD) IronlordHooks.onHeroDamaged(hero, effectiveDamage, src);
		if (hero.heroClass == HeroClass.AUTOMATON) AutomatonHooks.onHeroDamaged(hero, effectiveDamage, src);
	}

	//anyone got hit in melee
	public static void onDefenseProc( Char defender, Char attacker ){
		MoltenShield.Burnback bb = defender.buff(MoltenShield.Burnback.class);
		if (bb != null && attacker != null) bb.onAttacked(attacker);
	}

	//bonus armor
	public static int drBonus( Hero hero ){
		int dr = 0;
		if (hero.heroClass == HeroClass.JAILER){
			dr += Damnation.drBonus(hero);
		}
		if (hero.subClass == HeroSubClass.CONQUEROR && hero.buff(Hemorrhage.ExecutionerMight.class) != null
				&& hero.hasTalent(Talent.CQ_WARLORD)){
			dr += Random.NormalIntRange(0, 2*hero.pointsInTalent(Talent.CQ_WARLORD));
		}
		if (hero.subClass == HeroSubClass.PITBOSS && hero.hasTalent(Talent.PB_IRON_JAW)){
			PitGrit grit = PitGrit.of(hero);
			if (grit != null && grit.highGrit()){
				dr += Random.NormalIntRange(0, 2*hero.pointsInTalent(Talent.PB_IRON_JAW));
			}
		}
		if (hero.heroClass == HeroClass.GRAVEDIGGER) dr += GravediggerHooks.drBonus(hero);
		if (hero.heroClass == HeroClass.GUARDIAN) dr += GuardianHooks.drBonus(hero);
		if (hero.heroClass == HeroClass.BARD) dr += BardHooks.drBonus(hero);
		if (hero.heroClass == HeroClass.BOTANIST) dr += BotanistHooks.drBonus(hero);
		if (hero.heroClass == HeroClass.FORGER) dr += ForgerHooks.drBonus(hero);
		if (hero.heroClass == HeroClass.IRONLORD) dr += IronlordHooks.drBonus(hero);
		if (hero.heroClass == HeroClass.AUTOMATON) dr += AutomatonHooks.drBonus(hero);
		if (hero.heroClass == HeroClass.BEASTMASTER) dr += BeastmasterHooks.drBonus(hero);
		if (hero.heroClass == HeroClass.GUNSLINGER) dr += GunslingerHooks.drBonus(hero);
		return dr;
	}

	//about to die, not dead yet. ojo
	public static void onMobDeath( Mob mob, Object cause ){
		Damnation.onEnemyDeath(mob);
		GravediggerHooks.onMobDeath(mob, cause);
		GuardianHooks.onMobDeath(mob, cause);
		BardHooks.onMobDeath(mob, cause);
		BotanistHooks.onMobDeath(mob, cause);
		IronlordHooks.onMobDeath(mob, cause);
		BeastmasterHooks.onMobDeath(mob, cause);
		SpiritistHooks.onMobDeath(mob, cause);
		GunslingerHooks.onMobDeath(mob, cause);

		boolean bled = mob.buff(Hemorrhage.class) != null;
		boolean burned = mob.buff(Burning.class) != null;
		for (Hero h : Party.living()){
			if (bled && h.hasTalent(Talent.EXE_BLOODLUST)){
				reduceCooldowns(h, 1 + h.pointsInTalent(Talent.EXE_BLOODLUST));
			}
			if (burned && h.subClass == HeroSubClass.INFERNALIST && h.hasTalent(Talent.IN_HEARTFIRE)
					&& Dungeon.level.heroFOV[mob.pos]){
				heal(h, 2*h.pointsInTalent(Talent.IN_HEARTFIRE));
			}
		}

		Hero ctx = Dungeon.hero;
		if (ctx != null && ctx.subClass == HeroSubClass.SHOWMAN
				&& (cause == Haymaker.INSTANCE || cause == Facebreaker.INSTANCE || cause == ShowStopper.INSTANCE)){
			heal(ctx, Math.round(ctx.HT * 0.1f));
		}
	}

	//one step
	public static void onHeroMove( Hero hero, boolean travelling ){
		if (hero.heroClass == HeroClass.SCOUT && travelling){
			Camouflage.onMove(hero);
		}
		if (hero.heroClass == HeroClass.JAILER){
			Damnation.gatherAt(hero);
		}
		if (hero.heroClass == HeroClass.GRAVEDIGGER) GravediggerHooks.onHeroMove(hero, travelling);
		if (hero.heroClass == HeroClass.GUARDIAN) GuardianHooks.onHeroMove(hero, travelling);
		if (hero.heroClass == HeroClass.BARD) BardHooks.onHeroMove(hero, travelling);
		if (hero.heroClass == HeroClass.BOTANIST) BotanistHooks.onHeroMove(hero, travelling);
	}

	// *** talents tied to items ***

	public static void onFoodEaten( Hero hero ){
		if (hero.heroClass == HeroClass.GRAVEDIGGER) GravediggerHooks.onFoodEaten(hero);
		if (hero.heroClass == HeroClass.GUARDIAN) GuardianHooks.onFoodEaten(hero);
		if (hero.heroClass == HeroClass.BARD) BardHooks.onFoodEaten(hero);
		if (hero.heroClass == HeroClass.BOTANIST) BotanistHooks.onFoodEaten(hero);
		if (hero.heroClass == HeroClass.FORGER) ForgerHooks.onFoodEaten(hero);
		if (hero.heroClass == HeroClass.IRONLORD) IronlordHooks.onFoodEaten(hero);
		if (hero.heroClass == HeroClass.AUTOMATON) AutomatonHooks.onFoodEaten(hero);
		if (hero.heroClass == HeroClass.BEASTMASTER) BeastmasterHooks.onFoodEaten(hero);
		if (hero.heroClass == HeroClass.SPIRITIST) SpiritistHooks.onFoodEaten(hero);
		if (hero.heroClass == HeroClass.GUNSLINGER) GunslingerHooks.onFoodEaten(hero);
		if (hero.hasTalent(Talent.EXE_MEAL)){
			reduceCooldowns(hero, 1 + 2*hero.pointsInTalent(Talent.EXE_MEAL));
		}
		if (hero.hasTalent(Talent.SCO_MEAL)){
			Camouflage.apply(hero);
			if (hero.pointsInTalent(Talent.SCO_MEAL) >= 2) Buff.prolong(hero, Haste.class, 2f);
		}
		if (hero.hasTalent(Talent.PYR_MEAL)){
			Pyromania p = Pyromania.of(hero);
			if (p != null) p.gain(hero.pointsInTalent(Talent.PYR_MEAL));
		}
		if (hero.hasTalent(Talent.JAI_MEAL)){
			int amount = 1 + 2*hero.pointsInTalent(Talent.JAI_MEAL);
			Barrier b = Buff.affect(hero, Barrier.class);
			if (b.shielding() <= amount){
				b.setShield(amount);
				b.delay(Math.max(10 - b.cooldown(), 0));
			}
		}
		if (hero.hasTalent(Talent.BRA_MEAL)){
			PitGrit grit = PitGrit.of(hero);
			if (grit != null){
				grit.grit = Math.min(PitGrit.maxGrit(hero), grit.grit + PitGrit.maxGrit(hero) * 0.25f * hero.pointsInTalent(Talent.BRA_MEAL));
			}
		}
	}

	public static void onPotionUsed( Hero hero, float factor ){
		if (hero.heroClass == HeroClass.GRAVEDIGGER) GravediggerHooks.onPotionUsed(hero, factor);
		if (hero.heroClass == HeroClass.GUARDIAN) GuardianHooks.onPotionUsed(hero, factor);
		if (hero.heroClass == HeroClass.BARD) BardHooks.onPotionUsed(hero, factor);
		if (hero.heroClass == HeroClass.BOTANIST) BotanistHooks.onPotionUsed(hero, factor);
		if (hero.heroClass == HeroClass.FORGER) ForgerHooks.onPotionUsed(hero, factor);
		if (hero.heroClass == HeroClass.IRONLORD) IronlordHooks.onPotionUsed(hero, factor);
		if (hero.hasTalent(Talent.EXE_IRON_RESOLVE)){
			int shield = Math.round(factor * hero.HT * (0.02f + 0.03f*hero.pointsInTalent(Talent.EXE_IRON_RESOLVE)));
			if (shield > 0){
				Buff.affect(hero, Barrier.class).setShield(shield);
				hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shield), FloatingText.SHIELDING);
			}
		}
		if (hero.hasTalent(Talent.SCO_LIQUID_CAMO)){
			Camouflage.apply(hero);
			Buff.prolong(hero, Haste.class, factor * (1 + hero.pointsInTalent(Talent.SCO_LIQUID_CAMO)));
		}
		if (hero.hasTalent(Talent.PYR_FIRE_DRINKER)){
			reduceCooldowns(hero, factor * (1 + 2*hero.pointsInTalent(Talent.PYR_FIRE_DRINKER)));
		}
		if (hero.hasTalent(Talent.JAI_LIQUID_SOULS)){
			if (hero.pointsInTalent(Talent.JAI_LIQUID_SOULS) >= 2 || Random.Int(2) == 0){
				Damnation.gain(hero, 1);
			}
		}
		if (hero.hasTalent(Talent.BRA_LIQUID_GRIT)){
			PitGrit grit = PitGrit.of(hero);
			if (grit != null){
				grit.grit = Math.min(PitGrit.maxGrit(hero),
						grit.grit + PitGrit.maxGrit(hero) * factor * (0.1f + 0.1f*hero.pointsInTalent(Talent.BRA_LIQUID_GRIT)));
			}
		}
	}

	public static float itemIDSpeedFactor( Hero hero, Item item ){
		float factor = 1f;
		if (item instanceof MeleeWeapon){
			factor *= 1f + 0.75f*hero.pointsInTalent(Talent.EXE_INTUITION);
			factor *= 1f + 0.5f*hero.pointsInTalent(Talent.BRA_INTUITION);
		}
		if (item instanceof Armor){
			factor *= 1f + 0.75f*hero.pointsInTalent(Talent.JAI_INTUITION);
			factor *= 1f + 0.5f*hero.pointsInTalent(Talent.BRA_INTUITION);
		}
		if (item instanceof MissileWeapon || item instanceof Ring){
			factor *= 1f + hero.pointsInTalent(Talent.SCO_INTUITION);
		}
		if (item instanceof Wand){
			factor *= 1f + hero.pointsInTalent(Talent.PYR_INTUITION);
		}
		factor *= GravediggerHooks.itemIDSpeedFactor(hero, item);
		factor *= GuardianHooks.itemIDSpeedFactor(hero, item);
		factor *= BardHooks.itemIDSpeedFactor(hero, item);
		factor *= BotanistHooks.itemIDSpeedFactor(hero, item);
		factor *= ForgerHooks.itemIDSpeedFactor(hero, item);
		factor *= IronlordHooks.itemIDSpeedFactor(hero, item);
		factor *= AutomatonHooks.itemIDSpeedFactor(hero, item);
		factor *= SpiritistHooks.itemIDSpeedFactor(hero, item);
		factor *= GunslingerHooks.itemIDSpeedFactor(hero, item);
		return factor;
	}

	//recharging scroll refreshes abilities too, felt bad otherwise
	public static void onRecharge( Char ch ){
		if (ch instanceof Hero) reduceCooldowns((Hero) ch, 10);
	}

	//--- helpers ---

	public static void reduceCooldowns( Hero hero, float turns ){
		for (Buff b : hero.buffs()){
			if (b instanceof AbilityCooldown) ((AbilityCooldown) b).reduce(turns);
		}
		Item.updateQuickslot();
	}

	public static void heal( Char ch, int amount ){
		if (amount <= 0 || ch.HP >= ch.HT) return;
		amount = Math.min(amount, ch.HT - ch.HP);
		ch.HP += amount;
		if (ch.sprite != null) ch.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(amount), FloatingText.HEALING);
	}

	public static void cleanse( Char ch ){
		for (Buff b : ch.buffs()){
			if (b.type == Buff.buffType.NEGATIVE && !(b instanceof LostInventory)) b.detach();
		}
	}

	//primer char en la trayectoria
	public static Char boltTarget( Hero hero, Integer cell ){
		if (cell == null || cell == hero.pos) return null;
		Ballistica b = new Ballistica(hero.pos, cell, Ballistica.PROJECTILE);
		Char ch = Actor.findChar(b.collisionPos);
		return ch == hero ? null : ch;
	}

	//where a pulled enemy ends up. -1 = can't pull
	public static int pullDestination( Hero hero, Char enemy ){
		if (Char.hasProp(enemy, Char.Property.IMMOVABLE) || Dungeon.level.adjacent(hero.pos, enemy.pos)) return -1;
		Ballistica chain = new Ballistica(hero.pos, enemy.pos, Ballistica.PROJECTILE);
		if (chain.dist < 2) return -1;
		for (int i : chain.subPath(1, chain.dist)){
			if (i == enemy.pos) break;
			if (!Dungeon.level.solid[i] && Actor.findChar(i) == null
					&& (Dungeon.level.passable[i] || Dungeon.level.avoid[i])
					&& (!Char.hasProp(enemy, Char.Property.LARGE) || Dungeon.level.openSpace[i])){
				return i;
			}
		}
		return -1;
	}

	//instant move + slide anim
	public static void moveChar( Char ch, int to ){
		int from = ch.pos;
		if (from == to) return;
		ch.pos = to;
		if (ch.sprite != null) Actor.add(new Pushing(ch, from, to));
		Dungeon.level.occupyCell(ch);
		Dungeon.observe();
		GameScene.updateFog();
	}

	public static int landingNextTo( Char mover, int target ){
		int best = -1;
		float bestDist = Float.MAX_VALUE;
		for (int n : PathFinder.NEIGHBOURS8){
			int c = target + n;
			if (c < 0 || c >= Dungeon.level.length()) continue;
			if (c == mover.pos){
				return c;
			}
			if (Dungeon.level.solid[c] || !Dungeon.level.passable[c] || Actor.findChar(c) != null) continue;
			if (Char.hasProp(mover, Char.Property.LARGE) && !Dungeon.level.openSpace[c]) continue;
			float d = Dungeon.level.trueDistance(mover.pos, c);
			if (d < bestDist){
				bestDist = d;
				best = c;
			}
		}
		return best;
	}

	//operate anim, VISUAL ONLY. the normal one clears curAction when it ends and can resume the
	//turn whenever the screen feels like it -> other device sees a different game. this broke coop twice, don't touch
	//use it only from abilities that already spent their time
	public static void operateVisual( final Hero hero, int cell ){
		if (hero.sprite == null) return;
		hero.sprite.operate(cell, new Callback() {
			@Override
			public void call() {
				if (hero.sprite != null) hero.sprite.idle();
			}
		});
	}

	//landing after a leap
	public static void placeHero( Hero hero, int cell ){
		hero.move(cell, false);
		Dungeon.level.occupyCell(hero);
		Dungeon.observe();
		GameScene.updateFog();
	}

	//damaging auras and waves of that hero: also the other heroes with friendly fire on
	public static ArrayList<Char> enemiesAround( Char attacker, int pos, int radius ){
		ArrayList<Char> result = new ArrayList<>();
		for (Char ch : Actor.chars()){
			if (CoopRules.hits(attacker, ch) && ch.isAlive() && Dungeon.level.distance(pos, ch.pos) <= radius){
				result.add(ch);
			}
		}
		return result;
	}

	//enemies only (songs, fear, auto-aim...)
	public static ArrayList<Char> enemiesAround( int pos, int radius ){
		ArrayList<Char> result = new ArrayList<>();
		for (Char ch : Actor.chars()){
			if (ch.alignment == Char.Alignment.ENEMY && ch.isAlive() && Dungeon.level.distance(pos, ch.pos) <= radius){
				result.add(ch);
			}
		}
		return result;
	}
}
