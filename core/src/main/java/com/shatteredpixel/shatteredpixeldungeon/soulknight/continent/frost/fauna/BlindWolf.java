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
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.AshCritter;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.CritterSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.AshFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

//blind wolf. lean, dark frosted fur, a ridge of spine and milky eyes that see nothing: a torch is
//no use against them, and no use to them. they hunt in packs by ear alone. whatever one of them
//hears, the whole pack goes to; stand still and quiet and they may walk right past you
public class BlindWolf extends AshCritter {

	{
		spriteClass = WolfSprite.class;

		HP = HT = 95;
		defenseSkill = 30;
		EXP = 8;
		baseSpeed = 1.2f;

		minDmg = 20; maxDmg = 28; acc = 58;
		armorMin = 0; armorMax = 6;
	}

	//the first one of a pack brings the rest on its first turn
	public int pack = 0;

	@Override
	protected int ears() {
		return 7;
	}

	//it only "sees" what it bumps into
	@Override
	protected Char chooseEnemy() {
		boolean[] fov = fieldOfView;
		if (fov != null){
			Level l = Dungeon.level;
			for (int i = 0; i < fov.length; i++){
				if (fov[i] && l.distance(i, pos) > 1) fov[i] = false;
			}
		}
		return super.chooseEnemy();
	}

	@Override
	protected boolean act() {
		if (pack > 0) whelp();
		return super.act();
	}

	private void whelp(){
		int n = pack;
		pack = 0;
		ArrayList<Integer> cells = AshFauna.openAround(pos, 2);
		for (int i = 0; i < n && !cells.isEmpty(); i++){
			BlindWolf j = new BlindWolf();
			j.pos = cells.remove(Random.Int(cells.size()));
			j.leader = id();
			j.state = j.WANDERING;
			if (EXP == 0) j.summoned();
			GameScene.add(j);
		}
	}

	//whatever one hears, the pack goes to
	@Override
	protected void heard( Senses.Noise n ) {
		super.heard(n);
		if (sprite instanceof WolfSprite) ((WolfSprite) sprite).pose(4, 8, 8, 0);
		for (Mob m : AshFauna.sorted(Dungeon.level.mobs)){
			if (m instanceof BlindWolf && m != this && m.state != m.HUNTING && Dungeon.level.distance(m.pos, pos) <= 8){
				m.beckon(n.cell);
			}
		}
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("pack", pack);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		pack = bundle.getInt("pack");
	}

	//0-1 idle, 2-5 run, 6-7 the bite, 8 listening, head to one side
	public static class WolfSprite extends CritterSprite {

		public WolfSprite(){
			super();
			critter(FrostAssets.WOLF);
		}
	}
}
