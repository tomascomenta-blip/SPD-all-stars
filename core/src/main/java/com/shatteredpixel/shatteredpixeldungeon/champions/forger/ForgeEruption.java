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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//forger armor ability: shockwave (dmg + knockdown) then a ring of magma pillars w/ 4 gaps
public class ForgeEruption extends ArmorAbility {

	{
		baseChargeUse = 35f;
	}

	public static final int WAVE_RADIUS = 2;
	public static final int RING_RADIUS = 3;

	public static int waveMin( Hero hero ){
		return Math.round((5 + (hero.STR() - 10)) * (1f + 0.15f*hero.pointsInTalent(Talent.FER_WIDE_CRATER)));
	}

	public static int waveMax( Hero hero ){
		return Math.round((10 + 2*(hero.STR() - 10)) * (1f + 0.15f*hero.pointsInTalent(Talent.FER_WIDE_CRATER)));
	}

	public static int pillarTurns( Hero hero ){
		return 12 + 2*hero.pointsInTalent(Talent.FER_DENSE_MAGMA);
	}

	public static int brittleTurns( Hero hero ){
		return 3*hero.pointsInTalent(Talent.FER_SEISMIC);
	}

	@Override
	protected void activate( ClassArmor armor, Hero hero, Integer target ) {
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		ChampionHooks.operateVisual(hero, hero.pos);
		Sample.INSTANCE.play(Assets.Sounds.BLAST, 1f, 0.6f);
		Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 0.7f);
		PixelScene.shake(3, 0.7f);

		ArrayList<Char> hit = new ArrayList<>();
		for (Char ch : Actor.chars()){
			if (CoopRules.hits(hero, ch) && ch.isAlive() && Dungeon.level.distance(hero.pos, ch.pos) <= WAVE_RADIUS){
				hit.add(ch);
			}
		}
		for (int i = 0; i < Dungeon.level.length(); i++){
			if (Dungeon.level.heroFOV[i] && Dungeon.level.distance(hero.pos, i) <= WAVE_RADIUS && !Dungeon.level.solid[i]){
				CellEmitter.get(i).burst(FlameParticle.FACTORY, 4);
			}
		}
		for (Char ch : hit){
			ch.damage(Hero.heroDamageIntRange(waveMin(hero), waveMax(hero)), this);
			if (!ch.isAlive()) continue;
			Buff.prolong(ch, Paralysis.class, 3f);
			if (brittleTurns(hero) > 0) Brittle.apply(hero, ch, brittleTurns(hero));
		}

		//ring AFTER the wave, so the cells are clear
		int w = Dungeon.level.width();
		int cx = hero.pos % w;
		int cy = hero.pos / w;
		for (int dy = -RING_RADIUS; dy <= RING_RADIUS; dy++){
			for (int dx = -RING_RADIUS; dx <= RING_RADIUS; dx++){
				if (Math.max(Math.abs(dx), Math.abs(dy)) != RING_RADIUS) continue;
				if (dx == 0 || dy == 0) continue; //gates
				int x = cx + dx;
				int y = cy + dy;
				if (x < 0 || y < 0 || x >= w || y >= Dungeon.level.height()) continue;
				MagmaPillar.raise(x + y*w, pillarTurns(hero));
			}
		}

		Invisibility.dispel(hero);
		hero.spendAndNext(Actor.TICK);
	}

	@Override
	public int icon() {
		return HeroIcon.FORGER_ERUPTION;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.FER_WIDE_CRATER, Talent.FER_DENSE_MAGMA, Talent.FER_SEISMIC, Talent.HEROIC_ENERGY};
	}
}
