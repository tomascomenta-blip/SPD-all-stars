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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.starship;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMob;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//heart of the hive, the queen drops it. charges while you fight. wake it up and a varkolyn hatches on your
//side and follows you around. whelp dies -> recharge
public class HiveHeart extends Artifact {

	public static final String AC_AWAKEN = "AWAKEN";

	{
		image = ItemSpriteSheet.HIVE_HEART;
		levelCap = 10;
		charge = 0;
		chargeCap = 100;
		defaultAction = AC_AWAKEN;
		unique = true;
	}

	private int whelp = -1;

	@Override
	public int image() {
		return charge >= chargeCap ? ItemSpriteSheet.HIVE_HEART_LIT : ItemSpriteSheet.HIVE_HEART;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions(hero);
		if (isEquipped(hero) && !cursed) actions.add(AC_AWAKEN);
		return actions;
	}

	private boolean whelpAlive(){
		Actor a = whelp == -1 ? null : Actor.findById(whelp);
		return a instanceof Whelp && ((Whelp) a).isAlive();
	}

	@Override
	public void execute( Hero hero, String action ) {
		super.execute(hero, action);
		if (!action.equals(AC_AWAKEN)) return;
		if (!isEquipped(hero)){
			GLog.i(Messages.get(Artifact.class, "need_to_equip"));
			return;
		}
		if (whelpAlive()){
			GLog.i(Messages.get(this, "already"));
			return;
		}
		if (charge < chargeCap){
			GLog.i(Messages.get(this, "no_charge"));
			return;
		}
		int cell = -1;
		for (int n : PathFinder.NEIGHBOURS8){
			int c = hero.pos + n;
			if (Dungeon.level.passable[c] && !Dungeon.level.pit[c] && Actor.findChar(c) == null){
				cell = c;
				break;
			}
		}
		if (cell == -1){
			GLog.i(Messages.get(this, "no_room"));
			return;
		}
		charge = 0;
		Whelp w = new Whelp();
		w.grow(level());
		w.pos = cell;
		//ALLY before it's added or it gets the enemies' extra hp
		w.alignment = Char.Alignment.ALLY;
		GameScene.add(w);
		Buff.affect(w, Bond.class).owner = ChampionHooks.indexOf(hero);
		w.state = w.WANDERING;
		whelp = w.id();
		if (Dungeon.level.heroFOV[cell]) CellEmitter.get(cell).burst(Speck.factory(Speck.STEAM), 6);
		Sample.INSTANCE.play(Assets.Sounds.BURNING, 1f, 1.5f);
		GLog.p(Messages.get(this, "hatched"));
		exp++;
		if (exp >= level() + 2 && level() < levelCap){
			exp = 0;
			upgrade();
			GLog.p(Messages.get(this, "levelup"));
		}
		updateQuickslot();
		hero.spendAndNext(1f);
	}

	@Override
	protected ArtifactBuff passiveBuff() {
		return new Beat();
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
			if (whelpAlive()) desc += "\n\n" + Messages.get(this, "desc_whelp");
			else desc += "\n\n" + Messages.get(this, charge >= chargeCap ? "desc_ready" : "desc_charging");
		}
		return desc;
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("whelp", whelp);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		whelp = bundle.contains("whelp") ? bundle.getInt("whelp") : -1;
	}

	//beats while there's a fight in sight, only charges when no whelp is out
	public class Beat extends ArtifactBuff {
		@Override
		public boolean act() {
			if (charge < chargeCap && !cursed && !whelpAlive() && target instanceof Hero
					&& ((Hero) target).visibleEnemies() > 0){
				partialCharge += 1.2f + 0.12f * level();
				while (partialCharge >= 1 && charge < chargeCap){
					partialCharge--;
					charge++;
				}
				if (charge >= chargeCap) GLog.p(Messages.get(HiveHeart.class, "ready"));
				updateQuickslot();
			}
			spend(TICK);
			return true;
		}
	}

	//the heart's varkolyn, grows w/ the heart
	public static class Whelp extends SKMob {

		{
			spriteClass = StarshipMobs.VarkolynSprite.class;
			HP = HT = 20;
			defenseSkill = 10;
			EXP = 0;
			maxLvl = -1;
		}

		private int lvl = 0;

		void grow( int level ){
			lvl = level;
			HP = HT = 20 + 5 * level;
		}

		@Override public int damageRoll() { return Random.NormalIntRange(3 + lvl / 2, 8 + lvl); }
		@Override public int attackSkill( Char target ) { return 15 + lvl; }
		@Override public int drRoll() { return super.drRoll() + Random.NormalIntRange(0, 2 + lvl / 3); }

		@Override
		public String name() {
			return Messages.get(HiveHeart.class, "whelp_name");
		}

		@Override
		public String description() {
			return Messages.get(HiveHeart.class, "whelp_desc");
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("lvl", lvl);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			lvl = bundle.getInt("lvl");
		}
	}

	//on the bearer's side, follows them around like a (purple) puppy
	public static class Bond extends AllyBuff {

		{
			type = buffType.POSITIVE;
		}

		public int owner = 0;

		@Override
		public boolean attachTo( Char target ) {
			if (super.attachTo(target)){
				if (target instanceof Mob) ((Mob) target).makeIntelligentAlly();
				return true;
			}
			return false;
		}

		@Override
		public boolean act() {
			spend(TICK);
			Hero h = ChampionHooks.heroByIndex(owner);
			if (h != null && h.isAlive() && target instanceof Mob){
				Mob m = (Mob) target;
				if (m.state != m.HUNTING && Dungeon.level.distance(m.pos, h.pos) > 2) m.followTo(h.pos);
			}
			return true;
		}

		@Override
		public int icon() {
			return BuffIndicator.HEART;
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("owner", owner);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			owner = bundle.getInt("owner");
		}
	}
}
