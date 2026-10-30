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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.forest;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forgotten.ForgottenFeatures;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.props.SKProps;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

//stuff in the high priest's clearing: the big ritual bonfire in the middle (nothing goes through it)
//and the sacred stones (stop bullets, and when the giant star lands only those next to one are safe)
public class PriestProps {

	//nothing can hurt it, it's a bonfire / a rock
	public static abstract class Solid extends SKProps.SKProp {
		{
			HP = HT = 1000;
		}

		@Override
		public boolean isInvulnerable( Class effect ) {
			return true;
		}

		@Override
		public void damage( int dmg, Object src ) {
		}

		@Override
		public boolean add( Buff buff ) {
			return false;
		}
	}

	public static class Bonfire extends Solid {

		{
			spriteClass = BonfireSprite.class;
		}

		public static final int LIT = 0, COLUMN = 1, OUT = 2;
		public int look = LIT;

		public void look( int l ){
			look = l;
		}

		@Override
		public String description() {
			return Messages.get(this, look == OUT ? "desc_out" : "desc");
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("look", look);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			look = bundle.getInt("look");
		}
	}

	//bonfire.png 32x48: 0-3 fire, 4-6 fire column, 7 out, 8 embers
	public static class BonfireSprite extends ForgottenFeatures.StripSprite {

		private int shown = -1;

		public BonfireSprite(){
			super();
			perspectiveRaise = 0;
			strip(PriestAssets.BONFIRE, 32, 48, 8);
			show(Bonfire.LIT);
		}

		private void show( int look ){
			shown = look;
			idle = new Animation(look == Bonfire.COLUMN ? 12 : 8, true);
			switch (look){
				case Bonfire.COLUMN: idle.frames(film, 4, 5, 6, 5); break;
				case Bonfire.OUT:    idle = new Animation(2, false); idle.frames(film, 7, 8); break;
				default:             idle.frames(film, 0, 1, 2, 3); break;
			}
			run = idle.clone();
			attack = idle.clone();
			zap = idle.clone();
			play(idle, true);
		}

		@Override
		public void update() {
			super.update();
			if (ch instanceof Bonfire && ((Bonfire) ch).look != shown) show(((Bonfire) ch).look);
		}
	}

	public static class SacredStone extends Solid {

		{
			spriteClass = StoneSprite.class;
		}

		public int cracks = 0;

		//priest crashed into it, or the star landed next to it
		public void crack(){
			cracks++;
			if (Dungeon.level.heroFOV[pos]){
				Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 0.9f);
				CellEmitter.get(pos).burst(Speck.factory(Speck.ROCK), 5);
			}
		}

		@Override
		public String description() {
			return Messages.get(this, "desc");
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("cracks", cracks);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			cracks = bundle.getInt("cracks");
		}
	}

	//sacred_stone.png 16x32: 0 idle, 1 runes glowing (star incoming), 2 cracked
	public static class StoneSprite extends ForgottenFeatures.StripSprite {
		public StoneSprite(){
			super();
			perspectiveRaise = 0;
			strip(PriestAssets.STONE, 16, 32, 1);
			still(0);
		}

		@Override
		public void update() {
			super.update();
			if (!(ch instanceof SacredStone)) return;
			GoblinPriest p = ForestBossLevel.priest(Dungeon.level);
			if (p != null && p.starTimer > 0) still(1);
			else still(((SacredStone) ch).cracks > 0 ? 2 : 0);
		}
	}
}
