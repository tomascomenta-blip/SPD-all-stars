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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SnowParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAlly;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.watabou.utils.Random;

//pack wolf. with you the whole run, Pack respawns it after it dies
public class Wolf extends SKAlly {

	{
		spriteClass = Sprite.class;
		HP = HT = 9;
		defenseSkill = 5;
		baseSpeed = 1f;
	}

	//stats from the owner, recalc whenever they might've changed (lvl up, talents, whatever)
	public void refresh( Hero owner ){
		power = owner.lvl;
		float hpMulti = 1f + 0.15f*owner.pointsInTalent(Talent.BM_THICK_FUR);
		if (owner.subClass == HeroSubClass.PACK_ALPHA) hpMulti += 0.1f*owner.pointsInTalent(Talent.PA_THIRD_WOLF);
		int oldHT = HT;
		HT = Math.round((6 + 3*owner.lvl) * hpMulti);
		if (HT > oldHT) HP += HT - oldHT;
		HP = Math.min(HP, HT);
		defenseSkill = 4 + owner.lvl;
	}

	public float dmgMulti(){
		Hero o = owner();
		if (o == null) return 1f;
		float m = 1f + 0.15f*o.pointsInTalent(Talent.BM_SHARP_FANGS);
		if (o.subClass == HeroSubClass.PACK_ALPHA) m += 0.1f*o.pointsInTalent(Talent.PA_THIRD_WOLF);
		if (buff(PackRoar.Ferocity.class) != null) m += 0.25f;
		return m;
	}

	public static int minDmg( int power ){
		return 1 + power/4;
	}

	public static int maxDmg( int power ){
		return 3 + power/2;
	}

	@Override
	public int attackSkill( Char target ) {
		return 10 + power;
	}

	@Override
	public int damageRoll() {
		return Math.round(Random.NormalIntRange(minDmg(power), maxDmg(power)) * dmgMulti());
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 1 + power/6);
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc(enemy, damage);
		Hero o = owner();
		if (o != null && o.subClass == HeroSubClass.PACK_ALPHA && o.hasTalent(Talent.PA_PACK_TACTICS)
				&& Dungeon.level.adjacent(o.pos, enemy.pos)){
			damage += o.pointsInTalent(Talent.PA_PACK_TACTICS);
		}
		FrostfireWolves.Imbue imbue = buff(FrostfireWolves.Imbue.class);
		if (imbue != null && enemy.isAlive()){
			damage += imbue.bonus;
			if (imbue.fire()){
				Buff.affect(enemy, Burning.class).reignite(enemy, 2f);
				if (enemy.sprite != null && enemy.sprite.visible) CellEmitter.get(enemy.pos).burst(FlameParticle.FACTORY, 4);
			} else {
				Buff.prolong(enemy, Chill.class, 2f);
				if (enemy.sprite != null && enemy.sprite.visible) CellEmitter.get(enemy.pos).burst(SnowParticle.FACTORY, 4);
			}
			imbue.flip();
		}
		return damage;
	}

	@Override
	public void die( Object cause ) {
		Hero o = owner();
		super.die(cause);
		if (o != null){
			Pack pack = o.buff(Pack.class);
			if (pack != null) pack.onWolfDown(o);
		}
	}

	@Override
	public String description() {
		return Messages.get(this, "desc", Math.round(minDmg(power)*dmgMulti()), Math.round(maxDmg(power)*dmgMulti()));
	}

	public static class Sprite extends SKMobSprite {
		public Sprite(){
			super();
			setup(SKAssets.WOLF, 18, 14);
		}
	}
}
