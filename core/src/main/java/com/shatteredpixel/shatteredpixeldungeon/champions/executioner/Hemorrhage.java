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

package com.shatteredpixel.shatteredpixeldungeon.champions.executioner;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Splash;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.PointF;

//stacking bleed. each stack ticks every turn, all of it drops if you stop hitting for a few turns
public class Hemorrhage extends Buff implements Buff.DOTbuff {

	{
		type = buffType.NEGATIVE;
		announced = true;
	}

	public static final int MAX_STACKS = 5;

	public int stacks = 0;
	public int left = 0;
	//per stack per turn, from the applier's level
	public float power = 0.5f;
	//talent, <50% hp targets
	public int lowHpBonus = 0;

	public static int baseDuration( Hero hero ){
		return 5 + 2*hero.pointsInTalent(Talent.EXE_DEEP_WOUNDS);
	}

	//true = maxed out
	public static boolean apply( Hero hero, Char target, int amount ){
		if (!target.isAlive() || target.isImmune(Hemorrhage.class)) return false;
		Hemorrhage h = Buff.affect(target, Hemorrhage.class);
		boolean wasMax = h.stacks >= MAX_STACKS;
		if (hero.buff(ExecutionerMight.class) != null){
			amount = MAX_STACKS;
		}
		h.stacks = Math.min(MAX_STACKS, h.stacks + amount);
		h.left = Math.max(h.left, baseDuration(hero));
		h.power = 0.25f + hero.lvl / 14f;
		h.lowHpBonus = hero.pointsInTalent(Talent.HS_BLOODY_AXE);
		target.needsIncomingDOTUpdate = true;
		if (h.stacks >= MAX_STACKS && !wasMax){
			ExecutionerMight.trigger(hero);
		}
		return h.stacks >= MAX_STACKS;
	}

	public static int stacksOn( Char target ){
		Hemorrhage h = target.buff(Hemorrhage.class);
		return h == null ? 0 : h.stacks;
	}

	public int damagePerTurn(){
		int dmg = (int)Math.ceil(stacks * power);
		if (lowHpBonus > 0 && target != null && target.HP < target.HT/2){
			dmg += lowHpBonus;
		}
		return dmg;
	}

	@Override
	public boolean act() {
		if (target.isAlive()){
			int dmg = damagePerTurn();
			if (dmg > 0){
				target.damage(dmg, this);
				if (target.sprite != null && target.sprite.visible){
					Splash.at(target.sprite.center(), -PointF.PI / 2, PointF.PI / 6,
							target.sprite.blood(), Math.min(2 + stacks, 8));
				}
			}
			left--;
			if (left <= 0 || !target.isAlive()){
				detach();
			} else {
				target.needsIncomingDOTUpdate = true;
				spend(TICK);
			}
		} else {
			detach();
		}
		return true;
	}

	@Override
	public int totalIncomingDMG() {
		return damagePerTurn() * Math.max(0, left);
	}

	@Override
	public int icon() {
		return BuffIndicator.BLEEDING;
	}

	@Override
	public void tintIcon( Image icon ) {
		icon.hardlight(0.8f, 0.1f, 0.1f);
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(stacks);
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, 1f - stacks / (float)MAX_STACKS);
	}

	@Override
	public String name() {
		return Messages.get(this, "name");
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", stacks, damagePerTurn(), left);
	}

	private static final String STACKS = "stacks";
	private static final String LEFT = "left";
	private static final String POWER = "power";
	private static final String LOW_HP = "low_hp";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(STACKS, stacks);
		bundle.put(LEFT, left);
		bundle.put(POWER, power);
		bundle.put(LOW_HP, lowHpBonus);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		stacks = bundle.getInt(STACKS);
		left = bundle.getInt(LEFT);
		power = bundle.getFloat(POWER);
		lowHpBonus = bundle.getInt(LOW_HP);
	}

	//5 stacks -> next hits go straight to max. qué gusto
	public static class ExecutionerMight extends FlavourBuff {

		{
			type = buffType.POSITIVE;
			announced = true;
		}

		public static float duration( Hero hero ){
			return 5f + 2*hero.pointsInTalent(Talent.EXE_RELENTLESS);
		}

		public static void trigger( Hero hero ){
			boolean fresh = hero.buff(ExecutionerMight.class) == null;
			Buff.prolong(hero, ExecutionerMight.class, duration(hero));
			if (fresh && hero.sprite != null){
				hero.sprite.showStatus(CharSprite.NEGATIVE, Messages.get(ExecutionerMight.class, "name"));
			}
			if (fresh && hero.hasTalent(Talent.EXE_INTIMIDATE)){
				for (int n : PathFinder.NEIGHBOURS8){
					Char ch = Actor.findChar(hero.pos + n);
					if (ch != null && ch.alignment == Char.Alignment.ENEMY){
						Buff.prolong(ch, Weakness.class, 1 + 2*hero.pointsInTalent(Talent.EXE_INTIMIDATE));
					}
				}
			}
			if (fresh && hero.subClass == HeroSubClass.CONQUEROR){
				int heal = Math.round((hero.HT - hero.HP) * 0.15f);
				if (heal > 0){
					hero.HP += heal;
					if (hero.sprite != null) hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal),
							FloatingText.HEALING);
				}
			}
		}

		@Override
		public int icon() {
			return BuffIndicator.FURY;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(1f, 0.15f, 0.15f);
		}

		@Override
		public float iconFadePercent() {
			float max = target instanceof Hero ? duration((Hero) target) : 5f;
			return Math.max(0, (max - visualcooldown()) / max);
		}

		@Override
		public void fx( boolean on ) {
			if (target.sprite == null) return;
			if (on) target.sprite.aura(0xCC1111, 5);
			else    target.sprite.clearAura();
		}
	}
}
