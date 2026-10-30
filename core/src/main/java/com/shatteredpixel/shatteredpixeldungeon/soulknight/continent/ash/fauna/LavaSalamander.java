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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.AshFauna;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;

//black lava salamander, grown to three cells. it swims under the lava where nobody sees it or can
//touch it, and comes up to bite whoever walks the shore, with no warning at all (an ambush, not a
//killing blow). then it stays up a moment, and that's the time to hit it
public class LavaSalamander extends AshColossus {

	{
		size = 3;
		spriteClass = SalamanderSprite.class;

		HP = HT = 420;
		defenseSkill = 18;
		EXP = 28;

		minDmg = 38; maxDmg = 50; acc = 60;
		armorMin = 6; armorMax = 16;

		properties.add(Property.FIERY);
	}

	private boolean under = false;
	private int surfaced = 0;

	public boolean submerged(){
		return under;
	}

	//enough of the body in the lava to hide it
	private boolean inLava(){
		return lavaAt(pos);
	}

	private boolean lavaAt( int center ){
		if (!AshFauna.lava(center)) return false;
		int w = Dungeon.level.width(), n = 0;
		for (int y = -1; y <= 1; y++){
			for (int x = -1; x <= 1; x++){
				int c = center + x + y * w;
				if (c >= 0 && c < Dungeon.level.length() && AshFauna.lava(c)) n++;
			}
		}
		return n >= 5;
	}

	//under the lava it keeps to the lava and waits for whoever comes to the shore
	@Override
	protected boolean getCloser( int target ) {
		if (!under) return super.getCloser(target);
		if (rooted || target == pos) return false;
		boolean[] f = fits();
		int best = -1, bestD = Dungeon.level.distance(pos, target);
		for (int n : PathFinder.NEIGHBOURS8){
			int c = pos + n;
			if (c < 0 || c >= f.length || !f[c] || !lavaAt(c)) continue;
			int d = Dungeon.level.distance(c, target);
			if (d < bestD){
				bestD = d;
				best = c;
			}
		}
		if (best == -1) return false;
		walkTo(best);
		return true;
	}

	@Override
	protected int ears() {
		return 2;
	}

	private void sink(){
		if (under) return;
		under = true;
		invisible++;
		if (sprite instanceof SalamanderSprite) ((SalamanderSprite) sprite).under(true);
		if (seen()) Sample.INSTANCE.play(Assets.Sounds.WATER, 0.6f, 0.5f);
	}

	private void rise(){
		if (!under) return;
		under = false;
		invisible = Math.max(0, invisible - 1);
		if (sprite instanceof SalamanderSprite) ((SalamanderSprite) sprite).under(false);
		if (seen()){
			for (int c : cells()) CellEmitter.get(c).burst(FlameParticle.FACTORY, 3);
			Sample.INSTANCE.play(Assets.Sounds.WATER, 0.9f, 0.6f);
		}
	}

	@Override
	protected boolean act() {
		if (!inLava()){
			rise();
		} else if (paralysed <= 0){
			Hero shore = null;
			for (Hero h : Party.living()){
				if (touches(h.pos) && h.invisible <= 0){
					shore = h;
					break;
				}
			}
			if (under && shore != null){
				//out of the lava and straight into a bite
				rise();
				aggro(shore);
				surfaced = 2;
				if (shore.sprite != null) shore.sprite.showStatus(CharSprite.WARNING, Messages.get(this, "ambush"));
				return doAttack(shore);
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
	protected boolean doAttack( Char enemy ) {
		if (sprite != null) sprite.turnTo(pos, enemy.pos);
		face(enemy.pos);
		return super.doAttack(enemy);
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc(enemy, damage);
		if (enemy.isAlive() && !Char.hasProp(enemy, Property.FIERY)) Buff.affect(enemy, Burning.class).reignite(enemy, 3f);
		return damage;
	}

	@Override
	public boolean isInvulnerable( Class effect ) {
		return under || super.isInvulnerable(effect);
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		return !under && super.canAttack(enemy);
	}

	//under the lava it doesn't trample the shore, it slides along
	@Override
	protected void onTrample( Char ch ) {
		if (!under) super.onTrample(ch);
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

	//12-13 just the eyes and the back over the lava, 14-15 coming out
	public static class SalamanderSprite extends AshColossusSprite {

		private Animation above, below;

		public SalamanderSprite(){
			super();
			setup(FaunaAssets.SALAMANDER, 3, 64, 64, 8, 16);
			above = idle;
			below = loop(3, 12, 12, 13);
			attack = clip(12, 14, 15, 6, 7, 8);
		}

		public void under( boolean u ){
			look(u ? below : above);
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (ch instanceof LavaSalamander && ((LavaSalamander) ch).submerged()) under(true);
		}
	}
}
