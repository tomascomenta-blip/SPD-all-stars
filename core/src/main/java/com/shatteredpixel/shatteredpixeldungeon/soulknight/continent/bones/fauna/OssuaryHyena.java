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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.AshCritter;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.CritterSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.AshFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

//ossuary hyena: mangy, a sloped back, a mask of bone over the muzzle and a yellow eye of mist. they go
//in packs and laugh when they find you (the rest come running); they follow blood from far off
public class OssuaryHyena extends AshCritter {

	{
		spriteClass = HyenaSprite.class;

		HP = HT = 85;
		defenseSkill = 32;
		EXP = 7;
		baseSpeed = 1.2f;

		minDmg = 18; maxDmg = 26; acc = 56;
		armorMin = 0; armorMax = 6;
	}

	public int pack = 0;
	private float laughed = -99;

	@Override
	protected int ears() {
		return 3;
	}

	@Override
	protected boolean act() {
		if (pack > 0){
			int n = pack;
			pack = 0;
			ArrayList<Integer> cells = AshFauna.openAround(pos, 2);
			for (int i = 0; i < n && !cells.isEmpty(); i++){
				OssuaryHyena h = new OssuaryHyena();
				h.pos = cells.remove(Random.Int(cells.size()));
				h.leader = id();
				h.state = h.WANDERING;
				if (EXP == 0) h.summoned();
				GameScene.add(h);
			}
		}
		if (state != HUNTING){
			Hero prey = Senses.scent(this, 18);
			if (prey != null){
				aggro(prey);
				target = prey.pos;
			}
		}
		return super.act();
	}

	@Override
	public void notice() {
		super.notice();
		if (enemy == null || Actor.now() - laughed < 30) return;
		laughed = Actor.now();
		Senses.noise(pos, Senses.FIGHT);
		if (sprite instanceof HyenaSprite) ((HyenaSprite) sprite).pose(5, 8, 8, 8, 0);
		if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.MIMIC, 0.5f, 1.8f);
		for (Mob m : AshFauna.sorted(Dungeon.level.mobs)){
			if (m instanceof OssuaryHyena && m != this && m.state != m.HUNTING && Dungeon.level.distance(m.pos, pos) <= 12){
				((OssuaryHyena) m).laughed = laughed;
				m.beckon(enemy.pos);
			}
		}
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("pack", pack);
		bundle.put("laughed", laughed);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		pack = bundle.getInt("pack");
		laughed = bundle.contains("laughed") ? bundle.getFloat("laughed") : -99;
	}

	//0-1 idle, 2-5 run, 6-7 the bite, 8 laughing, head up
	public static class HyenaSprite extends CritterSprite {

		public HyenaSprite(){
			super();
			critter(BonesAssets.HYENA);
		}
	}
}
