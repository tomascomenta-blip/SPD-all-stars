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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.cathedral;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.AshColossusSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.fauna.CityColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.LinkedHashSet;

//the heart of the furnace, on the altar at the far end of the apse: a lump of iron and yog flesh
//that beats, and every beat the whole cathedral burns. it never moves. it throbs out a ring of fire
//round itself, spits jets of flame down the nave at whoever keeps away, and when the fire runs low
//its grates let out the stokers that feed it. halfway through its cage cracks: the dais burns,
//the jets fan wider. break it and the whole thing dies (Cathedral.heartBroken)
public class FurnaceHeart extends CityColossus {

	{
		size = 3;
		spriteClass = HeartSprite.class;

		HP = HT = 2600;
		defenseSkill = 0;
		EXP = 150;

		minDmg = 40; maxDmg = 60; acc = 70;
		armorMin = 16; armorMax = 30;

		properties.add(Property.FIERY);
		properties.add(Property.INORGANIC);
		properties.add(Property.IMMOVABLE);
		properties.add(Property.MINIBOSS);

		state = PASSIVE;
	}

	private static final int PULSE = 0, JETS = 1;
	//how close someone has to come up to wake it, and how far its jets reach
	private static final int WAKE = 6, JET_RANGE = 10;

	private boolean awake = false, cracked = false;
	//turns it burns hotter after a stoker fed it, and to the next stokers
	private int stoked = 0, bellows = 2;

	public boolean awake(){
		return awake;
	}

	@Override
	protected void onAdd() {
		super.onAdd();
		//testing: a tenth of its life, to get to the end quickly
		if (DevFlags.cathedralWeak() && HP == HT) HP = Math.max(1, HT / 10);
	}

	@Override
	protected int eyes() {
		return 8;
	}

	@Override
	protected int specialCooldown() {
		return 2;
	}

	@Override
	public boolean peaceful() {
		return !awake;
	}

	// *** it never moves ***

	@Override
	protected boolean getCloser( int target ) {
		return false;
	}

	@Override
	protected boolean getFurther( int target ) {
		return false;
	}

	@Override
	public String tameBlock( Hero h ) {
		return Messages.get(this, "untamable");
	}

	// *** waking ***

	@Override
	protected boolean act() {
		if (!awake){
			Hero h = nearestHero();
			if (h == null || reach(h.pos) > WAKE){
				spend(TICK);
				return true;
			}
			wake(h);
		}
		if (stoked > 0) stoked--;
		if (bellows > 0) bellows--;
		return super.act();
	}

	private void wake( Char foe ){
		if (awake) return;
		awake = true;
		state = HUNTING;
		if (foe != null){
			enemy = foe;
			target = foe.pos;
		}
		CoopLog.line("cathedral: the heart wakes t=" + (int) Actor.now());
		BossHealthBar.assignBoss(this);
		GLog.n(Messages.get(this, "wakes"));
		Sample.INSTANCE.play(Assets.Sounds.BOSS, 1f, 0.7f);
		Music.INSTANCE.play(Assets.Music.CITY_BOSS, true);
		Senses.noise(pos, Senses.ROAR);
	}

	@Override
	protected Char chooseEnemy() {
		if (!awake) return null;
		//it feels everyone in the cathedral: the nearest one
		Hero best = nearestHero();
		return best != null ? best : super.chooseEnemy();
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (!awake && dmg > 0) wake(src instanceof Char ? (Char) src : nearestHero());
		super.damage(dmg, src);
		if (isAlive() && !cracked && HP <= HT / 2) crack();
	}

	//the cage gives way: the dais around it catches fire and the jets fan wider
	private void crack(){
		cracked = true;
		CoopLog.line("cathedral: the heart cracks at " + HP + "/" + HT + " t=" + (int) Actor.now());
		GLog.w(Messages.get(this, "cracks"));
		Sample.INSTANCE.play(Assets.Sounds.BLAST, 1f, 0.6f);
		if (sprite instanceof HeartSprite) ((HeartSprite) sprite).crack();
		if (Dungeon.level instanceof CathedralLevel) ((CathedralLevel) Dungeon.level).scorchDais(this);
	}

	// *** the stokers ***

	//a stoker threw itself into the fire
	public void feed( Stoker s ){
		if (!isAlive()) return;
		int heal = HT / 25;
		HP = Math.min(HT, HP + heal);
		stoked = 4;
		if (sprite != null){
			sprite.showStatus(CharSprite.NEGATIVE, "+" + heal);
			if (sprite instanceof HeartSprite) ((HeartSprite) sprite).fed();
		}
		if (Dungeon.level.heroFOV[s.pos]) GLog.w(Messages.get(this, "fed"));
		CoopLog.line("cathedral: a stoker feeds the heart, " + HP + "/" + HT);
	}

	private int stokers(){
		int n = 0;
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof Stoker && m.isAlive()) n++;
		}
		return n;
	}

	//two more crawl out of the free grates, the ones nearest to it first
	private boolean bellows(){
		if (!(Dungeon.level instanceof CathedralLevel)) return false;
		ArrayList<Integer> free = new ArrayList<>();
		for (int g : ((CathedralLevel) Dungeon.level).grates()){
			if (Actor.findChar(g) == null && !covers(g)) free.add(g);
		}
		if (free.isEmpty()) return false;
		int n = Math.min(free.size(), cracked ? 3 : 2);
		boolean seen = false;
		for (int i = 0; i < n; i++){
			int g = free.remove(Random.Int(free.size()));
			Stoker s = new Stoker();
			s.summoned();
			s.pos = g;
			s.state = s.HUNTING;
			GameScene.add(s);
			if (Dungeon.level.heroFOV[g]){
				seen = true;
				CellEmitter.get(g).burst(FlameParticle.FACTORY, 6);
			}
		}
		if (seen) GLog.w(Messages.get(this, "bellows"));
		CoopLog.line("cathedral: the grates let out " + n + " stokers");
		return true;
	}

	// *** fighting ***

	private int hot( int dmg ){
		return Math.round(Cathedral.fire(dmg) * (stoked > 0 ? 1.3f : 1f));
	}

	@Override
	public int damageRoll() {
		return hot(super.damageRoll());
	}

	@Override
	protected boolean special( Char enemy ) {
		if (bellows <= 0 && stokers() < (cracked ? 4 : 3)){
			bellows = cracked ? 7 : 10;
			if (bellows()){
				spend(TICK);
				return true;
			}
		}
		int r = cracked ? 3 : 2;
		ArrayList<Integer> round = ring(r);
		if (!SKCombat.foesIn(this, round).isEmpty() && Random.Int(3) != 0){
			warn(PULSE, round, enemy.pos, 2f);
			return true;
		}
		if (reach(enemy.pos) >= 2){
			ArrayList<Integer> lines = jets(enemy.pos);
			if (lines.contains(enemy.pos)){
				warn(JETS, lines, enemy.pos, 2f);
				Senses.noise(pos, Senses.ROAR);
				return true;
			}
		}
		return false;
	}

	//straight lines of fire from the side of it that faces the target: one at the target, the others
	//fanned out round it
	private ArrayList<Integer> jets( int aim ){
		Level l = Dungeon.level;
		int w = l.width();
		int from = edgeToward(aim);
		double ax = aim % w - from % w, ay = aim / w - from / w;
		double base = Math.atan2(ay, ax);
		int n = cracked ? 5 : 3;
		LinkedHashSet<Integer> cells = new LinkedHashSet<>();
		for (int i = 0; i < n; i++){
			double a = base + (i - (n - 1) / 2.0) * 0.32;
			int tx = (int) Math.round(from % w + Math.cos(a) * JET_RANGE);
			int ty = (int) Math.round(from / w + Math.sin(a) * JET_RANGE);
			tx = Math.max(0, Math.min(w - 1, tx));
			ty = Math.max(0, Math.min(l.height() - 1, ty));
			Ballistica b = new Ballistica(from, tx + ty * w, Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID);
			for (int c : b.subPath(1, Math.min(b.dist, JET_RANGE))){
				if (l.solid[c]) break;
				if (!covers(c)) cells.add(c);
			}
		}
		return new ArrayList<>(cells);
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		strikeAnim(kind);
		boolean seen = false;
		for (int c : cells){
			if (Dungeon.level.heroFOV[c]){
				seen = true;
				CellEmitter.get(c).burst(FlameParticle.FACTORY, kind == JETS ? 5 : 3);
			}
		}
		if (seen) Sample.INSTANCE.play(kind == JETS ? Assets.Sounds.BURNING : Assets.Sounds.BLAST, 1f, 0.7f);
		for (Char ch : SKCombat.foesIn(this, cells)){
			if (kind == PULSE) blast(ch, hot(50), hot(80));
			else blast(ch, hot(70), hot(100));
			if (ch.isAlive() && !Char.hasProp(ch, Property.FIERY)) Buff.affect(ch, Burning.class).reignite(ch, 4f);
		}
	}

	// *** breaking ***

	@Override
	public void die( Object cause ) {
		CoopLog.line("cathedral: the heart is broken by " + (cause == null ? "?" : cause.getClass().getSimpleName()));
		super.die(cause);
		GLog.p(Messages.get(this, "broken"));
		Sample.INSTANCE.play(Assets.Sounds.BLAST, 1f, 0.5f);
		CathedralScreen.broken();
		Cathedral.heartBroken();
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("awake", awake);
		bundle.put("cracked", cracked);
		bundle.put("stoked", stoked);
		bundle.put("bellows", bellows);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		awake = bundle.getBoolean("awake");
		cracked = bundle.getBoolean("cracked");
		stoked = bundle.getInt("stoked");
		bellows = bundle.getInt("bellows");
		if (awake) BossHealthBar.assignBoss(this);
	}

	public boolean cracked(){
		return cracked;
	}

	//64x64, the 3x3 patch from (8, 16)
	public static class HeartSprite extends AshColossusSprite {

		public HeartSprite(){
			super();
			setup(CathedralAssets.HEART, 3, 64, 64, 8, 16);
			idle = loop(3, 0, 0, 1);
			run = idle.clone();
			attack = clip(10, 4, 5, 5);
			zap = attack.clone();
			die = clip(5, 8, 9, 10, 10);
			play(idle);
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (ch instanceof FurnaceHeart && ((FurnaceHeart) ch).cracked()) crack();
		}

		private boolean cracked = false;

		void crack(){
			cracked = true;
			look(loop(3, 2, 2, 3));
		}

		void fed(){
			once(clip(6, 7, 7, 7, cracked ? 2 : 0));
		}

		@Override
		public void windup( int kind ) {
			play(kind == JETS ? loop(6, 6, 4) : loop(4, 4, 1));
		}

		@Override
		public void strike( int kind ) {
			once(kind == JETS ? clip(10, 6, 6, 6, 5) : clip(10, 5, 5, 4));
		}

		@Override
		public void flash() {
			ra = ba = ga = 0.5f;
			flashTime = 0.05f;
		}
	}
}
