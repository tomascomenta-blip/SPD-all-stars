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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.forgotten;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SnowParticle;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.props.SKProps;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.quests.YetiQuest;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

/*
 per-turn forgotten land stuff, saved with the floor
 thin ice cracks on step, 2nd step = icy water. tar = stuck for a bit
 cold: standing still / wading in a cold room stacks frost (chill, then frozen) unless near a brazier.
 blizzard sometimes covers the cold rooms, less sight + more cold
 icicles on marked cells near heroes (more after explosions), and the tribe flame dying in the cold
*/
public class ForgottenFloor extends Blob {

	public static final int THIN_ICE = 1, CRACKED = 2, TAR = 3;

	{
		actPriority = BUFF_PRIO;
	}

	public int blizzardTimer = 60;
	public int blizzardLeft = 0;
	public int icicleTimer = 20;
	public int noise = 0;
	public int version = 0;

	private final ArrayList<int[]> pendingIcicles = new ArrayList<>(); //{cell, turn}
	private int turn = 0;

	public static ForgottenFloor get( Level level ){
		return level == null ? null : (ForgottenFloor) level.blobs.get(ForgottenFloor.class);
	}

	public static ForgottenFloor getOrMake( Level level ){
		ForgottenFloor f = get(level);
		if (f == null){
			f = new ForgottenFloor();
			f.seed(level, 0, 0);
			level.blobs.put(ForgottenFloor.class, f);
		}
		return f;
	}

	public void put( Level level, int cell, int kind ){
		if (cur == null) cur = new int[level.length()];
		if (off == null) off = new int[level.length()];
		cur[cell] = kind;
		volume = 0;
		for (int v : cur) if (v > 0) volume++;
		version++;
	}

	public int kindAt( int cell ){
		return cur == null || cell < 0 || cell >= cur.length ? 0 : cur[cell];
	}

	public boolean blizzard(){
		return blizzardLeft > 0;
	}

	private static ForgottenLevel level(){
		return Dungeon.level instanceof ForgottenLevel ? (ForgottenLevel) Dungeon.level : null;
	}

	//thin ice + tar

	public static void stepped( ForgottenLevel L, Char ch ){
		ForgottenFloor f = get(L);
		if (f == null || ch == null || ch.flying || !ch.isAlive()) return;
		int c = ch.pos;
		switch (f.kindAt(c)){
			case THIN_ICE:
				f.put(L, c, CRACKED);
				if (L.heroFOV[c]) Sample.INSTANCE.play(Assets.Sounds.SHATTER, 0.5f, 1.4f);
				if (ch instanceof Hero) GLog.w(Messages.get(ForgottenFloor.class, "crack"));
				break;
			case CRACKED:
				f.put(L, c, 0);
				L.set(c, Terrain.WATER);
				GameScene.updateMap(c);
				if (L.heroFOV[c]) Sample.INSTANCE.play(Assets.Sounds.SHATTER, 1f, 0.8f);
				ForgottenVisuals.splash(c);
				Buff.prolong(ch, Chill.class, 6f);
				if (ch instanceof Hero){
					GLog.n(Party.active() ? Messages.get(ForgottenFloor.class, "break", Party.name((Hero) ch)) : Messages.get(ForgottenFloor.class, "break_solo"));
					ForgottenCold cold = Buff.affect(ch, ForgottenCold.class);
					cold.add(6);
				}
				break;
			case TAR:
				if (!(ch instanceof SKProps.SKProp)){
					Buff.prolong(ch, Roots.class, 2f);
					Buff.prolong(ch, Slow.class, 3f);
					if (ch instanceof Hero) GLog.w(Messages.get(ForgottenFloor.class, "tar"));
				}
				break;
		}
	}

	//boom -> icicles
	public static void noise( int cell ){
		ForgottenLevel L = level();
		ForgottenFloor f = get(L);
		if (f == null || !L.cold(cell)) return;
		f.noise += 3;
		f.icicleTimer = Math.min(f.icicleTimer, 1);
	}

	public static boolean nearBrazier( Level L, int cell, int dist ){
		for (Mob m : L.mobs){
			if ((m instanceof ForgottenFeatures.Brazier && ((ForgottenFeatures.Brazier) m).lit)
					|| m instanceof Tribe.SacredFire){
				if (L.distance(m.pos, cell) <= dist) return true;
			}
		}
		return false;
	}

	@Override
	public boolean act() {
		spend(TICK);
		ForgottenLevel L = level();
		if (L == null || L.blobs.get(ForgottenFloor.class) != this) return true;
		turn++;

		//tar bubbles are only visual, nada que hacer. icicles due now
		ArrayList<int[]> due = new ArrayList<>();
		for (int[] p : pendingIcicles) if (p[1] <= turn) due.add(p);
		pendingIcicles.removeAll(due);
		for (int[] p : due) fall(L, p[0]);

		boolean coldFloor = Dungeon.depth <= 23;
		if (coldFloor && !(L instanceof ForgottenBossLevel)){
			if (blizzardLeft > 0){
				if (--blizzardLeft == 0){
					GLog.i(Messages.get(ForgottenFloor.class, "blizzard_end"));
					version++;
				}
			} else if (--blizzardTimer <= 0){
				blizzardTimer = 70 + Random.Int(40);
				blizzardLeft = 10;
				version++;
				GLog.n(Messages.get(ForgottenFloor.class, "blizzard"));
				Sample.INSTANCE.play(Assets.Sounds.GAS, 1f, 0.6f);
			}
			if (--icicleTimer <= 0){
				icicleTimer = 18 + Random.Int(12);
				icicles(L, 1 + noise / 3 + (Party.living().size() >= 3 ? 1 : 0));
				noise = 0;
			}
		}

		for (Hero h : Party.living()){
			coldTurn(L, h);
			Tribe.flameTurn(L, h, this);
			if (blizzardLeft > 0 && L.cold(h.pos)){
				Buff.prolong(h, Whiteout.class, 2f);
			}
		}
		return true;
	}

	private void coldTurn( ForgottenLevel L, Hero h ){
		//-Dspd.test.god=1, bots don't freeze to death on the way down
		if (DevFlags.god()) return;
		ForgottenCold cold = h.buff(ForgottenCold.class);
		//tribe furs / yeti king mantle = immune
		if (h.belongings.armor() instanceof Tribe.HideArmor || YetiQuest.mantled(h)){
			if (cold != null) cold.detach();
			return;
		}
		boolean warm = nearBrazier(L, h.pos, 2);
		boolean isCold = L.cold(h.pos) && !warm;
		if (!isCold){
			if (cold != null){
				cold.add(warm ? -100 : -2);
			}
			return;
		}
		if (cold == null) cold = Buff.affect(h, ForgottenCold.class);
		boolean still = h.pos == cold.lastPos;
		boolean wet = L.water[h.pos];
		int add = (wet ? 3 : still ? 1 : 0) + (blizzardLeft > 0 ? 1 : 0);
		if (add == 0) add = -2;
		cold.lastPos = h.pos;
		cold.add(add);
	}

	private void icicles( ForgottenLevel L, int n ){
		ArrayList<Integer> cells = new ArrayList<>();
		ArrayList<Hero> living = Party.living();
		for (int i = 0; i < n && !living.isEmpty(); i++){
			Hero h = Random.element(living);
			if (!L.cold(h.pos)) continue;
			for (int tries = 0; tries < 10; tries++){
				int c = h.pos + Random.IntRange(-2, 2) + Random.IntRange(-2, 2) * L.width();
				if (c >= 0 && c < L.length() && L.passable[c] && !L.solid[c] && !cells.contains(c)){
					cells.add(c);
					break;
				}
			}
		}
		if (cells.isEmpty()) return;
		float delay = SKCombat.announceDelay(TICK);
		SKCombat.mark(cells, delay);
		for (int c : cells) pendingIcicles.add(new int[]{c, turn + (int) Math.ceil(delay)});
		GLog.w(Messages.get(ForgottenFloor.class, "icicles"));
	}

	private void fall( ForgottenLevel L, int cell ){
		ForgottenVisuals.icicle(cell);
		if (L.heroFOV[cell]) Sample.INSTANCE.play(Assets.Sounds.SHATTER, 0.8f, 1.2f);
		Char ch = Actor.findChar(cell);
		if (ch != null && ch.isAlive() && !(ch instanceof SKProps.SKProp)){
			int dmg = Random.NormalIntRange(10, 18) - ch.drRoll() / 2;
			ch.damage(Math.max(1, dmg), this);
			if (ch.isAlive()) Buff.prolong(ch, Slow.class, 2f);
		}
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("blizzard_timer", blizzardTimer);
		bundle.put("blizzard_left", blizzardLeft);
		bundle.put("icicle_timer", icicleTimer);
		bundle.put("noise", noise);
		bundle.put("turn", turn);
		int[] flat = new int[pendingIcicles.size() * 2];
		for (int i = 0; i < pendingIcicles.size(); i++){
			flat[2*i] = pendingIcicles.get(i)[0];
			flat[2*i+1] = pendingIcicles.get(i)[1];
		}
		bundle.put("icicles", flat);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		blizzardTimer = bundle.getInt("blizzard_timer");
		blizzardLeft = bundle.getInt("blizzard_left");
		icicleTimer = bundle.getInt("icicle_timer");
		noise = bundle.getInt("noise");
		turn = bundle.getInt("turn");
		pendingIcicles.clear();
		if (bundle.contains("icicles")){
			int[] flat = bundle.getIntArray("icicles");
			for (int i = 0; i + 1 < flat.length; i += 2) pendingIcicles.add(new int[]{flat[i], flat[i+1]});
		}
	}

	@Override
	public String tileDesc() {
		return null;
	}

	//cold buff

	public static class ForgottenCold extends Buff {

		{
			type = buffType.NEGATIVE;
			announced = false;
		}

		public int level = 0;
		public int lastPos = -1;

		public void add( int n ){
			level = Math.max(0, Math.min(30, level + n));
			if (level >= 24){
				//paralysis, NOT Frost. Frost shatters potions and everyone hated it
				Buff.prolong(target, Paralysis.class, 2f);
				if (target.sprite != null) target.sprite.emitter().burst(SnowParticle.FACTORY, 8);
				if (target instanceof Hero) GLog.n(Party.active() ? Messages.get(ForgottenCold.class, "frozen", Party.name((Hero) target)) : Messages.get(ForgottenCold.class, "frozen_solo"));
				level = 12;
			} else if (level >= 12){
				Buff.prolong(target, Chill.class, 3f);
			}
			if (level <= 0) detach();
		}

		@Override
		public boolean act() {
			spend(TICK);
			return true;
		}

		@Override
		public int icon() {
			return BuffIndicator.FROST;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(0.6f, 0.85f, 1f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, 1f - level / 24f);
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString(level);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", level);
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("level", level);
			bundle.put("last", lastPos);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			level = bundle.getInt("level");
			lastPos = bundle.getInt("last");
		}
	}

	//blizzard, low sight
	public static class Whiteout extends FlavourBuff {

		{
			type = buffType.NEGATIVE;
			announced = false;
		}

		@Override
		public boolean attachTo( Char target ) {
			if (super.attachTo(target)){
				target.viewDistance = Math.min(target.viewDistance, 2);
				return true;
			}
			return false;
		}

		@Override
		public void detach() {
			super.detach();
			if (Dungeon.level != null) target.viewDistance = Dungeon.level.viewDistance;
			if (target == Dungeon.hero) Dungeon.observe();
		}

		@Override
		public int icon() {
			return BuffIndicator.BLINDNESS;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(0xD8F0FF);
		}
	}
}
