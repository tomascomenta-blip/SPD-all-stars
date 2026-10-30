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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.quests;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SpecialRoom;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/*
 * Grey Tusk, alpha boar of the forest. 4 parts
 *  I    gores + charges along marked lines, calls its young once
 *  --   at 3/4 hp runs off to another part of the floor, smashing walls on the way
 *  II   stomp (marked ring, slows), charges go through walls and leave it dazed
 *  --   1/2 hp: off again
 *  III  angry. double charge, calls the young again
 *  --   1/4: runs to its den
 *  IV   last stand in the den, everything more often
 *
 * can't be hurt while running. snares (HuntQuest.HunterTrap) catch it: can't move, x1.5 dmg
 * for a few turns, and it counts for the quest bonus
 */
public class GreyTusk extends MiniBoss {

	{
		spriteClass = GreyTuskSprite.class;
		HP = HT = 55;
		defenseSkill = 6;
		EXP = 20;
		maxLvl = 12;
		lootChance = 0;
	}

	private static final int CHARGE = 0, STOMP = 1;

	public int[] stops;             //where it runs: 2 rooms + the den (null in the priest's clearing)
	public boolean fleeing = false;
	public boolean waiting = false; //done running, licking its wounds until somebody shows up
	private int fleeTarget = -1;
	private int fleeTurns = 0;
	public int trapped = 0;         //snares stepped on
	private int snared = 0;         //turns left of extra dmg
	private int dazed = 0;
	private int called = 0;
	private boolean secondCharge = false;
	private float stompAgain = 0; //stomp cripples, so the next one waits or you never get out of the ring. ojo con esto

	public GreyTusk(){
		super();
		int d = Math.max(2, Math.min(4, Dungeon.depth));
		HP = HT = 45 + 5 * (d - 2);
	}

	@Override
	public String barAsset() {
		return QuestAssets.BAR_GREY_TUSK;
	}

	@Override
	protected String music() {
		return Assets.Music.SEWERS_TENSE;
	}

	@Override
	public String status() {
		if (fleeing) return Messages.get(GreyTusk.class, "status_flee");
		if (waiting) return Messages.get(GreyTusk.class, "status_wait");
		if (snared > 0) return Messages.get(GreyTusk.class, "status_snared");
		if (dazed > 0) return Messages.get(GreyTusk.class, "status_dazed");
		return Messages.get(GreyTusk.class, "status_" + Math.min(4, phase));
	}

	@Override
	public int barColor() {
		if (fleeing || waiting) return 0xFF8A8A8A;
		if (snared > 0) return 0xFFE0C040;
		return phase >= 3 ? 0xFFD01A10 : 0xFFB03A2A;
	}

	private int depthBonus(){
		return Math.max(0, Math.min(4, Dungeon.depth) - 2);
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(2 + depthBonus() / 2, 6 + depthBonus() / 2 + phase);
	}

	@Override
	public int attackSkill( Char target ) {
		return 11 + depthBonus();
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 2);
	}

	@Override
	protected int specialCooldown() {
		return phase >= 4 ? 2 : phase >= 3 ? 3 : 4;
	}

	@Override
	protected void onStart() {
		yell(Messages.get(GreyTusk.class, "notice"));
		Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 1f, 0.7f);
		PixelScene.shake(2, 0.5f);
	}

	@Override
	protected void onIntro( int left ) {
		if (sprite != null) sprite.showStatus(0xFF4040, "!");
		if (left == 0){
			cooldown = 1;
			if (called == 0) callYoung(2);
		}
	}

	@Override
	public boolean isInvulnerable( Class effect ) {
		return fleeing || super.isInvulnerable(effect);
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (fleeing) return;
		if (snared > 0) dmg = dmg * 3 / 2;
		if (waiting){
			waiting = false;
			MiniBossHud.banner(Messages.get(GreyTusk.class, "back"));
		}
		super.damage(dmg, src);
		if (!isAlive() || stops == null) return;
		if (phase == 1 && HP * 4 <= HT * 3) flee(0, 2);
		else if (phase == 2 && HP * 2 <= HT) flee(1, 3);
		else if (phase == 3 && HP * 4 <= HT) flee(2, 4);
	}

	@Override
	protected boolean act() {
		if (sprite instanceof GreyTuskSprite) ((GreyTuskSprite) sprite).angry(phase >= 3);
		if (snared > 0) snared--;
		if (fleeing){
			runStep();
			return true;
		}
		if (waiting){
			if (heroNear(4)){
				waiting = false;
				state = HUNTING;
				MiniBossHud.banner(Messages.get(GreyTusk.class, "back"));
				yell(Messages.get(GreyTusk.class, "notice_" + Math.min(4, phase)));
			} else {
				if (HP < HT && Random.Int(4) == 0) HP++;
				spend(TICK);
				return true;
			}
		}
		if (dazed > 0){
			dazed--;
			spend(TICK);
			return true;
		}
		return super.act();
	}

	private boolean heroNear( int dist ){
		for (Hero h : Party.living()){
			if (Dungeon.level.distance(h.pos, pos) <= dist) return true;
			if (fieldOfView != null && fieldOfView[h.pos] && Dungeon.level.distance(h.pos, pos) <= 7) return true;
		}
		return false;
	}

	// *** running away ***

	private void flee( int stop, int nextPhase ){
		newPhase(nextPhase);
		fleeing = true;
		waiting = false;
		pendingKind = -1;
		pendingCells = new int[0];
		fleeTurns = 0;
		fleeTarget = stops[Math.min(stop, stops.length - 1)];
		yell(Messages.get(GreyTusk.class, "flee"));
		Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 1f, 0.6f);
		ArrayList<Integer> path = breakThrough(pos, fleeTarget);
		//GLog.i("tusk hp " + HP + "/" + HT + " -> " + fleeTarget);
		//whatever it smashes stays there for the hunters to find
		if (path.size() >= 4){
			int[] kinds = stop == 2 ? new int[]{HuntQuest.FIND_SNARE, HuntQuest.FIND_CAMP}
					: stop == 1 ? new int[]{HuntQuest.FIND_SNARE, HuntQuest.FIND_BERRIES}
					: new int[]{HuntQuest.FIND_SNARE, HuntQuest.FIND_SPEARS};
			HuntQuest.placeFind(path.get(path.size() / 3), kinds[0], path);
			HuntQuest.placeFind(path.get(path.size() * 2 / 3), kinds[1], path);
		}
		CoopLog.line("grey tusk flees to " + fleeTarget + " phase " + nextPhase + " path " + path.size());
	}

	//straight line to a cell knocking walls down on the way. never the outer walls or special room walls
	//(no free vaults, nice try). returns the path cells
	private ArrayList<Integer> breakThrough( int from, int to ){
		Level L = Dungeon.level;
		ArrayList<Integer> cells = new ArrayList<>();
		Ballistica b = new Ballistica(from, to, Ballistica.STOP_TARGET);
		boolean broke = false;
		for (int c : b.subPath(1, b.dist)){
			cells.add(c);
			if (!L.solid[c]) continue;
			if (!breakable(L, c)) continue;
			Level.set(c, Terrain.EMPTY_DECO, L);
			GameScene.updateMap(c);
			if (L.heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.ROCK), 4);
			broke = true;
		}
		if (broke){
			Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.8f);
			PixelScene.shake(3, 0.6f);
			Dungeon.observe();
		}
		return cells;
	}

	static boolean breakable( Level L, int c ){
		int w = L.width();
		int x = c % w, y = c / w;
		if (x <= 0 || y <= 0 || x >= w - 1 || y >= L.height() - 1) return false;
		int t = L.map[c];
		if (t != Terrain.WALL && t != Terrain.WALL_DECO) return false;
		if (L instanceof RegularLevel){
			for (int n : PathFinder.NEIGHBOURS9){
				Room r = ((RegularLevel) L).room(c + n);
				if (r instanceof SpecialRoom) return false;
			}
		}
		return true;
	}

	private void runStep(){
		fleeTurns++;
		Level L = Dungeon.level;
		boolean arrived = L.distance(pos, fleeTarget) <= 1 || fleeTurns > 40;
		if (!arrived){
			int old = pos;
			if (getCloser(fleeTarget)){
				HuntQuest.HuntField.getOrMake(L).track(old, pos);
				spend(1 / 3f);
				moveSprite(old, pos);
				return;
			}
			if (fleeTurns > 6) arrived = true;
		}
		if (arrived){
			fleeing = false;
			waiting = true;
			state = WANDERING;
			target = pos;
			if (fleeTarget == stops[stops.length - 1]){
				callYoung(1);
				MiniBossHud.banner(Messages.get(GreyTusk.class, "den"));
			}
			CoopLog.line("grey tusk waits at " + pos + " hp " + HP + "/" + HT);
		}
		spend(TICK);
	}

	@Override
	public void move( int step, boolean travelling ) {
		super.move(step, travelling);
		if (HuntQuest.HuntField.armed(pos)) snare();
	}

	private void snare(){
		HuntQuest.HuntField.getOrMake(Dungeon.level).spring(pos, true);
		trapped++;
		snared = 4;
		Buff.prolong(this, Roots.class, 2f);
		super.damage(Math.max(3, HT * 8 / 100), this);
		Sample.INSTANCE.play(Assets.Sounds.TRAP);
		if (sprite != null) sprite.showStatus(0xFFE040, Messages.get(GreyTusk.class, "snared"));
		GLog.p(Messages.get(GreyTusk.class, "snared_log", trapped));
		CoopLog.line("grey tusk snared " + trapped);
	}

	private void callYoung( int n ){
		called++;
		int made = 0;
		for (int k : PathFinder.NEIGHBOURS8){
			if (made >= n) break;
			int c = pos + k;
			if (!Dungeon.level.passable[c] || Actor.findChar(c) != null || Dungeon.level.pit[c]) continue;
			Boarlet b = new Boarlet();
			b.summoned();
			b.pos = c;
			GameScene.add(b);
			b.state = b.HUNTING;
			if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.DUST), 4);
			made++;
		}
		if (made > 0) yell(Messages.get(GreyTusk.class, "call"));
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = Dungeon.level.distance(pos, enemy.pos);
		if (phase == 3 && called < 2){
			callYoung(1);
			spend(TICK);
			return true;
		}
		if (phase >= 2 && d <= 2 && Actor.now() >= stompAgain && Random.Int(2) == 0){
			stompAgain = Actor.now() + 6;
			ArrayList<Integer> ring = new ArrayList<>();
			for (int k : PathFinder.NEIGHBOURS8) ring.add(pos + k);
			if (phase >= 3){
				int w = Dungeon.level.width();
				for (int k : new int[]{2, -2, 2 * w, -2 * w}) ring.add(pos + k);
			}
			announce(STOMP, ring, enemy.pos);
			return true;
		}
		if (d >= 2 && d <= 7){
			ArrayList<Integer> line = line(enemy.pos, 7);
			if (!line.contains(enemy.pos)) return false;
			announce(CHARGE, line, enemy.pos);
			if (sprite != null) sprite.showStatus(0xFF4040, Messages.get(GreyTusk.class, "charge"));
			return true;
		}
		return false;
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		if (kind == STOMP){
			Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 0.8f);
			PixelScene.shake(2, 0.4f);
			for (Char ch : SKCombat.foesIn(this, cells)){
				ch.damage(Math.max(1, Random.NormalIntRange(2 + depthBonus() / 2, 5 + depthBonus() / 2) - ch.drRoll() / 2), this);
				Buff.prolong(ch, Cripple.class, 2f);
			}
			for (int c : cells) if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.DUST), 2);
			return;
		}
		charge(cells);
		if (phase >= 3 && !secondCharge && isAlive() && !fleeing && dazed == 0 && snared == 0){
			//again, right away. otra vez!!
			Char enemy = this.enemy;
			if (enemy != null && enemy.isAlive()){
				ArrayList<Integer> line = line(enemy.pos, 7);
				if (line.contains(enemy.pos) && line.size() >= 2){
					secondCharge = true;
					announce(CHARGE, line, enemy.pos);
					return;
				}
			}
		}
		secondCharge = false;
	}

	private void charge( ArrayList<Integer> cells ){
		Level L = Dungeon.level;
		int start = pos;
		Char victim = null;
		int last = pos;
		for (int c : cells){
			Char ch = Actor.findChar(c);
			if (ch != null){
				if (SKCombat.foes(this, ch)) victim = ch;
				break;
			}
			if (L.solid[c] || !L.passable[c]) break;
			move(c, false);
			last = c;
			if (snared > 0 && trapped > 0 && HuntQuest.HuntField.get(L) != null && HuntQuest.HuntField.get(L).traps[c] == HuntQuest.HuntField.BLOODY) break;
		}
		if (pos != start && sprite != null){
			sprite.move(start, pos);
			if (L.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.TRAMPLE);
		}
		if (victim != null){
			attack(victim, 1.5f, 0, 2f);
			if (victim.isAlive() && !victim.properties().contains(Property.IMMOVABLE)){
				Ballistica traj = new Ballistica(victim.pos, victim.pos + (victim.pos - pos), Ballistica.MAGIC_BOLT);
				WandOfBlastWave.throwChar(victim, traj, 2, false, true, this);
			}
			return;
		}
		//hit a wall: from phase 2 on it goes through and gets dazed. cartoon physics
		if (phase >= 2 && last != -1 && cells.size() > 0){
			int w = L.width();
			int dir = cells.size() >= 2 ? cells.get(cells.size() - 1) - cells.get(cells.size() - 2) : cells.get(0) - start;
			int wall = pos + dir;
			if (wall > 0 && wall < L.length() && L.solid[wall] && breakable(L, wall) && Math.abs(dir) <= w + 1){
				Level.set(wall, Terrain.EMPTY_DECO, L);
				GameScene.updateMap(wall);
				if (L.heroFOV[wall]) CellEmitter.get(wall).burst(Speck.factory(Speck.ROCK), 6);
				Sample.INSTANCE.play(Assets.Sounds.ROCKS);
				PixelScene.shake(2, 0.4f);
				Dungeon.observe();
				dazed = 2;
				if (sprite != null) sprite.showStatus(0xFFE040, Messages.get(GreyTusk.class, "dazed"));
			}
		}
	}

	@Override
	public void die( Object cause ) {
		int at = pos;
		super.die(cause);
		HuntQuest.Q.slain = true;
		HuntQuest.Q.bonus = trapped >= 2;
		if (stops != null || !HuntQuest.Q.failed){
			Dungeon.level.drop(new HuntQuest.GreyTuskFang(), at).sprite.drop();
		}
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (m instanceof Boarlet && m.alignment == Alignment.ENEMY) m.beckon(at);
		}
		GLog.p(Messages.get(GreyTusk.class, trapped >= 2 ? "slain_bonus" : "slain"));
		CoopLog.line("grey tusk slain trapped=" + trapped);
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		if (stops != null) bundle.put("stops", stops);
		bundle.put("fleeing", fleeing);
		bundle.put("waiting", waiting);
		bundle.put("flee_target", fleeTarget);
		bundle.put("flee_turns", fleeTurns);
		bundle.put("trapped", trapped);
		bundle.put("snared", snared);
		bundle.put("dazed", dazed);
		bundle.put("called", called);
		bundle.put("stomp_again", stompAgain);
		bundle.put("second", secondCharge);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		stompAgain = bundle.getFloat("stomp_again");
		stops = bundle.contains("stops") ? bundle.getIntArray("stops") : null;
		fleeing = bundle.getBoolean("fleeing");
		waiting = bundle.getBoolean("waiting");
		fleeTarget = bundle.getInt("flee_target");
		fleeTurns = bundle.getInt("flee_turns");
		trapped = bundle.getInt("trapped");
		snared = bundle.getInt("snared");
		dazed = bundle.getInt("dazed");
		called = bundle.getInt("called");
		secondCharge = bundle.getBoolean("second");
	}

	public static class GreyTuskSprite extends SKMobSprite {
		public GreyTuskSprite(){
			super();
			setup(QuestAssets.GREY_TUSK, 26, 21);
		}
		void angry( boolean on ){
			row(on ? 1 : 0);
		}
	}

	// ---- boarlets ----

	public static class Boarlet extends SKMob {

		{
			spriteClass = BoarletSprite.class;
			HP = HT = 10;
			defenseSkill = 4;
			EXP = 1;
			maxLvl = 6;
		}

		private int lvl = 0;

		//the hunting horn ones level up with it
		void grow( int level ){
			lvl = level;
			HP = HT = 14 + 3 * level;
		}

		@Override
		public int damageRoll() {
			return Random.NormalIntRange(1 + lvl / 2, 4 + lvl);
		}

		@Override
		public int attackSkill( Char target ) {
			return 10 + lvl;
		}

		@Override
		public int drRoll() {
			return super.drRoll() + Random.NormalIntRange(0, 1 + lvl / 4);
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("lvl", lvl);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			lvl = bundle.getInt("lvl");
		}

		//horn boarlets go home after a while. school night
		public static class Lifetime extends FlavourBuff {
			public int left = 25;

			@Override
			public boolean act() {
				if (--left <= 0){
					if (target.sprite != null) CellEmitter.get(target.pos).burst(Speck.factory(Speck.DUST), 4);
					target.die(null);
					return true;
				}
				spend(TICK);
				return true;
			}

			@Override
			public void storeInBundle( Bundle bundle ) {
				super.storeInBundle(bundle);
				bundle.put("left", left);
			}

			@Override
			public void restoreFromBundle( Bundle bundle ) {
				super.restoreFromBundle(bundle);
				left = bundle.getInt("left");
			}
		}
	}

	public static class BoarletSprite extends SKMobSprite {
		public BoarletSprite(){
			super();
			setup(QuestAssets.BOARLET, 13, 10);
		}
	}
}
