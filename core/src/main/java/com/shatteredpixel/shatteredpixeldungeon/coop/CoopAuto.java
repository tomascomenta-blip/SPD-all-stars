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

package com.shatteredpixel.shatteredpixeldungeon.coop;

import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Gnoll;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAreas;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.champions.WndChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.champions.bard.BardKit;
import com.shatteredpixel.shatteredpixeldungeon.champions.botanist.EarthWall;
import com.shatteredpixel.shatteredpixeldungeon.champions.forger.ForgeHammer;
import com.shatteredpixel.shatteredpixeldungeon.champions.forger.ForgerKit;
import com.shatteredpixel.shatteredpixeldungeon.champions.forger.LivingForge;
import com.shatteredpixel.shatteredpixeldungeon.champions.gravedigger.Shovel;
import com.shatteredpixel.shatteredpixeldungeon.champions.guardian.GuardianKit;
import com.shatteredpixel.shatteredpixeldungeon.champions.guardian.SandStaff;
import com.shatteredpixel.shatteredpixeldungeon.champions.ironlord.TestScript;
import com.shatteredpixel.shatteredpixeldungeon.champions.jailer.Soul;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.KingsCrown;
import com.shatteredpixel.shatteredpixeldungeon.items.TengusMask;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.levels.CavesBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.CityBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.PrisonBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.rift.BloodRose;
import com.shatteredpixel.shatteredpixeldungeon.rift.DrakeArmor;
import com.shatteredpixel.shatteredpixeldungeon.rift.ElderBlade;
import com.shatteredpixel.shatteredpixeldungeon.rift.HeraldEye;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.HeroSelectScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKBoss;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKTest;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.FaunaTest;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.big.BigTest;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.hunt.HuntTest;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.finale.reward.ThalTest;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forest.GoblinPriest;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forgotten.TribeTest;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.finale.ThalBot;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.boss.PyreBot;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.kingdom.KnightBot;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.kraken.KrakenBot;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.props.SKStatue;
import com.shatteredpixel.shatteredpixeldungeon.ui.Button;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.StatusPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentMobTest;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.survival.SurvivalTest;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.LandWalk;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.awake.AwakeTest;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.cathedral.CathedralTest;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.finale.echo.EchoTest;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forest.PriestBot;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.starship.VarkolynBot;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.GameSettings;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Point;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.quests.QuestBot;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndChooseAbility;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndChooseSubclass;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndMessage;

//desktop testing junk. all off unless you pass -D stuff, so it's harmless in a real game:
// -Dspd.coop.auto=host-lan|host-online|join-lan|join-online   start a session from the title screen
// -Dspd.coop.addr=127.0.0.1   -Dspd.coop.codefile=<file>      where to find the host
// -Dspd.coop.name=Ana  -Dspd.coop.class=MAGE                   lobby choices
// -Dspd.coop.bot=1                                             a simple bot plays the local hero
public class CoopAuto {

	private static final String MODE = System.getProperty("spd.coop.auto");
	private static final boolean BOT_PROPERTY = "1".equals(System.getProperty("spd.coop.bot"));

	//on phones there are no -D options: a player named TESTBOT gets the bot too
	private static boolean botEnabled(){
		return BOT_PROPERTY || GameSettings.getString("coop_name", "").contains("TESTBOT");
	}

	//testing: -Dspd.coop.startlvl=N starts both heroes at level N, -Dspd.coop.subclass=1|2 gives them
	//their first or second subclass, -Dspd.coop.alltalents=1 fills every talent they have.
	//host only, before the run is shared. otherwise: desync, obviously
	static void prepareTestHeroes( java.util.List<Hero> heroes ){
		int lvl = Integer.getInteger("spd.coop.startlvl", 0);
		int sub = Integer.getInteger("spd.coop.subclass", 0);
		boolean all = "1".equals(System.getProperty("spd.coop.alltalents"));
		//testing: -Dspd.coop.crown=1 gives every hero a crown, to choose an armor ability
		boolean crown = "1".equals(System.getProperty("spd.coop.crown"));
		//testing: -Dspd.coop.mask=1 gives every hero Tengu's mask, to choose a subclass
		boolean mask = "1".equals(System.getProperty("spd.coop.mask"));
		//testing: -Dspd.coop.gold=N starts the run with that gold
		Dungeon.gold += Integer.getInteger("spd.coop.gold", 0);
		//testing: -Dspd.coop.gear=N upgrades the weapon and armour of every hero N times (late floors)
		int gear = Integer.getInteger("spd.coop.gear", 0);
		if (gear > 0){
			for (Hero h : heroes){
				if (h.belongings.weapon != null) h.belongings.weapon.upgrade(gear);
				if (h.belongings.armor != null) h.belongings.armor.upgrade(gear);
				h.STR += gear / 2;
			}
		}
		//testing: -Dspd.coop.potions=N gives every hero N potions of healing
		final int potions = Integer.getInteger("spd.coop.potions", 0);
		if (potions > 0){
			for (Hero h : heroes){
				final Hero owner = h;
				Party.withHero(h, new Runnable() {
					@Override
					public void run() {
						PotionOfHealing p =
								new PotionOfHealing();
						p.quantity(potions);
						p.identify();
						p.collect(owner.belongings.backpack);
					}
				});
			}
		}
		LateKit.prepare(heroes);
		if (lvl <= 1 && sub == 0 && !all && !crown && !mask){
			ThalTest.prepare(heroes);
			return;
		}
		for (Hero h : heroes){
			while (h.lvl < lvl){
				h.testLevelUp();
			}
			h.updateHT(true);
			h.HP = h.HT;
			if (sub >= 1 && sub <= 2){
				h.subClass = h.heroClass.subClasses()[sub-1];
				Talent.initSubclassTalents(h);
			}
			if (crown){
				final Hero owner = h;
				Party.withHero(h, new Runnable() {
					@Override
					public void run() {
						new KingsCrown().collect(owner.belongings.backpack);
					}
				});
			}
			if (mask){
				final Hero owner = h;
				Party.withHero(h, new Runnable() {
					@Override
					public void run() {
						new TengusMask().collect(owner.belongings.backpack);
					}
				});
			}
			//botanico: -Dspd.test.armor=1 hands out the armor ability (+ lvl 4 talents)
			if (System.getProperty("spd.test.armor") != null && h.armorAbility == null && h.belongings.armor != null){
				ClassArmor ca =
						ClassArmor.upgrade(h, h.belongings.armor);
				h.belongings.armor = ca;
				ca.activate(h);
				ca.charge = 100;
				h.armorAbility = h.heroClass.armorAbilities()[0];
				Talent.initArmorTalents(h);
			}
			if (all){
				for (java.util.LinkedHashMap<Talent, Integer> tier : h.talents){
					for (Talent t : tier.keySet()){
						tier.put(t, t.maxPoints());
					}
				}
			}
			CoopLog.line("test hero " + h.heroClass + " lvl=" + h.lvl + " sub=" + h.subClass + " talents=" + all);
		}
		//testing: -Dspd.test.thalrewards=1 the loot of Thal-Vekhorr (after the levels above)
		ThalTest.prepare(heroes);
	}

	//-Dspd.coop.doom=T:H:HOW kills hero H at turn T to test the downed stuff. HOW = bleed | self.
	//comma separated for more. morbid but useful
	static void prepareTestDoom( java.util.List<Hero> heroes ){
		String prop = System.getProperty("spd.coop.doom");
		if (prop == null) return;
		for (String p : prop.split(",")){
			String[] parts = p.split(":");
			int idx = Integer.parseInt(parts[1]);
			if (idx < 0 || idx >= heroes.size()) continue;
			TestDoom d = Buff.affect(heroes.get(idx), TestDoom.class);
			d.when = Float.parseFloat(parts[0]);
			d.self = parts.length > 2 && parts[2].equals("self");
			CoopLog.line("test doom for hero " + idx + " at " + d.when + (d.self ? " (own turn)" : " (buff)"));
		}
	}

	public static class TestDoom extends Buff {
		public float when = 50;
		public boolean self = false;

		@Override
		public boolean act() {
			if (Actor.now() >= when){
				CoopLog.line("test doom: hero falls");
				detach();
				if (self && target instanceof Hero){
					((Hero) target).testDoomOnTurn = true;
				} else {
					target.damage(target.HP + target.shielding() + 100, this);
				}
				return true;
			}
			spend(TICK);
			return true;
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("when", when);
			bundle.put("self", self);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			when = bundle.getFloat("when");
			self = bundle.getBoolean("self");
		}
	}

	//testing: -Dspd.coop.startdepth=N makes a new cooperative run start at depth N (host only)
	static void prepareTestDepth(){
		int d = Integer.getInteger("spd.coop.startdepth", 0);
		if (d > 1){
			Dungeon.depth = d;
			CoopLog.line("test start depth " + d);
		}
		Continent.testStart();
	}

	//-Dspd.coop.chasm=1 digs a few holes near the arrival so the bots can jump. host only
	//-Dspd.coop.bossdoor=1: hero 0 starts right where the arena seals (tengu cell, dm-300 pylon, king's door),
	//the rest stay outside. for testing the "everyone gets pulled in" thing. host only
	private static void bossDoorTest(){
		if (!"1".equals(System.getProperty("spd.coop.bossdoor"))) return;
		Level level = Dungeon.level;
		if (level == null || !Party.active() || Party.heroes().isEmpty()) return;
		Point p = null;
		if (level instanceof PrisonBossLevel) p = new Point(10, 27);
		if (level instanceof CavesBossLevel)  p = new Point(7, 34);
		if (level instanceof CityBossLevel)   p = new Point(7, 34);
		//alt world bosses close their arena when they see a hero: the first hero starts in front of the boss
		if (p == null){
			for (Mob m : level.mobs){
				if (!(m instanceof SKBoss)
						&& !(m instanceof GoblinPriest)) continue;
				for (int i = 0; i < level.length() && p == null; i++){
					int d = level.distance(i, m.pos);
					if (d >= 2 && d <= 3 && level.passable[i] && Actor.findChar(i) == null) p = level.cellToPoint(i);
				}
			}
		}
		if (p == null) return;
		int cell = level.pointToCell(p);
		for (int n = 0; n < 9 && !level.passable[cell]; n++) cell = level.pointToCell(p) + PathFinder.NEIGHBOURS9[n];
		Hero first = Party.heroes().get(0);
		first.pos = cell;
		CoopLog.line("boss door test: hero 0 at " + cell + ", the others at " + Party.heroes().get(1).pos);
	}

	public static void carveTestChasm(){
		//-Dspd.coop.mapall=1, bots are bad at finding stairs
		if ("1".equals(System.getProperty("spd.coop.mapall")) && Dungeon.level != null){
			for (int i = 0; i < Dungeon.level.length(); i++){
				if (!Dungeon.level.solid[i] || Dungeon.level.getTransition(i) != null) Dungeon.level.mapped[i] = true;
			}
		}
		bossDoorTest();
		if (!"1".equals(System.getProperty("spd.coop.chasm"))) return;
		Level level = Dungeon.level;
		if (level == null || Dungeon.hero == null || !(level instanceof RegularLevel)) return;
		if (Dungeon.depth >= 24) return;
		int from = Dungeon.hero.pos;
		int carved = 0;
		for (int i = 0; i < level.length() && carved < 3; i++){
			int d = level.distance(from, i);
			if (d < 2 || d > 4) continue;
			if (level.map[i] != Terrain.EMPTY) continue;
			if (level.getTransition(i) != null || Actor.findChar(i) != null || level.heaps.get(i) != null) continue;
			boolean nearTransition = false;
			for (int n : PathFinder.NEIGHBOURS8){
				if (level.getTransition(i + n) != null) nearTransition = true;
			}
			if (nearTransition) continue;
			Level.set(i, Terrain.CHASM, level);
			carved++;
		}
		CoopLog.line("test chasm cells: " + carved + " near " + from + " depth=" + Dungeon.depth);
	}

	//testing: -Dspd.coop.jump=1 makes this device's bot jump into any chasm it knows of
	private static final boolean JUMPER = "1".equals(System.getProperty("spd.coop.jump"));

	private static boolean jumpIntoChasm( Hero me ){
		Level level = Dungeon.level;
		int best = -1, bestDist = Integer.MAX_VALUE;
		for (int i = 0; i < level.length(); i++){
			if (!level.pit[i] || level.solid[i] || !(level.visited[i] || level.mapped[i] || level.heroFOV[i])) continue;
			if (level.adjacent(me.pos, i)){
				CoopLog.line("bot jumps into the chasm at " + i);
				CoopOrders.handleCell(i);
				return true;
			}
			for (int n : PathFinder.NEIGHBOURS8){
				int c = i + n;
				if (c < 0 || c >= level.length() || !level.passable[c] || level.pit[c] || Actor.findChar(c) != null) continue;
				int d = level.distance(me.pos, c);
				if (d < bestDist){
					best = c;
					bestDist = d;
				}
			}
		}
		if (best != -1){
			CoopOrders.handleCell(best);
			return true;
		}
		return false;
	}

	private static boolean lobbyDone = false;
	private static boolean codeWritten = false;

	public static boolean enabled(){
		return MODE != null;
	}

	//testing solo runs: -Dspd.test.start=SCOUT starts a new solo run with that class,
	//-Dspd.test.scene=heroselect opens the hero selection, -Dspd.test.kit=1 opens the ability window
	public static void onTitle(){
		String start = System.getProperty("spd.test.start");
		if (!lobbyDone && "heroselect".equals(System.getProperty("spd.test.scene"))){
			lobbyDone = true;
			if (start != null) GamesInProgress.selectedClass = HeroClass.valueOf(start);
			ShatteredPixelDungeon.switchNoFade(HeroSelectScene.class);
			return;
		}
		if (!lobbyDone && start != null){
			lobbyDone = true;
			GamesInProgress.curSlot = 1;
			Dungeon.deleteGame(1, true);

			GamesInProgress.selectedClass = HeroClass.valueOf(start);
			Dungeon.hero = null;
			Dungeon.daily = Dungeon.dailyReplay = false;
			Dungeon.initSeed();
			InterlevelScene.mode = InterlevelScene.Mode.DESCEND;
			Game.switchScene(InterlevelScene.class);
			return;
		}
		if ((MODE != null || String.valueOf(System.getProperty("spd.test.scene")).startsWith("coop")) && !lobbyDone){
			lobbyDone = MODE == null;
			ShatteredPixelDungeon.switchNoFade(CoopScene.class);
		}
	}

	static void onLobby( CoopScene scene ){
		//testing: -Dspd.test.scene=coopclass opens the hero choice of the co-op room
		if ("coopclass".equals(System.getProperty("spd.test.scene"))){
			scene.askClass();
			return;
		}
		//testing: -Dspd.test.scene=coopsaved / coopoptions open the saved runs / the run options
		if ("coopsaved".equals(System.getProperty("spd.test.scene"))){
			scene.addToFront(new WndCoopGames(WndCoopGames.Mode.VIEW, null));
			return;
		}
		if ("coopdetails".equals(System.getProperty("spd.test.scene")) && !CoopSaves.all().isEmpty()){
			for (CoopSaves.Info i : CoopSaves.all()){
				scene.addToFront(new WndCoopGames.Details(i, WndCoopGames.Mode.CONTINUE, null, new WndCoopGames(WndCoopGames.Mode.CONTINUE, null)));
			}
			return;
		}
		if ("coopoptions".equals(System.getProperty("spd.test.scene"))){
			scene.addToFront(new WndCoopOptions(null));
			return;
		}
		//testing: -Dspd.coop.ff=1 friendly fire on, -Dspd.coop.roompassword=X the host's room password
		if (System.getProperty("spd.coop.ff") != null){
			SPDSettings.coopFriendlyFire("1".equals(System.getProperty("spd.coop.ff")));
		}
		if (System.getProperty("spd.coop.roompassword") != null){
			GameSettings.put("coop_password", System.getProperty("spd.coop.roompassword"));
		}
		if (MODE == null || lobbyDone) return;
		String name = System.getProperty("spd.coop.name");
		if (name != null) GameSettings.put("coop_name", name);
		String cls = System.getProperty("spd.coop.class");
		if (cls != null) GameSettings.put("coop_class", HeroClass.valueOf(cls).name());
		switch (MODE){
			case "host-lan":
				lobbyDone = true;
				scene.hostLan();
				break;
			case "host-online":
				lobbyDone = true;
				scene.hostOnline();
				break;
			case "join-lan":
				lobbyDone = true;
				scene.joinLan(System.getProperty("spd.coop.addr", "127.0.0.1"));
				break;
			case "join-online":
				//waits for the code file in onLobbyUpdate
				break;
		}
	}

	static void onLobbyUpdate( CoopScene scene ){
		if (MODE == null) return;
		CoopSession s = CoopSession.current;
		if (MODE.equals("join-online") && !lobbyDone){
			String code = readCode();
			if (code != null){
				lobbyDone = true;
				scene.joinOnline(code);
			}
			return;
		}
		//testing: -Dspd.coop.password=X is what a guest types when the room asks for a password
		if (s != null && !s.isHost() && s.passwordWanted && System.getProperty("spd.coop.password") != null){
			s.retryWithPassword(System.getProperty("spd.coop.password"));
		}
		if (s != null && s.isHost()){
			if (s.net == CoopSession.Net.ONLINE && !codeWritten && !s.code.isEmpty()){
				codeWritten = true;
				writeCode(s.code);
			}
			//testing: -Dspd.coop.guests=N waits for N guests (1 to 7) before starting
			//-Dspd.coop.resume=1 continues the last saved run instead of starting one
			if (s.guestCount() >= Integer.getInteger("spd.coop.guests", 1) && !s.started){
				scene.startGame("1".equals(System.getProperty("spd.coop.resume")));
			}
		}
	}

	private static String readCode(){
		String path = System.getProperty("spd.coop.codefile");
		if (path == null) return System.getProperty("spd.coop.code");
		try {
			File f = new File(path);
			if (!f.exists()) return null;
			String c = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8).trim();
			return c.isEmpty() ? null : c;
		} catch (Exception e){
			return null;
		}
	}

	private static void writeCode( String code ){
		String path = System.getProperty("spd.coop.codefile");
		if (path == null) return;
		try {
			Files.write(new File(path).toPath(), code.getBytes(StandardCharsets.UTF_8));
		} catch (Exception e){
			//testing only
		}
	}

	// *** bot ***

	private static float botTimer = 0;
	private static int lastDepth = -1;
	private static int turnsOnFloor = 0;
	private static int target = -1;

	private static float watchTimer = 0;
	private static float watchLastNow = -1;

	private static float shotTimer = Float.parseFloat(System.getProperty("spd.coop.firstshot", "8"));
	private static int shotCount = 0;

	//testing: -Dspd.coop.shots=<dir> saves a screenshot every 20 seconds
	private static void screenshots(){
		String dir = System.getProperty("spd.coop.shots");
		if (dir == null) return;
		shotTimer -= Game.elapsed;
		if (shotTimer > 0) return;
		shotTimer = Float.parseFloat(System.getProperty("spd.coop.shotevery", "20"));
		try {
			int w = com.badlogic.gdx.Gdx.graphics.getBackBufferWidth();
			int h = com.badlogic.gdx.Gdx.graphics.getBackBufferHeight();
			com.badlogic.gdx.graphics.Pixmap p = com.badlogic.gdx.graphics.Pixmap.createFromFrameBuffer(0, 0, w, h);
			//framebuffer is upside down. of course it is
			com.badlogic.gdx.graphics.Pixmap flipped = new com.badlogic.gdx.graphics.Pixmap(w, h, p.getFormat());
			for (int y = 0; y < h; y++){
				flipped.drawPixmap(p, 0, y, w, 1, 0, h - 1 - y, w, 1);
			}
			String name = System.getProperty("spd.profile", "x") + "-" + (shotCount++) + ".png";
			com.badlogic.gdx.graphics.PixmapIO.writePNG(com.badlogic.gdx.Gdx.files.absolute(dir + "/" + name), flipped);
			p.dispose();
			flipped.dispose();
		} catch (Exception e){
			CoopLog.line("screenshot failed " + e);
		}
	}

	private static boolean soloPrepared = false;

	//testing solo runs: every new run gets the test level and gear again (not only the first one after opening the game)
	public static void newSoloRun(){
		soloPrepared = false;
	}
	private static boolean kitShown = false;

	//every scene: testing screenshots
	public static void sceneTick(){
		screenshots();
		backgroundTest();
	}

	//-Dspd.test.bgpause=T,D fakes a phone going to background: at T seconds freeze for D, then resume
	private static float bgClock = 0;
	private static boolean bgDone = false;

	//-Dspd.test.afkwnd=1 opens a window and just leaves it there (afk has to keep working)
	private static boolean wndShown = false;

	private static void backgroundTest(){
		if (!wndShown && "1".equals(System.getProperty("spd.test.afkwnd")) && bgClock > 12f
				&& Game.scene() instanceof GameScene){
			wndShown = true;
			GameScene.show(new WndMessage("AFK test window"));
			CoopLog.line("afkwnd: window open");
		}
		if (Game.scene() instanceof GameScene && System.getProperty("spd.test.bgpause") == null) bgClock += Game.elapsed;
		String p = System.getProperty("spd.test.bgpause");
		if (p == null || bgDone || !(Game.scene() instanceof GameScene)) return;
		bgClock += Game.elapsed;
		String[] td = p.split(",");
		if (bgClock < Float.parseFloat(td[0])) return;
		bgDone = true;
		CoopLog.line("bgpause: game to the background for " + td[1] + " s");
		Game.instance.pause();
		try {
			Thread.sleep((long) (Float.parseFloat(td[1]) * 1000));
		} catch (InterruptedException e) {
		}
		Game.instance.resume();
		CoopLog.line("bgpause: game back");
	}

	//botanico solo: -Dspd.test.armor=1 spawns stuff and spams the armor ability + kit on it
	private static float testTimer = 8f;
	private static int testStep = 0;
	private static float testBlocked = 0f;

	private static void soloArmorTest(){
		if (System.getProperty("spd.test.armor") == null && System.getProperty("spd.test.cast") == null) return;
		Hero h = Dungeon.hero;
		if (GameScene.topWindow() != null || GameScene.isSelectingCell() || !h.ready){
			testBlocked += Game.elapsed;
			if (testBlocked > 3f){
				testBlocked = 0;
				Actor cur = Actor.current();
				CoopLog.line("test blocked: window=" + GameScene.topWindow() + " selecting=" + GameScene.isSelectingCell() + " ready=" + h.ready
						+ " current=" + (cur == null ? "null" : cur.getClass().getSimpleName()) + " act=" + h.curAction);
			}
			return;
		}
		testTimer -= Game.elapsed;
		if (testTimer > 0) return;
		testTimer = 3f;
		testStep++;
		//free cell ~3 away, in sight
		java.util.ArrayList<Integer> options = new java.util.ArrayList<>();
		for (int c : ChampionAreas.cells(h.pos, 3)){
			int d = Dungeon.level.distance(h.pos, c);
			if (d >= 2 && Dungeon.level.heroFOV[c] && Dungeon.level.passable[c]
					&& !Dungeon.level.solid[c] && Actor.findChar(c) == null
					&& Dungeon.level.map[c] != Terrain.WALL){
				options.add(c);
			}
		}
		int target = options.isEmpty() ? -1 : options.get((testStep * 7) % options.size());
		if (target < 0) return;
		if (testStep % 2 == 1){
			//enemies around the target
			int n = 0;
			for (int c : ChampionAreas.cells(target, 1)){
				if (n >= 3) break;
				if (c != h.pos && !Dungeon.level.solid[c] && Dungeon.level.passable[c]
						&& Actor.findChar(c) == null){
					Mob m = new Gnoll();
					m.pos = c;
					GameScene.add(m);
					n++;
				}
			}
			CoopLog.line("test: spawned " + n + " gnolls around " + target);
			return;
		}
		if (System.getProperty("spd.test.armor") != null){
			ClassArmor ca = (ClassArmor) h.belongings.armor;
			ca.charge = 100;
			ca.execute(h, "ABILITY");
			if (GameScene.isSelectingCell()) GameScene.handleCell(target);
			CoopLog.line("test: armor ability " + h.armorAbility.name() + " at " + target);
		} else {
			ChampionKit kit = h.belongings.getItem(ChampionKit.class);
			ChampionAbility[] abs = kit.abilities();
			String only = System.getProperty("spd.test.cast");
			ChampionAbility a = abs[(testStep / 2) % abs.length];
			for (ChampionAbility x : abs) if (x.slot().equals(only)) a = x;
			a.resetCooldown(h);
			kit.cast(h, a);
			if (GameScene.isSelectingCell()) GameScene.handleCell(target);
			CoopLog.line("test: kit ability " + a.slot() + " at " + target + " terrain=" + Dungeon.level.map[target] + " block=" + EarthWall.isBlock(target));
		}
	}

	private static float krakenSoloTimer = 2f;
	private static float krakenSoloNow = -1;
	private static int krakenSoloStuck = 0;

	public static void botTick(){
		BalanceBot.watch();
		if (System.getProperty("spd.test.start") != null && !CoopSession.active() && Dungeon.hero != null && Dungeon.hero.ready){
			if (!soloPrepared){
				soloPrepared = true;
				java.util.ArrayList<Hero> one = new java.util.ArrayList<>();
				one.add(Dungeon.hero);
				prepareTestHeroes(one);
				BalanceBot.prepare(Dungeon.hero);
				//testing: -Dspd.test.items=1 puts one of each new item in the backpack, to check their pictures
				if ("1".equals(System.getProperty("spd.test.items"))){
					Item[] all = {
						new DrakeArmor.Infernal(), new DrakeArmor.Ocean(),
						new DrakeArmor.Cloud(), new DrakeArmor.Mountain(),
						new DrakeArmor.Hextech(), new DrakeArmor.Chemtech(),
						new ElderBlade(), new HeraldEye(),
						new BloodRose(), new Shovel(),
						new SandStaff(), new GuardianKit(),
						new BardKit(), new Soul(),
						new ForgeHammer(), new ForgerKit()}; //ornn
					for (Item i : all) i.identify().collect();
				}
				StatusPane.talentBlink = 0;
			}
			soloArmorTest();
			if (!kitShown && "1".equals(System.getProperty("spd.test.subwnd")) && GameScene.topWindow() == null){
				kitShown = true;
				GameScene.show(new WndChooseSubclass(
						new TengusMask(), Dungeon.hero));
			}
			if (!kitShown && "1".equals(System.getProperty("spd.test.kit")) && GameScene.topWindow() == null){
				ChampionKit kit =
						Dungeon.hero.belongings.getItem(ChampionKit.class);
				if (kit != null){
					kitShown = true;
					GameScene.show(new WndChampionKit(kit, Dungeon.hero));
				}
			}
			//testing (-Dspd.test.kraken=1): a lone hero against the Kraken, played by the bot (-Dspd.test.krakenbot=0: played by a person)
			if ("1".equals(System.getProperty("spd.test.kraken")) && !"0".equals(System.getProperty("spd.test.krakenbot"))
					&& GameScene.topWindow() == null && !GameScene.isSelectingCell()){
				krakenSoloTimer -= Game.elapsed;
				if (krakenSoloTimer <= 0){
					krakenSoloTimer = 0.1f;
					//order didn't spend time? after a few, just rest. the bot gets confused sometimes
					float now = Actor.now();
					if (now == krakenSoloNow) krakenSoloStuck++; else { krakenSoloStuck = 0; krakenSoloNow = now; }
					if (krakenSoloStuck >= 4){
						krakenSoloStuck = 0;
						CoopLog.line("bot kraken stuck at " + now + ", rests");
						CoopOrders.named("rest");
					} else if (!KrakenBot.act(Dungeon.hero)) CoopOrders.named("rest");
				}
			}
			//testing (-Dspd.test.knightbot=1): a lone hero against the dark grand knight, played by the bot
			if ("1".equals(System.getProperty("spd.test.knightbot"))
					&& GameScene.topWindow() == null && !GameScene.isSelectingCell()){
				krakenSoloTimer -= Game.elapsed;
				if (krakenSoloTimer <= 0){
					krakenSoloTimer = 0.1f;
					float now = Actor.now();
					if (now == krakenSoloNow) krakenSoloStuck++; else { krakenSoloStuck = 0; krakenSoloNow = now; }
					if (krakenSoloStuck >= 6){
						krakenSoloStuck = 0;
						CoopLog.line("bot knight stuck at " + now + ", rests");
						CoopOrders.named("rest");
					} else {
						KnightBot.solo(Dungeon.hero);
					}
				}
			}
			//testing (-Dspd.test.priestbot=1): a lone hero against the goblin high priest, played by the bot
			if ("1".equals(System.getProperty("spd.test.priestbot"))
					&& GameScene.topWindow() == null && !GameScene.isSelectingCell()){
				krakenSoloTimer -= Game.elapsed;
				if (krakenSoloTimer <= 0){
					krakenSoloTimer = 0.1f;
					float now = Actor.now();
					if (now == krakenSoloNow) krakenSoloStuck++; else { krakenSoloStuck = 0; krakenSoloNow = now; }
					if (krakenSoloStuck >= 6){
						krakenSoloStuck = 0;
						CoopLog.line("bot priest stuck at " + now + ", rests");
						CoopOrders.named("rest");
					} else {
						PriestBot.solo(Dungeon.hero);
					}
				}
			}
			//testing (-Dspd.test.varkbot=1): a lone hero against the varkolyn queen, played by the bot
			if ("1".equals(System.getProperty("spd.test.varkbot"))
					&& GameScene.topWindow() == null && !GameScene.isSelectingCell()){
				krakenSoloTimer -= Game.elapsed;
				if (krakenSoloTimer <= 0){
					krakenSoloTimer = 0.1f;
					float now = Actor.now();
					if (now == krakenSoloNow) krakenSoloStuck++; else { krakenSoloStuck = 0; krakenSoloNow = now; }
					if (krakenSoloStuck >= 6){
						krakenSoloStuck = 0;
						CoopLog.line("bot queen stuck at " + now + ", rests");
						CoopOrders.named("rest");
					} else {
						VarkolynBot.solo(Dungeon.hero);
					}
				}
			}
			//testing (-Dspd.test.tribe=1..4): the quest of the tribe of the Forgotten Land
			if (DevFlags.tribe() > 0) TribeTest.tick(Dungeon.hero);
			SoloTest.tick(Dungeon.hero); //scripted solo tests (-Dspd.test.cast=...)
			ThalTest.solo(Dungeon.hero); //the loot of Thal-Vekhorr (-Dspd.test.thalrewards=1)
			BalanceBot.tick(Dungeon.hero);
			QuestBot.tick(Dungeon.hero);
			ThalBot.tick(Dungeon.hero); //-Dspd.test.thalbot=1
			PyreBot.tick(Dungeon.hero); //-Dspd.test.pyrebot=1
			SKTest.tick(Dungeon.hero); //alt world guns (testing only)
			EchoTest.solo(Dungeon.hero); //echoes of the final fight (testing only)
			ContinentMobTest.solo(Dungeon.hero); //monsters of the dark continent (testing only)
			SurvivalTest.solo(Dungeon.hero); //surviving over there (testing only)
			BigTest.tick(Dungeon.hero); //colossi (testing only)
			FaunaTest.tick(Dungeon.hero); //beasts of the ash plains (testing only)
			LandWalk.tick(Dungeon.hero); //walking from biome to biome (testing only)
			AwakeTest.tick(Dungeon.hero); //the way home of the true ending (testing only)
			CathedralTest.tick(Dungeon.hero); //inside the furnace cathedral (testing only)
			HuntTest.solo(Dungeon.hero); //hunting the big ones (testing only)
			TestScript.tick(); //mordekaiser (testing only)
		}
		//-Dspd.test.questbot in coop: this hero plays the quest, the others are the usual bots
		if (CoopSession.active() && QuestBot.enabled() && Dungeon.local() != null && Dungeon.local().isAlive()){
			QuestBot.tick(Dungeon.local());
			return;
		}
		if (!botEnabled() || !CoopSession.active()) return;
		botTalents();
		watchTimer += Game.elapsed;
		if (watchTimer > 5f){
			watchTimer = 0;
			float now = Actor.now();
			if (now == watchLastNow){
				Window tw = GameScene.topWindow();
				Actor cur = Actor.current();
				CoopLog.line("WATCH stuck now=" + now + " current=" + (cur == null ? "null" : cur.getClass().getSimpleName() + "#" + cur.id())
						+ " idle=" + Actor.idle()
						+ " localReady=" + Dungeon.local().ready + " localAlive=" + Dungeon.local().isAlive()
						+ " window=" + (tw == null ? "-" : tw.getClass().getName())
						+ " selecting=" + GameScene.isSelectingCell()
						+ " " + CoopSession.debugState());
				//which heroes are which, and what the turn thread is doing
				StringBuilder hs = new StringBuilder("WATCH heroes:");
				for (Hero h : Party.heroes()){
					hs.append(" #").append(h.id()).append("[pos=").append(h.pos).append(" hp=").append(h.HP)
							.append(" down=").append(h.coopDown).append(" ready=").append(h.ready)
							.append(" act=").append(h.curAction == null ? "-" : h.curAction.getClass().getSimpleName())
							.append(" inActors=").append(Actor.all().contains(h)).append("]");
				}
				CoopLog.line(hs.toString());
				for (java.util.Map.Entry<Thread, StackTraceElement[]> e : Thread.getAllStackTraces().entrySet()){
					if (!"SHPD Actor Thread".equals(e.getKey().getName())) continue;
					StringBuilder st = new StringBuilder("WATCH actor thread:");
					StackTraceElement[] tr = e.getValue();
					for (int i = 0; i < Math.min(9, tr.length); i++) st.append(" < ").append(tr[i].getClassName().replaceAll(".*\\.", "")).append(".").append(tr[i].getMethodName()).append(":").append(tr[i].getLineNumber());
					CoopLog.line(st.toString());
				}
			}
			watchLastNow = now;
		}
		botTimer -= Game.elapsed;
		if (botTimer > 0) return;
		botTimer = 0.12f;

		Window w = GameScene.topWindow();
		if (w != null){
			if (!CoopSession.localInputAllowed()) return;
			botTimer = 0.4f;
			answerWindow(w);
			return;
		}

		//testing (-Dspd.coop.bottaps=1): like a player, tap ahead while another hero has the turn
		if (BOT_TAPS && !CoopSession.localInputAllowed()){
			Hero me = Dungeon.local();
			if (me != null && me.isAlive() && !me.coopDown && me.curAction == null && !me.resting
					&& !CoopSession.tapQueued() && !GameScene.isSelectingCell() && rnd.nextInt(3) == 0){
				int cell = -1;
				for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
					if (m.alignment == Char.Alignment.ENEMY && Dungeon.level.heroFOV[m.pos]
							&& Dungeon.level.distance(me.pos, m.pos) <= 5) cell = m.pos;
				}
				if (cell == -1) cell = pickFrontier(me);
				if (cell != -1){
					CoopLog.line("bot taps ahead " + cell);
					GameScene.handleCell(cell);
				}
			}
			return;
		}

		if (!CoopSession.localInputAllowed()) return;
		Hero me = Dungeon.local();
		if (me == null || !me.isAlive() || !me.ready) return;

		//aiming something: pick an enemy, or any visible cell
		if (GameScene.isSelectingCell()){
			//the forge of the Forger asks for a hero: itself
			if (forgeAim){
				forgeAim = false;
				GameScene.handleCell(me.pos);
				return;
			}
			int hunt = HuntTest.aimFor(me);
			GameScene.handleCell(hunt != -1 ? hunt : aimCell(me));
			return;
		}

		//fire, recipes, sacred torch, and walking off alone (-Dspd.test.surv, tether): its own plan
		if (SurvivalTest.act(me)) return;
		//climbing, taming, knocking down, the forge (-Dspd.test.hunt)
		if (HuntTest.coop(me)) return;

		//use random items now and then, it pokes most prompt types (and finds bugs, a lot of them)
		if (turnsOnFloor % 23 == 0){
			if (useRandomItem(me)) return;
		}

		if (Dungeon.depth != lastDepth){
			lastDepth = Dungeon.depth;
			turnsOnFloor = 0;
			target = -1;
		}
		turnsOnFloor++;

		//low hp + nothing around = nap
		boolean enemyNear = false;
		for (Mob m : Dungeon.level.mobs){
			if (m.alignment == Char.Alignment.ENEMY && Dungeon.level.heroFOV[m.pos]) enemyNear = true;
		}
		if (!enemyNear && me.HP < me.HT/2){
			CoopOrders.named(rnd.nextInt(4) == 0 ? "search" : "rest");
			return;
		}

		//testing (-Dspd.coop.ffbot=1, friendly fire on): now and then hit the friend next to us
		if (FF_BOT && CoopRules.friendlyFire() && me.HP > me.HT/2 && rnd.nextInt(6) == 0){
			for (Hero h : Party.living()){
				if (h != me && Dungeon.level.adjacent(me.pos, h.pos)){
					CoopLog.line("bot hits its friend " + Party.indexOf(h));
					CoopOrders.attackFriend(h.pos);
					return;
				}
			}
		}

		//stuck (orders not spending time) -> try something else for a bit
		if (Actor.now() == lastTime){
			stuck++;
		} else {
			stuck = 0;
			stuckRounds = 0;
			lastTime = Actor.now();
		}
		if (stuck >= 3){
			stuck = 0;
			stairsCooldown = 15;
			//boxed in by the others in a corridor, wait. conga line problems
			if (++stuckRounds % 2 == 0){
				CoopOrders.named("rest");
				return;
			}
			target = pickFrontier(me);
			if (target == -1) target = randomVisited(me);
			if (target != -1) {
				CoopOrders.handleCell(target);
			} else {
				CoopOrders.named("rest");
			}
			return;
		}
		if (stairsCooldown > 0) stairsCooldown--;

		//kraken ship: own plan
		if (KrakenBot.act(me)) return;
		//same for the knight's throne hall
		if (KnightBot.act(me)) return;
		//and the priest's clearing
		if (PriestBot.act(me)) return;
		//and the hive. ya, every boss got its own bot. no me arrepiento
		if (VarkolynBot.act(me)) return;
		//and the clock giant up in the sky
		if (ThalBot.act(me)) return;
		//and the walking pyre on the ash plain
		if (PyreBot.act(me)) return;
		//echoes of the final fight (-Dspd.test.echo)
		if (EchoTest.act(me)) return;
		//monsters of the dark continent (-Dspd.test.cmob)
		if (ContinentMobTest.act(me)) return;
		//and the beasts of the ash plains (-Dspd.test.fauna)
		if (FaunaTest.act(me)) return;
		//and the walk across the biomes (-Dspd.test.landwalk)
		if (LandWalk.act(me)) return;
		//and the way home once the continent is awake (-Dspd.test.awakewalk)
		if (AwakeTest.act(me)) return;
		//and into the furnace cathedral (-Dspd.test.cathedralbot)
		if (CathedralTest.act(me)) return;

		//champion classes: now and then use an ability of the kit while enemies are in sight
		if (enemyNear && --kitTimer <= 0){
			ChampionKit kit =
					me.belongings.getItem(ChampionKit.class);
			if (kit != null && kit.readyCount(me) > 0){
				kitTimer = 6 + rnd.nextInt(8);
				CoopLog.line("bot opens kit");
				CoopOrders.execute(kit, ChampionKit.AC_ABILITIES);
				return;
			}
		}

		if (JUMPER && jumpIntoChasm(me)) return;

		//testing (-Dspd.coop.botstatue=1): go and get the blessing of the world statue
		if ("1".equals(System.getProperty("spd.coop.botstatue"))
				&& me.buff(SKStatue.Blessing.class) == null){
			for (Mob m : Dungeon.level.mobs){
				if (m instanceof SKStatue
						&& Dungeon.gold >= SKStatue.cost()){
					CoopLog.line("bot goes to the statue");
					CoopOrders.handleCell(m.pos);
					return;
				}
			}
		}

		//testing (-Dspd.coop.mask=1): put on Tengu's mask to choose a subclass
		TengusMask mask =
				me.belongings.getItem(TengusMask.class);
		if (mask != null && me.subClass == HeroSubClass.NONE){
			CoopLog.line("bot wears the mask");
			CoopOrders.execute(mask, "WEAR");
			return;
		}
		if (me.subClass != HeroSubClass.NONE && !subclassLogged){
			subclassLogged = true;
			CoopLog.line("bot subclass " + me.subClass + " mask left=" + (mask != null));
		}

		//testing (-Dspd.coop.crown=1): put on the crown to choose an armor ability
		KingsCrown crown =
				me.belongings.getItem(KingsCrown.class);
		if (crown != null && me.belongings.armor() != null){
			CoopLog.line("bot wears the crown");
			CoopOrders.execute(crown, "WEAR");
			return;
		}

		//testing (-Dspd.test.thalrewards=1): the heart, the chest and the clock of Thal-Vekhorr
		if (ThalTest.coopBot(me)) return;

		//fight anything adjacent, then chase what we can see
		Mob closest = null;
		for (Mob m : Dungeon.level.mobs){
			if (m.alignment == Char.Alignment.ENEMY && Dungeon.level.heroFOV[m.pos]){
				if (Dungeon.level.adjacent(me.pos, m.pos)){
					CoopOrders.handleCell(m.pos);
					return;
				}
				if (closest == null || Dungeon.level.distance(me.pos, m.pos) < Dungeon.level.distance(me.pos, closest.pos)){
					closest = m;
				}
			}
		}
		if (closest != null && Dungeon.level.distance(me.pos, closest.pos) <= 5){
			CoopOrders.handleCell(closest.pos);
			return;
		}

		//pick up items in view
		if (stairsCooldown == 0){
			for (Heap heap : Dungeon.level.heaps.valueList()){
				if (Dungeon.level.heroFOV[heap.pos] && heap.pos != me.pos
						&& Dungeon.level.distance(me.pos, heap.pos) <= 6){
					CoopOrders.handleCell(heap.pos);
					return;
				}
			}
		}

		//after a while, head for the stairs down
		if ((turnsOnFloor > STAIRS_AFTER && stairsCooldown == 0) || turnsOnFloor > 300){
			LevelTransition exit = Dungeon.level.getTransition(LevelTransition.Type.REGULAR_EXIT);
			if (exit != null && (Dungeon.level.visited[exit.cell()] || Dungeon.level.mapped[exit.cell()])){
				CoopOrders.handleCell(exit.cell());
				return;
			}
		}

		if (turnsOnFloor % 17 == 0){
			CoopOrders.named("search");
			return;
		}

		//explore: walk to a known cell next to unknown ones
		if (target == -1 || target == me.pos || turnsOnFloor % 25 == 0){
			target = pickFrontier(me);
		}
		if (target != -1){
			int t = target;
			if (turnsOnFloor % 9 == 0) target = -1;
			CoopOrders.handleCell(t);
		} else {
			//nothing left to explore that we can see: go down, or wander somewhere already seen
			LevelTransition exit = Dungeon.level.getTransition(LevelTransition.Type.REGULAR_EXIT);
			if (exit != null && (Dungeon.level.visited[exit.cell()] || Dungeon.level.mapped[exit.cell()])){
				CoopOrders.handleCell(exit.cell());
			} else {
				int r = randomVisited(me);
				if (r != -1) CoopOrders.handleCell(r);
				else CoopOrders.named("rest");
			}
		}
	}

	private static final java.util.Random rnd = new java.util.Random();
	private static final boolean BOT_TAPS = "1".equals(System.getProperty("spd.coop.bottaps"));
	private static final boolean FF_BOT = "1".equals(System.getProperty("spd.coop.ffbot"));

	//testing: -Dspd.coop.bottalents=1 spends talent points like a player in the talent screen,
	//also while another hero has the turn (then the upgrade waits for this player's turn)
	private static final boolean BOT_TALENTS = "1".equals(System.getProperty("spd.coop.bottalents"));
	private static float talentTimer = 1.5f;

	//FIXME picks talents in a fixed order, so the bots always build the same. fine for tests i guess
	private static void botTalents(){
		if (!BOT_TALENTS) return;
		talentTimer -= Game.elapsed;
		if (talentTimer > 0) return;
		talentTimer = 0.7f + rnd.nextFloat();
		Hero me = Dungeon.local();
		if (me == null || !me.isAlive() || me.coopDown || GameScene.topWindow() != null) return;
		for (int tier = 1; tier <= me.talents.size(); tier++){
			if (CoopOrders.talentPointsAvailable(me, tier) <= 0) continue;
			ArrayList<Talent> options = new ArrayList<>();
			for (Talent t : me.talents.get(tier-1).keySet()){
				if (CoopOrders.pointsInTalent(me, t) < t.maxPoints()) options.add(t);
			}
			if (options.isEmpty()) continue;
			Talent t = options.get(rnd.nextInt(options.size()));
			CoopLog.line("bot talent " + t + " tier " + tier + (CoopSession.localInputAllowed() ? " (own turn)" : " (other's turn)"));
			CoopOrders.upgradeTalent(t, null);
			return;
		}
	}
	//testing: -Dspd.coop.stairs=N turns on a floor before the bot heads for the stairs
	private static final int STAIRS_AFTER = Integer.getInteger("spd.coop.stairs", 40);

	private static int kitTimer = 3;
	private static boolean subclassLogged = false;
	private static boolean forgeAim = false; //the bot chose the forge button

	private static void answerWindow( Window w ){
		if (ThalBot.pickPortalWindow(w)) return;
		if (SurvivalTest.answer(w)) return;
		if (HuntTest.answer(w)) return;
		ArrayList<Button> buttons = CoopSession.buttonsOfWindow(w);
		//the jumper always says yes to the chasm
		if (JUMPER && w.getClass().getName().contains("Chasm") && !buttons.isEmpty()){
			Button yes = null;
			for (Button b : buttons) if (b instanceof RedButton){ yes = b; break; }
			if (yes != null){
				CoopLog.line("bot says yes to the chasm");
				if (!CoopSession.buttonClicked(yes)) yes.coopClick();
				return;
			}
		}
		//ability window of a champion: pick any ability that can be used (not the info buttons)
		if (w instanceof WndChampionKit){
			ArrayList<Button> abilities = new ArrayList<>();
			for (Button b : buttons){
				if (b instanceof RedButton && b.active) abilities.add(b);
			}
			if (!abilities.isEmpty()){
				Button choice = abilities.get(rnd.nextInt(abilities.size()));
				CoopLog.line("bot picks ability " + ((RedButton) choice).text());
				forgeAim = ((RedButton) choice).text().contains(
						Messages.titleCase(Messages.get(LivingForge.class, "name")));
				if (!CoopSession.buttonClicked(choice)) choice.coopClick();
				return;
			}
		}
		//the heart / the chest of Thal-Vekhorr with a fixed choice for testing
		if (ThalTest.answer(w)) return;
		//subclass or armor ability window: one of the choices (not the shuffle or info buttons)
		if (w instanceof WndChooseSubclass
				|| w instanceof WndChooseAbility
				|| ThalTest.isChoiceWindow(w)){
			ArrayList<Button> choices = new ArrayList<>();
			for (Button b : buttons){
				if (b instanceof RedButton && b.active) choices.add(b);
			}
			if (!choices.isEmpty()){
				Button choice = choices.get(rnd.nextInt(choices.size()));
				CoopLog.line("bot picks " + ((RedButton) choice).text());
				if (!CoopSession.buttonClicked(choice)) choice.coopClick();
				return;
			}
		}
		//inventory: pick a random item slot
		ArrayList<Button> slots = new ArrayList<>();
		for (Button b : buttons){
			if (b.getClass().getName().contains("Slot") && b.active) slots.add(b);
		}
		Button choice = null;
		if (!slots.isEmpty() && rnd.nextInt(5) > 0){
			choice = slots.get(rnd.nextInt(slots.size()));
		} else if (!buttons.isEmpty() && rnd.nextInt(6) > 0){
			ArrayList<Button> enabled = new ArrayList<>();
			for (Button b : buttons) if (b.active) enabled.add(b);
			if (!enabled.isEmpty()) choice = enabled.get(rnd.nextInt(Math.min(2, enabled.size())));
		}
		if (choice != null){
			if (!CoopSession.buttonClicked(choice)) choice.coopClick();
		} else if (!CoopSession.windowBack(w)){
			w.onBackPressed();
		}
	}

	private static int aimCell( Hero me ){
		for (Mob m : Dungeon.level.mobs){
			if (m.alignment == Char.Alignment.ENEMY && Dungeon.level.heroFOV[m.pos]){
				//sometimes a free cell next to the enemy (mushrooms, lantern...)
				if (rnd.nextInt(3) == 0){
					for (int n : PathFinder.NEIGHBOURS8){
						int c = m.pos + n;
						if (c > 0 && c < Dungeon.level.length() && Dungeon.level.passable[c]
								&& Actor.findChar(c) == null
								&& Dungeon.level.distance(me.pos, c) <= 3){
							return c;
						}
					}
				}
				return m.pos;
			}
		}
		ArrayList<Integer> cells = new ArrayList<>();
		for (int i = 0; i < Dungeon.level.length(); i++){
			if (Dungeon.level.heroFOV[i] && Dungeon.level.passable[i] && i != me.pos) cells.add(i);
		}
		return cells.isEmpty() ? me.pos : cells.get(rnd.nextInt(cells.size()));
	}

	private static boolean useRandomItem( Hero me ){
		ArrayList<Item> items = new ArrayList<>();
		for (Item i : me.belongings) items.add(i);
		if (items.isEmpty()) return false;
		Item item = items.get(rnd.nextInt(items.size()));
		ArrayList<String> actions = item.actions(me);
		actions.remove(Item.AC_DROP);
		//keep the bot alive longer: don't throw away gear or useful items
		if (!(item instanceof MissileWeapon)){
			actions.remove(Item.AC_THROW);
		}
		actions.remove("UNEQUIP");
		actions.remove("DETACH");
		if (actions.isEmpty()) return false;
		String action = actions.get(rnd.nextInt(actions.size()));
		CoopLog.line("bot uses " + item.getClass().getSimpleName() + " " + action);
		CoopOrders.execute(item, action);
		return true;
	}

	private static float lastTime = -1;
	private static int stuck = 0;
	private static int stuckRounds = 0;
	private static int stairsCooldown = 0;

	private static int randomVisited( Hero me ){
		ArrayList<Integer> options = new ArrayList<>();
		for (int i = 0; i < Dungeon.level.length(); i++){
			if (Dungeon.level.visited[i] && Dungeon.level.passable[i] && i != me.pos
					&& Dungeon.level.getTransition(i) == null) options.add(i);
		}
		if (options.isEmpty()) return -1;
		return options.get(new java.util.Random().nextInt(options.size()));
	}

	private static int pickFrontier( Hero me ){
		int w = Dungeon.level.width();
		ArrayList<Integer> options = new ArrayList<>();
		for (int i = w; i < Dungeon.level.length() - w; i++){
			if (!Dungeon.level.visited[i] || !Dungeon.level.passable[i] || i == me.pos
					|| Dungeon.level.getTransition(i) != null) continue;
			for (int n : PathFinder.NEIGHBOURS4){
				int c = i + n;
				if (!Dungeon.level.visited[c] && !Dungeon.level.mapped[c] && !Dungeon.level.solid[c]){
					options.add(i);
					break;
				}
			}
		}
		if (options.isEmpty()) return -1;
		return options.get(new java.util.Random().nextInt(options.size()));
	}
}
