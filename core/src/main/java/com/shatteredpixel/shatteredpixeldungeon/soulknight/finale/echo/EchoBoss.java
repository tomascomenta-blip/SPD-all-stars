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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.finale.echo;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Amok;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corruption;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Dread;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicalSleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Sleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKBulletField;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKWorlds;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.finale.RiftLevel;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.finale.ThalParts;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.finale.ThalVekhorr;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

/*
 an echo of a world boss, pulled out of its own time by the final boss.
 smaller and see-through, only 2-3 of the original's moves, no loot. comes out of a portal and fights
 until it breaks. never sleeps, never runs.
 the final boss spawns them with pos = cell + GameScene.add(), and just checks isAlive()/anyAlive()
*/
public abstract class EchoBoss extends SKMob {

	{
		state = HUNTING;
		EXP = 0;
		lootChance = 0;

		properties.add(Property.MINIBOSS);
		immunities.add(Sleep.class);
		immunities.add(MagicalSleep.class);
		immunities.add(Terror.class);
		immunities.add(Dread.class);
		immunities.add(Charm.class);
		immunities.add(Amok.class);
		immunities.add(Corruption.class);
	}

	//turns it spends stepping out of the portal. can't be hurt and does nothing meanwhile
	public static final int EMERGE = 2;
	public int emerge = EMERGE;
	//turns it holds together out of its own time, -1 = until it breaks (a lone hero's rift sets it)
	public int fade = -1;
	private float outAt = -1;

	public abstract int world();

	public static EchoBoss forWorld( int world ){
		switch (world){
			case SKWorlds.FOREST:    return new PriestEcho();
			case SKWorlds.KINGDOM:   return new KnightEcho();
			case SKWorlds.STARSHIP:  return new QueenEcho();
			case SKWorlds.UNDERSEA:  return new KrakenEcho();
			case SKWorlds.FORGOTTEN: return new TharogEcho();
			default: throw new IllegalArgumentException("no echo for world " + world);
		}
	}

	public static boolean anyAlive(){
		if (Dungeon.level == null) return false;
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof EchoBoss && m.isAlive()) return true;
		}
		return false;
	}

	public boolean emerging(){
		return emerge > 0;
	}

	@Override
	public boolean isInvulnerable( Class effect ) {
		return emerge > 0 || super.isInvulnerable(effect);
	}

	@Override
	protected void onAdd() {
		super.onAdd();
		if (emerge == EMERGE && outAt < 0){
			outAt = Actor.now();
			EchoFx.flare(this, pos, 2f);
			CoopLog.line("echo out " + getClass().getSimpleName() + " t=" + (int) Actor.now() + " hp=" + HT);
		}
	}

	@Override
	protected boolean act() {
		if (!isAlive()) return true;
		if (emerge > 0){
			if (--emerge == 0) arrived();
			spend(TICK);
			return true;
		}
		if (fade > 0 && --fade == 0){
			fadeAway();
			return true;
		}
		//no sleeping, no running, no wandering off
		if (state != HUNTING) state = HUNTING;
		boolean result = super.act();
		if (isAlive() && state != HUNTING){
			state = HUNTING;
			Char e = chooseEnemy();
			if (e != null) target = e.pos;
		}
		return result;
	}

	protected void arrived(){
		GLog.w(Messages.get(EchoBoss.class, "arrives", Messages.capitalize(name())));
		if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.CHARGEUP, 0.8f, 1.4f);
		cooldown = 1;
	}

	//it always knows where the heroes are. it's an echo, not a guard.
	//alone in the rift it goes for the hero, the summons don't get to hold it off for free
	@Override
	protected Char chooseEnemy() {
		if (ThalVekhorr.alone() && Dungeon.level instanceof RiftLevel
				&& Dungeon.hero != null && Dungeon.hero.isAlive()) return Dungeon.hero;
		Char e = super.chooseEnemy();
		if (e != null) return e;
		Char best = null;
		for (Hero h : Party.living()){
			if (best == null || Dungeon.level.distance(pos, h.pos) < Dungeon.level.distance(pos, best.pos)) best = h;
		}
		return best;
	}

	//back to its own time. the portal it came from is told, so it doesn't count as broken
	private void fadeAway(){
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof ThalParts.Portal && ((ThalParts.Portal) m).owns(id())) ((ThalParts.Portal) m).echoFaded();
		}
		GLog.i(Messages.get(ThalVekhorr.class, "echo_fades", Messages.capitalize(name())));
		CoopLog.line("echo fades " + getClass().getSimpleName() + " t=" + (int) Actor.now() + " hp=" + HP + "/" + HT);
		EchoFx.flare(this, pos, 1.5f);
		pendingKind = -1;
		SKBulletField field = (SKBulletField) Dungeon.level.blobs.get(SKBulletField.class);
		if (field != null) field.clearOwner(this);
		if (sprite != null) sprite.killAndErase();
		destroy();
	}

	@Override
	public void die( Object cause ) {
		pendingKind = -1;
		SKBulletField field = (SKBulletField) Dungeon.level.blobs.get(SKBulletField.class);
		if (field != null) field.clearOwner(this);
		super.die(cause);
		GLog.p(Messages.get(EchoBoss.class, "breaks", Messages.capitalize(name())));
		CoopLog.line("echo down " + getClass().getSimpleName() + " t=" + (int) Actor.now()
				+ " lasted=" + (outAt < 0 ? -1 : (int) (Actor.now() - outAt)));
	}

	// *** shared bits ***

	//special hit: half armor, like the world bosses
	protected void hitFoes( Iterable<Integer> cells, int min, int max ){
		for (Char ch : SKCombat.foesIn(this, cells)){
			ch.damage(special(ch, min, max), this);
		}
	}

	//special hit, a bit softer when the hero is alone in the rift
	protected int special( Char ch, int min, int max ){
		return ThalVekhorr.echoHit(SKCombat.specialDamage(this, ch, min, max));
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		return super.attackProc(enemy, ThalVekhorr.echoHit(damage));
	}

	//shove away from 'from', stops at walls and people
	protected void knock( Char ch, int from, int n ){
		int w = Dungeon.level.width();
		int dx = Integer.signum(ch.pos % w - from % w), dy = Integer.signum(ch.pos / w - from / w);
		if (dx == 0 && dy == 0) return;
		for (int i = 0; i < n; i++){
			int next = ch.pos + dx + dy * w;
			if (!Dungeon.level.passable[next] || Actor.findChar(next) != null) return;
			int old = ch.pos;
			ch.pos = next;
			Actor.add(new Pushing(ch, old, next));
			Dungeon.level.occupyCell(ch);
		}
	}

	//free cells near a spot, closest first (same order on every device)
	protected static ArrayList<Integer> freeNear( int center, int minD, int maxD ){
		ArrayList<Integer> out = new ArrayList<>();
		for (int d = minD; d <= maxD; d++){
			for (int c : SKCombat.area(center, d)){
				if (Dungeon.level.distance(c, center) != d) continue;
				if (!Dungeon.level.passable[c] || Dungeon.level.avoid[c] || Actor.findChar(c) != null) continue;
				out.add(c);
			}
		}
		return out;
	}

	private static final String EMERGE_KEY = "emerge";
	private static final String OUT_AT = "out_at";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(EMERGE_KEY, emerge);
		bundle.put("fade", fade);
		bundle.put(OUT_AT, outAt);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		emerge = bundle.getInt(EMERGE_KEY);
		fade = bundle.contains("fade") ? bundle.getInt("fade") : -1;
		outAt = bundle.getFloat(OUT_AT);
	}
}
