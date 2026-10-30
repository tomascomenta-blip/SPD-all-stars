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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.flesh.fauna;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.AshFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

//the herd that watches: pale fawns with half a dozen eyes down the neck and the flank, half shut
//while they graze. when one sees you the whole herd turns at once to stare, every eye open, and
//the big things of the forest hear it. then they run. cornered, they kick
public class WatcherFawn extends FleshCritter {

	{
		spriteClass = FawnSprite.class;

		HP = HT = 50;
		defenseSkill = 30;
		EXP = 5;
		baseSpeed = 1.2f;

		minDmg = 10; maxDmg = 16; acc = 50;
		armorMin = 0; armorMax = 4;
	}

	//the first one of a herd brings the rest on its first turn
	public int herd = 0;
	private float stared = -99;

	@Override
	protected int eyes() {
		return 3;
	}

	@Override
	protected int ears() {
		return 3;
	}

	@Override
	protected boolean act() {
		if (herd > 0) bringHerd();
		if (state == FLEEING && enemy != null && enemy.isAlive() && fieldOfView != null
				&& fieldOfView[enemy.pos] && Actor.now() - stared >= 8){
			stare();
		}
		return super.act();
	}

	private void bringHerd(){
		int n = herd;
		herd = 0;
		ArrayList<Integer> cells = AshFauna.openAround(pos, 2);
		for (int i = 0; i < n && !cells.isEmpty(); i++){
			WatcherFawn f = new WatcherFawn();
			f.pos = cells.remove(Random.Int(cells.size()));
			f.leader = id();
			f.state = f.WANDERING;
			if (EXP == 0) f.summoned();
			GameScene.add(f);
		}
	}

	@Override
	public void notice() {
		super.notice();
		if (enemy != null) stare();
	}

	//the whole herd at once: eyes on the prey, a noise every beast around hears, and away
	private void stare(){
		if (enemy == null) return;
		int at = enemy.pos;
		boolean any = false;
		for (Mob m : AshFauna.sorted(Dungeon.level.mobs)){
			if (!(m instanceof WatcherFawn) || Dungeon.level.distance(m.pos, pos) > 10) continue;
			WatcherFawn f = (WatcherFawn) m;
			if (Actor.now() - f.stared < 4) continue;
			f.stared = Actor.now();
			if (f.enemy == null) f.aggro(enemy);
			f.state = f.FLEEING;
			if (f.sprite != null){
				f.sprite.turnTo(f.pos, at);
				if (f.sprite instanceof FawnSprite) ((FawnSprite) f.sprite).special();
			}
			any = true;
		}
		if (!any) return;
		Senses.noise(pos, Senses.ROAR);
		if (Dungeon.level.heroFOV[pos]){
			Sample.INSTANCE.play(Assets.Sounds.ALERT, 0.6f, 1.4f);
			GLog.w(Messages.get(this, "stare"));
		}
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("herd", herd);
		bundle.put("stared", stared);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		herd = bundle.getInt("herd");
		stared = bundle.contains("stared") ? bundle.getFloat("stared") : -99;
	}

	//8 every eye open, looking at you
	public static class FawnSprite extends Sheet {

		public FawnSprite(){
			super();
			layout(FleshAssets.FAWN);
		}
	}
}
