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
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.props.SKProps;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

//forgotten land decoration, placed at levelgen. braziers, ice walls, frozen beasts/chests, thin ice,
//tar, nests, plants, and the tribe (camp on 21, altar on 24). basically the junk drawer
public class ForgottenFeatures {

	public static void decorate( ForgottenLevel level, ArrayList<Room> rooms ){
		ForgottenFloor floor = ForgottenFloor.getOrMake(level);

		ArrayList<Room> spots = new ArrayList<>();
		for (Room r : rooms){
			if (r instanceof StandardRoom && !r.isEntrance() && !r.isExit() && r.width() >= 5 && r.height() >= 5){
				spots.add(r);
			}
		}
		//coldest -> warmest
		final ForgottenLevel L = level;
		Collections.sort(spots, new Comparator<Room>() {
			@Override
			public int compare( Room a, Room b ) {
				return Float.compare(L.warmth(L.pointToCell(a.center())), L.warmth(L.pointToCell(b.center())));
			}
		});
		ArrayList<Room> cold = new ArrayList<>(), warm = new ArrayList<>();
		for (Room r : spots){
			if (L.cold(L.pointToCell(r.center()))) cold.add(r); else warm.add(r);
		}

		//tribe: camp in the coldest room that fits, altar in the warmest
		if (Dungeon.depth == 21){
			ArrayList<Room> order = new ArrayList<>(cold);
			order.addAll(warm);
			for (Room r : order){
				if (Tribe.placeCamp(level, r)){
					cold.remove(r);
					warm.remove(r);
					break;
				}
			}
		}
		if (Dungeon.depth == 24){
			ArrayList<Room> order = new ArrayList<>(warm);
			java.util.Collections.reverse(order);
			order.addAll(cold);
			for (Room r : order){
				if (Tribe.placeAltar(level, r)){
					cold.remove(r);
					warm.remove(r);
					break;
				}
			}
		}

		//ice
		if (!cold.isEmpty()){
			//a few braziers so the cold doesn't just kill you
			int braziers = Math.min(cold.size(), Dungeon.depth == 23 ? 2 : 3);
			for (int i = 0; i < braziers; i++){
				Room r = cold.get(i * cold.size() / braziers);
				int c = SKProps.nearCenter(level, r);
				if (c != -1) add(level, new Brazier(), c);
			}
			for (Room r : cold){
				int roll = Random.Int(6);
				if (roll == 0 || roll == 1){
					frozenBeast(level, r);
				} else if (roll == 2){
					frozenChest(level, r);
				} else if (roll == 3){
					thinIce(level, floor, r);
				} else if (roll == 4){
					iceAlcove(level, r);
				}
			}
		}

		//jungle
		for (Room r : warm){
			int roll = Random.Int(5);
			if (roll == 0 || roll == 1){
				tar(level, floor, r);
			} else if (roll == 2){
				int c = SKProps.nearCenter(level, r);
				if (c != -1) add(level, new Nest(), c);
			} else if (roll == 3){
				plants(level, r);
			}
		}

		//compy packs
		for (Mob m : level.mobs.toArray(new Mob[0])){
			if (m instanceof ForgottenMobs.Compy){
				int made = 0;
				for (int n : PathFinder.NEIGHBOURS8){
					int c = m.pos + n;
					if (made >= 2) break;
					if (level.passable[c] && !level.solid[c] && level.findMob(c) == null && !level.pit[c]){
						ForgottenMobs.Compy k = new ForgottenMobs.Compy();
						k.pos = c;
						level.mobs.add(k);
						made++;
					}
				}
			}
		}

		//and the ones the sacred flame needs on its way down
		TorchRoute.place(level);
	}

	static void add( Level level, Mob m, int cell ){
		m.pos = cell;
		level.mobs.add(m);
	}

	private static ArrayList<Integer> roomCells( Level level, Room r ){
		ArrayList<Integer> cells = new ArrayList<>();
		for (int y = r.top + 1; y < r.bottom; y++){
			for (int x = r.left + 1; x < r.right; x++){
				int c = x + y * level.width();
				if (SKProps.usable(level, c)) cells.add(c);
			}
		}
		Random.shuffle(cells);
		return cells;
	}

	private static void frozenBeast( Level level, Room r ){
		ArrayList<Integer> cells = roomCells(level, r);
		if (cells.isEmpty()) return;
		FrozenBlock b = new FrozenBlock();
		b.inside = Random.element(new Class[]{ForgottenMobs.Mammoth.class, ForgottenMobs.Sabertooth.class,
				ForgottenMobs.FrostWolf.class, ForgottenMobs.WoollyRhino.class, ForgottenMobs.Yeti.class});
		add(level, b, cells.get(0));
	}

	private static void frozenChest( Level level, Room r ){
		ArrayList<Integer> cells = roomCells(level, r);
		if (cells.isEmpty()) return;
		add(level, new FrozenChest(), cells.get(0));
	}

	private static void thinIce( Level level, ForgottenFloor floor, Room r ){
		for (int y = r.top + 1; y < r.bottom; y++){
			for (int x = r.left + 1; x < r.right; x++){
				int c = x + y * level.width();
				int t = level.map[c];
				if ((t == Terrain.EMPTY || t == Terrain.EMPTY_DECO) && Random.Int(3) == 0
						&& level.distance(c, level.entrance()) > 2 && level.distance(c, level.exit()) > 2){
					floor.put(level, c, ForgottenFloor.THIN_ICE);
				}
			}
		}
	}

	private static void tar( Level level, ForgottenFloor floor, Room r ){
		ArrayList<Integer> cells = roomCells(level, r);
		if (cells.isEmpty()) return;
		int center = cells.get(0);
		for (int n : PathFinder.NEIGHBOURS9){
			int c = center + n;
			int t = level.map[c];
			if ((t == Terrain.EMPTY || t == Terrain.EMPTY_DECO || t == Terrain.GRASS) && (n == 0 || Random.Int(3) > 0)
					&& level.distance(c, level.entrance()) > 2 && level.distance(c, level.exit()) > 2){
				floor.put(level, c, ForgottenFloor.TAR);
			}
		}
		//bones + amber in the tar
		if (Random.Int(2) == 0 && level.heaps.get(center) == null){
			level.drop(Random.Int(3) == 0 ? new Amber() : new Gold(Random.IntRange(40, 90)), center);
		}
	}

	private static void plants( Level level, Room r ){
		ArrayList<Integer> cells = roomCells(level, r);
		int n = 1 + Random.Int(2);
		for (int i = 0; i < n && i < cells.size(); i++){
			add(level, new ForgottenMobs.CarnivorePlant(), cells.get(i));
		}
	}

	//loot behind ice walls, melt with fire. corners of big rooms only so it never blocks a path
	//(it did once. the whole floor was unreachable. not fun)
	private static void iceAlcove( Level level, Room r ){
		if (r.width() < 7 || r.height() < 7) return;
		int w = level.width();
		int[][] corners = {{r.left + 1, r.top + 1}, {r.right - 1, r.top + 1}, {r.left + 1, r.bottom - 1}, {r.right - 1, r.bottom - 1}};
		for (int[] k : corners){
			int c = k[0] + k[1] * w;
			if (level.map[c] != Terrain.EMPTY && level.map[c] != Terrain.EMPTY_DECO) continue;
			if (level.findMob(c) != null || level.heaps.get(c) != null) continue;
			ArrayList<Integer> walls = new ArrayList<>();
			boolean ok = true;
			for (int n : PathFinder.NEIGHBOURS8){
				int t = c + n;
				if (!level.passable[t]) continue;
				int tile = level.map[t];
				if (tile == Terrain.DOOR || tile == Terrain.OPEN_DOOR || level.findMob(t) != null
						|| level.distance(t, level.entrance()) < 3 || level.distance(t, level.exit()) < 3){
					ok = false;
					break;
				}
				//and keep it off doors
				for (int m : PathFinder.NEIGHBOURS8){
					int u = level.map[t + m];
					if (u == Terrain.DOOR || u == Terrain.OPEN_DOOR || u == Terrain.LOCKED_DOOR) ok = false;
				}
				walls.add(t);
			}
			if (!ok || walls.isEmpty() || walls.size() > 3) continue;
			for (int t : walls) add(level, new IceWall(), t);
			level.drop(Generator.random(), c);
			return;
		}
	}

	/* the things */
	//frame strip. some loop, some just show whatever frame their state says
	public static class StripSprite extends MobSprite {

		protected TextureFilm film;
		protected int count = 1;
		private int shown = -1;

		protected void strip( String asset, int w, int h, float fps ){
			texture(asset);
			film = new TextureFilm(texture, w, h);
			count = Math.max(1, (int)(texture.width / w));
			Object[] frames = new Object[count];
			for (int i = 0; i < count; i++) frames[i] = i;
			idle = new Animation((int) fps, true);
			idle.frames(film, frames);
			run = idle.clone();
			attack = idle.clone();
			zap = idle.clone();
			die = new Animation(8, false);
			die.frames(film, count - 1);
			play(idle);
		}

		protected void still( int f ){
			f = Math.max(0, Math.min(count - 1, f));
			if (f == shown) return;
			shown = f;
			idle = new Animation(1, true);
			idle.frames(film, f);
			run = idle.clone();
			attack = idle.clone();
			zap = idle.clone();
			play(idle, true);
		}

	}

	//unhittable unless they override it
	public static abstract class Thing extends SKProps.SKProp {
		{
			HP = HT = 1000;
		}

		@Override
		public boolean isInvulnerable( Class effect ) {
			return true;
		}

		@Override
		public void damage( int dmg, Object src ) {
		}
	}

	//warm + keeps the sacred flame going
	public static class Brazier extends Thing {

		{
			spriteClass = BrazierSprite.class;
		}

		public boolean lit = true;

		@Override
		protected boolean act() {
			if (lit && Dungeon.level.heroFOV[pos] && Random.Int(3) == 0 && sprite != null){
				sprite.emitter().burst(FlameParticle.FACTORY, 1);
			}
			spend(TICK);
			return true;
		}

		@Override
		public boolean heroShouldInteract() {
			return true;
		}

		@Override
		public boolean interact( Char c ) {
			if (!lit && c instanceof Hero){
				lit = true;
				Sample.INSTANCE.play(Assets.Sounds.BURNING);
				GLog.p(Messages.get(this, "relit"));
				((Hero) c).spendAndNext(1f);
				return false;
			}
			GLog.i(Messages.get(this, "warm"));
			return true;
		}

		public void putOut(){
			if (!lit) return;
			lit = false;
			if (sprite != null) CellEmitter.get(pos).burst(Speck.factory(Speck.STEAM), 6);
		}

		@Override
		public String description() {
			return Messages.get(this, lit ? "desc" : "desc_out");
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("lit", lit);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			lit = !bundle.contains("lit") || bundle.getBoolean("lit");
		}
	}

	public static class BrazierSprite extends StripSprite {
		public BrazierSprite(){
			super();
			strip(ForgottenAssets.BRAZIER, 16, 24, 8);
		}
		@Override
		public void update() {
			super.update();
			if (ch instanceof Brazier) alpha(((Brazier) ch).lit ? 1f : 0.55f);
		}
	}

	//fire = instant, a golpes = slow
	public static class IceWall extends SKProps.SKProp {

		{
			spriteClass = IceWallSprite.class;
			HP = HT = 45;
			immunities.add(Chill.class);
			immunities.add(Frost.class);
		}

		@Override
		protected boolean act() {
			if (melting(pos)){
				damage(HT, new Fire());
			}
			spend(TICK);
			return true;
		}

		@Override
		public void die( Object cause ) {
			int at = pos;
			super.die(cause);
			if (Dungeon.level.heroFOV[at]){
				CellEmitter.get(at).burst(Speck.factory(Speck.STEAM), 8);
				Sample.INSTANCE.play(Assets.Sounds.SHATTER, 0.7f, 1.2f);
			}
		}

		@Override
		public String description() {
			return Messages.get(this, "desc");
		}
	}

	static boolean melting( int cell ){
		Blob fire = Dungeon.level.blobs.get(Fire.class);
		if (fire != null && fire.volume > 0){
			for (int n : PathFinder.NEIGHBOURS9){
				int c = cell + n;
				if (c >= 0 && c < fire.cur.length && fire.cur[c] > 0) return true;
			}
		}
		Char ch = Actor.findChar(cell);
		return ch != null && ch.buff(Burning.class) != null;
	}

	public static class IceWallSprite extends StripSprite {
		public IceWallSprite(){
			super();
			strip(ForgottenAssets.ICE_WALL, 16, 24, 1);
			still(0);
		}
		@Override
		public void update() {
			super.update();
			if (ch != null) still(ch.HP > ch.HT * 2 / 3 ? 0 : ch.HP > ch.HT / 3 ? 1 : 2);
		}
	}

	//frozen beast. fire, hits, or fighting nearby wakes it up. surprise!
	public static class FrozenBlock extends SKProps.SKProp {

		{
			spriteClass = FrozenSprite.class;
			HP = HT = 30;
		}

		public Class<? extends Mob> inside = ForgottenMobs.Mammoth.class;
		private int nearTurns = 0;

		@Override
		protected boolean act() {
			if (melting(pos)){
				thaw();
				return true;
			}
			boolean near = false;
			for (Hero h : Party.living()){
				if (Dungeon.level.adjacent(h.pos, pos)) near = true;
			}
			nearTurns = near ? nearTurns + 1 : 0;
			if (nearTurns >= 3){
				thaw();
				return true;
			}
			spend(TICK);
			return true;
		}

		@Override
		public void die( Object cause ) {
			thaw();
		}

		private boolean thawed = false;

		private void thaw(){
			if (thawed) return;
			thawed = true;
			int at = pos;
			if (Dungeon.level.heroFOV[at]){
				CellEmitter.get(at).burst(Speck.factory(Speck.STEAM), 10);
				Sample.INSTANCE.play(Assets.Sounds.SHATTER);
				GLog.w(Messages.get(FrozenBlock.class, "wakes"));
			}
			destroy();
			if (sprite != null) sprite.killAndErase();
			Mob m = Reflection.newInstance(inside);
			m.pos = at;
			m.state = m.HUNTING;
			GameScene.add(m);
			Dungeon.level.occupyCell(m);
		}

		@Override
		public String description() {
			return Messages.get(this, "desc");
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("inside", inside);
			bundle.put("near", nearTurns);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			if (bundle.contains("inside")) inside = bundle.getClass("inside");
			nearTurns = bundle.getInt("near");
		}
	}

	public static class FrozenSprite extends StripSprite {
		public FrozenSprite(){
			super();
			strip(ForgottenAssets.FROZEN, 24, 28, 1);
			still(0);
		}
		@Override
		public void update() {
			super.update();
			if (ch != null) still(ch.HP > ch.HT / 2 ? 0 : 1);
		}
	}

	//frozen chest, fire or lots of (noisy) hits
	public static class FrozenChest extends SKProps.SKProp {

		{
			spriteClass = FrozenChestSprite.class;
			HP = HT = 25;
		}

		@Override
		protected boolean act() {
			if (melting(pos)) damage(HT, new Fire());
			spend(TICK);
			return true;
		}

		@Override
		public void damage( int dmg, Object src ) {
			super.damage(dmg, src);
			//noise wakes the whole floor, that's the catch
			for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
				if (m.alignment == Alignment.ENEMY && Dungeon.level.distance(m.pos, pos) <= 7 && m.state == m.SLEEPING){
					m.beckon(pos);
				}
			}
		}

		@Override
		public void die( Object cause ) {
			int at = pos;
			super.die(cause);
			Item item = Generator.random();
			Heap h = Dungeon.level.drop(item, at);
			h.type = Heap.Type.CHEST;
			if (h.sprite != null) h.sprite.view(h);
			if (Dungeon.level.heroFOV[at]){
				CellEmitter.get(at).burst(Speck.factory(Speck.STEAM), 6);
				Sample.INSTANCE.play(Assets.Sounds.SHATTER, 0.7f, 1f);
			}
		}

		@Override
		public String description() {
			return Messages.get(this, "desc");
		}
	}

	public static class FrozenChestSprite extends StripSprite {
		public FrozenChestSprite(){
			super();
			strip(ForgottenAssets.FROZEN_CHEST, 16, 16, 1);
			still(0);
		}
		@Override
		public void update() {
			super.update();
			if (ch != null) still(ch.HP > ch.HT / 2 ? 0 : 1);
		}
	}

	//eggs! mom is never far
	public static class Nest extends Thing {

		{
			spriteClass = NestSprite.class;
		}

		public boolean empty = false;

		@Override
		public boolean heroShouldInteract() {
			return !empty;
		}

		@Override
		public boolean interact( Char c ) {
			if (empty || !(c instanceof Hero)) return true;
			Hero h = (Hero) c;
			empty = true;
			Item egg = new DinoEgg();
			if (!egg.collect(h.belongings.backpack)) Dungeon.level.drop(egg, h.pos).sprite.drop();
			GLog.n(Messages.get(this, "mother"));
			Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 0.6f);
			//here comes mom
			Mob mother = Random.Int(2) == 0 ? new ForgottenMobs.Triceratops() : new ForgottenMobs.Raptor();
			int best = -1;
			for (int i = 0; i < Dungeon.level.length(); i++){
				if (!Dungeon.level.passable[i] || Actor.findChar(i) != null || Dungeon.level.distance(i, pos) < 3) continue;
				if (Dungeon.level.distance(i, pos) > 6) continue;
				if (best == -1 || Dungeon.level.distance(i, pos) < Dungeon.level.distance(best, pos)) best = i;
			}
			if (best != -1){
				mother.pos = best;
				mother.state = mother.HUNTING;
				GameScene.add(mother);
				mother.aggro(h);
			}
			h.spendAndNext(1f);
			return false;
		}

		@Override
		public String description() {
			return Messages.get(this, empty ? "desc_empty" : "desc");
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("empty", empty);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			empty = bundle.getBoolean("empty");
		}
	}

	public static class NestSprite extends StripSprite {
		public NestSprite(){
			super();
			strip(ForgottenAssets.NEST, 28, 18, 1);
			still(0);
		}
		@Override
		public void update() {
			super.update();
			if (ch instanceof Nest) still(((Nest) ch).empty ? 1 : 0);
		}
	}

	//big egg, good food
	public static class DinoEgg extends Food {
		{
			image = ItemSpriteSheet.DINO_EGG;
			energy = 300f;
		}

		@Override
		public int value() {
			return 20 * quantity;
		}
	}

	//mosquito in amber. sell it. do NOT clone dinos with it
	public static class Amber extends Item {
		{
			image = ItemSpriteSheet.FOSSIL;
			stackable = true;
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
			return 150 * quantity;
		}
	}
}
