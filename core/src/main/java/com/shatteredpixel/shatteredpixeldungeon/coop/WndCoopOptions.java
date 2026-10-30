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

import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.AntiOP;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKDifficulty;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.WndSKOptions;
import com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.DungeonSeed;
import com.shatteredpixel.shatteredpixeldungeon.windows.IconTitle;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndChallenges;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTextInput;
import com.watabou.utils.GameSettings;

//the host's options for the run, in one window so the room still fits on a phone
public class WndCoopOptions extends Window {

	private static final int BTN_H = 18;
	private static final int GAP = 2;

	private final Runnable onChange;

	public WndCoopOptions( Runnable onChange ){
		this.onChange = onChange;
		build();
	}

	//the room password, empty = none. remembered between sessions
	public static String password(){
		return GameSettings.getString("coop_password", "", 20);
	}

	private void build(){
		int w = PixelScene.landscape() ? 190 : 135;
		IconTitle title = new IconTitle(Icons.get(Icons.PREFS), Messages.get(this, "title"));
		title.setRect(0, 0, w, 0);
		add(title);
		float y = title.bottom() + 4;

		y = button(Messages.get(WndSKOptions.class, AntiOP.chosenOn() ? "lobby_button_antiop" : "lobby_button",
				SKDifficulty.label(SPDSettings.skDifficulty()), SPDSettings.skWorldChance() * 10, AntiOP.chosenCap()), y, w, new Runnable() {
			@Override
			public void run() {
				ShatteredPixelDungeon.scene().addToFront(new WndSKOptions(new Runnable() {
					@Override
					public void run() {
						changed();
					}
				}));
			}
		});

		y = button(Messages.get(this, "challenges", Integer.bitCount(SPDSettings.challenges())), y, w, new Runnable() {
			@Override
			public void run() {
				ShatteredPixelDungeon.scene().addToFront(new WndChallenges(SPDSettings.challenges(), true){
					@Override
					public void onBackPressed() {
						super.onBackPressed();
						changed();
					}
				});
			}
		});

		String seed = SPDSettings.customSeed();
		y = button(seed.isEmpty() ? Messages.get(this, "seed_random") : Messages.get(this, "seed", seed), y, w, new Runnable() {
			@Override
			public void run() {
				askSeed();
			}
		});

		CheckBox ff = new CheckBox(Messages.get(this, "friendly_fire")){
			@Override
			protected void onClick() {
				super.onClick();
				SPDSettings.coopFriendlyFire(checked());
				changed();
			}
		};
		ff.checked(SPDSettings.coopFriendlyFire());
		ff.setRect(0, y, w, BTN_H);
		add(ff);
		y += BTN_H + GAP;

		y = button(password().isEmpty() ? Messages.get(this, "password_none") : Messages.get(this, "password_set"), y, w, new Runnable() {
			@Override
			public void run() {
				askPassword();
			}
		});

		RenderedTextBlock note = PixelScene.renderTextBlock(Messages.get(this, "note"), 6);
		note.maxWidth(w);
		note.setPos(0, y + 2);
		add(note);
		resize(w, (int)note.bottom() + 1);
	}

	private float button( String label, float y, int w, final Runnable action ){
		RedButton b = new RedButton(label, 7){
			@Override
			protected void onClick() {
				action.run();
			}
		};
		b.setRect(0, y, w, BTN_H);
		add(b);
		return y + BTN_H + GAP;
	}

	//shown again with the new values (a window can't be emptied and refilled: it would lose its frame)
	private void changed(){
		hide();
		ShatteredPixelDungeon.scene().addToFront(new WndCoopOptions(onChange));
		if (onChange != null) onChange.run();
	}

	private void askSeed(){
		ShatteredPixelDungeon.scene().addToFront(new WndTextInput(Messages.get(this, "seed_title"),
				Messages.get(this, "seed_desc"), SPDSettings.customSeed(), 20, false,
				Messages.get(this, "set"), Messages.get(this, "clear")){
			@Override
			public void onSelect( boolean positive, String text ) {
				text = DungeonSeed.formatText(text);
				if (positive && DungeonSeed.convertFromText(text) != -1){
					SPDSettings.customSeed(text);
				} else {
					SPDSettings.customSeed("");
				}
				changed();
			}
		});
	}

	private void askPassword(){
		ShatteredPixelDungeon.scene().addToFront(new WndTextInput(Messages.get(this, "password_title"),
				Messages.get(this, "password_desc"), password(), 20, false,
				Messages.get(this, "set"), Messages.get(this, "clear")){
			@Override
			public void onSelect( boolean positive, String text ) {
				GameSettings.put("coop_password", positive ? text.trim() : "");
				changed();
			}
		});
	}
}
