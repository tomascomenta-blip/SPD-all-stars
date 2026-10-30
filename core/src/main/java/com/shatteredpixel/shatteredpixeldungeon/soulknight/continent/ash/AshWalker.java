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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.Chasm;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentMob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

//walking ash. kill it and it falls into a heap and a cloud that blinds whoever's next to it.
//leave the heap alone for a few turns and it stands up again with half its health; step on it first
public class AshWalker extends ContinentMob {

	{
		spriteClass = WalkerSprite.class;

		HP = HT = 150;
		defenseSkill = 28;
		EXP = 15;

		minDmg = 26; maxDmg = 36; acc = 54;
		armorMin = 2; armorMax = 10;

		properties.add(Property.INORGANIC);
	}

	@Override
	public void die( Object cause ) {
		int cell = pos;
		boolean heap = cause != Chasm.class && !Dungeon.level.pit[cell] && !Dungeon.level.water[cell];
		super.die(cause);
		Cloud.puff(cell);
		if (heap && Actor.findChar(cell) == null){
			Heap h = new Heap();
			h.pos = cell;
			GameScene.add(h);
		}
	}

	//what's left of it. no fight in it, one hit (or a boot) scatters it
	public static class Heap extends Mob {

		{
			spriteClass = HeapSprite.class;

			HP = HT = 1;
			defenseSkill = 0;
			EXP = 0;
			lootChance = 0;

			properties.add(Property.INORGANIC);
			properties.add(Property.IMMOVABLE);

			state = PASSIVE;
		}

		public static final int REFORM = 5;
		private int turns = 0;

		@Override
		protected boolean act() {
			turns++;
			if (turns == REFORM - 1){
				if (sprite instanceof HeapSprite) ((HeapSprite) sprite).stir();
				if (Dungeon.level.heroFOV[pos]){
					if (sprite != null) sprite.showStatus(CharSprite.WARNING, Messages.get(this, "stir"));
					CellEmitter.get(pos).burst(Speck.factory(Speck.DUST), 4);
				}
			} else if (turns >= REFORM){
				reform();
				return true;
			}
			return super.act();
		}

		private void reform(){
			int cell = pos;
			destroy();
			if (sprite != null) sprite.killAndErase();
			AshWalker w = new AshWalker();
			w.summoned();
			w.pos = cell;
			w.HP = w.HT / 2;
			w.state = w.HUNTING;
			GameScene.add(w);
			if (Dungeon.level.heroFOV[cell]){
				CellEmitter.get(cell).burst(Speck.factory(Speck.DUST), 8);
				GLog.w(Messages.get(this, "reformed"));
			}
		}

		@Override
		protected boolean getCloser( int target ) {
			return false;
		}

		@Override
		protected boolean getFurther( int target ) {
			return false;
		}

		@Override
		public void die( Object cause ) {
			if (Dungeon.level.heroFOV[pos]){
				CellEmitter.get(pos).burst(Speck.factory(Speck.DUST), 10);
				Sample.INSTANCE.play(Assets.Sounds.PUFF, 0.8f, 1.2f);
			}
			super.die(cause);
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("turns", turns);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			turns = bundle.getInt("turns");
		}
	}

	//a short cloud of ash, blinds whoever is inside it
	public static class Cloud extends Blob {

		public static final float BLIND = 2f;

		public static void puff( int cell ){
			for (int c : SKCombat.openArea(cell, 1)){
				GameScene.add(Blob.seed(c, 2, Cloud.class));
				blind(Actor.findChar(c));
			}
			if (Dungeon.level.heroFOV[cell]) Sample.INSTANCE.play(Assets.Sounds.PUFF);
			Dungeon.observe();
		}

		private static void blind( Char ch ){
			if (ch == null || !ch.isAlive() || ch.alignment == Char.Alignment.ENEMY) return;
			Buff.prolong(ch, Blindness.class, BLIND);
		}

		@Override
		protected void evolve() {
			int w = Dungeon.level.width();
			boolean any = false;
			for (int i = area.left; i < area.right; i++){
				for (int j = area.top; j < area.bottom; j++){
					int cell = i + j*w;
					off[cell] = cur[cell] > 0 ? cur[cell] - 1 : 0;
					volume += off[cell];
					if (off[cell] > 0){
						Char ch = Actor.findChar(cell);
						if (ch != null && ch.alignment != Char.Alignment.ENEMY && ch.buff(Blindness.class) == null){
							blind(ch);
							any = true;
						}
					}
				}
			}
			if (any) Dungeon.observe();
		}

		@Override
		public void use( BlobEmitter emitter ) {
			super.use(emitter);
			emitter.pour(Speck.factory(Speck.SMOKE), 0.15f);
		}

		@Override
		public String tileDesc() {
			return Messages.get(this, "desc");
		}
	}

	public static class WalkerSprite extends ContinentMobSprite {
		public WalkerSprite(){
			super();
			setup(ContinentAssets.ASH_WALKER, 12, 16);
		}
	}

	//same sheet: 12 = the heap, 13 = getting up
	public static class HeapSprite extends ContinentMobSprite {

		public HeapSprite(){
			super();
			setup(ContinentAssets.ASH_WALKER, 12, 16);
			idle = loop(1, 12);
			die = new Animation(8, false);
			die.frames(frames, 12);
			play(idle);
		}

		public void stir(){
			look(loop(3, 12, 13, 13));
		}
	}
}
