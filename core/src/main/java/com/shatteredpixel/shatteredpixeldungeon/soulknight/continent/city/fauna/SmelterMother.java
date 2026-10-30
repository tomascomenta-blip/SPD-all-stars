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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.fauna;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

//smelter mother. a brass queen with a belly of glowing molten metal, two smoking chimneys and a
//crucible for a mouth. she spits a jet of molten metal in an arc in front of her (marked), and lays
//eggs of molten metal behind her that crack after a while and let out what grew in them
public class SmelterMother extends CityColossus {

	{
		size = 10;
		spriteClass = MotherSprite.class;

		HP = HT = 4400;
		defenseSkill = 0;
		EXP = 170;
		baseSpeed = 0.5f;

		minDmg = 50; maxDmg = 66; acc = 58;
		armorMin = 22; armorMax = 40;

		properties.add(Property.FIERY);
		properties.add(Property.INORGANIC);
	}

	private static final int SPIT = 0, EGG = 1;
	private static final int SPIT_RANGE = 7, EGGS = 3, BROOD = 6;

	private int laid = 0;

	//the molten belly between the rings, underneath
	@Override
	public int[][] weakSpots() {
		return new int[][]{{4, 9}, {5, 9}, {4, 6}};
	}

	//the riveted plates of the back
	@Override
	public int[][] climbable() {
		return new int[][]{{1, 3, 7}, {2, 2, 8}, {3, 2, 8}, {4, 2, 8}, {5, 3, 7}};
	}

	@Override
	protected int ears() {
		return 3;
	}

	@Override
	protected int roam() {
		return 10;
	}

	@Override
	protected int specialCooldown() {
		return 3;
	}

	//what already came out of her eggs and is still about
	private int brood(){
		int n = 0;
		for (Mob m : Dungeon.level.mobs) if (m instanceof SootRats && m.EXP == 0 && m.isAlive()) n++;
		return n;
	}

	private int eggs(){
		int n = 0;
		for (Mob m : Dungeon.level.mobs) if (m instanceof MoltenEgg && m.isAlive()) n++;
		return n;
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = reach(enemy.pos);
		//an egg every couple of jets, or now and then while they keep away
		if ((laid >= 2 || d > SPIT_RANGE && Random.Int(5) == 0) && eggs() < EGGS && brood() < BROOD){
			laid = 0;
			lay();
			return true;
		}
		if (d > SPIT_RANGE) return false;
		ArrayList<Integer> jet = cone(edgeToward(enemy.pos), enemy.pos, SPIT_RANGE, 0.8);
		if (!jet.contains(enemy.pos)) return false;
		laid++;
		warn(SPIT, jet, enemy.pos, 3f);
		return true;
	}

	//an egg drops behind her (she faces away from where it lands), it doesn't need a mark: it
	//only hurts when it cracks, and that is marked
	private void lay(){
		int w = Dungeon.level.width();
		int behind = (right ? left() - 2 : left() + size + 1) + (top() + 7) * w;
		int cell = besideToward(behind);
		if (cell != -1){
			MoltenEgg e = new MoltenEgg();
			e.pos = cell;
			GameScene.add(e);
			if (seen()) GLog.w(Messages.get(this, "lays"));
		}
		if (sprite instanceof MotherSprite) ((MotherSprite) sprite).lay();
		spend(TICK);
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		strikeAnim(kind);
		Senses.noise(aim, Senses.CRASH);
		if (seen()){
			Sample.INSTANCE.play(Assets.Sounds.BURNING, 1f, 0.6f);
			for (int c : cells) if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(FlameParticle.FACTORY, 4);
		}
		for (Char ch : SKCombat.foesIn(this, cells)){
			blast(ch, 120, 160);
			if (ch.isAlive() && !Char.hasProp(ch, Property.FIERY)) Buff.affect(ch, Burning.class).reignite(ch, 4f);
		}
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("laid", laid);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		laid = bundle.getInt("laid");
	}

	//6 the crucible open, 7 the jet, 8 laying, 9 down, 10 cold and dead
	public static class MotherSprite extends Sheet {

		public MotherSprite(){
			super();
			layout(CityAssets.MOTHER, 10, 256, 224, 40, 48, 9, 10);
			run = loop(5, 2, 3, 4, 5);
		}

		@Override
		protected PointF weakAnchor() {
			return new PointF(110, 196);
		}

		public void lay(){
			if (curAnim == idle || curAnim == run) once(clip(4, 8, 8, 8, 0));
		}

		@Override
		public void strike( int kind ) {
			once(clip(8, 7, 7, 7, 0));
		}
	}

	//an egg of molten metal. it glows, cools and cracks: the splash around it is marked the turn
	//before, and two soot rats come out. broken first, nothing hatches
	public static class MoltenEgg extends CityCritter {

		{
			spriteClass = EggSprite.class;

			HP = HT = 60;
			defenseSkill = 0;
			EXP = 0;

			minDmg = 0; maxDmg = 0; acc = 0;
			armorMin = 6; armorMax = 12;

			properties.add(Property.IMMOVABLE);
			properties.add(Property.INORGANIC);
			properties.add(Property.FIERY);
			state = PASSIVE;
		}

		private static final int HATCH = 8;
		private int age = 0;

		@Override
		protected boolean act() {
			age++;
			if (pendingKind != -1){
				int kind = pendingKind;
				ArrayList<Integer> cells = new ArrayList<>();
				for (int c : pendingCells) cells.add(c);
				pendingKind = -1;
				pendingCells = new int[0];
				release(kind, cells, -1);
				spend(TICK);
				return true;
			}
			if (age == HATCH - 1){
				if (sprite instanceof EggSprite) ((EggSprite) sprite).crack();
				announce(0, SKCombat.area(pos, 1), -1);
				return true;
			}
			spend(TICK);
			return true;
		}

		@Override
		protected void release( int kind, ArrayList<Integer> cells, int aim ) {
			if (Dungeon.level.heroFOV[pos]){
				Sample.INSTANCE.play(Assets.Sounds.SHATTER, 1f, 0.7f);
				for (int c : cells) CellEmitter.get(c).burst(FlameParticle.FACTORY, 3);
			}
			for (Char ch : SKCombat.foesIn(this, cells)){
				ch.damage(SKCombat.specialDamage(this, ch, 40, 60), this);
				if (ch.isAlive() && !Char.hasProp(ch, Property.FIERY)) Buff.affect(ch, Burning.class).reignite(ch, 3f);
			}
			int at = pos;
			die(null);
			for (int i = 0; i < 2; i++){
				ArrayList<Integer> free = freeAround(at);
				if (free.isEmpty()) break;
				SootRats r = new SootRats();
				r.pos = free.get(Random.Int(free.size()));
				r.summoned();
				GameScene.add(r);
			}
		}

		@Override
		public int damageRoll() {
			return 0;
		}

		@Override
		protected boolean canAttack( Char enemy ) {
			return false;
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("age", age);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			age = bundle.getInt("age");
		}
	}

	//32x32 round its own cell: 0 red hot, 1 cracking, 2 broken
	public static class EggSprite extends MobSprite {

		private final TextureFilm film;

		public EggSprite(){
			super();
			texture(CityAssets.EGG);
			film = new TextureFilm(texture, 32, 32);
			idle = new Animation(1, true);
			idle.frames(film, 0);
			run = idle.clone();
			attack = idle.clone();
			zap = idle.clone();
			die = new Animation(6, false);
			die.frames(film, 2, 2, 2);
			play(idle);
		}

		public void crack(){
			idle = new Animation(1, true);
			idle.frames(film, 1);
			play(idle);
		}
	}
}
