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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.yog;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.Beam;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentMob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Yog;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

//a tongue of yog in a puddle of corruption. doesn't move: whoever walks within 2 cells gets
//yanked next to it and held there, soaking up corruption, until somebody cuts it
public class YogTongue extends ContinentMob implements Yog {

	{
		spriteClass = TongueSprite.class;

		HP = HT = 90;
		defenseSkill = 30;
		EXP = 14;

		minDmg = 22; maxDmg = 30; acc = 58;
		armorMin = 10; armorMax = 22;

		properties.add(Property.DEMONIC);
		properties.add(Property.IMMOVABLE);

		state = WANDERING;
	}

	private static final int REACH = 2;
	//corruption per turn of holding someone, on top of the lashes
	private static final int SOAK = 2;

	@Override
	public int taintOnHit() {
		return 2;
	}

	private int held = -1;
	private int regrab = 0;
	private boolean out = false;

	@Override
	protected boolean getCloser( int target ) {
		return false;
	}

	@Override
	protected boolean getFurther( int target ) {
		return false;
	}

	@Override
	protected boolean act() {
		if (regrab > 0) regrab--;

		Char victim = held == -1 ? null : (Char) Actor.findById(held);
		if (victim != null && victim.isAlive() && Dungeon.level.adjacent(pos, victim.pos)
				&& victim.buff(Grip.class) != null){
			Continent.taint(victim, SOAK);
		} else if (held != -1){
			letGo();
		}

		boolean near = false;
		for (Hero h : Party.living()){
			if (Dungeon.level.distance(pos, h.pos) <= REACH + 1) near = true;
		}
		if (near != out){
			out = near;
			if (sprite instanceof TongueSprite) ((TongueSprite) sprite).out(out);
		}

		if (held == -1 && regrab <= 0 && paralysed == 0){
			Hero prey = prey();
			if (prey != null && grab(prey)){
				spend(TICK);
				return true;
			}
		}
		return super.act();
	}

	//closest hero in reach, fixed order on ties
	private Hero prey(){
		Hero best = null;
		for (Hero h : Party.living()){
			if (h.invisible > 0 || h.buff(Grip.class) != null) continue;
			int d = Dungeon.level.distance(pos, h.pos);
			if (d > REACH || new Ballistica(pos, h.pos, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID).collisionPos != h.pos) continue;
			if (best == null || d < Dungeon.level.distance(pos, best.pos)) best = h;
		}
		return best;
	}

	private boolean grab( Hero h ){
		int land = h.pos;
		if (!Dungeon.level.adjacent(pos, h.pos)){
			land = -1;
			for (int c : freeAround(pos)){
				if (!Dungeon.level.adjacent(c, h.pos) || Dungeon.level.avoid[c]) continue;
				if (land == -1 || Dungeon.level.trueDistance(c, h.pos) < Dungeon.level.trueDistance(land, h.pos)) land = c;
			}
			if (land == -1) return false;
		}
		if (sprite != null && sprite.parent != null && h.sprite != null){
			Beam lash = new Beam.HealthRay(sprite.center(), h.sprite.center());
			lash.hardlight(0xA03070);
			sprite.parent.add(lash);
			sprite.turnTo(pos, h.pos);
		}
		if (land != h.pos){
			int from = h.pos;
			h.pos = land;
			Actor.add(new Pushing(h, from, land));
			Dungeon.level.occupyCell(h);
		}
		if (!h.isAlive()) return true;
		Grip g = Buff.affect(h, Grip.class);
		g.tongue = id();
		held = h.id();
		enemy = h;
		state = HUNTING;
		h.interrupt();
		if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.CHAINS, 1f, 0.7f);
		if (h == Dungeon.local()) GLog.n(Messages.get(this, "grabbed"));
		return true;
	}

	private void letGo(){
		Char victim = held == -1 ? null : (Char) Actor.findById(held);
		held = -1;
		regrab = 3;
		if (victim != null){
			Grip g = victim.buff(Grip.class);
			if (g != null && g.tongue == id()) g.detach();
		}
	}

	@Override
	public void die( Object cause ) {
		letGo();
		super.die(cause);
	}

	@Override
	public void destroy() {
		if (held != -1) letGo();
		super.destroy();
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("held", held);
		bundle.put("regrab", regrab);
		bundle.put("out", out);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		held = bundle.getInt("held");
		regrab = bundle.getInt("regrab");
		out = bundle.getBoolean("out");
	}

	//held by a tongue: can't walk away. it lets go by itself if the tongue's gone or you're not next to it
	public static class Grip extends Buff {

		{
			type = buffType.NEGATIVE;
			announced = true;
		}

		public int tongue = -1;

		@Override
		public boolean attachTo( Char target ) {
			if (super.attachTo(target)){
				target.rooted = true;
				return true;
			}
			return false;
		}

		@Override
		public boolean act() {
			Actor a = Actor.findById(tongue);
			if (!(a instanceof YogTongue) || !((YogTongue) a).isAlive()
					|| !Dungeon.level.adjacent(((YogTongue) a).pos, target.pos)){
				detach();
				return true;
			}
			spend(TICK);
			return true;
		}

		@Override
		public void detach() {
			super.detach();
			if (target.buff(Roots.class) == null) target.rooted = false;
		}

		@Override
		public int icon() {
			return BuffIndicator.ROOTS;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(0xC040A0);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc");
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("tongue", tongue);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			tongue = bundle.getInt("tongue");
		}
	}

	public static class TongueSprite extends ContinentMobSprite {

		private Animation lashing, puddle;

		public TongueSprite(){
			super();
			setup(ContinentAssets.YOG_TONGUE, 16, 24);
			lashing = idle;
			puddle = loop(1, 12);
			idle = puddle;
			play(idle);
		}

		public void out( boolean out ){
			look(out ? lashing : puddle);
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (ch instanceof YogTongue && ((YogTongue) ch).out) out(true);
		}
	}
}
