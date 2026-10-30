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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.boss;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Amok;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corruption;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Dread;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicalSleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Sleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopBalance;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.Beam;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BlastParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SmokeParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.PlateArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TalismanOfForesight;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.AshWalker;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKBoss;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.Continent;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ContinentAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.SacredEssence;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Halo;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

/*
 The walking pyre, guardian of the ash plains. A giant of ash and embers with a heart of sacred
 essence in its chest. It burns itself down as it fights (heat), and when the fire runs low it walks
 to a lit brazier and drinks it: heals and comes back blazing. Knock the brazier over first and it
 goes dark for a few turns, wide open.
 1 embers 100-75: slam, stomp (stomped ground melts into black lava), ember spit, sweep
 2 ash storm 75-50: + storms that leave everyone half blind (it still glows), ash walkers, charge
 3 black lava 50-25: + quake rings, embers falling on the heroes
 4 the heart 25-0: it never gutters again, but every lit brazier feeds it from afar
*/
public class AshPyre extends SKBoss {

	public static final int BASE_HP = 1600;

	//what it's doing
	public static final int FIGHT = 0, GUTTER = 1, CHANNEL = 2, DARK = 3, FLARE = 4, DYING = 5;

	private static final int STOMP = 0, SWEEP = 1, CHARGE = 2, STORM = 3, QUAKE = 4, SPIT = 5;
	public static final int RAIN = 10;

	{
		spriteClass = PyreSprite.class;
		HP = HT = BASE_HP;
		defenseSkill = 26;
		EXP = 150;
		state = PASSIVE;
		properties.add(Property.LARGE);
		properties.add(Property.FIERY);
		properties.add(Property.INORGANIC);
		immunities.add(Paralysis.class);
		immunities.add(Frost.class);
		immunities.add(Chill.class);
		immunities.add(Sleep.class);
		immunities.add(MagicalSleep.class);
		immunities.add(Terror.class);
		immunities.add(Dread.class);
		immunities.add(Amok.class);
		immunities.add(Charm.class);
		immunities.add(Corruption.class);
		immunities.add(Vertigo.class);
	}

	public int intro = 0;   //0 asleep in the ashes, 1-3 getting up, 4 fighting
	public int phase = 1;
	public int heat = 100;
	public int mode = FIGHT;
	public int modeTurns = 0;
	public int blazing = 0;
	public int stunned = 0;
	private int fuel = -1;  //brazier it's walking to / drinking
	private int stormTimer = 12;
	private int rainTimer = 4;
	private boolean finished = false;

	@Override protected int scoreIndex() { return 4; }
	@Override protected int score() { return 6000; }

	public static AshPyre find(){
		return Dungeon.level == null ? null : AshBossLevel.pyre(Dungeon.level);
	}

	private AshBossLevel arena(){
		return Dungeon.level instanceof AshBossLevel ? (AshBossLevel) Dungeon.level : null;
	}

	public boolean fighting(){
		return intro >= 4 && mode != DYING && isAlive();
	}

	public boolean dying(){
		return mode == DYING;
	}

	public PyreBrazier fuel(){
		Actor a = fuel == -1 ? null : Actor.findById(fuel);
		return a instanceof PyreBrazier && ((PyreBrazier) a).lit ? (PyreBrazier) a : null;
	}

	private static void later( final Runnable r ){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				r.run();
			}
		});
	}

	//-Dspd.test.pyreweak=1: a tenth of the hp
	@Override
	protected void onAdd() {
		boolean first = firstAdded;
		super.onAdd();
		if (first && DevFlags.pyreWeak()){
			HT = Math.max(60, HT / 10);
			HP = HT;
		}
	}

	// *** stats ***

	//a lone hero gets hit a bit softer, like with the other big bosses
	private static float soloFactor(){
		return Party.living().size() <= 1 ? 0.75f : 1f;
	}

	@Override
	public int damageRoll() {
		float dmg = Random.NormalIntRange(42, 64) * (Party.living().size() <= 1 ? 0.85f : 1f);
		return Math.round(blazing > 0 ? dmg * 1.25f : dmg);
	}

	@Override
	public int attackSkill( Char target ) {
		return 50;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, phase >= 4 ? 8 : 18);
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc(enemy, damage);
		if (enemy.isAlive() && Random.Int(3) == 0) Buff.affect(enemy, Burning.class).reignite(enemy, 4f);
		return damage;
	}

	//dragging itself to a brazier, it's slow
	@Override
	public float speed() {
		return mode == GUTTER ? super.speed() * 0.7f : super.speed();
	}

	@Override
	protected int specialCooldown() {
		return phase == 1 ? Random.IntRange(3, 4) : Random.IntRange(2, 3);
	}

	// *** waking up ***

	private Hero heroOnPlain(){
		AshBossLevel L = arena();
		Hero best = null;
		for (Hero h : Party.living()){
			if (L != null ? !L.onPlain(h.pos) : Dungeon.level.distance(h.pos, pos) > 8) continue;
			if (best == null || Dungeon.level.distance(h.pos, pos) < Dungeon.level.distance(best.pos, pos)) best = h;
		}
		return best;
	}

	@Override
	public void notice() {
		//it gets up on its own (introAct)
	}

	private boolean introAct(){
		switch (intro){
			case 0: default: {
				final Hero h = heroOnPlain();
				if (h != null){
					intro = 1;
					BossHealthBar.assignBoss(this);
					Party.withHero(h, new Runnable() {
						@Override
						public void run() {
							Dungeon.level.seal();
						}
					});
					CoopLog.line("pyre wakes t=" + (int) Actor.now() + " hp=" + HP + "/" + HT + " heroes=" + Party.living().size());
					testKit();
					GLog.w(Messages.get(this, "wake"));
					PyreHud.banner(Messages.get(AshPyre.class, "banner_wake"));
					Sample.INSTANCE.play(Assets.Sounds.BURNING, 1f, 0.6f);
					if (sprite != null) CellEmitter.get(pos).burst(Speck.factory(Speck.DUST), 10);
				}
				break;
			}
			case 1:
				intro = 2;
				Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 0.5f);
				later(new Runnable() { public void run() { PixelScene.shake(2, 0.8f); } });
				break;
			case 2:
				intro = 3;
				Sample.INSTANCE.play(Assets.Sounds.BURNING, 1f, 0.9f);
				if (sprite != null){
					CellEmitter.get(pos).burst(FlameParticle.FACTORY, 20);
					Hero h = heroOnPlain();
					if (h != null) sprite.turnTo(pos, h.pos);
				}
				for (PyreBrazier b : braziers()){
					if (sprite != null && Dungeon.level.heroFOV[b.pos]) CellEmitter.get(b.pos).burst(FlameParticle.FACTORY, 10);
				}
				break;
			case 3:
				intro = 4;
				state = HUNTING;
				enraged = true;
				cooldown = 2;
				heat = 100;
				Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 1f, 0.7f);
				later(new Runnable() { public void run() {
					PixelScene.shake(4, 0.7f);
					GameScene.flash(0x60FF8A30);
				} });
				PyreHud.title(Messages.get(this, "title_name"), Messages.get(this, "title_sub"));
				yell(Messages.get(this, "notice"));
				GLog.w(Messages.get(this, "hint_1"));
				AshBossLevel.music(Assets.Music.CAVES_BOSS);
				testStart();
				break;
		}
		spend(TICK);
		return true;
	}

	//-Dspd.test.pyrekit=N: late game gear for the test heroes (the start kit +10 is tier 1)
	private static void testKit(){
		int plus = DevFlags.pyreKit();
		if (plus <= 0) return;
		for (Hero h : Party.living()){
			if (!(h.belongings.weapon instanceof MeleeWeapon) || ((MeleeWeapon) h.belongings.weapon).tier < 5){
				MeleeWeapon w = new Greatsword();
				w.identify();
				w.upgrade(plus);
				w.cursed = false;
				h.belongings.weapon = w;
			}
			if (h.belongings.armor == null || h.belongings.armor.tier < 5){
				Armor a = new PlateArmor();
				a.identify();
				a.upgrade(plus);
				a.cursed = false;
				h.belongings.armor = a;
				a.activate(h);
			}
			//and the strength for whatever they hold (the clockmaker's pieces ask for a lot)
			int str = 19;
			if (h.belongings.weapon instanceof MeleeWeapon) str = Math.max(str, ((MeleeWeapon) h.belongings.weapon).STRReq());
			if (h.belongings.armor != null) str = Math.max(str, h.belongings.armor.STRReq());
			h.STR = Math.max(h.STR, str);
			h.updateHT(true);
			h.HP = h.HT;
			CoopLog.line("pyre test kit: hero " + Party.indexOf(h) + " lvl=" + h.lvl + " HT=" + h.HT + " +" + plus);
		}
	}

	//-Dspd.test.pyrephase=N: the fight starts in phase N
	private void testStart(){
		int start = DevFlags.pyrePhase();
		if (start >= 2){ HP = HT * 3 / 4; flare(2); }
		if (start >= 3){ HP = HT / 2; flare(3); }
		if (start >= 4){ HP = HT / 4; flare(4); }
	}

	// *** turns ***

	@Override
	public boolean isInvulnerable( Class effect ) {
		return intro < 4 || mode == FLARE || mode == DYING || super.isInvulnerable(effect);
	}

	@Override
	protected boolean act() {
		if (!isAlive()) return true;
		if (intro < 4) return introAct();
		if (mode == DYING) return dyingAct();
		if (blazing > 0) blazing--;
		if (mode != FLARE) rainTick();
		if (((int) Actor.now()) % 10 == 0) CoopLog.line("pyre t=" + (int) Actor.now() + " hp=" + HP + "/" + HT
				+ " phase=" + phase + " mode=" + mode + " heat=" + heat + " lit=" + litLeft());
		switch (mode){
			case FLARE:   return flareAct();
			case DARK:    return darkAct();
			case CHANNEL: return channelAct();
			case GUTTER:  return gutterAct();
		}
		if (stunned > 0){
			stunned--;
			spend(TICK);
			return true;
		}
		heat -= phase >= 3 ? 4 : 3;
		if (phase >= 4){
			heat = 100;
			feed();
		}
		if (phase >= 2 && stormTimer > 0) stormTimer--;
		if (heat <= 0 && pendingKind == -1){
			startGutter();
			spend(TICK);
			return true;
		}
		//a marked attack always lands, even if it lost sight of everyone meanwhile
		if (pendingKind != -1 && state != HUNTING) state = HUNTING;
		return super.act();
	}

	//it can wade through black lava, but only when there's no way around: standing in the river where
	//nobody can reach it would be a cheap trick
	@Override
	protected boolean getCloser( int target ) {
		if (rooted || stunned > 0 || target == pos) return false;
		AshBossLevel L = arena();
		if (L == null) return super.getCloser(target);
		boolean[] wade = L.pyrePassable();
		int step;
		if (Dungeon.level.adjacent(pos, target)){
			step = wade[target] && Actor.findChar(target) == null && Dungeon.level.openSpace[target] ? target : -1;
		} else {
			step = Dungeon.findStep(this, target, Dungeon.level.passable, fieldOfView, true);
			if (step == -1) step = Dungeon.findStep(this, target, wade, fieldOfView, true);
		}
		if (step == -1 || Actor.findChar(step) != null) return false;
		move(step);
		return true;
	}

	@Override
	protected boolean getFurther( int target ) {
		return false;
	}

	// *** fire: guttering, drinking a brazier, going dark ***

	private ArrayList<PyreBrazier> braziers(){
		AshBossLevel L = arena();
		return L == null ? new ArrayList<PyreBrazier>() : L.braziers();
	}

	//closest lit brazier it can walk to
	private PyreBrazier pickBrazier(){
		AshBossLevel L = arena();
		if (L == null) return null;
		PathFinder.buildDistanceMap(pos, L.pyrePassable());
		PyreBrazier best = null;
		int bestD = Integer.MAX_VALUE;
		for (PyreBrazier b : L.braziers()){
			if (!b.lit) continue;
			int d = PathFinder.distance[b.pos];
			if (d < bestD){
				bestD = d;
				best = b;
			}
		}
		return best;
	}

	private void clearPending(){
		pendingKind = -1;
		pendingCells = new int[0];
		pendingAim = -1;
	}

	private void startGutter(){
		clearPending();
		PyreBrazier b = pickBrazier();
		if (b == null){
			goDark();
			return;
		}
		mode = GUTTER;
		modeTurns = 0;
		fuel = b.id();
		yell(Messages.get(this, "gutter"));
		PyreHud.banner(Messages.get(AshPyre.class, "banner_gutter"));
		GLog.w(Messages.get(this, "hint_gutter"));
		Sample.INSTANCE.play(Assets.Sounds.BURNING, 1f, 0.5f);
		CoopLog.line("pyre gutters t=" + (int) Actor.now() + " hp=" + HP + "/" + HT + " -> brazier " + b.pos);
	}

	private boolean gutterAct(){
		PyreBrazier b = fuel();
		if (b == null){
			//they knocked its brazier over on the way: no fire left to reach
			goDark();
			spend(TICK);
			return true;
		}
		if (Dungeon.level.adjacent(pos, b.pos)){
			startChannel(b);
			spend(TICK);
			return true;
		}
		if (++modeTurns > 24){
			goDark();
			spend(TICK);
			return true;
		}
		Dungeon.level.updateFieldOfView(this, fieldOfView);
		int old = pos;
		if (getCloser(b.pos)){
			spend(1 / speed());
			return moveSprite(old, pos);
		}
		//something's in the way: swat it
		for (int n : PathFinder.NEIGHBOURS8){
			Char ch = Actor.findChar(pos + n);
			if (ch != null && SKCombat.foes(this, ch)){
				enemy = ch;
				return doAttack(ch);
			}
		}
		spend(TICK);
		return true;
	}

	private void startChannel( PyreBrazier b ){
		mode = CHANNEL;
		modeTurns = 3;
		fuel = b.id();
		if (sprite != null) sprite.turnTo(pos, b.pos);
		GLog.n(Messages.get(this, "drinking"));
		tether(b);
	}

	private boolean channelAct(){
		PyreBrazier b = fuel();
		if (b == null){
			//knocked over under its nose
			goDark();
			spend(TICK);
			return true;
		}
		tether(b);
		if (--modeTurns > 0){
			spend(TICK);
			return true;
		}
		b.drained();
		int heal = Math.round(HT * 0.06f);
		HP = Math.min(HT, HP + heal);
		heat = 100;
		blazing = 10;
		mode = FIGHT;
		fuel = -1;
		cooldown = 1;
		if (sprite != null){
			sprite.showStatus(CharSprite.POSITIVE, Integer.toString(heal));
			CellEmitter.get(pos).burst(FlameParticle.FACTORY, 24);
		}
		Sample.INSTANCE.play(Assets.Sounds.BURNING, 1f, 1.3f);
		yell(Messages.get(this, "rekindled"));
		PyreHud.banner(Messages.get(AshPyre.class, "banner_rekindled"));
		CoopLog.line("pyre rekindles t=" + (int) Actor.now() + " hp=" + HP + "/" + HT + " lit=" + litLeft());
		spend(TICK);
		return true;
	}

	private int litLeft(){
		AshBossLevel L = arena();
		return L == null ? 0 : L.litBraziers();
	}

	private void goDark(){
		clearPending();
		mode = DARK;
		modeTurns = Party.living().size() <= 1 ? 5 : 4;
		heat = 0;
		fuel = -1;
		Sample.INSTANCE.play(Assets.Sounds.DEGRADE, 1f, 0.6f);
		if (sprite != null) CellEmitter.get(pos).burst(SmokeParticle.FACTORY, 16);
		PyreHud.banner(Messages.get(AshPyre.class, "banner_dark"));
		GLog.p(Messages.get(this, "dark"));
		CoopLog.line("pyre goes dark t=" + (int) Actor.now() + " hp=" + HP + "/" + HT);
	}

	private boolean darkAct(){
		if (--modeTurns <= 0){
			mode = FIGHT;
			heat = 60;
			cooldown = 1;
			yell(Messages.get(this, "relit"));
			if (sprite != null) CellEmitter.get(pos).burst(FlameParticle.FACTORY, 16);
		}
		spend(TICK);
		return true;
	}

	//the heart: every lit brazier pours into it, though it never gets far past the edge of the end
	private void feed(){
		int fed = 0;
		for (PyreBrazier b : braziers()){
			if (b.lit && Dungeon.level.distance(pos, b.pos) <= 10){
				fed++;
				tether(b);
			}
		}
		int cap = HT * 3 / 10;
		if (fed > 0 && HP < cap){
			int heal = Math.max(1, Math.min(cap - HP, Math.round(HT * 0.002f * fed)));
			HP += heal;
			if (sprite != null) sprite.showStatus(CharSprite.POSITIVE, Integer.toString(heal));
		}
	}

	private void tether( final PyreBrazier b ){
		if (sprite == null || b.sprite == null) return;
		if (!Dungeon.level.heroFOV[pos] && !Dungeon.level.heroFOV[b.pos]) return;
		final CharSprite from = b.sprite, to = sprite;
		later(new Runnable() { public void run() {
			if (from.parent == null || to.parent == null) return;
			PointF a = from.center();
			PointF c = to.center();
			GameScene.effect(new Beam.SunRay(new PointF(a.x, a.y - 4), new PointF(c.x, c.y - 2)));
		} });
	}

	// *** phases ***

	@Override
	public void damage( int dmg, Object src ) {
		if (!isAlive()) return;
		if (intro == 0) return;
		if (intro < 4 || mode == FLARE || mode == DYING){
			super.damage(dmg, src);
			return;
		}
		float mult = 1f;
		if (mode == GUTTER || mode == CHANNEL) mult = 1.25f;
		else if (mode == DARK) mult = 1.75f;
		//the heart is out in the open now
		if (phase >= 4) mult *= 1.25f;
		if (blazing > 0) mult *= 0.85f;
		dmg = Math.round(dmg * mult);
		if (dmg >= HP) dmg = HP - 1;
		if (dmg > 0) super.damage(dmg, src);
		if (!isAlive()) return;
		if (HP <= 1){
			startDying();
			return;
		}
		if (phase == 1 && HP <= HT * 3 / 4) flare(2);
		else if (phase == 2 && HP <= HT / 2) flare(3);
		else if (phase == 3 && HP <= HT / 4) flare(4);
	}

	private void flare( int to ){
		clearPending();
		phase = to;
		mode = FLARE;
		modeTurns = 2;
		heat = Math.min(100, Math.max(heat, 0) + 40);
		fuel = -1;
		stunned = 0;
		if (to == 2) stormTimer = 1;
		if (to == 3) rainTimer = 2;
		yell(Messages.get(this, "phase_" + to));
		PyreHud.banner(Messages.get(AshPyre.class, "banner_" + to));
		GLog.w(Messages.get(this, "hint_" + to));
		for (PyreBrazier b : braziers()) b.light(true);
		Sample.INSTANCE.play(Assets.Sounds.BURNING, 1f, 0.7f);
		Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 1f, 0.6f);
		later(new Runnable() { public void run() {
			PixelScene.shake(5, 1f);
			GameScene.flash(0x70FF6020);
		} });
		if (sprite != null) CellEmitter.get(pos).burst(FlameParticle.FACTORY, 30);
		if (to >= 4) AshBossLevel.music(Assets.Music.CAVES_BOSS_FINALE);
		CoopLog.line("pyre phase " + to + " t=" + (int) Actor.now() + " hp=" + HP + "/" + HT);
	}

	private boolean flareAct(){
		if (--modeTurns <= 0){
			mode = FIGHT;
			cooldown = 1;
			if (phase == 2 || phase == 3) walkers(1 + Party.living().size() / 3);
		}
		spend(TICK);
		return true;
	}

	// *** death ***

	private void startDying(){
		clearPending();
		mode = DYING;
		modeTurns = 4;
		HP = 1;
		AshBossLevel L = arena();
		if (L != null){
			L.strikes.clear();
			L.storm = 0;
		}
		for (Hero h : Party.living()){
			Buff.detach(h, AshBlind.class);
			Buff.detach(h, PyreGlow.class);
		}
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (m instanceof AshWalker && m.isAlive()) m.die(null);
		}
		yell(Messages.get(this, "dying"));
		PyreHud.banner(Messages.get(AshPyre.class, "banner_dying"));
		Sample.INSTANCE.play(Assets.Sounds.DEGRADE, 1f, 0.5f);
		later(new Runnable() { public void run() { PixelScene.shake(3, 2f); } });
		CoopLog.line("pyre falls t=" + (int) Actor.now());
	}

	private boolean dyingAct(){
		modeTurns--;
		if (sprite != null){
			CellEmitter.get(pos).burst(SmokeParticle.FACTORY, 10);
			if (modeTurns % 2 == 0) CellEmitter.center(pos).burst(Speck.factory(Speck.DUST), 8);
		}
		spend(TICK);
		if (modeTurns <= 0){
			finished = true;
			die(null);
		}
		return true;
	}

	@Override
	protected boolean dropsRewards() {
		return false;
	}

	@Override
	protected ArrayList<Item> loot() {
		return new ArrayList<>();
	}

	@Override
	public void die( Object cause ) {
		if (!finished){
			//nothing kills it outright, it goes out the slow way
			if (mode != DYING){
				HP = 1;
				startDying();
			} else {
				HP = Math.max(1, HP);
			}
			return;
		}
		int at = pos;
		super.die(cause);
		later(new Runnable() { public void run() {
			PixelScene.shake(4, 1.5f);
			GameScene.flash(0x80FFF0C0);
		} });
		PyreHud.banner(Messages.get(AshPyre.class, "banner_dead"));
		Dungeon.level.drop(new SacredEssence(Continent.GUARDIAN_ASH), at).sprite.drop();
		for (Hero h : partyHeroes()){
			give(h, new ScrollOfUpgrade());
			give(h, new PotionOfHealing());
		}
		GLog.p(Messages.get(this, "loot"));
		CoopLog.line("pyre dead t=" + (int) Actor.now());
	}

	private static ArrayList<Hero> partyHeroes(){
		ArrayList<Hero> list = new ArrayList<>();
		if (Party.active()) list.addAll(Party.heroes());
		else if (Dungeon.hero != null) list.add(Dungeon.hero);
		return list;
	}

	//backpack, or at their feet if it's full
	private static void give( final Hero h, final Item item ){
		item.identify();
		Party.withHero(h, new Runnable() {
			@Override
			public void run() {
				if (!item.collect(h.belongings.backpack)){
					Dungeon.level.drop(item, h.pos).sprite.drop();
				}
			}
		});
	}

	// *** attacks ***

	@Override
	protected boolean special( Char enemy ) {
		if (stunned > 0) return false;
		AshBossLevel L = arena();
		if (L == null) return false;
		if (phase >= 2 && stormTimer <= 0 && L.storm == 0){
			stormTimer = Math.max(12, 24 - 2 * phase) + (Party.living().size() <= 1 ? 4 : 0);
			heat -= 10;
			announce(STORM, new ArrayList<Integer>(), enemy.pos);
			pyreSprite(PyreSprite.STORM_CAST);
			yell(Messages.get(this, "storm"));
			return true;
		}
		ArrayList<Integer> kinds = new ArrayList<>();
		kinds.add(STOMP);
		kinds.add(SPIT);
		if (adjacentFoes() > 0) kinds.add(SWEEP);
		if (phase >= 2 && Dungeon.level.distance(pos, enemy.pos) >= 3) kinds.add(CHARGE);
		if (phase >= 3) kinds.add(QUAKE);
		int kind = kinds.get(pickSpell(kinds.size()));
		switch (kind){
			case STOMP: default: {
				ArrayList<Integer> cells = stompLine(enemy.pos, phase >= 3 ? 7 : 6);
				if (cells.isEmpty()) return false;
				heat -= 6;
				announce(STOMP, cells, enemy.pos);
				pyreSprite(PyreSprite.STOMP_CAST);
				GLog.w(Messages.get(this, "stomp"));
				return true;
			}
			case SPIT: {
				heat -= 4;
				if (sprite != null) sprite.turnTo(pos, enemy.pos);
				pyreSprite(PyreSprite.SPIT_CAST);
				arc(4 + phase, 70, enemy.pos, 1.5f, ItemSpriteSheet.SHOT_METEOR, 16, 26);
				Sample.INSTANCE.play(Assets.Sounds.BURNING, 1f, 1.4f);
				spend(TICK);
				return true;
			}
			case SWEEP: {
				ArrayList<Integer> cells = new ArrayList<>();
				for (int c : SKCombat.area(pos, phase >= 3 ? 2 : 1)){
					if (c != pos) cells.add(c);
				}
				heat -= 4;
				announce(SWEEP, cells, enemy.pos);
				pyreSprite(PyreSprite.STOMP_CAST);
				GLog.w(Messages.get(this, "sweep"));
				return true;
			}
			case CHARGE: {
				ArrayList<Integer> cells = chargeLine(enemy.pos);
				if (cells.size() < 2) return false;
				heat -= 6;
				announce(CHARGE, cells, enemy.pos);
				yell(Messages.get(this, "charge"));
				return true;
			}
			case QUAKE: {
				ArrayList<Integer> cells = new ArrayList<>();
				for (int c : SKCombat.openArea(pos, 3)){
					int d = Dungeon.level.distance(c, pos);
					if (d >= 2 && L.onPlain(c)) cells.add(c);
				}
				if (cells.isEmpty()) return false;
				heat -= 8;
				announce(QUAKE, cells, enemy.pos);
				pyreSprite(PyreSprite.STOMP_CAST);
				GLog.w(Messages.get(this, "quake"));
				return true;
			}
		}
	}

	private int adjacentFoes(){
		int n = 0;
		for (int k : PathFinder.NEIGHBOURS8){
			if (SKCombat.foes(this, Actor.findChar(pos + k))) n++;
		}
		return n;
	}

	//three cells wide, straight at the target
	private ArrayList<Integer> stompLine( int target, int length ){
		ArrayList<Integer> cells = new ArrayList<>();
		int w = Dungeon.level.width();
		int dx = Integer.signum(target % w - pos % w), dy = Integer.signum(target / w - pos / w);
		if (dx == 0 && dy == 0) return cells;
		//sideways step, perpendicular to the line
		int sx = -dy, sy = dx;
		for (int c : line(target, length)){
			for (int k = -1; k <= 1; k++){
				int cc = c + k * (sx + sy * w);
				if (cc < 0 || cc >= Dungeon.level.length() || Dungeon.level.solid[cc] || cc == pos) continue;
				if (!cells.contains(cc)) cells.add(cc);
			}
		}
		return cells;
	}

	private ArrayList<Integer> chargeLine( int target ){
		ArrayList<Integer> cells = new ArrayList<>();
		AshBossLevel L = arena();
		Ballistica b = new Ballistica(pos, target, Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID);
		for (int c : b.subPath(1, Math.min(b.dist, 8))){
			if (Dungeon.level.solid[c] || (L != null && !L.onPlain(c))) break;
			cells.add(c);
			if (Actor.findChar(c) != null) break;
		}
		return cells;
	}

	private void pyreSprite( int pose ){
		if (sprite instanceof PyreSprite) ((PyreSprite) sprite).cast(pose);
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		AshBossLevel L = arena();
		switch (kind){
			case STOMP: {
				later(new Runnable() { public void run() { PixelScene.shake(4, 0.5f); } });
				Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 0.6f);
				blast(cells, 40, 58, false);
				//the foot comes down on the middle of the stripe, the sides only crack
				if (L != null){
					for (int c : line(aim, phase >= 3 ? 7 : 6)) L.melt(c, Random.IntRange(12, 18));
				}
				break;
			}
			case SWEEP: {
				Sample.INSTANCE.play(Assets.Sounds.HIT_CRUSH, 1f, 0.7f);
				for (Char ch : blast(cells, 36, 52, false)){
					if (ch.isAlive()) knock(ch, 2);
				}
				for (int c : cells){
					if (Dungeon.level.heroFOV[c] && Random.Int(3) == 0) CellEmitter.get(c).burst(FlameParticle.FACTORY, 3);
				}
				break;
			}
			case CHARGE:
				charge(cells);
				break;
			case QUAKE: {
				later(new Runnable() { public void run() { PixelScene.shake(5, 0.7f); } });
				Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 0.5f);
				blast(cells, 34, 50, false);
				if (L != null){
					for (int c : cells){
						if (Random.Int(4) == 0) L.melt(c, Random.IntRange(10, 14));
					}
				}
				break;
			}
			case STORM:
				startStorm();
				break;
		}
	}

	//special hit on every foe in the cells, half armor
	private ArrayList<Char> blast( Iterable<Integer> cells, int min, int max, boolean burn ){
		ArrayList<Char> hit = SKCombat.foesIn(this, cells);
		for (Char ch : hit){
			ch.damage(Math.max(1, Math.round(SKCombat.specialDamage(this, ch, min, max) * CoopBalance.damageFactor(this) * soloFactor())), this);
			if (burn && ch.isAlive()) Buff.affect(ch, Burning.class).reignite(ch, 3f);
		}
		return hit;
	}

	private void charge( ArrayList<Integer> cells ){
		AshBossLevel L = arena();
		int start = pos;
		Char victim = null;
		PyreBrazier crashed = null;
		for (int c : cells){
			Char ch = Actor.findChar(c);
			if (ch instanceof PyreBrazier){
				crashed = (PyreBrazier) ch;
				break;
			}
			if (ch != null){
				if (SKCombat.foes(this, ch)) victim = ch;
				break;
			}
			if (Dungeon.level.solid[c] || (L != null && !L.onPlain(c))) break;
			move(c, false);
		}
		if (pos != start && sprite != null){
			sprite.move(start, pos);
			if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.TRAMPLE);
		}
		if (crashed != null){
			boolean wasLit = crashed.lit;
			crashed.knockOver(false);
			stunned = 2;
			if (wasLit) GLog.p(Messages.get(this, "crashed"));
			later(new Runnable() { public void run() { PixelScene.shake(5, 0.6f); } });
		} else if (victim != null){
			attack(victim, 1.5f, 0, 1.5f);
			if (victim.isAlive()) knock(victim, 2);
		}
	}

	//shove away from the pyre, lava included (that's on you)
	private void knock( Char ch, int n ){
		AshBossLevel L = arena();
		int w = Dungeon.level.width();
		int dx = Integer.signum(ch.pos % w - pos % w), dy = Integer.signum(ch.pos / w - pos / w);
		if (dx == 0 && dy == 0) return;
		if (Char.hasProp(ch, Property.IMMOVABLE)) return;
		for (int i = 0; i < n; i++){
			int next = ch.pos + dx + dy * w;
			boolean open = Dungeon.level.passable[next] || (L != null && L.lava(next));
			if (!open || Actor.findChar(next) != null) return;
			int from = ch.pos;
			ch.pos = next;
			Actor.add(new Pushing(ch, from, next));
			Dungeon.level.occupyCell(ch);
			if (!ch.isAlive()) return;
		}
	}

	// *** storm and falling embers ***

	private void startStorm(){
		AshBossLevel L = arena();
		if (L == null) return;
		L.storm = phase >= 4 ? 8 : 7;
		for (Hero h : Party.living()){
			Buff.prolong(h, AshBlind.class, L.storm + 1);
			PyreGlow g = Buff.prolong(h, PyreGlow.class, L.storm + 1);
			g.charID = id();
		}
		Dungeon.observe();
		PyreHud.banner(Messages.get(AshPyre.class, "banner_storm"));
		GLog.w(Messages.get(this, "hint_storm"));
		Sample.INSTANCE.play(Assets.Sounds.GAS, 1f, 0.6f);
		walkers(1 + Party.living().size() / 3);
		CoopLog.line("pyre storm t=" + (int) Actor.now());
	}

	private void rainTick(){
		if (phase < 3 || mode == DYING) return;
		if (--rainTimer > 0) return;
		rainTimer = 5 + (Party.living().size() <= 1 ? 1 : 0);
		AshBossLevel L = arena();
		if (L == null) return;
		ArrayList<Integer> cells = new ArrayList<>();
		int w = Dungeon.level.width();
		for (Hero h : Party.living()){
			if (L.onPlain(h.pos) && !cells.contains(h.pos)) cells.add(h.pos);
			for (int i = 0; i < 1; i++){
				int c = h.pos + Random.IntRange(-1, 1) + Random.IntRange(-1, 1) * w;
				if (L.onPlain(c) && !Dungeon.level.solid[c] && !cells.contains(c)) cells.add(c);
			}
		}
		//and now and then on a knocked brazier, which lights it again (not at the end, that's the heroes' part)
		if (phase == 3){
			for (PyreBrazier b : L.braziers()){
				if (!b.lit && Random.Int(6) == 0 && !cells.contains(b.pos)) cells.add(b.pos);
			}
		}
		if (cells.isEmpty()) return;
		float delay = SKCombat.announceDelay(TICK);
		SKCombat.mark(cells, delay);
		AshBossLevel.Strike s = new AshBossLevel.Strike();
		s.kind = RAIN;
		s.at = Actor.now() + delay;
		s.cells = new int[cells.size()];
		for (int i = 0; i < s.cells.length; i++) s.cells[i] = cells.get(i);
		L.strikes.add(s);
	}

	//embers landing (AshBossLevel.tick, on time)
	public static void strike( AshBossLevel L, AshBossLevel.Strike s ){
		AshPyre p = AshBossLevel.pyre(L);
		if (p == null || !p.fighting()) return;
		ArrayList<Integer> cells = new ArrayList<>();
		for (int c : s.cells) cells.add(c);
		for (int c : cells){
			if (L.heroFOV[c]){
				CellEmitter.center(c).burst(BlastParticle.FACTORY, 4);
				CellEmitter.get(c).burst(FlameParticle.FACTORY, 6);
			}
		}
		Sample.INSTANCE.play(Assets.Sounds.BLAST, 0.8f, 1.2f);
		p.blast(cells, 26, 40, true);
		if (p.phase == 3){
			for (int c : cells){
				Char ch = Actor.findChar(c);
				if (ch instanceof PyreBrazier && !((PyreBrazier) ch).lit) ((PyreBrazier) ch).light(false);
			}
		}
	}

	private void walkers( int n ){
		AshBossLevel L = arena();
		if (L == null) return;
		int alive = 0;
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof AshWalker && m.isAlive()) alive++;
		}
		ArrayList<Hero> living = Party.living();
		//fewer at the end, the braziers are the job there
		int cap = 1 + living.size() / (phase >= 4 ? 3 : 2);
		for (int i = 0; i < n && alive < cap && !living.isEmpty(); i++){
			Hero h = living.get(Random.Int(living.size()));
			ArrayList<Integer> cells = L.freePlain(h.pos, 2, 4);
			if (cells.isEmpty()) continue;
			int c = cells.get(Random.Int(cells.size()));
			AshWalker m = new AshWalker();
			m.pos = c;
			m.state = m.HUNTING;
			GameScene.add(m, 1f);
			if (L.heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.DUST), 8);
			alive++;
		}
	}

	@Override
	public String description() {
		String d = Messages.get(this, "desc");
		if (fighting()) d += "\n\n" + Messages.get(this, "desc_heat", Math.max(0, heat));
		return d;
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("intro", intro);
		bundle.put("phase", phase);
		bundle.put("heat", heat);
		bundle.put("mode", mode);
		bundle.put("mode_turns", modeTurns);
		bundle.put("blazing", blazing);
		bundle.put("stunned", stunned);
		bundle.put("fuel", fuel);
		bundle.put("storm_timer", stormTimer);
		bundle.put("rain_timer", rainTimer);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		intro = bundle.getInt("intro");
		phase = bundle.getInt("phase");
		heat = bundle.getInt("heat");
		mode = bundle.getInt("mode");
		modeTurns = bundle.getInt("mode_turns");
		blazing = bundle.getInt("blazing");
		stunned = bundle.getInt("stunned");
		fuel = bundle.getInt("fuel");
		stormTimer = bundle.getInt("storm_timer");
		rainTimer = bundle.getInt("rain_timer");
		if (intro < 4 && state != PASSIVE) state = PASSIVE;
	}

	// *** the storm, on the heroes ***

	//ash in your eyes: you see two cells around you
	public static class AshBlind extends FlavourBuff {

		{
			type = buffType.NEGATIVE;
			announced = false;
		}

		@Override
		public boolean attachTo( Char target ) {
			if (super.attachTo(target)){
				target.viewDistance = Math.min(target.viewDistance, 2);
				return true;
			}
			return false;
		}

		@Override
		public void detach() {
			super.detach();
			if (Dungeon.level != null) target.viewDistance = Dungeon.level.viewDistance;
			if (target == Dungeon.hero) Dungeon.observe();
		}

		@Override
		public int icon() {
			return BuffIndicator.BLINDNESS;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(0x9A8A7E);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", dispTurns());
		}
	}

	//...but the pyre glows through it
	public static class PyreGlow extends TalismanOfForesight.CharAwareness {
	}

	// *** the sprite ***

	//one row of 48x48: 0-1 idle, 2-5 run, 6-8 attack, 9-11 die, 12-13 dimmed, 14-15 relighting, 16-17 ash storm
	public static class PyreSprite extends MobSprite {

		public static final int STOMP_CAST = 0, SPIT_CAST = 1, STORM_CAST = 2;

		private final Animation dim, relight, storm, stomp, spit;
		private Emitter fire, smoke;
		private HeartGlow heart;
		private float t = 0;

		public PyreSprite(){
			super();
			texture(ContinentAssets.PYRE);
			TextureFilm film = new TextureFilm(texture, 48, 48);
			idle = new Animation(3, true);
			idle.frames(film, 0, 0, 1, 1);
			run = new Animation(8, true);
			run.frames(film, 2, 3, 4, 5);
			attack = new Animation(10, false);
			attack.frames(film, 6, 7, 8, 0);
			zap = attack.clone();
			die = new Animation(5, false);
			die.frames(film, 12, 9, 10, 11);
			dim = new Animation(2, true);
			dim.frames(film, 12, 13);
			relight = new Animation(6, true);
			relight.frames(film, 14, 15);
			storm = new Animation(6, true);
			storm.frames(film, 16, 17);
			stomp = new Animation(9, false);
			stomp.frames(film, 6, 6, 7, 8, 0);
			spit = new Animation(10, false);
			spit.frames(film, 16, 17, 0);
			play(dim);
		}

		private AshPyre pyre(){
			return ch instanceof AshPyre ? (AshPyre) ch : null;
		}

		//what it does when it isn't moving or hitting
		private Animation rest(){
			AshPyre p = pyre();
			if (p == null) return idle;
			if (p.intro == 0) return dim;
			if (p.intro < 4) return relight;
			switch (p.mode){
				case DARK: case DYING: return dim;
				case CHANNEL: case FLARE: return relight;
				default:
					if (p.pendingKind == STORM) return storm;
					return idle;
			}
		}

		public void cast( int pose ){
			switch (pose){
				case STOMP_CAST: play(stomp); break;
				case SPIT_CAST: play(spit); break;
				default: play(storm); break;
			}
		}

		@Override
		public void idle() {
			play(rest());
		}

		@Override
		public void onComplete( Animation anim ) {
			if (anim == stomp || anim == spit){
				idle();
				return;
			}
			super.onComplete(anim);
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (parent != null){
				heart = new HeartGlow();
				parent.add(heart);
			}
			idle();
		}

		@Override
		public void update() {
			super.update();
			t += Game.elapsed;
			AshPyre p = pyre();
			if (p == null) return;
			if (curAnim != null && curAnim.looped && curAnim != run){
				Animation want = rest();
				if (curAnim != want) play(want);
			}
			boolean lit = p.isAlive() && p.intro >= 2 && p.mode != DARK && p.mode != DYING;
			if (lit && visible && fire == null && parent != null){
				fire = emitter();
				if (fire != null){
					fire.pos(this, 12, 4, 24, 14);
					fire.pour(FlameParticle.FACTORY, 0.1f);
				}
			} else if ((!lit || !visible) && fire != null){
				fire.on = false;
				fire = null;
			}
			boolean smoking = p.isAlive() && visible && !lit;
			if (smoking && smoke == null && parent != null){
				smoke = emitter();
				if (smoke != null){
					smoke.pos(this, 14, 10, 20, 16);
					smoke.pour(SmokeParticle.FACTORY, 0.2f);
				}
			} else if (!smoking && smoke != null){
				smoke.on = false;
				smoke = null;
			}
			if (heart != null){
				//where the heart sits in the frames: standing, crouched dark, getting up
				float hx = 25.6f, hy = 22.2f;
				if (curAnim == dim){
					hx = 28.5f;
					hy = 29f;
				} else if (curAnim == relight){
					hx = 26f;
					hy = 26.5f;
				}
				heart.point(flipHorizontal ? x + width - hx : x + hx, y + hy);
				float glow;
				if (!p.isAlive()) glow = 0;
				else if (p.intro == 0 || p.mode == DARK) glow = 0.12f + 0.06f * (float) Math.sin(t * 2);
				else if (p.mode == DYING) glow = 0.25f * Math.max(0, (float) Math.sin(t * 9));
				else if (p.mode == CHANNEL || p.mode == FLARE || p.blazing > 0) glow = 0.7f + 0.1f * (float) Math.sin(t * 6);
				else if (p.mode == GUTTER) glow = 0.2f + 0.15f * Math.abs((float) Math.sin(t * 5));
				else glow = 0.35f + 0.25f * p.heat / 100f + 0.06f * (float) Math.sin(t * 3);
				heart.am = glow;
				heart.visible = visible;
			}
		}

		@Override
		public void kill() {
			super.kill();
			if (fire != null) fire.on = false;
			if (smoke != null) smoke.on = false;
			if (heart != null) heart.killAndErase();
		}

		@Override
		public int blood() {
			return 0xFFFF8A20;
		}
	}

	//the heart of sacred essence, shining through the ash
	public static class HeartGlow extends Halo {

		public HeartGlow(){
			super(14, 0xFFE9A0, 0.4f);
		}

		@Override
		public void draw() {
			Blending.setLightMode();
			super.draw();
			Blending.setNormalMode();
		}
	}
}
