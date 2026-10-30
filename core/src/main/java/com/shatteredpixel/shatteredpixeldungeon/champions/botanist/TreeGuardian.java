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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfAggression;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

//ancient tree. spooky big tree, pulls aggro from the whole level and roots (paralyse+drain) whatever's close.
//only a few roots at a time, bosses shake them off
public class TreeGuardian extends NPC {

	{
		spriteClass = Sprite.class;

		HP = HT = 60;
		defenseSkill = 0;

		alignment = Alignment.ALLY;
		state = PASSIVE;

		actPriority = MOB_PRIO + 1;

		properties.add(Property.IMMOVABLE);
		properties.add(Property.LARGE);
		immunities.add(AllyBuff.class);
		immunities.add(Paralysis.class);
	}

	public static final int ROOT_RADIUS = 2;
	public static final int CALL_EVERY = 5;

	public int ownerIndex = 0;
	public int power = 1;
	public int drain = 1;          //per rooted enemy per turn
	public int maxRoots = 2;
	public int healAllies = 0;     //druid heal per turn
	public float timeLeft = 40f;
	public int callTimer = 0;

	public static int health( Hero owner ){
		return Math.round((30 + 10*owner.lvl) * BotanistHooks.plantHealth(owner)
				* (1f + 0.25f*owner.pointsInTalent(Talent.BOT_STRONG_ROOTS))
				* (1f + 0.33f*owner.pointsInTalent(Talent.WG_HARD_WOOD)));
	}

	public static float duration( Hero owner ){
		return Math.round(40 * BotanistHooks.plantTime(owner));
	}

	public static int drain( Hero owner ){
		return Math.max(1, Math.round((1 + owner.lvl/4) * BotanistHooks.plantDmg(owner)
				* (1f + 0.25f*owner.pointsInTalent(Talent.BOT_THORNY_SPROUT))));
	}

	public static int maxRoots( Hero owner ){
		return 2 + owner.lvl/6;
	}

	public static int healAllies( Hero owner ){
		if (owner.subClass != HeroSubClass.DRUID) return 0;
		int p = owner.pointsInTalent(Talent.DR_SPRING_RAIN);
		return p == 0 ? 0 : p + owner.lvl/6;
	}

	public static TreeGuardian plant( Hero owner, int cell ){
		TreeGuardian t = new TreeGuardian();
		t.ownerIndex = ChampionHooks.indexOf(owner);
		t.power = owner.lvl;
		t.drain = drain(owner);
		t.maxRoots = maxRoots(owner);
		t.healAllies = healAllies(owner);
		t.timeLeft = duration(owner);
		t.HP = t.HT = health(owner);
		t.pos = cell;
		return t;
	}

	//"¡eh, todos aquí!"
	public void callEnemies(){
		for (Mob m : new ArrayList<>(Dungeon.level.mobs)){
			if (m.alignment == Alignment.ENEMY && m.isAlive()){
				m.beckon(pos);
			}
		}
	}

	@Override
	public boolean interact( Char c ) {
		return true;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, power/3);
	}

	@Override
	protected boolean act() {
		if (timeLeft <= 0){
			if (Dungeon.level.heroFOV[pos]){
				CellEmitter.get(pos).burst(LeafParticle.GENERAL, 12);
			}
			die(null);
			return true;
		}
		timeLeft--;

		Buff.prolong(this, StoneOfAggression.Aggression.class, 3f);
		if (callTimer-- <= 0){
			callTimer = CALL_EVERY;
			callEnemies();
		}

		int roots = 0;
		for (Char ch : Actor.chars()){
			if (roots >= maxRoots) break;
			if (ch.alignment != Alignment.ENEMY || !ch.isAlive()) continue;
			if (Dungeon.level.distance(pos, ch.pos) > ROOT_RADIUS) continue;
			if (Char.hasProp(ch, Char.Property.IMMOVABLE)) continue;
			roots++;
			boolean boss = Char.hasProp(ch, Char.Property.BOSS) || Char.hasProp(ch, Char.Property.MINIBOSS);
			//bosses break free half the time
			if (!boss || Random.Int(2) == 0) Buff.prolong(ch, Paralysis.class, 2f);
			if (Dungeon.level.heroFOV[ch.pos]){
				CellEmitter.get(ch.pos).burst(LeafParticle.GENERAL, 3);
			}
			int dmg = drain;
			ch.damage(dmg, this);
			if (HP < HT) HP = Math.min(HT, HP + Math.max(1, dmg/2));
		}

		if (healAllies > 0){
			for (Char ch : Actor.chars()){
				if (ch == this || ch.alignment != Alignment.ALLY || !ch.isAlive()) continue;
				if (Dungeon.level.distance(pos, ch.pos) <= 3) ChampionHooks.heal(ch, healAllies);
			}
		}

		spend(TICK);
		return true;
	}

	@Override
	public void beckon( int cell ) {
	}

	@Override
	public String description() {
		return Messages.get(this, "desc", drain, maxRoots, Math.round(timeLeft));
	}

	private static final String OWNER = "owner";
	private static final String POWER = "power";
	private static final String DRAIN = "drain";
	private static final String ROOTS = "roots";
	private static final String HEAL = "heal";
	private static final String TIME = "time";
	private static final String CALL = "call";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(OWNER, ownerIndex);
		bundle.put(POWER, power);
		bundle.put(DRAIN, drain);
		bundle.put(ROOTS, maxRoots);
		bundle.put(HEAL, healAllies);
		bundle.put(TIME, timeLeft);
		bundle.put(CALL, callTimer);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		ownerIndex = bundle.getInt(OWNER);
		power = bundle.getInt(POWER);
		drain = bundle.getInt(DRAIN);
		maxRoots = bundle.getInt(ROOTS);
		healAllies = bundle.getInt(HEAL);
		timeLeft = bundle.getFloat(TIME);
		callTimer = bundle.getInt(CALL);
	}

	public static class Sprite extends MobSprite {

		public Sprite() {
			super();
			texture(Assets.Sprites.ANCIENT_TREE);
			TextureFilm frames = new TextureFilm(texture, 16, 16);

			idle = new Animation(2, true);
			idle.frames(frames, 0, 0, 1, 0, 0, 2);

			run = idle.clone();

			attack = new Animation(10, false);
			attack.frames(frames, 3, 4, 0);

			zap = attack.clone();

			die = new Animation(8, false);
			die.frames(frames, 5, 6, 7);

			play(idle);
		}

		@Override
		public int blood() {
			return 0xFF558833;
		}
	}
}
