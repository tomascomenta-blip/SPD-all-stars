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

package com.shatteredpixel.shatteredpixeldungeon.soulknight;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.Collection;

//base for the new-world mobs w/ a special: cooldown, telegraphed cells, bullet patterns.
//subclasses do special() (when) and release() (what happens when the marked attack fires)
public abstract class SKMob extends Mob {

	{
		HUNTING = new SKHunting();
		WANDERING = new SKWandering();
	}

	//spawned by a nest/teleporter/egg -> no xp, no loot. otherwise people farm it (i would)
	public void summoned(){
		EXP = 0;
		lootChance = 0;
	}

	//follow this mob id while wandering (patrol knights follow the banner)
	public int leader = -1;

	protected class SKWandering extends Mob.Wandering {
		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			Actor a = leader == -1 ? null : Actor.findById(leader);
			if (a instanceof Char && ((Char) a).isAlive()){
				if (Dungeon.level.distance(pos, ((Char) a).pos) > 2) target = ((Char) a).pos;
			} else {
				leader = -1;
			}
			return super.act(enemyInFOV, justAlerted);
		}
	}

	protected int cooldown = 2;
	protected int pendingKind = -1;    //-1 = nothing pending
	protected int[] pendingCells = new int[0];
	protected int pendingAim = -1;

	//0 = melee only
	protected int shotRange(){
		return 0;
	}

	protected int specialCooldown(){
		return 4;
	}

	//kiting mobs, back off under 3 cells
	protected boolean keepsDistance(){
		return false;
	}

	//true = used (time spent). si devuelve false NO gastes tiempo
	protected boolean special( Char enemy ){
		return false;
	}

	protected void release( int kind, ArrayList<Integer> cells, int aim ){
	}

	@Override
	protected boolean act() {
		if (cooldown > 0 && pendingKind == -1) cooldown--;
		return super.act();
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		if (super.canAttack(enemy)) return true;
		int r = shotRange();
		return r > 0 && Dungeon.level.distance(pos, enemy.pos) <= r
				&& new Ballistica(pos, enemy.pos, Ballistica.PROJECTILE).collisionPos == enemy.pos;
	}

	protected class SKHunting extends Mob.Hunting {
		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			if (pendingKind != -1){
				enemySeen = enemyInFOV;
				int kind = pendingKind;
				ArrayList<Integer> cells = new ArrayList<>();
				for (int c : pendingCells) cells.add(c);
				int aim = pendingAim;
				pendingKind = -1;
				pendingCells = new int[0];
				spend(TICK);
				if (sprite != null) sprite.idle();
				release(kind, cells, aim);
				return true;
			}
			if (enemyInFOV && enemy != null && keepsDistance() && !rooted
					&& Dungeon.level.distance(pos, enemy.pos) <= 2){
				int old = pos;
				if (getFurther(enemy.pos)){
					enemySeen = true;
					spend(1 / speed());
					return moveSprite(old, pos);
				}
			}
			if (enemyInFOV && enemy != null && cooldown <= 0 && !isCharmedBy(enemy)){
				if (special(enemy)){
					enemySeen = true;
					//champions get it 1 turn sooner
					cooldown = Math.max(1, specialCooldown() - (SKChampion.is(SKMob.this) ? 1 : 0));
					return true;
				}
			}
			return super.act(enemyInFOV, justAlerted);
		}
	}

	//marks cells, fires next turn
	protected void announce( int kind, Collection<Integer> cells, int aim ){
		pendingKind = kind;
		pendingAim = aim;
		pendingCells = new int[cells.size()];
		int i = 0;
		for (int c : cells) pendingCells[i++] = c;
		float delay = SKCombat.announceDelay(TICK);
		SKCombat.mark(cells, delay);
		if (sprite != null){
			if (aim != -1) sprite.turnTo(pos, aim);
			if (sprite instanceof SKMobSprite) ((SKMobSprite) sprite).cast();
			if (Dungeon.level.heroFOV[pos]) sprite.showStatus(CharSprite.WARNING, "!");
		}
		spend(delay);
	}

	//straight line until a wall
	protected ArrayList<Integer> line( int target, int maxDist ){
		ArrayList<Integer> cells = new ArrayList<>();
		Ballistica b = new Ballistica(pos, target, Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID);
		for (int c : b.subPath(1, Math.min(b.dist, maxDist))){
			if (Dungeon.level.solid[c]) break;
			cells.add(c);
		}
		return cells;
	}

	protected SKBulletField.Bullet shoot( float degrees, float speed, int image, int min, int max ){
		return SKBulletField.get().fire(this, pos, degrees, speed, image, min, max);
	}

	//ring, first bullet aimed at 'aim'
	protected void ring( int n, int aim, float speed, int image, int min, int max ){
		n = SKChampion.more(this, n);
		float start = aim == -1 ? 0 : SKBulletField.angleTo(pos, aim);
		for (int i = 0; i < n; i++){
			shoot(start + i*360f/n, speed, image, min, max);
		}
	}

	//fan of n over 'span' deg
	protected void arc( int n, float span, int aim, float speed, int image, int min, int max ){
		n = SKChampion.more(this, n);
		float mid = SKBulletField.angleTo(pos, aim);
		for (int i = 0; i < n; i++){
			float deg = n == 1 ? mid : mid - span/2f + i*span/(n-1);
			shoot(deg, speed, image, min, max);
		}
	}

	private static final String COOLDOWN = "sk_cooldown";
	private static final String KIND = "sk_kind";
	private static final String CELLS = "sk_cells";
	private static final String AIM = "sk_aim";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(COOLDOWN, cooldown);
		bundle.put(KIND, pendingKind);
		bundle.put(CELLS, pendingCells);
		bundle.put(AIM, pendingAim);
		bundle.put("leader", leader);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		cooldown = bundle.getInt(COOLDOWN);
		pendingKind = bundle.contains(KIND) ? bundle.getInt(KIND) : -1;
		pendingCells = bundle.contains(CELLS) ? bundle.getIntArray(CELLS) : new int[0];
		pendingAim = bundle.contains(AIM) ? bundle.getInt(AIM) : -1;
		leader = bundle.contains("leader") ? bundle.getInt("leader") : -1;
	}
}
