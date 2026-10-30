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

package com.shatteredpixel.shatteredpixeldungeon.champions.forger;

import com.shatteredpixel.shatteredpixeldungeon.coop.CoopRules;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

//R: fire ram spawns by the nearest visible enemy and charges at the forger, burn+slow on the way.
//R again while it's passing close = headbutt it back, stun + brittle on the whole line. timing is the fun part
public class GodsCall extends ChampionAbility {

	public static final GodsCall INSTANCE = new GodsCall();

	@Override public String slot() { return "R"; }
	@Override public int unlockLevel() { return 6; }
	@Override public int icon() { return HeroIcon.FORGER_RAM; }
	@Override public int color() { return 0xFF4411; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }

	public static final int DISTANCE = 8;
	public static final int NEAR = 3;

	@Override
	public float cooldown( Hero hero ) {
		return 60;
	}

	public static int minDmg( Hero hero ){
		return Math.round((4 + hero.lvl*0.7f) * horns(hero));
	}

	public static int maxDmg( Hero hero ){
		return Math.round((9 + hero.lvl*1.3f) * horns(hero));
	}

	private static float horns( Hero hero ){
		return 1f + 0.2f*hero.pointsInTalent(Talent.FOR_RAM_HORNS);
	}

	public static int slowTurns( Hero hero ){
		return 3 + hero.pointsInTalent(Talent.FOR_RAM_HORNS);
	}

	public static int stunTurns( Hero hero ){
		return 3;
	}

	//no cd while the ram is out, button = headbutt
	@Override
	public boolean onCooldown( Hero hero ) {
		return RamRun.of(hero) == null && super.onCooldown(hero);
	}

	@Override
	public boolean ready( Hero hero ) {
		RamRun ram = RamRun.of(hero);
		return ram == null || ram.near(hero);
	}

	@Override
	public String notReadyText( Hero hero ) {
		return Messages.get(this, "far");
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		RamRun ram = RamRun.of(hero);
		if (ram == null){
			summon(hero);
		} else {
			headbutt(hero, ram);
		}
	}

	private void summon( Hero hero ){
		//ties -> actor order
		Char enemy = null;
		for (Char ch : Actor.chars()){
			if (ch.alignment == Char.Alignment.ENEMY && ch.isAlive() && Dungeon.level.heroFOV[ch.pos]
					&& Dungeon.level.distance(hero.pos, ch.pos) <= DISTANCE){
				if (enemy == null || Dungeon.level.distance(hero.pos, ch.pos) < Dungeon.level.distance(hero.pos, enemy.pos)){
					enemy = ch;
				}
			}
		}
		if (enemy == null){
			GLog.w(Messages.get(this, "no_target"));
			return;
		}

		Ballistica line = new Ballistica(hero.pos, enemy.pos, Ballistica.WONT_STOP);
		int far = 0;
		for (int i = 1; i < line.path.size() && i <= DISTANCE; i++){
			if (Dungeon.level.solid[line.path.get(i)]) break;
			far = i;
		}
		if (far < NEAR + 1){
			GLog.w(Messages.get(this, "no_room"));
			return;
		}
		//mismas celdas, far end -> forger
		int[] path = new int[far + 1];
		for (int i = 0; i <= far; i++){
			path[i] = line.path.get(far - i);
		}

		RamRun ram = Buff.affect(hero, RamRun.class);
		ram.path = path;
		ram.idx = 0;
		ram.depth = Dungeon.depth;
		ram.minDmg = minDmg(hero);
		ram.maxDmg = maxDmg(hero);
		ram.slow = slowTurns(hero);
		ram.hit.clear();

		ForgerHooks.gesture(hero, path[0]);
		if (Dungeon.level.heroFOV[path[0]]){
			CellEmitter.get(path[0]).burst(FlameParticle.FACTORY, 14);
			CellEmitter.get(path[0]).burst(Speck.factory(Speck.FORGE), 8);
		}
		Sample.INSTANCE.play(Assets.Sounds.BURNING, 1f, 0.6f);
		Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 0.6f);
		GLog.w(Messages.get(this, "summoned"));

		Invisibility.dispel(hero);
		startCooldown(hero);
		hero.spendAndNext(1f);
	}

	private void headbutt( Hero hero, RamRun ram ){
		int ramCell = ram.currentCell();
		int dirCell = ramCell == hero.pos ? ram.path[0] : ramCell;
		Ballistica line = new Ballistica(hero.pos, dirCell, Ballistica.WONT_STOP);
		ArrayList<Integer> cells = new ArrayList<>();
		for (int i = 1; i < line.path.size() && i <= DISTANCE; i++){
			int c = line.path.get(i);
			if (Dungeon.level.solid[c]) break;
			cells.add(c);
		}

		if (!cells.isEmpty()){
			((MagicMissile) hero.sprite.parent.recycle(MagicMissile.class)).reset(
					MagicMissile.FIRE, hero.sprite, cells.get(cells.size() - 1), null);
		}
		ForgerHooks.gesture(hero, dirCell);
		Sample.INSTANCE.play(Assets.Sounds.HIT_CRUSH, 1f, 0.6f);
		Sample.INSTANCE.play(Assets.Sounds.BLAST, 1f, 0.9f);
		PixelScene.shake(2, 0.5f);

		for (int c : cells){
			if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(FlameParticle.FACTORY, 6);
			Char ch = Actor.findChar(c);
			if (ch != null && CoopRules.hits(hero, ch) && ch.isAlive()){
				int dmg = Math.round(Hero.heroDamageIntRange(ram.minDmg, ram.maxDmg) * 0.6f);
				ch.damage(dmg, this);
				if (ch.isAlive()){
					Buff.prolong(ch, Paralysis.class, stunTurns(hero));
					Brittle.apply(hero, ch);
				}
			}
		}
		GLog.p(Messages.get(this, "headbutt"));

		ram.detach();
		Invisibility.dispel(hero);
		hero.spendAndNext(1f);
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", DISTANCE, minDmg(hero), maxDmg(hero), slowTurns(hero), NEAR, stunTurns(hero));
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}

	//the ram is a buff on the forger moving 1 cell/turn. not a mob. long story
	public static class RamRun extends Buff {

		{
			type = buffType.POSITIVE;
			announced = false;
		}

		public int[] path = new int[0];
		public int idx = 0;
		public int depth = -1;
		public int minDmg = 1;
		public int maxDmg = 2;
		public int slow = 3;
		public ArrayList<Integer> hit = new ArrayList<>();

		public static RamRun of( Hero hero ){
			return hero.buff(RamRun.class);
		}

		public int currentCell(){
			if (path.length == 0) return target.pos;
			return path[Math.max(0, Math.min(idx - 1, path.length - 1))];
		}

		public boolean near( Hero hero ){
			return Dungeon.level.distance(currentCell(), hero.pos) <= NEAR;
		}

		@Override
		public boolean act() {
			if (depth != Dungeon.depth || !target.isAlive() || idx >= path.length){
				finish();
				return true;
			}
			int cell = path[idx];
			if (Dungeon.level.heroFOV[cell]){
				CellEmitter.get(cell).burst(FlameParticle.FACTORY, 8);
				if (idx % 2 == 0) CellEmitter.get(cell).burst(Speck.factory(Speck.ROCK), 3);
			}
			if (idx % 3 == 0) Sample.INSTANCE.play(Assets.Sounds.BURNING, 0.6f, 1.3f);
			for (Char ch : Actor.chars()){
				if (ch.alignment == Char.Alignment.ENEMY && ch.isAlive() && !hit.contains(ch.id())
						&& Dungeon.level.distance(cell, ch.pos) <= 1){
					hit.add(ch.id());
					ch.damage(Hero.heroDamageIntRange(minDmg, maxDmg), INSTANCE);
					if (ch.isAlive()) Buff.prolong(ch, Slow.class, slow);
				}
			}
			idx++;
			spend(TICK);
			return true;
		}

		private void finish(){
			if (path.length > 0 && Dungeon.depth == depth && Dungeon.level.heroFOV[currentCell()]){
				CellEmitter.get(currentCell()).burst(FlameParticle.FACTORY, 10);
			}
			detach();
		}

		@Override
		public int icon() {
			return BuffIndicator.FIRE;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(1f, 0.5f, 0.2f);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", Math.max(0, path.length - idx));
		}

		private static final String PATH = "path";
		private static final String IDX = "idx";
		private static final String DEPTH = "depth";
		private static final String MIN = "min";
		private static final String MAX = "max";
		private static final String SLOW = "slow";
		private static final String HIT = "hit";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(PATH, path);
			bundle.put(IDX, idx);
			bundle.put(DEPTH, depth);
			bundle.put(MIN, minDmg);
			bundle.put(MAX, maxDmg);
			bundle.put(SLOW, slow);
			int[] h = new int[hit.size()];
			for (int i = 0; i < h.length; i++) h[i] = hit.get(i);
			bundle.put(HIT, h);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			path = bundle.getIntArray(PATH);
			idx = bundle.getInt(IDX);
			depth = bundle.getInt(DEPTH);
			minDmg = bundle.getInt(MIN);
			maxDmg = bundle.getInt(MAX);
			slow = bundle.getInt(SLOW);
			hit = new ArrayList<>();
			if (bundle.contains(HIT)){
				for (int c : bundle.getIntArray(HIT)) hit.add(c);
			}
		}
	}
}
