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

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.finale.ThalFinale;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.utils.Callback;

//someone walked into the portal with the essence: the game stops, every device shows the drops
//going in and the portal closing, and the run ends for the whole party at once
public class PortalSeal extends Buff {

	{
		type = buffType.NEUTRAL;
		revivePersists = true;
	}

	private boolean shown = false;

	@Override
	public boolean act() {
		if (!shown){
			shown = true;
			Awakening.seal();
			GLog.p(Messages.get(Awakening.class, "sealing"));
			//nobody moves while it closes: the turn passes on its own when the scene is over
			for (Char ch : Actor.chars()){
				Buff.prolong(ch, Still.class, 2f);
			}
			spend(TICK);
			final int drops = Continent.essence;
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					AwakeScenes.seal(drops, new Callback() {
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
		ThalFinale.finish(Awakening.Sealed.class);
		//nothing else moves: the victory comes next
		return false;
	}

	//held in place for the scene, with nothing on the screen to show for it
	public static class Still extends FlavourBuff {

		{
			type = buffType.NEUTRAL;
			announced = false;
		}

		@Override
		public boolean attachTo( Char target ) {
			if (super.attachTo(target)){
				target.paralysed++;
				return true;
			}
			return false;
		}

		@Override
		public void detach() {
			super.detach();
			if (target.paralysed > 0) target.paralysed--;
		}
	}
}
