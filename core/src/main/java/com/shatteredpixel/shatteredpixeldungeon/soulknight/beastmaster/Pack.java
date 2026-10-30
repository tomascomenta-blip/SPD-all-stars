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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.beastmaster;

import com.shatteredpixel.shatteredpixeldungeon.coop.CoopRules;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAlly;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

//beastmaster passive. permanent wolves, dead ones respawn next to him after a while (one at a time).
//pack alpha gets 3. good boys
public class Pack extends Buff {

	{
		type = buffType.POSITIVE;
		revivePersists = true;
		actPriority = HERO_PRIO - 1;
	}

	public int respawn = 0;

	public static int wanted( Hero hero ){
		return hero.subClass == HeroSubClass.PACK_ALPHA ? 3 : 2;
	}

	public static int respawnTime( Hero hero ){
		return 20 - 5*hero.pointsInTalent(Talent.BM_QUICK_RECOVERY);
	}

	public void onWolfDown( Hero hero ){
		if (respawn <= 0) respawn = respawnTime(hero);
	}

	@Override
	public boolean act() {
		spend(TICK);
		if (!(target instanceof Hero) || !target.isAlive()) return true;
		Hero hero = (Hero) target;
		if (Dungeon.level == null) return true;

		ArrayList<Wolf> wolves = SKAlly.of(hero, Wolf.class);
		//trim extras, e.g. back on a floor you fell from and the old wolves are still there
		while (wolves.size() > wanted(hero)){
			Wolf extra = wolves.remove(wolves.size() - 1);
			extra.destroy();
			if (extra.sprite != null) extra.sprite.killAndErase();
		}
		for (Wolf w : wolves) w.refresh(hero);

		if (wolves.size() < wanted(hero)){
			if (respawn > 0){
				respawn--;
			} else {
				//todos los que falten de golpe
				for (int i = wolves.size(); i < wanted(hero); i++){
					if (!call(hero)) break;
				}
			}
		} else {
			respawn = 0;
		}
		return true;
	}

	private boolean call( Hero hero ){
		int cell = -1;
		for (int n : PathFinder.NEIGHBOURS8){
			int c = hero.pos + n;
			if (Actor.findChar(c) == null && Dungeon.level.passable[c] && !Dungeon.level.solid[c] && !Dungeon.level.avoid[c]){
				cell = c;
				break;
			}
		}
		if (cell == -1) return false;
		Wolf w = new Wolf();
		w.setOwner(hero);
		w.refresh(hero);
		w.HP = w.HT;
		w.pos = cell;
		GameScene.add(w);
		Dungeon.level.occupyCell(w);
		if (w.sprite != null && Dungeon.level.heroFOV[cell]){
			w.sprite.emitter().burst(Speck.factory(Speck.WOOL), 5);
			Sample.INSTANCE.play(Assets.Sounds.GRASS, 1f, 0.8f);
		}
		//howl talent: returning wolf weakens stuff around it
		int howl = hero.pointsInTalent(Talent.BM_HOWL);
		if (howl > 0){
			for (Char ch : Actor.chars()){
				if (CoopRules.hits(hero, ch) && ch.isAlive() && Dungeon.level.distance(cell, ch.pos) <= 2){
					Buff.prolong(ch, Weakness.class, 1 + 2*howl);
				}
			}
		}
		return true;
	}

	@Override
	public int icon() {
		return respawn > 0 ? BuffIndicator.TIME : BuffIndicator.NONE;
	}

	@Override
	public void tintIcon( Image icon ) {
		icon.hardlight(0.6f, 0.9f, 0.5f);
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(respawn);
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", respawn);
	}

	private static final String RESPAWN = "respawn";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(RESPAWN, respawn);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		respawn = bundle.getInt(RESPAWN);
	}
}
