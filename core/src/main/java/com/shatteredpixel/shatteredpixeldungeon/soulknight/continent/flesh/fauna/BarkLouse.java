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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentMob;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;

//bark louse. a flat thing of bark plates, six legs and bone jaws, living on the flesh trees. it
//jumps from a few cells off straight onto whoever walks by and sticks there: the bite is small, but
//it holds you back while it's on
public class BarkLouse extends FleshCritter {

	{
		spriteClass = LouseSprite.class;

		HP = HT = 30;
		defenseSkill = 40;
		EXP = 3;
		baseSpeed = 1.5f;

		minDmg = 8; maxDmg = 12; acc = 64;
		armorMin = 0; armorMax = 4;
	}

	@Override
	protected int ears() {
		return 2;
	}

	@Override
	protected int specialCooldown() {
		return 4;
	}

	//a jump next to the prey and a bite on landing. no mark, so it can't finish anyone
	@Override
	protected boolean special( Char enemy ) {
		int d = Dungeon.level.distance(pos, enemy.pos);
		if (d < 2 || d > 4 || rooted) return false;
		ArrayList<Integer> cells = ContinentMob.freeAround(enemy.pos);
		if (cells.isEmpty()) return false;
		int best = cells.get(0);
		for (int c : cells) if (Dungeon.level.distance(c, pos) < Dungeon.level.distance(best, pos)) best = c;
		int from = pos;
		move(best, false);
		if (sprite != null){
			sprite.turnTo(from, enemy.pos);
			if (sprite instanceof LouseSprite) ((LouseSprite) sprite).leap();
			sprite.jump(from, best, new Callback() {
				@Override
				public void call() {
				}
			});
		}
		Dungeon.level.occupyCell(this);
		int dmg = unmarked(enemy, SKCombat.specialDamage(this, enemy, 8, 14));
		enemy.damage(dmg, this);
		stick(enemy);
		spend(TICK);
		return true;
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = unmarked(enemy, super.attackProc(enemy, damage));
		if (Random.Int(3) == 0) stick(enemy);
		return damage;
	}

	private void stick( Char enemy ){
		if (!enemy.isAlive()) return;
		Buff.prolong(enemy, Cripple.class, 2f);
		if (enemy.sprite != null && Dungeon.level.heroFOV[enemy.pos]){
			enemy.sprite.showStatus(CharSprite.WARNING, Messages.get(this, "stuck"));
			Sample.INSTANCE.play(Assets.Sounds.HIT, 0.6f, 1.6f);
		}
	}

	//8 in the air, 9 stuck on the prey
	public static class LouseSprite extends Sheet {

		public LouseSprite(){
			super();
			layout(FleshAssets.LOUSE);
			attack = new Animation(12, false);
			attack.frames(frames, 6, 7, 9, 9);
		}

		public void leap(){
			play(special);
		}
	}
}
