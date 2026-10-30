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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.DirectableAlly;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

//ally owned by ONE hero (drones, wolves, bear). follows its owner, not the party leader.
//goes downstairs like any DirectableAlly. timeLeft < 0 = lasts until it dies
public abstract class SKAlly extends DirectableAlly {

	{
		WANDERING = new FollowOwner();
		state = WANDERING;
		immunities.add(AllyBuff.class);
	}

	public int ownerIndex = 0;
	public int power = 1;          //owner lvl at summon
	public float timeLeft = -1f;   //<0 = para siempre (ish)

	public Hero owner(){
		return ChampionHooks.heroByIndex(ownerIndex);
	}

	public boolean ownedBy( Hero hero ){
		return ChampionHooks.indexOf(hero) == ownerIndex;
	}

	public void setOwner( Hero hero ){
		ownerIndex = ChampionHooks.indexOf(hero);
		power = hero.lvl;
	}

	//fixed order (coop)
	public static <T extends SKAlly> ArrayList<T> of( Hero hero, Class<T> cls ){
		ArrayList<T> found = new ArrayList<>();
		for (Char ch : Actor.chars()){
			if (cls.isInstance(ch) && ((SKAlly) ch).ownedBy(hero) && ch.isAlive()){
				found.add(cls.cast(ch));
			}
		}
		return found;
	}

	@Override
	protected boolean act() {
		if (timeLeft >= 0 && timeLeft <= 0.01f){
			if (Dungeon.level.heroFOV[pos]) GLog.i(Messages.get(this, "fades"));
			die(null);
			return true;
		}
		return super.act();
	}

	@Override
	protected void spend( float time ) {
		super.spend(time);
		if (timeLeft > 0) timeLeft = Math.max(0, timeLeft - time);
	}

	//DirectableAlly wandering but follows the owner
	protected class FollowOwner extends Mob.Wandering {

		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			if ( enemyInFOV
					&& attacksAutomatically
					&& !movingToDefendPos
					&& (defendingPos == -1 || !Dungeon.level.heroFOV[defendingPos] || canAttack(enemy))) {

				enemySeen = true;
				notice();
				alerted = true;
				state = HUNTING;
				target = enemy.pos;

			} else {

				enemySeen = false;
				int oldPos = pos;
				Hero o = owner();
				int home = o != null && o.isAlive() ? o.pos : Dungeon.hero.pos;
				target = defendingPos != -1 ? defendingPos : home;
				if (getCloser( target )) {
					spend( 1 / speed() );
					if (pos == defendingPos) movingToDefendPos = false;
					return moveSprite( oldPos, pos );
				} else {
					if (movingToDefendPos){
						defendingPos = pos;
						movingToDefendPos = false;
					}
					spend( TICK );
				}
			}
			return true;
		}
	}

	private static final String OWNER = "owner";
	private static final String POWER = "power";
	private static final String TIME  = "time";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(OWNER, ownerIndex);
		bundle.put(POWER, power);
		bundle.put(TIME, timeLeft);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		ownerIndex = bundle.getInt(OWNER);
		power = bundle.getInt(POWER);
		timeLeft = bundle.contains(TIME) ? bundle.getFloat(TIME) : -1f;
	}
}
