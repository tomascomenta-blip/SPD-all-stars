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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.survival.ColossusBone;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.survival.Survival;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.props.SKProps;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.IconTitle;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;

import java.util.ArrayList;

//the forge that rises by a purified shrine: sacred fire, a colossus vertebra for an anvil. the
//bones of the big ones go into the hero's weapon or armor, a level each time, a bone more each
//time the same piece goes back on the anvil
public class ColossusForge extends SKProps.SKProp {

	{
		spriteClass = ForgeSprite.class;
		HP = HT = 1000;
	}

	public static final float TIME_TO_FORGE = 3f;

	@Override
	public boolean isInvulnerable( Class effect ) {
		return true;
	}

	@Override
	public boolean heroShouldInteract() {
		return true;
	}

	@Override
	public boolean interact( Char c ) {
		if (!(c instanceof Hero)) return true;
		final Hero h = (Hero) c;
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				GameScene.show(new WndForge(ColossusForge.this, h));
			}
		});
		return true;
	}

	//by the shrine, where the whole 5x5 of it stands on open ground. the anvil is 3 right, 3 down
	public static void raise( int shrine ){
		Level l = Dungeon.level;
		int w = l.width(), sx = shrine % w, sy = shrine / w;
		int best = -1, bestD = Integer.MAX_VALUE;
		for (int y = sy - 8; y <= sy + 4; y++){
			for (int x = sx - 8; x <= sx + 4; x++){
				if (!open(x, y)) continue;
				int anvil = (x + 3) + (y + 3) * w;
				int d = l.distance(anvil, shrine);
				if (d >= 2 && d < bestD){
					bestD = d;
					best = anvil;
				}
			}
		}
		if (best == -1) return;
		ColossusForge f = new ColossusForge();
		f.pos = best;
		GameScene.add(f);
		CellEmitter.get(best).burst(Speck.factory(Speck.LIGHT), 12);
		CoopLog.line("hunt: forge raised at " + best);
	}

	private static boolean open( int x, int y ){
		Level l = Dungeon.level;
		int w = l.width();
		if (x < 1 || y < 1 || x + 5 >= w || y + 5 >= l.height()) return false;
		for (int j = 0; j < 5; j++){
			for (int i = 0; i < 5; i++){
				int c = x + i + (y + j) * w;
				if (!l.passable[c] || l.pit[c] || l.water[c] || Actor.findChar(c) != null || l.getTransition(c) != null) return false;
			}
		}
		return true;
	}

	// *** forging ***

	public static int cost( Item item ){
		return 2 + Math.max(0, item.level()) / 2;
	}

	public static boolean canForge( Hero h, Item item ){
		return item != null && item.isUpgradable() && Survival.count(h, ColossusBone.class) >= cost(item);
	}

	public void forge( Hero h, Item item ){
		if (!isAlive() || !canForge(h, item) || Dungeon.level.distance(h.pos, pos) > 1) return;
		Survival.take(h, ColossusBone.class, cost(item));
		item.upgrade();
		CoopLog.line("hunt: hero " + Party.indexOf(h) + " forges " + item.getClass().getSimpleName() + " to +" + item.level());
		if (h == Dungeon.local()) GLog.p(Messages.get(ColossusForge.class, "forged", item.name()));
		Sample.INSTANCE.play(Assets.Sounds.EVOKE);
		CellEmitter.get(pos).burst(Speck.factory(Speck.STAR), 8);
		h.spend(TIME_TO_FORGE);
		h.busy();
		if (h.sprite != null && h.sprite.parent != null){
			h.sprite.operate(pos);
		} else {
			h.next();
		}
	}

	public static class WndForge extends Window {

		private static final int WIDTH = 130, BTN_H = 18;
		private static final float GAP = 2;

		private final ArrayList<RedButton> buttons = new ArrayList<>();

		public WndForge( final ColossusForge forge, final Hero hero ){
			super();

			ItemSprite icon = new ItemSprite(new ColossusBone());
			IconTitle title = new IconTitle(icon, Messages.titleCase(forge.name()));
			title.setRect(0, 0, WIDTH, 0);
			add(title);

			RenderedTextBlock text = PixelScene.renderTextBlock(Messages.get(this, "text", Survival.count(hero, ColossusBone.class)), 6);
			text.maxWidth(WIDTH);
			text.setPos(0, title.bottom() + GAP * 2);
			add(text);

			float pos = text.bottom() + GAP * 3;
			EquipableItem[] pieces = {hero.belongings.weapon(), hero.belongings.armor()};
			for (final EquipableItem item : pieces){
				String label = item == null ? Messages.get(this, "none")
						: Messages.get(this, "forge", item.name(), cost(item));
				RedButton b = new RedButton(label, 7){
					@Override
					protected void onClick() {
						hide();
						forge.forge(hero, item);
					}
				};
				b.multiline = true;
				b.enable(canForge(hero, item));
				b.setRect(0, pos, WIDTH, Math.max(BTN_H, b.reqHeight() + 2));
				add(b);
				buttons.add(b);
				pos = b.bottom() + GAP;
			}

			RedButton close = new RedButton(Messages.get(this, "close")){
				@Override
				protected void onClick() {
					hide();
				}
			};
			close.setRect(0, pos + GAP, WIDTH, BTN_H);
			add(close);
			resize(WIDTH, (int) close.bottom());
		}

		//weapon, then armor (the test bot presses them like a player)
		public RedButton button( int index ){
			return index >= 0 && index < buttons.size() ? buttons.get(index) : null;
		}
	}

	//the whole 80x96 picture, drawn from the cell three left and three up of the anvil (one more
	//up for the chimney)
	public static class ForgeSprite extends MobSprite {

		public ForgeSprite(){
			super();
			texture(Hunt.FORGE);
			TextureFilm film = new TextureFilm(texture, 80, 96);
			perspectiveRaise = 0;
			idle = new Animation(1, true);
			idle.frames(film, 0);
			run = idle.clone();
			attack = idle.clone();
			zap = idle.clone();
			die = idle.clone();
			play(idle);
		}

		@Override
		public PointF worldToCamera( int cell ) {
			int s = DungeonTilemap.SIZE, w = Dungeon.level.width();
			return new PointF(
					PixelScene.align(Camera.main, (cell % w - 3) * s),
					PixelScene.align(Camera.main, (cell / w - 3) * s - s)
			);
		}

		@Override
		public void turnTo( int from, int to ) {
		}
	}
}
