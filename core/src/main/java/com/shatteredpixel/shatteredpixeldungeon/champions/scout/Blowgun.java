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

package com.shatteredpixel.shatteredpixeldungeon.champions.scout;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.effects.Splash;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

import java.util.ArrayList;

//blowgun. infinite darts, scales with lvl. basically the spirit bow but tiny
public class Blowgun extends Weapon {

	public static final String AC_SHOOT = "SHOOT";

	{
		image = ItemSpriteSheet.BLOWGUN;
		defaultAction = AC_SHOOT;
		usesTargeting = true;
		unique = true;
		bones = false;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions(hero);
		actions.remove(AC_EQUIP);
		actions.add(AC_SHOOT);
		return actions;
	}

	@Override
	public String actionName( String action, Hero hero ) {
		if (action.equals(AC_SHOOT)) return Messages.get(Blowgun.class, "ac_shoot");
		return super.actionName(action, hero);
	}

	@Override
	public void execute( final Hero hero, String action ) {
		super.execute(hero, action);
		if (action.equals(AC_SHOOT)) {
			curUser = hero;
			curItem = this;
			GameScene.selectCell(new CellSelector.Listener() {
				@Override
				public void onSelect( Integer target ) {
					if (target != null) {
						knockDart().cast(hero, target);
					}
				}

				@Override
				public String prompt() {
					return Messages.get(Blowgun.class, "prompt");
				}
			});
		}
	}

	private Hero owner(){
		Hero h = null;
		try {
			h = ChampionHooks.ownerOf(this);
		} catch (Exception e){
			//UI reading while the game thread edits the backpack. catch and move on
		}
		return h != null ? h : Dungeon.hero;
	}

	@Override
	public int STRReq( int lvl ) {
		return STRReq(1, lvl);
	}

	@Override
	public int min( int lvl ) {
		Hero h = owner();
		return 1 + (h == null ? 0 : h.lvl / 5);
	}

	@Override
	public int max( int lvl ) {
		Hero h = owner();
		return 5 + (h == null ? 0 : h.lvl / 3);
	}

	@Override
	public int damageRoll( Char owner ) {
		int damage = augment.damageFactor(super.damageRoll(owner));
		if (owner instanceof Hero) {
			int exStr = ((Hero)owner).STR() - STRReq();
			if (exStr > 0) {
				damage += Hero.heroDamageIntRange(0, exStr);
			}
		}
		return damage;
	}

	@Override
	protected float baseDelay( Char owner ) {
		float delay = super.baseDelay(owner);
		if (owner instanceof Hero && ((Hero) owner).subClass == HeroSubClass.SKIRMISHER){
			delay *= 0.5f;
		}
		return delay;
	}

	@Override
	public int level() {
		Hero h = owner();
		return h == null ? 0 : h.lvl / 5;
	}

	@Override
	public int buffedLvl() {
		return level();
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public String info() {
		String info = desc();
		info += "\n\n" + Messages.get(Blowgun.class, "stats", Math.round(augment.damageFactor(min())),
				Math.round(augment.damageFactor(max())), STRReq());
		if (enchantment != null && (cursedKnown || !enchantment.curse())){
			info += "\n\n" + Messages.capitalize(Messages.get(Weapon.class, "enchanted", enchantment.name()));
			info += " " + enchantment.desc();
		}
		info += "\n\n" + Messages.get(MissileWeapon.class, "distance");
		return info;
	}

	@Override
	public int targetingPos( Hero user, int dst ) {
		return knockDart().targetingPos(user, dst);
	}

	public BlowDart knockDart(){
		return new BlowDart();
	}

	public class BlowDart extends MissileWeapon {

		{
			image = ItemSpriteSheet.BLOWDART;
			hitSound = Assets.Sounds.HIT_ARROW;
			setID = 0;
		}

		@Override
		public ArrayList<String> actions( Hero hero ) {
			return new ArrayList<>();
		}

		@Override
		public String defaultAction() {
			return null;
		}

		@Override
		public int defaultQuantity() {
			return 1;
		}

		@Override
		public int damageRoll( Char owner ) {
			return Blowgun.this.damageRoll(owner);
		}

		@Override
		public boolean hasEnchant( Class<? extends Enchantment> type, Char owner ) {
			return Blowgun.this.hasEnchant(type, owner);
		}

		@Override
		public int proc( Char attacker, Char defender, int damage ) {
			return Blowgun.this.proc(attacker, defender, damage);
		}

		@Override
		public float delayFactor( Char user ) {
			return Blowgun.this.delayFactor(user);
		}

		@Override
		public int STRReq( int lvl ) {
			return Blowgun.this.STRReq();
		}

		@Override
		protected void onThrow( int cell ) {
			Char enemy = Actor.findChar(cell);
			if (enemy == null || enemy == curUser) {
				parent = null;
				Splash.at(cell, 0xCC66CC44, 1);
			} else {
				if (!curUser.shoot(enemy, this)) {
					Splash.at(cell, 0xCC66CC44, 1);
				}
			}
		}

		@Override
		public Item split( int amount ) {
			return null;
		}

		@Override
		public void throwSound() {
			Sample.INSTANCE.play(Assets.Sounds.ATK_SPIRITBOW, 1, Random.Float(1.3f, 1.5f));
		}
	}
}
