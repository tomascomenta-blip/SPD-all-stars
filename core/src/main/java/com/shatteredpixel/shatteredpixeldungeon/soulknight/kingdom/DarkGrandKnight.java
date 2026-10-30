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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.kingdom;

import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopRewards;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.quests.TourneyQuest;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.LloydsBeacon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKBoss;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKBulletField;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.props.SKProps;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.tweeners.AlphaTweener;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/*
 * dark grand knight, lord of the Knight Kingdom (floor 10)
 * used to be the champion sworn to guard the throne. the king fell and the shadow moved into his armor
 *
 * sits still on the throne like one more statue until a hero walks in: doors slam, braziers light up
 * one by one, he stands, bangs the sword on the shield, fight on.
 * the shield eats about a third of every hit from the front (see shieldFactor) and breaks as the fight goes
 *
 *  I   GUARDIAN (100-75%) on foot. blade fans/rings/trails, bursting orbs, long thrust down a marked line
 *      and the leap of judgement (lands on a marked hero)
 *  II  CHARGE (75-50%) war horse, faster, charges along marked lines
 *  III BANNER (50-25%) horse goes down, he plants the kingdom banner one cell ahead and never goes more
 *      than 3 cells from it. -30% dmg taken while it stands
 *  IV  SHADOW (25-0%) shield breaks, torches go out, the shadow pours out of the armor, shadow knights
 *      show up, more leaps and thrusts
 *
 * the hall helps: columns stop bullets and stun him if he crashes into one, powder barrels blow up under
 * his charges, shield walls stop bullets, and a chandelier drops on him if you cut its rope.
 * at the end the shadow leaves him, he kneels (free at last, poor guy) and leaves the sword + shield
 */
public class DarkGrandKnight extends SKBoss {

	{
		spriteClass = KnightSprite.class;
		HP = HT = Dungeon.isChallenged(Challenges.STRONGER_BOSSES) ? 300 : 240;
		defenseSkill = 14;
		EXP = 20;
		state = PASSIVE;
	}

	public int act = 1;
	public int intro = 0;           //0 on the throne, 1-3 waking up, 4 fighting
	public int stunned = 0;
	private int bannerId = -1;
	private int leapCell = -1;
	private int faceX = 0, faceY = 1;   //where the shield points = where he last moved or hit

	private int shadowWaves = 0;
	private int[] shadows = new int[0];
	private int shadowHits = 0;

	private static final int FAN = 0, RING = 1, TRAIL = 2, ORBS = 3, THRUST = 4, LEAP = 5, CHARGE = 6;

	public boolean awake(){
		return intro >= 1;
	}

	public boolean fighting(){
		return intro >= 4 && isAlive();
	}

	public boolean bannerUp(){
		return banner() != null;
	}

	public ThroneProps.Banner banner(){
		Actor a = bannerId == -1 ? null : Actor.findById(bannerId);
		return a instanceof ThroneProps.Banner && ((ThroneProps.Banner) a).isAlive() ? (ThroneProps.Banner) a : null;
	}

	// *** waking up ***

	private KingdomBossLevel hall(){
		return Dungeon.level instanceof KingdomBossLevel ? (KingdomBossLevel) Dungeon.level : null;
	}

	private boolean heroCame(){
		KingdomBossLevel L = hall();
		for (Hero h : Party.living()){
			if (L == null){
				if (Dungeon.level.distance(h.pos, pos) <= 6) return true;
			} else if (L.inHall(h.pos) && Dungeon.level.distance(h.pos, pos) <= 9){
				return true;
			}
		}
		return Dungeon.level.locked;
	}

	private boolean knownChampion = false; //heroes won the floor 8 tourney

	private boolean introAct(){
		switch (intro){
			case 0: default:
				if (heroCame()){
					intro = 1;
					BossHealthBar.assignBoss(this);
					Dungeon.level.seal();
					Dungeon.level.gatherParty();
					CoopLog.line("grand knight wakes" + " t=" + (int) Actor.now() + " hp=" + HP + "/" + HT + " heroes=" + Party.living().size() + " hero_hp=" + Dungeon.hero.HP);
					Sample.INSTANCE.play(Assets.Sounds.TOMB, 1f, 0.8f);
					later(new Runnable() { public void run() { PixelScene.shake(2, 0.4f); } });
					ThroneVisuals.lightUp();
				}
				break;
			case 1:
				intro = 2;
				break;
			case 2:
				intro = 3;
				Sample.INSTANCE.play(Assets.Sounds.TRAMPLE, 1f, 0.6f);
				break;
			case 3:
				intro = 4;
				state = HUNTING;
				cooldown = 2;
				Sample.INSTANCE.play(Assets.Sounds.HIT_PARRY, 1f, 0.7f);
				Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 1f, 0.8f);
				later(new Runnable() { public void run() {
					PixelScene.shake(3, 0.5f);
					GameScene.flash(0x60FFE0A0);
				} });
				if (sprite != null) CellEmitter.center(pos).burst(Speck.factory(Speck.STAR), 8);
				KnightHud.title(Messages.get(this, "title_name"), Messages.get(this, "title_sub"));
				yell(Messages.get(this, "notice"));
				leapDown();
				testStart();
				TourneyQuest.knightFight(this);
				break;
		}
		spend(TICK);
		return true;
	}

	//end of the entrance: he leaps off the dais into the middle of the hall
	private void leapDown(){
		KingdomBossLevel L = hall();
		if (L == null) return;
		int w = Dungeon.level.width();
		int land = -1;
		for (int dy : new int[]{4, 5, 3, 6}){
			for (int dx : new int[]{0, -1, 1}){
				int c = pos + dx + dy * w;
				if (L.inHall(c) && Dungeon.level.passable[c] && Actor.findChar(c) == null){
					land = c;
					break;
				}
			}
			if (land != -1) break;
		}
		if (land == -1) return;
		int from = pos;
		move(land, false);
		if (sprite != null){
			sprite.jump(from, land, 30, 0.4f, null);
			if (sprite instanceof KnightSprite) ((KnightSprite) sprite).land();
		}
		//no dmg, just shoves whoever's next to where he lands
		for (int n : PathFinder.NEIGHBOURS8){
			Char ch = Actor.findChar(land + n);
			if (ch != null && SKCombat.foes(this, ch)) knock(ch, 1);
		}
		if (Dungeon.level.heroFOV[land]){
			Sample.INSTANCE.play(Assets.Sounds.BLAST, 1f, 0.5f);
			for (int n : PathFinder.NEIGHBOURS9) CellEmitter.get(land + n).burst(Speck.factory(Speck.DUST), 3);
		}
	}

	//-Dspd.test.knightweak=1 low hp, -Dspd.test.knightact=2|3|4 starts in that act
	private void testStart(){
		if (DevFlags.knightWeak()){
			HT = 80;
			HP = HT;
		}
		int start = DevFlags.knightAct();
		if (start >= 2){ HP = HT * 3 / 4; toAct2(); }
		if (start >= 3){ HP = HT / 2; enraged = true; BossHealthBar.bleed(true); toAct3(); }
		if (start >= 4){ HP = HT / 4; toAct4(); }
	}

	@Override
	public void notice() {
		//he wakes up on his own (introAct)
	}

	@Override
	public boolean isInvulnerable( Class effect ) {
		return intro < 4 || super.isInvulnerable(effect);
	}

	@Override
	protected boolean act() {
		if (!isAlive()) return true;
		if (intro < 4) return introAct();
		if (shadows.length > 0 && !shadowsOut()) dispelShadows();
		if (stunned > 0){
			stunned--;
			spend(TICK);
			return true;
		}
		return super.act();
	}

	@Override
	protected boolean getCloser( int target ) {
		if (stunned > 0) return false;
		ThroneProps.Banner b = banner();
		if (b != null){
			//never more than 3 cells from his banner
			if (Dungeon.level.distance(pos, b.pos) > 3) return super.getCloser(b.pos);
			int best = -1, bestD = Dungeon.level.distance(pos, target);
			for (int n : PathFinder.NEIGHBOURS8){
				int c = pos + n;
				if (Dungeon.level.passable[c] && Actor.findChar(c) == null
						&& Dungeon.level.distance(c, b.pos) <= 3 && Dungeon.level.distance(c, target) < bestD){
					best = c;
					bestD = Dungeon.level.distance(c, target);
				}
			}
			if (best == -1) return false;
			move(best);
			return true;
		}
		return super.getCloser(target);
	}

	@Override
	protected boolean getFurther( int target ) {
		if (stunned > 0 || bannerUp()) return false;
		return super.getFurther(target);
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		if (stunned > 0) return false;
		return super.canAttack(enemy);
	}

	//on the horse he rides off to get a run-up
	@Override
	protected boolean keepsDistance() {
		return act == 2 && stunned == 0;
	}

	@Override
	public int damageRoll() {
		switch (act){
			case 2: return Random.NormalIntRange(7, 15);
			case 4: return Random.NormalIntRange(6, 15);
			default: return Random.NormalIntRange(4, 12);
		}
	}

	@Override public int attackSkill( Char target ) { return 18; }
	@Override public int drRoll() { return super.drRoll() + Random.NormalIntRange(0, 2); }

	@Override
	protected int specialCooldown() {
		switch (act){
			case 2: return Random.IntRange(2, 3);
			case 4: return 2;
			default: return Random.IntRange(3, 4);
		}
	}

	@Override protected int scoreIndex() { return 1; }
	@Override protected int score() { return 2500; }

	// shield + banner stuff

	private static Char attacker( Object src ){
		if (src instanceof Char) return (Char) src;
		if (src instanceof SKBulletField.Shot) return ((SKBulletField.Shot) src).owner;
		return null;
	}

	//turn the shield toward a cell
	private void face( int cell ){
		int w = Dungeon.level.width();
		int dx = cell % w - pos % w, dy = cell / w - pos / w;
		if (dx == 0 && dy == 0) return;
		faceX = dx;
		faceY = dy;
	}

	@Override
	public void move( int step, boolean travelling ) {
		face(step);
		super.move(step, travelling);
	}

	@Override
	public boolean attack( Char enemy, float dmgMulti, float dmgBonus, float accMulti ) {
		if (enemy != null) face(enemy.pos);
		return super.attack(enemy, dmgMulti, dmgBonus, accMulti);
	}

	//how much of a hit gets through the shield. from the front: 65% while it's whole, 80% once cracked (act 3),
	//everything from act 4 on or while he's stunned. "front" = where he last moved or hit, so right after a
	//thrust/charge/leap his back is open. that's the whole trick of the fight
	private float shieldFactor( Object src ){
		if (act >= 4 || stunned > 0) return 1f;
		Char a = attacker(src);
		//only hits from heroes + allies (not the barrels blowing up in his face)
		if (a == null || a == this || a.alignment != Alignment.ALLY) return 1f;
		int w = Dungeon.level.width();
		int fx = faceX, fy = faceY;
		int ax = a.pos % w - pos % w, ay = a.pos / w - pos / w;
		if ((fx == 0 && fy == 0) || (ax == 0 && ay == 0)) return 1f;
		double dot = (fx * ax + fy * ay) / (Math.sqrt(fx * fx + fy * fy) * Math.sqrt(ax * ax + ay * ay));
		//GLog.i("dot " + dot + " act " + act);
		if (dot < 0.5) return 1f;
		return act == 3 ? 0.8f : 0.65f;
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (!isAlive() || intro < 4) return;
		if (dmg > 0){
			float m = shieldFactor(src);
			if (m < 1f && Dungeon.level.heroFOV[pos]){
				Sample.INSTANCE.play(Assets.Sounds.HIT_PARRY, 0.8f, 0.9f);
				if (sprite != null && Random.Int(3) == 0) sprite.showStatus(CharSprite.NEUTRAL, Messages.get(this, "blocked"));
			}
			if (stunned > 0) m *= 1.25f;
			if (bannerUp()) m *= 0.7f;
			dmg = Math.max(1, Math.round(dmg * m));
		}
		super.damage(dmg, src);
		if (!isAlive()) return;
		if (shadowsOut() && dmg > 0 && ++shadowHits >= 3) dispelShadows();
		if (act == 1 && HP * 4 <= HT * 3) champion();
		if ((act == 2 || (act == 1 && knownChampion)) && HP * 2 <= HT) toAct3();
		if (act == 3 && HP * 4 <= HT) toAct4();
		if (act == 4 && shadowWaves == 1 && HP * 10 <= HT){
			shadowWaves = 2;
			summonShadows();
		}
	}

	//at 3/4: the horse, unless he knows them from the floor 8 tourney
	private void champion(){
		if (!TourneyQuest.recognized()){
			toAct2();
			return;
		}
		if (!knownChampion){
			knownChampion = true;
			yell(Messages.get(TourneyQuest.class, "knight_skip"));
		}
	}

	//horse comes when he whistles. good boy
	private void toAct2(){
		act = 2;
		baseSpeed = 1.5f;
		pendingKind = -1;
		cooldown = 1;
		yell(Messages.get(this, "mount"));
		GLog.w(Messages.get(this, "hint_2"));
		ThroneVisuals.horseComes(pos);
		later(new Runnable() { public void run() { PixelScene.shake(3, 0.8f); } });
		Sample.INSTANCE.play(Assets.Sounds.TRAMPLE, 1f, 0.7f);
		refill();
		CoopLog.line("grand knight act 2" + " t=" + (int) Actor.now() + " hp=" + HP + "/" + HT + " heroes=" + Party.living().size() + " hero_hp=" + Dungeon.hero.HP);
	}

	//horse goes down, he plants the banner
	private void toAct3(){
		boolean horse = act == 2;
		act = 3;
		baseSpeed = 1f;
		pendingKind = -1;
		stun(1, false);
		if (horse) ThroneVisuals.horseFalls(pos);
		later(new Runnable() { public void run() { PixelScene.shake(4, 0.6f); } });
		plantBanner();
		GLog.w(Messages.get(this, "hint_3"));
		refill();
		CoopLog.line("grand knight act 3" + " t=" + (int) Actor.now() + " hp=" + HP + "/" + HT + " heroes=" + Party.living().size() + " hero_hp=" + Dungeon.hero.HP);
	}

	private void plantBanner(){
		int at = -1;
		Char target = enemy != null ? enemy : null;
		if (target != null){
			int w = Dungeon.level.width();
			int dx = Integer.signum(target.pos % w - pos % w), dy = Integer.signum(target.pos / w - pos / w);
			int c = pos + dx + dy * w;
			if (c != pos && Dungeon.level.passable[c] && Actor.findChar(c) == null) at = c;
		}
		if (at == -1){
			for (int n : PathFinder.NEIGHBOURS8){
				int c = pos + n;
				if (Dungeon.level.passable[c] && Actor.findChar(c) == null){
					at = c;
					break;
				}
			}
		}
		if (at == -1) return;
		ThroneProps.Banner b = new ThroneProps.Banner();
		b.pos = at;
		GameScene.add(b);
		bannerId = b.id();
		if (Dungeon.level.heroFOV[at]){
			CellEmitter.get(at).burst(Speck.factory(Speck.ROCK), 6);
			Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG, 1f, 0.6f);
		}
		if (sprite != null) sprite.aura(0xFFD860, 5);
	}

	public void bannerFell(){
		bannerId = -1;
		if (sprite != null) sprite.clearAura();
		if (isAlive()){
			yell(Messages.get(this, "banner_fell"));
			GLog.p(Messages.get(this, "banner_down"));
		}
		later(new Runnable() { public void run() { PixelScene.shake(2, 0.4f); } });
	}

	//shield breaks, shadow pours out
	private void toAct4(){
		act = 4;
		pendingKind = -1;
		cooldown = 0;
		yell(Messages.get(this, "shadow"));
		GLog.w(Messages.get(this, "hint_4"));
		if (sprite != null){
			CellEmitter.get(pos).burst(Speck.factory(Speck.ROCK), 8);
			CellEmitter.get(pos).burst(ShadowParticle.UP, 12);
		}
		Sample.INSTANCE.play(Assets.Sounds.SHATTER, 1f, 0.6f);
		later(new Runnable() { public void run() {
			PixelScene.shake(5, 1.2f);
			GameScene.flash(0x80602080);
		} });
		if (Dungeon.level instanceof KingdomBossLevel) ((KingdomBossLevel) Dungeon.level).finalMusic();
		shadowWaves = 1;
		summonShadows();
		refill();
		CoopLog.line("grand knight act 4" + " t=" + (int) Actor.now() + " hp=" + HP + "/" + HT + " heroes=" + Party.living().size() + " hero_hp=" + Dungeon.hero.HP);
	}

	//powder barrels come back every act. who refills them? nadie sabe
	private void refill(){
		KingdomBossLevel L = hall();
		if (L != null) L.refillBarrels();
	}

	public void stun( int turns, boolean announce ){
		stunned = Math.max(stunned, turns);
		pendingKind = -1;
		if (announce){
			GLog.p(Messages.get(this, "stunned"));
			if (sprite != null) sprite.showStatus(CharSprite.POSITIVE, Messages.get(this, "stunned_short"));
		}
	}

	//chandelier fell on him. lol
	public void crushed(){
		if (!fighting()) return;
		super.damage(Math.max(1, Math.round(Random.NormalIntRange(28, 38) * (bannerUp() ? 0.7f : 1f))), ThroneProps.class);
		if (!isAlive()) return;
		stun(3, true);
		if (act == 1 && HP * 4 <= HT * 3) champion();
		if ((act == 2 || (act == 1 && knownChampion)) && HP * 2 <= HT) toAct3();
		if (act == 3 && HP * 4 <= HT) toAct4();
	}

	@Override
	protected boolean special( Char enemy ) {
		if (!fieldOfView[enemy.pos] || stunned > 0) return false;
		int[] options;
		switch (act){
			case 2: options = new int[]{CHARGE, FAN, CHARGE, TRAIL, CHARGE, RING}; break;
			case 3: options = new int[]{FAN, THRUST, RING, LEAP, ORBS}; break;
			case 4: options = new int[]{FAN, THRUST, LEAP, RING, THRUST, LEAP, TRAIL}; break;
			default: options = new int[]{FAN, THRUST, RING, LEAP, TRAIL, ORBS}; break;
		}
		int spell = options[pickSpell(options.length)];
		//hero right next to him -> sword. no blade fans in their face
		if (Dungeon.level.adjacent(pos, enemy.pos) && spell != RING && spell != LEAP){
			if (Random.Int(3) > 0) return false;
			spell = RING;
		}
		CoopLog.line("grand knight spell " + spell + " t=" + (int) Actor.now()
				+ " hp=" + HP + " hero_hp=" + Dungeon.hero.HP + " dist=" + Dungeon.level.distance(pos, enemy.pos));
		float sp = speedBonus();
		boolean visible = Dungeon.level.heroFOV[pos];
		if (sprite != null) sprite.turnTo(pos, enemy.pos);
		face(enemy.pos);
		switch (spell){
			case FAN: default:
				arc(act >= 3 ? 9 : 7, 180, enemy.pos, 1.5f + sp, ItemSpriteSheet.SHOT_BLADE, 3, 7);
				if (visible) GLog.w(Messages.get(this, "fan"));
				break;
			case RING:
				for (int i = 0; i < 12; i++){
					SKBulletField.Bullet b = shoot(i * 30f, 1f + sp, ItemSpriteSheet.SHOT_BULLET, 3, 6);
					b.bounces = 1;
				}
				if (visible) GLog.w(Messages.get(this, "ring"));
				break;
			case TRAIL: {
				float mid = SKBulletField.angleTo(pos, enemy.pos);
				for (int i = 0; i < 5; i++){
					SKBulletField.Bullet b = shoot(mid - 60 + i * 30, 1f + sp, ItemSpriteSheet.SHOT_BLADE, 3, 7);
					b.behaviour = SKBulletField.TRAIL;
					b.period = 3;
					b.childImage = ItemSpriteSheet.SHOT_BULLET;
					b.life = 8;
				}
				if (visible) GLog.w(Messages.get(this, "trail"));
				break;
			}
			case ORBS:
				for (int i = 0; i < 6; i++){
					SKBulletField.Bullet b = shoot(i * 60f + 30f, 1f, ItemSpriteSheet.SHOT_BIG_ORB, 4, 8);
					b.burst = true;
					b.children = 6;
					b.childImage = ItemSpriteSheet.SHOT_BULLET;
					b.childSpeed = SKBulletField.UNIT * 3 / 2;
					b.life = 14;
				}
				if (visible) GLog.w(Messages.get(this, "orbs"));
				break;
			case THRUST:
			case CHARGE: {
				boolean horse = spell == CHARGE;
				int d = Dungeon.level.distance(pos, enemy.pos);
				ArrayList<Integer> cells = leashed(line(enemy.pos, horse ? 9 : 7));
				if (d < 2 || !cells.contains(enemy.pos) && cells.size() < 2){
					//too close or nowhere to go, blade fan instead
					arc(7, 120, enemy.pos, 1.5f + sp, ItemSpriteSheet.SHOT_BLADE, 3, 7);
					break;
				}
				announce(spell, cells, enemy.pos);
				if (sprite instanceof KnightSprite) ((KnightSprite) sprite).prepare();
				if (visible) GLog.w(Messages.get(this, horse ? "charge" : "thrust"));
				return true;
			}
			case LEAP: {
				Hero target = leapTarget();
				if (target == null) return false;
				leapCell = target.pos;
				ArrayList<Integer> cells = new ArrayList<>();
				for (int n : PathFinder.NEIGHBOURS9){
					int c = target.pos + n;
					if (!Dungeon.level.solid[c]) cells.add(c);
				}
				announce(LEAP, cells, target.pos);
				if (sprite instanceof KnightSprite) ((KnightSprite) sprite).crouch();
				if (visible) GLog.w(Party.active() ? Messages.get(this, "leap", Party.name(target)) : Messages.get(this, "leap_solo"));
				return true;
			}
		}
		if (sprite instanceof SKMobSprite) ((SKMobSprite) sprite).cast();
		else if (sprite instanceof KnightSprite) ((KnightSprite) sprite).swing();
		if (visible) Sample.INSTANCE.play(Assets.Sounds.HIT_SLASH, 1f, 0.7f);
		spend(TICK);
		return true;
	}

	//while the banner's up his lines stop 3 cells from it
	private ArrayList<Integer> leashed( ArrayList<Integer> cells ){
		ThroneProps.Banner b = banner();
		if (b == null) return cells;
		ArrayList<Integer> out = new ArrayList<>();
		for (int c : cells){
			if (Dungeon.level.distance(c, b.pos) > 3) break;
			out.add(c);
		}
		return out;
	}

	private Hero leapTarget(){
		ThroneProps.Banner b = banner();
		ArrayList<Hero> options = new ArrayList<>();
		for (Hero h : Party.living()){
			int d = Dungeon.level.distance(pos, h.pos);
			if (d < 2 || d > 8 || !fieldOfView[h.pos]) continue;
			if (b != null && Dungeon.level.distance(h.pos, b.pos) > 4) continue;
			options.add(h);
		}
		return options.isEmpty() ? null : Random.element(options);
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		switch (kind){
			case THRUST: case CHARGE:
				rush(cells, kind == CHARGE);
				break;
			case LEAP:
				leap(cells);
				break;
		}
	}

	//thrust on foot or charge on the horse, along the marked line
	private void rush( ArrayList<Integer> cells, boolean horse ){
		int start = pos;
		Char victim = null;
		Char obstacle = null;
		for (int c : cells){
			Char ch = Actor.findChar(c);
			if (ch != null){
				if (SKCombat.foes(this, ch)) victim = ch;
				else obstacle = ch;
				break;
			}
			if (Dungeon.level.solid[c] || !Dungeon.level.passable[c]) break;
			move(c, false);
		}
		if (pos != start && sprite != null){
			sprite.move(start, pos);
			if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.TRAMPLE);
		}
		if (sprite instanceof KnightSprite) ((KnightSprite) sprite).lunge();
		if (obstacle instanceof ThroneProps.Column){
			((ThroneProps.Column) obstacle).crash();
			stun(2, true);
			later(new Runnable() { public void run() { PixelScene.shake(5, 0.6f); } });
		} else if (obstacle instanceof SKProps.Barrel){
			//powder goes boom in his face
			obstacle.die(this);
			if (isAlive()) stun(2, true);
			later(new Runnable() { public void run() { PixelScene.shake(6, 0.7f); } });
		} else if (obstacle instanceof ThroneProps.ShieldWall){
			obstacle.die(this);
		} else if (victim != null){
			attack(victim, horse ? 1.7f : 1.4f, 0, 2f);
			if (victim.isAlive()) knock(victim, horse ? 2 : 1);
		}
	}

	//leap of judgement: lands on the marked cells + ring of blades from where he lands
	private void leap( ArrayList<Integer> cells ){
		int target = leapCell;
		leapCell = -1;
		int land = -1;
		if (target != -1 && Actor.findChar(target) == null && Dungeon.level.passable[target]) land = target;
		if (land == -1){
			int best = 99;
			for (int c : cells){
				if (Actor.findChar(c) == null && Dungeon.level.passable[c] && Dungeon.level.distance(c, target) < best){
					best = Dungeon.level.distance(c, target);
					land = c;
				}
			}
		}
		if (land == -1) return;
		int from = pos;
		move(land, false);
		if (sprite != null){
			sprite.jump(from, land, 40, 0.35f, null);
			if (sprite instanceof KnightSprite) ((KnightSprite) sprite).land();
		}
		for (Char ch : SKCombat.foesIn(this, cells)){
			ch.damage(Math.max(1, Random.NormalIntRange(9, 16) - ch.drRoll() / 2), this);
			if (ch.isAlive()) knock(ch, 1);
		}
		for (int i = 0; i < 10; i++){
			shoot(i * 36f + 18f, 1.5f, ItemSpriteSheet.SHOT_BLADE, 3, 6);
		}
		if (Dungeon.level.heroFOV[land]){
			Sample.INSTANCE.play(Assets.Sounds.BLAST, 1f, 0.6f);
			for (int c : cells) CellEmitter.get(c).burst(Speck.factory(Speck.DUST), 3);
		}
		later(new Runnable() { public void run() { PixelScene.shake(5, 0.5f); } });
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

	private static void later( final Runnable r ){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				r.run();
			}
		});
	}

	// *** shadow knights, act 4 ***

	private boolean shadowsOut(){
		for (int id : shadows){
			Actor a = Actor.findById(id);
			if (a instanceof Char && ((Char) a).isAlive()) return true;
		}
		return false;
	}

	private void summonShadows(){
		dispelShadows();
		ArrayList<Integer> spots = new ArrayList<>();
		for (int c : SKCombat.area(pos, 5)){
			int d = Dungeon.level.distance(c, pos);
			if (d >= 2 && Dungeon.level.passable[c] && !Dungeon.level.pit[c] && Actor.findChar(c) == null) spots.add(c);
		}
		Random.shuffle(spots);
		ArrayList<Integer> chosen = new ArrayList<>();
		for (int c : spots){
			boolean far = true;
			for (int o : chosen) if (Dungeon.level.distance(c, o) < 2) far = false;
			if (far) chosen.add(c);
			if (chosen.size() == 3) break;
		}
		shadows = new int[chosen.size()];
		for (int i = 0; i < chosen.size(); i++){
			ShadowKnight k = new ShadowKnight();
			k.pos = chosen.get(i);
			k.state = k.HUNTING;
			GameScene.add(k);
			if (Dungeon.level.heroFOV[k.pos]) CellEmitter.get(k.pos).burst(ShadowParticle.UP, 8);
			shadows[i] = k.id();
		}
		shadowHits = 0;
		if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.CURSED, 1f, 0.7f);
		yell(Messages.get(this, "shadows"));
		CoopLog.line("grand knight shadows=" + shadows.length);
	}

	private void dispelShadows(){
		for (int id : shadows){
			Actor a = Actor.findById(id);
			if (a instanceof ShadowKnight && ((ShadowKnight) a).isAlive()) ((ShadowKnight) a).vanish();
		}
		shadows = new int[0];
	}

	@Override
	public void die( Object cause ) {
		dispelShadows();
		ThroneProps.Banner b = banner();
		bannerId = -1;
		if (b != null) b.die(null);
		stunned = 0;
		super.die(cause);
		CoopLog.line("grand knight dies t=" + (int) Actor.now());
		GLog.p(Messages.get(this, "freed"));
		later(new Runnable() { public void run() {
			PixelScene.shake(3, 1f);
			GameScene.flash(0x60FFF0C0);
		} });
		if (Dungeon.level instanceof KingdomBossLevel) ((KingdomBossLevel) Dungeon.level).knightFreed(pos);
	}

	@Override
	protected ArrayList<Item> loot() {
		ArrayList<Item> items = new ArrayList<>();
		//same loot Tengu would give
		items.addAll(CoopRewards.masks());
		LloydsBeacon beacon = Dungeon.hero.belongings.getItem(LloydsBeacon.class);
		if (beacon != null) beacon.upgrade();
		items.add(new ShadowBlade().random());
		items.add(new OathShield());
		return items;
	}

	@Override
	public String description() {
		if (intro == 0) return Messages.get(this, "desc_statue");
		return Messages.get(this, "desc") + "\n\n" + Messages.get(this, "desc_act" + Math.max(1, Math.min(4, act)));
	}

	@Override
	public String name() {
		return intro == 0 ? Messages.get(this, "statue_name") : super.name();
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("known_champion", knownChampion);
		bundle.put("act", act);
		bundle.put("intro", intro);
		bundle.put("stunned", stunned);
		bundle.put("banner", bannerId);
		bundle.put("leap", leapCell);
		bundle.put("face", new int[]{faceX, faceY});
		bundle.put("shadow_waves", shadowWaves);
		bundle.put("shadows", shadows);
		bundle.put("shadow_hits", shadowHits);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		knownChampion = bundle.getBoolean("known_champion");
		//old save: a knight saved before the intro existed was already fighting
		boolean old = !bundle.contains("intro");
		act = old ? (HP * 2 <= HT ? 3 : 1) : bundle.getInt("act");
		intro = old ? (state == PASSIVE || state == SLEEPING ? 0 : 4) : bundle.getInt("intro");
		stunned = bundle.getInt("stunned");
		bannerId = bundle.contains("banner") ? bundle.getInt("banner") : -1;
		leapCell = bundle.contains("leap") ? bundle.getInt("leap") : -1;
		if (bundle.contains("face")){
			int[] f = bundle.getIntArray("face");
			faceX = f[0];
			faceY = f[1];
		}
		shadowWaves = bundle.getInt("shadow_waves");
		shadows = bundle.contains("shadows") ? bundle.getIntArray("shadows") : new int[0];
		shadowHits = bundle.getInt("shadow_hits");
		baseSpeed = act == 2 ? 1.5f : 1f;
		if (intro < 4 && state != PASSIVE) state = PASSIVE;
	}

	//shadow knight. smoke warrior, throws its lord's blade fan, nothing can hurt it
	public static class ShadowKnight extends SKMob {

		{
			spriteClass = ShadowSprite.class;
			HP = HT = 1;
			defenseSkill = 0;
			EXP = 0;
			maxLvl = -1;
			properties.add(Property.INORGANIC);
		}

		private int life = 15;

		@Override protected int specialCooldown() { return 3; }
		@Override public int damageRoll() { return 0; }
		@Override public int attackSkill( Char target ) { return 0; }
		@Override protected boolean canAttack( Char enemy ) { return false; }

		@Override
		public boolean isInvulnerable( Class effect ) {
			return true;
		}

		@Override
		protected boolean act() {
			if (--life <= 0){
				vanish();
				return true;
			}
			return super.act();
		}

		@Override
		protected boolean special( Char enemy ) {
			if (!fieldOfView[enemy.pos] || Dungeon.level.distance(pos, enemy.pos) > 7) return false;
			arc(5, 150, enemy.pos, 1.5f, ItemSpriteSheet.SHOT_BLADE, 3, 7);
			if (sprite != null){
				sprite.turnTo(pos, enemy.pos);
				if (sprite instanceof SKMobSprite) ((SKMobSprite) sprite).cast();
			}
			spend(TICK);
			return true;
		}

		void vanish(){
			if (Dungeon.level.heroFOV[pos]) CellEmitter.get(pos).burst(ShadowParticle.UP, 6);
			destroy();
			if (sprite != null) sprite.killAndErase();
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("life", life);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			life = bundle.getInt("life");
		}
	}

	public static class ShadowSprite extends SKMobSprite {
		public ShadowSprite(){
			super();
			setup(ThroneAssets.SHADOW, 32, 36);
		}
		@Override
		public int blood() {
			return 0xFF301040;
		}
	}

	//foot: grand_knight.png 40x40. rows 0-2 = acts I, III, IV (0-1 idle, 2-3 slash, 4-7 walk, 8 ready to thrust,
	//9 thrust, 10 hit). row 3 = throne, leap, stunned, kneeling
	//horse: grand_knight_horse.png 56x48 (0-1 idle, 2-3 slash, 4-7 gallop, 8 rearing, 9-10 the fall)
	public static class KnightSprite extends MobSprite {

		private static final int COLS = 11;
		private static final int SPECIAL = 3 * COLS;

		private TextureFilm foot, horse;
		private String look = "";
		private Animation prepare, lunge, crouch, land, stun, seated, seatedLit, rise, clang;
		private float fadeT = -1;
		private boolean fading = false;

		public KnightSprite(){
			super();
			texture(ThroneAssets.KNIGHT_HORSE);
			horse = new TextureFilm(texture, 56, 48);
			texture(ThroneAssets.KNIGHT);
			foot = new TextureFilm(texture, 40, 40);
			footRow(0);
			still(seated);
		}

		private Animation one( TextureFilm film, int fps, boolean loop, int... frames ){
			Animation a = new Animation(fps, loop);
			Object[] f = new Object[frames.length];
			for (int i = 0; i < frames.length; i++) f[i] = frames[i];
			a.frames(film, f);
			return a;
		}

		private void footRow( int r ){
			texture(ThroneAssets.KNIGHT);
			int o = r * COLS;
			idle = one(foot, 2, true, o, o, o, o + 1, o, o, o + 1, o + 1);
			run = one(foot, 9, true, o + 4, o + 5, o + 6, o + 7);
			attack = one(foot, 12, false, o + 2, o + 3, o);
			zap = attack.clone();
			prepare = one(foot, 1, true, o + 8);
			lunge = one(foot, 8, false, o + 9, o + 9, o);
			die = one(foot, 3, false, SPECIAL + 9, SPECIAL + 10);
			crouch = one(foot, 1, true, SPECIAL + 4);
			land = one(foot, 6, false, SPECIAL + 6, SPECIAL + 6, o);
			stun = one(foot, 4, true, SPECIAL + 7, SPECIAL + 8);
			seated = one(foot, 1, true, SPECIAL);
			seatedLit = one(foot, 1, true, SPECIAL + 1);
			rise = one(foot, 1, true, SPECIAL + 2);
			clang = one(foot, 1, true, SPECIAL + 3);
		}

		private void mounted(){
			texture(ThroneAssets.KNIGHT_HORSE);
			idle = one(horse, 2, true, 0, 0, 1, 1);
			run = one(horse, 12, true, 4, 5, 6, 7);
			attack = one(horse, 12, false, 2, 3, 0);
			zap = attack.clone();
			prepare = one(horse, 1, true, 8);
			lunge = one(horse, 10, false, 6, 7, 0);
			die = one(horse, 4, false, 9, 10);
			crouch = prepare;
			land = attack.clone();
			stun = one(horse, 3, true, 8, 0);
			seated = seatedLit = rise = clang = idle;
		}

		private void still( Animation a ){
			play(a, true);
		}

		//what he should look like now (same idea as the priest)
		private String wanted(){
			if (!(ch instanceof DarkGrandKnight)) return look;
			DarkGrandKnight k = (DarkGrandKnight) ch;
			if (!k.isAlive()) return look;
			if (k.intro < 4) return "intro" + k.intro;
			//horse still running in
			if (k.act == 2 && ThroneVisuals.horseRunning()) return "foot0";
			String s = k.act == 2 ? "horse" : "foot" + (k.act >= 4 ? 2 : k.act == 3 ? 1 : 0);
			return k.stunned > 0 ? s + "_stun" : s;
		}

		private void apply( String w ){
			if (w == null || w.length() < 5) return;
			look = w;
			if (w.startsWith("intro")){
				footRow(0);
				switch (w){
					case "intro0": still(seated); break;
					case "intro1": still(seatedLit); break;
					case "intro2": still(rise); break;
					default: still(clang); break;
				}
			} else {
				if (w.startsWith("horse")) mounted();
				else footRow(w.charAt(4) - '0');
				still(w.endsWith("_stun") ? stun : idle);
			}
			if (ch != null && !isMoving) place(ch.pos);
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			apply(wanted());
		}

		@Override
		public void update() {
			String w = wanted();
			if (!w.equals(look) && (curAnim == null || curAnim.looped || curAnim == idle || curAnim == run) && !isMoving){
				apply(w);
			}
			super.update();
			if (fadeT >= 0){
				fadeT += Game.elapsed;
				if (visible && Random.Float() < Game.elapsed * 10){
					emitter().burst(ShadowParticle.UP, 1);
				}
				if (fadeT > 3f && !fading && parent != null){
					fading = true;
					parent.add(new AlphaTweener(this, 0, 2.5f){
						@Override
						protected void onComplete() {
							KnightSprite.this.killAndErase();
						}
					});
				}
			}
		}

		private Animation swingAnim;

		//same trick as the priest: never play the real 'attack' just for show
		public void swing(){
			swingAnim = attack.clone();
			play(swingAnim);
		}

		public void prepare(){
			play(prepare);
		}

		public void lunge(){
			play(lunge);
		}

		public void crouch(){
			play(crouch);
		}

		public void land(){
			play(land);
		}

		@Override
		public void onComplete( Animation anim ) {
			if (anim == die){
				//stays on his knees a moment before fading. ya sé, same as the priest, copy paste
				fadeT = 0;
				return;
			}
			super.onComplete(anim);
			if (anim == lunge || anim == land || anim == swingAnim) idle();
		}

		@Override
		public int blood() {
			return 0xFF402050;
		}
	}
}
