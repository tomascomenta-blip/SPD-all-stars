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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.cathedral;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.city.fauna.CityCritter;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Collections;

//melted faithful. the ones who were at mass when the furnace was lit: they ran into the wood of
//their pews and the wood into them. they sit there, dripping, until someone walks by too close,
//the heat climbs or something hits them; then they tear loose (a turn) and shamble after the
//party. slow, and their hug is wax at boiling point: the cell is marked and whoever stays in it
//gets stuck there for a turn, burning
public class MeltedFaithful extends CityCritter {

	{
		spriteClass = FaithfulSprite.class;

		HP = HT = 95;
		defenseSkill = 14;
		EXP = 9;
		baseSpeed = 0.8f;

		minDmg = 16; maxDmg = 24; acc = 52;
		armorMin = 2; armorMax = 8;

		properties.add(Property.FIERY);
	}

	private static final int EMBRACE = 0;
	//how close someone has to pass to tear one loose
	private static final int NOTICE = 2;

	private boolean seated = false;
	private int rising = 0;

	public void seat(){
		seated = true;
		state = PASSIVE;
	}

	public boolean seated(){
		return seated;
	}

	//out of the pew: a turn tearing loose, then after the nearest of the party
	public void rise(){
		if (!seated || !isAlive()) return;
		seated = false;
		rising = 1;
		Hero h = Party.nearestLiving(pos);
		state = HUNTING;
		if (h != null){
			aggro(h);
			target = h.pos;
		}
		if (sprite instanceof FaithfulSprite) ((FaithfulSprite) sprite).rise();
		if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.BURNING, 0.4f, 0.6f);
	}

	@Override
	protected int specialCooldown() {
		return 7;
	}

	@Override
	protected boolean act() {
		if (seated && paralysed <= 0){
			for (Hero h : Party.living()){
				if (h.invisible <= 0 && Dungeon.level.distance(h.pos, pos) <= NOTICE){
					rise();
					break;
				}
			}
		}
		if (seated){
			spend(TICK);
			return true;
		}
		if (rising > 0){
			rising--;
			spend(TICK);
			return true;
		}
		return super.act();
	}

	@Override
	public void damage( int dmg, Object src ) {
		super.damage(dmg, src);
		if (seated && isAlive()) rise();
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc(enemy, damage);
		if (enemy.isAlive() && !Char.hasProp(enemy, Property.FIERY) && Random.Int(4) == 0){
			Buff.affect(enemy, Burning.class).reignite(enemy, 2f);
		}
		return damage;
	}

	//right next to someone: it opens its arms (their cell is marked) and hugs whoever is still there
	@Override
	protected boolean special( Char enemy ) {
		//someone already stuck is left to the others: there's always a way to tear loose
		if (!Dungeon.level.adjacent(pos, enemy.pos) || enemy.rooted || enemy.buff(Roots.class) != null) return false;
		announce(EMBRACE, new ArrayList<>(Collections.singletonList(enemy.pos)), enemy.pos);
		return true;
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		//with a callback: the plain attack anim would make it hit AGAIN when it ends
		if (sprite != null) sprite.attack(aim, new Callback() {
			@Override
			public void call() {
				if (sprite != null) sprite.idle();
			}
		});
		for (Char ch : SKCombat.foesIn(this, cells)){
			blast(ch, Cathedral.fire(30), Cathedral.fire(45));
			if (!ch.isAlive()) continue;
			Buff.prolong(ch, Roots.class, 1f);
			if (!Char.hasProp(ch, Property.FIERY)) Buff.affect(ch, Burning.class).reignite(ch, 3f);
			if (ch == Dungeon.local()) GLog.w(Messages.get(this, "stuck"));
		}
	}

	@Override
	public String description() {
		return seated ? Messages.get(this, "desc_seated") : super.description();
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("seated", seated);
		bundle.put("rising", rising);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		seated = bundle.getBoolean("seated");
		rising = bundle.getInt("rising");
	}

	//16x16: 8-9 sitting in the pew, 10 tearing loose
	public static class FaithfulSprite extends Sheet {

		private final Animation walking, sitting, tearing;

		public FaithfulSprite(){
			super();
			layout(CathedralAssets.FAITHFUL);
			walking = idle;
			sitting = loop(1, 8, 8, 9);
			tearing = new Animation(5, false);
			tearing.frames(frames, 8, 10, 10, 0);
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (ch instanceof MeltedFaithful && ((MeltedFaithful) ch).seated()) look(sitting);
		}

		void rise(){
			idle = walking;
			play(tearing);
			showStatus(CharSprite.WARNING, "!");
		}

		@Override
		public void onComplete( Animation anim ) {
			if (anim == tearing){
				idle();
				return;
			}
			super.onComplete(anim);
		}
	}
}
