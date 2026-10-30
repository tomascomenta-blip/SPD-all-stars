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

import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopOrders;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopSession;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ColossusPreview;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.AshColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.fauna.BronzeGuard;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.flesh.fauna.RootElk;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.frost.fauna.IceElk;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.survival.ColossusMeat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.survival.SacredTorch;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.survival.Survival;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.Game;
import com.watabou.utils.PathFinder;

//testing only (-Dspd.test.tametest=1 with colpreview and fauna=elk,mammoth,hare...): tames the beast that
//lets itself be tamed (the party brings what each one asks for), eats every part in the bag, walks off to see it
//follow, watches it fight, scare the lesser things and shelter the hero, then picks up the part of the other
//big one once it's dead and eats it. in co-op the first hero tames and the second hunts.
//-Dspd.test.huntparts=1 fills the bag with parts, =2 lays them on the ground round the hero
public class BiomeHuntTest {

	public static boolean on(){
		return DevFlags.tameTest() && ColossusPreview.on();
	}

	private static final int TAME = 0, EAT = 1, WALK = 2, WATCH = 3, HUNT = 4, DONE = 5;

	private static int step = TAME, turns = 0, away = -1;
	private static boolean prepared = false, laid = false;
	private static float timer = 2f;
	private static int followed = 0, fought = 0, scared = 0, sheltered = 0, foes = 0;
	private static AshColossus target;

	//every hero on every device as they come through the portal (Survival.onArrive), so it's the same everywhere
	public static void stock( Hero me ){
		//-Dspd.test.duel=2: the hero only watches, out of everyone's sight
		if (DevFlags.duel() == 2 && ColossusPreview.on()) Buff.affect(me, Invisibility.class, 100000f);
		//-Dspd.test.tamed=1: the first one there that can be tamed is already the party's, to try it out by hand
		if (DevFlags.startTamed() && ColossusPreview.on() && Hunt.tamedHere() == null){
			for (Mob m : Dungeon.level.mobs){
				if (m instanceof AshColossus && ((AshColossus) m).tameable()){
					((AshColossus) m).tame(me);
					break;
				}
			}
		}
		//what the tamings ask for goes in first: thirty parts don't fit in a bag, the rest is left at the feet
		if (on()){
			Survival.give(me, new LightCollar());
			Survival.give(me, new SacredTorch());
			Survival.give(me, new ColossusMeat.Raw().quantity(2));
		}
		if (DevFlags.huntParts() == 1){
			for (int k = BiomeParts.FIRST; k < Mutations.COUNT; k++) Survival.give(me, new MutationPart(k));
			CoopLog.line("tame test: hero " + Party.indexOf(me) + " gets " + (Mutations.COUNT - BiomeParts.FIRST) + " parts");
		} else if (DevFlags.huntParts() == 2 && !laid){
			laid = true;
			int w = Dungeon.level.width(), k = BiomeParts.FIRST;
			for (int y = -3; y <= 3 && k < Mutations.COUNT; y++){
				for (int x = -3; x <= 3 && k < Mutations.COUNT; x++){
					int c = me.pos + x + 1 + y * w;
					if (c == me.pos || !Dungeon.level.passable[c]) continue;
					Dungeon.level.drop(new MutationPart(k++), c);
				}
			}
		}
		if (!on()) return;
		if (!prepared) prepare();
		//the root elk only takes it from someone it never saw coming: the tamer walks up unseen
		if (tamer(me)){
			for (Mob m : Dungeon.level.mobs){
				if (m instanceof RootElk) Buff.affect(me, Invisibility.class, 40f);
			}
		}
	}

	//what each tameable one asks for that a bag can't hold, the others weak enough to hunt quickly, and
	//held still for a while so the fight doesn't give the hero away before the collar is on
	private static void prepare(){
		prepared = true;
		//the plain is known, so the hero can walk up to them in the dark
		for (int i = 0; i < Dungeon.level.length(); i++){
			if (!Dungeon.level.solid[i]) Dungeon.level.mapped[i] = true;
		}
		for (Mob m : Dungeon.level.mobs){
			AshColossus c = m instanceof AshColossus ? (AshColossus) m : null;
			if (c != null && c.tameable()){
				if (c instanceof BronzeGuard) c.HP = c.HT / 3;
				if (c instanceof IceElk) c.down = 300;
			} else {
				if (c != null && c.size >= 5) c.HP = Math.max(1, c.HT / 40);
				Buff.affect(m, Paralysis.class, 25f);
				foes++;
			}
			CoopLog.line("tame test: " + m.getClass().getSimpleName() + " hp " + m.HP + "/" + m.HT + " tameable=" + (c != null && c.tameable()));
		}
	}

	private static boolean tamer( Hero me ){
		return !Party.active() || Party.indexOf(me) == 0;
	}

	private static boolean hunter( Hero me ){
		return !Party.active() || Party.indexOf(me) == 1;
	}

	public static void solo( Hero me ){
		if (Party.active() || me == null || !me.isAlive() || !me.ready) return;
		if ((timer -= Game.elapsed) > 0) return;
		timer = 0.15f;
		me.HP = Math.max(me.HP, me.HT / 2);
		Window w = GameScene.topWindow();
		if (w != null){
			answer(w);
			return;
		}
		if (GameScene.isSelectingCell()){
			GameScene.handleCell(aimFor(me));
			return;
		}
		act(me);
	}

	//co-op bots: the test keeps the turn even once it's done (the usual bot would head for the stairs)
	public static boolean coop( Hero me ){
		act(me);
		return true;
	}

	public static int aimFor( Hero me ){
		return target == null ? me.pos : body(me, target);
	}

	public static boolean answer( Window w ){
		if (!CoopSession.windowBack(w)) w.onBackPressed();
		return true;
	}

	private static int body( Hero me, AshColossus c ){
		for (int n : PathFinder.NEIGHBOURS8){
			if (c.covers(me.pos + n)) return me.pos + n;
		}
		return c.pos;
	}

	private static AshColossus tameable(){
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof AshColossus && m.isAlive() && ((AshColossus) m).tameable() && !((AshColossus) m).tamed) return (AshColossus) m;
		}
		return null;
	}

	private static AshColossus prey(){
		AshColossus best = null;
		for (Mob m : Dungeon.level.mobs){
			if (!(m instanceof AshColossus) || !m.isAlive()) continue;
			AshColossus c = (AshColossus) m;
			if (c.tamed || c.tameable() || c.size < 5 || c.alignment != Char.Alignment.ENEMY) continue;
			if (best == null || c.size > best.size) best = c;
		}
		return best;
	}

	private static Heap partsLeft( Hero me ){
		Heap best = null;
		for (Heap h : Dungeon.level.heaps.valueList()){
			if (h.peek() instanceof MutationPart && (best == null || Dungeon.level.distance(me.pos, h.pos) < Dungeon.level.distance(me.pos, best.pos))){
				best = h;
			}
		}
		return best;
	}

	private static void next( Hero me, String what ){
		CoopLog.line("tame test: hero " + Party.indexOf(me) + " step " + step + " done (" + what + ") t=" + (int) Actor.now());
		step++;
		turns = 0;
		target = null;
	}

	private static void act( Hero me ){
		turns++;
		switch (step){
			case EAT: {
				MutationPart p = me.belongings.getItem(MutationPart.class);
				if (p != null && turns < 600){
					CoopOrders.execute(p, Food.AC_EAT);
					return;
				}
				//the ones that didn't fit were left where the hero arrived
				Heap left = partsLeft(me);
				if (left != null && turns < 600){
					CoopOrders.handleCell(left.pos);
					return;
				}
				Mutations m = me.buff(Mutations.class);
				next(me, "ate, " + Mutations.count(me) + " mutations, mask " + (m == null ? 0 : Long.toHexString(m.mask))
						+ ", str " + me.STR + " ht " + me.HT + " stealth " + me.stealth() + " sight " + Mutations.sight(me));
				if (!Party.active() && m != null) probe(me);
				return;
			}
			case TAME: {
				if (!tamer(me)){
					next(me, "not the tamer");
					return;
				}
				target = tameable();
				if (target == null || turns > 120){
					next(me, target == null ? "nothing to tame" : "gave up");
					return;
				}
				if (target.reach(me.pos) > 1){
					CoopOrders.handleCell(target.approachCell(me.pos));
					return;
				}
				String no = target.tameBlock(me);
				if (no != null){
					CoopLog.line("tame test: " + target.getClass().getSimpleName() + " won't yet: " + no);
					CoopOrders.named("rest");
					return;
				}
				LightCollar collar = me.belongings.getItem(LightCollar.class);
				if (collar == null){
					next(me, "no collar");
					return;
				}
				CoopOrders.execute(collar, LightCollar.AC_TAME);
				return;
			}
			case HUNT: {
				if (!hunter(me)){
					next(me, "not the hunter");
					return;
				}
				if (turns > 200){
					next(me, "gave up");
					return;
				}
				MutationPart p = me.belongings.getItem(MutationPart.class);
				if (p != null){
					CoopLog.line("tame test: hero " + Party.indexOf(me) + " eats the part it hunted, " + p.kind);
					CoopOrders.execute(p, Food.AC_EAT);
					next(me, "hunted and ate part " + p.kind);
					return;
				}
				AshColossus c = prey();
				if (c != null){
					target = c;
					if (c.reach(me.pos) > 1){
						int to = c.approachCell(me.pos);
						if (turns % 20 == 1) CoopLog.line("tame test: hunter at " + me.pos + " goes for " + c.getClass().getSimpleName() + " by " + to);
						CoopOrders.handleCell(to);
					} else {
						//what it can't see it can't hit: the bot waits for it to come into view
						int cell = body(me, c);
						if (me.fieldOfView != null && me.fieldOfView[cell]) CoopOrders.handleCell(cell);
						else CoopOrders.named("rest");
					}
					return;
				}
				//dead: its part is lying somewhere about
				for (Heap h : Dungeon.level.heaps.valueList()){
					if (h.peek() instanceof MutationPart){
						CoopOrders.handleCell(h.pos);
						return;
					}
				}
				next(me, "nothing left to hunt");
				return;
			}
			case WALK: {
				AshColossus t = Hunt.tamedHere();
				if (t == null){
					next(me, "no tamed one");
					return;
				}
				if (away == -1){
					//in co-op over to the other hero (alone in the dark, the tether would bring the stalkers)
					away = Party.active() ? partner(me) : spotAway(me, t);
					CoopLog.line("tame test: walks off to " + away + " from " + me.pos);
				}
				if (away == -1 || me.pos == away || turns > 30){
					next(me, "walked off, the tamed one is " + t.reach(me.pos) + " away");
					return;
				}
				CoopOrders.handleCell(away);
				return;
			}
			case WATCH: {
				AshColossus t = Hunt.tamedHere();
				if (t == null || turns > 60){
					int left = 0;
					for (Mob m : Dungeon.level.mobs) if (m != t && m.isAlive() && m.alignment == Char.Alignment.ENEMY) left++;
					next(me, "watched: followed " + followed + " fought " + fought + " scared " + scared + " sheltered " + sheltered
							+ ", " + left + " of " + foes + " foes left" + (t == null ? " (no tamed one)" : ""));
					return;
				}
				if (t.reach(me.pos) <= t.half() + 5) followed++;
				if (t.state == t.HUNTING) fought++;
				if (Hunt.sheltered(me)) sheltered++;
				int afraid = 0;
				for (Mob m : Dungeon.level.mobs){
					Terror tr = m.buff(Terror.class);
					if (tr != null && tr.object == t.id()) afraid++;
				}
				if (afraid > 0) scared++;
				if (turns % 5 == 0){
					CoopLog.line("tame test: " + t.getClass().getSimpleName() + " at " + t.reach(me.pos) + " from hero, state "
							+ t.state.getClass().getSimpleName() + ", hp " + t.HP + ", " + afraid + " afraid of it, sheltered " + Hunt.sheltered(me));
					for (Mob m : Dungeon.level.mobs){
						if (m == t || !m.isAlive()) continue;
						CoopLog.line("tame test:   " + m.getClass().getSimpleName() + " hp " + m.HP + " at " + t.reach(m.pos) + " from it, "
								+ m.state.getClass().getSimpleName() + (m.paralysed > 0 ? " paralysed" : "") + (m.buff(Terror.class) != null ? " afraid" : ""));
					}
				}
				CoopOrders.named("rest");
				return;
			}
			default:
				CoopOrders.named("rest");
		}
	}

	//alone: what the hooks give with every part in, without having to get hit for it (the god switch skips it)
	private static void probe( Hero me ){
		CoopLog.line("tame test: probe evasion x" + Mutations.evasion(me) + " armor " + Mutations.armor(me)
				+ " taint 10 -> " + Mutations.taint(me, 10) + " immune poison " + me.isImmune(Poison.class)
				+ " chill " + me.isImmune(Chill.class) + " blind " + me.isImmune(Blindness.class)
				+ " vertigo " + me.isImmune(Vertigo.class) + " roots " + me.isImmune(Roots.class));
		int hp = me.HP;
		CoopLog.line("tame test: probe a blow of " + (hp + 40) + " -> " + Mutations.guard(me, hp + 40, null)
				+ ", again -> " + Mutations.guard(me, hp + 40, null) + ", a blow of 20 -> " + Mutations.guard(me, 20, null));
		float low = hp - Math.max(1, me.HT / 5);
		CoopLog.line("tame test: probe a blow of " + low + " (leaves a fifth) -> " + Mutations.guard(me, low, null));
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof AshColossus && ((AshColossus) m).size >= 5 && ((AshColossus) m).reach(me.pos) <= 8){
				CoopLog.line("tame test: probe bones out of " + m.getClass().getSimpleName() + " nearby: +" + Mutations.extraBones((AshColossus) m));
			}
		}
	}

	//a free cell next to the other hero
	private static int partner( Hero me ){
		for (Hero h : Party.living()){
			if (h == me) continue;
			for (int n : PathFinder.NEIGHBOURS8){
				int c = h.pos + n;
				if (Dungeon.level.passable[c] && Actor.findChar(c) == null) return c;
			}
		}
		return -1;
	}

	//an open cell some way off, on the far side from the tamed one
	private static int spotAway( Hero me, AshColossus t ){
		int w = Dungeon.level.width();
		int dx = Integer.signum(me.pos % w - t.pos % w), dy = Integer.signum(me.pos / w - t.pos / w);
		if (dx == 0 && dy == 0) dx = 1;
		for (int d = 10; d >= 4; d--){
			int c = me.pos + dx * d + dy * d * w;
			if (c > 0 && c < Dungeon.level.length() && Dungeon.level.passable[c] && Actor.findChar(c) == null) return c;
		}
		return -1;
	}
}
