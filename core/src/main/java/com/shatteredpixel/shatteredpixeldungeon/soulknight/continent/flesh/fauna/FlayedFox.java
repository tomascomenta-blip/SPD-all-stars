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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.AshFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//flayed fox. a fox with no skin, all red muscle and white tendons. they hunt in packs: the first to
//see you howls and the rest come, they close in from every side and their bites open wounds. blood
//they follow from far away
public class FlayedFox extends FleshCritter {

	{
		spriteClass = FoxSprite.class;

		HP = HT = 75;
		defenseSkill = 34;
		EXP = 7;

		minDmg = 16; maxDmg = 24; acc = 56;
		armorMin = 0; armorMax = 6;
	}

	//the first one of a pack brings the rest on its first turn
	public int pack = 0;
	private float howled = -99;

	@Override
	protected int ears() {
		return 3;
	}

	@Override
	protected boolean act() {
		if (pack > 0) whelp();
		if (state != HUNTING){
			Hero prey = Senses.scent(this, 18);
			if (prey != null){
				aggro(prey);
				target = prey.pos;
			}
		}
		return super.act();
	}

	private void whelp(){
		int n = pack;
		pack = 0;
		ArrayList<Integer> cells = AshFauna.openAround(pos, 2);
		for (int i = 0; i < n && !cells.isEmpty(); i++){
			FlayedFox f = new FlayedFox();
			f.pos = cells.remove(Random.Int(cells.size()));
			f.leader = id();
			f.state = state == HUNTING ? f.HUNTING : f.WANDERING;
			if (EXP == 0) f.summoned();
			GameScene.add(f);
		}
	}

	@Override
	public void notice() {
		super.notice();
		if (enemy == null || Actor.now() - howled < 30) return;
		howled = Actor.now();
		Senses.noise(pos, Senses.FIGHT);
		if (sprite instanceof FoxSprite) ((FoxSprite) sprite).special();
		if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 0.5f, 1.8f);
		for (Mob m : AshFauna.sorted(Dungeon.level.mobs)){
			if (m instanceof FlayedFox && m != this && m.state != m.HUNTING && Dungeon.level.distance(m.pos, pos) <= 12){
				((FlayedFox) m).howled = howled;
				m.beckon(enemy.pos);
			}
		}
	}

	//they spread around the prey, each on its own side
	@Override
	protected boolean getCloser( int target ) {
		if (state != HUNTING || enemy == null || Dungeon.level.distance(pos, enemy.pos) > 4) return super.getCloser(target);
		Level l = Dungeon.level;
		int best = -1, bestScore = Integer.MAX_VALUE;
		for (int n : PathFinder.NEIGHBOURS8){
			int c = enemy.pos + n;
			if (c < 0 || c >= l.length() || !l.passable[c] || l.avoid[c] || Actor.findChar(c) != null) continue;
			int score = l.distance(c, pos) * 2;
			for (int m : PathFinder.NEIGHBOURS8){
				if (Actor.findChar(c + m) instanceof FlayedFox && c + m != pos) score += 3;
			}
			if (score < bestScore){
				bestScore = score;
				best = c;
			}
		}
		return super.getCloser(best == -1 ? target : best);
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc(enemy, damage);
		if (enemy.isAlive() && Random.Int(3) == 0) Buff.affect(enemy, Bleeding.class).set(6);
		return damage;
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("pack", pack);
		bundle.put("howled", howled);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		pack = bundle.getInt("pack");
		howled = bundle.getFloat("howled");
	}

	//8 howling
	public static class FoxSprite extends Sheet {

		public FoxSprite(){
			super();
			layout(FleshAssets.FOX);
		}
	}
}
