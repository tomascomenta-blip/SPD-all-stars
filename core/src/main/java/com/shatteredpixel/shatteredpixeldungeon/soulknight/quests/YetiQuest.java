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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.PlateArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Gauntlet;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SpecialRoom;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKWorlds;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forgotten.ForgottenFeatures;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forgotten.Tharog;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forgotten.Tribe;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndQuest;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;

//yeti king, forgotten land floor 22 (next to the tribe's eternal flame)
//a tribe huntress waits by the stairs: the yeti king took the shaman's son to his ice cave.
//kill the king, free the kid, bring him back. cave icicles hurt the king too, hit him w/ two of them = bonus
//done: the kid tells the tribe where Tharog's plates crack (they protect less and break w/ enough dmg)
//not done: the yeti king comes down into Tharog's glaciation
public class YetiQuest {

	public static class State extends QuestState {
		public boolean freed = false;   //kid is out of the cave

		public State(){
			super("quest_yeti");
		}

		@Override
		public void reset() {
			super.reset();
			freed = false;
		}

		@Override
		protected void storeMore( Bundle b ) {
			b.put("freed", freed);
		}

		@Override
		protected void restoreMore( Bundle b ) {
			freed = b.getBoolean("freed");
		}
	}

	public static final State Q = SKQuests.YETI;

	//kill switch for the quest. all done, leave it true
	public static boolean READY = true;

	public static ArrayList<Room> rooms( ArrayList<Room> rooms ){
		if (!READY || Q.depth != -1 || Dungeon.depth != 22 || DevFlags.noQuests()) return rooms;
		rooms.add(new IceCave());
		return rooms;
	}

	public static IceCave cave( Level level ){
		if (!(level instanceof RegularLevel) || ((RegularLevel) level).rooms() == null) return null;
		for (Room r : ((RegularLevel) level).rooms()){
			if (r instanceof IceCave) return (IceCave) r;
		}
		return null;
	}

	public static void place( RegularLevel level, Room entrance ){
		IceCave c = cave(level);
		if (c == null || Q.depth != -1) return;
		int h = HuntQuest.freeCell(level, entrance, -1);
		if (h == -1) return;
		Q.depth = Dungeon.depth;
		HuntQuest.add(level, new Huntress(), h);
		YetiKing k = new YetiKing();
		k.pos = level.pointToCell(c.center());
		level.mobs.add(k);
		int boy = HuntQuest.freeCell(level, c, k.pos);
		if (boy != -1) HuntQuest.add(level, new Captive(), boy);
		CoopLog.line("yeti placed huntress=" + h + " king=" + k.pos + " boy=" + boy);
	}

	public static boolean helps(){
		return Q.done;
	}

	//Tharog wakes up, the kid told them where the plates crack
	public static void tharogFight(){
		if (helps()) GLog.p(Messages.get(YetiQuest.class, "cracks"));
	}

	public static void glaciation( Tharog t ){
		if (!Q.punish() || Q.failed) return;
		Q.failed = true;
		int cell = -1;
		for (int tries = 0; tries < 80 && cell == -1; tries++){
			int c = t.pos + Random.IntRange(-6, 6) + Random.IntRange(-6, 6) * Dungeon.level.width();
			if (c > 0 && c < Dungeon.level.length() && Dungeon.level.passable[c] && Actor.findChar(c) == null
					&& Dungeon.level.distance(c, t.pos) >= 3) cell = c;
		}
		if (cell == -1) return;
		YetiKing k = new YetiKing();
		k.pos = cell;
		GameScene.add(k);
		k.state = k.HUNTING;
		k.startFight();
		GLog.n(Messages.get(YetiQuest.class, "followed"));
		CoopLog.line("yeti quest failed: the king comes down at " + cell);
	}

	static ArrayList<Hero> heroes(){
		ArrayList<Hero> l = new ArrayList<>();
		if (Party.active()) l.addAll(Party.living());
		else if (Dungeon.hero != null && Dungeon.hero.isAlive()) l.add(Dungeon.hero);
		return l;
	}

	// *** tribe huntress ***

	public static class Huntress extends NPC {

		{
			spriteClass = Tribe.WarriorSprite.class;
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
				CoopLog.line("yeti given");
				Game.runOnRenderThread(new Callback() {
					@Override
					public void call() {
						GameScene.show(new WndQuest(Huntress.this, Messages.get(Huntress.class, "intro")));
					}
				});
				GLog.p(Messages.get(Huntress.class, "intro_hint"));
				return true;
			}
			if (!Q.done){
				if (!Q.freed){
					yell(Messages.get(Huntress.class, Q.slain ? "free_him" : "hint_" + (talk++ % 3)));
					return true;
				}
				Q.done = true;
				Sample.INSTANCE.play(Assets.Sounds.MASTERY);
				yell(Messages.get(Huntress.class, Q.bonus ? "thanks_bonus" : "thanks"));
			}
			if (Q.claimedBy(h)){
				yell(Messages.get(Huntress.class, "after"));
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
					Messages.get(Huntress.class, Q.bonus ? "reward_bonus" : "reward"),
					Messages.get(YetiClaws.class, "name"),
					Messages.get(YetiMantle.class, "name"),
					Messages.get(FrostFang.class, "name")){
				@Override
				protected void onSelect( int index ) {
					if (Q.claimedBy(h)) return;
					Item item;
					switch (index){
						case 0: default: item = new YetiClaws(); break;
						case 1: item = new YetiMantle(); break;
						case 2: item = new FrostFang(); break;
					}
					if (item instanceof Artifact){ if (Q.bonus) item.upgrade(2); }
					else if (item.isUpgradable()) item.upgrade(Q.bonus ? 2 : 1);
					item.identify();
					Q.claim(h);
					if (!item.collect(h.belongings.backpack)) Dungeon.level.drop(item, h.pos).sprite.drop();
					GLog.p(Messages.get(Huntress.class, "got", item.name()));
					CoopLog.line("yeti reward " + item.getClass().getSimpleName() + " for hero " + ChampionHooks.indexOf(h));
				}
				@Override
				public void onBackPressed() {
					//a choice must be made. (sounds epic, it's just a reward window)
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

	// *** the kid, tied up in the cave ***

	public static class Captive extends ForgottenFeatures.Thing {

		{
			spriteClass = CaptiveSprite.class;
		}

		@Override
		public boolean heroShouldInteract() {
			return !Q.freed;
		}

		@Override
		public boolean interact( Char c ) {
			if (!(c instanceof Hero) || Q.freed) return true;
			for (Mob m : Dungeon.level.mobs){
				if (m instanceof YetiKing && m.isAlive()){
					GLog.w(Messages.get(Captive.class, "king_alive"));
					return true;
				}
			}
			Q.freed = true;
			yell(Messages.get(Captive.class, "freed"));
			GLog.p(Messages.get(Captive.class, "freed_hint"));
			Sample.INSTANCE.play(Assets.Sounds.UNLOCK);
			if (sprite != null) CellEmitter.get(pos).burst(Speck.factory(Speck.STEAM), 6);
			destroy();
			if (sprite != null) sprite.killAndErase();
			((Hero) c).spendAndNext(1f);
			return false;
		}

		@Override
		public String description() {
			return Messages.get(this, "desc");
		}
	}

	public static class CaptiveSprite extends ForgottenFeatures.StripSprite {
		public CaptiveSprite(){
			super();
			strip(QuestAssets.CAPTIVE, 16, 16, 2);
		}
	}

	// ice cave

	public static class IceCave extends SpecialRoom {

		@Override
		public int minWidth() { return 13; }
		public int maxWidth() { return 13; }
		public int minHeight() { return 11; }
		public int maxHeight() { return 11; }

		@Override
		public void paint( Level level ) {
			Painter.fill(level, this, Terrain.WALL);
			Painter.fill(level, this, 1, Terrain.EMPTY);
			Painter.fill(level, this, 3, Terrain.EMPTY_SP);
			//ice columns, para que no sea una sala vacía
			Painter.set(level, left + 3, top + 3, Terrain.STATUE);
			Painter.set(level, right - 3, top + 3, Terrain.STATUE);
			Painter.set(level, left + 3, bottom - 3, Terrain.STATUE);
			Painter.set(level, right - 3, bottom - 3, Terrain.STATUE);
			entrance().set(Door.Type.REGULAR);
		}

		public boolean inCave( Level level, int cell ){
			return inside(level.cellToPoint(cell));
		}
	}

	// *** rewards ***

	//yeti king claws. quick hits, chill sometimes
	public static class YetiClaws extends Gauntlet {

		{
			image = ItemSpriteSheet.YETI_CLAWS;
		}

		@Override
		public int proc( Char attacker, Char defender, int damage ) {
			if (Random.Int(4) == 0 && defender.isAlive()) Buff.prolong(defender, Chill.class, 3f);
			return super.proc(attacker, defender, damage);
		}
	}

	//yeti king hide: thick fur plate, the forgotten land cold doesn't bite
	public static class YetiMantle extends PlateArmor {
		{
			image = ItemSpriteSheet.YETI_MANTLE;
		}
	}

	public static boolean mantled( Char ch ){
		return ch instanceof Hero && ((Hero) ch).belongings.armor() instanceof YetiMantle;
	}

	//ice fang. strike it, frost wave around the bearer
	public static class FrostFang extends Artifact {

		public static final String AC_FREEZE = "FREEZE";

		{
			image = ItemSpriteSheet.FROST_FANG;
			levelCap = 10;
			charge = 0;
			chargeCap = 100;
			defaultAction = AC_FREEZE;
			unique = true;
		}

		@Override
		public ArrayList<String> actions( Hero hero ) {
			ArrayList<String> actions = super.actions(hero);
			if (isEquipped(hero) && !cursed) actions.add(AC_FREEZE);
			return actions;
		}

		@Override
		public void execute( Hero hero, String action ) {
			super.execute(hero, action);
			if (!action.equals(AC_FREEZE)) return;
			if (!isEquipped(hero)){
				GLog.i(Messages.get(Artifact.class, "need_to_equip"));
				return;
			}
			if (charge < chargeCap){
				GLog.i(Messages.get(this, "no_charge"));
				return;
			}
			charge = 0;
			int r = 2 + level() / 4;
			for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
				if (m.alignment != Char.Alignment.ENEMY || Dungeon.level.distance(m.pos, hero.pos) > r) continue;
				m.damage(Random.NormalIntRange(8 + 2 * level(), 16 + 3 * level()), this);
				if (m.isAlive()) Buff.prolong(m, Chill.class, 4f);
			}
			CellEmitter.get(hero.pos).burst(Speck.factory(Speck.STEAM), 16);
			Sample.INSTANCE.play(Assets.Sounds.SHATTER, 1f, 1.2f);
			GLog.p(Messages.get(this, "freeze"));
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
			return new Frost();
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
			String desc = Messages.get(this, "desc", 2 + level() / 4);
			if (isEquipped(Dungeon.hero)) desc += "\n\n" + Messages.get(this, charge >= chargeCap ? "desc_ready" : "desc_charging");
			return desc;
		}

		public class Frost extends ArtifactBuff {
			@Override
			public boolean act() {
				if (charge < chargeCap && !cursed && target instanceof Hero && ((Hero) target).visibleEnemies() > 0){
					partialCharge += 1.4f + 0.15f * level();
					while (partialCharge >= 1 && charge < chargeCap){
						partialCharge--;
						charge++;
					}
					if (charge >= chargeCap) GLog.p(Messages.get(FrostFang.class, "ready"));
					updateQuickslot();
				}
				spend(TICK);
				return true;
			}
		}
	}

	public static boolean here(){
		return SKWorlds.isSK(Dungeon.depth) && cave(Dungeon.level) != null;
	}
}
