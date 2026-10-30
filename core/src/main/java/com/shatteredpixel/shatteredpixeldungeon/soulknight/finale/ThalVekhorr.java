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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.finale;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Amok;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corruption;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicalSleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Recharging;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Sleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopBalance;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKBulletField;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKWorlds;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.finale.reward.ThalRewards;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.kraken.Kraken;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.props.SKProps;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Arrays;

//thal-vekhorr, end of the hours. a colossus of melting clocks hanging in the sky over the dungeon.
//never on the floor himself, he's a director like the kraken: hands, portals and (last act) the
//core are what gets hit, and they pass it up. 6 acts: one per world (the floor turns into it, its
//monsters and its boss's echo come through) and then the end of time, everything at once
public class ThalVekhorr extends SKMob {

	public static final int BASE_HP = 4000;
	public static final float HAND_PASS = 0.6f;

	//hp left when each act ends
	private static final float[] ACT_END = {1f, 0.84f, 0.68f, 0.52f, 0.36f, 0.20f, 0f};
	private static final int[] ACT_WORLD = {0, SKWorlds.FOREST, SKWorlds.KINGDOM, SKWorlds.STARSHIP,
			SKWorlds.UNDERSEA, SKWorlds.FORGOTTEN, 0};

	private static final int BUBBLES = 0, PUDDLES = 1, SWAP = 2, REWIND = 3, STOP = 4, ECHOES = 5;
	//spells each act knows. it learns as it bleeds
	private static final int[][] ACT_MAGIC = {
			{},
			{BUBBLES, PUDDLES},
			{BUBBLES, PUDDLES, SWAP},
			{BUBBLES, PUDDLES, SWAP, REWIND},
			{PUDDLES, SWAP, REWIND, STOP},
			{BUBBLES, SWAP, REWIND, STOP, ECHOES},
			{BUBBLES, PUDDLES, SWAP, REWIND, STOP, ECHOES}
	};

	private static final int P_SLAM = 0, P_HAZARD = 1, P_STOP = 2, P_SWAP = 3;

	private static final int HIST = 8;

	{
		spriteClass = ProxySprite.class;
		HP = HT = BASE_HP;
		defenseSkill = 0;
		EXP = 0;
		state = HUNTING;
		alignment = Alignment.ENEMY;
		invisible = 1;

		properties.add(Property.BOSS);
		properties.add(Property.IMMOVABLE);
		properties.add(Property.INORGANIC);
		properties.add(Property.STATIC);

		immunities.add(Paralysis.class);
		immunities.add(Frost.class);
		immunities.add(Chill.class);
		immunities.add(Slow.class);
		immunities.add(Sleep.class);
		immunities.add(MagicalSleep.class);
		immunities.add(Terror.class);
		immunities.add(Amok.class);
		immunities.add(Charm.class);
		immunities.add(Corruption.class);
		immunities.add(Vertigo.class);
		immunities.add(ThalCall.Stasis.class);
	}

	//set by a rewind, the hud says it once
	public static boolean rewoundBanner = false;

	private int act = 0;
	private int turns = 0, actTurns = 0;
	private int handTimer = 4, magicTimer = 8, hazardTimer = 6;
	private int nextHero = 0, lastMagic = -1;
	private int actStartHp = BASE_HP;
	private int dying = -1;
	//alone: quiet turns after an act starts, and the turn of the last thing he threw
	private int calm = 0, lastThrow = -10;
	private boolean introDone = false;
	private int[] hpHistory = new int[16];
	private int[] stepHist = new int[Party.MAX_HEROES * HIST];
	private boolean[] swingHist = new boolean[Party.MAX_HEROES * HIST];

	private static class Pending {
		int kind;
		int[] cells;
		float at;
		int a, b;
	}

	private final ArrayList<Pending> pending = new ArrayList<>();

	public static ThalVekhorr find(){
		if (Dungeon.level == null) return null;
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof ThalVekhorr) return (ThalVekhorr) m;
		}
		return null;
	}

	private static RiftLevel level(){
		return Dungeon.level instanceof RiftLevel ? (RiftLevel) Dungeon.level : null;
	}

	public int currentAct(){
		return act;
	}

	public boolean dying(){
		return dying >= 0;
	}

	//-Dspd.test.thalweak=1 -> a tenth of the hp, to get through the acts fast
	@Override
	protected void onAdd() {
		boolean first = firstAdded;
		super.onAdd();
		if (first && DevFlags.thalWeak()){
			HT = Math.max(100, HT / 10);
			HP = HT;
		}
		if (first) actStartHp = HP;
	}

	@Override
	public boolean isInvulnerable( Class effect ) {
		return true;
	}

	//he's up in the sky. hitting him means hitting his hands, portals, core
	@Override
	public void damage( int dmg, Object src ) {
	}

	public void hurt( int dmg ){
		if (dying >= 0 || !introDone || dmg <= 0) return;
		HP = Math.max(0, HP - dmg);
		checkAct();
	}

	private void checkAct(){
		RiftLevel L = level();
		if (L == null) return;
		if (act >= 1 && act <= 5 && HP <= Math.round(ACT_END[act] * HT)){
			startAct(L, act + 1);
		} else if (act == 6 && HP <= 0){
			beginDeath(L);
		}
	}

	@Override
	protected boolean act() {
		RiftLevel L = level();
		invisible = 1;
		if (L == null){
			spend(TICK);
			return true;
		}
		turns++;
		if (dying >= 0){
			dyingTurn(L);
			spend(TICK);
			return true;
		}
		if (!introDone){
			intro(L);
			spend(TICK);
			return true;
		}
		actTurns++;
		hpHistory[turns % hpHistory.length] = HP;
		record();
		resolvePending(L);
		if (dying >= 0){
			spend(TICK);
			return true;
		}
		L.tickField();
		applyBubbles(L);
		if (calm > 0){
			calm--;
		} else {
			handTurn(L);
			magicTurn(L);
			hazardTurn(L);
		}
		spend(TICK);
		return true;
	}

	private void intro( RiftLevel L ){
		introDone = true;
		ThalFinale.snapshot();
		CoopLog.line("thal: fight starts, hp " + HP + " heroes " + Party.living().size());
		yell(Messages.get(this, "notice"));
		ThalHud.nameBanner();
		Sample.INSTANCE.play(Assets.Sounds.BOSS);
		PixelScene.shake(3, 2f);
		int start = Math.max(1, Math.min(6, DevFlags.thalAct()));
		if (start > 1) HP = Math.round(ACT_END[start - 1] * HT);
		startAct(L, start);
		handTimer = 4;
	}

	// *** acts ***

	private void startAct( RiftLevel L, int a ){
		//whatever came out of the old portals goes back in with them
		for (Mob m : new ArrayList<>(L.mobs)){
			if (m instanceof ThalParts.Portal) ((ThalParts.Portal) m).collapse();
			else if (m instanceof ThalParts.Hourglass) ((ThalParts.Hourglass) m).vanish();
		}
		act = a;
		L.act = a;
		actTurns = 0;
		actStartHp = HP;
		int wait = 0;
		if (alone()){
			//alone, every act starts clean and with a breath: what the last one let out goes away
			//(also from portals that were already closed) and nothing falls for a few turns
			for (Mob m : new ArrayList<>(L.mobs)){
				if (m.alignment == Alignment.ENEMY && !(m instanceof ThalParts.Part) && m != this
						&& !(m instanceof SKProps.SKProp)){
					if (m.sprite != null){
						CellEmitter.get(m.pos).burst(Speck.factory(Speck.LIGHT), 4);
						m.sprite.killAndErase();
					}
					m.destroy();
				}
			}
			calm = wait = SOLO_CALM;
			//and the hours give the hero back a bit of what the last act took
			Hero h = Dungeon.hero;
			if (a > 1 && h != null && h.isAlive() && h.HP < h.HT){
				int heal = Math.min(h.HT - h.HP, Math.round(h.HT * SOLO_HEAL));
				h.HP += heal;
				if (h.sprite != null){
					h.sprite.showStatus(CharSprite.POSITIVE, "+" + heal);
					h.sprite.emitter().burst(Speck.factory(Speck.HEALING), 4);
				}
				GLog.p(Messages.get(this, "solo_breath"));
			}
		}
		int[] spots = RiftLevel.portalCells();
		if (a <= 5){
			openPortal(L, spots[0], ACT_WORLD[a], true, 2 + wait);
			openPortal(L, spots[1], ACT_WORLD[a], false, 3 + wait);
			openPortal(L, spots[2], ACT_WORLD[a], false, 5 + wait);
		} else {
			openPortal(L, spots[1], 0, false, 3 + wait);
			openPortal(L, spots[2], 0, false, 5 + wait);
			placeCore(L);
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					if (Dungeon.level instanceof RiftLevel) Dungeon.level.playLevelMusic();
				}
			});
		}
		magicTimer = Math.min(magicTimer, 6);
		hazardTimer = 4;
		lastMagic = -1;
		ThalHud.actBanner(a);
		PixelScene.shake(4, 1.5f);
		Sample.INSTANCE.play(Assets.Sounds.TELEPORT, 1f, 0.6f);
		GLog.w(Messages.get(this, "act_" + a));
		CoopLog.line("thal: act " + a + " hp " + HP + "/" + HT + " turn " + turns);
	}

	//something standing there gets nudged out of the way
	private int clear( RiftLevel L, int cell ){
		Char c = Actor.findChar(cell);
		if (c != null){
			if (c instanceof ThalParts.Part) return -1;
			int to = L.freeFloorNear(cell, 4);
			if (to == -1 || to == cell) return -1;
			ScrollOfTeleportation.appear(c, to);
			L.occupyCell(c);
		}
		return cell;
	}

	private void openPortal( RiftLevel L, int cell, int world, boolean echo, int timer ){
		if (clear(L, cell) == -1) return;
		ThalParts.Portal p = new ThalParts.Portal();
		p.world = world;
		p.echo = echo;
		p.timer = timer;
		p.pos = cell;
		GameScene.add(p);
		CellEmitter.get(cell).burst(Speck.factory(Speck.LIGHT), 12);
	}

	private void placeCore( RiftLevel L ){
		int cell = RiftLevel.coreCell();
		if (clear(L, cell) == -1) return;
		ThalParts.Core c = new ThalParts.Core();
		c.pos = cell;
		GameScene.add(c);
		CellEmitter.get(cell).burst(Speck.factory(Speck.LIGHT), 20);
		yell(Messages.get(this, "core"));
	}

	public void portalClosed( ThalParts.Portal p ){
		CoopLog.line("thal: portal closed world " + p.world + " echo " + p.echo);
		GLog.p(Messages.get(this, "portal_closed"));
		hurt(Math.round(HT * 0.03f));
	}

	public void echoBroken(){
		CoopLog.line("thal: echo broken in act " + act);
		GLog.p(Messages.get(this, "echo_broken"));
		hurt(Math.round(HT * 0.04f));
	}

	public int portalInterval(){
		int base = act >= 6 ? 5 : 7 - act / 2;
		if (alone()) return base + 2;
		return Party.living().size() >= 3 ? base - 1 : base;
	}

	//an hourglass ran out: back to the hp of a dozen turns ago, never past the act's start
	public void rewindHp(){
		int back = hpHistory[(turns + 4) % hpHistory.length];
		int to = Math.min(actStartHp, Math.max(HP, back));
		if (to > HP){
			CoopLog.line("thal: rewind " + HP + " -> " + to);
			HP = to;
			yell(Messages.get(this, "rewind"));
			ThalHud.banner(Messages.get(this, "banner_rewind"));
			Sample.INSTANCE.play(Assets.Sounds.TELEPORT, 1f, 0.4f);
		}
	}

	// *** hands ***

	private int handInterval(){
		int[] base = {7, 7, 7, 6, 6, 5, 4};
		int i = base[Math.min(act, 6)];
		if (alone()) return i + SOLO_SLOWER;
		return Party.living().size() <= 1 ? i + 1 : i;
	}

	private int handsDown(){
		int n = 0;
		for (Mob m : Dungeon.level.mobs) if (m instanceof ThalParts.Hand) n++;
		return n;
	}

	private Hero pickHero(){
		ArrayList<Hero> alive = Party.living();
		if (alive.isEmpty()) return null;
		nextHero = (nextHero + 1) % alive.size();
		return alive.get(nextHero);
	}

	private void handTurn( RiftLevel L ){
		if (--handTimer > 0) return;
		if (busy()){
			handTimer = 1;
			return;
		}
		handTimer = handInterval();
		if (handsDown() >= 2) return;
		Hero h = pickHero();
		if (h == null) return;
		ArrayList<Integer> cells = new ArrayList<>();
		for (int c : SKCombat.area(h.pos, 1)){
			if (L.floor(c)) cells.add(c);
		}
		announce(P_SLAM, cells, h.pos, -1, 1f);
	}

	private void slam( RiftLevel L, Pending p ){
		int center = p.a;
		hitFoes(p.cells, 32, 52);
		Sample.INSTANCE.play(Assets.Sounds.ROCKS);
		PixelScene.shake(3, 0.7f);
		for (int c : p.cells) CellEmitter.get(c).burst(Speck.factory(Speck.DUST), 3);
		Char there = Actor.findChar(center);
		if (there != null && !(there instanceof ThalParts.Part)){
			int to = L.freeFloorNear(center, 2);
			if (to != -1 && to != center){
				ScrollOfTeleportation.appear(there, to);
				L.occupyCell(there);
			}
		}
		if (Actor.findChar(center) == null && L.floor(center)){
			ThalParts.Hand hand = new ThalParts.Hand();
			hand.left = center % RiftLevel.W < RiftLevel.CX;
			hand.life = act >= 5 ? 4 : 5;
			hand.pos = center;
			GameScene.add(hand);
		}
		int drip = center + PathFinder.NEIGHBOURS8[Random.Int(8)];
		if (Actor.findChar(drip) == null) L.addPuddle(drip, 25);
	}

	// *** spells ***

	private int magicInterval(){
		int[] base = {12, 12, 11, 10, 9, 8, 7};
		return base[Math.min(act, 6)] + (alone() ? SOLO_SLOWER : 0);
	}

	private void magicTurn( RiftLevel L ){
		if (--magicTimer > 0) return;
		if (busy()){
			magicTimer = 1;
			return;
		}
		magicTimer = magicInterval();
		int[] list = ACT_MAGIC[Math.min(act, 6)];
		if (list.length == 0) return;
		int m;
		do {
			m = list[Random.Int(list.length)];
		} while (m == lastMagic && list.length > 1);
		lastMagic = m;
		lastThrow = turns;
		cast(L, m);
	}

	private void cast( RiftLevel L, int spell ){
		ArrayList<Hero> alive = Party.living();
		if (alive.isEmpty()) return;
		CoopLog.line("thal: spell " + spell + " turn " + turns);
		switch (spell){
			case BUBBLES: default: {
				int n = act >= 6 ? 3 : 2;
				for (int i = 0; i < n; i++){
					int kind = i % 2;
					int center = kind == 0 ? Random.element(alive).pos : randomFloor(L, -1, 0);
					if (center == -1) continue;
					L.bubbles.add(new int[]{center, 2, 14, kind});
				}
				GLog.w(Messages.get(this, "spell_bubbles"));
				break;
			}
			case PUDDLES: {
				for (Hero h : alive){
					for (int i = 0; i < 2; i++){
						int c = randomFloorNear(L, h.pos, 3);
						if (c != -1) L.addPuddle(c, 30);
					}
				}
				GLog.w(Messages.get(this, "spell_puddles"));
				break;
			}
			case SWAP: {
				Char a = Random.element(alive), b = null;
				if (alive.size() >= 2){
					do {
						b = Random.element(alive);
					} while (b == a);
				} else {
					ArrayList<Mob> foes = new ArrayList<>();
					for (Mob m : L.mobs){
						if (m.alignment == Alignment.ENEMY && !(m instanceof ThalParts.Part)
								&& !(m instanceof ThalVekhorr) && L.floor(m.pos)) foes.add(m);
					}
					if (!foes.isEmpty()) b = Random.element(foes);
				}
				if (b == null){
					cast(L, BUBBLES);
					return;
				}
				ArrayList<Integer> cells = new ArrayList<>();
				cells.add(a.pos);
				cells.add(b.pos);
				announce(P_SWAP, cells, a.id(), b.id(), 0f);
				GLog.w(Messages.get(this, "spell_swap"));
				break;
			}
			case REWIND: {
				int n = alive.size() >= 3 ? 3 : alone() ? 1 : 2;
				for (int i = 0; i < n; i++){
					int c = randomFloor(L, -1, 3);
					if (c == -1) continue;
					ThalParts.Hourglass g = new ThalParts.Hourglass();
					g.sand = g.sandMax = act >= 6 ? 12 : 14;
					g.pos = c;
					GameScene.add(g);
					CellEmitter.get(c).burst(Speck.factory(Speck.LIGHT), 6);
				}
				yell(Messages.get(this, "spell_rewind"));
				ThalHud.banner(Messages.get(this, "banner_hourglass"));
				break;
			}
			case STOP: {
				ArrayList<Integer> cells = stopPattern(L, Random.Int(3));
				announce(P_STOP, cells, -1, -1, 2f);
				yell(Messages.get(this, "spell_stop"));
				ThalHud.banner(Messages.get(this, "banner_stop_soon"));
				break;
			}
			case ECHOES: {
				int made = 0;
				for (Hero h : alive){
					if (made >= 4) break;
					int idx = Party.active() ? Party.indexOf(h) : 0;
					if (idx < 0) continue;
					int[] path = new int[HIST];
					boolean[] swing = new boolean[HIST];
					int n = 0;
					for (int k = 1; k <= HIST; k++){
						int slot = idx * HIST + (turns + k) % HIST;
						if (stepHist[slot] <= 0) continue;
						path[n] = stepHist[slot];
						swing[n] = swingHist[slot];
						n++;
					}
					if (n < 3) continue;
					int start = path[0];
					if (Actor.findChar(start) != null || !L.floor(start)) start = L.freeFloorNear(start, 2);
					if (start == -1) continue;
					ThalParts.HeroEcho e = ThalParts.HeroEcho.of(h, Arrays.copyOf(path, n), Arrays.copyOf(swing, n));
					e.pos = start;
					GameScene.add(e);
					CellEmitter.get(start).burst(Speck.factory(Speck.BLUE_LIGHT), 6);
					made++;
				}
				if (made > 0) yell(Messages.get(this, "spell_echoes"));
				break;
			}
		}
	}

	//0 clock hands, 1 rings, 2 two quarters
	private ArrayList<Integer> stopPattern( RiftLevel L, int kind ){
		ArrayList<Integer> cells = new ArrayList<>();
		int phase = Random.Int(2);
		for (int c : L.floorCells()){
			int x = c % RiftLevel.W - RiftLevel.CX, y = c / RiftLevel.W - RiftLevel.CY;
			boolean hit;
			switch (kind){
				case 0: default: {
					double ang = Math.toDegrees(Math.atan2(y, x)) + 360 + phase * 15;
					hit = (x != 0 || y != 0) && (ang % 30) < 11;
					break;
				}
				case 1: {
					int d = Math.max(Math.abs(x), Math.abs(y));
					hit = (d + phase) % 3 == 0;
					break;
				}
				case 2:
					hit = phase == 0 ? (x < 0) == (y < 0) : (x < 0) != (y < 0);
					break;
			}
			if (hit) cells.add(c);
		}
		return cells;
	}

	private int randomFloor( RiftLevel L, int near, int awayFromHeroes ){
		ArrayList<Integer> ok = new ArrayList<>();
		for (int c : L.floorCells()){
			if (Actor.findChar(c) != null) continue;
			boolean far = true;
			for (Hero h : Party.living()){
				if (L.distance(c, h.pos) < awayFromHeroes) far = false;
			}
			if (far) ok.add(c);
		}
		return ok.isEmpty() ? -1 : Random.element(ok);
	}

	private int randomFloorNear( RiftLevel L, int cell, int dist ){
		ArrayList<Integer> ok = new ArrayList<>();
		for (int c : L.floorCells()){
			if (L.distance(c, cell) <= dist && L.distance(c, cell) >= 1 && Actor.findChar(c) == null) ok.add(c);
		}
		return ok.isEmpty() ? -1 : Random.element(ok);
	}

	private void applyBubbles( RiftLevel L ){
		if (L.bubbles.isEmpty()) return;
		for (Char ch : Actor.chars()){
			if (ch instanceof ThalParts.Part || ch == this) continue;
			int k = L.bubbleAt(ch.pos);
			if (k == 0) Buff.prolong(ch, Slow.class, 1.1f);
			else if (k == 1) Buff.prolong(ch, Haste.class, 1.1f);
		}
	}

	public static void steppedInPuddle( Char ch ){
		if (ch instanceof ThalParts.Part) return;
		int roll = Random.Int(7);
		switch (roll){
			case 0: default: Buff.affect(ch, Roots.class, 2f); break;
			case 1: Buff.prolong(ch, Slow.class, 3f); break;
			case 2: Buff.prolong(ch, Vertigo.class, 3f); break;
			case 3: Buff.prolong(ch, ThalCall.Stasis.class, 1f); break;
			//the nice ones
			case 4: Buff.prolong(ch, Haste.class, 4f); break;
			case 5: {
				int heal = Math.max(1, ch.HT / 7);
				ch.HP = Math.min(ch.HT, ch.HP + heal);
				if (ch.sprite != null) ch.sprite.showStatus(CharSprite.POSITIVE, "+" + heal);
				break;
			}
			case 6: Buff.affect(ch, Recharging.class, 4f); break;
		}
		if (ch.sprite != null) CellEmitter.get(ch.pos).burst(Speck.factory(Speck.STEAM), 4);
		Sample.INSTANCE.play(Assets.Sounds.WATER, 0.6f, 1.4f);
	}

	// *** each world brings its own trouble ***

	private void hazardTurn( RiftLevel L ){
		if (--hazardTimer > 0) return;
		if (busy()){
			hazardTimer = 1;
			return;
		}
		hazardTimer = (act >= 6 ? 5 : 7) + (alone() ? SOLO_SLOWER : 0);
		int w = act >= 6 ? Random.IntRange(SKWorlds.FOREST, SKWorlds.FORGOTTEN) : ACT_WORLD[act];
		ArrayList<Hero> alive = Party.living();
		if (alive.isEmpty() || w == 0) return;
		Hero aim = Random.element(alive);
		int ax = aim.pos % RiftLevel.W, ay = aim.pos / RiftLevel.W;
		ArrayList<Integer> cells = new ArrayList<>();
		switch (w){
			case SKWorlds.FOREST:
				for (Hero h : alive){
					for (int i = 0; i < 3; i++){
						int c = randomFloorNear(L, h.pos, 2);
						if (c != -1 && !cells.contains(c)) cells.add(c);
					}
					if (!cells.contains(h.pos)) cells.add(h.pos);
				}
				break;
			case SKWorlds.KINGDOM:
				for (int c : L.floorCells()) if (c / RiftLevel.W == ay) cells.add(c);
				break;
			case SKWorlds.STARSHIP:
				for (int c : L.floorCells()) if (c % RiftLevel.W == ax) cells.add(c);
				break;
			case SKWorlds.UNDERSEA: {
				boolean leftSide = ax < RiftLevel.CX;
				for (int c : L.floorCells()){
					int x = c % RiftLevel.W, y = c / RiftLevel.W;
					if (Math.abs(y - ay) <= 1 && (leftSide ? x <= RiftLevel.CX : x >= RiftLevel.CX)) cells.add(c);
				}
				break;
			}
			case SKWorlds.FORGOTTEN:
				for (int i = 0; i < 3; i++){
					Hero h = Random.element(alive);
					int ox = Random.Int(2), oy = Random.Int(2);
					for (int dy = -1 + oy; dy <= oy; dy++){
						for (int dx = -1 + ox; dx <= ox; dx++){
							int c = h.pos + dx + dy * RiftLevel.W;
							if (L.floor(c) && !cells.contains(c)) cells.add(c);
						}
					}
				}
				break;
		}
		if (!cells.isEmpty()) announce(P_HAZARD, cells, w, -1, 1f);
	}

	private void hazard( RiftLevel L, Pending p ){
		int w = p.a;
		int min = 18, max = 32;
		if (w == SKWorlds.STARSHIP){
			min = 24;
			max = 38;
		}
		for (Char ch : hitFoes(p.cells, min, max)){
			if (!ch.isAlive()) continue;
			switch (w){
				case SKWorlds.FOREST:    Buff.affect(ch, Roots.class, 2f); break;
				case SKWorlds.KINGDOM:   Buff.prolong(ch, Cripple.class, 3f); break;
				case SKWorlds.UNDERSEA:  Buff.prolong(ch, Slow.class, 3f); break;
				case SKWorlds.FORGOTTEN: Buff.prolong(ch, Chill.class, 3f); break;
			}
		}
		int fx;
		switch (w){
			case SKWorlds.FOREST:    fx = Speck.ROCK; break;
			case SKWorlds.KINGDOM:   fx = Speck.FORGE; break;
			case SKWorlds.STARSHIP:  fx = Speck.BLUE_LIGHT; break;
			case SKWorlds.UNDERSEA:  fx = Speck.BUBBLE; break;
			default:                 fx = Speck.BLIZZARD; break;
		}
		for (int c : p.cells) CellEmitter.get(c).burst(Speck.factory(fx), 3);
		Sample.INSTANCE.play(w == SKWorlds.STARSHIP ? Assets.Sounds.RAY : Assets.Sounds.BLAST, 0.8f);
	}

	// *** alone ***

	//turns of quiet when an act starts, and how much later hands, spells and hazards come
	private static final int SOLO_CALM = 6;
	private static final int SOLO_SLOWER = 3;
	private static final float SOLO_HEAL = 0.3f;

	//a run of one hero. a co-op party never goes through any of this, even with one left standing
	public static boolean alone(){
		return !Party.active();
	}

	//alone: one thing at a time. what's next waits while a mark is still on the floor
	//or something was thrown a moment ago
	private boolean busy(){
		return alone() && (!pending.isEmpty() || turns - lastThrow < 2);
	}

	//the hero and whatever fights for them right now (summons, pets, turrets)
	public static int alliesOnField(){
		int n = Party.living().size();
		if (Dungeon.level == null) return n;
		for (Mob m : Dungeon.level.mobs){
			if (m.alignment == Alignment.ALLY && m.isAlive() && !(m instanceof SKProps.SKProp)) n++;
		}
		return n;
	}

	//alone, the echoes come out as big as the side they face: half for a lone hero, whole with three helpers
	public static float echoSize(){
		if (!alone() || !(Dungeon.level instanceof RiftLevel)) return 1f;
		return Math.min(1f, 0.5f + (alliesOnField() - 1) / 6f);
	}

	//and a lone hero only has to hold out against one for a while: it fades after 30 turns per
	//fighter on the field. with three helpers or more it stays until it breaks
	public static int echoLife(){
		if (!alone()) return -1;
		int n = alliesOnField();
		return n >= 4 ? -1 : 30 * n;
	}

	//and they hit like his own specials do alone
	public static int echoHit( int dmg ){
		if (!alone() || !(Dungeon.level instanceof RiftLevel)) return dmg;
		return Math.max(1, Math.round(dmg * SOLO_SPECIAL));
	}

	private static final float SOLO_SPECIAL = 0.75f;

	//alone, what the summons do to his parts counts half: they help, they don't win it for you
	public static int summonShare( Object src, int dmg ){
		if (!alone() || dmg <= 0) return dmg;
		Char ch = src instanceof Char ? (Char) src : null;
		if (ch == null && src instanceof SKBulletField.Shot) ch = ((SKBulletField.Shot) src).owner;
		if (ch == null || ch instanceof Hero || ch.alignment != Alignment.ALLY) return dmg;
		return Math.max(1, dmg / 2);
	}

	// *** telegraphs ***

	private void announce( int kind, ArrayList<Integer> cells, int a, int b, float extra ){
		Pending p = new Pending();
		p.kind = kind;
		p.a = a;
		p.b = b;
		p.cells = new int[cells.size()];
		for (int i = 0; i < p.cells.length; i++) p.cells[i] = cells.get(i);
		float delay = SKCombat.announceDelay(TICK) + extra;
		p.at = now() + delay;
		pending.add(p);
		lastThrow = turns;
		SKCombat.mark(cells, delay);
	}

	private void resolvePending( RiftLevel L ){
		for (Pending p : new ArrayList<>(pending)){
			if (p.at > now() + 0.01f) continue;
			pending.remove(p);
			switch (p.kind){
				case P_SLAM:   slam(L, p); break;
				case P_HAZARD: hazard(L, p); break;
				case P_STOP:   timeStop(L, p); break;
				case P_SWAP:   swap(L, p); break;
			}
			if (dying >= 0) return;
		}
	}

	private int special( Char target, int min, int max ){
		int dmg = SKCombat.specialDamage(this, target, min, max);
		//alone it's still hard, just not a coin flip every slam
		float solo = Party.living().size() <= 1 ? SOLO_SPECIAL : 1f;
		return Math.max(1, Math.round(dmg * CoopBalance.damageFactor(this) * solo));
	}

	private ArrayList<Char> hitFoes( int[] cells, int min, int max ){
		ArrayList<Integer> list = new ArrayList<>();
		for (int c : cells) list.add(c);
		ArrayList<Char> foes = SKCombat.foesIn(this, list);
		for (Char ch : foes){
			ch.damage(special(ch, min, max), this);
		}
		return foes;
	}

	private void timeStop( RiftLevel L, Pending p ){
		ThalHud.banner(Messages.get(this, "banner_stop"));
		ThalHud.stopFlash();
		Sample.INSTANCE.play(Assets.Sounds.CHARGEUP, 1f, 0.5f);
		for (Char ch : Actor.chars()){
			if (ch instanceof ThalParts.Part || ch == this) continue;
			Buff.prolong(ch, ThalCall.Stasis.class, 2f);
		}
		hitFoes(p.cells, 45, 70);
		for (int c : p.cells) CellEmitter.get(c).burst(Speck.factory(Speck.LIGHT), 2);
	}

	private void swap( RiftLevel L, Pending p ){
		Actor a = Actor.findById(p.a), b = Actor.findById(p.b);
		if (!(a instanceof Char) || !(b instanceof Char)) return;
		Char ca = (Char) a, cb = (Char) b;
		if (!ca.isAlive() || !cb.isAlive()) return;
		int pa = ca.pos, pb = cb.pos;
		//park one in the sky for a moment so they don't stand on each other
		ca.pos = RiftLevel.farCell() + 1;
		ScrollOfTeleportation.appear(cb, pa);
		ScrollOfTeleportation.appear(ca, pb);
		L.occupyCell(ca);
		L.occupyCell(cb);
		Dungeon.observe();
		GameScene.updateFog();
		SKCombat.focusLocal();
	}

	// *** the echoes need to know where you walked ***

	private void record(){
		ArrayList<Hero> list = Kraken.partyHeroes();
		for (Hero h : list){
			int idx = Party.active() ? Party.indexOf(h) : 0;
			if (idx < 0 || idx >= Party.MAX_HEROES) continue;
			int slot = idx * HIST + turns % HIST;
			int prev = stepHist[idx * HIST + (turns + HIST - 1) % HIST];
			boolean alive = h.isAlive() && !h.coopDown;
			stepHist[slot] = alive ? h.pos : -1;
			boolean fighting = false;
			if (alive && prev == h.pos){
				for (int n : PathFinder.NEIGHBOURS8){
					Char c = Actor.findChar(h.pos + n);
					if (c != null && c.alignment == Alignment.ENEMY) fighting = true;
				}
			}
			swingHist[slot] = fighting;
		}
	}

	// *** the end of the end ***

	private void beginDeath( RiftLevel L ){
		CoopLog.line("thal: defeated at turn " + turns);
		dying = 5;
		act = 7;
		L.act = 7;
		pending.clear();
		yell(Messages.get(this, "defeated"));
		ThalHud.banner(Messages.get(this, "banner_dead"));
		Sample.INSTANCE.play(Assets.Sounds.BOSS, 1f, 0.7f);
		PixelScene.shake(6, 3f);
		for (Mob m : new ArrayList<>(L.mobs)){
			if (m == this) continue;
			if (m instanceof ThalParts.Portal) ((ThalParts.Portal) m).collapse();
			else if (m instanceof ThalParts.Part) ((ThalParts.Part) m).vanish();
			else if (m instanceof ThalParts.HeroEcho) ((ThalParts.HeroEcho) m).die(null);
			else if (m.alignment == Alignment.ENEMY) m.die(null);
		}
		L.bubbles.clear();
		Arrays.fill(L.puddles, 0);
		for (Char ch : Actor.chars()){
			Buff s = ch.buff(ThalCall.Stasis.class);
			if (s != null) s.detach();
		}
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				if (Dungeon.level instanceof RiftLevel) Dungeon.level.playLevelMusic();
			}
		});
	}

	private void dyingTurn( RiftLevel L ){
		dying--;
		if (dying == 3) PixelScene.shake(4, 2f);
		if (dying > 0) return;

		ThalFinale.defeated = true;
		ArrayList<Hero> heroes = Kraken.partyHeroes();
		Hero standing = null;
		for (Hero h : heroes) if (h.isAlive() && !h.coopDown) standing = h;
		//time puts the fallen back together
		for (Hero h : heroes){
			if (h.coopDown) h.coopReviveOnFloor(standing);
		}
		for (Hero h : heroes){
			ThalRewards.grant(h);
			Party.withHero(h, new Runnable() {
				@Override
				public void run() {
					h.earnExp(400, ThalVekhorr.class);
				}
			});
		}
		int cell = RiftLevel.coreCell();
		clear(L, cell);
		ThalParts.ExitPortal exit = new ThalParts.ExitPortal();
		exit.pos = cell;
		GameScene.add(exit);
		CellEmitter.get(cell).burst(Speck.factory(Speck.YELLOW_LIGHT), 20);
		GLog.p(Messages.get(this, "portal_open"));
		ThalHud.banner(Messages.get(this, "banner_portal"));
		dying = -2;
		destroy();
		if (sprite != null) sprite.killAndErase();
		for (Hero h : heroes){
			if (h.isAlive()) askVote(h);
		}
	}

	// *** the vote at the portal ***

	private static ThalParts.ExitPortal exit(){
		if (Dungeon.level == null) return null;
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof ThalParts.ExitPortal) return (ThalParts.ExitPortal) m;
		}
		return null;
	}

	private static int indexOf( Hero h ){
		return Party.active() ? Party.indexOf(h) : 0;
	}

	public static void askVote( final Hero h ){
		final ThalParts.ExitPortal p = exit();
		if (p == null || p.decided) return;
		final int idx = indexOf(h);
		if (idx < 0) return;
		Party.withHero(h, new Runnable() {
			@Override
			public void run() {
				Game.runOnRenderThread(new Callback() {
					@Override
					public void call() {
						GameScene.show(new WndPortal(idx));
					}
				});
			}
		});
	}

	static void vote( int idx, int choice ){
		ThalParts.ExitPortal p = exit();
		if (p == null || p.decided || idx < 0 || idx >= p.votes.length) return;
		p.votes[idx] = choice;
		CoopLog.line("thal: hero " + idx + " votes " + choice);
		int finish = 0, go = 0;
		boolean all = true;
		for (Hero h : Kraken.partyHeroes()){
			if (!h.isAlive() || h.coopDown) continue;
			int v = p.votes[indexOf(h)];
			if (v == 0) all = false;
			else if (v == 1) finish++;
			else go++;
		}
		if (Party.active()){
			GLog.i(Messages.get(ThalVekhorr.class, choice == 1 ? "voted_finish" : "voted_go", Party.name(idx)));
		}
		if (!all) return;
		p.decided = true;
		boolean continent = go > finish || (go == finish && p.votes[0] == 2);
		if (continent && ThalFinale.CONTINENT_READY){
			//they can come back and vote again
			p.decided = false;
			Arrays.fill(p.votes, 0);
			Continent.enter(p.pos);
		} else {
			ThalFinale.finish();
		}
	}

	public static class WndPortal extends WndOptions {

		private final int idx;

		public WndPortal( int idx ){
			super(Messages.get(ThalVekhorr.class, "wnd_title"),
					Messages.get(ThalVekhorr.class, Party.active() ? "wnd_body_coop" : "wnd_body"),
					Messages.get(ThalVekhorr.class, "wnd_finish"),
					Messages.get(ThalVekhorr.class, ThalFinale.CONTINENT_READY ? "wnd_continent" : "wnd_continent_soon"));
			this.idx = idx;
		}

		@Override
		protected boolean enabled( int index ) {
			return index == 0 || ThalFinale.CONTINENT_READY;
		}

		@Override
		protected void onSelect( int index ) {
			vote(idx, index == 0 ? 1 : 2);
		}
	}

	// *** save ***

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("act", act);
		bundle.put("turns", turns);
		bundle.put("act_turns", actTurns);
		bundle.put("hand_timer", handTimer);
		bundle.put("magic_timer", magicTimer);
		bundle.put("hazard_timer", hazardTimer);
		bundle.put("next_hero", nextHero);
		bundle.put("last_magic", lastMagic);
		bundle.put("act_start_hp", actStartHp);
		bundle.put("dying", dying);
		bundle.put("calm", calm);
		bundle.put("last_throw", lastThrow);
		bundle.put("intro_done", introDone);
		bundle.put("hp_history", hpHistory);
		bundle.put("step_hist", stepHist);
		bundle.put("swing_hist", swingHist);
		int[] flat = new int[pending.size() * 4];
		ArrayList<Integer> cells = new ArrayList<>();
		float[] at = new float[pending.size()];
		for (int i = 0; i < pending.size(); i++){
			Pending p = pending.get(i);
			flat[i*4] = p.kind;
			flat[i*4 + 1] = p.a;
			flat[i*4 + 2] = p.b;
			flat[i*4 + 3] = p.cells.length;
			at[i] = p.at;
			for (int c : p.cells) cells.add(c);
		}
		int[] cellArr = new int[cells.size()];
		for (int i = 0; i < cellArr.length; i++) cellArr[i] = cells.get(i);
		bundle.put("pending", flat);
		bundle.put("pending_at", at);
		bundle.put("pending_cells", cellArr);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		act = bundle.getInt("act");
		turns = bundle.getInt("turns");
		actTurns = bundle.getInt("act_turns");
		handTimer = bundle.getInt("hand_timer");
		magicTimer = bundle.getInt("magic_timer");
		hazardTimer = bundle.getInt("hazard_timer");
		nextHero = bundle.getInt("next_hero");
		lastMagic = bundle.getInt("last_magic");
		actStartHp = bundle.getInt("act_start_hp");
		dying = bundle.getInt("dying");
		calm = bundle.getInt("calm");
		lastThrow = bundle.contains("last_throw") ? bundle.getInt("last_throw") : -10;
		introDone = bundle.getBoolean("intro_done");
		hpHistory = bundle.getIntArray("hp_history");
		stepHist = bundle.getIntArray("step_hist");
		swingHist = bundle.getBooleanArray("swing_hist");
		pending.clear();
		int[] flat = bundle.getIntArray("pending");
		float[] at = bundle.getFloatArray("pending_at");
		int[] cellArr = bundle.getIntArray("pending_cells");
		int k = 0;
		for (int i = 0; flat != null && i * 4 + 3 < flat.length; i++){
			Pending p = new Pending();
			p.kind = flat[i*4];
			p.a = flat[i*4 + 1];
			p.b = flat[i*4 + 2];
			p.cells = new int[flat[i*4 + 3]];
			for (int j = 0; j < p.cells.length; j++) p.cells[j] = cellArr[k++];
			p.at = at[i];
			pending.add(p);
		}
	}

	//never drawn, he's painted on the sky
	public static class ProxySprite extends MobSprite {
		public ProxySprite(){
			super();
			texture(FinaleAssets.HOURGLASS);
			TextureFilm film = new TextureFilm(texture, 16, 24);
			idle = new Animation(1, true);
			idle.frames(film, 0);
			run = idle.clone();
			attack = idle.clone();
			zap = idle.clone();
			die = idle.clone();
			play(idle);
		}

		@Override
		public void draw() {
		}
	}
}
