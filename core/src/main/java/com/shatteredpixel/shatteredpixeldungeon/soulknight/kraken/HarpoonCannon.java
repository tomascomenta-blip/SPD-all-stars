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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

//bow cannon. walk into it holding a harpoon to fire. open eye = counts as the eye hit,
//tentacle = big wound + stun, eye guard = dead guard
public class HarpoonCannon extends NPC {

	{
		spriteClass = Kraken.KrakenProxySprite.class;
		properties.add(Property.IMMOVABLE);
		properties.add(Property.INORGANIC);
	}

	@Override
	protected boolean act() {
		spend(TICK);
		return true;
	}

	@Override
	public boolean isInvulnerable( Class effect ) {
		return true;
	}

	@Override
	public void damage( int dmg, Object src ) {
	}

	@Override
	public boolean add( Buff buff ) {
		return false;
	}

	@Override
	public boolean reset() {
		return true;
	}

	@Override
	public boolean interact( Char c ) {
		if (!(c instanceof Hero)) return true;
		Hero h = (Hero) c;
		KrakenHarpoon harpoon = h.belongings.getItem(KrakenHarpoon.class);
		if (harpoon == null){
			GLog.i(Messages.get(this, "empty"));
			return true;
		}
		Char target = target();
		if (target == null){
			GLog.i(Messages.get(this, "nothing"));
			return true;
		}
		harpoon.detach(h.belongings.backpack);
		KrakenVisuals.harpoonShot(pos, target.pos);
		Sample.INSTANCE.play(Assets.Sounds.HIT_ARROW, 1f, 0.5f);
		if (target instanceof Kraken){
			target.damage(1, h);
		} else if (target instanceof GiantTentacle){
			GiantTentacle t = (GiantTentacle) target;
			int dmg = Math.max(10, Math.round(t.HT * 0.2f));
			t.hitFrom(t.sucker, dmg, this, segmentOrSelf(t));
			Kraken k = Kraken.find();
			if (k != null) k.stun(3);
			GLog.p(Party.active() ? Messages.get(this, "hit_tentacle", Party.name(h)) : Messages.get(this, "hit_tentacle_solo"));
		} else {
			target.damage(target.HP + 1, h);
		}
		h.spendAndNext(1f);
		return false;
	}

	private Char segmentOrSelf( GiantTentacle t ){
		int c = t.segmentCell(t.sucker);
		Char ch = c == -1 ? null : Actor.findChar(c);
		return ch != null ? ch : t;
	}

	//priority: eye > guard > tentacle
	private Char target(){
		Kraken k = Kraken.find();
		if (k != null && k.vulnerable()) return k;
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof GuardTentacle && m.isAlive()) return m;
		}
		if (k != null) return k.activeTentacle();
		return null;
	}

	@Override
	public String description() {
		return Messages.get(this, "desc");
	}
}
