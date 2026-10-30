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

package com.shatteredpixel.shatteredpixeldungeon.soulknight;

import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox;
import com.shatteredpixel.shatteredpixeldungeon.ui.OptionSlider;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;

//the 2 sliders: difficulty (mob hp) and new-world chance, plus the anti-op cap. UI only, goes to
//SPDSettings, the run copies them at start
public class WndSKOptions extends Window {

	private static final int WIDTH  = 140;
	private static final int SLIDER = 26;
	private static final int GAP    = 3;
	private static final int ROW    = 16;
	private static final int CAP_X  = 94;

	private final Runnable onClose;
	private RenderedTextBlock difficultyText;
	private RenderedTextBlock worldsText;
	private RenderedTextBlock capText;
	private RedButton[] capButtons;

	public WndSKOptions(){
		this(null);
	}

	public WndSKOptions( Runnable onClose ){
		super();
		this.onClose = onClose;

		RenderedTextBlock title = PixelScene.renderTextBlock(Messages.get(this, "title"), 9);
		title.hardlight(TITLE_COLOR);
		title.setPos((WIDTH - title.width())/2f, 2);
		PixelScene.align(title);
		add(title);
		float y = title.bottom() + 2*GAP;

		OptionSlider difficulty = new OptionSlider(Messages.get(this, "difficulty"),
				Messages.get(this, "easy"), Messages.get(this, "hardest"), 0, SKDifficulty.FACTORS.length - 1) {
			@Override
			protected void onChange() {
				SPDSettings.skDifficulty(getSelectedValue());
				refresh();
			}
		};
		difficulty.setSelectedValue(SPDSettings.skDifficulty());
		difficulty.setRect(0, y, WIDTH, SLIDER);
		add(difficulty);
		y = difficulty.bottom() + 1;

		difficultyText = PixelScene.renderTextBlock(6);
		difficultyText.maxWidth(WIDTH);
		add(difficultyText);
		difficultyText.setPos(0, y);
		y += 18 + GAP;

		OptionSlider worlds = new OptionSlider(Messages.get(this, "worlds"),
				Messages.get(this, "never"), Messages.get(this, "always"), 0, 10) {
			@Override
			protected void onChange() {
				SPDSettings.skWorldChance(getSelectedValue());
				refresh();
			}
		};
		worlds.setSelectedValue(SPDSettings.skWorldChance());
		worlds.setRect(0, y, WIDTH, SLIDER);
		add(worlds);
		y = worlds.bottom() + 1;

		worldsText = PixelScene.renderTextBlock(6);
		worldsText.maxWidth(WIDTH);
		add(worldsText);
		worldsText.setPos(0, y);
		y += 18 + GAP;

		//anti-op: the switch, then -50 -10 [cap] +10 +50 on the same row
		CheckBox antiOp = new CheckBox(Messages.get(this, "antiop")){
			@Override
			protected void onClick() {
				super.onClick();
				AntiOP.chosenOn(checked());
				refresh();
			}
		};
		antiOp.checked(AntiOP.chosenOn());
		antiOp.setRect(0, y, 46, ROW + 2);
		add(antiOp);

		int[] steps = {-AntiOP.BIG_STEP, -AntiOP.STEP, AntiOP.STEP, AntiOP.BIG_STEP};
		int[] xs = {48, 66, 105, 123};
		capButtons = new RedButton[steps.length];
		for (int i = 0; i < steps.length; i++){
			final int step = steps[i];
			RedButton b = new RedButton((step > 0 ? "+" : "-") + Math.abs(step), 6){
				@Override
				protected void onClick() {
					AntiOP.chosenCap(AntiOP.chosenCap() + step);
					refresh();
				}
			};
			b.setRect(xs[i], y + 1, 17, ROW);
			add(b);
			capButtons[i] = b;
		}
		//the number, between the minus and the plus buttons
		capText = PixelScene.renderTextBlock(8);
		add(capText);
		capText.setPos(CAP_X, y + 5);
		y += ROW + 2 + 1;

		RenderedTextBlock note = PixelScene.renderTextBlock(Messages.get(this, "note"), 6);
		note.maxWidth(WIDTH);
		note.hardlight(0xAAAAAA);
		note.setPos(0, y);
		add(note);
		y = note.bottom() + GAP;

		refresh();
		resize(WIDTH, (int) y);
	}

	private void refresh(){
		difficultyText.text(Messages.get(this, "difficulty_now", SKDifficulty.label(SPDSettings.skDifficulty())));
		worldsText.text(Messages.get(this, "worlds_now", SPDSettings.skWorldChance() * 10));
		boolean on = AntiOP.chosenOn();
		int cap = AntiOP.chosenCap();
		capText.text(Integer.toString(cap));
		capText.alpha(on ? 1f : 0.4f);
		capText.setPos(CAP_X - capText.width() / 2f, capText.top());
		PixelScene.align(capText);
		capButtons[0].enable(on && cap > AntiOP.MIN);
		capButtons[1].enable(on && cap > AntiOP.MIN);
		capButtons[2].enable(on && cap < AntiOP.MAX);
		capButtons[3].enable(on && cap < AntiOP.MAX);
	}

	@Override
	public void hide() {
		super.hide();
		if (onClose != null) onClose.run();
	}
}
