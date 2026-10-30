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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forgotten.ForgottenFeatures;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.props.SKProps;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

//portable totem the high priest carved for his guard. charges while the bearer fights.
//plant it next to you and for a few turns it shields everyone on your side standing close
public class PortableTotem extends Artifact {

	public static final String AC_PLANT = "PLANT";

	{
		image = ItemSpriteSheet.PORTABLE_TOTEM;
		levelCap = 10;
		charge = 0;
		chargeCap = 100;
		defaultAction = AC_PLANT;
		unique = true;
	}

	@Override
	public int image() {
		return charge >= chargeCap ? ItemSpriteSheet.PORTABLE_TOTEM_LIT : ItemSpriteSheet.PORTABLE_TOTEM;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions(hero);
		if (isEquipped(hero) && !cursed) actions.add(AC_PLANT);
		return actions;
	}

	private int turns(){
		return 6 + level() / 2;
	}

	private int shield(){
		return 3 + level();
	}

	@Override
	public void execute( Hero hero, String action ) {
		super.execute(hero, action);
		if (!action.equals(AC_PLANT)) return;
		if (!isEquipped(hero)){
			GLog.i(Messages.get(Artifact.class, "need_to_equip"));
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
		Planted t = new Planted();
		t.pos = cell;
		t.left = turns();
		t.shield = shield();
		GameScene.add(t);
		if (Dungeon.level.heroFOV[cell]) CellEmitter.get(cell).burst(Speck.factory(Speck.LIGHT), 6);
		Sample.INSTANCE.play(Assets.Sounds.CHARMS, 1f, 0.9f);
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
		return new Carving();
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
			desc += "\n\n" + Messages.get(this, charge >= chargeCap ? "desc_ready" : "desc_charging", shield(), turns());
		}
		return desc;
	}

	//charges while there's a fight in sight
	public class Carving extends ArtifactBuff {
		@Override
		public boolean act() {
			if (charge < chargeCap && !cursed && target instanceof Hero
					&& ((Hero) target).visibleEnemies() > 0){
				partialCharge += 1.4f + 0.15f * level();
				while (partialCharge >= 1 && charge < chargeCap){
					partialCharge--;
					charge++;
				}
				if (charge >= chargeCap) GLog.p(Messages.get(PortableTotem.class, "ready"));
				updateQuickslot();
			}
			spend(TICK);
			return true;
		}
	}

	//planted: every other turn, shield for each hero/ally within 2 cells
	public static class Planted extends SKProps.SKProp {

		{
			spriteClass = PlantedSprite.class;
			alignment = Alignment.ALLY;
		}

		public int left = 6;
		public int shield = 3;
		private int timer = 0;

		public Planted(){
			super();
			HP = HT = 12 + 2 * Dungeon.scalingDepth();
		}

		@Override
		protected boolean act() {
			spend(TICK);
			if (--left <= 0){
				vanish();
				return true;
			}
			if (--timer > 0) return true;
			timer = 2;
			boolean any = false;
			for (Char ch : Actor.chars()){
				if (ch == this || !ch.isAlive() || ch.alignment != Alignment.ALLY || Char.hasProp(ch, Property.IMMOVABLE)) continue;
				if (Dungeon.level.distance(ch.pos, pos) > 2) continue;
				Barrier b = ch.buff(Barrier.class);
				if (b != null && b.shielding() >= shield) continue;
				Buff.affect(ch, Barrier.class).setShield(shield);
				any = true;
				if (ch.sprite != null && Dungeon.level.heroFOV[ch.pos]) ch.sprite.showStatus(CharSprite.POSITIVE, "+" + shield);
			}
			if (any && Dungeon.level.heroFOV[pos]){
				CellEmitter.get(pos).burst(Speck.factory(Speck.LIGHT), 3);
				Sample.INSTANCE.play(Assets.Sounds.CHARMS, 0.5f, 1.2f);
			}
			return true;
		}

		private void vanish(){
			if (Dungeon.level.heroFOV[pos]) CellEmitter.get(pos).burst(Speck.factory(Speck.LIGHT), 4);
			destroy();
			if (sprite != null) sprite.die();
		}

		@Override
		public String name() {
			return Messages.get(PortableTotem.class, "planted_name");
		}

		@Override
		public String description() {
			return Messages.get(PortableTotem.class, "planted_desc", shield, left);
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("left", left);
			bundle.put("shield", shield);
			bundle.put("timer", timer);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			left = bundle.getInt("left");
			shield = bundle.getInt("shield");
			timer = bundle.getInt("timer");
		}
	}

	//portable_totem.png 16x24: 0-1 glow, 2-4 fade out
	public static class PlantedSprite extends ForgottenFeatures.StripSprite {
		public PlantedSprite(){
			super();
			strip(PriestAssets.PORTABLE, 16, 24, 3);
			idle = new Animation(3, true);
			idle.frames(film, 0, 1);
			run = idle.clone();
			attack = idle.clone();
			zap = idle.clone();
			die = new Animation(8, false);
			die.frames(film, 2, 3, 4);
			play(idle);
		}
	}
}
