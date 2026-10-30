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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.kingdom;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKBulletField;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

//the dark grand knight's shield, left by his side when the shadow let him go.
//charges while you fight. raise it and for a few turns it faces the nearest enemy and blocks part of
//every hit/bullet from that side, like it did for him
public class OathShield extends Artifact {

	public static final String AC_RAISE = "RAISE";

	{
		image = ItemSpriteSheet.OATH_SHIELD;
		levelCap = 10;
		charge = 0;
		chargeCap = 100;
		defaultAction = AC_RAISE;
		unique = true;
	}

	@Override
	public int image() {
		return charge >= chargeCap ? ItemSpriteSheet.OATH_SHIELD_LIT : ItemSpriteSheet.OATH_SHIELD;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions(hero);
		if (isEquipped(hero) && !cursed) actions.add(AC_RAISE);
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ) {
		super.execute(hero, action);
		if (!action.equals(AC_RAISE)) return;
		if (!isEquipped(hero)){
			GLog.i(Messages.get(Artifact.class, "need_to_equip"));
			return;
		}
		if (charge < chargeCap){
			GLog.i(Messages.get(this, "no_charge"));
			return;
		}
		//faces the nearest visible enemy
		Mob near = null;
		for (Mob m : Dungeon.level.mobs){
			if (m.alignment != Char.Alignment.ENEMY || !hero.fieldOfView[m.pos]) continue;
			if (near == null || Dungeon.level.distance(hero.pos, m.pos) < Dungeon.level.distance(hero.pos, near.pos)) near = m;
		}
		if (near == null){
			GLog.i(Messages.get(this, "no_enemy"));
			return;
		}
		charge = 0;
		Guard g = Buff.affect(hero, Guard.class, 4 + level() / 2f);
		int w = Dungeon.level.width();
		g.dx = near.pos % w - hero.pos % w;
		g.dy = near.pos / w - hero.pos / w;
		g.block = 0.5f + 0.03f * level();
		exp++;
		if (exp >= level() + 2 && level() < levelCap){
			exp = 0;
			upgrade();
			GLog.p(Messages.get(this, "levelup"));
		}
		Sample.INSTANCE.play(Assets.Sounds.HIT_PARRY, 1f, 0.8f);
		if (hero.sprite != null){
			hero.sprite.turnTo(hero.pos, near.pos);
			hero.sprite.showStatus(CharSprite.POSITIVE, Messages.get(this, "raised"));
		}
		updateQuickslot();
		hero.spendAndNext(1f);
	}

	@Override
	protected ArtifactBuff passiveBuff() {
		return new Oath();
	}

	@Override
	public void charge( Hero target, float amount ) {
		if (charge < chargeCap){
			partialCharge += 2.5f * amount;
			while (partialCharge >= 1 && charge < chargeCap){
				partialCharge--;
				charge++;
			}
			updateQuickslot();
		}
	}

	@Override
	public String desc() {
		String desc = Messages.get(this, "desc");
		if (isEquipped(Dungeon.hero)){
			desc += "\n\n" + Messages.get(this, charge >= chargeCap ? "desc_ready" : "desc_charging",
					Math.round(100 * (0.5f + 0.03f * level())), 4 + level() / 2);
		}
		return desc;
	}

	//charges while there's a fight in sight
	public class Oath extends ArtifactBuff {
		@Override
		public boolean act() {
			if (charge < chargeCap && !cursed && target instanceof Hero
					&& ((Hero) target).visibleEnemies() > 0){
				partialCharge += 1.6f + 0.2f * level();
				while (partialCharge >= 1 && charge < chargeCap){
					partialCharge--;
					charge++;
				}
				if (charge >= chargeCap) GLog.p(Messages.get(OathShield.class, "ready"));
				updateQuickslot();
			}
			spend(TICK);
			return true;
		}
	}

	//the raised shield
	public static class Guard extends FlavourBuff {

		{
			type = buffType.POSITIVE;
			announced = true;
		}

		public int dx, dy;
		public float block = 0.5f;

		@Override
		public int icon() {
			return BuffIndicator.ARMOR;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(1f, 0.8f, 0.3f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (6 - visualcooldown()) / 6);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", Math.round(block * 100), dispTurns());
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("dx", dx);
			bundle.put("dy", dy);
			bundle.put("block", block);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			dx = bundle.getInt("dx");
			dy = bundle.getInt("dy");
			block = bundle.getFloat("block");
		}
	}

	//called from Hero.damage: what's left of a hit after the raised shield
	public static float guard( Hero hero, float damage, Object src ){
		Guard g = hero.buff(Guard.class);
		if (g == null || damage <= 0) return damage;
		Char a = src instanceof Char ? (Char) src : src instanceof SKBulletField.Shot ? ((SKBulletField.Shot) src).owner : null;
		if (a == null || a == hero) return damage;
		int w = Dungeon.level.width();
		int ax = a.pos % w - hero.pos % w, ay = a.pos / w - hero.pos / w;
		if ((ax == 0 && ay == 0) || (g.dx == 0 && g.dy == 0)) return damage;
		double dot = (g.dx * ax + g.dy * ay) / (Math.sqrt(g.dx * g.dx + g.dy * g.dy) * Math.sqrt(ax * ax + ay * ay));
		if (dot < 0.5) return damage;
		if (hero.sprite != null && Dungeon.level.heroFOV[hero.pos]){
			Sample.INSTANCE.play(Assets.Sounds.HIT_PARRY, 0.7f, 1.1f);
		}
		return damage * (1f - g.block);
	}
}
