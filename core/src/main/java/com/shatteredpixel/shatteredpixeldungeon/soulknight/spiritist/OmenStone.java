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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

import java.util.ArrayList;

//W: rock from the sky. dmg under it, leaves a plague (PlagueCloud)
public class OmenStone extends ChampionAbility {

	public static final OmenStone INSTANCE = new OmenStone();

	@Override public String slot() { return "W"; }
	@Override public int unlockLevel() { return 2; }
	@Override public int icon() { return HeroIcon.SP_OMEN; }
	@Override public int color() { return 0x77CC55; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }
	@Override public boolean targeted() { return true; }

	public static final int RANGE = 6;

	@Override
	public float cooldown( Hero hero ) {
		return 25;
	}

	public static int minDmg( Hero hero ){
		return 3 + hero.lvl/3 + 2*hero.pointsInTalent(Talent.SP_OMEN_WEIGHT);
	}

	public static int maxDmg( Hero hero ){
		return 8 + hero.lvl/2 + 2*hero.pointsInTalent(Talent.SP_OMEN_WEIGHT);
	}

	public static int plagueRadius( Hero hero ){
		return hero.subClass == HeroSubClass.PLAGUE_HERALD ? 2 : 1;
	}

	public static int plagueTurns( Hero hero ){
		return 8 + 3*hero.pointsInTalent(Talent.PH_SPREAD);
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		if (target == null) return;
		if (Dungeon.level.distance(hero.pos, target) > RANGE || !Dungeon.level.heroFOV[target] || Dungeon.level.solid[target]){
			GLog.w(Messages.get(ChampionAbility.class, "too_far"));
			return;
		}

		ArrayList<Integer> hit = SKCombat.openArea(target, 1);
		for (int c : hit){
			if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.ROCK), 3);
		}
		for (Char ch : SKCombat.foesIn(hero, hit)){
			ch.damage(Math.max(1, Random.NormalIntRange(minDmg(hero), maxDmg(hero)) - ch.drRoll()/2), this);
		}
		for (int c : SKCombat.openArea(target, plagueRadius(hero))){
			GameScene.add(Blob.seed(c, plagueTurns(hero), PlagueCloud.class));
		}
		Sample.INSTANCE.play(Assets.Sounds.ROCKS);
		PixelScene.shake(1, 0.3f);

		ChampionHooks.operateVisual(hero, target);
		Invisibility.dispel(hero);
		startCooldown(hero);
		hero.spendAndNext(1f);
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", RANGE, minDmg(hero), maxDmg(hero), plagueRadius(hero), plagueTurns(hero),
				PlagueCloud.strength());
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}
}
