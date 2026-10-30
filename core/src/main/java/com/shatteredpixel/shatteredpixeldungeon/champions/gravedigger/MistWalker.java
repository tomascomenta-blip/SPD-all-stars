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

package com.shatteredpixel.shatteredpixeldungeon.champions.gravedigger;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//little grave spirit. fast, dies quick, fights for the gravedigger (and whoever he's paired with)
//stats copied from the owner on raise
//NOT an intelligentAlly for holdAllies -> stays behind when you take the stairs. intended
public class MistWalker extends NPC {

	{
		spriteClass = Sprite.class;

		HP = HT = 8;
		defenseSkill = 3;
		baseSpeed = 1.5f;

		alignment = Alignment.ALLY;
		WANDERING = new Following();
		state = WANDERING;

		//same prio as the other summons, como siempre
		actPriority = MOB_PRIO + 1;

		properties.add(Property.UNDEAD);
		immunities.add(AllyBuff.class);
	}

	public int ownerIndex = 0;
	public int power = 1;
	public float dmgMulti = 1f;
	public float timeLeft = 25f;
	public boolean fromMaiden = false;
	public int burst = 0;          //mistlord burst pts
	public float idle = 0;

	//nothing to fight for 5 turns -> back in the ground
	public static final float IDLE_FADE = 5f;

	public static float duration( Hero owner ){
		return owner.subClass == HeroSubClass.MISTLORD ? 35f : 25f;
	}

	public static int health( Hero owner ){
		float multi = 1f + 0.25f*owner.pointsInTalent(Talent.GRA_HARD_BONES);
		if (owner.subClass == HeroSubClass.MISTLORD) multi *= 1.5f;
		return Math.round((4 + 1.5f*owner.lvl) * multi);
	}

	public static float dmgMulti( Hero owner ){
		return owner.subClass == HeroSubClass.MISTLORD ? 1.25f : 1f;
	}

	public static int minDmg( int power ){
		return 1 + power/5;
	}

	public static int maxDmg( int power ){
		return 2 + power/3;
	}

	public static MistWalker raise( Hero owner, int cell, boolean fromMaiden ){
		MistWalker w = new MistWalker();
		w.ownerIndex = ChampionHooks.indexOf(owner);
		w.power = owner.lvl;
		w.dmgMulti = dmgMulti(owner);
		w.timeLeft = duration(owner);
		w.fromMaiden = fromMaiden;
		if (owner.subClass == HeroSubClass.MISTLORD) w.burst = owner.pointsInTalent(Talent.ML_MIST_BURST);
		w.HP = w.HT = health(owner);
		w.defenseSkill = 3 + owner.lvl/2;
		w.pos = cell;
		GameScene.add(w);
		Dungeon.level.occupyCell(w);
		if (Dungeon.level.heroFOV[cell]){
			CellEmitter.get(cell).burst(MistParticle.UP, 8);
		}
		return w;
	}

	//up to 'count' walkers around center, returns how many actually came up
	//outer ring (dist 2) fills first and 2 cells next to the center stay free, otherwise
	//your own walkers box you in. very funny the first time, less the 5th
	public static int raiseAround( Hero owner, int center, int count, boolean fromMaiden ){
		if (count <= 0) return 0;
		ArrayList<Integer> near = new ArrayList<>();
		ArrayList<Integer> ring2 = new ArrayList<>();
		ArrayList<Integer> ring3 = new ArrayList<>();
		for (int c : freeCellsAround(center, 3)){
			int d = Dungeon.level.distance(c, center);
			if (d <= 1) near.add(c);
			else if (d == 2) ring2.add(c);
			else ring3.add(c);
		}
		ArrayList<Integer> cells = new ArrayList<>(ring2);
		for (int i = 0; i < near.size() - 2; i++) cells.add(near.get(i));
		cells.addAll(ring3);
		int raised = 0;
		for (int c : cells){
			if (raised >= count) break;
			raise(owner, c, fromMaiden);
			raised++;
		}
		if (raised > 0) Sample.INSTANCE.play(Assets.Sounds.GHOST, 0.8f, 0.9f);
		return raised;
	}

	//closest first, fixed order (coop). beyond 1 step it has to be in sight or they spawn behind walls
	public static ArrayList<Integer> freeCellsAround( int center, int radius ){
		ArrayList<Integer> result = new ArrayList<>();
		int w = Dungeon.level.width();
		int cx = center % w;
		int cy = center / w;
		for (int r = 1; r <= radius; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max(Math.abs(dx), Math.abs(dy)) != r) continue;
					int x = cx + dx;
					int y = cy + dy;
					if (x < 0 || y < 0 || x >= w || y >= Dungeon.level.height()) continue;
					int c = x + y*w;
					if (!isFree(c)) continue;
					if (r > 1 && !Dungeon.level.heroFOV[c]) continue;
					result.add(c);
				}
			}
		}
		return result;
	}

	static boolean isFree( int c ){
		return Dungeon.level.insideMap(c) && !Dungeon.level.solid[c] && Dungeon.level.passable[c]
				&& !Dungeon.level.avoid[c] && Actor.findChar(c) == null;
	}

	static int freeNeighbours( int cell ){
		int free = 0;
		for (int n : PathFinder.NEIGHBOURS8){
			if (isFree(cell + n)) free++;
		}
		return free;
	}

	public boolean ownedBy( Hero hero ){
		return ChampionHooks.indexOf(hero) == ownerIndex;
	}

	//owner, or the context hero if the owner's down
	private Hero leader(){
		Hero owner = ChampionHooks.heroByIndex(ownerIndex);
		if (owner != null && owner.isAlive() && !owner.coopDown) return owner;
		return Dungeon.hero;
	}

	@Override
	public int attackSkill( Char target ) {
		return 10 + power;
	}

	@Override
	public int damageRoll() {
		return Math.round(Random.NormalIntRange(minDmg(power), maxDmg(power)) * dmgMulti);
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, power/4);
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc(enemy, damage);
		if (enemy.buff(MistMark.class) != null){
			damage = Math.round(damage * MistMark.DAMAGE_BONUS);
		}
		ShepherdOfSouls.communion(ChampionHooks.heroByIndex(ownerIndex), damage);
		return damage;
	}

	@Override
	protected Char chooseEnemy() {
		Char marked = GravediggerHooks.markedTarget(this, enemy);
		if (marked != null) return marked;
		//HACK: flip intelligentAlly on just while picking a target so it ignores sleeping/wandering mobs,
		//then off again so holdAllies doesn't drag them downstairs
		intelligentAlly = true;
		try {
			return super.chooseEnemy();
		} finally {
			intelligentAlly = false;
		}
	}

	@Override
	protected boolean act() {
		if (timeLeft <= 0.01f || idle >= IDLE_FADE){
			if (Dungeon.level.heroFOV[pos]) CellEmitter.get(pos).burst(MistParticle.UP, 6);
			die(null);
			return true;
		}
		return super.act();
	}

	@Override
	protected void spend( float time ) {
		super.spend(time);
		timeLeft = Math.max(0, timeLeft - time);
		if (state == HUNTING && enemy != null && enemy.isAlive()) idle = 0;
		else idle += time;
	}

	@Override
	public void die( Object cause ) {
		//mistlord burst: pops into mist, dmg + mark around
		if (burst > 0 && pos >= 0 && pos < Dungeon.level.length()){
			int points = burst;
			burst = 0;
			int dmg = points * (1 + power/4);
			if (Dungeon.level.heroFOV[pos]) CellEmitter.center(pos).burst(MistParticle.FACTORY, 14);
			for (int n : PathFinder.NEIGHBOURS8){
				Char ch = Actor.findChar(pos + n);
				if (ch != null && ch.alignment == Alignment.ENEMY && ch.isAlive()){
					ch.damage(dmg, this);
					if (ch.isAlive()){
						MistMark m = Buff.prolong(ch, MistMark.class, 4f);
						m.owner = ownerIndex;
					}
				}
			}
		}
		super.die(cause);
	}

	@Override
	public String description() {
		String d = Messages.get(this, "desc", Math.round(minDmg(power)*dmgMulti), Math.round(maxDmg(power)*dmgMulti));
		d += "\n\n" + Messages.get(this, "desc_time", Math.max(1, Math.round(timeLeft)), Math.round(IDLE_FADE));
		return d;
	}

	private static final String OWNER = "owner";
	private static final String POWER = "power";
	private static final String DMG = "dmg";
	private static final String TIME = "time";
	private static final String MAIDEN = "maiden";
	private static final String BURST = "burst";
	private static final String IDLE = "idle";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(OWNER, ownerIndex);
		bundle.put(POWER, power);
		bundle.put(DMG, dmgMulti);
		bundle.put(TIME, timeLeft);
		bundle.put(MAIDEN, fromMaiden);
		bundle.put(BURST, burst);
		bundle.put(IDLE, idle);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		ownerIndex = bundle.getInt(OWNER);
		power = bundle.getInt(POWER);
		dmgMulti = bundle.getFloat(DMG);
		timeLeft = bundle.getFloat(TIME);
		fromMaiden = bundle.getBoolean(MAIDEN);
		burst = bundle.getInt(BURST);
		idle = bundle.getFloat(IDLE);
		defenseSkill = 3 + power/2;
	}

	//tags along a step behind the owner, attacks whatever it sees.
	//steps aside if a hero next to it is cramped, and if it gets stuck in a corridor it just blinks to the owner
	private class Following extends Mob.Wandering {

		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			if (enemyInFOV){
				enemySeen = true;
				notice();
				alerted = true;
				state = HUNTING;
				target = enemy.pos;
				return true;
			}
			enemySeen = false;
			int oldPos = pos;
			Hero leader = leader();

			int away = makeWayCell(pos);
			if (away != -1){
				target = away;
				if (getCloser(away)){
					spend(1 / speed());
					return moveSprite(oldPos, pos);
				}
			}

			if (Dungeon.level.distance(pos, leader.pos) <= 2){
				spend(TICK);
				return true;
			}
			target = leader.pos;
			if (getCloser(target)){
				spend(1 / speed());
				return moveSprite(oldPos, pos);
			}
			int c = blinkCell(leader.pos);
			if (c != -1) blink(MistWalker.this, c);
			spend(TICK);
			return true;
		}
	}

	//where to step so a cramped hero next to us gets room. -1 = nobody needs it
	static int makeWayCell( int pos ){
		for (Hero h : Party.living()){
			if (Dungeon.level.adjacent(pos, h.pos) && freeNeighbours(h.pos) < 2){
				for (int n : PathFinder.NEIGHBOURS8){
					int c = pos + n;
					if (isFree(c) && !Dungeon.level.adjacent(c, h.pos)) return c;
				}
			}
		}
		return -1;
	}

	//instant move, mist fx
	static void blink( Mob ally, int cell ){
		if (Dungeon.level.heroFOV[ally.pos]) CellEmitter.get(ally.pos).burst(MistParticle.FACTORY, 6);
		ally.move(cell, false);
		if (ally.sprite != null){
			ally.sprite.interruptMotion();
			ally.sprite.place(cell);
		}
		Dungeon.level.occupyCell(ally);
		if (Dungeon.level.heroFOV[cell]) CellEmitter.get(cell).burst(MistParticle.UP, 6);
	}

	//2 steps from the hero (or adjacent if there's room)
	static int blinkCell( int heroPos ){
		int fallback = -1;
		for (int c : freeCellsAround(heroPos, 2)){
			if (!Dungeon.level.adjacent(c, heroPos)) return c;
			if (fallback == -1 && freeNeighbours(heroPos) > 2) fallback = c;
		}
		return fallback;
	}

	//16x16. idle 0-1 run 2-5 atk 6-8 die 9-11 cast 12-14
	public static class Sprite extends MobSprite {

		public Sprite() {
			super();
			texture(Assets.Sprites.MIST_WALKER);
			TextureFilm frames = new TextureFilm(texture, 16, 16);

			idle = new Animation(3, true);
			idle.frames(frames, 0, 0, 1, 1);

			run = new Animation(14, true);
			run.frames(frames, 2, 3, 4, 5);

			attack = new Animation(14, false);
			attack.frames(frames, 6, 7, 8, 0);

			die = new Animation(10, false);
			die.frames(frames, 9, 10, 11);

			zap = new Animation(12, false);
			zap.frames(frames, 12, 13, 14, 0);

			play(idle);
		}

		@Override
		public int blood() {
			return 0xFF88CCBB;
		}
	}
}
