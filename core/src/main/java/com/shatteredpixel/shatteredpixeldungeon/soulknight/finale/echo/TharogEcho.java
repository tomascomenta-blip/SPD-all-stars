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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.finale.echo;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SnowParticle;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKBulletField;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKWorlds;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forgotten.ForgottenAssets;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

//echo of Tharog. tail sweep in an arc in front of it, icy breath in a cone, and a roar that brings icicles
//down around every hero. smaller than the real one (that thing is a building)
public class TharogEcho extends EchoBoss {

	{
		spriteClass = Sprite.class;
		HP = HT = 2300;
		defenseSkill = 22;
		baseSpeed = 0.9f;
		properties.add(Property.INORGANIC);
		immunities.add(Chill.class);
		immunities.add(Frost.class);
	}

	private static final int SWEEP = 0, BREATH = 1, ROAR = 2;

	private int lastSpell = -1;

	@Override
	public int world() {
		return SKWorlds.FORGOTTEN;
	}

	@Override public int damageRoll() { return Random.NormalIntRange(38, 56); }
	@Override public int attackSkill( Char target ) { return 48; }
	@Override public int drRoll() { return super.drRoll() + Random.NormalIntRange(6, 16); }

	@Override
	protected int specialCooldown() {
		return HP * 2 < HT ? Random.IntRange(2, 3) : 3;
	}

	@Override
	protected boolean special( Char enemy ) {
		if (!fieldOfView[enemy.pos]) return false;
		int d = Dungeon.level.distance(pos, enemy.pos);
		int spell;
		if (d <= 2 && lastSpell != SWEEP) spell = SWEEP;
		else if (d <= 5 && lastSpell != BREATH && Random.Int(3) > 0) spell = BREATH;
		else spell = lastSpell == ROAR ? BREATH : ROAR;
		lastSpell = spell;
		if (sprite != null) sprite.turnTo(pos, enemy.pos);
		if (sprite instanceof Sprite) ((Sprite) sprite).roar();
		boolean visible = Dungeon.level.heroFOV[pos];

		switch (spell){
			case SWEEP: default: {
				ArrayList<Integer> cells = new ArrayList<>();
				float aim = SKBulletField.angleTo(pos, enemy.pos);
				for (int c : SKCombat.openArea(pos, 2)){
					if (c == pos) continue;
					float diff = Math.abs(SKBulletField.angleTo(pos, c) - aim) % 360;
					if (diff > 180) diff = 360 - diff;
					if (diff <= 100) cells.add(c);
				}
				announce(SWEEP, cells, enemy.pos);
				if (visible) GLog.w(Messages.get(this, "sweep"));
				return true;
			}
			case BREATH: {
				Ballistica aim = new Ballistica(pos, enemy.pos, Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID);
				ConeAOE cone = new ConeAOE(aim, 5, 70, Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID);
				ArrayList<Integer> cells = new ArrayList<>(cone.cells);
				cells.remove((Integer) pos);
				if (cells.isEmpty()) return false;
				announce(BREATH, cells, enemy.pos);
				if (visible) GLog.w(Messages.get(this, "breath"));
				return true;
			}
			case ROAR: {
				ArrayList<Integer> cells = new ArrayList<>();
				int w = Dungeon.level.width();
				for (Hero h : Party.living()){
					if (Dungeon.level.distance(pos, h.pos) > 9) continue;
					for (int i = 0; i < 3; i++){
						int c = h.pos + Random.IntRange(-2, 2) + Random.IntRange(-2, 2) * w;
						if (c >= 0 && c < Dungeon.level.length() && Dungeon.level.passable[c] && !cells.contains(c)) cells.add(c);
					}
				}
				if (cells.isEmpty()) return false;
				announce(ROAR, cells, enemy.pos);
				yell(Messages.get(this, "roar"));
				Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 1f, 0.6f);
				return true;
			}
		}
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		switch (kind){
			case SWEEP:
				if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.HIT_CRUSH, 1f, 0.8f);
				for (int c : cells){
					if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.DUST), 2);
				}
				for (Char ch : SKCombat.foesIn(this, cells)){
					ch.damage(special(ch, 46, 62), this);
					if (ch.isAlive()) knock(ch, pos, 2);
				}
				EchoFx.shake(3, 0.4f);
				break;
			case BREATH:
				for (int c : cells){
					if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(SnowParticle.FACTORY, 3);
				}
				if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.GAS, 1f, 0.7f);
				for (Char ch : SKCombat.foesIn(this, cells)){
					ch.damage(special(ch, 36, 52), this);
					if (ch.isAlive()) Buff.prolong(ch, Chill.class, 4f);
				}
				break;
			case ROAR:
				for (int c : cells){
					EchoFx.fall(c, ForgottenAssets.ICICLE, 16, 32, c % 3, 1f, 110, 0.25f, SnowParticle.FACTORY, 6);
				}
				Sample.INSTANCE.play(Assets.Sounds.SHATTER, 1f, 0.9f);
				hitFoes(cells, 40, 56);
				EchoFx.shake(3, 0.5f);
				break;
		}
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("last_spell", lastSpell);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		lastSpell = bundle.getInt("last_spell");
	}

	/* tharog.png, 64x56: row 0 the usual 11 frames, row 1 roaring, row 2 stunned */
	public static class Sprite extends EchoSprite {

		private Animation roar;

		public Sprite(){
			super();
			texture(ForgottenAssets.THAROG);
			TextureFilm film = new TextureFilm(texture, 64, 56);
			idle = anim(film, 2, true, 0, 0, 0, 1, 0, 0, 1, 1);
			run = anim(film, 10, true, 4, 5, 6, 7);
			attack = anim(film, 12, false, 2, 3, 0);
			zap = attack.clone();
			roar = anim(film, 8, false, 11 + 2, 11 + 3, 11 + 3, 11);
			die = anim(film, 10, false, 8, 9, 10);
			echo(0.62f);
			play(idle);
		}

		public void roar(){
			play(roar);
		}

		@Override
		public int blood() {
			return 0xFF7FE8FF;
		}
	}
}
