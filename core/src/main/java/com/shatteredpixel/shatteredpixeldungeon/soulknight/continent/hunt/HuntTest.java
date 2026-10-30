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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopOrders;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopSession;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.AshColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.VolcanoTurtle;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.survival.ColossusBone;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.survival.ColossusMeat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.survival.Survival;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.Game;
import com.watabou.utils.PathFinder;

//testing only (-Dspd.test.hunt=1 with colpreview and fauna=turtle,pyre...): the hero goes through
//the hunt by itself. eats two parts, tames the turtle, climbs the biggest one and hits it from up
//there until it falls off, jumps down, then hits whatever is left until it goes down, and forges
public class HuntTest {

	public static boolean on(){
		return DevFlags.hunt();
	}

	private static float timer = 2f;
	private static int step = 0, turns = 0;

	public static void solo( Hero me ){
		if (BiomeHuntTest.on()){
			BiomeHuntTest.solo(me);
			return;
		}
		if (!on() || Party.active() || me == null || !me.isAlive() || !me.ready) return;
		if ((timer -= Game.elapsed) > 0) return;
		timer = 0.2f;
		//it's here to see the hunt, not to die of it (alone only: in co-op that's -Dspd.test.god)
		me.HP = me.HT;
		Window w = GameScene.topWindow();
		if (w != null){
			answer(w);
			return;
		}
		if (GameScene.isSelectingCell()){
			GameScene.handleCell(aim(me));
			return;
		}
		act(me);
	}

	//co-op bots (CoopAuto): true when the hunt took this turn
	public static boolean coop( Hero me ){
		if (BiomeHuntTest.on()) return BiomeHuntTest.coop(me);
		if (!on() || step > 7) return false;
		act(me);
		return true;
	}

	//where the bot taps while the grapple or the collar waits for a cell, -1 when it isn't ours
	public static int aimFor( Hero me ){
		if (BiomeHuntTest.on()) return BiomeHuntTest.aimFor(me);
		return on() && target != null ? aim(me) : -1;
	}

	//every hero, on every device, as they come through the portal (Survival.onArrive)
	public static void stock( Hero me ){
		BiomeHuntTest.stock(me);
		if (!on()) return;
		Survival.give(me, new BoneGrapple());
		Survival.give(me, new LightCollar().quantity(2));
		Survival.give(me, new ColossusMeat.Roast().quantity(2));
		Survival.give(me, new ColossusBone().quantity(8));
		Survival.give(me, new MutationPart(Mutations.CHIMNEY));
		Survival.give(me, new MutationPart(Mutations.FIRE_GLAND));
		Continent.essence += 2;
		CoopLog.line("hunt bot: stocked");
	}

	private static AshColossus target;

	private static int aim( Hero me ){
		return target == null ? me.pos : body(me, target);
	}

	//a cell of its body right next to the hero, the way a player taps it
	private static int body( Hero me, AshColossus c ){
		for (int n : PathFinder.NEIGHBOURS8){
			if (c.covers(me.pos + n)) return me.pos + n;
		}
		return c.pos;
	}

	private static AshColossus find( Class<? extends AshColossus> kind, boolean big ){
		AshColossus best = null;
		for (Mob m : Dungeon.level.mobs){
			if (!(m instanceof AshColossus) || !m.isAlive() || ((AshColossus) m).tamed) continue;
			if (kind != null && !kind.isInstance(m)) continue;
			if (big && ((AshColossus) m).size < Climbing.SIZE) continue;
			if (best == null || ((AshColossus) m).size > best.size) best = (AshColossus) m;
		}
		return best;
	}

	private static void act( Hero me ){
		turns++;
		switch (step){
			case 0: case 1: {
				MutationPart p = me.belongings.getItem(MutationPart.class);
				if (p == null){
					next("no parts");
					return;
				}
				CoopLog.line("hunt bot: eats part " + p.kind);
				CoopOrders.execute(p, Food.AC_EAT);
				next("ate");
				return;
			}
			case 2: {
				target = find(VolcanoTurtle.class, false);
				if (target == null){
					next("no turtle");
					return;
				}
				if (target.reach(me.pos) > 1){
					CoopOrders.handleCell(target.approachCell(me.pos));
					return;
				}
				LightCollar c = me.belongings.getItem(LightCollar.class);
				CoopOrders.execute(c, LightCollar.AC_TAME);
				next("collar on the turtle, tamed=" + target.tamed);
				return;
			}
			case 3: {
				target = find(null, true);
				if (target == null){
					next("nothing to climb");
					return;
				}
				if (me.buff(Climbing.class) != null){
					next("up on " + target.getClass().getSimpleName());
					return;
				}
				if (target.reach(me.pos) > 1){
					CoopOrders.handleCell(target.approachCell(me.pos));
					return;
				}
				CoopOrders.execute(me.belongings.getItem(BoneGrapple.class), BoneGrapple.AC_CLIMB);
				return;
			}
			case 4: {
				Climbing b = me.buff(Climbing.class);
				if (b == null || turns > 40){
					next("off its back, grip " + (b == null ? 0 : (int) b.grip) + " hp " + (target == null ? 0 : target.HP));
					return;
				}
				CoopLog.line("hunt bot: hits from its back, grip " + (int) b.grip + " its hp " + target.HP + " down " + target.down);
				CoopOrders.handleCell(target.pos);
				return;
			}
			case 5: {
				if (me.buff(Climbing.class) != null){
					//jump down to the side
					CoopOrders.handleCell(target.left() - 2 + (target.top() + target.size / 2) * Dungeon.level.width());
					return;
				}
				next("down on the ground");
				return;
			}
			case 6: {
				if (target == null || !target.isAlive() || turns > 60){
					next("stops hitting");
					return;
				}
				if (target.reach(me.pos) > 1){
					CoopOrders.handleCell(target.approachCell(me.pos));
					return;
				}
				CoopLog.line("hunt bot: hits from the ground, its hp " + target.HP + " down " + target.down + " stagger " + (int) target.stagger);
				CoopOrders.handleCell(body(me, target));
				return;
			}
			case 7: {
				ColossusForge f = null;
				for (Mob m : Dungeon.level.mobs) if (m instanceof ColossusForge) f = (ColossusForge) m;
				if (f == null){
					//alone it can just be put there; in co-op it has to come from a shrine like in a game
					if (Party.active()){
						next("no forge");
						return;
					}
					ColossusForge.raise(me.pos);
					return;
				}
				if (Dungeon.level.distance(me.pos, f.pos) > 1){
					CoopOrders.handleCell(f.pos);
					return;
				}
				CoopOrders.handleCell(f.pos);
				next("at the forge");
				return;
			}
			default:
				CoopOrders.named("rest");
		}
	}

	public static boolean answer( Window w ){
		if (BiomeHuntTest.on()) return BiomeHuntTest.answer(w);
		if (!on()) return false;
		if (w instanceof ColossusForge.WndForge){
			for (int i = 0; i < 2; i++){
				RedButton b = ((ColossusForge.WndForge) w).button(i);
				if (b != null && b.active){
					CoopLog.line("hunt bot: forges " + i);
					if (!CoopSession.buttonClicked(b)) b.coopClick();
					return true;
				}
			}
		}
		if (!CoopSession.windowBack(w)) w.onBackPressed();
		return true;
	}

	private static void next( String what ){
		CoopLog.line("hunt bot: step " + step + " done (" + what + ") t=" + (int) Actor.now());
		step++;
		turns = 0;
	}
}
