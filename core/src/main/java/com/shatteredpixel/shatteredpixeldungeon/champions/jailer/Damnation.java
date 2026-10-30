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

package com.shatteredpixel.shatteredpixeldungeon.champions.jailer;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

//jailer passive: deaths in sight can drop a soul, souls = perma armor + ability dmg
public class Damnation extends Buff {

	{
		type = buffType.POSITIVE;
		revivePersists = true;
	}

	public int souls = 0;

	public static int souls( Hero hero ){
		Damnation d = hero.buff(Damnation.class);
		return d == null ? 0 : d.souls;
	}

	public static int armor( Hero hero ){
		float a = souls(hero) / 4f;
		a *= 1f + 0.1f*hero.pointsInTalent(Talent.SR_SOUL_ARMOR);
		return (int) a;
	}

	public static int drBonus( Hero hero ){
		int a = armor(hero);
		return a > 0 ? Random.NormalIntRange(0, a) : 0;
	}

	public static int abilityBonus( Hero hero ){
		return souls(hero) / 10;
	}

	public static float dropChance( Hero hero ){
		float c = 0.25f + 0.10f*hero.pointsInTalent(Talent.JAI_HUNGRY_SOULS);
		if (hero.subClass == HeroSubClass.SOULREAPER) c *= 2f;
		return Math.min(1f, c);
	}

	public static void onEnemyDeath( Mob mob ){
		if (mob.EXP <= 0 && !Char.hasProp(mob, Char.Property.BOSS) && !Char.hasProp(mob, Char.Property.MINIBOSS)) return;
		ArrayList<Hero> heroes = new ArrayList<>(Party.living());
		for (Hero h : heroes){
			if (h.heroClass != HeroClass.JAILER) continue;
			//shared party vision, but has to be within 8 of the jailer
			if (mob.pos < 0 || !Dungeon.level.heroFOV[mob.pos] || Dungeon.level.distance(h.pos, mob.pos) > 8) continue;
			boolean big = Char.hasProp(mob, Char.Property.BOSS) || Char.hasProp(mob, Char.Property.MINIBOSS);
			if (big || Random.Float() < dropChance(h)){
				int amount = Char.hasProp(mob, Char.Property.BOSS) ? 3 : 1;
				for (int i = 0; i < amount; i++){
					Heap heap = Dungeon.level.drop(new Soul(), mob.pos);
					if (heap.sprite != null) heap.sprite.drop();
				}
			}
			return;
		}
	}

	public static void gain( Hero hero, int amount ){
		Damnation d = Buff.affect(hero, Damnation.class);
		d.souls += amount;
		if (hero.sprite != null){
			hero.sprite.showStatus(0x66FF99, Messages.get(Damnation.class, "soul", d.souls));
		}
		Sample.INSTANCE.play(Assets.Sounds.CHARGEUP, 0.6f, 1.4f);
		if (hero.subClass == HeroSubClass.SOULREAPER || hero.hasTalent(Talent.JAI_SOUL_MEND)){
			int heal = (hero.subClass == HeroSubClass.SOULREAPER ? 1 : 0) + hero.pointsInTalent(Talent.JAI_SOUL_MEND);
			if (heal > 0 && hero.HP < hero.HT){
				hero.HP = Math.min(hero.HT, hero.HP + heal);
				if (hero.sprite != null) hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
			}
		}
		BuffIndicator.refreshHero();
	}

	//only the jailer can pick them up
	public static void gatherAt( Hero hero ){
		if (hero.heroClass != HeroClass.JAILER) return;
		Heap heap = Dungeon.level.heaps.get(hero.pos);
		if (heap == null) return;
		int count = 0;
		while (!heap.isEmpty() && heap.peek() instanceof Soul){
			count += heap.peek().quantity();
			heap.pickUp();
			if (heap.isEmpty()) break;
		}
		if (count > 0) gain(hero, count);
	}

	@Override
	public boolean act() {
		if (!(target instanceof Hero) || ((Hero) target).heroClass != HeroClass.JAILER){
			detach();
			return true;
		}
		spend(TICK);
		return true;
	}

	@Override
	public int icon() {
		return BuffIndicator.ARMOR;
	}

	@Override
	public void tintIcon( Image icon ) {
		icon.hardlight(0.3f, 1f, 0.6f);
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(souls);
	}

	@Override
	public String name() {
		return Messages.get(Damnation.class, "name");
	}

	@Override
	public String desc() {
		int a = target instanceof Hero ? armor((Hero) target) : souls/3;
		int b = target instanceof Hero ? abilityBonus((Hero) target) : souls/10;
		return Messages.get(Damnation.class, "desc", souls, a, b);
	}

	private static final String SOULS = "souls";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(SOULS, souls);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		souls = bundle.getInt(SOULS);
	}
}
