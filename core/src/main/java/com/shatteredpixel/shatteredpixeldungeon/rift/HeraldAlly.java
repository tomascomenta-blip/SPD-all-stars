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

package com.shatteredpixel.shatteredpixeldungeon.rift;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.DirectableAlly;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.PurpleParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;

//the herald on OUR side (eye of the herald). stats from the depth you summon her at. charges
//through enemies every few turns, each charge eats 2/3 of her current hp. glass cannon
public class HeraldAlly extends DirectableAlly {

	{
		spriteClass = Sprite.class;
		HP = HT = 60;
		defenseSkill = 10;
		viewDistance = 8;
		baseSpeed = 1f;

		HUNTING = new Hunting();

		immunities.add(AllyBuff.class);
	}

	public static final float DURATION = 200f;
	public static final int CHARGE_RANGE = 6;
	public static final int CHARGE_COOLDOWN = 8;
	public static final float CHARGE_MULTI = 3f;
	public static final float CHARGE_COST = 0.66f;

	public int ownerIndex = 0;
	public int power = 10;           //depth where she was summoned
	public float timeLeft = DURATION;
	public int chargeCooldown = 2;

	public static int hpFor( int depth ){
		return 20 + 6*depth;
	}

	public static int minDamage( int depth ){
		return 1 + depth/3;
	}

	public static int maxDamage( int depth ){
		return 4 + 2*depth/3;
	}

	public static HeraldAlly summon( Hero owner, int cell ){
		HeraldAlly a = new HeraldAlly();
		a.ownerIndex = ChampionHooks.indexOf(owner);
		a.power = Math.max(1, Dungeon.scalingDepth());
		a.HP = a.HT = hpFor(a.power);
		a.defenseSkill = 5 + a.power;
		a.pos = cell;
		return a;
	}

	public boolean ownedBy( Hero hero ){
		return ChampionHooks.indexOf(hero) == ownerIndex;
	}

	@Override
	public int attackSkill( Char target ) {
		return 12 + power;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(minDamage(power), maxDamage(power));
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, power/3);
	}

	@Override
	protected boolean act() {
		if (timeLeft <= 0.01f){
			if (Dungeon.level.heroFOV[pos]) GLog.i(Messages.get(this, "fades"));
			die(null);
			return true;
		}
		if (chargeCooldown > 0) chargeCooldown--;
		return super.act();
	}

	@Override
	protected void spend( float time ) {
		super.spend(time);
		timeLeft = Math.max(0, timeLeft - time);
	}

	private class Hunting extends Mob.Hunting {
		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			if (enemyInFOV && chargeCooldown <= 0 && enemy != null && enemy.alignment == Alignment.ENEMY){
				int dist = Dungeon.level.distance(pos, enemy.pos);
				if (dist >= 2 && dist <= CHARGE_RANGE){
					int result = charge(enemy);
					if (result != NO_CHARGE) return result == CHARGED;
				}
			}
			return super.act(enemyInFOV, justAlerted);
		}
	}

	private static final int NO_CHARGE = 0;
	private static final int CHARGED = 1;    //done at once
	private static final int CHARGING = 2;   //waits for the leap animation

	//leaps along the line towards an enemy, through every enemy on it
	private int charge( Char target ){
		ArrayList<Integer> line = RiftHerald.chargeLine(pos, target.pos, CHARGE_RANGE);
		int targetIdx = line.indexOf(target.pos);
		if (targetIdx < 0) return NO_CHARGE;
		int land = -1;
		for (int i = line.size() - 1; i > targetIdx; i--){
			if (Actor.findChar(line.get(i)) == null){
				land = i;
				break;
			}
		}
		if (land == -1) return NO_CHARGE;

		final ArrayList<Char> hit = new ArrayList<>();
		for (int i = 0; i < land; i++){
			Char ch = Actor.findChar(line.get(i));
			if (ch != null && ch.alignment == Alignment.ENEMY) hit.add(ch);
		}
		final int from = pos;
		final int to = line.get(land);
		chargeCooldown = CHARGE_COOLDOWN;
		spend(TICK);

		if (sprite != null){
			sprite.visible = Dungeon.level.heroFOV[from] || Dungeon.level.heroFOV[to];
			if (sprite.visible) sprite.showStatus(CharSprite.POSITIVE, Messages.get(this, "charge"));
			float dist = Dungeon.level.trueDistance(from, to);
			sprite.jump(from, to, 3f, 0.08f + 0.04f*dist, new Callback() {
				@Override
				public void call() {
					landCharge(from, to, hit);
					next();
				}
			});
			return CHARGING;
		}
		landCharge(from, to, hit);
		return CHARGED;
	}

	private void landCharge( int from, int to, ArrayList<Char> hit ){
		pos = to;
		Dungeon.level.occupyCell(this);
		if (sprite != null) sprite.idle();

		boolean seen = Dungeon.level.heroFOV[to];
		for (Char ch : hit){
			if (!ch.isAlive()) continue;
			seen = seen || Dungeon.level.heroFOV[ch.pos];
			ch.damage(Math.round(CHARGE_MULTI * damageRoll()), this);
			if (ch.sprite != null) ch.sprite.flash();
			if (ch.isAlive()) RiftHerald.shoveAside(ch, from, to);
		}
		if (seen){
			Sample.INSTANCE.play(Assets.Sounds.ROCKS);
			PixelScene.shake(1, 0.3f);
			CellEmitter.get(to).burst(Speck.factory(Speck.DUST), 6);
		}

		//the charge takes two thirds of her current health
		int cost = Math.max(1, Math.round(HP * CHARGE_COST));
		if (cost >= HP){
			if (sprite != null) sprite.emitter().burst(PurpleParticle.BURST, 10);
			die(this);
		} else {
			HP -= cost;
			if (sprite != null) sprite.showStatus(CharSprite.NEGATIVE, Integer.toString(cost));
		}
	}

	@Override
	public String description() {
		String d = Messages.get(this, "desc", Math.round(CHARGE_MULTI * minDamage(power)),
				Math.round(CHARGE_MULTI * maxDamage(power)), Math.round(CHARGE_COST * 100));
		d += "\n\n" + Messages.get(this, "desc_time", Math.round(timeLeft));
		return d;
	}

	private static final String OWNER = "owner";
	private static final String POWER = "power";
	private static final String TIME = "time";
	private static final String CHARGE_CD = "charge_cd";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(OWNER, ownerIndex);
		bundle.put(POWER, power);
		bundle.put(TIME, timeLeft);
		bundle.put(CHARGE_CD, chargeCooldown);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		ownerIndex = bundle.getInt(OWNER);
		power = bundle.getInt(POWER);
		timeLeft = bundle.getFloat(TIME);
		chargeCooldown = bundle.getInt(CHARGE_CD);
		defenseSkill = 5 + power;
	}

	//same look as the Herald, with a cold glow so she is told apart from an enemy one
	public static class Sprite extends RiftHerald.Sprite {

		public Sprite() {
			super();
			resetColor();
		}

		@Override
		public void resetColor() {
			super.resetColor();
			tint(0x66CCFF, 0.3f);
		}
	}
}
