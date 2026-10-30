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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Light;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Ghost;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Spear;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKWorlds;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndQuest;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//caiman quest (sewers, floor 2-4). the legend is real, who knew
//old sewer worker by the stairs wants the beast in the flooded channel dead. it hides underwater, lunges, drags you in.
//on dry floor it's slow + soft so fight it there (killing it on land = bonus)
//done: worker closes Goo's drains, room stays dry, Goo can't heal in water
//not done: the caiman crawls into Goo's room. good luck lol
public class CaimanQuest {

	public static final QuestState Q = SKQuests.CAIMAN;

	/* quest on/off. it's all drawn and tested now, keep it true */
	public static boolean READY = true;

	public static void spawn( RegularLevel level, ArrayList<Room> rooms, Room entrance ){
		if (!READY || Q.depth != -1 || Dungeon.depth < 2 || Dungeon.depth > 4) return;
		if (SKWorlds.isSK(Dungeon.depth)) return;
		if (DevFlags.noQuests()) return;
		boolean test = DevFlags.forceQuest("caiman");
		//not on the ghost's floor unless it's the last chance
		if (!test && Ghost.Quest.active() && Dungeon.depth < 4) return;
		if (!test && Random.Int(5 - Dungeon.depth) != 0) return;

		//biggest room where a channel fits
		Room canal = null;
		for (int min = 8; min >= 6 && canal == null; min--){
			for (Room r : rooms){
				if (!(r instanceof StandardRoom) || r == entrance || r == null) continue;
				if (r.width() < min || r.height() < min) continue;
				if (level.exit() >= 0 && r.inside(level.cellToPoint(level.exit()))) continue;
				if (canal == null || r.width() * r.height() > canal.width() * canal.height()) canal = r;
			}
		}
		if (canal == null){
			CoopLog.line("caiman: no room for the channel");
			return;
		}
		int workerCell = HuntQuest.freeCell(level, entrance, -1);
		if (workerCell == -1) return;

		//deep channel across the room, dry ground both sides
		int w = level.width();
		boolean across = canal.width() >= canal.height();
		for (int y = canal.top + 1; y < canal.bottom; y++){
			for (int x = canal.left + 1; x < canal.right; x++){
				int c = x + y * w;
				int t = level.map[c];
				if (t != Terrain.EMPTY && t != Terrain.EMPTY_DECO && t != Terrain.GRASS && t != Terrain.WATER) continue;
				boolean inChannel = across
						? Math.abs(y - (canal.top + canal.bottom) / 2) <= 1
						: Math.abs(x - (canal.left + canal.right) / 2) <= 1;
				boolean pool = Math.abs(x - (canal.left + canal.right) / 2) <= 2 && Math.abs(y - (canal.top + canal.bottom) / 2) <= 2;
				if (inChannel || pool) Level.set(c, Terrain.WATER, level);
			}
		}

		Sewerman s = new Sewerman();
		HuntQuest.add(level, s, workerCell);

		Caiman cm = new Caiman();
		int mid = level.pointToCell(canal.center());
		HuntQuest.add(level, cm, level.passable[mid] && level.findMob(mid) == null ? mid : HuntQuest.freeCell(level, canal, -1));
		cm.submerged = level.water[cm.pos];

		Q.depth = Dungeon.depth;
		CoopLog.line("caiman placed depth=" + Dungeon.depth + " worker=" + workerCell + " caiman=" + cm.pos);
	}

	//Goo's floor gets built: caiman dead -> worker already closed the drains
	public static void drain( Level level ){
		if (!Q.done) return;
		int n = 0;
		for (int i = 0; i < level.length(); i++){
			if (level.map[i] == Terrain.WATER){
				Level.set(i, Terrain.EMPTY_DECO, level);
				n++;
			}
		}
		CoopLog.line("caiman quest: goo's floor drained, " + n + " cells");
	}

	public static void gooFight( Goo goo ){
		if (Q.done){
			GLog.p(Messages.get(CaimanQuest.class, "dry"));
			return;
		}
		if (!Q.punish() || Q.failed) return;
		Q.failed = true;
		int cell = -1;
		for (int i = 0; i < Dungeon.level.length() && cell == -1; i++){
			if (Dungeon.level.water[i] && Actor.findChar(i) == null && Dungeon.level.distance(i, goo.pos) >= 3
					&& Dungeon.level.distance(i, goo.pos) <= 8) cell = i;
		}
		for (int tries = 0; tries < 80 && cell == -1; tries++){
			int c = goo.pos + Random.IntRange(-6, 6) + Random.IntRange(-6, 6) * Dungeon.level.width();
			if (c > 0 && c < Dungeon.level.length() && Dungeon.level.passable[c] && Actor.findChar(c) == null) cell = c;
		}
		if (cell == -1) return;
		Caiman c = new Caiman();
		c.pos = cell;
		c.submerged = Dungeon.level.water[cell];
		GameScene.add(c);
		c.state = c.HUNTING;
		c.startFight();
		GLog.n(Messages.get(CaimanQuest.class, "followed"));
		CoopLog.line("caiman quest failed: it joins goo at " + cell);
	}

	// ---- sewer guy ----

	public static class Sewerman extends NPC {

		{
			spriteClass = SewermanSprite.class;
			properties.add(Property.IMMOVABLE);
		}

		private int talk = 0;

		@Override
		protected boolean act() {
			spend(TICK);
			return true;
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
			if (!Q.given){
				Q.given = true;
				CoopLog.line("caiman given");
				Game.runOnRenderThread(new Callback() {
					@Override
					public void call() {
						GameScene.show(new WndQuest(Sewerman.this, Messages.get(Sewerman.class, "intro")));
					}
				});
				GLog.p(Messages.get(Sewerman.class, "intro_hint"));
				return true;
			}
			if (!Q.done){
				CaimanTooth tooth = h.belongings.getItem(CaimanTooth.class);
				if (tooth == null){
					yell(Messages.get(Sewerman.class, Q.slain ? "bring" : "hint_" + (talk++ % 3)));
					return true;
				}
				tooth.detachAll(h.belongings.backpack);
				Q.done = true;
				Sample.INSTANCE.play(Assets.Sounds.MASTERY);
				yell(Messages.get(Sewerman.class, Q.bonus ? "thanks_bonus" : "thanks"));
				CoopLog.line("caiman done bonus=" + Q.bonus);
			}
			if (Q.claimedBy(h)){
				yell(Messages.get(Sewerman.class, "after"));
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

		private void reward( final Hero h ){
			GameScene.show(new WndOptions(
					Messages.titleCase(name()),
					Messages.get(Sewerman.class, Q.bonus ? "reward_bonus" : "reward"),
					Messages.get(CaimanHide.class, "name"),
					Messages.get(SewerHook.class, "name"),
					Messages.get(SewerLantern.class, "name")){
				@Override
				protected void onSelect( int index ) {
					if (Q.claimedBy(h)) return;
					Item item;
					switch (index){
						case 0: default: item = new CaimanHide(); break;
						case 1: item = new SewerHook(); break;
						case 2: item = new SewerLantern(); break;
					}
					if (item.isUpgradable()) item.upgrade(Q.bonus ? 2 : 1);
					else if (item instanceof SewerLantern && Q.bonus) ((SewerLantern) item).boost();
					item.identify();
					Q.claim(h);
					if (!item.collect(h.belongings.backpack)) Dungeon.level.drop(item, h.pos).sprite.drop();
					GLog.p(Messages.get(Sewerman.class, "got", item.name()));
					CoopLog.line("caiman reward " + item.getClass().getSimpleName() + " for hero " + ChampionHooks.indexOf(h));
				}
				@Override
				public void onBackPressed() {
					//pick one, no take-backs
				}
			});
		}

		@Override
		public String description() {
			return Messages.get(this, "desc");
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("talk", talk);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			talk = bundle.getInt("talk");
		}
	}

	public static class SewermanSprite extends SKMobSprite {
		public SewermanSprite(){
			super();
			setup(QuestAssets.SEWERMAN, 12, 15);
		}
	}

	// *** items ***

	public static class CaimanTooth extends Item {
		{
			image = ItemSpriteSheet.CAIMAN_TOOTH;
			unique = true;
		}

		@Override
		public boolean isUpgradable() {
			return false;
		}

		@Override
		public boolean isIdentified() {
			return true;
		}
	}

	//caiman hide: -25% dmg while the wearer stands in water
	public static class CaimanHide extends LeatherArmor {

		{
			image = ItemSpriteSheet.CAIMAN_HIDE;
		}

		@Override
		public int proc( Char attacker, Char defender, int damage ) {
			int d = super.proc(attacker, defender, damage);
			if (Dungeon.level.water[defender.pos] && !defender.flying) d = d * 3 / 4;
			return d;
		}
	}

	//worker's pole. long, and the hook yanks the target closer sometimes
	public static class SewerHook extends Spear {

		{
			image = ItemSpriteSheet.SEWER_HOOK;
		}

		@Override
		public int proc( Char attacker, Char defender, int damage ) {
			if (Random.Int(10) < 3 && defender.isAlive() && !Dungeon.level.adjacent(attacker.pos, defender.pos)
					&& !defender.properties().contains(Char.Property.IMMOVABLE)
					&& !defender.properties().contains(Char.Property.BOSS)){
				int best = -1;
				for (int n : PathFinder.NEIGHBOURS8){
					int c = attacker.pos + n;
					if (Dungeon.level.passable[c] && Actor.findChar(c) == null
							&& Dungeon.level.distance(c, defender.pos) < Dungeon.level.distance(attacker.pos, defender.pos)){
						if (best == -1 || Dungeon.level.distance(c, defender.pos) < Dungeon.level.distance(best, defender.pos)) best = c;
					}
				}
				if (best != -1){
					int from = defender.pos;
					defender.pos = best;
					Dungeon.level.occupyCell(defender);
					if (defender.sprite != null) defender.sprite.move(from, best);
					Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG, 0.6f, 1.3f);
				}
			}
			return super.proc(attacker, defender, damage);
		}
	}

	//worker's lantern. lit = shows the area + traps + secret doors. kinda cheating tbh
	public static class SewerLantern extends Artifact {

		public static final String AC_LIGHT = "LIGHT";

		{
			image = ItemSpriteSheet.SEWER_LANTERN;
			levelCap = 10;
			charge = 100;
			chargeCap = 100;
			defaultAction = AC_LIGHT;
			unique = true;
		}

		void boost(){
			upgrade(2);
		}

		private int radius(){
			return 6 + level() / 2;
		}

		@Override
		public ArrayList<String> actions( Hero hero ) {
			ArrayList<String> actions = super.actions(hero);
			if (isEquipped(hero) && !cursed) actions.add(AC_LIGHT);
			return actions;
		}

		@Override
		public void execute( Hero hero, String action ) {
			super.execute(hero, action);
			if (!action.equals(AC_LIGHT)) return;
			if (!isEquipped(hero)){
				GLog.i(Messages.get(Artifact.class, "need_to_equip"));
				return;
			}
			if (charge < chargeCap){
				GLog.i(Messages.get(this, "no_charge"));
				return;
			}
			charge = 0;
			Level L = Dungeon.level;
			int r = radius();
			int w = L.width();
			boolean found = false;
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					int x = hero.pos % w + dx, y = hero.pos / w + dy;
					if (x < 0 || y < 0 || x >= w || y >= L.height()) continue;
					int c = x + y * w;
					L.mapped[c] = true;
					if ((L.map[c] == Terrain.SECRET_DOOR || L.map[c] == Terrain.SECRET_TRAP)){
						L.discover(c);
						if (L.heroFOV[c]) CellEmitter.get(c).start(Speck.factory(Speck.DISCOVER), 0.1f, 4);
						found = true;
					}
				}
			}
			Buff.affect(hero, Light.class, 20f + 2 * level());
			GameScene.updateFog();
			Sample.INSTANCE.play(Assets.Sounds.BURNING, 0.6f, 1.4f);
			GLog.p(Messages.get(this, found ? "lit_found" : "lit"));
			exp++;
			if (exp >= level() + 2 && level() < levelCap){
				exp = 0;
				upgrade();
				GLog.p(Messages.get(this, "levelup"));
			}
			updateQuickslot();
			hero.spendAndNext(1f);
		}

		@Override
		protected ArtifactBuff passiveBuff() {
			return new Wick();
		}

		@Override
		public void charge( Hero target, float amount ) {
			if (charge < chargeCap){
				charge = Math.min(chargeCap, charge + Math.round(3 * amount));
				updateQuickslot();
			}
		}

		@Override
		public String desc() {
			String desc = Messages.get(this, "desc", radius());
			if (isEquipped(Dungeon.hero)) desc += "\n\n" + Messages.get(this, charge >= chargeCap ? "desc_ready" : "desc_charging");
			return desc;
		}

		//refills slowly, a bit faster at higher lvl. balance? ni idea, seems fine
		public class Wick extends ArtifactBuff {
			@Override
			public boolean act() {
				if (charge < chargeCap && !cursed){
					partialCharge += 0.5f + 0.05f * level();
					while (partialCharge >= 1 && charge < chargeCap){
						partialCharge--;
						charge++;
					}
					if (charge >= chargeCap) GLog.p(Messages.get(SewerLantern.class, "ready"));
					updateQuickslot();
				}
				spend(TICK);
				return true;
			}
		}
	}

	//true if a quest caiman is still alive here
	static boolean caimanHere(){
		for (Mob m : Dungeon.level.mobs) if (m instanceof Caiman && m.isAlive()) return true;
		return false;
	}
}
