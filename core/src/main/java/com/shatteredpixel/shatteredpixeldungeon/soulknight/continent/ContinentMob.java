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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.awake.Awakening;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.OpenWorld;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Collections;

//shared numbers for the monsters over there: they're meant for heroes past level 30,
//so xp keeps coming until 40. yogs corrupt with every hit, normal or special
public abstract class ContinentMob extends SKMob {

	{
		maxLvl = 40;
		lootChance = 0;
	}

	protected int minDmg = 20, maxDmg = 30, acc = 50;
	protected int armorMin = 0, armorMax = 10;

	//on the open floors only what's near the party moves and thinks; the rest waits, frozen
	public static final int AWAKE_RANGE = 40;

	@Override
	protected boolean act() {
		//the continent woke up: it comes for the party, whatever it is
		int prey = Awakening.drive(this);
		if (prey != -1) target = prey;
		if (Dungeon.level instanceof OpenWorld && state != HUNTING && farFromParty()){
			spend(TICK * 3);
			return true;
		}
		int lure = Senses.stir(this, target);
		if (lure != -1) beckon(lure);
		return super.act();
	}

	protected boolean farFromParty(){
		for (Hero h : Party.active() ? Party.living() : Collections.singletonList(Dungeon.hero)){
			if (h != null && h.isAlive() && Dungeon.level.distance(h.pos, pos) <= AWAKE_RANGE) return false;
		}
		return true;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(minDmg, maxDmg);
	}

	@Override
	public int attackSkill( Char target ) {
		return acc;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(armorMin, armorMax);
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		//the dark bites through armor: a third of a hit always gets in
		damage = Math.max(damage, Math.round(damageRoll() * 0.35f));
		if (this instanceof Yog) Continent.taint(enemy, ((Yog) this).taintOnHit());
		Senses.noise(enemy.pos, Senses.FIGHT);
		return super.attackProc(enemy, damage);
	}

	//a fight is heard from afar
	@Override
	public void damage( int dmg, Object src ) {
		super.damage(dmg, src);
		if (dmg > 0) Senses.noise(pos, Senses.FIGHT);
	}

	//the flying ones still keep off chasms: hovering over one they'd be out of reach of any sword
	@Override
	public boolean[] modifyPassable( boolean[] passable ) {
		if (flying){
			boolean[] pit = Dungeon.level.pit;
			for (int i = 0; i < passable.length; i++){
				if (pit[i]) passable[i] = false;
			}
		}
		return passable;
	}

	//a telegraphed hit: half armor, and the corruption on top if it's a yog
	protected void blast( Char ch, int min, int max ){
		ch.damage(SKCombat.specialDamage(this, ch, min, max), this);
		if (this instanceof Yog && ch.isAlive()) Continent.taint(ch, ((Yog) this).taintOnHit());
	}

	//free floor cells around a spot, in a fixed order
	public static ArrayList<Integer> freeAround( int cell ){
		ArrayList<Integer> cells = new ArrayList<>();
		Level l = Dungeon.level;
		for (int n : PathFinder.NEIGHBOURS8){
			int c = cell + n;
			if (c < 0 || c >= l.length()) continue;
			if (l.passable[c] && !l.pit[c] && Actor.findChar(c) == null) cells.add(c);
		}
		return cells;
	}
}
