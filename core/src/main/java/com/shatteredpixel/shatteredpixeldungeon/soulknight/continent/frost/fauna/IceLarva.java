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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.AshCritter;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.CritterSprite;
import com.watabou.utils.Bundle;

//ice larva. pale blue rings you can see the dark gut through, and black pincers. it lies buried in
//the snow and comes up under whoever steps next to it (that bite chills, but never kills), then
//crawls after them. left alone a while, it digs back in
public class IceLarva extends AshCritter {

	{
		spriteClass = LarvaSprite.class;

		HP = HT = 70;
		defenseSkill = 20;
		EXP = 5;
		baseSpeed = 0.8f;

		minDmg = 16; maxDmg = 24; acc = 56;
		armorMin = 2; armorMax = 8;
	}

	private boolean buried = false;
	private int idleT = 0;

	@Override
	protected boolean act() {
		if (buried){
			for (Hero h : Party.living()){
				if (h.invisible == 0 && Dungeon.level.adjacent(h.pos, pos)){
					burst(h);
					return true;
				}
			}
			spend(TICK);
			return true;
		}
		if (state != HUNTING){
			if (++idleT >= 6) bury();
		} else {
			idleT = 0;
		}
		return super.act();
	}

	private void bury(){
		buried = true;
		idleT = 0;
		invisible++;
		if (sprite instanceof LarvaSprite) ((LarvaSprite) sprite).hold(1, 8);
	}

	private void burst( Hero h ){
		buried = false;
		invisible = Math.max(0, invisible - 1);
		if (sprite instanceof LarvaSprite){
			((LarvaSprite) sprite).hold(2, 0, 0, 1, 1);
			((LarvaSprite) sprite).pose(10, 6, 7, 7, 0);
		}
		int d = Math.min(SKCombat.specialDamage(this, h, 24, 34), h.HP - 1);
		if (d > 0) h.damage(d, this);
		if (h.isAlive()) Buff.prolong(h, Chill.class, 3f);
		aggro(h);
		state = HUNTING;
		spend(TICK);
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (buried && src instanceof Char){
			buried = false;
			invisible = Math.max(0, invisible - 1);
			if (sprite instanceof LarvaSprite) ((LarvaSprite) sprite).hold(2, 0, 0, 1, 1);
		}
		super.damage(dmg, src);
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("buried", buried);
		bundle.put("idle_t", idleT);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		buried = bundle.getBoolean("buried");
		idleT = bundle.getInt("idle_t");
		if (buried) invisible++;
	}

	//0-1 idle, 2-5 crawling, 6-7 the bite, 8 digging in
	public static class LarvaSprite extends CritterSprite {

		public LarvaSprite(){
			super();
			critter(FrostAssets.LARVA);
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (ch instanceof IceLarva && ((IceLarva) ch).buried) hold(1, 8);
		}
	}
}
