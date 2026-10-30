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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.starship;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKBulletField;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forgotten.ForgottenFeatures;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.props.SKProps;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;

//queen's lair props, which is just the airlock console. hit it to arm the airlock, it opens a turn later
//and pulls everything not holding a handhold. then it needs a few turns to recharge
public class HiveProps {

	public static class AirlockConsole extends SKProps.SKProp {

		{
			spriteClass = ConsoleSprite.class;
			HP = HT = 1000;
		}

		//hits reach it (that's how you use it) but never hurt it, see damage()
		@Override
		public boolean add( Buff buff ) {
			return false;
		}

		@Override
		protected boolean act() {
			spend(TICK);
			if (Dungeon.level instanceof StarshipBossLevel) ((StarshipBossLevel) Dungeon.level).airlockTurn();
			return true;
		}

		//only heroes + allies can work it, and only while the queen's fighting in her lair
		@Override
		public void damage( int dmg, Object src ) {
			Char ch = src instanceof Char ? (Char) src : src instanceof SKBulletField.Shot ? ((SKBulletField.Shot) src).owner : null;
			if (ch == null || ch.alignment != Alignment.ALLY) return;
			if (!(Dungeon.level instanceof StarshipBossLevel)) return;
			StarshipBossLevel L = (StarshipBossLevel) Dungeon.level;
			VarkolynLeader q = StarshipBossLevel.queen(L);
			if (q == null || !q.inLair()){
				if (Dungeon.level.heroFOV[pos]) GLog.i(Messages.get(this, "locked"));
				return;
			}
			if (L.airlock != StarshipBossLevel.AIRLOCK_READY){
				if (Dungeon.level.heroFOV[pos]) GLog.i(Messages.get(this, "busy"));
				return;
			}
			Sample.INSTANCE.play(Assets.Sounds.HIT_PARRY, 0.7f, 1.3f);
			L.armAirlock();
		}

		@Override
		public String description() {
			if (!(Dungeon.level instanceof StarshipBossLevel)) return Messages.get(this, "desc");
			int state = ((StarshipBossLevel) Dungeon.level).airlock;
			return Messages.get(this, "desc") + "\n\n" + Messages.get(this, "state_" + state);
		}
	}

	//airlock_console.png 16x24: 0 ready, 1 armed, 2 recharging, 3 struck
	public static class ConsoleSprite extends ForgottenFeatures.StripSprite {
		public ConsoleSprite(){
			super();
			strip(VarkolynAssets.CONSOLE, 16, 24, 1);
			still(0);
		}

		@Override
		public void update() {
			super.update();
			if (!(Dungeon.level instanceof StarshipBossLevel)) return;
			switch (((StarshipBossLevel) Dungeon.level).airlock){
				case StarshipBossLevel.AIRLOCK_ARMED: case StarshipBossLevel.AIRLOCK_OPEN:
					still(((int) (Game.timeTotal * 4)) % 2 == 0 ? 1 : 2);
					break;
				case StarshipBossLevel.AIRLOCK_RECHARGE: still(2); break;
				default: still(0); break;
			}
		}
	}
}
