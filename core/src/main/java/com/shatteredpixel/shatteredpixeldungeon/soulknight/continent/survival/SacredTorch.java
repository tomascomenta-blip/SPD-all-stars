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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.survival;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.awake.Awakening;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

import java.util.ArrayList;

//a torch wrapped in white cloth, waiting. lighting it burns one drop of the party's sacred
//essence: then it shines a long while, the monsters keep away from whoever carries it and the
//yogs run (SacredLight). when it goes out the essence has gone with it
public class SacredTorch extends Item {

	public static final String AC_LIGHT = "LIGHT";

	{
		image = ItemSpriteSheet.SACRED_TORCH;
		stackable = true;
		defaultAction = AC_LIGHT;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_LIGHT);
		return actions;
	}

	@Override
	public void execute( final Hero hero, String action ) {
		super.execute(hero, action);
		if (!action.equals(AC_LIGHT)) return;

		if (hero.buff(SacredLight.class) != null){
			GLog.w(Messages.get(this, "already"));
			return;
		}
		if (Continent.essence <= 0){
			GLog.w(Messages.get(this, "no_essence"));
			return;
		}
		//it's dear: ask first. in co-op the answer is replayed like any other window
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				GameScene.show(new WndOptions(new ItemSprite(SacredTorch.this),
						Messages.titleCase(name()),
						Awakening.torchWarning() + Messages.get(SacredTorch.class, "confirm", Continent.essence, Continent.NEEDED),
						Messages.get(SacredTorch.class, "yes"),
						Messages.get(SacredTorch.class, "no")){
					@Override
					protected void onSelect( int index ) {
						if (index == 0) light(hero);
					}
				});
			}
		});
	}

	public void light( Hero hero ){
		if (Continent.essence <= 0 || hero.buff(SacredLight.class) != null || !hero.belongings.contains(this)) return;
		detach(hero.belongings.backpack);
		Continent.essence--;
		CoopLog.line("survival: hero " + Party.indexOf(hero) + " lit a sacred torch, essence left " + Continent.essence);
		SacredLight light = Buff.affect(hero, SacredLight.class);
		light.set(SacredLight.DURATION);
		light.see();
		Dungeon.observe();
		GLog.p(Messages.get(this, "lit", Continent.essence, Continent.NEEDED));
		Sample.INSTANCE.play(Assets.Sounds.MASTERY, 0.8f, 1.4f);
		hero.spend(1f);
		hero.busy();
		if (hero.sprite != null){
			hero.sprite.centerEmitter().burst(Speck.factory(Speck.LIGHT), 12);
			hero.sprite.operate(hero.pos);
		} else {
			hero.next();
		}
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", SacredLight.RADIUS * 2, Continent.essence, Continent.NEEDED);
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
		return 20 * quantity;
	}
}
