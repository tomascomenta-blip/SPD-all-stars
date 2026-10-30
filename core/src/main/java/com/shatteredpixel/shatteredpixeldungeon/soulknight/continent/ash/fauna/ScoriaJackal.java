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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.AshFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//scoria jackal. they run in packs of four to six: one howls and the rest come, they spread around
//whoever they're after, and the ones behind shove them, preferably into the lava. they smell blood
public class ScoriaJackal extends AshCritter {

	{
		spriteClass = JackalSprite.class;

		HP = HT = 80;
		defenseSkill = 34;
		EXP = 7;

		minDmg = 18; maxDmg = 26; acc = 56;
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
		//blood carries far
		if (state != HUNTING){
			Hero prey = Senses.scent(this, 16);
			if (prey != null){
				aggro(prey);
				target = prey.pos;
			}
		} else if (enemy instanceof Hero && Senses.bleeding(enemy) && Senses.dist(this, enemy.pos) <= 16){
			target = enemy.pos;
		}
		return super.act();
	}

	private void whelp(){
		int n = pack;
		pack = 0;
		ArrayList<Integer> cells = AshFauna.openAround(pos, 2);
		for (int i = 0; i < n && !cells.isEmpty(); i++){
			ScoriaJackal j = new ScoriaJackal();
			j.pos = cells.remove(Random.Int(cells.size()));
			j.leader = id();
			j.state = state == HUNTING ? j.HUNTING : j.WANDERING;
			if (EXP == 0) j.summoned();
			GameScene.add(j);
		}
	}

	//the first one to spot someone howls and the whole pack comes
	@Override
	public void notice() {
		super.notice();
		if (enemy == null || Actor.now() - howled < 30) return;
		howled = Actor.now();
		Senses.noise(pos, Senses.FIGHT);
		if (sprite instanceof JackalSprite) ((JackalSprite) sprite).howl();
		if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 0.5f, 1.6f);
		for (Mob m : AshFauna.sorted(Dungeon.level.mobs)){
			if (m instanceof ScoriaJackal && m != this && m.state != m.HUNTING
					&& Dungeon.level.distance(m.pos, pos) <= 12){
				((ScoriaJackal) m).howled = howled;
				m.beckon(enemy != null ? enemy.pos : pos);
			}
		}
	}

	//they spread out around the prey, each on a side, and best of all with lava behind it
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
				if (Actor.findChar(c + m) instanceof ScoriaJackal && c + m != pos) score += 3;
			}
			int behind = enemy.pos - n;
			if (behind >= 0 && behind < l.length() && AshFauna.lava(behind)) score -= 6;
			if (score < bestScore){
				bestScore = score;
				best = c;
			}
		}
		return super.getCloser(best == -1 ? target : best);
	}

	//with a mate also at it, the bite throws the prey a step back
	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc(enemy, damage);
		if (!(enemy instanceof Hero) || !enemy.isAlive() || enemy.rooted) return damage;
		int mates = 0;
		for (int n : PathFinder.NEIGHBOURS8){
			Char c = Actor.findChar(enemy.pos + n);
			if (c instanceof ScoriaJackal && c != this) mates++;
		}
		if (mates > 0 && Random.Int(5) < 2) shove(enemy);
		return damage;
	}

	private void shove( Char ch ){
		Level l = Dungeon.level;
		int to = ch.pos + (ch.pos - pos);
		if (to < 0 || to >= l.length() || !l.passable[to] || l.pit[to] || Actor.findChar(to) != null) return;
		ch.pos = to;
		if (ch.sprite != null){
			ch.sprite.place(to);
			ch.sprite.showStatus(CharSprite.WARNING, Messages.get(this, "shoved"));
		}
		l.occupyCell(ch);
		Dungeon.observe();
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

	public static class JackalSprite extends ContinentMobSprite {

		public JackalSprite(){
			super();
			setup(FaunaAssets.JACKAL, 16, 16);
		}

		public void howl(){
			play(zap);
		}

		@Override
		protected void setup( String tex, int width, int height ) {
			super.setup(tex, width, height);
			zap = new Animation(4, false);
			zap.frames(frames, 12, 12, 12, 0);
		}
	}
}
