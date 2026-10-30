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
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

//crowd on the coliseum walls + the dark grand knight in the royal box, watching.
//crowd.png frames: 0-1 sitting, 2-3 cheering, 4-5 booing
//royal_box.png: 0 sitting, 1 standing, 2 banging his shield
public class TourneyVisuals extends Group {

	private static float cheerT = 0, booT = 0;

	public static void cheer(){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				cheerT = 2.5f;
				booT = 0;
			}
		});
	}

	public static void boo(){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				booT = 2.5f;
				cheerT = 0;
			}
		});
	}

	private boolean built = false;
	private final ArrayList<Image> crowd = new ArrayList<>();
	private final ArrayList<Float> phase = new ArrayList<>();
	private Image box;
	private TextureFilm crowdFilm, boxFilm;
	private float t = 0;

	private void build(){
		built = true;
		ColiseumRoom c = TourneyQuest.coliseum(Dungeon.level);
		if (c == null) return;
		Level L = Dungeon.level;
		int gate = c.gateCell(L);
		for (int x = c.left + 1; x < c.right; x++){
			for (int y = c.top; y <= c.bottom; y++){
				boolean edge = y == c.top || y == c.bottom;
				if (!edge && x != c.left + 1 && x != c.right - 1) continue;
				int cell = x + y * L.width();
				if (cell == gate || !L.solid[cell]) continue;
				Image im = new Image(QuestAssets.CROWD);
				if (crowdFilm == null) crowdFilm = new TextureFilm(im.texture, 16, 16);
				im.frame(crowdFilm.get(0));
				PointF p = DungeonTilemap.tileToWorld(cell);
				im.x = p.x;
				im.y = p.y;
				crowd.add(im);
				phase.add(Random.Float(6.28f));
				add(im);
			}
		}
		//royal box, middle of the wall facing the gate
		int bx, by;
		switch (c.gateSide){
			case 0: bx = (c.left + c.right) / 2; by = c.bottom; break;
			case 1: bx = c.left; by = (c.top + c.bottom) / 2; break;
			case 3: bx = c.right; by = (c.top + c.bottom) / 2; break;
			default: bx = (c.left + c.right) / 2; by = c.top; break;
		}
		box = new Image(QuestAssets.ROYAL_BOX);
		boxFilm = new TextureFilm(box.texture, 48, 32);
		box.frame(boxFilm.get(0));
		PointF p = DungeonTilemap.tileToWorld(bx + by * L.width());
		box.x = p.x + 8 - 24;
		box.y = p.y + 16 - 32;
		add(box);
	}

	@Override
	public void update() {
		super.update();
		if (!built) build();
		float e = Game.elapsed;
		t += e;
		if (cheerT > 0) cheerT -= e;
		if (booT > 0) booT -= e;
		for (int i = 0; i < crowd.size(); i++){
			float s = (float) Math.sin(t * (cheerT > 0 ? 9 : 2.5f) + phase.get(i));
			int base = cheerT > 0 ? 2 : booT > 0 ? 4 : 0;
			crowd.get(i).frame(crowdFilm.get(base + (s > 0 ? 1 : 0)));
		}
		if (box != null){
			int f = 0;
			if (TourneyQuest.Q.duel > 0) f = 1;
			if (cheerT > 0) f = ((int) (t * 4)) % 2 == 0 ? 2 : 1;
			box.frame(boxFilm.get(f));
			box.visible = !TourneyQuest.recognized() || TourneyQuest.Q.duel > 0 || cheerT > 0 || TourneyQuest.Q.round < 3;
		}
	}
}
