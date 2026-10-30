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
import com.shatteredpixel.shatteredpixeldungeon.items.food.MysteryMeat;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

//forest boar. target in a straight line a few cells away -> lowers its head for a turn, then charges down that line.
//they come in pairs: when one lowers its head the partner charges too if it can, from its own side
public class Boar extends Mob {

	{
		spriteClass = ForestSprites.BoarSprite.class;

		HP = HT = 9;
		defenseSkill = 3;

		EXP = 1;
		maxLvl = 5;

		loot = MysteryMeat.class;
		lootChance = 0.2f;

		HUNTING = new Hunting();
	}

	private static final int CHARGE_RANGE = 4;

	private int cooldown = 2;
	private boolean charging = false;
	private int dirX, dirY;
	private int partner = -1; //id of the other boar

	public int partner(){
		return partner;
	}

	public void pairWith( Boar other ){
		partner = other.id();
		other.partner = id();
	}

	private Boar partnerBoar(){
		Actor a = partner == -1 ? null : Actor.findById(partner);
		return a instanceof Boar && ((Boar) a).isAlive() ? (Boar) a : null;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 1, 4 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 8;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 1 );
	}

	@Override
	protected boolean act() {
		if (cooldown > 0 && !charging) cooldown--;
		return super.act();
	}

	//charge line cells, from the next one until a wall
	private ArrayList<Integer> line( int dx, int dy ){
		ArrayList<Integer> cells = new ArrayList<>();
		int w = Dungeon.level.width();
		int x = pos % w, y = pos / w;
		for (int i = 1; i <= CHARGE_RANGE; i++){
			int nx = x + dx*i, ny = y + dy*i;
			if (nx < 0 || ny < 0 || nx >= w || ny >= Dungeon.level.height()) break;
			int c = nx + ny*w;
			if (Dungeon.level.solid[c] || !Dungeon.level.passable[c]) break;
			cells.add(c);
		}
		return cells;
	}

	private boolean canCharge( Char target ){
		int w = Dungeon.level.width();
		int dx = target.pos % w - pos % w;
		int dy = target.pos / w - pos / w;
		int dist = Math.max(Math.abs(dx), Math.abs(dy));
		if (dist < 2 || dist > CHARGE_RANGE) return false;
		if (dx != 0 && dy != 0 && Math.abs(dx) != Math.abs(dy)) return false;
		int sx = Integer.signum(dx), sy = Integer.signum(dy);
		ArrayList<Integer> cells = line(sx, sy);
		//clear shot to the target?
		for (int c : cells){
			if (c == target.pos) return true;
			if (Actor.findChar(c) != null) return false;
		}
		return false;
	}

	private class Hunting extends Mob.Hunting {
		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			if (charging){
				enemySeen = enemyInFOV;
				return charge();
			}
			if (enemyInFOV && enemy != null && cooldown <= 0 && !rooted && canCharge(enemy)){
				enemySeen = true;
				return announce();
			}
			return super.act( enemyInFOV, justAlerted );
		}
	}

	private boolean announce(){
		spend(lowerHead(enemy));
		//partner joins in from where it stands, if it's fighting too. teamwork!
		Boar other = partnerBoar();
		if (other != null && other.state == other.HUNTING && !other.charging && !other.rooted
				&& other.fieldOfView != null && other.fieldOfView[enemy.pos] && other.canCharge(enemy)){
			other.postpone(other.lowerHead(enemy));
		}
		return true;
	}

	//marks the line toward the target, returns how long till the charge
	private float lowerHead( Char target ){
		int w = Dungeon.level.width();
		dirX = Integer.signum(target.pos % w - pos % w);
		dirY = Integer.signum(target.pos / w - pos / w);
		charging = true;
		float delay = SKCombat.announceDelay(TICK);
		SKCombat.mark(line(dirX, dirY), delay);
		if (sprite != null){
			sprite.turnTo(pos, target.pos);
			if (Dungeon.level.heroFOV[pos]) sprite.showStatus(CharSprite.WARNING, Messages.get(this, "charge"));
		}
		return delay;
	}

	private boolean charge(){
		charging = false;
		cooldown = Random.IntRange(4, 6);
		int start = pos;
		Char victim = null;
		for (int c : line(dirX, dirY)){
			Char ch = Actor.findChar(c);
			if (ch != null){
				if (SKCombat.foes(this, ch)) victim = ch;
				break;
			}
			if (rooted || !isAlive()) break;
			move(c, false);
		}
		spend(TICK);
		if (!isAlive()) return true;
		if (pos != start){
			if (Dungeon.level.heroFOV[start] || Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.TRAMPLE);
			moveSprite(start, pos);
		}
		if (victim != null){
			//charge hits harder + more accurate than a bite. it's a boar, obviously
			attack(victim, 1.5f, 0, 1.5f);
		}
		return true;
	}

	private static final String COOLDOWN = "cooldown";
	private static final String CHARGING = "charging";
	private static final String DIR_X = "dir_x";
	private static final String DIR_Y = "dir_y";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(COOLDOWN, cooldown);
		bundle.put(CHARGING, charging);
		bundle.put(DIR_X, dirX);
		bundle.put(DIR_Y, dirY);
		bundle.put("partner", partner);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		cooldown = bundle.getInt(COOLDOWN);
		charging = bundle.getBoolean(CHARGING);
		dirX = bundle.getInt(DIR_X);
		dirY = bundle.getInt(DIR_Y);
		partner = bundle.contains("partner") ? bundle.getInt("partner") : -1;
	}
}
