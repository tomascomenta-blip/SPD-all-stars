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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.armory;

import com.shatteredpixel.shatteredpixeldungeon.coop.CoopRules;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.effects.Beam;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Lightning;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BlastParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SmokeParticle;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.guns.SKGun;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;

//armory guns = a handful of numbers. how it shoots (single, burst, cone, pierce, blast, chain, rockets)
//+ what the bullets do. data-driven-ish
public abstract class ArmoryGun extends SKGun {

	public static final int SINGLE = 0;
	public static final int BURST  = 1;
	public static final int CONE   = 2;
	public static final int PIERCE = 3;
	public static final int BLAST  = 4;
	public static final int CHAIN  = 5;
	public static final int VOLLEY = 6;
	private static final String[] KINDS = {"single", "burst", "cone", "pierce", "blast", "chain", "volley"};

	public static final int NONE   = 0;
	public static final int BURN   = 1;
	public static final int CHILL  = 2;
	public static final int POISON = 3;
	public static final int STUN   = 4;
	public static final int ROOT   = 5;
	public static final int SLOW   = 6;
	private static final String[] EFFECTS = {"none", "burn", "chill", "poison", "stun", "root", "slow"};

	protected int kind = SINGLE;
	protected int count = 1;          //overloaded, means different things per mode. sorry
	protected int reach = 6;          //range-ish
	protected int effect = NONE;
	protected int charges = 3;
	public int tier = 1;
	protected int baseMin = 2, lvlMin = 1, baseMax = 6, lvlMax = 2;
	protected int shot = ItemSpriteSheet.SHOT_BULLET;
	protected String sound = Assets.Sounds.HIT_ARROW;
	protected boolean beam = false;   //pierce but drawn as a beam

	{
		bones = true;
	}

	@Override
	protected int shotImage() {
		return shot;
	}

	@Override
	protected String shotSound() {
		return sound;
	}

	@Override
	public int initialCharges() {
		return charges;
	}

	@Override
	public int min( int lvl ) {
		return baseMin + lvlMin*lvl;
	}

	@Override
	public int max( int lvl ) {
		return baseMax + lvlMax*lvl;
	}

	@Override
	public int value() {
		return (15 + 10*tier) * (level() + 1);
	}

	private ConeAOE cone( Ballistica bolt ){
		return new ConeAOE(bolt, reach, 50, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID);
	}

	private int lineReach( Ballistica beam ){
		int max = Math.min(reach, beam.dist);
		for (int i = 1; i <= max; i++){
			if (Dungeon.level.solid[beam.path.get(i)]) return i - 1;
		}
		return max;
	}

	private boolean foe( Char ch ){
		return ch != null && ch != curUser && ch.isAlive() && (ch.alignment != curUser.alignment || CoopRules.friendlyTarget(curUser, ch));
	}

	//per bullet
	protected void hit( Char ch, int dmg ){
		if (!foe(ch)) return;
		shoot(ch, dmg);
		if (!ch.isAlive()) return;
		switch (effect){
			case BURN:   Buff.affect(ch, Burning.class).reignite(ch, 4f); break;
			case CHILL:  Buff.prolong(ch, Chill.class, 4f); break;
			case POISON: Buff.affect(ch, Poison.class).set(3 + buffedLvl()); break;
			case STUN:   if (Random.Int(3) == 0) Buff.prolong(ch, Paralysis.class, 1f); break;
			case ROOT:   Buff.prolong(ch, Roots.class, 2f); break;
			case SLOW:   Buff.prolong(ch, Slow.class, 3f); break;
		}
	}

	@Override
	public void onZap( Ballistica bolt ) {
		Char target = Actor.findChar(bolt.collisionPos);
		switch (kind){
			case SINGLE: default:
				hit(target, damageRoll());
				break;
			case BURST:
				for (int i = 0; i < count; i++) hit(target, damageRoll());
				break;
			case CONE: {
				ArrayList<Char> found = new ArrayList<>();
				for (int c : cone(bolt).cells){
					Char ch = Actor.findChar(c);
					if (foe(ch) && !found.contains(ch)) found.add(ch);
				}
				for (Char ch : found) hit(ch, damageRoll());
				break;
			}
			case PIERCE: {
				ArrayList<Char> found = new ArrayList<>();
				for (int i = 1; i <= lineReach(bolt); i++){
					Char ch = Actor.findChar(bolt.path.get(i));
					if (foe(ch)) found.add(ch);
				}
				for (Char ch : found) hit(ch, damageRoll());
				break;
			}
			case BLAST: {
				ArrayList<Integer> area = SKCombat.openArea(bolt.collisionPos, count);
				for (int c : area){
					if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(SmokeParticle.FACTORY, 2);
				}
				if (Dungeon.level.heroFOV[bolt.collisionPos]) CellEmitter.center(bolt.collisionPos).burst(BlastParticle.FACTORY, 20);
				Sample.INSTANCE.play(Assets.Sounds.BLAST);
				PixelScene.shake(1, 0.3f);
				for (Char ch : SKCombat.foesIn(curUser, area)) hit(ch, damageRoll());
				break;
			}
			case CHAIN: {
				ArrayList<Char> chain = new ArrayList<>();
				Char cur = foe(target) ? target : null;
				while (cur != null && chain.size() < count){
					chain.add(cur);
					Char next = null;
					int best = Integer.MAX_VALUE;
					for (Char ch : Actor.chars()){
						if (!foe(ch) || chain.contains(ch)) continue;
						int d = Dungeon.level.distance(cur.pos, ch.pos);
						if (d <= 3 && d < best && Dungeon.level.heroFOV[ch.pos]){
							best = d;
							next = ch;
						}
					}
					cur = next;
				}
				ArrayList<Lightning.Arc> arcs = new ArrayList<>();
				Char from = curUser;
				for (Char ch : chain){
					if (from.sprite != null && ch.sprite != null) arcs.add(new Lightning.Arc(from.sprite.center(), ch.sprite.center()));
					from = ch;
				}
				if (curUser.sprite != null && curUser.sprite.parent != null && !arcs.isEmpty()){
					curUser.sprite.parent.addToFront(new Lightning(arcs, null));
				}
				Sample.INSTANCE.play(Assets.Sounds.LIGHTNING);
				for (Char ch : chain) hit(ch, damageRoll());
				break;
			}
			case VOLLEY: {
				ArrayList<Char> foes = new ArrayList<>();
				if (foe(target)) foes.add(target);
				for (Char ch : Actor.chars()){
					if (foe(ch) && !foes.contains(ch) && Dungeon.level.heroFOV[ch.pos]
							&& Dungeon.level.distance(curUser.pos, ch.pos) <= reach){
						foes.add(ch);
					}
				}
				if (foes.isEmpty()) break;
				for (int i = 0; i < count; i++){
					Char ch = foes.get(i % foes.size());
					if (i > 0 && curUser.sprite != null && curUser.sprite.parent != null){
						((MissileSprite) curUser.sprite.parent.recycle(MissileSprite.class)).reset(
								curUser.sprite, ch.pos, new SKMobSprite.Shot(shot), null);
					}
					hit(ch, damageRoll());
				}
				break;
			}
		}
	}

	@Override
	public void fx( Ballistica bolt, Callback callback ) {
		if (kind == CONE){
			ConeAOE cone = cone(bolt);
			for (Ballistica ray : cone.outerRays){
				((MissileSprite) curUser.sprite.parent.recycle(MissileSprite.class)).reset(
						curUser.sprite, ray.path.get(ray.dist), new SKMobSprite.Shot(shot), null);
			}
			((MissileSprite) curUser.sprite.parent.recycle(MissileSprite.class)).reset(
					curUser.sprite, cone.coreRay.path.get(cone.coreRay.dist), new SKMobSprite.Shot(shot), callback);
			Sample.INSTANCE.play(sound, 1f, 1.2f);
		} else if (kind == PIERCE){
			int end = bolt.path.get(Math.max(1, lineReach(bolt)));
			if (beam){
				curUser.sprite.parent.add(new Beam.LightRay(curUser.sprite.center(), DungeonTilemap.raisedTileCenterToWorld(end)));
				Sample.INSTANCE.play(Assets.Sounds.RAY);
				callback.call();
			} else {
				((MissileSprite) curUser.sprite.parent.recycle(MissileSprite.class)).reset(
						curUser.sprite, end, new SKMobSprite.Shot(shot), callback);
				Sample.INSTANCE.play(sound, 1f, 0.9f);
			}
		} else if (kind == BURST){
			for (int i = 1; i < count; i++){
				((MissileSprite) curUser.sprite.parent.recycle(MissileSprite.class)).reset(
						curUser.sprite, bolt.collisionPos, new SKMobSprite.Shot(shot), null);
			}
			super.fx(bolt, callback);
		} else {
			super.fx(bolt, callback);
		}
	}

	@Override
	public String statsDesc() {
		String s = Messages.get(ArmoryGun.class, "stats_" + KINDS[kind], min(), max(), count, reach);
		if (effect != NONE) s += " " + Messages.get(ArmoryGun.class, "effect_" + EFFECTS[effect]);
		return s;
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc") + "\n\n" + Messages.get(ArmoryGun.class, "wand_like");
	}
}
