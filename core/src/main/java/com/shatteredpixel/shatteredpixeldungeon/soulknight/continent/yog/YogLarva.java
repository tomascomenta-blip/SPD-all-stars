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
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentMob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Yog;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

//yog larvae. weak alone, they come in fours, and three that stay huddled long enough
//melt into a lesser eye. kill them before they get cozy
public class YogLarva extends ContinentMob implements Yog {

	{
		spriteClass = LarvaSprite.class;

		HP = HT = 45;
		defenseSkill = 24;
		EXP = 5;

		minDmg = 14; maxDmg = 20; acc = 50;
		armorMin = 0; armorMax = 4;

		properties.add(Property.DEMONIC);
	}

	public static final int BROOD = 4;
	//turns three of them have to stay together
	private static final int FUSE = 5;

	//the first one of a brood brings the other three on its first turn
	private boolean brood = true;
	private int huddle = 0;

	@Override
	public int taintOnHit() {
		return 1;
	}

	@Override
	protected boolean act() {
		hatch();
		if (isAlive() && huddled()){
			return true;
		}
		return super.act();
	}

	//also callable right after adding one, if it could die before its first turn
	public void hatch(){
		if (!brood) return;
		brood = false;
		ArrayList<Integer> cells = freeAround(pos);
		for (int i = 1; i < BROOD && !cells.isEmpty(); i++){
			int c = cells.remove(Random.Int(cells.size()));
			YogLarva l = new YogLarva();
			l.brood = false;
			if (EXP == 0) l.summoned();
			l.pos = c;
			l.state = state == SLEEPING ? l.SLEEPING : state == HUNTING ? l.HUNTING : l.WANDERING;
			GameScene.add(l);
		}
	}

	//true if this turn went into melting
	private boolean huddled(){
		ArrayList<YogLarva> mates = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS8){
			Char ch = Actor.findChar(pos + n);
			if (ch instanceof YogLarva && ch.isAlive() && ch.alignment == alignment) mates.add((YogLarva) ch);
		}
		//only once they're after someone: a sleeping brood would melt before anybody got there
		if (mates.size() < 2 || state != HUNTING){
			huddle = 0;
			return false;
		}
		huddle++;
		if (huddle == FUSE - 1){
			if (sprite != null && Dungeon.level.heroFOV[pos]){
				sprite.showStatus(CharSprite.WARNING, Messages.get(this, "pulse"));
				CellEmitter.get(pos).burst(ShadowParticle.UP, 6);
			}
			return false;
		}
		if (huddle < FUSE) return false;

		Collections.sort(mates, new Comparator<YogLarva>() {
			@Override
			public int compare( YogLarva a, YogLarva b ) {
				return Integer.compare(a.id(), b.id());
			}
		});
		int hp = HP;
		boolean summoned = EXP == 0;
		for (int i = 0; i < 2; i++){
			YogLarva m = mates.get(i);
			hp += m.HP;
			if (m.EXP == 0) summoned = true;
			melt(m);
		}
		int cell = pos;
		melt(this);

		YogEye eye = new YogEye();
		if (summoned) eye.summoned();
		eye.pos = cell;
		eye.state = eye.HUNTING;
		GameScene.add(eye);
		eye.HP = Math.min(eye.HT, hp);
		if (Dungeon.level.heroFOV[cell]){
			CellEmitter.center(cell).burst(ShadowParticle.CURSE, 12);
			Sample.INSTANCE.play(Assets.Sounds.CURSED);
			GLog.w(Messages.get(this, "fused"));
		}
		return true;
	}

	//gone into the eye: no death, no xp
	private static void melt( YogLarva l ){
		l.EXP = 0;
		l.destroy();
		if (l.sprite != null){
			if (Dungeon.level.heroFOV[l.pos]) CellEmitter.get(l.pos).burst(ShadowParticle.UP, 5);
			l.sprite.killAndErase();
		}
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("brood", brood);
		bundle.put("huddle", huddle);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		brood = bundle.getBoolean("brood");
		huddle = bundle.getInt("huddle");
	}

	public static class LarvaSprite extends ContinentMobSprite {
		public LarvaSprite(){
			super();
			setup(ContinentAssets.YOG_LARVA, 12, 10);
		}
	}
}
