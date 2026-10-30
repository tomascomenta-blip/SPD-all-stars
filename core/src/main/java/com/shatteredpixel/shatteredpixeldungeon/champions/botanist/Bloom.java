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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAreas;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.gravedigger.GravediggerHooks;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.plants.Earthroot;
import com.shatteredpixel.shatteredpixeldungeon.plants.Mageroyal;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import com.shatteredpixel.shatteredpixeldungeon.plants.Starflower;
import com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass;
import com.shatteredpixel.shatteredpixeldungeon.plants.Swiftthistle;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

//armor ability: instant garden. tall grass, a few random useful plants, party in the area gets healed
public class Bloom extends ArmorAbility {

	{
		baseChargeUse = 35f;
	}

	public static final int RANGE = 7;

	private static final Class<?>[] SEEDS = {
			Sungrass.Seed.class, Earthroot.Seed.class, Mageroyal.Seed.class, Swiftthistle.Seed.class, Starflower.Seed.class
	};

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	public int targetedPos( Char user, int dst ) {
		return new Ballistica(user.pos, dst, Ballistica.STOP_SOLID | Ballistica.STOP_TARGET).collisionPos;
	}

	public static int radius( Hero hero ){
		return 2 + hero.pointsInTalent(Talent.BOT_DEEP_BLOOM)/2;
	}

	public static int plants( Hero hero ){
		return 3 + hero.pointsInTalent(Talent.BOT_LUSH_GARDEN);
	}

	public static int heal( Hero hero, Char ch ){
		return Math.round(0.1f*ch.HT + 2 + hero.lvl/3f) + 3*hero.pointsInTalent(Talent.BOT_HEALING_POLLEN);
	}

	@Override
	protected void activate( ClassArmor armor, Hero hero, Integer target ) {
		if (target == null) return;
		if (Dungeon.level.distance(hero.pos, target) > RANGE){
			GLog.w(Messages.get(ChampionAbility.class, "too_far"));
			return;
		}
		hero.busy();
		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		ArrayList<Integer> area = ChampionAreas.cells(target, radius(hero));
		ArrayList<Integer> plantable = new ArrayList<>();
		for (int c : area){
			int t = Dungeon.level.map[c];
			if (t == Terrain.EMPTY || t == Terrain.EMPTY_DECO || t == Terrain.EMBERS || t == Terrain.GRASS){
				if (Actor.findChar(c) == null && Dungeon.level.heaps.get(c) == null && Dungeon.level.plants.get(c) == null
						&& Dungeon.level.traps.get(c) == null) plantable.add(c);
				Level.set(c, Terrain.HIGH_GRASS);
				GameScene.updateMap(c);
			}
			if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(LeafParticle.GENERAL, 4);
		}

		int idx = ChampionHooks.indexOf(hero);
		for (int i = 0; i < plants(hero) && !plantable.isEmpty(); i++){
			int cell = plantable.remove(Random.Int(plantable.size()));
			Plant.Seed seed = (Plant.Seed) Reflection.newInstance(SEEDS[Random.Int(SEEDS.length)]);
			Plant p = Dungeon.level.plant(seed, cell);
			if (p != null) p.botanist = idx;
		}

		for (Char ch : ChampionAreas.friendsIn(area)){
			ChampionHooks.heal(ch, heal(hero, ch));
			if (ch.sprite != null && ch.sprite.visible) ch.sprite.emitter().burst(Speck.factory(Speck.HEALING), 4);
		}

		Sample.INSTANCE.play(Assets.Sounds.PLANT);
		Sample.INSTANCE.play(Assets.Sounds.DRINK, 0.7f, 1.3f);
		GravediggerHooks.castVisual(hero, target);
		Dungeon.observe();
		GameScene.updateFog();
		Invisibility.dispel(hero);
		hero.spendAndNext(1f);
	}

	@Override
	public String desc() {
		String d = Messages.get(this, "desc", RANGE);
		if (Dungeon.hero != null && Dungeon.hero.heroClass == HeroClass.BOTANIST){
			Hero h = Dungeon.hero;
			d += " " + Messages.get(this, "desc_numbers", radius(h)*2 + 1, plants(h), heal(h, h));
		}
		return d + "\n\n" + Messages.get(this, "cost", (int) baseChargeUse);
	}

	@Override
	public int icon() {
		return HeroIcon.BOT_BLOOM;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.BOT_DEEP_BLOOM, Talent.BOT_LUSH_GARDEN, Talent.BOT_HEALING_POLLEN, Talent.HEROIC_ENERGY};
	}
}
