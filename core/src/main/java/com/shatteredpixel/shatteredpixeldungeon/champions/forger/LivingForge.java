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

package com.shatteredpixel.shatteredpixeldungeon.champions.forger;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

//forger passive. natural armor + the forge: once a floor (twice for master smith) +1 lvl to his
//or an adjacent ally's weapon/armor until next floor.
//player picks go through kit window -> cell select -> WndOptions, all stuff coop already replicates,
//so no new netcode. also tracks the living mountain hp
public class LivingForge extends Buff {

	{
		type = buffType.POSITIVE;
		revivePersists = true;
	}

	public int depth = -1;      //floor 'used' belongs to
	public int used = 0;
	public int mountainBoost = 0; //living mountain hp currently in HTBoost

	public static LivingForge of( Hero hero ){
		return hero.buff(LivingForge.class);
	}

	public static int maxUses( Hero hero ){
		return hero.subClass == HeroSubClass.MASTERSMITH ? 2 : 1;
	}

	public static int usesLeft( Hero hero ){
		LivingForge f = of(hero);
		if (f == null || f.depth != Dungeon.depth) return maxUses(hero);
		return Math.max(0, maxUses(hero) - f.used);
	}

	public static int mountainHealth( Hero hero ){
		if (hero.subClass != HeroSubClass.LIVINGMOUNTAIN) return 0;
		return Math.round((20 + 5*(hero.lvl-1)) * 0.15f);
	}

	@Override
	public boolean act() {
		Hero hero = (Hero) target;
		int want = mountainHealth(hero);
		if (want != mountainBoost){
			hero.HTBoost += want - mountainBoost;
			mountainBoost = want;
			hero.updateHT(true);
		}
		spend(TICK);
		return true;
	}

	//forge picked in the kit window -> who gets it?
	public static void begin( final Hero forger ){
		if (usesLeft(forger) <= 0){
			GLog.w(Messages.get(LivingForge.class, "no_uses"));
			return;
		}
		GameScene.selectCell(new CellSelector.Listener() {
			@Override
			public void onSelect( Integer cell ) {
				if (cell == null) return;
				Char ch = Actor.findChar(cell);
				if (!(ch instanceof Hero) || !ch.isAlive()
						|| (ch != forger && !Dungeon.level.adjacent(forger.pos, ch.pos))){
					GLog.w(Messages.get(LivingForge.class, "no_target"));
					return;
				}
				chooseSlot(forger, (Hero) ch);
			}

			@Override
			public String prompt() {
				return Messages.get(LivingForge.class, "prompt");
			}
		});
	}

	private static void chooseSlot( final Hero forger, final Hero target ){
		if (usesLeft(forger) <= 0) return;
		final boolean hasWeapon = target.belongings.weapon != null;
		final boolean hasArmor = target.belongings.armor != null;
		if (!hasWeapon && !hasArmor){
			GLog.w(Messages.get(LivingForge.class, "nothing"));
			return;
		}
		String weaponName = hasWeapon ? Messages.titleCase(target.belongings.weapon.name()) : Messages.get(LivingForge.class, "none");
		String armorName = hasArmor ? Messages.titleCase(target.belongings.armor.name()) : Messages.get(LivingForge.class, "none");
		GameScene.show(new WndOptions(Messages.titleCase(Messages.get(LivingForge.class, "name")),
				Messages.get(LivingForge.class, "choose"), weaponName, armorName){
			@Override
			protected void onSelect( int index ) {
				if (index == 0 && hasWeapon) forge(forger, target, true);
				else if (index == 1 && hasArmor) forge(forger, target, false);
			}
		});
	}

	public static void forge( Hero forger, Hero target, boolean weapon ){
		LivingForge f = of(forger);
		if (f == null || usesLeft(forger) <= 0 || !forger.isAlive() || !target.isAlive()) return;
		if (f.depth != Dungeon.depth){
			f.depth = Dungeon.depth;
			f.used = 0;
		}
		f.used++;

		Boost b = Buff.affect(target, Boost.class);
		if (b.depth != Dungeon.depth){
			b.depth = Dungeon.depth;
			b.weapon = b.armor = 0;
		}
		if (weapon) b.weapon++; else b.armor++;
		b.molten = Math.max(b.molten, 10*forger.pointsInTalent(Talent.MS_MOLTEN_GIFT));

		int shield = 4*forger.pointsInTalent(Talent.FOR_TEMPER);
		if (shield > 0) ForgerHooks.shield(target, shield + forger.lvl/3);
		if (forger.hasTalent(Talent.MS_FINE_TEMPER)){
			ChampionHooks.reduceCooldowns(forger, 3*forger.pointsInTalent(Talent.MS_FINE_TEMPER));
		}

		if (target.sprite != null){
			target.sprite.emitter().burst(Speck.factory(Speck.FORGE), 8);
			target.sprite.showStatus(CharSprite.POSITIVE, Messages.get(LivingForge.class, "forged"));
		}
		GLog.p(Messages.get(LivingForge.class, weapon ? "forged_weapon" : "forged_armor", Messages.titleCase(target.name())));
		ChampionHooks.operateVisual(forger, target.pos);
		Item.updateQuickslot();
		float time = Math.max(0.1f, 1f - 0.4f*forger.pointsInTalent(Talent.MS_QUICK_ANVIL));
		forger.spendAndNext(time);
	}

	//called from Item.buffedLvl
	public static int bonus( Item item ){
		if (item == null) return 0;
		if (!(item instanceof Weapon
				|| item instanceof Armor)) return 0;
		if (Party.active()){
			for (Hero h : Party.heroes()){
				int b = boostOf(h, item);
				if (b != 0) return b;
			}
			return 0;
		}
		return Dungeon.hero == null ? 0 : boostOf(Dungeon.hero, item);
	}

	private static int boostOf( Hero hero, Item item ){
		Boost b = hero.buff(Boost.class);
		if (b == null || b.depth != Dungeon.depth) return 0;
		if (item == hero.belongings.weapon) return b.weapon;
		if (item == hero.belongings.armor) return b.armor;
		return 0;
	}

	//molten gift dmg factor
	public static float damageFactor( Char attacker ){
		Boost b = attacker.buff(Boost.class);
		if (b == null || b.depth != Dungeon.depth || b.weapon <= 0 || b.molten <= 0) return 1f;
		return 1f + b.molten/100f;
	}

	private static final String DEPTH = "depth";
	private static final String USED = "used";
	private static final String MOUNTAIN = "mountain";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(DEPTH, depth);
		bundle.put(USED, used);
		bundle.put(MOUNTAIN, mountainBoost);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		depth = bundle.getInt(DEPTH);
		used = bundle.getInt(USED);
		mountainBoost = bundle.getInt(MOUNTAIN);
	}

	//+1 per forge, solo este piso
	public static class Boost extends Buff {

		{
			type = buffType.POSITIVE;
			revivePersists = true;
		}

		public int depth = -1;
		public int weapon = 0;
		public int armor = 0;
		public int molten = 0;  //extra dmg %

		@Override
		public boolean act() {
			if (depth != Dungeon.depth){
				detach();
				Item.updateQuickslot();
				return true;
			}
			spend(TICK);
			return true;
		}

		@Override
		public int icon() {
			return BuffIndicator.UPGRADE;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(1f, 0.6f, 0.2f);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", weapon, armor);
		}

		private static final String DEPTH = "depth";
		private static final String WEAPON = "weapon";
		private static final String ARMOR = "armor";
		private static final String MOLTEN = "molten";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(DEPTH, depth);
			bundle.put(WEAPON, weapon);
			bundle.put(ARMOR, armor);
			bundle.put(MOLTEN, molten);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			depth = bundle.getInt(DEPTH);
			weapon = bundle.getInt(WEAPON);
			armor = bundle.getInt(ARMOR);
			molten = bundle.getInt(MOLTEN);
		}
	}
}
