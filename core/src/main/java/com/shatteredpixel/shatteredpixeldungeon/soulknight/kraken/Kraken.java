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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.kraken;

import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.quests.LabQuest;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Amok;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corruption;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicalSleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Sleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopBalance;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopRewards;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.WornKey;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.armory.SKArmory;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.props.SKProps;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.undersea.AbyssSonar;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.undersea.UnderseaFeatures;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.undersea.UnderseaMobs;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;

//the big boy. 800 hp flat, you only hurt it by hitting the eye (200 a pop, so 4 hits)
//it never moves, it just runs the whole ship fight: tentacles (1 per assault), lightning,
//waves, sea mobs... and opens the eye when the ship gets to the whirlpool. eye hit = ship gets pushed back
public class Kraken extends SKMob {

	public static final int MAX_HP = 800;
	public static final int EYE_HIT = 200;

	{
		spriteClass = KrakenProxySprite.class;
		HP = HT = MAX_HP;
		defenseSkill = 0;
		EXP = 60;
		baseSpeed = 1f;
		state = HUNTING;
		alignment = Alignment.ENEMY;

		properties.add(Property.BOSS);
		properties.add(Property.IMMOVABLE);
		properties.add(Property.INORGANIC);

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
	}

	//always 800, ignores party size AND difficulty. on purpose
	@Override
	protected void onAdd() {
		boolean first = firstAdded;
		super.onAdd();
		if (first){
			HT = MAX_HP;
			HP = Math.min(HP, MAX_HP);
		}
	}

	private static KrakenLevel level(){
		return Dungeon.level instanceof KrakenLevel ? (KrakenLevel) Dungeon.level : null;
	}

	public static Kraken find(){
		if (Dungeon.level == null) return null;
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof Kraken) return (Kraken) m;
		}
		return null;
	}

	//timers + the telegraphed stuff

	private int tentacle = -1;       //-1 = none
	private int lightningTimer = 3;
	private int monsterTimer = 6;
	private int waveTimer = 6;
	private int inkTimer = 7;
	private int harpoonTimer = 4;
	private int thrashTimer = 5;
	private int jetTimer = 0;
	private int pushTimer = 0;
	private int guardTimer = 0;
	private int crushTimer = 0;
	private int turns = 0;
	private boolean eyeHitThisApproach = false;
	private boolean mastStruck = false;
	private boolean introDone = false;

	private static final int SLAM = 0, LIGHTNING = 1, WAVE = 2, JET = 3, THRASH = 4, MAST_FALL = 5;

	private static class Pending {
		int kind;
		int[] cells;
		float at;
		int aim;
	}

	private final ArrayList<Pending> pending = new ArrayList<>();

	private void announce( int kind, ArrayList<Integer> cells, int aim, float extra ){
		Pending p = new Pending();
		p.kind = kind;
		p.aim = aim;
		p.cells = new int[cells.size()];
		for (int i = 0; i < p.cells.length; i++) p.cells[i] = cells.get(i);
		float delay = SKCombat.announceDelay(TICK) + extra;
		p.at = now() + delay;
		pending.add(p);
		SKCombat.mark(cells, delay);
	}

	@Override
	public boolean isInvulnerable( Class effect ) {
		return !vulnerable();
	}

	public boolean vulnerable(){
		KrakenLevel L = level();
		return L != null && L.stage == KrakenLevel.ARRIVED && L.eyeOpen && !eyeHitThisApproach && !guardsAlive();
	}

	@Override
	public void damage( int dmg, Object src ) {
		KrakenLevel L = level();
		if (L == null || !isAlive()) return;
		if (!vulnerable()){
			if (sprite != null && L.stage == KrakenLevel.ARRIVED){
				sprite.showStatus(CharSprite.WARNING, Messages.get(this, guardsAlive() ? "guarded" : "closed"));
			}
			return;
		}
		//a dart does the same as a crit. it's an eye, it hurts
		eyeHitThisApproach = true;
		L.eyeOpen = false;
		HP = Math.max(0, HP - EYE_HIT);
		if (sprite != null) sprite.showStatus(CharSprite.NEGATIVE, Integer.toString(EYE_HIT));
		KrakenVisuals.eyeHit(HP <= 0);
		Sample.INSTANCE.play(Assets.Sounds.BLAST, 1f, 0.6f);
		if (HP <= 0){
			die(src);
		} else {
			L.stage = KrakenLevel.PUSHED;
			pushTimer = 3;
			yell(Messages.get(this, "hurt_" + L.assault));
			KrakenHud.banner(Messages.get(this, "banner_pushed", HP / EYE_HIT));
		}
	}

	@Override
	protected boolean act() {
		KrakenLevel L = level();
		if (L == null || L.stage == KrakenLevel.WON || L.stage == KrakenLevel.SUNK){
			spend(TICK);
			return true;
		}
		turns++;
		invisible = L.stage == KrakenLevel.ARRIVED ? 0 : 1;

		resolvePending(L);
		if (L.stage == KrakenLevel.SUNK){
			spend(TICK);
			return true;
		}

		switch (L.stage){
			case KrakenLevel.INTRO:
				intro(L);
				break;
			case KrakenLevel.TENTACLE:
				tentacleTurn(L);
				break;
			case KrakenLevel.SAILING:
				sailingTurn(L);
				break;
			case KrakenLevel.ARRIVED:
				arrivedTurn(L);
				break;
			case KrakenLevel.PUSHED:
				if (--pushTimer <= 0){
					L.assault = Math.min(4, L.assault + 1);
					//GLog.i("kraken assault " + L.assault + " hp " + HP);
					moveTo(KrakenLevel.farCell());
					startTentacle(L);
				}
				break;
		}

		//holes let water in, slower if solo
		if (L.stage != KrakenLevel.WON && !L.holes.isEmpty()){
			if (turns % (L.solo() ? 3 : 2) == 0){
				L.damageShip(L.holes.size());
			}
		}

		spend(TICK);
		return true;
	}

	private void intro( KrakenLevel L ){
		if (!introDone){
			introDone = true;
			//hull: 190 alone (+ the deckhand), 550 in co-op. 150 sank way too fast with a party
			if (L.solo()){
				L.shipHT = L.shipHP = 190;
			} else {
				L.shipHT = L.shipHP = 550;
			}
			//hammer for everyone + the deckhand if you're alone
			for (Hero h : partyHeroes()){
				give(h, new ShipHammer());
			}
			if (L.solo()){
				Grumete g = new Grumete();
				g.pos = L.freeDeckNear(KrakenLevel.ladderCell());
				if (g.pos != -1){
					GameScene.add(g);
				}
			}
			//-Dspd.test.krakenstart=N -> jump to assault N (kraken hp lowered to match). saves me 10 min per test
			int start = DevFlags.krakenStart();
			if (start > 1){
				L.assault = Math.min(4, start);
				HP = MAX_HP - EYE_HIT * (L.assault - 1);
			}
			yell(Messages.get(this, "notice"));
			KrakenHud.banner(Messages.get(this, "banner_intro"));
			GLog.w(Messages.get(this, "hint_hammer"));
			GLog.w(Messages.get(this, "hint_helm"));
			GLog.w(Messages.get(this, "hint_cannon"));
			moveTo(KrakenLevel.farCell());
			return;
		}
		startTentacle(L);
	}

	public static ArrayList<Hero> partyHeroes(){
		ArrayList<Hero> list = new ArrayList<>();
		if (Party.active()){
			list.addAll(Party.heroes());
		} else if (Dungeon.hero != null){
			list.add(Dungeon.hero);
		}
		return list;
	}

	//backpack, or drop it at their feet if full
	public static void give( final Hero h, final Item item ){
		Party.withHero(h, new Runnable() {
			@Override
			public void run() {
				if (!item.collect(h.belongings.backpack)){
					Dungeon.level.drop(item, h.pos).sprite.drop();
				}
			}
		});
	}

	private void moveTo( int cell ){
		if (pos == cell) return;
		if (Actor.findChar(cell) != null && Actor.findChar(cell) != this) return;
		pos = cell;
		if (sprite != null) sprite.place(cell);
	}

	/* tentacles */

	private ArrayList<Integer> slamRows( KrakenLevel L ){
		ArrayList<Integer> rows = new ArrayList<>();
		for (int r = 5; r <= 22; r++){
			if (Math.abs(r - KrakenLevel.MAST_ROW) <= 1) continue;
			if (r == 20) continue; //helm row, never slam it (sin timón = se acabó)
			rows.add(r);
		}
		return rows;
	}

	private void startTentacle( KrakenLevel L ){
		L.stage = KrakenLevel.TENTACLE;
		eyeHitThisApproach = false;
		L.eyeOpen = false;
		int type = L.assault;
		ArrayList<Integer> rows = slamRows(L);
		//aim at a row with heroes in it
		int row = Random.element(rows);
		ArrayList<Hero> living = Party.living();
		if (!living.isEmpty()){
			Hero target = Random.element(living);
			int hr = KrakenLevel.shipY(target.pos);
			int best = row;
			for (int r : rows){
				if (Math.abs(r - hr) < Math.abs(best - hr)) best = r;
			}
			row = best;
		}
		int side = Random.Int(2) == 0 ? -1 : 1;
		L.riseRow = row;
		L.riseSide = side;
		L.riseType = type;

		ArrayList<Integer> strip = new ArrayList<>();
		int half = type == 4 ? 2 : 1;
		for (int r = row - half; r <= row + half; r++){
			strip.addAll(L.rowCells(r));
		}
		announce(SLAM, strip, row, 1f);
		KrakenVisuals.rise(side, row, type);
		Sample.INSTANCE.play(Assets.Sounds.WATER, 1f, 0.5f);

		if (type == 4 && L.countdown < 0){
			L.countdownMax = L.countdown = L.solo() ? 120 : 90;
		}
		KrakenHud.banner(Messages.get(this, "banner_tentacle_" + type));
		GLog.w(Messages.get(this, "hint_tentacle_" + type));
	}

	private void slam( KrakenLevel L, Pending p ){
		int row = p.aim;
		int side = L.riseSide;
		int type = L.riseType;
		L.riseRow = -1;

		ArrayList<Integer> strip = new ArrayList<>();
		for (int c : p.cells) strip.add(c);

		for (Char ch : SKCombat.foesIn(this, strip)){
			int dmg = scaled(Random.NormalIntRange(8 + 3*type, 16 + 4*type));
			ch.damage(Math.max(1, dmg - ch.drRoll()/2), this);
		}
		L.damageShip(8 + 3*type);
		int holes = type == 4 ? 3 : 2;
		ArrayList<Integer> spots = new ArrayList<>(strip);
		for (int i = 0; i < holes && !spots.isEmpty(); i++){
			int c = spots.remove(Random.Int(spots.size()));
			if (KrakenLevel.shipY(c) != row) L.addHole(c);
			else i--;
		}
		KrakenVisuals.slam(side, row, type);

		//and then it just lies there across the deck
		GiantTentacle t = new GiantTentacle();
		t.setup(type, side, row);
		int base = KrakenLevel.cell(side < 0 ? -1 : KrakenLevel.SHIP_W, row);
		t.pos = base;
		GameScene.add(t);
		tentacle = t.id();

		//lies from its own side, tip leaves the last cell on the far side free
		ArrayList<Integer> line = L.rowCells(row);
		if (line.size() > 2){
			line.remove(side < 0 ? line.size() - 1 : 0);
		}
		for (int c : line){
			Char ch = Actor.findChar(c);
			if (ch != null){
				if (!shove(L, ch, row)) continue;
			}
		}
		t.spawnSegments(line);

		//hooked ones (2 and 4) grab someone
		if (type == 2 || type == 4){
			Hero victim = null;
			for (Char ch : SKCombat.foesIn(this, strip)){
				if (ch instanceof Hero && ch.isAlive()){
					victim = (Hero) ch;
					break;
				}
			}
			if (victim == null) victim = Party.nearestLiving(KrakenLevel.cell(side < 0 ? 1 : 9, row));
			if (victim != null && victim.isAlive() && victim.buff(KrakenHold.class) == null){
				KrakenHold.grab(victim, t);
				KrakenHud.banner(Party.active() ? Messages.get(this, "banner_grab", Party.name(victim)) : Messages.get(this, "banner_grab_solo"));
			}
		}
		thrashTimer = 5;
	}

	//shove whoever's on the slam line to the next row
	private boolean shove( KrakenLevel L, Char ch, int row ){
		if (ch instanceof GiantTentacle.Segment || ch instanceof HarpoonCannon) return false;
		int x = KrakenLevel.shipX(ch.pos);
		int[] tries = {KrakenLevel.cell(x, row - 1), KrakenLevel.cell(x, row + 1),
				KrakenLevel.cell(x - 1, row - 1), KrakenLevel.cell(x + 1, row + 1),
				KrakenLevel.cell(x + 1, row - 1), KrakenLevel.cell(x - 1, row + 1)};
		for (int c : tries){
			if (L.deck(c) && Actor.findChar(c) == null){
				int from = ch.pos;
				ch.pos = c;
				Actor.add(new Pushing(ch, from, c));
				L.occupyCell(ch);
				return true;
			}
		}
		int c = L.freeDeckNear(ch.pos);
		if (c == -1) return false;
		int from = ch.pos;
		ch.pos = c;
		Actor.add(new Pushing(ch, from, c));
		L.occupyCell(ch);
		return true;
	}

	//harpoon hit, tentacle chills for a bit
	public void stun( int turns ){
		thrashTimer += turns;
	}

	public GiantTentacle activeTentacle(){
		Actor a = tentacle == -1 ? null : Actor.findById(tentacle);
		return a instanceof GiantTentacle && ((GiantTentacle) a).isAlive() ? (GiantTentacle) a : null;
	}

	private void tentacleTurn( KrakenLevel L ){
		boolean rising = L.riseRow != -1;
		GiantTentacle t = activeTentacle();
		if (!rising && t == null){
			//tentacle down -> full sail
			L.stage = KrakenLevel.SAILING;
			L.distanceMax = L.distance = L.assault == 4 ? 4 : 13 - L.assault;
			KrakenHud.banner(Messages.get(this, L.assault == 4 ? "banner_spiral" : "banner_sail"));
			return;
		}
		hazards(L, true);
		if (t != null && --thrashTimer <= 0){
			thrashTimer = (L.solo() ? 8 : 6) - Math.min(2, L.assault / 2);
			ArrayList<Integer> cells = new ArrayList<>();
			cells.addAll(L.rowCells(t.row - 1));
			cells.addAll(L.rowCells(t.row + 1));
			announce(THRASH, cells, t.row, 0f);
			KrakenVisuals.thrash();
		}
		if (L.assault == 4 && L.countdown > 0){
			L.countdown--;
			if (--crushTimer <= 0){
				crushTimer = 6;
				ArrayList<Integer> deck = L.deckCells();
				if (!deck.isEmpty()) L.addHole(Random.element(deck));
			}
			if (!L.solo() || turns % 2 == 0) L.damageShip(1);
			if (L.countdown == 30 || L.countdown == 10) KrakenHud.banner(Messages.get(this, "banner_countdown", L.countdown));
			if (L.countdown <= 0){
				shipLost(true);
			}
		}
	}

	private void sailingTurn( KrakenLevel L ){
		L.distance -= helmManned() ? 2 : 1;
		hazards(L, false);
		if (L.distance <= 0){
			L.distance = 0;
			arrive(L);
		}
	}

	private void arrive( KrakenLevel L ){
		L.stage = KrakenLevel.ARRIVED;
		L.countdown = L.assault == 4 ? -1 : L.countdown;
		moveTo(KrakenLevel.eyeCell());
		invisible = 0;
		KrakenVisuals.headRises();
		Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 0.5f);
		yell(Messages.get(this, "arrive_" + L.assault));
		switch (L.assault){
			case 1: default:
				L.eyeOpen = true;
				break;
			case 2:
				jet(L);
				break;
			case 3:
				spawnGuards(L);
				L.eyeOpen = true;
				guardTimer = 0;
				break;
			case 4:
				L.eyeOpen = true;
				break;
		}
		KrakenHud.banner(Messages.get(this, L.assault == 3 ? "banner_guards" : "banner_eye"));
		jetTimer = 4;
	}

	private void arrivedTurn( KrakenLevel L ){
		if (L.assault == 2 && !L.eyeOpen && !hasPending(JET)){
			L.eyeOpen = true;
		}
		if (L.assault == 2 && --jetTimer <= 0 && !hasPending(JET)){
			jetTimer = 7;
			jet(L);
		}
		if (L.assault == 3 && !guardsAlive()){
			if (++guardTimer >= 8){
				spawnGuards(L);
				guardTimer = 0;
				KrakenHud.banner(Messages.get(this, "banner_regrow"));
			}
		}
		if (--lightningTimer <= 0){
			lightningTimer = 5;
			lightning(L, 1);
		}
	}

	private boolean hasPending( int kind ){
		for (Pending p : pending) if (p.kind == kind) return true;
		return false;
	}

	//water jet down the middle of the ship
	private void jet( KrakenLevel L ){
		ArrayList<Integer> cells = new ArrayList<>();
		Hero h = Party.nearestLiving(KrakenLevel.bowsprit());
		int x = 5;
		if (h != null && L.deck(h.pos) && KrakenLevel.shipY(h.pos) < 14) x = Math.max(1, Math.min(9, KrakenLevel.shipX(h.pos)));
		for (int y = 0; y <= 14; y++){
			int c = KrakenLevel.cell(x, y);
			if (L.deck(c)) cells.add(c);
		}
		if (cells.isEmpty()) return;
		L.eyeOpen = false;
		announce(JET, cells, x, 0f);
		yell(Messages.get(this, "jet"));
	}

	private void spawnGuards( KrakenLevel L ){
		int[] spots = {KrakenLevel.cell(3, 1), KrakenLevel.cell(7, 1)};
		for (int c : spots){
			if (Actor.findChar(c) != null) continue;
			GuardTentacle g = new GuardTentacle();
			g.pos = c;
			g.state = g.HUNTING;
			GameScene.add(g);
			KrakenVisuals.splash(c);
		}
	}

	public static boolean guardsAlive(){
		if (Dungeon.level == null) return false;
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof GuardTentacle && m.isAlive()) return true;
		}
		return false;
	}

	public static boolean helmManned(){
		Char ch = Actor.findChar(KrakenLevel.helmsmanCell());
		return ch instanceof Hero && ch.isAlive();
	}

	private void hazards( KrakenLevel L, boolean full ){
		int a = L.assault;
		boolean solo = L.solo();
		if (--lightningTimer <= 0){
			lightningTimer = full ? Math.max(solo ? 4 : 3, (solo ? 7 : 6) - a) : (solo ? 6 : 5);
			int n = 1 + (a >= 3 && !solo ? 1 : 0) + (Party.living().size() >= 3 ? 1 : 0);
			if (!full) n = 1;
			lightning(L, n);
		}
		if (full && --monsterTimer <= 0){
			monsterTimer = solo ? 11 - a : Math.max(6, 11 - a);
			monsters(L);
		}
		if (a >= 2 && --waveTimer <= 0){
			waveTimer = full ? 7 : 6;
			wave(L);
		}
		if (full && a == 3 && --inkTimer <= 0){
			inkTimer = 11;
			Hero h = Random.element(Party.living());
			if (h != null){
				GameScene.add(Blob.seed(h.pos, 70, UnderseaFeatures.InkCloud.class));
				KrakenVisuals.splash(h.pos);
				GLog.w(Messages.get(this, "ink"));
			}
		}
		if (--harpoonTimer <= 0){
			harpoonTimer = 9;
			harpoon(L);
		}
		//mast: 0 ok, 1 burning, 2 down, 3 falling (iirc)
		if (L.mast == 1){
			if (--L.mastTimer <= 0){
				L.mast = 3; //telegraphed
				ArrayList<Integer> cells = new ArrayList<>();
				for (int x = 0; x < KrakenLevel.SHIP_W; x++){
					int c = KrakenLevel.cell(x, KrakenLevel.MAST_ROW);
					if (KrakenLevel.fallenMastCell(c, L.mastSide)) cells.add(c);
				}
				announce(MAST_FALL, cells, KrakenLevel.MAST_ROW, 0f);
				KrakenHud.banner(Messages.get(this, "banner_mast"));
			}
		}
	}

	private void lightning( KrakenLevel L, int n ){
		ArrayList<Integer> cells = new ArrayList<>();
		ArrayList<Hero> living = Party.living();
		//assault 3: first bolt always hits the mast. drama
		if (L.assault == 3 && L.mast == 0 && !mastStruck){
			mastStruck = true;
			cells.add(KrakenLevel.mastCell());
			n--;
		}
		ArrayList<Integer> deck = L.deckCells();
		for (int i = 0; i < n; i++){
			int c;
			ArrayList<Integer> limbs = LabQuest.helps() ? tentacleCells(L) : null;
			if (limbs != null && !limbs.isEmpty() && Random.Int(10) < 7){
				//lab quest done -> lightning hits tentacles instead. cheap payoff but people liked it
				c = Random.element(limbs);
			} else if (limbs == null && !living.isEmpty() && Random.Int(10) < 6){
				Hero h = Random.element(living);
				c = h.pos;
			} else {
				c = deck.isEmpty() ? -1 : Random.element(deck);
			}
			if (c != -1 && !cells.contains(c)) cells.add(c);
		}
		if (cells.isEmpty()) return;
		announce(LIGHTNING, cells, -1, 0f);
	}

	private ArrayList<Integer> tentacleCells( KrakenLevel L ){
		ArrayList<Integer> cells = new ArrayList<>();
		for (Mob m : L.mobs){
			if (m instanceof GiantTentacle.Segment && m.isAlive()) cells.add(m.pos);
		}
		return cells;
	}

	private void strike( KrakenLevel L, int cell ){
		KrakenVisuals.bolt(cell);
		if (cell == KrakenLevel.mastCell() && L.mast == 0){
			L.mast = 1;
			L.mastTimer = 8;
			L.mastSide = Random.Int(2) == 0 ? -1 : 1;
			GLog.w(Messages.get(this, "mast_fire"));
			return;
		}
		Char ch = Actor.findChar(cell);
		int dmg = scaled(Random.NormalIntRange(6 + 2*L.assault, 12 + 3*L.assault));
		if (ch != null && ch.isAlive()){
			if (LabQuest.boltOnTentacle(ch)){
				GLog.p(Messages.get(LabQuest.class, "bolt"));
			} else if (ch instanceof SKProps.ExplosiveBarrel){
				ch.damage(ch.HP + 1, this);
			} else if (SKCombat.foes(this, ch)){
				ch.damage(Math.max(1, dmg), this);
				//assault 4: chains between heroes standing close. spread out!!
				if (L.assault == 4 && ch instanceof Hero){
					for (Hero o : Party.living()){
						if (o != ch && L.distance(o.pos, ch.pos) <= 2){
							KrakenVisuals.arc(ch.pos, o.pos);
							o.damage(Math.max(1, dmg/2), this);
						}
					}
				}
			}
		}
		if (L.deck(cell)){
			L.damageShip(3);
			if (Random.Int(2) == 0) L.addHole(cell);
		}
	}

	private void wave( KrakenLevel L ){
		int row = Random.IntRange(5, 21);
		GiantTentacle t = activeTentacle();
		if (t != null && Math.abs(row - t.row) <= 1) row = row < t.row ? Math.max(5, t.row - 3) : Math.min(21, t.row + 3);
		ArrayList<Integer> cells = new ArrayList<>(L.rowCells(row));
		cells.addAll(L.rowCells(row + 1));
		int dir = Random.Int(2) == 0 ? -1 : 1;
		announce(WAVE, cells, dir * 100 + row, 0f);
		KrakenVisuals.waveComing(row, dir);
	}

	private void monsters( KrakenLevel L ){
		int alive = 0;
		for (Mob m : L.mobs){
			if (m instanceof UnderseaMobs.KrakenMinion || m instanceof UnderseaMobs.InfectedCrew) alive++;
		}
		int cap = L.solo() ? 1 : 1 + Party.living().size() / 2;
		int n = Party.living().size() >= 3 ? 2 : 1;
		for (int i = 0; i < n && alive < cap; i++){
			ArrayList<Integer> edge = new ArrayList<>();
			for (int c : L.deckCells()){
				int x = KrakenLevel.shipX(c);
				if ((x == 1 || x == 9) && Actor.findChar(c) == null) edge.add(c);
			}
			if (edge.isEmpty()) return;
			int c = Random.element(edge);
			SKMob m = Random.Int(2) == 0 ? new UnderseaMobs.KrakenMinion() : new UnderseaMobs.InfectedCrew();
			m.summoned();
			m.pos = c;
			m.state = m.HUNTING;
			GameScene.add(m);
			KrakenVisuals.splash(c);
			alive++;
		}
		GLog.w(Messages.get(this, "boarders"));
	}

	private void harpoon( KrakenLevel L ){
		int onDeck = 0;
		for (Heap h : L.heaps.valueList()){
			for (Item i : h.items) if (i instanceof KrakenHarpoon) onDeck++;
		}
		if (onDeck >= 2) return;
		ArrayList<Integer> deck = L.deckCells();
		for (int tries = 0; tries < 20 && !deck.isEmpty(); tries++){
			int c = Random.element(deck);
			if (L.heaps.get(c) == null && Actor.findChar(c) == null){
				L.drop(new KrakenHarpoon(), c).sprite.drop();
				KrakenVisuals.splash(c);
				return;
			}
		}
	}

	private void resolvePending( KrakenLevel L ){
		ArrayList<Pending> due = new ArrayList<>();
		for (Pending p : pending){
			if (p.at <= now() + 0.01f) due.add(p);
		}
		pending.removeAll(due);
		for (Pending p : due){
			ArrayList<Integer> cells = new ArrayList<>();
			for (int c : p.cells) cells.add(c);
			switch (p.kind){
				case SLAM:
					slam(L, p);
					break;
				case LIGHTNING:
					for (int c : cells) strike(L, c);
					Game.runOnRenderThread(new Callback() {
						@Override
						public void call() {
							GameScene.flash(0x80FFFFFF);
						}
					});
					Sample.INSTANCE.play(Assets.Sounds.LIGHTNING);
					break;
				case WAVE: {
					final int dir = p.aim > 0 ? 1 : -1;
					int row = p.aim > 0 ? p.aim - 100 : p.aim + 100;
					KrakenVisuals.waveCrash(row, dir);
					Sample.INSTANCE.play(Assets.Sounds.WATER, 1f, 0.6f);
					ArrayList<Char> hit = new ArrayList<>();
					for (int c : cells){
						Char ch = Actor.findChar(c);
						if (ch != null && pushable(ch)) hit.add(ch);
					}
					//push the ones closest to the wave's end first or they block each other
					java.util.Collections.sort(hit, new java.util.Comparator<Char>() {
						@Override
						public int compare( Char a, Char b ) {
							return Integer.compare(dir * KrakenLevel.shipX(b.pos), dir * KrakenLevel.shipX(a.pos));
						}
					});
					for (Char ch : hit) push(L, ch, dir, 0, 3);
					break;
				}
				case JET: {
					KrakenVisuals.jet(p.aim);
					Sample.INSTANCE.play(Assets.Sounds.WATER, 1f, 0.4f);
					ArrayList<Char> hit = SKCombat.foesIn(this, cells);
					for (Char ch : hit){
						ch.damage(Math.max(1, scaled(Random.NormalIntRange(12, 20)) - ch.drRoll()/2), this);
					}
					java.util.Collections.sort(hit, new java.util.Comparator<Char>() {
						@Override
						public int compare( Char a, Char b ) {
							return Integer.compare(b.pos, a.pos);
						}
					});
					for (Char ch : hit) if (ch.isAlive() && pushable(ch)) push(L, ch, 0, 1, 2);
					L.damageShip(6);
					if (!cells.isEmpty()) L.addHole(Random.element(cells));
					L.eyeOpen = true;
					break;
				}
				case THRASH: {
					KrakenVisuals.thrashHit();
					for (Char ch : SKCombat.foesIn(this, cells)){
						ch.damage(Math.max(1, scaled(Random.NormalIntRange(6, 12 + 2*L.assault)) - ch.drRoll()/2), this);
					}
					break;
				}
				case MAST_FALL: {
					L.mast = 2;
					KrakenVisuals.mastFalls();
					Sample.INSTANCE.play(Assets.Sounds.ROCKS);
					for (int c : cells){
						Char ch = Actor.findChar(c);
						if (ch != null && SKCombat.foes(this, ch)){
							ch.damage(Math.max(1, scaled(Random.NormalIntRange(15, 25)) - ch.drRoll()/2), this);
						}
						L.removeHole(c);
						L.updateCellFlags(c);
						Heap heap = L.heaps.get(c);
						if (heap != null){
							int to = L.freeDeckNear(c + KrakenLevel.W);
							if (to != -1) {
								for (Item it : heap.items.toArray(new Item[0])) L.drop(it, to).sprite.drop(c);
								heap.destroy();
							}
						}
						if (ch != null && ch.isAlive() && !(ch instanceof GiantTentacle.Segment)){
							shove(L, ch, KrakenLevel.MAST_ROW);
						}
					}
					L.updateCellFlags(KrakenLevel.mastCell());
					GameScene.updateMap();
					L.damageShip(10);
					break;
				}
			}
		}
	}

	private boolean pushable( Char ch ){
		if (ch instanceof GiantTentacle.Segment || ch instanceof HarpoonCannon || ch instanceof Grumete) return false;
		if (ch instanceof SKProps.SKProp) return false;
		return !ch.properties().contains(Property.IMMOVABLE);
	}

	//push across the deck, past the rail = sea
	public static void push( KrakenLevel L, Char ch, int dx, int dy, int steps ){
		for (int i = 0; i < steps && ch.isAlive(); i++){
			int next = ch.pos + dx + dy * KrakenLevel.W;
			if (L.deck(next)){
				if (Actor.findChar(next) != null) return;
				int from = ch.pos;
				ch.pos = next;
				Actor.add(new Pushing(ch, from, next));
				L.occupyCell(ch);
				continue;
			}
			char c = KrakenLevel.plan(next);
			if (c == '#' || c == ' '){
				//bye
				int sea = next;
				while (KrakenLevel.onShip(sea) && sea >= 0 && sea < L.length()) sea += dx + dy * KrakenLevel.W;
				if (sea < 0 || sea >= L.length()) return;
				if (ch instanceof Hero){
					KrakenHold.overboard((Hero) ch, sea);
				} else {
					KrakenVisuals.splash(next);
					ch.die(null);
				}
			}
			return;
		}
	}

	private int scaled( int dmg ){
		return Math.round(dmg * CoopBalance.damageFactor(this));
	}

	//ship sank / got crushed. everyone dies, no exceptions
	public static void shipLost( boolean crushed ){
		KrakenLevel L = level();
		if (L == null || L.stage == KrakenLevel.SUNK || L.stage == KrakenLevel.WON) return;
		L.stage = KrakenLevel.SUNK;
		KrakenVisuals.sinking();
		GLog.n(Messages.get(Kraken.class, crushed ? "crushed" : "sunk"));
		Kraken k = find();
		for (Hero h : partyHeroes()){
			if (h.isAlive()){
				h.damage(h.HT * 10 + h.shielding() + 100, k != null ? k : new Kraken());
			}
		}
	}

	@Override
	public void die( Object cause ) {
		if (HP > 0) return;
		KrakenLevel L = level();
		super.die(cause);
		if (L == null) return;
		L.stage = KrakenLevel.WON;
		L.eyeOpen = false;
		L.riseRow = -1;
		L.countdown = -1;
		L.holes.clear();

		//clean up every mob the kraken brought
		for (Mob m : L.mobs.toArray(new Mob[0])){
			if (m == this) continue;
			if (m instanceof GiantTentacle || m instanceof GiantTentacle.Segment || m instanceof GuardTentacle
					|| m instanceof UnderseaMobs.KrakenMinion || m instanceof UnderseaMobs.InfectedCrew
					|| m instanceof UnderseaFeatures.KrakenTentacle){
				m.destroy();
				if (m.sprite != null) m.sprite.killAndErase();
			}
		}
		for (Hero h : partyHeroes()){
			KrakenHold hold = h.buff(KrakenHold.class);
			if (hold != null) hold.release(false);
			ShipHammer hammer;
			while ((hammer = h.belongings.getItem(ShipHammer.class)) != null) hammer.detachAll(h.belongings.backpack);
			KrakenHarpoon harpoon;
			while ((harpoon = h.belongings.getItem(KrakenHarpoon.class)) != null) harpoon.detachAll(h.belongings.backpack);
		}

		//harpoons on the deck too, nobody gets to keep them
		for (Heap heap : L.heaps.valueList()){
			for (Item i : heap.items.toArray(new Item[0])){
				if (i instanceof KrakenHarpoon) heap.items.remove(i);
			}
			if (heap.items.isEmpty()) heap.destroy();
			else if (heap.sprite != null) heap.sprite.view(heap);
		}

		//loot, delante del mástil
		ArrayList<Item> loot = new ArrayList<>();
		loot.add(new WornKey(20));
		loot.addAll(CoopRewards.crowns());
		loot.add(new AbyssSonar().random());
		loot.add(SKArmory.random(SKArmory.tierForDepth(20) + 1));
		loot.add(new KrakenEyeGun().random());
		CoopRewards.dropAround(loot, KrakenLevel.cell(5, 7));

		GameScene.bossSlain();
		Statistics.bossScores[3] += 3000;
		Badges.validateBossSlain();

		KrakenVisuals.victory();
		KrakenHud.banner(Messages.get(Kraken.class, "banner_won"));
		yell(Messages.get(this, "defeated"));
		GLog.p(Messages.get(this, "hint_leave"));
		L.playLevelMusic();
	}

	@Override
	public String description() {
		return Messages.get(this, "desc");
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("tentacle", tentacle);
		bundle.put("t_lightning", lightningTimer);
		bundle.put("t_monster", monsterTimer);
		bundle.put("t_wave", waveTimer);
		bundle.put("t_ink", inkTimer);
		bundle.put("t_harpoon", harpoonTimer);
		bundle.put("t_thrash", thrashTimer);
		bundle.put("t_jet", jetTimer);
		bundle.put("t_push", pushTimer);
		bundle.put("t_guard", guardTimer);
		bundle.put("t_crush", crushTimer);
		bundle.put("turns", turns);
		bundle.put("eye_hit", eyeHitThisApproach);
		bundle.put("mast_struck", mastStruck);
		bundle.put("intro_done", introDone);
		int n = pending.size();
		int[] kinds = new int[n], aims = new int[n];
		float[] ats = new float[n];
		for (int i = 0; i < n; i++){
			Pending p = pending.get(i);
			kinds[i] = p.kind;
			aims[i] = p.aim;
			ats[i] = p.at;
			bundle.put("p_cells_" + i, p.cells);
		}
		bundle.put("p_kinds", kinds);
		bundle.put("p_aims", aims);
		bundle.put("p_ats", ats);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		tentacle = bundle.getInt("tentacle");
		lightningTimer = bundle.getInt("t_lightning");
		monsterTimer = bundle.getInt("t_monster");
		waveTimer = bundle.getInt("t_wave");
		inkTimer = bundle.getInt("t_ink");
		harpoonTimer = bundle.getInt("t_harpoon");
		thrashTimer = bundle.getInt("t_thrash");
		jetTimer = bundle.getInt("t_jet");
		pushTimer = bundle.getInt("t_push");
		guardTimer = bundle.getInt("t_guard");
		crushTimer = bundle.getInt("t_crush");
		turns = bundle.getInt("turns");
		eyeHitThisApproach = bundle.getBoolean("eye_hit");
		mastStruck = bundle.getBoolean("mast_struck");
		introDone = bundle.getBoolean("intro_done");
		pending.clear();
		if (bundle.contains("p_kinds")){
			int[] kinds = bundle.getIntArray("p_kinds");
			int[] aims = bundle.getIntArray("p_aims");
			float[] ats = bundle.getFloatArray("p_ats");
			for (int i = 0; i < kinds.length; i++){
				Pending p = new Pending();
				p.kind = kinds[i];
				p.aim = aims[i];
				p.at = ats[i];
				p.cells = bundle.getIntArray("p_cells_" + i);
				pending.add(p);
			}
		}
	}

	//dummy sprite. the real kraken is KrakenVisuals, this is just so damage numbers have somewhere to go
	public static class KrakenProxySprite extends MobSprite {
		public KrakenProxySprite(){
			super();
			texture(KrakenAssets.HOLES);
			TextureFilm film = new TextureFilm(texture, 16, 16);
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
			//yes it's empty
		}
		@Override
		public int blood() {
			return 0xFF7A1A4A;
		}
	}
}
