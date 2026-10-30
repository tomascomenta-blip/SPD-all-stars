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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.IconTitle;

import java.util.List;

//end of a co-op run: what each player did (CoopStats). shown when the party falls and on a win
public class WndCoopScore extends Window {

	public WndCoopScore(){
		int w = PixelScene.landscape() ? 200 : 135;
		IconTitle title = new IconTitle(Icons.get(Icons.RANKINGS), Messages.get(this, "title", Dungeon.depth));
		title.setRect(0, 0, w, 0);
		add(title);
		float y = title.bottom() + 4;

		List<Hero> heroes = Party.heroes();
		int best = -1, mostDealt = 0, bestFriend = -1, mostFriend = 0;
		for (int i = 0; i < heroes.size(); i++){
			CoopStats s = heroes.get(i).coopStats;
			if (s.dealt > mostDealt){
				mostDealt = s.dealt;
				best = i;
			}
			if (s.friendDealt > mostFriend){
				mostFriend = s.friendDealt;
				bestFriend = i;
			}
		}

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < heroes.size(); i++){
			Hero h = heroes.get(i);
			CoopStats s = h.coopStats;
			if (i > 0) sb.append("\n\n");
			sb.append("_").append(Party.name(i)).append("_ (").append(Messages.titleCase(h.heroClass.title())).append(")");
			if (i == best) sb.append(" - ").append(Messages.get(this, "mvp"));
			sb.append("\n").append(Messages.get(this, "line", s.dealt, s.kills, s.taken, s.downs, s.gold));
			if (s.friendDealt > 0 || s.friendDowns > 0){
				sb.append("\n").append(Messages.get(this, "friendly", s.friendDealt, s.friendDowns));
				if (i == bestFriend) sb.append(" - ").append(Messages.get(this, "traitor"));
			}
		}
		RenderedTextBlock text = PixelScene.renderTextBlock(sb.toString(), 6);
		text.maxWidth(w);
		text.setPos(0, y);
		add(text);
		resize(w, (int)text.bottom() + 1);
	}
}
