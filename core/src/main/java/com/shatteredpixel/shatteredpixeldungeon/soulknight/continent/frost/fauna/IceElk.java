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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.AshFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PointF;

import java.util.ArrayList;

//elk of the ice antlers. dark frosted hide, antlers of clear ice, a muzzle stained with blood. it
//grazes and leaves you be, until it's hurt or you walk right up to it: then it bellows (the herd
//comes) and lowers the antlers down a marked lane, and goes through it. beaten down, it lets the
//collar on
public class IceElk extends WildColossus {

	{
		size = 5;
		spriteClass = ElkSprite.class;

		HP = HT = 860;
		defenseSkill = 16;
		EXP = 55;

		minDmg = 40; maxDmg = 54; acc = 60;
		armorMin = 8; armorMax = 18;
	}

	private static final int CHARGE = 0;

	private boolean startled = false;

	@Override
	public boolean peaceful() {
		return !startled && !tamed;
	}

	//the withers, seen from above
	@Override
	public int[][] weakSpots() {
		return new int[][]{{3, 0}, {4, 0}};
	}

	@Override
	public boolean tameable() {
		return true;
	}

	@Override
	public String tameBlock( Hero h ) {
		return down > 0 ? null : Messages.get(this, "standing");
	}

	@Override
	protected int ears() {
		return 3;
	}

	@Override
	protected boolean act() {
		Hero h = nearestHero();
		if (!startled && !tamed && h != null && reach(h.pos) <= 2) startle(h);
		return super.act();
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (!startled && !tamed && dmg > 0 && src instanceof Char && isAlive()) startle((Char) src);
		super.damage(dmg, src);
	}

	private void startle( Char by ){
		startled = true;
		aggro(by);
		Senses.noise(pos, Senses.ROAR);
		if (sprite instanceof ElkSprite) ((ElkSprite) sprite).pose(5, 10, 10, 10, 0);
		if (seen()) Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 0.8f, 0.7f);
		for (Mob m : AshFauna.sorted(Dungeon.level.mobs)){
			if (m instanceof IceElk && m != this && Dungeon.level.distance(m.pos, pos) <= 14) ((IceElk) m).join(by);
		}
	}

	//the rest of the herd comes along
	private void join( Char by ){
		if (startled || tamed) return;
		startled = true;
		aggro(by);
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = reach(enemy.pos);
		if (d < 2 || d > 8) return false;
		ArrayList<Integer> lane = lane(enemy.pos, 7, 5);
		if (!caught(lane, enemy)) return false;
		warn(CHARGE, lane, enemy.pos, 3f);
		return true;
	}

	@Override
	protected int[] power( int kind ) {
		return new int[]{95, 125};
	}

	//and it goes on through, as far as there's room
	@Override
	protected void afterBlast( int kind, ArrayList<Integer> cells, int aim ) {
		int start = pos;
		for (int i = 0; i < 6 && isAlive(); i++){
			int w = Dungeon.level.width();
			int dx = Integer.signum(aim % w - pos % w), dy = Integer.signum(aim / w - pos / w);
			int next = pos + dx + dy * w;
			if (dx == 0 && dy == 0 || !fitsAt(next)) break;
			walkTo(next);
		}
		if (sprite != null && pos != start) sprite.move(start, pos);
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("startled", startled);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		startled = bundle.getBoolean("startled");
	}

	//0-1 idle, 2-5 walk, 6 antlers down, 7 the charge, 8 down, 9 dead, 10 bellows
	public static class ElkSprite extends WildSprite {

		public ElkSprite(){
			super();
			beast(FrostAssets.ELK, 5, 112, 112, 16, 32);
		}

		@Override
		protected int[] tamedIdle() {
			return new int[]{0, 10, 0, 1};
		}

		@Override
		protected String tamedSheet() {
			return FrostAssets.ELK_TAMED;
		}

		@Override
		protected PointF weakAnchor() {
			return new PointF(74, 56);
		}
	}
}
