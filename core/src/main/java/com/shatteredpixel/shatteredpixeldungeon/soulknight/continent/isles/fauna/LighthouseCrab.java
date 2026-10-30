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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.isles.fauna;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.survival.SacredTorch;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.watabou.utils.PointF;

//lighthouse crab. a huge crab wearing a ruined lighthouse for a shell, the lamp still lit inside a
//cage of iron. the light draws things: its beam sweeps a marked strip, blinds and drags whoever it
//caught a few steps towards the pincers. the pincer itself is a marked blow in front
public class LighthouseCrab extends WildColossus {

	{
		size = 5;
		spriteClass = CrabSprite.class;

		HP = HT = 900;
		defenseSkill = 14;
		EXP = 60;

		minDmg = 40; maxDmg = 54; acc = 62;
		armorMin = 14; armorMax = 26;
	}

	private static final int PINCER = 0, BEAM = 1;

	@Override
	protected boolean wades() {
		return true;
	}

	//where the tower sinks into the shell
	@Override
	public int[][] weakSpots() {
		return new int[][]{{1, 4}, {2, 4}};
	}

	//its lamp goes out in the end: whoever brings it a sacred torch for it gets to put the collar on
	@Override
	public boolean tameable() {
		return true;
	}

	@Override
	public String tameBlock( Hero h ) {
		return h.belongings.getItem(SacredTorch.class) != null ? null : Messages.get(this, "lamp");
	}

	@Override
	public void tame( Hero h ) {
		Item torch = h.belongings.getItem(SacredTorch.class);
		if (torch != null) torch.detach(h.belongings.backpack);
		super.tame(h);
	}

	//its own lamp: it sees whoever it lights
	@Override
	protected int eyes() {
		return 6;
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = reach(enemy.pos);
		if (d >= 3 && d <= 10){
			warn(BEAM, lane(enemy.pos, 10, 3), enemy.pos, 2f);
			return true;
		}
		if (d <= 2){
			face(enemy.pos);
			if (caught(front(2, 1), enemy)){
				warn(PINCER, front(2, 1), enemy.pos, 2f);
				return true;
			}
		}
		return false;
	}

	@Override
	protected int[] power( int kind ) {
		return kind == PINCER ? new int[]{80, 110} : new int[]{25, 40};
	}

	@Override
	protected int dust( int kind ) {
		return kind == BEAM ? Speck.LIGHT : Speck.DUST;
	}

	@Override
	protected String blowSound( int kind ) {
		return kind == BEAM ? Assets.Sounds.RAY : Assets.Sounds.HIT_STRONG;
	}

	@Override
	protected void onBlast( int kind, Char ch ) {
		if (kind != BEAM) return;
		Buff.prolong(ch, Blindness.class, 2f);
		pull(ch, 3);
	}

	//towards the body, as far as there's room
	private void pull( Char ch, int cells ){
		if (ch.rooted || Char.hasProp(ch, Property.IMMOVABLE)) return;
		Level l = Dungeon.level;
		int w = l.width();
		int to = ch.pos;
		for (int i = 0; i < cells && reach(to) > 1; i++){
			int dx = -Integer.signum(to % w - pos % w), dy = -Integer.signum(to / w - pos / w);
			int next = to + dx + dy * w;
			if (next < 0 || next >= l.length() || !l.passable[next] || l.pit[next] || Actor.findChar(next) != null || covers(next)) break;
			to = next;
		}
		if (to == ch.pos) return;
		ch.pos = to;
		if (ch.sprite != null) ch.sprite.place(to);
		l.occupyCell(ch);
		if (ch instanceof Hero) Dungeon.observe();
		Senses.noise(to, Senses.STEP);
	}

	//0-1 idle, 2-5 walk, 6 ready, 7 pincer, 8 down, 9 dead, 10 the beam
	public static class CrabSprite extends WildSprite {

		public CrabSprite(){
			super();
			beast(IslesAssets.CRAB, 5, 112, 112, 16, 32);
		}

		@Override
		protected int ready( int kind ) {
			return kind == BEAM ? 10 : 6;
		}

		@Override
		protected int blow( int kind ) {
			return kind == BEAM ? 10 : 7;
		}

		@Override
		protected PointF weakAnchor() {
			return new PointF(50, 82);
		}

		@Override
		protected String tamedSheet() {
			return IslesAssets.CRAB_TAMED;
		}
	}
}
