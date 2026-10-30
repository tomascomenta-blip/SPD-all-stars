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

package com.shatteredpixel.shatteredpixeldungeon.champions.gravedigger;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

//passive: stuff dying near him = a grave. graves are just a counter here, nothing on the map. Last Rites uses them
public class ShepherdOfSouls extends Buff {

	{
		type = buffType.POSITIVE;
		revivePersists = true;
	}

	public static final int RANGE = 5;

	public int graves = 0;
	//leftover fraction so tiny hits still add up
	public float communion = 0;

	public static ShepherdOfSouls of( Hero hero ){
		return hero.buff(ShepherdOfSouls.class);
	}

	public static int graves( Hero hero ){
		ShepherdOfSouls s = of(hero);
		return s == null ? 0 : s.graves;
	}

	public static int maxGraves( Hero hero ){
		return 3 + hero.pointsInTalent(Talent.GRA_DEEP_GRAVES);
	}

	//threshold for Last Rites. exhumer needs a full graveyard
	public static int awakenAt( Hero hero ){
		if (hero.subClass == HeroSubClass.EXHUMER) return Math.max(3, maxGraves(hero));
		return 3;
	}

	//exhumer dmg per grave
	public static float exhumerBonus( Hero hero ){
		if (hero.subClass != HeroSubClass.EXHUMER) return 0;
		return graves(hero) * (0.03f + 0.02f*hero.pointsInTalent(Talent.EX_GRAVE_STRENGTH));
	}

	//xp-giving mobs + bosses
	public static boolean counts( Mob mob ){
		return mob.EXP > 0 || Char.hasProp(mob, Char.Property.BOSS) || Char.hasProp(mob, Char.Property.MINIBOSS);
	}

	//every gravedigger nearby who saw it gets one
	public static void onEnemyDeath( Mob mob ){
		if (!counts(mob) || mob.pos < 0 || mob.pos >= Dungeon.level.length()) return;
		if (!Dungeon.level.heroFOV[mob.pos]) return;
		ArrayList<Hero> heroes = new ArrayList<>(Party.living());
		for (Hero h : heroes){
			if (h.heroClass != HeroClass.GRAVEDIGGER) continue;
			if (Dungeon.level.distance(h.pos, mob.pos) > RANGE) continue;
			gain(h, Char.hasProp(mob, Char.Property.BOSS) ? 2 : 1, mob.pos);
		}
	}

	public static void gain( Hero hero, int amount, int pos ){
		if (amount <= 0) return;
		ShepherdOfSouls s = Buff.affect(hero, ShepherdOfSouls.class);
		int before = s.graves;
		s.graves = Math.min(maxGraves(hero), s.graves + amount);
		if (s.graves == before) return;
		if (pos >= 0 && pos < Dungeon.level.length() && Dungeon.level.heroFOV[pos]){
			CellEmitter.get(pos).burst(ShadowParticle.UP, 6);
		}
		if (hero.sprite != null){
			hero.sprite.showStatus(0x99DDCC, Messages.get(ShepherdOfSouls.class, "grave", s.graves));
		}
		Sample.INSTANCE.play(Assets.Sounds.BONES, 0.5f, 0.9f);
		BuffIndicator.refreshHero();
	}

	public static void spendGraves( Hero hero, int amount ){
		ShepherdOfSouls s = of(hero);
		if (s == null) return;
		s.graves = Math.max(0, s.graves - amount);
		BuffIndicator.refreshHero();
	}

	//mistlord: ally hits heal him a bit
	public static void communion( Hero owner, int damage ){
		if (owner == null || !owner.isAlive() || owner.subClass != HeroSubClass.MISTLORD) return;
		int points = owner.pointsInTalent(Talent.ML_DARK_COMMUNION);
		if (points <= 0 || damage <= 0) return;
		ShepherdOfSouls s = of(owner);
		if (s == null) return;
		s.communion += damage * 0.1f * points;
		int heal = (int) s.communion;
		if (heal > 0){
			s.communion -= heal;
			ChampionHooks.heal(owner, heal);
		}
	}

	@Override
	public boolean act() {
		if (!(target instanceof Hero) || ((Hero) target).heroClass != HeroClass.GRAVEDIGGER){
			detach();
			return true;
		}
		//talent reset can lower the cap, clamp
		Hero hero = (Hero) target;
		if (graves > maxGraves(hero)) graves = maxGraves(hero);
		spend(TICK);
		return true;
	}

	@Override
	public int icon() {
		return BuffIndicator.CORRUPT;
	}

	@Override
	public void tintIcon( Image icon ) {
		if (target instanceof Hero && graves >= awakenAt((Hero) target)){
			icon.hardlight(0.5f, 1f, 0.85f);
		} else {
			icon.hardlight(0.55f, 0.7f, 0.7f);
		}
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(graves);
	}

	@Override
	public String name() {
		return Messages.get(ShepherdOfSouls.class, "name");
	}

	@Override
	public String desc() {
		Hero hero = target instanceof Hero ? (Hero) target : null;
		int max = hero == null ? 3 : maxGraves(hero);
		int need = hero == null ? 3 : awakenAt(hero);
		String d = Messages.get(ShepherdOfSouls.class, "desc", RANGE, graves, max, need);
		if (hero != null && hero.subClass == HeroSubClass.EXHUMER){
			d += "\n\n" + Messages.get(ShepherdOfSouls.class, "desc_exhumer", Math.round(100*exhumerBonus(hero)));
		}
		return d;
	}

	private static final String GRAVES = "graves";
	private static final String COMMUNION = "communion";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(GRAVES, graves);
		bundle.put(COMMUNION, communion);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		graves = bundle.getInt(GRAVES);
		communion = bundle.getFloat(COMMUNION);
	}
}
