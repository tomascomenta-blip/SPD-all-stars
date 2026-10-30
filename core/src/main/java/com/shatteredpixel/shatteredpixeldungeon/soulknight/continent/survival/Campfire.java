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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.survival;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Light;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.DarkTaint;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.props.SKProps;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Halo;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

//a fire on the ash. while it burns it heals whoever sits by it, keeps the corruption back, cooks
//and lets you make things (WndCampfire). and every monster for a long way around knows it's there
public class Campfire extends SKProps.SKProp {

	{
		spriteClass = CampfireSprite.class;
		HP = HT = 1000;
		//Continent.glows: the heroes see it from far off in the dark, like lava
		properties.add(Property.FIERY);
	}

	public static final int FUEL = 300, MAX_FUEL = 500, TORCH_FUEL = 100;
	//turns it stays as embers after going out (a torch lights it again)
	public static final int EMBERS = 30;
	public static final int WARM = 2;
	//the light and the crackle: heard this far, this often
	public static final int NOISE = 14, NOISE_EVERY = 3;

	public int fuel = FUEL;
	public int embers = EMBERS;
	private int clock = 0;
	//where each hero was last turn, by party index: standing still by the fire is resting
	private int[] lastPos = new int[0];

	public boolean lit(){
		return fuel > 0;
	}

	public static Campfire near( int cell, int dist ){
		Campfire best = null;
		for (Mob m : Dungeon.level.mobs){
			if (!(m instanceof Campfire) || !((Campfire) m).lit()) continue;
			int d = Dungeon.level.distance(cell, m.pos);
			if (d <= dist && (best == null || d < Dungeon.level.distance(cell, best.pos))) best = (Campfire) m;
		}
		return best;
	}

	public static boolean warms( Char ch ){
		return ch instanceof Hero && Dungeon.level != null && near(ch.pos, WARM) != null;
	}

	//a free cell next to the hero with room around it: a fire in a corridor would wall it off
	public static int spotNear( int from ){
		Level l = Dungeon.level;
		int best = -1, bestOpen = -1;
		for (int n : PathFinder.NEIGHBOURS8){
			int c = from + n;
			if (!spotFree(c)) continue;
			int open = 0;
			for (int m : PathFinder.NEIGHBOURS8){
				if (l.passable[c + m] && !l.pit[c + m]) open++;
			}
			if (open >= 5 && open > bestOpen){
				best = c;
				bestOpen = open;
			}
		}
		return best;
	}

	public static boolean spotFree( int c ){
		Level l = Dungeon.level;
		return c >= 0 && c < l.length() && l.passable[c] && !l.pit[c] && !l.water[c] && !l.avoid[c]
				&& Actor.findChar(c) == null && l.heaps.get(c) == null && l.getTransition(c) == null;
	}

	//game side (CampfireKit.Building)
	public static Campfire build( int cell ){
		Campfire f = new Campfire();
		f.pos = cell;
		GameScene.add(f);
		CellEmitter.get(cell).burst(Speck.factory(Speck.SMOKE), 6);
		Sample.INSTANCE.play(Assets.Sounds.BURNING);
		CoopLog.line("survival: campfire lit at " + cell + " t=" + (int) Actor.now());
		return f;
	}

	@Override
	public boolean isInvulnerable( Class effect ) {
		return true;
	}

	@Override
	public boolean heroShouldInteract() {
		return true;
	}

	@Override
	public boolean interact( Char c ) {
		if (!(c instanceof Hero)) return true;
		final Hero h = (Hero) c;
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				GameScene.show(new WndCampfire(Campfire.this, h));
			}
		});
		return true;
	}

	@Override
	protected boolean act() {
		if (fuel > 0){
			clock++;
			warmHeroes();
			if (clock % NOISE_EVERY == 0) Senses.noise(pos, NOISE);
			if (--fuel <= 0) goOut();
		} else if (--embers <= 0){
			CoopLog.line("survival: campfire at " + pos + " is gone");
			destroy();
			if (sprite != null) sprite.killAndErase();
			return true;
		}
		spend(TICK);
		return true;
	}

	private void warmHeroes(){
		List<Hero> all = Party.active() ? Party.heroes() : Collections.singletonList(Dungeon.hero);
		if (lastPos.length != all.size()){
			lastPos = new int[all.size()];
			Arrays.fill(lastPos, -1);
		}
		for (int i = 0; i < all.size(); i++){
			Hero h = all.get(i);
			//two fires side by side don't heal twice: only the closest one counts
			if (h == null || !h.isAlive() || h.coopDown || near(h.pos, WARM) != this){
				lastPos[i] = -1;
				continue;
			}
			//the fire lights them up: they see further, and they can be seen (Senses.lit)
			Buff.prolong(h, Light.class, 2f);
			boolean resting = lastPos[i] == h.pos && h.visibleEnemies() == 0;
			lastPos[i] = h.pos;
			int heal;
			if (resting){
				heal = Math.max(1, h.HT / 40);
				if (clock % 3 == 0){
					DarkTaint t = h.buff(DarkTaint.class);
					if (t != null) t.cleanse(1);
				}
			} else {
				heal = clock % 2 == 0 ? 1 : 0;
			}
			heal = Math.round(heal / DarkTaint.regenDelay(h));
			heal = Math.min(heal, h.HT - h.HP);
			if (heal > 0){
				h.HP += heal;
				if (resting && h.sprite != null && clock % 2 == 0){
					h.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
				}
			}
		}
	}

	private void goOut(){
		embers = EMBERS;
		CoopLog.line("survival: campfire at " + pos + " went out");
		if (sprite instanceof CampfireSprite) ((CampfireSprite) sprite).lit(false);
		if (Dungeon.level.heroFOV[pos]) GLog.w(Messages.get(this, "out"));
	}

	//a torch on the fire: more time, or lit again if it's down to embers
	public void feed(){
		boolean wasOut = !lit();
		fuel = Math.min(MAX_FUEL, fuel + TORCH_FUEL);
		embers = EMBERS;
		if (wasOut && sprite instanceof CampfireSprite) ((CampfireSprite) sprite).lit(true);
		CoopLog.line("survival: campfire at " + pos + " fed, fuel " + fuel);
	}

	@Override
	public String name() {
		return Messages.get(this, "name");
	}

	@Override
	public String description() {
		return lit() ? Messages.get(this, "desc", fuel) : Messages.get(this, "desc_out");
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("fuel", fuel);
		bundle.put("embers", embers);
		bundle.put("clock", clock);
		bundle.put("last_pos", lastPos);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		fuel = bundle.getInt("fuel");
		embers = bundle.getInt("embers");
		clock = bundle.getInt("clock");
		if (bundle.contains("last_pos")) lastPos = bundle.getIntArray("last_pos");
	}

	//16x24, the flames go 8px over its cell (fogata.png: 0-3 burning, 4 embers, 5 unlit)
	public static class CampfireSprite extends MobSprite {

		private final TextureFilm film;
		private Glow glow;
		private boolean burning = true;

		public CampfireSprite(){
			super();
			texture(Survival.CAMPFIRE);
			film = new TextureFilm(texture, 16, 24);
			perspectiveRaise = 0;
			lit(true);
		}

		public void lit( boolean on ){
			burning = on;
			if (on){
				idle = new Animation(8, true);
				idle.frames(film, 0, 1, 2, 3);
			} else {
				idle = new Animation(1, true);
				idle.frames(film, 4);
			}
			run = idle.clone();
			attack = idle.clone();
			zap = idle.clone();
			die = idle.clone();
			play(idle, true);
			if (glow != null) glow.dim(!on);
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (ch instanceof Campfire) lit(((Campfire) ch).lit());
			if (glow == null){
				glow = new Glow(this);
				glow.dim(!burning);
				GameScene.effect(glow);
			}
		}

		@Override
		public void kill() {
			super.kill();
			if (glow != null){
				glow.killAndErase();
				glow = null;
			}
		}

		@Override
		public int blood() {
			return 0xFFFF8030;
		}
	}

	//the warm light around it, flickering. just embers when it's out
	public static class Glow extends Halo {

		private final CharSprite fire;
		private float phase = 0;
		private boolean dim = false;

		public Glow( CharSprite fire ){
			super(30, 0xFFA040, 0.3f);
			this.fire = fire;
		}

		public void dim( boolean value ){
			dim = value;
			hardlight(value ? 0xC03010 : 0xFFA040);
			radius(value ? 10 : 30);
		}

		@Override
		public void update() {
			super.update();
			phase += Game.elapsed;
			float base = dim ? 0.15f : 0.28f;
			am = base + 0.05f * (float) Math.sin(phase * 7) + 0.03f * (float) Math.sin(phase * 13);
			visible = fire.visible;
			point(fire.x + fire.width() / 2f, fire.y + fire.height() - 6);
		}

		@Override
		public void draw() {
			Blending.setLightMode();
			super.draw();
			Blending.setNormalMode();
		}
	}
}
