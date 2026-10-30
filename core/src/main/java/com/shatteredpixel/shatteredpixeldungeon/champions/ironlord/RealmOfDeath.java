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

package com.shatteredpixel.shatteredpixeldungeon.champions.ironlord;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.champions.AbilityCooldown;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionAbility;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionKit;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

//R. duel: him + 1 enemy go to a pocket room (DuelRealm) for a while, he steals part of its max hp + dmg.
//kill it in there and he keeps the stolen stuff until next floor
//bosses / unmovable stuff stay put, he just steals in place. R again = leave early
public class RealmOfDeath extends ChampionAbility {

	public static final RealmOfDeath INSTANCE = new RealmOfDeath();

	public static final int RANGE = 6;

	@Override public String slot() { return "R"; }
	@Override public int unlockLevel() { return 6; }
	@Override public int icon() { return HeroIcon.IRON_REALM; }
	@Override public int color() { return 0x5577FF; }
	@Override public Class<? extends AbilityCooldown> cooldownClass() { return CD.class; }

	//button = exit while dueling
	@Override
	public boolean targeted() {
		return Dungeon.hero == null || Dungeon.hero.buff(Duel.class) == null;
	}

	@Override
	public boolean onCooldown( Hero hero ) {
		if (hero.buff(Duel.class) != null) return false;
		return super.onCooldown(hero);
	}

	@Override
	public float cooldown( Hero hero ) {
		return 80;
	}

	public static int duration( Hero hero ){
		return 8 + hero.pointsInTalent(Talent.IRL_UNDYING_WILL);
	}

	//% of enemy max hp stolen
	public static float healthShare( Hero hero ){
		return 0.25f + 0.05f*hero.pointsInTalent(Talent.IRL_UNDYING_WILL);
	}

	public static int damagePercent( Hero hero ){
		return 20 + 5*hero.pointsInTalent(Talent.IRL_UNDYING_WILL);
	}

	public static boolean canTransport( Char enemy ){
		return enemy instanceof Mob
				&& !Char.hasProp(enemy, Char.Property.BOSS)
				&& !Char.hasProp(enemy, Char.Property.IMMOVABLE)
				&& !Char.hasProp(enemy, Char.Property.STATIC)
				&& !Char.hasProp(enemy, Char.Property.OBJECT);
	}

	public static int boost( Hero hero, Char enemy, int damage ){
		Duel d = hero.buff(Duel.class);
		if (d != null && enemy.id() == d.enemyId){
			damage = Math.round(damage * (1f + d.percent / 100f));
		}
		Spoils s = hero.buff(Spoils.class);
		if (s != null){
			damage = Math.round(damage * (1f + s.percent / 100f));
		}
		return damage;
	}

	@Override
	public void activate( Hero hero, ChampionKit kit, Integer target ) {
		Duel running = hero.buff(Duel.class);
		if (running != null){
			running.end();
			hero.spendAndNext(1f);
			return;
		}

		final Char enemy = ChampionHooks.boltTarget(hero, target);
		if (enemy == null || enemy.alignment != Char.Alignment.ENEMY || !enemy.isAlive()){
			GLog.w(Messages.get(this, "no_target"));
			return;
		}
		if (Dungeon.level.distance(hero.pos, enemy.pos) > RANGE){
			GLog.w(Messages.get(ChampionAbility.class, "too_far"));
			return;
		}

		boolean transport = canTransport(enemy) && DuelRealm.exists(Dungeon.level)
				&& Actor.findChar(DuelRealm.heroCell(Dungeon.level)) == null
				&& Actor.findChar(DuelRealm.enemyCell(Dungeon.level)) == null;

		Duel duel = Buff.affect(hero, Duel.class);
		duel.enemyId = enemy.id();
		duel.left = duration(hero);
		duel.max = duel.left;
		duel.percent = damagePercent(hero);
		duel.depth = Dungeon.depth;
		duel.transported = transport;
		duel.heroOrigin = hero.pos;
		duel.enemyOrigin = enemy.pos;

		//yoink
		int take = Math.round(enemy.HT * healthShare(hero));
		take = Math.max(1, Math.min(take, Math.min(enemy.HT - 1, hero.HT / 2)));
		duel.taken = take;
		hero.HTBoost += take;
		hero.updateHT(false);
		hero.HP = Math.min(hero.HT, hero.HP + take);
		enemy.HT -= take;
		enemy.HP = Math.min(enemy.HP, enemy.HT);
		Buff.prolong(enemy, Weakness.class, duel.left);
		hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(take), FloatingText.HEALING);

		Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 1f, 0.6f);
		if (transport){
			DuelRealm.place(hero, DuelRealm.heroCell(Dungeon.level));
			DuelRealm.place(enemy, DuelRealm.enemyCell(Dungeon.level));
			if (enemy instanceof Mob) ((Mob) enemy).aggro(hero);
		}

		Invisibility.dispel(hero);
		startCooldown(hero);
		hero.spendAndNext(1f);
	}

	@Override
	public String desc( Hero hero ) {
		return Messages.get(this, "desc", duration(hero), Math.round(healthShare(hero)*100), damagePercent(hero));
	}

	public static class CD extends AbilityCooldown {
		@Override
		public ChampionAbility ability() {
			return INSTANCE;
		}
	}

	public static class Duel extends Buff {

		{
			type = buffType.POSITIVE;
			announced = true;
		}

		public int enemyId = -1;
		public int left = 0;
		public int max = 1;
		public int percent = 0;
		public int taken = 0;
		public int depth = 0;
		public boolean transported = false;
		public int heroOrigin = -1;
		public int enemyOrigin = -1;

		private Char enemy(){
			Actor a = Actor.findById(enemyId);
			return a instanceof Char ? (Char) a : null;
		}

		@Override
		public boolean act() {
			if (!(target instanceof Hero) || !target.isAlive()){
				detach();
				return true;
			}
			if (Dungeon.depth != depth){
				//left the floor some other way, just undo the theft
				revert((Hero) target);
				detach();
				return true;
			}
			Char e = enemy();
			left--;
			if (e == null || !e.isAlive() || left <= 0){
				end();
			} else {
				spend(TICK);
			}
			return true;
		}

		//both go back. theft stays only if the enemy died
		public void end(){
			Hero hero = (Hero) target;
			Char e = enemy();
			boolean killed = e == null || !e.isAlive();
			if (transported){
				DuelRealm.leave(hero, heroOrigin);
				if (!killed) DuelRealm.leave(e, enemyOrigin);
			}
			DuelRealm.rescueLoot(hero);
			if (killed){
				Spoils s = Buff.affect(hero, Spoils.class);
				s.gained += taken;
				s.percent = Math.max(s.percent, percent);
				s.depth = Dungeon.depth;
				if (hero.sprite != null) hero.sprite.showStatus(CharSprite.POSITIVE, Messages.get(RealmOfDeath.class, "spoils"));
			} else {
				revert(hero);
				if (e != null) e.HT += taken;
			}
			detach();
		}

		//hero went down (coop) -> everyone back, give the stuff back. this one was a pain
		public void abort(){
			Hero hero = (Hero) target;
			Char e = enemy();
			if (transported){
				DuelRealm.leave(hero, heroOrigin);
				if (e != null && e.isAlive()) DuelRealm.leave(e, enemyOrigin);
			}
			DuelRealm.rescueLoot(hero);
			revert(hero);
			if (e != null && e.isAlive()) e.HT += taken;
			detach();
		}

		private void revert( Hero hero ){
			hero.HTBoost -= taken;
			hero.updateHT(false);
			hero.HP = Math.min(hero.HP, hero.HT);
		}

		@Override
		public int icon() {
			return BuffIndicator.MARK;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(0.3f, 0.5f, 1f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, 1f - left / (float) Math.max(1, max));
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString(left);
		}

		@Override
		public String name() {
			return Messages.get(this, "name");
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", taken, percent, left);
		}

		private static final String ENEMY = "enemy";
		private static final String LEFT = "left";
		private static final String MAX = "max";
		private static final String PERCENT = "percent";
		private static final String TAKEN = "taken";
		private static final String DEPTH = "depth";
		private static final String TRANSPORTED = "transported";
		private static final String HERO_ORIGIN = "hero_origin";
		private static final String ENEMY_ORIGIN = "enemy_origin";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(ENEMY, enemyId);
			bundle.put(LEFT, left);
			bundle.put(MAX, max);
			bundle.put(PERCENT, percent);
			bundle.put(TAKEN, taken);
			bundle.put(DEPTH, depth);
			bundle.put(TRANSPORTED, transported);
			bundle.put(HERO_ORIGIN, heroOrigin);
			bundle.put(ENEMY_ORIGIN, enemyOrigin);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			enemyId = bundle.getInt(ENEMY);
			left = bundle.getInt(LEFT);
			max = bundle.getInt(MAX);
			percent = bundle.getInt(PERCENT);
			taken = bundle.getInt(TAKEN);
			depth = bundle.getInt(DEPTH);
			transported = bundle.getBoolean(TRANSPORTED);
			heroOrigin = bundle.getInt(HERO_ORIGIN);
			enemyOrigin = bundle.getInt(ENEMY_ORIGIN);
		}
	}

	//spoils, until next floor
	public static class Spoils extends Buff {

		{
			type = buffType.POSITIVE;
			revivePersists = true;
		}

		public int gained = 0;
		public int percent = 0;
		public int depth = 0;

		@Override
		public boolean act() {
			if (!(target instanceof Hero) || !target.isAlive() || Dungeon.depth != depth){
				if (target instanceof Hero){
					Hero hero = (Hero) target;
					hero.HTBoost -= gained;
					hero.updateHT(false);
					hero.HP = Math.min(hero.HP, hero.HT);
				}
				gained = 0;
				detach();
				return true;
			}
			spend(TICK);
			return true;
		}

		@Override
		public int icon() {
			return BuffIndicator.HEART;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(0.3f, 0.5f, 1f);
		}

		@Override
		public float iconFadePercent() {
			return 0;
		}

		@Override
		public String name() {
			return Messages.get(this, "name");
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", gained, percent);
		}

		private static final String GAINED = "gained";
		private static final String PERCENT = "percent";
		private static final String DEPTH = "depth";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(GAINED, gained);
			bundle.put(PERCENT, percent);
			bundle.put(DEPTH, depth);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			gained = bundle.getInt(GAINED);
			percent = bundle.getInt(PERCENT);
			depth = bundle.getInt(DEPTH);
		}
	}
}
