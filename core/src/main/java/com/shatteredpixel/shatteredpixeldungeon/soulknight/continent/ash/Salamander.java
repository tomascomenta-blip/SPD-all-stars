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
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentMob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentMobSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//black lava salamander. under the lava you can't see it or touch it; it comes up to spit embers,
//stays up a moment and sinks again. out of the lava it's just a big angry lizard
public class Salamander extends ContinentMob {

	{
		spriteClass = SalamanderSprite.class;

		HP = HT = 160;
		defenseSkill = 30;
		EXP = 17;

		minDmg = 26; maxDmg = 36; acc = 55;
		armorMin = 2; armorMax = 10;

		properties.add(Property.FIERY);
	}

	private boolean under = false;
	//turns it stays up after spitting
	private int surfaced = 0;

	public boolean submerged(){
		return under;
	}

	private boolean inLava(){
		return Dungeon.level.water[pos];
	}

	private void sink(){
		if (under) return;
		under = true;
		invisible++;
		if (sprite instanceof SalamanderSprite) ((SalamanderSprite) sprite).under(true);
		if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.WATER, 0.6f, 0.6f);
	}

	private void rise(){
		if (!under) return;
		under = false;
		invisible = Math.max(0, invisible - 1);
		if (sprite instanceof SalamanderSprite) ((SalamanderSprite) sprite).under(false);
		if (Dungeon.level.heroFOV[pos]){
			CellEmitter.get(pos).burst(FlameParticle.FACTORY, 6);
			Sample.INSTANCE.play(Assets.Sounds.WATER, 0.8f, 0.8f);
		}
	}

	@Override
	protected boolean act() {
		if (!inLava()){
			rise();
		} else if (pendingKind == -1){
			Hero shore = null;
			for (Hero h : Party.living()){
				if (Dungeon.level.adjacent(pos, h.pos) && h.invisible <= 0){
					shore = h;
					break;
				}
			}
			if (under && shore != null){
				//someone at the shore: come up and bite
				rise();
				aggro(shore);
				surfaced = 1;
				spend(TICK);
				return true;
			}
			if (!under){
				if (shore != null) surfaced = Math.max(surfaced, 1);
				if (surfaced > 0) surfaced--;
				else sink();
			}
		}
		return super.act();
	}

	@Override
	public boolean isInvulnerable( Class effect ) {
		return under || super.isInvulnerable(effect);
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		return !under && super.canAttack(enemy);
	}

	//in the lava it keeps to the lava
	@Override
	protected boolean getCloser( int target ) {
		if (!inLava() || rooted) return super.getCloser(target);
		int best = -1;
		int bestD = Dungeon.level.distance(pos, target);
		for (int n : PathFinder.NEIGHBOURS8){
			int c = pos + n;
			if (!Dungeon.level.water[c] || !Dungeon.level.passable[c] || Actor.findChar(c) != null) continue;
			int d = Dungeon.level.distance(c, target);
			if (d < bestD){
				best = c;
				bestD = d;
			}
		}
		if (best == -1) return false;
		move(best);
		return true;
	}

	@Override
	protected int specialCooldown() {
		return 4;
	}

	@Override
	protected boolean special( Char enemy ) {
		if (!inLava()) return false;
		int d = Dungeon.level.distance(pos, enemy.pos);
		if (d < 2 || d > 6) return false;
		if (new Ballistica(pos, enemy.pos, Ballistica.PROJECTILE).collisionPos != enemy.pos) return false;
		rise();
		ArrayList<Integer> cells = new ArrayList<>();
		cells.add(enemy.pos);
		ArrayList<Integer> around = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS8){
			int c = enemy.pos + n;
			if (!Dungeon.level.solid[c] && !Dungeon.level.water[c]) around.add(c);
		}
		for (int i = 0; i < 2 && !around.isEmpty(); i++){
			cells.add(around.remove(Random.Int(around.size())));
		}
		announce(0, cells, enemy.pos);
		return true;
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		surfaced = 1;
		if (sprite != null && sprite.parent != null && Dungeon.level.heroFOV[pos]){
			for (int c : cells) MagicMissile.boltFromChar(sprite.parent, MagicMissile.FIRE, sprite, c, null);
			Sample.INSTANCE.play(Assets.Sounds.BURNING);
		}
		for (int c : cells){
			if (!Dungeon.level.water[c]) GameScene.add(Blob.seed(c, 2, Fire.class));
		}
		for (Char ch : SKCombat.foesIn(this, cells)){
			blast(ch, 24, 34);
			if (ch.isAlive()) Buff.affect(ch, Burning.class).reignite(ch, 4f);
		}
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("under", under);
		bundle.put("surfaced", surfaced);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		under = bundle.getBoolean("under");
		surfaced = bundle.getInt("surfaced");
		if (under) invisible++;
	}

	//12-13: just the back and the eyes over the lava
	public static class SalamanderSprite extends ContinentMobSprite {

		private Animation above, below;

		public SalamanderSprite(){
			super();
			setup(ContinentAssets.SALAMANDER, 20, 12);
			above = idle;
			below = loop(3, 12, 12, 13);
		}

		public void under( boolean u ){
			look(u ? below : above);
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (ch instanceof Salamander && ((Salamander) ch).submerged()) under(true);
		}
	}
}
