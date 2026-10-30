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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentMob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

//obsidian colossus. slow and armored like a wall; the shell cracks at 3/4, 1/2 and 1/4 of its health,
//and when it breaks the lava inside spills out around it and the thing gets fast
public class ObsidianColossus extends ContinentMob {

	{
		spriteClass = ColossusSprite.class;

		HP = HT = 450;
		defenseSkill = 16;
		EXP = 30;
		baseSpeed = 0.6f;

		minDmg = 32; maxDmg = 40; acc = 56;
		armorMin = SHELL_MIN[0]; armorMax = SHELL_MAX[0];

		loot = Generator.Category.STONE;
		lootChance = 0.3f;

		properties.add(Property.INORGANIC);
		properties.add(Property.FIERY);
		properties.add(Property.MINIBOSS);
	}

	//armor of the shell, intact to broken
	private static final int[] SHELL_MIN = {10, 6, 3, 0};
	private static final int[] SHELL_MAX = {26, 18, 10, 4};

	private int cracks = 0;

	public boolean molten(){
		return cracks >= 3;
	}

	private void shell(){
		armorMin = SHELL_MIN[cracks];
		armorMax = SHELL_MAX[cracks];
	}

	@Override
	public float speed() {
		return molten() ? super.speed() * 2f : super.speed();
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		if (molten() && Random.Int(2) == 0) Buff.affect(enemy, Burning.class).reignite(enemy, 4f);
		return super.attackProc(enemy, damage);
	}

	@Override
	public void damage( int dmg, Object src ) {
		super.damage(dmg, src);
		while (isAlive() && cracks < 3 && HP <= HT * (3 - cracks) / 4){
			crack();
		}
	}

	private void crack(){
		cracks++;
		shell();
		if (sprite instanceof ColossusSprite) ((ColossusSprite) sprite).shell(cracks);
		boolean seen = Dungeon.level.heroFOV[pos];
		if (seen){
			CellEmitter.get(pos).burst(Speck.factory(Speck.ROCK), 6 + 2 * cracks);
			Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 1.2f - 0.1f * cracks);
		}
		if (!molten()){
			if (seen && sprite != null) sprite.showStatus(CharSprite.WARNING, Messages.get(this, "crack"));
			return;
		}
		//the shell gives: lava all around
		for (int c : SKCombat.area(pos, 1)){
			if (c == pos || Dungeon.level.water[c]) continue;
			GameScene.add(Blob.seed(c, 4, Fire.class));
			if (seen) CellEmitter.get(c).burst(FlameParticle.FACTORY, 5);
		}
		if (seen){
			PixelScene.shake(2, 0.5f);
			GLog.w(Messages.get(this, "molten"));
		}
	}

	//a two-handed slam on everything next to it, marked a turn before
	@Override
	protected int specialCooldown() {
		return 4;
	}

	@Override
	protected boolean special( Char enemy ) {
		if (!Dungeon.level.adjacent(pos, enemy.pos)) return false;
		announce(0, SKCombat.area(pos, 1), enemy.pos);
		return true;
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		if (Dungeon.level.heroFOV[pos]){
			Sample.INSTANCE.play(Assets.Sounds.HIT_CRUSH);
			PixelScene.shake(2, 0.3f);
		}
		for (Char ch : SKCombat.foesIn(this, cells)){
			blast(ch, 36, 50);
			if (ch.isAlive()) Buff.prolong(ch, Cripple.class, 3f);
			if (ch.isAlive() && molten()) Buff.affect(ch, Burning.class).reignite(ch, 4f);
		}
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("cracks", cracks);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		cracks = bundle.getInt("cracks");
		shell();
	}

	//12-13 the shell cracked once and twice, 14 broken open with the lava showing
	public static class ColossusSprite extends ContinentMobSprite {

		private Animation whole;

		public ColossusSprite(){
			super();
			setup(ContinentAssets.COLOSSUS, 24, 24);
			whole = idle;
		}

		public void shell( int cracks ){
			switch (cracks){
				case 0: look(whole); break;
				case 1: look(loop(1, 12)); break;
				case 2: look(loop(1, 13)); break;
				default: look(loop(1, 14)); break;
			}
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (ch instanceof ObsidianColossus) shell(((ObsidianColossus) ch).cracks);
		}
	}
}
