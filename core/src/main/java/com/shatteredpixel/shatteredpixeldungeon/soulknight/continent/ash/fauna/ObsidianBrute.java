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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.AntiMagic;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//obsidian colossus, at its new size. slow, and it keeps to its own patch of ground. a fist raised
//over its head means a cell of ground about to be smashed (two turns to get out); both fists up, the
//whole ring around it. its glass skin throws magic back at whoever cast it
public class ObsidianBrute extends AshColossus {

	{
		size = 5;
		spriteClass = BruteSprite.class;

		HP = HT = 950;
		defenseSkill = 6;
		EXP = 55;
		baseSpeed = 0.5f;

		minDmg = 40; maxDmg = 55; acc = 60;
		armorMin = 16; armorMax = 32;

		properties.add(Property.INORGANIC);
		properties.add(Property.FIERY);

		territory = 10;
	}

	private static final int FIST = 0, BOTH = 1;

	//the lava core, open once the shell breaks
	@Override
	public int[][] weakSpots() {
		return new int[][]{{2, 1}};
	}

	@Override
	protected int specialCooldown() {
		return 3;
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = reach(enemy.pos);
		if (d > 3) return false;
		int close = SKCombat.foesIn(this, ring(2)).size();
		if (close >= 2 || close >= 1 && HP < HT / 2 && d <= 2){
			warn(BOTH, ring(2), enemy.pos, 3f);
			return true;
		}
		warn(FIST, square(enemy.pos, 1), enemy.pos, 2f);
		return true;
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		strikeAnim(kind);
		Senses.noise(aim, Senses.CRASH);
		if (seen()){
			Sample.INSTANCE.play(Assets.Sounds.HIT_CRUSH, 1f, kind == BOTH ? 0.6f : 0.8f);
			for (int c : cells) if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.ROCK), 2);
		}
		for (Char ch : SKCombat.foesIn(this, cells)){
			if (kind == BOTH){
				blast(ch, 150, 200);
				knock(ch, 2);
			} else {
				blast(ch, 130, 170);
			}
			if (ch.isAlive()) Buff.prolong(ch, Cripple.class, 3f);
		}
	}

	// *** the glass skin ***

	private static boolean magic( Object src ){
		return src instanceof Wand || AntiMagic.RESISTS.contains(src.getClass());
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (dmg > 0 && magic(src) && isAlive()){
			//it was the hero acting who cast it (same guess as Mob.damage)
			Hero caster = Dungeon.hero;
			if (sprite instanceof BruteSprite) ((BruteSprite) sprite).mirror();
			if (caster != null && caster.isAlive() && Party.isPartyHero(caster)){
				caster.damage(Math.max(1, dmg / 2), this);
				if (caster.sprite != null) caster.sprite.showStatus(CharSprite.WARNING, Messages.get(this, "reflected"));
			}
			if (state != HUNTING && caster != null) aggro(caster);
			return;
		}
		super.damage(dmg, src);
		if (sprite instanceof BruteSprite) ((BruteSprite) sprite).cracks(HP, HT);
	}

	//6 one fist up, 12 both, 13 mirror, 14-15 cracked, 16 open
	public static class BruteSprite extends AshColossusSprite {

		@Override
		protected int[] downFrames() {
			return new int[]{17, 18, 19, 20};
		}

		private int shown = 0;

		public BruteSprite(){
			super();
			setup(FaunaAssets.OBSIDIAN, 5, 112, 112, 16, 32);
		}

		@Override
		public void windup( int kind ) {
			play(loop(2, kind == BOTH ? 12 : 6));
		}

		public void mirror(){
			once(clip(6, 13, 13, 13));
		}

		public void cracks( int hp, int ht ){
			int stage = hp * 100 <= ht * 15 ? 3 : hp * 3 <= ht ? 2 : hp * 3 <= ht * 2 ? 1 : 0;
			if (stage == shown) return;
			shown = stage;
			switch (stage){
				case 1: look(loop(1, 14)); break;
				case 2: look(loop(1, 15)); break;
				case 3: look(loop(1, 16)); break;
			}
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			cracks(ch.HP, ch.HT);
		}
	}
}
