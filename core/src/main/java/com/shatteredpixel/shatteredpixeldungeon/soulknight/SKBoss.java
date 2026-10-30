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

package com.shatteredpixel.shatteredpixeldungeon.soulknight;

import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.WornKey;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.armory.SKArmory;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//boss base for floors 10/15/20 of the new worlds. hp bar, seals the floor, enrage under 50%.
//on death: exit key, boss weapon, + whatever the vanilla boss would've dropped (tengu mask, crown...)
//so you don't lose subclass/armor ability progress by picking a weird world
public abstract class SKBoss extends SKMob {

	{
		properties.add(Property.BOSS);
	}

	protected boolean enraged = false;
	protected int lastSpell = -1;

	//Statistics.bossScores index + points
	protected abstract int scoreIndex();
	protected abstract int score();

	//drops besides the key
	protected abstract ArrayList<Item> loot();

	protected int pickSpell( int count ){
		int s;
		do {
			s = Random.Int(count);
		} while (s == lastSpell && count > 1);
		lastSpell = s;
		return s;
	}

	protected float speedBonus(){
		return enraged ? 0.5f : 0f;
	}

	@Override
	protected int specialCooldown() {
		return enraged ? Random.IntRange(1, 2) : Random.IntRange(2, 3);
	}

	@Override
	public void notice() {
		super.notice();
		if (!BossHealthBar.isAssigned()) {
			BossHealthBar.assignBoss(this);
			Dungeon.level.seal();
			Dungeon.level.gatherParty();
			yell(Messages.get(this, "notice"));
		}
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (!BossHealthBar.isAssigned()){
			BossHealthBar.assignBoss(this);
			Dungeon.level.seal();
			Dungeon.level.gatherParty();
		}
		super.damage(dmg, src);
		if (!enraged && isAlive() && HP*2 <= HT){
			enraged = true;
			BossHealthBar.bleed(true);
			yell(Messages.get(this, "enraged"));
			cooldown = 0;
		}
	}

	@Override
	public void die( Object cause ) {
		super.die(cause);

		SKBulletField field = (SKBulletField) Dungeon.level.blobs.get(SKBulletField.class);
		if (field != null) field.clearOwner(this);

		Dungeon.level.unseal();
		GameScene.bossSlain();

		if (dropsRewards()){
			Dungeon.level.drop(new WornKey(Dungeon.depth), pos).sprite.drop();
			for (Item i : loot()){
				Dungeon.level.drop(i, freeCellAround()).sprite.drop(pos);
			}
			//+ armory gun, next tier
			Dungeon.level.drop(SKArmory.random(
					SKArmory.tierForDepth(Dungeon.depth)+1), freeCellAround()).sprite.drop(pos);
		}

		Badges.validateBossSlain();
		if (Statistics.qualifiedForBossChallengeBadge){
			Badges.validateBossChallengeCompleted();
		}
		Statistics.bossScores[scoreIndex()] += score();

		yell(Messages.get(this, "defeated"));
	}

	//false = rewards come later (submarine -> kraken drops them)
	protected boolean dropsRewards(){
		return true;
	}

	protected int freeCellAround(){
		for (int tries = 0; tries < 20; tries++){
			int c = pos + PathFinder.NEIGHBOURS8[Random.Int(8)];
			if (!Dungeon.level.solid[c] && Dungeon.level.passable[c]) return c;
		}
		return pos;
	}

	private static final String ENRAGED = "enraged";
	private static final String LAST = "last_spell";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(ENRAGED, enraged);
		bundle.put(LAST, lastSpell);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		enraged = bundle.getBoolean(ENRAGED);
		lastSpell = bundle.getInt(LAST);
		if (state != SLEEPING) BossHealthBar.assignBoss(this);
		if (enraged) BossHealthBar.bleed(true);
	}

	protected void hitFoes( Iterable<Integer> cells, int min, int max ){
		for (Char ch : SKCombat.foesIn(this, cells)){
			ch.damage(Math.max(1, Random.NormalIntRange(min, max) - ch.drRoll()/2), this);
		}
	}
}
