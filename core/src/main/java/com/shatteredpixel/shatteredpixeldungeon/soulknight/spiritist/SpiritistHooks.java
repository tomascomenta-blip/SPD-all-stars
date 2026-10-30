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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.spiritist;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Bee;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Piranha;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Statue;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Cudgel;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAlly;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKBoss;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

//spiritist hooks. passive "soul harvest" = small heal per nearby death
//also remembers where stuff died so Raise the Fallen has something to raise
public class SpiritistHooks {

	public static final int HARVEST_RANGE = 4;

	public static void initHero( Hero hero ){
		(hero.belongings.weapon = new Cudgel()).identify();
		hero.belongings.weapon.activate(hero);

		SpiritistKit kit = new SpiritistKit();
		kit.identify().collect();
		PlagueStaff staff = new PlagueStaff();
		staff.identify().collect();

		Dungeon.quickslot.setSlot(0, kit);
		Dungeon.quickslot.setSlot(1, staff);

		new PotionOfHealing().identify();
		new ScrollOfRecharging().identify();
	}

	public static void ensurePassives( Hero hero ){
		Buff.affect(hero, Graves.class);
	}

	public static boolean is( Char ch ){
		return ch instanceof Hero && ((Hero) ch).heroClass == HeroClass.SPIRITIST;
	}

	public static int harvest( Hero hero ){
		return 1 + hero.lvl/8 + hero.pointsInTalent(Talent.SP_SOUL_HARVEST);
	}

	public static void onMobDeath( Mob mob, Object cause ){
		boolean plagued = false;
		Blob plague = Dungeon.level.blobs.get(PlagueCloud.class);
		if (plague != null && plague.volume > 0 && plague.cur[mob.pos] > 0) plagued = true;
		boolean heraldDone = false;

		for (Hero h : Party.living()){
			if (h.heroClass != HeroClass.SPIRITIST) continue;
			Graves g = Buff.affect(h, Graves.class);
			g.add(mob);
			if (Dungeon.level.distance(h.pos, mob.pos) <= HARVEST_RANGE){
				ChampionHooks.heal(h, harvest(h));
				Souls.gain(h);
			}
			//plague herald: dies in the plague -> skeleton (once per death, or it got silly fast)
			if (plagued && !heraldDone && h.subClass == HeroSubClass.PLAGUE_HERALD && raisable(mob)){
				int cell = spawnCell(mob.pos, false);
				if (cell != -1){
					float m = 1f + 0.2f*h.pointsInTalent(Talent.PH_BONE_HARVEST);
					Skeleton.spawn(h, cell, 10, m);
					heraldDone = true;
				}
			}
		}

		//nightmare death releases its ghosts
		Nightmare.Mark mark = mob.buff(Nightmare.Mark.class);
		if (mark != null){
			Hero owner = ChampionHooks.heroByIndex(mark.owner);
			if (owner != null && owner.isAlive()){
				summonGhosts(owner, mob.pos, Nightmare.ghosts(owner), false);
			}
		}
	}

	//no bosses, no fixed/special stuff. raising a mimic was funny but no
	public static boolean raisable( Mob mob ){
		if (Char.hasProp(mob, Char.Property.BOSS) || Char.hasProp(mob, Char.Property.MINIBOSS)
				|| Char.hasProp(mob, Char.Property.BOSS_MINION) || Char.hasProp(mob, Char.Property.IMMOVABLE)){
			return false;
		}
		return !(mob instanceof Mimic || mob instanceof Statue || mob instanceof Piranha || mob instanceof Bee
				|| mob instanceof NPC || mob instanceof SKAlly || mob instanceof SKBoss);
	}

	//-1 = none
	public static int spawnCell( int center, boolean allowCenter ){
		if (allowCenter && free(center)) return center;
		for (int n : PathFinder.NEIGHBOURS8){
			int c = center + n;
			if (free(c)) return c;
		}
		for (int c : SKCombat.area(center, 2)){
			if (free(c)) return c;
		}
		return -1;
	}

	private static boolean free( int c ){
		return c >= 0 && c < Dungeon.level.length() && !Dungeon.level.solid[c] && Dungeon.level.passable[c]
				&& !Dungeon.level.pit[c] && Actor.findChar(c) == null;
	}

	public static int summonGhosts( Hero owner, int near, int count, boolean allowCenter ){
		int made = 0;
		for (int i = 0; i < count; i++){
			int cell = spawnCell(near, allowCenter);
			if (cell == -1) break;
			Ghost g = Ghost.make(owner, cell);
			GameScene.add(g);
			Dungeon.level.occupyCell(g);
			if (Dungeon.level.heroFOV[cell]) CellEmitter.get(cell).burst(ShadowParticle.UP, 6);
			made++;
		}
		return made;
	}

	//dark pact: dead summon feeds the owner
	public static void onSummonDeath( Hero owner ){
		if (owner != null && owner.isAlive() && owner.hasTalent(Talent.SP_DARK_PACT)){
			ChampionHooks.heal(owner, 2*owner.pointsInTalent(Talent.SP_DARK_PACT));
		}
	}

	public static void onFoodEaten( Hero hero ){
		if (!hero.hasTalent(Talent.SP_SOUL_FEAST)) return;
		PlagueStaff staff = hero.belongings.getItem(PlagueStaff.class);
		if (staff != null){
			staff.gainCharge(hero.pointsInTalent(Talent.SP_SOUL_FEAST), true);
			Item.updateQuickslot();
		}
	}

	public static float itemIDSpeedFactor( Hero hero, Item item ){
		if (hero.heroClass == HeroClass.SPIRITIST && (item instanceof Ring || item instanceof Wand)){
			return 1f + 0.75f*hero.pointsInTalent(Talent.SP_DEATH_SENSE);
		}
		return 1f;
	}

	public static class Graves extends Buff {

		{
			type = buffType.NEUTRAL;
			revivePersists = true;
		}

		public static final int MAX = 4;
		public static final float FRESH = 30f;

		public ArrayList<String> classes = new ArrayList<>();
		public ArrayList<Integer> cells = new ArrayList<>();
		public ArrayList<Float> times = new ArrayList<>();
		public int depth = -1;

		@Override
		public boolean act() {
			spend(TICK*10);
			return true;
		}

		public void add( Mob mob ){
			if (!raisable(mob)) return;
			if (depth != Dungeon.depth){
				classes.clear(); cells.clear(); times.clear();
				depth = Dungeon.depth;
			}
			classes.add(0, mob.getClass().getName());
			cells.add(0, mob.pos);
			times.add(0, Actor.now());
			while (classes.size() > MAX){
				classes.remove(MAX); cells.remove(MAX); times.remove(MAX);
			}
		}

		//newest first
		public ArrayList<Integer> near( Hero hero, int range ){
			ArrayList<Integer> found = new ArrayList<>();
			if (depth != Dungeon.depth) return found;
			for (int i = 0; i < classes.size(); i++){
				if (Actor.now() - times.get(i) <= FRESH && Dungeon.level.distance(hero.pos, cells.get(i)) <= range){
					found.add(i);
				}
			}
			return found;
		}

		public void remove( int i ){
			classes.remove(i); cells.remove(i); times.remove(i);
		}

		private static final String CLASSES = "classes";
		private static final String CELLS   = "cells";
		private static final String TIMES   = "times";
		private static final String DEPTH   = "depth";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(CLASSES, classes.toArray(new String[0]));
			int[] c = new int[cells.size()];
			float[] t = new float[times.size()];
			for (int i = 0; i < c.length; i++){ c[i] = cells.get(i); t[i] = times.get(i); }
			bundle.put(CELLS, c);
			bundle.put(TIMES, t);
			bundle.put(DEPTH, depth);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			classes.clear(); cells.clear(); times.clear();
			if (bundle.contains(CLASSES)){
				for (String s : bundle.getStringArray(CLASSES)) classes.add(s);
				for (int c : bundle.getIntArray(CELLS)) cells.add(c);
				for (float t : bundle.getFloatArray(TIMES)) times.add(t);
			}
			depth = bundle.getInt(DEPTH);
		}
	}
}
