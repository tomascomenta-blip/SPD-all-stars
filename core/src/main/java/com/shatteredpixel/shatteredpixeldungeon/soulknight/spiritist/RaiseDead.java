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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

//E: bring back the last stuff that died near him, on our side for a while. no bosses, no weird stuff
public class RaiseDead extends ChampionAbility {

	public static final RaiseDead INSTANCE = new RaiseDead();

	@Override public String slot() { return "E"; }
	@Override public int unlockLevel() { return 4; }
	@Override public int icon() { return HeroIcon.SP_RAISE; }
	@Override public int color() { return 0x55DD99; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }

	public static final int RANGE = 5;

	@Override
	public float cooldown( Hero hero ) {
		return 50;
	}

	public static int count( Hero hero ){
		return hero.lvl >= 15 ? 2 : 1;
	}

	public static int duration( Hero hero ){
		return 25 + 10*hero.pointsInTalent(Talent.SP_RESTLESS_DEAD);
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		SpiritistHooks.Graves graves = Buff.affect(hero, SpiritistHooks.Graves.class);
		ArrayList<Integer> found = graves.near(hero, RANGE);
		if (found.isEmpty()){
			GLog.w(Messages.get(this, "no_corpse"));
			return;
		}

		int raised = 0;
		ArrayList<Integer> used = new ArrayList<>();
		for (int i : found){
			if (raised >= count(hero)) break;
			used.add(i);
			Class<?> cls = Reflection.forName(graves.classes.get(i));
			if (cls == null || !Mob.class.isAssignableFrom(cls)) continue;
			Mob m = (Mob) Reflection.newInstance(cls);
			if (m == null) continue;
			int cell = SpiritistHooks.spawnCell(graves.cells.get(i), true);
			if (cell == -1) continue;
			m.pos = cell;
			m.state = m.HUNTING;
			GameScene.add(m);
			Buff.affect(m, Raised.class).set(duration(hero), ChampionHooks.indexOf(hero));
			Dungeon.level.occupyCell(m);
			if (Dungeon.level.heroFOV[cell]) CellEmitter.get(cell).burst(ShadowParticle.UP, 12);
			raised++;
		}
		//remove from the end or the indexes shift. classic
		for (int k = used.size()-1; k >= 0; k--) graves.remove(used.get(k));

		if (raised == 0){
			GLog.w(Messages.get(this, "no_corpse"));
			return;
		}
		Sample.INSTANCE.play(Assets.Sounds.BONES);
		Dungeon.observe();

		ChampionHooks.operateVisual(hero, hero.pos);
		Invisibility.dispel(hero);
		startCooldown(hero);
		hero.spendAndNext(1f);
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", RANGE, (int) SpiritistHooks.Graves.FRESH, count(hero), duration(hero));
	}

	//zombie buff, temp
	public static class Raised extends AllyBuff {

		{
			type = buffType.POSITIVE;
		}

		public float left = 0;
		public int owner = 0;

		public void set( float turns, int owner ){
			left = turns;
			this.owner = owner;
		}

		@Override
		public boolean act() {
			left -= TICK;
			if (left <= 0){
				if (target.isAlive()){
					SpiritistHooks.onSummonDeath(ChampionHooks.heroByIndex(owner));
					target.die(null);
				}
				detach();
				return true;
			}
			spend(TICK);
			return true;
		}

		@Override
		public void fx( boolean on ) {
			if (target.sprite == null) return;
			if (on) target.sprite.aura(0x55DD99, 5);
			else target.sprite.clearAura();
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", Math.round(left));
		}

		private static final String LEFT  = "left";
		private static final String OWNER = "owner";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(LEFT, left);
			bundle.put(OWNER, owner);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			left = bundle.getFloat(LEFT);
			owner = bundle.getInt(OWNER);
		}
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}
}
