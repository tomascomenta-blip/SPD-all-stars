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
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKBulletField;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKChampion;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.guns.GoblinShotgun;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

//elite goblin guard w/ a shotgun. at range the pellet spread also hits whoever's next to the target (weaker).
//now and then a cone of 5 visible pellets, plus a metal sheet that halves ranged hits from the front
public class GoblinElite extends GoblinGunner {

	{
		spriteClass = ForestSprites.EliteSprite.class;

		HP = HT = 18;
		defenseSkill = 5;

		EXP = 4;
		maxLvl = 9;

		lootChance = 0.06f;

		HUNTING = new Hunting();
	}

	private int coneCooldown = 2;

	@Override
	protected boolean act() {
		if (coneCooldown > 0) coneCooldown--;
		return super.act();
	}

	private class Hunting extends Mob.Hunting {
		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			if (enemyInFOV && enemy != null && coneCooldown <= 0){
				int d = Dungeon.level.distance(pos, enemy.pos);
				if (d >= 2 && d <= 6){
					enemySeen = true;
					cone(enemy);
					return true;
				}
			}
			return super.act(enemyInFOV, justAlerted);
		}
	}

	private void cone( Char target ){
		coneCooldown = 4;
		int n = SKChampion.more(this, 5);
		float mid = SKBulletField.angleTo(pos, target.pos);
		for (int i = 0; i < n; i++){
			float deg = mid - 25f + i*50f/(n-1);
			SKBulletField.get().fire(this, pos, deg, 1.5f, ItemSpriteSheet.SHOT_PELLET, 2, 4);
		}
		if (sprite != null){
			sprite.turnTo(pos, target.pos);
			if (sprite instanceof SKMobSprite) ((SKMobSprite) sprite).cast();
		}
		if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.BLAST, 0.8f, 1.3f);
		spend(TICK);
	}

	//who's behind a hit, to know where it came from
	private static Char attackerOf( Object src ){
		if (src instanceof Char) return (Char) src;
		if (src instanceof SKBulletField.Shot) return ((SKBulletField.Shot) src).owner;
		if (src instanceof Item) return Dungeon.hero;
		return null;
	}

	@Override
	public void damage( int dmg, Object src ) {
		Char from = attackerOf(src);
		if (dmg > 0 && from != null && enemy != null && from.pos != pos
				&& !Dungeon.level.adjacent(pos, from.pos) && facing(from.pos)){
			dmg = (dmg + 1)/2;
			if (sprite != null && Dungeon.level.heroFOV[pos]){
				sprite.showStatus(CharSprite.NEUTRAL, Messages.get(GoblinElite.class, "plate"));
				CellEmitter.center(pos).burst(SparkParticle.FACTORY, 3);
			}
		}
		super.damage(dmg, src);
	}

	//is cell in front of it (toward the hero it's fighting)? 60 deg each side
	private boolean facing( int cell ){
		int w = Dungeon.level.width();
		int ex = enemy.pos % w - pos % w, ey = enemy.pos / w - pos / w;
		int ax = cell % w - pos % w, ay = cell / w - pos / w;
		double dot = ex*ax + ey*ay;
		double len = Math.sqrt((double)(ex*ex + ey*ey) * (ax*ax + ay*ay));
		return len > 0 && dot / len >= 0.5;
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("cone_cooldown", coneCooldown);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		coneCooldown = bundle.getInt("cone_cooldown");
	}

	@Override
	protected int range() {
		return 4;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 2, 6 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 11;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 1 );
	}

	private boolean spreading = false;

	@Override
	public boolean attack( Char enemy, float dmgMulti, float dmgBonus, float accMulti ) {
		boolean result = super.attack(enemy, dmgMulti, dmgBonus, accMulti);
		if (!spreading && enemy != null && !Dungeon.level.adjacent(pos, enemy.pos)){
			spreading = true;
			for (Char ch : SKCombat.foesIn(this, SKCombat.area(enemy.pos, 1))){
				if (ch != enemy && new Ballistica(pos, ch.pos, Ballistica.PROJECTILE).collisionPos == ch.pos){
					super.attack(ch, 0.5f, 0, 1f);
				}
			}
			spreading = false;
		}
		return result;
	}

	@Override
	protected Item gun() {
		return new GoblinShotgun();
	}
}
