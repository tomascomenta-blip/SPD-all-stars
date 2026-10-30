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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.awake;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.watabou.noosa.Game;
import com.watabou.utils.Callback;

//the turn after yog falls: the game waits while every device shows the continent waking up
//(like ThalCall waits for the cracked victory), then the hunt is on
public class WakeCall extends Buff {

	{
		type = buffType.NEUTRAL;
		revivePersists = true;
	}

	private boolean shown = false;

	@Override
	public boolean act() {
		if (!shown){
			shown = true;
			spend(TICK);
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					AwakeScenes.wake(new Callback() {
						@Override
						public void call() {
							next();
						}
					});
				}
			});
			return false;
		}
		detach();
		return true;
	}
}
