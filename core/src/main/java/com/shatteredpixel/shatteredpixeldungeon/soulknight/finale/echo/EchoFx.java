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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.finale.echo;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Flare;
import com.shatteredpixel.shatteredpixeldungeon.effects.TargetedCell;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

//the look of the time echoes: cyan + gold motes, sand, stuff falling from the sky.
//everything here is drawing only. game code calls the static ones, they hop to the render thread
public class EchoFx {

	public static final int CYAN = 0x7FE8FF;
	public static final int GOLD = 0xFFD36A;
	public static final int PALE = 0xE6FFFF;

	public static void later( final Runnable r ){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				r.run();
			}
		});
	}

	public static boolean seen( int cell ){
		return cell >= 0 && cell < Dungeon.level.length() && Dungeon.level.heroFOV[cell];
	}

	// *** particles ***

	//little mote that drifts up and shrinks. 1 in 3 is gold
	public static class Mote extends PixelParticle.Shrinking {
		public void reset( float x, float y ){
			revive();
			this.x = x;
			this.y = y;
			color(Random.Visual.Int(3) == 0 ? GOLD : CYAN);
			size(this.size = Random.Visual.Float(1.5f, 3f));
			left = lifespan = Random.Visual.Float(0.6f, 1.2f);
			speed.set(Random.Visual.Float(-5, 5), -Random.Visual.Float(6, 16));
			acc.set(0, 0);
		}
	}

	public static final Emitter.Factory MOTE = new Emitter.Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			((Mote) emitter.recycle(Mote.class)).reset(x, y);
		}
		@Override
		public boolean lightMode() {
			return true;
		}
	};

	//hourglass sand, falls and fades
	public static class Grain extends PixelParticle {
		public void reset( float x, float y ){
			revive();
			this.x = x;
			this.y = y;
			color(Random.Visual.Int(4) == 0 ? PALE : GOLD);
			size(this.size = Random.Visual.Float(1f, 2f));
			left = lifespan = Random.Visual.Float(0.5f, 1.1f);
			speed.set(Random.Visual.Float(-10, 10), Random.Visual.Float(-12, 4));
			acc.set(0, 60);
		}
		@Override
		public void update() {
			super.update();
			am = left / lifespan;
		}
	}

	public static final Emitter.Factory SAND = new Emitter.Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			((Grain) emitter.recycle(Grain.class)).reset(x, y);
		}
	};

	// *** effects ***

	public static PointF center( int cell ){
		return DungeonTilemap.tileCenterToWorld(cell);
	}

	//a burst of time light on a cell (portal, blink). goes in the layer of whoever caused it
	public static void flare( final Char near, final int cell, final float size ){
		later(new Runnable() { public void run() {
			if (!seen(cell)) return;
			CellEmitter.center(cell).burst(MOTE, 10);
			if (near == null || near.sprite == null || near.sprite.parent == null) return;
			Flare f = new Flare(7, 16 * size);
			f.color(Random.Visual.Int(2) == 0 ? CYAN : GOLD, true);
			f.angularSpeed = 120;
			f.show(near.sprite.parent, center(cell), 1.2f);
		} });
	}

	//an attack got called off: drops its red marks early (only the ones still set for that moment)
	public static void unmark( final int[] cells, final float until ){
		later(new Runnable() { public void run() {
			synchronized (TargetedCell.cells){
				for (int c : cells){
					TargetedCell tc = TargetedCell.cells.get(c);
					if (tc != null && Math.abs(tc.time - until) < 0.01f) tc.time = 0;
				}
			}
		} });
	}

	public static void sparks( final int cell, final int n ){
		later(new Runnable() { public void run() {
			if (seen(cell)) CellEmitter.center(cell).burst(MOTE, n);
		} });
	}

	public static void shake( final float power, final float time ){
		later(new Runnable() { public void run() { PixelScene.shake(power, time); } });
	}

	public static void flash( final int color ){
		later(new Runnable() { public void run() { GameScene.flash(color); } });
	}

	//something falls on a cell from above: icicles, meteors, stars. one frame of a strip
	public static void fall( final int cell, final String asset, final int fw, final int fh, final int frame,
							 final float scale, final float height, final float time, final Emitter.Factory land, final int n ){
		later(new Runnable() { public void run() {
			if (!seen(cell)) return;
			GameScene.effect(new Falling(cell, asset, fw, fh, frame, scale, height, time, land, n));
		} });
	}

	public static class Falling extends Image {

		private final int cell;
		private final float toX, toY, height, time;
		private final Emitter.Factory land;
		private final int n;
		private float t = 0;

		public Falling( int cell, String asset, int fw, int fh, int frame, float s, float height, float time, Emitter.Factory land, int n ){
			super(asset);
			frame(frame * fw, 0, fw, fh);
			this.cell = cell;
			this.height = height;
			this.time = time;
			this.land = land;
			this.n = n;
			scale.set(s);
			PointF c = EchoFx.center(cell);
			toX = c.x - fw * s / 2f;
			toY = c.y - fh * s + 4;
			x = toX;
			y = toY - height;
			alpha(0.85f);
		}

		@Override
		public void update() {
			super.update();
			t += Game.elapsed;
			float p = Math.min(1f, t / time);
			y = toY - height * (1 - p * p);
			if (p >= 1f){
				if (land != null) CellEmitter.center(cell).burst(land, n);
				killAndErase();
			}
		}
	}
}
