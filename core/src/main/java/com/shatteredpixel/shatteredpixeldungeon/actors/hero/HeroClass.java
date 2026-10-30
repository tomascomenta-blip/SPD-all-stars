/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.QuickSlot;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.AscendedForm;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.Challenge;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.ElementalStrike;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.Feint;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.NaturesPower;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpectralBlades;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpiritHawk;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.ElementalBlast;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WarpBeacon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WildMagic;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.DeathMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.ShadowClone;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.SmokeBomb;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Endure;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.HeroicLeap;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Shockwave;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.bard.BardKit;
import com.shatteredpixel.shatteredpixeldungeon.champions.bard.GrandOverture;
import com.shatteredpixel.shatteredpixeldungeon.champions.botanist.Bloom;
import com.shatteredpixel.shatteredpixeldungeon.champions.botanist.BotanistKit;
import com.shatteredpixel.shatteredpixeldungeon.champions.botanist.WoodenStaff;
import com.shatteredpixel.shatteredpixeldungeon.champions.brawler.BrassKnuckles;
import com.shatteredpixel.shatteredpixeldungeon.champions.brawler.BrawlerKit;
import com.shatteredpixel.shatteredpixeldungeon.champions.brawler.Ovation;
import com.shatteredpixel.shatteredpixeldungeon.champions.executioner.BloodHarvest;
import com.shatteredpixel.shatteredpixeldungeon.champions.executioner.ExecutionerAxe;
import com.shatteredpixel.shatteredpixeldungeon.champions.executioner.ExecutionerKit;
import com.shatteredpixel.shatteredpixeldungeon.champions.forger.ForgeEruption;
import com.shatteredpixel.shatteredpixeldungeon.champions.forger.ForgeHammer;
import com.shatteredpixel.shatteredpixeldungeon.champions.forger.ForgerKit;
import com.shatteredpixel.shatteredpixeldungeon.champions.gravedigger.GravediggerKit;
import com.shatteredpixel.shatteredpixeldungeon.champions.gravedigger.Headstone;
import com.shatteredpixel.shatteredpixeldungeon.champions.gravedigger.Shovel;
import com.shatteredpixel.shatteredpixeldungeon.champions.guardian.GuardianKit;
import com.shatteredpixel.shatteredpixeldungeon.champions.guardian.SandStaff;
import com.shatteredpixel.shatteredpixeldungeon.champions.guardian.SandTomb;
import com.shatteredpixel.shatteredpixeldungeon.champions.ironlord.IronEclipse;
import com.shatteredpixel.shatteredpixeldungeon.champions.ironlord.IronlordKit;
import com.shatteredpixel.shatteredpixeldungeon.champions.ironlord.IronlordMace;
import com.shatteredpixel.shatteredpixeldungeon.champions.jailer.ChainSickle;
import com.shatteredpixel.shatteredpixeldungeon.champions.jailer.JailerKit;
import com.shatteredpixel.shatteredpixeldungeon.champions.jailer.SpectralShackles;
import com.shatteredpixel.shatteredpixeldungeon.champions.pyromancer.PyromancerKit;
import com.shatteredpixel.shatteredpixeldungeon.champions.pyromancer.Supernova;
import com.shatteredpixel.shatteredpixeldungeon.champions.scout.Blowgun;
import com.shatteredpixel.shatteredpixeldungeon.champions.scout.RainOfDarts;
import com.shatteredpixel.shatteredpixeldungeon.champions.scout.ScoutKit;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHaste;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfParalyticGas;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRetribution;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTerror;
import com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Waterskin;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.VelvetPouch;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfInvisibility;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLiquidFlame;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfMindVision;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfPurity;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfLullaby;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMirrorImage;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRage;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRemoveCurse;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Cudgel;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Gloves;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Rapier;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingSpike;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingStone;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.automaton.AutomatonHooks;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.automaton.SentryTurret;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.beastmaster.BeastmasterHooks;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.beastmaster.PackRoar;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.gunslinger.GunslingerHooks;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.gunslinger.LeadRain;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.spiritist.SoulVeil;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.spiritist.SpiritistHooks;
import com.watabou.utils.DeviceCompat;

public enum HeroClass {

	WARRIOR( HeroSubClass.BERSERKER, HeroSubClass.GLADIATOR ),
	MAGE( HeroSubClass.BATTLEMAGE, HeroSubClass.WARLOCK ),
	ROGUE( HeroSubClass.ASSASSIN, HeroSubClass.FREERUNNER ),
	HUNTRESS( HeroSubClass.SNIPER, HeroSubClass.WARDEN ),
	DUELIST( HeroSubClass.CHAMPION, HeroSubClass.MONK ),
	CLERIC( HeroSubClass.PRIEST, HeroSubClass.PALADIN ),

	//the champion classes (ours)
	EXECUTIONER( HeroSubClass.CONQUEROR, HeroSubClass.HEADSMAN ),
	SCOUT( HeroSubClass.TRAPPER, HeroSubClass.SKIRMISHER ),
	PYROMANCER( HeroSubClass.TAMER, HeroSubClass.INFERNALIST ),
	JAILER( HeroSubClass.SOULREAPER, HeroSubClass.LANTERNKEEPER ),
	BRAWLER( HeroSubClass.PITBOSS, HeroSubClass.SHOWMAN ),
	GRAVEDIGGER( HeroSubClass.MISTLORD, HeroSubClass.EXHUMER ),
	GUARDIAN( HeroSubClass.CURATOR, HeroSubClass.COLOSSUS ),
	BARD( HeroSubClass.TROUBADOUR, HeroSubClass.MINSTREL ),
	//botanico
	BOTANIST( HeroSubClass.WARGARDENER, HeroSubClass.DRUID ),

	//ornn
	FORGER( HeroSubClass.MASTERSMITH, HeroSubClass.LIVINGMOUNTAIN ),

	//mordekaiser
	IRONLORD( HeroSubClass.TYRANT, HeroSubClass.REVENANT ),

	//the alt world heroes
	AUTOMATON( HeroSubClass.SWARM_ENGINEER, HeroSubClass.TESLA_COIL ),
	BEASTMASTER( HeroSubClass.PACK_ALPHA, HeroSubClass.FOREST_WARDEN ),
	SPIRITIST( HeroSubClass.SPECTRE_LORD, HeroSubClass.PLAGUE_HERALD ),
	GUNSLINGER( HeroSubClass.QUICKDRAW, HeroSubClass.BOUNTY_HUNTER );

	private HeroSubClass[] subClasses;

	HeroClass( HeroSubClass...subClasses ) {
		this.subClasses = subClasses;
	}

	public void initHero( Hero hero ) {

		hero.heroClass = this;
		Talent.initClassTalents(hero);

		Item i = new ClothArmor().identify();
		if (!Challenges.isItemBlocked(i)) hero.belongings.armor = (ClothArmor)i;

		i = new Food();
		if (!Challenges.isItemBlocked(i)) i.collect();

		new VelvetPouch().collect();
		Dungeon.LimitedDrops.VELVET_POUCH.drop();

		Waterskin waterskin = new Waterskin();
		waterskin.collect();

		new ScrollOfIdentify().identify();

		switch (this) {
			case WARRIOR:
				initWarrior( hero );
				break;

			case MAGE:
				initMage( hero );
				break;

			case ROGUE:
				initRogue( hero );
				break;

			case HUNTRESS:
				initHuntress( hero );
				break;

			case DUELIST:
				initDuelist( hero );
				break;

			case CLERIC:
				initCleric( hero );
				break;

			case EXECUTIONER:
				initExecutioner( hero );
				break;

			case SCOUT:
				initScout( hero );
				break;

			case PYROMANCER:
				initPyromancer( hero );
				break;

			case JAILER:
				initJailer( hero );
				break;

			case BRAWLER:
				initBrawler( hero );
				break;

			case GRAVEDIGGER:
				initGravedigger( hero );
				break;

			case GUARDIAN:
				initGuardian( hero );
				break;

			case BARD:
				initBard( hero );
				break;

			//botanico
			case BOTANIST:
				initBotanist( hero );
				break;
			//ornn
			case FORGER:
				initForger( hero );
				break;
			//mordekaiser
			case IRONLORD:
				initIronlord( hero );
				break;
			//alt worlds
			case AUTOMATON:
				AutomatonHooks.initHero( hero );
				break;
			case BEASTMASTER:
				BeastmasterHooks.initHero( hero );
				break;
			case SPIRITIST:
				SpiritistHooks.initHero( hero );
				break;
			case GUNSLINGER:
				GunslingerHooks.initHero( hero );
				break;
		}

		ChampionHooks.ensurePassives( hero );

		if (SPDSettings.quickslotWaterskin()) {
			for (int s = 0; s < QuickSlot.SIZE; s++) {
				if (Dungeon.quickslot.getItem(s) == null) {
					Dungeon.quickslot.setSlot(s, waterskin);
					break;
				}
			}
		}

	}

	public Badges.Badge masteryBadge() {
		switch (this) {
			case WARRIOR:
				return Badges.Badge.MASTERY_WARRIOR;
			case MAGE:
				return Badges.Badge.MASTERY_MAGE;
			case ROGUE:
				return Badges.Badge.MASTERY_ROGUE;
			case HUNTRESS:
				return Badges.Badge.MASTERY_HUNTRESS;
			case DUELIST:
				return Badges.Badge.MASTERY_DUELIST;
			case CLERIC:
				return Badges.Badge.MASTERY_CLERIC;
		}
		return null;
	}

	private static void initWarrior( Hero hero ) {
		(hero.belongings.weapon = new WornShortsword()).identify();
		ThrowingStone stones = new ThrowingStone();
		stones.identify().collect();

		Dungeon.quickslot.setSlot(0, stones);

		if (hero.belongings.armor != null){
			hero.belongings.armor.affixSeal(new BrokenSeal());
			Catalog.setSeen(BrokenSeal.class); //as it's not added to the inventory
		}

		new PotionOfHealing().identify();
		new ScrollOfRage().identify();
	}

	private static void initMage( Hero hero ) {
		MagesStaff staff;

		staff = new MagesStaff(new WandOfMagicMissile());

		(hero.belongings.weapon = staff).identify();
		hero.belongings.weapon.activate(hero);

		Dungeon.quickslot.setSlot(0, staff);

		new ScrollOfUpgrade().identify();
		new PotionOfLiquidFlame().identify();
	}

	private static void initRogue( Hero hero ) {
		(hero.belongings.weapon = new Dagger()).identify();

		CloakOfShadows cloak = new CloakOfShadows();
		(hero.belongings.artifact = cloak).identify();
		hero.belongings.artifact.activate( hero );

		ThrowingKnife knives = new ThrowingKnife();
		knives.identify().collect();

		Dungeon.quickslot.setSlot(0, cloak);
		Dungeon.quickslot.setSlot(1, knives);

		new ScrollOfMagicMapping().identify();
		new PotionOfInvisibility().identify();
	}

	private static void initHuntress( Hero hero ) {

		(hero.belongings.weapon = new Gloves()).identify();
		SpiritBow bow = new SpiritBow();
		bow.identify().collect();

		Dungeon.quickslot.setSlot(0, bow);

		new PotionOfMindVision().identify();
		new ScrollOfLullaby().identify();
	}

	private static void initDuelist( Hero hero ) {

		(hero.belongings.weapon = new Rapier()).identify();
		hero.belongings.weapon.activate(hero);

		ThrowingSpike spikes = new ThrowingSpike();
		spikes.quantity(2).identify().collect(); //set quantity is 3, but Duelist starts with 2

		Dungeon.quickslot.setSlot(0, hero.belongings.weapon);
		Dungeon.quickslot.setSlot(1, spikes);

		new PotionOfStrength().identify();
		new ScrollOfMirrorImage().identify();
	}

	private static void initCleric( Hero hero ) {

		(hero.belongings.weapon = new Cudgel()).identify();
		hero.belongings.weapon.activate(hero);

		HolyTome tome = new HolyTome();
		(hero.belongings.artifact = tome).identify();
		hero.belongings.artifact.activate( hero );

		Dungeon.quickslot.setSlot(0, tome);

		new PotionOfPurity().identify();
		new ScrollOfRemoveCurse().identify();
	}

	public boolean isChampion(){
		return ChampionHooks.isChampion(this);
	}

	private static void initExecutioner( Hero hero ) {
		(hero.belongings.weapon = new ExecutionerAxe()).identify();
		hero.belongings.weapon.activate(hero);

		ExecutionerKit kit = new ExecutionerKit();
		kit.identify().collect();

		Dungeon.quickslot.setSlot(0, kit);

		new PotionOfHealing().identify();
		new ScrollOfTerror().identify();
	}

	private static void initScout( Hero hero ) {
		(hero.belongings.weapon = new Dagger()).identify();
		hero.belongings.weapon.activate(hero);

		Blowgun blowgun = new Blowgun();
		blowgun.identify().collect();
		ScoutKit kit = new ScoutKit();
		kit.identify().collect();

		Dungeon.quickslot.setSlot(0, kit);
		Dungeon.quickslot.setSlot(1, blowgun);

		new PotionOfToxicGas().identify();
		new ScrollOfTeleportation().identify();
	}

	private static void initPyromancer( Hero hero ) {
		(hero.belongings.weapon = new Gloves()).identify();
		hero.belongings.weapon.activate(hero);

		PyromancerKit kit = new PyromancerKit();
		kit.identify().collect();

		Dungeon.quickslot.setSlot(0, kit);

		new PotionOfLiquidFlame().identify();
		new ScrollOfRecharging().identify();
	}

	private static void initJailer( Hero hero ) {
		(hero.belongings.weapon = new ChainSickle()).identify();
		hero.belongings.weapon.activate(hero);

		JailerKit kit = new JailerKit();
		kit.identify().collect();

		Dungeon.quickslot.setSlot(0, kit);

		new PotionOfParalyticGas().identify();
		new ScrollOfRetribution().identify();
	}

	private static void initBrawler( Hero hero ) {
		(hero.belongings.weapon = new BrassKnuckles()).identify();
		hero.belongings.weapon.activate(hero);

		BrawlerKit kit = new BrawlerKit();
		kit.identify().collect();

		Dungeon.quickslot.setSlot(0, kit);

		new PotionOfHealing().identify();
		new ScrollOfRage().identify();
	}

	private static void initGravedigger( Hero hero ) {
		(hero.belongings.weapon = new Shovel()).identify();
		hero.belongings.weapon.activate(hero);

		GravediggerKit kit = new GravediggerKit();
		kit.identify().collect();

		Dungeon.quickslot.setSlot(0, kit);

		new PotionOfMindVision().identify();
		new ScrollOfTerror().identify();
	}

	private static void initGuardian( Hero hero ) {
		(hero.belongings.weapon = new SandStaff()).identify();
		hero.belongings.weapon.activate(hero);

		GuardianKit kit = new GuardianKit();
		kit.identify().collect();

		Dungeon.quickslot.setSlot(0, kit);

		new PotionOfHaste().identify();
		new ScrollOfRage().identify();
	}

	//mordekaiser
	private static void initIronlord( Hero hero ) {
		(hero.belongings.weapon = new IronlordMace()).identify();
		hero.belongings.weapon.activate(hero);

		IronlordKit kit = new IronlordKit();
		kit.identify().collect();

		Dungeon.quickslot.setSlot(0, kit);

		new PotionOfHealing().identify();
		new ScrollOfTerror().identify();
	}

	private static void initBard( Hero hero ) {
		(hero.belongings.weapon = new Dagger()).identify();
		hero.belongings.weapon.activate(hero);

		BardKit kit = new BardKit();
		kit.identify().collect();

		Dungeon.quickslot.setSlot(0, kit);

		new PotionOfHealing().identify();
		new ScrollOfLullaby().identify();
	}

	//botanico
	private static void initBotanist( Hero hero ) {
		(hero.belongings.weapon = new WoodenStaff()).identify();
		hero.belongings.weapon.activate(hero);

		BotanistKit kit = new BotanistKit();
		kit.identify().collect();

		Dungeon.quickslot.setSlot(0, kit);

		for (int i = 0; i < 3; i++){
			Plant.Seed seed = (Plant.Seed)
					Generator.randomUsingDefaults(Generator.Category.SEED);
			seed.identify().collect();
			if (i == 0) Dungeon.quickslot.setSlot(1, seed);
		}

		new PotionOfHealing().identify();
		new ScrollOfRemoveCurse().identify();
	}

	//ornn
	private static void initForger( Hero hero ) {
		(hero.belongings.weapon = new ForgeHammer()).identify();
		hero.belongings.weapon.activate(hero);

		ForgerKit kit = new ForgerKit();
		kit.identify().collect();

		Dungeon.quickslot.setSlot(0, kit);

		new PotionOfHaste().identify();
		new ScrollOfRage().identify();
	}

	public String title() {
		return Messages.get(HeroClass.class, name());
	}

	public String desc(){
		return Messages.get(HeroClass.class, name()+"_desc");
	}

	public String shortDesc(){
		return Messages.get(HeroClass.class, name()+"_desc_short");
	}

	public HeroSubClass[] subClasses() {
		return subClasses;
	}

	public ArmorAbility[] armorAbilities(){
		switch (this) {
			case WARRIOR: default:
				return new ArmorAbility[]{new HeroicLeap(), new Shockwave(), new Endure()};
			case MAGE:
				return new ArmorAbility[]{new ElementalBlast(), new WildMagic(), new WarpBeacon()};
			case ROGUE:
				return new ArmorAbility[]{new SmokeBomb(), new DeathMark(), new ShadowClone()};
			case HUNTRESS:
				return new ArmorAbility[]{new SpectralBlades(), new NaturesPower(), new SpiritHawk()};
			case DUELIST:
				return new ArmorAbility[]{new Challenge(), new ElementalStrike(), new Feint()};
			case CLERIC:
				return new ArmorAbility[]{new AscendedForm(), new Trinity(), new PowerOfMany()};
			case EXECUTIONER:
				return new ArmorAbility[]{new BloodHarvest()};
			case SCOUT:
				return new ArmorAbility[]{new RainOfDarts()};
			case PYROMANCER:
				return new ArmorAbility[]{new Supernova()};
			case JAILER:
				return new ArmorAbility[]{new SpectralShackles()};
			case BRAWLER:
				return new ArmorAbility[]{new Ovation()};
			case GRAVEDIGGER:
				return new ArmorAbility[]{new Headstone()};
			case GUARDIAN:
				return new ArmorAbility[]{new SandTomb()};
			case BARD:
				return new ArmorAbility[]{new GrandOverture()};
			case BOTANIST:
				return new ArmorAbility[]{new Bloom()};
			case FORGER:
				return new ArmorAbility[]{new ForgeEruption()};
			case IRONLORD:
				return new ArmorAbility[]{new IronEclipse()};
			case AUTOMATON:
				return new ArmorAbility[]{new SentryTurret()};
			case BEASTMASTER:
				return new ArmorAbility[]{new PackRoar()};
			case SPIRITIST:
				return new ArmorAbility[]{new SoulVeil()};
			case GUNSLINGER:
				return new ArmorAbility[]{new LeadRain()};
		}
	}

	public String spritesheet() {
		switch (this) {
			case WARRIOR: default:
				return Assets.Sprites.WARRIOR;
			case MAGE:
				return Assets.Sprites.MAGE;
			case ROGUE:
				return Assets.Sprites.ROGUE;
			case HUNTRESS:
				return Assets.Sprites.HUNTRESS;
			case DUELIST:
				return Assets.Sprites.DUELIST;
			case CLERIC:
				return Assets.Sprites.CLERIC;
			case EXECUTIONER:
				return Assets.Sprites.EXECUTIONER;
			case SCOUT:
				return Assets.Sprites.SCOUT;
			case PYROMANCER:
				return Assets.Sprites.PYROMANCER;
			case JAILER:
				return Assets.Sprites.JAILER;
			case BRAWLER:
				return Assets.Sprites.BRAWLER;
			case GRAVEDIGGER:
				return Assets.Sprites.GRAVEDIGGER;
			case GUARDIAN:
				return Assets.Sprites.SAND_GUARDIAN;
			case BARD:
				return Assets.Sprites.BARD;
			case BOTANIST:
				return Assets.Sprites.BOTANIST;
			case FORGER:
				return Assets.Sprites.FORGER;
			//mordekaiser
			case IRONLORD:
				return Assets.Sprites.IRONLORD;
			case AUTOMATON:
				return SKAssets.AUTOMATON;
			case BEASTMASTER:
				return SKAssets.BEASTMASTER;
			case SPIRITIST:
				return SKAssets.SPIRITIST;
			case GUNSLINGER:
				return SKAssets.GUNSLINGER;
		}
	}

	public String splashArt(){
		switch (this) {
			case WARRIOR: default:
				return Assets.Splashes.WARRIOR;
			case MAGE:
				return Assets.Splashes.MAGE;
			case ROGUE:
				return Assets.Splashes.ROGUE;
			case HUNTRESS:
				return Assets.Splashes.HUNTRESS;
			case DUELIST:
				return Assets.Splashes.DUELIST;
			case CLERIC:
				return Assets.Splashes.CLERIC;
			case EXECUTIONER:
				return Assets.Splashes.EXECUTIONER;
			case SCOUT:
				return Assets.Splashes.SCOUT;
			case PYROMANCER:
				return Assets.Splashes.PYROMANCER;
			case JAILER:
				return Assets.Splashes.JAILER;
			case BRAWLER:
				return Assets.Splashes.BRAWLER;
			case GRAVEDIGGER:
				return Assets.Splashes.GRAVEDIGGER;
			case GUARDIAN:
				return Assets.Splashes.GUARDIAN;
			case BARD:
				return Assets.Splashes.BARD;
			case BOTANIST:
				return Assets.Splashes.BOTANIST;
			case FORGER:
				return Assets.Splashes.FORGER;
			//mordekaiser
			case IRONLORD:
				return Assets.Splashes.IRONLORD;
			case AUTOMATON:
				return SKAssets.SPLASH_AUTOMATON;
			case BEASTMASTER:
				return SKAssets.SPLASH_BEASTMASTER;
			case SPIRITIST:
				return SKAssets.SPLASH_SPIRITIST;
			case GUNSLINGER:
				return SKAssets.SPLASH_GUNSLINGER;
		}
	}
	
	public boolean isUnlocked(){
		//always unlock on debug builds
		if (DeviceCompat.isDebug()) return true;

		switch (this){
			case WARRIOR: default:
				return true;
			case MAGE:
				return Badges.isUnlocked(Badges.Badge.UNLOCK_MAGE);
			case ROGUE:
				return Badges.isUnlocked(Badges.Badge.UNLOCK_ROGUE);
			case HUNTRESS:
				return Badges.isUnlocked(Badges.Badge.UNLOCK_HUNTRESS);
			case DUELIST:
				return Badges.isUnlocked(Badges.Badge.UNLOCK_DUELIST);
			case CLERIC:
				return Badges.isUnlocked(Badges.Badge.UNLOCK_CLERIC);
			case EXECUTIONER: case SCOUT: case PYROMANCER: case JAILER: case BRAWLER:
			case GRAVEDIGGER: case GUARDIAN: case BARD: case BOTANIST: case FORGER: case IRONLORD:
			case AUTOMATON: case BEASTMASTER: case SPIRITIST: case GUNSLINGER:
				//the champions are always available
				return true;
		}
	}
	
	public String unlockMsg() {
		return shortDesc() + "\n\n" + Messages.get(HeroClass.class, name()+"_unlock");
	}

}
