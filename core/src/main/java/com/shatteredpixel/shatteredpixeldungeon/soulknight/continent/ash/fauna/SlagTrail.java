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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.utils.Random;

//molten slag left behind by a smelter beetle: a furrow that burns whoever stands in it, and cools
//down in a few turns. it never spreads, so a whole plain of ash doesn't go up
public class SlagTrail extends Blob {

	public static final int HEAT = 9;

	public static void pour( int cell, int heat ){
		if (cell < 0 || Dungeon.level.solid[cell] || Dungeon.level.pit[cell]) return;
		GameScene.add(Blob.seed(cell, heat, SlagTrail.class));
	}

	@Override
	protected void evolve() {
		int w = Dungeon.level.width();
		for (int i = area.left; i < area.right; i++){
			for (int j = area.top; j < area.bottom; j++){
				int cell = i + j * w;
				off[cell] = cur[cell] > 0 ? cur[cell] - 1 : 0;
				volume += off[cell];
				if (off[cell] > 0){
					Char ch = Actor.findChar(cell);
					if (ch != null && ch.pos == cell && !ch.flying && !Char.hasProp(ch, Char.Property.FIERY) && ch.isAlive()){
						Buff.affect(ch, Burning.class).reignite(ch, 3f);
						ch.damage(Random.NormalIntRange(6, 12), this);
					}
				}
			}
		}
	}

	@Override
	public void use( BlobEmitter emitter ) {
		super.use(emitter);
		emitter.pour(FlameParticle.FACTORY, 0.25f);
	}

	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}
}
