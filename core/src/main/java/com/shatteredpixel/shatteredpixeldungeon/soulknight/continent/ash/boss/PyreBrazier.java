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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.boss;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKBulletField;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.props.SKProps;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundle;

//the four braziers of the plain. the pyre feeds on them when it burns low, so you knock them over.
//a knocked brazier smoulders and catches again after a while, every one of them catches when the fight
//changes phase, and a falling ember can light one too. at the very end (the heart) they stay down
public class PyreBrazier extends SKProps.SKProp {

	{
		spriteClass = BrazierSprite.class;
	}

	public boolean lit = true;
	//turns until it catches again by itself
	public int smoulder = 0;

	public PyreBrazier(){
		super();
		HP = HT = 60 + 25 * Math.max(1, Party.active() ? Party.size() : 1);
	}

	//0 = it won't catch again by itself
	public static int relightTime( int phase ){
		return phase >= 4 ? 0 : 30 - 2 * phase;
	}

	@Override
	protected boolean act() {
		spend(TICK);
		if (!lit && smoulder > 0 && Dungeon.level.locked){
			AshPyre p = AshBossLevel.pyre(Dungeon.level);
			if (p != null && p.fighting() && --smoulder <= 0) light(false);
		}
		return true;
	}

	//only the heroes' side knocks them over. the pyre's own fire and lava don't
	private static boolean fromHeroes( Object src ){
		Char ch = null;
		if (src instanceof Char) ch = (Char) src;
		else if (src instanceof SKBulletField.Shot) ch = ((SKBulletField.Shot) src).owner;
		if (ch != null) return ch.alignment == Alignment.ALLY;
		return !(src instanceof Buff || src instanceof Blob || src instanceof AshBossLevel.Lava);
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (!lit || dmg <= 0 || !fromHeroes(src)) return;
		HP -= dmg;
		if (sprite != null){
			sprite.flash();
			sprite.showStatus(CharSprite.WARNING, Integer.toString(dmg));
		}
		if (HP <= 0) knockOver(true);
	}

	@Override
	public boolean isInvulnerable( Class effect ) {
		return !lit;
	}

	@Override
	public void die( Object cause ) {
		//nothing gets rid of them, it only puts them out
		knockOver(true);
	}

	public void knockOver( boolean byHeroes ){
		if (!lit) return;
		lit = false;
		HP = HT;
		AshPyre p = AshBossLevel.pyre(Dungeon.level);
		smoulder = relightTime(p == null ? 1 : p.phase);
		if (Dungeon.level.heroFOV[pos]){
			Sample.INSTANCE.play(Assets.Sounds.HIT_CRUSH, 1f, 0.8f);
			CellEmitter.get(pos).burst(Speck.factory(Speck.STEAM), 6);
			CellEmitter.center(pos).burst(FlameParticle.FACTORY, 10);
		}
		if (byHeroes) GLog.p(Messages.get(this, "knocked"));
		CoopLog.line("pyre brazier " + pos + " knocked over" + (byHeroes ? "" : " (pyre)") + " lit=" + (Dungeon.level instanceof AshBossLevel ? ((AshBossLevel) Dungeon.level).litBraziers() : -1));
	}

	public void light( boolean quiet ){
		if (lit) return;
		lit = true;
		smoulder = 0;
		HP = HT;
		if (Dungeon.level.heroFOV[pos]){
			CellEmitter.get(pos).burst(FlameParticle.FACTORY, 12);
			if (!quiet) Sample.INSTANCE.play(Assets.Sounds.BURNING, 1f, 1.2f);
		}
		if (!quiet) GLog.w(Messages.get(this, "relit"));
	}

	//the pyre drank it
	public void drained(){
		if (!lit) return;
		lit = false;
		HP = HT;
		smoulder = 40;
		if (Dungeon.level.heroFOV[pos]) CellEmitter.get(pos).burst(Speck.factory(Speck.SMOKE), 8);
	}

	@Override
	public String name() {
		return lit ? super.name() : Messages.get(this, "name_knocked");
	}

	@Override
	public String description() {
		if (lit) return Messages.get(this, "desc", HP, HT);
		return smoulder > 0 ? Messages.get(this, "desc_knocked", smoulder) : Messages.get(this, "desc_out");
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("lit", lit);
		bundle.put("smoulder", smoulder);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		lit = bundle.getBoolean("lit");
		smoulder = bundle.getInt("smoulder");
	}

	public static class BrazierSprite extends MobSprite {

		private final Animation burning, fallen;
		private Emitter fire;

		public BrazierSprite(){
			super();
			texture(ContinentAssets.BRAZIER);
			TextureFilm film = new TextureFilm(texture, 16, 24);
			burning = new Animation(6, true);
			burning.frames(film, 0, 1);
			fallen = new Animation(1, false);
			fallen.frames(film, 2);
			idle = burning;
			run = burning.clone();
			attack = burning.clone();
			zap = burning.clone();
			die = fallen.clone();
			play(idle);
		}

		private boolean lit(){
			return !(ch instanceof PyreBrazier) || ((PyreBrazier) ch).lit;
		}

		@Override
		public void idle() {
			play(lit() ? burning : fallen);
		}

		@Override
		public void update() {
			super.update();
			if (ch == null) return;
			boolean on = lit();
			Animation want = on ? burning : fallen;
			if (curAnim != want) play(want);
			if (on && visible && fire == null && parent != null){
				fire = emitter();
				if (fire != null){
					fire.pos(this, 4, 4, 8, 6);
					fire.pour(FlameParticle.FACTORY, 0.09f);
				}
			} else if ((!on || !visible) && fire != null){
				fire.on = false;
				fire = null;
			}
		}

		@Override
		public void kill() {
			super.kill();
			if (fire != null) fire.on = false;
		}

		@Override
		public int blood() {
			return 0xFFFF8A20;
		}
	}
}
