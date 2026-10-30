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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.flesh.fauna;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//flesh chimera. several animals sewn together: bare muscle between scraps of hide that don't
//match, each leg off a different beast (the short one makes it limp), a dog's head, a goat's head
//with its eyes stitched shut and a snake for a tail. the dog bites what's in front (marked), the
//three heads roar together all around it, and the snake bites whoever stands behind, unmarked
public class FleshChimera extends FleshColossus {

	{
		size = 5;
		spriteClass = ChimeraSprite.class;

		HP = HT = 900;
		defenseSkill = 10;
		EXP = 55;
		baseSpeed = 0.7f;

		minDmg = 42; maxDmg = 56; acc = 60;
		armorMin = 10; armorMax = 24;
	}

	private static final int BITE = 0, ROAR = 1;

	private int tailT = 0;

	//the bruised patch on its back, where the stitches gave
	@Override
	public int[][] weakSpots() {
		return new int[][]{{2, 0}, {2, 1}};
	}

	@Override
	protected int ears() {
		return 3;
	}

	@Override
	protected int specialCooldown() {
		return 3;
	}

	@Override
	protected boolean act() {
		if (state == HUNTING && paralysed <= 0 && down == 0 && !tamed && ++tailT >= 3 && tail()){
			tailT = 0;
			spend(TICK);
			return true;
		}
		return super.act();
	}

	//the snake goes for whoever stands right behind it
	private boolean tail(){
		int w = Dungeon.level.width();
		for (Hero h : Party.living()){
			if (reach(h.pos) != 1) continue;
			int dx = h.pos % w - pos % w;
			if (right ? dx >= 0 : dx <= 0) continue;
			int dmg = Math.min(SKCombat.specialDamage(this, h, 24, 34), Math.max(0, h.HP - 1));
			h.damage(dmg, this);
			if (h.isAlive()) Buff.affect(h, Poison.class).set(4);
			if (h.sprite != null && h.sprite.visible) h.sprite.showStatus(CharSprite.WARNING, Messages.get(this, "tail"));
			return true;
		}
		return false;
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = reach(enemy.pos);
		if (d > 3) return false;
		if (SKCombat.foesIn(this, ring(2)).size() >= 2 || d >= 2){
			warn(ROAR, ring(2), enemy.pos, 2f);
			return true;
		}
		ArrayList<Integer> f = front(enemy.pos, 2, 0);
		if (!f.contains(enemy.pos)) return false;
		warn(BITE, f, enemy.pos, 1.5f);
		return true;
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		strikeAnim(kind);
		Senses.noise(pos, kind == ROAR ? Senses.ROAR : Senses.CRASH);
		if (seen()) Sample.INSTANCE.play(kind == ROAR ? Assets.Sounds.CHALLENGE : Assets.Sounds.HIT_STRONG, 1f, 0.7f);
		for (Char ch : SKCombat.foesIn(this, cells)){
			if (kind == BITE){
				blast(ch, 80, 110);
				if (ch.isAlive()) Buff.affect(ch, Bleeding.class).set(12);
			} else {
				blast(ch, 55, 75);
				if (ch.isAlive()) Buff.prolong(ch, Vertigo.class, 3f);
			}
		}
	}

	//6 crouching, 7 the bite, 8 down, 9 dead, 10 the roar of the three heads
	public static class ChimeraSprite extends Sheet {

		public ChimeraSprite(){
			super();
			layout(FleshAssets.CHIMERA, 5, 112, 112, 16, 32, 8, 9);
			run = loop(6, 2, 3, 4, 5);
		}

		@Override
		public void windup( int kind ) {
			play(loop(1, kind == ROAR ? 10 : 6));
		}

		@Override
		public void strike( int kind ) {
			once(kind == ROAR ? clip(5, 10, 10, 0) : clip(10, 7, 7, 0));
		}
	}
}
