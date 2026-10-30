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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.big;

import com.badlogic.gdx.graphics.Pixmap;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.watabou.gltextures.SmartTexture;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.GameMath;
import com.watabou.utils.Rect;
import com.watabou.utils.RectF;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

//what makes the big ones feel big, all of it on the screen of each player and nothing in the game
//logic: the camera pulls back while one of 8x8 or more is in sight, the whole of a big one in
//sight shows (the fog lifts off the box its drawing covers), the first time you see one its name
//comes up, their steps shake the screen before you see them, out in the dark they're a shape
//against the sky, and the one hunting you gets the boss bar
public class ColossusPresence extends Group {

	public static final int BIG = 8;

	//the fog lifted over the drawings of the big ones in sight: null when there's none
	private static boolean[] lifted;
	private static boolean[] view;
	private static ArrayList<Rect> liftedBoxes = new ArrayList<>();

	//camera
	private static final float PULL_RANGE = 30;
	private float userZoom = -1;

	//title card
	private RenderedTextBlock title, sub;
	private ColorBlock back;
	private float titleT = -1;

	//shapes in the dark: its drawing, dark and cold, and a pixel up and left of it a pale rim, as if
	//the sky behind lit its edge. they breathe a little and come and go slowly
	private static final int SHAPE_RANGE = 45;
	private final HashMap<Colossus, Image[]> shapes = new HashMap<>();
	private final HashMap<Colossus, Float> shapeA = new HashMap<>();
	private float breath = 0;

	private final ArrayList<Colossus> near = new ArrayList<>();
	private float lookT = 99;

	@Override
	public void update() {
		super.update();
		look();
		pullCamera();
		updateShapes();
		updateTitle();
	}

	//the mob list belongs to the actor thread: a copy now and then is enough
	private void look(){
		lookT += Game.elapsed;
		if (lookT < 0.25f) return;
		lookT = 0;
		near.clear();
		ArrayList<Colossus> all = new ArrayList<>();
		try {
			for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
				if (!(m instanceof Colossus) || !m.isAlive()) continue;
				all.add((Colossus) m);
				if (((Colossus) m).size >= BIG) near.add((Colossus) m);
			}
		} catch (Exception e){
			near.clear();
			all.clear();
		}
		lift(all);
		Hero me = Dungeon.local();
		for (Colossus c : near){
			if (me == null || !c.seen()) continue;
			if (!c.announced){
				c.announced = true;
				showTitle(c.name(), c.epithet());
			}
			if (c.state == c.HUNTING && !BossHealthBar.isAssigned()) BossHealthBar.assignBoss(c);
		}
	}

	// *** the whole of it in sight ***

	//the drawing of a big one sticks out of its patch by a lot, so seeing a corner of the patch
	//left the rest of it cut by the fog. the cells its drawing covers (and one more round them)
	//are shown as if in sight: the fog is drawn with fogView and the small things standing there
	//show too. it follows the shape of the drawing, not its box: a box of open ground lit round a
	//giant in the dark looked like a room. the game logic still goes by the real field of view
	private void lift( ArrayList<Colossus> all ){
		int w = Dungeon.level.width(), h = Dungeon.level.height();
		ArrayList<Rect> boxes = new ArrayList<>();
		boolean[] l = null;
		for (Colossus c : all){
			CharSprite sp = c.sprite;
			if (sp == null || !sp.visible || !c.seen()) continue;
			if (l == null) l = new boolean[Dungeon.level.length()];
			boxes.add(shape(sp, l, w, h));
		}
		if (l == null && lifted == null) return;
		if (l != null && lifted != null && Arrays.equals(l, lifted)) return;
		lifted = l;
		for (Rect r : liftedBoxes) GameScene.updateFog(r.left, r.top, r.width(), r.height());
		for (Rect r : boxes) GameScene.updateFog(r.left, r.top, r.width(), r.height());
		liftedBoxes = boxes;
		//the mob list belongs to the actor thread: if it's busy, the next observe does it
		try {
			GameScene.afterObserve();
		} catch (Exception e){
			//nothing
		}
	}

	//marks the cells under the solid pixels of the frame on show, grown by a cell, and gives back
	//the box round them (for the fog to redraw). without the picture at hand, the whole box
	private static Rect shape( CharSprite sp, boolean[] l, int w, int h ){
		int s = DungeonTilemap.SIZE;
		Rect box = new Rect(
				Math.max(0, (int) Math.floor(sp.x / s) - 1),
				Math.max(0, (int) Math.floor(sp.y / s) - 1),
				Math.min(w, (int) Math.ceil((sp.x + sp.width()) / s) + 1),
				Math.min(h, (int) Math.ceil((sp.y + sp.height()) / s) + 1));
		Pixmap pic = sp.texture instanceof SmartTexture ? ((SmartTexture) sp.texture).bitmap : null;
		if (pic == null){
			for (int y = box.top; y < box.bottom; y++){
				for (int x = box.left; x < box.right; x++) l[x + y * w] = true;
			}
			return box;
		}
		RectF f = sp.frame();
		int fx = Math.round(f.left * pic.getWidth()), fy = Math.round(f.top * pic.getHeight());
		int fw = Math.round(f.width() * pic.getWidth()), fh = Math.round(f.height() * pic.getHeight());
		boolean[] hit = new boolean[box.width() * box.height()];
		for (int py = 0; py < fh; py += 2){
			for (int px = 0; px < fw; px += 2){
				if ((pic.getPixel(fx + px, fy + py) & 0xFF) == 0) continue;
				int cx = (int) ((sp.x + (sp.flipHorizontal ? fw - 1 - px : px)) / s) - box.left;
				int cy = (int) ((sp.y + py) / s) - box.top;
				if (cx >= 0 && cy >= 0 && cx < box.width() && cy < box.height()) hit[cx + cy * box.width()] = true;
			}
		}
		for (int y = 0; y < box.height(); y++){
			for (int x = 0; x < box.width(); x++){
				if (!hit[x + y * box.width()]) continue;
				for (int dy = -1; dy <= 1; dy++){
					for (int dx = -1; dx <= 1; dx++){
						int mx = box.left + x + dx, my = box.top + y + dy;
						if (mx >= 0 && my >= 0 && mx < w && my < h) l[mx + my * w] = true;
					}
				}
			}
		}
		return box;
	}

	//what the fog is drawn with: the field of view plus the lifted boxes
	public static boolean[] fogView( boolean[] fov ){
		boolean[] l = lifted;
		if (l == null || l.length != fov.length) return fov;
		if (view == null || view.length != fov.length) view = new boolean[fov.length];
		for (int i = 0; i < fov.length; i++) view[i] = fov[i] || l[i];
		return view;
	}

	public static boolean lifted( int cell ){
		boolean[] l = lifted;
		return l != null && cell >= 0 && cell < l.length && l[cell];
	}

	@Override
	public void destroy() {
		super.destroy();
		lifted = null;
		liftedBoxes = new ArrayList<>();
	}

	// *** camera ***

	private static float normalZoom(){
		return GameMath.gate(PixelScene.minZoom, PixelScene.defaultZoom + SPDSettings.zoom(), PixelScene.maxZoom);
	}

	private void pullCamera(){
		Hero me = Dungeon.local();
		if (me == null || me.sprite == null) return;
		int w = Dungeon.level.width();
		int x0 = me.pos % w, y0 = me.pos / w, x1 = x0, y1 = y0;
		boolean any = false;
		for (Colossus c : near){
			if (!c.seen() || c.reach(me.pos) > PULL_RANGE) continue;
			any = true;
			x0 = Math.min(x0, c.left());
			y0 = Math.min(y0, c.top());
			x1 = Math.max(x1, c.left() + c.size - 1);
			y1 = Math.max(y1, c.top() + c.size - 1);
		}
		Camera cam = Camera.main;
		float target;
		if (any){
			if (userZoom < 0) userZoom = cam.zoom;
			//hero in the middle, so the box is as wide as twice the far side
			int hx = me.pos % w, hy = me.pos / w;
			float spanX = 2 * Math.max(hx - x0, x1 - hx) + 5;
			float spanY = 2 * Math.max(hy - y0, y1 - hy) + 5;
			float fit = Math.min(cam.screenWidth() / (spanX * DungeonTilemap.SIZE), cam.screenHeight() / (spanY * DungeonTilemap.SIZE));
			target = Math.max(Math.max(1f, userZoom * 0.4f), Math.min(userZoom, fit));
		} else if (userZoom > 0){
			target = userZoom;
		} else {
			return;
		}
		float z = cam.zoom;
		if (Math.abs(target - z) < 0.01f){
			if (!any) userZoom = -1;
			return;
		}
		z += (target - z) * Math.min(1f, Game.elapsed * 1.5f);
		cam.zoom(z);
		//zooming snaps the camera, so it has to be told to follow again
		cam.panFollow(me.sprite, 5f);
	}

	// *** shapes in the dark ***

	private void updateShapes(){
		Hero me = Dungeon.local();
		breath += Game.elapsed;
		for (Colossus c : near){
			Image[] s = shapes.get(c);
			CharSprite sp = c.sprite;
			boolean show = me != null && sp != null && !sp.visible && c.reach(me.pos) <= SHAPE_RANGE;
			if (s == null){
				if (!show) continue;
				Image rim = new Image();
				rim.rm = rim.gm = rim.bm = 0;
				rim.ra = 0.52f;
				rim.ga = 0.56f;
				rim.ba = 0.68f;
				//its own drawing, dark and cold: the shapes of it can be made out, not a cut-out
				Image body = new Image();
				body.rm = 0.2f;
				body.gm = 0.22f;
				body.bm = 0.3f;
				s = new Image[]{rim, body};
				shapes.put(c, s);
				shapeA.put(c, 0f);
				GameScene.effectOverFog(rim);
				GameScene.effectOverFog(body);
			}
			//closer is clearer; in and out slowly, never popping
			float target = show ? 0.95f - 0.5f * c.reach(me.pos) / (float) SHAPE_RANGE : 0f;
			float a = shapeA.get(c);
			a += GameMath.gate(-Game.elapsed * 0.8f, target - a, Game.elapsed * 0.8f);
			shapeA.put(c, a);
			boolean on = a > 0.01f && sp != null && !sp.visible;
			for (Image i : s) i.visible = on;
			if (!on) continue;
			float pulse = a * (0.9f + 0.1f * (float) Math.sin(breath * 0.9f + c.id()));
			for (Image i : s){
				i.texture = sp.texture;
				i.frame(sp.frame());
				i.flipHorizontal = sp.flipHorizontal;
				i.x = sp.x;
				i.y = sp.y;
			}
			s[0].x -= 1;
			s[0].y -= 1;
			s[0].am = pulse * 0.7f;
			s[1].am = pulse;
		}
		for (Colossus c : new ArrayList<>(shapes.keySet())){
			if (!near.contains(c)){
				for (Image i : shapes.remove(c)) i.killAndErase();
				shapeA.remove(c);
			}
		}
	}

	// *** title ***

	private void showTitle( String name, String epithet ){
		if (title != null){
			title.killAndErase();
			sub.killAndErase();
			back.killAndErase();
		}
		float uiw = PixelScene.uiCamera.width, uih = PixelScene.uiCamera.height;
		title = PixelScene.renderTextBlock(name, 12);
		title.hardlight(0xE8D8C0);
		sub = PixelScene.renderTextBlock(epithet, 7);
		sub.hardlight(0xA89888);
		float bw = Math.max(title.width(), sub.width()) + 28;
		back = new ColorBlock(bw, title.height() + sub.height() + 16, 0xD0080606);
		back.x = (uiw - bw) / 2f;
		back.y = uih * 0.16f;
		title.setPos((uiw - title.width()) / 2f, back.y + 6);
		sub.setPos((uiw - sub.width()) / 2f, title.bottom() + 4);
		PixelScene.align(title);
		PixelScene.align(sub);
		add(back);
		add(title);
		add(sub);
		titleT = 0;
		Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 0.5f);
	}

	private void updateTitle(){
		if (titleT < 0 || title == null) return;
		titleT += Game.elapsed;
		//fade in, hold, fade out
		float a = titleT < 0.6f ? titleT / 0.6f : titleT < 3.6f ? 1f : Math.max(0, 1f - (titleT - 3.6f));
		title.alpha(a);
		sub.alpha(a);
		back.alpha(a * 0.82f);
		if (a <= 0 && titleT > 3.6f){
			title.killAndErase();
			sub.killAndErase();
			back.killAndErase();
			title = null;
			titleT = -1;
		}
	}

	// *** steps ***

	//a step of something big: the ground shakes and you hear it well before you see it
	public static void step( Colossus c ){
		if (c.size < BIG || !c.solid()) return;
		Hero me = Dungeon.local();
		if (me == null) return;
		final int d = c.reach(me.pos);
		if (d > PULL_RANGE) return;
		final float strength = (1f - d / PULL_RANGE) * Math.min(1f, c.size / 15f);
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				PixelScene.shake(0.5f + 2.5f * strength, 0.25f);
				Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.25f + 0.75f * strength, 0.45f);
			}
		});
	}
}
