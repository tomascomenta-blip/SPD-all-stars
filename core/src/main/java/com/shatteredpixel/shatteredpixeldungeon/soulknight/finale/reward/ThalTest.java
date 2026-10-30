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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.finale.reward;

import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.RipperDemon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Succubus;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopOrders;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopSession;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfExperience;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.Button;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndHero;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

//testing only. -Dspd.test.thalrewards=1 hands out the loot as the run starts; with thalwalk=1 a solo
//run walks through the heart, the chest and the clock by itself, and the co-op bots use them too.
//-Dspd.test.thalpath=0..2 / thalgear=0..2 pick what the solo walkthrough chooses
public class ThalTest {

	public static boolean enabled(){
		return DevFlags.thalRewards();
	}

	//host (or the solo hero) before the run is shared, same place the other test kits are handed out
	public static void prepare( List<Hero> heroes ){
		if (!enabled()) return;
		for (final Hero h : heroes){
			ThalRewards.grant(h);
			if (DevFlags.thalClock()){
				MeltedClock clock = h.belongings.getItem(MeltedClock.class);
				if (clock != null) clock.fill();
			}
			final int levels = DevFlags.thalExp();
			if (levels > 0){
				Party.withHero(h, new Runnable() {
					@Override
					public void run() {
						for (int i = 0; i < levels; i++) h.earnExp(h.maxExp(), PotionOfExperience.class);
						//and one more level for later, in the middle of the run
						new PotionOfExperience().identify().collect(h.belongings.backpack);
					}
				});
			}
			CoopLog.line("thal test hero " + h.heroClass + " lvl=" + h.lvl + " cap=" + h.lvlCap + " hp=" + h.HP + "/" + h.HT);
		}
	}

	public static boolean isChoiceWindow( Window w ){
		return w instanceof WndChooseSpecialization || w instanceof WndChooseGear;
	}

	//co-op bots with -Dspd.test.thalpath / thalgear: each hero takes the next one, so two bots
	//end up on different paths. without them the bots just pick at random
	public static boolean answer( Window w ){
		String key = w instanceof WndChooseSpecialization ? "spd.test.thalpath" : w instanceof WndChooseGear ? "spd.test.thalgear" : null;
		if (key == null || Integer.getInteger(key) == null || Dungeon.local() == null) return false;
		int n = (Integer.getInteger(key) + Party.indexOf(Dungeon.local())) % 3;
		ArrayList<RedButton> reds = new ArrayList<>();
		for (Button b : CoopSession.buttonsOfWindow(w)){
			if (b instanceof RedButton && b.active) reds.add((RedButton) b);
		}
		if (reds.size() < 3) return false;
		RedButton b = reds.get(n);
		CoopLog.line("bot picks " + b.text().replace('\n', ' '));
		if (!CoopSession.buttonClicked(b)) b.coopClick();
		return true;
	}

	// *** co-op bots ***

	//the bot's own whims, never the game's random
	private static final Random botRnd = new Random();
	private static boolean clockTried = false;
	private static boolean gearTried = false;
	private static boolean logged = false;

	//returns true if the bot gave an order this turn
	public static boolean coopBot( Hero me ){
		if (!enabled()) return false;
		ThalHeart heart = me.belongings.getItem(ThalHeart.class);
		if (heart != null && me.specialization == null){
			CoopLog.line("bot listens to the heart");
			CoopOrders.execute(heart, ThalHeart.AC_LISTEN);
			return true;
		}
		ClockmakerChest chest = me.belongings.getItem(ClockmakerChest.class);
		if (chest != null){
			CoopLog.line("bot opens the clockmaker's chest");
			CoopOrders.execute(chest, ClockmakerChest.AC_OPEN);
			return true;
		}
		MeltedClock clock = me.belongings.getItem(MeltedClock.class);
		if (clock != null && !clock.isEquipped(me) && !clockTried){
			clockTried = true;
			CoopLog.line("bot equips the melted clock");
			CoopOrders.execute(clock, EquipableItem.AC_EQUIP);
			return true;
		}
		Item gear = forged(me);
		if (gear != null && !gearTried){
			gearTried = true;
			CoopLog.line("bot equips " + gear.getClass().getSimpleName() + " +" + gear.level());
			CoopOrders.execute(gear, EquipableItem.AC_EQUIP);
			return true;
		}
		PotionOfExperience exp = me.belongings.getItem(PotionOfExperience.class);
		if (exp != null && me.specialization != null){
			CoopLog.line("bot drinks experience lvl=" + me.lvl + " t5pts=" + me.talentPointsAvailable(5));
			CoopOrders.execute(exp, Potion.AC_DRINK);
			return true;
		}
		if (!logged && me.specialization != null){
			logged = true;
			CoopLog.line("bot thal state " + state(me));
		}
		if (clock != null && clock.isEquipped(me) && me.visibleEnemies() > 0){
			if (clock.windUp() >= MeltedClock.REWIND_COST && me.HP < me.HT * 0.8f){
				CoopLog.line("bot rewinds hp=" + me.HP + " pos=" + me.pos);
				CoopOrders.execute(clock, MeltedClock.AC_REWIND);
				return true;
			}
			//now and then it saves up for a rewind instead
			if (clock.windUp() >= MeltedClock.HALT_COST && (clock.windUp() >= MeltedClock.REWIND_COST || botRnd.nextInt(40) == 0)){
				CoopLog.line("bot halts time charge=" + clock.windUp());
				CoopOrders.execute(clock, MeltedClock.AC_HALT);
				return true;
			}
		}
		return false;
	}

	private static Item forged( Hero me ){
		for (Item i : me.belongings.backpack){
			if (i instanceof MinuteHand || i instanceof Pendulum || i instanceof GearPlate) return i;
		}
		return null;
	}

	public static String state( Hero h ){
		StringBuilder sb = new StringBuilder();
		sb.append(h.heroClass).append(" lvl=").append(h.lvl).append("/").append(h.lvlCap)
				.append(" path=").append(h.specialization)
				.append(" t5pts=").append(h.talentPointsAvailable(5));
		if (h.talents.size() >= 5){
			for (Talent t : h.talents.get(4).keySet()) sb.append(" ").append(t).append("=").append(h.pointsInTalent(t));
		}
		sb.append(" wep=").append(h.belongings.weapon == null ? "-" : h.belongings.weapon.getClass().getSimpleName() + "+" + h.belongings.weapon.level());
		sb.append(" arm=").append(h.belongings.armor == null ? "-" : h.belongings.armor.getClass().getSimpleName() + "+" + h.belongings.armor.level());
		MeltedClock c = h.belongings.getItem(MeltedClock.class);
		sb.append(" clock=").append(c == null ? "-" : c.windUp() + "%+" + c.level() + (c.isEquipped(h) ? "E" : ""));
		return sb.toString();
	}

	// *** solo walkthrough ***

	private static int step = 0;
	private static float timer = 4f;

	public static void solo( Hero hero ){
		if (!enabled() || !DevFlags.thalWalk() || CoopSession.active() || hero == null || !hero.isAlive()) return;
		timer -= Game.elapsed;
		if (timer > 0) return;
		timer = 3f;
		Window top = GameScene.topWindow();
		switch (step){
			case 0: {
				ThalHeart heart = hero.belongings.getItem(ThalHeart.class);
				if (heart == null || top != null || !hero.ready) return;
				CoopLog.line("thal solo: " + state(hero));
				CoopOrders.execute(heart, ThalHeart.AC_LISTEN);
				step++;
				return;
			}
			case 1:
				if (top instanceof WndChooseSpecialization){
					click(top, Integer.getInteger("spd.test.thalpath", hero.heroClass.ordinal() % 3));
					step++;
				}
				return;
			case 2: case 5:
				if (top instanceof WndOptions){
					click(top, 0);
					step++;
				}
				return;
			case 3: {
				ClockmakerChest chest = hero.belongings.getItem(ClockmakerChest.class);
				if (chest == null || top != null || !hero.ready) return;
				CoopLog.line("thal solo: path chosen, " + state(hero));
				CoopOrders.execute(chest, ClockmakerChest.AC_OPEN);
				step++;
				return;
			}
			case 4:
				if (top instanceof WndChooseGear){
					click(top, Integer.getInteger("spd.test.thalgear", 0));
					step++;
				}
				return;
			case 6: {
				if (top != null || !hero.ready) return;
				MeltedClock clock = hero.belongings.getItem(MeltedClock.class);
				if (clock != null && !clock.isEquipped(hero)) CoopOrders.execute(clock, EquipableItem.AC_EQUIP);
				step++;
				return;
			}
			case 7: {
				if (top != null || !hero.ready) return;
				Item gear = forged(hero);
				if (gear != null) CoopOrders.execute(gear, EquipableItem.AC_EQUIP);
				step++;
				return;
			}
			case 8:
				if (top != null || !hero.ready) return;
				PotionOfExperience exp = hero.belongings.getItem(PotionOfExperience.class);
				if (exp != null){
					CoopLog.line("thal solo: drinks experience lvl=" + hero.lvl);
					CoopOrders.execute(exp, Potion.AC_DRINK);
					return;
				}
				CoopLog.line("thal solo: geared, " + state(hero));
				WndHero.lastIdx = 1;
				GameScene.show(new WndHero());
				step++;
				return;
			case 9:
				step++;
				return;
			case 10:
				if (top != null) top.hide();
				spawnPrey(hero);
				step++;
				timer = 0.5f;
				return;
			default: {
				timer = 0.5f;
				if (top != null || !hero.ready || GameScene.isSelectingCell()) return;
				if (hero.talentPointsAvailable(5) > 0){
					for (Talent t : hero.talents.get(4).keySet()){
						if (hero.pointsInTalent(t) < t.maxPoints()){
							CoopLog.line("thal solo: talent " + t);
							CoopOrders.upgradeTalent(t, null);
							return;
						}
					}
				}
				MeltedClock clock = hero.belongings.getItem(MeltedClock.class);
				if (clock != null && clock.isEquipped(hero) && hero.visibleEnemies() > 0){
					if (clock.windUp() >= MeltedClock.REWIND_COST && hero.HP < hero.HT * 0.8f){
						CoopLog.line("thal solo: rewind hp=" + hero.HP + " pos=" + hero.pos);
						CoopOrders.execute(clock, MeltedClock.AC_REWIND);
						return;
					} else if (clock.windUp() >= MeltedClock.HALT_COST && clock.windUp() < MeltedClock.REWIND_COST){
						CoopLog.line("thal solo: halt charge=" + clock.windUp());
						CoopOrders.execute(clock, MeltedClock.AC_HALT);
						return;
					}
				}
				int sensed = 0;
				for (Mob m : hero.mindVisionEnemies) if (SpecPowers.isPrey(m)) sensed++;
				for (Mob m : Dungeon.level.mobs){
					if (m.alignment == Char.Alignment.ENEMY && Dungeon.level.adjacent(hero.pos, m.pos)){
						CoopLog.line("thal solo: hits " + m.getClass().getSimpleName() + " hp=" + m.HP
								+ " me=" + hero.HP + "/" + hero.HT + " sensed=" + sensed);
						CoopOrders.handleCell(m.pos);
						return;
					}
				}
				CoopOrders.named("rest");
			}
		}
	}

	//a few demons around, one of them behind a wall if there's room
	private static void spawnPrey( Hero hero ){
		int n = 0;
		for (int c : PathFinder.NEIGHBOURS8){
			if (n >= 2) break;
			int cell = hero.pos + 3 * c;
			if (cell < 0 || cell >= Dungeon.level.length() || !Dungeon.level.passable[cell] || Actor.findChar(cell) != null) continue;
			RipperDemon d = new RipperDemon();
			d.pos = cell;
			GameScene.add(d);
			n++;
		}
		for (int i = 0; i < Dungeon.level.length(); i++){
			int d = Dungeon.level.distance(hero.pos, i);
			if (d >= 5 && d <= 6 && Dungeon.level.passable[i] && !Dungeon.level.heroFOV[i] && Actor.findChar(i) == null){
				Succubus s = new Succubus();
				s.pos = i;
				GameScene.add(s);
				n++;
				break;
			}
		}
		CoopLog.line("thal solo: spawned " + n + " demons");
	}

	//the n-th red button of a window (the choices, not the info buttons)
	private static void click( Window w, int n ){
		ArrayList<RedButton> reds = new ArrayList<>();
		for (Button b : CoopSession.buttonsOfWindow(w)){
			if (b instanceof RedButton && b.active) reds.add((RedButton) b);
		}
		if (reds.isEmpty()) return;
		RedButton b = reds.get(Math.max(0, Math.min(n, reds.size() - 1)));
		CoopLog.line("thal solo: clicks " + b.text().replace('\n', ' '));
		b.coopClick();
	}
}
