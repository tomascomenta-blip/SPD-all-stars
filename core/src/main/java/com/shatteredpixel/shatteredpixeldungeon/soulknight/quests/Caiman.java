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
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Splash;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.SewerBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//the sewer caiman. underwater it barely takes dmg (x0.3), on land it's slow and squishy (x1.5).
//pops up for a few turns when it attacks
// I   bites, lunges out of the water along marked lines
// II  2/3 hp: death roll (grabs whoever is next to it, drags them in) + tail sweep
// III 1/3: longer lunges, calls the channel rats
public class Caiman extends MiniBoss {

	{
		spriteClass = CaimanSprite.class;
		HP = HT = 50;
		defenseSkill = 7;
		EXP = 20;
		maxLvl = 12;
		lootChance = 0;
	}

	private static final int LUNGE = 0, ROLL = 1, SWEEP = 2;

	public boolean submerged = false;
	private int surfaced = 0;
	private boolean ratsCalled = false;

	public Caiman(){
		super();
		int d = Math.max(2, Math.min(4, Dungeon.depth));
		HP = HT = 50 + 10 * (d - 2);
	}

	private int depthBonus(){
		return Math.max(0, Math.min(4, Dungeon.depth) - 2);
	}

	private boolean onLand(){
		return !Dungeon.level.water[pos];
	}

	@Override
	public String barAsset() {
		return QuestAssets.BAR_CAIMAN;
	}

	@Override
	protected String music() {
		return Assets.Music.SEWERS_TENSE;
	}

	@Override
	public String status() {
		String p = Messages.get(Caiman.class, "status_" + Math.min(3, phase));
		if (submerged) return p + "   " + Messages.get(Caiman.class, "status_under");
		if (onLand()) return p + "   " + Messages.get(Caiman.class, "status_land");
		return p;
	}

	@Override
	public int barColor() {
		if (submerged) return 0xFF3A6A8A;
		if (onLand()) return 0xFFE0A030;
		return 0xFF4E8A3A;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(3 + depthBonus(), 7 + depthBonus() + phase);
	}

	@Override
	public int attackSkill( Char target ) {
		return 12 + depthBonus();
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(1, 3);
	}

	@Override
	public float speed() {
		return super.speed() * (onLand() ? 0.75f : 1.5f);
	}

	@Override
	protected int specialCooldown() {
		return phase >= 3 ? 2 : 3;
	}

	@Override
	protected void onStart() {
		yell(Messages.get(Caiman.class, "notice"));
		Sample.INSTANCE.play(Assets.Sounds.WATER, 1f, 0.6f);
		PixelScene.shake(2, 0.4f);
	}

	@Override
	protected void onIntro( int left ) {
		if (Dungeon.level.heroFOV[pos]) Splash.at(pos, 0xFF5A8A60, 6);
	}

	@Override
	protected boolean act() {
		if (onLand()){
			submerged = false;
		} else if (surfaced > 0){
			surfaced--;
			submerged = false;
		} else {
			submerged = true;
		}
		if (sprite instanceof CaimanSprite) ((CaimanSprite) sprite).under(submerged);
		return super.act();
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (submerged) dmg = Math.max(1, dmg * 3 / 10);
		else if (onLand()) dmg = dmg * 3 / 2;
		super.damage(dmg, src);
		if (!isAlive()) return;
		if (phase == 1 && HP * 3 <= HT * 2) newPhase(2);
		else if (phase == 2 && HP * 3 <= HT){
			newPhase(3);
			callRats();
		}
	}

	private void callRats(){
		if (ratsCalled) return;
		ratsCalled = true;
		int made = 0;
		for (int i = 0; i < Dungeon.level.length() && made < 2; i++){
			if (!Dungeon.level.water[i] || Actor.findChar(i) != null || Dungeon.level.distance(i, pos) > 5) continue;
			Rat r = new Rat();
			r.EXP = 0;
			r.pos = i;
			GameScene.add(r);
			r.state = r.HUNTING;
			if (Dungeon.level.heroFOV[i]) Splash.at(i, 0xFF5A8A60, 4);
			made++;
		}
		if (made > 0) yell(Messages.get(Caiman.class, "rats"));
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = Dungeon.level.distance(pos, enemy.pos);
		//on land and nobody near -> back in the water, como un cobarde
		if (onLand() && d >= 2){
			int w = nearestWater(pos, 6);
			if (w != -1 && Random.Int(3) > 0){
				int old = pos;
				if (getCloser(w)){
					spend(1 / speed());
					moveSprite(old, pos);
					return true;
				}
			}
		}
		if (phase >= 2 && d == 1){
			if (Random.Int(3) == 0){
				ArrayList<Integer> ring = new ArrayList<>();
				for (int k : PathFinder.NEIGHBOURS8) ring.add(pos + k);
				announce(SWEEP, ring, enemy.pos);
				return true;
			}
			if (Random.Int(2) == 0){
				ArrayList<Integer> one = new ArrayList<>();
				one.add(enemy.pos);
				announce(ROLL, one, enemy.pos);
				if (sprite != null) sprite.showStatus(0xFF4040, Messages.get(Caiman.class, "roll"));
				return true;
			}
		}
		int reach = phase >= 3 ? 7 : 5;
		if (d >= 2 && d <= reach){
			ArrayList<Integer> line = line(enemy.pos, reach);
			if (!line.contains(enemy.pos)) return false;
			announce(LUNGE, line, enemy.pos);
			return true;
		}
		return false;
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		surfaced = 3;
		submerged = false;
		if (kind == LUNGE){
			lunge(cells);
		} else if (kind == ROLL){
			Char ch = Actor.findChar(aim);
			if (ch == null || !SKCombat.foes(this, ch) || !Dungeon.level.adjacent(pos, ch.pos)) return;
			ch.damage(Math.max(1, Random.NormalIntRange(4 + depthBonus(), 9 + depthBonus()) - ch.drRoll() / 2), this);
			if (!ch.isAlive()) return;
			int water = -1;
			for (int k : PathFinder.NEIGHBOURS8){
				int c = pos + k;
				if (Dungeon.level.water[c] && Dungeon.level.passable[c] && Actor.findChar(c) == null){
					water = c;
					break;
				}
			}
			if (water != -1 && !ch.properties().contains(Property.IMMOVABLE) && !ch.properties().contains(Property.BOSS)){
				int from = ch.pos;
				ch.pos = water;
				Dungeon.level.occupyCell(ch);
				if (ch.sprite != null) ch.sprite.move(from, water);
				Splash.at(water, 0xFF5A8A60, 8);
			}
			Buff.prolong(ch, Roots.class, 2f);
			Sample.INSTANCE.play(Assets.Sounds.WATER, 1f, 0.8f);
			if (ch == Dungeon.hero) GLog.n(Messages.get(Caiman.class, "dragged"));
		} else {
			Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG, 1f, 0.7f);
			for (Char ch : SKCombat.foesIn(this, cells)){
				ch.damage(Math.max(1, Random.NormalIntRange(2 + depthBonus(), 6 + depthBonus()) - ch.drRoll() / 2), this);
				if (ch.isAlive() && !ch.properties().contains(Property.IMMOVABLE)){
					Ballistica traj = new Ballistica(ch.pos, ch.pos + (ch.pos - pos), Ballistica.MAGIC_BOLT);
					WandOfBlastWave.throwChar(ch, traj, 1, false, true, this);
				}
			}
		}
	}

	private void lunge( ArrayList<Integer> cells ){
		Level L = Dungeon.level;
		int start = pos;
		Char victim = null;
		for (int c : cells){
			Char ch = Actor.findChar(c);
			if (ch != null){
				if (SKCombat.foes(this, ch)) victim = ch;
				break;
			}
			if (L.solid[c] || !L.passable[c]) break;
			move(c, false);
		}
		if (pos != start && sprite != null){
			sprite.move(start, pos);
			if (L.heroFOV[start] && L.water[start]) Splash.at(start, 0xFF5A8A60, 8);
		}
		Sample.INSTANCE.play(Assets.Sounds.WATER, 1f, 1.1f);
		if (victim != null) attack(victim, 1.5f, 0, 2f);
	}

	private int nearestWater( int from, int max ){
		int best = -1;
		for (int i = 0; i < Dungeon.level.length(); i++){
			if (!Dungeon.level.water[i] || !Dungeon.level.passable[i] || Actor.findChar(i) != null) continue;
			int d = Dungeon.level.distance(from, i);
			if (d <= max && (best == -1 || d < Dungeon.level.distance(from, best))) best = i;
		}
		return best;
	}

	@Override
	public void die( Object cause ) {
		int at = pos;
		boolean dry = onLand();
		super.die(cause);
		CaimanQuest.Q.slain = true;
		if (!(Dungeon.level instanceof SewerBossLevel)){
			CaimanQuest.Q.bonus = dry;
			Dungeon.level.drop(new CaimanQuest.CaimanTooth(), at).sprite.drop();
			GLog.p(Messages.get(Caiman.class, dry ? "slain_dry" : "slain"));
		}
		if (Dungeon.level.heroFOV[at]) CellEmitter.get(at).burst(Speck.factory(Speck.BUBBLE), 8);
		CoopLog.line("caiman slain dry=" + dry);
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("submerged", submerged);
		bundle.put("surfaced", surfaced);
		bundle.put("rats", ratsCalled);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		submerged = bundle.getBoolean("submerged");
		surfaced = bundle.getInt("surfaced");
		ratsCalled = bundle.getBoolean("rats");
	}

	public static class CaimanSprite extends SKMobSprite {
		public CaimanSprite(){
			super();
			setup(QuestAssets.CAIMAN, 32, 16);
		}
		void under( boolean on ){
			row(on ? 1 : 0);
		}
	}
}
