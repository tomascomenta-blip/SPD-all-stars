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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.beastmaster;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Bat;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Crab;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Spinner;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forest.Boar;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.kingdom.KingdomMobs;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.starship.StarshipMobs;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.undersea.UnderseaMobs;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

//taming. get an animal under 25% hp and it's yours. one pet at a time (old one leaves),
//heals to 50%, follows you, takes the stairs w/ you
public class Taming {

	public static final int RANGE = 3;

	public static boolean animal( Char ch ){
		return ch instanceof Rat || ch instanceof Crab || ch instanceof Bat || ch instanceof Spinner
				|| ch instanceof Boar || ch instanceof StarshipMobs.Varkolyn
				|| ch instanceof KingdomMobs.RoyalSlime || ch instanceof KingdomMobs.SmallSlime
				|| ch instanceof UnderseaMobs.Subject001 || ch instanceof UnderseaMobs.Subject004;
	}

	//null = ok, otherwise the reason
	public static String problem( Hero hero, Char ch ){
		if (!(ch instanceof Mob) || ch.alignment != Char.Alignment.ENEMY || !ch.isAlive()) return Messages.get(Taming.class, "no_target");
		if (!animal(ch) || Char.hasProp(ch, Char.Property.BOSS) || Char.hasProp(ch, Char.Property.MINIBOSS)) return Messages.get(Taming.class, "not_animal");
		if (ch.HP*4 > ch.HT) return Messages.get(Taming.class, "too_healthy");
		if (Dungeon.level.distance(hero.pos, ch.pos) > RANGE) return Messages.get(Taming.class, "too_far");
		return null;
	}

	public static void tame( Hero hero, Mob mob ){
		int owner = ChampionHooks.indexOf(hero);
		//bye old pet :(
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			Tamed t = m.buff(Tamed.class);
			if (t != null && t.owner == owner && m != mob){
				if (m.sprite != null) m.sprite.emitter().burst(Speck.factory(Speck.WOOL), 6);
				m.destroy();
				if (m.sprite != null) m.sprite.killAndErase();
			}
		}
		Buff.affect(mob, Tamed.class).owner = owner;
		mob.makeIntelligentAlly();
		mob.HP = Math.max(mob.HP, mob.HT/2);
		mob.state = mob.WANDERING;
		if (mob.sprite != null){
			mob.sprite.emitter().burst(Speck.factory(Speck.HEART), 8);
		}
		Sample.INSTANCE.play(Assets.Sounds.CHARMS);
		GLog.p(Messages.get(Taming.class, "tamed", mob.name()));
		CoopLog.line("tamed " + mob.getClass().getSimpleName() + " at " + mob.pos);
		hero.spendAndNext(Actor.TICK);
	}

	//tamed for good
	public static class Tamed extends AllyBuff {

		{
			type = buffType.POSITIVE;
		}

		public int owner = 0;

		@Override
		public boolean attachTo( Char target ) {
			if (super.attachTo(target)){
				if (target instanceof Mob) ((Mob) target).makeIntelligentAlly();
				return true;
			}
			return false;
		}

		@Override
		public boolean act() {
			spend(TICK);
			Hero h = ChampionHooks.heroByIndex(owner);
			if (h != null && h.isAlive() && target instanceof Mob){
				Mob m = (Mob) target;
				if (m.state != m.HUNTING && Dungeon.level.distance(m.pos, h.pos) > 2) m.followTo(h.pos);
			}
			return true;
		}

		@Override
		public int icon() {
			return BuffIndicator.HEART;
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("owner", owner);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			owner = bundle.getInt("owner");
		}
	}
}
