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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.kraken;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

//el grumete. solo only, runs around patching holes (slowly). invulnerable, nobody hurts the grumete
public class Grumete extends NPC {

	{
		spriteClass = GrumeteSprite.class;
		baseSpeed = 1f;
		state = PASSIVE;
	}

	private int work = 0;

	@Override
	protected boolean act() {
		if (!(Dungeon.level instanceof KrakenLevel)){
			spend(TICK);
			return true;
		}
		KrakenLevel L = (KrakenLevel) Dungeon.level;
		if (fieldOfView == null || fieldOfView.length != L.length()){
			fieldOfView = new boolean[L.length()];
		}
		L.updateFieldOfView(this, fieldOfView);
		int hole = L.holeNear(pos, 99);
		if (hole != -1){
			if (Dungeon.level.adjacent(pos, hole) || pos == hole){
				if (++work >= 4){
					work = 0;
					L.removeHole(hole);
					L.repairShip(ShipHammer.REPAIR / 2);
					KrakenVisuals.holeClosed(hole);
					if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.EVOKE, 0.4f, 1.8f);
				}
				if (sprite != null) sprite.turnTo(pos, hole);
				spend(TICK);
				return true;
			}
			int old = pos;
			if (getCloser(hole)){
				spend(1 / speed());
				return moveSprite(old, pos);
			}
		}
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
		if (sprite != null) sprite.turnTo(pos, c.pos);
		yell(Messages.get(this, "talk_" + Random.Int(3)));
		return true;
	}

	@Override
	public String description() {
		return Messages.get(this, "desc");
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("work", work);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		work = bundle.getInt("work");
	}

	public static class GrumeteSprite extends SKMobSprite {
		public GrumeteSprite(){
			super();
			setup(KrakenAssets.GRUMETE, 16, 16);
		}
	}
}
