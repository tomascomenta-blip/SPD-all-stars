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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

//thorn sprout. little turret plant, shoots the closest enemy for a few turns. killable. stats from the owner
public class Sprout extends NPC {

	{
		spriteClass = Sprite.class;

		HP = HT = 8;
		defenseSkill = 0;

		alignment = Alignment.ALLY;
		state = PASSIVE;

		//summon prio
		actPriority = MOB_PRIO + 1;

		properties.add(Property.IMMOVABLE);
		immunities.add(AllyBuff.class);
		immunities.add(Paralysis.class);
	}

	public static final int RANGE = 7;

	public int ownerIndex = 0;
	public int power = 1;
	public float dmgMulti = 1f;
	public int extraShot = 0;      //% for a 2nd thorn
	public float timeLeft = 10f;

	public static int minDmg( int power ){
		return 2 + power/4;
	}

	public static int maxDmg( int power ){
		return 4 + power/2;
	}

	public static Sprout plant( Hero owner, int cell, float factor ){
		Sprout s = new Sprout();
		s.ownerIndex = ChampionHooks.indexOf(owner);
		s.power = owner.lvl;
		s.dmgMulti = factor * BotanistHooks.plantDmg(owner) * (1f + 0.25f*owner.pointsInTalent(Talent.BOT_THORNY_SPROUT));
		s.extraShot = 33 * owner.pointsInTalent(Talent.WG_THORN_BARRAGE);
		s.timeLeft = ThornShooter.duration(owner);
		s.HP = s.HT = Math.round((6 + owner.lvl) * BotanistHooks.plantHealth(owner)
				* (1f + 0.33f*owner.pointsInTalent(Talent.WG_HARD_WOOD)));
		s.pos = cell;
		return s;
	}

	public boolean ownedBy( Hero hero ){
		return ChampionHooks.indexOf(hero) == ownerIndex;
	}

	@Override
	public boolean interact( Char c ) {
		return true;
	}

	//closest reachable, ties -> first in order
	private Char nearestEnemy(){
		Char best = null;
		int bestDist = Integer.MAX_VALUE;
		for (Char ch : Actor.chars()){
			if (ch.alignment != Alignment.ENEMY || !ch.isAlive() || ch.invisible > 0) continue;
			int d = Dungeon.level.distance(pos, ch.pos);
			if (d > RANGE || d >= bestDist) continue;
			if (new Ballistica(pos, ch.pos, Ballistica.PROJECTILE).collisionPos != ch.pos) continue;
			best = ch;
			bestDist = d;
		}
		return best;
	}

	private void shoot( final Char enemy ){
		if (sprite != null && sprite.parent != null && (Dungeon.level.heroFOV[pos] || Dungeon.level.heroFOV[enemy.pos])){
			sprite.zap(enemy.pos);
			MagicMissile.boltFromChar(sprite.parent, MagicMissile.FOLIAGE, sprite, enemy.pos, new Callback() {
				@Override
				public void call() {
				}
			});
			Sample.INSTANCE.play(Assets.Sounds.HIT_ARROW, 0.6f, 1.6f);
		}
		int dmg = Math.round(Random.NormalIntRange(minDmg(power), maxDmg(power)) * dmgMulti);
		enemy.damage(Math.max(1, dmg - enemy.drRoll()/2), this);
	}

	@Override
	protected boolean act() {
		if (timeLeft <= 0){
			if (Dungeon.level.heroFOV[pos]){
				CellEmitter.get(pos).burst(LeafParticle.GENERAL, 6);
			}
			die(null);
			return true;
		}
		timeLeft--;
		Char enemy = nearestEnemy();
		if (enemy != null){
			shoot(enemy);
			if (extraShot > 0 && enemy.isAlive() && Random.Int(100) < extraShot) shoot(enemy);
		}
		spend(TICK);
		return true;
	}

	@Override
	public void beckon( int cell ) {
	}

	@Override
	public String description() {
		return Messages.get(this, "desc", minDmg(power), maxDmg(power), Math.round(timeLeft));
	}

	private static final String OWNER = "owner";
	private static final String POWER = "power";
	private static final String DMG = "dmg";
	private static final String EXTRA = "extra";
	private static final String TIME = "time";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(OWNER, ownerIndex);
		bundle.put(POWER, power);
		bundle.put(DMG, dmgMulti);
		bundle.put(EXTRA, extraShot);
		bundle.put(TIME, timeLeft);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		ownerIndex = bundle.getInt(OWNER);
		power = bundle.getInt(POWER);
		dmgMulti = bundle.getFloat(DMG);
		extraShot = bundle.getInt(EXTRA);
		timeLeft = bundle.getFloat(TIME);
	}

	public static class Sprite extends MobSprite {

		public Sprite() {
			super();
			texture(Assets.Sprites.SPROUT);
			TextureFilm frames = new TextureFilm(texture, 16, 16);

			idle = new Animation(2, true);
			idle.frames(frames, 0, 0, 1, 0, 2, 0);

			run = idle.clone();

			attack = new Animation(14, false);
			attack.frames(frames, 3, 4, 0);

			zap = attack.clone();

			die = new Animation(10, false);
			die.frames(frames, 5, 6, 7);

			play(idle);
		}

		@Override
		public int blood() {
			return 0xFF66AA33;
		}
	}
}
