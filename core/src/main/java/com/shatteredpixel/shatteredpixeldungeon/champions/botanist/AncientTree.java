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

package com.shatteredpixel.shatteredpixeldungeon.champions.botanist;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

//R: the big spooky tree (TreeGuardian). pulls the whole level for ~40 turns and roots whatever gets close
public class AncientTree extends ChampionAbility {

	public static final AncientTree INSTANCE = new AncientTree();

	@Override public String slot() { return "R"; }
	@Override public int unlockLevel() { return 6; }
	@Override public int icon() { return HeroIcon.BOT_ANCIENT_TREE; }
	@Override public int color() { return 0x338844; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }
	@Override public boolean targeted() { return true; }

	public static final int RANGE = 6;

	@Override
	public float cooldown( Hero hero ) {
		return 100;
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		if (target == null) return;
		if (Dungeon.level.distance(hero.pos, target) > RANGE || !Dungeon.level.heroFOV[target]){
			GLog.w(Messages.get(ChampionAbility.class, "too_far"));
			return;
		}
		if (Dungeon.level.solid[target] || !Dungeon.level.passable[target] || Dungeon.level.avoid[target]
				|| Actor.findChar(target) != null){
			GLog.w(Messages.get(this, "no_space"));
			return;
		}

		TreeGuardian tree = TreeGuardian.plant(hero, target);
		GameScene.add(tree);
		Dungeon.level.occupyCell(tree);
		if (Dungeon.level.heroFOV[target]){
			CellEmitter.get(target).burst(LeafParticle.GENERAL, 16);
			CellEmitter.get(target).burst(Speck.factory(Speck.ROCK), 6);
		}
		Sample.INSTANCE.play(Assets.Sounds.PLANT);
		Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 0.7f);
		Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 0.8f, 0.8f);
		PixelScene.shake(2, 0.6f);
		tree.callEnemies();
		Dungeon.observe();

		ChampionHooks.operateVisual(hero, target);
		Invisibility.dispel(hero);
		BotanistHooks.afterCast(hero);
		startCooldown(hero);
		hero.spendAndNext(1f);
	}

	@Override
	public String desc( Hero hero ) {
		String d = Messages.get(this, "desc", RANGE, TreeGuardian.health(hero), Math.round(TreeGuardian.duration(hero)),
				TreeGuardian.maxRoots(hero), TreeGuardian.drain(hero));
		if (TreeGuardian.healAllies(hero) > 0) d += " " + Messages.get(this, "desc_druid", TreeGuardian.healAllies(hero));
		if (BotanistHooks.gardener(hero)) d += " " + Messages.get(this, "desc_gardener");
		return d;
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}
}
