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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.props;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SmokeScreen;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Amok;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corruption;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicalSleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Sleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Splash;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BlastParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SmokeParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SnowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKWorlds;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forgotten.ForgottenFloor;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.kingdom.KingdomBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.undersea.UnderseaFeatures;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Collection;

//crates and barrels for the new worlds. they block bullets (both sides) and break
//barrels pop when broken: red = boom, plus one per world (sap/frost/toxic/ink)
public class SKProps {

	//it's a Mob that never acts. yes a crate is a mob. it's fine. it's fine
	public static abstract class SKProp extends Mob {

		{
			alignment = Alignment.NEUTRAL;
			state = PASSIVE;
			EXP = 0;
			defenseSkill = 0;
			properties.add(Property.IMMOVABLE);
			properties.add(Property.INORGANIC);
			properties.add(Property.OBJECT);

			immunities.add(Amok.class);
			immunities.add(Charm.class);
			immunities.add(Corruption.class);
			immunities.add(Terror.class);
			immunities.add(Sleep.class);
			immunities.add(MagicalSleep.class);
			immunities.add(Paralysis.class);
		}

		@Override
		protected boolean act() {
			spend(TICK*5);
			return true;
		}

		@Override
		public boolean heroShouldInteract() {
			return false;
		}

		@Override
		public void beckon( int cell ) {
		}

		@Override
		protected boolean getCloser( int target ) {
			return false;
		}

		@Override
		protected boolean getFurther( int target ) {
			return false;
		}

		@Override
		public int attackSkill( Char target ) {
			return 0;
		}

		@Override
		public float spawningWeight() {
			return 0;
		}

		@Override
		public void notice() {
		}
	}

	public static class Crate extends SKProp {

		{
			spriteClass = CrateSprite.class;
		}

		public Crate(){
			super();
			HP = HT = 8 + Dungeon.scalingDepth();
		}

		@Override
		public void die( Object cause ) {
			int at = pos;
			super.die(cause);
			if (Random.Int(4) == 0){
				Dungeon.level.drop(new Gold(Random.IntRange(5, 10 + 2*Dungeon.depth)), at).sprite.drop();
			}
		}
	}

	public static abstract class Barrel extends SKProp {

		{
			HP = HT = 1;
		}

		@Override
		public void die( Object cause ) {
			int at = pos;
			super.die(cause);
			CoopLog.line("barrel bursts " + getClass().getSimpleName() + " at " + at);
			burst(at);
		}

		protected abstract void burst( int cell );

		//own cell + open neighbours
		protected ArrayList<Integer> blastCells( int cell ){
			ArrayList<Integer> cells = new ArrayList<>();
			for (int n : PathFinder.NEIGHBOURS9){
				int c = cell + n;
				if (c >= 0 && c < Dungeon.level.length() && !Dungeon.level.solid[c]) cells.add(c);
			}
			return cells;
		}

		//fixed order!! (coop)
		protected ArrayList<Char> caught( ArrayList<Integer> cells ){
			ArrayList<Char> chars = new ArrayList<>();
			for (int c : cells){
				Char ch = Actor.findChar(c);
				if (ch != null && ch.isAlive()) chars.add(ch);
			}
			return chars;
		}
	}

	//red one. boom + burns grass
	public static class ExplosiveBarrel extends Barrel {

		{
			spriteClass = ExplosiveBarrelSprite.class;
		}

		@Override
		protected void burst( int cell ) {
			ArrayList<Integer> cells = blastCells(cell);
			if (Dungeon.level.heroFOV[cell]){
				CellEmitter.center(cell).burst(BlastParticle.FACTORY, 30);
				Sample.INSTANCE.play(Assets.Sounds.BLAST);
			}
			for (int c : cells){
				if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(SmokeParticle.FACTORY, 4);
				if (Dungeon.level.flamable[c]){
					Dungeon.level.destroy(c);
					GameScene.updateMap(c);
				}
			}
			ForgottenFloor.noise(cell);
			//throne hall remembers the scorch marks
			if (Dungeon.level instanceof KingdomBossLevel){
				((KingdomBossLevel) Dungeon.level).scorch(cell);
			}
			int depth = Dungeon.scalingDepth();
			for (Char ch : caught(cells)){
				if (!ch.isAlive()) continue;
				int dmg = Random.NormalIntRange(3 + depth, 8 + 2*depth) - ch.drRoll();
				if (dmg > 0) ch.damage(dmg, this);
			}
		}
	}

	//forest: sap, roots
	public static class SapBarrel extends Barrel {

		{
			spriteClass = SapBarrelSprite.class;
		}

		@Override
		protected void burst( int cell ) {
			ArrayList<Integer> cells = blastCells(cell);
			for (int c : cells){
				if (Dungeon.level.heroFOV[c]) Splash.at(c, 0xFFD27A1F, 5);
			}
			if (Dungeon.level.heroFOV[cell]) Sample.INSTANCE.play(Assets.Sounds.SHATTER, 0.7f, 0.8f);
			for (Char ch : caught(cells)){
				ch.damage(Random.NormalIntRange(1, 3 + Dungeon.scalingDepth()/3), this);
				if (ch.isAlive()) Buff.prolong(ch, Roots.class, 3f);
			}
		}
	}

	//kingdom: frost. mobs freeze, heroes only get chilled bc Frost shatters potions and people were mad
	public static class IceBarrel extends Barrel {

		{
			spriteClass = IceBarrelSprite.class;
		}

		@Override
		protected void burst( int cell ) {
			ArrayList<Integer> cells = blastCells(cell);
			for (int c : cells){
				if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(SnowParticle.FACTORY, 6);
			}
			if (Dungeon.level.heroFOV[cell]) Sample.INSTANCE.play(Assets.Sounds.SHATTER);
			for (Char ch : caught(cells)){
				ch.damage(Random.NormalIntRange(1, 3 + Dungeon.scalingDepth()/3), this);
				if (!ch.isAlive()) continue;
				if (ch instanceof Hero) Buff.prolong(ch, Chill.class, 5f);
				else                    Buff.prolong(ch, Frost.class, 3f);
			}
		}
	}

	//starship: toxic gas
	public static class ToxicBarrel extends Barrel {

		{
			spriteClass = ToxicBarrelSprite.class;
		}

		@Override
		protected void burst( int cell ) {
			if (Dungeon.level.heroFOV[cell]) Sample.INSTANCE.play(Assets.Sounds.GAS);
			GameScene.add(Blob.seed(cell, 200 + 10*Dungeon.scalingDepth(), ToxicGas.class));
		}
	}

	//undersea: ink
	public static class InkBarrel extends Barrel {

		{
			spriteClass = InkBarrelSprite.class;
		}

		@Override
		protected void burst( int cell ) {
			for (int c : blastCells(cell)){
				if (Dungeon.level.heroFOV[c]) Splash.at(c, 0xFF2A1638, 5);
			}
			if (Dungeon.level.heroFOV[cell]) Sample.INSTANCE.play(Assets.Sounds.GAS);
			GameScene.add(Blob.seed(cell, 300, UnderseaFeatures.InkCloud.class));
		}
	}

	//per-world barrel (not counting red)
	public static Barrel worldBarrel(){
		switch (SKWorlds.region(Dungeon.depth)){
			case SKWorlds.FOREST:   default: return new SapBarrel();
			case SKWorlds.KINGDOM:  return new IceBarrel();
			case SKWorlds.STARSHIP: return new ToxicBarrel();
			case SKWorlds.UNDERSEA: return new InkBarrel();
			case SKWorlds.FORGOTTEN: return new IceBarrel();
		}
	}

	//placement: open rooms only. no corridors, not by doors/stairs, never adjacent to each other,
	//so you can always walk around them and nothing gets blocked
	public static void place( Level level, ArrayList<Room> rooms, Collection<Room> skip ){
		ArrayList<Room> spots = new ArrayList<>();
		for (Room r : rooms){
			if (r instanceof StandardRoom && !r.isEntrance() && !r.isExit() && !skip.contains(r)
					&& r.width() >= 5 && r.height() >= 5){
				spots.add(r);
			}
		}
		Random.shuffle(spots);
		int crates = 3 + Random.Int(4);
		int barrels = 1 + Random.Int(3);
		for (Room r : spots){
			if (crates + barrels <= 0) break;
			ArrayList<Integer> cells = new ArrayList<>();
			for (int y = r.top + 1; y < r.bottom; y++){
				for (int x = r.left + 1; x < r.right; x++){
					int c = x + y*level.width();
					if (usable(level, c)) cells.add(c);
				}
			}
			Random.shuffle(cells);
			int here = 1 + Random.Int(3);
			for (int c : cells){
				if (here <= 0 || crates + barrels <= 0) break;
				if (!usable(level, c)) continue; //recheck, the previous one might be next to it now
				SKProp p;
				if (barrels > 0 && (crates == 0 || Random.Int(3) == 0)){
					p = Random.Int(2) == 0 ? new ExplosiveBarrel() : worldBarrel();
					barrels--;
				} else {
					p = new Crate();
					crates--;
				}
				p.pos = c;
				level.mobs.add(p);
				here--;
			}
		}
	}

	//closest usable cell to the room center, -1 if nothing
	public static int nearCenter( Level level, Room r ){
		int center = level.pointToCell(r.center());
		int best = -1;
		for (int y = r.top + 1; y < r.bottom; y++){
			for (int x = r.left + 1; x < r.right; x++){
				int c = x + y*level.width();
				if (!usable(level, c)) continue;
				if (best == -1 || level.distance(c, center) < level.distance(best, center)) best = c;
			}
		}
		return best;
	}

	public static boolean usable( Level level, int c ){
		if (c < 0 || c >= level.length()) return false;
		if (!level.passable[c] || level.pit[c] || level.water[c] || level.solid[c]) return false;
		if (level.heaps.get(c) != null || level.traps.get(c) != null || level.plants.get(c) != null) return false;
		int t = level.map[c];
		if (t != Terrain.EMPTY && t != Terrain.EMPTY_SP && t != Terrain.EMPTY_DECO
				&& t != Terrain.GRASS && t != Terrain.HIGH_GRASS && t != Terrain.FURROWED_GRASS) return false;
		for (int n : PathFinder.NEIGHBOURS4){
			if (!level.passable[c + n]) return false;
		}
		for (int n : PathFinder.NEIGHBOURS8){
			int k = level.map[c + n];
			if (k == Terrain.DOOR || k == Terrain.OPEN_DOOR || k == Terrain.LOCKED_DOOR
					|| k == Terrain.CRYSTAL_DOOR || k == Terrain.SECRET_DOOR) return false;
			for (Mob m : level.mobs){
				if (m.pos == c + n && m instanceof SKProp) return false;
			}
		}
		for (Mob m : level.mobs){
			if (m.pos == c) return false;
		}
		int entrance = level.entrance(), exit = level.exit();
		if (entrance >= 0 && level.distance(c, entrance) < 3) return false;
		if (exit >= 0 && level.distance(c, exit) < 3) return false;
		return true;
	}

	//sprites

	public static class CrateSprite extends SKMobSprite {
		public CrateSprite(){
			super();
			setup(SKAssets.CRATE, 16, 16);
		}
		@Override
		public void update() {
			//agrietado a mitad de vida
			if (ch != null && ch.isAlive()) row(ch.HP*2 <= ch.HT ? 1 : 0);
			super.update();
		}
		@Override
		public int blood() {
			return 0xFF8A5A2B;
		}
	}

	public static class ExplosiveBarrelSprite extends SKMobSprite {
		public ExplosiveBarrelSprite(){
			super();
			setup(SKAssets.BARREL_EXPLOSIVE, 16, 16);
		}
		@Override
		public int blood() {
			return 0xFFB33A2A;
		}
	}

	public static class SapBarrelSprite extends SKMobSprite {
		public SapBarrelSprite(){
			super();
			setup(SKAssets.BARREL_SAP, 16, 16);
		}
		@Override
		public int blood() {
			return 0xFFD27A1F;
		}
	}

	public static class IceBarrelSprite extends SKMobSprite {
		public IceBarrelSprite(){
			super();
			setup(SKAssets.BARREL_ICE, 16, 16);
		}
		@Override
		public int blood() {
			return 0xFFBFE6FF;
		}
	}

	public static class ToxicBarrelSprite extends SKMobSprite {
		public ToxicBarrelSprite(){
			super();
			setup(SKAssets.BARREL_TOXIC, 16, 16);
		}
		@Override
		public int blood() {
			return 0xFF6BDB3A;
		}
	}

	public static class InkBarrelSprite extends SKMobSprite {
		public InkBarrelSprite(){
			super();
			setup(SKAssets.BARREL_INK, 16, 16);
		}
		@Override
		public int blood() {
			return 0xFF2A1638;
		}
	}
}
