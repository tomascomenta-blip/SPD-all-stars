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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.starship;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//the queen's hive on the bridge
//- biomass: slows whoever walks on it, heals the queen while she's on it. spreads while she fights,
//  fire and explosions burn it off
//- acid: she spits it once she's shed her shell, hurts heroes standing in it
//just the state here, VarkolynVisuals draws it
public class Hive extends Blob {

	{
		actPriority = BUFF_PRIO;
	}

	public int[] acid;              //acid turns left per cell
	public int version = 0;         //bump when the picture needs redrawing
	private int growTimer = 5;

	public static Hive get( Level level ){
		return level == null ? null : (Hive) level.blobs.get(Hive.class);
	}

	public static Hive getOrMake( Level level ){
		Hive h = get(level);
		if (h == null){
			h = new Hive();
			h.seed(level, 0, 0);
			level.blobs.put(Hive.class, h);
		}
		return h;
	}

	private void arrays( int length ){
		if (cur == null) cur = new int[length];
		if (off == null) off = new int[length];
		if (acid == null) acid = new int[length];
	}

	public void put( Level level, int cell ){
		arrays(level.length());
		if (cur[cell] == 0){
			cur[cell] = 1;
			volume++;
			version++;
		}
	}

	public void remove( int cell ){
		if (cur == null || cur[cell] == 0) return;
		cur[cell] = 0;
		volume--;
		version++;
	}

	public boolean biomass( int cell ){
		return cur != null && cell >= 0 && cell < cur.length && cur[cell] > 0;
	}

	public int acidAt( int cell ){
		return acid == null || cell < 0 || cell >= acid.length ? 0 : acid[cell];
	}

	public void acid( Level level, int cell, int turns ){
		arrays(level.length());
		if (level.solid[cell]) return;
		acid[cell] = Math.max(acid[cell], turns);
		version++;
	}

	public int count(){
		int n = 0;
		if (cur != null) for (int v : cur) if (v > 0) n++;
		return n;
	}

	//fire + explosions burn the biomass off
	public static void burnAround( int cell, int radius ){
		Hive h = get(Dungeon.level);
		if (h == null || h.cur == null) return;
		boolean any = false;
		int w = Dungeon.level.width();
		for (int dy = -radius; dy <= radius; dy++){
			for (int dx = -radius; dx <= radius; dx++){
				int c = cell + dx + dy * w;
				if (c < 0 || c >= h.cur.length || !h.biomass(c)) continue;
				h.remove(c);
				any = true;
				if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(FlameParticle.FACTORY, 3);
			}
		}
		if (any) h.version++;
	}

	//queen's dead, everything withers
	public void wither(){
		if (cur != null) for (int i = 0; i < cur.length; i++) cur[i] = 0;
		if (acid != null) for (int i = 0; i < acid.length; i++) acid[i] = 0;
		volume = 0;
		version++;
	}

	@Override
	public boolean act() {
		spend(TICK);
		Level L = Dungeon.level;
		if (L == null || L.blobs.get(Hive.class) != this) return true;
		arrays(L.length());
		VarkolynLeader q = StarshipBossLevel.queen(L);
		boolean fight = q != null && q.isAlive() && q.fighting() && L.locked;

		//fire burns it
		Blob fire = L.blobs.get(Fire.class);
		if (fire != null && fire.volume > 0 && fire.cur != null){
			for (int c = 0; c < cur.length; c++){
				if (cur[c] > 0 && fire.cur[c] > 0) burnAround(c, 0);
			}
		}

		//spreads while she fights (not while she hides)
		if (fight && !q.fleeing() && --growTimer <= 0){
			growTimer = 6;
			grow(L, 2);
		}

		//heroes on it get slowed, it's sticky
		for (Hero h : Party.living()){
			if (biomass(h.pos) && !h.flying) Buff.prolong(h, Cripple.class, 1f);
		}

		boolean anyAcid = false;
		for (int c = 0; c < acid.length; c++){
			if (acid[c] <= 0) continue;
			anyAcid = true;
			if (--acid[c] == 0) version++;
			Char ch = Actor.findChar(c);
			if (ch != null && ch.alignment == Char.Alignment.ALLY && !ch.flying && ch.isAlive()){
				ch.damage(Random.NormalIntRange(2, 4), this);
			}
		}
		if (anyAcid && Random.Int(4) == 0) version++;
		return true;
	}

	//new biomass next to the old, only inside the bridge, max 1/4 of it
	private void grow( Level L, int n ){
		if (!(L instanceof StarshipBossLevel)) return;
		StarshipBossLevel B = (StarshipBossLevel) L;
		int inside = Math.max(1, (B.brR - B.brL - 1) * (B.brB - B.brT - 1));
		if (count() * 4 >= inside) return;
		ArrayList<Integer> edge = new ArrayList<>();
		for (int c = 0; c < cur.length; c++){
			if (cur[c] == 0) continue;
			for (int k : PathFinder.NEIGHBOURS4){
				int t = c + k;
				if (t < 0 || t >= cur.length || cur[t] > 0 || !B.inBridge(t) || L.solid[t] || !L.passable[t]) continue;
				if (!edge.contains(t)) edge.add(t);
			}
		}
		Random.shuffle(edge);
		for (int i = 0; i < n && i < edge.size(); i++) put(L, edge.get(i));
	}

	@Override
	public void use( BlobEmitter emitter ) {
		//VarkolynVisuals draws it, nothing to do here
	}

	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		if (acid != null) bundle.put("acid", acid);
		bundle.put("grow_timer", growTimer);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		if (bundle.contains("acid")) acid = bundle.getIntArray("acid");
		growTimer = bundle.contains("grow_timer") ? bundle.getInt("grow_timer") : 5;
	}
}
