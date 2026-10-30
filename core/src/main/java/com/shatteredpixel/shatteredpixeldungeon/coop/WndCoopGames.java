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

package com.shatteredpixel.shatteredpixeldungeon.coop;

import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKDifficulty;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.IconTitle;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndChallenges;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Image;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

//the co-op runs saved on this device, like the solo list. VIEW from the menu (look, erase),
//CONTINUE from the host's room, REPLACE when the 8 slots are taken and a new run needs one
public class WndCoopGames extends Window {

	public enum Mode { VIEW, CONTINUE, REPLACE }

	public interface Listener {
		//slot to continue (CONTINUE) or to start the new run in (REPLACE); also told -1 after an erase
		void picked( int slot );
	}

	private static final int ROW = 20;
	private static final int GAP = 2;

	private static int width(){
		return PixelScene.landscape() ? 190 : 135;
	}

	public WndCoopGames( final Mode mode, final Listener listener ){
		int w = width();
		IconTitle title = new IconTitle(Icons.get(Icons.TALENT),
				Messages.get(this, mode == Mode.REPLACE ? "title_replace" : "title"));
		title.setRect(0, 0, w, 0);
		add(title);
		float y = title.bottom() + 4;

		ArrayList<CoopSaves.Info> saves = CoopSaves.all();
		if (mode == Mode.REPLACE){
			RenderedTextBlock t = PixelScene.renderTextBlock(Messages.get(this, "replace_body"), 6);
			t.maxWidth(w);
			t.setPos(0, y);
			add(t);
			y = t.bottom() + 4;
		}
		if (saves.isEmpty()){
			RenderedTextBlock t = PixelScene.renderTextBlock(Messages.get(this, "none"), 7);
			t.maxWidth(w);
			t.setPos(0, y);
			add(t);
			y = t.bottom() + 2;
		}
		int row = ROW;
		int room = (int)PixelScene.uiCamera.height - 20 - (int)y;
		while (row > 16 && saves.size() * (row + GAP) > room) row--;
		for (final CoopSaves.Info info : saves){
			RedButton b = new RedButton(summary(info), 7){
				@Override
				protected void onClick() {
					ShatteredPixelDungeon.scene().addToFront(new Details(info, mode, listener, WndCoopGames.this));
				}
			};
			if (info.classes.length > 0 && info.classes[0] != null) b.icon(classIcon(info.classes[0]));
			b.leftJustify = true;
			b.setRect(0, y, w, row);
			add(b);
			y += row + GAP;
		}
		resize(w, (int)y);
	}

	static Image classIcon( HeroClass c ){
		return new Image(c.spritesheet(), 0, 90, 12, 15);
	}

	//"tomIII, etayo · floor 10"
	private static String summary( CoopSaves.Info info ){
		StringBuilder names = new StringBuilder();
		for (String n : info.names){
			if (names.length() > 0) names.append(", ");
			names.append(n);
		}
		String s = names.toString();
		if (s.length() > 22) s = s.substring(0, 21) + "…";
		return Messages.get(WndCoopGames.class, "row", s, info.depth);
	}

	static String date( long time ){
		return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(time));
	}

	//one run: who plays it and how, and what can be done with it from here
	public static class Details extends Window {

		public Details( final CoopSaves.Info info, final Mode mode, final Listener listener, final Window list ){
			int w = width();
			IconTitle title = new IconTitle(info.classes.length > 0 && info.classes[0] != null
					? classIcon(info.classes[0]) : Icons.get(Icons.TALENT),
					Messages.get(WndCoopGames.class, "details_title", info.depth));
			title.setRect(0, 0, w, 0);
			add(title);
			float y = title.bottom() + 4;

			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < info.names.length; i++){
				HeroClass c = info.classes[i];
				sb.append(Messages.get(WndCoopGames.class, "player", info.names[i],
						c == null ? "?" : Messages.titleCase(c.title()), info.levels[i]));
				if (i == 0) sb.append(" - ").append(Messages.get(CoopScene.class, "host_tag"));
				sb.append("\n");
			}
			sb.append("\n").append(Messages.get(WndCoopGames.class, "difficulty", SKDifficulty.label(info.difficulty)));
			sb.append("\n").append(Messages.get(WndCoopGames.class, info.friendlyFire ? "ff_on" : "ff_off"));
			if (info.customSeed != null && !info.customSeed.isEmpty()){
				sb.append("\n").append(Messages.get(WndCoopGames.class, "seed", info.customSeed));
			}
			sb.append("\n").append(Messages.get(WndCoopGames.class, "last_played", date(info.lastPlayed)));
			if (!info.hostedBy(CoopScene.myName())){
				sb.append("\n\n").append(Messages.get(WndCoopGames.class, "other_host", info.host()));
			}
			if (mode == Mode.VIEW){
				sb.append("\n\n").append(Messages.get(WndCoopGames.class, "how_to_continue"));
			}
			RenderedTextBlock text = PixelScene.renderTextBlock(sb.toString(), 6);
			text.maxWidth(w);
			text.setPos(0, y);
			add(text);
			y = text.bottom() + 4;

			if (info.challenges > 0){
				RedButton chal = new RedButton(Messages.get(WndCoopGames.class, "challenges", Integer.bitCount(info.challenges)), 7){
					@Override
					protected void onClick() {
						ShatteredPixelDungeon.scene().addToFront(new WndChallenges(info.challenges, false));
					}
				};
				chal.icon(Icons.get(Icons.CHALLENGE_COLOR));
				chal.setRect(0, y, w, 18);
				add(chal);
				y += 20;
			}

			ArrayList<RedButton> buttons = new ArrayList<>();
			if (mode == Mode.CONTINUE){
				RedButton cont = new RedButton(Messages.get(WndCoopGames.class, "continue")){
					@Override
					protected void onClick() {
						hide();
						list.hide();
						if (listener != null) listener.picked(info.slot);
					}
				};
				cont.icon(Icons.get(Icons.ENTER));
				buttons.add(cont);
			}
			if (mode == Mode.REPLACE){
				RedButton use = new RedButton(Messages.get(WndCoopGames.class, "replace")){
					@Override
					protected void onClick() {
						confirm(Messages.get(WndCoopGames.class, "replace_warn"), new Runnable() {
							@Override
							public void run() {
								hide();
								list.hide();
								if (listener != null) listener.picked(info.slot);
							}
						});
					}
				};
				use.icon(Icons.get(Icons.CLOSE));
				buttons.add(use);
			} else {
				RedButton erase = new RedButton(Messages.get(WndCoopGames.class, "erase")){
					@Override
					protected void onClick() {
						confirm(Messages.get(WndCoopGames.class, "erase_warn"), new Runnable() {
							@Override
							public void run() {
								CoopSaves.delete(info.slot);
								hide();
								list.hide();
								ShatteredPixelDungeon.scene().addToFront(new WndCoopGames(mode, listener));
								if (listener != null) listener.picked(-1);
							}
						});
					}
				};
				erase.icon(Icons.get(Icons.CLOSE));
				buttons.add(erase);
			}
			float bw = (w - (buttons.size() - 1) * 2f) / buttons.size();
			for (int i = 0; i < buttons.size(); i++){
				buttons.get(i).setRect(i * (bw + 2), y, bw, 20);
				add(buttons.get(i));
			}
			resize(w, (int)(y + 21));
		}

		private void confirm( String body, final Runnable yes ){
			ShatteredPixelDungeon.scene().addToFront(new WndOptions(Icons.get(Icons.WARNING),
					Messages.get(WndCoopGames.class, "warn_title"), body,
					Messages.get(WndCoopGames.class, "yes"), Messages.get(WndCoopGames.class, "no")){
				@Override
				protected void onSelect( int index ) {
					if (index == 0) yes.run();
				}
			});
		}
	}
}
