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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.finale.reward;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

import java.util.ArrayList;

//the clockwork heart of the colossus, still ticking out of step. press it to your chest and pick
//which way you go past the end of hours. works like tengu's mask, one per hero
public class ThalHeart extends Item {

	public static final String AC_LISTEN = "LISTEN";

	{
		stackable = false;
		image = ItemSpriteSheet.THAL_HEART;
		defaultAction = AC_LISTEN;
		unique = true;
		bones = false;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_LISTEN);
		return actions;
	}

	@Override
	public void execute( final Hero hero, String action ) {
		super.execute(hero, action);

		if (action.equals(AC_LISTEN)){
			//one per hero in co-op: if you already picked, this one's for a friend
			if (hero.specialization != null){
				GLog.w(Messages.get(this, "already"));
				return;
			}
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					GameScene.show(new WndChooseSpecialization(ThalHeart.this, hero));
				}
			});
		}
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	//the hero of the window, never curUser: someone else may have used an item meanwhile
	public void choose( Specialization spec, Hero hero ){
		if (hero.specialization != null) return;

		detach(hero.belongings.backpack);

		hero.spend(Actor.TICK);
		hero.busy();

		hero.specialization = spec;
		Talent.initSpecTalents(hero);
		SpecPowers.onChosen(hero);
		BuffIndicator.refreshHero();
		CoopLog.line("thal path " + spec + " for " + hero.heroClass);

		if (hero.sprite != null){
			hero.sprite.operate(hero.pos);
			hero.sprite.centerEmitter().burst(Speck.factory(Speck.STAR), 12);
		} else {
			hero.next();
		}
		Sample.INSTANCE.play(Assets.Sounds.MASTERY);
		GLog.p(Messages.get(this, "used", spec.title()));
	}
}
