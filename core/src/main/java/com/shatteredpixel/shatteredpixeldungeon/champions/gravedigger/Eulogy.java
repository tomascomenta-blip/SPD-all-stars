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
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;

//R: summon the maiden. she fights + raises walkers, his hits on whatever she just hit do extra.
//while she's alive R = point her somewhere
public class Eulogy extends ChampionAbility {

	public static final Eulogy INSTANCE = new Eulogy();

	@Override public String slot() { return "R"; }
	@Override public int unlockLevel() { return 6; }
	@Override public int icon() { return HeroIcon.LOL_EULOGY; }
	@Override public int color() { return 0xAACCEE; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }
	@Override public boolean targeted() { return true; }

	public static final int RANGE = 4;
	//% of target's CURRENT hp
	public static final int BONUS_PERCENT = 10;

	@Override
	public float cooldown( Hero hero ) {
		return 80;
	}

	//can always direct her, cd or not
	@Override
	public boolean onCooldown( Hero hero ) {
		return MistMaiden.of(hero) == null && super.onCooldown(hero);
	}

	//boss cap
	public static int bossCap( Hero hero ){
		return 3 + hero.lvl/2;
	}

	public static int maidenBonus( Hero hero, Char enemy ){
		MaidenMark mark = enemy.buff(MaidenMark.class);
		if (mark == null || mark.owner != ChampionHooks.indexOf(hero) || !enemy.isAlive()) return 0;
		int bonus = Math.max(1, Math.round(enemy.HP * BONUS_PERCENT / 100f));
		if (Char.hasProp(enemy, Char.Property.BOSS) || Char.hasProp(enemy, Char.Property.MINIBOSS)){
			bonus = Math.min(bonus, bossCap(hero));
		}
		if (enemy.sprite != null && enemy.sprite.visible){
			enemy.sprite.emitter().burst(MistParticle.FACTORY, 5);
		}
		return bonus;
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		if (target == null) return;

		MistMaiden maiden = MistMaiden.of(hero);
		if (maiden != null){
			maiden.directTocell(target);
			return;
		}

		if (Dungeon.level.distance(hero.pos, target) > RANGE || !Dungeon.level.heroFOV[target]){
			GLog.w(Messages.get(ChampionAbility.class, "too_far"));
			return;
		}
		int cell = target;
		if (Actor.findChar(cell) != null || Dungeon.level.solid[cell] || !Dungeon.level.passable[cell]){
			//spawns next to the chosen cell
			cell = -1;
			for (int n : PathFinder.NEIGHBOURS8){
				int c = target + n;
				if (Actor.findChar(c) == null && Dungeon.level.passable[c] && !Dungeon.level.solid[c]
						&& Dungeon.level.distance(hero.pos, c) <= RANGE + 1){
					cell = c;
					break;
				}
			}
			if (cell == -1){
				GLog.w(Messages.get(this, "no_space"));
				return;
			}
		}

		maiden = MistMaiden.summon(hero, cell);
		GameScene.add(maiden);
		Dungeon.level.occupyCell(maiden);
		if (Dungeon.level.heroFOV[cell]){
			CellEmitter.get(cell).burst(ShadowParticle.UP, 12);
			CellEmitter.center(cell).burst(MistParticle.UP, 16);
		}
		Sample.INSTANCE.play(Assets.Sounds.GHOST, 1f, 0.7f);
		GLog.p(Messages.get(this, "summoned"));
		Dungeon.observe();

		GravediggerHooks.castVisual(hero, cell);
		Invisibility.dispel(hero);
		startCooldown(hero);
		hero.spendAndNext(1f);
	}

	@Override
	public String desc( Hero hero ) {
		String d = Messages.get(this, "desc", RANGE, MistMaiden.health(hero),
				Math.round(MistMaiden.raiseEvery(hero)), BONUS_PERCENT, bossCap(hero));
		if (hero.subClass == HeroSubClass.MISTLORD) d += " " + Messages.get(this, "desc_mistlord");
		else d += " " + Messages.get(this, "desc_time");
		return d;
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}
}
