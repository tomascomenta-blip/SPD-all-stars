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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.guns;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.DamageWand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

//guns + staffs. basically wands: recharge over time, quickslot, upgrade w/ scrolls.
//always IDed, never cursed. bullets are physical so armor eats some of it
public abstract class SKGun extends DamageWand {

	{
		levelKnown = true;
		cursedKnown = true;
		curChargeKnown = true;
		collisionProperties = Ballistica.PROJECTILE;
	}

	//SHOT_*
	protected abstract int shotImage();

	//gunslinger only: empty gun -> half charges, 15 turn cd per gun
	public static final String AC_RELOAD = "RELOAD";
	public static final float RELOAD_COOLDOWN = 15f;
	private float lastReload = -999f;

	public boolean canReload( Hero hero ){
		return hero != null && hero.heroClass == HeroClass.GUNSLINGER
				&& curCharges == 0 && Statistics.duration - lastReload >= RELOAD_COOLDOWN;
	}

	@Override
	public java.util.ArrayList<String> actions( Hero hero ) {
		java.util.ArrayList<String> actions = super.actions(hero);
		if (canReload(hero)) actions.add(AC_RELOAD);
		return actions;
	}

	@Override
	public String actionName( String action, Hero hero ) {
		if (action.equals(AC_RELOAD)) return Messages.get(SKGun.class, "ac_reload");
		return super.actionName(action, hero);
	}

	@Override
	public void execute( Hero hero, String action ) {
		if (action.equals(AC_RELOAD)){
			if (!canReload(hero)) return;
			lastReload = Statistics.duration;
			curCharges = Math.max(1, (maxCharges + 1)/2);
			updateQuickslot();
			if (hero.sprite != null) hero.sprite.operate(hero.pos);
			Sample.INSTANCE.play(Assets.Sounds.UNLOCK, 1f, 1.3f);
			GLog.i(Messages.get(SKGun.class, "reloaded", name()));
			hero.spendAndNext(Actor.TICK);
			return;
		}
		super.execute(hero, action);
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("last_reload", lastReload);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		lastReload = bundle.contains("last_reload") ? bundle.getFloat("last_reload") : -999f;
	}

	public int shotImageForFx(){
		return shotImage();
	}

	protected boolean spinningShot(){
		return false;
	}

	protected String shotSound(){
		return Assets.Sounds.HIT_ARROW;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public Item random() {
		//same odds as wands (66/26/6), no curses. no sé si cuadra con el 100 pero bueno
		int n = 0;
		if (Random.Int(3) == 0) {
			n++;
			if (Random.Int(5) == 0){
				n++;
			}
		}
		level(n);
		curCharges = maxCharges;
		cursed = false;
		return this;
	}

	@Override
	public Item identify( boolean byHero ) {
		super.identify(byHero);
		cursed = false;
		return this;
	}

	@Override
	public void onZap( Ballistica shot ) {
		Char ch = Actor.findChar(shot.collisionPos);
		if (ch != null){
			shoot(ch, damageRoll());
		}
	}

	//armor blocks half its usual
	protected void shoot( Char ch, int dmg ){
		dmg = Math.max(1, dmg - ch.drRoll()/2);
		wandProc(ch, chargesPerCast());
		ch.damage(dmg, this);
		if (ch.sprite != null && ch.sprite.visible) Sample.INSTANCE.play(Assets.Sounds.HIT, 1f, Random.Visual.Float(1.1f, 1.3f));
		if (ch.sprite != null) ch.sprite.flash();
	}

	@Override
	public void fx( Ballistica bolt, Callback callback ) {
		Item shot = spinningShot() ? new SKMobSprite.SpinningShot(shotImage()) : new SKMobSprite.Shot(shotImage());
		((MissileSprite) curUser.sprite.parent.recycle(MissileSprite.class)).reset(
				curUser.sprite, bolt.collisionPos, shot, callback);
		Sample.INSTANCE.play(shotSound(), 1f, Random.Visual.Float(0.95f, 1.1f));
	}

	@Override
	public void onHit( MagesStaff staff, Char attacker, Char defender, int damage ) {
		//no imbuing guns into the mage staff. nope
	}

	@Override
	public String statsDesc() {
		return Messages.get(this, "stats_desc", min(), max());
	}

	@Override
	public int value() {
		int price = 60;
		if (level() > 0) price *= (level() + 1);
		return price;
	}
}
