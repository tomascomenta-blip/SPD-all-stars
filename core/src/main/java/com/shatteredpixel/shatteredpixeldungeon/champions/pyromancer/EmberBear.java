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

package com.shatteredpixel.shatteredpixeldungeon.champions.pyromancer;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.DirectableAlly;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

//Brasas!! the fire bear. stats from the owner
public class EmberBear extends DirectableAlly {

	{
		spriteClass = Sprite.class;
		HP = HT = 20;
		defenseSkill = 5;
		baseSpeed = 1f;

		immunities.add(Burning.class);
		immunities.add(Fire.class);
		immunities.add(AllyBuff.class);
	}

	public int ownerIndex = 0;
	public int power = 1;
	public float dmgMulti = 1f;
	public int auraBonus = 0;
	public int bondHeal = 0;       //% of its dmg that heals the owner
	public float timeLeft = 30f;   //<0 = until it dies

	public static EmberBear summon( Hero owner, int cell ){
		EmberBear b = new EmberBear();
		b.ownerIndex = ChampionHooks.indexOf(owner);
		b.power = owner.lvl;
		float hpMulti = 1f + 0.2f*owner.pointsInTalent(Talent.PYR_BEAR_HUG);
		b.dmgMulti = 1f;
		if (owner.subClass == HeroSubClass.TAMER){
			hpMulti += 0.5f;
			b.dmgMulti += 0.5f + 0.1f*owner.pointsInTalent(Talent.TM_BIG_BEAR);
			b.bondHeal = 10*owner.pointsInTalent(Talent.TM_BOND);
			b.timeLeft = -1;
		}
		b.auraBonus = owner.pointsInTalent(Talent.PYR_SCORCHED);
		b.HP = b.HT = Math.round((15 + 5*owner.lvl) * hpMulti);
		b.defenseSkill = 4 + owner.lvl;
		b.pos = cell;
		return b;
	}

	public boolean ownedBy( Hero hero ){
		return ChampionHooks.indexOf(hero) == ownerIndex;
	}

	public static EmberBear of( Hero hero ){
		for (Char ch : Actor.chars()){
			if (ch instanceof EmberBear && ((EmberBear) ch).ownedBy(hero) && ch.isAlive()){
				return (EmberBear) ch;
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
		return Math.round(Random.NormalIntRange(2 + power/2, 6 + power) * dmgMulti);
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, power/2);
	}

	public int auraDamage(){
		return 1 + power/5 + auraBonus;
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc(enemy, damage);
		if (enemy.isAlive()){
			Buff.affect(enemy, Burning.class).reignite(enemy);
		}
		if (bondHeal > 0){
			Hero owner = ChampionHooks.heroByIndex(ownerIndex);
			if (owner != null && owner.isAlive()){
				int heal = Math.round(damage * bondHeal / 100f);
				if (heal > 0 && owner.HP < owner.HT){
					owner.HP = Math.min(owner.HT, owner.HP + heal);
					if (owner.sprite != null) owner.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
				}
			}
		}
		return damage;
	}

	@Override
	protected boolean act() {
		if (timeLeft >= 0 && timeLeft <= 0.01f){
			if (Dungeon.level.heroFOV[pos]) GLog.i(Messages.get(this, "fades"));
			die(null);
			return true;
		}
		//aura
		int dmg = auraDamage();
		for (int n : PathFinder.NEIGHBOURS8){
			Char ch = Actor.findChar(pos + n);
			if (ch != null && ch.alignment == Alignment.ENEMY && ch.isAlive()){
				ch.damage(dmg, this);
				if (ch.sprite != null && ch.sprite.visible){
					CellEmitter.get(ch.pos).burst(FlameParticle.FACTORY, 3);
				}
			}
		}
		return super.act();
	}

	@Override
	protected void spend( float time ) {
		super.spend(time);
		if (timeLeft > 0) timeLeft = Math.max(0, timeLeft - time);
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
		String d = Messages.get(this, "desc", auraDamage());
		if (timeLeft >= 0) d += "\n\n" + Messages.get(this, "desc_time", Math.round(timeLeft));
		else d += "\n\n" + Messages.get(this, "desc_forever");
		return d;
	}

	private static final String OWNER = "owner";
	private static final String POWER = "power";
	private static final String DMG = "dmg";
	private static final String AURA = "aura";
	private static final String BOND = "bond";
	private static final String TIME = "time";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(OWNER, ownerIndex);
		bundle.put(POWER, power);
		bundle.put(DMG, dmgMulti);
		bundle.put(AURA, auraBonus);
		bundle.put(BOND, bondHeal);
		bundle.put(TIME, timeLeft);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		ownerIndex = bundle.getInt(OWNER);
		power = bundle.getInt(POWER);
		dmgMulti = bundle.getFloat(DMG);
		auraBonus = bundle.getInt(AURA);
		bondHeal = bundle.getInt(BOND);
		timeLeft = bundle.getFloat(TIME);
		defenseSkill = 4 + power;
	}

	public static class Sprite extends MobSprite {

		public Sprite() {
			super();
			texture(Assets.Sprites.EMBER_BEAR);
			TextureFilm frames = new TextureFilm(texture, 16, 16);

			idle = new Animation(3, true);
			idle.frames(frames, 0, 0, 1, 0);

			run = new Animation(10, true);
			run.frames(frames, 2, 3, 4, 5);

			attack = new Animation(12, false);
			attack.frames(frames, 6, 7, 8, 0);

			die = new Animation(10, false);
			die.frames(frames, 9, 10, 11);

			play(idle);
		}

		@Override
		public int blood() {
			return 0xFFFF7722;
		}
	}
}
