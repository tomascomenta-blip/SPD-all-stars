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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.quests;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKBulletField;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.undersea.UnderseaMobs;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//ammonite = baby kraken that got out of the burst lab. the shell eats hits from the front (half dmg) until it cracks
// I   rolls in its shell along marked lines (can't be hurt while rolling), squirts ink, grabs anyone close w/ tentacles
// II  75%: shell cracks. no more half dmg but it gets faster and spins a water ring
// III 30%: calls daddy's servants. rude
public class Ammonite extends MiniBoss {

	{
		spriteClass = AmmoniteSprite.class;
		HP = HT = 160;
		defenseSkill = 12;
		EXP = 40;
		maxLvl = 24;
		lootChance = 0;
	}

	private static final int ROLL = 0, GRAB = 1;
	private boolean rolling = false;
	private boolean called = false;
	private int facing = -1;

	@Override public String barAsset() { return QuestAssets.BAR_AMMONITE; }
	@Override protected String music() { return Assets.Music.CITY_TENSE; }

	@Override public int damageRoll() { return Random.NormalIntRange(9, 16 + 2 * phase); }
	@Override public int attackSkill( Char target ) { return 24; }
	@Override public int drRoll() { return super.drRoll() + Random.NormalIntRange(phase >= 2 ? 0 : 2, phase >= 2 ? 3 : 6); }

	@Override
	public float speed() {
		return super.speed() * (phase >= 2 ? 1.25f : 1f);
	}

	@Override
	protected int specialCooldown() {
		return phase >= 3 ? 2 : 3;
	}

	@Override
	public String status() {
		if (rolling) return Messages.get(Ammonite.class, "status_roll");
		return Messages.get(Ammonite.class, "status_" + Math.min(3, phase));
	}

	@Override
	public int barColor() {
		return rolling ? 0xFFB0B0C0 : phase >= 2 ? 0xFFD06A40 : 0xFFB08A50;
	}

	@Override
	protected void onStart() {
		yell(Messages.get(Ammonite.class, "notice"));
		Sample.INSTANCE.play(Assets.Sounds.WATER, 1f, 0.5f);
	}

	@Override
	protected boolean act() {
		if (enemy != null && enemy.isAlive()) facing = enemy.pos;
		if (sprite instanceof AmmoniteSprite) ((AmmoniteSprite) sprite).cracked(phase >= 2);
		return super.act();
	}

	@Override
	public boolean isInvulnerable( Class effect ) {
		return rolling || super.isInvulnerable(effect);
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (phase == 1 && src instanceof Char && facing != -1 && TourneyFoes.inFront(pos, facing, ((Char) src).pos)){
			dmg = Math.max(1, dmg / 2);
		}
		super.damage(dmg, src);
		if (!isAlive()) return;
		if (phase == 1 && HP * 4 <= HT * 3){
			newPhase(2);
			Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 1.4f);
			if (Dungeon.level.heroFOV[pos]) CellEmitter.get(pos).burst(Speck.factory(Speck.ROCK), 8);
		} else if (phase == 2 && HP * 10 <= HT * 3){
			newPhase(3);
			callServants();
		}
	}

	private void callServants(){
		if (called) return;
		called = true;
		int made = 0;
		for (int k : PathFinder.NEIGHBOURS8){
			if (made >= 2) break;
			int c = pos + k * 2;
			if (c < 0 || c >= Dungeon.level.length() || !Dungeon.level.passable[c] || Actor.findChar(c) != null) continue;
			UnderseaMobs.KrakenMinion m = new UnderseaMobs.KrakenMinion();
			m.summoned();
			m.pos = c;
			GameScene.add(m);
			m.state = m.HUNTING;
			made++;
		}
		yell(Messages.get(Ammonite.class, "call"));
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = Dungeon.level.distance(pos, enemy.pos);
		if (d <= 2 && Random.Int(3) == 0){
			ArrayList<Integer> one = new ArrayList<>();
			one.add(enemy.pos);
			announce(GRAB, one, enemy.pos);
			return true;
		}
		if (phase >= 2 && Random.Int(3) == 0){
			ring(12, enemy.pos, 1f, ItemSpriteSheet.SHOT_BUBBLE, 8, 14);
			if (sprite instanceof SKMobSprite) ((SKMobSprite) sprite).cast();
			Sample.INSTANCE.play(Assets.Sounds.WATER, 1f, 1.2f);
			spend(TICK);
			return true;
		}
		if (d >= 2 && d <= 6){
			ArrayList<Integer> line = line(enemy.pos, 6);
			if (!line.contains(enemy.pos)) return false;
			rolling = true;
			announce(ROLL, line, enemy.pos);
			return true;
		}
		if (d <= 3){
			LabQuest.ink(pos);
			Sample.INSTANCE.play(Assets.Sounds.GAS, 0.8f, 0.8f);
			getFurther(enemy.pos);
			spend(TICK);
			return true;
		}
		return false;
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		if (kind == GRAB){
			Char ch = Actor.findChar(aim);
			if (ch == null || !SKCombat.foes(this, ch)) return;
			int to = -1;
			for (int k : PathFinder.NEIGHBOURS8){
				int c = pos + k;
				if (Dungeon.level.passable[c] && Actor.findChar(c) == null){
					if (to == -1 || Dungeon.level.distance(c, ch.pos) < Dungeon.level.distance(to, ch.pos)) to = c;
				}
			}
			if (to != -1 && !ch.properties().contains(Property.IMMOVABLE)){
				int from = ch.pos;
				ch.pos = to;
				Dungeon.level.occupyCell(ch);
				if (ch.sprite != null) ch.sprite.move(from, to);
			}
			Buff.prolong(ch, Roots.class, 2f);
			ch.damage(Math.max(1, Random.NormalIntRange(8, 14) - ch.drRoll() / 2), this);
			Sample.INSTANCE.play(Assets.Sounds.WATER, 1f, 0.7f);
			return;
		}
		rolling = false;
		int start = pos;
		Char victim = null;
		for (int c : cells){
			Char ch = Actor.findChar(c);
			if (ch != null){
				if (SKCombat.foes(this, ch)) victim = ch;
				break;
			}
			if (Dungeon.level.solid[c] || !Dungeon.level.passable[c]) break;
			move(c, false);
		}
		if (pos != start && sprite != null) sprite.move(start, pos);
		PixelScene.shake(2, 0.3f);
		if (victim != null){
			attack(victim, 1.4f, 0, 2f);
			if (victim.isAlive() && !victim.properties().contains(Property.IMMOVABLE)){
				Ballistica traj = new Ballistica(victim.pos, victim.pos + (victim.pos - pos), Ballistica.MAGIC_BOLT);
				WandOfBlastWave.throwChar(victim, traj, 2, false, true, this);
			}
		}
	}

	@Override
	public void die( Object cause ) {
		int at = pos;
		super.die(cause);
		Dungeon.level.drop(new Gold(Random.IntRange(150, 250)), at).sprite.drop();
		SKBulletField f = SKBulletField.get();
		if (f != null) f.clearOwner(this);
		CoopLog.line("ammonite slain");
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("rolling", rolling);
		bundle.put("called", called);
		bundle.put("facing", facing);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		rolling = bundle.getBoolean("rolling");
		called = bundle.getBoolean("called");
		facing = bundle.getInt("facing");
	}

	public static class AmmoniteSprite extends SKMobSprite {
		public AmmoniteSprite(){
			super();
			setup(QuestAssets.AMMONITE, 28, 24);
		}
		void cracked( boolean on ){
			row(on ? 1 : 0);
		}
	}
}
