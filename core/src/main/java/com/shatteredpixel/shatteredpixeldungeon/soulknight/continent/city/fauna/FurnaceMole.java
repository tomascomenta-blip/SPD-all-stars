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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

//furnace mole. it digs under the streets with a stove burning in its back and comes up through
//the drains right under whoever walks above: the cobbles rise first (marked), then it bursts out.
//up on the street it throws cobbles; left alone a while, it digs back down
public class FurnaceMole extends CityColossus {

	{
		size = 3;
		spriteClass = MoleSprite.class;

		HP = HT = 420;
		defenseSkill = 14;
		EXP = 28;

		minDmg = 34; maxDmg = 46; acc = 58;
		armorMin = 12; armorMax = 24;

		properties.add(Property.FIERY);
	}

	private static final int EMERGE = 0, COBBLES = 1;
	private static final int FEEL = 8, UP = 14;

	private boolean under = true, added = false;
	private int up = 0;

	public boolean submerged(){
		return under;
	}

	@Override
	public boolean solid() {
		return !under;
	}

	@Override
	protected void onAdd() {
		super.onAdd();
		if (!added){
			added = true;
			if (under) invisible++;
		}
	}

	@Override
	public boolean isInvulnerable( Class effect ) {
		return under || super.isInvulnerable(effect);
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		return !under && super.canAttack(enemy);
	}

	@Override
	protected int specialCooldown() {
		return 3;
	}

	@Override
	protected boolean act() {
		if (under && pendingKind == -1 && paralysed <= 0 && !farFromParty()){
			Hero prey = null;
			for (Hero h : Party.living()){
				if (h.invisible > 0 || Dungeon.level.distance(h.pos, pos) > FEEL) continue;
				if (prey == null || Dungeon.level.distance(h.pos, pos) < Dungeon.level.distance(prey.pos, pos)) prey = h;
			}
			if (prey == null){
				spend(TICK);
				return true;
			}
			int spot = fitsAt(prey.pos) ? prey.pos : nearestFit(fits(), prey.pos);
			if (spot == -1){
				spend(TICK);
				return true;
			}
			move(spot);
			if (sprite != null) sprite.place(spot);
			aggro(prey);
			state = HUNTING;
			warn(EMERGE, SKCombat.area(spot, 1), prey.pos, 2f);
			Senses.noise(spot, Senses.STEP);
			if (prey == Dungeon.local()) GLog.w(Messages.get(this, "rumble"));
			return true;
		}
		if (!under && pendingKind == -1 && --up <= 0 && (enemy == null || reach(enemy.pos) > 1)){
			sink();
			spend(TICK);
			return true;
		}
		return super.act();
	}

	@Override
	protected boolean special( Char enemy ) {
		if (under) return false;
		int d = reach(enemy.pos);
		if (d < 2 || d > 6) return false;
		warn(COBBLES, square(enemy.pos, 1), enemy.pos, 2f);
		return true;
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		if (kind == EMERGE){
			under = false;
			invisible = Math.max(0, invisible - 1);
			up = UP;
			if (sprite instanceof MoleSprite) ((MoleSprite) sprite).emerge();
			Senses.noise(pos, Senses.CRASH);
		} else {
			strikeAnim(kind);
			Senses.noise(aim, Senses.FIGHT);
		}
		if (seen()){
			Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 0.8f);
			for (int c : cells) if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.ROCK), 2);
		}
		for (Char ch : SKCombat.foesIn(this, cells)){
			blast(ch, kind == EMERGE ? 60 : 50, kind == EMERGE ? 90 : 70);
		}
		//whatever is still on top gets thrown off
		if (kind == EMERGE) trample(pos);
	}

	private void sink(){
		under = true;
		invisible++;
		enemy = null;
		state = WANDERING;
		if (sprite instanceof MoleSprite) ((MoleSprite) sprite).dig();
		if (seen()) Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.7f, 0.6f);
	}

	@Override
	protected boolean getCloser( int target ) {
		return !under && super.getCloser(target);
	}

	@Override
	protected void onTrample( Char ch ) {
		if (!under) super.onTrample(ch);
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("under", under);
		bundle.put("added", added);
		bundle.put("up", up);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		under = bundle.getBoolean("under");
		added = bundle.getBoolean("added");
		up = bundle.getInt("up");
		if (under) invisible++;
	}

	//8 coming up through a drain, 9 digging (throwing cobbles)
	public static class MoleSprite extends Sheet {

		private final Animation standing;

		public MoleSprite(){
			super();
			layout(CityAssets.MOLE, 3, 64, 64, 8, 16, -1, -1);
			standing = idle;
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (ch instanceof FurnaceMole && ((FurnaceMole) ch).submerged()) dig();
		}

		public void emerge(){
			idle = standing;
			once(clip(6, 8, 8, 7, 0));
		}

		public void dig(){
			idle = loop(1, 8);
			play(idle);
		}

		@Override
		public void windup( int kind ) {
			play(loop(2, kind == EMERGE ? 8 : 9));
		}

		@Override
		public void strike( int kind ) {
			once(clip(8, 9, 7, 7, 0));
		}
	}
}
