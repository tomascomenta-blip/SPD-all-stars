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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.finale;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopOrders;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopSession;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.TargetedCell;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.Button;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.Game;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;
import java.util.Random;

//test bot for the rift: off the marked tiles first, then hourglasses > core > hands > portals > whatever's near.
//solo with -Dspd.test.thalbot=1, the coop bots use act() like for the other bosses
public class ThalBot {

	private static float timer = 0;
	private static float lastNow = -1;
	private static int stuck = 0;
	private static int lastHP = -1;
	private static float kitNext = 0;
	private static float allyLog = 0;
	//the bot's own whims, never the game's random
	private static final Random botRnd = new Random(7);

	public static boolean pickPortalWindow( Window w ){
		if (!(w instanceof ThalVekhorr.WndPortal)) return false;
		//-Dspd.test.thalvote=2: the bots cross to the continent instead
		int skip = Integer.getInteger("spd.test.thalvote", 1) - 1;
		for (Button b : CoopSession.buttonsOfWindow(w)){
			if (b instanceof RedButton && b.active && skip-- <= 0){
				CoopLog.line("bot thal votes: " + ((RedButton) b).text());
				if (!CoopSession.buttonClicked(b)) b.coopClick();
				return true;
			}
		}
		return false;
	}

	public static void tick( Hero me ){
		if (!"1".equals(System.getProperty("spd.test.thalbot"))) return;
		if (me == null || !me.isAlive()) return;
		timer -= Game.elapsed;
		if (timer > 0) return;
		timer = 0.1f;
		Window w = GameScene.topWindow();
		if (w != null){
			if (!pickPortalWindow(w)) w.hide();
			return;
		}
		if (GameScene.isSelectingCell() || !me.ready) return;
		if (!(Dungeon.level instanceof RiftLevel)){
			CoopOrders.named("rest");
			return;
		}
		float now = Actor.now();
		if (now == lastNow) stuck++; else { stuck = 0; lastNow = now; }
		if (stuck >= 6){
			stuck = 0;
			CoopOrders.named("rest");
			return;
		}
		ThalVekhorr t = ThalVekhorr.find();
		if (t != null && me.HP != lastHP){
			CoopLog.line("bot thal hero_hp " + lastHP + " -> " + me.HP + " t=" + (int) now + " thal=" + t.HP + "/" + t.HT
					+ " act=" + t.currentAct());
			lastHP = me.HP;
		}
		if (t != null && now >= allyLog){
			allyLog = now + 50;
			CoopLog.line("bot thal allies " + allies() + " t=" + (int) now + " act=" + t.currentAct());
		}
		if (t != null && useKit(me)) return;
		if (act(me)) return;
		Mob near = nearestFoe(me, 30);
		if (near != null){
			CoopOrders.handleCell(near.pos);
			return;
		}
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof ThalParts.ExitPortal){
				CoopOrders.handleCell(m.pos);
				return;
			}
		}
		CoopOrders.named("rest");
	}

	private static int allies(){
		int n = 0;
		for (Mob m : Dungeon.level.mobs){
			if (m.alignment == Char.Alignment.ALLY && m.isAlive()) n++;
		}
		return n;
	}

	//solo: the champion classes use their kit now and then, like a person would
	private static boolean useKit( Hero me ){
		ChampionKit kit = me.belongings.getItem(ChampionKit.class);
		//time went back (the rewind)
		if (kitNext > Actor.now() + 10) kitNext = 0;
		if (allyLog > Actor.now() + 60) allyLog = 0;
		if (kit == null || Actor.now() < kitNext) return false;
		ArrayList<ChampionAbility> ready = new ArrayList<>();
		for (ChampionAbility a : kit.abilities()){
			if (kit.canCast(me, a, false)) ready.add(a);
		}
		if (ready.isEmpty()) return false;
		kitNext = Actor.now() + 4;
		ChampionAbility a = ready.get(botRnd.nextInt(ready.size()));
		CoopLog.line("bot thal kit " + a.name() + " t=" + (int) Actor.now());
		kit.cast(me, a);
		if (GameScene.isSelectingCell()){
			Mob foe = nearestFoe(me, 8);
			GameScene.handleCell(foe != null ? foe.pos : me.pos);
		}
		return true;
	}

	private static Mob nearestFoe( Hero me, int range ){
		Mob best = null;
		int bd = Integer.MAX_VALUE;
		for (Mob m : Dungeon.level.mobs){
			if (m.alignment != Char.Alignment.ENEMY || m instanceof ThalVekhorr || !m.isAlive()) continue;
			int d = Dungeon.level.distance(me.pos, m.pos);
			if (d < bd && d <= range){
				bd = d;
				best = m;
			}
		}
		return best;
	}

	private static Mob nearest( Hero me, Class<? extends Mob> cls, int range ){
		Mob best = null;
		int bd = Integer.MAX_VALUE;
		for (Mob m : Dungeon.level.mobs){
			if (!cls.isInstance(m) || !m.isAlive()) continue;
			int d = Dungeon.level.distance(me.pos, m.pos);
			if (d < bd && d <= range){
				bd = d;
				best = m;
			}
		}
		return best;
	}

	public static boolean act( Hero me ){
		if (!(Dungeon.level instanceof RiftLevel)) return false;
		RiftLevel L = (RiftLevel) Dungeon.level;
		ThalVekhorr t = ThalVekhorr.find();
		if (t == null || t.dying() || t.currentAct() < 1) return false;

		if (me.HP < me.HT / 3){
			PotionOfHealing p = me.belongings.getItem(PotionOfHealing.class);
			if (p != null){
				CoopOrders.execute(p, "DRINK");
				return true;
			}
		}

		//off the marked tiles, away from slow bubbles and puddles if possible
		if (TargetedCell.cells.get(me.pos) != null){
			int best = -1, bestScore = Integer.MIN_VALUE;
			for (int n : PathFinder.NEIGHBOURS8){
				int c = me.pos + n;
				if (!L.passable[c] || Actor.findChar(c) != null || TargetedCell.cells.get(c) != null) continue;
				int score = 0;
				if (L.puddle(c)) score -= 2;
				if (L.bubbleAt(c) == 0) score -= 3;
				if (score > bestScore){
					bestScore = score;
					best = c;
				}
			}
			if (best != -1){
				CoopOrders.handleCell(best);
				return true;
			}
		}

		int idx = Party.active() ? Party.indexOf(me) : 0;
		Mob target = nearest(me, ThalParts.Hourglass.class, 30);
		if (target == null) target = nearest(me, ThalParts.Core.class, 30);
		if (target == null && idx % 2 == 0) target = nearest(me, ThalParts.Hand.class, 12);
		if (target == null) target = nearest(me, ThalParts.Portal.class, 30);
		if (target == null) target = nearest(me, ThalParts.Hand.class, 30);
		if (target != null){
			Mob close = nearestFoe(me, 1);
			//something chewing on us, deal with it first
			if (close != null && !(close instanceof ThalParts.Part) && Dungeon.level.distance(me.pos, target.pos) > 1){
				target = close;
			}
			CoopOrders.handleCell(target.pos);
			return true;
		}
		return false;
	}
}
