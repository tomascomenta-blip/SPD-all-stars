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

package com.shatteredpixel.shatteredpixeldungeon.champions.bard;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LostInventory;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Callback;

import java.util.ArrayList;

//shared stuff for the 4 bard songs: targets, power (fortissimo/duet), end-of-song stuff
//(applause/harmony/dissonance). use the hero you're given, not the local one, coop!!
public abstract class BardSong extends ChampionAbility {

	//party heroes (standing) + allies
	public static boolean isFriend( Char ch ){
		if (ch == null || !ch.isAlive() || ch.alignment != Char.Alignment.ALLY) return false;
		return !(ch instanceof Hero) || !((Hero) ch).coopDown;
	}

	//bard first, then actor order. order matters for coop
	public static ArrayList<Char> friendsWithin( Hero hero, int radius ){
		ArrayList<Char> result = new ArrayList<>();
		result.add(hero);
		for (Char ch : Actor.chars()){
			if (ch == hero || !isFriend(ch)) continue;
			if (Dungeon.level.distance(hero.pos, ch.pos) <= radius) result.add(ch);
		}
		return result;
	}

	public static ArrayList<Char> friendsInView( Hero hero ){
		ArrayList<Char> result = new ArrayList<>();
		result.add(hero);
		for (Char ch : Actor.chars()){
			if (ch == hero || !isFriend(ch)) continue;
			if (ch.pos >= 0 && ch.pos < Dungeon.level.length() && Dungeon.level.heroFOV[ch.pos]) result.add(ch);
		}
		return result;
	}

	public static ArrayList<Char> enemiesInView( int pos, int radius ){
		ArrayList<Char> result = new ArrayList<>();
		for (Char ch : ChampionHooks.enemiesAround(pos, radius)){
			if (Dungeon.level.heroFOV[ch.pos]) result.add(ch);
		}
		return result;
	}

	public static float power( Hero hero, boolean forte, boolean duet ){
		float p = 1f;
		if (forte) p += Refrain.fortePower(hero);
		if (duet) p += 0.15f*hero.pointsInTalent(Talent.BAR_DUET);
		return p;
	}

	//singing to yourself doesn't count
	public static boolean duet( ArrayList<Char> reached ){
		return reached.size() > 1;
	}

	//minstrel / dissonance

	public static int dissonanceDamage( Hero hero ){
		if (hero.subClass != HeroSubClass.MINSTREL) return 0;
		return hero.pointsInTalent(Talent.MI_DISSONANCE) * (1 + hero.lvl/4);
	}

	public static void dissonance( Hero hero, ArrayList<Char> enemies, Object src ){
		int dmg = dissonanceDamage(hero);
		if (dmg <= 0) return;
		for (Char ch : enemies){
			if (!ch.isAlive() || ch.alignment != Char.Alignment.ENEMY) continue;
			ch.damage(dmg, src);
			if (ch.sprite != null && ch.isAlive()) ch.sprite.emitter().burst(Speck.factory(Speck.NOTE), 3);
		}
	}

	//troubadour / harmony (armonía)
	//cleanse oldest debuffs first, returns how many
	public static int cleanse( Char ch, int count ){
		int removed = 0;
		for (Buff b : ch.buffs()){
			if (removed >= count) break;
			if (b.type != Buff.buffType.NEGATIVE || b instanceof LostInventory || b instanceof AllyBuff) continue;
			b.detach();
			removed++;
		}
		return removed;
	}

	public static int harmonyCount( Hero hero ){
		if (hero.subClass != HeroSubClass.TROUBADOUR) return 0;
		int p = hero.pointsInTalent(Talent.TB_HARMONY);
		return p >= 3 ? 99 : p;
	}

	public static void harmony( Hero hero, Char ch ){
		int n = harmonyCount(hero);
		if (n > 0) cleanse(ch, n);
	}

	public static void notes( Char ch ){
		if (ch.sprite != null && ch.sprite.visible) ch.sprite.centerEmitter().start(Speck.factory(Speck.NOTE), 0.3f, 4);
	}

	//just the gesture. the empty callback stops onOperateComplete() from messing w/ curAction
	//at some random screen-dependent moment (= coop desync). same trick as operateVisual
	public static void gesture( final Hero hero, int cell, boolean zap ){
		if (hero.sprite == null) return;
		Callback idle = new Callback() {
			@Override
			public void call() {
				hero.sprite.idle();
			}
		};
		if (zap) hero.sprite.zap(cell, idle);
		else     hero.sprite.operate(cell, idle);
	}

	//applause: heal per friend reached, then the normal ability ending
	protected void finish( Hero hero, int reached ){
		int applause = reached * hero.pointsInTalent(Talent.BAR_APPLAUSE);
		if (applause > 0) ChampionHooks.heal(hero, applause);
		Invisibility.dispel(hero);
		startCooldown(hero);
		hero.spendAndNext(1f);
	}

	//fortissimo desc line
	protected String forteDesc( Hero hero, Object... args ){
		String d = "\n\n" + Messages.get(this, "desc_forte", args);
		if (Refrain.charged(hero)) d += " " + Messages.get(BardSong.class, "forte_ready");
		return d;
	}

	protected static int fortePercent( Hero hero ){
		return Math.round(100*Refrain.fortePower(hero));
	}
}
