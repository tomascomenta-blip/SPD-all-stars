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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKWorlds;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndQuest;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

//the herald's tourney, coliseum on kingdom floor 8 (wandmaker's spot)
//3 rounds. each round the herald shows 2 rivals + what beating each is worth: an easy one (kingdom mob,
//way stronger than usual) and a hard one (an actual champion, better prize). gate closes during the fight.
//if every hero is about to drop the herald stops it: round lost and that champion joins the guard of
//the dark grand knight, who watches everything from the royal box.
//all 3 rounds played -> the knight recognizes them and doesn't bother with his horse.
//each hero picks a last prize, better if you won the 3 hard rounds
public class TourneyQuest {

	public static class State extends QuestState {
		public int round = 0;     //rounds already played
		public int duel = 0;      //round being fought now +1, 0 = none
		public boolean duelHard = false;
		public int hard = 0;      //bitmask, rounds won vs the hard rival
		public int lost = 0;      //bitmask, rounds lost
		public int lostHard = 0;  //bitmask, lost rounds where the hard rival was picked
		public int foe = -1;      //id of the rival in the ring

		public State(){
			super("quest_tourney");
		}

		@Override
		public void reset() {
			super.reset();
			round = duel = hard = lost = lostHard = 0;
			duelHard = false;
			foe = -1;
		}

		@Override
		protected void storeMore( Bundle b ) {
			b.put("round", round);
			b.put("duel", duel);
			b.put("duel_hard", duelHard);
			b.put("hard", hard);
			b.put("lost", lost);
			b.put("lost_hard", lostHard);
			b.put("foe", foe);
		}

		@Override
		protected void restoreMore( Bundle b ) {
			round = b.getInt("round");
			duel = b.getInt("duel");
			duelHard = b.getBoolean("duel_hard");
			hard = b.getInt("hard");
			lost = b.getInt("lost");
			lostHard = b.getInt("lost_hard");
			foe = b.contains("foe") ? b.getInt("foe") : -1;
		}

		@Override
		public boolean punish() {
			return lost != 0;
		}
	}

	public static final State Q = SKQuests.TOURNEY;

	public static final int DEPTH = 8;

	//off until... no wait, it's all done. true
	public static boolean READY = true;

	//placing it: the coliseum is just one of the rooms of kingdom floor 8
	public static ArrayList<Room> rooms( ArrayList<Room> rooms ){
		if (READY && Dungeon.depth == DEPTH && Q.depth == -1 && !DevFlags.noQuests()){
			rooms.add(new ColiseumRoom());
		}
		return rooms;
	}

	public static ColiseumRoom coliseum( Level level ){
		if (!(level instanceof RegularLevel) || ((RegularLevel) level).rooms() == null) return null;
		for (Room r : ((RegularLevel) level).rooms()){
			if (r instanceof ColiseumRoom) return (ColiseumRoom) r;
		}
		return null;
	}

	public static void placeHerald( Level level ){
		ColiseumRoom c = coliseum(level);
		if (c == null || Q.depth != -1) return;
		Herald h = new Herald();
		h.pos = c.heraldCell(level);
		level.mobs.add(h);
		Q.depth = Dungeon.depth;
		CoopLog.line("tourney placed depth=" + Dungeon.depth + " herald=" + h.pos + " gate side " + c.gateSide);
	}

	// *** rivals ***

	public static Mob rival( int round, boolean hard ){
		if (hard){
			switch (round){
				case 0: default: return new TourneyFoes.SirBastion();
				case 1: return new TourneyFoes.SlimeKing();
				case 2: return new TourneyFoes.BladeDame();
			}
		}
		return TourneyFoes.veteran(round);
	}

	public static String rivalName( int round, boolean hard ){
		return Messages.get(TourneyQuest.class, (hard ? "hard_" : "easy_") + round);
	}

	public static String prizeName( int round, boolean hard ){
		return Messages.get(TourneyQuest.class, (hard ? "prize_hard_" : "prize_easy_") + round);
	}

	//what every hero gets per round won
	private static void roundPrize( Hero h, int round, boolean hard ){
		ArrayList<Item> got = new ArrayList<>();
		if (hard){
			switch (round){
				case 0: default: got.add(new ScrollOfUpgrade()); break;
				case 1: {
					Item ring = Generator.random(Generator.Category.RING);
					ring.cursed = false;
					ring.upgrade(1);
					got.add(ring);
					break;
				}
				case 2: {
					Item wand = Generator.random(Generator.Category.WAND);
					wand.cursed = false;
					wand.upgrade(2);
					got.add(wand);
					break;
				}
			}
		} else {
			switch (round){
				case 0: default: got.add(new PotionOfHealing()); break;
				case 1: got.add(new PotionOfHealing().quantity(2)); break;
				case 2: got.add(new Bomb().quantity(2)); got.add(new PotionOfHealing()); break;
			}
		}
		for (Item i : got){
			i.identify();
			if (!i.collect(h.belongings.backpack)) Dungeon.level.drop(i, h.pos).sprite.drop();
			GLog.p(Messages.get(Herald.class, "got", i.name()));
		}
	}

	// --- dark grand knight fight

	//he saw them win the tourney -> no horse for them
	public static boolean recognized(){
		return Q.round >= 3;
	}

	//the champions that beat the heroes stand by the knight
	public static void knightFight( Mob knight ){
		if (recognized()) knight.yell(Messages.get(TourneyQuest.class, "knight_knows"));
		if (Q.lost == 0 || Q.failed) return;
		Q.failed = true;
		for (int r = 0; r < 3; r++){
			if ((Q.lost & (1 << r)) == 0) continue;
			Mob m = rival(r, (Q.lostHard & (1 << r)) != 0);
			int cell = -1;
			for (int k = 2; k <= 4 && cell == -1; k++){
				for (int n : PathFinder.NEIGHBOURS8){
					int c = knight.pos + n * k;
					if (c > 0 && c < Dungeon.level.length() && Dungeon.level.passable[c] && Actor.findChar(c) == null){
						cell = c;
						break;
					}
				}
			}
			if (cell == -1) continue;
			m.pos = cell;
			GameScene.add(m);
			m.state = m.HUNTING;
			if (m instanceof MiniBoss) ((MiniBoss) m).startFight();
			CoopLog.line("tourney: the champion of round " + r + " guards the knight at " + cell);
		}
		GLog.n(Messages.get(TourneyQuest.class, "guards"));
	}

	// *** the herald ***

	public static class Herald extends NPC {

		{
			spriteClass = HeraldSprite.class;
			properties.add(Property.IMMOVABLE);
		}

		@Override
		protected boolean act() {
			if (Q.duel > 0) watchDuel();
			spend(TICK);
			return true;
		}

		private void watchDuel(){
			Actor a = Actor.findById(Q.foe);
			if (!(a instanceof Char) || !((Char) a).isAlive()){
				win();
				return;
			}
			ArrayList<Hero> living = Party.active() ? Party.living() : new ArrayList<Hero>();
			if (!Party.active() && Dungeon.hero != null && Dungeon.hero.isAlive()) living.add(Dungeon.hero);
			boolean allLow = !living.isEmpty();
			for (Hero h : living){
				if (h.HP > h.HT / 5) allLow = false;
			}
			if (allLow) lose((Char) a);
		}

		private void win(){
			int r = Q.duel - 1;
			boolean hard = Q.duelHard;
			Q.duel = 0;
			Q.foe = -1;
			Q.round = Math.max(Q.round, r + 1);
			if (hard) Q.hard |= 1 << r;
			clearRing();
			openGate();
			yell(Messages.get(Herald.class, hard ? "win_hard" : "win"));
			Sample.INSTANCE.play(Assets.Sounds.MASTERY, 1f, 1.1f);
			TourneyVisuals.cheer();
			for (Hero h : living()){
				h.HP = Math.max(h.HT / 2, Math.min(h.HT, h.HP + h.HT / 4));
				h.sprite.emitter().burst(Speck.factory(Speck.HEALING), 3);
				roundPrize(h, r, hard);
			}
			MiniBossHud.banner(Messages.get(Herald.class, Q.round >= 3 ? "banner_last" : "banner_win", r + 1));
			if (Q.round >= 3) Q.done = true;
			if (Dungeon.level != null) Dungeon.level.playLevelMusic();
			CoopLog.line("tourney round " + r + " won hard=" + hard);
		}

		private void lose( Char foe ){
			int r = Q.duel - 1;
			Q.lost |= 1 << r;
			if (Q.duelHard) Q.lostHard |= 1 << r;
			Q.duel = 0;
			Q.foe = -1;
			Q.round = Math.max(Q.round, r + 1);
			if (foe instanceof Mob){
				if (foe.sprite != null) foe.sprite.killAndErase();
				((Mob) foe).destroy();
			}
			clearRing();
			openGate();
			yell(Messages.get(Herald.class, "stop"));
			TourneyVisuals.boo();
			//nobody walks into the next round half dead, the herald is nice like that
			for (Hero h : living()){
				h.HP = Math.max(h.HP, h.HT / 2);
			}
			MiniBossHud.banner(Messages.get(Herald.class, "banner_lost", r + 1));
			if (Q.round >= 3) Q.done = true;
			if (Dungeon.level != null) Dungeon.level.playLevelMusic();
			CoopLog.line("tourney round " + r + " lost");
		}

		//whatever the rival brought leaves the ring with it
		private void clearRing(){
			ColiseumRoom c = coliseum(Dungeon.level);
			if (c == null) return;
			for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
				if (m.alignment == Alignment.ENEMY && c.inRing(Dungeon.level, m.pos) && m.isAlive()){
					if (m.sprite != null) m.sprite.emitter().burst(Speck.factory(Speck.SMOKE), 4);
					m.destroy();
					if (m.sprite != null) m.sprite.killAndErase();
				}
			}
		}

		private ArrayList<Hero> living(){
			ArrayList<Hero> l = new ArrayList<>();
			if (Party.active()) l.addAll(Party.living());
			else if (Dungeon.hero != null && Dungeon.hero.isAlive()) l.add(Dungeon.hero);
			return l;
		}

		private void openGate(){
			ColiseumRoom c = coliseum(Dungeon.level);
			if (c == null) return;
			int g = c.gateCell(Dungeon.level);
			if (Dungeon.level.map[g] == Terrain.LOCKED_DOOR){
				Level.set(g, Terrain.DOOR, Dungeon.level);
				GameScene.updateMap(g);
			}
		}

		private void closeGate(){
			ColiseumRoom c = coliseum(Dungeon.level);
			if (c == null) return;
			int g = c.gateCell(Dungeon.level);
			Level.set(g, Terrain.LOCKED_DOOR, Dungeon.level);
			GameScene.updateMap(g);
			Sample.INSTANCE.play(Assets.Sounds.UNLOCK, 1f, 0.6f);
		}

		void startDuel( int round, boolean hard ){
			if (Q.duel > 0 || Q.round != round) return;
			ColiseumRoom c = coliseum(Dungeon.level);
			if (c == null) return;
			Q.duel = round + 1;
			Q.duelHard = hard;
			closeGate();
			//everyone into the ring, vamos
			for (Hero h : living()){
				if (c.inRing(Dungeon.level, h.pos)) continue;
				int to = -1;
				for (int k : PathFinder.NEIGHBOURS8){
					int cell = pos + k;
					if (c.inRing(Dungeon.level, cell) && Dungeon.level.passable[cell] && Actor.findChar(cell) == null){
						to = cell;
						break;
					}
				}
				if (to != -1) ScrollOfTeleportation.appear(h, to);
			}
			Mob foe = rival(round, hard);
			int at = c.foeCell(Dungeon.level);
			if (Actor.findChar(at) != null || !Dungeon.level.passable[at]){
				for (int k : PathFinder.NEIGHBOURS8){
					if (Dungeon.level.passable[at + k] && Actor.findChar(at + k) == null){
						at = at + k;
						break;
					}
				}
			}
			foe.pos = at;
			GameScene.add(foe);
			foe.state = foe.HUNTING;
			Q.foe = foe.id();
			if (foe instanceof MiniBoss){
				((MiniBoss) foe).startFight();
			} else {
				BossHealthBar.assignBoss(foe);
				MiniBossHud.title(rivalName(round, false), Messages.get(Herald.class, "subtitle_easy", round + 1));
				Music.INSTANCE.play(Assets.Music.PRISON_TENSE, true);
			}
			yell(Messages.get(Herald.class, "fight_" + round));
			TourneyVisuals.cheer();
			CoopLog.line("tourney round " + round + " starts hard=" + hard + " foe " + foe.getClass().getSimpleName() + " at " + at);
		}

		@Override
		public int defenseSkill( Char enemy ) {
			return INFINITE_EVASION;
		}

		@Override
		public void damage( int dmg, Object src ) {
		}

		@Override
		public boolean add( Buff buff ) {
			return false;
		}

		@Override
		public boolean isInvulnerable( Class effect ) {
			return true;
		}

		@Override
		public boolean reset() {
			return true;
		}

		@Override
		public boolean interact( Char c ) {
			if (!(c instanceof Hero)) return true;
			final Hero h = (Hero) c;
			if (sprite != null) sprite.turnTo(pos, h.pos);
			if (Q.duel > 0){
				yell(Messages.get(Herald.class, "fighting"));
				return true;
			}
			if (!Q.given){
				Q.given = true;
				CoopLog.line("tourney given");
				Game.runOnRenderThread(new Callback() {
					@Override
					public void call() {
						GameScene.show(new WndQuest(Herald.this, Messages.get(Herald.class, "intro")));
					}
				});
				GLog.p(Messages.get(Herald.class, "intro_hint"));
				return true;
			}
			if (Q.round < 3){
				final int r = Q.round;
				Game.runOnRenderThread(new Callback() {
					@Override
					public void call() {
						offer(r);
					}
				});
				return true;
			}
			if (Q.claimedBy(h)){
				yell(Messages.get(Herald.class, "after"));
				return true;
			}
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					reward(h);
				}
			});
			return true;
		}

		private void offer( final int r ){
			GameScene.show(new WndOptions(
					Messages.get(Herald.class, "round_title", r + 1),
					Messages.get(Herald.class, "round_text", rivalName(r, false), prizeName(r, false), rivalName(r, true), prizeName(r, true)),
					Messages.get(Herald.class, "pick_easy", rivalName(r, false)),
					Messages.get(Herald.class, "pick_hard", rivalName(r, true)),
					Messages.get(Herald.class, "pick_later")){
				@Override
				protected void onSelect( int index ) {
					if (index == 0 || index == 1) startDuel(r, index == 1);
				}
			});
		}

		private void reward( final Hero h ){
			final boolean best = Q.hard == 7;
			GameScene.show(new WndOptions(
					Messages.titleCase(name()),
					Messages.get(Herald.class, best ? "reward_best" : "reward"),
					Messages.get(TourneyFoes.JoustingLance.class, "name"),
					Messages.get(TourneyFoes.ChampionArmor.class, "name"),
					Messages.get(TourneyFoes.HeraldBanner.class, "name")){
				@Override
				protected void onSelect( int index ) {
					if (Q.claimedBy(h)) return;
					Item item;
					switch (index){
						case 0: default: item = new TourneyFoes.JoustingLance(); break;
						case 1: item = new TourneyFoes.ChampionArmor(); break;
						case 2: item = new TourneyFoes.HeraldBanner(); break;
					}
					if (item.isUpgradable()) item.upgrade(best ? 2 : 1);
					else if (item instanceof TourneyFoes.HeraldBanner && best) ((TourneyFoes.HeraldBanner) item).boost();
					item.identify();
					Q.claim(h);
					if (!item.collect(h.belongings.backpack)) Dungeon.level.drop(item, h.pos).sprite.drop();
					GLog.p(Messages.get(Herald.class, "got", item.name()));
					CoopLog.line("tourney reward " + item.getClass().getSimpleName() + " for hero " + ChampionHooks.indexOf(h));
				}
				@Override
				public void onBackPressed() {
					//elige uno
				}
			});
		}

		@Override
		public String description() {
			return Messages.get(this, "desc");
		}
	}

	public static class HeraldSprite extends SKMobSprite {
		public HeraldSprite(){
			super();
			setup(QuestAssets.HERALD, 12, 15);
		}
	}

	//true on the coliseum floor (visuals + music)
	public static boolean here(){
		return SKWorlds.isSK(Dungeon.depth) && Dungeon.depth == DEPTH && coliseum(Dungeon.level) != null;
	}
}
