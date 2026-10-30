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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.fauna;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.world.Senses;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

//chain dog. still chained to the post its masters left it on: it can't get further than the
//chain, and barks at everything. pull at it three times trying to get at someone and the chain
//gives way
public class ChainDog extends CityCritter {

	{
		spriteClass = DogSprite.class;

		HP = HT = 110;
		defenseSkill = 30;
		EXP = 9;

		minDmg = 22; maxDmg = 30; acc = 58;
		armorMin = 2; armorMax = 8;
	}

	private static final int CHAIN = 4;

	//the post (-1 once it's loose) and how many times it has pulled
	public int post = -1;
	private int pulls = 0;
	private boolean placed = false;

	@Override
	protected int ears() {
		return 4;
	}

	@Override
	protected void onAdd() {
		super.onAdd();
		if (!placed){
			placed = true;
			post = pos;
		}
	}

	private boolean chained( int cell ){
		return post == -1 || Dungeon.level.distance(post, cell) <= CHAIN;
	}

	//it goes as far as the chain lets it, towards whatever it wants
	@Override
	protected boolean getCloser( int target ) {
		if (post != -1 && !chained(target)){
			Level l = Dungeon.level;
			int w = l.width(), best = -1, bestD = Integer.MAX_VALUE;
			for (int y = post / w - CHAIN; y <= post / w + CHAIN; y++){
				for (int x = post % w - CHAIN; x <= post % w + CHAIN; x++){
					if (x < 0 || y < 0 || x >= w || y >= l.height()) continue;
					int c = x + y * w;
					if (!l.passable[c] || l.pit[c] || Actor.findChar(c) != null && c != pos) continue;
					int d = l.distance(c, target);
					if (d < bestD){
						bestD = d;
						best = c;
					}
				}
			}
			if (best == -1 || best == pos){
				if (state == HUNTING && enemy != null){
					pull();
					return true;
				}
				return false;
			}
			target = best;
		}
		return super.getCloser(target);
	}

	private void pull(){
		pulls++;
		Senses.noise(pos, Senses.FIGHT);
		if (sprite instanceof DogSprite) ((DogSprite) sprite).special();
		if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.CHAINS, 0.7f, 1.4f);
		if (pulls >= 3){
			post = -1;
			if (sprite != null && Dungeon.level.heroFOV[pos]){
				sprite.showStatus(CharSprite.WARNING, Messages.get(this, "loose"));
				GLog.w(Messages.get(this, "breaks"));
			}
		}
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("post", post);
		bundle.put("pulls", pulls);
		bundle.put("placed", placed);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		post = bundle.getInt("post");
		pulls = bundle.getInt("pulls");
		placed = bundle.getBoolean("placed");
	}

	public static class DogSprite extends Sheet {

		public DogSprite(){
			super();
			layout(CityAssets.DOG);
		}
	}
}
