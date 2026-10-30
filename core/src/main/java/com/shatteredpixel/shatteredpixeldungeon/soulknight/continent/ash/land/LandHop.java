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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.land;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;

//testing only, solo: -Dspd.test.landhop=1 walks the hero round the three layers of the ash
//plains, as a player tapping the way: down the skull, back up, down the chasm, back up.
//each layer is left and found again, so they get saved and loaded on the way
class LandHop extends Group {

	private static int arrivals = 0;
	private static Level last;
	private float wait = 2f;

	@Override
	public void update() {
		super.update();
		Hero hero = Dungeon.hero;
		Level level = Dungeon.level;
		if (level != last){
			last = level;
			arrivals++;
			CoopLog.line("land hop: arrival " + arrivals + " on layer " + ((AshLand) level).layer()
					+ (arrivals == 5 ? ", round done" : ""));
			//as if they had a map of it, the hero only walks where it knows
			for (int i = 0; i < level.length(); i++){
				if (!level.solid[i] || level.getTransition(i) != null) level.mapped[i] = true;
			}
		}
		if (arrivals >= 5 || hero == null || !hero.ready || GameScene.topWindow() != null) return;
		if ((wait -= Game.elapsed) > 0) return;
		wait = 0.5f;
		LevelTransition t;
		if (level instanceof AshSurface){
			t = level.getTransition(arrivals == 1 ? LevelTransition.Type.REGULAR_EXIT : LevelTransition.Type.BRANCH_EXIT);
		} else {
			t = level.getTransition((LevelTransition.Type) null);
		}
		if (t == null) return;
		GameScene.handleCell(t.type == LevelTransition.Type.BRANCH_EXIT ? t.left + 1 + t.top * level.width() : t.cell());
	}
}
