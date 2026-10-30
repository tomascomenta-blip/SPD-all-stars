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
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barkskin;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAreas;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//E: 5x5 meadow, 5 turns. heals allies in it + puts out fire. druid: more heal + full cleanse
public class HealingMeadow extends ChampionAbility {

	public static final HealingMeadow INSTANCE = new HealingMeadow();

	@Override public String slot() { return "E"; }
	@Override public int unlockLevel() { return 4; }
	@Override public int icon() { return HeroIcon.BOT_HEALING_MEADOW; }
	@Override public int color() { return 0x44DD66; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }
	@Override public boolean targeted() { return true; }

	public static final int RANGE = 7;
	public static final int RADIUS = 2;

	@Override
	public float cooldown( Hero hero ) {
		return 25;
	}

	public static int turns( Hero hero ){
		return 5 + hero.pointsInTalent(Talent.BOT_OVERGROWTH);
	}

	//per turn: 4% + flat from botanist lvl
	public static int heal( Hero hero, Char ch ){
		float f = 1f;
		if (hero.subClass == HeroSubClass.DRUID) f += 0.4f + 0.2f*hero.pointsInTalent(Talent.DR_LIFE_BLOOM);
		return Math.max(1, Math.round((0.04f*ch.HT + 1 + hero.lvl/5f) * f));
	}

	//blob, per-cell turns left. asks for the party's botanist to know how much to heal (firstBotanist, yes)
	public static class Meadow extends Blob {

		@Override
		protected void evolve() {
			Hero owner = BotanistHooks.firstBotanist();
			int w = Dungeon.level.width();
			Fire fire = (Fire) Dungeon.level.blobs.get(Fire.class);
			for (int j = area.top; j < area.bottom; j++){
				for (int i = area.left; i < area.right; i++){
					int cell = i + j*w;
					if (cur[cell] <= 0){
						off[cell] = 0;
						continue;
					}
					off[cell] = cur[cell] - 1;
					volume += off[cell];

					if (fire != null && fire.volume > 0 && fire.cur[cell] > 0) fire.clear(cell);
					Char ch = Actor.findChar(cell);
					if (ch != null && ch.isAlive() && ch.alignment == Char.Alignment.ALLY){
						Burning burning = ch.buff(Burning.class);
						if (burning != null) burning.detach();
						if (owner != null){
							ChampionHooks.heal(ch, heal(owner, ch));
							if (owner.subClass == HeroSubClass.DRUID){
								ChampionHooks.cleanse(ch);
								int bark = 2*owner.pointsInTalent(Talent.DR_PURIFYING_SAP);
								if (bark > 0) Barkskin.conditionallyAppend(ch, bark, 3);
							}
						}
					}
				}
			}
		}

		@Override
		public void use( BlobEmitter emitter ) {
			super.use(emitter);
			emitter.start(Speck.factory(Speck.HEALING), 0.4f, 0);
		}

		@Override
		public String tileDesc() {
			return Messages.get(this, "desc");
		}
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		if (target == null) return;
		if (Dungeon.level.distance(hero.pos, target) > RANGE || !Dungeon.level.heroFOV[target]){
			GLog.w(Messages.get(ChampionAbility.class, "too_far"));
			return;
		}
		ArrayList<Integer> cells = ChampionAreas.cells(target, RADIUS);
		int t = turns(hero);
		for (int c : cells){
			int ter = Dungeon.level.map[c];
			if (ter == Terrain.EMPTY || ter == Terrain.EMPTY_DECO || ter == Terrain.EMBERS){
				Level.set(c, Terrain.GRASS);
				GameScene.updateMap(c);
			}
			if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(LeafParticle.GENERAL, 3);
			GameScene.add(Blob.seed(c, t, Meadow.class));
		}
		Dungeon.observe();
		Sample.INSTANCE.play(Assets.Sounds.PLANT);
		Sample.INSTANCE.play(Assets.Sounds.DRINK, 0.6f, 1.4f);
		ChampionHooks.operateVisual(hero, target);
		Invisibility.dispel(hero);
		BotanistHooks.afterCast(hero);
		startCooldown(hero);
		hero.spendAndNext(1f);
	}

	@Override
	public String desc( Hero hero ) {
		String d = Messages.get(this, "desc", RADIUS*2 + 1, turns(hero), heal(hero, hero), RANGE);
		if (hero.subClass == HeroSubClass.DRUID) d += " " + Messages.get(this, "desc_druid");
		return d;
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}
}
