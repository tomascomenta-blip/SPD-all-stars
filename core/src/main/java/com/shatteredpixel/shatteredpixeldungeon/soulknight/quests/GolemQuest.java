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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.PlateArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WarHammer;
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
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndQuest;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

/*
 * great golem of the royal vault (dwarven city, 17-19, next to the imp)
 * old dwarf treasurer wants the vault's treasure back but the guardian golem never sleeps.
 * 4 runes on the vault floor buff it (less dmg taken + regen). a hero standing on a rune for a turn puts it out.
 * all 4 out when it dies = bonus
 *
 * done: golem core stuns the dwarf king when his last phase starts
 * not done: the golem comes to guard the king's throne
 */
public class GolemQuest {

	public static class State extends QuestState {
		public int[] runes = new int[0];  //rune cells in the vault
		public boolean[] lit = new boolean[0];

		public State(){
			super("quest_golem");
		}

		@Override
		public void reset() {
			super.reset();
			runes = new int[0];
			lit = new boolean[0];
		}

		@Override
		protected void storeMore( Bundle b ) {
			b.put("runes", runes);
			b.put("lit", lit);
		}

		@Override
		protected void restoreMore( Bundle b ) {
			runes = b.contains("runes") ? b.getIntArray("runes") : new int[0];
			lit = b.contains("lit") ? b.getBooleanArray("lit") : new boolean[0];
		}

		public int litCount(){
			int n = 0;
			for (boolean l : lit) if (l) n++;
			return n;
		}
	}

	public static final State Q = SKQuests.GOLEM;

	//quest switch. was off while the art wasn't finished, it is now
	public static boolean READY = true;

	public static ArrayList<Room> rooms( ArrayList<Room> rooms ){
		if (!READY || Q.depth != -1 || SKWorlds.isSK(Dungeon.depth) || DevFlags.noQuests()) return rooms;
		if (Dungeon.depth < 17 || Dungeon.depth > 19) return rooms;
		if (Dungeon.depth == 19 || DevFlags.forceQuest("golem") || Random.Int(20 - Dungeon.depth) == 0){
			rooms.add(new VaultRoom());
		}
		return rooms;
	}

	public static VaultRoom vault( Level level ){
		if (!(level instanceof RegularLevel) || ((RegularLevel) level).rooms() == null) return null;
		for (Room r : ((RegularLevel) level).rooms()){
			if (r instanceof VaultRoom) return (VaultRoom) r;
		}
		return null;
	}

	public static void place( RegularLevel level, Room entrance ){
		VaultRoom v = vault(level);
		if (v == null || Q.depth != -1) return;
		int t = HuntQuest.freeCell(level, entrance, -1);
		if (t == -1) return;
		Q.depth = Dungeon.depth;
		HuntQuest.add(level, new Treasurer(), t);
		GreatGolem g = new GreatGolem();
		g.pos = level.pointToCell(v.center());
		level.mobs.add(g);
		int w = level.width();
		int cx = (v.left + v.right) / 2, cy = (v.top + v.bottom) / 2;
		Q.runes = new int[]{cx - 3 + (cy - 3) * w, cx + 3 + (cy - 3) * w, cx - 3 + (cy + 3) * w, cx + 3 + (cy + 3) * w};
		Q.lit = new boolean[]{true, true, true, true};
		CoopLog.line("golem placed depth=" + Dungeon.depth + " treasurer=" + t + " golem=" + g.pos);
	}

	static boolean onRune( int cell ){
		for (int i = 0; i < Q.runes.length; i++) if (Q.runes[i] == cell && Q.lit[i]) return true;
		return false;
	}

	//standing on a lit rune puts it out (takes a turn, no vale pasar por encima)
	static void stepCheck( ArrayList<Hero> heroes ){
		for (Hero h : heroes){
			for (int i = 0; i < Q.runes.length; i++){
				if (Q.runes[i] == h.pos && Q.lit[i]){
					Q.lit[i] = false;
					GLog.p(Messages.get(GolemQuest.class, "rune_out", Q.litCount()));
					Sample.INSTANCE.play(Assets.Sounds.DEGRADE, 1f, 0.8f);
					if (Dungeon.level.heroFOV[h.pos]) CellEmitter.get(h.pos).burst(Speck.factory(Speck.SMOKE), 6);
				}
			}
		}
	}

	public static void kingFight( DwarfKing king ){
		if (Q.done){
			GLog.p(Messages.get(GolemQuest.class, "core_ready"));
			return;
		}
		if (!Q.punish() || Q.failed) return;
		Q.failed = true;
		GreatGolem g = new GreatGolem();
		g.guard = true;
		int cell = -1;
		for (int tries = 0; tries < 80 && cell == -1; tries++){
			int c = king.pos + Random.IntRange(-4, 4) + Random.IntRange(-4, 4) * Dungeon.level.width();
			//it's a big boy, only fits in open space
			if (c > 0 && c < Dungeon.level.length() && Dungeon.level.passable[c] && Dungeon.level.openSpace[c]
					&& Actor.findChar(c) == null && Dungeon.level.distance(c, king.pos) >= 2) cell = c;
		}
		if (cell == -1) return;
		g.pos = cell;
		GameScene.add(g);
		g.state = g.HUNTING;
		g.startFight();
		GLog.n(Messages.get(GolemQuest.class, "guards"));
		CoopLog.line("golem quest failed: it guards the throne at " + cell);
	}

	//king's last phase starts -> core stuns him
	public static void kingPhase3( DwarfKing king ){
		if (!Q.done) return;
		Buff.prolong(king, Paralysis.class, 4f);
		CoopLog.line("golem core stuns the king");
		if (king.sprite != null) king.sprite.emitter().burst(Speck.factory(Speck.STAR), 8);
		Sample.INSTANCE.play(Assets.Sounds.LIGHTNING);
		GLog.p(Messages.get(GolemQuest.class, "core_stuns"));
	}

	// *** the treasurer ***

	public static class Treasurer extends NPC {

		{
			spriteClass = TreasurerSprite.class;
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
				CoopLog.line("golem given");
				Game.runOnRenderThread(new Callback() {
					@Override
					public void call() {
						GameScene.show(new WndQuest(Treasurer.this, Messages.get(Treasurer.class, "intro")));
					}
				});
				GLog.p(Messages.get(Treasurer.class, "intro_hint"));
				return true;
			}
			if (!Q.done){
				GolemCore core = h.belongings.getItem(GolemCore.class);
				if (core == null){
					yell(Messages.get(Treasurer.class, Q.slain ? "bring" : "hint_" + (talk++ % 3)));
					return true;
				}
				//he only writes it in his ledger, the core goes on to the throne w/ the heroes
				Q.done = true;
				Sample.INSTANCE.play(Assets.Sounds.MASTERY);
				yell(Messages.get(Treasurer.class, Q.bonus ? "thanks_bonus" : "thanks"));
			}
			if (Q.claimedBy(h)){
				yell(Messages.get(Treasurer.class, "after"));
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
					Messages.get(Treasurer.class, Q.bonus ? "reward_bonus" : "reward"),
					Messages.get(RuneHammer.class, "name"),
					Messages.get(GolemPlate.class, "name"),
					Messages.get(RuneStone.class, "name")){
				@Override
				protected void onSelect( int index ) {
					if (Q.claimedBy(h)) return;
					Item item;
					switch (index){
						case 0: default: item = new RuneHammer(); break;
						case 1: item = new GolemPlate(); break;
						case 2: item = new RuneStone(); break;
					}
					if (item instanceof Artifact){ if (Q.bonus) item.upgrade(2); }
					else if (item.isUpgradable()) item.upgrade(Q.bonus ? 2 : 1);
					item.identify();
					Q.claim(h);
					if (!item.collect(h.belongings.backpack)) Dungeon.level.drop(item, h.pos).sprite.drop();
					GLog.p(Messages.get(Treasurer.class, "got", item.name()));
					CoopLog.line("golem reward " + item.getClass().getSimpleName() + " for hero " + ChampionHooks.indexOf(h));
				}
				@Override
				public void onBackPressed() {
					//choose. ya
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

	public static class TreasurerSprite extends SKMobSprite {
		public TreasurerSprite(){
			super();
			setup(QuestAssets.TREASURER, 12, 14);
		}
	}

	public static class VaultRoom extends SpecialRoom {

		@Override
		public int minWidth() { return 11; }
		public int maxWidth() { return 11; }
		public int minHeight() { return 11; }
		public int maxHeight() { return 11; }

		@Override
		public void paint( Level level ) {
			Painter.fill(level, this, Terrain.WALL);
			Painter.fill(level, this, 1, Terrain.EMPTY_SP);
			Painter.fill(level, this, 3, Terrain.EMPTY);
			entrance().set(Door.Type.REGULAR);
		}
	}

	//vault runes, drawn under everything else
	public static class RuneVisuals extends Group {
		private TextureFilm film;
		private final ArrayList<Image> imgs = new ArrayList<>();
		private float t = 0;

		@Override
		public void update() {
			super.update();
			t += Game.elapsed;
			if (Q.runes.length == 0 || Q.depth != Dungeon.depth) return;
			if (imgs.isEmpty()){
				for (int c : Q.runes){
					Image im = new Image(QuestAssets.RUNE);
					if (film == null) film = new TextureFilm(im.texture, 16, 16);
					PointF p = DungeonTilemap.tileToWorld(c);
					im.x = p.x;
					im.y = p.y;
					imgs.add(im);
					add(im);
				}
			}
			for (int i = 0; i < imgs.size() && i < Q.lit.length; i++){
				imgs.get(i).frame(film.get(Q.lit[i] ? (((int) (t * 3) + i) % 2) : 2));
			}
		}
	}

	// *** items ***

	public static class GolemCore extends Item {
		{
			image = ItemSpriteSheet.GOLEM_CORE;
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

		@Override
		public String desc() {
			return Messages.get(this, Q.done ? "desc_done" : "desc");
		}
	}

	//rune hammer, 1 in 5 hits dazes. bonk
	public static class RuneHammer extends WarHammer {

		{
			image = ItemSpriteSheet.RUNE_HAMMER;
			tier = 4;
		}

		@Override
		public int proc( Char attacker, Char defender, int damage ) {
			if (Random.Int(5) == 0 && defender.isAlive() && !defender.properties().contains(Char.Property.BOSS)){
				Buff.prolong(defender, Paralysis.class, 1f);
				if (defender.sprite != null) defender.sprite.emitter().burst(Speck.factory(Speck.STAR), 4);
			}
			return super.proc(attacker, defender, damage);
		}
	}

	//golem plates. heavy but they shave a few points off every hit
	public static class GolemPlate extends PlateArmor {

		{
			image = ItemSpriteSheet.GOLEM_PLATE;
		}

		@Override
		public int proc( Char attacker, Char defender, int damage ) {
			return Math.max(0, super.proc(attacker, defender, damage) - 1 - buffedLvl() / 3);
		}
	}

	//rune stone: struck, it sends a stun pulse around the bearer
	public static class RuneStone extends Artifact {

		public static final String AC_SET = "SET";

		{
			image = ItemSpriteSheet.RUNE_STONE;
			levelCap = 10;
			charge = 0;
			chargeCap = 100;
			defaultAction = AC_SET;
			unique = true;
		}

		@Override
		public ArrayList<String> actions( Hero hero ) {
			ArrayList<String> actions = super.actions(hero);
			if (isEquipped(hero) && !cursed) actions.add(AC_SET);
			return actions;
		}

		@Override
		public void execute( Hero hero, String action ) {
			super.execute(hero, action);
			if (!action.equals(AC_SET)) return;
			if (!isEquipped(hero)){
				GLog.i(Messages.get(Artifact.class, "need_to_equip"));
				return;
			}
			if (charge < chargeCap){
				GLog.i(Messages.get(this, "no_charge"));
				return;
			}
			charge = 0;
			//stun pulse, bigger at higher lvl
			int r = 1 + level() / 4;
			for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
				if (m.alignment == Char.Alignment.ENEMY && Dungeon.level.distance(m.pos, hero.pos) <= r
						&& !m.properties().contains(Char.Property.BOSS)){
					Buff.prolong(m, Paralysis.class, 2f + level() / 3);
				}
			}
			CellEmitter.get(hero.pos).burst(Speck.factory(Speck.STAR), 10);
			Sample.INSTANCE.play(Assets.Sounds.LIGHTNING, 0.8f, 1.2f);
			GLog.p(Messages.get(this, "pulse"));
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
			return new Glow();
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
			String desc = Messages.get(this, "desc", 1 + level() / 4);
			if (isEquipped(Dungeon.hero)) desc += "\n\n" + Messages.get(this, charge >= chargeCap ? "desc_ready" : "desc_charging");
			return desc;
		}

		public class Glow extends ArtifactBuff {
			@Override
			public boolean act() {
				if (charge < chargeCap && !cursed && target instanceof Hero && ((Hero) target).visibleEnemies() > 0){
					partialCharge += 1.4f + 0.15f * level();
					while (partialCharge >= 1 && charge < chargeCap){
						partialCharge--;
						charge++;
					}
					if (charge >= chargeCap) GLog.p(Messages.get(RuneStone.class, "ready"));
					updateQuickslot();
				}
				spend(TICK);
				return true;
			}
		}
	}
}
