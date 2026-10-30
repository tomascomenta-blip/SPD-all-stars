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

package com.shatteredpixel.shatteredpixeldungeon.champions.jailer;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.watabou.noosa.particles.PixelParticle;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundle;

//R: the box. ghost walls around him, enemies that cross break them and get hurt + slowed
public class TheBox extends ChampionAbility {

	public static final TheBox INSTANCE = new TheBox();

	@Override public String slot() { return "R"; }
	@Override public int unlockLevel() { return 6; }
	@Override public int icon() { return HeroIcon.LOL_THE_BOX; }
	@Override public int color() { return 0x00FF77; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }

	@Override
	public float cooldown( Hero hero ) {
		return 60;
	}

	public static int duration( Hero hero ){
		return 10 + 3*hero.pointsInTalent(Talent.JAI_PRISON);
	}

	public static int wallDamage( Hero hero ){
		return Math.round((4 + hero.lvl) * (1f + 0.2f*hero.pointsInTalent(Talent.JAI_PRISON)));
	}

	public static int slowTurns( Hero hero ){
		int t = 3;
		if (hero.subClass == HeroSubClass.LANTERNKEEPER) t += hero.pointsInTalent(Talent.LK_BOX_LANTERN);
		return t;
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		SoulWall wall = (SoulWall) Dungeon.level.blobs.get(SoulWall.class);
		int dur = duration(hero);
		for (int i = 0; i < Dungeon.level.length(); i++){
			if (Dungeon.level.distance(hero.pos, i) != 2) continue;
			if (Dungeon.level.solid[i]) continue;
			wall = (SoulWall) Blob.seed(i, dur, SoulWall.class);
			GameScene.add(wall);
			if (Dungeon.level.heroFOV[i]) CellEmitter.get(i).burst(SoulParticle.FACTORY, 6);
		}
		if (wall != null){
			wall.damage = wallDamage(hero);
			wall.slow = slowTurns(hero);
		}
		Sample.INSTANCE.play(Assets.Sounds.CHARGEUP, 1f, 0.7f);
		ChampionHooks.operateVisual(hero, hero.pos);
		Invisibility.dispel(hero);
		startCooldown(hero);
		hero.spendAndNext(1f);
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", duration(hero), wallDamage(hero), slowTurns(hero));
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}

	//walls blob, per-cell timer, breaks when an enemy steps in
	public static class SoulWall extends Blob {

		public int damage = 5;
		public int slow = 3;

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
						ch.damage(damage, this);
						if (ch.isAlive()) Buff.prolong(ch, Slow.class, slow);
						if (l.heroFOV[cell]){
							CellEmitter.get(cell).burst(SoulParticle.FACTORY, 10);
							Sample.INSTANCE.play(Assets.Sounds.HIT_MAGIC, 1f, 0.8f);
						}
						off[cell] = 0;
					} else {
						off[cell] = cur[cell] - 1;
					}
					volume += off[cell];
				}
			}
		}

		@Override
		public void use( BlobEmitter emitter ) {
			super.use(emitter);
			emitter.pour(SoulParticle.FACTORY, 0.12f);
		}

		@Override
		public String tileDesc() {
			return Messages.get(this, "desc");
		}

		private static final String DAMAGE = "damage";
		private static final String SLOW = "slow";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(DAMAGE, damage);
			bundle.put(SLOW, slow);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			damage = bundle.getInt(DAMAGE);
			slow = bundle.getInt(SLOW);
		}
	}

	//green sparkles
	public static class SoulParticle extends PixelParticle.Shrinking {

		public static final Emitter.Factory FACTORY = new Emitter.Factory() {
			@Override
			public void emit( Emitter emitter, int index, float x, float y ) {
				((SoulParticle) emitter.recycle(SoulParticle.class)).reset(x, y);
			}

			@Override
			public boolean lightMode() {
				return true;
			}
		};

		public SoulParticle() {
			super();
			color(0x44FF99);
			lifespan = 0.8f;
			acc.set(0, -40);
		}

		public void reset( float x, float y ) {
			revive();
			this.x = x;
			this.y = y;
			left = lifespan;
			size = 3;
			speed.set(0);
		}

		@Override
		public void update() {
			super.update();
			float p = left / lifespan;
			am = p > 0.75f ? (1 - p) * 4 : 1;
		}
	}
}
