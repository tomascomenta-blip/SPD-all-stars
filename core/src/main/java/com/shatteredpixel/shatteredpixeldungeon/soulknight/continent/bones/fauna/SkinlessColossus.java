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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.bones.fauna;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.survival.ColossusMeat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildSprite;
import com.watabou.utils.PointF;

import java.util.ArrayList;

//skinless colossus: a young one, all muscle and no hide, the spine bare, crawling on all fours with
//a short arm and a long one. it raises the long arm (marked) and brings it down on the band in
//front of it. it's always hungry: hand it raw colossus meat and it lets the collar on
public class SkinlessColossus extends WildColossus {

	{
		size = 5;
		spriteClass = SkinlessSprite.class;

		HP = HT = 980;
		defenseSkill = 12;
		EXP = 60;
		baseSpeed = 0.9f;

		minDmg = 44; maxDmg = 58; acc = 62;
		armorMin = 6; armorMax = 16;
	}

	private static final int SWIPE = 0;

	//the bare spine
	@Override
	public int[][] weakSpots() {
		return new int[][]{{2, 0}, {3, 0}};
	}

	@Override
	public boolean tameable() {
		return true;
	}

	@Override
	public String tameBlock( Hero h ) {
		return h.belongings.getItem(ColossusMeat.Raw.class) != null ? null : Messages.get(this, "hungry");
	}

	@Override
	public void tame( Hero h ) {
		Item meat = h.belongings.getItem(ColossusMeat.Raw.class);
		if (meat != null) meat.detach(h.belongings.backpack);
		super.tame(h);
	}

	@Override
	protected boolean special( Char enemy ) {
		if (reach(enemy.pos) > 3) return false;
		face(enemy.pos);
		ArrayList<Integer> f = front(3, 1);
		if (!caught(f, enemy)) return false;
		warn(SWIPE, f, enemy.pos, 2f);
		return true;
	}

	@Override
	protected int[] power( int kind ) {
		return new int[]{105, 140};
	}

	@Override
	protected void onBlast( int kind, Char ch ) {
		knock(ch, 2);
	}

	//0-1 idle, 2-5 crawl, 6 ready, 7 the blow, 8 down, 9 dead, 10 the long arm up
	public static class SkinlessSprite extends WildSprite {

		public SkinlessSprite(){
			super();
			beast(BonesAssets.SKINLESS, 5, 112, 112, 16, 32);
		}

		@Override
		protected int ready( int kind ) {
			return 10;
		}

		@Override
		protected int[] tamedIdle() {
			return new int[]{0, 1};
		}

		@Override
		protected String tamedSheet() {
			return BonesAssets.SKINLESS_TAMED;
		}

		@Override
		protected PointF weakAnchor() {
			return new PointF(58, 52);
		}
	}
}
