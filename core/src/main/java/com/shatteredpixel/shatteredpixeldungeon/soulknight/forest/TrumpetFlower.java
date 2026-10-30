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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.forest;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

//trumpet flower. carnivorous plant, never moves. someone gets close -> swells for a turn, then pops
//bubbles all around
public class TrumpetFlower extends Mob {

	{
		spriteClass = ForestSprites.TrumpetSprite.class;

		HP = HT = 10;
		defenseSkill = 0;

		EXP = 2;
		maxLvl = 8;

		loot = Generator.Category.SEED;
		lootChance = 0.25f;

		properties.add(Property.IMMOVABLE);

		HUNTING = new Hunting();
	}

	private int cooldown = 1;
	private boolean swelling = false;
	//closes for 2 turns after bubbling, and a flower left alive plants another one
	private int closed = 0;
	private int age = 0;
	private int seeds = 0;
	private int seedCell = -1;

	public boolean isClosed(){
		return closed > 0;
	}

	@Override
	public boolean isInvulnerable( Class effect ) {
		return closed > 0 || super.isInvulnerable(effect);
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 1, 3 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 8;
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
	protected boolean act() {
		if (cooldown > 0 && !swelling) cooldown--;
		if (seedCell != -1){
			sprout();
			spend(TICK);
			return true;
		}
		if (closed > 0){
			closed--;
			spend(TICK);
			return true;
		}
		if (state == HUNTING && ++age >= 20 && seeds < 2 && flowers() < 8){
			int c = seedSpot();
			if (c != -1){
				age = 0;
				seedCell = c;
				SKCombat.mark(java.util.Collections.singletonList(c), TICK);
				if (Dungeon.level.heroFOV[pos]) CellEmitter.get(pos).burst(LeafParticle.LEVEL_SPECIFIC, 6);
				spend(TICK);
				return true;
			}
		}
		return super.act();
	}

	private static int flowers(){
		int n = 0;
		for (Mob m : Dungeon.level.mobs) if (m instanceof TrumpetFlower) n++;
		return n;
	}

	private int seedSpot(){
		ArrayList<Integer> spots = new ArrayList<>();
		for (int c : SKCombat.openArea(pos, 3)){
			if (Dungeon.level.distance(c, pos) < 2 || !Dungeon.level.passable[c] || Dungeon.level.pit[c]) continue;
			if (Actor.findChar(c) != null || Dungeon.level.heaps.get(c) != null) continue;
			spots.add(c);
		}
		return spots.isEmpty() ? -1 : spots.get(Random.Int(spots.size()));
	}

	private void sprout(){
		int c = seedCell;
		seedCell = -1;
		if (Actor.findChar(c) != null || !Dungeon.level.passable[c]) return;
		TrumpetFlower f = new TrumpetFlower();
		f.pos = c;
		f.state = f.HUNTING;
		f.seeds = 2; //sown flowers don't sow again. or you get a whole floor of them, ask me how I know
		GameScene.add(f);
		seeds++;
		if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(LeafParticle.LEVEL_SPECIFIC, 8);
	}

	private ArrayList<Integer> burstCells(){
		ArrayList<Integer> cells = SKCombat.openArea(pos, 2);
		cells.remove((Integer) pos);
		return cells;
	}

	private class Hunting extends Mob.Hunting {
		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			enemySeen = enemyInFOV;
			if (swelling) return burst();
			if (enemyInFOV && enemy != null && cooldown <= 0 && Dungeon.level.distance(pos, enemy.pos) <= 2){
				swelling = true;
				float delay = SKCombat.announceDelay(TICK);
				SKCombat.mark(burstCells(), delay);
				if (sprite instanceof SKMobSprite) ((SKMobSprite) sprite).cast();
				spend(delay);
				return true;
			}
			return super.act(enemyInFOV, justAlerted);
		}
	}

	private boolean burst(){
		swelling = false;
		cooldown = 3;
		closed = 2;
		spend(TICK);
		boolean visible = Dungeon.level.heroFOV[pos];
		if (sprite != null) sprite.idle();
		if (visible) Sample.INSTANCE.play(Assets.Sounds.GAS, 0.7f, 1.4f);
		for (int c : burstCells()){
			if (visible && Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.BUBBLE), 2);
		}
		for (Char ch : SKCombat.foesIn(this, burstCells())){
			ch.damage(SKCombat.specialDamage(this, ch, 1, 4), this);
		}
		return true;
	}

	private static final String COOLDOWN = "cooldown";
	private static final String SWELLING = "swelling";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(COOLDOWN, cooldown);
		bundle.put(SWELLING, swelling);
		bundle.put("closed", closed);
		bundle.put("age", age);
		bundle.put("seeds", seeds);
		bundle.put("seed_cell", seedCell);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		cooldown = bundle.getInt(COOLDOWN);
		swelling = bundle.getBoolean(SWELLING);
		closed = bundle.getInt("closed");
		age = bundle.getInt("age");
		seeds = bundle.getInt("seeds");
		seedCell = bundle.contains("seed_cell") ? bundle.getInt("seed_cell") : -1;
	}
}
