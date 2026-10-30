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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.starship;

import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.quests.AndroidQuest;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.LloydsBeacon;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.MetalShard;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKBoss;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKBulletField;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.LinkedHashSet;

/*
 * varkolyn queen, boss of the Starship. mother of the hive that took over the bridge
 *
 * sleeps in a big cocoon on the wall. hero walks in -> ship alarm, membrane closes the doors, the cocoon
 * swells and bursts and she drops right in front of them.
 *
 *  I   BRIDGE: poison line, floor tentacles, spike rings, laser sweeps, a slow core.
 *      in a pool of her own poison she's immune for a few turns, then exposed. varkolyns hatch from the
 *      pools and the biomass. biomass spreads, slows heroes, heals her
 *  II  FLIGHT (50%): lights out. she fades, can't be hurt, runs really fast. every few turns she stops and
 *      lays a nest where she is (1 per hero) and leaves a slime trail. motion radar shows roughly where.
 *      all nests laid + broken -> she takes the heroes and her brood to her lair
 *  III LAIR: at 1/4 hp she sheds her shell. faster, no defense, spits acid, more brood.
 *      a console opens the airlock: whatever isn't holding a handhold gets pulled, the brood gets sucked
 *      out, and she clings to the floor taking double dmg
 */
public class VarkolynLeader extends SKBoss {

	{
		spriteClass = Sprite.class;
		HP = HT = Dungeon.isChallenged(Challenges.STRONGER_BOSSES) ? 330 : 270;
		defenseSkill = 15;
		EXP = 30;
		//asleep in the cocoon until a hero comes. ojo: NOT PASSIVE, SKBoss would show her hp bar on load
		state = SLEEPING;
	}

	private static final int POISON = 0;
	private static final int LASERS = 1;
	private static final int ACID = 2;

	public static final int MAX_TENTACLES = 4;

	public int intro = 0;           //0 cocoon, 1-3 waking up, 4 fighting
	private int phase = 1;          //1 bridge, 2 flight (while nests stand), 3 lair
	public boolean molted = false;
	private int[] nests = new int[0];
	private int nestsToLay = 0;
	private int nestTimer = 0;
	private int wanderTarget = -1;
	private int wanderTimer = 0;
	private int fade = 0;
	public static final int FADE = 15;
	//where she went while fleeing (newest last), for the slime trail
	public int[] trail = new int[0];
	public float[] trailTime = new float[0];

	public boolean fighting(){
		return intro >= 4 && isAlive();
	}

	public boolean fleeing(){
		return phase == 2;
	}

	public boolean inLair(){
		return phase == 3;
	}

	//clinging to the floor while the airlock's open
	public boolean holding(){
		return phase == 3 && Dungeon.level instanceof StarshipBossLevel
				&& ((StarshipBossLevel) Dungeon.level).airlock == StarshipBossLevel.AIRLOCK_OPEN;
	}

	public ArrayList<Integer> nestCells(){
		ArrayList<Integer> out = new ArrayList<>();
		for (int id : nests){
			Actor a = Actor.findById(id);
			if (a instanceof Char && ((Char) a).isAlive()) out.add(((Char) a).pos);
		}
		return out;
	}

	public int nestsLeftToLay(){
		return nestsToLay;
	}

	//how visible she is while fleeing: fades over 15 turns, the closer a hero the better she shows.
	//just a picture, game logic never reads it (coop!)
	public float flightAlpha(){
		int d = 99;
		for (Hero h : Party.living()){
			if (Dungeon.level != null) d = Math.min(d, Dungeon.level.distance(h.pos, pos));
		}
		float near = Math.max(0.06f, Math.min(0.9f, 1f - (d - 1) / 9f));
		return near + (1f - near) * (fade / (float) FADE);
	}

	@Override
	public float speed() {
		return super.speed() * (phase == 2 ? 2.5f : 1f);
	}
	private int poolTimer = 0;      //>0 turns safe in her pool, <0 turns exposed after
	private int poolCooldown = 0;
	private int broodTimer = 8;

	@Override
	public boolean isInvulnerable( Class effect ) {
		return intro < 4 || phase == 2 || poolTimer > 0 || super.isInvulnerable(effect);
	}

	private static void later( final Runnable r ){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				r.run();
			}
		});
	}

	private StarshipBossLevel bridge(){
		return Dungeon.level instanceof StarshipBossLevel && ((StarshipBossLevel) Dungeon.level).cocoon != -1
				? (StarshipBossLevel) Dungeon.level : null;
	}

	// *** waking up ***

	private boolean heroCame(){
		StarshipBossLevel L = bridge();
		for (Hero h : Party.living()){
			if (L == null){
				if (Dungeon.level.distance(h.pos, pos) <= 6) return true;
			} else if (L.inBridge(h.pos) && Dungeon.level.distance(h.pos, pos) <= 9){
				return true;
			}
		}
		return Dungeon.level.locked;
	}

	private boolean introAct(){
		switch (intro){
			case 0: default:
				if (heroCame()){
					intro = 1;
					state = PASSIVE;
					BossHealthBar.assignBoss(this);
					Dungeon.level.seal();
					Dungeon.level.gatherParty();
					Sample.INSTANCE.play(Assets.Sounds.ALERT, 1f, 0.7f);
					VarkolynHud.banner(Messages.get(this, "banner_alert"));
					GLog.n(Messages.get(this, "alarm"));
					CoopLog.line("varkolyn queen wakes t=" + (int) Actor.now() + " heroes=" + Party.living().size());
				}
				break;
			case 1:
				intro = 2;
				Sample.INSTANCE.play(Assets.Sounds.BURNING, 1f, 0.5f);
				later(new Runnable() { public void run() { PixelScene.shake(1, 1f); } });
				break;
			case 2:
				intro = 3;
				dropFromCocoon();
				break;
			case 3:
				intro = 4;
				state = HUNTING;
				cooldown = 2;
				testStart();
				AndroidQuest.queenFight(this);
				break;
		}
		spend(TICK);
		return true;
	}

	//cocoon bursts, she drops 2 cells in front of it
	private void dropFromCocoon(){
		invisible = 0;
		VarkolynVisuals.cocoonBursts();
		int w = Dungeon.level.width();
		int land = -1;
		for (int dy : new int[]{2, 3, 1}){
			for (int dx : new int[]{0, -1, 1}){
				int c = pos + dx + dy * w;
				if (Dungeon.level.passable[c] && Actor.findChar(c) == null){
					land = c;
					break;
				}
			}
			if (land != -1) break;
		}
		if (land != -1){
			int from = pos;
			move(land, false);
			if (sprite != null) sprite.jump(from, land, 24, 0.35f, null);
			for (int n : PathFinder.NEIGHBOURS8){
				Char ch = Actor.findChar(land + n);
				if (ch != null && SKCombat.foes(this, ch)) knock(ch, land);
			}
		}
		Sample.INSTANCE.play(Assets.Sounds.BLAST, 1f, 0.6f);
		Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 1f, 0.6f);
		later(new Runnable() { public void run() {
			PixelScene.shake(4, 0.8f);
			GameScene.flash(0x6040C060);
		} });
		if (Dungeon.level.heroFOV[pos]) CellEmitter.get(pos).burst(Speck.factory(Speck.STEAM), 10);
		if (sprite instanceof Sprite) ((Sprite) sprite).roar();
		VarkolynHud.title(Messages.get(this, "title_name"), Messages.get(this, "title_sub"));
		yell(Messages.get(this, "notice"));
	}

	private void knock( Char ch, int from ){
		int w = Dungeon.level.width();
		int dx = Integer.signum(ch.pos % w - from % w), dy = Integer.signum(ch.pos / w - from / w);
		int next = ch.pos + dx + dy * w;
		if (!Dungeon.level.passable[next] || Actor.findChar(next) != null) return;
		int old = ch.pos;
		ch.pos = next;
		Actor.add(new Pushing(ch, old, next));
		Dungeon.level.occupyCell(ch);
	}

	//-Dspd.test.varkweak=1 low hp, -Dspd.test.varkphase=2|3 starts there
	private void testStart(){
		if (DevFlags.varkWeak()){
			HT = 60;
			HP = HT;
		}
		int start = DevFlags.varkPhase();
		if (start >= 2){
			HP = HT / 2;
			enraged = true;
			BossHealthBar.bleed(true);
			startFlight();
		}
		if (start >= 3){
			nestsToLay = 0;
			for (int id : nests){
				Actor a = Actor.findById(id);
				if (a instanceof Char) ((Char) a).die(null);
			}
			lastStand();
		}
	}

	@Override
	public void notice() {
		//queen from an older save (no cocoon to wake up in): fight starts when she sees a hero
		if (intro >= 4) super.notice();
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (!isAlive() || intro < 4) return;
		if (dmg > 0){
			float m = 1f;
			if (poolTimer < 0) m *= 1.5f;
			if (holding()) m *= 2f;
			dmg = Math.max(1, Math.round(dmg * m));
		}
		super.damage(dmg, src);
		if (!isAlive()) return;
		if (phase == 1 && HP*2 <= HT) startFlight();
		if (phase == 3 && !molted && HP*4 <= HT) molt();
	}

	@Override
	public void move( int step, boolean travelling ) {
		int from = pos;
		super.move(step, travelling);
		Hive hive = Hive.get(Dungeon.level);
		if (phase == 2){
			addTrail(from);
		} else if (hive != null && intro >= 4){
			//hive grows where she walks, and after the molt she drips acid
			if (Random.Int(4) == 0) hive.put(Dungeon.level, from);
			if (molted && Random.Int(3) == 0) hive.acid(Dungeon.level, from, 10);
		}
	}

	private void addTrail( int cell ){
		int n = Math.min(trail.length + 1, 40);
		int[] t = new int[n];
		float[] tt = new float[n];
		int skip = trail.length + 1 - n;
		for (int i = skip; i < trail.length; i++){
			t[i - skip] = trail[i];
			tt[i - skip] = trailTime[i];
		}
		t[n - 1] = cell;
		tt[n - 1] = Actor.now();
		trail = t;
		trailTime = tt;
	}

	@Override
	protected boolean act() {
		if (!isAlive()) return true;
		if (intro < 4) return introAct();
		if (phase == 2){
			if (fade > 0) fade--;
			darkness();
			if (nestsToLay <= 0 && !nestsStand()){
				lastStand();
				spend(TICK);
				return true;
			}
			return flee();
		}
		//clinging while the airlock pulls
		if (holding()){
			if (sprite instanceof Sprite) ((Sprite) sprite).hold();
			spend(TICK);
			return true;
		}
		//safe in her own poison for a bit, then exposed
		if (poolTimer > 0){
			if (--poolTimer == 0){
				poolTimer = -2;
				if (sprite != null){
					sprite.remove(CharSprite.State.SHIELDED);
					if (Dungeon.level.heroFOV[pos]) sprite.showStatus(CharSprite.NEGATIVE, Messages.get(VarkolynLeader.class, "exposed"));
				}
			}
		} else if (poolTimer < 0){
			if (++poolTimer == 0) poolCooldown = 6;
		} else if (poolCooldown > 0){
			poolCooldown--;
		} else if (inPool()){
			poolTimer = 3;
			if (sprite != null){
				sprite.add(CharSprite.State.SHIELDED);
				if (Dungeon.level.heroFOV[pos]) sprite.showStatus(CharSprite.WARNING, Messages.get(VarkolynLeader.class, "pool"));
			}
		}
		//biomass heals her. burn it, en serio
		Hive hive = Hive.get(Dungeon.level);
		if (hive != null && hive.biomass(pos) && HP < HT){
			HP = Math.min(HT, HP + 1);
			if (sprite != null && Dungeon.level.heroFOV[pos] && Random.Int(3) == 0) sprite.emitter().burst(Speck.factory(Speck.HEALING), 2);
		}
		//little varkolyns hatch from the pools + biomass
		if (Dungeon.level.locked && --broodTimer <= 0){
			broodTimer = phase == 3 ? (molted ? 8 : 10) : 9;
			brood();
		}
		return super.act();
	}

	private boolean inPool(){
		Blob gas = Dungeon.level.blobs.get(ToxicGas.class);
		return gas != null && gas.volume > 0 && gas.cur[pos] > 0;
	}

	private static int varkolyns(){
		int n = 0;
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof StarshipMobs.Varkolyn && m.isAlive()) n++;
		}
		return n;
	}

	private void brood(){
		int heroes = Party.living().size();
		int max = phase == 3 ? 2 * heroes + (molted ? 1 : 0) : 1 + 2 * heroes;
		if (varkolyns() >= max) return;
		Blob gas = Dungeon.level.blobs.get(ToxicGas.class);
		Hive hive = Hive.get(Dungeon.level);
		VarkolynLair lair = phase == 3 ? VarkolynLair.find() : null;
		ArrayList<Integer> cells = new ArrayList<>();
		for (int c = 0; c < Dungeon.level.length(); c++){
			//her poison anywhere, biomass only near her
			boolean source = (gas != null && gas.volume > 0 && gas.cur[c] > 0)
					|| (hive != null && hive.biomass(c) && Dungeon.level.distance(c, pos) <= 4);
			if (!source || !Dungeon.level.passable[c] || Actor.findChar(c) != null) continue;
			if (lair != null && !lair.inside(Dungeon.level.cellToPoint(c))) continue;
			cells.add(c);
		}
		if (cells.isEmpty()) return;
		StarshipMobs.Varkolyn v = new StarshipMobs.Varkolyn();
		v.noEgg();
		v.summoned();
		v.pos = cells.get(Random.Int(cells.size()));
		v.state = v.HUNTING;
		GameScene.add(v);
		if (Dungeon.level.heroFOV[v.pos]) CellEmitter.get(v.pos).burst(Speck.factory(Speck.STEAM), 5);
	}

	// *** phase 2, flight ***

	private void startFlight(){
		phase = 2;
		fade = FADE;
		poolTimer = 0;
		pendingKind = -1;
		pendingCells = new int[0];
		nestsToLay = Math.max(1, Party.living().size());
		nestTimer = 3;
		wanderTarget = -1;
		if (sprite != null) sprite.remove(CharSprite.State.SHIELDED);
		if (Dungeon.level.heroFOV[pos]) CellEmitter.get(pos).burst(Speck.factory(Speck.STEAM), 10);
		darkness();
		Dungeon.observe();
		GameScene.updateFog();
		Sample.INSTANCE.play(Assets.Sounds.DEGRADE, 1f, 0.7f);
		yell(Messages.get(this, "hide"));
		GLog.w(Messages.get(this, "nests", nestsToLay));
		VarkolynHud.banner(Messages.get(this, "banner_flight"));
		CoopLog.line("varkolyn queen flees t=" + (int) Actor.now() + " nests=" + nestsToLay);
	}

	//ship lights go out while she hides
	private void darkness(){
		for (Hero h : Party.living()) Buff.prolong(h, HiveDark.class, 2f);
	}

	private boolean nestsStand(){
		//-Dspd.test.lairnests=T breaks the nests at turn T
		int t = DevFlags.lairNests();
		if (t > 0 && Actor.now() >= t){
			nestsToLay = 0;
			for (int id : nests){
				Actor a = Actor.findById(id);
				if (a instanceof Char && ((Char) a).isAlive()) ((Char) a).die(null);
			}
		}
		for (int id : nests){
			Actor a = Actor.findById(id);
			if (a instanceof Char && ((Char) a).isAlive()) return true;
		}
		return false;
	}

	//stops and lays a nest next to her if no hero's close
	private boolean layNest(){
		for (Hero h : Party.living()){
			if (Dungeon.level.distance(h.pos, pos) <= 3) return false;
		}
		VarkolynLair lair = VarkolynLair.find();
		int cell = -1;
		for (int n : PathFinder.NEIGHBOURS8){
			int c = pos + n;
			if (!Dungeon.level.passable[c] || Dungeon.level.pit[c] || Actor.findChar(c) != null) continue;
			int k = Dungeon.level.map[c];
			if (k == Terrain.DOOR || k == Terrain.OPEN_DOOR || k == Terrain.LOCKED_DOOR || k == Terrain.ENTRANCE
					|| k == Terrain.EXIT || k == Terrain.LOCKED_EXIT) continue;
			if (lair != null && lair.inside(Dungeon.level.cellToPoint(c))) continue;
			cell = c;
			break;
		}
		if (cell == -1) return false;
		LairNest n = new LairNest();
		n.pos = cell;
		GameScene.add(n);
		if (Dungeon.level.heroFOV[cell]) CellEmitter.get(cell).burst(Speck.factory(Speck.STEAM), 6);
		int[] ids = new int[nests.length + 1];
		System.arraycopy(nests, 0, ids, 0, nests.length);
		ids[nests.length] = n.id();
		nests = ids;
		nestsToLay--;
		if (sprite instanceof Sprite) ((Sprite) sprite).lay();
		Sample.INSTANCE.play(Assets.Sounds.BURNING, 0.8f, 1.4f);
		GLog.w(Messages.get(this, "nest_laid", nestsToLay));
		VarkolynHud.ping();
		CoopLog.line("varkolyn queen lays a nest at " + cell + " left=" + nestsToLay + " t=" + (int) Actor.now());
		return true;
	}

	//runs from the closest hero. nobody near -> goes somewhere far from them
	private boolean flee(){
		if (nestsToLay > 0 && --nestTimer <= 0){
			if (layNest()){
				nestTimer = 6 + Random.Int(3);
				spend(TICK);
				return true;
			}
			nestTimer = 1;
		}
		Hero near = null;
		for (Hero h : Party.living()){
			if (near == null || Dungeon.level.distance(h.pos, pos) < Dungeon.level.distance(near.pos, pos)) near = h;
		}
		int old = pos;
		boolean moved = false;
		if (!rooted && near != null){
			if (Dungeon.level.distance(pos, near.pos) <= 6){
				moved = getFurther(near.pos);
			} else {
				if (wanderTarget == -1 || pos == wanderTarget || --wanderTimer <= 0) pickWanderTarget();
				if (wanderTarget != -1) moved = getCloser(wanderTarget);
			}
		}
		if (moved){
			spend(1 / speed());
			return moveSprite(old, pos);
		}
		spend(TICK);
		return true;
	}

	private void pickWanderTarget(){
		wanderTimer = 15;
		VarkolynLair lair = VarkolynLair.find();
		int best = -1, bestD = -1;
		for (int tries = 0; tries < 30; tries++){
			int c = Random.Int(Dungeon.level.length());
			if (!Dungeon.level.passable[c] || Dungeon.level.pit[c]) continue;
			if (lair != null && lair.inside(Dungeon.level.cellToPoint(c))) continue;
			int d = 99;
			for (Hero h : Party.living()) d = Math.min(d, Dungeon.level.distance(c, h.pos));
			if (d > bestD){
				bestD = d;
				best = c;
			}
		}
		wanderTarget = best;
	}

	// *** phase 3, lair ***

	private void lastStand(){
		phase = 3;
		enraged = true;
		cooldown = 1;
		for (Hero h : Party.living()){
			HiveDark d = h.buff(HiveDark.class);
			if (d != null) d.detach();
		}

		VarkolynLair lair = VarkolynLair.find();
		if (lair != null){
			int center = Dungeon.level.pointToCell(lair.center());
			if (Actor.findChar(center) == null || Actor.findChar(center) == this){
				ScrollOfTeleportation.appear(this, center);
			}
			ArrayList<Integer> cells = lair.cells(Dungeon.level);
			int w = Dungeon.level.width();
			int midX = (lair.left + lair.right)/2;
			ArrayList<Integer> west = new ArrayList<>(), east = new ArrayList<>();
			for (int c : cells){
				if (!Dungeon.level.passable[c] || Actor.findChar(c) != null) continue;
				if (c % w < midX) west.add(c); else if (c % w > midX) east.add(c);
			}
			//heroes + allies on one side...
			ArrayList<Char> heroes = new ArrayList<>();
			heroes.addAll(Party.living());
			for (Mob m : Dungeon.level.mobs){
				if (m.alignment == Char.Alignment.ALLY && !m.properties().contains(Property.IMMOVABLE)) heroes.add(m);
			}
			bring(heroes, west);
			//...and as much brood as there are heroes on the other. the rest + eggs stay behind
			ArrayList<Char> brood = new ArrayList<>();
			int room = Party.living().size();
			for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
				if (m instanceof StarshipMobs.Varkolyn && m.isAlive()){
					if (brood.size() < room) brood.add(m);
					else {
						m.destroy();
						if (m.sprite != null) m.sprite.killAndErase();
					}
				}
				if (m instanceof StarshipFeatures.VarkolynEgg) m.die(null);
			}
			bring(brood, east);
		}
		if (sprite != null) CellEmitter.get(pos).burst(Speck.factory(Speck.STEAM), 10);
		Dungeon.observe();
		GameScene.updateFog();
		SKCombat.focusLocal();
		if (Dungeon.level instanceof StarshipBossLevel) ((StarshipBossLevel) Dungeon.level).finalMusic();
		yell(Messages.get(this, "last_stand"));
		GLog.w(Messages.get(this, "hint_lair"));
		VarkolynHud.banner(Messages.get(this, "banner_lair"));
		CoopLog.line("varkolyn queen last stand t=" + (int) Actor.now() + " lair=" + (lair != null));
	}

	private void bring( ArrayList<Char> chars, ArrayList<Integer> cells ){
		for (Char ch : chars){
			int best = -1;
			for (int c : cells){
				if (Actor.findChar(c) != null) continue;
				best = c;
				break;
			}
			if (best == -1) break;
			cells.remove((Integer) best);
			ScrollOfTeleportation.appear(ch, best);
		}
	}

	//sheds the shell at 1/4 hp. gross
	private void molt(){
		molted = true;
		baseSpeed = 1.5f;
		defenseSkill = 5;
		pendingKind = -1;
		broodTimer = 1;
		cooldown = 1;
		if (sprite instanceof Sprite) ((Sprite) sprite).molt();
		if (Dungeon.level.heroFOV[pos]){
			CellEmitter.get(pos).burst(Speck.factory(Speck.ROCK), 10);
			Sample.INSTANCE.play(Assets.Sounds.SHATTER, 1f, 0.6f);
		}
		later(new Runnable() { public void run() {
			PixelScene.shake(4, 0.8f);
			GameScene.flash(0x6060C040);
		} });
		yell(Messages.get(this, "molt"));
		GLog.w(Messages.get(this, "hint_molt"));
		VarkolynHud.banner(Messages.get(this, "banner_molt"));
		CoopLog.line("varkolyn queen molts t=" + (int) Actor.now() + " hp=" + HP + "/" + HT);
	}

	//nests she leaves while hiding. weaker, fewer varkolyns each
	public static class LairNest extends StarshipFeatures.VarkolynNest {
		public LairNest(){
			super();
			HP = HT = 12 + Dungeon.scalingDepth();
		}
		@Override protected int period() { return 7; }
		@Override protected int max() { return 2; }
		@Override protected boolean champions() { return false; }
	}

	//ship lights out: heroes see 3 cells around them
	public static class HiveDark extends FlavourBuff {

		{
			type = buffType.NEGATIVE;
			announced = false;
		}

		@Override
		public boolean attachTo( Char target ) {
			if (super.attachTo(target)){
				target.viewDistance = Math.min(target.viewDistance, 3);
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
			icon.hardlight(0xD04040);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc");
		}
	}

	@Override public int damageRoll() { return Random.NormalIntRange(5, 12); }
	@Override public int attackSkill( Char target ) { return 22; }
	@Override public int drRoll() { return molted ? 0 : super.drRoll() + Random.NormalIntRange(0, 6); }

	@Override protected int scoreIndex() { return 2; }
	@Override protected int score() { return 3000; }

	private int tentacles(){
		int n = 0;
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof StarshipMobs.Tentacle && m.isAlive()) n++;
		}
		return n;
	}

	@Override
	protected boolean special( Char enemy ) {
		if (!fieldOfView[enemy.pos]) return false;
		int spell = pickSpell(molted ? 6 : 5);
		if (spell == 1 && tentacles() >= MAX_TENTACLES) spell = 2;
		boolean visible = Dungeon.level.heroFOV[pos];
		if (sprite != null){
			sprite.turnTo(pos, enemy.pos);
			if (sprite instanceof Sprite) ((Sprite) sprite).spit();
		}
		switch (spell){
			case 0: default: {
				//4 poison puddles in a line toward the target
				ArrayList<Integer> cells = line(enemy.pos, 8);
				LinkedHashSet<Integer> marked = new LinkedHashSet<>();
				for (int i = 1; i < cells.size() && marked.size() < 4*9; i += 2){
					marked.addAll(SKCombat.area(cells.get(i), 1));
				}
				if (marked.isEmpty()) marked.addAll(SKCombat.area(enemy.pos, 1));
				announce(POISON, marked, enemy.pos);
				if (visible) GLog.w(Messages.get(this, "poison"));
				return true;
			}
			case 1: {
				int n = Math.min(enraged ? 3 : 2, MAX_TENTACLES - tentacles());
				ArrayList<Integer> spots = new ArrayList<>();
				for (int c : SKCombat.area(enemy.pos, 4)){
					if (Dungeon.level.passable[c] && Actor.findChar(c) == null && Dungeon.level.distance(c, enemy.pos) >= 2){
						spots.add(c);
					}
				}
				for (int i = 0; i < n && !spots.isEmpty(); i++){
					StarshipMobs.Tentacle t = new StarshipMobs.Tentacle();
					t.pos = spots.remove(Random.Int(spots.size()));
					t.state = t.HUNTING;
					GameScene.add(t);
				}
				if (visible) GLog.w(Messages.get(this, "tentacles"));
				break;
			}
			case 2:
				ring(enraged ? 20 : 16, enemy.pos, 1f + speedBonus(), ItemSpriteSheet.SHOT_SPIKE, 3, 7);
				if (visible) GLog.w(Messages.get(this, "ring"));
				break;
			case 3: {
				//laser sweep, 2 lines (3 when enraged), marked a turn before
				LinkedHashSet<Integer> marked = new LinkedHashSet<>();
				float aim = SKBulletField.angleTo(pos, enemy.pos);
				float[] offsets = enraged ? new float[]{-40, 0, 40} : new float[]{-25, 25};
				for (float o : offsets){
					double rad = StrictMath.toRadians(aim + o);
					int w = Dungeon.level.width();
					int tx = pos % w + (int)StrictMath.round(StrictMath.cos(rad)*9);
					int ty = pos / w + (int)StrictMath.round(StrictMath.sin(rad)*9);
					tx = Math.max(0, Math.min(w-1, tx));
					ty = Math.max(0, Math.min(Dungeon.level.height()-1, ty));
					marked.addAll(line(tx + ty*w, 9));
				}
				if (marked.isEmpty()) break;
				announce(LASERS, marked, enemy.pos);
				if (visible) GLog.w(Messages.get(this, "lasers"));
				return true;
			}
			case 4: {
				SKBulletField.Bullet core = shoot(SKBulletField.angleTo(pos, enemy.pos), 0.5f, ItemSpriteSheet.SHOT_BIG_ORB, 4, 9);
				core.behaviour = SKBulletField.SPINNER;
				core.children = enraged ? 4 : 3;
				core.spread = 25;
				core.childImage = ItemSpriteSheet.SHOT_SPIKE;
				core.childSpeed = SKBulletField.UNIT;
				core.life = 8;
				if (visible) GLog.w(Messages.get(this, "core"));
				break;
			}
			case 5: {
				//acid around every hero she sees (after the molt only)
				LinkedHashSet<Integer> marked = new LinkedHashSet<>();
				for (Hero h : Party.living()){
					if (fieldOfView[h.pos]) marked.addAll(SKCombat.openArea(h.pos, 1));
				}
				if (marked.isEmpty()) marked.addAll(SKCombat.openArea(enemy.pos, 1));
				announce(ACID, marked, enemy.pos);
				if (visible) GLog.w(Messages.get(this, "acid"));
				return true;
			}
		}
		spend(TICK);
		return true;
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		if (kind == POISON){
			for (int c : cells){
				if (!Dungeon.level.solid[c]) GameScene.add(Blob.seed(c, 8, ToxicGas.class));
			}
		} else if (kind == ACID){
			Hive hive = Hive.getOrMake(Dungeon.level);
			for (int c : cells) hive.acid(Dungeon.level, c, 12);
			hitFoes(cells, 3, 6);
			Sample.INSTANCE.play(Assets.Sounds.GAS, 0.8f, 1.4f);
		} else {
			StarshipMobs.laser(this, cells, 8, 16);
		}
	}

	//her own puddle poison doesn't hurt her, obvio
	@Override
	public boolean isImmune( Class effect ) {
		if (effect == ToxicGas.class) return true;
		return super.isImmune(effect);
	}

	@Override
	public void die( Object cause ) {
		//tentacles die w/ her
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (m instanceof StarshipMobs.Tentacle && m.isAlive()) m.die(null);
			if (m instanceof LairNest && m.isAlive()) m.die(null);
		}
		super.die(cause);
		//hive withers, lights back on
		Hive hive = Hive.get(Dungeon.level);
		if (hive != null) hive.wither();
		for (Hero h : Party.living()){
			HiveDark d = h.buff(HiveDark.class);
			if (d != null) d.detach();
		}
		if (Dungeon.level instanceof StarshipBossLevel){
			StarshipBossLevel L = (StarshipBossLevel) Dungeon.level;
			L.airlock = StarshipBossLevel.AIRLOCK_READY;
			L.airlockTimer = 0;
		}
		VarkolynHud.banner(Messages.get(this, "banner_won"), true);
		GLog.p(Messages.get(this, "won"));
		later(new Runnable() { public void run() {
			PixelScene.shake(3, 1f);
			GameScene.flash(0x6060FF80);
		} });
		//lair door opens
		VarkolynLair lair = VarkolynLair.find();
		if (lair != null && lair.doorCell != -1 && Dungeon.level.map[lair.doorCell] == Terrain.WALL){
			Level.set(lair.doorCell, Terrain.DOOR);
			GameScene.updateMap(lair.doorCell);
			Dungeon.observe();
		}
		CoopLog.line("varkolyn queen dies t=" + (int) Actor.now());
	}

	@Override
	public String description() {
		if (intro < 3) return Messages.get(this, "desc_cocoon");
		return Messages.get(this, molted ? "desc_molted" : "desc") + "\n\n" + Messages.get(this, "desc_phase" + Math.max(1, Math.min(3, phase)));
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("intro", intro);
		bundle.put("phase", phase);
		bundle.put("molted", molted);
		bundle.put("nests", nests);
		bundle.put("nests_to_lay", nestsToLay);
		bundle.put("nest_timer", nestTimer);
		bundle.put("wander", wanderTarget);
		bundle.put("wander_timer", wanderTimer);
		bundle.put("fade", fade);
		bundle.put("pool_timer", poolTimer);
		bundle.put("pool_cooldown", poolCooldown);
		bundle.put("brood_timer", broodTimer);
		bundle.put("trail", trail);
		bundle.put("trail_time", trailTime);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		//old save from back when this boss was the varkolyn lord: no intro saved, so she's already awake
		intro = bundle.contains("intro") ? bundle.getInt("intro") : 4;
		phase = bundle.contains("phase") ? bundle.getInt("phase") : 1;
		molted = bundle.getBoolean("molted");
		nests = bundle.contains("nests") ? bundle.getIntArray("nests") : new int[0];
		nestsToLay = bundle.getInt("nests_to_lay");
		nestTimer = bundle.getInt("nest_timer");
		wanderTarget = bundle.contains("wander") ? bundle.getInt("wander") : -1;
		wanderTimer = bundle.getInt("wander_timer");
		fade = bundle.getInt("fade");
		poolTimer = bundle.getInt("pool_timer");
		poolCooldown = bundle.getInt("pool_cooldown");
		broodTimer = bundle.contains("brood_timer") ? bundle.getInt("brood_timer") : 8;
		trail = bundle.contains("trail") ? bundle.getIntArray("trail") : new int[0];
		trailTime = bundle.contains("trail_time") ? bundle.getFloatArray("trail_time") : new float[0];
		if (trailTime.length != trail.length){
			trail = new int[0];
			trailTime = new float[0];
		}
		if (molted){
			baseSpeed = 1.5f;
			defenseSkill = 5;
		}
		if (intro < 3) invisible = 1;
		if (intro < 4) state = intro == 0 ? SLEEPING : PASSIVE;
	}

	@Override
	protected void onAdd() {
		super.onAdd();
		//asleep in the cocoon, nobody sees her yet
		if (intro < 3) invisible = Math.max(invisible, 1);
	}

	@Override
	protected ArrayList<Item> loot() {
		ArrayList<Item> items = new ArrayList<>();
		//DM-300's loot: 2-4 metal shards
		int shards = Random.chances(new float[]{0, 0, 6, 3, 1});
		for (int i = 0; i < shards; i++) items.add(new MetalShard());
		LloydsBeacon beacon = Dungeon.hero.belongings.getItem(LloydsBeacon.class);
		if (beacon != null) beacon.upgrade();
		items.add(new VoidRifle().random());
		items.add(new HiveHeart());
		return items;
	}

	/*
	 queen.png 40x40
	   row 0  w/ shell: 0-1 idle, 2-3 bite, 4-7 walk, 8-10 death
	   row 1  same, no shell
	   row 2  w/ shell: 0-1 spit, 2 roar, 3 lay, 6-8 molt, 9 falling out of the cocoon, 10 stunned,
	          and 4-5 = clinging WITHOUT the shell
	   row 3  same as row 2 but no shell, and 4-5 = clinging WITH it. yes it's backwards, don't ask
	*/
	public static class Sprite extends MobSprite {

		private static final int COLS = 11;

		private TextureFilm film;
		private int row = -1;
		private Animation spit, roar, lay, hold, molt;
		private boolean faded = false;
		private boolean molting = false;

		public Sprite(){
			super();
			texture(VarkolynAssets.QUEEN);
			film = new TextureFilm(texture, 40, 40);
			bodyRow(0);
			play(idle);
		}

		private Animation one( int fps, boolean loop, int... frames ){
			Animation a = new Animation(fps, loop);
			Object[] f = new Object[frames.length];
			for (int i = 0; i < frames.length; i++) f[i] = frames[i];
			a.frames(film, f);
			return a;
		}

		private void bodyRow( int r ){
			row = r;
			int o = r * COLS;
			int s = (r == 1 ? 3 : 2) * COLS;            //special poses for this look
			int h = (r == 1 ? 2 : 3) * COLS;            //clinging is swapped, see above
			idle = one(2, true, o, o, o, o + 1, o, o, o + 1, o + 1);
			run = one(12, true, o + 4, o + 5, o + 6, o + 7);
			attack = one(12, false, o + 2, o + 3, o);
			zap = one(10, false, s, s + 1, s + 1, o);
			die = one(8, false, o + 8, o + 9, o + 10);
			spit = zap;
			roar = one(2, false, s + 2, s + 2, s + 2, o);
			lay = one(4, false, s + 3, s + 3, o);
			hold = one(4, true, h + 4, h + 5);
			molt = one(4, false, 2 * COLS + 6, 2 * COLS + 7, 2 * COLS + 8, COLS);
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (ch instanceof VarkolynLeader && ((VarkolynLeader) ch).molted) bodyRow(1);
			play(idle);
		}

		public void spit(){
			play(spit);
		}

		public void roar(){
			play(roar);
		}

		public void lay(){
			play(lay);
		}

		public void hold(){
			if (curAnim != hold) play(hold);
		}

		public void molt(){
			molting = true;
			play(molt);
		}

		@Override
		public void update() {
			super.update();
			if (!(ch instanceof VarkolynLeader)) return;
			VarkolynLeader q = (VarkolynLeader) ch;
			//inside the cocoon she isn't drawn, VarkolynVisuals does the cocoon
			if (q.intro < 3) visible = false;
			if (q.fleeing()){
				alpha(q.flightAlpha());
				faded = true;
			} else if (faded){
				alpha(1f);
				faded = false;
			}
			if (curAnim == hold && !q.holding()) play(idle);
			if (!molting && q.molted && row != 1 && (curAnim == idle || curAnim == run)){
				bodyRow(1);
				play(idle);
			}
		}

		@Override
		public void onComplete( Animation anim ) {
			if (anim == molt){
				molting = false;
				bodyRow(1);
				play(idle);
				return;
			}
			super.onComplete(anim);
			if (anim == spit || anim == roar || anim == lay) play(idle);
		}

		@Override
		public int blood() {
			return 0xFF9944CC;
		}
	}
}
