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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.spiritist;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAlly;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

//spiritist ghost. floaty, follows the owner, fights for a few turns. boo
public class Ghost extends SKAlly {

	{
		spriteClass = Sprite.class;
		HP = HT = 8;
		defenseSkill = 10;
		flying = true;
		properties.add(Property.UNDEAD);
		properties.add(Property.INORGANIC);
	}

	public float dmgMulti = 1f;
	public int wailing = 0;

	public static Ghost make( Hero owner, int cell ){
		Ghost g = new Ghost();
		g.setOwner(owner);
		float hp = 1f;
		if (owner.subClass == HeroSubClass.SPECTRE_LORD){
			hp += 0.25f + 0.2f*owner.pointsInTalent(Talent.SL_SPECTRAL_HIDE);
			g.wailing = owner.pointsInTalent(Talent.SL_WAILING);
		}
		g.HP = g.HT = Math.round((6 + 2*owner.lvl) * hp);
		g.defenseSkill = 8 + owner.lvl;
		g.dmgMulti = 1f + 0.2f*owner.pointsInTalent(Talent.SP_GHOSTLY_GRASP);
		g.timeLeft = duration(owner);
		g.pos = cell;
		return g;
	}

	public static int duration( Hero owner ){
		int t = 15;
		if (owner.subClass == HeroSubClass.SPECTRE_LORD) t += 5 + 5*owner.pointsInTalent(Talent.SL_ETHEREAL);
		return t;
	}

	public static int minDmg( int power ){
		return 1 + power/4;
	}

	public static int maxDmg( int power ){
		return 4 + power/2;
	}

	@Override
	public int attackSkill( Char target ) {
		return 12 + power;
	}

	@Override
	public int damageRoll() {
		return Math.round(Random.NormalIntRange(minDmg(power), maxDmg(power)) * dmgMulti);
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		//wailing talent, may terrify
		if (wailing > 0 && enemy.isAlive() && Random.Int(10) < wailing){
			Buff.affect(enemy, Terror.class, 2f).object = id();
		}
		return super.attackProc(enemy, damage);
	}

	@Override
	public void die( Object cause ) {
		if (timeLeft != 0) SpiritistHooks.onSummonDeath(owner());
		super.die(cause);
	}

	@Override
	public String description() {
		String d = Messages.get(this, "desc", Math.round(minDmg(power)*dmgMulti), Math.round(maxDmg(power)*dmgMulti));
		if (timeLeft >= 0) d += "\n\n" + Messages.get(this, "desc_time", Math.round(timeLeft));
		return d;
	}

	private static final String DMG  = "dmg";
	private static final String WAIL = "wail";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(DMG, dmgMulti);
		bundle.put(WAIL, wailing);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		dmgMulti = bundle.getFloat(DMG);
		wailing = bundle.getInt(WAIL);
		defenseSkill = 8 + power;
	}

	public static class Sprite extends SKMobSprite {
		public Sprite(){
			super();
			setup(SKAssets.GHOST, 14, 16);
		}

		@Override
		public int blood() {
			return 0xFF99EEDD;
		}
	}
}
