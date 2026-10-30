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
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKBulletField;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKWorlds;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.kingdom.ThroneAssets;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//echo of the dark grand knight, on foot. shield up front, thrust along a line, leap of judgement, blade fan.
//the shield points wherever he last moved or hit, so after a thrust or a leap his back is open
public class KnightEcho extends EchoBoss {

	{
		spriteClass = Sprite.class;
		HP = HT = 1700;
		defenseSkill = 26;
	}

	private static final int FAN = 0, THRUST = 1, LEAP = 2;

	private int faceX = 0, faceY = 1;
	private int leapCell = -1;
	private int lastSpell = -1;
	private int blocks = 0;

	@Override
	public int world() {
		return SKWorlds.KINGDOM;
	}

	@Override public int damageRoll() { return Random.NormalIntRange(34, 52); }
	@Override public int attackSkill( Char target ) { return 46; }
	@Override public int drRoll() { return super.drRoll() + Random.NormalIntRange(4, 14); }

	@Override
	protected int specialCooldown() {
		return HP * 2 < HT ? Random.IntRange(2, 3) : 3;
	}

	private void face( int cell ){
		int w = Dungeon.level.width();
		int dx = cell % w - pos % w, dy = cell / w - pos / w;
		if (dx == 0 && dy == 0) return;
		faceX = dx;
		faceY = dy;
	}

	@Override
	public void move( int step, boolean travelling ) {
		face(step);
		super.move(step, travelling);
	}

	@Override
	public boolean attack( Char enemy, float dmgMulti, float dmgBonus, float accMulti ) {
		if (enemy != null) face(enemy.pos);
		return super.attack(enemy, dmgMulti, dmgBonus, accMulti);
	}

	private static Char attacker( Object src ){
		if (src instanceof Char) return (Char) src;
		if (src instanceof SKBulletField.Shot) return ((SKBulletField.Shot) src).owner;
		return null;
	}

	//hits from the front (60 deg either side) only get half through
	private boolean blocked( Object src ){
		Char a = attacker(src);
		if (a == null || a == this || a.alignment != Alignment.ALLY) return false;
		int w = Dungeon.level.width();
		int ax = a.pos % w - pos % w, ay = a.pos / w - pos / w;
		if (ax == 0 && ay == 0) return false;
		double dot = (faceX * ax + faceY * ay) / (Math.sqrt(faceX * faceX + faceY * faceY) * Math.sqrt(ax * ax + ay * ay));
		return dot >= 0.5;
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (!isAlive() || emerging()) return;
		if (dmg > 0 && blocked(src)){
			dmg = Math.max(1, dmg / 2);
			if (Dungeon.level.heroFOV[pos]){
				Sample.INSTANCE.play(Assets.Sounds.HIT_PARRY, 0.8f, 1.1f);
				if (sprite != null && blocks++ % 3 == 0) sprite.showStatus(CharSprite.NEUTRAL, Messages.get(this, "blocked"));
			}
		}
		super.damage(dmg, src);
	}

	@Override
	protected boolean special( Char enemy ) {
		if (!fieldOfView[enemy.pos]) return false;
		int[] options = {FAN, THRUST, LEAP, FAN, THRUST};
		int spell;
		int tries = 0;
		do {
			spell = options[Random.Int(options.length)];
		} while (spell == lastSpell && ++tries < 6);
		boolean adjacent = Dungeon.level.adjacent(pos, enemy.pos);
		//right in his face: the sword half the time, else he jumps on someone else or fans them point blank
		if (adjacent && spell != LEAP){
			if (Random.Int(2) == 0) return false;
			spell = LEAP;
		}
		lastSpell = spell;
		if (sprite != null) sprite.turnTo(pos, enemy.pos);
		face(enemy.pos);
		boolean visible = Dungeon.level.heroFOV[pos];

		if (spell == LEAP){
			Hero target = leapTarget();
			if (target != null){
				leapCell = target.pos;
				ArrayList<Integer> cells = new ArrayList<>();
				for (int n : PathFinder.NEIGHBOURS9){
					int c = target.pos + n;
					if (!Dungeon.level.solid[c]) cells.add(c);
				}
				announce(LEAP, cells, target.pos);
				if (sprite instanceof Sprite) ((Sprite) sprite).crouch();
				if (visible) GLog.w(Party.active() ? Messages.get(this, "leap", Party.name(target)) : Messages.get(this, "leap_solo"));
				return true;
			}
			spell = FAN;
		}
		if (spell == THRUST){
			ArrayList<Integer> cells = line(enemy.pos, 7);
			if (Dungeon.level.distance(pos, enemy.pos) >= 2 && cells.size() >= 2){
				announce(THRUST, cells, enemy.pos);
				if (sprite instanceof Sprite) ((Sprite) sprite).prepare();
				if (visible) GLog.w(Messages.get(this, "thrust"));
				return true;
			}
		}
		arc(adjacent ? 5 : 7, 150, enemy.pos, 1.5f, ItemSpriteSheet.SHOT_BLADE, 20, 30);
		if (sprite instanceof Sprite) ((Sprite) sprite).swing();
		if (visible){
			Sample.INSTANCE.play(Assets.Sounds.HIT_SLASH, 1f, 0.7f);
			GLog.w(Messages.get(this, "fan"));
		}
		spend(TICK);
		return true;
	}

	private Hero leapTarget(){
		ArrayList<Hero> options = new ArrayList<>();
		for (Hero h : Party.living()){
			int d = Dungeon.level.distance(pos, h.pos);
			if (d < 2 || d > 7 || !fieldOfView[h.pos]) continue;
			options.add(h);
		}
		return options.isEmpty() ? null : Random.element(options);
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		if (kind == THRUST) thrust(cells);
		else if (kind == LEAP) leap(cells);
	}

	private void thrust( ArrayList<Integer> cells ){
		int start = pos;
		Char victim = null;
		for (int c : cells){
			Char ch = Actor.findChar(c);
			if (ch != null){
				if (SKCombat.foes(this, ch)) victim = ch;
				break;
			}
			if (Dungeon.level.solid[c] || !Dungeon.level.passable[c]) break;
			move(c, false);
		}
		if (pos != start && sprite != null){
			sprite.move(start, pos);
			if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.TRAMPLE);
		}
		if (sprite instanceof Sprite) ((Sprite) sprite).lunge();
		if (victim != null){
			attack(victim, 1.4f, 0, 2f);
			if (victim.isAlive()) knock(victim, pos, 1);
		}
	}

	private void leap( ArrayList<Integer> cells ){
		int target = leapCell;
		leapCell = -1;
		int land = -1;
		if (target != -1 && Actor.findChar(target) == null && Dungeon.level.passable[target]) land = target;
		if (land == -1){
			int best = 99;
			for (int c : cells){
				if (Actor.findChar(c) == null && Dungeon.level.passable[c] && Dungeon.level.distance(c, target) < best){
					best = Dungeon.level.distance(c, target);
					land = c;
				}
			}
		}
		if (land == -1) return;
		int from = pos;
		face(land);
		move(land, false);
		if (sprite != null){
			sprite.jump(from, land, 40, 0.35f, null);
			if (sprite instanceof Sprite) ((Sprite) sprite).land();
		}
		for (Char ch : SKCombat.foesIn(this, cells)){
			ch.damage(special(ch, 40, 56), this);
			if (ch.isAlive()) knock(ch, land, 1);
		}
		for (int i = 0; i < 8; i++){
			shoot(i * 45f + 22.5f, 1.5f, ItemSpriteSheet.SHOT_BLADE, 18, 26);
		}
		if (Dungeon.level.heroFOV[land]){
			Sample.INSTANCE.play(Assets.Sounds.BLAST, 1f, 0.6f);
			for (int c : cells) CellEmitter.get(c).burst(Speck.factory(Speck.DUST), 3);
		}
		EchoFx.sparks(land, 12);
		EchoFx.shake(5, 0.5f);
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("face_x", faceX);
		bundle.put("face_y", faceY);
		bundle.put("leap", leapCell);
		bundle.put("last_spell", lastSpell);
		bundle.put("blocks", blocks);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		faceX = bundle.getInt("face_x");
		faceY = bundle.getInt("face_y");
		leapCell = bundle.getInt("leap");
		lastSpell = bundle.getInt("last_spell");
		blocks = bundle.getInt("blocks");
	}

	/* grand_knight.png, 40x40. row 0 = act I (0-1 idle, 2-3 slash, 4-7 walk, 8 ready, 9 thrust, 10 hit),
	   row 3 = the odd poses (4 crouch, 6 landing, 9-10 on his knees) */
	public static class Sprite extends EchoSprite {

		private static final int SPECIAL = 33;

		private Animation prepare, lunge, crouch, land, swingAnim;

		public Sprite(){
			super();
			texture(ThroneAssets.KNIGHT);
			TextureFilm film = new TextureFilm(texture, 40, 40);
			idle = anim(film, 2, true, 0, 0, 0, 1, 0, 0, 1, 1);
			run = anim(film, 9, true, 4, 5, 6, 7);
			attack = anim(film, 12, false, 2, 3, 0);
			zap = attack.clone();
			prepare = anim(film, 1, true, 8);
			lunge = anim(film, 8, false, 9, 9, 0);
			crouch = anim(film, 1, true, SPECIAL + 4);
			land = anim(film, 6, false, SPECIAL + 6, SPECIAL + 6, 0);
			die = anim(film, 3, false, SPECIAL + 9, SPECIAL + 10);
			echo(0.85f);
			play(idle);
		}

		public void swing(){
			swingAnim = attack.clone();
			play(swingAnim);
		}

		public void prepare(){
			play(prepare);
		}

		public void lunge(){
			play(lunge);
		}

		public void crouch(){
			play(crouch);
		}

		public void land(){
			play(land);
		}

		@Override
		public int blood() {
			return 0xFF7FE8FF;
		}
	}
}
