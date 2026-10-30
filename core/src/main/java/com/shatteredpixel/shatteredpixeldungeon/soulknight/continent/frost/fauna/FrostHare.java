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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.frost.fauna;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.AshCritter;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.CritterSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

//frost hare. matted grey fur stiff with frost, long ears, a red eye. harmless: it runs. but the
//first one to see you stands up and shrieks, and everything that hunts around here hears it
public class FrostHare extends AshCritter {

	{
		spriteClass = HareSprite.class;

		HP = HT = 45;
		defenseSkill = 44;
		EXP = 3;
		baseSpeed = 1.6f;

		minDmg = 6; maxDmg = 12; acc = 40;
		armorMin = 0; armorMax = 2;
	}

	private boolean alerted = false;

	@Override
	protected int eyes() {
		return 3;
	}

	@Override
	protected int ears() {
		return 5;
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		return false;
	}

	@Override
	public void notice() {
		super.notice();
		if (!alerted){
			alerted = true;
			Senses.noise(pos, Senses.ROAR);
			if (sprite instanceof HareSprite) ((HareSprite) sprite).pose(4, 8, 8, 8, 0);
			if (Dungeon.level.heroFOV[pos]){
				Sample.INSTANCE.play(Assets.Sounds.ALERT, 0.6f, 1.6f);
				GLog.w(Messages.get(this, "shriek"));
			}
		}
		state = FLEEING;
	}

	//once it has seen someone it only runs
	@Override
	protected boolean act() {
		if (alerted && state == HUNTING) state = FLEEING;
		return super.act();
	}

	//a noise only sends it running
	@Override
	protected void heard( Senses.Noise n ) {
		if (state == HUNTING) state = FLEEING;
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("alerted", alerted);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		alerted = bundle.getBoolean("alerted");
	}

	//0-1 idle, 2-5 hops, 6-7 a kick, 8 up on its hind legs
	public static class HareSprite extends CritterSprite {

		public HareSprite(){
			super();
			critter(FrostAssets.HARE);
		}
	}
}
