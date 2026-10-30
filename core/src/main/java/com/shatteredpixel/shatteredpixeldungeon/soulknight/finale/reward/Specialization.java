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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.finale.reward;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;

//the three paths the heart of Thal-Vekhorr offers. any class can take any of them, they are
//about what waits on the other side (demons, the dark, not much food), not about how you fight.
//the power of each one lives in SpecPowers, the talents are the 5th row (levels 31-40)
public enum Specialization {

	YOG_HUNTER(HeroIcon.YOG_HUNTER),
	LIGHTBEARER(HeroIcon.LIGHTBEARER),
	TIME_WANDERER(HeroIcon.TIME_WANDERER);

	private final int icon;

	Specialization( int icon ){
		this.icon = icon;
	}

	public int icon(){
		return icon;
	}

	public String title(){
		return Messages.get(this, name());
	}

	public String shortDesc(){
		return Messages.get(this, name() + "_short_desc");
	}

	public String desc(){
		return Messages.get(this, name() + "_desc");
	}

	public Talent[] talents(){
		switch (this){
			case YOG_HUNTER: default:
				return new Talent[]{Talent.YH_BANE, Talent.YH_SCENT, Talent.YH_HIDE, Talent.YH_TROPHY};
			case LIGHTBEARER:
				return new Talent[]{Talent.LB_BEACON, Talent.LB_VESSEL, Talent.LB_DAWN, Talent.LB_RADIANCE};
			case TIME_WANDERER:
				return new Talent[]{Talent.TW_STOMACH, Talent.TW_LONG_PAUSE, Talent.TW_CLOCKWORK, Talent.TW_QUICKSTEP};
		}
	}
}
