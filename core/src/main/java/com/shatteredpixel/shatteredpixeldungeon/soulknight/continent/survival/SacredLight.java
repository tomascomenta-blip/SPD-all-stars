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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Light;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Yog;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.big.Colossus;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Halo;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundle;

//a lit sacred torch in someone's hand. 24x24 cells of light around them, nothing comes closer than
//a few steps and the yogs run from it. it burns down, and the drop of essence goes with it
public class SacredLight extends Buff {

	{
		type = buffType.POSITIVE;
		announced = true;
	}

	public static final int DURATION = 200;
	//sight around the bearer: 24x24
	public static final int RADIUS = 12;
	//the rest keep this far, the yogs run from further
	public static final int KEEP_OFF = 3, YOG_RUN = 8;

	public int left = 0;

	private Glow glow;
	private Emitter sparks;

	public void set( int turns ){
		left = Math.max(left, turns);
	}

	@Override
	public boolean act() {
		if (!target.isAlive()){
			detach();
			return true;
		}
		Buff.prolong(target, Light.class, 2f);
		see();
		keepOff();
		if (--left <= 0){
			CoopLog.line("survival: sacred torch of hero " + Party.indexOf((Hero) target) + " burned out");
			GLog.w(Messages.get(this, "out"));
			detach();
			see();
			Dungeon.observe();
			return true;
		}
		spend(TICK);
		return true;
	}

	//over there the continent decides how far a hero sees (Survival.sight); back home the Light does
	public void see(){
		if (target instanceof Hero && Continent.in()) target.viewDistance = Continent.vision((Hero) target);
	}

	private void keepOff(){
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (!m.isAlive() || m.alignment != Char.Alignment.ENEMY || Char.hasProp(m, Char.Property.BOSS)) continue;
			int d = m instanceof Colossus ? ((Colossus) m).reach(target.pos) : Dungeon.level.distance(m.pos, target.pos);
			boolean yog = m instanceof Yog;
			if (d <= KEEP_OFF || yog && d <= YOG_RUN){
				Terror t = Buff.prolong(m, Terror.class, yog ? 4f : 2f);
				t.object = target.id();
			}
		}
	}

	@Override
	public void fx( boolean on ) {
		if (target.sprite == null) return;
		if (on){
			if (glow == null){
				glow = new Glow(target.sprite);
				GameScene.effect(glow);
			}
			if (sparks == null){
				sparks = target.sprite.emitter();
				sparks.pour(Speck.factory(Speck.LIGHT), 0.6f);
			}
		} else {
			if (glow != null){
				glow.putOut();
				glow = null;
			}
			if (sparks != null){
				sparks.on = false;
				sparks = null;
			}
		}
	}

	@Override
	public int icon() {
		return BuffIndicator.LIGHT;
	}

	@Override
	public void tintIcon( Image icon ) {
		icon.hardlight(0xFFF4C8);
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, (DURATION - left) / (float) DURATION);
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(left);
	}

	@Override
	public String name() {
		return Messages.get(this, "name");
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", left);
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("left", left);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		left = bundle.getInt("left");
	}

	//white and gold, wide and soft, breathing slowly
	public static class Glow extends Halo {

		private final CharSprite target;
		private float phase = 0, fade = 0;
		private boolean out = false;

		public Glow( CharSprite target ){
			super(56, 0xFFF0C0, 0.25f);
			this.target = target;
			am = 0;
		}

		public void putOut(){
			out = true;
		}

		@Override
		public void update() {
			super.update();
			phase += Game.elapsed;
			if (out){
				fade -= Game.elapsed;
				if (fade <= 0){
					killAndErase();
					return;
				}
			} else {
				fade = Math.min(1, fade + Game.elapsed);
			}
			am = fade * (0.22f + 0.04f * (float) Math.sin(phase * 2));
			visible = target.visible;
			point(target.x + target.width() / 2f, target.y + target.height() / 2f);
		}

		@Override
		public void draw() {
			Blending.setLightMode();
			super.draw();
			Blending.setNormalMode();
		}
	}
}
