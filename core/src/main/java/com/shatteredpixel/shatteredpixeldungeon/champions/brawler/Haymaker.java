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

package com.shatteredpixel.shatteredpixeldungeon.champions.brawler;

import com.shatteredpixel.shatteredpixeldungeon.coop.CoopRules;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

import java.util.ArrayList;

//W: dump all grit into one big punch. cone, middle line takes more. shield from the grit spent
public class Haymaker extends ChampionAbility {

	public static final Haymaker INSTANCE = new Haymaker();

	@Override public String slot() { return "W"; }
	@Override public int unlockLevel() { return 2; }
	@Override public int icon() { return HeroIcon.LOL_HAYMAKER; }
	@Override public int color() { return 0xFF9933; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }
	@Override public boolean targeted() { return true; }

	public static final int RANGE = 3;

	@Override
	public float cooldown( Hero hero ) {
		return 16;
	}

	public static int minDmg( Hero hero ){
		return 2 + hero.lvl/2;
	}

	public static int maxDmg( Hero hero ){
		return 5 + hero.lvl;
	}

	public static float dmgMulti( Hero hero ){
		return 1f + (hero.subClass == HeroSubClass.PITBOSS ? 0.1f*hero.pointsInTalent(Talent.PB_HAYMAKER) : 0);
	}

	public static float shieldMulti( Hero hero ){
		float m = 0.6f * (1f + 0.15f*hero.pointsInTalent(Talent.BRA_GRIT_SHIELD));
		if (hero.subClass == HeroSubClass.PITBOSS) m *= 1.5f;
		return m;
	}

	@Override
	public void activate( final Hero hero, ChampionKit kit, Integer target ) {
		if (target == null || target == hero.pos) return;
		final Ballistica aim = new Ballistica(hero.pos, target, Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID);
		final ConeAOE cone = new ConeAOE(aim, RANGE, 60, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID);
		final PitGrit grit = PitGrit.of(hero);
		final float stored = grit == null ? 0 : grit.grit;

		hero.busy();
		Invisibility.dispel(hero);
		Sample.INSTANCE.play(Assets.Sounds.BLAST, 0.8f, 1.2f);
		for (Ballistica ray : cone.outerRays){
			((MagicMissile) hero.sprite.parent.recycle(MagicMissile.class)).reset(
					MagicMissile.FORCE_CONE, hero.sprite, ray.path.get(ray.dist), null);
		}
		hero.sprite.attack(target, new Callback() {
			@Override
			public void call() {
				ArrayList<Char> hit = new ArrayList<>();
				for (int cell : cone.cells){
					if (cell == hero.pos) continue;
					Char ch = Actor.findChar(cell);
					if (ch != null && CoopRules.hits(hero, ch) && ch.isAlive()) hit.add(ch);
				}
				for (Char ch : hit){
					float dmg = Hero.heroDamageIntRange(minDmg(hero), maxDmg(hero)) + stored * 0.5f;
					if (aim.subPath(1, Math.min(aim.dist, RANGE)).contains(ch.pos)) dmg *= 1.5f;
					ch.damage(Math.round(dmg * dmgMulti(hero)), Haymaker.this);
				}
				if (stored > 0){
					int shield = Math.round(stored * shieldMulti(hero));
					if (shield > 0){
						Buff.affect(hero, Barrier.class).incShield(shield);
						hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shield), FloatingText.SHIELDING);
					}
					grit.grit = 0;
				}
				PixelScene.shake(1, 0.3f);
				startCooldown(hero);
				hero.spendAndNext(1f);
			}
		});
	}

	@Override
	public String desc( Hero hero ) {
		PitGrit g = PitGrit.of(hero);
		int stored = g == null ? 0 : Math.round(g.grit);
		return Messages.get(this, "desc", RANGE, minDmg(hero), maxDmg(hero), stored,
				Math.round(100 * shieldMulti(hero)));
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}
}
