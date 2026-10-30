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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.fauna;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Light;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.AshColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.AshFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

//lamplighter. an empty skin walking about with a lit lantern. it keeps its distance and, when it
//finds someone, lifts the lantern over them: whoever it lights up is seen from far by every big
//thing of the city, and the ones close by come
public class Lamplighter extends CityCritter {

	{
		spriteClass = LampSprite.class;

		HP = HT = 70;
		defenseSkill = 30;
		EXP = 8;

		minDmg = 14; maxDmg = 20; acc = 52;
		armorMin = 0; armorMax = 4;
	}

	private static final int REACH = 6, LIT = 10;

	@Override
	protected int eyes() {
		return 2;
	}

	@Override
	protected boolean keepsDistance() {
		return true;
	}

	@Override
	protected int specialCooldown() {
		return 8;
	}

	@Override
	protected boolean special( Char enemy ) {
		if (!(enemy instanceof Hero) || Dungeon.level.distance(pos, enemy.pos) > REACH) return false;
		//someone already lit up is left alone
		Light lit = enemy.buff(Light.class);
		if (lit != null && lit.cooldown() > 3) return false;
		Buff.prolong(enemy, Light.class, LIT);
		for (Mob m : AshFauna.sorted(Dungeon.level.mobs)){
			if (m instanceof AshColossus && m.alignment == alignment && m.state != m.HUNTING
					&& Dungeon.level.distance(m.pos, enemy.pos) <= 24){
				m.beckon(enemy.pos);
			}
		}
		Senses.noise(pos, Senses.FIGHT);
		if (sprite instanceof LampSprite){
			sprite.turnTo(pos, enemy.pos);
			((LampSprite) sprite).special();
		}
		if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.BURNING, 0.5f, 1.5f);
		if (enemy == Dungeon.local()) GLog.w(Messages.get(this, "lit"));
		spend(TICK);
		return true;
	}

	public static class LampSprite extends Sheet {

		public LampSprite(){
			super();
			layout(CityAssets.LAMPLIGHTER);
		}
	}
}
