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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.yog;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentMob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Yog;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//a burning fist, torn off whatever it used to belong to. hits like a truck and sets you on fire,
//and every few turns it throws a fireball (cross shaped, so one step sideways is enough)
public class BurningFist extends ContinentMob implements Yog {

	{
		spriteClass = FistSprite.class;

		HP = HT = 420;
		defenseSkill = 26;
		EXP = 28;
		flying = true;

		minDmg = 30; maxDmg = 40; acc = 60;
		armorMin = 4; armorMax = 14;

		loot = Generator.Category.POTION;
		lootChance = 0.25f;

		properties.add(Property.DEMONIC);
		properties.add(Property.FIERY);
		properties.add(Property.MINIBOSS);
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		if (Random.Int(2) == 0) Buff.affect(enemy, Burning.class).reignite(enemy, 4f);
		return super.attackProc(enemy, damage);
	}

	@Override
	protected int specialCooldown() {
		return 5;
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = Dungeon.level.distance(pos, enemy.pos);
		if (d < 2 || d > 6) return false;
		if (new Ballistica(pos, enemy.pos, Ballistica.PROJECTILE).collisionPos != enemy.pos) return false;
		ArrayList<Integer> cells = new ArrayList<>();
		cells.add(enemy.pos);
		for (int n : PathFinder.NEIGHBOURS4){
			int c = enemy.pos + n;
			if (!Dungeon.level.solid[c]) cells.add(c);
		}
		announce(0, cells, enemy.pos);
		return true;
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		if (sprite != null && sprite.parent != null && Dungeon.level.heroFOV[pos]){
			MagicMissile.boltFromChar(sprite.parent, MagicMissile.FIRE, sprite, aim, null);
			Sample.INSTANCE.play(Assets.Sounds.BLAST);
		}
		for (int c : cells){
			if (!Dungeon.level.water[c]) GameScene.add(Blob.seed(c, 3, Fire.class));
			if (Dungeon.level.heroFOV[c]) CellEmitter.center(c).burst(FlameParticle.FACTORY, 8);
		}
		for (Char ch : SKCombat.foesIn(this, cells)){
			blast(ch, 30, 42);
			if (ch.isAlive()) Buff.affect(ch, Burning.class).reignite(ch, 5f);
		}
	}

	public static class FistSprite extends ContinentMobSprite {

		private Emitter flames;

		public FistSprite(){
			super();
			setup(ContinentAssets.FIST_BURNING, 20, 20);
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (flames == null){
				flames = emitter();
				flames.pour(FlameParticle.FACTORY, 0.08f);
			}
		}

		@Override
		public void update() {
			super.update();
			if (flames != null) flames.visible = visible;
		}

		@Override
		public void die() {
			super.die();
			if (flames != null) flames.on = false;
		}

		@Override
		public void kill() {
			super.kill();
			if (flames != null) flames.killAndErase();
		}
	}
}
