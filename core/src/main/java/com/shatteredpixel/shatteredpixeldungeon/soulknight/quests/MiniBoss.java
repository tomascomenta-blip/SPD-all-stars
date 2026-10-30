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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMob;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.Bundle;

/**
 * the monsters at the end of the side quests. not quite bosses but they get a little show:
 * short intro the first time they see the heroes (no fighting during it), big name title,
 * their own hp bar (MiniBossHud), the world's fight music, and phases.
 * they DON'T seal the floor
 */
public abstract class MiniBoss extends SKMob {

	{
		properties.add(Property.MINIBOSS);
	}

	public boolean started = false;
	public int phase = 1;
	protected int introLeft = 0;
	private boolean musicPending = false;

	//hp bar frame. 176x32, life goes in x 38-167, y 12-19
	public abstract String barAsset();

	protected abstract String music();

	public String title(){
		return Messages.titleCase(name());
	}

	public String subtitle(){
		return Messages.get(this, "subtitle");
	}

	//the little line under the bar
	public String status(){
		return Messages.get(MiniBoss.class, "phase", phase);
	}

	//bar colour, ARGB
	public int barColor(){
		return 0xFFB03A2A;
	}

	//0-1
	public float barFill(){
		return Math.max(0, HP) / (float) HT;
	}

	protected int introTurns(){
		return 2;
	}

	//once per intro turn, introTurns()-1 down to 0
	protected void onIntro( int left ){
	}

	protected void onStart(){
	}

	public void startFight(){
		if (started) return;
		started = true;
		introLeft = introTurns();
		MiniBossHud.title(title(), subtitle());
		playMusic();
		onStart();
	}

	public void playMusic(){
		//dragged into a region boss fight (the punishment): boss music wins
		if (Dungeon.level != null && Dungeon.level.locked) return;
		Music.INSTANCE.play(music(), true);
	}

	public boolean inIntro(){
		return introLeft > 0;
	}

	protected void newPhase( int p ){
		phase = p;
		String key = "phase_" + p;
		String txt = Messages.get(this, key);
		if (!txt.equals(Messages.NO_TEXT_FOUND) && !txt.contains(key)) MiniBossHud.banner(txt);
	}

	@Override
	public void notice() {
		super.notice();
		startFight();
	}

	@Override
	protected boolean act() {
		if (musicPending){
			musicPending = false;
			playMusic();
		}
		if (started && introLeft > 0){
			introLeft--;
			onIntro(introLeft);
			spend(TICK);
			return true;
		}
		return super.act();
	}

	@Override
	public boolean isInvulnerable( Class effect ) {
		return inIntro() || super.isInvulnerable(effect);
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (!started) startFight();
		super.damage(dmg, src);
	}

	@Override
	public void die( Object cause ) {
		super.die(cause);
		MiniBossHud.banner(Messages.get(this, "defeated"));
		if (Dungeon.level != null) Dungeon.level.playLevelMusic();
	}

	//the quest mob being fought on this floor, if any. the bar asks every frame, and Actor.chars() is a copy
	//made under the actors lock bc the game thread changes them meanwhile. cuidado con esto, don't "simplify" it
	public static MiniBoss active(){
		if (Dungeon.level == null) return null;
		for (Char ch : Actor.chars()){
			if (ch instanceof MiniBoss && ch.isAlive() && ((MiniBoss) ch).started) return (MiniBoss) ch;
		}
		return null;
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("mb_started", started);
		bundle.put("mb_phase", phase);
		bundle.put("mb_intro", introLeft);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		started = bundle.getBoolean("mb_started");
		phase = Math.max(1, bundle.getInt("mb_phase"));
		introLeft = bundle.getInt("mb_intro");
		musicPending = started;
	}
}
