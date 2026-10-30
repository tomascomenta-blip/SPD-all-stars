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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.MobSpawner;
import com.shatteredpixel.shatteredpixeldungeon.effects.TargetedCell;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.MailArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.PlateArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ScaleArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Longsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Shortsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKWorlds;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.props.SKProps;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.quests.QuestBot;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.undersea.UnderseaFeatures;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.starship.StarshipFeatures;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.Game;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

//testing only. a dumb bot w/ "normal" gear for that depth that walks floors (or duels, see arena)
//and writes down how much it got hurt. it's how the worlds got rebalanced, so blame it
// -Dspd.test.balkit=R      gear of region R (1-5), see KIT
// -Dspd.test.balfloors=N   floors to play before quitting (default 4)
// -Dspd.test.balstairs=T   turns on a floor before heading down (default 400)
// -Dspd.test.arena=1      instead of walking floors: duels against the monsters of the region, one at a time,
//                          healed between duels, with nothing else on the floor (-Dspd.test.duels=N, default 30)
//output: lines starting w/ "BAL" in the coop log (-Dspd.coop.log). arena mode is the one to trust,
//the floor walker drowns in the sea and steps on every spike
public class BalanceBot {

	private static final int REGION = Integer.getInteger("spd.test.balkit", 0);
	private static final int FLOORS = Integer.getInteger("spd.test.balfloors", 4);
	private static final int STAIRS = Integer.getInteger("spd.test.balstairs", 400);
	private static final boolean ARENA = "1".equals(System.getProperty("spd.test.arena"));
	private static final int DUELS = Integer.getInteger("spd.test.duels", 30);

	//level, weapon tier, weapon +, armor tier, armor +, healing potions
	//roughly what the loot of a run gives by the middle of each region: 3 upgrades and
	//2 strength potions per region, a weapon and an armor of the region's tier. The potions stand
	//for everything else a player carries (wands, scrolls...), so the bot lasts a few floors
	private static final int[][] KIT = {
			{},
			{3, 1, 1, 1, 1, 4},
			{8, 2, 2, 2, 2, 4},
			{13, 3, 3, 3, 3, 4},
			{17, 4, 4, 4, 5, 4},
			{22, 5, 6, 5, 6, 4},
	};

	public static boolean enabled(){
		return REGION > 0;
	}

	public static void prepare( Hero h ){
		if (!enabled()) return;
		int[] k = KIT[Math.min(5, REGION)];
		while (h.lvl < k[0]) h.testLevelUp();
		h.STR = 8 + 2 * Math.min(5, REGION);

		MeleeWeapon w = weapon(k[1]);
		w.identify();
		w.upgrade(k[2]);
		w.cursed = false;
		h.belongings.weapon = w;

		Armor a = armor(k[3]);
		a.identify();
		a.upgrade(k[4]);
		a.cursed = false;
		h.belongings.armor = a;
		a.activate(h);

		new Food().quantity(2).identify().collect(h.belongings.backpack);

		PotionOfHealing p = new PotionOfHealing();
		p.quantity(k[5]);
		p.identify();
		p.collect(h.belongings.backpack);

		if (REGION >= 3){
			h.subClass = h.heroClass.subClasses()[0];
			Talent.initSubclassTalents(h);
		}
		//talent points go to the first talents of each tier, like a quick player would do
		for (int tier = 1; tier <= h.talents.size(); tier++){
			for (Talent t : h.talents.get(tier - 1).keySet()){
				while (h.talentPointsAvailable(tier) > 0 && h.pointsInTalent(t) < t.maxPoints()) h.upgradeTalent(t);
			}
		}
		h.updateHT(true);
		h.HP = h.HT;
		startDepth = Dungeon.depth;
		CoopLog.line("BAL kit region=" + REGION + " lvl=" + h.lvl + " HT=" + h.HT + " str=" + h.STR
				+ " weapon=" + w.getClass().getSimpleName() + "+" + w.level()
				+ " armor=" + a.getClass().getSimpleName() + "+" + a.level()
				+ " sub=" + h.subClass + " sk=" + SKWorlds.isSK(Dungeon.depth));
	}

	private static MeleeWeapon weapon( int tier ){
		switch (tier){
			case 1: return new WornShortsword();
			case 2: return new Shortsword();
			case 3: return new Sword();
			case 4: return new Longsword();
			default: return new Greatsword();
		}
	}

	private static Armor armor( int tier ){
		switch (tier){
			case 1: return new ClothArmor();
			case 2: return new LeatherArmor();
			case 3: return new MailArmor();
			case 4: return new ScaleArmor();
			default: return new PlateArmor();
		}
	}

	// *** the walk ***

	private static int startDepth = -1;
	private static int floorDepth = -1;
	private static float floorStart;
	private static int floorKills;
	private static int floorTaken;
	private static int floorPotions;
	private static int lastHP = -1;
	private static int target = -1;
	private static float timer = 0;
	private static float lastNow = -1;
	private static int stuck = 0;
	private static boolean done = false;
	private static int said = 0;

	private static void say( Hero me, String what ){
		if (said++ % 25 == 0) CoopLog.line("BALDBG t=" + (int) (Actor.now() - floorStart) + " pos=" + me.pos + " hp=" + me.HP + " " + what);
	}

	//every frame, also when the hero can't act (dead heroes are never ready)
	public static void watch(){
		Hero me = Dungeon.hero;
		if (!enabled() || done || me == null || startDepth < 0) return;
		if (lastHP >= 0 && me.HP < lastHP){
			floorTaken += lastHP - me.HP;
			duelTaken += lastHP - me.HP;
			hurtAt = Actor.now();
		}
		lastHP = me.HP;
		if (!me.isAlive()){
			closeFloor(me, "death");
			finish("death");
		}
	}

	private static float hurtAt = -100;
	private static int duels = 0, foeId = -1, duelTaken = 0;
	private static float duelStart = 0;
	private static String foeName = "";
	private static int foeDepth = 0;
	private static int floorTurns = 0;

	public static void tick( Hero me ){
		//with the quest bot it only hands out the kit (-Dspd.test.balkit=R), the quest bot plays
		if (!enabled() || done || me == null || startDepth < 0 || QuestBot.enabled()) return;
		if (!me.isAlive()) return;

		if (Dungeon.depth != floorDepth){
			if (floorDepth != -1) closeFloor(me, "stairs");
			if (Dungeon.depth >= startDepth + FLOORS){
				finish("floors");
				return;
			}
			floorDepth = Dungeon.depth;
			floorStart = Actor.now();
			floorKills = Statistics.enemiesSlain;
			floorTaken = 0;
			floorPotions = 0;
			floorTurns = 0;
			target = -1;
		}

		timer -= Game.elapsed;
		if (timer > 0) return;
		timer = 0.06f;

		Window w = GameScene.topWindow();
		if (w != null){
			w.hide();
			return;
		}
		if (GameScene.isSelectingCell()){
			GameScene.handleCell(me.pos);
			return;
		}
		if (!me.ready){
			say(me, "not ready");
			return;
		}

		if (ARENA){
			arena(me);
			return;
		}

		float now = Actor.now();
		if (now == lastNow) stuck++; else { stuck = 0; lastNow = now; }
		if (stuck >= 4){
			stuck = 0;
			target = randomVisited(me);
			if (target != -1 && Random.Int(2) == 0) CoopOrders.handleCell(target);
			else CoopOrders.named("rest");
			return;
		}

		boolean enemyNear = false;
		Mob closest = null;
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (m.alignment != Char.Alignment.ENEMY || !Dungeon.level.heroFOV[m.pos]) continue;
			if (m.properties().contains(Char.Property.IMMOVABLE) && Dungeon.level.distance(me.pos, m.pos) > 1) continue;
			enemyNear = true;
			if (closest == null || Dungeon.level.distance(me.pos, m.pos) < Dungeon.level.distance(me.pos, closest.pos)) closest = m;
		}

		//out of a marked cell, like a player would
		if (TargetedCell.cells.get(me.pos) != null){
			for (int n : PathFinder.NEIGHBOURS8){
				int c = me.pos + n;
				if (Dungeon.level.passable[c] && Actor.findChar(c) == null && TargetedCell.cells.get(c) == null){
					say(me, "dodge");
					CoopOrders.handleCell(c);
					return;
				}
			}
		}
		//hurt by something it can't see (a slit, a sniper in the dark): don't sit there
		boolean underFire = Actor.now() - hurtAt < 3;

		//and eats when hungry
		Hunger hunger = me.buff(Hunger.class);
		if (hunger != null && hunger.hunger() >= Hunger.HUNGRY && !enemyNear){
			Food food = me.belongings.getItem(Food.class);
			if (food != null){
				say(me, "eat");
				CoopOrders.execute(food, Food.AC_EAT);
				return;
			}
		}

		//a player drinks before dying
		if ((enemyNear || underFire) && me.HP < me.HT * 0.3f){
			PotionOfHealing p = me.belongings.getItem(PotionOfHealing.class);
			if (p != null){
				floorPotions++;
				say(me, "drink");
				CoopOrders.execute(p, "DRINK");
				return;
			}
		}
		if (!enemyNear && !underFire && me.HP < me.HT * 0.6f && (hunger == null || !hunger.isStarving())){
			say(me, "rest");
			CoopOrders.named("rest");
			return;
		}

		//like a player, it breaks whatever keeps bringing monsters before fighting them
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if ((m instanceof StarshipFeatures.Spawner || m instanceof StarshipFeatures.VarkolynEgg)
					&& m.isAlive() && Dungeon.level.heroFOV[m.pos] && Dungeon.level.distance(me.pos, m.pos) <= 8){
				say(me, "spawner " + m.getClass().getSimpleName() + "@" + m.pos);
				CoopOrders.handleCell(m.pos);
				return;
			}
		}

		if (closest != null && Dungeon.level.distance(me.pos, closest.pos) <= 6){
			say(me, "fight " + closest.getClass().getSimpleName() + "@" + closest.pos);
			CoopOrders.handleCell(closest.pos);
			return;
		}

		float onFloor = Actor.now() - floorStart;
		floorTurns = (int) onFloor;
		for (Heap heap : Dungeon.level.heaps.valueList()){
			if (Dungeon.level.heroFOV[heap.pos] && heap.pos != me.pos && Dungeon.level.distance(me.pos, heap.pos) <= 6
					&& heap.type == Heap.Type.HEAP){
				say(me, "heap " + heap.pos);
				CoopOrders.handleCell(heap.pos);
				return;
			}
		}

		LevelTransition exit = Dungeon.level.getTransition(LevelTransition.Type.REGULAR_EXIT);
		boolean exitKnown = exit != null && (Dungeon.level.visited[exit.cell()] || Dungeon.level.mapped[exit.cell()]);
		if (exitKnown && onFloor > STAIRS){
			CoopOrders.handleCell(exit.cell());
			return;
		}

		if (target == -1 || target == me.pos || ((int) onFloor) % 30 == 0) target = frontier(me);
		if (target != -1){
			say(me, "explore " + target);
			CoopOrders.handleCell(target);
		} else if (exitKnown){
			CoopOrders.handleCell(exit.cell());
		} else if (onFloor > STAIRS * 3){
			//lost: show the floor, like a scroll of magic mapping would
			for (int i = 0; i < Dungeon.level.length(); i++) Dungeon.level.mapped[i] = true;
			CoopOrders.named("search");
		} else {
			say(me, "search");
			CoopOrders.named("search");
		}
	}

	// *** duels ***

	private static void arena( Hero me ){
		UnderseaFeatures.Oxygen air = me.buff(UnderseaFeatures.Oxygen.class);
		if (air != null) air.oxygen = UnderseaFeatures.Oxygen.MAX;
		Actor a = Actor.findById(foeId);
		boolean foeAlive = a instanceof Mob && ((Mob) a).isAlive() && Dungeon.level.mobs.contains(a);
		if (foeId != -1 && foeAlive && Actor.now() - duelStart > 80){
			Mob m = (Mob) a;
			m.destroy();
			if (m.sprite != null) m.sprite.killAndErase();
			endDuel(me, "timeout");
			return;
		}
		if (foeId != -1 && !foeAlive){
			endDuel(me, "kill");
			return;
		}
		if (foeId == -1){
			if (duels >= DUELS){
				finish("duels");
				return;
			}
			//nothing else on the floor, and a fresh hero
			for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
				if (m.alignment == Char.Alignment.ENEMY && !(m instanceof SKProps.SKProp)){
					m.destroy();
					if (m.sprite != null) m.sprite.killAndErase();
				}
			}
			for (Buff b : me.buffs().toArray(new Buff[0])){
				if (b.type == Buff.buffType.NEGATIVE) b.detach();
			}
			me.HP = me.HT;
			lastHP = me.HP;
			Hunger hunger = me.buff(Hunger.class);
			if (hunger != null) hunger.satisfy(Hunger.STARVING);
			int first = (Math.min(5, REGION) - 1) * 5 + 1;
			foeDepth = first + Random.Int(4);
			ArrayList<Class<? extends Mob>> rot = SKWorlds.isSK(Dungeon.depth) ? SKWorlds.mobRotation(foeDepth) : MobSpawner.getMobRotation(foeDepth);
			if (rot == null || rot.isEmpty()) rot = MobSpawner.getMobRotation(foeDepth);
			Class<? extends Mob> c = Random.element(rot);
			Mob m = Reflection.newInstance(c);
			int cell = -1;
			//in sight 3-5 cells away if possible; in a cramped room, anywhere close
			for (int tries = 0; tries < 600 && cell == -1; tries++){
				int k = me.pos + Random.IntRange(-6, 6) + Random.IntRange(-6, 6) * Dungeon.level.width();
				if (k < 0 || k >= Dungeon.level.length() || !Dungeon.level.passable[k] || Dungeon.level.solid[k]) continue;
				int d = Dungeon.level.distance(k, me.pos);
				if (Actor.findChar(k) != null || d < 2) continue;
				if (tries < 300 && (d < 3 || d > 5 || !Dungeon.level.heroFOV[k])) continue;
				cell = k;
			}
			if (cell == -1){
				CoopOrders.named("rest");
				return;
			}
			m.pos = cell;
			GameScene.add(m);
			m.state = m.HUNTING;
			m.beckon(me.pos);
			foeId = m.id();
			foeName = c.getSimpleName();
			duelTaken = 0;
			duelStart = Actor.now();
			CoopOrders.named("rest");
			return;
		}
		//the duel: out of marked cells, then at it
		Mob foe = (Mob) a;
		if (TargetedCell.cells.get(me.pos) != null){
			for (int n : PathFinder.NEIGHBOURS8){
				int c = me.pos + n;
				if (Dungeon.level.passable[c] && Actor.findChar(c) == null && TargetedCell.cells.get(c) == null){
					CoopOrders.handleCell(c);
					return;
				}
			}
		}
		if (me.HP < me.HT * 0.3f){
			PotionOfHealing p = me.belongings.getItem(PotionOfHealing.class);
			if (p != null){
				CoopOrders.execute(p, "DRINK");
				return;
			}
		}
		Mob target = foe;
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (m.alignment == Char.Alignment.ENEMY && m.isAlive() && Dungeon.level.adjacent(m.pos, me.pos)
					&& !(m instanceof SKProps.SKProp)){
				target = m;
				break;
			}
		}
		CoopOrders.handleCell(target.pos);
	}

	private static void endDuel( Hero me, String why ){
		CoopLog.line("BAL duel depth=" + foeDepth + " sk=" + SKWorlds.isSK(Dungeon.depth) + " mob=" + foeName + " end=" + why
				+ " taken=" + duelTaken + " HT=" + me.HT + " turns=" + (int) (Actor.now() - duelStart));
		duels++;
		foeId = -1;
	}

	private static void closeFloor( Hero me, String why ){
		if (floorDepth == -1) return;
		CoopLog.line("BAL floor depth=" + floorDepth + " sk=" + SKWorlds.isSK(floorDepth) + " end=" + why
				+ " turns=" + floorTurns
				+ " kills=" + (Statistics.enemiesSlain - floorKills)
				+ " taken=" + floorTaken + " HT=" + me.HT + " hp=" + me.HP
				+ " potions=" + floorPotions + " lvl=" + me.lvl);
		floorDepth = -1;
	}

	private static void finish( String why ){
		done = true;
		CoopLog.line("BAL done " + why);
		System.out.println("BAL done " + why);
		Game.instance.finish();
		new Thread(){
			@Override
			public void run() {
				try { Thread.sleep(1500); } catch (InterruptedException e) {}
				System.exit(0);
			}
		}.start();
	}

	private static int frontier( Hero me ){
		int best = -1, bestDist = Integer.MAX_VALUE;
		for (int i = 0; i < Dungeon.level.length(); i++){
			if (!Dungeon.level.passable[i] || !(Dungeon.level.visited[i] || Dungeon.level.mapped[i])) continue;
			if (Actor.findChar(i) != null) continue;
			boolean edge = false;
			for (int n : PathFinder.NEIGHBOURS8){
				int c = i + n;
				if (c >= 0 && c < Dungeon.level.length() && !Dungeon.level.visited[c] && !Dungeon.level.mapped[c] && !Dungeon.level.solid[c]){
					edge = true;
					break;
				}
			}
			if (!edge) continue;
			int d = Dungeon.level.distance(me.pos, i);
			if (d > 0 && d < bestDist){
				best = i;
				bestDist = d;
			}
		}
		return best;
	}

	private static int randomVisited( Hero me ){
		for (int tries = 0; tries < 200; tries++){
			int i = Random.Int(Dungeon.level.length());
			if (Dungeon.level.passable[i] && Dungeon.level.visited[i] && Actor.findChar(i) == null) return i;
		}
		return -1;
	}
}
