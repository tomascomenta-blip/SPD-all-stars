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
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

//flesh gargoyle. it sits up on the ledges as still as the stone ones and drops on whoever walks
//under it: the leap isn't marked, so it never takes anyone down from full in one go
public class FleshGargoyle extends CityCritter {

	{
		spriteClass = GargoyleSprite.class;

		HP = HT = 95;
		defenseSkill = 32;
		EXP = 10;

		minDmg = 20; maxDmg = 28; acc = 60;
		armorMin = 4; armorMax = 12;

		flying = true;
	}

	private static final int DROP = 4;

	//still on its perch until someone comes close
	public boolean perched = true;

	@Override
	protected boolean act() {
		if (perched && state != HUNTING && paralysed <= 0){
			Hero prey = null;
			for (Hero h : Party.living()){
				if (h.invisible > 0 || Dungeon.level.distance(h.pos, pos) > DROP) continue;
				if (prey == null || Dungeon.level.distance(h.pos, pos) < Dungeon.level.distance(prey.pos, pos)) prey = h;
			}
			if (prey == null){
				//nothing to drop on: it stays put, whatever it hears
				spend(TICK);
				return true;
			}
			perched = false;
			if (drop(prey)) return true;
		}
		return super.act();
	}

	//lands next to the prey and bites
	private boolean drop( Hero prey ){
		ArrayList<Integer> cells = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS8){
			int c = prey.pos + n;
			if (c < 0 || c >= Dungeon.level.length() || !Dungeon.level.passable[c] || Dungeon.level.pit[c] || Actor.findChar(c) != null) continue;
			cells.add(c);
		}
		if (cells.isEmpty()) return false;
		int best = cells.get(0);
		for (int c : cells) if (Dungeon.level.distance(c, pos) < Dungeon.level.distance(best, pos)) best = c;
		int from = pos;
		move(best);
		if (sprite != null){
			sprite.turnTo(from, prey.pos);
			sprite.place(best);
			if (sprite instanceof GargoyleSprite) ((GargoyleSprite) sprite).special();
			if (Dungeon.level.heroFOV[best]) sprite.showStatus(CharSprite.WARNING, Messages.get(this, "drops"));
		}
		if (Dungeon.level.heroFOV[best]) Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG, 1f, 1.2f);
		aggro(prey);
		prey.damage(unmarked(prey, damageRoll()), this);
		spend(TICK);
		return true;
	}

	@Override
	public void damage( int dmg, Object src ) {
		perched = false;
		super.damage(dmg, src);
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("perched", perched);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		perched = bundle.getBoolean("perched");
	}

	public static class GargoyleSprite extends Sheet {

		public GargoyleSprite(){
			super();
			layout(CityAssets.GARGOYLE);
		}
	}
}
