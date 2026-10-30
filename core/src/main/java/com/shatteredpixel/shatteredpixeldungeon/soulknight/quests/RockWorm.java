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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.levels.CavesBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//rock worm. underground it can't be seen or hurt (you only see the floor shaking) and it pops up
//under a hero: marked 3x3, hurts + knocks back. while up it bites and spits rocks for a few turns,
//then digs back down. every time it surfaces part of the cave caves in (marked cells, rocks fall a turn later)
// II  60%: a larva comes up with it
// III 30%: faster underground, shorter time up
public class RockWorm extends MiniBoss {

	{
		spriteClass = RockWormSprite.class;
		HP = HT = 140;
		defenseSkill = 8;
		EXP = 30;
		maxLvl = 20;
		lootChance = 0;
		invisible = 1; //starts underground
	}

	private static final int EMERGE = 0;

	public boolean under = true;
	private int up = 0;          //turns left up top
	public int emerges = 0;
	private int[] falling = new int[0];
	private int fallTimer = 0;

	public RockWorm(){
		super();
		int d = Math.max(12, Math.min(14, Dungeon.depth));
		HP = HT = 140 + 15 * (d - 12);
	}

	@Override public String barAsset() { return QuestAssets.BAR_WORM; }
	@Override protected String music() { return Assets.Music.CAVES_TENSE; }

	@Override public int damageRoll() { return Random.NormalIntRange(8, 16 + 2 * phase); }
	@Override public int attackSkill( Char target ) { return 22; }
	@Override public int drRoll() { return super.drRoll() + Random.NormalIntRange(2, 8); }

	@Override
	public float speed() {
		return super.speed() * (under ? (phase >= 3 ? 2f : 1.5f) : 0.8f);
	}

	@Override
	protected int specialCooldown() {
		return 2;
	}

	@Override
	public String status() {
		String p = Messages.get(RockWorm.class, "status_" + Math.min(3, phase));
		return p + "   " + Messages.get(RockWorm.class, under ? "status_under" : "status_up");
	}

	@Override
	public int barColor() {
		return under ? 0xFF6A5A40 : 0xFFC08040;
	}

	@Override
	protected void onStart() {
		yell(Messages.get(RockWorm.class, "notice"));
		Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 0.6f);
		PixelScene.shake(3, 0.8f);
	}

	@Override
	public boolean isInvulnerable( Class effect ) {
		return under || super.isInvulnerable(effect);
	}

	private void burrow(){
		under = true;
		invisible = Math.max(invisible, 1);
		if (sprite != null){
			sprite.visible = false;
			if (Dungeon.level.heroFOV[pos]) CellEmitter.get(pos).burst(Speck.factory(Speck.ROCK), 6);
		}
		Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.7f, 0.8f);
	}

	private void surface(){
		under = false;
		invisible = 0;
		if (sprite != null){
			sprite.visible = true;
			sprite.place(pos);
		}
	}

	@Override
	protected boolean act() {
		if (fallTimer > 0 && --fallTimer == 0) rocksFall();
		if (started && !inIntro()){
			if (under){
				if (Dungeon.level.heroFOV[pos] && Random.Int(2) == 0) CellEmitter.get(pos).burst(Speck.factory(Speck.DUST), 2);
				Hero target = nearestHero();
				if (target == null){
					spend(TICK);
					return true;
				}
				if (Dungeon.level.distance(pos, target.pos) <= 1 && pendingKind == -1){
					ArrayList<Integer> area = new ArrayList<>();
					area.add(target.pos);
					for (int k : PathFinder.NEIGHBOURS8) if (!Dungeon.level.solid[target.pos + k]) area.add(target.pos + k);
					enemy = target;
					announce(EMERGE, area, target.pos);
					GLog.w(Messages.get(RockWorm.class, "rumble"));
					return true;
				}
				if (pendingKind != -1) return super.act();
				int old = pos;
				if (getCloser(target.pos)){
					spend(1 / speed());
					moveSprite(old, pos);
					return true;
				}
				spend(TICK);
				return true;
			}
			if (--up <= 0 && pendingKind == -1){
				burrow();
				spend(TICK);
				return true;
			}
		}
		return super.act();
	}

	private Hero nearestHero(){
		Hero best = null;
		ArrayList<Hero> list = new ArrayList<>();
		if (Party.active()) list.addAll(Party.living());
		else if (Dungeon.hero != null && Dungeon.hero.isAlive()) list.add(Dungeon.hero);
		for (Hero h : list){
			if (best == null || Dungeon.level.distance(pos, h.pos) < Dungeon.level.distance(pos, best.pos)) best = h;
		}
		return best;
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (under) return;
		super.damage(dmg, src);
		if (!isAlive()) return;
		if (phase == 1 && HP * 5 <= HT * 3) newPhase(2);
		else if (phase == 2 && HP * 10 <= HT * 3) newPhase(3);
	}

	@Override
	protected boolean special( Char enemy ) {
		if (under || Dungeon.level.distance(pos, enemy.pos) < 2) return false;
		arc(phase >= 2 ? 4 : 3, 40, enemy.pos, 1.5f, ItemSpriteSheet.SHOT_BIG_ORB, 4, 9);
		if (sprite instanceof SKMobSprite) ((SKMobSprite) sprite).cast();
		Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.6f, 1.3f);
		spend(TICK);
		return true;
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		if (kind != EMERGE) return;
		int land = pos;
		if (Actor.findChar(aim) == null && Dungeon.level.passable[aim]) land = aim;
		else for (int c : cells) if (Actor.findChar(c) == null && Dungeon.level.passable[c]){ land = c; break; }
		if (land != pos) move(land, false);
		surface();
		up = phase >= 3 ? 2 : 3;
		emerges++;
		PixelScene.shake(3, 0.6f);
		Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 0.7f);
		if (Dungeon.level.heroFOV[pos]) CellEmitter.get(pos).burst(Speck.factory(Speck.ROCK), 10);
		for (Char ch : SKCombat.foesIn(this, cells)){
			ch.damage(Math.max(1, Random.NormalIntRange(6, 12) - ch.drRoll() / 2), this);
			if (ch.isAlive() && !ch.properties().contains(Property.IMMOVABLE) && ch.pos != pos){
				Ballistica traj = new Ballistica(ch.pos, ch.pos + (ch.pos - pos), Ballistica.MAGIC_BOLT);
				WandOfBlastWave.throwChar(ch, traj, 1, false, true, this);
			}
		}
		if (phase >= 2) larva();
		collapse();
		CoopLog.line("rock worm comes up at " + pos + " (" + emerges + ")");
	}

	private void larva(){
		for (int k : PathFinder.NEIGHBOURS8){
			int c = pos + k;
			if (!Dungeon.level.passable[c] || Actor.findChar(c) != null) continue;
			Larva l = new Larva();
			l.summoned();
			l.pos = c;
			GameScene.add(l);
			l.state = l.HUNTING;
			return;
		}
	}

	//ceiling gives way around it: marked now, rocks fall next turn
	private void collapse(){
		Level L = Dungeon.level;
		ArrayList<Integer> cells = new ArrayList<>();
		for (int tries = 0; tries < 40 && cells.size() < 5; tries++){
			int c = pos + Random.IntRange(-4, 4) + Random.IntRange(-4, 4) * L.width();
			if (c < 0 || c >= L.length() || L.solid[c] || !L.passable[c] || cells.contains(c) || c == pos) continue;
			if (L.getTransition(c) != null) continue;
			cells.add(c);
		}
		if (cells.isEmpty()) return;
		falling = new int[cells.size()];
		for (int i = 0; i < falling.length; i++) falling[i] = cells.get(i);
		fallTimer = 2;
		SKCombat.mark(cells, SKCombat.announceDelay(TICK));
	}

	private void rocksFall(){
		Level L = Dungeon.level;
		ArrayList<Integer> cells = new ArrayList<>();
		for (int c : falling) cells.add(c);
		falling = new int[0];
		Sample.INSTANCE.play(Assets.Sounds.ROCKS);
		for (int c : cells){
			if (L.heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.ROCK), 4);
			if (L.map[c] == Terrain.EMPTY || L.map[c] == Terrain.GRASS){
				Level.set(c, Terrain.EMPTY_DECO, L);
				GameScene.updateMap(c);
			}
		}
		for (Char ch : SKCombat.foesIn(this, cells)){
			ch.damage(Math.max(1, Random.NormalIntRange(6, 12) - ch.drRoll() / 2), this);
		}
	}

	@Override
	public void die( Object cause ) {
		int at = pos;
		super.die(cause);
		WormQuest.Q.slain = true;
		if (!(Dungeon.level instanceof CavesBossLevel)){
			WormQuest.Q.bonus = emerges <= 6;
			Dungeon.level.drop(new WormQuest.WormTooth(), at).sprite.drop();
			GLog.p(Messages.get(RockWorm.class, emerges <= 6 ? "slain_bonus" : "slain"));
		}
		CoopLog.line("rock worm slain after " + emerges + " times up");
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("under", under);
		bundle.put("up", up);
		bundle.put("emerges", emerges);
		bundle.put("falling", falling);
		bundle.put("fall_timer", fallTimer);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		under = bundle.getBoolean("under");
		up = bundle.getInt("up");
		emerges = bundle.getInt("emerges");
		falling = bundle.contains("falling") ? bundle.getIntArray("falling") : new int[0];
		fallTimer = bundle.getInt("fall_timer");
		invisible = under ? 1 : 0;
	}

	public static class RockWormSprite extends SKMobSprite {
		public RockWormSprite(){
			super();
			setup(QuestAssets.ROCK_WORM, 24, 24);
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (ch instanceof RockWorm && ((RockWorm) ch).under) visible = false;
		}
	}

	public static class Larva extends SKMob {

		{
			spriteClass = LarvaSprite.class;
			HP = HT = 18;
			defenseSkill = 10;
			EXP = 0;
		}

		@Override public int damageRoll() { return Random.NormalIntRange(4, 9); }
		@Override public int attackSkill( Char target ) { return 20; }
	}

	public static class LarvaSprite extends SKMobSprite {
		public LarvaSprite(){
			super();
			setup(QuestAssets.WORM_LARVA, 14, 10);
		}
	}
}
