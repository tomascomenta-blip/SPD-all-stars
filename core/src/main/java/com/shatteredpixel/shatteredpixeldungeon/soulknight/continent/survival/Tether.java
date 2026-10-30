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

import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundle;

//co-op only, over there: whoever wanders off more than a screen from the nearest friend gets
//swallowed by the dark (sight 1, corruption creeping in) and after a few turns something comes
//for them (Stalker). back with the others it all goes away
public class Tether extends Buff {

	{
		type = buffType.NEGATIVE;
		announced = false;
		revivePersists = true;
	}

	public static final int LEASH = 12;
	//back with the group a bit inside the edge, so it doesn't flicker right on the line
	public static final int BACK = 10;
	public static final int HUNT_AFTER = 6;
	public static final int TAINT_EVERY = 3;
	//a beaten stalker comes back after this long if you're still alone
	public static final int RESPAWN = 12;

	//testing (-Dspd.test.tether=1 in a solo run): the way in stands for the rest of the party
	private static final boolean SOLO_TEST = DevFlags.tetherTest();

	public static boolean applies(){
		return Party.active() || SOLO_TEST;
	}

	public boolean lost = false;
	public int away = 0;
	private int cooldown = 0;

	private Emitter smoke;

	public static boolean lost( Char ch ){
		Tether t = ch == null ? null : ch.buff(Tether.class);
		return t != null && t.lost;
	}

	//to the nearest friend still standing, -1 if there's nobody else
	public static int nearestFriend( Hero h ){
		if (!Party.active()) return SOLO_TEST ? Dungeon.level.distance(h.pos, Dungeon.level.entrance()) : -1;
		int best = -1;
		for (Hero o : Party.living()){
			if (o == h) continue;
			int d = Dungeon.level.distance(h.pos, o.pos);
			if (best == -1 || d < best) best = d;
		}
		return best;
	}

	@Override
	public boolean act() {
		if (!Continent.in() || !applies()){
			setLost(false);
			detach();
			return true;
		}
		Hero h = (Hero) target;
		int d = h.isAlive() && !h.coopDown ? nearestFriend(h) : -1;
		if (lost && (d == -1 || d <= BACK)){
			setLost(false);
		} else if (!lost && d > LEASH){
			setLost(true);
		}
		if (lost){
			away++;
			if (away % TAINT_EVERY == 0) Continent.taint(h, 1);
			if (Stalker.of(h) == null){
				if (cooldown > 0){
					cooldown--;
				} else if (away >= HUNT_AFTER && Stalker.spawn(h)){
					cooldown = RESPAWN;
				}
			}
		}
		spend(TICK);
		return true;
	}

	private void setLost( boolean on ){
		if (lost == on) return;
		lost = on;
		away = 0;
		Hero h = (Hero) target;
		CoopLog.line("tether: hero " + Party.indexOf(h) + (on ? " alone in the dark" : " back with the group")
				+ " at " + h.pos + " t=" + (int) now());
		if (on){
			if (h.sprite != null) h.sprite.showStatus(CharSprite.NEGATIVE, Messages.get(this, "alone"));
			if (h == Dungeon.local()) GLog.n(Messages.get(this, "lost"));
			else GLog.w(Messages.get(this, "friend_lost", Party.name(h)));
		} else {
			cooldown = 0;
			if (h == Dungeon.local()) GLog.p(Messages.get(this, "back"));
		}
		fx(on);
		h.viewDistance = Continent.vision(h);
		Dungeon.observe();
		BuffIndicator.refreshHero();
	}

	@Override
	public void fx( boolean on ) {
		if (target.sprite == null) return;
		if (on && lost){
			target.sprite.add(CharSprite.State.DARKENED);
			if (smoke == null){
				smoke = target.sprite.emitter();
				smoke.pour(ShadowParticle.UP, 0.12f);
			}
		} else {
			target.sprite.remove(CharSprite.State.DARKENED);
			if (smoke != null){
				smoke.on = false;
				smoke = null;
			}
		}
	}

	@Override
	public int icon() {
		return lost ? BuffIndicator.BLINDNESS : BuffIndicator.NONE;
	}

	@Override
	public void tintIcon( Image icon ) {
		icon.hardlight(0x9A60E0);
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(away);
	}

	@Override
	public String name() {
		return Messages.get(this, "name");
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", LEASH, away);
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("lost", lost);
		bundle.put("away", away);
		bundle.put("cooldown", cooldown);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		lost = bundle.getBoolean("lost");
		away = bundle.getInt("away");
		cooldown = bundle.getInt("cooldown");
	}
}
