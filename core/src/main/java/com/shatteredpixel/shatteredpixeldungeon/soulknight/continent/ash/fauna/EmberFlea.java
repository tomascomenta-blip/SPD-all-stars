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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentMobSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

//ember flea. lives on the backs of the fire beasts (bison, giant, turtle, the pyre) and jumps on
//whoever comes close. tiny, quick to die, hard to hit, and every bite can set you alight
public class EmberFlea extends AshCritter {

	{
		spriteClass = FleaSprite.class;

		HP = HT = 26;
		defenseSkill = 44;
		EXP = 2;

		minDmg = 8; maxDmg = 12; acc = 62;
		armorMin = 0; armorMax = 2;

		properties.add(Property.FIERY);
	}

	private static final int JUMP = 4;

	@Override
	protected int specialCooldown() {
		return 4;
	}

	//a jump next to the prey, from a few cells away
	@Override
	protected boolean special( Char enemy ) {
		int d = Dungeon.level.distance(pos, enemy.pos);
		if (d < 2 || d > JUMP || rooted) return false;
		if (new Ballistica(pos, enemy.pos, Ballistica.PROJECTILE).collisionPos != enemy.pos) return false;
		int land = landing(enemy.pos, pos);
		if (land == -1) return false;
		hop(land);
		spend(TICK);
		return true;
	}

	private void hop( int to ){
		int from = pos;
		move(to, false);
		if (sprite != null){
			if (sprite instanceof FleaSprite) ((FleaSprite) sprite).leap();
			sprite.jump(from, to, 6, 0.2f, null);
		}
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc(enemy, damage);
		if (enemy.isAlive() && !Char.hasProp(enemy, Property.FIERY) && Random.Int(2) == 0){
			Buff.affect(enemy, Burning.class).reignite(enemy, 3f);
		}
		return damage;
	}

	//free cell next to 'at', the one closest to 'from'
	private static int landing( int at, int from ){
		Level l = Dungeon.level;
		int best = -1;
		for (int n : PathFinder.NEIGHBOURS8){
			int c = at + n;
			if (c < 0 || c >= l.length() || !l.passable[c] || l.pit[c] || Actor.findChar(c) != null) continue;
			if (best == -1 || l.distance(c, from) < l.distance(best, from)) best = c;
		}
		return best;
	}

	//off a host's back and onto someone. false if there's nowhere to land
	public static boolean leap( AshColossus host, Char prey ){
		int from = host.approachCell(prey.pos);
		int land = landing(prey.pos, from);
		if (land == -1) return false;
		EmberFlea f = new EmberFlea();
		f.pos = land;
		f.state = f.HUNTING;
		f.cooldown = 2;
		GameScene.add(f);
		f.aggro(prey);
		if (f.sprite != null){
			f.sprite.place(from);
			f.sprite.jump(from, land, 8, 0.25f, null);
			if (f.sprite instanceof FleaSprite) ((FleaSprite) f.sprite).leap();
		}
		if (!Char.hasProp(prey, Property.FIERY)) Buff.affect(prey, Burning.class).reignite(prey, 3f);
		if (Dungeon.level.heroFOV[land]){
			CellEmitter.get(land).burst(FlameParticle.FACTORY, 4);
			Sample.INSTANCE.play(Assets.Sounds.BURNING, 0.5f, 1.6f);
		}
		return true;
	}

	//12-13 jump, 14 setting fire
	public static class FleaSprite extends ContinentMobSprite {

		public FleaSprite(){
			super();
			setup(FaunaAssets.FLEA, 10, 10);
		}

		@Override
		protected void setup( String tex, int width, int height ) {
			super.setup(tex, width, height);
			attack = new Animation(14, false);
			attack.frames(frames, 6, 7, 14, 8);
			zap = new Animation(12, false);
			zap.frames(frames, 12, 13, 13, 0);
		}

		public void leap(){
			play(zap);
		}
	}
}
