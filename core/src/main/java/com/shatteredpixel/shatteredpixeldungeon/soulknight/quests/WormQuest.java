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
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM300;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.MailArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Mace;
import com.shatteredpixel.shatteredpixeldungeon.levels.CavesBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
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
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.utils.PointF;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

//rock worm, caves 12-14 (next to the blacksmith)
//old dwarf miner says a worm the size of a tunnel is bringing the mine down. digs under the floor,
//pops up under the heroes, and every time it does part of the cave collapses.
//kill it before it surfaces too many times = mine saved (bonus)
//done: its tunnels are cover in DM-300's room (half dmg while standing in one)
//not done: it pops up in DM-300's room instead. have fun
public class WormQuest {

	public static final QuestState Q = SKQuests.WORM;

	//on (used to be off while the art wasn't there)
	public static boolean READY = true;

	public static void spawn( RegularLevel level, ArrayList<Room> rooms, Room entrance ){
		if (!READY || Q.depth != -1 || Dungeon.depth < 12 || Dungeon.depth > 14) return;
		if (SKWorlds.isSK(Dungeon.depth)) return;
		if (DevFlags.noQuests()) return;
		if (!DevFlags.forceQuest("worm") && Random.Int(15 - Dungeon.depth) != 0) return;

		ArrayList<Room> far = new ArrayList<>();
		for (Room r : rooms){
			if (r instanceof StandardRoom && r != entrance && r.width() >= 6 && r.height() >= 6) far.add(r);
		}
		if (far.isEmpty()) return;
		final int from = level.entrance();
		Collections.sort(far, new Comparator<Room>() {
			@Override
			public int compare( Room a, Room b ){
				return level.distance(from, level.pointToCell(b.center())) - level.distance(from, level.pointToCell(a.center()));
			}
		});
		int minerCell = HuntQuest.freeCell(level, entrance, -1);
		int wormCell = HuntQuest.freeCell(level, far.get(0), -1);
		if (minerCell == -1 || wormCell == -1) return;
		HuntQuest.add(level, new DwarfMiner(), minerCell);
		RockWorm w = new RockWorm();
		HuntQuest.add(level, w, wormCell);
		Q.depth = Dungeon.depth;
		CoopLog.line("worm placed depth=" + Dungeon.depth + " miner=" + minerCell + " worm=" + wormCell);
	}

	//its old tunnels in DM-300's room, cover for the heroes
	public static void tunnels( CavesBossLevel level ){
		if (!Q.done) return;
		WormTunnels t = WormTunnels.getOrMake(level);
		int made = 0;
		for (int tries = 0; tries < 400 && made < 5; tries++){
			int c = Random.Int(level.length());
			if (!level.passable[c] || level.solid[c] || HuntQuest.stairs(level, c) || level.findMob(c) != null) continue;
			if (level.distance(c, level.entrance()) < 4) continue;
			boolean near = false;
			for (int i = 0; i < t.cur.length; i++) if (t.cur[i] > 0 && level.distance(i, c) < 4) near = true;
			if (near) continue;
			t.dig(c);
			made++;
		}
		CoopLog.line("worm quest: " + made + " tunnels in the room of dm-300");
	}

	public static float coverFactor( Char ch ){
		if (!(Dungeon.level instanceof CavesBossLevel) || ch.alignment != Char.Alignment.ALLY) return 1f;
		WormTunnels t = WormTunnels.get(Dungeon.level);
		return t != null && t.covers(ch.pos) ? 0.5f : 1f;
	}

	public static void dmFight( DM300 dm ){
		if (Q.done){
			GLog.p(Messages.get(WormQuest.class, "cover"));
			return;
		}
		if (!Q.punish() || Q.failed) return;
		Q.failed = true;
		RockWorm w = new RockWorm();
		int cell = -1;
		for (int tries = 0; tries < 80 && cell == -1; tries++){
			int c = dm.pos + Random.IntRange(-6, 6) + Random.IntRange(-6, 6) * Dungeon.level.width();
			if (c > 0 && c < Dungeon.level.length() && Dungeon.level.passable[c] && Actor.findChar(c) == null
					&& Dungeon.level.distance(c, dm.pos) >= 3) cell = c;
		}
		if (cell == -1) return;
		w.pos = cell;
		GameScene.add(w);
		w.state = w.HUNTING;
		w.startFight();
		GLog.n(Messages.get(WormQuest.class, "followed"));
		CoopLog.line("worm quest failed: it joins dm-300 at " + cell);
	}

	// *** old dwarf miner ***

	public static class DwarfMiner extends NPC {

		{
			spriteClass = DwarfMinerSprite.class;
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
				CoopLog.line("worm given");
				Game.runOnRenderThread(new Callback() {
					@Override
					public void call() {
						GameScene.show(new WndQuest(DwarfMiner.this, Messages.get(DwarfMiner.class, "intro")));
					}
				});
				GLog.p(Messages.get(DwarfMiner.class, "intro_hint"));
				return true;
			}
			if (!Q.done){
				WormTooth tooth = h.belongings.getItem(WormTooth.class);
				if (tooth == null){
					yell(Messages.get(DwarfMiner.class, Q.slain ? "bring" : "hint_" + (talk++ % 3)));
					return true;
				}
				tooth.detachAll(h.belongings.backpack);
				Q.done = true;
				Sample.INSTANCE.play(Assets.Sounds.MASTERY);
				yell(Messages.get(DwarfMiner.class, Q.bonus ? "thanks_bonus" : "thanks"));
				CoopLog.line("worm done bonus=" + Q.bonus);
			}
			if (Q.claimedBy(h)){
				yell(Messages.get(DwarfMiner.class, "after"));
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
					Messages.get(DwarfMiner.class, Q.bonus ? "reward_bonus" : "reward"),
					Messages.get(WormDrill.class, "name"),
					Messages.get(StoneScaleMail.class, "name"),
					Messages.get(PowderSack.class, "name")){
				@Override
				protected void onSelect( int index ) {
					if (Q.claimedBy(h)) return;
					Item item;
					switch (index){
						case 0: default: item = new WormDrill(); break;
						case 1: item = new StoneScaleMail(); break;
						case 2: item = new PowderSack(); break;
					}
					if (item.isUpgradable()) item.upgrade(Q.bonus ? 2 : 1);
					else if (item instanceof PowderSack && Q.bonus) ((PowderSack) item).boost();
					item.identify();
					Q.claim(h);
					if (!item.collect(h.belongings.backpack)) Dungeon.level.drop(item, h.pos).sprite.drop();
					GLog.p(Messages.get(DwarfMiner.class, "got", item.name()));
					CoopLog.line("worm reward " + item.getClass().getSimpleName() + " for hero " + ChampionHooks.indexOf(h));
				}
				@Override
				public void onBackPressed() {
					//one reward, choose
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

	public static class DwarfMinerSprite extends SKMobSprite {
		public DwarfMinerSprite(){
			super();
			setup(QuestAssets.DWARF_MINER, 12, 14);
		}
	}

	// *** items ***

	public static class WormTooth extends Item {
		{
			image = ItemSpriteSheet.WORM_TOOTH;
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

	//heavy drill made from the worm's tooth, ignores half the armor
	public static class WormDrill extends Mace {

		{
			image = ItemSpriteSheet.WORM_DRILL;
		}

		@Override
		public int proc( Char attacker, Char defender, int damage ) {
			//1 in 5 hits finds a crack and hits harder
			if (Random.Int(5) == 0) damage += defender.drRoll() / 2 + 2;
			return super.proc(attacker, defender, damage);
		}
	}

	//mail w/ stone plates from the worm's hide. falling rocks and blasts hurt half
	public static class StoneScaleMail extends MailArmor {

		{
			image = ItemSpriteSheet.STONE_SCALE_MAIL;
		}

		@Override
		public int proc( Char attacker, Char defender, int damage ) {
			int d = super.proc(attacker, defender, damage);
			if (attacker == null || !Dungeon.level.adjacent(attacker.pos, defender.pos)) d = d * 4 / 5;
			return d;
		}
	}

	//sack of dwarven powder, every now and then it's full enough for a bomb
	public static class PowderSack extends Artifact {

		public static final String AC_MAKE = "MAKE";

		{
			image = ItemSpriteSheet.POWDER_SACK;
			levelCap = 10;
			charge = 0;
			chargeCap = 100;
			defaultAction = AC_MAKE;
			unique = true;
		}

		void boost(){
			upgrade(2);
		}

		@Override
		public ArrayList<String> actions( Hero hero ) {
			ArrayList<String> actions = super.actions(hero);
			if (isEquipped(hero) && !cursed) actions.add(AC_MAKE);
			return actions;
		}

		@Override
		public void execute( Hero hero, String action ) {
			super.execute(hero, action);
			if (!action.equals(AC_MAKE)) return;
			if (!isEquipped(hero)){
				GLog.i(Messages.get(Artifact.class, "need_to_equip"));
				return;
			}
			if (charge < chargeCap){
				GLog.i(Messages.get(this, "no_charge"));
				return;
			}
			charge = 0;
			Bomb b = new Bomb();
			if (!b.collect(hero.belongings.backpack)) Dungeon.level.drop(b, hero.pos).sprite.drop();
			Sample.INSTANCE.play(Assets.Sounds.ITEM);
			GLog.p(Messages.get(this, "made"));
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
			return new Fill();
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
			String desc = Messages.get(this, "desc");
			if (isEquipped(Dungeon.hero)) desc += "\n\n" + Messages.get(this, charge >= chargeCap ? "desc_ready" : "desc_charging");
			return desc;
		}

		public class Fill extends ArtifactBuff {
			@Override
			public boolean act() {
				if (charge < chargeCap && !cursed){
					partialCharge += 0.35f + 0.05f * level();
					while (partialCharge >= 1 && charge < chargeCap){
						partialCharge--;
						charge++;
					}
					if (charge >= chargeCap) GLog.p(Messages.get(PowderSack.class, "ready"));
					updateQuickslot();
				}
				spend(TICK);
				return true;
			}
		}
	}

	public static class TunnelVisuals extends Group {
		private int shown = -1;

		@Override
		public void update() {
			super.update();
			WormTunnels t = WormTunnels.get(Dungeon.level);
			if (t == null || t.cur == null || t.version == shown) return;
			shown = t.version;
			clear();
			for (int c = 0; c < t.cur.length; c++){
				if (t.cur[c] == 0) continue;
				Image im = new Image(QuestAssets.WORM_TUNNEL);
				PointF p = DungeonTilemap.tileToWorld(c);
				im.x = p.x;
				im.y = p.y;
				add(im);
			}
		}
	}

	// *** tunnels in DM-300's room (just the state, WormTunnels.Visual draws them) ***

	public static class WormTunnels extends Blob {

		public int version = 0;

		public static WormTunnels get( Level level ){
			return level == null ? null : (WormTunnels) level.blobs.get(WormTunnels.class);
		}

		public static WormTunnels getOrMake( Level level ){
			WormTunnels t = get(level);
			if (t == null){
				t = new WormTunnels();
				t.seed(level, 0, 0);
				level.blobs.put(WormTunnels.class, t);
			}
			if (t.cur == null){
				t.cur = new int[level.length()];
				t.off = new int[level.length()];
			}
			return t;
		}

		void dig( int cell ){
			cur[cell] = 1;
			volume++;
			version++;
		}

		boolean covers( int cell ){
			return cur != null && cell >= 0 && cell < cur.length && cur[cell] > 0;
		}

		@Override
		public boolean act() {
			spend(TICK);
			return true;
		}

		@Override
		public void use( BlobEmitter emitter ) {
		}

		@Override
		public String tileDesc() {
			return Messages.get(this, "desc");
		}
	}
}
