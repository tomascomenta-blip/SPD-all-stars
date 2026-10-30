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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.finale;

import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.items.Amulet;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.SurfaceScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKWorlds;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.FileUtils;

import java.util.Collections;

//the secret ending: every region was one of the new worlds and you walk out with the amulet.
//the victory screen cracks and the clock thing drags everyone to the rift (depth 1, branch 3)
public class ThalFinale {

	public static final int DEPTH = 1;
	public static final int BRANCH = 3;

	//the dark continent behind the portal (off = the option shows greyed out)
	public static final boolean CONTINENT_READY = true;

	public static boolean called = false;
	public static boolean defeated = false;
	public static boolean rewindUsed = false;

	//set while the rewind reload is running, so the loader skips the usual resume stuff
	public static volatile boolean rewinding = false;

	public static void reset(){
		called = false;
		defeated = false;
		rewindUsed = false;
	}

	public static void store( Bundle quests ){
		Bundle b = new Bundle();
		b.put("called", called);
		b.put("defeated", defeated);
		b.put("rewind_used", rewindUsed);
		quests.put("thal_finale", b);
	}

	public static void restore( Bundle quests ){
		if (!quests.contains("thal_finale")){
			reset();
			return;
		}
		Bundle b = quests.getBundle("thal_finale");
		called = b.getBoolean("called");
		defeated = b.getBoolean("defeated");
		rewindUsed = b.getBoolean("rewind_used");
	}

	public static boolean qualifies(){
		if (DevFlags.thal()) return true;
		for (int r = SKWorlds.FOREST; r <= SKWorlds.FORGOTTEN; r++){
			if (!SKWorlds.regionSK(r)) return false;
		}
		return true;
	}

	public static boolean inRift(){
		return Dungeon.level instanceof RiftLevel;
	}

	//SewerLevel's surface exit, with the amulet. true = we took over the ending
	public static boolean onSurface( Hero hero ){
		if (defeated || !qualifies()) return false;
		if (Dungeon.depth != 1 || Dungeon.branch != 0) return false;
		for (Hero h : Party.active() ? Party.heroes() : Collections.singletonList(Dungeon.hero)){
			if (h.buff(ThalCall.class) != null) return true;
		}
		CoopLog.line("thal: victory interrupted by hero " + Party.indexOf(hero));
		Buff.affect(hero, ThalCall.class);
		return true;
	}

	//going in
	public static void enterRift( int fromPos ){
		called = true;
		Level.beforeTransition();
		InterlevelScene.curTransition = new LevelTransition(Dungeon.level, fromPos,
				LevelTransition.Type.BRANCH_EXIT, DEPTH, BRANCH, LevelTransition.Type.BRANCH_ENTRANCE);
		InterlevelScene.mode = InterlevelScene.Mode.DESCEND;
		Game.switchScene(InterlevelScene.class);
	}

	// *** one rewind per run ***

	private static String snapGame(){
		return GamesInProgress.gameFolder(GamesInProgress.curSlot) + "/rewind_game.dat";
	}

	private static String snapLevel(){
		return GamesInProgress.gameFolder(GamesInProgress.curSlot) + "/rewind_level.dat";
	}

	//right when the fight starts. the save on disk is still the one from arriving
	public static void snapshot(){
		if (rewindUsed) return;
		try {
			FileUtils.getFileHandle(GamesInProgress.gameFile(GamesInProgress.curSlot))
					.copyTo(FileUtils.getFileHandle(snapGame()));
			FileUtils.getFileHandle(GamesInProgress.depthFile(GamesInProgress.curSlot, DEPTH, BRANCH))
					.copyTo(FileUtils.getFileHandle(snapLevel()));
			CoopLog.line("thal: snapshot taken");
		} catch (Exception e){
			CoopLog.line("thal: snapshot failed " + e);
		}
	}

	public static boolean canRewind(){
		return inRift() && !rewindUsed && !defeated
				&& FileUtils.fileExists(snapGame()) && FileUtils.fileExists(snapLevel());
	}

	//Hero.die, the last standing hero is going down. true = time goes back instead
	public static boolean rewindOnWipe( Hero dying ){
		//already going back, whatever else hits them doesn't count
		if (rewinding && inRift()){
			dying.HP = 1;
			return true;
		}
		if (!canRewind()) return false;
		CoopLog.line("thal: party wiped, rewinding");
		dying.HP = 1;
		try {
			FileUtils.getFileHandle(snapGame())
					.copyTo(FileUtils.getFileHandle(GamesInProgress.gameFile(GamesInProgress.curSlot)));
			FileUtils.getFileHandle(snapLevel())
					.copyTo(FileUtils.getFileHandle(GamesInProgress.depthFile(GamesInProgress.curSlot, DEPTH, BRANCH)));
		} catch (Exception e){
			CoopLog.line("thal: rewind copy failed " + e);
			return false;
		}
		rewinding = true;
		ThalHud.rewindFlash();
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				InterlevelScene.mode = InterlevelScene.Mode.CONTINUE;
				Game.switchScene(InterlevelScene.class);
			}
		});
		return true;
	}

	//after the snapshot got loaded back (host or solo)
	public static void afterRewind(){
		rewinding = false;
		rewindUsed = true;
		FileUtils.deleteFile(snapGame());
		FileUtils.deleteFile(snapLevel());
		ThalVekhorr.rewoundBanner = true;
	}

	// *** the end ***

	//"finish" won the vote. same as walking out of floor 1 with the amulet, plus the bonus
	public static void finish(){
		finish(Amulet.class);
	}

	//the endings of the dark continent go through here too, with their own line in the rankings
	public static void finish( final Class<?> cause ){
		CoopLog.line("thal: finishing the run, " + cause.getSimpleName());
		Statistics.ascended = true;
		Game.switchScene(SurfaceScene.class, new Game.SceneChangeCallback() {
			@Override
			public void beforeCreate() {
			}

			@Override
			public void afterCreate() {
				Badges.validateHappyEnd();
				Dungeon.win( cause );
				Dungeon.deleteGame( GamesInProgress.curSlot, true );
				Badges.saveGlobal();
			}
		});
	}
}
