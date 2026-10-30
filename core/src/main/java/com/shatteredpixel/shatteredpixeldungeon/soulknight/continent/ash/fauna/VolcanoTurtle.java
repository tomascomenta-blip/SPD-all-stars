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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.survival.ColossusMeat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.survival.Survival;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.PointF;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.LinkedHashSet;

//volcano turtle. ancient, gentle, a mountain that walks: it grazes the ash so slowly you could
//build on its back (someone did, long ago). hurt it and the volcano wakes up: every few turns it
//throws lava bombs around whoever is fighting it, each landing spot marked two turns before.
//it calms down if left alone long enough
public class VolcanoTurtle extends AshColossus {

	{
		size = 10;
		spriteClass = TurtleSprite.class;

		HP = HT = 4200;
		defenseSkill = 0;
		EXP = 160;
		baseSpeed = 0.25f;

		minDmg = 50; maxDmg = 65; acc = 50;
		armorMin = 24; armorMax = 44;

		properties.add(Property.FIERY);
		properties.add(Property.MINIBOSS);
	}

	private static final int RANGE = 14;

	private int anger = 0;

	//calm and fed: two roasts of colossus meat and it's yours
	@Override
	public String tameBlock( Hero h ) {
		if (!peaceful()) return Messages.get(this, "angry");
		if (Survival.count(h, ColossusMeat.Roast.class) < 2) return Messages.get(this, "hungry");
		return null;
	}

	@Override
	public void tame( Hero h ) {
		Survival.take(h, ColossusMeat.Roast.class, 2);
		anger = 0;
		super.tame(h);
	}

	@Override
	public boolean tameable() {
		return true;
	}

	//the shell
	@Override
	public int[][] climbable() {
		return new int[][]{{1, 2, 6}, {2, 1, 8}, {3, 0, 8}, {4, 0, 8}, {5, 0, 8}, {6, 1, 7}, {7, 3, 6}};
	}

	//row 4, columns 1-2 of the shell: the old slabs between the ruins, where a rider can lie down
	public static final int[] LEDGE = {4, 1, 2};

	@Override
	public boolean peaceful() {
		return anger <= 0;
	}

	@Override
	protected int roam() {
		return 10;
	}

	@Override
	protected int specialCooldown() {
		return 4;
	}

	@Override
	protected boolean act() {
		if (anger > 0 && paralysed <= 0 && --anger == 0){
			state = WANDERING;
			enemy = null;
			enemySeen = false;
			if (seen()) GLog.i(Messages.get(this, "calm"));
		}
		return super.act();
	}

	@Override
	public void damage( int dmg, Object src ) {
		boolean was = anger > 0;
		super.damage(dmg, src);
		if (!isAlive() || dmg <= 0) return;
		anger = 30;
		Char foe = src instanceof Char && SKCombat.foes(this, (Char) src) ? (Char) src : Party.nearestLiving(pos);
		if (foe != null) aggro(foe);
		if (!was){
			cooldown = 1;
			if (seen()) GLog.w(Messages.get(this, "wakes"));
		}
	}

	//lava bombs: on everyone near enough, and a few more around itself
	@Override
	protected boolean special( Char enemy ) {
		LinkedHashSet<Integer> cells = new LinkedHashSet<>();
		for (Hero h : Party.living()){
			if (reach(h.pos) > RANGE) continue;
			bomb(cells, h.pos);
			for (int i = 0; i < 2; i++) bomb(cells, around(h.pos, 3));
		}
		if (cells.isEmpty()) return false;
		for (int i = 0; i < 4; i++) bomb(cells, around(pos, half() + 4));
		warn(0, cells, enemy.pos, 2f);
		Senses.noise(pos, Senses.ROAR);
		return true;
	}

	private int around( int cell, int r ){
		int w = Dungeon.level.width();
		int x = cell % w + Random.IntRange(-r, r), y = cell / w + Random.IntRange(-r, r);
		x = Math.max(1, Math.min(w - 2, x));
		y = Math.max(1, Math.min(Dungeon.level.height() - 2, y));
		return x + y * w;
	}

	private void bomb( LinkedHashSet<Integer> cells, int at ){
		if (Dungeon.level.solid[at] || covers(at)) return;
		cells.addAll(square(at, 1));
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		strikeAnim(kind);
		Senses.noise(pos, Senses.CRASH);
		if (seen()) Sample.INSTANCE.play(Assets.Sounds.BLAST, 1f, 0.6f);
		for (int c : cells){
			if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(FlameParticle.FACTORY, 4);
		}
		for (Char ch : SKCombat.foesIn(this, cells)){
			blast(ch, 90, 130);
			if (ch.isAlive() && !Char.hasProp(ch, Property.FIERY)) Buff.affect(ch, Burning.class).reignite(ch, 4f);
		}
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("anger", anger);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		anger = bundle.getInt("anger");
	}

	//6 head in, 7 the eruption, 8 settling
	public static class TurtleSprite extends AshColossusSprite {

		@Override
		protected int[] downFrames() {
			return new int[]{13, 13, 14, 13};
		}

		@Override
		protected PointF weakAnchor() {
			return new PointF(86, 94);
		}

		@Override
		protected int[] tamedIdle() {
			return new int[]{17, 18};
		}

		public TurtleSprite(){
			super();
			setup(FaunaAssets.TURTLE, 10, 192, 256, 16, 88);
			run = loop(4, 2, 3, 4, 5);
		}

		@Override
		public void strike( int kind ) {
			once(clip(6, 7, 7, 8, 8));
		}
	}
}
