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

package com.shatteredpixel.shatteredpixeldungeon.rift;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BloodParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

//blood rose from atakhan, one each. eat it -> "spiritual purification" for the rest of the run:
//kills pop a blessing that chunks nearby enemies (% current hp) and slows them. +max hp once too
public class BloodRose extends Item {

	public static final String AC_ABSORB = "ABSORB";

	public static final float HP_BOOST = 0.10f;
	public static final int RADIUS = 2;
	public static final float CURRENT_HP = 0.15f;
	public static final float SLOW = 2f;

	{
		image = ItemSpriteSheet.BLOOD_ROSE;
		stackable = true;
		defaultAction = AC_ABSORB;
		bones = false;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_ABSORB);
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ) {
		super.execute(hero, action);
		if (!action.equals(AC_ABSORB) || !hero.belongings.backpack.contains(this)) return;

		detach(hero.belongings.backpack);
		Purification.give(hero);
		if (hero.sprite != null){
			hero.sprite.emitter().burst(BloodParticle.BURST, 12);
			hero.sprite.emitter().burst(Speck.factory(Speck.HEALING), 4);
		}
		Sample.INSTANCE.play(Assets.Sounds.DRINK);
		GLog.p(Messages.get(this, "absorbed"));

		//the gesture is only visual: the turn ends here, not when the animation does
		final Hero user = hero;
		hero.sprite.operate(hero.pos, new Callback() {
			@Override
			public void call() {
				user.sprite.idle();
			}
		});
		hero.spendAndNext(1f);
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public int value() {
		return 60 * quantity;
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return new ItemSprite.Glowing(0xDD1133, 2f);
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", RADIUS, Math.round(CURRENT_HP*100), Math.round(SLOW),
				Math.round(HP_BOOST*100));
	}

	//the permanent blessing of the hero who absorbed a rose
	public static class Purification extends Buff {

		{
			type = buffType.POSITIVE;
			revivePersists = true;
		}

		public static void give( Hero hero ){
			Buff.affect(hero, Purification.class);
			int boost = Math.max(1, Math.round(hero.HT * HP_BOOST));
			hero.HTBoost += boost;
			hero.updateHT(true);
			if (hero.sprite != null){
				hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(boost), FloatingText.HEALING);
			}
			BuffIndicator.refreshHero();
		}

		//damage of the burst on an enemy: part of its current health, never enough to kill it
		public static int burstDamage( Hero hero, Char ch ){
			int dmg = (int)(ch.HP * CURRENT_HP);
			int cap = 5 + hero.lvl;
			if (Char.hasProp(ch, Char.Property.BOSS) || Char.hasProp(ch, Char.Property.MINIBOSS)) cap /= 2;
			return Math.min(dmg, cap);
		}

		//every enemy on the floor carries a mark that reports its death (see Mark)
		@Override
		public boolean act() {
			for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
				if (m.alignment == Char.Alignment.ENEMY && m.isAlive() && m.buff(Mark.class) == null){
					Buff.affect(m, Mark.class);
				}
			}
			spend(TICK);
			return true;
		}

		//an enemy died: if the hero acting now carries the blessing, it bursts there
		static void onEnemyDeath( Char dead ){
			Actor current = Actor.current();
			if (!(current instanceof Hero)) return;
			Hero hero = (Hero) current;
			Purification p = hero.buff(Purification.class);
			if (p == null || !hero.isAlive()) return;
			p.burst(hero, dead.pos);
		}

		private void burst( Hero hero, int cell ){
			if (Dungeon.level.heroFOV[cell]){
				CellEmitter.center(cell).burst(Speck.factory(Speck.LIGHT), 6);
				for (int n : PathFinder.NEIGHBOURS9){
					int c = cell + n;
					if (c >= 0 && c < Dungeon.level.length() && Dungeon.level.heroFOV[c]){
						CellEmitter.get(c).burst(BloodParticle.BURST, 3);
					}
				}
				Sample.INSTANCE.play(Assets.Sounds.CHARGEUP, 0.6f, 1.6f);
			}
			for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
				if (m.alignment != Char.Alignment.ENEMY || !m.isAlive()) continue;
				if (Dungeon.level.distance(cell, m.pos) > RADIUS) continue;
				int dmg = burstDamage(hero, m);
				if (dmg > 0) m.damage(dmg, this);
				if (m.isAlive()) Buff.prolong(m, Slow.class, SLOW);
			}
		}

		@Override
		public int icon() {
			return BuffIndicator.BLESS;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(1f, 0.3f, 0.35f);
		}

		@Override
		public String desc() {
			int cap = target instanceof Hero ? 5 + ((Hero) target).lvl : 5;
			return Messages.get(this, "desc", RADIUS, Math.round(CURRENT_HP*100), cap, Math.round(SLOW));
		}
	}

	//invisible mark on the enemies while some hero carries the blessing: its removal on death
	//is the only notice of a kill that does not need changes elsewhere in the game
	public static class Mark extends Buff {

		{
			type = buffType.NEUTRAL;
		}

		private boolean reported = false;

		@Override
		public void detach() {
			Char dead = target;
			super.detach();
			if (!reported && dead != null && !dead.isAlive() && dead.alignment == Char.Alignment.ENEMY){
				reported = true;
				Purification.onEnemyDeath(dead);
			}
		}
	}
}
