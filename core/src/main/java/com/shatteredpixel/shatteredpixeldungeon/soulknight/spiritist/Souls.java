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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.spiritist;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;

//souls from nearby deaths, max 10. 5 souls = next ability spends them for half cd.
//3 souls = ghost scout that peeks through walls
public class Souls extends Buff {

	{
		type = buffType.POSITIVE;
		revivePersists = true;
	}

	public static final int MAX = 10;
	public static final int EMPOWER = 5;
	public static final int SCOUT = 3;
	public static final int SCOUT_RANGE = 10;

	public int souls = 0;

	@Override
	public boolean act() {
		spend(TICK*10);
		return true;
	}

	public static Souls of( Hero hero ){
		return Buff.affect(hero, Souls.class);
	}

	public static void gain( Hero hero ){
		Souls s = of(hero);
		if (s.souls < MAX) s.souls++;
	}

	//5 souls -> half cd
	public static float cooldown( Hero hero, ChampionAbility ability, float turns ){
		if (hero == null || hero.heroClass != HeroClass.SPIRITIST || turns <= 0) return turns;
		Souls s = hero.buff(Souls.class);
		if (s == null || s.souls < EMPOWER) return turns;
		s.souls -= EMPOWER;
		if (hero.sprite != null) hero.sprite.emitter().burst(ShadowParticle.UP, 10);
		GLog.p(Messages.get(Souls.class, "empower", ability.name()));
		CoopLog.line("souls empower " + ability.getClass().getSimpleName() + " left=" + s.souls);
		return turns / 2f;
	}

	//reveal around him, las paredes dan igual
	public static void scout( Hero hero ){
		Souls s = hero.buff(Souls.class);
		if (s == null || s.souls < SCOUT){
			GLog.w(Messages.get(Souls.class, "not_enough", SCOUT));
			return;
		}
		s.souls -= SCOUT;
		int w = Dungeon.level.width();
		int cx = hero.pos % w, cy = hero.pos / w;
		for (int y = Math.max(0, cy - SCOUT_RANGE); y <= Math.min(Dungeon.level.height() - 1, cy + SCOUT_RANGE); y++){
			for (int x = Math.max(0, cx - SCOUT_RANGE); x <= Math.min(w - 1, cx + SCOUT_RANGE); x++){
				int c = x + y*w;
				if (Dungeon.level.distance(c, hero.pos) > SCOUT_RANGE) continue;
				Dungeon.level.mapped[c] = true;
			}
		}
		for (int n : PathFinder.NEIGHBOURS8){
			int c = hero.pos + n;
			if (c >= 0 && c < Dungeon.level.length()) CellEmitter.get(c).burst(ShadowParticle.UP, 3);
		}
		GameScene.updateFog();
		Sample.INSTANCE.play(Assets.Sounds.GHOST);
		GLog.p(Messages.get(Souls.class, "scouted"));
		CoopLog.line("ghost scout around " + hero.pos);
		hero.spendAndNext(Actor.TICK);
	}

	@Override
	public int icon() {
		return BuffIndicator.CORRUPT;
	}

	@Override
	public void tintIcon( Image icon ) {
		icon.hardlight(0x9FE3FF);
	}

	@Override
	public float iconFadePercent() {
		return 1f - souls / (float) MAX;
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(souls);
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", souls, MAX, EMPOWER, SCOUT);
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("souls", souls);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		souls = bundle.getInt("souls");
	}
}
