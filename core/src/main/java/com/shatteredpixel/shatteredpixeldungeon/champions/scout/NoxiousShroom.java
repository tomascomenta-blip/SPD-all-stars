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

package com.shatteredpixel.shatteredpixeldungeon.champions.scout;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.PoisonParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//scout's shroom. enemies only, friends can step on it. numbers copied from the scout at plant time
public class NoxiousShroom extends Plant {

	{
		image = 15;
	}

	public int power = 1;       //lvl at plant time
	public int radius = 1;
	public float dmgMulti = 1f;
	public int rootTurns = 0;
	public int bounceChance = 0; //percent

	public static NoxiousShroom plantFor( Hero hero, int cell ){
		NoxiousShroom s = new NoxiousShroom();
		s.pos = cell;
		s.power = hero.lvl;
		s.radius = hero.subClass == HeroSubClass.TRAPPER ? 2 : 1;
		s.dmgMulti = 1f + 0.15f*hero.pointsInTalent(Talent.SCO_BIG_SHROOM);
		s.rootTurns = hero.pointsInTalent(Talent.TR_PARALYTIC);
		s.bounceChance = 25*hero.pointsInTalent(Talent.TR_BOUNCE);
		place(s);
		return s;
	}

	private static void place( NoxiousShroom s ){
		Plant old = Dungeon.level.plants.get(s.pos);
		if (old != null) Dungeon.level.uproot(s.pos);
		Dungeon.level.plants.put(s.pos, s);
		GameScene.plantSeed(s.pos);
	}

	public static boolean canPlantAt( int cell ){
		if (cell < 0 || cell >= Dungeon.level.length()) return false;
		if (Dungeon.level.solid[cell] || Dungeon.level.pit[cell] || !Dungeon.level.passable[cell]) return false;
		if (Dungeon.level.water[cell]) return false;
		if (Actor.findChar(cell) != null) return false;
		if (Dungeon.level.traps.get(cell) != null) return false;
		return !(Dungeon.level.plants.get(cell) instanceof NoxiousShroom);
	}

	public int damageRoll(){
		return Math.round(Random.NormalIntRange(2 + power/3, 4 + power) * dmgMulti);
	}

	public float poisonTurns(){
		return (4 + power/2f) * dmgMulti;
	}

	@Override
	public void trigger() {
		Char ch = Actor.findChar(pos);
		//not an enemy, ignore
		if (ch == null || ch.alignment != Char.Alignment.ENEMY) return;
		Dungeon.level.uproot(pos);
		activate(ch);
	}

	@Override
	public void activate( Char ch ) {
		if (Dungeon.level.heroFOV[pos]){
			Sample.INSTANCE.play(Assets.Sounds.GAS, 1f, 1.3f);
			CellEmitter.center(pos).burst(Speck.factory(Speck.TOXIC), 10);
		}
		for (int i = 0; i < Dungeon.level.length(); i++){
			if (Dungeon.level.distance(pos, i) > radius) continue;
			if (Dungeon.level.solid[i]) continue;
			if (Dungeon.level.heroFOV[i]){
				CellEmitter.get(i).burst(PoisonParticle.SPLASH, 4);
			}
			Char c = Actor.findChar(i);
			if (c == null || c.alignment != Char.Alignment.ENEMY || !c.isAlive()) continue;
			c.damage(damageRoll(), this);
			if (c.isAlive()){
				Buff.affect(c, Poison.class).set(poisonTurns());
				Buff.prolong(c, Cripple.class, 3f);
				if (rootTurns > 0) Buff.prolong(c, Roots.class, rootTurns);
			}
		}

		//bouncing shrooms talent, boing
		if (bounceChance > 0 && Random.Int(100) < bounceChance){
			ArrayList<Integer> cells = new ArrayList<>();
			for (int i = 0; i < Dungeon.level.length(); i++){
				int d = Dungeon.level.distance(pos, i);
				if (d >= 1 && d <= 2 && canPlantAt(i) && Dungeon.level.heroFOV[i]) cells.add(i);
			}
			if (!cells.isEmpty()){
				NoxiousShroom s = new NoxiousShroom();
				s.pos = cells.get(Random.Int(cells.size()));
				s.power = power;
				s.radius = radius;
				s.dmgMulti = dmgMulti;
				s.rootTurns = rootTurns;
				s.bounceChance = bounceChance / 2;
				place(s);
			}
		}
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc");
	}

	private static final String POWER = "power";
	private static final String RADIUS = "radius";
	private static final String DMG = "dmg";
	private static final String ROOT = "root";
	private static final String BOUNCE = "bounce";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(POWER, power);
		bundle.put(RADIUS, radius);
		bundle.put(DMG, dmgMulti);
		bundle.put(ROOT, rootTurns);
		bundle.put(BOUNCE, bounceChance);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		power = bundle.getInt(POWER);
		radius = bundle.getInt(RADIUS);
		dmgMulti = bundle.getFloat(DMG);
		rootTurns = bundle.getInt(ROOT);
		bounceChance = bundle.getInt(BOUNCE);
	}
}
