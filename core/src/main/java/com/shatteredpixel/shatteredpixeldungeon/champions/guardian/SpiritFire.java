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

package com.shatteredpixel.shatteredpixeldungeon.champions.guardian;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

//E: spirit fire patch. burns enemies each turn + shreds armor so his hits land harder
public class SpiritFire extends ChampionAbility {

	public static final SpiritFire INSTANCE = new SpiritFire();

	@Override public String slot() { return "E"; }
	@Override public int unlockLevel() { return 4; }
	@Override public int icon() { return HeroIcon.LOL_SPIRIT_FIRE; }
	@Override public int color() { return 0x9977FF; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }
	@Override public boolean targeted() { return true; }

	public static final int RANGE = 5;
	public static final int DURATION = 5;

	@Override
	public float cooldown( Hero hero ) {
		return 16;
	}

	public static int radius( Hero hero ){
		return hero.pointsInTalent(Talent.GUA_HOT_SPIRIT) >= 2 ? 2 : 1;
	}

	private static float multiplier( Hero hero ){
		return 1f + 0.2f*hero.pointsInTalent(Talent.GUA_HOT_SPIRIT);
	}

	public static int minDmg( Hero hero ){
		return Math.round((1 + hero.lvl/8) * multiplier(hero));
	}

	public static int maxDmg( Hero hero ){
		return Math.round((2 + hero.lvl/4) * multiplier(hero));
	}

	//bonus melee dmg vs scorched
	public static int shred( Hero hero ){
		return 1 + hero.lvl/6;
	}

	//curator: weakened turns after leaving
	public static int taxTurns( Hero hero ){
		if (hero.subClass != HeroSubClass.CURATOR || !hero.hasTalent(Talent.CU_SPIRIT_TAX)) return 0;
		return 2*hero.pointsInTalent(Talent.CU_SPIRIT_TAX) - 1;
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		if (target == null) return;
		if (Dungeon.level.distance(hero.pos, target) > RANGE || !Dungeon.level.heroFOV[target]){
			GLog.w(Messages.get(ChampionAbility.class, "too_far"));
			return;
		}
		if (Dungeon.level.solid[target]){
			GLog.w(Messages.get(this, "bad_cell"));
			return;
		}

		Flames old = (Flames) Dungeon.level.blobs.get(Flames.class);
		boolean wasBurning = old != null && old.volume > 0;
		Flames fire = null;
		int r = radius(hero);
		Level l = Dungeon.level;
		for (int i = 0; i < l.length(); i++){
			if (l.solid[i] || l.distance(target, i) > r) continue;
			int add = Math.max(0, DURATION - Blob.volumeAt(i, Flames.class));
			fire = Blob.seed(i, add, Flames.class);
			if (l.heroFOV[i]) CellEmitter.get(i).burst(SpiritParticle.FACTORY, 6);
		}
		if (fire != null){
			fire.setPower(minDmg(hero), maxDmg(hero), shred(hero), taxTurns(hero), wasBurning);
			GameScene.add(fire);
		}

		Sample.INSTANCE.play(Assets.Sounds.BURNING, 1f, 0.7f);
		//visual only, callback = idle, nothing else. dont add logic here
		if (hero.sprite != null) hero.sprite.zap(target, new Callback() {
			@Override
			public void call() {
				if (hero.sprite != null) hero.sprite.idle();
			}
		});
		Invisibility.dispel(hero);
		startCooldown(hero);
		hero.spendAndNext(1f);
	}

	@Override
	public String desc( Hero hero ) {
		int size = 2*radius(hero) + 1;
		String d = Messages.get(this, "desc", RANGE, size, size, DURATION, minDmg(hero), maxDmg(hero), shred(hero));
		if (taxTurns(hero) > 0) d += " " + Messages.get(this, "desc_tax", taxTurns(hero));
		return d;
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}

	//the fire blob. per cell timer, enemies only
	public static class Flames extends Blob {

		public int minDmg = 1;
		public int maxDmg = 2;
		public int shred = 1;
		public int tax = 0;

		//one blob per floor, so multiple casts keep the best values
		//TODO 2 guardians w/ different talents end up sharing the best of both. nobody's noticed yet
		public void setPower( int min, int max, int shred, int tax, boolean burning ){
			minDmg = burning ? Math.max(minDmg, min) : min;
			maxDmg = burning ? Math.max(maxDmg, max) : max;
			this.shred = burning ? Math.max(this.shred, shred) : shred;
			this.tax = burning ? Math.max(this.tax, tax) : tax;
		}

		@Override
		protected void evolve() {
			Level l = Dungeon.level;
			int cell;
			for (int i = area.left; i < area.right; i++){
				for (int j = area.top; j < area.bottom; j++){
					cell = i + j*l.width();
					if (cur[cell] <= 0){
						off[cell] = 0;
						continue;
					}
					Char ch = Actor.findChar(cell);
					if (ch != null && ch.alignment == Char.Alignment.ENEMY && ch.isAlive()){
						burn(ch);
					}
					off[cell] = cur[cell] - 1;
					volume += off[cell];
				}
			}
		}

		private void burn( Char ch ){
			ch.damage(Random.NormalIntRange(minDmg, Math.max(minDmg, maxDmg)), this);
			if (!ch.isAlive()) return;
			Scorched s = Buff.prolong(ch, Scorched.class, 2f);
			s.shred = Math.max(s.shred, shred);
			if (tax > 0) Buff.prolong(ch, Weakness.class, tax + 1);
		}

		@Override
		public void use( BlobEmitter emitter ) {
			super.use(emitter);
			emitter.pour(SpiritParticle.FACTORY, 0.08f);
		}

		@Override
		public String tileDesc() {
			return Messages.get(this, "desc");
		}

		private static final String MIN = "min";
		private static final String MAX = "max";
		private static final String SHRED = "shred";
		private static final String TAX = "tax";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(MIN, minDmg);
			bundle.put(MAX, maxDmg);
			bundle.put(SHRED, shred);
			bundle.put(TAX, tax);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			minDmg = bundle.getInt(MIN);
			maxDmg = bundle.getInt(MAX);
			shred = bundle.getInt(SHRED);
			tax = bundle.getInt(TAX);
		}
	}

	public static class Scorched extends FlavourBuff {

		{
			type = buffType.NEGATIVE;
			announced = true;
		}

		public int shred = 0;

		public static int shredOf( Char ch ){
			Scorched s = ch.buff(Scorched.class);
			return s == null ? 0 : s.shred;
		}

		@Override
		public int icon() {
			return BuffIndicator.FIRE;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(0.6f, 0.45f, 1f);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", shred, dispTurns());
		}

		private static final String SHRED = "shred";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(SHRED, shred);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			shred = bundle.getInt(SHRED);
		}
	}

	//purple flames
	public static class SpiritParticle extends PixelParticle.Shrinking {

		public static final Emitter.Factory FACTORY = new Emitter.Factory() {
			@Override
			public void emit( Emitter emitter, int index, float x, float y ) {
				((SpiritParticle) emitter.recycle(SpiritParticle.class)).reset(x, y);
			}

			@Override
			public boolean lightMode() {
				return true;
			}
		};

		public SpiritParticle() {
			super();
			color(0x8866FF);
			lifespan = 0.7f;
			acc.set(0, -70);
		}

		public void reset( float x, float y ) {
			revive();
			this.x = x;
			this.y = y;
			left = lifespan;
			size = 4;
			speed.set(0);
		}

		@Override
		public void update() {
			super.update();
			float p = left / lifespan;
			am = p > 0.8f ? (1 - p) * 5 : 1;
		}
	}
}
