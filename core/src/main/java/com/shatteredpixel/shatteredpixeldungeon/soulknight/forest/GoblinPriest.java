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

import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.quests.HuntQuest;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BlastParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.WornKey;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKBulletField;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.guns.MeteorStaff;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.watabou.noosa.tweeners.AlphaTweener;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.LinkedHashSet;

/*
 * goblin high priest, boss of the Forest
 *
 * before the fight he leads a ritual around the big bonfire w/ his back to the entrance, goblins dancing
 * and drumming. a hero steps into the clearing -> drums stop, roots close the doors, the bonfire goes up
 * in a column of fire and he turns around.
 * if any ritual totem from the floors above is still standing he drinks its power and becomes the brute:
 * 2x hp, melee (charges, staff slam, grabs and throws heroes), casts less
 *
 *  I   SERMON (100-66%): bursting stars, turret seeds, orbiting orbs, meteors. honor guard every few turns
 *  II  RITUAL (66-33%): 3 totems pop up (fire, ivy, spirit) and nothing hurts him while any is standing.
 *      fire = flames around a hero, ivy = roots one, spirit = the other two take half.
 *      last one falls -> he's stunned and exposed
 *  III RAIN OF STARS (33-0%): night. meteors keep falling on marked cells, and now and then he floats up
 *      and calls a giant star. hurt him enough before the count ends and the spell breaks, if not it hits
 *      the whole clearing and only whoever is next to a sacred stone is safe
 *
 * yes this file is huge. sorry
 */
public class GoblinPriest extends SKMob {

	{
		spriteClass = PriestSprite.class;

		HP = HT = Dungeon.isChallenged(Challenges.STRONGER_BOSSES) ? 135 : 110;
		defenseSkill = 8;
		EXP = 10;

		properties.add(Property.BOSS);
		state = PASSIVE;
	}

	public int intro = 0;           //0 ritual, 1-3 entrance, 4 fighting
	public int act = 1;
	public boolean brute = false;
	//how hard his blows land (-Dspd.test.duel puts him up against a colossus)
	private int power = 1;
	public int stunned = 0;
	public int exposed = 0;         //turns of extra dmg after the ritual broke

	private int[] totems = new int[0];
	private int guardTimer = 6;
	private int[] guardCells = new int[0];
	private int grabbed = -1;       //id of the hero the brute is about to throw

	//act 3 stuff
	public static final int STAR_TURNS = 6;
	public int starTimer = 0;       //turns till the giant star lands, 0 = none coming
	public int starHurt = 0;        //dmg taken while he's calling it
	public int starNeed = 0;
	private int starCooldown = 0;
	private int[] rainCells = new int[0];
	private float rainAt = -1;

	private static final int STARS = 0, SEEDS = 1, ORBS = 2, METEORS = 3, CHARGE = 4, SLAM = 5, GRAB = 6;
	private int lastSpell = -1;

	public boolean fighting(){
		return intro >= 4 && isAlive();
	}

	public boolean channeling(){
		return starTimer > 0;
	}

	public boolean ritualUp(){
		for (int id : totems){
			Actor a = Actor.findById(id);
			if (a instanceof Char && ((Char) a).isAlive()) return true;
		}
		return false;
	}

	//act 2 totems still up, in order fire/ivy/spirit (null where one fell)
	public RitualTotem[] ritualTotems(){
		RitualTotem[] out = new RitualTotem[3];
		for (int id : totems){
			Actor a = Actor.findById(id);
			if (a instanceof RitualTotem && ((RitualTotem) a).isAlive()){
				RitualTotem t = (RitualTotem) a;
				if (t.kind >= 0 && t.kind < 3) out[t.kind] = t;
			}
		}
		return out;
	}

	public boolean hadRitual(){
		return totems.length > 0;
	}

	private ForestBossLevel clearing(){
		return Dungeon.level instanceof ForestBossLevel && ((ForestBossLevel) Dungeon.level).bonfire != -1
				? (ForestBossLevel) Dungeon.level : null;
	}

	private static void later( final Runnable r ){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				r.run();
			}
		});
	}

	// ---------- entrance ----------

	private boolean heroCame(){
		ForestBossLevel L = clearing();
		for (Hero h : Party.living()){
			if (L == null){
				if (Dungeon.level.distance(h.pos, pos) <= 6) return true;
			} else if (L.inClearing(h.pos) && Dungeon.level.distance(h.pos, pos) <= 9){
				return true;
			}
		}
		return Dungeon.level.locked;
	}

	private boolean introAct(){
		ForestBossLevel L = clearing();
		switch (intro){
			case 0: default:
				if (heroCame()){
					intro = 1;
					brute = RitualTotems.empowered();
					BossHealthBar.assignBoss(this);
					Dungeon.level.seal();
					Dungeon.level.gatherParty();
					CoopLog.line("priest wakes t=" + (int) Actor.now() + " brute=" + brute + " totems=" + RitualTotems.broken + "/" + RitualTotems.placed
							+ " heroes=" + Party.living().size());
					PriestVisuals.drumsStop();
					Sample.INSTANCE.play(Assets.Sounds.TRAMPLE, 1f, 0.6f);
					if (L != null) GLog.w(Messages.get(this, "roots"));
				}
				break;
			case 1:
				intro = 2;
				if (L != null) L.bonfire(PriestProps.Bonfire.COLUMN);
				PriestVisuals.dancersFlee();
				Sample.INSTANCE.play(Assets.Sounds.BURNING, 1f, 0.8f);
				later(new Runnable() { public void run() { PixelScene.shake(2, 0.5f); } });
				if (brute){
					//drinks the power of whatever totems are still standing. glug glug
					HT *= 2;
					HP = HT;
					PriestVisuals.absorb(pos);
					Sample.INSTANCE.play(Assets.Sounds.CHARGEUP, 1f, 0.7f);
					GLog.n(Messages.get(this, "absorb", RitualTotems.standing()));
				}
				break;
			case 2:
				intro = 3;
				if (brute){
					Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 1f, 0.6f);
					later(new Runnable() { public void run() {
						PixelScene.shake(4, 0.7f);
						GameScene.flash(0x60FF4020);
					} });
				} else {
					Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 1f, 0.9f);
					later(new Runnable() { public void run() { GameScene.flash(0x50FFE080); } });
				}
				PriestHud.title(Messages.get(this, brute ? "title_brute" : "title_name"),
						Messages.get(this, brute ? "title_sub_brute" : "title_sub"));
				yell(Messages.get(this, brute ? "notice_brute" : "notice"));
				break;
			case 3:
				intro = 4;
				state = HUNTING;
				cooldown = 1;
				if (L != null) L.bonfire(PriestProps.Bonfire.LIT);
				testStart();
				HuntQuest.priestFight(this);
				break;
		}
		spend(TICK);
		return true;
	}

	//-Dspd.test.priestweak=1 -> low hp, -Dspd.test.priestact=2|3 starts in that act, -Dspd.test.duel -> the
	//life and the blows to stand up to a colossus
	private void testStart(){
		if (DevFlags.duel() > 0){
			HT *= 20;
			HP = HT;
			power = 15;
		}
		if (DevFlags.priestWeak()){
			HT = brute ? 60 : 30;
			HP = HT;
		}
		int start = DevFlags.priestAct();
		if (start >= 2){ HP = HT * 2 / 3; toAct2(); }
		if (start >= 3){
			for (int id : totems){
				Actor a = Actor.findById(id);
				if (a instanceof RitualTotem) ((RitualTotem) a).die(null);
			}
			HP = HT / 3;
			toAct3();
		}
	}

	@Override
	public void notice() {
		//priest from an older save (no clearing to wake up in): fight starts when he sees a hero
		if (intro >= 4 && !BossHealthBar.isAssigned()){
			super.notice();
			BossHealthBar.assignBoss(this);
			Dungeon.level.seal();
			Dungeon.level.gatherParty();
			yell(Messages.get(this, "notice"));
		}
	}

	@Override
	public boolean isInvulnerable( Class effect ) {
		return intro < 4 || ritualUp() || super.isInvulnerable(effect);
	}

	@Override
	protected boolean act() {
		if (!isAlive()) return true;
		if (intro < 4) return introAct();

		if (rainCells.length > 0 && Actor.now() >= rainAt) rainFalls();

		if (totems.length > 0 && act == 2 && !ritualUp()) ritualBroken();
		if (act == 2 && !ritualUp() && HP * 3 <= HT) toAct3();
		if (exposed > 0) exposed--;

		if (stunned > 0){
			stunned--;
			spend(TICK);
			return true;
		}

		if (starTimer > 0){
			if (--starTimer == 0) starFalls();
			spend(TICK);
			return true;
		}
		if (starCooldown > 0) starCooldown--;

		if (act >= 3 && rainCells.length == 0 && Dungeon.level.locked) markRain();

		if (guardCells.length > 0){
			callGuards();
			spend(TICK);
			return true;
		}
		//no guard during the ritual, the totems are enough pain already
		if (state == HUNTING && Dungeon.level.locked && !ritualUp() && --guardTimer <= 0){
			guardTimer = brute ? 10 : 8;
			if (markGuards()){
				spend(TICK);
				return true;
			}
		}
		return super.act();
	}

	//thin priest keeps his distance, the brute comes at you
	@Override
	protected boolean keepsDistance() {
		return !brute;
	}

	@Override
	protected boolean getCloser( int target ) {
		if (stunned > 0 || starTimer > 0) return false;
		return super.getCloser(target);
	}

	@Override
	protected boolean getFurther( int target ) {
		if (stunned > 0 || starTimer > 0) return false;
		return super.getFurther(target);
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		if (stunned > 0 || starTimer > 0) return false;
		return super.canAttack(enemy);
	}

	@Override
	public int damageRoll() {
		return power * (brute ? Random.NormalIntRange(3, 8) : Random.NormalIntRange(2, 6));
	}

	@Override
	public int attackSkill( Char target ) {
		return brute ? 14 : 12;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, brute ? 3 : 2);
	}

	@Override
	protected int specialCooldown() {
		if (act >= 3) return brute ? 2 : Random.IntRange(1, 2);
		if (act == 2) return Random.IntRange(3, 4);
		return Random.IntRange(2, 3);
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (!isAlive() || intro < 4) return;
		if (dmg > 0){
			float m = 1f;
			if (exposed > 0) m *= 1.5f;
			else if (stunned > 0) m *= 1.25f;
			dmg = Math.max(1, Math.round(dmg * m));
		}
		int before = HP;
		super.damage(dmg, src);
		if (!isAlive()) return;
		if (starTimer > 0 && HP < before){
			starHurt += before - HP;
			if (starHurt >= starNeed) starBroken();
		}
		if (act == 1 && HP * 3 <= HT * 2) toAct2();
		if (act == 2 && !ritualUp() && HP * 3 <= HT) toAct3();
	}

	// *** act 2, ritual ***

	private void toAct2(){
		act = 2;
		pendingKind = -1;
		grabbed = -1;
		cooldown = 2;
		ritual();
		PriestHud.banner(Messages.get(this, "banner_ritual"));
		GLog.w(Messages.get(this, "hint_2"));
		CoopLog.line("priest act 2 t=" + (int) Actor.now() + " hp=" + HP + "/" + HT);
	}

	//3 totems around the clearing, spread out: fire, ivy, spirit
	private void ritual(){
		ArrayList<Integer> spots = new ArrayList<>();
		ForestBossLevel L = clearing();
		for (int c : SKCombat.area(pos, 6)){
			int d = Dungeon.level.distance(c, pos);
			if (d < 3 || !Dungeon.level.passable[c] || Dungeon.level.pit[c] || Actor.findChar(c) != null) continue;
			if (L != null && (!L.inClearing(c) || L.nearDoor(c))) continue;
			spots.add(c);
		}
		Random.shuffle(spots);
		ArrayList<Integer> chosen = new ArrayList<>();
		for (int c : spots){
			boolean far = true;
			for (int o : chosen) if (Dungeon.level.distance(c, o) < 4) far = false;
			if (far) chosen.add(c);
			if (chosen.size() == 3) break;
		}
		if (chosen.isEmpty()) return;
		totems = new int[chosen.size()];
		for (int i = 0; i < chosen.size(); i++){
			RitualTotem t = new RitualTotem();
			t.kind = i;
			t.pos = chosen.get(i);
			GameScene.add(t);
			ScrollOfTeleportation.appearVFX(t);
			if (Dungeon.level.heroFOV[t.pos]) CellEmitter.get(t.pos).burst(Speck.factory(Speck.ROCK), 5);
			totems[i] = t.id();
		}
		if (sprite != null) sprite.add(CharSprite.State.SHIELDED);
		later(new Runnable() { public void run() { PixelScene.shake(3, 0.6f); } });
		yell(Messages.get(this, "ritual"));
		CoopLog.line("priest ritual totems=" + totems.length);
	}

	private void ritualBroken(){
		if (sprite != null) sprite.remove(CharSprite.State.SHIELDED);
		stun(3);
		exposed = 4;
		yell(Messages.get(this, "ritual_broken"));
		GLog.p(Messages.get(this, "exposed"));
		PriestHud.banner(Messages.get(this, "banner_exposed"));
		totems = new int[0];
	}

	public void stun( int turns ){
		stunned = Math.max(stunned, turns);
		pendingKind = -1;
		grabbed = -1;
		if (sprite != null && Dungeon.level.heroFOV[pos]) sprite.showStatus(CharSprite.POSITIVE, Messages.get(this, "stunned_short"));
	}

	// *** act 3: star rain ***

	private void toAct3(){
		act = 3;
		pendingKind = -1;
		grabbed = -1;
		starCooldown = 1;
		totems = new int[0];
		if (sprite != null) sprite.remove(CharSprite.State.SHIELDED);
		flying = true;
		yell(Messages.get(this, "enraged"));
		GLog.w(Messages.get(this, "hint_3"));
		PriestHud.banner(Messages.get(this, "banner_night"));
		later(new Runnable() { public void run() {
			PixelScene.shake(3, 1f);
			GameScene.flash(0x6020206A);
		} });
		if (Dungeon.level instanceof ForestBossLevel) ((ForestBossLevel) Dungeon.level).finalMusic();
		CoopLog.line("priest act 3 t=" + (int) Actor.now() + " hp=" + HP + "/" + HT);
	}

	//one meteor near each hero (on them or next to them) + one more anywhere close
	private void markRain(){
		ArrayList<Integer> cells = new ArrayList<>();
		ForestBossLevel L = clearing();
		for (Hero h : Party.living()){
			if (L != null && !L.inClearing(h.pos)) continue;
			ArrayList<Integer> near = SKCombat.area(h.pos, 1);
			int c = Random.Int(2) == 0 ? h.pos : near.get(Random.Int(near.size()));
			if (!cells.contains(c) && Dungeon.level.passable[c]) cells.add(c);
		}
		if (cells.isEmpty()) return;
		rainCells = new int[cells.size()];
		for (int i = 0; i < cells.size(); i++) rainCells[i] = cells.get(i);
		float delay = SKCombat.announceDelay(TICK) + 1f;
		rainAt = Actor.now() + delay;
		SKCombat.mark(cells, delay);
	}

	private void rainFalls(){
		ArrayList<Integer> cells = new ArrayList<>();
		for (int c : rainCells) cells.add(c);
		rainCells = new int[0];
		rainAt = -1;
		for (int c : cells){
			if (Dungeon.level.heroFOV[c]){
				CellEmitter.center(c).burst(BlastParticle.FACTORY, 2);
				CellEmitter.get(c).burst(FlameParticle.FACTORY, 2);
			}
		}
		Sample.INSTANCE.play(Assets.Sounds.BLAST, 0.6f, 1.3f);
		for (Char ch : SKCombat.foesIn(this, cells)){
			ch.damage(Math.max(1, Random.NormalIntRange(3, 6) - ch.drRoll() / 2), this);
		}
	}

	private void callStar(){
		starTimer = STAR_TURNS;
		starHurt = 0;
		starNeed = Math.max(8, HT * 12 / 100);
		starNeed = HuntQuest.starNeed(starNeed);
		pendingKind = -1;
		grabbed = -1;
		rainCells = new int[0];
		ForestBossLevel L = clearing();
		PriestVisuals.starComing(L != null ? L.center() : pos);
		yell(Messages.get(this, "star_call"));
		GLog.w(Messages.get(this, "star_hint", starNeed));
		Sample.INSTANCE.play(Assets.Sounds.CHARGEUP, 1f, 0.5f);
		for (Hero h : Party.living()) h.interrupt();
		CoopLog.line("priest calls the star t=" + (int) Actor.now() + " need=" + starNeed);
	}

	private void starBroken(){
		starTimer = 0;
		starCooldown = 14;
		stun(3);
		PriestVisuals.starBroken();
		yell(Messages.get(this, "star_broken"));
		GLog.p(Messages.get(this, "star_broken_hint"));
		PriestHud.banner(Messages.get(this, "banner_star_broken"));
		Sample.INSTANCE.play(Assets.Sounds.SHATTER, 1f, 0.8f);
		later(new Runnable() { public void run() { GameScene.flash(0x60FFFFFF); } });
		CoopLog.line("priest star broken t=" + (int) Actor.now());
	}

	//star hits the whole clearing, only those next to a sacred stone are safe
	private void starFalls(){
		starCooldown = 14;
		ForestBossLevel L = clearing();
		PriestVisuals.starFalls();
		Sample.INSTANCE.play(Assets.Sounds.BLAST, 1f, 0.5f);
		Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 0.7f);
		later(new Runnable() { public void run() {
			PixelScene.shake(8, 1.2f);
			GameScene.flash(0xA0FFF0C0);
		} });
		int hit = 0;
		for (Char ch : Actor.chars().toArray(new Char[0])){
			if (ch == this || !ch.isAlive() || ch instanceof RitualTotem || Char.hasProp(ch, Property.IMMOVABLE)) continue;
			if (L != null && !L.inClearing(ch.pos)) continue;
			if (L != null && L.safeFromStar(ch.pos)) continue;
			//half their max hp, 8 min. his own goblins eat the whole thing, oops
			int dmg = Math.max(8, ch.HT / 2);
			if (ch.alignment == Alignment.ENEMY) dmg = ch.HT;
			//GLog.w("star -> " + ch.name() + " " + dmg);
			ch.damage(dmg, this);
			hit++;
		}
		if (L != null) for (PriestProps.SacredStone s : L.stones()) s.crack();
		yell(Messages.get(this, "star_fell"));
		CoopLog.line("priest star falls t=" + (int) Actor.now() + " hit=" + hit);
	}

	private int pick( int... options ){
		int s;
		int tries = 0;
		do {
			s = options[Random.Int(options.length)];
		} while (s == lastSpell && options.length > 1 && ++tries < 10);
		lastSpell = s;
		return s;
	}

	@Override
	protected boolean special( Char enemy ) {
		if (!fieldOfView[enemy.pos] || stunned > 0 || starTimer > 0) return false;
		if (act >= 3 && starCooldown <= 0){
			callStar();
			spend(TICK);
			return true;
		}
		int spell;
		if (brute){
			boolean adjacent = Dungeon.level.adjacent(pos, enemy.pos);
			//don't grab a rooted hero, no way out of that
			if (adjacent && enemy instanceof Hero && !enemy.rooted && Random.Int(2) == 0) spell = GRAB;
			else if (act >= 3) spell = pick(CHARGE, SLAM, CHARGE, STARS);
			else spell = pick(CHARGE, SLAM, STARS, CHARGE, SLAM, ORBS);
		} else {
			ArrayList<Integer> options = new ArrayList<>();
			options.add(STARS);
			options.add(SEEDS);
			if (orbsAlive() == 0) options.add(ORBS);
			if (act < 3) options.add(METEORS);
			int[] o = new int[options.size()];
			for (int i = 0; i < o.length; i++) o[i] = options.get(i);
			spell = pick(o);
		}
		if (spell == ORBS && orbsAlive() > 0) spell = STARS;
		CoopLog.line("priest spell " + spell + " t=" + (int) Actor.now() + " hp=" + HP + " act=" + act);

		if (sprite != null) sprite.turnTo(pos, enemy.pos);
		switch (spell){
			case STARS: default:  castStars(enemy); break;
			case SEEDS:           castSeeds(enemy); break;
			case ORBS:            castOrbs(); break;
			case METEORS:         return announceMeteors(enemy);
			case CHARGE:          return announceCharge(enemy);
			case SLAM:            return announceSlam();
			case GRAB:            return announceGrab((Hero) enemy);
		}
		if (sprite instanceof PriestSprite) ((PriestSprite) sprite).cast();
		spend(TICK);
		return true;
	}

	private int orbsAlive(){
		SKBulletField field = (SKBulletField) Dungeon.level.blobs.get(SKBulletField.class);
		if (field == null) return 0;
		int n = 0;
		for (SKBulletField.Bullet b : field.bullets){
			if (b.ownerId == id() && b.behaviour == SKBulletField.ORBIT && b.life > 0) n++;
		}
		return n;
	}

	private void castStars( Char enemy ){
		SKBulletField field = SKBulletField.get();
		int n = act >= 3 ? 8 : 6;
		float start = SKBulletField.angleTo(pos, enemy.pos) + 180f / n;
		for (int i = 0; i < n; i++){
			SKBulletField.Bullet b = field.fire(this, pos, start + i * 360f / n, 1f, ItemSpriteSheet.SHOT_STAR, 2, 5);
			b.behaviour = SKBulletField.SPLIT;
			b.timer = 3;
			b.life = 4;
			b.children = 3;
			b.spread = 40;
			b.childImage = ItemSpriteSheet.SHOT_BULLET;
			b.childSpeed = SKBulletField.UNIT * 3 / 2;
		}
		if (Dungeon.level.heroFOV[pos]){
			Sample.INSTANCE.play(Assets.Sounds.ZAP, 1f, 0.8f);
			GLog.w(Messages.get(this, "stars"));
		}
	}

	private void castSeeds( Char enemy ){
		SKBulletField field = SKBulletField.get();
		float aim = SKBulletField.angleTo(pos, enemy.pos);
		float[] angles = act >= 2 ? new float[]{aim - 25, aim + 25} : new float[]{aim};
		for (float a : angles){
			SKBulletField.Bullet b = field.fire(this, pos, a, 1f, ItemSpriteSheet.SHOT_BUBBLE, 2, 5);
			b.behaviour = SKBulletField.SEEDER;
			b.timer = 1;
			b.period = 2;
			b.life = 7;
			b.childImage = ItemSpriteSheet.SHOT_BULLET;
			b.markImage = ItemSpriteSheet.SHOT_ORB;
		}
		if (Dungeon.level.heroFOV[pos]){
			Sample.INSTANCE.play(Assets.Sounds.PLANT, 1f, 0.8f);
			GLog.w(Messages.get(this, "seeds"));
		}
	}

	private void castOrbs(){
		SKBulletField field = SKBulletField.get();
		int n = act >= 2 ? 3 : 2;
		for (int i = 0; i < n; i++){
			field.orbit(this, i * 8 / n, 1, 10, ItemSpriteSheet.SHOT_ORB, 2, 5);
		}
		if (Dungeon.level.heroFOV[pos]){
			Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);
			GLog.w(Messages.get(this, "orbs"));
		}
	}

	//one meteor on each hero he can see, the rest near his target
	private boolean announceMeteors( Char enemy ){
		ArrayList<Integer> centers = new ArrayList<>();
		for (Hero h : Party.living()){
			if (fieldOfView != null && fieldOfView[h.pos]) centers.add(h.pos);
		}
		if (centers.isEmpty()) centers.add(enemy.pos);
		int wanted = Math.max(centers.size(), act >= 2 ? 4 : 3);
		int tries = 0;
		while (centers.size() < wanted && tries++ < 30){
			ArrayList<Integer> near = SKCombat.area(enemy.pos, 3);
			int c = near.get(Random.Int(near.size()));
			if (!centers.contains(c)) centers.add(c);
		}
		LinkedHashSet<Integer> cells = new LinkedHashSet<>();
		for (int c : centers) cells.addAll(SKCombat.openArea(c, 1));
		announce(METEORS, cells, enemy.pos);
		if (Dungeon.level.heroFOV[pos]){
			if (sprite != null) sprite.showStatus(CharSprite.WARNING, "!!!");
			GLog.w(Messages.get(this, "meteors"));
		}
		return true;
	}

	//brute: charge along a marked line
	private boolean announceCharge( Char enemy ){
		int d = Dungeon.level.distance(pos, enemy.pos);
		ArrayList<Integer> cells = line(enemy.pos, 6);
		if (d < 2 || cells.size() < 2) return announceSlam();
		announce(CHARGE, cells, enemy.pos);
		if (sprite instanceof PriestSprite) ((PriestSprite) sprite).prepare();
		if (Dungeon.level.heroFOV[pos]) GLog.w(Messages.get(this, "charge"));
		return true;
	}

	//brute: staff slam, everything around shakes
	private boolean announceSlam(){
		ArrayList<Integer> cells = new ArrayList<>();
		for (int c : SKCombat.openArea(pos, 2)) if (c != pos) cells.add(c);
		announce(SLAM, cells, -1);
		if (sprite instanceof PriestSprite) ((PriestSprite) sprite).cast();
		if (Dungeon.level.heroFOV[pos]) GLog.w(Messages.get(this, "slam"));
		return true;
	}

	//brute: reaches for a hero next to him
	private boolean announceGrab( Hero h ){
		grabbed = h.id();
		ArrayList<Integer> cells = new ArrayList<>();
		cells.add(h.pos);
		announce(GRAB, cells, h.pos);
		if (sprite instanceof PriestSprite) ((PriestSprite) sprite).reach();
		if (Dungeon.level.heroFOV[pos]){
			GLog.w(Party.active() ? Messages.get(this, "grab", Party.name(h)) : Messages.get(this, "grab_solo"));
		}
		return true;
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		switch (kind){
			case METEORS: meteors(cells); break;
			case CHARGE:  rush(cells); break;
			case SLAM:    slam(cells); break;
			case GRAB:    grabAndThrow(cells); break;
		}
	}

	private void meteors( ArrayList<Integer> cells ){
		boolean anyVisible = false;
		for (int c : cells){
			if (Dungeon.level.heroFOV[c]){
				anyVisible = true;
				CellEmitter.center(c).burst(BlastParticle.FACTORY, 3);
				CellEmitter.get(c).burst(FlameParticle.FACTORY, 3);
			}
		}
		if (anyVisible){
			Sample.INSTANCE.play(Assets.Sounds.BLAST);
			later(new Runnable() { public void run() { PixelScene.shake(2, 0.5f); } });
		}
		for (Char ch : SKCombat.foesIn(this, cells)){
			ch.damage(Math.max(1, Random.NormalIntRange(3, 8) - ch.drRoll() / 2), this);
		}
	}

	private void rush( ArrayList<Integer> cells ){
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
		if (sprite instanceof PriestSprite) ((PriestSprite) sprite).lunge();
		if (obstacle instanceof PriestProps.SacredStone || obstacle instanceof PriestProps.Bonfire){
			if (obstacle instanceof PriestProps.SacredStone) ((PriestProps.SacredStone) obstacle).crack();
			stun(2);
			GLog.p(Messages.get(this, "crashed"));
			later(new Runnable() { public void run() { PixelScene.shake(5, 0.6f); } });
		} else if (victim != null){
			attack(victim, 1.3f, 0, 2f);
			if (victim.isAlive()) knock(victim, 2);
		}
	}

	private void slam( ArrayList<Integer> cells ){
		for (Char ch : SKCombat.foesIn(this, cells)){
			ch.damage(Math.max(1, Random.NormalIntRange(4, 8) - ch.drRoll() / 2), this);
			if (ch.isAlive() && Dungeon.level.adjacent(pos, ch.pos)) knock(ch, 1);
		}
		for (int i = 0; i < 8; i++){
			shoot(i * 45f + 22.5f, 1f, ItemSpriteSheet.SHOT_BULLET, 2, 5);
		}
		if (sprite instanceof PriestSprite) ((PriestSprite) sprite).slam();
		if (Dungeon.level.heroFOV[pos]){
			Sample.INSTANCE.play(Assets.Sounds.BLAST, 1f, 0.6f);
			for (int c : cells) CellEmitter.get(c).burst(Speck.factory(Speck.DUST), 2);
		}
		later(new Runnable() { public void run() { PixelScene.shake(4, 0.5f); } });
	}

	//hero still next to him gets thrown, 4 cells max, hurts more against a wall
	private void grabAndThrow( ArrayList<Integer> cells ){
		Actor a = grabbed == -1 ? null : Actor.findById(grabbed);
		grabbed = -1;
		if (!(a instanceof Hero) || !((Hero) a).isAlive()) return;
		Hero h = (Hero) a;
		if (!Dungeon.level.adjacent(pos, h.pos) && !cells.contains(h.pos)){
			if (sprite instanceof PriestSprite) ((PriestSprite) sprite).swing();
			return;
		}
		int w = Dungeon.level.width();
		int dx = Integer.signum(h.pos % w - pos % w), dy = Integer.signum(h.pos / w - pos / w);
		if (dx == 0 && dy == 0) dx = 1;
		int from = h.pos, to = h.pos;
		boolean wall = false;
		for (int i = 0; i < 4; i++){
			int next = to + dx + dy * w;
			if (!Dungeon.level.passable[next] || Actor.findChar(next) != null){
				wall = true;
				break;
			}
			to = next;
		}
		if (sprite instanceof PriestSprite) ((PriestSprite) sprite).toss();
		if (to != from){
			h.pos = to;
			Actor.add(new Pushing(h, from, to));
			Dungeon.level.occupyCell(h);
		}
		int dmg = Random.NormalIntRange(3, 7) + (wall ? 3 : 0);
		h.damage(Math.max(1, dmg - h.drRoll() / 2), this);
		if (Dungeon.level.heroFOV[to]){
			Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG, 1f, 0.7f);
			CellEmitter.get(to).burst(Speck.factory(Speck.DUST), 4);
		}
		later(new Runnable() { public void run() { PixelScene.shake(3, 0.4f); } });
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

	// honor guard

	private int guardsAlive(){
		int n = 0;
		for (Mob m : Dungeon.level.mobs){
			if ((m instanceof GoblinSpearman || m instanceof GoblinGunner || m instanceof GoblinElite) && m.isAlive()) n++;
		}
		return n;
	}

	//2 cells away from the heroes, marked a turn before the guards show up
	private boolean markGuards(){
		if (guardsAlive() >= 4) return false;
		ForestBossLevel L = clearing();
		ArrayList<Integer> spots = new ArrayList<>();
		for (int c : SKCombat.area(pos, 6)){
			if (!Dungeon.level.passable[c] || Dungeon.level.pit[c] || Actor.findChar(c) != null) continue;
			if (L != null && !L.inClearing(c)) continue;
			boolean far = true;
			for (Hero h : Party.living()) if (Dungeon.level.distance(c, h.pos) < 4) far = false;
			if (far) spots.add(c);
		}
		if (spots.isEmpty()) return false;
		Random.shuffle(spots);
		int n = Math.min(2, spots.size());
		guardCells = new int[n];
		ArrayList<Integer> marked = new ArrayList<>();
		for (int i = 0; i < n; i++){
			guardCells[i] = spots.get(i);
			marked.add(spots.get(i));
		}
		SKCombat.mark(marked, TICK);
		if (sprite instanceof PriestSprite) ((PriestSprite) sprite).cast();
		if (Dungeon.level.heroFOV[pos]) GLog.w(Messages.get(this, "guards"));
		return true;
	}

	private void callGuards(){
		for (int c : guardCells){
			if (Actor.findChar(c) != null || !Dungeon.level.passable[c]) continue;
			Mob m = act >= 2 && Random.Int(2) == 0 ? new GoblinElite() : (Random.Int(2) == 0 ? new GoblinSpearman() : new GoblinGunner());
			m.EXP = 0;
			m.pos = c;
			m.state = m.HUNTING;
			GameScene.add(m);
			ScrollOfTeleportation.appearVFX(m);
		}
		guardCells = new int[0];
	}

	@Override
	public void die( Object cause ) {
		//his totems die w/ him and the honor guard runs off into the jungle
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (!m.isAlive()) continue;
			if (m instanceof RitualTotem) m.die(null);
			else if ((m instanceof GoblinSpearman || m instanceof GoblinGunner)
					&& m.alignment == Alignment.ENEMY){
				if (Dungeon.level.heroFOV[m.pos]) CellEmitter.get(m.pos).burst(Speck.factory(Speck.SMOKE), 4);
				m.destroy();
				if (m.sprite != null) m.sprite.killAndErase();
			}
		}
		starTimer = 0;
		rainCells = new int[0];
		stunned = 0;
		super.die(cause);

		SKBulletField field = (SKBulletField) Dungeon.level.blobs.get(SKBulletField.class);
		if (field != null) field.clearOwner(this);

		ForestBossLevel L = clearing();
		if (L != null) L.bonfire(PriestProps.Bonfire.OUT);
		Dungeon.level.unseal();
		GameScene.bossSlain();

		Dungeon.level.drop(new WornKey(Dungeon.depth), pos).sprite.drop();
		Dungeon.level.drop(new MeteorStaff().random(), freeCellAround()).sprite.drop(pos);
		Dungeon.level.drop(new PortableTotem(), freeCellAround()).sprite.drop(pos);
		if (brute) Dungeon.level.drop(new PriestBracers(), freeCellAround()).sprite.drop(pos);

		Badges.validateBossSlain();
		if (Statistics.qualifiedForBossChallengeBadge){
			Badges.validateBossChallengeCompleted();
		}
		Statistics.bossScores[0] += brute ? 1500 : 1000;

		yell(Messages.get(this, brute ? "defeated_brute" : "defeated"));
		GLog.p(Messages.get(this, "freed"));
		later(new Runnable() { public void run() {
			PixelScene.shake(3, 1f);
			GameScene.flash(0x60FFE0A0);
		} });
		PriestVisuals.priestFell();
		CoopLog.line("priest dies t=" + (int) Actor.now() + " brute=" + brute);
	}

	private int freeCellAround(){
		for (int tries = 0; tries < 20; tries++){
			int c = pos + PathFinder.NEIGHBOURS8[Random.Int(8)];
			if (!Dungeon.level.solid[c] && Dungeon.level.passable[c] && Actor.findChar(c) == null) return c;
		}
		return pos;
	}

	@Override
	public String name() {
		return Messages.get(this, brute ? "name_brute" : "name");
	}

	@Override
	public String description() {
		if (intro == 0) return Messages.get(this, "desc_rite");
		return Messages.get(this, brute ? "desc_brute" : "desc") + "\n\n" + Messages.get(this, "desc_act" + Math.max(1, Math.min(3, act)));
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("intro", intro);
		bundle.put("act", act);
		bundle.put("brute", brute);
		bundle.put("stunned", stunned);
		bundle.put("exposed", exposed);
		bundle.put("totems", totems);
		bundle.put("guard_timer", guardTimer);
		bundle.put("guard_cells", guardCells);
		bundle.put("grabbed", grabbed);
		bundle.put("star_timer", starTimer);
		bundle.put("star_hurt", starHurt);
		bundle.put("star_need", starNeed);
		bundle.put("star_cooldown", starCooldown);
		bundle.put("rain_cells", rainCells);
		bundle.put("rain_at", rainAt);
		bundle.put("last_spell", lastSpell);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		//HACK old saves: a priest saved before the ritual clearing existed was already awake
		boolean old = !bundle.contains("intro");
		intro = old ? 4 : bundle.getInt("intro");
		act = old ? (bundle.getBoolean("enraged") ? 2 : 1) : bundle.getInt("act");
		brute = bundle.getBoolean("brute");
		stunned = bundle.getInt("stunned");
		exposed = bundle.getInt("exposed");
		totems = bundle.contains("totems") ? bundle.getIntArray("totems") : new int[0];
		guardTimer = bundle.contains("guard_timer") ? bundle.getInt("guard_timer") : 6;
		guardCells = bundle.contains("guard_cells") ? bundle.getIntArray("guard_cells") : new int[0];
		grabbed = bundle.contains("grabbed") ? bundle.getInt("grabbed") : -1;
		starTimer = bundle.getInt("star_timer");
		starHurt = bundle.getInt("star_hurt");
		starNeed = bundle.getInt("star_need");
		starCooldown = bundle.getInt("star_cooldown");
		rainCells = bundle.contains("rain_cells") ? bundle.getIntArray("rain_cells") : new int[0];
		rainAt = bundle.contains("rain_at") ? bundle.getFloat("rain_at") : -1;
		lastSpell = bundle.contains("last_spell") ? bundle.getInt("last_spell") : -1;
		flying = act >= 3;
		if (intro >= 1 && state != SLEEPING) BossHealthBar.assignBoss(this);
		if (intro < 4 && state != PASSIVE) state = PASSIVE;
	}

	// *** ritual totems (act 2) ***

	//kind 0 fire: flames around a hero every 3 turns. 1 ivy: roots a hero every 4. 2 spirit: the other
	//totems take half dmg while it's up
	public static class RitualTotem extends Mob {

		{
			spriteClass = RitualTotemSprite.class;
			defenseSkill = 0;
			EXP = 0;
			properties.add(Property.IMMOVABLE);
			properties.add(Property.INORGANIC);
			state = PASSIVE;
		}

		public static final int FIRE = 0, IVY = 1, SPIRIT = 2;
		public int kind = FIRE;
		private int timer = 2;
		private int[] marked = new int[0];
		private float markedAt = -1;

		public RitualTotem(){
			super();
			HP = HT = 8 + 2 * Dungeon.scalingDepth();
		}

		private boolean spiritStands(){
			for (Mob m : Dungeon.level.mobs){
				if (m instanceof RitualTotem && m != this && ((RitualTotem) m).kind == SPIRIT && m.isAlive()) return true;
			}
			return false;
		}

		@Override
		public void damage( int dmg, Object src ) {
			if (dmg > 0 && kind != SPIRIT && spiritStands()){
				dmg = Math.max(1, dmg / 2);
				if (sprite != null && Dungeon.level.heroFOV[pos] && Random.Int(2) == 0){
					sprite.showStatus(CharSprite.NEUTRAL, Messages.get(GoblinPriest.class, "spirit_guard"));
				}
			}
			super.damage(dmg, src);
		}

		@Override
		protected boolean act() {
			spend(TICK);
			if (marked.length > 0 && Actor.now() >= markedAt){
				strike();
				return true;
			}
			if (kind == SPIRIT || --timer > 0) return true;
			timer = kind == FIRE ? 4 : 5;
			Hero target = null;
			for (Hero h : Party.living()){
				if (Dungeon.level.distance(h.pos, pos) > 9) continue;
				if (target == null || Dungeon.level.distance(h.pos, pos) < Dungeon.level.distance(target.pos, pos)) target = h;
			}
			if (target == null) return true;
			ArrayList<Integer> cells = kind == FIRE ? SKCombat.openArea(target.pos, 1) : new ArrayList<Integer>();
			if (kind == IVY) cells.add(target.pos);
			marked = new int[cells.size()];
			for (int i = 0; i < marked.length; i++) marked[i] = cells.get(i);
			float delay = SKCombat.announceDelay(TICK);
			markedAt = Actor.now() + delay;
			SKCombat.mark(cells, delay);
			if (sprite != null) sprite.showStatus(CharSprite.WARNING, "!");
			return true;
		}

		private void strike(){
			ArrayList<Integer> cells = new ArrayList<>();
			for (int c : marked) cells.add(c);
			marked = new int[0];
			markedAt = -1;
			for (int c : cells){
				if (!Dungeon.level.heroFOV[c]) continue;
				if (kind == FIRE) CellEmitter.get(c).burst(FlameParticle.FACTORY, 4);
				else CellEmitter.get(c).burst(LeafParticle.GENERAL, 6);
			}
			Sample.INSTANCE.play(kind == FIRE ? Assets.Sounds.BURNING : Assets.Sounds.PLANT, 0.8f, 0.9f);
			for (Char ch : SKCombat.foesIn(this, cells)){
				if (kind == FIRE){
					ch.damage(Math.max(1, Random.NormalIntRange(3, 6) - ch.drRoll() / 2), this);
				} else {
					ch.damage(Random.NormalIntRange(1, 3), this);
					if (ch.isAlive()) Buff.prolong(ch, Roots.class, 2f);
				}
			}
		}

		@Override
		protected boolean canAttack( Char enemy ) {
			return false;
		}

		@Override
		protected boolean getCloser( int target ) {
			return false;
		}

		@Override
		protected boolean getFurther( int target ) {
			return false;
		}

		@Override
		public void die( Object cause ) {
			super.die(cause);
			if (Dungeon.level.heroFOV[pos]){
				CellEmitter.get(pos).burst(Speck.factory(Speck.ROCK), 6);
				CellEmitter.get(pos).burst(ShadowParticle.UP, 6);
			}
			GLog.p(Messages.get(GoblinPriest.class, "totem_fell_" + kind));
		}

		@Override
		public String name() {
			return Messages.get(GoblinPriest.class, "totem_name_" + kind);
		}

		@Override
		public String description() {
			return Messages.get(GoblinPriest.class, "totem_desc_" + kind);
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("kind", kind);
			bundle.put("timer", timer);
			bundle.put("marked", marked);
			bundle.put("marked_at", markedAt);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			kind = bundle.getInt("kind");
			timer = bundle.contains("timer") ? bundle.getInt("timer") : 2;
			marked = bundle.contains("marked") ? bundle.getIntArray("marked") : new int[0];
			markedAt = bundle.contains("marked_at") ? bundle.getFloat("marked_at") : -1;
		}
	}

	public static class RitualTotemSprite extends RitualTotems.TotemSprite {
		@Override
		protected int wantedRow() {
			if (ch instanceof RitualTotem){
				switch (((RitualTotem) ch).kind){
					case RitualTotem.IVY: return PriestAssets.TOTEM_IVY;
					case RitualTotem.SPIRIT: return PriestAssets.TOTEM_SPIRIT;
					default: return PriestAssets.TOTEM_FIRE;
				}
			}
			return PriestAssets.TOTEM_FIRE;
		}
	}

	/* sprites
	   thin: sk_goblin_priest.png (20x20: 0-1 idle, 2-3 cast, 4-7 run, 8-10 death) + priest_rite.png
	         (20x20: 0-1 back turned in the ritual, 2 turning, 3-4 floating, 5-6 stunned, 7-9 kneeling)
	   brute: priest_brute.png (32x32). row 0 idle, swing, walk, death. row 1 charge, grab, throw, slam,
	          stunned, cast, floating, roar. row 2 = thin priest swelling into the brute */
	public static class PriestSprite extends MobSprite {

		private static final int COLS = 11;

		private TextureFilm thin, rite, big;
		private String look = "";
		private Animation prepare, lunge, reach, toss, slam, stun, floatAnim, back, turn, swell, roar, kneel;
		private float t = 0, fadeT = -1;
		private boolean fading = false;
		private float raise = 6 / 16f;

		public PriestSprite(){
			super();
			texture(PriestAssets.BRUTE);
			big = new TextureFilm(texture, 32, 32);
			texture(PriestAssets.RITE);
			rite = new TextureFilm(texture, 20, 20);
			texture(SKAssets.GOBLIN_PRIEST);
			thin = new TextureFilm(texture, 20, 20);
			thinLook();
			play(back);
		}

		private Animation one( TextureFilm film, int fps, boolean loop, int... frames ){
			Animation a = new Animation(fps, loop);
			Object[] f = new Object[frames.length];
			for (int i = 0; i < frames.length; i++) f[i] = frames[i];
			a.frames(film, f);
			return a;
		}

		//thin priest: own sheet + the extra poses (same size)
		private void thinLook(){
			texture(SKAssets.GOBLIN_PRIEST);
			idle = one(thin, 2, true, 0, 0, 0, 1, 0, 0, 1, 1);
			run = one(thin, 12, true, 4, 5, 6, 7);
			attack = one(thin, 12, false, 2, 3, 0);
			zap = attack.clone();
			prepare = lunge = reach = toss = slam = attack;
			die = one(thin, 12, false, 8, 9, 10);
			back = turn = floatAnim = stun = kneel = swell = roar = null;
		}

		private void riteLook(){
			texture(PriestAssets.RITE);
			back = one(rite, 3, true, 0, 1);
			turn = one(rite, 1, true, 2);
			floatAnim = one(rite, 3, true, 3, 4);
			stun = one(rite, 4, true, 5, 6);
			kneel = one(rite, 3, false, 7, 8, 9);
			idle = floatAnim;
		}

		private void bruteLook(){
			texture(PriestAssets.BRUTE);
			idle = one(big, 2, true, 0, 0, 0, 1, 0, 0, 1, 1);
			run = one(big, 9, true, 4, 5, 6, 7);
			attack = one(big, 12, false, 2, 3, 0);
			zap = one(big, 8, false, COLS + 7, COLS + 8, COLS + 8, 0);
			prepare = one(big, 1, true, COLS);
			lunge = one(big, 8, false, COLS + 1, COLS + 1, 0);
			reach = one(big, 1, true, COLS + 2);
			toss = one(big, 8, false, COLS + 3, COLS + 3, 0);
			slam = one(big, 8, false, COLS + 4, COLS + 4, 0);
			stun = one(big, 4, true, COLS + 5, COLS + 6);
			floatAnim = one(big, 2, true, COLS + 9);
			roar = one(big, 1, true, COLS + 10);
			swell = one(big, 5, false, 2 * COLS, 2 * COLS + 1, 2 * COLS + 2, 2 * COLS + 3, 2 * COLS + 4, 2 * COLS + 5);
			die = one(big, 4, false, 8, 9, 10);
			kneel = die;
		}

		//what he should look like right now
		private String wanted(){
			if (!(ch instanceof GoblinPriest)) return look;
			GoblinPriest p = (GoblinPriest) ch;
			if (!p.isAlive()) return look;
			String s = p.brute && p.intro >= 2 ? "brute" : "thin";
			if (p.intro == 0) return "rite";
			if (p.intro == 1) return "turn";
			if (p.intro == 2) return p.brute ? "swell" : "rise";
			if (p.intro == 3) return p.brute ? "roar" : "raise";
			if (p.stunned > 0) return s + "_stun";
			if (p.starTimer > 0) return s + "_float";
			return s;
		}

		private void apply( String w ){
			look = w;
			switch (w){
				case "rite":        riteLook(); idle = back; break;
				case "turn":        riteLook(); idle = turn; break;
				case "rise":        riteLook(); idle = floatAnim; break;
				case "raise":       thinLook(); idle = one(thin, 1, true, 3); break;
				case "swell":       bruteLook(); idle = swell; break;
				case "roar":        bruteLook(); idle = roar; break;
				case "thin_stun":   riteLook(); idle = stun; break;
				case "thin_float":  riteLook(); idle = floatAnim; break;
				case "brute":       bruteLook(); break;
				case "brute_stun":  bruteLook(); idle = stun; break;
				case "brute_float": bruteLook(); idle = floatAnim; break;
				default:            thinLook(); break;
			}
			play(idle, true);
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			apply(wanted());
		}

		@Override
		public void update() {
			String w = wanted();
			if (!w.equals(look) && !isMoving && (curAnim == null || curAnim.looped || curAnim == idle || curAnim == run)){
				apply(w);
			}
			super.update();
			t += Game.elapsed;

			//floating: while calling the star, and un poquito all night long
			float lift = 0;
			if (ch instanceof GoblinPriest && ch.isAlive()){
				GoblinPriest p = (GoblinPriest) ch;
				if (p.starTimer > 0) lift = 5 + 1.5f * (float) Math.sin(t * 3);
				else if (p.act >= 3 && !p.brute && p.stunned == 0) lift = 2 + (float) Math.sin(t * 2);
			}
			float r = 6 / 16f + lift / 16f;
			if (r != raise){
				raise = r;
				perspectiveRaise = r;
				if (!isMoving && ch != null) place(ch.pos);
			}

			if (fadeT >= 0){
				fadeT += Game.elapsed;
				if (fadeT > 2.5f && !fading && parent != null){
					fading = true;
					parent.add(new AlphaTweener(this, 0, 2.5f){
						@Override
						protected void onComplete() {
							PriestSprite.this.killAndErase();
						}
					});
				}
			}
		}

		@Override
		public void die() {
			if (ch instanceof GoblinPriest && !((GoblinPriest) ch).brute){
				riteLook();
				die = kneel;
			}
			super.die();
		}

		public void cast(){
			play(zap);
		}

		private Animation swingAnim;

		//a copy, not the attack itself: when 'attack' ends CharSprite makes the mob attack for real,
		//and only the devices that see him would do it (co-op desync + a free hit, fun)
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

		public void reach(){
			play(reach);
		}

		public void toss(){
			play(toss);
		}

		public void slam(){
			play(slam);
		}

		@Override
		public void onComplete( Animation anim ) {
			if (anim == die){
				//stays on his knees a sec before fading out
				fadeT = 0;
				return;
			}
			super.onComplete(anim);
			if (anim == lunge || anim == toss || anim == slam || anim == zap || anim == swingAnim) play(idle);
		}

		@Override
		public int blood() {
			return 0xFF6A3A9A;
		}
	}
}
