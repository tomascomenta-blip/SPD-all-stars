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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.big.Colossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.wild.WildSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.AshFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PointF;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

//the mourner. a very tall, thin thing wrapped in shrouds, a bone mask under a veil, and tears of
//yellow mist running out of the empty sockets. it weeps for the dead and raises them: whatever small
//thing dies near it gets up again a while later, once. its long bone fingers rake a marked patch
public class Mourner extends WildColossus {

	{
		size = 8;
		spriteClass = MournerSprite.class;

		HP = HT = 2700;
		defenseSkill = 12;
		EXP = 140;
		baseSpeed = 0.8f;

		minDmg = 44; maxDmg = 60; acc = 66;
		armorMin = 10; armorMax = 24;

		properties.add(Property.UNDEAD);
	}

	private static final int RAKE = 0;
	private static final int RANGE = 10, MOST = 4;

	//the small ones around it (to notice when they die) and the dead waiting to get up. kept only
	//while the floor is loaded, the same on every device
	private final ArrayList<Mob> watched = new ArrayList<>();
	private final ArrayList<Mob> dead = new ArrayList<>();
	private int raiseT = 0;

	//the hands, at its sides
	@Override
	public int[][] weakSpots() {
		return new int[][]{{7, 3}, {0, 3}};
	}

	@Override
	public int[][] climbable() {
		return new int[][]{{2, 2, 5}, {3, 2, 5}, {4, 2, 5}, {5, 2, 5}};
	}

	@Override
	protected int ears() {
		return 5;
	}

	@Override
	protected boolean act() {
		if (!farFromParty()) mourn();
		return super.act();
	}

	private void mourn(){
		Level l = Dungeon.level;
		for (Mob m : new ArrayList<>(watched)){
			if (!m.isAlive() || !l.mobs.contains(m)){
				watched.remove(m);
				if (!m.isAlive() && dead.size() < MOST * 2) dead.add(m);
			}
		}
		for (Mob m : AshFauna.sorted(l.mobs)){
			if (m == this || m instanceof Colossus || !(m instanceof SKMob) || m.EXP <= 0) continue;
			if (m.alignment != Char.Alignment.ENEMY || reach(m.pos) > RANGE || watched.contains(m)) continue;
			watched.add(m);
		}
	}

	@Override
	protected boolean special( Char enemy ) {
		if (!dead.isEmpty() && ++raiseT >= 2){
			raiseT = 0;
			if (raise()) return true;
		}
		if (reach(enemy.pos) > 3) return false;
		face(enemy.pos);
		ArrayList<Integer> f = front(3, 1);
		if (!f.contains(enemy.pos)) f = ring(2);
		warn(RAKE, f, enemy.pos, 2f);
		return true;
	}

	//the dead get up where they fell, without anything to give
	private boolean raise(){
		int n = 0;
		Level l = Dungeon.level;
		for (Mob d : new ArrayList<>(dead)){
			if (n >= MOST) break;
			dead.remove(d);
			int cell = d.pos;
			if (cell < 0 || cell >= l.length() || !l.passable[cell] || Actor.findChar(cell) != null){
				ArrayList<Integer> free = freeAround(cell);
				if (free.isEmpty()) continue;
				cell = free.get(0);
			}
			Mob m = Reflection.newInstance(d.getClass());
			if (!(m instanceof SKMob)) continue;
			((SKMob) m).summoned();
			m.pos = cell;
			m.state = m.HUNTING;
			GameScene.add(m);
			if (l.heroFOV[cell]) CellEmitter.get(cell).burst(Speck.factory(Speck.BONE), 4);
			n++;
		}
		if (n == 0) return false;
		CoopLog.line("fauna: Mourner raises " + n + " t=" + (int) Actor.now());
		Senses.noise(pos, Senses.FIGHT);
		if (sprite instanceof MournerSprite) ((MournerSprite) sprite).pose(3, 6, 8, 8, 8, 0);
		if (seen()){
			Sample.INSTANCE.play(Assets.Sounds.GHOST, 1f, 0.7f);
			GLog.w(Messages.get(this, "raises"));
		}
		spend(TICK);
		return true;
	}

	@Override
	protected int[] power( int kind ) {
		return new int[]{95, 125};
	}

	@Override
	protected String blowSound( int kind ) {
		return Assets.Sounds.HIT_SLASH;
	}

	//0-1 weeping, 2-5 dragging along, 6 arms up and a wail, 7 the rake, 8 raising the dead, 9 on its
	//knees, 10 dead
	public static class MournerSprite extends WildSprite {

		public MournerSprite(){
			super();
			beast(BonesAssets.MOURNER, 8, 184, 288, 28, 136);
			die = clip(4, 9, 10, 10);
		}

		@Override
		protected int[] downFrames() {
			return new int[]{9, 9, 9, 0};
		}

		@Override
		protected PointF weakAnchor() {
			return new PointF(98, 54);
		}
	}
}
