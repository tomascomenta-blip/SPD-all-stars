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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ScaleArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Glaive;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SpecialRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forgotten.ForgottenFeatures;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.kraken.GiantTentacle;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.props.SKProps;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.undersea.UnderseaFeatures;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndQuest;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/*
 * flooded deep lab. undersea floor 17 or 18, in the imp's spot
 *
 * subjects 001, 002 and 004 got out (don't ask about 003). a very sorry scientist hiding inside wants
 * the 3 bulkheads sealed (1 in the lab, 2 somewhere on the floor) before the pressure bursts them.
 * 3 turns at the wheel each.
 * in time: in the Kraken fight the lightning hits its tentacles instead of the heroes (still hurts the ship)
 * too late: an ammonite (baby kraken) breaks out of the lab and you get to fight it
 */
public class LabQuest {

	public static class State extends QuestState {
		public float left = -1;        //turns until the bulkheads burst, -1 = quest not given yet
		public int sealed = 0;

		public State(){
			super("quest_lab");
		}

		@Override
		public void reset() {
			super.reset();
			left = -1;
			sealed = 0;
		}

		@Override
		protected void storeMore( Bundle b ) {
			b.put("left", left);
			b.put("sealed", sealed);
		}

		@Override
		protected void restoreMore( Bundle b ) {
			left = b.contains("left") ? b.getFloat("left") : -1;
			sealed = b.getInt("sealed");
		}

		@Override
		public boolean punish() {
			return failed;
		}
	}

	public static final State Q = SKQuests.LAB;

	//-Dspd.test.labtime=N -> N turns instead, to watch them burst
	public static final int TIME = DevFlags.labTime();

	//on/off for the whole quest. it's done, on
	public static boolean READY = true;

	public static ArrayList<Room> rooms( ArrayList<Room> rooms ){
		if (!READY || Q.depth != -1 || DevFlags.noQuests()) return rooms;
		if (Dungeon.depth == 17 && Random.Int(2) == 0 || Dungeon.depth == 18) rooms.add(new LabRoom());
		return rooms;
	}

	public static LabRoom lab( Level level ){
		if (!(level instanceof RegularLevel) || ((RegularLevel) level).rooms() == null) return null;
		for (Room r : ((RegularLevel) level).rooms()){
			if (r instanceof LabRoom) return (LabRoom) r;
		}
		return null;
	}

	public static void place( RegularLevel level ){
		LabRoom lab = lab(level);
		if (lab == null || Q.depth != -1) return;
		Q.depth = Dungeon.depth;
		Scientist s = new Scientist();
		s.pos = HuntQuest.freeCell(level, lab, -1);
		if (s.pos == -1) return;
		level.mobs.add(s);
		//1 bulkhead in the lab, 2 in the rooms farthest from it
		int inLab = HuntQuest.freeCell(level, lab, s.pos);
		if (inLab != -1) HuntQuest.add(level, new Bulkhead(), inLab);
		ArrayList<Room> others = new ArrayList<>();
		for (Room r : level.rooms()){
			if (r instanceof StandardRoom && r.width() >= 5 && r.height() >= 5) others.add(r);
		}
		final int mid = level.pointToCell(lab.center());
		Collections.sort(others, new Comparator<Room>() {
			@Override
			public int compare( Room a, Room b ){
				return level.distance(mid, level.pointToCell(b.center())) - level.distance(mid, level.pointToCell(a.center()));
			}
		});
		int made = 0;
		for (Room r : others){
			if (made >= 2) break;
			int c = HuntQuest.freeCell(level, r, -1);
			if (c == -1) continue;
			HuntQuest.add(level, new Bulkhead(), c);
			made++;
		}
		PathFinder.buildDistanceMap(level.entrance(), level.passable);
		CoopLog.line("lab placed depth=" + Dungeon.depth + " scientist=" + s.pos + " (" + PathFinder.distance[s.pos]
				+ " steps from the entrance) bulkheads=" + (made + 1));
	}

	static void sealedOne(){
		Q.sealed++;
		GLog.p(Messages.get(LabQuest.class, "sealed", Q.sealed));
		Sample.INSTANCE.play(Assets.Sounds.UNLOCK, 1f, 0.7f);
		if (Q.sealed >= 3 && !Q.failed){
			Q.slain = true;
			Q.bonus = Q.left >= TIME / 2f;
			MiniBossHud.banner(Messages.get(LabQuest.class, "banner_done"));
			GLog.p(Messages.get(LabQuest.class, "all_sealed"));
			CoopLog.line("lab: all sealed, " + (int) Q.left + " turns left");
		}
	}

	static void burst(){
		if (Q.failed || Q.sealed >= 3) return;
		Q.failed = true;
		LabRoom lab = lab(Dungeon.level);
		int cell = lab == null ? -1 : HuntQuest.freeCell(Dungeon.level, lab, -1);
		PixelScene.shake(4, 1f);
		Sample.INSTANCE.play(Assets.Sounds.BLAST, 1f, 0.6f);
		GLog.n(Messages.get(LabQuest.class, "burst"));
		MiniBossHud.banner(Messages.get(LabQuest.class, "banner_burst"));
		if (cell != -1){
			Ammonite a = new Ammonite();
			a.pos = cell;
			GameScene.add(a);
			a.state = a.HUNTING;
			a.startFight();
			if (Dungeon.level.heroFOV[cell]) CellEmitter.get(cell).burst(Speck.factory(Speck.BUBBLE), 12);
			//goes for the heroes wherever they are, no escape jaja
			Hero to = Party.active() ? (Party.living().isEmpty() ? null : Party.living().get(0)) : Dungeon.hero;
			if (to != null) a.beckon(to.pos);
		}
		CoopLog.line("lab: the bulkheads burst, ammonite at " + cell);
	}

	//scientist marks the bulkhead rooms on the map. least he can do
	static void reveal( Level level ){
		if (!(level instanceof RegularLevel)) return;
		for (Mob m : level.mobs){
			if (!(m instanceof Bulkhead)) continue;
			for (Room r : ((RegularLevel) level).rooms()){
				if (!r.inside(level.cellToPoint(m.pos))) continue;
				for (int y = r.top; y <= r.bottom; y++){
					for (int x = r.left; x <= r.right; x++) level.mapped[x + y * level.width()] = true;
				}
				break;
			}
		}
		GameScene.updateFog();
	}

	public static boolean helps(){
		return Q.done || (Q.sealed >= 3 && !Q.failed);
	}

	//bolt lands on a tentacle lying on the deck = big chunk of its hp
	public static boolean boltOnTentacle( Char ch ){
		if (!helps() || !(ch instanceof GiantTentacle.Segment)) return false;
		GiantTentacle t = ((GiantTentacle.Segment) ch).owner();
		if (t == null || !t.isAlive()) return false;
		t.hitFrom(t.armoured() ? t.sucker : -1, Math.max(8, t.HT / 8), new Object(), ch);
		return true;
	}

	// ---- scientist ----

	public static class Scientist extends NPC {

		{
			spriteClass = ScientistSprite.class;
			properties.add(Property.IMMOVABLE);
		}

		private int talk = 0;

		//ojo: countdown uses the floor's turns, NOT actor time (that gets shifted on save/load and the timer went nuts)
		@Override
		protected boolean act() {
			if (Q.given && !Q.failed && Q.sealed < 3 && Q.left > 0){
				Q.left -= TICK;
				if (Q.left <= 0) burst();
				else if (Q.left <= 40 && (int) Q.left % 20 == 0) GLog.w(Messages.get(LabQuest.class, "hurry", (int) Q.left));
			}
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
				Q.left = TIME;
				reveal(Dungeon.level);
				CoopLog.line("lab given");
				Game.runOnRenderThread(new Callback() {
					@Override
					public void call() {
						GameScene.show(new WndQuest(Scientist.this, Messages.get(Scientist.class, "intro")));
					}
				});
				GLog.p(Messages.get(Scientist.class, "intro_hint", TIME));
				return true;
			}
			if (Q.failed){
				yell(Messages.get(Scientist.class, "failed"));
				return true;
			}
			if (Q.sealed < 3){
				yell(Messages.get(Scientist.class, "hint_" + (talk++ % 3), 3 - Q.sealed));
				return true;
			}
			if (!Q.done){
				Q.done = true;
				Sample.INSTANCE.play(Assets.Sounds.MASTERY);
				yell(Messages.get(Scientist.class, Q.bonus ? "thanks_bonus" : "thanks"));
			}
			if (Q.claimedBy(h)){
				yell(Messages.get(Scientist.class, "after"));
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
					Messages.get(Scientist.class, Q.bonus ? "reward_bonus" : "reward"),
					Messages.get(DiveSuit.class, "name"),
					Messages.get(HarpoonGlaive.class, "name"),
					Messages.get(StableSample.class, "name")){
				@Override
				protected void onSelect( int index ) {
					if (Q.claimedBy(h)) return;
					Item item;
					switch (index){
						case 0: default: item = new DiveSuit(); break;
						case 1: item = new HarpoonGlaive(); break;
						case 2: item = new StableSample(); break;
					}
					if (item instanceof Artifact){ if (Q.bonus) item.upgrade(2); }
					else if (item.isUpgradable()) item.upgrade(Q.bonus ? 2 : 1);
					item.identify();
					Q.claim(h);
					if (!item.collect(h.belongings.backpack)) Dungeon.level.drop(item, h.pos).sprite.drop();
					GLog.p(Messages.get(Scientist.class, "got", item.name()));
					CoopLog.line("lab reward " + item.getClass().getSimpleName() + " for hero " + ChampionHooks.indexOf(h));
				}
				@Override
				public void onBackPressed() {
					//choice time
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

	public static class ScientistSprite extends SKMobSprite {
		public ScientistSprite(){
			super();
			setup(QuestAssets.SCIENTIST, 12, 15);
		}
	}

	// *** bulkhead, 3 turns at the wheel to seal ***

	public static class Bulkhead extends ForgottenFeatures.Thing {

		{
			spriteClass = BulkheadSprite.class;
		}

		public int turns = 0;
		public boolean sealed = false;

		@Override
		public boolean heroShouldInteract() {
			return !sealed;
		}

		@Override
		public boolean interact( Char c ) {
			if (!(c instanceof Hero) || sealed) return true;
			Hero h = (Hero) c;
			if (!Q.given || Q.failed){
				GLog.i(Messages.get(Bulkhead.class, Q.failed ? "too_late" : "stuck"));
				return true;
			}
			turns++;
			Sample.INSTANCE.play(Assets.Sounds.UNLOCK, 0.6f, 0.6f + 0.2f * turns);
			if (turns >= 3){
				sealed = true;
				if (sprite instanceof BulkheadSprite) ((BulkheadSprite) sprite).still(1);
				sealedOne();
			} else {
				GLog.i(Messages.get(Bulkhead.class, "turning", turns));
			}
			h.spendAndNext(1f);
			return false;
		}

		@Override
		public String description() {
			return Messages.get(this, sealed ? "desc_sealed" : "desc");
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("turns", turns);
			bundle.put("sealed", sealed);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			turns = bundle.getInt("turns");
			sealed = bundle.getBoolean("sealed");
		}
	}

	public static class BulkheadSprite extends ForgottenFeatures.StripSprite {
		public BulkheadSprite(){
			super();
			strip(QuestAssets.BULKHEAD, 16, 16, 1);
			still(0);
		}
		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (ch instanceof Bulkhead && ((Bulkhead) ch).sealed) still(1);
		}
		@Override
		public void still( int f ) {
			super.still(f);
		}
	}

	public static class LabRoom extends SpecialRoom {

		@Override
		public int minWidth() { return 9; }
		public int maxWidth() { return 9; }
		public int minHeight() { return 9; }
		public int maxHeight() { return 9; }

		@Override
		public void paint( Level level ) {
			Painter.fill(level, this, Terrain.WALL);
			Painter.fill(level, this, 1, Terrain.EMPTY_SP);
			Painter.fill(level, this, 2, Terrain.WATER);
			//the subjects' broken tanks. lovely decor
			Painter.set(level, left + 2, top + 2, Terrain.STATUE_SP);
			Painter.set(level, right - 2, top + 2, Terrain.STATUE_SP);
			Painter.set(level, left + 2, bottom - 2, Terrain.STATUE_SP);
			Painter.set(level, right - 2, bottom - 2, Terrain.STATUE_SP);
			entrance().set(Door.Type.REGULAR);
		}
	}

	// countdown at the top of the screen

	public static class LabHud extends Group {

		private final RenderedTextBlock text;
		private final ColorBlock back;
		private String shown = "";

		public static boolean wanted(){
			return READY && lab(Dungeon.level) != null;
		}

		public LabHud( float top ){
			back = new ColorBlock(10, 10, 0xAA001018);
			add(back);
			text = PixelScene.renderTextBlock(7);
			text.hardlight(0x80E0FF);
			add(text);
			back.y = top;
		}

		@Override
		public void update() {
			super.update();
			boolean show = Q.given && !Q.failed && Q.sealed < 3 && Q.left > 0;
			back.visible = text.visible = show;
			if (!show) return;
			String t = Messages.get(LabQuest.class, "hud", Q.sealed, (int) Math.ceil(Q.left));
			if (!t.equals(shown)){
				shown = t;
				text.text(t);
				float uiw = PixelScene.uiCamera.width;
				back.size(text.width() + 8, text.height() + 4);
				back.x = (uiw - back.width()) / 2f;
				text.setPos(back.x + 4, back.y + 2);
				PixelScene.align(text);
			}
		}
	}

	// *** rewards ***

	//lab dive suit, air lasts way longer under the sea
	public static class DiveSuit extends ScaleArmor {
		{
			image = ItemSpriteSheet.DIVE_SUIT;
		}
	}

	public static boolean suited( Char ch ){
		return ch instanceof Hero && ((Hero) ch).belongings.armor() instanceof DiveSuit;
	}

	//harpoon glaive. long reach, and sometimes the barb tears (bleed)
	public static class HarpoonGlaive extends Glaive {

		{
			image = ItemSpriteSheet.HARPOON_GLAIVE;
		}

		@Override
		public int proc( Char attacker, Char defender, int damage ) {
			if (Random.Int(4) == 0 && defender.isAlive() && !defender.properties().contains(Char.Property.INORGANIC)){
				Buff.affect(defender, Bleeding.class).set(3 + buffedLvl());
			}
			return super.proc(attacker, defender, damage);
		}
	}

	//stabilised serum sample. drink = heal + cleanse, the vial refills slowly
	public static class StableSample extends Artifact {

		public static final String AC_DRINK = "DRINK";

		{
			image = ItemSpriteSheet.STABLE_SAMPLE;
			levelCap = 10;
			charge = 0;
			chargeCap = 100;
			defaultAction = AC_DRINK;
			unique = true;
		}

		int heal(){
			return 15 + 4 * level();
		}

		@Override
		public ArrayList<String> actions( Hero hero ) {
			ArrayList<String> actions = super.actions(hero);
			if (isEquipped(hero) && !cursed) actions.add(AC_DRINK);
			return actions;
		}

		@Override
		public void execute( Hero hero, String action ) {
			super.execute(hero, action);
			if (!action.equals(AC_DRINK)) return;
			if (!isEquipped(hero)){
				GLog.i(Messages.get(Artifact.class, "need_to_equip"));
				return;
			}
			if (charge < chargeCap){
				GLog.i(Messages.get(this, "no_charge"));
				return;
			}
			charge = 0;
			hero.HP = Math.min(hero.HT, hero.HP + heal());
			PotionOfHealing.cure(hero);
			hero.sprite.emitter().burst(Speck.factory(Speck.HEALING), 4);
			Sample.INSTANCE.play(Assets.Sounds.DRINK);
			GLog.p(Messages.get(this, "drunk"));
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
			return new Brew();
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
			String desc = Messages.get(this, "desc", heal());
			if (isEquipped(Dungeon.hero)) desc += "\n\n" + Messages.get(this, charge >= chargeCap ? "desc_ready" : "desc_charging");
			return desc;
		}

		public class Brew extends ArtifactBuff {
			@Override
			public boolean act() {
				if (charge < chargeCap && !cursed){
					partialCharge += 0.3f + 0.04f * level();
					while (partialCharge >= 1 && charge < chargeCap){
						partialCharge--;
						charge++;
					}
					if (charge >= chargeCap) GLog.p(Messages.get(StableSample.class, "ready"));
					updateQuickslot();
				}
				spend(TICK);
				return true;
			}
		}
	}

	//true while some lab quest mob is still alive here
	static boolean ammoniteHere(){
		for (Mob m : Dungeon.level.mobs) if (m instanceof Ammonite && m.isAlive()) return true;
		return false;
	}

	static void ink( int cell ){
		GameScene.add(Blob.seed(cell, 60, UnderseaFeatures.InkCloud.class));
	}

	//tiny helpers so the props don't block paths
	static boolean free( Level L, int c ){
		return SKProps.usable(L, c) && L.findMob(c) == null;
	}
}
