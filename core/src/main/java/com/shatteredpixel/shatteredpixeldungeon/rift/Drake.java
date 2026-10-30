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
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Electricity;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Lightning;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BlastParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.BArray;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.GameMath;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//elemental drake, joins goo on 5. 6 elements (RiftBosses picks), each w/ its own numbers and a
//special every ~6 turns, marked a turn early so you can dodge. it hits goo too -> sometimes they fight.
//drops an armor of its element for everyone (DrakeArmor)
public class Drake extends RiftBoss {

	public static final int INFERNAL = 0;
	public static final int OCEAN    = 1;
	public static final int CLOUD    = 2;
	public static final int MOUNTAIN = 3;
	public static final int HEXTECH  = 4;
	public static final int CHEMTECH = 5;
	public static final int ELEMENTS = 6;

	{
		spriteClass = DrakeSprite.class;
		HP = HT = 80;
		EXP = 10;
		defenseSkill = 8;

		HUNTING = new Hunting();
		state = HUNTING;
	}

	public int element = 0;

	private float abilityCooldown = 3;
	//announced attack: element of the attack (-1 if none), where it aims and the marked cells
	private int pendingType = -1;
	private int pendingAim = -1;
	private int[] pendingCells = new int[0];

	private float regen = 0;          //ocean: partial health
	private boolean zombieUsed = false; //chemtech: undying once below 30% health
	private float zombieLeft = 0;

	//damage of its special attacks: whose it is (boss rivalry) and how a hero died
	private final Strike strike = new Strike();

	public class Strike implements BossRivalry.Source, Hero.Doom {
		@Override
		public Char rivalrySource() {
			return Drake.this;
		}

		@Override
		public void onDeath() {
			Dungeon.fail(Drake.this);
			GLog.n(Messages.get(Drake.class, "kill", name()));
		}
	}

	public static int valid( int element ){
		return Math.max(0, Math.min(ELEMENTS-1, element));
	}

	public static int color( int element ){
		switch (element){
			case INFERNAL: default: return 0xFF5522;
			case OCEAN:    return 0x2F7FE0;
			case CLOUD:    return 0xC8DCF0;
			case MOUNTAIN: return 0x8A6440;
			case HEXTECH:  return 0x4CC8E8;
			case CHEMTECH: return 0x78C83C;
		}
	}

	// *** stats ***

	@Override
	public String name() {
		return Messages.get(Drake.class, "name_" + valid(element));
	}

	@Override
	public String description() {
		return Messages.get(Drake.class, "desc_" + valid(element))
				+ "\n\n" + Messages.get(Drake.class, "desc_common");
	}

	@Override
	public int damageRoll() {
		switch (element){
			case INFERNAL: return Random.NormalIntRange(3, 8);
			case CLOUD: case MOUNTAIN: return Random.NormalIntRange(2, 6);
			default: return Random.NormalIntRange(2, 7);
		}
	}

	@Override
	public int attackSkill( Char target ) {
		return 12;
	}

	@Override
	public int defenseSkill( Char enemy ) {
		int def = super.defenseSkill(enemy);
		//cloud: very hard to hit
		return element == CLOUD ? Math.round(def * 1.6f) : def;
	}

	@Override
	public int drRoll() {
		//mountain: thick armor
		if (element == MOUNTAIN) return super.drRoll() + Random.NormalIntRange(2, 6);
		return super.drRoll() + Random.NormalIntRange(0, 2);
	}

	@Override
	public float speed() {
		//cloud: very fast
		return element == CLOUD ? super.speed() * 1.5f : super.speed();
	}

	@Override
	public boolean isImmune( Class effect ) {
		if (element == INFERNAL && (effect == Burning.class || effect == Fire.class)) return true;
		if (element == HEXTECH && effect == Electricity.class) return true;
		if (element == CHEMTECH && ToxicGas.class.isAssignableFrom(effect)) return true;
		return super.isImmune(effect);
	}

	@Override
	public CharSprite sprite() {
		DrakeSprite s = new DrakeSprite();
		s.setElement(element);
		return s;
	}

	@Override
	protected Item riftLoot() {
		return DrakeArmor.forElement(element).identify(false);
	}

	@Override
	protected void onAdd() {
		if (firstAdded) comeOutReachable();
		super.onAdd();
	}

	//RiftBosses picks its cell by straight distance: if that cell is walled off from the fight
	//(a long way around to reach Goo), it comes out on a cell that is close by walking instead
	private void comeOutReachable(){
		Mob main = RiftBosses.mainBoss(Dungeon.level);
		if (main == null || pos < 0 || pos >= Dungeon.level.length()) return;
		PathFinder.buildDistanceMap(main.pos, BArray.or(Dungeon.level.passable, Dungeon.level.avoid, null));
		if (PathFinder.distance[pos] <= Dungeon.level.distance(pos, main.pos) + 4) return;
		int best = -1;
		int bestScore = -1;
		for (int c = 0; c < Dungeon.level.length(); c++){
			int walk = PathFinder.distance[c];
			if (walk < 2 || walk > 8) continue;
			if (Dungeon.level.solid[c] || !Dungeon.level.passable[c] || Dungeon.level.pit[c] || Actor.findChar(c) != null) continue;
			if (Dungeon.level.getTransition(c) != null) continue;
			int score = Integer.MAX_VALUE;
			for (Hero h : Party.living()){
				score = Math.min(score, Dungeon.level.distance(h.pos, c));
			}
			if (score > bestScore){
				bestScore = score;
				best = c;
			}
		}
		if (best == -1) return;
		pos = best;
		if (sprite != null) sprite.place(pos);
	}

	// *** turns ***

	@Override
	protected boolean act() {
		//an announced attack goes off on the next turn, whatever happened meanwhile
		if (pendingType >= 0) return release();
		return super.act();
	}

	@Override
	protected void spend( float time ) {
		super.spend(time);
		abilityCooldown -= time;

		int heal = 0;
		//ocean: regenerates 1 health every 3 turns
		if (element == OCEAN && HP < HT){
			regen += time / 3f;
		}
		//chemtech, undying: 2 health per turn
		if (zombieLeft > 0){
			float t = Math.min(time, zombieLeft);
			zombieLeft -= t;
			regen += 2*t;
			if (zombieLeft <= 0 && sprite != null) sprite.clearAura();
		}
		if (regen >= 1){
			heal = (int)regen;
			regen -= heal;
		}
		if (heal > 0 && HP < HT && isAlive()){
			heal = Math.min(heal, HT - HP);
			HP += heal;
			if (sprite != null && Dungeon.level.heroFOV[pos]){
				sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
			}
		}
	}

	@Override
	public void damage( int dmg, Object src ) {
		//chemtech, undying: half damage
		if (zombieLeft > 0 && dmg > 0) dmg = (dmg + 1) / 2;
		super.damage(dmg, src);
		if (element == CHEMTECH && !zombieUsed && isAlive() && HP < HT * 0.3f){
			zombieUsed = true;
			zombieLeft = 6;
			if (sprite != null){
				sprite.aura(color(CHEMTECH), 5);
				sprite.showStatus(CharSprite.WARNING, Messages.get(Drake.class, "zombie_status"));
			}
			if (Dungeon.level.heroFOV[pos]) GLog.w(Messages.get(Drake.class, "zombie", name()));
		}
	}

	@Override
	public void die( Object cause ) {
		pendingType = -1;
		pendingCells = new int[0];
		super.die(cause);
		if (Dungeon.level.heroFOV[pos]) GLog.p(Messages.get(Drake.class, "defeated", name()));
	}

	@Override
	public void updateSpriteState() {
		super.updateSpriteState();
		if (pendingType >= 0 && sprite instanceof DrakeSprite) ((DrakeSprite) sprite).charge();
		if (zombieLeft > 0 && sprite != null) sprite.aura(color(CHEMTECH), 5);
	}

	private class Hunting extends Mob.Hunting {
		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			if (enemyInFOV && enemy != null && abilityCooldown <= 0 && announce()){
				enemySeen = true;
				return true;
			}
			return super.act(enemyInFOV, justAlerted);
		}
	}

	// *** special attacks ***

	//marks the cells of its attack against the current enemy; false if it can't use it from here
	private boolean announce(){
		int dist = Dungeon.level.distance(pos, enemy.pos);
		int aim = enemy.pos;
		int[] cells;
		float delay = TICK;
		switch (element){
			case INFERNAL: case CHEMTECH: default: {
				//a lobbed fireball / flask that bursts in 3x3
				if (dist > 6 || !clearPath(aim)) return false;
				cells = area(aim, 1);
				break;
			}
			case OCEAN: {
				//a tidal bolt along a line
				if (dist > 7 || !clearPath(aim)) return false;
				Ballistica b = new Ballistica(pos, aim, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID);
				cells = toArray(b.subPath(1, b.dist));
				break;
			}
			case CLOUD: {
				//a gust in a cone that blows everyone away
				if (dist > 3) return false;
				cells = toArray(gust(aim).cells, pos);
				break;
			}
			case MOUNTAIN: {
				//a stomp around itself; two turns to get away
				if (dist > 2) return false;
				aim = pos;
				cells = toArray(areaList(pos, 2), pos);
				delay = 2*TICK;
				break;
			}
			case HEXTECH: {
				//a lightning bolt on the target's area that jumps to whoever is close
				if (dist > 6) return false;
				cells = area(aim, 1);
				break;
			}
		}
		if (cells.length == 0) return false;

		pendingType = valid(element);
		pendingAim = aim;
		pendingCells = cells;

		//don't punish heroes that are slow to act
		float d = delay;
		for (Hero h : Party.living()){
			d = Math.max(d, (float)Math.ceil(h.cooldown()));
		}
		d = GameMath.gate(delay, d, delay + 2*TICK);
		for (int c : cells){
			GameScene.targetedCell(c, d);
		}
		if (sprite != null){
			sprite.turnTo(pos, aim);
			if (sprite instanceof DrakeSprite) ((DrakeSprite) sprite).charge();
			if (Dungeon.level.heroFOV[pos]){
				sprite.showStatus(CharSprite.WARNING, "!!!");
				GLog.w(Messages.get(Drake.class, "warn_" + pendingType, name()));
			}
		}
		for (Hero h : Party.living()){
			h.interrupt();
		}
		spend(d);
		return true;
	}

	//the announced attack goes off
	private boolean release(){
		final int type = pendingType;
		final int aim = pendingAim;
		final int[] cells = pendingCells;
		pendingType = -1;
		pendingCells = new int[0];
		//with the turns spent announcing and releasing it, one attack every 5-6 turns
		abilityCooldown = Random.IntRange(4, 5);
		Invisibility.dispel(this);
		spend(TICK);

		boolean visible = Dungeon.level.heroFOV[pos] && sprite != null && sprite.parent != null;
		if (sprite != null) sprite.idle();

		if (visible && (type == INFERNAL || type == CHEMTECH || type == OCEAN)){
			int to = type == OCEAN ? boltTarget(aim) : aim;
			int fx = type == INFERNAL ? MagicMissile.FIRE : type == OCEAN ? MagicMissile.FROST : MagicMissile.SPECK + Speck.TOXIC;
			sprite.zap(to);
			MagicMissile.boltFromChar(sprite.parent, fx, sprite, to, new Callback() {
				@Override
				public void call() {
					resolve(type, aim, cells);
					next();
				}
			});
			return false;
		}
		if (visible) sprite.zap(aim);
		resolve(type, aim, cells);
		return true;
	}

	private void resolve( int type, int aim, int[] cells ){
		if (!isAlive()) return;
		boolean visible = Dungeon.level.heroFOV[pos] || anyVisible(cells);
		switch (type){
			case INFERNAL: {
				for (int c : cells){
					if (visible) CellEmitter.center(c).burst(BlastParticle.FACTORY, 4);
					if (visible) CellEmitter.get(c).burst(FlameParticle.FACTORY, 4);
				}
				if (visible){
					Sample.INSTANCE.play(Assets.Sounds.BLAST);
					PixelScene.shake(2, 0.3f);
				}
				for (Char ch : charsIn(cells)){
					ch.damage(Random.NormalIntRange(3, 7), strike);
					if (ch.isAlive() && !ch.isImmune(Burning.class)){
						//short enough not to burn scrolls (that starts on the 4th turn)
						Buff.affect(ch, Burning.class).reignite(ch, 3f);
					}
				}
				break;
			}
			case OCEAN: {
				int hitPos = boltTarget(aim);
				Char ch = Actor.findChar(hitPos);
				if (visible) CellEmitter.center(hitPos).burst(Speck.factory(Speck.STEAM), 6);
				if (ch != null && ch != this && ch.isAlive()){
					int dmg = Random.NormalIntRange(4, 9);
					int before = ch.HP + ch.shielding();
					ch.damage(dmg, strike);
					if (ch.isAlive()){
						Buff.detach(ch, Burning.class);
						Buff.affect(ch, Slow.class, 3f);
					}
					//the tide flows back into the drake
					int dealt = Math.max(0, before - (ch.HP + ch.shielding()));
					healSelf(dealt / 2);
				}
				if (visible) Sample.INSTANCE.play(Assets.Sounds.WATER);
				break;
			}
			case CLOUD: {
				if (visible && sprite != null && sprite.parent != null){
					for (Ballistica ray : gust(aim).outerRays){
						((MagicMissile) sprite.parent.recycle(MagicMissile.class)).reset(
								MagicMissile.FORCE_CONE, sprite, ray.path.get(ray.dist), null);
					}
					Sample.INSTANCE.play(Assets.Sounds.BLAST, 0.7f, 1.3f);
				}
				for (Char ch : charsIn(cells)){
					ch.damage(Random.NormalIntRange(2, 5), strike);
					if (ch.isAlive()){
						Ballistica dir = new Ballistica(pos, ch.pos, Ballistica.WONT_STOP);
						Ballistica trajectory = new Ballistica(ch.pos, dir.collisionPos, Ballistica.MAGIC_BOLT);
						WandOfBlastWave.throwChar(ch, trajectory, 3, false, true, this);
					}
				}
				break;
			}
			case MOUNTAIN: {
				for (int c : cells){
					if (visible) CellEmitter.get(c).start(Speck.factory(Speck.ROCK), 0.07f, 4);
				}
				if (visible){
					Sample.INSTANCE.play(Assets.Sounds.ROCKS);
					PixelScene.shake(3, 0.6f);
				}
				for (Char ch : charsIn(cells)){
					ch.damage(Random.NormalIntRange(4, 9), strike);
					if (ch.isAlive()) Buff.prolong(ch, Paralysis.class, 1f);
				}
				break;
			}
			case HEXTECH: {
				ArrayList<Char> chain = lightningChain(aim, cells);
				if (visible && sprite != null && sprite.parent != null){
					ArrayList<Lightning.Arc> arcs = new ArrayList<>();
					int from = pos;
					if (chain.isEmpty()){
						arcs.add(new Lightning.Arc(sprite.center(), aim));
					} else {
						arcs.add(new Lightning.Arc(sprite.center(), chain.get(0).pos));
						from = chain.get(0).pos;
						for (int i = 1; i < chain.size(); i++){
							arcs.add(new Lightning.Arc(from, chain.get(i).pos));
							from = chain.get(i).pos;
						}
					}
					sprite.parent.addToFront(new Lightning(arcs, null));
					Sample.INSTANCE.play(Assets.Sounds.LIGHTNING);
				}
				for (Char ch : chain){
					if (visible) CellEmitter.center(ch.pos).burst(SparkParticle.FACTORY, 4);
					ch.damage(Random.NormalIntRange(3, 7), strike);
					if (ch.isAlive()) Buff.affect(ch, Slow.class, 2f);
				}
				break;
			}
			case CHEMTECH: {
				for (int c : cells){
					GameScene.add(Blob.seed(c, c == aim ? 40 : 10, Toxin.class));
				}
				if (visible) Sample.INSTANCE.play(Assets.Sounds.GAS);
				break;
			}
		}
	}

	//first character hit by the hextech bolt, then up to two more it jumps to
	private ArrayList<Char> lightningChain( int aim, int[] cells ){
		ArrayList<Char> chain = new ArrayList<>();
		Char first = null;
		int best = Integer.MAX_VALUE;
		for (int c : cells){
			Char ch = Actor.findChar(c);
			if (ch == null || ch == this || !ch.isAlive()) continue;
			int d = Dungeon.level.distance(aim, c);
			if (d < best){
				best = d;
				first = ch;
			}
		}
		if (first == null) return chain;
		chain.add(first);
		Char cur = first;
		while (chain.size() < 3){
			Char next = null;
			int nextDist = Integer.MAX_VALUE;
			for (Char ch : Actor.chars()){
				if (ch == this || chain.contains(ch) || !ch.isAlive()) continue;
				int d = Dungeon.level.distance(cur.pos, ch.pos);
				if (d > 3 || d >= nextDist) continue;
				if (new Ballistica(cur.pos, ch.pos, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID).collisionPos != ch.pos) continue;
				next = ch;
				nextDist = d;
			}
			if (next == null) break;
			chain.add(next);
			cur = next;
		}
		return chain;
	}

	private ConeAOE gust( int aim ){
		Ballistica b = new Ballistica(pos, aim, Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID);
		return new ConeAOE(b, 4, 90, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID);
	}

	//where a bolt from the drake towards a cell stops (first character or wall)
	private int boltTarget( int aim ){
		if (aim == pos) return pos;
		return new Ballistica(pos, aim, Ballistica.PROJECTILE | Ballistica.IGNORE_SOFT_SOLID).collisionPos;
	}

	//a lobbed attack needs no wall between the drake and the cell
	private boolean clearPath( int cell ){
		return new Ballistica(pos, cell, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID).collisionPos == cell;
	}

	private void healSelf( int amount ){
		if (amount <= 0 || HP >= HT) return;
		amount = Math.min(amount, HT - HP);
		HP += amount;
		if (sprite != null && Dungeon.level.heroFOV[pos]){
			sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(amount), FloatingText.HEALING);
		}
	}


	//living characters on some cells, the drake excluded
	private ArrayList<Char> charsIn( int[] cells ){
		ArrayList<Char> result = new ArrayList<>();
		for (int c : cells){
			Char ch = Actor.findChar(c);
			if (ch != null && ch != this && ch.isAlive() && !result.contains(ch)) result.add(ch);
		}
		return result;
	}

	private static boolean anyVisible( int[] cells ){
		for (int c : cells){
			if (Dungeon.level.heroFOV[c]) return true;
		}
		return false;
	}

	//non-solid cells in a square around a cell
	private static ArrayList<Integer> areaList( int center, int radius ){
		ArrayList<Integer> result = new ArrayList<>();
		int w = Dungeon.level.width();
		int h = Dungeon.level.height();
		int cx = center % w;
		int cy = center / w;
		for (int y = cy - radius; y <= cy + radius; y++){
			for (int x = cx - radius; x <= cx + radius; x++){
				if (x < 0 || y < 0 || x >= w || y >= h) continue;
				int c = x + y*w;
				if (!Dungeon.level.solid[c]) result.add(c);
			}
		}
		return result;
	}

	private static int[] area( int center, int radius ){
		return toArray(areaList(center, radius), -1);
	}

	private static int[] toArray( Iterable<Integer> cells ){
		return toArray(cells, -1);
	}

	private static int[] toArray( Iterable<Integer> cells, int except ){
		ArrayList<Integer> list = new ArrayList<>();
		for (int c : cells){
			if (c != except && !list.contains(c)) list.add(c);
		}
		int[] result = new int[list.size()];
		for (int i = 0; i < result.length; i++) result[i] = list.get(i);
		return result;
	}

	//toxic cloud of the chemtech drake: it is the drake's, so it can anger the other boss
	public static class Toxin extends ToxicGas implements BossRivalry.Source {
		@Override
		public Char rivalrySource() {
			for (Mob m : Dungeon.level.mobs){
				if (m instanceof Drake && m.isAlive()) return m;
			}
			return null;
		}
	}

	// *** saving ***

	private static final String ELEMENT     = "element";
	private static final String COOLDOWN    = "ability_cooldown";
	private static final String PENDING     = "pending";
	private static final String AIM         = "aim";
	private static final String CELLS       = "cells";
	private static final String REGEN       = "regen";
	private static final String ZOMBIE_USED = "zombie_used";
	private static final String ZOMBIE_LEFT = "zombie_left";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(ELEMENT, element);
		bundle.put(COOLDOWN, abilityCooldown);
		bundle.put(PENDING, pendingType);
		bundle.put(AIM, pendingAim);
		bundle.put(CELLS, pendingCells);
		bundle.put(REGEN, regen);
		bundle.put(ZOMBIE_USED, zombieUsed);
		bundle.put(ZOMBIE_LEFT, zombieLeft);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		element = bundle.getInt(ELEMENT);
		abilityCooldown = bundle.getFloat(COOLDOWN);
		pendingType = bundle.contains(PENDING) ? bundle.getInt(PENDING) : -1;
		pendingAim = bundle.getInt(AIM);
		pendingCells = bundle.contains(CELLS) ? bundle.getIntArray(CELLS) : new int[0];
		regen = bundle.getFloat(REGEN);
		zombieUsed = bundle.getBoolean(ZOMBIE_USED);
		zombieLeft = bundle.getFloat(ZOMBIE_LEFT);
	}
}
