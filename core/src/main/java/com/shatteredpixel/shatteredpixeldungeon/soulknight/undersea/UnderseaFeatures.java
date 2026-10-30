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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.undersea;

import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.quests.LabQuest;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SmokeScreen;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroAction;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKFloor;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKWorlds;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.props.SKProps;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//undersea stuff besides the mobs:
//- oxygen: drains slowly, bubble columns refill it, at 0 the hero loses hp every turn
//- giant clams, open now and then: pearl inside, and a bite for whoever's next to them when they shut
//- ink clouds, heroes can't see through them but sea mobs can
//- kraken tentacles (from infected crew + kraken minions) and the minions' mind control
public class UnderseaFeatures {

	public static boolean undersea(){
		return Dungeon.level instanceof UnderseaLevel && !(Dungeon.level instanceof UnderseaBossLevel);
	}

	// *** oxygen ***

	public static class Oxygen extends Buff implements Hero.Doom {

		{
			type = buffType.NEUTRAL;
			revivePersists = true;
		}

		public static final int MAX = 100;

		public int oxygen = MAX;
		private int depth = -1;
		private boolean warned = false;

		@Override
		public boolean act() {
			spend(TICK);
			if (!undersea()){
				detach();
				return true;
			}
			if (depth != Dungeon.depth){
				depth = Dungeon.depth;
				oxygen = MAX;
			}
			if (oxygen > 0){
				//lab dive suit: half the air use
				if (!LabQuest.suited(target) || ((int) Actor.now()) % 2 == 0) oxygen--;
				if (oxygen == 20 && target == Dungeon.local()) GLog.w(Messages.get(Oxygen.class, "low"));
				warned = false;
			} else if (target.isAlive()){
				if (!warned){
					warned = true;
					CoopLog.line("hero out of oxygen at " + target.pos);
					if (target == Dungeon.local()) GLog.n(Messages.get(Oxygen.class, "out"));
				}
				target.damage(1 + Dungeon.scalingDepth()/6, this);
			}
			return true;
		}

		@Override
		public void onDeath() {
			Dungeon.fail(this);
			GLog.n(Messages.get(Oxygen.class, "drowned"));
		}

		@Override
		public int icon() {
			return BuffIndicator.LIGHT;
		}

		@Override
		public void tintIcon( Image icon ) {
			if (oxygen > 40)      icon.hardlight(0x6FD3FF);
			else if (oxygen > 0)  icon.hardlight(0xFFC34D);
			else                  icon.hardlight(0xFF4D4D);
		}

		@Override
		public float iconFadePercent() {
			return 1f - oxygen / (float) MAX;
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString(oxygen);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", oxygen, MAX);
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("oxygen", oxygen);
			bundle.put("depth", depth);
			bundle.put("warned", warned);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			oxygen = bundle.getInt("oxygen");
			depth = bundle.getInt("depth");
			warned = bundle.getBoolean("warned");
		}
	}

	//FIXME static, so it stays true for the whole session. test only flag so whatever
	private static boolean testControlDone = false;

	//every hero on an undersea floor gets its oxygen buff. SKFloor calls this every turn
	public static void breathe(){
		if (!undersea()) return;
		//-Dspd.test.mindcontrol=T -> 2nd hero (or the only one) gets mind controlled at turn T
		int t = DevFlags.mindControl();
		if (t > 0 && !testControlDone && Actor.now() >= t){
			testControlDone = true;
			ArrayList<Hero> living = Party.living();
			Hero h = living.size() > 1 ? living.get(1) : (living.isEmpty() ? null : living.get(0));
			if (h != null){
				Buff.prolong(h, MindControl.class, MindControl.DURATION);
				CoopLog.line("test mind control on hero at " + h.pos + " now=" + Actor.now());
			}
		}
		for (Hero h : Party.living()){
			if (h.buff(Oxygen.class) == null) Buff.affect(h, Oxygen.class);
		}
	}

	//bubble column: +25 oxygen a turn
	public static void refill( Hero h ){
		Oxygen o = h.buff(Oxygen.class);
		if (o == null || o.oxygen >= Oxygen.MAX) return;
		o.oxygen = Math.min(Oxygen.MAX, o.oxygen + 25);
		if (h.sprite != null && Dungeon.level.heroFOV[h.pos]) h.sprite.emitter().burst(Speck.factory(Speck.BUBBLE), 4);
	}

	//like smoke but it blinds heroes + allies instead of the mobs (see UnderseaLevel)
	public static class InkCloud extends SmokeScreen {

		@Override
		public void use( BlobEmitter emitter ) {
			this.emitter = emitter;
			emitter.pour(ShadowParticle.UP, 0.04f);
		}

		@Override
		public String tileDesc() {
			return Messages.get(InkCloud.class, "desc");
		}
	}

	//ink geyser near a hero, splat
	public static void inkGeyser(){
		ArrayList<Hero> living = Party.living();
		if (living.isEmpty()) return;
		Hero h = living.get(Random.Int(living.size()));
		ArrayList<Integer> spots = new ArrayList<>();
		for (int c = 0; c < Dungeon.level.length(); c++){
			int d = Dungeon.level.distance(c, h.pos);
			if (d >= 3 && d <= 6 && Dungeon.level.passable[c] && !Dungeon.level.solid[c]) spots.add(c);
		}
		if (spots.isEmpty()) return;
		int c = spots.get(Random.Int(spots.size()));
		GameScene.add(Blob.seed(c, 260, InkCloud.class));
		if (Dungeon.level.heroFOV[c]) Sample.INSTANCE.play(Assets.Sounds.GAS, 0.8f, 0.6f);
		CoopLog.line("ink geyser at " + c);
	}

	// *** giant clams ***

	public static class GiantClam extends SKProps.SKProp {

		{
			spriteClass = ClamSprite.class;
			HP = HT = 1;
		}

		private boolean open = false;
		private boolean pearl = true;
		private int timer = 6;

		public int look(){
			return !open ? 0 : (pearl ? 1 : 2);
		}

		@Override
		public boolean heroShouldInteract() {
			return true;
		}

		@Override
		public boolean isInvulnerable( Class effect ) {
			return true;
		}

		@Override
		public void damage( int dmg, Object src ) {
		}

		@Override
		public boolean add( Buff buff ) {
			return false;
		}

		@Override
		protected boolean act() {
			spend(TICK);
			if (!pearl) return true;
			if (--timer > 0) return true;
			if (open){
				//shuts: bite for whoever's next to it
				open = false;
				timer = 6;
				if (sprite instanceof SKMobSprite) ((SKMobSprite) sprite).cast();
				boolean bit = false;
				for (int n : PathFinder.NEIGHBOURS8){
					Char ch = Actor.findChar(pos + n);
					if (ch != null && ch.isAlive() && !(ch instanceof SKProps.SKProp)){
						ch.damage(Math.max(1, Random.NormalIntRange(3 + Dungeon.scalingDepth()/2, 6 + Dungeon.scalingDepth()) - ch.drRoll()/2), this);
						bit = true;
					}
				}
				if (bit && Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.HIT_CRUSH);
			} else {
				open = true;
				timer = 3;
				if (Dungeon.level.heroFOV[pos]) CellEmitter.center(pos).burst(Speck.factory(Speck.BUBBLE), 4);
			}
			return true;
		}

		@Override
		public boolean interact( Char c ) {
			if (!(c instanceof Hero)) return true;
			if (!pearl){
				GLog.i(Messages.get(GiantClam.class, "empty"));
				return true;
			}
			if (!open){
				GLog.i(Messages.get(GiantClam.class, "closed"));
				return true;
			}
			pearl = false;
			Item prize = Random.Int(10) < 7 ? new Gold(15 + 5*Dungeon.depth) : Generator.random();
			Dungeon.level.drop(prize, c.pos).sprite.drop();
			Sample.INSTANCE.play(Assets.Sounds.GOLD);
			GLog.p(Messages.get(GiantClam.class, "pearl"));
			return true;
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("open", open);
			bundle.put("pearl", pearl);
			bundle.put("clam_timer", timer);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			open = bundle.getBoolean("open");
			pearl = bundle.getBoolean("pearl");
			timer = bundle.getInt("clam_timer");
		}
	}

	//kraken tentacle, pops out of the floor for a few turns
	public static class KrakenTentacle extends SKMob {

		{
			spriteClass = TentacleSprite.class;
			defenseSkill = 8;
			EXP = 2;
			maxLvl = 26;
			properties.add(Property.IMMOVABLE);
		}

		private int life = 12;

		public KrakenTentacle(){
			super();
			HP = HT = 12 + Dungeon.scalingDepth()/2;
		}

		@Override public int damageRoll() { return Random.NormalIntRange(5, 10); }
		@Override public int attackSkill( Char target ) { return 22; }
		@Override protected boolean getCloser( int target ) { return false; }
		@Override protected boolean getFurther( int target ) { return false; }

		@Override
		protected boolean act() {
			if (--life <= 0){
				if (Dungeon.level.heroFOV[pos]) CellEmitter.get(pos).burst(Speck.factory(Speck.BUBBLE), 6);
				destroy();
				if (sprite != null) sprite.die();
				return true;
			}
			return super.act();
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

	public static KrakenTentacle tentacle( int cell ){
		if (cell < 0 || Actor.findChar(cell) != null || !Dungeon.level.passable[cell] || Dungeon.level.pit[cell]) return null;
		KrakenTentacle t = new KrakenTentacle();
		t.pos = cell;
		t.state = t.HUNTING;
		GameScene.add(t);
		if (Dungeon.level.heroFOV[cell]){
			CellEmitter.get(cell).burst(Speck.factory(Speck.BUBBLE), 8);
			Sample.INSTANCE.play(Assets.Sounds.WATER, 0.8f, 0.7f);
		}
		return t;
	}

	// *** mind control ***

	//hero under a kraken minion's control acts on its own for a few turns: in coop it attacks the nearest
	//teammate, solo it walks toward the mobs. the action is picked in Hero.act from game state only so every
	//device plays the same turn. DON'T read anything local in here or coop desyncs
	public static class MindControl extends FlavourBuff {

		{
			type = buffType.NEGATIVE;
			announced = true;
		}

		public static final float DURATION = 4f;

		@Override
		public int icon() {
			return BuffIndicator.VERTIGO;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(0xB040FF);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (DURATION - visualcooldown()) / DURATION);
		}

		@Override
		public void detach() {
			super.detach();
			if (target instanceof Hero){
				((Hero) target).resting = false;
				((Hero) target).curAction = null;
			}
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", dispTurns());
		}
	}

	public static void steer( Hero h ){
		if (h.buff(MindControl.class) == null) return;
		CoopLog.line("mind controlled hero acts at " + h.pos);
		h.resting = false;
		Char target = null;
		if (Party.active()){
			for (Hero o : Party.living()){
				if (o == h) continue;
				if (target == null || Dungeon.level.distance(o.pos, h.pos) < Dungeon.level.distance(target.pos, h.pos)) target = o;
			}
			if (target != null){
				h.curAction = Dungeon.level.adjacent(h.pos, target.pos) ? new HeroAction.Attack(target) : new HeroAction.Move(target.pos);
			}
		} else {
			for (Mob m : Dungeon.level.mobs){
				if (m.alignment != Char.Alignment.ENEMY || !m.isAlive() || m instanceof UnderseaMobs.KrakenMinion) continue;
				if (target == null || Dungeon.level.distance(m.pos, h.pos) < Dungeon.level.distance(target.pos, h.pos)) target = m;
			}
			if (target != null && !Dungeon.level.adjacent(h.pos, target.pos)){
				h.curAction = new HeroAction.Move(target.pos);
			}
		}
		if (h.curAction == null){
			//nothing to go for, stands there lost. pobrecito
			h.resting = true;
		}
		if (h.sprite != null && Dungeon.level.heroFOV[h.pos]){
			h.sprite.showStatus(CharSprite.NEGATIVE, Messages.get(MindControl.class, "status"));
		}
	}

	// placement, after the mobs

	public static void decorate( Level level, ArrayList<Room> rooms ){
		boolean force = DevFlags.skUndersea();
		SKFloor floor = SKFloor.getOrMake(level);
		ArrayList<Room> spots = new ArrayList<>();
		for (Room r : rooms){
			if (r instanceof StandardRoom && r.width() >= 5 && r.height() >= 5) spots.add(r);
		}
		Random.shuffle(spots);

		//bubble column in most rooms, always one in the stairs room
		int columns = 0;
		for (Room r : spots){
			if (!r.isEntrance() && Random.Int(10) >= 6) continue;
			int c = bubbleSpot(level, r, floor);
			if (c == -1) continue;
			floor.put(level, c, SKFloor.BUBBLES);
			columns++;
		}
		//clams (almejas)
		int clams = 1 + Random.Int(2);
		for (Room r : spots){
			if (clams <= 0) break;
			if (r.isEntrance() || r.isExit()) continue;
			int c = SKProps.nearCenter(level, r);
			if (c == -1) continue;
			GiantClam g = new GiantClam();
			g.pos = c;
			level.mobs.add(g);
			clams--;
		}
		floor.inkTimer = (force ? 10 : 25) + Random.Int(25);
		CoopLog.line("undersea features depth=" + Dungeon.depth + " bubble columns=" + columns);
	}

	private static int bubbleSpot( Level level, Room r, SKFloor floor ){
		ArrayList<Integer> cells = new ArrayList<>();
		for (int y = r.top + 1; y < r.bottom; y++){
			for (int x = r.left + 1; x < r.right; x++){
				int c = x + y*level.width();
				if (!level.passable[c] || level.pit[c] || level.solid[c] || floor.kindAt(c) != 0) continue;
				if (level.findMob(c) != null || level.heaps.get(c) != null || level.traps.get(c) != null) continue;
				int k = level.map[c];
				if (k == Terrain.ENTRANCE || k == Terrain.EXIT) continue;
				cells.add(c);
			}
		}
		return cells.isEmpty() ? -1 : cells.get(Random.Int(cells.size()));
	}

	// *** sprites ***

	public static class ClamSprite extends SKMobSprite {
		public ClamSprite(){
			super();
			setup(SKAssets.CLAM, 16, 16);
		}
		@Override
		public void update() {
			if (ch instanceof GiantClam) row(((GiantClam) ch).look());
			super.update();
		}
	}

	public static class TentacleSprite extends SKMobSprite {
		public TentacleSprite(){
			super();
			setup(SKAssets.KRAKEN_TENTACLE, 16, 18);
		}
		@Override
		public int blood() {
			return 0xFFB0405A;
		}
	}
}
