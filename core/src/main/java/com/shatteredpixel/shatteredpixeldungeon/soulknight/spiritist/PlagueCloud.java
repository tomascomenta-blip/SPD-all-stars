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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.spiritist;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

//omen stone plague. no spreading, fades 1/turn, enemies only
public class PlagueCloud extends Blob {

	public static int strength(){
		int s = 3 + Dungeon.depth/3;
		int virulence = 0;
		for (Hero h : Party.living()){
			if (h.heroClass == HeroClass.SPIRITIST) virulence = Math.max(virulence, h.pointsInTalent(Talent.PH_VIRULENCE));
		}
		return s + 2*virulence;
	}

	@Override
	protected void evolve() {
		int w = Dungeon.level.width();
		int str = -1;
		for (int i = area.left; i < area.right; i++){
			for (int j = area.top; j < area.bottom; j++){
				int cell = i + j*w;
				off[cell] = cur[cell] > 0 ? cur[cell] - 1 : 0;
				volume += off[cell];
				if (off[cell] > 0){
					Char ch = Actor.findChar(cell);
					if (ch != null && ch.alignment == Char.Alignment.ENEMY && ch.isAlive()){
						if (str < 0) str = strength();
						Poison p = ch.buff(Poison.class);
						if (p == null || p.cooldown() < str) Buff.affect(ch, Poison.class).set(str);
					}
				}
			}
		}
	}

	@Override
	public void use( BlobEmitter emitter ) {
		super.use(emitter);
		emitter.pour(Speck.factory(Speck.TOXIC), 0.5f);
	}

	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}
}
