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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.forest;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

import java.util.ArrayList;

//high priest bracers, the brute drops them. worn = melee hits harder. charged: throws the bearer at the
//nearest enemy in a straight line (4 cells max), big hit + knockback + short stun
public class PriestBracers extends Artifact {

	public static final String AC_CHARGE = "CHARGE";

	{
		image = ItemSpriteSheet.PRIEST_BRACERS;
		levelCap = 10;
		charge = 0;
		chargeCap = 100;
		defaultAction = AC_CHARGE;
		unique = true;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions(hero);
		if (isEquipped(hero) && !cursed) actions.add(AC_CHARGE);
		return actions;
	}

	private float bonus(){
		return 0.10f + 0.02f * level();
	}

	//called from Hero.damageRoll. melee bonus while worn
	public static int melee( Hero hero, KindOfWeapon weapon, int dmg ){
		if (weapon instanceof MissileWeapon) return dmg;
		Might m = hero.buff(Might.class);
		if (m == null || m.cursed()) return dmg;
		return Math.round(dmg * (1f + m.bonus()));
	}

	@Override
	public void execute( Hero hero, String action ) {
		super.execute(hero, action);
		if (!action.equals(AC_CHARGE)) return;
		if (!isEquipped(hero)){
			GLog.i(Messages.get(Artifact.class, "need_to_equip"));
			return;
		}
		if (charge < chargeCap){
			GLog.i(Messages.get(this, "no_charge"));
			return;
		}
		//nearest visible enemy a straight line can reach
		Mob target = null;
		for (Mob m : Dungeon.level.mobs){
			if (m.alignment != Char.Alignment.ENEMY || !hero.fieldOfView[m.pos] || Char.hasProp(m, Char.Property.OBJECT)) continue;
			int d = Dungeon.level.distance(hero.pos, m.pos);
			if (d > 4) continue;
			if (new Ballistica(hero.pos, m.pos, Ballistica.PROJECTILE).collisionPos != m.pos) continue;
			if (target == null || d < Dungeon.level.distance(hero.pos, target.pos)) target = m;
		}
		if (target == null){
			GLog.i(Messages.get(this, "no_enemy"));
			return;
		}
		charge = 0;

		//up to the cell before the enemy
		Ballistica path = new Ballistica(hero.pos, target.pos, Ballistica.PROJECTILE);
		int from = hero.pos, to = hero.pos;
		for (int c : path.subPath(1, path.dist)){
			if (c == target.pos || Actor.findChar(c) != null || !Dungeon.level.passable[c]) break;
			to = c;
		}
		if (to != from){
			hero.pos = to;
			Actor.add(new Pushing(hero, from, to));
			Dungeon.level.occupyCell(hero);
		}
		if (hero.sprite != null) hero.sprite.turnTo(hero.pos, target.pos);
		hero.attack(target, 1.5f, 0, Char.INFINITE_ACCURACY);
		if (target.isAlive()){
			knock(hero, target);
			Buff.prolong(target, Paralysis.class, 1f);
		}
		if (Dungeon.level.heroFOV[target.pos]){
			Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG, 1f, 0.7f);
			CellEmitter.get(target.pos).burst(Speck.factory(Speck.DUST), 5);
		}
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				PixelScene.shake(2, 0.3f);
			}
		});

		exp++;
		if (exp >= level() + 2 && level() < levelCap){
			exp = 0;
			upgrade();
			GLog.p(Messages.get(this, "levelup"));
		}
		updateQuickslot();
		hero.spendAndNext(1f);
	}

	private static void knock( Hero hero, Char ch ){
		int w = Dungeon.level.width();
		int dx = Integer.signum(ch.pos % w - hero.pos % w), dy = Integer.signum(ch.pos / w - hero.pos / w);
		if (dx == 0 && dy == 0 || Char.hasProp(ch, Char.Property.IMMOVABLE)) return;
		for (int i = 0; i < 2; i++){
			int next = ch.pos + dx + dy * w;
			if (!Dungeon.level.passable[next] || Actor.findChar(next) != null) return;
			int from = ch.pos;
			ch.pos = next;
			Actor.add(new Pushing(ch, from, next));
			Dungeon.level.occupyCell(ch);
		}
	}

	@Override
	protected ArtifactBuff passiveBuff() {
		return new Might();
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
			desc += "\n\n" + Messages.get(this, charge >= chargeCap ? "desc_ready" : "desc_charging", Math.round(bonus() * 100));
		}
		return desc;
	}

	//worn: melee bonus, charges while a fight's in sight
	public class Might extends ArtifactBuff {

		float bonus(){
			return PriestBracers.this.bonus();
		}

		boolean cursed(){
			return cursed;
		}

		@Override
		public boolean act() {
			if (charge < chargeCap && !cursed && target instanceof Hero
					&& ((Hero) target).visibleEnemies() > 0){
				partialCharge += 1.5f + 0.15f * level();
				while (partialCharge >= 1 && charge < chargeCap){
					partialCharge--;
					charge++;
				}
				if (charge >= chargeCap) GLog.p(Messages.get(PriestBracers.class, "ready"));
				updateQuickslot();
			}
			spend(TICK);
			return true;
		}
	}
}
