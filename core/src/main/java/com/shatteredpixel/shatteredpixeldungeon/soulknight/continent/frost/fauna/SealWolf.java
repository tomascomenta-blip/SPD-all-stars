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
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

//seal-wolf: a seal's body under frosted fur with raw bald patches, and a wolf's head. it waits under
//the black water of a hole in the ice and comes out at whoever walks by the edge (an ambush: it
//bites hard but never kills), then fights on land. with nobody around it goes back under
public class SealWolf extends WildColossus {

	{
		size = 3;
		spriteClass = SealSprite.class;

		HP = HT = 420;
		defenseSkill = 18;
		EXP = 34;

		minDmg = 34; maxDmg = 46; acc = 62;
		armorMin = 4; armorMax = 12;
	}

	private boolean hidden = false;
	private int idleT = 0;

	@Override
	protected boolean wades() {
		return true;
	}

	//the head
	@Override
	public int[][] weakSpots() {
		return new int[][]{{2, 1}};
	}

	@Override
	protected int ears() {
		return 2;
	}

	@Override
	protected boolean act() {
		if (hidden){
			Hero h = nearestHero();
			if (h != null && h.invisible == 0 && reach(h.pos) <= 2){
				surface(h);
				return true;
			}
			spend(TICK);
			return true;
		}
		if (state != HUNTING && Dungeon.level.water[pos] && !tamed){
			if (++idleT >= 8) dive();
		} else {
			idleT = 0;
		}
		return super.act();
	}

	private void dive(){
		hidden = true;
		idleT = 0;
		invisible++;
		if (sprite instanceof SealSprite) ((SealSprite) sprite).under(true);
	}

	private void surface( Hero h ){
		hidden = false;
		invisible = Math.max(0, invisible - 1);
		if (sprite instanceof SealSprite){
			((SealSprite) sprite).under(false);
			((SealSprite) sprite).pose(6, 8, 8, 7, 0);
		}
		Senses.noise(pos, Senses.FIGHT);
		if (seen()) Sample.INSTANCE.play(Assets.Sounds.WATER, 1f, 0.8f);
		ambush(h, 45, 65);
		aggro(h);
		state = HUNTING;
		spend(TICK);
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (hidden && src instanceof Hero && isAlive()){
			hidden = false;
			invisible = Math.max(0, invisible - 1);
			if (sprite instanceof SealSprite) ((SealSprite) sprite).under(false);
		}
		super.damage(dmg, src);
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("hidden", hidden);
		bundle.put("idle_t", idleT);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		hidden = bundle.getBoolean("hidden");
		idleT = bundle.getInt("idle_t");
		if (hidden) invisible++;
	}

	//0-1 idle, 2-5 crawl, 6 ready, 7 bite, 8 just the head out of a hole
	public static class SealSprite extends WildSprite {

		private Animation land;

		public SealSprite(){
			super();
			beast(FrostAssets.SEAL, 3, 64, 64, 8, 16);
			land = idle;
		}

		public void under( boolean on ){
			look(on ? loop(1, 8) : land);
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (ch instanceof SealWolf && ((SealWolf) ch).hidden) under(true);
		}
	}
}
