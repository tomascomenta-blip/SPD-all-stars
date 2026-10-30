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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.forgotten;

import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SnowParticle;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.props.SKProps;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.quests.YetiQuest;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Amok;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corruption;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Sleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKBoss;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

/*
 Tharog, heart of the valley. big stone+moss beast, its heart keeps the land warm. 4 phases:
 1 trapped 100-75: just head + claw out of the ice. icy breath, roar drops icicles, wolves/sabertooths
 2 free 75-50: hall goes green around it, steam breath, tail, stomp -> tar, charges at its prey.
   charge into an ice pillar = stunned + loses a back plate (3 total). raptors, pteros
 3 glaciation 50-25: ice creeps in from the walls, hug the braziers. tribe shows up if you did the flame quest
 4 extinction 25-0: red sky, meteors on marked cells. they hit him too lol
*/
public class Tharog extends SKBoss {

	{
		spriteClass = TharogSprite.class;
		HP = HT = 720;
		defenseSkill = 22;
		EXP = 80;
		baseSpeed = 0.9f;
		properties.add(Property.LARGE);
		properties.add(Property.INORGANIC);
		immunities.add(Chill.class);
		immunities.add(Frost.class);
		immunities.add(Paralysis.class);
		immunities.add(Sleep.class);
		immunities.add(Terror.class);
		immunities.add(Amok.class);
		immunities.add(Charm.class);
		immunities.add(Corruption.class);
	}

	public int phase = 1;
	//how hard he hits (-Dspd.test.duel puts him up against a colossus, one on one)
	private float power = 1f;
	public int plates = 3;
	private int plateDamage = 0;
	public int stunned = 0;
	public int heat = 0;        //p2, green radius
	public int frost = 0;       //p3, cuánto ha avanzado el hielo
	public int arenaVersion = 0;
	private int prey = -1;
	private int preyTimer = 10;
	private int summonTimer = 6;
	private int meteorTimer = 1;
	private boolean introDone = false;

	private static final int BREATH = 0, ROAR = 1, SWEEP = 2, STOMP = 3, CHARGE = 4, METEORS = 5;

	@Override protected int scoreIndex() { return 4; }
	@Override protected int score() { return 5000; }

	@Override public int damageRoll() { return Math.round(power * Random.NormalIntRange(24, 36)); }
	@Override public int attackSkill( Char target ) { return 42; }
	@Override public int drRoll() { return super.drRoll() + Random.NormalIntRange(0, 4 + 4 * plates); }

	@Override
	protected int specialCooldown() {
		return phase >= 4 ? Random.IntRange(1, 2) : Random.IntRange(2, 3);
	}

	//heat stuff

	private int lairCell(){
		return pos;
	}

	public float warmthAt( int cell ){
		switch (phase){
			case 1: return 0.12f;
			case 2: return Dungeon.level.distance(cell, lairCell()) <= heat ? 0.8f : 0.25f;
			case 3: return edgeDistance(cell) < frost ? 0.1f : 0.7f;
			default: return 0.85f;
		}
	}

	//only hurts in phase 3
	public boolean freezing( int cell ){
		return phase == 3 && edgeDistance(cell) < frost;
	}

	private int edgeDistance( int cell ){
		int best = 99;
		int w = Dungeon.level.width();
		int x = cell % w, y = cell / w;
		for (int d = 1; d < 12; d++){
			if (x - d < 0 || x + d >= w || y - d < 0 || y + d >= Dungeon.level.height()) return d - 1;
			if (Dungeon.level.solid[cell - d] || Dungeon.level.solid[cell + d]
					|| Dungeon.level.solid[cell - d * w] || Dungeon.level.solid[cell + d * w]) return d - 1;
		}
		return best;
	}

	@Override
	public void notice() {
		super.notice();
		if (!introDone){
			introDone = true;
			if (DevFlags.duel() > 0){
				HT *= 5;
				HP = HT;
				power = 1.5f;
			}
			TharogHud.banner(Messages.get(Tharog.class, "banner_wake"));
			GLog.w(Messages.get(Tharog.class, "hint_1"));
			YetiQuest.tharogFight();
			later(new Runnable() { public void run() {
				PixelScene.shake(5, 2f);
				GameScene.flash(0x80FFFFFF);
			} });
			Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 0.5f);
			Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 1f, 0.5f);
			//icicles fall on the roar. just for show, nobody gets hurt yet
			for (int i = 0; i < 10; i++){
				int c = pos + Random.IntRange(-6, 6) + Random.IntRange(-2, 7) * Dungeon.level.width();
				if (c >= 0 && c < Dungeon.level.length() && Dungeon.level.passable[c]) ForgottenVisuals.icicle(c);
			}
		}
	}

	private static void later( final Runnable r ){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				r.run();
			}
		});
	}

	@Override
	protected boolean getCloser( int target ) {
		if (phase == 1 || stunned > 0) return false;
		return super.getCloser(target);
	}

	@Override
	protected boolean getFurther( int target ) {
		if (phase == 1 || stunned > 0) return false;
		return super.getFurther(target);
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		if (stunned > 0) return false;
		//reach 2 while trapped, otherwise you could just stand there and poke it
		if (phase == 1) return Dungeon.level.distance(pos, enemy.pos) <= 2;
		return super.canAttack(enemy);
	}

	@Override
	protected Char chooseEnemy() {
		Actor a = prey == -1 ? null : Actor.findById(prey);
		if (a instanceof Hero && ((Hero) a).isAlive() && ((Hero) a).buff(PreyMark.class) != null) return (Hero) a;
		return super.chooseEnemy();
	}

	@Override
	protected boolean act() {
		if (!isAlive()) return true;
		//a test duel (-Dspd.test.duel): nobody walks up to him, he comes out at once
		if (DevFlags.duel() > 0 && !introDone){
			state = HUNTING;
			notice();
		}
		boolean fighting = Dungeon.level.locked;
		if (fighting){
			if (phase == 2 && heat < 14){ heat++; arenaVersion++; }
			if (phase == 3 && frost < 9 && Random.Int(2) == 0){ frost++; arenaVersion++; }
			if (phase == 4){
				if (--meteorTimer <= 0){
					meteorTimer = Party.living().size() >= 3 ? 1 : 2;
					meteors();
				}
			}
			if (phase >= 2 && --preyTimer <= 0){
				preyTimer = 14;
				markPrey();
			}
			if (--summonTimer <= 0){
				summonTimer = phase == 1 ? 9 : 11;
				summon();
			}
		}
		if (stunned > 0){
			stunned--;
			if (sprite instanceof TharogSprite) ((TharogSprite) sprite).stunned(stunned > 0);
			spend(TICK);
			return true;
		}
		return super.act();
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (!isAlive()) return;
		//each plate eats 10% dmg, only 6% if the yeti quest kid told you where they crack
		float mult = 1f - (YetiQuest.helps() ? 0.06f : 0.1f) * plates;
		if (stunned > 0) mult *= 1.5f;
		dmg = Math.max(1, Math.round(dmg * mult));
		super.damage(dmg, src);
		if (!isAlive()) return;
		if (YetiQuest.helps() && plates > 0 && phase > 1){
			plateDamage += dmg;
			if (plateDamage >= HT / 8){
				plateDamage = 0;
				plates--;
				GLog.p(Messages.get(this, "plate", plates));
				TharogHud.banner(Messages.get(Tharog.class, "banner_plate", plates));
			}
		}
		if (phase == 1 && HP <= HT * 3 / 4) phase2();
		else if (phase == 2 && HP <= HT / 2) phase3();
		else if (phase == 3 && HP <= HT / 4) phase4();
	}

	private void phase2(){
		phase = 2;
		heat = 1;
		arenaVersion++;
		yell(Messages.get(this, "free"));
		TharogHud.banner(Messages.get(Tharog.class, "banner_2"));
		GLog.w(Messages.get(Tharog.class, "hint_2"));
		later(new Runnable() { public void run() {
			PixelScene.shake(8, 1.5f);
			GameScene.flash(0xA0D8F0FF);
		} });
		Sample.INSTANCE.play(Assets.Sounds.SHATTER, 1f, 0.6f);
		if (sprite instanceof TharogSprite) ((TharogSprite) sprite).shatter();
		cooldown = 0;
	}

	private void phase3(){
		phase = 3;
		//GLog.i("tharog -> p3, hp " + HP + "/" + HT);
		YetiQuest.glaciation(this);
		frost = 1;
		arenaVersion++;
		yell(Messages.get(this, "cold"));
		TharogHud.banner(Messages.get(Tharog.class, "banner_3"));
		GLog.w(Messages.get(Tharog.class, "hint_3"));
		ArrayList<Integer> free = arenaCells(3);
		for (int i = 0; i < 3 && !free.isEmpty(); i++){
			int c = free.remove(Random.Int(free.size()));
			IcePillar p = new IcePillar();
			p.pos = c;
			GameScene.add(p);
		}
		//tribe backup if the flame got to the altar
		if (Tribe.Quest.completed && DevFlags.duel() == 0){
			int made = 0;
			for (Hero h : Party.living()){
				for (int n : PathFinder.NEIGHBOURS8){
					if (made >= 3) break;
					int c = h.pos + 2 * n;
					if (c > 0 && c < Dungeon.level.length() && Dungeon.level.passable[c] && Actor.findChar(c) == null){
						Tribe.Warrior w = new Tribe.Warrior();
						w.pos = c;
						GameScene.add(w);
						made++;
					}
				}
				if (made >= 3) break;
			}
			TharogHud.banner(Messages.get(Tharog.class, "banner_tribe"));
		}
	}

	private void phase4(){
		phase = 4;
		arenaVersion++;
		yell(Messages.get(this, "sky"));
		TharogHud.banner(Messages.get(Tharog.class, "banner_4"));
		GLog.w(Messages.get(Tharog.class, "hint_4"));
		later(new Runnable() { public void run() {
			PixelScene.shake(6, 2f);
			GameScene.flash(0xA0FF4020);
		} });
		meteorTimer = 1;
	}

	private ArrayList<Integer> arenaCells( int margin ){
		ArrayList<Integer> cells = new ArrayList<>();
		for (int c : SKCombat.area(pos, 9)){
			if (Dungeon.level.passable[c] && Actor.findChar(c) == null && edgeDistance(c) >= margin
					&& Dungeon.level.distance(c, pos) >= 2) cells.add(c);
		}
		return cells;
	}

	@Override
	protected boolean special( Char enemy ) {
		if (stunned > 0) return false;
		if (sprite instanceof TharogSprite) ((TharogSprite) sprite).roar();
		int choice;
		if (phase == 1){
			choice = pickSpell(2) == 0 ? BREATH : ROAR;
		} else {
			int[] options = {BREATH, ROAR, SWEEP, STOMP, CHARGE};
			choice = options[pickSpell(options.length)];
		}
		switch (choice){
			case BREATH: default: {
				Ballistica aim = new Ballistica(pos, enemy.pos, Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID);
				ConeAOE cone = new ConeAOE(aim, 5, 70, Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID);
				ArrayList<Integer> cells = new ArrayList<>(cone.cells);
				cells.remove((Integer) pos);
				announce(BREATH, cells, enemy.pos);
				GLog.w(Messages.get(this, phase == 2 || phase == 4 ? "steam" : "breath"));
				return true;
			}
			case ROAR: {
				ArrayList<Integer> cells = new ArrayList<>();
				for (Char h : foes()){
					for (int i = 0; i < 3; i++){
						int c = h.pos + Random.IntRange(-2, 2) + Random.IntRange(-2, 2) * Dungeon.level.width();
						if (c >= 0 && c < Dungeon.level.length() && Dungeon.level.passable[c] && !cells.contains(c)) cells.add(c);
					}
				}
				if (cells.isEmpty()) return false;
				announce(ROAR, cells, enemy.pos);
				yell(Messages.get(this, "roar"));
				Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 1f, 0.5f);
				return true;
			}
			case SWEEP: {
				ArrayList<Integer> cells = new ArrayList<>();
				for (int c : SKCombat.area(pos, 2)){
					if (c != pos && Dungeon.level.distance(c, enemy.pos) >= 1) cells.add(c);
				}
				announce(SWEEP, cells, enemy.pos);
				GLog.w(Messages.get(this, "sweep"));
				return true;
			}
			case STOMP: {
				ArrayList<Integer> line = line(enemy.pos, 7);
				if (line.isEmpty()) return false;
				announce(STOMP, line, enemy.pos);
				GLog.w(Messages.get(this, "stomp"));
				return true;
			}
			case CHARGE: {
				int d = Dungeon.level.distance(pos, enemy.pos);
				if (d < 2) return false;
				ArrayList<Integer> line = new ArrayList<>();
				Ballistica b = new Ballistica(pos, enemy.pos, Ballistica.STOP_SOLID | Ballistica.STOP_CHARS);
				for (int c : b.subPath(1, Math.min(b.dist, 8))){
					line.add(c);
					Char ch = Actor.findChar(c);
					if (ch instanceof IcePillar) break;
				}
				announce(CHARGE, line, enemy.pos);
				yell(Messages.get(this, "charge"));
				return true;
			}
		}
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		switch (kind){
			case BREATH: {
				boolean hot = phase == 2 || phase == 4;
				for (Char ch : SKCombat.foesIn(this, cells)){
					ch.damage(Math.max(1, Math.round(power * Random.NormalIntRange(16, 26)) - ch.drRoll() / 2), this);
					if (!ch.isAlive()) continue;
					if (hot) Buff.affect(ch, Burning.class).reignite(ch, 3f);
					else Buff.prolong(ch, Chill.class, 4f);
				}
				//breath kills the braziers. mean
				if (!hot){
					for (int c : cells){
						Char ch = Actor.findChar(c);
						if (ch instanceof ForgottenFeatures.Brazier) ((ForgottenFeatures.Brazier) ch).putOut();
					}
				}
				for (int c : cells){
					if (Dungeon.level.heroFOV[c] && Random.Int(3) == 0){
						if (hot) CellEmitter.get(c).burst(Speck.factory(Speck.STEAM), 2);
						else CellEmitter.get(c).burst(SnowParticle.FACTORY, 3);
					}
				}
				break;
			}
			case ROAR:
				for (int c : cells){
					if (phase == 1 || phase == 3) ForgottenVisuals.icicle(c);
					else ForgottenVisuals.meteor(c);
				}
				hitFoes(cells, 14, 22);
				later(new Runnable() { public void run() { PixelScene.shake(3, 0.6f); } });
				break;
			case SWEEP:
				if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.HIT_CRUSH);
				for (Char ch : SKCombat.foesIn(this, cells)){
					ch.damage(Math.max(1, Math.round(power * Random.NormalIntRange(20, 30)) - ch.drRoll() / 2), this);
					if (ch.isAlive()) knock(ch, 2);
				}
				break;
			case STOMP: {
				later(new Runnable() { public void run() { PixelScene.shake(4, 0.5f); } });
				hitFoes(cells, 16, 26);
				ForgottenFloor f = ForgottenFloor.getOrMake(Dungeon.level);
				for (int c : cells){
					if (Random.Int(2) == 0 && Actor.findChar(c) == null) f.put(Dungeon.level, c, ForgottenFloor.TAR);
				}
				break;
			}
			case CHARGE: {
				int start = pos;
				Char victim = null;
				IcePillar pillar = null;
				for (int c : cells){
					Char ch = Actor.findChar(c);
					if (ch instanceof IcePillar){
						pillar = (IcePillar) ch;
						break;
					}
					if (ch != null){
						if (SKCombat.foes(this, ch)) victim = ch;
						break;
					}
					if (!Dungeon.level.passable[c]) break;
					move(c, false);
				}
				if (pos != start && sprite != null){
					sprite.move(start, pos);
					if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.TRAMPLE);
				}
				if (pillar != null){
					pillar.shatter();
					stunned = 3;
					if (plates > 0){
						plates--;
						GLog.p(Messages.get(this, "plate", plates));
						TharogHud.banner(Messages.get(Tharog.class, "banner_plate", plates));
					}
					GLog.p(Messages.get(this, "stunned"));
					if (sprite instanceof TharogSprite) ((TharogSprite) sprite).stunned(true);
					later(new Runnable() { public void run() { PixelScene.shake(6, 0.8f); } });
				} else if (victim != null){
					attack(victim, 1.6f, 0, 2f);
					if (victim.isAlive()) knock(victim, 2);
				}
				break;
			}
			case METEORS: {
				for (int c : cells) ForgottenVisuals.meteor(c);
				Sample.INSTANCE.play(Assets.Sounds.BLAST, 1f, 0.7f);
				for (int c : cells){
					Char ch = Actor.findChar(c);
					if (ch == this){
						//meteors don't care whose side you're on
						super.damage(Math.max(1, HT * 6 / 100), Fire.class);
						if (sprite != null) sprite.showStatus(CharSprite.NEGATIVE, Messages.get(this, "meteor_hit"));
					} else if (ch != null && ch.isAlive() && !(ch instanceof IcePillar)){
						ch.damage(Math.max(1, Math.round(power * Random.NormalIntRange(18, 28)) - ch.drRoll() / 2), this);
						if (ch.isAlive()) Buff.affect(ch, Burning.class).reignite(ch, 3f);
					}
				}
				if (isAlive() && phase == 3 && HP <= HT / 4) phase4();
				break;
			}
		}
	}

	private void meteors(){
		ArrayList<Integer> cells = new ArrayList<>();
		ArrayList<Char> living = foes();
		int n = 1 + living.size() / 2;
		for (int i = 0; i < n && !living.isEmpty(); i++){
			Char h = Random.element(living);
			int c = h.pos + Random.IntRange(-1, 1) + Random.IntRange(-1, 1) * Dungeon.level.width();
			for (int k : new int[]{0, 1, Dungeon.level.width(), Dungeon.level.width() + 1}){
				int cc = c + k;
				if (cc >= 0 && cc < Dungeon.level.length() && Dungeon.level.passable[cc] && !cells.contains(cc)) cells.add(cc);
			}
		}
		if (Random.Int(3) == 0) cells.add(pos);
		if (cells.isEmpty()) return;
		//NOT announce(): meteors fall while it keeps doing other stuff
		float delay = SKCombat.announceDelay(TICK);
		SKCombat.mark(cells, delay);
		pendingMeteors.add(cells);
		pendingMeteorTime.add(Actor.now() + delay);
	}

	private final ArrayList<ArrayList<Integer>> pendingMeteors = new ArrayList<>();
	private final ArrayList<Float> pendingMeteorTime = new ArrayList<>();

	@Override
	public void spend( float time ) {
		super.spend(time);
		//on time, no matter what tharog's up to
		for (int i = pendingMeteors.size() - 1; i >= 0; i--){
			if (pendingMeteorTime.get(i) <= Actor.now() + 0.01f){
				ArrayList<Integer> cells = pendingMeteors.remove(i);
				pendingMeteorTime.remove(i);
				release(METEORS, cells, -1);
			}
		}
	}

	private void knock( Char ch, int n ){
		int w = Dungeon.level.width();
		int dx = Integer.signum(ch.pos % w - pos % w), dy = Integer.signum(ch.pos / w - pos / w);
		if (dx == 0 && dy == 0) return;
		for (int i = 0; i < n; i++){
			int next = ch.pos + dx + dy * w;
			if (!Dungeon.level.passable[next] || Actor.findChar(next) != null) return;
			int from = ch.pos;
			ch.pos = next;
			Actor.add(new Pushing(ch, from, next));
			Dungeon.level.occupyCell(ch);
		}
	}

	//who his blows are meant for: the heroes, or in a test duel (-Dspd.test.duel) the one he's fighting
	private ArrayList<Char> foes(){
		ArrayList<Char> out = new ArrayList<>();
		if (DevFlags.duel() > 0){
			if (enemy != null && enemy.isAlive()) out.add(enemy);
			return out;
		}
		out.addAll(Party.living());
		return out;
	}

	private void markPrey(){
		if (DevFlags.duel() > 0) return;
		ArrayList<Hero> living = Party.living();
		if (living.isEmpty()) return;
		Hero h = Random.element(living);
		prey = h.id();
		Buff.prolong(h, PreyMark.class, living.size() <= 1 ? 5f : 9f);
		GLog.n(Party.active() ? Messages.get(this, "prey", Party.name(h)) : Messages.get(this, "prey_solo"));
		yell(Messages.get(this, "prey_yell"));
	}

	private void summon(){
		if (DevFlags.duel() > 0) return;
		int alive = 0;
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof ForgottenMobs.Beast && m.alignment == Alignment.ENEMY) alive++;
		}
		int cap = 1 + Party.living().size();
		if (alive >= cap) return;
		ArrayList<Integer> cells = arenaCells(1);
		if (cells.isEmpty()) return;
		Class<? extends Mob> cls;
		if (phase == 1 || phase == 3) cls = Random.Int(3) == 0 ? ForgottenMobs.Sabertooth.class : ForgottenMobs.FrostWolf.class;
		else cls = Random.Int(3) == 0 ? ForgottenMobs.Pterodactyl.class : ForgottenMobs.Raptor.class;
		int n = cls == ForgottenMobs.FrostWolf.class || cls == ForgottenMobs.Raptor.class ? 2 : 1;
		for (int i = 0; i < n && !cells.isEmpty() && alive < cap; i++){
			int c = cells.remove(Random.Int(cells.size()));
			Mob m = Reflection.newInstance(cls);
			((ForgottenMobs.Beast) m).summoned();
			m.pos = c;
			m.state = m.HUNTING;
			GameScene.add(m);
			alive++;
		}
		yell(Messages.get(this, "summon"));
	}

	@Override
	protected ArrayList<Item> loot() {
		ArrayList<Item> items = new ArrayList<>();
		items.add(new TharogHeart());
		return items;
	}

	@Override
	public void die( Object cause ) {
		pendingMeteors.clear();
		pendingMeteorTime.clear();
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (m instanceof ForgottenMobs.Beast && m.alignment == Alignment.ENEMY && m != this){
				m.die(null);
			}
		}
		super.die(cause);
		phase = 5;
		arenaVersion++;
		TharogHud.banner(Messages.get(Tharog.class, "banner_dead"));
		later(new Runnable() { public void run() {
			PixelScene.shake(6, 2.5f);
			GameScene.flash(0x80FFD890);
		} });
	}

	@Override
	public String description() {
		return Messages.get(this, "desc", plates);
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("phase", phase);
		bundle.put("plates", plates);
		bundle.put("plate_damage", plateDamage);
		bundle.put("stunned", stunned);
		bundle.put("heat", heat);
		bundle.put("frost", frost);
		bundle.put("prey", prey);
		bundle.put("prey_timer", preyTimer);
		bundle.put("summon_timer", summonTimer);
		bundle.put("meteor_timer", meteorTimer);
		bundle.put("intro", introDone);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		phase = bundle.getInt("phase");
		plates = bundle.getInt("plates");
		plateDamage = bundle.getInt("plate_damage");
		stunned = bundle.getInt("stunned");
		heat = bundle.getInt("heat");
		frost = bundle.getInt("frost");
		prey = bundle.getInt("prey");
		preyTimer = bundle.getInt("prey_timer");
		summonTimer = bundle.getInt("summon_timer");
		meteorTimer = bundle.getInt("meteor_timer");
		introDone = bundle.getBoolean("intro");
		BossHealthBarHider.hide();
	}

	static class BossHealthBarHider {
		static void hide(){
			BossHealthBar.hidden = true;
		}
	}

	public static class PreyMark extends FlavourBuff {
		{
			type = buffType.NEGATIVE;
			announced = true;
		}

		@Override
		public int icon() {
			return BuffIndicator.MARK;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(1f, 0.6f, 0.1f);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", dispTurns());
		}
	}

	// *** the pillars of ice ***

	public static class IcePillar extends SKProps.SKProp {

		{
			spriteClass = PillarSprite.class;
			HP = HT = 1000;
		}

		private boolean broken = false;

		@Override
		public boolean isInvulnerable( Class effect ) {
			return true;
		}

		@Override
		public void damage( int dmg, Object src ) {
		}

		public void shatter(){
			if (broken) return;
			broken = true;
			int at = pos;
			if (Dungeon.level.heroFOV[at]){
				Sample.INSTANCE.play(Assets.Sounds.SHATTER);
				CellEmitter.get(at).burst(Speck.factory(Speck.STEAM), 10);
			}
			destroy();
			if (sprite != null) sprite.killAndErase();
		}

		@Override
		public String description() {
			return Messages.get(this, "desc");
		}
	}

	public static class PillarSprite extends ForgottenFeatures.StripSprite {
		public PillarSprite(){
			super();
			strip(ForgottenAssets.ICE_PILLAR, 16, 32, 1);
			still(0);
		}
	}

	//drop. originally you ate it. then i thought about it for 2 seconds. now you hold it and get warm forever
	public static class TharogHeart extends Item {

		public static final String AC_ABSORB = "ABSORB";

		{
			image = ItemSpriteSheet.THAROG_HEART;
			defaultAction = AC_ABSORB;
			unique = true;
		}

		@Override
		public ArrayList<String> actions( Hero hero ) {
			ArrayList<String> actions = super.actions(hero);
			actions.add(AC_ABSORB);
			return actions;
		}

		@Override
		public void execute( Hero hero, String action ) {
			super.execute(hero, action);
			if (!action.equals(AC_ABSORB)) return;
			detach(hero.belongings.backpack);
			hero.HTBoost += 20;
			hero.updateHT(true);
			hero.HP = hero.HT;
			if (hero.sprite != null) hero.sprite.emitter().burst(Speck.factory(Speck.HEALING), 10);
			Sample.INSTANCE.play(Assets.Sounds.DRINK);
			GLog.p(Messages.get(this, "absorbed"));
			hero.spendAndNext(1f);
		}

		@Override
		public boolean isUpgradable() {
			return false;
		}

		@Override
		public boolean isIdentified() {
			return true;
		}
	}

	//sprite: big, plates + the ice block drawn on top

	public static class TharogSprite extends SKMobSprite {

		//plate centers in frame 0, eyeballed from the png
		public static int[][] PLATES = {{20, 19}, {30, 15}, {39, 12}};

		private Image ice;
		private final Image[] plateImgs = new Image[3];
		private float shatterT = -1;

		public TharogSprite(){
			super();
			setup(ForgottenAssets.THAROG, 64, 56);
		}

		public void roar(){
			row(1);
			play(zap);
		}

		public void stunned( boolean on ){
			row(on ? 2 : 0);
		}

		public void shatter(){
			shatterT = 0;
		}

		@Override
		public void onComplete( Animation anim ) {
			super.onComplete(anim);
			if (anim == zap && ch instanceof Tharog && ((Tharog) ch).stunned == 0) row(0);
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (parent != null){
				ice = new Image(ForgottenAssets.THAROG_ICE);
				ice.frame(0, 0, 96, 80);
				parent.add(ice);
				for (int i = 0; i < 3; i++){
					plateImgs[i] = new Image(ForgottenAssets.PLATES);
					plateImgs[i].frame(16 * i, 0, 16, 16);
					parent.add(plateImgs[i]);
				}
			}
		}

		@Override
		public void update() {
			super.update();
			if (!(ch instanceof Tharog)) return;
			Tharog t = (Tharog) ch;
			if (ice != null){
				boolean trapped = t.phase == 1 || (shatterT >= 0 && shatterT < 0.8f);
				if (shatterT >= 0) shatterT += Game.elapsed;
				ice.visible = visible && trapped && t.isAlive();
				ice.x = x + width / 2f - 48;
				ice.y = y + height - 80 + 12;
				int f = shatterT >= 0 ? 2 : (t.HP < t.HT * 0.87f ? 1 : 0);
				ice.frame(96 * f, 0, 96, 80);
				ice.alpha(shatterT >= 0 ? Math.max(0, 1 - shatterT / 0.8f) : 0.95f);
			}
			for (int i = 0; i < 3; i++){
				Image p = plateImgs[i];
				if (p == null) continue;
				p.visible = visible && t.isAlive() && i < t.plates && t.phase > 1;
				int px = flipHorizontal ? 64 - PLATES[i][0] : PLATES[i][0];
				p.x = x + px - 8;
				p.y = y + PLATES[i][1] - 8;
				p.flipHorizontal = flipHorizontal;
			}
		}

		@Override
		public void kill() {
			super.kill();
			if (ice != null) ice.killAndErase();
			for (Image p : plateImgs) if (p != null) p.killAndErase();
		}

		@Override
		public int blood() {
			return 0xFF6A7A3A;
		}
	}
}
