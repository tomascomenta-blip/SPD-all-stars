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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;

//goblin spear guard. thrust reaches 2 cells. 3-5 cells away it throws the spear along a line (marked a turn
//before), then punches until it picks it up again. grab the spear first and it stays unarmed lol
public class GoblinSpearman extends Mob {

	{
		spriteClass = ForestSprites.SpearmanSprite.class;

		HP = HT = 10;
		defenseSkill = 3;

		EXP = 2;
		maxLvl = 7;

		loot = Gold.class;
		lootChance = 0.3f;

		HUNTING = new Hunting();
	}

	private boolean hasSpear = true;
	private int spearCell = -1;   //where its thrown spear is lying
	private boolean aiming = false;
	private int aimCell = -1;
	private int cooldown = 2;

	public boolean hasSpear(){
		return hasSpear;
	}

	@Override
	public int damageRoll() {
		return hasSpear ? Random.NormalIntRange( 1, 5 ) : Random.NormalIntRange( 1, 2 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 9;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 1 );
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		if (super.canAttack(enemy)) return true;
		if (!hasSpear) return false;
		//like gnoll guards, needs a clear straight line for the long thrust
		return Dungeon.level.distance(enemy.pos, pos) <= 2
				&& new Ballistica( pos, enemy.pos, Ballistica.PROJECTILE).collisionPos == enemy.pos
				&& new Ballistica( enemy.pos, pos, Ballistica.PROJECTILE).collisionPos == pos;
	}

	@Override
	protected boolean act() {
		if (cooldown > 0 && !aiming) cooldown--;
		return super.act();
	}

	//cells the spear flies over toward a cell, until it hits a wall or someone
	private ArrayList<Integer> throwPath( int target ){
		ArrayList<Integer> cells = new ArrayList<>();
		Ballistica b = new Ballistica(pos, target, Ballistica.PROJECTILE);
		for (int c : b.subPath(1, Math.min(b.dist, 6))){
			cells.add(c);
		}
		return cells;
	}

	private boolean spearLying(){
		if (spearCell == -1) return false;
		Heap h = Dungeon.level.heaps.get(spearCell);
		if (h == null) return false;
		for (Item i : h.items){
			if (i instanceof GoblinSpearItem) return true;
		}
		return false;
	}

	private class Hunting extends Mob.Hunting {
		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			if (aiming){
				enemySeen = enemyInFOV;
				return throwSpear();
			}
			if (hasSpear && enemyInFOV && enemy != null && cooldown <= 0 && !rooted){
				int d = Dungeon.level.distance(pos, enemy.pos);
				if (d >= 3 && d <= 5 && new Ballistica(pos, enemy.pos, Ballistica.PROJECTILE).collisionPos == enemy.pos){
					enemySeen = true;
					aim(enemy);
					return true;
				}
			}
			//unarmed: go fetch the spear, unless someone's right next to it
			if (!hasSpear && spearLying() && (enemy == null || !Dungeon.level.adjacent(pos, enemy.pos))){
				enemySeen = enemyInFOV;
				if (pos == spearCell){
					pickUpSpear();
					return true;
				}
				int old = pos;
				if (getCloser(spearCell)){
					spend(1 / speed());
					return moveSprite(old, pos);
				}
			}
			return super.act(enemyInFOV, justAlerted);
		}
	}

	private void aim( Char target ){
		aiming = true;
		aimCell = target.pos;
		float delay = SKCombat.announceDelay(TICK);
		SKCombat.mark(throwPath(aimCell), delay);
		if (sprite != null){
			sprite.turnTo(pos, aimCell);
			if (Dungeon.level.heroFOV[pos]) sprite.showStatus(CharSprite.WARNING, Messages.get(this, "aim"));
		}
		spend(delay);
	}

	private boolean throwSpear(){
		aiming = false;
		cooldown = 8;
		spend(TICK);
		ArrayList<Integer> path = throwPath(aimCell);
		if (path.isEmpty()) return true;
		Char victim = null;
		int land = path.get(path.size()-1);
		for (int c : path){
			Char ch = Actor.findChar(c);
			if (ch != null){
				victim = ch;
				land = c;
				break;
			}
		}
		if (Dungeon.level.solid[land]){
			int i = path.indexOf(land);
			land = i > 0 ? path.get(i-1) : pos;
		}
		hasSpear = false;
		spearCell = land;
		final int at = land;
		if (sprite != null && sprite.parent != null){
			((MissileSprite) sprite.parent.recycle(MissileSprite.class)).reset(sprite, at, new GoblinSpearItem(), new Callback() {
				@Override
				public void call() {
				}
			});
			Sample.INSTANCE.play(Assets.Sounds.MISS, 1f, 0.8f);
		}
		if (victim != null && SKCombat.foes(this, victim)){
			attack(victim, 1.3f, 0, 1.5f);
		}
		Dungeon.level.drop(new GoblinSpearItem(), land);
		return true;
	}

	private void pickUpSpear(){
		spend(TICK);
		Heap h = Dungeon.level.heaps.get(spearCell);
		if (h != null){
			for (Item i : new ArrayList<>(h.items)){
				if (i instanceof GoblinSpearItem){
					h.remove(i);
					break;
				}
			}
		}
		hasSpear = true;
		spearCell = -1;
		if (sprite != null) sprite.idle();
	}

	@Override
	public void die( Object cause ) {
		//1 in 4 it drops its spear, más no que se llena el suelo
		if (hasSpear && Random.Int(4) == 0) Dungeon.level.drop(new GoblinSpearItem(), pos).sprite.drop();
		super.die(cause);
	}

	@Override
	public String description() {
		String desc = super.description();
		if (!hasSpear) desc += "\n\n" + Messages.get(this, "unarmed");
		return desc;
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("has_spear", hasSpear);
		bundle.put("spear_cell", spearCell);
		bundle.put("aiming", aiming);
		bundle.put("aim_cell", aimCell);
		bundle.put("cooldown", cooldown);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		hasSpear = !bundle.contains("has_spear") || bundle.getBoolean("has_spear");
		spearCell = bundle.contains("spear_cell") ? bundle.getInt("spear_cell") : -1;
		aiming = bundle.getBoolean("aiming");
		aimCell = bundle.contains("aim_cell") ? bundle.getInt("aim_cell") : -1;
		cooldown = bundle.getInt("cooldown");
	}

	//goblin guard spear. weak throwing spear
	public static class GoblinSpearItem extends MissileWeapon {
		{
			image = ItemSpriteSheet.THROWING_SPEAR;
			hitSound = Assets.Sounds.HIT_STAB;
			hitSoundPitch = 1.1f;
			tier = 1;
			baseUses = 5;
		}
	}
}
