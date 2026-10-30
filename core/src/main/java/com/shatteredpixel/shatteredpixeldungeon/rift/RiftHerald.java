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

package com.shatteredpixel.shatteredpixeldungeon.rift;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.PurpleParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.PrisonBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Rift Herald, joins tengu on 10. drops the eye (summon her back for 200 turns)
//  - her hits also take a part of the target's current health
//  - Eye of the Baron: every few turns the eye on her back opens, the next hit of a hero deals true damage
//  - Charge: announced for 2 turns, she leaps along a line, hurting and shoving aside whoever is on it
//  - Swipe: a cone of claws, announced one turn before, when she drops below 2/3 and 1/3 of her health
//her aoe hits tengu too, sometimes they go at it (BossRivalry)
public class RiftHerald extends RiftBoss {

	{
		spriteClass = Sprite.class;
		HP = HT = 150;
		defenseSkill = 12;
		EXP = 15;
		viewDistance = 10;

		HUNTING = new Hunting();
		state = HUNTING;

		immunities.add(Roots.class);
		immunities.add(Vertigo.class);
		immunities.add(Slow.class);
		immunities.add(Cripple.class);
	}

	public static final float CURRENT_HP = 0.20f;   //extra damage of her hits, part of current health
	public static final int CURRENT_HP_CAP = 8;
	public static final float EYE_DAMAGE = 0.12f;   //true damage of a hit on the open eye, part of her max health
	public static final int EYE_FIRST = 3;          //turns before the eye opens the first time
	public static final int EYE_CLOSED = 6;         //turns the eye stays closed after it is struck
	public static final int CHARGE_RANGE = 7;
	public static final int CHARGE_PREP = 2;
	public static final int CHARGE_COOLDOWN = 9;
	public static final int SWIPE_RANGE = 3;
	public static final float SWIPE_DEGREES = 100f;

	private int firstAttacker = -1;   //party index of the first hero who hurt her
	private int eyeCooldown = EYE_FIRST;
	private int chargeCooldown = 4;
	private int[] chargePath = null;  //announced charge
	private int chargeWait = 0;
	private int[] swipeCells = null;  //announced swipe
	private int swipeAim = -1;
	private int swipesDone = 0;
	private boolean dying = false;

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(3, 7);
	}

	@Override
	public int attackSkill( Char target ) {
		return 16;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 4);
	}

	public static int currentHpBonus( Char ch ){
		return Math.min(CURRENT_HP_CAP, Math.round(ch.HP * CURRENT_HP));
	}

	public int eyeDamage(){
		return Math.round(HT * EYE_DAMAGE);
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc(enemy, damage);
		return damage + currentHpBonus(enemy);
	}

	// *** timers ***

	@Override
	protected boolean act() {
		if (eyeCooldown > 0 && --eyeCooldown == 0){
			openEye();
		}
		if (chargeCooldown > 0) chargeCooldown--;

		//announced attacks go off whatever she is doing
		if (swipeCells != null){
			return doSwipe();
		}
		if (chargePath != null){
			if (--chargeWait > 0){
				spend(TICK);
				return true;
			}
			return doCharge();
		}
		return super.act();
	}

	private class Hunting extends Mob.Hunting {
		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			//while fighting the other boss she just brawls
			if (enemyInFOV && buff(BossRivalry.class) == null){
				if (swipesDone < swipesDue() && announceSwipe()) return true;
				if (chargeCooldown <= 0 && announceCharge()) return true;
			}
			return super.act(enemyInFOV, justAlerted);
		}
	}

	// *** Eye of the Baron ***

	private void openEye(){
		Buff.affect(this, BaronEye.class);
		if (sprite != null && Dungeon.level.heroFOV[pos]){
			sprite.showStatus(0xCC88FF, Messages.get(this, "eye_open"));
		}
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (isAlive() && dmg >= 0){
			Hero hero = null;
			if (src instanceof Hero) hero = (Hero) src;
			else if (Actor.current() instanceof Hero) hero = (Hero) Actor.current();
			if (hero != null && Party.isPartyHero(hero) && firstAttacker < 0){
				firstAttacker = ChampionHooks.indexOf(hero);
			}
			//only the blow of a hero strikes the eye
			BaronEye eye = buff(BaronEye.class);
			if (eye != null && src instanceof Hero){
				eye.detach();
				eyeCooldown = EYE_CLOSED;
				dmg += eyeDamage();
				if (sprite != null){
					sprite.showStatus(0xCC88FF, Messages.get(this, "eye_hit"));
					if (sprite.visible) sprite.emitter().burst(PurpleParticle.BURST, 12);
				}
				Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
			}
		}
		super.damage(dmg, src);
	}

	// *** Charge ***

	//the hero she charges at: the first one who hurt her, or else the closest one she sees
	private Char chargeTarget(){
		Hero first = firstAttacker >= 0 ? ChampionHooks.heroByIndex(firstAttacker) : null;
		if (canChargeAt(first)) return first;
		Hero best = null;
		int bestDist = Integer.MAX_VALUE;
		for (Hero h : Party.living()){
			if (!canChargeAt(h)) continue;
			int d = Dungeon.level.distance(pos, h.pos);
			if (d < bestDist){
				best = h;
				bestDist = d;
			}
		}
		return best;
	}

	private boolean canChargeAt( Hero h ){
		if (h == null || !h.isAlive() || h.coopDown || h.invisible > 0) return false;
		if (fieldOfView == null || !fieldOfView[h.pos]) return false;
		int d = Dungeon.level.distance(pos, h.pos);
		return d >= 2 && d <= CHARGE_RANGE;
	}

	//cells of a charge from a cell towards another, up to the range or the first obstacle
	public static ArrayList<Integer> chargeLine( int from, int to, int range ){
		ArrayList<Integer> line = new ArrayList<>();
		Ballistica b = new Ballistica(from, to, Ballistica.STOP_SOLID);
		for (int i = 1; i < b.path.size() && line.size() < range; i++){
			int c = b.path.get(i);
			if (Dungeon.level.solid[c] || !Dungeon.level.passable[c]) break;
			line.add(c);
		}
		return line;
	}

	private boolean announceCharge(){
		Char target = chargeTarget();
		if (target == null) return false;
		ArrayList<Integer> line = chargeLine(pos, target.pos, CHARGE_RANGE);
		if (!line.contains(target.pos)) return false;

		chargePath = new int[line.size()];
		for (int i = 0; i < line.size(); i++) chargePath[i] = line.get(i);
		chargeWait = CHARGE_PREP;
		chargeCooldown = CHARGE_COOLDOWN;

		boolean seen = Dungeon.level.heroFOV[pos];
		for (int c : chargePath){
			seen = seen || Dungeon.level.heroFOV[c];
			GameScene.targetedCell(c, CHARGE_PREP);
		}
		if (sprite instanceof Sprite) ((Sprite) sprite).prepare(target.pos);
		if (seen){
			GLog.w(Messages.get(this, "charge_warn"));
			interruptHeroes();
		}
		spend(TICK);
		return true;
	}

	private boolean doCharge(){
		final int[] path = chargePath;
		chargePath = null;
		chargeWait = 0;

		//she lands on the furthest free cell of the line
		int stop = path.length;
		for (int i = 0; i < path.length; i++){
			if (Dungeon.level.solid[path[i]] || !Dungeon.level.passable[path[i]]){
				stop = i;
				break;
			}
		}
		int land = -1;
		for (int i = stop - 1; i >= 0; i--){
			if (Actor.findChar(path[i]) == null){
				land = i;
				break;
			}
		}
		spend(TICK);
		if (land == -1){
			if (sprite != null) sprite.idle();
			return true;
		}

		final ArrayList<Char> hit = new ArrayList<>();
		for (int i = 0; i < land; i++){
			Char ch = Actor.findChar(path[i]);
			if (ch != null && ch != this) hit.add(ch);
		}
		final int from = pos;
		final int to = path[land];

		if (sprite != null){
			sprite.visible = Dungeon.level.heroFOV[from] || Dungeon.level.heroFOV[to];
			float dist = Dungeon.level.trueDistance(from, to);
			sprite.jump(from, to, 3f, 0.08f + 0.04f*dist, new Callback() {
				@Override
				public void call() {
					landCharge(from, to, hit);
					next();
				}
			});
			return false;
		}
		landCharge(from, to, hit);
		return true;
	}

	private void landCharge( int from, int to, ArrayList<Char> hit ){
		pos = to;
		Dungeon.level.occupyCell(this);
		if (sprite != null) sprite.idle();

		boolean seen = Dungeon.level.heroFOV[to];
		Strike strike = new Strike(this);
		for (Char ch : hit){
			if (!ch.isAlive() || !hurts(ch)) continue;
			seen = seen || Dungeon.level.heroFOV[ch.pos];
			ch.damage(2*damageRoll() + currentHpBonus(ch), strike);
			if (ch.sprite != null) ch.sprite.flash();
			checkHeroDeath(ch);
			if (ch.isAlive()) shoveAside(ch, from, to);
		}
		if (seen){
			Sample.INSTANCE.play(Assets.Sounds.ROCKS);
			PixelScene.shake(2, 0.4f);
			CellEmitter.get(to).burst(Speck.factory(Speck.DUST), 6);
		}
	}

	//pushes a character one cell to a side of a line
	public static void shoveAside( Char ch, int from, int to ){
		if (Char.hasProp(ch, Property.IMMOVABLE) || Char.hasProp(ch, Property.BOSS)) return;
		int w = Dungeon.level.width();
		int dx = Integer.signum(to % w - from % w);
		int dy = Integer.signum(to / w - from / w);
		if (dx == 0 && dy == 0) return;
		int[] sides = new int[]{ -dy + dx*w, dy - dx*w };
		for (int side : sides){
			int c = ch.pos + side;
			if (c < 0 || c >= Dungeon.level.length()) continue;
			if (Dungeon.level.solid[c] || !Dungeon.level.passable[c] || Actor.findChar(c) != null) continue;
			if (Char.hasProp(ch, Property.LARGE) && !Dungeon.level.openSpace[c]) continue;
			ChampionHooks.moveChar(ch, c);
			if (ch instanceof Hero) ((Hero) ch).interrupt();
			return;
		}
	}

	// *** Swipe ***

	private int swipesDue(){
		if (HP <= HT/3) return 2;
		if (HP <= HT*2/3) return 1;
		return 0;
	}

	private ConeAOE swipeCone( int aim ){
		Ballistica core = new Ballistica(pos, aim, Ballistica.STOP_SOLID);
		return new ConeAOE(core, SWIPE_RANGE, SWIPE_DEGREES, Ballistica.STOP_SOLID);
	}

	private boolean announceSwipe(){
		Hero target = null;
		int bestDist = Integer.MAX_VALUE;
		for (Hero h : Party.living()){
			if (fieldOfView == null || !fieldOfView[h.pos]) continue;
			int d = Dungeon.level.distance(pos, h.pos);
			if (d <= SWIPE_RANGE && d < bestDist){
				target = h;
				bestDist = d;
			}
		}
		if (target == null) return false;

		swipesDone++;
		swipeAim = target.pos;
		ConeAOE cone = swipeCone(swipeAim);
		swipeCells = new int[cone.cells.size()];
		int i = 0;
		boolean seen = Dungeon.level.heroFOV[pos];
		for (int c : cone.cells){
			swipeCells[i++] = c;
			seen = seen || Dungeon.level.heroFOV[c];
			GameScene.targetedCell(c, 1);
		}
		if (sprite != null){
			sprite.turnTo(pos, swipeAim);
			if (seen) sprite.showStatus(CharSprite.WARNING, Messages.get(this, "swipe_status"));
		}
		if (seen){
			GLog.w(Messages.get(this, "swipe_warn"));
			interruptHeroes();
		}
		spend(TICK);
		return true;
	}

	private boolean doSwipe(){
		final int[] cells = swipeCells;
		final int aim = swipeAim;
		swipeCells = null;
		swipeAim = -1;
		spend(TICK);
		Callback effect = new Callback() {
			@Override
			public void call() {
				swipe(cells);
			}
		};
		if (sprite != null && (sprite.visible || Dungeon.level.heroFOV[pos])){
			sprite.attack(aim, new Callback() {
				@Override
				public void call() {
					effect.call();
					sprite.idle();
					next();
				}
			});
			return false;
		}
		effect.call();
		return true;
	}

	private void swipe( int[] cells ){
		boolean seen = false;
		for (int c : cells){
			if (Dungeon.level.heroFOV[c]){
				seen = true;
				CellEmitter.get(c).burst(PurpleParticle.BURST, 3);
			}
		}
		Strike strike = new Strike(this);
		for (Char ch : Actor.chars()){
			if (ch == this || !ch.isAlive() || !hurts(ch)) continue;
			boolean inside = false;
			for (int c : cells) if (c == ch.pos) inside = true;
			if (!inside) continue;
			ch.damage(Math.round(1.5f*damageRoll()) + currentHpBonus(ch), strike);
			if (ch.sprite != null) ch.sprite.flash();
			checkHeroDeath(ch);
		}
		if (seen) Sample.INSTANCE.play(Assets.Sounds.HIT_SLASH);
	}

	// *** helpers ***

	//her areas hurt the party and its allies, and also the other boss of the floor
	private boolean hurts( Char ch ){
		//(not neutral things such as DM-300's pylons)
		return ch.alignment == Alignment.ALLY
				|| (ch != this && ch.alignment == alignment && Char.hasProp(ch, Property.BOSS));
	}

	private void checkHeroDeath( Char ch ){
		if (ch instanceof Hero && !ch.isAlive()){
			Dungeon.fail(this);
			GLog.n(Messages.capitalize(Messages.get(Char.class, "kill", name())));
		}
	}

	private static void interruptHeroes(){
		for (Hero h : Party.living()) h.interrupt();
	}

	//damage from her areas: not a plain hit, but it is still hers (for BossRivalry)
	public static class Strike implements BossRivalry.Source {
		private final Char boss;
		public Strike( Char boss ){
			this.boss = boss;
		}
		@Override
		public Char rivalrySource() {
			return boss;
		}
	}

	// *** loot and leaving ***

	@Override
	protected Item riftLoot() {
		return new HeraldEye();
	}

	@Override
	public void die( Object cause ) {
		dying = true;
		super.die(cause);
	}

	@Override
	public void destroy() {
		//when Tengu falls his prison removes every mob outside his cell: the Herald goes back
		//to the Rift, but the party still gets her eye
		boolean leaving = !dying && HP > 0 && Dungeon.level instanceof PrisonBossLevel
				&& ((PrisonBossLevel) Dungeon.level).state() == PrisonBossLevel.State.FIGHT_ARENA;
		super.destroy();
		if (leaving){
			GLog.w(Messages.get(this, "leaves"));
			//one for the whole party, like any boss reward
			Heap h = Dungeon.level.drop(riftLoot(), Dungeon.hero.pos);
			if (h.sprite != null) h.sprite.drop(Dungeon.hero.pos);
		}
	}

	@Override
	public String description() {
		return Messages.get(this, "desc", Math.round(CURRENT_HP*100), CURRENT_HP_CAP, eyeDamage());
	}

	// *** saving ***

	private static final String FIRST = "first_attacker";
	private static final String EYE_CD = "eye_cd";
	private static final String CHARGE_CD = "charge_cd";
	private static final String CHARGE_PATH = "charge_path";
	private static final String CHARGE_WAIT = "charge_wait";
	private static final String SWIPE_CELLS = "swipe_cells";
	private static final String SWIPE_AIM = "swipe_aim";
	private static final String SWIPES = "swipes";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(FIRST, firstAttacker);
		bundle.put(EYE_CD, eyeCooldown);
		bundle.put(CHARGE_CD, chargeCooldown);
		if (chargePath != null) bundle.put(CHARGE_PATH, chargePath);
		bundle.put(CHARGE_WAIT, chargeWait);
		if (swipeCells != null) bundle.put(SWIPE_CELLS, swipeCells);
		bundle.put(SWIPE_AIM, swipeAim);
		bundle.put(SWIPES, swipesDone);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		firstAttacker = bundle.getInt(FIRST);
		eyeCooldown = bundle.getInt(EYE_CD);
		chargeCooldown = bundle.getInt(CHARGE_CD);
		chargePath = bundle.contains(CHARGE_PATH) ? bundle.getIntArray(CHARGE_PATH) : null;
		chargeWait = bundle.getInt(CHARGE_WAIT);
		swipeCells = bundle.contains(SWIPE_CELLS) ? bundle.getIntArray(SWIPE_CELLS) : null;
		swipeAim = bundle.getInt(SWIPE_AIM);
		swipesDone = bundle.getInt(SWIPES);
	}

	//the open eye on her back: tinted aura on the sprite
	public static class BaronEye extends Buff {

		{
			type = buffType.NEUTRAL;
		}

		@Override
		public void fx( boolean on ) {
			if (on) target.sprite.aura(0xBB55FF, 6);
			else    target.sprite.clearAura();
		}

		@Override
		public int icon() {
			return BuffIndicator.FORESIGHT;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(0.75f, 0.35f, 1f);
		}

		@Override
		public String desc() {
			int dmg = target instanceof RiftHerald ? ((RiftHerald) target).eyeDamage() : 0;
			return Messages.get(this, "desc", dmg, EYE_CLOSED);
		}
	}

	public static class Sprite extends MobSprite {

		private Animation prep;
		private Animation leap;

		public Sprite() {
			super();
			texture(sheet());
			TextureFilm frames = new TextureFilm(texture, 32, 32);

			idle = new Animation(3, true);
			idle.frames(frames, 0, 0, 1, 0);

			run = new Animation(10, true);
			run.frames(frames, 2, 3, 4, 5);

			attack = new Animation(12, false);
			attack.frames(frames, 6, 7, 8, 0);

			zap = new Animation(10, false);
			zap.frames(frames, 12, 13, 14, 0);

			prep = new Animation(5, true);
			prep.frames(frames, 12, 13);

			leap = new Animation(1, true);
			leap.frames(frames, 14);

			die = new Animation(10, false);
			die.frames(frames, 9, 10, 11);

			play(idle);
		}

		protected String sheet(){
			return Assets.Sprites.RIFT_HERALD;
		}

		public void prepare( int cell ){
			turnTo(ch.pos, cell);
			play(prep);
		}

		@Override
		public void jump( int from, int to, float height, float duration, Callback callback ) {
			super.jump(from, to, height, duration, callback);
			play(leap);
		}

		@Override
		public void onComplete( Animation anim ) {
			if (anim == zap) idle();
			super.onComplete(anim);
		}

		@Override
		public int blood() {
			return 0xFF9944DD;
		}
	}

	// *** testing ***

	//Desktop tests only, off unless the property is set. -Dspd.rift.test=1 brings the rift boss of the
	//starting floor next to the heroes at once; -Dspd.rift.testloot=N also gives every hero N eyes and
	//N roses. Host only, while the first floor is built, before it is shared (needs a call from
	//CoopAuto.carveTestChasm, which is only added while testing).
	public static void testSpawn(){
		Level level = Dungeon.level;
		if (level == null || Dungeon.hero == null) return;
		if ("1".equals(System.getProperty("spd.rift.test")) && !level.riftSpawned){
			RiftBoss boss = RiftBosses.create(Dungeon.depth);
			if (boss != null){
				int cell = RiftBosses.findCell(level, Dungeon.hero.pos, boss);
				if (cell != -1){
					boss.pos = cell;
					boss.state = boss.HUNTING;
					level.mobs.add(boss);
					level.riftSpawned = true;
				}
			}
		}
		int loot = Integer.getInteger("spd.rift.testloot", 0);
		if (loot > 0){
			for (final Hero h : Party.active() ? Party.heroes() : java.util.Collections.singletonList(Dungeon.hero)){
				Party.withHero(h, new Runnable() {
					@Override
					public void run() {
						new HeraldEye().quantity(loot).collect(h.belongings.backpack);
						new BloodRose().quantity(loot).collect(h.belongings.backpack);
					}
				});
			}
		}
	}
}
