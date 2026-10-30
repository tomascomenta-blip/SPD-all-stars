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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentMob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.watabou.utils.Bundle;

//the small beasts of the ash plains: same senses as the big ones (dark eyes, good ears)
public abstract class AshCritter extends ContinentMob implements Fauna {

	{
		viewDistance = Senses.LIT_SIGHT + 1;
		state = WANDERING;
	}

	private float listened = -1;

	protected int eyes(){
		return 0;
	}

	protected int ears(){
		return 0;
	}

	@Override
	protected Char chooseEnemy() {
		Senses.dim(this, fieldOfView, eyes());
		return super.chooseEnemy();
	}

	@Override
	protected boolean act() {
		if (listened < 0) listened = Actor.now();
		if (paralysed <= 0){
			Senses.Noise n = Senses.heard(this, listened, ears());
			listened = Actor.now();
			if (n != null) heard(n);
		}
		return super.act();
	}

	//by default it goes to see, or follows the racket if it lost who it was after
	protected void heard( Senses.Noise n ){
		if (state == HUNTING){
			if (enemy == null || !enemySeen) target = n.cell;
		} else if (state == WANDERING || state == SLEEPING){
			beckon(n.cell);
		}
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("listened", listened);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		listened = bundle.contains("listened") ? bundle.getFloat("listened") : -1;
	}
}
