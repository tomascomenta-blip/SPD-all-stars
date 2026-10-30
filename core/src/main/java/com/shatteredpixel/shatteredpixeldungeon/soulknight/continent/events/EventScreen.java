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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.events;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Biome;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.OpenWorld;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.NoosaScript;
import com.watabou.noosa.SkinnedBlock;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.Tilemap;
import com.watabou.noosa.Visual;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.noosa.tweeners.AlphaTweener;
import com.watabou.utils.Callback;
import com.watabou.utils.GameMath;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

import java.util.Arrays;
import java.util.HashMap;

//how the events look and sound, on each screen for its own hero and never in the game logic: the
//card with its name, dust on the edge of the screen the way it's coming from, the thuds of the
//giant (louder the closer, with the ground shaking), its prints, the tide over the ground, and the
//weather over everything (a veil drifting with the wind and what it carries)
public class EventScreen extends Group {

	public static final String DIR = "environment/surface/events/";

	private WorldEvent shown;
	private int card = -1;
	private int stepSeen = -1;
	private float rumble = 0;

	//on the map
	private TideMap tideMap;
	private Veil veil;
	private String veilKey;
	private Emitter weather;
	private Weather weatherKind;

	//on the screen
	private final Horizon horizon;
	private RenderedTextBlock title, sub;
	private ColorBlock back;
	private float titleT = -1;

	public EventScreen(){
		if (Dungeon.level instanceof OpenWorld){
			GameScene.add(new PrintTiles(), false);
			TideTiles tt = new TideTiles();
			GameScene.add(tt, false);
			tideMap = tt.map;
		}
		horizon = new Horizon();
		add(horizon);
	}

	@Override
	public void update() {
		super.update();
		WorldEvent e = WorldEvents.active();
		Hero me = Dungeon.local();
		if (e != shown){
			shown = e;
			card = e == null ? -1 : e.card - (e.age <= 1 ? 1 : 0);
			stepSeen = e instanceof GiantSteps ? ((GiantSteps) e).landed : -1;
		}
		if (e != null && e.card != card){
			card = e.card;
			showTitle(e);
		}
		updateTitle();
		if (e instanceof GiantSteps) steps((GiantSteps) e, me);
		if (e instanceof Migration) rumble((Migration) e);
		if (tideMap != null) tideMap.follow(e instanceof YogTide ? (YogTide) e : null);
		weather(e, me);
		horizon.point(e, me);
		if (DevFlags.event() != null) EventTest.tick(me);
	}

	// *** the card ***

	private void showTitle( WorldEvent e ){
		if (title != null){
			title.killAndErase();
			sub.killAndErase();
			back.killAndErase();
		}
		float uiw = PixelScene.uiCamera.width, uih = PixelScene.uiCamera.height;
		title = PixelScene.renderTextBlock(e.title(), 9);
		title.hardlight(colorOf(e));
		sub = PixelScene.renderTextBlock(e.line(), 6);
		sub.hardlight(0xC8BCB0);
		sub.maxWidth((int) (uiw * 0.7f));
		float bw = Math.max(title.width(), sub.width()) + 20;
		back = new ColorBlock(bw, title.height() + sub.height() + 12, 0xD0080606);
		back.x = (uiw - bw) / 2f;
		back.y = uih * 0.27f;
		title.setPos((uiw - title.width()) / 2f, back.y + 4);
		sub.setPos((uiw - sub.width()) / 2f, title.bottom() + 4);
		PixelScene.align(title);
		PixelScene.align(sub);
		add(back);
		add(title);
		add(sub);
		titleT = 0;
		String sound = e instanceof YogTide ? Assets.Sounds.CURSED : e instanceof Storm ? Assets.Sounds.GAS
				: e instanceof Migration ? Assets.Sounds.CHALLENGE : Assets.Sounds.ROCKS;
		Sample.INSTANCE.play(sound, 0.6f, 0.6f);
	}

	private static int colorOf( WorldEvent e ){
		if (e instanceof YogTide) return 0xD8A0F0;
		if (e instanceof Storm) return 0xD8D0C4;
		if (e instanceof Migration) return 0xE8C890;
		return 0xE8D8C0;
	}

	private void updateTitle(){
		if (titleT < 0 || title == null) return;
		titleT += Game.elapsed;
		float a = titleT < 0.5f ? titleT / 0.5f : titleT < 4f ? 1f : Math.max(0, 1f - (titleT - 4f));
		title.alpha(a);
		sub.alpha(a);
		back.alpha(a * 0.8f);
		if (a <= 0 && titleT > 4f){
			title.killAndErase();
			sub.killAndErase();
			back.killAndErase();
			title = null;
			titleT = -1;
		}
	}

	// *** the giant ***

	private void steps( GiantSteps g, Hero me ){
		while (stepSeen < g.landed){
			stepSeen++;
			thud(g, stepSeen, me);
		}
	}

	//the closer, the harder the ground shakes and the louder it is; right by it the dust flies
	private void thud( GiantSteps g, int k, Hero me ){
		if (me == null) return;
		int w = Dungeon.level.width();
		int x = g.footX(k), y = g.footY(k);
		float dx = x - me.pos % w, dy = y - me.pos / w;
		float d = (float) Math.sqrt(dx * dx + dy * dy);
		float s = GameMath.gate(0, 1f - d / 95f, 1);
		if (s <= 0) return;
		PixelScene.shake(0.4f + 4.6f * s * s, 0.2f + 0.4f * s);
		Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.2f + 0.8f * s, 0.3f + 0.15f * s);
		boolean[] fov = Dungeon.level.heroFOV;
		for (int c : GiantSteps.patch(x, y)){
			if (fov[c]) CellEmitter.get(c).burst(Speck.factory(g.under ? Speck.ROCK : Speck.DUST), g.under ? 4 : 3);
		}
		//down below, the roof sheds over everyone's heads with every step
		if (g.under && s > 0.3f){
			for (int i = 0; i < 3; i++){
				int c = me.pos + Random.Visual.IntRange(-3, 3) + Random.Visual.IntRange(-3, 3) * w;
				if (c >= 0 && c < fov.length && fov[c]) CellEmitter.get(c).burst(Speck.factory(Speck.DUST), 1);
			}
		}
	}

	//a herd on its way: the ground hums now and then
	private void rumble( Migration m ){
		if (m.origin() == -1) return;
		rumble -= Game.elapsed;
		if (rumble > 0) return;
		rumble = 1.4f + Random.Visual.Float(0.6f);
		PixelScene.shake(0.5f + 0.5f * m.power(), 0.3f);
		Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.1f + 0.15f * m.power(), 0.3f);
	}

	// *** the weather ***

	private void weather( WorldEvent e, Hero me ){
		String key = null;
		float want = 0;
		if (e instanceof Storm){
			key = ((Storm) e).key();
			want = 0.85f * e.power();
		} else if (e instanceof YogTide){
			key = "tide";
			want = me != null && ((YogTide) e).covers(me.pos) ? 0.9f : 0f;
		}
		if (key != null && !key.equals(veilKey)){
			if (sky == null){
				sky = new Sky();
				GameScene.effectOverFog(sky);
			}
			sky.inner.clear();
			veilKey = key;
			weatherKind = Weather.of(key);
			veil = new Veil(DIR + "veil_" + key + ".png", weatherKind.windX, weatherKind.windY);
			sky.inner.add(veil);
			weather = new Emitter();
			//about the same number on the screen at once, whatever they are
			weather.pour(new Weather.Factory(weatherKind, this), weatherKind.life / 130f);
			sky.inner.add(weather);
		}
		if (veil != null) veil.want = want;
		if (weather != null){
			//over what the camera sees, but never past the edges of the map
			Camera c = Camera.main;
			float m = 24, s = DungeonTilemap.SIZE;
			float x0 = Math.max(0, c.scroll.x - m), y0 = Math.max(0, c.scroll.y - m);
			float x1 = Math.min(Dungeon.level.width() * s, c.scroll.x + c.width + m);
			float y1 = Math.min(Dungeon.level.height() * s, c.scroll.y + c.height + m);
			weather.pos(x0, y0, Math.max(1, x1 - x0), Math.max(1, y1 - y0));
			weatherPower = veil == null ? 0 : veil.a;
		}
	}

	private float weatherPower = 0;
	private Sky sky;

	//over the map and over the fog too. the scene only takes single visuals up there, so this one
	//carries a group of its own: the veil and what the wind blows
	private static class Sky extends Visual {

		final Group inner = new Group();

		Sky(){
			super(0, 0, 0, 0);
			camera = inner.camera = Camera.main;
		}

		//it has no size of its own: what it carries decides
		@Override
		public boolean isVisible() {
			return visible;
		}

		@Override
		public void update() {
			inner.update();
		}

		@Override
		public void draw() {
			inner.draw();
		}

		@Override
		public void destroy() {
			super.destroy();
			inner.destroy();
		}
	}

	//the veil of the weather over the whole floor, drifting with the wind
	private static class Veil extends SkinnedBlock {

		private final float wx, wy;
		float want = 0, a = 0;

		Veil( String tex, float wx, float wy ){
			super(Dungeon.level.width() * DungeonTilemap.SIZE, Dungeon.level.height() * DungeonTilemap.SIZE, tex);
			this.wx = wx;
			this.wy = wy;
			autoAdjust = true;
			alpha(0);
		}

		@Override
		public void update() {
			super.update();
			float e = Game.elapsed;
			offset(-wx * e, -wy * e);
			a += GameMath.gate(-e * 0.35f, want - a, e * 0.35f);
			alpha(a);
			visible = a > 0.01f;
		}
	}

	//what each weather carries: the 8 little things of its sheet (8x8 each), how often each, how
	//they move and for how long
	static class Weather {

		final String key, sheet;
		final float windX, windY;
		final float[] chances;
		final float vx0, vx1, vy0, vy1, life, sway;
		//kinds that splash where they land instead of flying (the blood rain)
		final int splashFrom;
		final boolean onTide;

		Weather( String key, float windX, float windY, float[] chances, float vx0, float vx1, float vy0, float vy1,
				float life, float sway, int splashFrom, boolean onTide ){
			this.key = key;
			this.sheet = DIR + "drops_" + key + ".png";
			this.windX = windX;
			this.windY = windY;
			this.chances = chances;
			this.vx0 = vx0;
			this.vx1 = vx1;
			this.vy0 = vy0;
			this.vy1 = vy1;
			this.life = life;
			this.sway = sway;
			this.splashFrom = splashFrom;
			this.onTide = onTide;
		}

		private static final HashMap<String, Weather> ALL = new HashMap<>();

		static {
			put(new Weather("ash", 36, 9, new float[]{3, 4, 3, 3, 1, 1, 1, 0}, 40, 70, -4, 8, 2.4f, 0, -1, false));
			put(new Weather("frost", 62, 16, new float[]{4, 4, 3, 3, 1, 1, 2, 0}, 60, 100, 8, 20, 2f, 0, -1, false));
			put(new Weather("flesh", 6, 70, new float[]{3, 2, 4, 4, 2, 2, 1, 0}, 4, 12, 110, 150, 0.9f, 0, 4, false));
			put(new Weather("city", 10, 14, new float[]{4, 4, 2, 2, 1, 1, 2, 0}, -6, 10, 14, 28, 3.5f, 14, -1, false));
			put(new Weather("bones", 34, 4, new float[]{4, 4, 3, 2, 2, 2, 1, 0}, 30, 55, -6, 6, 2.6f, 10, -1, false));
			put(new Weather("isles", 10, 2, new float[]{4, 4, 3, 3, 1, 1, 1, 0}, 8, 16, -2, 2, 4f, 4, -1, false));
			put(new Weather("tide", 14, -3, new float[]{2, 2, 1, 1, 4, 4, 2, 0}, -3, 3, -20, -10, 1.4f, 6, -1, true));
		}

		//like Random.chances, with the dice of the screen
		private static int pick( float[] chances ){
			float sum = 0;
			for (float c : chances) sum += c;
			float v = Random.Visual.Float(sum);
			for (int i = 0; i < chances.length; i++){
				v -= chances[i];
				if (v < 0) return i;
			}
			return 0;
		}

		private static void put( Weather w ){
			ALL.put(w.key, w);
		}

		static Weather of( String key ){
			Weather w = ALL.get(key);
			return w != null ? w : ALL.get("ash");
		}

		static class Factory extends Emitter.Factory {

			private final Weather w;
			private final EventScreen screen;

			Factory( Weather w, EventScreen screen ){
				this.w = w;
				this.screen = screen;
			}

			@Override
			public void emit( Emitter emitter, int index, float x, float y ) {
				if (Random.Visual.Float() > screen.weatherPower) return;
				if (w.onTide){
					WorldEvent e = WorldEvents.active();
					int cx = (int) (x / DungeonTilemap.SIZE), cy = (int) (y / DungeonTilemap.SIZE);
					if (cx < 0 || cy < 0 || cx >= Dungeon.level.width() || cy >= Dungeon.level.height()) return;
					if (!(e instanceof YogTide) || !((YogTide) e).covers(cx + cy * Dungeon.level.width())) return;
				}
				int kind = pick(w.chances);
				Drop d = (Drop) emitter.recycle(Drop.class);
				if (w.splashFrom >= 0 && kind >= w.splashFrom && kind < w.splashFrom + 2){
					d.reset(w.sheet, kind, x, y, 0, 0, 0.35f, 0);
				} else {
					d.reset(w.sheet, kind, x, y, Random.Visual.Float(w.vx0, w.vx1), Random.Visual.Float(w.vy0, w.vy1),
							w.life * Random.Visual.Float(0.7f, 1.2f), w.sway);
				}
			}
		}
	}

	//one of the little things the weather carries
	public static class Drop extends Image {

		private static final HashMap<String, TextureFilm> FILMS = new HashMap<>();

		private float left, life, sway, phase;

		public Drop(){
			super();
			origin.set(4, 4);
		}

		void reset( String sheet, int kind, float x, float y, float vx, float vy, float life, float sway ){
			revive();
			texture(sheet);
			TextureFilm film = FILMS.get(sheet);
			if (film == null){
				film = new TextureFilm(texture, 8, 8);
				FILMS.put(sheet, film);
			}
			frame(film.get(kind));
			this.x = x;
			this.y = y;
			speed.set(vx, vy);
			this.life = left = life;
			this.sway = sway;
			phase = Random.Visual.Float(6.28f);
			am = 0;
		}

		@Override
		public void update() {
			super.update();
			left -= Game.elapsed;
			//gone once it blows off the edge of the map
			float s = DungeonTilemap.SIZE;
			if (left <= 0 || x < 0 || y < 0 || x > Dungeon.level.width() * s || y > Dungeon.level.height() * s){
				kill();
				return;
			}
			if (sway != 0) x += (float) Math.sin(left * 3f + phase) * sway * Game.elapsed;
			float p = left / life;
			am = p > 0.85f ? (1 - p) / 0.15f : p < 0.3f ? p / 0.3f : 1f;
		}
	}

	// *** the ground ***

	//the prints of the giant on this floor, under everything that stands
	public static class PrintTiles extends CustomTilemap {

		PrintTiles(){
			texture = GiantSteps.printSheet(Biome.current());
			setRect(0, 0, Dungeon.level.width(), Dungeon.level.height());
		}

		@Override
		public Tilemap create() {
			vis = new PrintMap(texture, tileW, tileH);
			return vis;
		}
	}

	private static class PrintMap extends Tilemap {

		private final int[] data;
		private final int w;
		private int version = -1;

		PrintMap( Object tex, int w, int h ){
			super(tex, new TextureFilm(tex, DungeonTilemap.SIZE, DungeonTilemap.SIZE));
			this.w = w;
			data = new int[w * h];
			Arrays.fill(data, -1);
			map(data, w);
		}

		@Override
		protected NoosaScript script() {
			return NoosaScript.get();
		}

		@Override
		public void update() {
			super.update();
			//the prints age with the turns: look again every so often
			int v = GiantSteps.printsVersion * 1000 + WorldEvents.clock() / 50;
			if (v == version) return;
			version = v;
			Arrays.fill(data, -1);
			int h = data.length / w;
			for (int[] p : GiantSteps.prints()){
				if (p[0] != Dungeon.depth || p[1] != Dungeon.branch) continue;
				int age = WorldEvents.clock() - p[5];
				if (age > GiantSteps.LIFE) continue;
				int frame = p[4] + (age > GiantSteps.FRESH ? 2 : 0);
				for (int dy = -1; dy <= 1; dy++){
					for (int dx = -1; dx <= 1; dx++){
						int x = p[2] + dx, y = p[3] + dy;
						if (x < 0 || y < 0 || x >= w || y >= h) continue;
						data[x + y * w] = (dy + 1) * 12 + frame * 3 + dx + 1;
					}
				}
			}
			updateMap();
		}
	}

	//the tide over the ground, with its edge where it meets the dry
	public static class TideTiles extends CustomTilemap {

		TideMap map;

		TideTiles(){
			texture = DIR + "tide.png";
			setRect(0, 0, Dungeon.level.width(), Dungeon.level.height());
			map = new TideMap(texture, tileW, tileH);
		}

		@Override
		public Tilemap create() {
			vis = map;
			return vis;
		}
	}

	static class TideMap extends Tilemap {

		private final int[] data;
		private final int w;
		private YogTide tide;
		private int reach = -1;
		private float t = 0;

		TideMap( Object tex, int w, int h ){
			super(tex, new TextureFilm(tex, DungeonTilemap.SIZE, DungeonTilemap.SIZE));
			this.w = w;
			data = new int[w * h];
			Arrays.fill(data, -1);
			map(data, w);
		}

		@Override
		protected NoosaScript script() {
			return NoosaScript.get();
		}

		void follow( YogTide e ){
			int[] d = e == null ? null : e.dist();
			int r = d == null ? -1 : e.reach;
			if (e == tide && r == reach) return;
			tide = e;
			reach = r;
			Arrays.fill(data, -1);
			if (d != null && d.length == data.length){
				int h = data.length / w;
				for (int c = 0; c < data.length; c++){
					if (!e.covers(c)) continue;
					int x = c % w, y = c / w, mask = 0;
					if (y == 0 || !e.covers(c - w)) mask |= 1;
					if (x == w - 1 || !e.covers(c + 1)) mask |= 2;
					if (y == h - 1 || !e.covers(c + w)) mask |= 4;
					if (x == 0 || !e.covers(c - 1)) mask |= 8;
					//plain, ripples, bubbles, and now and then an eye looking up
					int v = (x * 7 + y * 13) & 15;
					data[c] = mask + 16 * (v == 0 ? 3 : v % 3);
				}
			}
			updateMap();
		}

		@Override
		public void update() {
			super.update();
			t += Game.elapsed;
			alpha(0.82f + 0.1f * (float) Math.sin(t * 1.7f));
		}
	}

	// *** dust on the horizon ***

	//at the edge of the screen, the way it comes from: dust for the giant and the herds, a violet
	//glow for the tide
	private static class Horizon extends Group {

		private final Emitter dust = new Emitter();
		private int color = 0x8A7A6A;
		private float strength = 0;

		Horizon(){
			add(dust);
			dust.pour(new Emitter.Factory() {
				@Override
				public void emit( Emitter emitter, int index, float x, float y ) {
					if (Random.Visual.Float() > strength) return;
					((Puff) emitter.recycle(Puff.class)).reset(x, y, color);
				}
			}, 0.025f);
			//it comes and goes: it mustn't die off the first time it's left empty
			dust.autoKill = false;
			dust.on = false;
		}

		void point( WorldEvent e, Hero me ){
			int from = e == null ? -1 : e.origin();
			if (from == -1 || me == null){
				dust.on = false;
				return;
			}
			int w = Dungeon.level.width();
			float dx = from % w - me.pos % w, dy = from / w - me.pos / w;
			float d = (float) Math.sqrt(dx * dx + dy * dy);
			if (d < 8){
				dust.on = false;
				return;
			}
			color = e instanceof YogTide ? 0xB070E0 : e instanceof Migration ? 0xC8A878 : 0xA89C8C;
			strength = e instanceof GiantSteps ? 0.4f + 0.6f * Math.max(0.2f, e.power()) : 0.4f + 0.6f * e.power();
			//from where the hero is on the screen (the camera stops at the edges of the map)
			float uiw = PixelScene.uiCamera.width, uih = PixelScene.uiCamera.height;
			float cx = uiw / 2f, cy = uih / 2f;
			if (me.sprite != null){
				Point p = Camera.main.cameraToScreen(me.sprite.center().x, me.sprite.center().y);
				cx = GameMath.gate(20, p.x / PixelScene.uiCamera.zoom, uiw - 20);
				cy = GameMath.gate(40, p.y / PixelScene.uiCamera.zoom, uih - 40);
			}
			float left = 8, right = uiw - 8, top = 34, bottom = uih - 30;
			float t = Float.MAX_VALUE;
			if (dx > 0) t = Math.min(t, (right - cx) / dx);
			if (dx < 0) t = Math.min(t, (left - cx) / dx);
			if (dy > 0) t = Math.min(t, (bottom - cy) / dy);
			if (dy < 0) t = Math.min(t, (top - cy) / dy);
			float px = cx + dx * t, py = cy + dy * t;
			dust.pos(px - 14, py - 8, 28, 16);
			dust.on = true;
		}
	}

	public static class Puff extends PixelParticle {

		public Puff(){
			super();
		}

		void reset( float x, float y, int color ){
			reset(x, y, color, 3 + Random.Visual.Int(3), 1.8f);
			speed.set(Random.Visual.Float(-8, 8), Random.Visual.Float(-14, -4));
			am = 0;
		}

		@Override
		public void update() {
			super.update();
			float p = left / lifespan;
			am = (p > 0.8f ? (1 - p) * 5 : p) * 0.85f;
			size(size + Game.elapsed * 3f);
		}
	}

	// *** others ***

	//a beast walking off the floor (or melting away): it fades instead of dying
	public static void fadeOut( final CharSprite sprite ){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				if (sprite.parent == null){
					sprite.killAndErase();
					return;
				}
				sprite.parent.add(new AlphaTweener(sprite, 0, 0.6f) {
					@Override
					protected void onComplete() {
						sprite.killAndErase();
						parent.erase(this);
					}
				});
			}
		});
	}
}
