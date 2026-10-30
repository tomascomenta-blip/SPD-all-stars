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

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ChampionEnemy;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.PurpleParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

//hand of baron on an ally: big boi mode
//  +50% size, +50% max health (and health when it starts), +50% damage (Char.attack, via damageFactor),
//  +25% accuracy and evasion (see Skill).
//once per ally. we keep the HT bonus here so it can be taken back
public class BaronEmpowered extends Buff {

	{
		type = buffType.POSITIVE;
		revivePersists = true;
	}

	public static final float FACTOR = 1.5f;
	public static final float SIZE = 1.5f;
	public static final float SKILL = 1.25f;
	public static final int COLOR = 0x9933FF;

	//max health of the ally without the bonus, and with it
	public int baseHT = 0;
	public int boostedHT = 0;

	//multiplier for the damage of a character's attacks
	public static float damageFactor( Char ch ){
		return ch.buff(BaronEmpowered.class) != null ? FACTOR : 1f;
	}

	public static boolean canEmpower( Char ch ){
		return ch instanceof Mob && ch.alignment == Char.Alignment.ALLY && ch.isAlive()
				&& ch.buff(BaronEmpowered.class) == null;
	}

	public static void empower( Char ch ){
		if (!canEmpower(ch)) return;
		BaronEmpowered b = Buff.affect(ch, BaronEmpowered.class);
		if (b.target != ch) return;
		b.baseHT = ch.HT;
		b.boostedHT = Math.round(ch.HT * FACTOR);
		ch.HT = b.boostedHT;
		ch.HP = Math.min(ch.HT, Math.round(ch.HP * FACTOR));
		Buff.affect(ch, Skill.class);
		if (ch.sprite != null){
			ch.sprite.showStatus(CharSprite.POSITIVE, Messages.get(BaronEmpowered.class, "empowered"));
			if (ch.sprite.visible) ch.sprite.emitter().burst(PurpleParticle.BURST, 15);
		}
	}

	@Override
	public boolean act() {
		//an ally that changes sides loses the blessing
		if (target.alignment != Char.Alignment.ALLY){
			detach();
			return true;
		}
		//some allies set their own max health again (the ghost every turn, wards when upgraded...)
		if (boostedHT > 0 && target.HT != boostedHT){
			baseHT = target.HT;
			boostedHT = Math.round(baseHT * FACTOR);
			target.HT = boostedHT;
		}
		if (target.buff(Skill.class) == null){
			Buff.affect(target, Skill.class);
		}
		spend(TICK);
		return true;
	}

	@Override
	public void detach() {
		if (target != null){
			if (target.isAlive() && boostedHT > 0 && target.HT == boostedHT){
				target.HT = baseHT;
				target.HP = Math.min(target.HP, target.HT);
			}
			Buff.detach(target, Skill.class);
		}
		super.detach();
	}

	//bigger sprite: also called again when the sprite is made after loading a game
	@Override
	public void fx( boolean on ) {
		CharSprite s = target.sprite;
		if (s == null) return;
		if (on){
			s.scale.set(SIZE);
			s.aura(COLOR, 4);
		} else {
			s.scale.set(1f);
			s.clearAura();
		}
		if (!s.isMoving) s.place(target.pos);
	}

	@Override
	public int icon() {
		return BuffIndicator.UPGRADE;
	}

	@Override
	public void tintIcon( Image icon ) {
		icon.hardlight(COLOR);
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", Math.round((FACTOR-1)*100), Math.round((SKILL-1)*100));
	}

	private static final String BASE_HT = "base_ht";
	private static final String BOOSTED_HT = "boosted_ht";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(BASE_HT, baseHT);
		bundle.put(BOOSTED_HT, boostedHT);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		baseHT = bundle.getInt(BASE_HT);
		boostedHT = bundle.getInt(BOOSTED_HT);
	}

	//Accuracy and evasion: the game already scales them for champion buffs (Char.hit), so a hidden
	//champion buff does it. It is not brought back from saves, because a champion buff on a mob makes
	//the floor warn about "a deadly presence" when it loads: BaronEmpowered adds it again on its next turn.
	public static class Skill extends ChampionEnemy {

		{
			type = buffType.POSITIVE;
			//champions resist being turned into allies, but this one is already an ally
			immunities.remove(AllyBuff.class);
		}

		private boolean fromSave = false;

		@Override
		public boolean attachTo( Char target ) {
			if (fromSave) return false;
			return super.attachTo(target);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			fromSave = true;
		}

		@Override
		public float evasionAndAccuracyFactor() {
			return SKILL;
		}

		@Override
		public float meleeDamageFactor() {
			return 1f;
		}

		@Override
		public void fx( boolean on ) {
			//the look is BaronEmpowered's
		}

		@Override
		public int icon() {
			return BuffIndicator.NONE;
		}

		@Override
		public String name() {
			return Messages.get(BaronEmpowered.class, "name");
		}

		@Override
		public String desc() {
			return Messages.get(BaronEmpowered.class, "desc", Math.round((FACTOR-1)*100), Math.round((SKILL-1)*100));
		}
	}
}
