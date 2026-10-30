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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.awake.Awakening;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.survival.Survival;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

//the continent's corruption, 0-100. creeps up with time over there (Survival.taintEvery) and with every yog hit.
//25: wounds close slower / 50: you see less and get hungry faster / 75: less max hp /
//100: you're prey, every yog comes for you and it eats you slowly until a shrine cleans it
public class DarkTaint extends Buff {

	{
		type = buffType.NEGATIVE;
		announced = false;
		revivePersists = true;
	}

	public int level = 0;
	private int clock = 0;

	public static int of( Char ch ){
		DarkTaint t = ch == null ? null : ch.buff(DarkTaint.class);
		return t == null ? 0 : t.level;
	}

	public static int step( Char ch ){
		return of(ch) / 25;
	}

	//Regeneration: turns between points, multiplied
	public static float regenDelay( Char ch ){
		return step(ch) >= 1 ? 2f : 1f;
	}

	public static float hungerFactor( Char ch ){
		return step(ch) >= 2 ? 1.5f : 1f;
	}

	public static int sightPenalty( Char ch ){
		return step(ch) >= 2 ? 1 : 0;
	}

	public static float htFactor( Char ch ){
		return step(ch) >= 3 ? 0.8f : 1f;
	}

	public void add( int amount ){
		int before = level / 25;
		level = Math.min(100, level + amount);
		stepChanged(before);
	}

	public void cleanse( int amount ){
		int before = level / 25;
		level = Math.max(0, level - amount);
		stepChanged(before);
	}

	private void stepChanged( int before ){
		int now = level / 25;
		if (now == before) return;
		if (target instanceof Hero){
			((Hero) target).updateHT(false);
			if (now > before && target.sprite != null){
				target.sprite.showStatus(CharSprite.NEGATIVE, Messages.get(this, "step_" + now));
			}
			if (now > before && target == Dungeon.local()){
				GLog.n(Messages.get(this, "warn_" + now));
			}
		}
		BuffIndicator.refreshHero();
	}

	@Override
	public boolean act() {
		if (Continent.in()){
			if (target instanceof Hero) target.viewDistance = Continent.vision((Hero) target);
			//the place itself: slowly up on the surface, fast down below, not at all by a fire
			int every = Awakening.taintEvery(Survival.taintEvery(target));
			if (every > 0 && ++clock >= every){
				clock = 0;
				add(1);
			}
			if (level >= 100){
				//prey: it eats you and they all know where you are
				int dmg = Math.max(1, target.HT / 100);
				target.damage(dmg, this);
				if (clock % 4 == 0){
					for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
						if (m instanceof Yog && m.alignment == Char.Alignment.ENEMY) m.beckon(target.pos);
					}
				}
			}
		} else if (level > 0 && ++clock >= 20){
			//back home it fades, very slowly
			clock = 0;
			cleanse(1);
		}
		if (level <= 0 && !Continent.in()){
			detach();
			return true;
		}
		spend(TICK);
		return true;
	}

	@Override
	public int icon() {
		return level >= 25 ? BuffIndicator.CORRUPT : BuffIndicator.NONE;
	}

	@Override
	public void tintIcon( Image icon ) {
		icon.hardlight(level >= 100 ? 0xFF2040 : level >= 75 ? 0xC040FF : 0x8A50C8);
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, (100 - level) / 100f);
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(level);
	}

	@Override
	public String name() {
		return Messages.get(this, "name");
	}

	@Override
	public String desc() {
		String d = Messages.get(this, "desc", level);
		for (int s = 1; s <= 4; s++){
			if (level >= s * 25) d += "\n\n" + Messages.get(this, "effect_" + s);
		}
		return d;
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("level", level);
		bundle.put("clock", clock);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		level = bundle.getInt("level");
		clock = bundle.getInt("clock");
	}
}
