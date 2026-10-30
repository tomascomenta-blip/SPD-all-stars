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

package com.shatteredpixel.shatteredpixeldungeon.rift;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.PurpleParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;

//elder mark: when it runs out and you're under 20% hp, you die. new hits don't refresh it, so HEAL
public class ElderMark extends FlavourBuff {

	{
		type = buffType.NEGATIVE;
		announced = true;
	}

	public static final float DURATION = 3f;
	public static final float THRESHOLD = 0.2f;

	public static void mark( Char ch ){
		if (ch == null || !ch.isAlive() || ch.buff(ElderMark.class) != null) return;
		Buff.affect(ch, ElderMark.class, DURATION);
	}

	//health under which the mark executes
	public static int threshold( Char ch ){
		return Math.round(ch.HT * THRESHOLD);
	}

	@Override
	public boolean act() {
		if (target.isAlive() && target.HP < threshold(target)){
			execute(target);
		}
		detach();
		return true;
	}

	private static void execute( Char ch ){
		if (ch.isInvulnerable(ElderMark.class)) return;
		if (ch.sprite != null){
			ch.sprite.showStatus(CharSprite.NEGATIVE, Messages.get(ElderMark.class, "executed"));
			if (ch.sprite.visible) ch.sprite.emitter().burst(PurpleParticle.BURST, 25);
		}
		if (Dungeon.level.heroFOV[ch.pos]) Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG, 1f, 0.7f);
		String who = ch instanceof Hero && Party.active() ? Party.name((Hero) ch) : ch.name();
		GLog.n(Messages.get(ElderMark.class, "executed_log", who));
		ch.HP = 0;
		ch.die(ElderDragon.class);
		if (ch instanceof Hero && !ch.isAlive()){
			Dungeon.fail(ElderDragon.class);
		}
	}

	@Override
	public void fx( boolean on ) {
		if (on) target.sprite.add(CharSprite.State.MARKED);
		else target.sprite.remove(CharSprite.State.MARKED);
	}

	@Override
	public int icon() {
		return BuffIndicator.MARK;
	}

	@Override
	public void tintIcon( Image icon ) {
		icon.hardlight(ElderBurn.COLOR);
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, (DURATION - visualcooldown()) / DURATION);
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", threshold(target), Math.round(THRESHOLD*100), dispTurns());
	}
}
