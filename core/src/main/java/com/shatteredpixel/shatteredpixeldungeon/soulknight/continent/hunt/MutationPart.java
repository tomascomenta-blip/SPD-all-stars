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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.hunt;

import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.AshTarantula;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.AshWhale;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.BasaltBison;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.BeakRex;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.LavaSalamander;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.ObsidianBrute;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.SlagGiant;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.SmelterBeetle;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.VolcanoTurtle;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.WalkingPyre;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.big.Colossus;
import com.watabou.utils.Bundle;

//a piece of one of the big ones: a heart that still glows, a gill, a horn... it feeds a little, and
//whoever eats it takes something of the beast (Mutations)
public class MutationPart extends Food {

	public int kind;

	{
		energy = 100;
	}

	public MutationPart(){
		this(0);
	}

	public MutationPart( int kind ){
		this.kind = kind;
		image = PartIcons.image(kind);
	}

	//which part a beast leaves, -1 for none
	public static int of( Colossus c ){
		if (c instanceof WalkingPyre) return Mutations.EMBER_HEART;
		if (c instanceof SlagGiant) return Mutations.NAPE;
		if (c instanceof VolcanoTurtle) return Mutations.CHIMNEY;
		if (c instanceof AshWhale) return Mutations.GILL;
		if (c instanceof AshTarantula) return Mutations.SPIDER_EGG;
		if (c instanceof BeakRex) return Mutations.FIRE_GLAND;
		if (c instanceof ObsidianBrute) return Mutations.OBSIDIAN;
		if (c instanceof BasaltBison) return Mutations.HORN;
		if (c instanceof SmelterBeetle) return Mutations.BEAK;
		if (c instanceof LavaSalamander) return Mutations.THROAT;
		return BiomeParts.of(c);
	}

	//the big ones always leave theirs
	public static float chance( int size ){
		if (DevFlags.partDrop()) return 1f;
		return size >= 8 ? 1f : size >= 5 ? 0.6f : 0.35f;
	}

	@Override
	protected void satisfy( Hero hero ) {
		super.satisfy(hero);
		Mutations.mutate(hero, kind);
	}

	@Override
	public boolean isSimilar( Item item ) {
		return super.isSimilar(item) && ((MutationPart) item).kind == kind;
	}

	@Override
	public String name() {
		return Messages.get(this, "name_" + kind);
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc_" + kind) + "\n\n" + Messages.get(this, "desc");
	}

	@Override
	public int value() {
		return 30 * quantity;
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("kind", kind);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		kind = bundle.getInt("kind");
		image = PartIcons.image(kind);
	}
}
