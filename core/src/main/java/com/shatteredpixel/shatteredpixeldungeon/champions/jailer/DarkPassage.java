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
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;

//W: lantern throw. friend at the spot gets yanked next to the jailer, both get a shield. "click the lantern"
public class DarkPassage extends ChampionAbility {

	public static final DarkPassage INSTANCE = new DarkPassage();

	@Override public String slot() { return "W"; }
	@Override public int unlockLevel() { return 2; }
	@Override public int icon() { return HeroIcon.LOL_DARK_PASSAGE; }
	@Override public int color() { return 0x33FF88; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }
	@Override public boolean targeted() { return true; }

	@Override
	public float cooldown( Hero hero ) {
		float cd = 20;
		if (hero.subClass == HeroSubClass.LANTERNKEEPER) cd *= 0.6f;
		return cd;
	}

	public static int range( Hero hero ){
		return 5 + (hero.subClass == HeroSubClass.LANTERNKEEPER ? 3 : 0);
	}

	public static int shield( Hero hero ){
		float s = 3 + hero.lvl/2f + Damnation.souls(hero)/4f;
		s *= 1f + 0.25f*hero.pointsInTalent(Talent.JAI_HEAVY_LANTERN);
		if (hero.subClass == HeroSubClass.LANTERNKEEPER) s *= 2f;
		return Math.round(s);
	}

	@Override
	public String prompt() {
		return Messages.get(this, "prompt");
	}

	//other hero first, then allies
	private static Char friendAt( Hero hero, int cell ){
		Char best = null;
		for (int n : PathFinder.NEIGHBOURS9){
			Char ch = Actor.findChar(cell + n);
			if (ch == null || ch == hero || !ch.isAlive() || ch.alignment != Char.Alignment.ALLY) continue;
			if (Char.hasProp(ch, Char.Property.IMMOVABLE)) continue;
			if (Party.isPartyHero(ch)) return ch;
			if (best == null) best = ch;
		}
		return best;
	}

	@Override
	public void activate( final Hero hero, ChampionKit kit, Integer target ) {
		if (target == null) return;
		if (Dungeon.level.distance(hero.pos, target) > range(hero) || !Dungeon.level.heroFOV[target]){
			GLog.w(Messages.get(ChampionAbility.class, "too_far"));
			return;
		}
		final int cell = target;
		hero.busy();
		hero.sprite.zap(cell);
		Sample.INSTANCE.play(Assets.Sounds.CHAINS, 1f, 1.5f);
		MagicMissile.boltFromChar(hero.sprite.parent, MagicMissile.WARD, hero.sprite, cell, new Callback() {
			@Override
			public void call() {
				land(hero, cell);
				startCooldown(hero);
				hero.spendAndNext(1f);
			}
		});
	}

	private void land( Hero hero, int cell ){
		int amount = shield(hero);
		Char friend = friendAt(hero, cell);
		if (friend != null){
			int dest = Dungeon.level.adjacent(friend.pos, hero.pos) ? friend.pos : ChampionHooks.landingNextTo(friend, hero.pos);
			if (dest != -1 && dest != friend.pos){
				if (friend instanceof Hero) ((Hero) friend).interrupt();
				ScrollOfTeleportation.appear(friend, dest);
				Dungeon.level.occupyCell(friend);
			}
			giveShield(friend, amount);
			if (hero.subClass == HeroSubClass.LANTERNKEEPER){
				ChampionHooks.cleanse(friend);
				int heal = 3*hero.pointsInTalent(Talent.LK_LIGHT_WARD);
				if (heal > 0) ChampionHooks.heal(friend, heal);
			}
			GLog.p(Messages.get(this, "rescued", friend.name()));
		} else if (hero.hasTalent(Talent.LK_ESCAPE)
				&& Dungeon.level.distance(hero.pos, cell) <= 2 + hero.pointsInTalent(Talent.LK_ESCAPE)
				&& Actor.findChar(cell) == null && Dungeon.level.passable[cell] && !hero.rooted){
			ScrollOfTeleportation.appear(hero, cell);
			Dungeon.level.occupyCell(hero);
		}
		giveShield(hero, amount);
		if (hero.subClass == HeroSubClass.LANTERNKEEPER && hero.hasTalent(Talent.LK_LIGHT_WARD)){
			ChampionHooks.heal(hero, 3*hero.pointsInTalent(Talent.LK_LIGHT_WARD));
		}
		Dungeon.observe();
		GameScene.updateFog();
	}

	private static void giveShield( Char ch, int amount ){
		Buff.affect(ch, Barrier.class).incShield(amount);
		if (ch.sprite != null){
			ch.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(amount), FloatingText.SHIELDING);
		}
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", range(hero), shield(hero));
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}
}
