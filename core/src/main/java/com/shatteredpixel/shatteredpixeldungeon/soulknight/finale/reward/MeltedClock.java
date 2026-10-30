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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.finale.reward;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfEnergy;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.BArray;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

//the heart of the colossus kept ticking out of time, and this is what dripped off it.
//only winds up while you fight. two tricks:
// - rewind: back to where you stood (and how healthy you were) a few turns ago
// - halt: the world waits while you act. unlike the hourglass you can keep swinging
public class MeltedClock extends Artifact {

	public static final String AC_USE    = "USE";
	public static final String AC_REWIND = "REWIND";
	public static final String AC_HALT   = "HALT";

	public static final int REWIND_COST = 100;
	public static final int HALT_COST   = 50;
	//how far back the rewind goes
	public static final int REWIND_TURNS = 5;
	//uses per level
	private static final int USES_PER_LEVEL = 3;

	{
		image = ItemSpriteSheet.MELTED_CLOCK;
		levelCap = 10;
		charge = 0;
		chargeCap = 100;
		defaultAction = AC_USE;
		unique = true;
		bones = false;
	}

	//where the owner stood, oldest first. one entry per turn, cleared on a new floor
	private int[] pastPos = new int[REWIND_TURNS + 1];
	private int[] pastHP = new int[REWIND_TURNS + 1];
	private int pastCount = 0;
	private int pastFloor = -1;
	private boolean pastLocked = false;

	public int windUp(){
		return charge;
	}

	public int haltTurns(){
		return 2 + level() / 4;
	}

	public void fill(){
		charge = chargeCap;
		partialCharge = 0;
		updateQuickslot();
	}

	@Override
	public int image() {
		return charge >= HALT_COST && !cursed ? ItemSpriteSheet.MELTED_CLOCK_LIT : ItemSpriteSheet.MELTED_CLOCK;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions(hero);
		if (isEquipped(hero) && !cursed && hero.buff(MagicImmune.class) == null) actions.add(AC_USE);
		return actions;
	}

	@Override
	public void execute( final Hero hero, String action ) {
		super.execute(hero, action);

		if (!action.equals(AC_USE) && !action.equals(AC_REWIND) && !action.equals(AC_HALT)) return;
		if (hero.buff(MagicImmune.class) != null) return;

		if (!isEquipped(hero)){
			GLog.i(Messages.get(Artifact.class, "need_to_equip"));
			return;
		}
		if (cursed){
			GLog.i(Messages.get(this, "cursed"));
			return;
		}
		if (action.equals(AC_REWIND)){
			rewind(hero);
			return;
		}
		if (action.equals(AC_HALT)){
			halt(hero);
			return;
		}
		if (charge < HALT_COST){
			GLog.i(Messages.get(this, "no_charge"));
			return;
		}
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				GameScene.show(new WndOptions(new ItemSprite(MeltedClock.this),
						Messages.titleCase(name()),
						Messages.get(MeltedClock.class, "prompt"),
						Messages.get(MeltedClock.class, "opt_rewind", REWIND_COST),
						Messages.get(MeltedClock.class, "opt_halt", HALT_COST, haltTurns())){
					@Override
					protected boolean enabled( int index ) {
						return charge >= (index == 0 ? REWIND_COST : HALT_COST);
					}

					@Override
					protected void onSelect( int index ) {
						if (index == 0)      rewind(hero);
						else if (index == 1) halt(hero);
					}
				});
			}
		});
	}

	private void used( Hero hero, int cost ){
		charge -= cost;
		partialCharge = 0;
		Talent.onArtifactUsed(hero);
		exp++;
		if (exp >= USES_PER_LEVEL && level() < levelCap){
			exp = 0;
			upgrade();
			GLog.p(Messages.get(this, "levelup"));
		}
		updateQuickslot();
	}

	private void rewind( Hero hero ){
		if (charge < REWIND_COST){
			GLog.i(Messages.get(this, "no_charge"));
			return;
		}
		used(hero, REWIND_COST);

		int cell = pastCount > 0 ? rewindCell(hero, pastPos[0]) : -1;
		int hp = pastCount > 0 ? pastHP[0] : hero.HP;
		CoopLog.line("clock rewind hp " + hero.HP + "->" + Math.max(hero.HP, Math.min(hp, hero.HT)) + " pos " + hero.pos + "->" + cell);
		clearPast();

		GameScene.flash(0x80B08A50);
		Sample.INSTANCE.play(Assets.Sounds.TELEPORT, 1f, 0.7f);
		if (cell != -1 && cell != hero.pos){
			ScrollOfTeleportation.appear(hero, cell);
			Dungeon.level.occupyCell(hero);
			Buff.detach(hero, Roots.class);
			Dungeon.observe();
			GameScene.updateFog();
		}
		if (hp > hero.HP){
			int heal = Math.min(hp, hero.HT) - hero.HP;
			hero.HP += heal;
			if (hero.sprite != null && heal > 0){
				hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
			}
		}
		GLog.p(Messages.get(this, "rewound"));
		hero.spendAndNext(Actor.TICK);
	}

	//the old cell, or the nearest free one, as long as you could still walk there from here
	private int rewindCell( Hero hero, int old ){
		if (old < 0 || old >= Dungeon.level.length()) return -1;
		boolean[] walkable = BArray.or(Dungeon.level.passable, Dungeon.level.avoid, null);
		PathFinder.buildDistanceMap(hero.pos, walkable);
		int best = -1;
		for (int i : PathFinder.NEIGHBOURS9){
			int c = old + i;
			if (c < 0 || c >= Dungeon.level.length() || !Dungeon.level.passable[c]) continue;
			if (PathFinder.distance[c] == Integer.MAX_VALUE) continue;
			Char ch = Actor.findChar(c);
			if (ch != null && ch != hero) continue;
			if (i == 0) return c;
			if (best == -1) best = c;
		}
		return best;
	}

	private void halt( Hero hero ){
		if (charge < HALT_COST){
			GLog.i(Messages.get(this, "no_charge"));
			return;
		}
		used(hero, HALT_COST);
		GameScene.flash(0x80B08A50);
		Sample.INSTANCE.play(Assets.Sounds.TELEPORT, 1f, 1.3f);
		GLog.p(Messages.get(this, "halted"));
		CoopLog.line("clock halt " + haltTurns() + " turns");
		//+1, since this action is the first thing the halt eats
		Buff.affect(hero, Halt.class).set(haltTurns() + 1);
		hero.spendAndNext(Actor.TICK);
	}

	private void clearPast(){
		pastCount = 0;
	}

	private void remember( Hero hero ){
		int floor = Dungeon.depth + 1000 * Dungeon.branch;
		boolean locked = Dungeon.level.locked;
		if (floor != pastFloor || locked != pastLocked){
			pastFloor = floor;
			pastLocked = locked;
			clearPast();
		}
		if (pastCount == pastPos.length){
			System.arraycopy(pastPos, 1, pastPos, 0, pastCount - 1);
			System.arraycopy(pastHP, 1, pastHP, 0, pastCount - 1);
			pastCount--;
		}
		pastPos[pastCount] = hero.pos;
		pastHP[pastCount] = hero.HP;
		pastCount++;
	}

	@Override
	protected ArtifactBuff passiveBuff() {
		return new Winding();
	}

	@Override
	public void charge( Hero target, float amount ) {
		if (charge < chargeCap && !cursed && target.buff(MagicImmune.class) == null){
			gain(2.5f * amount);
		}
	}

	private void gain( float amount ){
		boolean wasReady = charge >= HALT_COST;
		partialCharge += amount;
		while (partialCharge >= 1 && charge < chargeCap){
			partialCharge--;
			charge++;
		}
		if (charge >= chargeCap) partialCharge = 0;
		if (!wasReady && charge >= HALT_COST) GLog.p(Messages.get(MeltedClock.class, "ready"));
		updateQuickslot();
	}

	@Override
	public String desc() {
		String desc = Messages.get(this, "desc", REWIND_TURNS, REWIND_COST, HALT_COST, haltTurns());
		if (isEquipped(Dungeon.hero)){
			if (cursed){
				desc += "\n\n" + Messages.get(this, "desc_cursed");
			} else if (level() < levelCap){
				desc += "\n\n" + Messages.get(this, "desc_hint");
			}
		}
		return desc;
	}

	private static final String PAST_POS    = "past_pos";
	private static final String PAST_HP     = "past_hp";
	private static final String PAST_FLOOR  = "past_floor";
	private static final String PAST_LOCKED = "past_locked";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		int[] pos = new int[pastCount];
		int[] hp = new int[pastCount];
		System.arraycopy(pastPos, 0, pos, 0, pastCount);
		System.arraycopy(pastHP, 0, hp, 0, pastCount);
		bundle.put(PAST_POS, pos);
		bundle.put(PAST_HP, hp);
		bundle.put(PAST_FLOOR, pastFloor);
		bundle.put(PAST_LOCKED, pastLocked);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		pastCount = 0;
		if (bundle.contains(PAST_POS)){
			int[] pos = bundle.getIntArray(PAST_POS);
			int[] hp = bundle.getIntArray(PAST_HP);
			pastCount = Math.min(Math.min(pos.length, hp.length), pastPos.length);
			System.arraycopy(pos, 0, pastPos, 0, pastCount);
			System.arraycopy(hp, 0, pastHP, 0, pastCount);
			pastFloor = bundle.getInt(PAST_FLOOR);
			pastLocked = bundle.getBoolean(PAST_LOCKED);
		}
	}

	//keeps the record of the last turns, and winds up while there's a fight in sight
	public class Winding extends ArtifactBuff {
		@Override
		public boolean act() {
			if (target instanceof Hero && target.isAlive()){
				Hero hero = (Hero) target;
				remember(hero);
				if (charge < chargeCap && !cursed && hero.buff(MagicImmune.class) == null
						&& hero.visibleEnemies() > 0){
					float rate = 1.5f + 0.15f * level();
					rate *= RingOfEnergy.artifactChargeMultiplier(hero);
					rate *= SpecPowers.clockRate(hero);
					gain(rate);
				}
			}
			spend(TICK);
			return true;
		}
	}

	//stopped time: the owner keeps acting while nobody else gets a turn
	public static class Halt extends Buff {

		{
			type = buffType.POSITIVE;
			announced = true;
		}

		private float left;

		public void set( float turns ){
			left = Math.max(left, turns);
		}

		public void processTime( float time ){
			left -= time;
			//1/1000 for rounding, as in the hourglass
			if (left < 0.001f){
				detach();
			}
		}

		@Override
		public boolean act() {
			//never ticks on its own, only the owner's actions use it up
			diactivate();
			return true;
		}

		@Override
		public void fx( boolean on ) {
			if (!(target instanceof Hero)) return;
			Emitter.freezeEmitters = on;
			for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
				if (mob.sprite == null) continue;
				if (on) mob.sprite.add(CharSprite.State.PARALYSED);
				else if (mob.paralysed <= 0) mob.sprite.remove(CharSprite.State.PARALYSED);
			}
		}

		@Override
		public int icon() {
			return BuffIndicator.TIME;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(0.9f, 0.6f, 0.25f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (4f - left) / 4f);
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString((int)(left + 0.001f));
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", dispTurns(Math.max(0, left)));
		}

		private static final String LEFT = "left";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(LEFT, left);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			left = bundle.getFloat(LEFT);
		}
	}
}
