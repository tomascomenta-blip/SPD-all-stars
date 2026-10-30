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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;

//goblin giant, rare in the forest. slow, very tanky. every few turns it raises the hammer and slams
//the ground, hurting + slowing everyone around
public class GoblinGiant extends Mob {

	{
		spriteClass = ForestSprites.GiantSprite.class;

		HP = HT = 40;
		defenseSkill = 6;
		baseSpeed = 0.75f;

		EXP = 8;
		maxLvl = 12;

		loot = Gold.class;
		lootChance = 1f;

		HUNTING = new Hunting();
	}

	private int cooldown = 2;
	private boolean raising = false;
	//every 3rd hit taken it answers with a smaller slam, no warning. rude
	private int hitsTaken = 0;
	private boolean furious = false;

	@Override
	public void damage( int dmg, Object src ) {
		super.damage(dmg, src);
		if (isAlive() && dmg > 0 && ++hitsTaken >= 3){
			hitsTaken = 0;
			furious = true;
		}
	}

	private boolean quickSlam(){
		furious = false;
		spend(TICK);
		boolean visible = Dungeon.level.heroFOV[pos];
		//with a callback: the plain attack anim would make him hit AGAIN when it ends
		if (sprite != null) sprite.attack(enemy != null ? enemy.pos : pos, new Callback() {
			@Override
			public void call() {
				if (sprite != null) sprite.idle();
			}
		});
		if (visible){
			Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.8f, 1.2f);
			PixelScene.shake(1, 0.3f);
			sprite.showStatus(CharSprite.NEGATIVE, Messages.get(GoblinGiant.this, "fury"));
		}
		ArrayList<Integer> cells = SKCombat.openArea(pos, 1);
		cells.remove((Integer) pos);
		for (int c : cells){
			if (visible && Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.ROCK), 1);
		}
		for (Char ch : SKCombat.foesIn(this, cells)){
			ch.damage(SKCombat.specialDamage(this, ch, 2, 5), this);
		}
		return true;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 3, 9 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 12;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 1, 3 );
	}

	@Override
	protected boolean act() {
		if (cooldown > 0 && !raising) cooldown--;
		return super.act();
	}

	private ArrayList<Integer> slamCells(){
		ArrayList<Integer> cells = SKCombat.openArea(pos, 2);
		cells.remove((Integer) pos);
		return cells;
	}

	private class Hunting extends Mob.Hunting {
		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			enemySeen = enemyInFOV;
			if (raising) return slam();
			if (furious) return quickSlam();
			if (enemyInFOV && enemy != null && cooldown <= 0 && Dungeon.level.distance(pos, enemy.pos) <= 2){
				raising = true;
				float delay = SKCombat.announceDelay(TICK);
				SKCombat.mark(slamCells(), delay);
				if (sprite != null){
					sprite.turnTo(pos, enemy.pos);
					if (Dungeon.level.heroFOV[pos]) sprite.showStatus(CharSprite.WARNING, Messages.get(GoblinGiant.this, "raise"));
				}
				spend(delay);
				return true;
			}
			return super.act(enemyInFOV, justAlerted);
		}
	}

	private boolean slam(){
		raising = false;
		cooldown = Random.IntRange(4, 5);
		spend(TICK);
		boolean visible = Dungeon.level.heroFOV[pos];
		if (visible){
			Sample.INSTANCE.play(Assets.Sounds.ROCKS);
			PixelScene.shake(2, 0.4f);
		}
		ArrayList<Integer> cells = slamCells();
		for (int c : cells){
			if (visible && Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.ROCK), 2);
		}
		for (Char ch : SKCombat.foesIn(this, cells)){
			ch.damage(SKCombat.specialDamage(this, ch, 3, 7), this);
			if (ch.isAlive()) Buff.prolong(ch, Cripple.class, 2f);
		}
		return true;
	}

	private static final String COOLDOWN = "cooldown";
	private static final String RAISING = "raising";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(COOLDOWN, cooldown);
		bundle.put(RAISING, raising);
		bundle.put("hits_taken", hitsTaken);
		bundle.put("furious", furious);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		cooldown = bundle.getInt(COOLDOWN);
		raising = bundle.getBoolean(RAISING);
		hitsTaken = bundle.getInt("hits_taken");
		furious = bundle.getBoolean("furious");
	}
}
