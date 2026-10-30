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
import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MindVision;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKWorlds;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forgotten.ForgottenFeatures;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.props.SKProps;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.starship.Hive;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.starship.StarshipBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.starship.StarshipFeatures;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.starship.StarshipMobs;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndQuest;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/*
 * M-4 quest. starship, floor 12 or 13, takes the blacksmith's spot
 *
 * the ship monsters want to boot up M-4 (huge maintenance bot) and sic it on the heroes.
 * boot starts when a hero walks into its room, more guards alive = faster boot.
 * break the core first and it's off for good + the ship shows where every mob on the floor is.
 * too slow and M-4 wakes up: unkillable, stairs lock, go find the emergency hatch somewhere on the floor
 *
 * shut down -> ship helps vs the varkolyn queen (exact radar, airlock recharges sooner)
 * woke up -> queen's hive starts bigger. so yeah, hurry
 */
public class AndroidQuest {

	public static class State extends QuestState {
		public float boot = 0;       //percent, 0..100
		public boolean running = false;
		public boolean awake = false;
		public int hatch = -1;

		public State(){
			super("quest_android");
		}

		@Override
		public void reset() {
			super.reset();
			boot = 0;
			running = awake = false;
			hatch = -1;
		}

		@Override
		protected void storeMore( Bundle b ) {
			b.put("boot", boot);
			b.put("running", running);
			b.put("awake", awake);
			b.put("hatch", hatch);
		}

		@Override
		protected void restoreMore( Bundle b ) {
			boot = b.getFloat("boot");
			running = b.getBoolean("running");
			awake = b.getBoolean("awake");
			hatch = b.contains("hatch") ? b.getInt("hatch") : -1;
		}

		@Override
		public boolean punish() {
			return awake;
		}
	}

	public static final State Q = SKQuests.ANDROID;

	//master switch. art + test are done, leave it on
	public static boolean READY = true;

	//-Dspd.test.androidboot=N -> boot starts at N%, so you can watch M-4 wake up w/o waiting forever
	private static final int TEST_BOOT = DevFlags.androidBoot();

	public static ArrayList<Room> rooms( ArrayList<Room> rooms ){
		if (!READY || Q.depth != -1 || DevFlags.noQuests()) return rooms;
		if (Dungeon.depth == 12 && Random.Int(2) == 0 || Dungeon.depth == 13){
			rooms.add(new MaintenanceRoom());
		}
		return rooms;
	}

	public static MaintenanceRoom room( Level level ){
		if (!(level instanceof RegularLevel) || ((RegularLevel) level).rooms() == null) return null;
		for (Room r : ((RegularLevel) level).rooms()){
			if (r instanceof MaintenanceRoom) return (MaintenanceRoom) r;
		}
		return null;
	}

	//after the room is painted: terminal, core, M-4 and its guards
	public static void place( Level level ){
		MaintenanceRoom r = room(level);
		if (r == null || Q.depth != -1) return;
		Q.depth = Dungeon.depth;
		int w = level.width();
		int cx = (r.left + r.right) / 2, cy = (r.top + r.bottom) / 2;

		AndroidM4 m4 = new AndroidM4();
		m4.pos = r.dock(level);
		level.mobs.add(m4);

		Core core = new Core();
		core.pos = cx + cy * w;
		level.mobs.add(core);

		Terminal t = new Terminal();
		t.pos = r.terminalCell(level);
		level.mobs.add(t);

		int[][] guards = {{-3, -2}, {3, -2}, {-3, 2}, {3, 2}, {-1, 2}, {1, -2}};
		//3 guards solo, +1 por cada hero extra
		int want = Math.min(guards.length, 2 + Math.max(1, heroes().size()));
		int i = 0;
		for (int[] g : guards){
			if (i >= want) break;
			int c = cx + g[0] + (cy + g[1]) * w;
			if (!level.passable[c] || level.findMob(c) != null) continue;
			Mob m = i % 3 == 2 ? new StarshipMobs.Varkolyn() : (i % 2 == 0 ? new StarshipMobs.AlienSword() : new StarshipMobs.AlienLaser());
			m.pos = c;
			m.state = m.SLEEPING;
			level.mobs.add(m);
			i++;
		}
		CoopLog.line("android placed depth=" + Dungeon.depth + " room " + r.left + "," + r.top);
	}

	//guards alive keep the boot going
	static int guards( Level level, MaintenanceRoom r ){
		int n = 0;
		for (Mob m : level.mobs){
			if (m.alignment == Char.Alignment.ENEMY && m.isAlive() && !(m instanceof Core) && !(m instanceof AndroidM4)
					&& r.inside(level.cellToPoint(m.pos))) n++;
		}
		return n;
	}

	static AndroidM4 android( Level level ){
		for (Mob m : level.mobs){
			if (m instanceof AndroidM4) return (AndroidM4) m;
		}
		return null;
	}

	static void shutDown(){
		if (Q.done || Q.awake) return;
		Q.done = true;
		Q.running = false;
		Q.bonus = Q.boot < 50;
		//kill the bar + fight music. silencio
		AndroidM4 m4 = android(Dungeon.level);
		if (m4 != null) m4.started = false;
		Dungeon.level.playLevelMusic();
		for (Hero h : heroes()) Buff.affect(h, MindVision.class, 400f);
		GLog.p(Messages.get(AndroidQuest.class, "shutdown"));
		MiniBossHud.banner(Messages.get(AndroidQuest.class, "banner_shutdown"));
		Sample.INSTANCE.play(Assets.Sounds.MASTERY);
		CoopLog.line("android shut down at " + (int) Q.boot + "%");
	}

	static void wake( Level level ){
		if (Q.awake || Q.done) return;
		Q.awake = true;
		Q.running = false;
		for (Mob m : level.mobs.toArray(new Mob[0])){
			if (m instanceof AndroidM4) ((AndroidM4) m).activate();
			if (m instanceof Core){
				m.destroy();
				if (m.sprite != null) m.sprite.killAndErase();
			}
		}
		//stairs lock, a hatch opens somewhere. reachable, far from the bay but not at the other end of the ship (that was mean)
		int from = heroes().isEmpty() ? level.entrance() : heroes().get(0).pos;
		MaintenanceRoom bay = room(level);
		PathFinder.buildDistanceMap(from, level.passable);
		ArrayList<Integer> far = new ArrayList<>(), near = new ArrayList<>();
		for (int c = 0; c < level.length(); c++){
			int d = PathFinder.distance[c];
			if (d == Integer.MAX_VALUE || d < 10) continue;
			if (level.solid[c] || level.findMob(c) != null || level.heaps.get(c) != null) continue;
			if (level.map[c] != Terrain.EMPTY && level.map[c] != Terrain.EMPTY_DECO) continue;
			if (bay != null && bay.inside(level.cellToPoint(c))) continue;
			if (d >= 18 && d <= 36) far.add(c);
			else near.add(c);
		}
		int best = !far.isEmpty() ? Random.element(far) : !near.isEmpty() ? Random.element(near) : -1;
		int steps = best == -1 ? -1 : PathFinder.distance[best];
		if (best != -1){
			LevelTransition exit = level.getTransition(LevelTransition.Type.REGULAR_EXIT);
			if (exit != null){
				int ec = exit.cell();
				level.transitions.remove(exit);
				Level.set(ec, Terrain.LOCKED_EXIT, level);
				GameScene.updateMap(ec);
			}
			Q.hatch = best;
			Level.set(best, Terrain.EXIT, level);
			level.transitions.add(new LevelTransition(level, best, LevelTransition.Type.REGULAR_EXIT));
			GameScene.updateMap(best);
		}
		GLog.n(Messages.get(AndroidQuest.class, "awake"));
		MiniBossHud.banner(Messages.get(AndroidQuest.class, "banner_awake"));
		PixelScene.shake(4, 1f);
		Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 1f, 0.6f);
		CoopLog.line("android awake, hatch at " + best + " (" + steps + " steps)");
	}

	static ArrayList<Hero> heroes(){
		ArrayList<Hero> l = new ArrayList<>();
		if (Party.active()) l.addAll(Party.living());
		else if (Dungeon.hero != null && Dungeon.hero.isAlive()) l.add(Dungeon.hero);
		return l;
	}

	public static boolean helps(){
		return Q.done;
	}

	public static void queenFight( Mob queen ){
		if (Q.done){
			GLog.p(Messages.get(AndroidQuest.class, "ship_helps"));
			CoopLog.line("android: the ship helps against the queen");
			return;
		}
		if (!Q.awake || Q.failed) return;
		Q.failed = true;
		Hive h = Hive.getOrMake(Dungeon.level);
		int made = 0;
		for (int r = 1; r <= 4 && made < 14; r++){
			for (int k : PathFinder.NEIGHBOURS8){
				int c = queen.pos + k * r;
				if (c < 0 || c >= Dungeon.level.length() || Dungeon.level.solid[c]) continue;
				if (Dungeon.level instanceof StarshipBossLevel && !((StarshipBossLevel) Dungeon.level).inBridge(c)) continue;
				h.put(Dungeon.level, c);
				made++;
			}
		}
		GLog.n(Messages.get(AndroidQuest.class, "hive_grown"));
		CoopLog.line("android: the hive of the queen grows by " + made + " cells");
	}

	// ===== ship terminal: gives the quest, the rewards, and runs the boot =====

	public static class Terminal extends NPC {

		{
			spriteClass = StarshipFeatures.ConsoleSprite.class;
			properties.add(Property.IMMOVABLE);
		}

		@Override
		protected boolean act() {
			MaintenanceRoom r = room(Dungeon.level);
			if (r != null && !Q.done && !Q.awake){
				if (!Q.running){
					for (Hero h : heroes()){
						if (r.inside(Dungeon.level.cellToPoint(h.pos))){
							Q.running = true;
							Q.boot = Math.max(Q.boot, TEST_BOOT);
							GLog.w(Messages.get(AndroidQuest.class, "boot_start"));
							//bar = boot %
							AndroidM4 m4 = android(Dungeon.level);
							if (m4 != null) m4.startFight();
							for (Mob m : Dungeon.level.mobs){
								if (m.alignment == Alignment.ENEMY && r.inside(Dungeon.level.cellToPoint(m.pos))
										&& !(m instanceof Core) && !(m instanceof AndroidM4)) m.beckon(h.pos);
							}
							break;
						}
					}
				}
				if (Q.running){
					Q.boot += 1f + 0.7f * guards(Dungeon.level, r);
					if (Q.boot >= 100){
						Q.boot = 100;
						wake(Dungeon.level);
					}
				}
			}
			//radar finds the hatch once someone gets close (si no, good luck finding it)
			if (Q.awake && Q.hatch != -1 && !Dungeon.level.mapped[Q.hatch]){
				for (Hero h : heroes()){
					if (Dungeon.level.distance(h.pos, Q.hatch) <= 12){
						Dungeon.level.mapped[Q.hatch] = true;
						GameScene.updateFog();
						GLog.p(Messages.get(AndroidQuest.class, "hatch_found"));
						break;
					}
				}
			}
			spend(TICK);
			return true;
		}

		@Override
		public int defenseSkill( Char enemy ) {
			return INFINITE_EVASION;
		}

		@Override
		public void damage( int dmg, Object src ) {
		}

		@Override
		public boolean add( Buff buff ) {
			return false;
		}

		@Override
		public boolean isInvulnerable( Class effect ) {
			return true;
		}

		@Override
		public boolean reset() {
			return true;
		}

		@Override
		public boolean interact( Char c ) {
			if (!(c instanceof Hero)) return true;
			final Hero h = (Hero) c;
			if (!Q.given){
				Q.given = true;
				Game.runOnRenderThread(new Callback() {
					@Override
					public void call() {
						GameScene.show(new WndQuest(Terminal.this, Messages.get(Terminal.class, "intro")));
					}
				});
				GLog.p(Messages.get(Terminal.class, "intro_hint"));
				return true;
			}
			if (Q.awake){
				yell(Messages.get(Terminal.class, "run"));
				return true;
			}
			if (!Q.done){
				yell(Messages.get(Terminal.class, Q.running ? "hurry" : "hint"));
				return true;
			}
			if (Q.claimedBy(h)){
				yell(Messages.get(Terminal.class, "after"));
				return true;
			}
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					reward(h);
				}
			});
			return true;
		}

		private void reward( final Hero h ){
			GameScene.show(new WndOptions(
					Messages.titleCase(name()),
					Messages.get(Terminal.class, Q.bonus ? "reward_bonus" : "reward"),
					Messages.get(HydraulicFist.class, "name"),
					Messages.get(SpareCell.class, "name"),
					Messages.get(PlasmaShield.class, "name")){
				@Override
				protected void onSelect( int index ) {
					if (Q.claimedBy(h)) return;
					Item item;
					switch (index){
						case 0: default: item = new HydraulicFist(); break;
						case 1: item = new SpareCell(); break;
						case 2: item = new PlasmaShield(); break;
					}
					if (item.isUpgradable() && !(item instanceof Artifact)) item.upgrade(Q.bonus ? 2 : 1);
					else if (item instanceof Artifact && Q.bonus) item.upgrade(2);
					item.identify();
					Q.claim(h);
					if (!item.collect(h.belongings.backpack)) Dungeon.level.drop(item, h.pos).sprite.drop();
					GLog.p(Messages.get(Terminal.class, "got", item.name()));
					CoopLog.line("android reward " + item.getClass().getSimpleName() + " for hero " + ChampionHooks.indexOf(h));
				}
				@Override
				public void onBackPressed() {
					//gotta pick one
				}
			});
		}

		@Override
		public String description() {
			if (Q.running && !Q.done && !Q.awake) return Messages.get(this, "desc_boot", (int) Q.boot);
			return Messages.get(this, "desc");
		}
	}

	// *** M-4 core, smash it ***

	public static class Core extends SKProps.SKProp {

		{
			spriteClass = CoreSprite.class;
			alignment = Alignment.ENEMY;
			HP = HT = 40;
		}

		public Core(){
			super();
			HP = HT = 30 + 15 * Math.max(1, heroes().size());
		}

		@Override
		public void die( Object cause ) {
			int at = pos;
			super.die(cause);
			if (Dungeon.level.heroFOV[at]) CellEmitter.center(at).burst(Speck.factory(Speck.STEAM), 10);
			Sample.INSTANCE.play(Assets.Sounds.BLAST, 0.8f, 1.2f);
			shutDown();
		}

		@Override
		public String description() {
			return Messages.get(this, "desc");
		}
	}

	public static class CoreSprite extends ForgottenFeatures.StripSprite {
		public CoreSprite(){
			super();
			strip(QuestAssets.ANDROID_CORE, 16, 16, 6);
		}
	}

	//sleeps on its dock until it boots. awake it can't be hurt, chases the closest hero and every hit takes 1/4 of a hero's hp.
	//every few turns it rushes down a marked line
	public static class AndroidM4 extends MiniBoss {

		{
			spriteClass = AndroidSprite.class;
			HP = HT = 999;
			defenseSkill = 0;
			EXP = 0;
			state = PASSIVE;
			properties.add(Property.INORGANIC);
		}

		private static final int RUSH = 0;

		void activate(){
			state = HUNTING;
			startFight();
			yell(Messages.get(AndroidM4.class, "notice"));
		}

		@Override public String barAsset() { return QuestAssets.BAR_ANDROID; }
		@Override protected String music() { return Assets.Music.CAVES_TENSE; }

		@Override
		public String subtitle() {
			return Q.awake ? super.subtitle() : Messages.get(AndroidQuest.class, "boot_sub");
		}

		@Override
		public String status() {
			return Messages.get(AndroidM4.class, Q.awake ? "status_awake" : "status_boot", (int) Q.boot);
		}

		@Override
		public float barFill() {
			return Q.awake ? 1f : Q.boot / 100f;
		}

		@Override
		public int barColor() {
			return 0xFFE04040;
		}

		@Override
		public void notice() {
			if (Q.awake) super.notice();
		}

		@Override
		protected boolean act() {
			if (!Q.awake){
				spend(TICK);
				return true;
			}
			return super.act();
		}

		@Override
		public boolean isInvulnerable( Class effect ) {
			return true;
		}

		@Override
		public void damage( int dmg, Object src ) {
			if (sprite != null && Q.awake && Dungeon.level.heroFOV[pos]) sprite.showStatus(0xC0C0C0, Messages.get(AndroidM4.class, "immune"));
		}

		@Override
		public int damageRoll() {
			Char e = enemy;
			int base = e == null ? 20 : Math.max(12, e.HT / 4);
			return Random.NormalIntRange(base * 4 / 5, base);
		}

		@Override
		public int attackSkill( Char target ) {
			return 30;
		}

		@Override
		public int attackProc( Char enemy, int damage ) {
			//no stun vs a wall. hit + stun + hit again = no way out, tested it, it was awful
			if (enemy.isAlive() && !enemy.properties().contains(Property.IMMOVABLE)){
				Ballistica traj = new Ballistica(enemy.pos, enemy.pos + (enemy.pos - pos), Ballistica.MAGIC_BOLT);
				WandOfBlastWave.throwChar(enemy, traj, 2, false, false, this);
			}
			return super.attackProc(enemy, damage);
		}

		@Override
		protected int specialCooldown() {
			return 5;
		}

		@Override
		protected boolean special( Char enemy ) {
			int d = Dungeon.level.distance(pos, enemy.pos);
			if (d < 3 || d > 6) return false;
			ArrayList<Integer> line = line(enemy.pos, 6);
			if (!line.contains(enemy.pos)) return false;
			announce(RUSH, line, enemy.pos);
			return true;
		}

		@Override
		protected void release( int kind, ArrayList<Integer> cells, int aim ) {
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
			Sample.INSTANCE.play(Assets.Sounds.BLAST, 0.6f, 0.7f);
			if (victim != null) attack(victim, 1f, 0, 2f);
		}

		@Override
		public String description() {
			return Messages.get(this, Q.awake ? "desc_awake" : "desc");
		}
	}

	public static class AndroidSprite extends SKMobSprite {
		public AndroidSprite(){
			super();
			setup(QuestAssets.ANDROID, 24, 26);
		}
	}

	// *** rewards ***

	//hydraulic fist. heavy hits, knocks back now and then
	public static class HydraulicFist extends MeleeWeapon {

		{
			image = ItemSpriteSheet.HYDRAULIC_FIST;
			hitSound = Assets.Sounds.HIT_CRUSH;
			hitSoundPitch = 0.9f;
			tier = 3;
		}

		@Override
		public int proc( Char attacker, Char defender, int damage ) {
			if (Random.Int(10) < 3 && defender.isAlive() && !defender.properties().contains(Char.Property.IMMOVABLE)
					&& !defender.properties().contains(Char.Property.BOSS)){
				Ballistica traj = new Ballistica(defender.pos, defender.pos + (defender.pos - attacker.pos), Ballistica.MAGIC_BOLT);
				WandOfBlastWave.throwChar(defender, traj, 2, false, true, this);
			}
			return super.proc(attacker, defender, damage);
		}
	}

	//M-4's spare power cell. plug it in and every wand/gun the bearer has gets a charge
	public static class SpareCell extends Artifact {

		public static final String AC_PLUG = "PLUG";

		{
			image = ItemSpriteSheet.SPARE_CELL;
			levelCap = 10;
			charge = 0;
			chargeCap = 100;
			defaultAction = AC_PLUG;
			unique = true;
		}

		@Override
		public ArrayList<String> actions( Hero hero ) {
			ArrayList<String> actions = super.actions(hero);
			if (isEquipped(hero) && !cursed) actions.add(AC_PLUG);
			return actions;
		}

		@Override
		public void execute( Hero hero, String action ) {
			super.execute(hero, action);
			if (!action.equals(AC_PLUG)) return;
			if (!isEquipped(hero)){
				GLog.i(Messages.get(Artifact.class, "need_to_equip"));
				return;
			}
			if (charge < chargeCap){
				GLog.i(Messages.get(this, "no_charge"));
				return;
			}
			charge = 0;
			int n = 1 + level() / 4;
			for (Item i : hero.belongings){
				if (i instanceof Wand){
					Wand w = (Wand) i;
					w.curCharges = Math.min(w.maxCharges, w.curCharges + n);
					w.updateQuickslot();
				}
			}
			hero.sprite.emitter().burst(Speck.factory(Speck.UP), 6);
			Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);
			GLog.p(Messages.get(this, "plugged", n));
			exp++;
			if (exp >= level() + 2 && level() < levelCap){
				exp = 0;
				upgrade();
				GLog.p(Messages.get(this, "levelup"));
			}
			updateQuickslot();
			hero.spendAndNext(1f);
		}

		@Override
		protected ArtifactBuff passiveBuff() {
			return new Trickle();
		}

		@Override
		public void charge( Hero target, float amount ) {
			if (charge < chargeCap){
				charge = Math.min(chargeCap, charge + Math.round(3 * amount));
				updateQuickslot();
			}
		}

		@Override
		public String desc() {
			String desc = Messages.get(this, "desc", 1 + level() / 4);
			if (isEquipped(Dungeon.hero)) desc += "\n\n" + Messages.get(this, charge >= chargeCap ? "desc_ready" : "desc_charging");
			return desc;
		}

		public class Trickle extends ArtifactBuff {
			@Override
			public boolean act() {
				if (charge < chargeCap && !cursed){
					partialCharge += 0.4f + 0.05f * level();
					while (partialCharge >= 1 && charge < chargeCap){
						partialCharge--;
						charge++;
					}
					if (charge >= chargeCap) GLog.p(Messages.get(SpareCell.class, "ready"));
					updateQuickslot();
				}
				spend(TICK);
				return true;
			}
		}
	}

	//shield projector, wraps you in plasma while it's on. very sci-fi
	public static class PlasmaShield extends Artifact {

		public static final String AC_RAISE = "RAISE";

		{
			image = ItemSpriteSheet.PLASMA_SHIELD;
			levelCap = 10;
			charge = 0;
			chargeCap = 100;
			defaultAction = AC_RAISE;
			unique = true;
		}

		int shield(){
			return 12 + 3 * level();
		}

		@Override
		public ArrayList<String> actions( Hero hero ) {
			ArrayList<String> actions = super.actions(hero);
			if (isEquipped(hero) && !cursed) actions.add(AC_RAISE);
			return actions;
		}

		@Override
		public void execute( Hero hero, String action ) {
			super.execute(hero, action);
			if (!action.equals(AC_RAISE)) return;
			if (!isEquipped(hero)){
				GLog.i(Messages.get(Artifact.class, "need_to_equip"));
				return;
			}
			if (charge < chargeCap){
				GLog.i(Messages.get(this, "no_charge"));
				return;
			}
			charge = 0;
			Buff.affect(hero, Barrier.class).setShield(shield());
			hero.sprite.emitter().burst(Speck.factory(Speck.LIGHT), 6);
			Sample.INSTANCE.play(Assets.Sounds.CHARGEUP, 1f, 1.4f);
			GLog.p(Messages.get(this, "raised"));
			exp++;
			if (exp >= level() + 2 && level() < levelCap){
				exp = 0;
				upgrade();
				GLog.p(Messages.get(this, "levelup"));
			}
			updateQuickslot();
			hero.spendAndNext(1f);
		}

		@Override
		protected ArtifactBuff passiveBuff() {
			return new Hum();
		}

		@Override
		public void charge( Hero target, float amount ) {
			if (charge < chargeCap){
				charge = Math.min(chargeCap, charge + Math.round(3 * amount));
				updateQuickslot();
			}
		}

		@Override
		public String desc() {
			String desc = Messages.get(this, "desc", shield());
			if (isEquipped(Dungeon.hero)) desc += "\n\n" + Messages.get(this, charge >= chargeCap ? "desc_ready" : "desc_charging");
			return desc;
		}

		public class Hum extends ArtifactBuff {
			@Override
			public boolean act() {
				if (charge < chargeCap && !cursed && target instanceof Hero && ((Hero) target).visibleEnemies() > 0){
					partialCharge += 1.6f + 0.15f * level();
					while (partialCharge >= 1 && charge < chargeCap){
						partialCharge--;
						charge++;
					}
					if (charge >= chargeCap) GLog.p(Messages.get(PlasmaShield.class, "ready"));
					updateQuickslot();
				}
				spend(TICK);
				return true;
			}
		}
	}

	//true on M-4's floor (for the music)
	public static boolean here(){
		return SKWorlds.isSK(Dungeon.depth) && room(Dungeon.level) != null;
	}
}
