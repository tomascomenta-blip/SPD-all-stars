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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.audio.Sample;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.guns.GoblinPistol;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

//goblin guard w/ a pistol, shoots from a few cells away like an archer.
//base class for the other goblins that shoot
public class GoblinGunner extends Mob {

	{
		spriteClass = ForestSprites.GunnerSprite.class;

		HP = HT = 8;
		defenseSkill = 4;

		EXP = 2;
		maxLvl = 7;

		lootChance = 0.04f;
	}

	protected int range(){
		return 5;
	}

	//pistol goblin yells the first time it sees a hero, goblins around come over. snitch
	private boolean shouted = false;

	protected boolean raisesAlarm(){
		return getClass() == GoblinGunner.class;
	}

	@Override
	protected boolean act() {
		if (!shouted && raisesAlarm() && state == HUNTING && enemy != null
				&& fieldOfView != null && fieldOfView[enemy.pos]){
			shouted = true;
			int called = 0;
			for (Mob m : Dungeon.level.mobs){
				if (m != this && (ForestFeatures.isGoblin(m) || m instanceof Boar) && m.state != m.HUNTING
						&& Dungeon.level.distance(m.pos, pos) <= 6){
					m.beckon(enemy.pos);
					called++;
				}
			}
			if (sprite != null && Dungeon.level.heroFOV[pos]){
				sprite.showStatus(CharSprite.WARNING, Messages.get(GoblinGunner.class, "alarm"));
				Sample.INSTANCE.play(Assets.Sounds.ALERT, 0.8f, 1.3f);
			}
			CoopLog.line("goblin alarm at " + pos + " called=" + called);
		}
		return super.act();
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("shouted", shouted);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		shouted = bundle.getBoolean("shouted");
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 1, 4 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 10;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 1 );
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		if (super.canAttack(enemy)) return true;
		return Dungeon.level.distance(pos, enemy.pos) <= range()
				&& new Ballistica( pos, enemy.pos, Ballistica.PROJECTILE).collisionPos == enemy.pos;
	}

	@Override
	public float lootChance() {
		//each gun dropped makes the next one 1/3 as likely, same trick as the gnoll shaman wands
		return super.lootChance() * (float)StrictMath.pow(1/3f, Dungeon.LimitedDrops.SK_GUNS.count);
	}

	@Override
	public Item createLoot() {
		Dungeon.LimitedDrops.SK_GUNS.count++;
		return gun();
	}

	protected Item gun(){
		return new GoblinPistol();
	}
}
