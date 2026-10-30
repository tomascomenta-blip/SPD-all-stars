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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.kraken;

import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Amok;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corruption;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Sleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicalSleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMob;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

//giant tentacle lying on the deck after a slam. the real mob sits in the sea by the hull,
//each deck cell gets a Segment that forwards hits to it. so it blocks the deck and you can hit it anywhere
//types: 1 slam, 2 hook (grabs someone), 3 armored (only the glowy sucker takes full dmg), 4 mama (all of the above, bigger)
public class GiantTentacle extends SKMob {

	{
		spriteClass = Kraken.KrakenProxySprite.class;
		HP = HT = 90;
		defenseSkill = 0;
		EXP = 20;
		state = HUNTING;
		properties.add(Property.IMMOVABLE);
		properties.add(Property.INORGANIC);
		properties.add(Property.MINIBOSS);
		immunities.add(Corruption.class);
		immunities.add(Charm.class);
		immunities.add(Amok.class);
		immunities.add(Terror.class);
		immunities.add(Sleep.class);
		immunities.add(MagicalSleep.class);
	}

	public int type = 1, side = 1, row = 10;
	public int sucker = 0;       //glowy segment, types 3/4
	private int suckerTimer = 3;
	public int[] segments = new int[0];

	public void setup( int type, int side, int row ){
		this.type = type;
		this.side = side;
		this.row = row;
		HP = HT = new int[]{0, 90, 110, 130, 170}[Math.max(1, Math.min(4, type))];
		//-Dspd.test.krakenweak=1: 1hp tentacles, to test the ending without suffering
		if (DevFlags.krakenWeak()) HP = HT = 1;
	}

	public boolean armoured(){
		return type >= 3;
	}

	public void spawnSegments( ArrayList<Integer> cells ){
		ArrayList<Integer> ids = new ArrayList<>();
		int i = 0;
		for (int c : cells){
			if (Actor.findChar(c) != null) continue;
			Segment s = new Segment();
			s.owner = id();
			s.index = i++;
			s.pos = c;
			GameScene.add(s);
			ids.add(s.id());
		}
		segments = new int[ids.size()];
		for (int k = 0; k < segments.length; k++) segments[k] = ids.get(k);
		sucker = segments.length > 0 ? Random.Int(segments.length) : 0;
	}

	public int segmentCell( int index ){
		if (index < 0 || index >= segments.length) return -1;
		Actor a = Actor.findById(segments[index]);
		return a instanceof Char ? ((Char) a).pos : -1;
	}

	private Object lastSrc = null;
	private float lastTime = -1;

	//index -1 = hit the tentacle directly
	public void hitFrom( int index, int dmg, Object src, Char where ){
		if (!isAlive()) return;
		//AoE over several segments only counts once. before this a bomb did like 6x dmg lol
		if (src != null && src == lastSrc && lastTime == Actor.now()) return;
		lastSrc = src;
		lastTime = Actor.now();
		if (armoured()){
			if (index == sucker && index >= 0){
				dmg = Math.round(dmg * 1.5f);
				if (where != null && where.sprite != null) where.sprite.showStatus(CharSprite.POSITIVE, Messages.get(this, "weak"));
			} else {
				dmg = Math.max(1, Math.round(dmg * (type == 4 ? 0.5f : 0.35f)));
			}
		}
		if (where != null && where.sprite != null && where != this){
			where.sprite.showStatus(CharSprite.NEGATIVE, Integer.toString(dmg));
			where.sprite.bloodBurstA(where.sprite.center(), dmg);
			where.sprite.flash();
		}
		super.damage(dmg, src);
		KrakenVisuals.tentacleHurt();
	}

	@Override
	public void damage( int dmg, Object src ) {
		hitFrom(-1, dmg, src, this);
	}

	@Override
	protected boolean act() {
		if (armoured() && segments.length > 0 && --suckerTimer <= 0){
			suckerTimer = 4;
			int old = sucker;
			if (segments.length > 1){
				do { sucker = Random.Int(segments.length); } while (sucker == old);
			}
		}
		spend(TICK);
		return true;
	}

	@Override
	public void die( Object cause ) {
		super.die(cause);
		for (int id : segments){
			Actor a = Actor.findById(id);
			if (a instanceof Segment){
				((Segment) a).destroy();
				if (((Segment) a).sprite != null) ((Segment) a).sprite.killAndErase();
			}
		}
		for (Hero h : Kraken.partyHeroes()){
			KrakenHold hold = h.buff(KrakenHold.class);
			if (hold != null && hold.tentacle == id()) hold.release(true);
		}
		KrakenVisuals.tentacleDied(type, side, row);
		yell(Messages.get(this, "dies"));
	}

	@Override
	public String name() {
		return Messages.get(this, "name_" + type);
	}

	@Override
	public String description() {
		return Messages.get(this, "desc_" + type);
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("type", type);
		bundle.put("side", side);
		bundle.put("row", row);
		bundle.put("sucker", sucker);
		bundle.put("sucker_timer", suckerTimer);
		bundle.put("segments", segments);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		type = bundle.getInt("type");
		side = bundle.getInt("side");
		row = bundle.getInt("row");
		sucker = bundle.getInt("sucker");
		suckerTimer = bundle.getInt("sucker_timer");
		segments = bundle.getIntArray("segments");
	}

	//deck piece. blocks, forwards hits. no tiene vida propia
	public static class Segment extends Mob {

		{
			spriteClass = Kraken.KrakenProxySprite.class;
			HP = HT = 1000;
			defenseSkill = 0;
			EXP = 0;
			lootChance = 0;
			state = PASSIVE;
			alignment = Alignment.ENEMY;
			properties.add(Property.IMMOVABLE);
			properties.add(Property.INORGANIC);
			properties.add(Property.STATIC);
			immunities.add(Corruption.class);
			immunities.add(Charm.class);
			immunities.add(Amok.class);
			immunities.add(Terror.class);
			immunities.add(Sleep.class);
			immunities.add(MagicalSleep.class);
		}

		public int owner = -1;
		public int index = 0;

		@Override
		protected void onAdd() {
			firstAdded = false;
			super.onAdd();
		}

		public GiantTentacle owner(){
			Actor a = Actor.findById(owner);
			return a instanceof GiantTentacle ? (GiantTentacle) a : null;
		}

		@Override
		public void damage( int dmg, Object src ) {
			GiantTentacle t = owner();
			if (t != null) t.hitFrom(index, dmg, src, this);
		}

		@Override
		public boolean isInvulnerable( Class effect ) {
			GiantTentacle t = owner();
			return t == null || !t.isAlive();
		}

		@Override
		protected boolean act() {
			if (owner() == null){
				destroy();
				if (sprite != null) sprite.killAndErase();
				return true;
			}
			spend(TICK);
			return true;
		}

		@Override
		public void die( Object cause ) {
		}

		@Override
		public String name() {
			GiantTentacle t = owner();
			return t != null ? t.name() : Messages.get(GiantTentacle.class, "name_1");
		}

		@Override
		public String description() {
			GiantTentacle t = owner();
			return t != null ? t.description() : "";
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("owner", owner);
			bundle.put("index", index);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			owner = bundle.getInt("owner");
			index = bundle.getInt("index");
		}
	}
}
