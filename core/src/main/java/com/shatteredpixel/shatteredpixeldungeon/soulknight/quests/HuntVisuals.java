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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.quests;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.PointF;

//snares + hunt tracks, on the floor under everything
public class HuntVisuals extends Group {

	private int shown = -1;
	private TextureFilm trapFilm, trackFilm;

	@Override
	public void update() {
		super.update();
		HuntQuest.HuntField f = HuntQuest.HuntField.get(Dungeon.level);
		if (f == null || f.traps == null || f.version == shown) return;
		shown = f.version;
		clear();
		Level L = Dungeon.level;
		for (int c = 0; c < f.traps.length && c < L.length(); c++){
			int t = f.traps[c];
			int k = f.tracks == null ? 0 : f.tracks[c];
			if (t == 0 && k == 0) continue;
			if (!L.visited[c] && !L.mapped[c]) continue;
			Image im;
			if (t != 0){
				im = new Image(QuestAssets.TRAP);
				if (trapFilm == null) trapFilm = new TextureFilm(im.texture, 16, 16);
				im.frame(trapFilm.get(t == HuntQuest.HuntField.ARMED ? 0 : t == HuntQuest.HuntField.SPRUNG ? 1 : 2));
			} else {
				im = new Image(QuestAssets.HOOFPRINTS);
				if (trackFilm == null) trackFilm = new TextureFilm(im.texture, 16, 16);
				im.frame(trackFilm.get(k - 1));
			}
			PointF p = DungeonTilemap.tileToWorld(c);
			im.x = p.x;
			im.y = p.y;
			add(im);
		}
	}
}
