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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.quests;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopOrders;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopSession;
import com.shatteredpixel.shatteredpixeldungeon.effects.TargetedCell;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.props.SKProps;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.shatteredpixel.shatteredpixeldungeon.ui.Button;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;
import java.util.HashMap;

//TEST ONLY. -Dspd.test.questbot=hunt|caiman (+ -Dspd.test.hunt=1 or -Dspd.test.caiman=1)
//a lone bot that plays a whole side quest: talks to the giver, finds the quest mob, drops snares
//in its way, chases it when it runs, brings the trophy back, picks a reward.
//-Dspd.test.questpick=N picks reward N (0-2). writes "QUESTBOT" lines in the coop log
public class QuestBot {

	private static final String MODE = System.getProperty("spd.test.questbot");
	private static final int PICK = Integer.getInteger("spd.test.questpick", 0);
	private static final boolean RUNES = !"0".equals(System.getProperty("spd.test.questrunes"));
	private static final boolean FAST = "1".equals(System.getProperty("spd.test.questfast"));
	private static float timer = 2f;
	private static float lastNow = -1;
	private static int stuck = 0;
	private static int said = 0;
	private static boolean finished = false;
	private static int minHp = Integer.MAX_VALUE; //lowest hero hp while a quest mob fights

	public static boolean enabled(){
		return MODE != null;
	}

	private static final HashMap<Integer, Integer> heapTries = new HashMap<>();
	private static final boolean ALONE = "1".equals(System.getProperty("spd.test.questalone"));
	private static int clearedFor = -1;
	private static int aimCell = -1, aimBest = Integer.MAX_VALUE;
	private static float aimSince = 0;

	//walk to a cell (npc, heap, mob...), exploring toward it while it's unknown.
	//it's a test, not a pathfinding contest: no progress in 12 turns and the hero just pops up next to it. yes it's cheating
	private static void approach( Hero me, int cell ){
		int d = Dungeon.level.distance(me.pos, cell);
		if (cell != aimCell){
			aimCell = cell;
			aimBest = d;
			aimSince = Actor.now();
		} else if (d < aimBest){
			aimBest = d;
			aimSince = Actor.now();
		} else if (d > 1 && Actor.now() - aimSince > 12 && !CoopSession.active()){
			//next to it, or as close as there's room (big boss inside a block of ice, say)
			int w = Dungeon.level.width();
			for (int r = 1; r <= 3; r++){
				for (int dy = -r; dy <= r; dy++){
					for (int dx = -r; dx <= r; dx++){
						if (Math.max(Math.abs(dx), Math.abs(dy)) != r) continue;
						int c = cell + dx + dy * w;
						if (c >= 0 && c < Dungeon.level.length() && Dungeon.level.passable[c] && Actor.findChar(c) == null && !Dungeon.level.pit[c]){
							CoopLog.line("QUESTBOT can't get there, jumps next to " + cell);
							ScrollOfTeleportation.appear(me, c);
							Dungeon.observe();
							aimSince = Actor.now();
							aimBest = r;
							return;
						}
					}
				}
			}
		}
		if (d <= 1){
			CoopOrders.handleCell(cell);
			return;
		}
		boolean known = Dungeon.level.heroFOV[cell] || Dungeon.level.visited[cell] || Dungeon.level.mapped[cell];
		Char ch = Actor.findChar(cell);
		if (known && (ch == null || Dungeon.level.heroFOV[cell] || Dungeon.level.adjacent(me.pos, cell))){
			CoopOrders.handleCell(cell);
			return;
		}
		int near = knownNear(me, cell);
		if (near != -1 && near != me.pos){
			CoopOrders.handleCell(near);
			return;
		}
		int f = frontierTowards(me, cell);
		if (f != -1) CoopOrders.handleCell(f);
		else CoopOrders.named("search");
	}

	private static int knownNear( Hero me, int goal ){
		int best = -1, bestScore = Integer.MAX_VALUE;
		for (int i = 0; i < Dungeon.level.length(); i++){
			if (!Dungeon.level.passable[i] || !(Dungeon.level.visited[i] || Dungeon.level.mapped[i])) continue;
			if (Actor.findChar(i) != null && i != me.pos) continue;
			int dg = Dungeon.level.distance(i, goal);
			if (dg > 3) continue;
			int score = dg * 10 + Dungeon.level.distance(i, me.pos);
			if (score < bestScore){
				bestScore = score;
				best = i;
			}
		}
		return best;
	}

	private static int frontierTowards( Hero me, int goal ){
		int best = -1, bestScore = Integer.MAX_VALUE;
		for (int i = 0; i < Dungeon.level.length(); i++){
			if (!Dungeon.level.passable[i] || !(Dungeon.level.visited[i] || Dungeon.level.mapped[i]) || i == me.pos) continue;
			boolean edge = false;
			for (int n : PathFinder.NEIGHBOURS8){
				int c = i + n;
				if (c >= 0 && c < Dungeon.level.length() && !Dungeon.level.visited[c] && !Dungeon.level.mapped[c] && !Dungeon.level.solid[c]){
					edge = true;
					break;
				}
			}
			if (!edge) continue;
			int score = Dungeon.level.distance(i, goal) * 3 + Dungeon.level.distance(i, me.pos);
			if (score < bestScore){
				bestScore = score;
				best = i;
			}
		}
		return best;
	}

	private static QuestState quest(){
		switch (MODE){
			case "caiman": return SKQuests.CAIMAN;
			case "tourney": return SKQuests.TOURNEY;
			case "worm": return SKQuests.WORM;
			case "android": return SKQuests.ANDROID;
			case "lab": return SKQuests.LAB;
			case "golem": return SKQuests.GOLEM;
			case "yeti": return SKQuests.YETI;
			case "seals": return SKQuests.SEALS;
			default: return SKQuests.HUNT;
		}
	}

	private static boolean isGiver( Mob m ){
		return m instanceof HuntQuest.Hunter || m instanceof CaimanQuest.Sewerman || m instanceof TourneyQuest.Herald
				|| m instanceof WormQuest.DwarfMiner || m instanceof AndroidQuest.Terminal || m instanceof LabQuest.Scientist
				|| m instanceof GolemQuest.Treasurer || m instanceof YetiQuest.Huntress || m instanceof SealsQuest.LostSoul;
	}

	//time to go back to the giver (trophy, new round, reward)
	private static boolean backToGiver( QuestState q ){
		if (!q.given) return true;
		switch (MODE){
			case "tourney": return TourneyQuest.Q.duel == 0;
			case "android": return q.done;
			case "lab": return LabQuest.Q.sealed >= 3;
			case "yeti": return YetiQuest.Q.freed;
			case "seals": return SealsQuest.Q.broken >= 3;
			default: return q.slain;
		}
	}

	//what to go for now: quest mob, core, bulkhead, the boy, a seal
	private static Char goal( Hero me ){
		Char best = null;
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (!m.isAlive()) continue;
			Char c = null;
			if (m instanceof MiniBoss && !(m instanceof AndroidQuest.AndroidM4)) c = m;
			if (m instanceof AndroidQuest.Core) c = m;
			if (m instanceof LabQuest.Bulkhead && !((LabQuest.Bulkhead) m).sealed && !LabQuest.Q.failed) c = m;
			if (m instanceof YetiQuest.Captive && YetiQuest.Q.slain) c = m;
			if (m instanceof SealsQuest.DemonSeal) c = m;
			if (MODE.equals("tourney") && m.id() == TourneyQuest.Q.foe) c = m;
			//-Dspd.test.questbot=boss: straight to the floor boss (to check the perks and punishments)
			if (MODE.equals("boss") && (m.properties().contains(Char.Property.BOSS) || m instanceof MiniBoss)
					&& m.alignment == Char.Alignment.ENEMY && !m.isInvulnerable(Hero.class)) c = m;
			if (c != null && (best == null || Dungeon.level.distance(me.pos, c.pos) < Dungeon.level.distance(me.pos, best.pos))) best = c;
		}
		return best;
	}

	private static void say( String s ){
		if (said++ % 20 == 0) CoopLog.line("QUESTBOT " + s);
	}

	public static void tick( Hero me ){
		if (!enabled() || me == null || !me.isAlive()) return;
		if (MiniBoss.active() != null) minHp = Math.min(minHp, me.HP);
		//finished, but in coop the others still need this hero taking turns
		if (finished){
			if (CoopSession.active() && me.ready && GameScene.topWindow() == null){
				timer -= Game.elapsed;
				if (timer <= 0){
					timer = 0.3f;
					CoopOrders.named("search");
				}
			}
			return;
		}
		timer -= Game.elapsed;
		if (timer > 0) return;
		timer = 0.08f;

		Window w = GameScene.topWindow();
		if (w != null){
			if (w instanceof WndOptions){
				ArrayList<Button> bs = CoopSession.buttonsOfWindow(w);
				int n = 0;
				for (Button b : bs){
					if (b instanceof RedButton && n++ == PICK){
						CoopLog.line("QUESTBOT picks reward " + PICK + ": " + ((RedButton) b).text());
						if (!CoopSession.buttonClicked(b)) b.coopClick();
						return;
					}
				}
			}
			w.hide();
			return;
		}
		if (GameScene.isSelectingCell()){
			GameScene.handleCell(me.pos);
			return;
		}
		if (!me.ready) return;

		//-Dspd.test.questfast=1 (boss, solo): the minions holding the dwarf king's shield die as they come,
		//so you reach his last phase w/o a long grind
		if (FAST && MODE.equals("boss") && !CoopSession.active()){
			for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
				if (m.isAlive() && m.buff(DwarfKing.KingDamager.class) != null){
					CoopLog.line("QUESTBOT fast: a minion of the king falls");
					m.die(me);
					return;
				}
			}
		}

		//-Dspd.test.questalone=1 (solo only): no normal mobs, to measure the quest mob by itself
		if (ALONE && !CoopSession.active() && Dungeon.level != null && clearedFor != Dungeon.depth){
			clearedFor = Dungeon.depth;
			int n = 0;
			for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
				if (m.alignment != Char.Alignment.ENEMY || m instanceof MiniBoss) continue;
				if (m.properties().contains(Char.Property.BOSS) || m.properties().contains(Char.Property.IMMOVABLE)) continue;
				m.destroy();
				if (m.sprite != null) m.sprite.killAndErase();
				n++;
			}
			CoopLog.line("QUESTBOT cleared " + n + " monsters");
		}

		QuestState q = quest();
		if (!MODE.equals("boss") && q.done && q.claimedBy(me)){
			finished = true;
			CoopLog.line("QUESTBOT done: quest finished, bonus=" + q.bonus + " hp=" + me.HP + "/" + me.HT + " lowest in the fight=" + minHp);
			System.out.println("QUESTBOT done");
			return;
		}
		if (MODE.equals("lab") && LabQuest.Q.failed && Dungeon.depth == LabQuest.Q.depth && goal(me) == null){
			finished = true;
			CoopLog.line("QUESTBOT done: the bulkheads burst and the ammonite is dead, hp=" + me.HP + "/" + me.HT);
			System.out.println("QUESTBOT done");
			return;
		}
		if (MODE.equals("android") && AndroidQuest.Q.awake && Dungeon.depth != AndroidQuest.Q.depth){
			finished = true;
			CoopLog.line("QUESTBOT done: escaped through the hatch to depth " + Dungeon.depth + " hp=" + me.HP + "/" + me.HT);
			System.out.println("QUESTBOT done");
			return;
		}

		float now = Actor.now();
		if (now == lastNow) stuck++; else { stuck = 0; lastNow = now; }
		if (stuck >= 5){
			stuck = 0;
			CoopOrders.named("rest");
			return;
		}

		//get off marked cells, primero
		if (TargetedCell.cells.get(me.pos) != null){
			for (int n : PathFinder.NEIGHBOURS8){
				int c = me.pos + n;
				if (Dungeon.level.passable[c] && Actor.findChar(c) == null && TargetedCell.cells.get(c) == null){
					say("dodge");
					CoopOrders.handleCell(c);
					return;
				}
			}
		}

		Mob giver = null;
		Char boss = goal(me);
		Mob closest = null;
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (isGiver(m) && !(m instanceof HuntQuest.Hunter && ((HuntQuest.Hunter) m).helper)) giver = m;
			if (m.alignment == Char.Alignment.ENEMY && Dungeon.level.heroFOV[m.pos] && m != boss && !(m instanceof MiniBoss)
					&& !(m instanceof SKProps.SKProp) && !m.isInvulnerable(Hero.class)){
				if (closest == null || Dungeon.level.distance(me.pos, m.pos) < Dungeon.level.distance(me.pos, closest.pos)) closest = m;
			}
		}

		Hunger hunger = me.buff(Hunger.class);
		if (hunger != null && hunger.hunger() >= Hunger.HUNGRY){
			Food food = me.belongings.getItem(Food.class);
			if (food != null){
				say("eat");
				CoopOrders.execute(food, Food.AC_EAT);
				return;
			}
			//a test, not a survival game. no hunger (solo only, ojo, in coop this would desync)
			if (!CoopSession.active()) hunger.satisfy(Hunger.STARVING);
		}

		if (me.HP < me.HT * 0.45f){
			PotionOfHealing p = me.belongings.getItem(PotionOfHealing.class);
			if (p != null){
				CoopLog.line("QUESTBOT drinks at hp=" + me.HP + "/" + me.HT + ", " + (p.quantity() - 1) + " left");
				CoopOrders.execute(p, "DRINK");
				return;
			}
		}

		//coop + no progress for a while (no teleporting there): something's in the way or an ambush shut the doors.
		//clear whatever's in sight
		if (closest != null && CoopSession.active() && aimCell != -1 && Actor.now() - aimSince > 12){
			say("stuck, fights " + closest.getClass().getSimpleName());
			CoopOrders.handleCell(closest.pos);
			return;
		}

		//M-4 woke up, RUN for the hatch
		if (MODE.equals("android") && AndroidQuest.Q.awake && AndroidQuest.Q.hatch != -1){
			say("to the hatch " + AndroidQuest.Q.hatch + " d=" + Dungeon.level.distance(me.pos, AndroidQuest.Q.hatch));
			approach(me, AndroidQuest.Q.hatch);
			return;
		}

		//quest mob's trophy first
		if (q.slain && !q.done){
			for (Heap h : Dungeon.level.heaps.valueList()){
				for (Item it : h.items){
					if (it instanceof HuntQuest.GreyTuskFang || it instanceof CaimanQuest.CaimanTooth
							|| it instanceof WormQuest.WormTooth || it instanceof GolemQuest.GolemCore){
						say("trophy at " + h.pos);
						approach(me, h.pos);
						return;
					}
				}
			}
		}

		//hit the quest mob if it's right there and hittable (the worm is only up for a sec)
		if (boss instanceof Mob && boss.alignment == Char.Alignment.ENEMY && boss.invisible <= 0
				&& Dungeon.level.heroFOV[boss.pos] && Dungeon.level.distance(me.pos, boss.pos) <= 2){
			CoopOrders.handleCell(boss.pos);
			return;
		}

		//whatever's in teh way first (on the way back to the giver too)
		if (closest != null && Dungeon.level.adjacent(me.pos, closest.pos)){
			CoopOrders.handleCell(closest.pos);
			return;
		}

		//1: talk to the giver. 3: bring back the trophy
		if (giver != null && !MODE.equals("boss") && backToGiver(q)){
			say("to the giver " + giver.pos);
			approach(me, giver.pos);
			return;
		}

		//golem runes before the golem (-Dspd.test.questrunes=0 leaves them lit)
		if (MODE.equals("golem") && RUNES && q.given && !q.slain && GolemQuest.Q.depth == Dungeon.depth){
			int rune = -1;
			for (int i = 0; i < GolemQuest.Q.runes.length; i++){
				int c = GolemQuest.Q.runes[i];
				if (!GolemQuest.Q.lit[i] || (Actor.findChar(c) != null && c != me.pos)) continue;
				if (rune == -1 || Dungeon.level.distance(me.pos, c) < Dungeon.level.distance(me.pos, rune)) rune = c;
			}
			if (rune == me.pos){
				CoopOrders.named("search");
				return;
			}
			if (rune != -1){
				say("rune at " + rune);
				approach(me, rune);
				return;
			}
		}

		//hunter snares lying around (unless one's been tried for too long)
		for (Heap h : Dungeon.level.heaps.valueList()){
			if (h.pos == me.pos || !Dungeon.level.heroFOV[h.pos] || Dungeon.level.distance(me.pos, h.pos) > 7) continue;
			if (!(h.peek() instanceof HuntQuest.HunterTrap)) continue;
			int n = heapTries.containsKey(h.pos) ? heapTries.get(h.pos) : 0;
			if (n > 30) continue;
			heapTries.put(h.pos, n + 1);
			say("snare at " + h.pos);
			CoopOrders.handleCell(h.pos);
			return;
		}

		//2: the quest mob (or core, bulkhead, boy, seal)
		if (boss != null){
			int d = Dungeon.level.distance(me.pos, boss.pos);
			if (boss instanceof GreyTusk){
				GreyTusk t = (GreyTusk) boss;
				HuntQuest.HunterTrap trap = me.belongings.getItem(HuntQuest.HunterTrap.class);
				if (trap != null && !t.fleeing && d >= 2 && d <= 6 && Dungeon.level.heroFOV[boss.pos]
						&& !HuntQuest.HuntField.armed(me.pos) && !Dungeon.level.water[me.pos] && Dungeon.level.getTransition(me.pos) == null){
					CoopLog.line("QUESTBOT sets a snare at " + me.pos + " (tusk at " + boss.pos + ")");
					CoopOrders.execute(trap, HuntQuest.HunterTrap.AC_SET);
					return;
				}
			}
			if (boss instanceof Caiman && ((Caiman) boss).submerged && d <= 3){
				//wait on dry floor, let it come out. paciencia
				int dry = -1;
				for (int i = 0; i < Dungeon.level.length(); i++){
					if (Dungeon.level.passable[i] && !Dungeon.level.water[i] && Actor.findChar(i) == null
							&& Dungeon.level.distance(i, boss.pos) >= 3 && Dungeon.level.distance(i, boss.pos) <= 5
							&& (dry == -1 || Dungeon.level.distance(me.pos, i) < Dungeon.level.distance(me.pos, dry))) dry = i;
				}
				if (dry != -1 && dry != me.pos && Dungeon.level.water[me.pos]){
					say("to dry floor " + dry);
					CoopOrders.handleCell(dry);
					return;
				}
			}
			say("goal " + boss.getClass().getSimpleName() + "@" + boss.pos + " d=" + d + " hp=" + boss.HP + "/" + boss.HT + " me=" + me.HP + "/" + me.HT);
			approach(me, boss.pos);
			return;
		}

		if (closest != null && Dungeon.level.distance(me.pos, closest.pos) <= 5){
			CoopOrders.handleCell(closest.pos);
			return;
		}

		//boss mode: most bosses only show up once you walk into their hall at the far end
		if (MODE.equals("boss") && Dungeon.level.exit() >= 0 && Dungeon.level.distance(me.pos, Dungeon.level.exit()) > 3){
			say("to the hall of the boss");
			approach(me, Dungeon.level.exit());
			return;
		}
		CoopOrders.named("search");
	}
}
