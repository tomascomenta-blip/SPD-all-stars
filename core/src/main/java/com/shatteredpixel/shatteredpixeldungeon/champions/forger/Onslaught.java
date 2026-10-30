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

package com.shatteredpixel.shatteredpixeldungeon.champions.forger;

import com.shatteredpixel.shatteredpixeldungeon.coop.CoopRules;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

//E: charge up to 4 cells, shoves whatever's in the way. hit a wall/pillar/unpushable -> shockwave
//(dmg + knockdown around, breaks adjacent pillars). small shield after, for the effort
public class Onslaught extends ChampionAbility {

	public static final Onslaught INSTANCE = new Onslaught();

	@Override public String slot() { return "E"; }
	@Override public int unlockLevel() { return 4; }
	@Override public int icon() { return HeroIcon.FORGER_CHARGE; }
	@Override public int color() { return 0xCC6633; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }
	@Override public boolean targeted() { return true; }

	public static final int WAVE_RADIUS = 2;

	@Override
	public float cooldown( Hero hero ) {
		return 18;
	}

	public static int range( Hero hero ){
		return 4 + hero.pointsInTalent(Talent.FOR_LONG_CHARGE);
	}

	public static int minDmg( Hero hero ){
		return 2 + hero.lvl/3;
	}

	public static int maxDmg( Hero hero ){
		return 6 + Math.round(hero.lvl*0.8f);
	}

	public static int waveMin( Hero hero ){
		return 1 + hero.lvl/4;
	}

	public static int waveMax( Hero hero ){
		return 4 + Math.round(hero.lvl*0.7f);
	}

	public static int shield( Hero hero ){
		return 2 + hero.lvl/3;
	}

	//living mountain talent. "seconds" = turns, whatever
	public static int quakeTurns( Hero hero ){
		if (hero.subClass != HeroSubClass.LIVINGMOUNTAIN) return 0;
		return 3*hero.pointsInTalent(Talent.LM_QUAKE) + (hero.hasTalent(Talent.LM_QUAKE) ? 3 : 0);
	}

	private static boolean blocked( int c ){
		return Dungeon.level.solid[c] || !Dungeon.level.passable[c];
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		if (target == null || target == hero.pos) return;
		Ballistica aim = new Ballistica(hero.pos, target, Ballistica.WONT_STOP);
		int max = range(hero);

		int pos = hero.pos;
		int crashCell = -1;
		ArrayList<Integer> hit = new ArrayList<>();
		for (int i = 1; i <= max && i < aim.path.size(); i++){
			int c = aim.path.get(i);
			if (blocked(c)){
				crashCell = c;
				break;
			}
			if (Dungeon.level.pit[c]) break; //don't charge into a pit. learned that one
			Char ch = Actor.findChar(c);
			if (ch != null){
				if (!CoopRules.hits(hero, ch) || !ch.isAlive()) break;
				if (!hit.contains(ch.id())){
					hit.add(ch.id());
					ch.damage(Hero.heroDamageIntRange(minDmg(hero), maxDmg(hero)), this);
				}
				if (ch.isAlive()){
					//shove forward if there's room
					int next = i + 1 < aim.path.size() ? aim.path.get(i + 1) : -1;
					if (next == -1 || blocked(next) || Actor.findChar(next) != null || Char.hasProp(ch, Char.Property.IMMOVABLE)){
						crashCell = next == -1 ? c : next;
						break;
					}
					ChampionHooks.moveChar(ch, next);
				}
			}
			pos = c;
		}

		if (pos == hero.pos && crashCell == -1){
			GLog.w(Messages.get(this, "no_room"));
			return;
		}

		if (pos != hero.pos) ChampionHooks.moveChar(hero, pos);
		ChampionHooks.operateVisual(hero, target);
		Sample.INSTANCE.play(Assets.Sounds.HIT_CRUSH, 1f, 0.7f);
		ForgerHooks.shield(hero, shield(hero));

		if (crashCell != -1) crash(hero, pos, crashCell);

		Invisibility.dispel(hero);
		startCooldown(hero);
		hero.spendAndNext(1f);
	}

	private void crash( Hero hero, int pos, int crashCell ){
		boolean pillars = false;
		if (MagmaPillar.shatter(crashCell)) pillars = true;
		for (int n : PathFinder.NEIGHBOURS8){
			if (MagmaPillar.shatter(pos + n)) pillars = true;
		}
		float factor = pillars ? 1f + 0.2f*hero.pointsInTalent(Talent.FOR_FIRE_PILLARS) : 1f;

		Sample.INSTANCE.play(Assets.Sounds.BLAST, 0.8f, 0.8f);
		PixelScene.shake(2, 0.5f);
		if (Dungeon.level.heroFOV[pos]) CellEmitter.get(pos).burst(Speck.factory(Speck.ROCK), 10);

		ArrayList<Char> around = new ArrayList<>();
		for (Char ch : Actor.chars()){
			if (CoopRules.hits(hero, ch) && ch.isAlive() && Dungeon.level.distance(pos, ch.pos) <= WAVE_RADIUS){
				around.add(ch);
			}
		}
		for (Char ch : around){
			ch.damage(Math.round(Hero.heroDamageIntRange(waveMin(hero), waveMax(hero)) * factor), this);
			if (!ch.isAlive()) continue;
			Buff.prolong(ch, Paralysis.class, 2f);
			if (quakeTurns(hero) > 0) Brittle.apply(hero, ch, quakeTurns(hero));
		}
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", range(hero), minDmg(hero), maxDmg(hero), WAVE_RADIUS,
				waveMin(hero), waveMax(hero), shield(hero));
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}
}
