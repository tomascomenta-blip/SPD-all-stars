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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.gunslinger;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMarks;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

//Q: spin the cylinder and dump it into everything in sight. bounty first, then closest
public class GunSpin extends ChampionAbility {

	public static final GunSpin INSTANCE = new GunSpin();

	@Override public String slot() { return "Q"; }
	@Override public int unlockLevel() { return 1; }
	@Override public int icon() { return HeroIcon.GS_SPIN; }
	@Override public int color() { return 0xDDAA44; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }

	public static final int RANGE = 6;

	@Override
	public float cooldown( Hero hero ) {
		return 12 - 2*hero.pointsInTalent(Talent.GS_FAST_RELOAD);
	}

	public static int shots( Hero hero ){
		int s = 3 + hero.pointsInTalent(Talent.GS_DEADEYE);
		if (hero.subClass == HeroSubClass.QUICKDRAW) s += 2;
		return s;
	}

	public static int minDmg( Hero hero ){
		return 2 + hero.lvl/3;
	}

	public static int maxDmg( Hero hero ){
		return 5 + hero.lvl/2;
	}

	//clear shot only. bounty, then distance
	public static ArrayList<Char> targets( final Hero hero, int range ){
		ArrayList<Char> found = new ArrayList<>();
		for (Char ch : Actor.chars()){
			if (ch.alignment != Char.Alignment.ENEMY || !ch.isAlive() || !Dungeon.level.heroFOV[ch.pos]) continue;
			if (Dungeon.level.distance(hero.pos, ch.pos) > range) continue;
			if (new Ballistica(hero.pos, ch.pos, Ballistica.PROJECTILE).collisionPos != ch.pos) continue;
			found.add(ch);
		}
		Collections.sort(found, new Comparator<Char>() {
			@Override
			public int compare( Char a, Char b ) {
				boolean ba = a.buff(BountyTag.Mark.class) != null, bb = b.buff(BountyTag.Mark.class) != null;
				if (ba != bb) return ba ? -1 : 1;
				int d = Dungeon.level.distance(hero.pos, a.pos) - Dungeon.level.distance(hero.pos, b.pos);
				return d != 0 ? d : a.pos - b.pos;
			}
		});
		return found;
	}

	public static void shoot( Hero hero, Char ch, int min, int max, Object src ){
		float dmg = Random.NormalIntRange(min, max) * GunslingerHooks.shotFactor(hero);
		SKMarks.shotFx(hero, ch.pos, ItemSpriteSheet.SHOT_BULLET);
		ch.damage(Math.max(1, Math.round(dmg) - ch.drRoll()/2), src);
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		ArrayList<Char> foes = targets(hero, RANGE);
		if (foes.isEmpty()){
			GLog.w(Messages.get(this, "no_target"));
			return;
		}
		int ricochet = hero.pointsInTalent(Talent.GS_RICOCHET);
		int n = shots(hero);
		for (int i = 0; i < n; i++){
			Char ch = foes.get(i % foes.size());
			if (!ch.isAlive()) continue;
			shoot(hero, ch, minDmg(hero), maxDmg(hero), this);
			//ricochet, 15% per point
			if (ricochet > 0 && foes.size() > 1 && Random.Int(100) < 15*ricochet){
				Char other = foes.get((i+1) % foes.size());
				if (other.isAlive()) shoot(hero, other, minDmg(hero), maxDmg(hero), this);
			}
		}
		if (hero.sprite != null) hero.sprite.turnTo(hero.pos, foes.get(0).pos);

		ChampionHooks.operateVisual(hero, foes.get(0).pos);
		Invisibility.dispel(hero);
		startCooldown(hero);
		hero.spendAndNext(1f);
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", shots(hero), RANGE, minDmg(hero), maxDmg(hero));
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}
}
