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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.gunslinger;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMarks;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.guns.SKGun;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

//gunslinger hooks
//passive "streak": 3 kills in quick succession -> duel mode, 2x cooldowns + revolver hits harder
public class GunslingerHooks {

	public static final int STREAK = 3;

	//dual wield: firing one gun also fires the next charged gun in the bag at the same cell (costs a charge)
	private static boolean dual = false;

	public static void secondGun( Wand fired, Hero hero,
	                              Ballistica shot ){
		if (dual || hero == null || hero.heroClass != HeroClass.GUNSLINGER) return;
		if (!(fired instanceof SKGun)) return;
		SKGun other = null;
		//backpack order, NOT quickslots. quickslots are per device, the backpack is shared state
		for (SKGun g : hero.belongings.getAllItems(SKGun.class)){
			if (g != fired && g.curCharges > 0){
				other = g;
				break;
			}
		}
		if (other == null) return;
		int target = shot.collisionPos;
		Ballistica second =
				new Ballistica(hero.pos, target, other.collisionProperties(target));
		dual = true;
		try {
			other.onZap(second);
			other.curCharges--;
			other.updateQuickslot();
		} finally {
			dual = false;
		}
		SKMarks.shotFx(hero, second.collisionPos, other.shotImageForFx());
		CoopLog.line("second gun " + other.getClass().getSimpleName() + " charges=" + other.curCharges);
	}
	public static final int STREAK_WINDOW = 6;

	public static void initHero( Hero hero ){
		(hero.belongings.weapon = new Dagger()).identify();
		hero.belongings.weapon.activate(hero);

		GunslingerKit kit = new GunslingerKit();
		kit.identify().collect();
		JusticeRevolver gun = new JusticeRevolver();
		gun.identify().collect();

		Dungeon.quickslot.setSlot(0, kit);
		Dungeon.quickslot.setSlot(1, gun);

		new PotionOfHealing().identify();
		new ScrollOfRecharging().identify();
	}

	public static boolean is( Char ch ){
		return ch instanceof Hero && ((Hero) ch).heroClass == HeroClass.GUNSLINGER;
	}

	public static int drBonus( Hero hero ){
		return Random.NormalIntRange(0, hero.pointsInTalent(Talent.GS_THICK_COAT));
	}

	public static float itemIDSpeedFactor( Hero hero, Item item ){
		if (hero.heroClass == HeroClass.GUNSLINGER && (item instanceof Weapon || item instanceof Armor)){
			return 1f + 0.75f*hero.pointsInTalent(Talent.GS_APPRAISAL);
		}
		return 1f;
	}

	public static void onFoodEaten( Hero hero ){
		if (!hero.hasTalent(Talent.GS_GUN_OIL)) return;
		JusticeRevolver gun = hero.belongings.getItem(JusticeRevolver.class);
		if (gun != null){
			gun.gainCharge(hero.pointsInTalent(Talent.GS_GUN_OIL), true);
			Item.updateQuickslot();
		}
	}

	public static int duelTurns( Hero hero ){
		int t = 8 + 3*hero.pointsInTalent(Talent.GS_HOT_STREAK);
		if (hero.subClass == HeroSubClass.QUICKDRAW) t += 2*hero.pointsInTalent(Talent.QD_DUELIST);
		return t;
	}

	//which gunslinger got the kill (si alguno)
	private static Hero killer( Object cause ){
		Hero h = null;
		if (cause instanceof Hero){
			h = (Hero) cause;
		} else if (cause instanceof Item){
			h = ChampionHooks.ownerOf((Item) cause);
		} else if (cause == GunSpin.INSTANCE || cause == HighNoon.INSTANCE || cause == AirStrike.INSTANCE
				|| cause instanceof LeadRain){
			h = Dungeon.hero;
		}
		return h != null && h.heroClass == HeroClass.GUNSLINGER && h.isAlive() ? h : null;
	}

	public static void onMobDeath( Mob mob, Object cause ){
		Hero k = killer(cause);
		if (k != null) Buff.affect(k, Streak.class).kill(k);

		BountyTag.Mark bounty = mob.buff(BountyTag.Mark.class);
		if (bounty != null){
			Hero hunter = ChampionHooks.heroByIndex(bounty.owner);
			int gold = BountyTag.reward(hunter);
			Dungeon.level.drop(new Gold(gold), mob.pos).sprite.drop();
			if (hunter != null && hunter.isAlive()){
				if (hunter.subClass == HeroSubClass.BOUNTY_HUNTER && hunter.hasTalent(Talent.BH_COLLECTOR)){
					ChampionHooks.heal(hunter, 3*hunter.pointsInTalent(Talent.BH_COLLECTOR));
				}
				//bounty jumps to the closest visible enemy
				Char next = null;
				int best = Integer.MAX_VALUE;
				for (Char ch : Actor.chars()){
					if (ch == mob || ch.alignment != Char.Alignment.ENEMY || !ch.isAlive() || !Dungeon.level.heroFOV[ch.pos]) continue;
					int d = Dungeon.level.distance(mob.pos, ch.pos);
					if (d <= BountyTag.RANGE && d < best){
						best = d;
						next = ch;
					}
				}
				if (next != null && bounty.cooldown() > 1){
					BountyTag.mark(hunter, next, bounty.cooldown());
					GLog.i(Messages.get(BountyTag.class, "moved"));
				}
			}
		}
	}

	//3rd kill = duel
	public static class Streak extends Buff {

		{
			type = buffType.NEUTRAL;
			revivePersists = true;
		}

		public int kills = 0;
		public float left = 0;

		public void kill( Hero hero ){
			kills++;
			left = STREAK_WINDOW;
			if (kills >= STREAK){
				kills = 0;
				Buff.affect(hero, Duel.class).set(duelTurns(hero));
				if (hero.sprite != null) hero.sprite.showStatus(CharSprite.POSITIVE, Messages.get(GunslingerHooks.class, "duel"));
				if (hero.subClass == HeroSubClass.QUICKDRAW && hero.hasTalent(Talent.QD_SPEEDLOADER)){
					JusticeRevolver gun = hero.belongings.getItem(JusticeRevolver.class);
					if (gun != null){
						gun.gainCharge(hero.pointsInTalent(Talent.QD_SPEEDLOADER), true);
						Item.updateQuickslot();
					}
				}
			}
		}

		@Override
		public boolean act() {
			if (left > 0){
				left -= TICK;
				if (left <= 0) kills = 0;
			}
			spend(TICK);
			return true;
		}

		private static final String KILLS = "kills";
		private static final String LEFT  = "left";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(KILLS, kills);
			bundle.put(LEFT, left);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			kills = bundle.getInt(KILLS);
			left = bundle.getFloat(LEFT);
		}
	}

	//duel: 2x cd speed, revolver +25%
	public static class Duel extends Buff {

		{
			type = buffType.POSITIVE;
			announced = true;
		}

		public static final float DAMAGE = 1.25f;

		public float left = 0;

		public void set( float turns ){
			left = Math.max(left, turns);
		}

		@Override
		public boolean act() {
			if (target instanceof Hero) ChampionHooks.reduceCooldowns((Hero) target, 1f);
			left -= TICK;
			if (left <= 0){
				detach();
			} else {
				spend(TICK);
			}
			return true;
		}

		@Override
		public int icon() {
			return BuffIndicator.FURY;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(1f, 0.8f, 0.2f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (12 - left) / 12f);
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString(Math.round(left));
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", Math.round((DAMAGE-1)*100), Math.round(left));
		}

		private static final String LEFT = "left";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(LEFT, left);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			left = bundle.getFloat(LEFT);
		}
	}

	public static boolean inDuel( Char ch ){
		return ch != null && ch.buff(Duel.class) != null;
	}

	//ability shot dmg mult (duel + hair trigger)
	public static float shotFactor( Hero hero ){
		float f = inDuel(hero) ? Duel.DAMAGE : 1f;
		if (hero.subClass == HeroSubClass.QUICKDRAW) f *= 1f + 0.1f*hero.pointsInTalent(Talent.QD_HAIR_TRIGGER);
		return f;
	}

	static ChampionAbility[] abilities(){
		return new ChampionAbility[]{ GunSpin.INSTANCE, BountyTag.INSTANCE, AirStrike.INSTANCE, HighNoon.INSTANCE };
	}
}
