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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

//a drop of sacred essence. it doesn't go in the bag: it belongs to the whole party (Continent.essence),
//picking it up just adds it. shrines and guardians drop one each, with their source id
public class SacredEssence extends Item {

	{
		image = ItemSpriteSheet.SACRED_ESSENCE;
		unique = true;
	}

	public int source = Continent.SHRINE_ASH;

	public SacredEssence(){
	}

	public SacredEssence( int source ){
		this.source = source;
	}

	@Override
	public boolean doPickUp( Hero hero, int pos ) {
		Continent.addEssence(source);
		GameScene.pickUp(this, pos);
		Sample.INSTANCE.play(Assets.Sounds.MASTERY, 0.8f, 1.3f);
		hero.spendAndNext(pickupDelay());
		return true;
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", Continent.essence, Continent.NEEDED);
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("source", source);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		source = bundle.getInt("source");
	}
}
