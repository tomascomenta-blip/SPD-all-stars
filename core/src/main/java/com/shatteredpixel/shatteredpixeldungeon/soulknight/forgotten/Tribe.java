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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.forgotten;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ScaleArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Glaive;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.beastmaster.Taming;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.props.SKProps;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//the tribe quest. shaman (camp, floor 21) wants the eternal flame carried to the jungle altar (24)
//cold/blizzard weaken it, braziers top it up, if it goes out -> back to the sacred fire, start over
//deliver it = reward pick + the warriors show up vs tharog. on 25 the shaman tells the legend w/ tips
//yes it's an escort quest. but the escort is a torch so it can't run into lava at least
public class Tribe {

	//quest state, saved with the run (not the floor!)

	public static class Quest {
		public static boolean given = false;
		public static boolean completed = false;

		public static void reset(){
			given = false;
			completed = false;
		}

		public static void storeInBundle( Bundle bundle ){
			Bundle b = new Bundle();
			b.put("given", given);
			b.put("completed", completed);
			bundle.put("forgotten_tribe", b);
		}

		public static void restoreFromBundle( Bundle bundle ){
			if (!bundle.contains("forgotten_tribe")){
				reset();
				return;
			}
			Bundle b = bundle.getBundle("forgotten_tribe");
			given = b.getBoolean("given");
			completed = b.getBoolean("completed");
		}
	}

	static boolean placeCamp( Level level, Room r ){
		int c = SKProps.nearCenter(level, r);
		if (c == -1) return false;
		ForgottenFeatures.add(level, new SacredFire(), c);
		Shaman s = new Shaman();
		for (int n : PathFinder.NEIGHBOURS8){
			int k = c + n;
			if (level.passable[k] && level.findMob(k) == null && !level.solid[k]){
				ForgottenFeatures.add(level, s, k);
				break;
			}
		}
		decor(level, r, new Hut());
		decor(level, r, new Totem());
		return true;
	}

	static boolean placeAltar( Level level, Room r ){
		int c = SKProps.nearCenter(level, r);
		if (c == -1) return false;
		ForgottenFeatures.add(level, new Altar(), c);
		int made = 0;
		for (int n : PathFinder.NEIGHBOURS8){
			int k = c + 2*n;
			if (made >= 2) break;
			if (k > 0 && k < level.length() && level.passable[k] && level.findMob(k) == null && !level.solid[k]
					&& level.map[k] != Terrain.DOOR){
				Warrior w = new Warrior();
				w.decor = true;
				ForgottenFeatures.add(level, w, k);
				made++;
			}
		}
		decor(level, r, new Totem());
		return true;
	}

	//free corner, away from doors
	private static void decor( Level level, Room r, Mob m ){
		int w = level.width();
		int[][] corners = {{r.left + 1, r.top + 1}, {r.right - 1, r.top + 1}, {r.left + 1, r.bottom - 1}, {r.right - 1, r.bottom - 1}};
		for (int[] k : corners){
			int c = k[0] + k[1] * w;
			int t = level.map[c];
			if (t != Terrain.EMPTY
					&& t != Terrain.EMPTY_DECO) continue;
			if (level.findMob(c) != null || level.heaps.get(c) != null) continue;
			boolean door = false;
			for (int n : PathFinder.NEIGHBOURS8){
				int u = level.map[c + n];
				if (u == Terrain.DOOR
						|| u == Terrain.OPEN_DOOR
						|| level.findMob(c + n) != null) door = true;
			}
			if (door || level.distance(c, level.entrance()) < 3 || level.distance(c, level.exit()) < 3) continue;
			ForgottenFeatures.add(level, m, c);
			return;
		}
	}

	//flame tick

	//the braziers on its way are in TorchRoute. an ember still catches on a lit brazier,
	//so a dead flame costs a walk back, never the quest
	static void flameTurn( ForgottenLevel L, Hero h, ForgottenFloor floor ){
		SacredFlame f = h.belongings.getItem(SacredFlame.class);
		if (f == null) return;
		if (!f.lit){
			if (ForgottenFloor.nearBrazier(L, h.pos, 1)){
				f.lit = true;
				f.flame = 100;
				GLog.p(Messages.get(SacredFlame.class, "relit"));
				Sample.INSTANCE.play(Assets.Sounds.BURNING);
			}
			return;
		}
		if (ForgottenFloor.nearBrazier(L, h.pos, 2)){
			if (f.flame < 100){
				f.flame = 100;
				GLog.p(Messages.get(SacredFlame.class, "revived"));
			}
			return;
		}
		//the warm side of the land feeds it back, slowly
		if (!L.cold(h.pos)){
			if (f.flame < 100) f.flame = Math.min(100, f.flame + TorchRoute.WARM_BACK);
			return;
		}
		int drain = (floor.blizzard() ? 3 : TorchRoute.COLD) + (L.water[h.pos] ? TorchRoute.WET : 0);
		int before = f.flame;
		f.flame = Math.max(0, f.flame - drain);
		if (before > 30 && f.flame <= 30) GLog.w(Messages.get(SacredFlame.class, "weak"));
		if (f.flame <= 0){
			f.lit = false;
			GLog.n(Messages.get(SacredFlame.class, "out"));
		}
	}

	public static class Shaman extends NPC {

		{
			spriteClass = ShamanSprite.class;
			properties.add(Property.IMMOVABLE);
		}

		private int talk = 0;

		@Override
		protected boolean act() {
			spend(TICK);
			return true;
		}

		@Override
		public boolean isInvulnerable( Class effect ) {
			return true;
		}

		@Override
		public void damage( int dmg, Object src ) {
		}

		@Override
		public boolean add( Buff buff ) {
			return false;
		}

		@Override
		public boolean reset() {
			return true;
		}

		@Override
		public boolean interact( Char c ) {
			if (!(c instanceof Hero)) return true;
			Hero h = (Hero) c;
			if (sprite != null) sprite.turnTo(pos, h.pos);
			//floor 25: legend, poco a poco
			if (Dungeon.depth == 25){
				yell(Messages.get(Shaman.class, "legend_" + (talk % 5)));
				talk++;
				return true;
			}
			if (!Quest.given){
				Quest.given = true;
				SacredFlame f = new SacredFlame();
				if (!f.collect(h.belongings.backpack)) Dungeon.level.drop(f, h.pos).sprite.drop();
				yell(Messages.get(Shaman.class, "quest"));
				GLog.p(Messages.get(Shaman.class, "quest_hint"));
				Sample.INSTANCE.play(Assets.Sounds.BURNING);
				return true;
			}
			if (Quest.completed){
				yell(Messages.get(Shaman.class, "thanks"));
				return true;
			}
			SacredFlame f = h.belongings.getItem(SacredFlame.class);
			if (f != null && !f.lit){
				yell(Messages.get(Shaman.class, "relight"));
			} else {
				yell(Messages.get(Shaman.class, "talk_" + (talk % 3)));
				talk++;
			}
			return true;
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

	public static class ShamanSprite extends SKMobSprite {
		public ShamanSprite(){
			super();
			setup(ForgottenAssets.SHAMAN, 16, 20);
		}
	}

	// *** the fire, the altar, the camp ***

	public static class SacredFire extends ForgottenFeatures.Thing {

		{
			spriteClass = SacredFireSprite.class;
		}

		@Override
		public boolean heroShouldInteract() {
			return true;
		}

		@Override
		public boolean interact( Char c ) {
			if (!(c instanceof Hero)) return true;
			SacredFlame f = ((Hero) c).belongings.getItem(SacredFlame.class);
			if (f != null && (!f.lit || f.flame < 100)){
				f.lit = true;
				f.flame = 100;
				GLog.p(Messages.get(SacredFire.class, "lit"));
				Sample.INSTANCE.play(Assets.Sounds.BURNING);
				((Hero) c).spendAndNext(1f);
				return false;
			}
			GLog.i(Messages.get(SacredFire.class, Quest.given ? "burns" : "ask"));
			return true;
		}

		@Override
		public String description() {
			return Messages.get(this, "desc");
		}
	}

	public static class SacredFireSprite extends ForgottenFeatures.StripSprite {
		public SacredFireSprite(){
			super();
			strip(ForgottenAssets.SACRED_FIRE, 32, 32, 8);
		}
	}

	public static class Altar extends ForgottenFeatures.Thing {

		{
			spriteClass = AltarSprite.class;
		}

		@Override
		public boolean heroShouldInteract() {
			return true;
		}

		@Override
		public boolean interact( Char c ) {
			if (!(c instanceof Hero)) return true;
			final Hero h = (Hero) c;
			if (Quest.completed){
				GLog.i(Messages.get(Altar.class, "done"));
				return true;
			}
			SacredFlame f = h.belongings.getItem(SacredFlame.class);
			if (f == null){
				GLog.i(Messages.get(Altar.class, "nothing"));
				return true;
			}
			if (!f.lit){
				GLog.w(Messages.get(Altar.class, "out"));
				return true;
			}
			f.detachAll(h.belongings.backpack);
			Quest.completed = true;
			if (sprite != null) sprite.emitter().burst(Speck.factory(Speck.STAR), 16);
			Sample.INSTANCE.play(Assets.Sounds.MASTERY);
			GLog.p(Messages.get(Altar.class, "complete"));
			for (Mob m : Dungeon.level.mobs){
				if (m instanceof Warrior) ((Warrior) m).yell(Messages.get(Warrior.class, "cheer"));
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
					Messages.get(Altar.class, "name"),
					Messages.get(Altar.class, "reward"),
					Messages.get(FlintSpear.class, "name"),
					Messages.get(HideArmor.class, "name"),
					Messages.get(RaptorEgg.class, "name")){
				@Override
				protected void onSelect( int index ) {
					Item item;
					switch (index){
						case 0: default: item = new FlintSpear(); break;
						case 1: item = new HideArmor(); break;
						case 2: item = new RaptorEgg(); break;
					}
					if (item.isUpgradable()) item.upgrade(2);
					item.identify();
					if (!item.collect(h.belongings.backpack)) Dungeon.level.drop(item, h.pos).sprite.drop();
					GLog.p(Messages.get(Altar.class, "got", item.name()));
				}
				@Override
				public void onBackPressed() {
					//gotta pick one
				}
			});
		}

		@Override
		public String description() {
			return Messages.get(this, Quest.completed ? "desc_done" : "desc");
		}
	}

	public static class AltarSprite extends ForgottenFeatures.StripSprite {
		public AltarSprite(){
			super();
			strip(ForgottenAssets.ALTAR, 32, 24, 1);
		}
	}

	public static class Hut extends ForgottenFeatures.Thing {
		{
			spriteClass = HutSprite.class;
		}
		@Override
		public String description() {
			return Messages.get(this, "desc");
		}
	}

	public static class HutSprite extends ForgottenFeatures.StripSprite {
		public HutSprite(){
			super();
			strip(ForgottenAssets.HUT, 48, 40, 1);
		}
	}

	public static class Totem extends ForgottenFeatures.Thing {
		{
			spriteClass = TotemSprite.class;
		}
		@Override
		public String description() {
			return Messages.get(this, "desc");
		}
	}

	public static class TotemSprite extends ForgottenFeatures.StripSprite {
		public TotemSprite(){
			super();
			strip(ForgottenAssets.TOTEM, 16, 32, 1);
		}
	}

	//warriors: guard the altar, then help vs tharog

	public static class Warrior extends SKMob {

		{
			spriteClass = WarriorSprite.class;
			HP = HT = 70;
			defenseSkill = 20;
			EXP = 0;
			lootChance = 0;
			alignment = Alignment.ALLY;
			state = HUNTING;
		}

		public boolean decor = false;

		@Override
		protected void onAdd() {
			if (decor){
				alignment = Alignment.NEUTRAL;
				state = PASSIVE;
				properties.add(Property.IMMOVABLE);
			}
			super.onAdd();
		}

		@Override
		public boolean isInvulnerable( Class effect ) {
			return decor || super.isInvulnerable(effect);
		}

		@Override
		protected int shotRange() {
			return decor ? 0 : 3;
		}

		@Override
		public int damageRoll() {
			return Random.NormalIntRange(12, 20);
		}

		@Override
		public int attackSkill( Char target ) {
			return 40;
		}

		@Override
		protected boolean act() {
			if (decor){
				spend(TICK);
				return true;
			}
			//they relight the arena braziers, which is honestly the main reason to do the quest
			for (Mob m : Dungeon.level.mobs){
				if (m instanceof ForgottenFeatures.Brazier && !((ForgottenFeatures.Brazier) m).lit
						&& Dungeon.level.adjacent(m.pos, pos)){
					((ForgottenFeatures.Brazier) m).lit = true;
					if (Dungeon.level.heroFOV[pos]) GLog.p(Messages.get(Warrior.class, "relit"));
					spend(TICK);
					return true;
				}
			}
			return super.act();
		}

		@Override
		public boolean interact( Char c ) {
			if (decor){
				yell(Messages.get(Warrior.class, "talk"));
				return true;
			}
			return super.interact(c);
		}

		@Override
		public String description() {
			return Messages.get(this, "desc");
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("decor", decor);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			decor = bundle.getBoolean("decor");
			if (decor){
				alignment = Alignment.NEUTRAL;
				state = PASSIVE;
			}
		}
	}

	public static class WarriorSprite extends SKMobSprite {
		public WarriorSprite(){
			super();
			shot = ItemSpriteSheet.SHOT_ARROW;
			setup(ForgottenAssets.WARRIOR, 16, 18);
		}
	}

	//items
	//the flame. bone torch
	public static class SacredFlame extends Item {

		{
			image = ItemSpriteSheet.SACRED_FLAME;
			unique = true;
			bones = false;
		}

		public int flame = 100;
		public boolean lit = true;

		@Override
		public String status() {
			return lit ? flame + "%" : "x";
		}

		@Override
		public String desc() {
			return Messages.get(this, lit ? "desc" : "desc_out", flame);
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
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("flame", flame);
			bundle.put("lit", lit);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			flame = bundle.getInt("flame");
			lit = bundle.getBoolean("lit");
		}
	}

	//flint spear, reach + bleed
	public static class FlintSpear extends Glaive {
		{
			image = ItemSpriteSheet.FLINT_SPEAR;
			DLY = 1f;
		}

		@Override
		public int max( int lvl ) {
			return Math.round(5.5f*(tier+1)) + lvl*Math.round(1.1f*(tier+1));
		}

		@Override
		public int proc( Char attacker, Char defender, int damage ) {
			if (Random.Int(4) == 0) Buff.affect(defender, Bleeding.class).set(Math.max(3, damage / 3));
			return super.proc(attacker, defender, damage);
		}
	}

	//furs. immune to the forgotten land cold
	public static class HideArmor extends ScaleArmor {
		{
			image = ItemSpriteSheet.HIDE_ARMOR;
		}
	}

	//hatches a pet raptor :)
	public static class RaptorEgg extends Item {

		public static final String AC_HATCH = "HATCH";

		{
			image = ItemSpriteSheet.RAPTOR_EGG;
			defaultAction = AC_HATCH;
			unique = true;
		}

		@Override
		public ArrayList<String> actions( Hero hero ) {
			ArrayList<String> actions = super.actions(hero);
			actions.add(AC_HATCH);
			return actions;
		}

		@Override
		public void execute( Hero hero, String action ) {
			super.execute(hero, action);
			if (!action.equals(AC_HATCH)) return;
			int cell = -1;
			for (int n : PathFinder.NEIGHBOURS8){
				int c = hero.pos + n;
				if (Dungeon.level.passable[c] && Actor.findChar(c) == null){
					cell = c;
					break;
				}
			}
			if (cell == -1){
				GLog.w(Messages.get(this, "no_space"));
				return;
			}
			detach(hero.belongings.backpack);
			ForgottenMobs.Raptor r = new ForgottenMobs.Raptor();
			r.pos = cell;
			r.EXP = 0;
			GameScene.add(r);
			Buff.affect(r, Taming.Tamed.class).owner = ChampionHooks.indexOf(hero);
			r.HP = r.HT;
			r.state = r.WANDERING;
			if (r.sprite != null) r.sprite.emitter().burst(Speck.factory(Speck.HEART), 8);
			GLog.p(Messages.get(this, "hatched"));
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
	}
}
