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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.kraken;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Lightning;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.utils.Callback;
import com.watabou.utils.GameMath;
import com.watabou.utils.PathFinder;
import com.watabou.utils.PointF;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

//all the big kraken drawing. Low = under the chars (sea, whirlpool, head, ship, holes, lying tentacles),
//High = on top (mast, rising tentacles, waves, splashes, lightning)
//both just read KrakenLevel every frame so loading a save looks right for free.
//one-off fx (slam, bolt, eye hit) go through the static methods below
public class KrakenVisuals {

	static final int S = DungeonTilemap.SIZE;

	private static Low low;
	private static High high;
	//OWN random. if you use the game's Random here coop desyncs, ask me how i know
	private static final Random rnd = new Random();

	private static void later( final Runnable r ){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				r.run();
			}
		});
	}

	static float cx( int cell ){
		return (cell % KrakenLevel.W) * S + S/2f;
	}

	static float cy( int cell ){
		return (cell / KrakenLevel.W) * S + S/2f;
	}

	static class Strip extends Image {
		final int fw, fh, count;
		Strip( String asset, int fw, int fh ){
			super(asset);
			this.fw = fw;
			this.fh = fh;
			this.count = Math.max(1, (int)(texture.width / fw));
			show(0);
		}
		void show( int f ){
			f = Math.max(0, Math.min(count - 1, f));
			frame(f * fw, 0, fw, fh);
		}
	}

	//plays once, kills itself
	static class OneShot extends Strip {
		float t = 0, fps;
		OneShot( String asset, int fw, int fh, float fps ){
			super(asset, fw, fh);
			this.fps = fps;
		}
		@Override
		public void update() {
			super.update();
			t += Game.elapsed;
			int f = (int)(t * fps);
			if (f >= count){
				killAndErase();
				return;
			}
			show(f);
		}
	}

	//events. called from game logic, drawn on every device

	public static void splash( final int cell ){
		later(new Runnable() { public void run() { if (high != null) high.splashAt(cx(cell), cy(cell), 1f); } });
	}

	public static void shipHit( int amount ){
		if (amount >= 6) later(new Runnable() { public void run() { PixelScene.shake(1.5f, 0.3f); } });
	}

	public static void holeOpened( final int cell ){
		later(new Runnable() { public void run() { if (high != null) high.debris(cell); } });
	}

	public static void holeClosed( final int cell ){
	}

	public static void rise( final int side, final int row, final int type ){
		later(new Runnable() { public void run() { if (high != null) high.riseStart = 0f; } });
	}

	public static void slam( final int side, final int row, final int type ){
		later(new Runnable() { public void run() {
			PixelScene.shake(type == 4 ? 6 : 4, 0.6f);
			if (high != null){
				int y = KrakenLevel.SY + row;
				for (int x = 0; x < KrakenLevel.SHIP_W; x += 2){
					high.splashAt((KrakenLevel.SX + x) * S + S/2f, y * S + S/2f, 1.2f);
				}
			}
			if (low != null) low.slamFlash = 1f;
		} });
	}

	public static void thrash(){
	}

	public static void thrashHit(){
		later(new Runnable() { public void run() { PixelScene.shake(2, 0.3f); } });
	}

	public static void tentacleHurt(){
		later(new Runnable() { public void run() { if (low != null) low.tentacleFlash = 0.25f; } });
	}

	public static void tentacleDied( final int type, final int side, final int row ){
		later(new Runnable() { public void run() {
			if (low != null) low.dying(type, side, row);
			PixelScene.shake(3, 0.5f);
		} });
	}

	public static void headRises(){
		later(new Runnable() { public void run() {
			if (low != null) low.headRise = 0f;
			PixelScene.shake(5, 1.2f);
			if (high != null) high.splashAt(cx(KrakenLevel.eyeCell()), cy(KrakenLevel.eyeCell()) - 20, 2.5f);
		} });
	}

	public static void eyeHit( final boolean last ){
		later(new Runnable() { public void run() {
			if (low != null) low.eyeHitT = 0f;
			PixelScene.shake(last ? 8 : 6, last ? 2f : 1f);
			GameScene.flash(last ? 0xFFFFE0A0 : 0xFFFF8060);
			if (high != null){
				for (int i = 0; i < 6; i++){
					high.splashAt(cx(KrakenLevel.eyeCell()) + rnd.nextInt(80) - 40, cy(KrakenLevel.eyeCell()) - rnd.nextInt(30), 2f);
				}
			}
		} });
	}

	public static void bolt( final int cell ){
		later(new Runnable() { public void run() {
			if (high == null) return;
			PointF to = DungeonTilemap.tileCenterToWorld(cell);
			PointF from = new PointF(to.x + rnd.nextInt(40) - 20, to.y - 220);
			high.add(new Lightning(from, cell, null));
			high.splashAt(to.x, to.y, 0.8f);
			PixelScene.shake(2, 0.3f);
		} });
	}

	public static void arc( final int a, final int b ){
		later(new Runnable() { public void run() { if (high != null) high.add(new Lightning(a, b, null)); } });
	}

	public static void waveComing( final int row, final int dir ){
		later(new Runnable() { public void run() { if (high != null) high.waveWarn(row, dir); } });
	}

	public static void waveCrash( final int row, final int dir ){
		later(new Runnable() { public void run() { if (high != null) high.waveGo(row, dir); } });
	}

	public static void jet( final int x ){
		later(new Runnable() { public void run() {
			if (high == null) return;
			for (int y = 0; y <= 14; y += 2){
				high.splashAt((KrakenLevel.SX + x) * S + S/2f, (KrakenLevel.SY + y) * S + S/2f, 1.3f);
			}
			PixelScene.shake(3, 0.5f);
		} });
	}

	public static void mastFalls(){
		later(new Runnable() { public void run() {
			PixelScene.shake(5, 0.8f);
			if (high != null){
				for (int x = 1; x <= 9; x += 2) high.debris(KrakenLevel.cell(x, KrakenLevel.MAST_ROW));
			}
		} });
	}

	public static void harpoonShot( final int from, final int to ){
		later(new Runnable() { public void run() {
			if (high == null) return;
			MissileSprite m = (MissileSprite) high.recycle(MissileSprite.class);
			m.reset(from, to, new SKMobSprite.Shot(ItemSpriteSheet.KRAKEN_HARPOON), null);
			high.splashAt(cx(to), cy(to), 1f);
		} });
	}

	public static void sinking(){
		later(new Runnable() { public void run() {
			PixelScene.shake(6, 3f);
			KrakenHud.darken();
		} });
	}

	public static void victory(){
		later(new Runnable() { public void run() {
			if (low != null) low.victoryT = 0f;
			KrakenHud.calm();
		} });
	}

	//floor 20 transition: water floods the screen, eye opens in the dark. my favourite bit tbh
	public static void callOverlay(){
		later(new Runnable() { public void run() {
			if (Game.scene() == null) return;
			CallOverlay o = new CallOverlay();
			o.camera = PixelScene.uiCamera;
			Game.scene().add(o);
		} });
	}

	//===== Low =====

	public static class Low extends Group {

		final KrakenLevel L;
		final Strip whirl, head;
		final Image ship, shore;
		final Image fallenMast;
		final Strip helm, cannon;
		final HashMap<Integer, Strip> holeImgs = new HashMap<>();
		final HashMap<Integer, Strip> lying = new HashMap<>();
		final HashMap<Integer, Strip> suckers = new HashMap<>();
		final Group foam = new Group();
		final Group holesGroup = new Group();
		final Group tentGroup = new Group();
		Strip grab;

		float t = 0;
		float headRise = 9f, eyeHitT = 9f, victoryT = -1f, slamFlash = 0, tentacleFlash = 0;
		float whirlY, whirlScale;
		boolean placed = false;

		public Low( KrakenLevel level ){
			L = level;
			low = this;

			add(foam);
			for (int i = 0; i < 36; i++){
				ColorBlock f = new ColorBlock(1, 4 + rnd.nextInt(6), 0x66DDEEFF);
				f.x = rnd.nextInt(KrakenLevel.W * S);
				f.y = rnd.nextInt(KrakenLevel.H * S);
				foam.add(f);
			}

			whirl = new Strip(KrakenAssets.WHIRLPOOL, 352, 208);
			whirl.origin.set(176, 104);
			add(whirl);

			head = new Strip(KrakenAssets.HEAD, 256, 176);
			head.origin.set(128, 150);
			add(head);

			shore = new Image(KrakenAssets.SHORE);
			shore.alpha(0);
			add(shore);

			ship = new Image(KrakenAssets.SHIP);
			ship.x = KrakenLevel.SX * S - 16;
			ship.y = KrakenLevel.SY * S - 32;
			add(ship);

			fallenMast = new Image(KrakenAssets.MAST_FALLEN);
			fallenMast.visible = false;
			add(fallenMast);

			helm = new Strip(KrakenAssets.HELM, 32, 32);
			helm.x = cx(KrakenLevel.helmCell()) - 16;
			helm.y = cy(KrakenLevel.helmCell()) + 8 - 32;
			add(helm);

			cannon = new Strip(KrakenAssets.CANNON, 32, 32);
			cannon.x = cx(KrakenLevel.cannonCell()) - 16;
			cannon.y = cy(KrakenLevel.cannonCell()) + 8 - 32;
			add(cannon);

			add(holesGroup);
			add(tentGroup);
		}

		@Override
		public void destroy() {
			super.destroy();
			if (low == this) low = null;
		}

		void dying( int type, int side, int row ){
			//tentacle mob's already gone, so play the death anim on a copy
			Strip s = new Strip(KrakenAssets.lie(type), type == 4 ? 240 : 208, type == 4 ? 80 : 64){
				float tt = 0;
				@Override
				public void update() {
					super.update();
					tt += Game.elapsed;
					show(tt < 0.25f ? 3 : 4);
					alpha(GameMath.gate(0, 1.4f - tt, 1));
					if (tt > 1.4f) killAndErase();
				}
			};
			placeLying(s, type, side, row);
			tentGroup.add(s);
		}

		void placeLying( Image s, int type, int side, int row ){
			float centerY = (KrakenLevel.SY + row) * S + S/2f;
			if (type == 4){
				s.x = (KrakenLevel.SX - 2) * S;
				s.y = centerY - 40;
			} else {
				s.x = (KrakenLevel.SX - 1) * S;
				s.y = centerY - 32;
			}
			s.flipHorizontal = side > 0;
		}

		@Override
		public void update() {
			super.update();
			float e = Game.elapsed;
			t += e;
			headRise += e;
			eyeHitT += e;
			if (victoryT >= 0) victoryT += e;
			slamFlash = Math.max(0, slamFlash - e*2);
			tentacleFlash = Math.max(0, tentacleFlash - e);

			//sea scrolling past
			float speed = L.waterSpeed() * 1.6f;
			for (Gizmo g : foam.membersList()){
				if (!(g instanceof ColorBlock)) continue;
				ColorBlock f = (ColorBlock) g;
				f.y += speed * e;
				if (f.y > KrakenLevel.H * S) f.y -= KrakenLevel.H * S;
				if (f.y < 0) f.y += KrakenLevel.H * S;
				f.alpha(L.stage == KrakenLevel.WON ? 0.15f : 0.45f);
			}

			//whirlpool gets closer while sailing
			float eyeX = cx(KrakenLevel.eyeCell()), eyeY = cy(KrakenLevel.eyeCell());
			float farY = 3 * S, d;
			switch (L.stage){
				case KrakenLevel.ARRIVED: d = 0; break;
				case KrakenLevel.WON: d = 0; break;
				case KrakenLevel.PUSHED: d = 1f; break;
				case KrakenLevel.SAILING: d = L.distanceMax <= 0 ? 0 : L.distance / (float) L.distanceMax; break;
				default: d = 1f;
			}
			float targetY = eyeY + 6 + (farY - eyeY) * d;
			float targetScale = 1f - 0.55f * d;
			if (!placed){
				whirlY = targetY;
				whirlScale = targetScale;
				placed = true;
			}
			whirlY += (targetY - whirlY) * Math.min(1, e * 1.5f);
			whirlScale += (targetScale - whirlScale) * Math.min(1, e * 1.5f);
			whirl.x = eyeX - 176;
			whirl.y = whirlY - 104;
			whirl.scale.set(whirlScale);
			whirl.show((int)(t * 8) % whirl.count);
			if (victoryT >= 0){
				whirl.alpha(GameMath.gate(0, 1 - victoryT / 3f, 1));
			} else {
				whirl.alpha(1);
			}

			//head, only when it's in front of the bow
			boolean showHead = L.stage == KrakenLevel.ARRIVED || (L.stage == KrakenLevel.PUSHED && eyeHitT < 2.5f)
					|| (L.stage == KrakenLevel.WON && victoryT < 5f);
			if (showHead){
				float rise = GameMath.gate(0, headRise / 1.2f, 1);
				float sink = 0;
				if (L.stage == KrakenLevel.PUSHED) sink = GameMath.gate(0, (eyeHitT - 0.8f) / 1.5f, 1);
				if (L.stage == KrakenLevel.WON) sink = GameMath.gate(0, (victoryT - 1.5f) / 3f, 1);
				head.visible = true;
				head.x = eyeX - 128;
				head.y = eyeY - 150 + (1 - rise) * 60 + sink * 90;
				head.scale.set(0.7f + 0.3f * rise);
				head.alpha(GameMath.gate(0, rise * (1 - sink), 1));
				int f;
				if (eyeHitT < 0.7f) f = 3;
				else if (L.stage == KrakenLevel.WON || L.stage == KrakenLevel.PUSHED) f = eyeHitT < 1.6f ? 4 : 3;
				else if (L.eyeOpen && !Kraken.guardsAlive()) f = 2;
				else if (headRise < 0.6f) f = 0;
				else f = (int)(t * 1.5f) % 5 == 0 ? 4 : 1;
				head.show(f);
				if (eyeHitT < 0.3f) head.tint(0xFFFFFFFF, 1f - eyeHitT / 0.3f);
				else head.resetColor();
			} else {
				head.visible = false;
			}

			//shore after the win
			if (victoryT >= 0){
				shore.x = eyeX - shore.width / 2f;
				shore.y = eyeY - shore.height + 8 - GameMath.gate(0, 1 - (victoryT - 2f) / 3f, 1) * 60;
				shore.alpha(GameMath.gate(0, (victoryT - 2f) / 2f, 1));
			} else if (L.stage == KrakenLevel.WON){
				shore.x = eyeX - shore.width / 2f;
				shore.y = eyeY - shore.height + 8;
				shore.alpha(1);
			}

			//rocking. 1.3 and 0.6 just felt right
			float rock = L.stage == KrakenLevel.WON ? 0 : (float)Math.sin(t * 1.3f) * 0.6f;
			ship.y = KrakenLevel.SY * S - 32 + rock;

			if (L.mast == 2){
				fallenMast.visible = true;
				fallenMast.flipHorizontal = L.mastSide < 0;
				fallenMast.x = (KrakenLevel.SX) * S;
				fallenMast.y = (KrakenLevel.SY + KrakenLevel.MAST_ROW) * S + S/2f - fallenMast.height/2f;
			} else {
				fallenMast.visible = false;
			}

			helm.show(Kraken.helmManned() ? (int)(t * 8) % helm.count : 0);
			cannon.show(hasHarpoonNear() ? 1 : 0);

			syncHoles();
			syncTentacles();
		}

		private boolean hasHarpoonNear(){
			for (int n : PathFinder.NEIGHBOURS8){
				Char ch = Actor.findChar(KrakenLevel.cannonCell() + n);
				if (ch instanceof Hero && ((Hero) ch).belongings.getItem(KrakenHarpoon.class) != null) return true;
			}
			return false;
		}

		private void syncHoles(){
			ArrayList<Integer> want = new ArrayList<>(L.holes);
			for (Integer c : new ArrayList<>(holeImgs.keySet())){
				if (!want.contains(c)){
					holeImgs.remove(c).killAndErase();
				}
			}
			for (int c : want){
				Strip s = holeImgs.get(c);
				if (s == null){
					s = new Strip(KrakenAssets.HOLES, 16, 16);
					s.x = (c % KrakenLevel.W) * S;
					s.y = (c / KrakenLevel.W) * S;
					holesGroup.add(s);
					holeImgs.put(c, s);
				}
				int variant = (c * 7) % 3;
				s.show(variant * 2 + ((int)(t * 3 + c) % 2));
			}
		}

		private void syncTentacles(){
			ArrayList<Integer> alive = new ArrayList<>();
			Strip grabbing = null;
			//copy under the actors lock, game thread messes with the list meanwhile
			for (Char m : Actor.chars()){
				if (!(m instanceof GiantTentacle) || !m.isAlive()) continue;
				GiantTentacle gt = (GiantTentacle) m;
				alive.add(gt.id());
				Strip s = lying.get(gt.id());
				if (s == null){
					s = new Strip(KrakenAssets.lie(gt.type), gt.type == 4 ? 240 : 208, gt.type == 4 ? 80 : 64);
					placeLying(s, gt.type, gt.side, gt.row);
					tentGroup.addToBack(s);
					lying.put(gt.id(), s);
				}
				s.show((int)(t * 4 + gt.id()) % 3);
				if (tentacleFlash > 0) s.tint(0xFFFFFFFF, tentacleFlash * 2);
				else s.resetColor();

				if (gt.armoured()){
					Strip g = suckers.get(gt.id());
					if (g == null){
						g = new Strip(KrakenAssets.SUCKER, 16, 16);
						tentGroup.add(g);
						suckers.put(gt.id(), g);
					}
					int c = gt.segmentCell(gt.sucker);
					g.visible = c != -1;
					if (c != -1){
						g.x = (c % KrakenLevel.W) * S;
						g.y = (c / KrakenLevel.W) * S;
						g.show((int)(t * 6) % g.count);
						g.alpha(0.7f + 0.3f * (float)Math.sin(t * 8));
					}
				}
			}
			for (Integer id : new ArrayList<>(lying.keySet())){
				if (!alive.contains(id)){
					lying.remove(id).killAndErase();
					Strip g = suckers.remove(id);
					if (g != null) g.killAndErase();
				}
			}

			//alguien colgando del gancho
			Hero held = null;
			for (Hero h : Kraken.partyHeroes()){
				KrakenHold k = h.buff(KrakenHold.class);
				if (k != null && k.kind == KrakenHold.GRABBED && k.helpless && h.isAlive()) held = h;
			}
			if (held != null){
				if (grab == null){
					grab = new Strip(KrakenAssets.GRAB, 64, 176);
					tentGroup.add(grab);
				}
				grab.visible = true;
				grab.x = cx(held.pos) - 32;
				grab.y = cy(held.pos) - 30;
				grab.show((int)(t * 4) % grab.count);
			} else if (grab != null){
				grab.visible = false;
			}
		}
	}

	//===== High =====

	public static class High extends Group {

		final KrakenLevel L;
		final Image mast;
		final Strip fire;
		final Strip rising;
		int risingType = -1;
		float riseStart = 9f;
		final Strip wave;
		float waveT = 9f, warnT = 9f;
		int waveRow = -1, waveDir = 1;
		float t = 0;

		public High( KrakenLevel level ){
			L = level;
			high = this;

			mast = new Image(KrakenAssets.MAST);
			mast.x = cx(KrakenLevel.mastCell()) - 32;
			mast.y = cy(KrakenLevel.mastCell()) + 8 - 128;
			add(mast);

			fire = new Strip(KrakenAssets.MAST_FIRE, 64, 128);
			fire.x = mast.x;
			fire.y = mast.y;
			fire.visible = false;
			add(fire);

			rising = new Strip(KrakenAssets.rise(1), 64, 176);
			rising.visible = false;
			add(rising);

			wave = new Strip(KrakenAssets.WAVE, 208, 56);
			wave.visible = false;
			add(wave);
		}

		@Override
		public void destroy() {
			super.destroy();
			if (high == this) high = null;
		}

		void splashAt( float x, float y, float scale ){
			OneShot s = new OneShot(KrakenAssets.SPLASH, 48, 48, 14);
			s.origin.set(24, 40);
			s.scale.set(scale);
			s.x = x - 24;
			s.y = y - 40;
			add(s);
		}

		void debris( int cell ){
			CellEmitter.get(cell).burst(
					Speck.factory(Speck.ROCK), 5);
		}

		void waveWarn( int row, int dir ){
			warnT = 0;
			waveRow = row;
			waveDir = dir;
		}

		void waveGo( int row, int dir ){
			waveT = 0;
			waveRow = row;
			waveDir = dir;
		}

		@Override
		public void update() {
			super.update();
			float e = Game.elapsed;
			t += e;
			riseStart += e;
			waveT += e;
			warnT += e;

			//mast goes see-through if someone's under the sail
			boolean standing = L.mast != 2;
			mast.visible = standing;
			boolean under = false;
			for (Hero h : Kraken.partyHeroes()){
				int x = KrakenLevel.shipX(h.pos), y = KrakenLevel.shipY(h.pos);
				if (y >= KrakenLevel.MAST_ROW - 6 && y < KrakenLevel.MAST_ROW && x >= 3 && x <= 7) under = true;
			}
			mast.alpha(under ? 0.45f : 1f);
			mast.angle = L.stage == KrakenLevel.WON ? 0 : (float)Math.sin(t * 1.1f) * 1.2f;
			mast.origin.set(32, 120);
			fire.visible = standing && (L.mast == 1 || L.mast == 3);
			if (fire.visible){
				fire.show((int)(t * 10) % fire.count);
				fire.alpha(mast.alpha());
			}

			//rising tentacle (the telegraph before the slam)
			if (L.riseRow != -1 && L.stage != KrakenLevel.WON){
				if (risingType != L.riseType){
					risingType = L.riseType;
					rising.texture(KrakenAssets.rise(risingType));
				}
				int fw = risingType == 4 ? 80 : 64, fh = risingType == 4 ? 208 : 176;
				rising.visible = true;
				int count = Math.max(1, (int)(rising.texture.width / fw));
				rising.frame(((int)(t * 6) % count) * fw, 0, fw, fh);
				float baseX = L.riseSide < 0 ? (KrakenLevel.SX - 1) * S + S/2f - 6 : (KrakenLevel.SX + KrakenLevel.SHIP_W) * S + S/2f + 6;
				float baseY = (KrakenLevel.SY + L.riseRow + 1) * S + 10;
				float up = GameMath.gate(0, riseStart / 0.6f, 1);
				rising.origin.set(fw / 2f, fh);
				rising.x = baseX - fw / 2f;
				rising.y = baseY - fh;
				rising.scale.set(1f, 0.3f + 0.7f * up);
				rising.flipHorizontal = L.riseSide > 0;
				rising.angle = (float)Math.sin(t * 3) * 4 + (L.riseSide < 0 ? 8 : -8) * up;
			} else {
				rising.visible = false;
			}

			if (waveT < 1.1f && waveRow != -1){
				wave.visible = true;
				float p = waveT / 1.1f;
				float y = (KrakenLevel.SY + waveRow + 1) * S - 40;
				float startX = waveDir > 0 ? (KrakenLevel.SX - 13) * S : (KrakenLevel.SX + KrakenLevel.SHIP_W + 2) * S;
				float endX = waveDir > 0 ? (KrakenLevel.SX + KrakenLevel.SHIP_W + 2) * S : (KrakenLevel.SX - 13) * S;
				wave.x = startX + (endX - startX) * p;
				wave.y = y;
				wave.flipHorizontal = waveDir < 0;
				wave.show((int)(waveT * 10) % wave.count);
				wave.alpha(GameMath.gate(0, Math.min(p * 4, (1 - p) * 4), 1));
			} else if (warnT < 1.5f && waveRow != -1){
				//builds up on the side first
				wave.visible = true;
				float x = waveDir > 0 ? (KrakenLevel.SX - 13) * S + 40 : (KrakenLevel.SX + KrakenLevel.SHIP_W) * S - 40;
				wave.x = x;
				wave.y = (KrakenLevel.SY + waveRow + 1) * S - 40;
				wave.flipHorizontal = waveDir < 0;
				wave.show(0);
				wave.alpha(0.5f * GameMath.gate(0, warnT * 2, 1));
			} else {
				wave.visible = false;
			}
		}
	}

	//floor 20 overlay

	public static class CallOverlay extends Group {

		final ColorBlock dark;
		final Strip eye;
		final Group bubbles = new Group();
		float t = 0;

		public CallOverlay(){
			float w = PixelScene.uiCamera.width, h = PixelScene.uiCamera.height;
			dark = new ColorBlock(w, h, 0xFF02060C);
			dark.alpha(0);
			add(dark);
			add(bubbles);
			for (int i = 0; i < 50; i++){
				ColorBlock b = new ColorBlock(2 + rnd.nextInt(3), 2 + rnd.nextInt(3), 0xAA9FE8FF);
				b.x = rnd.nextInt((int) w);
				b.y = h + rnd.nextInt((int) h);
				bubbles.add(b);
			}
			eye = new Strip(KrakenAssets.EYE_CLOSEUP, 160, 80);
			eye.origin.set(80, 40);
			eye.scale.set(1.5f);
			eye.x = w / 2f - 80;
			eye.y = h / 2f - 40;
			eye.alpha(0);
			add(eye);
		}

		@Override
		public void update() {
			super.update();
			float e = Game.elapsed;
			t += e;
			dark.alpha(GameMath.gate(0, t / 1.5f, 0.92f));
			for (Gizmo g : bubbles.membersList()){
				if (g instanceof ColorBlock){
					((ColorBlock) g).y -= (60 + (g.hashCode() % 40)) * e;
				}
			}
			float et = t - 1.6f;
			if (et > 0){
				eye.alpha(GameMath.gate(0, et, 1));
				eye.show(Math.min(eye.count - 1, (int)(et * 2.5f)));
			}
		}
	}
}
