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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

//sub sank -> few turns later the sea eats the party and the kraken wakes up.
//lives on party hero 0 bc buffs save and every device ticks it at the same time
public class KrakenCall extends Buff {

	{
		type = buffType.NEUTRAL;
		revivePersists = true;
	}

	private int turns = 4;

	@Override
	public boolean act() {
		//wait if the floor's locked (rift boss might still be going)
		if (Dungeon.level.locked || Dungeon.depth != 20 || Dungeon.branch != 0){
			spend(TICK);
			return true;
		}
		turns--;
		switch (turns){
			case 3:
				GLog.n(Messages.get(this, "rumble"));
				Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 0.6f);
				PixelScene.shake(2, 1f);
				break;
			case 2:
				GLog.n(Messages.get(this, "water"));
				Sample.INSTANCE.play(Assets.Sounds.WATER, 1f, 0.5f);
				PixelScene.shake(3, 1.5f);
				KrakenVisuals.callOverlay();
				break;
			case 1:
				GLog.n(Messages.get(this, "eye"));
				PixelScene.shake(4, 1.5f);
				break;
		}
		if (turns <= 0){
			detach();
			KrakenLevel.pendingReturnPos = target.pos;
			Level.beforeTransition();
			InterlevelScene.curTransition = new LevelTransition(Dungeon.level, target.pos,
					LevelTransition.Type.BRANCH_EXIT, 20, 2, LevelTransition.Type.BRANCH_ENTRANCE);
			InterlevelScene.mode = InterlevelScene.Mode.DESCEND;
			Game.switchScene(InterlevelScene.class);
			return true;
		}
		spend(TICK);
		return true;
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("turns", turns);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		turns = bundle.getInt("turns");
	}
}
