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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.PoisonParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKWorlds;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.starship.VarkolynAssets;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.LinkedHashSet;

//echo of the varkolyn queen. acid on every hero she sees, a ring of spikes, and a couple of echo whelps.
//the whelps go with her when she breaks
public class QueenEcho extends EchoBoss {

	{
		spriteClass = Sprite.class;
		HP = HT = 2000;
		defenseSkill = 24;
	}

	private static final int ACID = 0, SPIKES = 1, BROOD = 2;

	private int lastSpell = -1;
	private int broodTimer = 2;

	@Override
	public int world() {
		return SKWorlds.STARSHIP;
	}

	@Override public int damageRoll() { return Random.NormalIntRange(30, 46); }
	@Override public int attackSkill( Char target ) { return 44; }
	@Override public int drRoll() { return super.drRoll() + Random.NormalIntRange(2, 12); }

	@Override
	protected int specialCooldown() {
		return HP * 2 < HT ? Random.IntRange(2, 3) : 3;
	}

	@Override
	protected void arrived() {
		super.arrived();
		if (sprite instanceof Sprite) ((Sprite) sprite).roar();
	}

	private int whelps(){
		int n = 0;
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof Whelp && m.isAlive()) n++;
		}
		return n;
	}

	private int maxWhelps(){
		return Party.living().size() >= 3 ? 3 : 2;
	}

	@Override
	protected boolean special( Char enemy ) {
		if (!fieldOfView[enemy.pos]) return false;
		int spell;
		if (--broodTimer <= 0 && whelps() < maxWhelps()){
			spell = BROOD;
		} else {
			spell = lastSpell == ACID ? SPIKES : (lastSpell == SPIKES ? ACID : Random.Int(2));
		}
		lastSpell = spell;
		if (sprite != null) sprite.turnTo(pos, enemy.pos);
		boolean visible = Dungeon.level.heroFOV[pos];

		switch (spell){
			case ACID: default: {
				LinkedHashSet<Integer> cells = new LinkedHashSet<>();
				for (Hero h : Party.living()){
					if (fieldOfView[h.pos]) cells.addAll(SKCombat.openArea(h.pos, 1));
				}
				if (cells.isEmpty()) cells.addAll(SKCombat.openArea(enemy.pos, 1));
				announce(ACID, cells, enemy.pos);
				if (sprite instanceof Sprite) ((Sprite) sprite).spit();
				if (visible) GLog.w(Messages.get(this, "acid"));
				return true;
			}
			case SPIKES:
				ring(HP * 2 < HT ? 20 : 16, enemy.pos, 1f, ItemSpriteSheet.SHOT_SPIKE, 20, 28);
				if (sprite instanceof Sprite) ((Sprite) sprite).roar();
				if (visible){
					Sample.INSTANCE.play(Assets.Sounds.HIT_STAB, 1f, 0.6f);
					GLog.w(Messages.get(this, "spikes"));
				}
				break;
			case BROOD:
				broodTimer = 5;
				brood();
				if (sprite instanceof Sprite) ((Sprite) sprite).lay();
				if (visible) GLog.w(Messages.get(this, "brood"));
				break;
		}
		spend(TICK);
		return true;
	}

	private void brood(){
		ArrayList<Integer> cells = freeNear(pos, 1, 2);
		int n = Math.min(2, maxWhelps() - whelps());
		for (int i = 0; i < n && !cells.isEmpty(); i++){
			Whelp w = new Whelp();
			w.pos = cells.remove(Random.Int(cells.size()));
			GameScene.add(w);
			EchoFx.sparks(w.pos, 6);
		}
		if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.MELD, 1f, 1.3f);
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		if (kind != ACID) return;
		for (int c : cells){
			if (Dungeon.level.heroFOV[c]) CellEmitter.center(c).burst(PoisonParticle.SPLASH, 4);
		}
		if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.GAS, 0.8f, 1.4f);
		for (Char ch : SKCombat.foesIn(this, cells)){
			ch.damage(special(ch, 30, 42), this);
			if (ch.isAlive()) Buff.affect(ch, Ooze.class).set(3);
		}
	}

	@Override
	public void die( Object cause ) {
		super.die(cause);
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (m instanceof Whelp && m.isAlive()) m.die(null);
		}
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("last_spell", lastSpell);
		bundle.put("brood", broodTimer);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		lastSpell = bundle.getInt("last_spell");
		broodTimer = bundle.getInt("brood");
	}

	//little echo varkolyn. bites, that's it
	public static class Whelp extends SKMob {

		{
			spriteClass = WhelpSprite.class;
			HP = HT = 70;
			defenseSkill = 18;
			state = HUNTING;
			summoned();
		}

		@Override public int damageRoll() { return Random.NormalIntRange(16, 26); }
		@Override public int attackSkill( Char target ) { return 40; }
		@Override public int drRoll() { return super.drRoll() + Random.NormalIntRange(0, 6); }

		@Override
		protected boolean act() {
			if (isAlive() && state != HUNTING && paralysed <= 0) state = HUNTING;
			return super.act();
		}
	}

	//sk_varkolyn.png, 18x14, the usual 11 frames
	public static class WhelpSprite extends EchoSprite {
		public WhelpSprite(){
			super();
			texture(SKAssets.VARKOLYN);
			TextureFilm film = new TextureFilm(texture, 18, 14);
			idle = anim(film, 2, true, 0, 0, 0, 1, 0, 0, 1, 1);
			run = anim(film, 12, true, 4, 5, 6, 7);
			attack = anim(film, 12, false, 2, 3, 0);
			zap = attack.clone();
			die = anim(film, 12, false, 8, 9, 10);
			baseAlpha = 0.7f;
			play(idle);
		}

		@Override
		public int blood() {
			return 0xFF7FE8FF;
		}
	}

	/* queen.png 40x40. row 0 with her shell (0-1 idle, 2-3 bite, 4-7 walk, 8-10 death),
	   row 2 the specials (0-1 spit, 2 roar, 3 lay) */
	public static class Sprite extends EchoSprite {

		private static final int COLS = 11;

		private Animation spit, roar, lay;

		public Sprite(){
			super();
			texture(VarkolynAssets.QUEEN);
			TextureFilm film = new TextureFilm(texture, 40, 40);
			int s = 2 * COLS;
			idle = anim(film, 2, true, 0, 0, 0, 1, 0, 0, 1, 1);
			run = anim(film, 12, true, 4, 5, 6, 7);
			attack = anim(film, 12, false, 2, 3, 0);
			zap = anim(film, 10, false, s, s + 1, s + 1, 0);
			spit = zap.clone();
			roar = anim(film, 3, false, s + 2, s + 2, s + 2, 0);
			lay = anim(film, 4, false, s + 3, s + 3, 0);
			die = anim(film, 8, false, 8, 9, 10);
			echo(0.85f);
			play(idle);
		}

		public void spit(){
			play(spit);
		}

		public void roar(){
			play(roar);
		}

		public void lay(){
			play(lay);
		}

		@Override
		public int blood() {
			return 0xFF7FE8FF;
		}
	}
}
