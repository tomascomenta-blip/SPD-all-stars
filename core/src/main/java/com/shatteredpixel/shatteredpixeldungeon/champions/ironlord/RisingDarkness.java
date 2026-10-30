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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

//ironlord passive. 3 melee hits close together -> dark aura that hurts adjacent enemies for a bit.
//also logs recent dmg dealt, Indestructible turns that into a shield
public class RisingDarkness extends Buff {

	{
		type = buffType.POSITIVE;
		revivePersists = true;
	}

	public static final int HITS_NEEDED = 3;
	public static final int CHAIN_TURNS = 3;
	public static final int RECENT_TURNS = 8;

	public int hits = 0;
	public int idle = 0;
	//[0] = this turn
	public int[] recent = new int[RECENT_TURNS];

	public static RisingDarkness of( Hero hero ){
		return hero.buff(RisingDarkness.class);
	}

	public static void addDamage( Hero hero, int damage ){
		RisingDarkness rd = of(hero);
		if (rd != null && damage > 0) rd.recent[0] += damage;
	}

	public static int recentDamage( Hero hero ){
		RisingDarkness rd = of(hero);
		if (rd == null) return 0;
		int sum = 0;
		for (int d : rd.recent) sum += d;
		return sum;
	}

	public void onHit( Hero hero ){
		hits++;
		idle = 0;
		if (hits >= HITS_NEEDED){
			hits = 0;
			DarkAura.trigger(hero, DarkAura.baseTurns(hero), false);
		}
	}

	//resonance talent: abilities count as hits
	public void addHits( Hero hero, int amount ){
		for (int i = 0; i < amount; i++) onHit(hero);
	}

	@Override
	public boolean act() {
		if (!(target instanceof Hero) || ((Hero) target).heroClass != HeroClass.IRONLORD){
			detach();
			return true;
		}
		idle++;
		if (idle > CHAIN_TURNS) hits = 0;
		for (int i = RECENT_TURNS-1; i > 0; i--) recent[i] = recent[i-1];
		recent[0] = 0;
		spend(TICK);
		return true;
	}

	@Override
	public int icon() {
		return BuffIndicator.FURY;
	}

	@Override
	public void tintIcon( Image icon ) {
		icon.hardlight(0.5f, 0.3f, 0.9f);
	}

	@Override
	public float iconFadePercent() {
		return 1f - hits / (float) HITS_NEEDED;
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(hits);
	}

	@Override
	public String name() {
		return Messages.get(this, "name");
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", hits, HITS_NEEDED);
	}

	private static final String HITS = "hits";
	private static final String IDLE = "idle";
	private static final String RECENT = "recent";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(HITS, hits);
		bundle.put(IDLE, idle);
		bundle.put(RECENT, recent);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		hits = bundle.getInt(HITS);
		idle = bundle.getInt(IDLE);
		int[] r = bundle.getIntArray(RECENT);
		if (r != null && r.length == RECENT_TURNS) recent = r;
	}

	public static class DarkAura extends Buff {

		{
			type = buffType.POSITIVE;
			announced = true;
		}

		public int left = 0;
		public int max = 1;
		//iron eclipse: bigger + slows
		public boolean eclipse = false;

		public static boolean active( Hero hero ){
			return hero.buff(DarkAura.class) != null;
		}

		public static int baseTurns( Hero hero ){
			return 4 + hero.pointsInTalent(Talent.IRL_DEEPENING_DARK);
		}

		public static int radius( Hero hero, boolean eclipse ){
			int r = 1;
			if (hero.subClass == HeroSubClass.TYRANT) r++;
			if (eclipse) r++;
			return r;
		}

		public static int damage( Hero hero, boolean eclipse ){
			float d = 1 + hero.lvl / 3f;
			if (hero.subClass == HeroSubClass.TYRANT) d *= 1f + 0.2f*hero.pointsInTalent(Talent.TY_CRUSHING_AURA);
			if (eclipse) d *= 1.5f + 0.25f*hero.pointsInTalent(Talent.IE_SMOTHERING_DARK);
			return Math.max(1, Math.round(d));
		}

		public static void trigger( Hero hero, int turns, boolean eclipse ){
			DarkAura a = Buff.affect(hero, DarkAura.class);
			a.left = Math.max(a.left, turns);
			a.max = Math.max(a.max, a.left);
			a.eclipse |= eclipse;
			if (hero.sprite != null){
				hero.sprite.showStatus(0x9966CC, Messages.get(RisingDarkness.class, "aura"));
			}
		}

		@Override
		public boolean act() {
			if (!(target instanceof Hero) || !target.isAlive()){
				detach();
				return true;
			}
			Hero hero = (Hero) target;
			int dmg = damage(hero, eclipse);
			int dealt = 0;
			int enemies = 0;
			for (Char ch : ChampionHooks.enemiesAround(hero, hero.pos, radius(hero, eclipse))){
				if (!Dungeon.level.adjacent(hero.pos, ch.pos)
						&& new Ballistica(hero.pos, ch.pos, Ballistica.PROJECTILE).collisionPos != ch.pos) continue;
				if (eclipse){
					Buff.prolong(ch, Slow.class, 2f);
				}
				int before = ch.HP;
				ch.damage(dmg, this);
				dealt += Math.max(0, before - Math.max(0, ch.HP));
				enemies++;
				if (ch.sprite != null && ch.sprite.visible){
					CellEmitter.get(ch.pos).burst(ShadowParticle.UP, 3);
				}
			}
			RisingDarkness.addDamage(hero, dealt);
			if (enemies > 0 && hero.subClass == HeroSubClass.REVENANT){
				ChampionHooks.heal(hero, hero.pointsInTalent(Talent.RV_ENDLESS_NIGHT));
			}
			if (dealt > 0 && eclipse && hero.hasTalent(Talent.IE_IRON_HUNGER)){
				ChampionHooks.heal(hero, Math.round(dealt * 0.25f * hero.pointsInTalent(Talent.IE_IRON_HUNGER)));
			}
			left--;
			if (left <= 0){
				detach();
			} else {
				spend(TICK);
			}
			return true;
		}

		@Override
		public int icon() {
			return BuffIndicator.CORRUPT;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(0.6f, 0.3f, 1f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, 1f - left / (float) Math.max(1, max));
		}

		@Override
		public void fx( boolean on ) {
			if (target.sprite == null) return;
			if (on) target.sprite.aura(0x6633AA, 4);
			else    target.sprite.clearAura();
		}

		@Override
		public String name() {
			return Messages.get(this, "name");
		}

		@Override
		public String desc() {
			int r = target instanceof Hero ? radius((Hero) target, eclipse) : 1;
			int d = target instanceof Hero ? damage((Hero) target, eclipse) : 1;
			return Messages.get(this, "desc", r, d, left);
		}

		private static final String LEFT = "left";
		private static final String MAX = "max";
		private static final String ECLIPSE = "eclipse";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(LEFT, left);
			bundle.put(MAX, max);
			bundle.put(ECLIPSE, eclipse);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			left = bundle.getInt(LEFT);
			max = bundle.getInt(MAX);
			eclipse = bundle.getBoolean(ECLIPSE);
		}
	}
}
