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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ChampionEnemy;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.MailArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Spear;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKBulletField;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forgotten.ForgottenFeatures;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.kingdom.KingdomMobs;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//tourney rivals + prizes
public class TourneyFoes {

	// *** easy rivals: kingdom mobs on steroids ***

	public static Mob veteran( int round ){
		Mob m;
		float hp;
		switch (round){
			case 0: default: m = new KingdomMobs.Soldier(); hp = 2.5f; break;
			case 1: m = new KingdomMobs.Swordsman(); hp = 2.2f; break;
			case 2: m = new KingdomMobs.DarkKnightFoot(); hp = 1.8f; break;
		}
		m.EXP = Math.max(1, m.EXP);
		Veteran v = new Veteran();
		v.hp = hp;
		v.attachTo(m);
		return m;
	}

	//x3 hp, x1.5 dmg, gold aura. magic numbers, they just feel right
	public static class Veteran extends ChampionEnemy {

		{
			color = 0xFFD040;
			rays = 6;
		}

		public float hp = 2.2f;
		private boolean grown = false;

		@Override
		public boolean attachTo( Char target ) {
			if (super.attachTo(target)){
				if (!grown){
					grown = true;
					target.HT = Math.round(target.HT * hp);
					target.HP = target.HT;
				}
				return true;
			}
			return false;
		}

		@Override
		public float meleeDamageFactor() {
			return 1.3f;
		}

		@Override
		public float evasionAndAccuracyFactor() {
			return 1.2f;
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("hp", hp);
			bundle.put("grown", grown);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			hp = bundle.getFloat("hp");
			grown = bundle.getBoolean("grown");
		}
	}

	//angle between two cells seen from a third one. trig, sorry
	static boolean inFront( int from, int facing, int other ){
		int w = Dungeon.level.width();
		float fx = facing % w - from % w, fy = facing / w - from / w;
		float ox = other % w - from % w, oy = other / w - from / w;
		float lf = (float) Math.sqrt(fx * fx + fy * fy), lo = (float) Math.sqrt(ox * ox + oy * oy);
		if (lf == 0 || lo == 0) return false;
		return (fx * ox + fy * oy) / (lf * lo) >= 0.5f;
	}

	//Sir Bastion, tower shield guy. hit him from the sides or the back
	// I   blocks from the front, shield bash along a marked line
	// II  60%: shield wall too, marked ring around him and he can't be hurt meanwhile
	// III 35%: shield breaks. no more blocking but faster, wide sweeps
	public static class SirBastion extends MiniBoss {

		{
			spriteClass = BastionSprite.class;
			HP = HT = 100;
			defenseSkill = 10;
			EXP = 25;
			maxLvl = 16;
			lootChance = 0;
		}

		private static final int BASH = 0, WALL = 1, SWEEP = 2;
		private int facing = -1;
		private boolean walled = false;

		@Override public String barAsset() { return QuestAssets.BAR_BASTION; }
		@Override protected String music() { return Assets.Music.PRISON_TENSE; }

		@Override public int damageRoll() { return Random.NormalIntRange(4, 11 + phase); }
		@Override public int attackSkill( Char target ) { return 16; }
		@Override public int drRoll() { return super.drRoll() + Random.NormalIntRange(2, phase >= 3 ? 3 : 6); }

		@Override
		public float speed() {
			return super.speed() * (phase >= 3 ? 1.3f : 1f);
		}

		@Override
		public String status() {
			if (walled) return Messages.get(SirBastion.class, "status_wall");
			return Messages.get(SirBastion.class, "status_" + Math.min(3, phase));
		}

		@Override
		public int barColor() {
			return walled ? 0xFFB0B0C0 : phase >= 3 ? 0xFFD0602A : 0xFFB03A2A;
		}

		@Override
		protected void onStart() {
			yell(Messages.get(SirBastion.class, "notice"));
			Sample.INSTANCE.play(Assets.Sounds.HIT_PARRY, 1f, 0.7f);
		}

		@Override
		protected boolean act() {
			if (enemy != null && enemy.isAlive()) facing = enemy.pos;
			if (sprite instanceof BastionSprite) ((BastionSprite) sprite).shieldless(phase >= 3);
			return super.act();
		}

		@Override
		public boolean isInvulnerable( Class effect ) {
			return walled || super.isInvulnerable(effect);
		}

		@Override
		public void damage( int dmg, Object src ) {
			if (phase < 3 && src instanceof Char && facing != -1 && inFront(pos, facing, ((Char) src).pos)){
				dmg = Math.max(1, dmg / 4);
				if (sprite != null && Dungeon.level.heroFOV[pos]){
					Sample.INSTANCE.play(Assets.Sounds.HIT_PARRY, 0.8f, 1f);
					if (Random.Int(2) == 0) sprite.showStatus(0xC0C0C0, Messages.get(SirBastion.class, "blocked"));
				}
			}
			super.damage(dmg, src);
			if (!isAlive()) return;
			if (phase == 1 && HP * 5 <= HT * 3) newPhase(2);
			else if (phase == 2 && HP * 20 <= HT * 7){
				newPhase(3);
				yell(Messages.get(SirBastion.class, "broken"));
				Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 1.3f);
				if (Dungeon.level.heroFOV[pos]) CellEmitter.get(pos).burst(Speck.factory(Speck.ROCK), 8);
			}
		}

		@Override
		protected boolean special( Char enemy ) {
			int d = Dungeon.level.distance(pos, enemy.pos);
			if (phase == 2 && d <= 2 && Random.Int(3) == 0){
				ArrayList<Integer> ring = new ArrayList<>();
				for (int k : PathFinder.NEIGHBOURS8) ring.add(pos + k);
				walled = true;
				announce(WALL, ring, enemy.pos);
				if (sprite != null) sprite.showStatus(0xC0C0C0, Messages.get(SirBastion.class, "wall"));
				return true;
			}
			if (phase >= 3 && d == 1){
				//the 3 cells on the enemy's side
				ArrayList<Integer> arc = new ArrayList<>();
				for (int k : PathFinder.NEIGHBOURS8){
					int c = pos + k;
					if (Dungeon.level.distance(c, enemy.pos) <= 1) arc.add(c);
				}
				announce(SWEEP, arc, enemy.pos);
				return true;
			}
			if (d >= 2 && d <= 4){
				ArrayList<Integer> line = line(enemy.pos, 4);
				if (!line.contains(enemy.pos)) return false;
				announce(BASH, line, enemy.pos);
				return true;
			}
			return false;
		}

		@Override
		protected void release( int kind, ArrayList<Integer> cells, int aim ) {
			if (kind == WALL){
				walled = false;
				PixelScene.shake(2, 0.4f);
				Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG, 1f, 0.6f);
				for (Char ch : SKCombat.foesIn(this, cells)){
					ch.damage(Math.max(1, Random.NormalIntRange(6, 12) - ch.drRoll() / 2), this);
				}
				return;
			}
			if (kind == SWEEP){
				Sample.INSTANCE.play(Assets.Sounds.HIT_SLASH, 1f, 0.8f);
				for (Char ch : SKCombat.foesIn(this, cells)){
					ch.damage(Math.max(1, Random.NormalIntRange(6, 13) - ch.drRoll() / 2), this);
				}
				return;
			}
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
			if (victim != null){
				attack(victim, 1.2f, 0, 2f);
				if (victim.isAlive() && !victim.properties().contains(Property.IMMOVABLE)){
					Ballistica traj = new Ballistica(victim.pos, victim.pos + (victim.pos - pos), Ballistica.MAGIC_BOLT);
					WandOfBlastWave.throwChar(victim, traj, 2, false, true, this);
					Buff.prolong(victim, Paralysis.class, 1f);
				}
			}
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("facing", facing);
			bundle.put("walled", walled);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			facing = bundle.getInt("facing");
			walled = bundle.getBoolean("walled");
		}
	}

	public static class BastionSprite extends SKMobSprite {
		public BastionSprite(){
			super();
			setup(QuestAssets.BASTION, 22, 22);
		}
		void shieldless( boolean on ){
			row(on ? 1 : 0);
		}
	}

	/* Slime King, from the pit. jumps on the heroes (marked 3x3), spits a bubble ring,
	   sheds royal slimes at 2/3 and 1/3 hp. at 1/3 the crown falls off and he gets faster. sad */
	public static class SlimeKing extends MiniBoss {

		{
			spriteClass = SlimeKingSprite.class;
			HP = HT = 95;
			defenseSkill = 6;
			EXP = 25;
			maxLvl = 16;
			lootChance = 0;
		}

		private static final int JUMP = 0;

		@Override public String barAsset() { return QuestAssets.BAR_SLIME_KING; }
		@Override protected String music() { return Assets.Music.PRISON_TENSE; }

		@Override public int damageRoll() { return Random.NormalIntRange(4, 10 + phase); }
		@Override public int attackSkill( Char target ) { return 15; }
		@Override public int drRoll() { return super.drRoll() + Random.NormalIntRange(0, 3); }

		@Override
		public float speed() {
			return super.speed() * (phase >= 3 ? 1.25f : 0.9f);
		}

		@Override
		protected int specialCooldown() {
			return phase >= 3 ? 2 : 3;
		}

		@Override
		public String status() {
			return Messages.get(SlimeKing.class, "status_" + Math.min(3, phase));
		}

		@Override
		public int barColor() {
			return phase >= 3 ? 0xFF8AD040 : 0xFF5AB040;
		}

		@Override
		protected void onStart() {
			yell(Messages.get(SlimeKing.class, "notice"));
			Sample.INSTANCE.play(Assets.Sounds.MELD, 1f, 0.8f);
		}

		@Override
		protected boolean act() {
			if (sprite instanceof SlimeKingSprite) ((SlimeKingSprite) sprite).crownless(phase >= 3);
			return super.act();
		}

		@Override
		public void damage( int dmg, Object src ) {
			super.damage(dmg, src);
			if (!isAlive()) return;
			if (phase == 1 && HP * 3 <= HT * 2){
				newPhase(2);
				shed(2);
			} else if (phase == 2 && HP * 3 <= HT){
				newPhase(3);
				shed(2);
				yell(Messages.get(SlimeKing.class, "crown"));
			}
		}

		private void shed( int n ){
			int made = 0;
			for (int k : PathFinder.NEIGHBOURS8){
				if (made >= n) break;
				int c = pos + k;
				if (!Dungeon.level.passable[c] || Actor.findChar(c) != null) continue;
				KingdomMobs.RoyalSlime s = new KingdomMobs.RoyalSlime();
				s.summoned();
				s.pos = c;
				GameScene.add(s);
				s.state = s.HUNTING;
				if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.STEAM), 3);
				made++;
			}
			Sample.INSTANCE.play(Assets.Sounds.MELD, 1f, 1.2f);
		}

		@Override
		protected boolean special( Char enemy ) {
			int d = Dungeon.level.distance(pos, enemy.pos);
			if (d >= 2 && d <= 5 && Random.Int(3) > 0){
				ArrayList<Integer> area = new ArrayList<>();
				area.add(enemy.pos);
				for (int k : PathFinder.NEIGHBOURS8) if (!Dungeon.level.solid[enemy.pos + k]) area.add(enemy.pos + k);
				announce(JUMP, area, enemy.pos);
				if (sprite != null) sprite.showStatus(0x80FF60, Messages.get(SlimeKing.class, "jump"));
				return true;
			}
			ring(phase >= 2 ? 10 : 8, enemy.pos, 1f, ItemSpriteSheet.SHOT_BUBBLE, 3, 6);
			if (sprite instanceof SKMobSprite) ((SKMobSprite) sprite).cast();
			Sample.INSTANCE.play(Assets.Sounds.MELD, 0.8f, 1.4f);
			spend(TICK);
			return true;
		}

		@Override
		protected void release( int kind, ArrayList<Integer> cells, int aim ) {
			int land = -1;
			if (Actor.findChar(aim) == null && Dungeon.level.passable[aim]) land = aim;
			else for (int c : cells) if (Actor.findChar(c) == null && Dungeon.level.passable[c]){ land = c; break; }
			if (land != -1){
				int from = pos;
				move(land, false);
				if (sprite != null) sprite.jump(from, land, 12f, 0.25f, null);
			}
			PixelScene.shake(2, 0.4f);
			Sample.INSTANCE.play(Assets.Sounds.MELD, 1f, 0.6f);
			for (Char ch : SKCombat.foesIn(this, cells)){
				ch.damage(Math.max(1, Random.NormalIntRange(5, 11) - ch.drRoll() / 2), this);
			}
			ring(6, aim, 1f, ItemSpriteSheet.SHOT_BUBBLE, 3, 6);
		}
	}

	public static class SlimeKingSprite extends SKMobSprite {
		public SlimeKingSprite(){
			super();
			setup(QuestAssets.SLIME_KING, 28, 24);
		}
		void crownless( boolean on ){
			row(on ? 1 : 0);
		}
	}

	//Lady of the Blades, the kingdom's champion. blades circle around her, she dashes along marked lines
	//cutting everyone in the way, and parries: hit her while she's on guard and you eat a double riposte.
	//1/2 hp: two spectral swords join her. 1/4: faster
	public static class BladeDame extends MiniBoss {

		{
			spriteClass = BladeDameSprite.class;
			HP = HT = 85;
			defenseSkill = 16;
			EXP = 25;
			maxLvl = 16;
			lootChance = 0;
		}

		private static final int DASH = 0;
		private int guard = 0;

		@Override public String barAsset() { return QuestAssets.BAR_BLADE_DAME; }
		@Override protected String music() { return Assets.Music.PRISON_TENSE; }

		@Override public int damageRoll() { return Random.NormalIntRange(5, 12 + phase); }
		@Override public int attackSkill( Char target ) { return 18; }
		@Override public int drRoll() { return super.drRoll() + Random.NormalIntRange(0, 3); }

		@Override
		public float speed() {
			return super.speed() * (phase >= 3 ? 1.3f : 1f);
		}

		@Override
		public String status() {
			if (guard > 0) return Messages.get(BladeDame.class, "status_guard");
			return Messages.get(BladeDame.class, "status_" + Math.min(3, phase));
		}

		@Override
		public int barColor() {
			return guard > 0 ? 0xFFE0E0FF : 0xFF8A50C0;
		}

		@Override
		protected void onStart() {
			yell(Messages.get(BladeDame.class, "notice"));
			Sample.INSTANCE.play(Assets.Sounds.HIT_SLASH, 1f, 1.3f);
			blades(3);
		}

		private void blades( int n ){
			SKBulletField f = SKBulletField.get();
			for (int i = 0; i < n; i++){
				f.orbit(this, i * 8 / n, 1, 40, ItemSpriteSheet.SHOT_BLADE, 3, 6);
			}
		}

		@Override
		protected boolean act() {
			if (guard > 0) guard--;
			return super.act();
		}

		@Override
		public void damage( int dmg, Object src ) {
			if (guard > 0 && src instanceof Char && Dungeon.level.adjacent(pos, ((Char) src).pos) && ((Char) src).isAlive()){
				guard = 0;
				Char foe = (Char) src;
				if (sprite != null){
					sprite.showStatus(0xE0E0FF, Messages.get(BladeDame.class, "riposte"));
					//callback, or the anim ends in a second (free) hit
					sprite.attack(foe.pos, new Callback() {
						@Override
						public void call() {
							if (sprite != null) sprite.idle();
						}
					});
				}
				Sample.INSTANCE.play(Assets.Sounds.HIT_PARRY, 1f, 1.2f);
				foe.damage(Math.max(1, 2 * damageRoll() - foe.drRoll()), this);
				return;
			}
			super.damage(dmg, src);
			if (!isAlive()) return;
			if (phase == 1 && HP * 2 <= HT){
				newPhase(2);
				callSwords();
				blades(3);
			} else if (phase == 2 && HP * 4 <= HT){
				newPhase(3);
				blades(4);
			}
		}

		private void callSwords(){
			int made = 0;
			for (int k : PathFinder.NEIGHBOURS8){
				if (made >= 2) break;
				int c = pos + k * 2;
				if (c < 0 || c >= Dungeon.level.length() || !Dungeon.level.passable[c] || Actor.findChar(c) != null) continue;
				SpectralBlade b = new SpectralBlade();
				b.summoned();
				b.pos = c;
				GameScene.add(b);
				b.state = b.HUNTING;
				if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.LIGHT), 4);
				made++;
			}
			yell(Messages.get(BladeDame.class, "swords"));
		}

		@Override
		protected boolean special( Char enemy ) {
			int d = Dungeon.level.distance(pos, enemy.pos);
			if (d == 1 && Random.Int(3) == 0){
				guard = 2;
				if (sprite != null) sprite.showStatus(0xE0E0FF, Messages.get(BladeDame.class, "guard"));
				spend(TICK);
				return true;
			}
			if (d >= 2 && d <= 6){
				ArrayList<Integer> line = line(enemy.pos, 6);
				if (!line.contains(enemy.pos)) return false;
				announce(DASH, line, enemy.pos);
				return true;
			}
			return false;
		}

		@Override
		protected void release( int kind, ArrayList<Integer> cells, int aim ) {
			int start = pos;
			int last = pos;
			for (int c : cells){
				if (Dungeon.level.solid[c] || !Dungeon.level.passable[c]) break;
				Char ch = Actor.findChar(c);
				if (ch != null){
					if (SKCombat.foes(this, ch)) ch.damage(Math.max(1, Random.NormalIntRange(6, 12) - ch.drRoll() / 2), this);
					continue;
				}
				last = c;
			}
			if (last != start){
				move(last, false);
				if (sprite != null) sprite.move(start, last);
			}
			Sample.INSTANCE.play(Assets.Sounds.HIT_SLASH, 1f, 1.1f);
			if (phase >= 3 && enemy != null && enemy.isAlive() && Dungeon.level.distance(pos, enemy.pos) >= 2){
				ArrayList<Integer> line = line(enemy.pos, 6);
				if (line.contains(enemy.pos)) announce(DASH, line, enemy.pos);
			}
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("guard", guard);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			guard = bundle.getInt("guard");
		}
	}

	public static class BladeDameSprite extends SKMobSprite {
		public BladeDameSprite(){
			super();
			setup(QuestAssets.BLADE_DAME, 16, 18);
		}
	}

	public static class SpectralBlade extends SKMob {

		{
			spriteClass = SpectralBladeSprite.class;
			HP = HT = 14;
			defenseSkill = 14;
			EXP = 0;
			flying = true;
			baseSpeed = 1.5f;
			properties.add(Property.INORGANIC);
		}

		@Override public int damageRoll() { return Random.NormalIntRange(3, 7); }
		@Override public int attackSkill( Char target ) { return 18; }
	}

	public static class SpectralBladeSprite extends SKMobSprite {
		public SpectralBladeSprite(){
			super();
			setup(QuestAssets.SPECTRAL_BLADE, 12, 12);
		}
	}

	// *** prizes ***

	//heavy lance, hits from 2 cells away, knocks back sometimes
	public static class JoustingLance extends Spear {

		{
			image = ItemSpriteSheet.JOUSTING_LANCE;
			tier = 3;
		}

		@Override
		public int proc( Char attacker, Char defender, int damage ) {
			if (Random.Int(4) == 0 && defender.isAlive() && !defender.properties().contains(Char.Property.IMMOVABLE)
					&& !defender.properties().contains(Char.Property.BOSS)){
				Ballistica traj = new Ballistica(defender.pos, defender.pos + (defender.pos - attacker.pos), Ballistica.MAGIC_BOLT);
				WandOfBlastWave.throwChar(defender, traj, 2, false, true, this);
			}
			return super.proc(attacker, defender, damage);
		}
	}

	//champion armor. made to fight in, weighs less than it looks
	public static class ChampionArmor extends MailArmor {

		{
			image = ItemSpriteSheet.CHAMPION_ARMOR;
		}

		@Override
		public int STRReq( int lvl ) {
			return super.STRReq(lvl) - 1;
		}
	}

	//herald's banner. plant it and it cheers on the heroes near it
	public static class HeraldBanner extends Artifact {

		public static final String AC_PLANT = "PLANT";

		{
			image = ItemSpriteSheet.HERALD_BANNER;
			levelCap = 10;
			charge = 0;
			chargeCap = 100;
			defaultAction = AC_PLANT;
			unique = true;
		}

		void boost(){
			upgrade(2);
		}

		int turns(){
			return 8 + level() / 2;
		}

		@Override
		public ArrayList<String> actions( Hero hero ) {
			ArrayList<String> actions = super.actions(hero);
			if (isEquipped(hero) && !cursed) actions.add(AC_PLANT);
			return actions;
		}

		@Override
		public void execute( Hero hero, String action ) {
			super.execute(hero, action);
			if (!action.equals(AC_PLANT)) return;
			if (!isEquipped(hero)){
				GLog.i(Messages.get(Artifact.class, "need_to_equip"));
				return;
			}
			if (charge < chargeCap){
				GLog.i(Messages.get(this, "no_charge"));
				return;
			}
			int cell = -1;
			for (int n : PathFinder.NEIGHBOURS8){
				int c = hero.pos + n;
				if (Dungeon.level.passable[c] && !Dungeon.level.pit[c] && Actor.findChar(c) == null){
					cell = c;
					break;
				}
			}
			if (cell == -1){
				GLog.i(Messages.get(this, "no_room"));
				return;
			}
			charge = 0;
			PlantedBanner b = new PlantedBanner();
			b.pos = cell;
			b.left = turns();
			b.power = level();
			GameScene.add(b);
			Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG, 0.8f, 1.2f);
			GLog.p(Messages.get(this, "planted"));
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
			return new Cheer();
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
			String desc = Messages.get(this, "desc", turns(), 20 + 2 * level());
			if (isEquipped(Dungeon.hero)) desc += "\n\n" + Messages.get(this, charge >= chargeCap ? "desc_ready" : "desc_charging");
			return desc;
		}

		public class Cheer extends ArtifactBuff {
			@Override
			public boolean act() {
				if (charge < chargeCap && !cursed && target instanceof Hero && ((Hero) target).visibleEnemies() > 0){
					partialCharge += 1.4f + 0.15f * level();
					while (partialCharge >= 1 && charge < chargeCap){
						partialCharge--;
						charge++;
					}
					if (charge >= chargeCap) GLog.p(Messages.get(HeraldBanner.class, "ready"));
					updateQuickslot();
				}
				spend(TICK);
				return true;
			}
		}
	}

	//planted banner: heroes + allies within 3 cells hit harder and take less
	public static class PlantedBanner extends ForgottenFeatures.Thing {

		{
			spriteClass = PlantedBannerSprite.class;
		}

		public int left = 8;
		public int power = 0;

		public static float dealtFactor( Char ch ){
			return near(ch) == null ? 1f : 1.2f + 0.02f * near(ch).power;
		}

		public static float takenFactor( Char ch ){
			return near(ch) == null ? 1f : 0.85f;
		}

		private static PlantedBanner near( Char ch ){
			if (Dungeon.level == null || ch.alignment != Char.Alignment.ALLY) return null;
			for (Mob m : Dungeon.level.mobs){
				if (m instanceof PlantedBanner && Dungeon.level.distance(m.pos, ch.pos) <= 3) return (PlantedBanner) m;
			}
			return null;
		}

		@Override
		protected boolean act() {
			if (--left <= 0){
				if (Dungeon.level.heroFOV[pos]) CellEmitter.get(pos).burst(Speck.factory(Speck.SMOKE), 4);
				destroy();
				if (sprite != null) sprite.killAndErase();
				return true;
			}
			if (sprite != null && Dungeon.level.heroFOV[pos] && left % 2 == 0) sprite.emitter().burst(Speck.factory(Speck.STAR), 1);
			spend(TICK);
			return true;
		}

		@Override
		public String description() {
			return Messages.get(this, "desc", left);
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("left", left);
			bundle.put("power", power);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			left = bundle.getInt("left");
			power = bundle.getInt("power");
		}
	}

	public static class PlantedBannerSprite extends ForgottenFeatures.StripSprite {
		public PlantedBannerSprite(){
			super();
			strip(QuestAssets.HERALD_BANNER_PLANTED, 16, 24, 4);
		}
	}
}
