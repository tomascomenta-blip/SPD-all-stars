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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.quests;

import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.watabou.utils.Bundle;

//all the new side quests, saved together w/ the run's other quests
public class SKQuests {

	public static final QuestState HUNT   = new QuestState("quest_hunt");    //grey tusk, forest
	public static final QuestState CAIMAN = new QuestState("quest_caiman");  //caiman / sewers
	public static final TourneyQuest.State TOURNEY = new TourneyQuest.State(); //herald, kingdom
	public static final QuestState WORM   = new QuestState("quest_worm");    //worm, caves
	public static final AndroidQuest.State ANDROID = new AndroidQuest.State(); //M-4, starship
	public static final LabQuest.State LAB = new LabQuest.State();       //flooded lab (undersea)
	public static final GolemQuest.State GOLEM = new GolemQuest.State(); //golem, city
	public static final YetiQuest.State YETI = new YetiQuest.State();    //yeti king, forgotten land
	public static final SealsQuest.State SEALS = new SealsQuest.State(); //seals, halls

	private static final QuestState[] ALL = {HUNT, CAIMAN, TOURNEY, WORM, ANDROID, LAB, GOLEM, YETI, SEALS};

	public static void reset(){
		for (QuestState q : ALL) q.reset();
		testOutcomes();
	}

	//-Dspd.test.questset=caiman:win,hunt:fail ... so you can try the boss fights w/o playing the quest first
	private static void testOutcomes(){
		String set = DevFlags.questSet();
		if (set == null) return;
		for (String part : set.split(",")){
			String[] kv = part.trim().split(":");
			if (kv.length != 2) continue;
			QuestState q = null;
			for (QuestState s : ALL) if (s.key.equals("quest_" + kv[0])) q = s;
			if (q == null) continue;
			boolean win = kv[1].equals("win");
			q.given = true;
			q.slain = q.done = win;
			if (q == TOURNEY){
				TOURNEY.round = 3;
				TOURNEY.lost = win ? 0 : 1;
			}
			if (q == ANDROID) ANDROID.awake = !win;
			if (q == LAB){
				LAB.sealed = win ? 3 : 0;
				LAB.failed = !win;
			}
			if (q == SEALS) SEALS.broken = win ? 3 : 0;
			//seals:N = a later seals floor with N already broken (in time)
			if (q == SEALS && kv[1].matches("[0-2]")) SEALS.broken = SEALS.quick = Integer.parseInt(kv[1]);
		}
	}

	public static void store( Bundle bundle ){
		for (QuestState q : ALL) q.store(bundle);
	}

	public static void restore( Bundle bundle ){
		for (QuestState q : ALL) q.restore(bundle);
	}
}
