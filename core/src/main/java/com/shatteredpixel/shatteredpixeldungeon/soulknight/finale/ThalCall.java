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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.finale;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

//you walked out with the amulet... and the victory screen freezes and cracks.
//the buff waits for the little cutscene (render side) and then everyone gets pulled in.
//every device plays its own copy of the cutscene, the turn loop just waits for it like for an attack anim
public class ThalCall extends Buff {

	{
		type = buffType.NEUTRAL;
		revivePersists = true;
	}

	private boolean shown = false;

	@Override
	public boolean act() {
		if (!shown){
			shown = true;
			for (Char ch : Actor.chars()){
				Buff.prolong(ch, Stasis.class, 3f);
			}
			Sample.INSTANCE.play(Assets.Sounds.TELEPORT, 1f, 0.5f);
			GLog.p(Messages.get(ThalCall.class, "escaped"));
			spend(TICK);
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					ThalHud.victoryCrack(new Callback() {
						@Override
						public void call() {
							next();
						}
					});
				}
			});
			return false;
		}
		GLog.n(Messages.get(ThalCall.class, "pulled"));
		int from = target.pos;
		detach();
		ThalFinale.enterRift(from);
		return true;
	}

	//time stopped for this one. used for the cutscene and for the time stop spell
	public static class Stasis extends FlavourBuff {

		{
			type = buffType.NEGATIVE;
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

		@Override
		public int icon() {
			return BuffIndicator.TIME;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(0x66CCFF);
		}

		@Override
		public void fx( boolean on ) {
			if (target.sprite == null) return;
			if (on) target.sprite.add(CharSprite.State.PARALYSED);
			else if (target.paralysed <= 1) target.sprite.remove(CharSprite.State.PARALYSED);
		}

		@Override
		public String name() {
			return Messages.get(ThalCall.class, "stasis_name");
		}

		@Override
		public String desc() {
			return Messages.get(ThalCall.class, "stasis_desc", dispTurns());
		}
	}
}
