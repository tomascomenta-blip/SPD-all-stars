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

package com.shatteredpixel.shatteredpixeldungeon.champions.forger;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

//magma pillars. solid for a few turns (terrain -> rock statue, doesn't block sight), terrain comes back after.
//Embestida and the forge eruption smash them. one blob per floor, copy-paste of the gravedigger's GraveWall basically
public class MagmaPillar extends Blob {

	{
		alwaysVisible = true;
	}

	//original terrain, -1 = no pillar
	private int[] orig;

	public static boolean at( int cell ){
		if (Dungeon.level == null) return false;
		MagmaPillar p = (MagmaPillar) Dungeon.level.blobs.get(MagmaPillar.class);
		return p != null && p.volume > 0 && p.cur != null && p.cur[cell] > 0;
	}

	private static boolean canStand( Level l, int cell ){
		switch (l.map[cell]){
			case Terrain.EMPTY: case Terrain.EMPTY_DECO: case Terrain.EMPTY_SP: case Terrain.GRASS:
			case Terrain.HIGH_GRASS: case Terrain.FURROWED_GRASS: case Terrain.EMBERS: case Terrain.WATER:
				return true;
			default:
				return false;
		}
	}

	//true if it went up
	public static boolean raise( int cell, int turns ){
		Level l = Dungeon.level;
		if (!l.insideMap(cell) || l.solid[cell] || l.pit[cell] || Actor.findChar(cell) != null || !canStand(l, cell)) return false;
		GameScene.add(Blob.seed(cell, Math.min(turns, 20), MagmaPillar.class));
		if (l.heroFOV[cell]) CellEmitter.get(cell).burst(FlameParticle.FACTORY, 6);
		return true;
	}

	//smash + rock shower. true if there was one
	public static boolean shatter( int cell ){
		if (!at(cell)) return false;
		MagmaPillar p = (MagmaPillar) Dungeon.level.blobs.get(MagmaPillar.class);
		p.clear(cell);
		if (Dungeon.level.heroFOV[cell]){
			CellEmitter.get(cell).burst(Speck.factory(Speck.ROCK), 8);
			CellEmitter.get(cell).burst(FlameParticle.FACTORY, 6);
			Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.7f, 1.1f);
		}
		return true;
	}

	private void restore( int cell ){
		if (orig == null || orig[cell] < 0) return;
		int t = orig[cell];
		orig[cell] = -1;
		Level.set(cell, t);
		GameScene.updateMap(cell);
	}

	@Override
	protected void evolve() {
		int cell;
		Level l = Dungeon.level;
		for (int i = area.left; i < area.right; i++){
			for (int j = area.top; j < area.bottom; j++){
				cell = i + j*l.width();
				off[cell] = cur[cell] > 0 ? Math.min(cur[cell] - 1, 20) : 0;
				volume += off[cell];
				if (off[cell] == 0 && cur[cell] > 0){
					restore(cell);
					cellsToFlagUpdate.add(cell);
					if (l.heroFOV[cell]) CellEmitter.get(cell).burst(Speck.factory(Speck.ROCK), 4);
				}
			}
		}
	}

	@Override
	public void seed( Level level, int cell, int amount ) {
		if (orig == null){
			orig = new int[level.length()];
			java.util.Arrays.fill(orig, -1);
		}
		if (orig[cell] < 0 && level.map[cell] != Terrain.STATUE){
			orig[cell] = level.map[cell];
			Level.set(cell, Terrain.STATUE);
			GameScene.updateMap(cell);
		}
		super.seed(level, cell, amount);
		level.updateCellFlags(cell);
	}

	@Override
	public void clear( int cell ) {
		if (cur != null && cur[cell] > 0) restore(cell);
		super.clear(cell);
		if (cur == null) return;
		Dungeon.level.updateCellFlags(cell);
	}

	@Override
	public void fullyClear() {
		if (orig != null && cur != null){
			for (int i = 0; i < orig.length; i++) restore(i);
		}
		super.fullyClear();
		Dungeon.level.buildFlagMaps();
	}

	@Override
	public void onBuildFlagMaps( Level l ) {
		if (volume > 0){
			for (int i = 0; i < l.length(); i++){
				onUpdateCellFlags(l, i);
			}
		}
	}

	@Override
	public void onUpdateCellFlags( Level l, int cell ) {
		if (volume > 0 && cur[cell] > 0){
			l.solid[cell] = true;
			l.passable[cell] = false;
			l.avoid[cell] = false;
			//level redoes openSpace after, no lo hagas aquí
		}
	}

	@Override
	public void use( BlobEmitter emitter ) {
		super.use(emitter);
		emitter.pour(FlameParticle.FACTORY, 0.09f);
	}

	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}

	private static final String ORIG = "orig";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		if (orig != null) bundle.put(ORIG, orig);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		if (bundle.contains(ORIG)) orig = bundle.getIntArray(ORIG);
	}
}
