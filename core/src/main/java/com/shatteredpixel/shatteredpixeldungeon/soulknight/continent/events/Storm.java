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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.hunt.Hunt;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.survival.Campfire;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.survival.SacredLight;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Biome;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.OpenWorld;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

//the weather of the biome, on its surface. every storm is the same deal at heart: you see less, but
//you're harder to make out too, and the wind covers your noise. each biome adds its own turn of it:
//the cold of the moor, the blood rain that washes trails away, the soot that hides every glow, the
//bone dust that's thicker than any, the sea mist that carries sound further instead
public class Storm extends WorldEvent {

	static final int WARN = 0, BLOW = 1, WANE = 2;

	public static final int NONE = 0, COLD = 1, SCENTLESS = 2, NOGLOW = 3;

	//by zone (Continent.Z_ASH first): cells of sight lost, cells less the beasts make you out from,
	//how far noise carries (percent), and the turn of its own
	private static final int[][] KINDS = {
			{1, 2, 50, NONE},
			{1, 2, 60, SCENTLESS},
			{1, 2, 60, NOGLOW},
			{1, 2, 50, COLD},
			{2, 3, 70, NONE},
			{2, 2, 140, NONE}
	};

	private static final int WARN_TURNS = 10, WANE_TURNS = 10;

	public int zone;
	int phase = WARN, timer;
	private transient boolean coldSaid = false;

	@Override
	public int kind() {
		return WorldEvents.STORM;
	}

	@Override
	boolean begin( int layer ) {
		if (layer != OpenWorld.SURFACE) return false;
		zone = Biome.current().zone;
		timer = WARN_TURNS;
		GLog.w(Messages.get(this, "warn_" + key()));
		card++;
		return true;
	}

	public String key(){
		return Biome.zone(zone).key;
	}

	private int[] stats(){
		return KINDS[Math.max(0, Math.min(KINDS.length - 1, zone - 1))];
	}

	public boolean raging(){
		return phase == BLOW;
	}

	public int sightCut(){
		return stats()[0];
	}

	public int hideCut(){
		return stats()[1];
	}

	public int noise(){
		return stats()[2];
	}

	public int twist(){
		return stats()[3];
	}

	@Override
	boolean act() {
		age++;
		switch (phase){
			case WARN:
				if (calming) return false;
				if (--timer <= 0){
					phase = BLOW;
					timer = 70 + Random.Int(40);
					GLog.n(Messages.get(this, "start_" + key()));
					card++;
				}
				break;
			case BLOW:
				if (twist() == COLD) cold();
				if (calming) timer = 0;
				if (--timer <= 0){
					phase = WANE;
					timer = WANE_TURNS;
					GLog.i(Messages.get(this, "end_" + key()));
				}
				break;
			default:
				if (--timer <= 0) return false;
				break;
		}
		return true;
	}

	//out in the blizzard, away from a fire, a tamed beast or the sacred light, the cold slows you down
	private void cold(){
		for (Hero h : Party.living()){
			if (Campfire.warms(h) || Hunt.sheltered(h) || h.buff(SacredLight.class) != null) continue;
			Buff.prolong(h, Chill.class, 2f);
			if (h == Dungeon.local() && !coldSaid){
				coldSaid = true;
				GLog.w(Messages.get(this, "cold"));
			}
		}
	}

	// *** for the screens ***

	@Override
	public float power() {
		switch (phase){
			case WARN: return 0.6f * (WARN_TURNS - timer) / WARN_TURNS;
			case BLOW: return 1f;
			default:   return Math.max(0, timer / (float) WANE_TURNS);
		}
	}

	@Override
	public String title() {
		return Messages.get(this, "name_" + key());
	}

	@Override
	public String line() {
		return Messages.get(this, (phase == WARN ? "card_warn_" : "card_") + key());
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("zone", zone);
		bundle.put("phase", phase);
		bundle.put("timer", timer);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		zone = bundle.getInt("zone");
		phase = bundle.getInt("phase");
		timer = bundle.getInt("timer");
	}
}
