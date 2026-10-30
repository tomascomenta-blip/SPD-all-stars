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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.DirectableAlly;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

//mist maiden, from Eulogy. fights next to the gravedigger and pops a walker every few turns. stats from the owner
public class MistMaiden extends DirectableAlly {

	{
		spriteClass = Sprite.class;
		HP = HT = 20;
		defenseSkill = 5;
		baseSpeed = 1f;

		properties.add(Property.UNDEAD);
		immunities.add(AllyBuff.class);

		WANDERING = new Escort();
		state = WANDERING;
	}

	public static final int MAX_WALKERS = 2;

	public int ownerIndex = 0;
	public int power = 1;
	public float dmgMulti = 1f;
	public float timeLeft = 40f;   //<0 = hasta que muera
	public float raiseEvery = 5f;
	public float raiseCharge = 0f;

	public static int health( Hero owner ){
		float multi = owner.subClass == HeroSubClass.MISTLORD ? 1.25f : 1f;
		return Math.round((15 + 5*owner.lvl) * multi);
	}

	public static float dmgMulti( Hero owner ){
		float multi = 1f + 0.15f*owner.pointsInTalent(Talent.GRA_MAIDENS_WRATH);
		if (owner.subClass == HeroSubClass.MISTLORD) multi += 0.2f;
		return multi;
	}

	public static float raiseEvery( Hero owner ){
		float every = 5;
		if (owner.subClass == HeroSubClass.MISTLORD) every -= owner.pointsInTalent(Talent.ML_ENDLESS_NIGHT);
		return Math.max(2, every);
	}

	public static int minDmg( int power ){
		return 2 + power/2;
	}

	public static int maxDmg( int power ){
		return 5 + power;
	}

	public static MistMaiden summon( Hero owner, int cell ){
		MistMaiden m = new MistMaiden();
		m.ownerIndex = ChampionHooks.indexOf(owner);
		m.power = owner.lvl;
		m.dmgMulti = dmgMulti(owner);
		m.raiseEvery = raiseEvery(owner);
		m.timeLeft = owner.subClass == HeroSubClass.MISTLORD ? -1 : 40;
		m.HP = m.HT = health(owner);
		m.defenseSkill = 4 + owner.lvl;
		m.pos = cell;
		return m;
	}

	public boolean ownedBy( Hero hero ){
		return ChampionHooks.indexOf(hero) == ownerIndex;
	}

	public static MistMaiden of( Hero hero ){
		for (Char ch : Actor.chars()){
			if (ch instanceof MistMaiden && ((MistMaiden) ch).ownedBy(hero) && ch.isAlive()){
				return (MistMaiden) ch;
			}
		}
		return null;
	}

	@Override
	public int attackSkill( Char target ) {
		return 12 + power;
	}

	@Override
	public int damageRoll() {
		return Math.round(Random.NormalIntRange(minDmg(power), maxDmg(power)) * dmgMulti);
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, power/2);
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc(enemy, damage);
		if (enemy.buff(MistMark.class) != null){
			damage = Math.round(damage * MistMark.DAMAGE_BONUS);
		}
		if (enemy.isAlive()){
			MaidenMark mark = Buff.prolong(enemy, MaidenMark.class, MaidenMark.DURATION);
			mark.owner = ownerIndex;
		}
		ShepherdOfSouls.communion(ChampionHooks.heroByIndex(ownerIndex), damage);
		return damage;
	}

	@Override
	protected Char chooseEnemy() {
		if (!movingToDefendPos){
			Char marked = GravediggerHooks.markedTarget(this, enemy);
			if (marked != null) return marked;
		}
		return super.chooseEnemy();
	}

	@Override
	protected boolean act() {
		if (timeLeft >= 0 && timeLeft <= 0.01f){
			if (Dungeon.level.heroFOV[pos]) GLog.i(Messages.get(this, "fades"));
			die(null);
			return true;
		}
		boolean result = super.act();
		if (isAlive()) tryRaise();
		return result;
	}

	//max 2 of her walkers at once, more was a lag fest on phones
	private void tryRaise(){
		if (raiseCharge < raiseEvery) return;
		if (state != HUNTING || enemy == null || !enemy.isAlive()) return;
		Hero owner = ChampionHooks.heroByIndex(ownerIndex);
		if (owner == null) return;
		int mine = 0;
		for (Char ch : Actor.chars()){
			if (ch instanceof MistWalker && ch.isAlive() && ((MistWalker) ch).fromMaiden
					&& ((MistWalker) ch).ownerIndex == ownerIndex){
				mine++;
			}
		}
		if (mine >= MAX_WALKERS) return;
		ArrayList<Integer> cells = MistWalker.freeCellsAround(pos, 1);
		if (cells.isEmpty()) return;
		raiseCharge = 0;
		MistWalker.raise(owner, cells.get(0), true);
	}

	@Override
	protected void spend( float time ) {
		super.spend(time);
		if (timeLeft > 0) timeLeft = Math.max(0, timeLeft - time);
		raiseCharge = Math.min(raiseCharge + time, raiseEvery);
	}

	@Override
	public void defendPos( int cell ) {
		GLog.i(Messages.get(this, "direct_defend"));
		super.defendPos(cell);
	}

	@Override
	public void followHero() {
		GLog.i(Messages.get(this, "direct_follow"));
		super.followHero();
	}

	@Override
	public void targetChar( Char ch ) {
		GLog.i(Messages.get(this, "direct_attack"));
		super.targetChar(ch);
	}

	@Override
	public String description() {
		String d = Messages.get(this, "desc", Math.round(minDmg(power)*dmgMulti), Math.round(maxDmg(power)*dmgMulti), Math.round(raiseEvery));
		if (timeLeft >= 0) d += "\n\n" + Messages.get(this, "desc_time", Math.max(1, Math.round(timeLeft)));
		else d += "\n\n" + Messages.get(this, "desc_forever");
		return d;
	}

	private static final String OWNER = "owner";
	private static final String POWER = "power";
	private static final String DMG = "dmg";
	private static final String TIME = "time";
	private static final String EVERY = "every";
	private static final String CHARGE = "charge";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(OWNER, ownerIndex);
		bundle.put(POWER, power);
		bundle.put(DMG, dmgMulti);
		bundle.put(TIME, timeLeft);
		bundle.put(EVERY, raiseEvery);
		bundle.put(CHARGE, raiseCharge);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		ownerIndex = bundle.getInt(OWNER);
		power = bundle.getInt(POWER);
		dmgMulti = bundle.getFloat(DMG);
		timeLeft = bundle.getFloat(TIME);
		raiseEvery = bundle.getFloat(EVERY);
		raiseCharge = bundle.getFloat(CHARGE);
		defenseSkill = 4 + power;
	}

	//owner, or context hero if he's down (copy of MistWalker.leader, yes i know)
	private Hero leader(){
		Hero owner = ChampionHooks.heroByIndex(ownerIndex);
		if (owner != null && owner.isAlive() && !owner.coopDown) return owner;
		return Dungeon.hero;
	}

	//directable ally + personal space: keeps a step away, makes room for cramped heroes, blinks if blocked
	private class Escort extends Mob.Wandering {

		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			if (enemyInFOV && attacksAutomatically && !movingToDefendPos
					&& (defendingPos == -1 || !Dungeon.level.heroFOV[defendingPos] || canAttack(enemy))){
				enemySeen = true;
				notice();
				alerted = true;
				state = HUNTING;
				target = enemy.pos;
				return true;
			}
			enemySeen = false;
			int oldPos = pos;

			int away = MistWalker.makeWayCell(pos);
			if (away != -1){
				target = away;
				if (getCloser(away)){
					spend(1 / speed());
					return moveSprite(oldPos, pos);
				}
			}

			if (defendingPos != -1){
				target = defendingPos;
				if (getCloser(target)){
					spend(1 / speed());
					if (pos == defendingPos) movingToDefendPos = false;
					return moveSprite(oldPos, pos);
				}
				//can't get there, just hold
				if (movingToDefendPos){
					defendingPos = pos;
					movingToDefendPos = false;
				}
				spend(TICK);
				return true;
			}

			Hero leader = leader();
			if (Dungeon.level.distance(pos, leader.pos) <= 2){
				spend(TICK);
				return true;
			}
			target = leader.pos;
			if (getCloser(target)){
				spend(1 / speed());
				return moveSprite(oldPos, pos);
			}
			int c = MistWalker.blinkCell(leader.pos);
			if (c != -1) MistWalker.blink(MistMaiden.this, c);
			spend(TICK);
			return true;
		}
	}

	//same layout as the walker sheet
	public static class Sprite extends MobSprite {

		public Sprite() {
			super();
			texture(Assets.Sprites.MIST_MAIDEN);
			TextureFilm frames = new TextureFilm(texture, 16, 16);

			idle = new Animation(2, true);
			idle.frames(frames, 0, 0, 1, 1);

			run = new Animation(10, true);
			run.frames(frames, 2, 3, 4, 5);

			attack = new Animation(12, false);
			attack.frames(frames, 6, 7, 8, 0);

			die = new Animation(8, false);
			die.frames(frames, 9, 10, 11);

			zap = new Animation(10, false);
			zap.frames(frames, 12, 13, 14, 0);

			play(idle);
		}

		@Override
		public int blood() {
			return 0xFFAADDEE;
		}
	}
}
