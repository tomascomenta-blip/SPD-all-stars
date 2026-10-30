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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ShadowCaster;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.SacredEssence;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.awake.Awakening;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.big.Colossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.hunt.Climbing;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.hunt.Hunt;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.hunt.MutationPart;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.hunt.Mutations;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.AshFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Collection;

//the big beasts of the ash plains: what every one of them shares. they see little in the dark
//(a torch gives you away from far), they hear fights, some keep to a patch of ground, some carry
//fleas and a few carry sacred essence inside. their heavy hits are always marked first (warn)
public abstract class AshColossus extends Colossus implements Fauna {

	{
		WANDERING = new Roam();
		state = WANDERING;
	}

	//sacred essence inside it (a Continent source) or -1
	public int essence = -1;
	//where it lives and how far it strays from there (0 = it goes anywhere)
	public int home = -1;
	public int territory = 0;
	//which way it faces. the sheets face right; kept by the game, never read off the sprite
	public boolean right = true;
	//ember fleas living on it
	public int fleas = 0;

	//heavy blows add up; past a point it goes down for a few turns with its weak spot open
	public float stagger = 0;
	public int down = 0;
	private int downs = 0;
	//the party's now (Hunt): follows them, fights for them, scares the lesser things off
	public boolean tamed = false;
	private int shakeT = 0, scareT = 0;

	private float listened = 0;

	//how much further than a man it makes things out, and hears them
	protected int eyes(){
		return size / 3;
	}

	protected int ears(){
		return 0;
	}

	//grazers: they don't come for anyone, whatever they see
	public boolean peaceful(){
		return false;
	}

	//how far it wanders off in one go
	protected int roam(){
		return 20;
	}

	//fire beasts walk the lava too
	protected boolean wades(){
		return Char.hasProp(this, Property.FIERY);
	}

	@Override
	protected boolean walkable( int cell ) {
		return super.walkable(cell) && (wades() || !AshFauna.lava(cell));
	}

	@Override
	protected void onAdd() {
		super.onAdd();
		viewDistance = Math.min(ShadowCaster.MAX_DISTANCE, half() + Senses.LIT_SIGHT + eyes());
		if (home == -1) home = pos;
		if (listened == 0) listened = Actor.now();
	}

	// *** senses ***

	@Override
	protected Char chooseEnemy() {
		Senses.dim(this, fieldOfView, eyes());
		Char e = super.chooseEnemy();
		//the territorial ones don't even look at whoever is well out of their ground (or they would
		//give up and take them up again in the same turn, forever)
		if (e != null && !tamed && territory > 0 && home != -1 && Dungeon.level.distance(home, e.pos) > territory + 6){
			e = null;
		}
		//tamed, it stays with the party: no chasing what it scared off, nor a fight that takes it far
		if (Hunt.letsGo(this, e)) e = null;
		//whoever is up on its back is out of reach: it goes for the others and shakes that one off
		if (e != null && Climbing.on(e, this)){
			e = null;
			for (Hero h : Party.living()){
				if (!Climbing.on(h, this) && fieldOfView[h.pos]){
					e = h;
					break;
				}
			}
		}
		return e;
	}

	@Override
	protected boolean act() {
		if (down > 0){
			if (--down == 0) standUp();
			spend(TICK);
			return true;
		}
		stagger *= 0.97f;
		if (tamed){
			scareOff();
		} else if (paralysed <= 0 && shakeOff()){
			return true;
		}
		if (paralysed <= 0 && !farFromParty()){
			if (!tamed) listen();
			shedFleas();
			if (!tamed) keepToTerritory();
		}
		return super.act();
	}

	private void listen(){
		Senses.Noise n = Senses.heard(this, listened, ears());
		listened = Actor.now();
		if (n == null) return;
		if (state == HUNTING){
			//lost sight of them: the racket says where they went
			if (enemy == null || !enemySeen) target = n.cell;
		} else {
			heard(n);
		}
	}

	//a noise while it's not after anyone. by default it goes to see
	protected void heard( Senses.Noise n ){
		if (peaceful() || tamed) return;
		beckon(n.cell);
	}

	//the territorial ones give up on whoever runs far enough, and go back
	private void keepToTerritory(){
		if (territory <= 0 || home == -1 || state != HUNTING || enemy == null || Awakening.on()) return;
		if (Dungeon.level.distance(home, enemy.pos) > territory + 6 && pendingKind == -1){
			state = WANDERING;
			target = home;
			enemy = null;
			enemySeen = false;
			if (sprite != null) sprite.showLost();
		}
	}

	// *** fleas ***

	//whoever walks close to a flea-ridden beast gets jumped on
	private void shedFleas(){
		if (fleas <= 0) return;
		for (Hero h : Party.living()){
			if (h.invisible > 0 || reach(h.pos) > 2) continue;
			if (Random.Int(2) == 0) return;
			int n = Math.min(fleas, 1 + Random.Int(2)), off = 0;
			for (int i = 0; i < n; i++){
				if (EmberFlea.leap(this, h)) off++;
			}
			fleas -= off;
			if (off > 0 && h == Dungeon.local()) GLog.w(Messages.get(EmberFlea.class, "leap"));
			onFleasLost();
			return;
		}
	}

	protected void onFleasLost(){
	}

	// *** walking ***

	public void face( int cell ){
		if (cell < 0) return;
		int w = Dungeon.level.width();
		if (cell % w > pos % w) right = true;
		else if (cell % w < pos % w) right = false;
	}

	@Override
	protected void walkTo( int step ) {
		face(step);
		super.walkTo(step);
	}

	//towards the spot nearest the target where its whole body fits (the target itself rarely is one)
	@Override
	protected boolean getCloser( int target ) {
		if (rooted || target < 0 || target == pos) return false;
		boolean[] f = fits();
		int goal = target < f.length && f[target] ? target : nearestFit(f, target);
		if (goal == -1 || goal == pos) return false;
		int step = PathFinder.getStep(pos, goal, f);
		if (step == -1 || !f[step]) return false;
		walkTo(step);
		return true;
	}

	protected int nearestFit( boolean[] f, int cell ){
		Level l = Dungeon.level;
		int w = l.width(), cx = cell % w, cy = cell / w, r = size + 2;
		int best = -1, bestD = Integer.MAX_VALUE;
		for (int y = Math.max(0, cy - r); y <= Math.min(l.height() - 1, cy + r); y++){
			for (int x = Math.max(0, cx - r); x <= Math.min(w - 1, cx + r); x++){
				int c = x + y * w;
				if (!f[c]) continue;
				int d = Math.max(Math.abs(x - cx), Math.abs(y - cy));
				if (d < bestD){
					bestD = d;
					best = c;
				}
			}
		}
		return best;
	}

	//a random spot where it fits, within radius of center, or -1
	protected int fitNear( int center, int radius ){
		boolean[] f = fits();
		Level l = Dungeon.level;
		int w = l.width(), cx = center % w, cy = center / w;
		ArrayList<Integer> cells = new ArrayList<>();
		for (int y = Math.max(0, cy - radius); y <= Math.min(l.height() - 1, cy + radius); y++){
			for (int x = Math.max(0, cx - radius); x <= Math.min(w - 1, cx + radius); x++){
				if (f[x + y * w]) cells.add(x + y * w);
			}
		}
		return cells.isEmpty() ? -1 : Random.element(cells);
	}

	//wandering. one with a leader (a herd) keeps a few cells off its side instead of walking into it
	protected class Roam extends Wandering {

		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			Actor a = leader == -1 ? null : Actor.findById(leader);
			if (a instanceof Char && ((Char) a).isAlive()){
				int lp = ((Char) a).pos, gap = size + 4;
				if (Dungeon.level.distance(pos, lp) > gap && (target == -1 || Dungeon.level.distance(target, lp) > gap)){
					int c = fitNear(lp, gap - 1);
					if (c != -1) target = c;
				}
			} else {
				leader = -1;
			}
			if (peaceful()) return continueWandering();
			return super.act(enemyInFOV, justAlerted);
		}

		//from the edge of the body, not its middle
		@Override
		protected float detectionChance( Char enemy ) {
			return 1 / (reach(enemy.pos) / 2f + enemy.stealth());
		}

		@Override
		protected int randomDestination() {
			if (tamed){
				Hero h = nearestHero();
				int c = h == null ? -1 : fitNear(h.pos, half() + 3);
				if (c != -1) return c;
			}
			//round its home, not round wherever it got to: left to drift, they all end up in a heap.
			//and not right next to another big one
			int center = home != -1 ? home : pos;
			int radius = territory > 0 && home != -1 ? territory : roam();
			for (int tries = 0; tries < 6; tries++){
				int c = fitNear(center, radius);
				if (c == -1) break;
				if (leader != -1 || !crowded(c)) return c;
			}
			return pos;
		}
	}

	// *** fighting ***

	//like announce, with more time to get away (a zone of radius r needs r+1 steps out of it)
	protected void warn( int kind, Collection<Integer> cells, int aim, float turns ){
		face(aim);
		pendingKind = kind;
		pendingAim = aim;
		pendingCells = new int[cells.size()];
		int i = 0;
		for (int c : cells) pendingCells[i++] = c;
		float delay = SKCombat.announceDelay(turns);
		SKCombat.mark(cells, delay);
		CoopLog.line("fauna: " + getClass().getSimpleName() + " warns " + kind + " on " + cells.size() + " cells for " + delay + " t=" + (int) Actor.now());
		if (sprite != null){
			if (aim != -1) sprite.turnTo(pos, aim);
			if (sprite instanceof AshColossusSprite) ((AshColossusSprite) sprite).windup(kind);
			if (seen()) sprite.showStatus(CharSprite.WARNING, "!");
		}
		spend(delay);
	}

	//a marked hit landing, written down so the numbers can be checked
	@Override
	protected void blast( Char ch, int min, int max ) {
		int before = ch.HP;
		super.blast(ch, min, max);
		CoopLog.line("fauna: " + getClass().getSimpleName() + " hits " + ch.getClass().getSimpleName() + " for " + (before - ch.HP) + " (" + ch.HP + " left)");
	}

	protected void strikeAnim( int kind ){
		if (sprite instanceof AshColossusSprite) ((AshColossusSprite) sprite).strike(kind);
	}

	//whether a blow over those cells catches someone: anywhere on its body if it's a big one too
	protected boolean caught( Collection<Integer> cells, Char ch ){
		if (!(ch instanceof Colossus)) return cells.contains(ch.pos);
		for (int c : cells){
			if (((Colossus) ch).covers(c)) return true;
		}
		return false;
	}

	//every open cell of a square of radius r around center, off its own body
	protected ArrayList<Integer> square( int center, int r ){
		ArrayList<Integer> out = new ArrayList<>();
		for (int c : SKCombat.area(center, r)){
			if (!covers(c) && !Dungeon.level.pit[c]) out.add(c);
		}
		return out;
	}

	//the ground from 1 to r cells off its body
	protected ArrayList<Integer> ring( int r ){
		ArrayList<Integer> out = new ArrayList<>();
		Level l = Dungeon.level;
		int w = l.width();
		for (int y = top() - r; y < top() + size + r; y++){
			for (int x = left() - r; x < left() + size + r; x++){
				if (x < 0 || y < 0 || x >= w || y >= l.height()) continue;
				int c = x + y * w;
				if (!covers(c) && !l.solid[c] && !l.pit[c]) out.add(c);
			}
		}
		return out;
	}

	//shoves something a few cells straight away from the body, as far as there's room
	protected void knock( Char ch, int cells ){
		if (!ch.isAlive() || ch.rooted || Char.hasProp(ch, Property.IMMOVABLE) || ch instanceof Colossus) return;
		Level l = Dungeon.level;
		int w = l.width();
		int dx = Integer.signum(ch.pos % w - pos % w), dy = Integer.signum(ch.pos / w - pos / w);
		int to = ch.pos;
		for (int i = 0; i < cells; i++){
			int next = to + dx + dy * w;
			if (next < 0 || next >= l.length() || !l.passable[next] || l.pit[next] && !ch.flying
					|| Actor.findChar(next) != null || covers(next)) break;
			to = next;
		}
		if (to == ch.pos) return;
		ch.pos = to;
		if (ch.sprite != null) ch.sprite.place(to);
		l.occupyCell(ch);
		if (ch instanceof Hero) Dungeon.observe();
	}

	// *** knocked down ***

	//the blows it takes before going down, more each time. the biggest ones have so much life that
	//a smaller share of it is enough
	protected float staggerLimit(){
		return HT * (size >= 15 ? 0.05f : size >= 8 ? 0.07f : 0.10f) * (1 + 0.5f * downs);
	}

	//standing next to a weak spot (or up on its back) the blows land where it hurts
	public boolean atWeakSpot( Char ch ){
		if (Climbing.on(ch, this)) return true;
		for (int c : weakCells()){
			if (Dungeon.level.distance(ch.pos, c) <= 1) return true;
		}
		return false;
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (dmg > 0 && isAlive() && !tamed){
			boolean weak = false;
			if (down > 0){
				dmg *= 2;
				weak = true;
			} else if (src instanceof Hero && Climbing.on((Hero) src, this)){
				dmg = Math.round(dmg * 2.5f);
				weak = true;
			} else if (src instanceof Hero && atWeakSpot((Hero) src)){
				dmg = Math.round(dmg * 1.75f);
				weak = true;
			}
			if (weak && sprite != null && seen()) sprite.showStatus(CharSprite.WARNING, Messages.get(AshColossus.class, "weak_hit"));
			if (size >= 5 && down == 0){
				stagger += dmg * (weak ? 2 : 1) * Mutations.staggerFactor(src instanceof Char ? (Char) src : null);
				if (stagger >= staggerLimit() && HP > dmg) knockDown();
			}
		}
		super.damage(dmg, src);
	}

	@Override
	public int defenseSkill( Char enemy ) {
		return down > 0 ? 0 : super.defenseSkill(enemy);
	}

	public void knockDown(){
		down = 3 + size / 4;
		downs++;
		stagger = 0;
		pendingKind = -1;
		pendingCells = new int[0];
		CoopLog.line("hunt: " + getClass().getSimpleName() + " knocked down for " + down + " t=" + (int) Actor.now());
		if (seen()) GLog.p(Messages.get(AshColossus.class, "down", name()));
		Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 0.6f);
		if (sprite instanceof AshColossusSprite) ((AshColossusSprite) sprite).knockDown();
	}

	protected void standUp(){
		if (seen()) GLog.w(Messages.get(AshColossus.class, "rise", name()));
		if (sprite instanceof AshColossusSprite) ((AshColossusSprite) sprite).rise();
	}

	// *** climbed ***

	//every few turns with someone up there it throws itself about, the turn before it's seen coming
	private boolean shakeOff(){
		if (Climbing.climbers(this).isEmpty()){
			shakeT = 0;
			return false;
		}
		shakeT++;
		if (shakeT == 3 && sprite != null) sprite.showStatus(CharSprite.WARNING, "!");
		if (shakeT < 4) return false;
		shakeT = 0;
		Climbing.shake(this, 30);
		spend(TICK);
		return true;
	}

	// *** tamed ***

	//null when it lets this hero put the collar on, otherwise why not
	public String tameBlock( Hero h ){
		return Messages.get(AshColossus.class, "untamable");
	}

	public void tame( Hero h ){
		tamed = true;
		alignment = Alignment.ALLY;
		state = WANDERING;
		enemy = null;
		enemySeen = false;
		territory = 0;
		down = 0;
		pendingKind = -1;
		pendingCells = new int[0];
		Hunt.tamed = id();
		CoopLog.line("hunt: hero " + Party.indexOf(h) + " tames " + getClass().getSimpleName());
		GLog.p(Messages.get(AshColossus.class, "tamed", name()));
		Sample.INSTANCE.play(Assets.Sounds.MASTERY);
		if (sprite instanceof AshColossusSprite) ((AshColossusSprite) sprite).tame();
	}

	//the party's own beast: a hero right by it climbs up on its back to rest there (no grapple needed),
	//and walks off it by tapping the ground outside its body
	@Override
	public boolean canInteract( Char c ) {
		if (tamed && c instanceof Hero) return reach(c.pos) <= 1;
		return super.canInteract(c);
	}

	@Override
	public boolean interact( Char c ) {
		if (!tamed || !(c instanceof Hero)) return super.interact(c);
		Hero h = (Hero) c;
		if (Climbing.on(h, this)) return true;
		if (size < Climbing.SIZE){
			if (h == Dungeon.local()) GLog.w(Messages.get(AshColossus.class, "ride_small", name(), Climbing.SIZE));
			return true;
		}
		if (!Climbing.canClimb(h, this)){
			if (h == Dungeon.local()) GLog.w(Messages.get(AshColossus.class, "ride_full", name()));
			return true;
		}
		Climbing.climb(h, this);
		h.spend(1f);
		h.busy();
		return true;
	}

	protected Hero nearestHero(){
		Hero best = null;
		for (Hero h : Party.living()){
			if (best == null || reach(h.pos) < reach(best.pos)) best = h;
		}
		return best;
	}

	//nothing smaller comes close while it's with you, and it keeps up with the party
	private void scareOff(){
		if (++scareT < 5) return;
		scareT = 0;
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (Hunt.scares(this, m) && !m.isImmune(Terror.class)) Buff.affect(m, Terror.class, 5f).object = id();
		}
		Hero h = nearestHero();
		if (h != null && state != HUNTING && reach(h.pos) > half() + 4){
			int c = fitNear(h.pos, half() + 3);
			if (c != -1) target = c;
		}
	}

	// *** death ***

	@Override
	public void die( Object cause ) {
		int at = pos;
		if (tamed && Hunt.tamed == id()) Hunt.tamed = -1;
		int part = MutationPart.of(this);
		super.die(cause);
		if (part >= 0 && Random.Float() < MutationPart.chance(size)){
			Heap ph = Dungeon.level.drop(new MutationPart(part), dropCell(at));
			if (ph.sprite != null) ph.sprite.drop();
		}
		if (essence >= 0 && !Continent.hasClaimed(essence)){
			Heap h = Dungeon.level.drop(new SacredEssence(essence), dropCell(at));
			if (h.sprite != null) h.sprite.drop();
			GLog.p(Messages.get(AshColossus.class, "essence", name()));
		}
	}

	private static int dropCell( int at ){
		Level l = Dungeon.level;
		if (l.passable[at] && !l.pit[at]) return at;
		for (int n : PathFinder.NEIGHBOURS8){
			int c = at + n;
			if (c >= 0 && c < l.length() && l.passable[c] && !l.pit[c]) return c;
		}
		return at;
	}

	//the weak spots as cells of the map, mirrored when it faces left
	public ArrayList<Integer> weakCells(){
		ArrayList<Integer> out = new ArrayList<>();
		int w = Dungeon.level.width();
		for (int[] s : weakSpots()){
			int x = right ? s[0] : size - 1 - s[0];
			out.add(origin() + x + s[1] * w);
		}
		return out;
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("essence", essence);
		bundle.put("home", home);
		bundle.put("territory", territory);
		bundle.put("right", right);
		bundle.put("fleas", fleas);
		bundle.put("listened", listened);
		bundle.put("stagger", stagger);
		bundle.put("down", down);
		bundle.put("downs", downs);
		bundle.put("tamed", tamed);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		essence = bundle.contains("essence") ? bundle.getInt("essence") : -1;
		home = bundle.contains("home") ? bundle.getInt("home") : -1;
		territory = bundle.getInt("territory");
		right = bundle.getBoolean("right");
		fleas = bundle.getInt("fleas");
		listened = bundle.getFloat("listened");
		stagger = bundle.getFloat("stagger");
		down = bundle.getInt("down");
		downs = bundle.getInt("downs");
		tamed = bundle.getBoolean("tamed");
		if (tamed) alignment = Alignment.ALLY;
	}
}
