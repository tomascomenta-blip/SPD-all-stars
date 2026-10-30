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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.flesh.fauna;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.AshColossus;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.AshFauna;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.LinkedHashSet;

//root elk. an elk gone bad: mangy hide, raw flesh, ribs out on the flank and roots growing over its
//back. its antlers are roots too, and what hangs from them like fruit are eyes, shut most of the time.
//when it spots you it screams, every eye on the antlers opens, and the whole forest around knows
//where you are. then it lowers the antlers and charges down a marked lane, or gores what's in front
public class RootElk extends FleshColossus {

	{
		size = 10;
		spriteClass = ElkSprite.class;

		HP = HT = 3800;
		defenseSkill = 8;
		EXP = 150;
		baseSpeed = 0.8f;

		minDmg = 50; maxDmg = 68; acc = 62;
		armorMin = 16; armorMax = 32;
	}

	private static final int CHARGE = 0, GORE = 1;
	private static final int CHARGE_RANGE = 10, CALL = 30;

	private float screamed = -99;

	//the ribs showing on the flank
	@Override
	public int[][] weakSpots() {
		return new int[][]{{5, 9}, {6, 9}, {5, 5}};
	}

	//the roots over its back
	@Override
	public int[][] climbable() {
		return new int[][]{{0, 2, 7}, {1, 1, 8}, {2, 1, 8}, {3, 2, 7}};
	}

	//only if it never saw you coming: no light, and walked up to while it grazes
	@Override
	public boolean tameable() {
		return true;
	}

	@Override
	public String tameBlock( Hero h ) {
		if (state == HUNTING) return Messages.get(this, "seen");
		if (Senses.lit(h)) return Messages.get(this, "light");
		return null;
	}

	//its eyes are shut while it grazes
	@Override
	protected int eyes() {
		return state == HUNTING ? 6 : 1;
	}

	@Override
	protected int ears() {
		return 5;
	}

	@Override
	protected int specialCooldown() {
		return 4;
	}

	@Override
	public void notice() {
		super.notice();
		if (enemy != null && !tamed) scream();
	}

	@Override
	protected boolean act() {
		if (state == HUNTING && enemy != null && !tamed && Actor.now() - screamed > 25 && paralysed <= 0 && down == 0){
			scream();
		}
		return super.act();
	}

	//every beast of the forest within earshot comes to where it's looking
	private void scream(){
		if (Actor.now() - screamed < 10) return;
		screamed = Actor.now();
		Senses.noise(pos, Senses.STAMPEDE);
		if (sprite instanceof ElkSprite) ((ElkSprite) sprite).scream();
		if (seen()){
			Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 1f, 0.4f);
			GLog.w(Messages.get(this, "scream"));
		}
		int at = enemy != null ? enemy.pos : pos;
		for (Mob m : AshFauna.sorted(Dungeon.level.mobs)){
			if (m == this || !AshFauna.ours(m) || m.alignment != Alignment.ENEMY || m.state == m.HUNTING) continue;
			if (m instanceof AshColossus && (((AshColossus) m).peaceful() || ((AshColossus) m).tamed)) continue;
			if (Dungeon.level.distance(m.pos, pos) <= CALL) m.beckon(at);
		}
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = reach(enemy.pos);
		if (d <= 2){
			ArrayList<Integer> f = front(enemy.pos, 2, 1);
			if (caught(f, enemy)){
				warn(GORE, f, enemy.pos, 2f);
				return true;
			}
		}
		if (d >= 3 && d <= CHARGE_RANGE){
			int[] dir = toward(enemy.pos);
			ArrayList<Integer> p = path(dir[0], dir[1], CHARGE_RANGE);
			LinkedHashSet<Integer> lane = lane(p);
			if (p.size() >= 3 && caught(lane, enemy)){
				warn(CHARGE, lane, enemy.pos, 3f);
				return true;
			}
		}
		return false;
	}

	private boolean charging = false;

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		if (kind == GORE){
			strikeAnim(GORE);
			Senses.noise(aim, Senses.CRASH);
			if (seen()) Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG, 1f, 0.7f);
			for (Char ch : SKCombat.foesIn(this, cells)){
				blast(ch, 100, 130);
				knock(ch, 2);
			}
			return;
		}
		int[] dir = toward(aim);
		charging = true;
		int n = run(path(dir[0], dir[1], CHARGE_RANGE));
		charging = false;
		if (!isAlive()) return;
		Senses.noise(pos, Senses.CRASH);
		if (n > 0) strikeAnim(CHARGE);
	}

	@Override
	protected void onTrample( Char ch ) {
		if (!charging || !SKCombat.foes(this, ch)){
			super.onTrample(ch);
			return;
		}
		if (!firstRunOver(ch)) return;
		blast(ch, 110, 150);
		if (ch.isAlive()) Buff.prolong(ch, Paralysis.class, 1f);
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("screamed", screamed);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		screamed = bundle.contains("screamed") ? bundle.getFloat("screamed") : -99;
	}

	//6 antlers down, 7 ramming, 8 the scream with every eye open
	public static class ElkSprite extends Sheet {

		public ElkSprite(){
			super();
			layout(FleshAssets.ELK, 10, 248, 280, 32, 102, 9, 10);
		}

		@Override
		protected PointF standingSpot() {
			return new PointF(118, 186);
		}

		@Override
		protected String tamedSheet() {
			return FleshAssets.ELK_TAMED;
		}

		@Override
		protected PointF lyingSpot() {
			return new PointF(116, 218);
		}

		public void scream(){
			once(clip(4, 8, 8, 8, 8, 0));
		}

		@Override
		public void windup( int kind ) {
			play(loop(1, 6));
		}

		@Override
		public void strike( int kind ) {
			once(clip(8, 7, 7, 7, 0));
		}
	}
}
