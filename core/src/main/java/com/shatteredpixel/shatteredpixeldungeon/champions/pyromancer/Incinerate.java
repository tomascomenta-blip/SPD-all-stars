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

package com.shatteredpixel.shatteredpixeldungeon.champions.pyromancer;

import com.shatteredpixel.shatteredpixeldungeon.coop.CoopRules;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

import java.util.ArrayList;

//W: fire cone. hits the other heroes only with friendly fire on
public class Incinerate extends ChampionAbility {

	public static final Incinerate INSTANCE = new Incinerate();

	@Override public String slot() { return "W"; }
	@Override public int unlockLevel() { return 2; }
	@Override public int icon() { return HeroIcon.LOL_INCINERATE; }
	@Override public int color() { return 0xFF4411; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }
	@Override public boolean targeted() { return true; }

	@Override
	public float cooldown( Hero hero ) {
		return 12 - 2*hero.pointsInTalent(Talent.IN_FLASHFIRE);
	}

	public static int range( Hero hero ){
		return 3 + hero.pointsInTalent(Talent.PYR_WIDE_FLAMES);
	}

	public static float degrees( Hero hero ){
		return hero.subClass == HeroSubClass.INFERNALIST ? 90 : 60;
	}

	public static int minDmg( Hero hero ){
		return 1 + hero.lvl/3;
	}

	public static int maxDmg( Hero hero ){
		return 4 + Math.round(hero.lvl*0.8f);
	}

	//infernalist +20% vs burning
	public static float burningBonus( Hero hero, Char target ){
		if (hero.subClass == HeroSubClass.INFERNALIST && target.buff(Burning.class) != null) return 1.2f;
		return 1f;
	}

	@Override
	public void activate( final Hero hero, ChampionKit kit, Integer target ) {
		if (target == null || target == hero.pos) return;
		Ballistica aim = new Ballistica(hero.pos, target, Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID);
		final ConeAOE cone = new ConeAOE(aim, range(hero), degrees(hero),
				Ballistica.STOP_TARGET | Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID);

		final boolean stun = Pyromania.onOffensiveCast(hero);
		hero.busy();
		hero.sprite.zap(target);
		Invisibility.dispel(hero);

		Ballistica longest = null;
		for (Ballistica ray : cone.outerRays){
			if (longest == null || ray.dist > longest.dist) longest = ray;
			((MagicMissile) hero.sprite.parent.recycle(MagicMissile.class)).reset(
					MagicMissile.FIRE_CONE, hero.sprite, ray.path.get(ray.dist), null);
		}
		int mid = longest == null ? target : longest.path.get(Math.max(1, longest.dist / 2));
		Sample.INSTANCE.play(Assets.Sounds.BURNING);
		MagicMissile.boltFromChar(hero.sprite.parent, MagicMissile.FIRE_CONE, hero.sprite, mid, new Callback() {
			@Override
			public void call() {
				ArrayList<Char> hit = new ArrayList<>();
				for (int cell : cone.cells){
					if (cell == hero.pos) continue;
					if (Dungeon.level.map[cell] == Terrain.DOOR){
						Level.set(cell, Terrain.OPEN_DOOR);
						GameScene.updateMap(cell);
					}
					Char ch = Actor.findChar(cell);
					if (ch != null && CoopRules.hits(hero, ch) && ch.isAlive()) hit.add(ch);
				}
				for (Char ch : hit){
					int dmg = Math.round(Hero.heroDamageIntRange(minDmg(hero), maxDmg(hero)) * burningBonus(hero, ch));
					ch.damage(dmg, Incinerate.this);
					if (ch.isAlive()){
						Buff.affect(ch, Burning.class).reignite(ch);
						if (stun) Pyromania.stun(hero, ch);
					}
				}
				startCooldown(hero);
				hero.spendAndNext(1f);
			}
		});
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", range(hero), Math.round(degrees(hero)), minDmg(hero), maxDmg(hero));
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}
}
