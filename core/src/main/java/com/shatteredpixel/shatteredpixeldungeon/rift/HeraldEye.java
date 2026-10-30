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

package com.shatteredpixel.shatteredpixeldungeon.rift;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.PurpleParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

//eye of the herald, one each, single use: she comes back for 200 turns as an ally
public class HeraldEye extends Item {

	public static final String AC_SUMMON = "SUMMON";

	{
		image = ItemSpriteSheet.HERALD_EYE;
		stackable = true;
		defaultAction = AC_SUMMON;
		bones = false;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_SUMMON);
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ) {
		super.execute(hero, action);
		if (!action.equals(AC_SUMMON) || !hero.belongings.backpack.contains(this)) return;

		//a free cell next to the hero
		int cell = -1;
		for (int n : PathFinder.NEIGHBOURS8){
			int c = hero.pos + n;
			if (c < 0 || c >= Dungeon.level.length()) continue;
			if (Actor.findChar(c) == null && Dungeon.level.passable[c] && !Dungeon.level.solid[c] && !Dungeon.level.pit[c]){
				cell = c;
				break;
			}
		}
		if (cell == -1){
			GLog.w(Messages.get(this, "no_space"));
			return;
		}

		detach(hero.belongings.backpack);
		HeraldAlly ally = HeraldAlly.summon(hero, cell);
		GameScene.add(ally);
		Dungeon.level.occupyCell(ally);
		if (ally.sprite != null) ally.sprite.emitter().burst(PurpleParticle.BURST, 15);
		Dungeon.observe();
		GLog.p(Messages.get(this, "summoned"));
		Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);

		//the gesture is only visual: the turn ends here, not when the animation does
		final Hero user = hero;
		hero.sprite.operate(hero.pos, new Callback() {
			@Override
			public void call() {
				user.sprite.idle();
			}
		});
		hero.spendAndNext(1f);
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
	public int value() {
		return 40 * quantity;
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return new ItemSprite.Glowing(0xAA55FF, 1.5f);
	}

	@Override
	public String desc() {
		int depth = Math.max(1, Dungeon.scalingDepth());
		return Messages.get(this, "desc", Math.round(HeraldAlly.DURATION), HeraldAlly.hpFor(depth),
				HeraldAlly.minDamage(depth), HeraldAlly.maxDamage(depth),
				Math.round(HeraldAlly.CHARGE_MULTI * HeraldAlly.minDamage(depth)),
				Math.round(HeraldAlly.CHARGE_MULTI * HeraldAlly.maxDamage(depth)),
				Math.round(HeraldAlly.CHARGE_COST * 100));
	}
}
