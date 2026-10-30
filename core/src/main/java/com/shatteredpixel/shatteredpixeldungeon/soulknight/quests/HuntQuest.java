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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Spear;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.secret.SecretRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SpecialRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forest.GoblinPriest;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forest.GoblinSpearman;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forgotten.ForgottenFeatures;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.props.SKProps;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.starship.HiveHeart;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndQuest;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

//Grey Tusk hunt. forest, takes the Ghost's place there
//old hunter camping by the stairs (floor 2-4) wants the tusk of the alpha boar that runs the floor.
//the boar bails 3 times mid fight, smashing walls to get somewhere else, and you follow the tracks
//finding the stuff it wrecked (snares, berries, goblin spears, trashed camps). last stand in its den
//more snares on it = cleaner hunt = better rewards
//w/ the tusk the hunter helps vs the high priest (giant star needs half the dmg)
//w/o it the boar follows you to the priest's clearing. which is way worse
public class HuntQuest {

	public static final QuestState Q = SKQuests.HUNT;

	//on. everything's in, leave it
	public static boolean READY = true;

	// placing it, same idea as the Ghost

	public static void spawn( RegularLevel level, ArrayList<Room> rooms, Room entrance ){
		if (!READY || Q.depth != -1 || Dungeon.depth < 2 || Dungeon.depth > 4) return;
		if (DevFlags.noQuests()) return;
		if (!DevFlags.forceQuest("hunt") && Random.Int(5 - Dungeon.depth) != 0) return;

		//rooms by distance from the stairs. can't use PathFinder here, it isn't sized for a level that's still being built
		final int from = level.entrance();
		//big rooms = better chase, small ones ok if the floor is cramped (pasa más de lo que crees)
		ArrayList<Room> far = new ArrayList<>();
		for (int min = 5; min >= 3 && far.size() < 4; min--){
			far.clear();
			for (Room r : rooms){
				if (!(r instanceof StandardRoom) || r == entrance) continue;
				if (r.width() < min || r.height() < min) continue;
				far.add(r);
			}
		}
		//few rooms? corridors and junctions work as stops
		//TODO looks kinda dumb on really tiny floors, revisit someday
		if (far.size() < 4){
			ArrayList<Room> more = new ArrayList<>();
			for (Room r : rooms){
				if (r == null || r == entrance || far.contains(r)) continue;
				if (r instanceof SpecialRoom || r instanceof SecretRoom) continue;
				if (r.width() < 3 || r.height() < 3) continue;
				more.add(r);
			}
			Collections.sort(more, new Comparator<Room>() {
				@Override
				public int compare( Room a, Room b ){
					return b.width() * b.height() - a.width() * a.height();
				}
			});
			for (Room r : more){
				if (far.size() >= 4) break;
				far.add(r);
			}
		}
		if (far.size() < 4){
			CoopLog.line("hunt: not enough rooms (" + far.size() + " of " + rooms.size() + ")");
			return;
		}
		Collections.sort(far, new Comparator<Room>() {
			@Override
			public int compare( Room a, Room b ){
				return level.distance(from, level.pointToCell(a.center())) - level.distance(from, level.pointToCell(b.center()));
			}
		});
		int n = far.size();
		Room home = far.get(Math.max(0, n * 2 / 5 - 1));
		Room stop1 = far.get(Math.min(n - 3, Math.max(n * 3 / 5, far.indexOf(home) + 1)));
		Room stop2 = far.get(Math.min(n - 2, Math.max(n * 4 / 5, far.indexOf(stop1) + 1)));
		Room den = far.get(n - 1);

		int hunterCell = freeCell(level, entrance, -1);
		int homeCell = freeCell(level, home, -1);
		int denCell = freeCell(level, den, -1);
		if (hunterCell == -1 || homeCell == -1 || denCell == -1){
			CoopLog.line("hunt: no free cells " + hunterCell + " " + homeCell + " " + denCell);
			return;
		}

		Hunter h = new Hunter();
		add(level, h, hunterCell);
		int camp = freeCell(level, entrance, hunterCell);
		if (camp != -1) add(level, new HunterCamp(), camp);

		GreyTusk tusk = new GreyTusk();
		//the middle of a corridor can be a wall (yes really), use a free cell of it
		int s1 = freeCell(level, stop1, -1), s2 = freeCell(level, stop2, -1);
		if (s1 == -1) s1 = level.pointToCell(stop1.center());
		if (s2 == -1) s2 = level.pointToCell(stop2.center());
		tusk.stops = new int[]{s1, s2, denCell};
		add(level, tusk, homeCell);
		int denProp = freeCell(level, den, denCell);
		if (denProp != -1) add(level, new Den(), denProp);

		Q.depth = Dungeon.depth;
		CoopLog.line("hunt placed depth=" + Dungeon.depth + " hunter=" + hunterCell + " tusk=" + homeCell
				+ " stops=" + tusk.stops[0] + "," + tusk.stops[1] + " den=" + denCell);
	}

	static void add( Level level, Mob m, int cell ){
		m.pos = cell;
		level.mobs.add(m);
	}

	//cell of r where a prop or mob fits, near the middle, and != not (yes the param is called not, sorry)
	static int freeCell( Level level, Room r, int not ){
		//best cells frist. room has none (water, statues...)? any free floor then
		for (int strict = 2; strict >= 0; strict--){
			int c = freeCell(level, r, not, strict);
			if (c != -1) return c;
		}
		return -1;
	}

	private static int freeCell( Level level, Room r, int not, int strict ){
		ArrayList<Integer> ok = new ArrayList<>();
		for (int y = r.top + 1; y < r.bottom; y++){
			for (int x = r.left + 1; x < r.right; x++){
				int c = x + y * level.width();
				if (c == not || c == level.entrance() || c == level.exit()) continue;
				if (level.findMob(c) != null || level.heaps.get(c) != null) continue;
				if (strict == 2 && !SKProps.usable(level, c)) continue;
				if (strict < 2 && (!level.passable[c] || level.solid[c] || level.pit[c] || stairs(level, c))) continue;
				if (strict == 1 && (level.water[c] || level.map[c] == Terrain.DOOR || level.map[c] == Terrain.OPEN_DOOR)) continue;
				ok.add(c);
			}
		}
		if (ok.isEmpty()) return -1;
		final int mid = level.pointToCell(r.center());
		Collections.sort(ok, new Comparator<Integer>() {
			@Override
			public int compare( Integer a, Integer b ){
				return level.distance(a, mid) - level.distance(b, mid);
			}
		});
		return ok.get(Math.min(ok.size() - 1, Random.Int(Math.min(4, ok.size()))));
	}

	//stairs cell? safe while the level is still being built (Level.getTransition reads Dungeon.level, aprendido a las malas)
	static boolean stairs( Level level, int c ){
		if (c == level.entrance() || c == level.exit()) return true;
		if (level.transitions == null) return false;
		Point p = level.cellToPoint(c);
		for (LevelTransition t : level.transitions){
			if (t.inside(p)) return true;
		}
		return false;
	}

	//hunter tagged along to the priest's clearing
	public static boolean hunterHelps(){
		return Q.done;
	}

	//clearing built: if he got his tusk the hunter waits by a door
	public static void placeHelper( Level level, int[] doors, int clrL, int clrT, int clrR, int clrB ){
		if (!Q.done || doors == null || doors.length == 0) return;
		int w = level.width();
		for (int d : doors){
			for (int n : PathFinder.NEIGHBOURS8){
				int c = d + n;
				int x = c % w, y = c / w;
				if (x <= clrL || x >= clrR || y <= clrT || y >= clrB) continue;
				//flag maps aren't built yet while painting, so check Terrain.flags by hand
				if ((Terrain.flags[level.map[c]] & Terrain.PASSABLE) == 0 || level.findMob(c) != null) continue;
				Hunter h = new Hunter();
				h.helper = true;
				add(level, h, c);
				CoopLog.line("hunt: the hunter waits at the clearing, " + c);
				return;
			}
		}
	}

	//priest fight starts. whatever you left undone gets paid now
	public static void priestFight( GoblinPriest priest ){
		if (Q.punish() && !Q.failed){
			Q.failed = true;
			int cell = -1;
			for (int tries = 0; tries < 60 && cell == -1; tries++){
				int c = priest.pos + Random.IntRange(-5, 5) + Random.IntRange(-5, 5) * Dungeon.level.width();
				if (c > 0 && c < Dungeon.level.length() && Dungeon.level.passable[c] && Actor.findChar(c) == null
						&& Dungeon.level.distance(c, priest.pos) >= 3) cell = c;
			}
			if (cell == -1) return;
			GreyTusk t = new GreyTusk();
			t.pos = cell;
			GameScene.add(t);
			t.HP = t.HT * 4 / 5;
			t.state = t.HUNTING;
			t.startFight();
			GLog.n(Messages.get(HuntQuest.class, "followed"));
			CoopLog.line("hunt failed: grey tusk joins the priest at " + cell);
		}
		if (Q.done){
			for (Mob m : Dungeon.level.mobs){
				if (m instanceof Hunter && ((Hunter) m).helper) m.yell(Messages.get(Hunter.class, "helper_start"));
			}
		}
	}

	//giant star: hunter shoots at it, breaks with half the dmg
	public static int starNeed( int need ){
		if (!hunterHelps()) return need;
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof Hunter && ((Hunter) m).helper) m.yell(Messages.get(Hunter.class, "helper_star"));
		}
		return Math.max(4, need / 2);
	}

	// *** the hunter ***

	public static class Hunter extends NPC {

		{
			spriteClass = HunterSprite.class;
			properties.add(Property.IMMOVABLE);
		}

		public boolean helper = false; //the one at the priest's clearing
		private int talk = 0;

		@Override
		protected boolean act() {
			if (helper){
				GoblinPriest p = null;
				for (Mob m : Dungeon.level.mobs) if (m instanceof GoblinPriest) p = (GoblinPriest) m;
				if (p != null && p.starTimer > 0 && sprite != null && Dungeon.level.heroFOV[pos]){
					final GoblinPriest target = p;
					sprite.zap(p.pos);
					((MissileSprite) sprite.parent.recycle(MissileSprite.class)).reset(
							sprite, target.sprite, new SKMobSprite.Shot(ItemSpriteSheet.SHOT_ARROW), new Callback() {
								@Override
								public void call() {
								}
							});
				}
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
			if (helper){
				yell(Messages.get(Hunter.class, "helper_talk"));
				return true;
			}
			if (!Q.given){
				Q.given = true;
				CoopLog.line("hunt given");
				Game.runOnRenderThread(new Callback() {
					@Override
					public void call() {
						GameScene.show(new WndQuest(Hunter.this, Messages.get(Hunter.class, "intro")));
					}
				});
				GLog.p(Messages.get(Hunter.class, "intro_hint"));
				return true;
			}
			if (!Q.done){
				GreyTuskFang fang = h.belongings.getItem(GreyTuskFang.class);
				if (fang == null){
					yell(Messages.get(Hunter.class, Q.slain ? "bring" : "hint_" + (talk++ % 3)));
					return true;
				}
				fang.detachAll(h.belongings.backpack);
				Q.done = true;
				Sample.INSTANCE.play(Assets.Sounds.MASTERY);
				yell(Messages.get(Hunter.class, Q.bonus ? "thanks_bonus" : "thanks"));
				CoopLog.line("hunt done bonus=" + Q.bonus);
			}
			if (Q.claimedBy(h)){
				yell(Messages.get(Hunter.class, "after"));
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
					Messages.get(Hunter.class, Q.bonus ? "reward_bonus" : "reward"),
					Messages.get(TuskSpear.class, "name"),
					Messages.get(BoarCloak.class, "name"),
					Messages.get(HuntingHorn.class, "name")){
				@Override
				protected void onSelect( int index ) {
					if (Q.claimedBy(h)) return;
					Item item;
					switch (index){
						case 0: default: item = new TuskSpear(); break;
						case 1: item = new BoarCloak(); break;
						case 2: item = new HuntingHorn(); break;
					}
					if (item.isUpgradable()) item.upgrade(Q.bonus ? 2 : 1);
					else if (item instanceof HuntingHorn && Q.bonus) ((HuntingHorn) item).boost();
					item.identify();
					Q.claim(h);
					if (!item.collect(h.belongings.backpack)) Dungeon.level.drop(item, h.pos).sprite.drop();
					GLog.p(Messages.get(Hunter.class, "got", item.name()));
					CoopLog.line("hunt reward " + item.getClass().getSimpleName() + " for hero " + ChampionHooks.indexOf(h));
				}
				@Override
				public void onBackPressed() {
					//one. no, you can't have all three
				}
			});
		}

		@Override
		public String description() {
			return Messages.get(this, helper ? "desc_helper" : "desc");
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("helper", helper);
			bundle.put("talk", talk);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			helper = bundle.getBoolean("helper");
			talk = bundle.getInt("talk");
		}
	}

	public static class HunterSprite extends SKMobSprite {
		public HunterSprite(){
			super();
			setup(QuestAssets.HUNTER, 12, 15);
		}
	}

	// --- props

	public static class HunterCamp extends ForgottenFeatures.Thing {
		{
			spriteClass = HunterCampSprite.class;
		}
		@Override
		public String description() {
			return Messages.get(this, "desc");
		}
	}

	public static class HunterCampSprite extends ForgottenFeatures.StripSprite {
		public HunterCampSprite(){
			super();
			strip(QuestAssets.HUNTER_CAMP, 32, 24, 1);
		}
	}

	public static class Den extends ForgottenFeatures.Thing {
		{
			spriteClass = DenSprite.class;
		}
		@Override
		public String description() {
			return Messages.get(this, "desc");
		}
	}

	public static class DenSprite extends ForgottenFeatures.StripSprite {
		public DenSprite(){
			super();
			strip(QuestAssets.DEN, 32, 24, 1);
		}
	}

	//what's left of a goblin camp the boar trashed. some gold/food around
	public static class WreckedCamp extends ForgottenFeatures.Thing {
		{
			spriteClass = WreckedCampSprite.class;
		}
		@Override
		public String description() {
			return Messages.get(this, "desc");
		}
	}

	public static class WreckedCampSprite extends ForgottenFeatures.StripSprite {
		public WreckedCampSprite(){
			super();
			strip(QuestAssets.WRECKED_CAMP, 32, 20, 1);
		}
	}

	//wild berries, eat = small heal, once. not poisonous, promise
	public static class BerryBush extends ForgottenFeatures.Thing {

		{
			spriteClass = BerryBushSprite.class;
		}

		public boolean eaten = false;

		@Override
		public boolean heroShouldInteract() {
			return !eaten;
		}

		@Override
		public boolean interact( Char c ) {
			if (!(c instanceof Hero) || eaten) return true;
			Hero h = (Hero) c;
			eaten = true;
			int heal = Math.max(5, h.HT * 3 / 10);
			h.HP = Math.min(h.HT, h.HP + heal);
			h.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
			h.sprite.emitter().burst(Speck.factory(Speck.HEALING), 3);
			Sample.INSTANCE.play(Assets.Sounds.EAT);
			GLog.p(Messages.get(BerryBush.class, "eat"));
			if (sprite instanceof BerryBushSprite) ((BerryBushSprite) sprite).still(1);
			h.spendAndNext(1f);
			return false;
		}

		@Override
		public String description() {
			return Messages.get(this, eaten ? "desc_eaten" : "desc");
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("eaten", eaten);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			eaten = bundle.getBoolean("eaten");
		}
	}

	public static class BerryBushSprite extends ForgottenFeatures.StripSprite {
		public BerryBushSprite(){
			super();
			strip(QuestAssets.BERRY_BUSH, 16, 16, 1);
			still(0);
		}
		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (ch instanceof BerryBush && ((BerryBush) ch).eaten) still(1);
		}
		@Override
		public void still( int f ) {
			super.still(f);
		}
	}

	// *** snares + tracks on the floor. just the state, HuntVisuals does the drawing ***

	public static class HuntField extends Blob {

		public static final int ARMED = 1, SPRUNG = 2, BLOODY = 3;

		public int[] traps;
		public int[] tracks;    //0 none, 1-4 = boar went right/left/up/down
		public int version = 0;

		public static HuntField get( Level level ){
			return level == null ? null : (HuntField) level.blobs.get(HuntField.class);
		}

		public static HuntField getOrMake( Level level ){
			HuntField f = get(level);
			if (f == null){
				f = new HuntField();
				f.seed(level, 0, 0);
				level.blobs.put(HuntField.class, f);
			}
			f.arrays(level.length());
			return f;
		}

		private void arrays( int length ){
			if (cur == null) cur = new int[length];
			if (off == null) off = new int[length];
			if (traps == null) traps = new int[length];
			if (tracks == null) tracks = new int[length];
		}

		public static boolean armed( int cell ){
			HuntField f = get(Dungeon.level);
			return f != null && f.traps != null && cell >= 0 && cell < f.traps.length && f.traps[cell] == ARMED;
		}

		public void arm( int cell ){
			traps[cell] = ARMED;
			version++;
		}

		public void spring( int cell, boolean caught ){
			traps[cell] = caught ? BLOODY : SPRUNG;
			version++;
		}

		public void track( int from, int to ){
			int w = Dungeon.level.width();
			int dx = to % w - from % w, dy = to / w - from / w;
			int dir;
			if (Math.abs(dx) >= Math.abs(dy)) dir = dx >= 0 ? 1 : 2;
			else dir = dy < 0 ? 3 : 4;
			if (from >= 0 && from < tracks.length && traps[from] == 0){
				tracks[from] = dir;
				version++;
			}
		}

		@Override
		public boolean act() {
			spend(TICK);
			return true;
		}

		@Override
		public void use( BlobEmitter emitter ) {
			//HuntVisuals draws this
		}

		@Override
		public String tileDesc() {
			return null;
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			if (traps != null) bundle.put("traps", traps);
			if (tracks != null) bundle.put("tracks", tracks);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			if (bundle.contains("traps")) traps = bundle.getIntArray("traps");
			if (bundle.contains("tracks")) tracks = bundle.getIntArray("tracks");
		}
	}

	// *** items ***

	//snare. put it down, it bites the first boar that steps in
	public static class HunterTrap extends Item {

		public static final String AC_SET = "SET";

		{
			image = ItemSpriteSheet.HUNTER_TRAP;
			stackable = true;
			defaultAction = AC_SET;
		}

		@Override
		public ArrayList<String> actions( Hero hero ) {
			ArrayList<String> actions = super.actions(hero);
			actions.add(AC_SET);
			return actions;
		}

		@Override
		public void execute( Hero hero, String action ) {
			super.execute(hero, action);
			if (!action.equals(AC_SET)) return;
			if (HuntField.armed(hero.pos) || Dungeon.level.getTransition(hero.pos) != null || Dungeon.level.water[hero.pos]){
				GLog.w(Messages.get(this, "no_room"));
				return;
			}
			HuntField.getOrMake(Dungeon.level).arm(hero.pos);
			detach(hero.belongings.backpack);
			Sample.INSTANCE.play(Assets.Sounds.TRAP);
			GLog.i(Messages.get(this, "set"));
			hero.spendAndNext(1f);
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
		public int value() {
			return 5 * quantity;
		}
	}

	//proof of the hunt. a big tooth basically
	public static class GreyTuskFang extends Item {
		{
			image = ItemSpriteSheet.GREY_TUSK_FANG;
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

	//rewards: tusk spear, the boar's hide, hunting horn
	public static class TuskSpear extends Spear {

		{
			image = ItemSpriteSheet.TUSK_SPEAR;
		}

		@Override
		public int proc( Char attacker, Char defender, int damage ) {
			if (Random.Int(4) == 0 && defender.isAlive() && !defender.properties().contains(Char.Property.INORGANIC)){
				Buff.affect(defender, Bleeding.class).set(2 + buffedLvl());
			}
			return super.proc(attacker, defender, damage);
		}
	}

	public static class BoarCloak extends LeatherArmor {

		{
			image = ItemSpriteSheet.BOAR_CLOAK;
		}

		//1 in 5 times someone hits you up close they get thrown back + dazed
		@Override
		public int proc( Char attacker, Char defender, int damage ) {
			if (attacker != null && attacker.isAlive() && Dungeon.level.adjacent(attacker.pos, defender.pos)
					&& Random.Int(5) == 0 && !attacker.properties().contains(Char.Property.IMMOVABLE)){
				Ballistica traj = new Ballistica(defender.pos, attacker.pos + (attacker.pos - defender.pos), Ballistica.MAGIC_BOLT);
				WandOfBlastWave.throwChar(attacker, traj, 1, false, true, this);
				Buff.prolong(attacker, Paralysis.class, 1f);
			}
			return super.proc(attacker, defender, damage);
		}
	}

	//blow it and a boarlet fights for you for a while. cutest artifact in the mod
	public static class HuntingHorn extends Artifact {

		public static final String AC_BLOW = "BLOW";

		{
			image = ItemSpriteSheet.HUNTING_HORN;
			levelCap = 10;
			charge = 0;
			chargeCap = 100;
			defaultAction = AC_BLOW;
			unique = true;
		}

		void boost(){
			upgrade(2);
		}

		@Override
		public ArrayList<String> actions( Hero hero ) {
			ArrayList<String> actions = super.actions(hero);
			if (isEquipped(hero) && !cursed) actions.add(AC_BLOW);
			return actions;
		}

		@Override
		public void execute( Hero hero, String action ) {
			super.execute(hero, action);
			if (!action.equals(AC_BLOW)) return;
			if (!isEquipped(hero)){
				GLog.i(Messages.get(Artifact.class, "need_to_equip"));
				return;
			}
			if (charge < chargeCap){
				GLog.i(Messages.get(this, "no_charge"));
				return;
			}
			int called = 0;
			int wanted = 1 + level() / 4;
			for (int n : PathFinder.NEIGHBOURS8){
				if (called >= wanted) break;
				int c = hero.pos + n;
				if (!Dungeon.level.passable[c] || Dungeon.level.pit[c] || Actor.findChar(c) != null) continue;
				GreyTusk.Boarlet b = new GreyTusk.Boarlet();
				b.grow(level());
				b.pos = c;
				b.alignment = Char.Alignment.ALLY;
				GameScene.add(b);
				Buff.affect(b, HiveHeart.Bond.class).owner = ChampionHooks.indexOf(hero);
				Buff.affect(b, GreyTusk.Boarlet.Lifetime.class).left = 25 + 2 * level();
				b.state = b.WANDERING;
				if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.DUST), 4);
				called++;
			}
			if (called == 0){
				GLog.i(Messages.get(this, "no_room"));
				return;
			}
			charge = 0;
			Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 0.8f, 1.4f);
			GLog.p(Messages.get(this, "blown"));
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
			return new Echo();
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

		//charges while you fight, igual que todos
		public class Echo extends ArtifactBuff {
			@Override
			public boolean act() {
				if (charge < chargeCap && !cursed && target instanceof Hero && ((Hero) target).visibleEnemies() > 0){
					partialCharge += 1.4f + 0.15f * level();
					while (partialCharge >= 1 && charge < chargeCap){
						partialCharge--;
						charge++;
					}
					if (charge >= chargeCap) GLog.p(Messages.get(HuntingHorn.class, "ready"));
					updateQuickslot();
				}
				spend(TICK);
				return true;
			}
		}
	}

	// *** stuff along the escape tracks ***

	static final int FIND_SNARE = 0, FIND_BERRIES = 1, FIND_SPEARS = 2, FIND_CAMP = 3;

	//something the boar smashed or dropped near a cell of its path
	static void placeFind( int near, int kind, ArrayList<Integer> path ){
		Level L = Dungeon.level;
		int cell = -1;
		boolean prop = kind == FIND_BERRIES || kind == FIND_CAMP;
		for (int r = 1; r <= 3 && cell == -1; r++){
			for (int dy = -r; dy <= r && cell == -1; dy++){
				for (int dx = -r; dx <= r && cell == -1; dx++){
					int c = near + dx + dy * L.width();
					if (c < 0 || c >= L.length() || !L.passable[c] || L.solid[c] || Actor.findChar(c) != null) continue;
					if (L.heaps.get(c) != null || L.getTransition(c) != null || L.pit[c]) continue;
					//props never block the path the boar opened
					if (prop && (path.contains(c) || !SKProps.usable(L, c))) continue;
					cell = c;
				}
			}
		}
		if (cell == -1){
			if (prop) placeFind(near, FIND_SNARE, path);
			return;
		}
		switch (kind){
			case FIND_SNARE:
				L.drop(new HunterTrap(), cell).sprite.drop();
				break;
			case FIND_SPEARS:
				L.drop(new GoblinSpearman.GoblinSpearItem().quantity(2), cell).sprite.drop();
				break;
			case FIND_BERRIES:
				BerryBush b = new BerryBush();
				b.pos = cell;
				GameScene.add(b);
				break;
			case FIND_CAMP:
				WreckedCamp w = new WreckedCamp();
				w.pos = cell;
				GameScene.add(w);
				for (int n : PathFinder.NEIGHBOURS8){
					int k = cell + n;
					if (L.passable[k] && Actor.findChar(k) == null && L.heaps.get(k) == null && !path.contains(k)){
						L.drop(Random.Int(2) == 0 ? new Gold(Random.IntRange(15, 25 + 10 * Dungeon.depth)) : new Food(), k).sprite.drop();
						break;
					}
				}
				break;
		}
		if (L.heroFOV[cell]) CellEmitter.get(cell).burst(Speck.factory(Speck.DUST), 4);
	}
}
