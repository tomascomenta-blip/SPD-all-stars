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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.finale;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Amok;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corruption;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicalSleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Sleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.AntiOP;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMob;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKWorlds;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.finale.echo.EchoBoss;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.props.SKProps;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.HeroSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

//everything of his that touches the floor. none of it has real hp of its own except the portals
//and the hourglasses: hands and core just pass the hit up to him
public class ThalParts {

	//colour of the time light per world. 0 = all of them mixed, 6 = the exit
	public static int worldColor( int world ){
		switch (world){
			case SKWorlds.FOREST:    return 0x6ED45A;
			case SKWorlds.KINGDOM:   return 0xA272E8;
			case SKWorlds.STARSHIP:  return 0x4AE6EE;
			case SKWorlds.UNDERSEA:  return 0x4A86F0;
			case SKWorlds.FORGOTTEN: return 0xF2A436;
			case 6:                  return 0xFFE08A;
			default:                 return 0xDDF4FF;
		}
	}

	static boolean fromParty( Object src ){
		if (src instanceof Char) return ((Char) src).alignment == Char.Alignment.ALLY;
		//wands, bombs, fire... all the heroes' doing in here
		return src != null && !(src instanceof ThalVekhorr);
	}

	public static abstract class Part extends SKMob {

		{
			alignment = Alignment.ENEMY;
			state = PASSIVE;
			EXP = 0;
			defenseSkill = 0;
			properties.add(Property.IMMOVABLE);
			properties.add(Property.INORGANIC);
			properties.add(Property.STATIC);

			immunities.add(Paralysis.class);
			immunities.add(Frost.class);
			immunities.add(Chill.class);
			immunities.add(Slow.class);
			immunities.add(Sleep.class);
			immunities.add(MagicalSleep.class);
			immunities.add(Terror.class);
			immunities.add(Amok.class);
			immunities.add(Charm.class);
			immunities.add(Corruption.class);
			immunities.add(Vertigo.class);
			immunities.add(ThalCall.Stasis.class);
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
		public int attackSkill( Char target ) {
			return 0;
		}

		@Override
		public void beckon( int cell ) {
		}

		@Override
		public float spawningWeight() {
			return 0;
		}

		@Override
		public void notice() {
		}

		@Override
		public String description() {
			return Messages.get(this, "desc");
		}

		//gone without a death (act over, fight over)
		public void vanish(){
			if (sprite != null){
				CellEmitter.get(pos).burst(Speck.factory(Speck.LIGHT), 6);
				sprite.killAndErase();
			}
			destroy();
		}
	}

	// *** hands ***

	public static class Hand extends Part {

		{
			spriteClass = HandSprite.class;
			HP = HT = 10000;
			properties.add(Property.LARGE);
		}

		public int life = 5;
		public boolean left = false;

		@Override
		protected void onAdd() {
			boolean first = firstAdded;
			super.onAdd();
			if (first) HP = HT = 10000;
		}

		@Override
		protected boolean act() {
			life--;
			if (life <= 0){
				lift();
				return true;
			}
			//it drips while it rests on the grass
			if (life % 2 == 0 && Dungeon.level instanceof RiftLevel){
				RiftLevel L = (RiftLevel) Dungeon.level;
				int c = pos + PathFinder.NEIGHBOURS8[Random.Int(8)];
				if (L.floor(c) && Actor.findChar(c) == null) L.addPuddle(c, 25);
			}
			spend(TICK);
			return true;
		}

		public void lift(){
			if (sprite instanceof HandSprite) ((HandSprite) sprite).lift();
			else if (sprite != null) sprite.killAndErase();
			destroy();
		}

		@Override
		public void damage( int dmg, Object src ) {
			if (!isAlive() || dmg <= 0 || !fromParty(src)) return;
			ThalVekhorr t = ThalVekhorr.find();
			if (t == null) return;
			dmg = ThalVekhorr.summonShare(src, AntiOP.cap(this, src, dmg));
			int pass = Math.max(1, Math.round(dmg * ThalVekhorr.HAND_PASS));
			if (sprite != null){
				sprite.showStatus(CharSprite.NEGATIVE, Integer.toString(pass));
				sprite.flash();
			}
			t.hurt(pass);
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("life", life);
			bundle.put("left", left);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			life = bundle.getInt("life");
			left = bundle.getBoolean("left");
		}
	}

	// *** portals ***

	public static class Portal extends Part {

		{
			spriteClass = PortalSprite.class;
			HP = HT = 90;
		}

		public int world = SKWorlds.FOREST;
		public boolean echo = false;
		public int timer = 3;
		private boolean echoOut = false;
		private boolean echoDead = false;
		private ArrayList<Integer> kids = new ArrayList<>();

		private int alive(){
			int n = 0;
			for (int i = kids.size() - 1; i >= 0; i--){
				Actor a = Actor.findById(kids.get(i));
				if (a instanceof Char && ((Char) a).isAlive()) n++;
				else kids.remove(i);
			}
			return n;
		}

		private int cap(){
			ThalVekhorr t = ThalVekhorr.find();
			int cap = t != null && t.currentAct() >= 6 ? 2 : 3;
			//alone, a portal only sends as many as there are to face them
			if (ThalVekhorr.alone()) cap = Math.max(1, Math.min(cap, ThalVekhorr.alliesOnField()));
			return cap;
		}

		@Override
		protected boolean act() {
			//the echo broke: that one hurt him
			if (echo && echoOut && !echoDead && !kids.isEmpty() && !Echoes.alive(kids.get(0))){
				echoDead = true;
				ThalVekhorr t = ThalVekhorr.find();
				if (t != null) t.echoBroken();
			}
			if (--timer <= 0){
				if (echo){
					if (!echoOut){
						echoOut = true;
						int c = exitCell();
						if (c != -1){
							Mob e = Echoes.make(world);
							e.pos = c;
							GameScene.add(e);
							float size = ThalVekhorr.echoSize();
							if (size < 1f){
								e.HT = Math.max(1, Math.round(e.HT * size));
								e.HP = e.HT;
							}
							if (e instanceof EchoBoss) ((EchoBoss) e).fade = ThalVekhorr.echoLife();
							Echoes.emerge(e);
							kids.add(e.id());
						}
					}
					timer = 999;
				} else {
					ThalVekhorr t = ThalVekhorr.find();
					timer = t == null ? 6 : t.portalInterval();
					if (alive() < cap()) spawn();
				}
			}
			spend(TICK);
			return true;
		}

		private int exitCell(){
			ArrayList<Integer> c = new ArrayList<>();
			for (int n : PathFinder.NEIGHBOURS8){
				int x = pos + n;
				if (Dungeon.level.passable[x] && Actor.findChar(x) == null) c.add(x);
			}
			return c.isEmpty() ? -1 : Random.element(c);
		}

		public boolean owns( int id ){
			return kids.contains(id);
		}

		//its echo went back to its own time: that one doesn't hurt him
		public void echoFaded(){
			echoDead = true;
		}

		private void spawn(){
			int c = exitCell();
			if (c == -1) return;
			int w = world == 0 ? Random.IntRange(SKWorlds.FOREST, SKWorlds.FORGOTTEN) : world;
			ArrayList<Class<? extends Mob>> rot = SKWorlds.rotation(w * 5 - 1);
			if (rot == null || rot.isEmpty()) return;
			Mob m = Reflection.newInstance(Random.element(rot));
			if (m == null) return;
			m.pos = c;
			m.state = m.HUNTING;
			GameScene.add(m, 1f);
			if (m.sprite != null) CellEmitter.get(c).burst(Speck.factory(Speck.LIGHT), 4);
			kids.add(m.id());
			if (sprite instanceof PortalSprite) ((PortalSprite) sprite).pulse();
		}

		//act over: the portal swallows back whatever came out of it
		public void collapse(){
			for (int id : kids){
				Actor a = Actor.findById(id);
				if (a instanceof Mob && ((Mob) a).isAlive()){
					Mob m = (Mob) a;
					if (m.sprite != null){
						CellEmitter.get(m.pos).burst(Speck.factory(Speck.LIGHT), 4);
						m.sprite.killAndErase();
					}
					m.destroy();
				}
			}
			kids.clear();
			if (sprite instanceof PortalSprite){
				CellEmitter.get(pos).burst(Speck.factory(Speck.LIGHT), 6);
				((PortalSprite) sprite).shut();
				destroy();
				sprite.killAndErase();
			} else {
				vanish();
			}
		}

		@Override
		public void damage( int dmg, Object src ) {
			if (!fromParty(src)) return;
			super.damage(ThalVekhorr.summonShare(src, dmg), src);
		}

		@Override
		public void die( Object cause ) {
			super.die(cause);
			ThalVekhorr t = ThalVekhorr.find();
			if (t != null) t.portalClosed(this);
		}

		@Override
		public String name() {
			return Messages.get(this, echo ? "name_echo" : "name");
		}

		@Override
		public String description() {
			return Messages.get(this, "desc", Messages.get(ThalVekhorr.class, "world_" + world));
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("world", world);
			bundle.put("echo", echo);
			bundle.put("timer", timer);
			bundle.put("echo_out", echoOut);
			bundle.put("echo_dead", echoDead);
			int[] k = new int[kids.size()];
			for (int i = 0; i < k.length; i++) k[i] = kids.get(i);
			bundle.put("kids", k);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			world = bundle.getInt("world");
			echo = bundle.getBoolean("echo");
			timer = bundle.getInt("timer");
			echoOut = bundle.getBoolean("echo_out");
			echoDead = bundle.getBoolean("echo_dead");
			kids.clear();
			for (int k : bundle.getIntArray("kids")) kids.add(k);
		}
	}

	// *** hourglasses: break them or he rewinds ***

	public static class Hourglass extends Part {

		{
			spriteClass = HourglassSprite.class;
			HP = HT = 45;
		}

		public int sand = 14, sandMax = 14;

		@Override
		protected boolean act() {
			sand--;
			if (sprite instanceof HourglassSprite) ((HourglassSprite) sprite).sand(sand / (float) sandMax);
			if (sand <= 0){
				ThalVekhorr t = ThalVekhorr.find();
				if (t != null) t.rewindHp();
				vanish();
				return true;
			}
			spend(TICK);
			return true;
		}

		@Override
		public void damage( int dmg, Object src ) {
			if (!fromParty(src)) return;
			super.damage(ThalVekhorr.summonShare(src, dmg), src);
		}

		@Override
		public void die( Object cause ) {
			super.die(cause);
			Sample.INSTANCE.play(Assets.Sounds.SHATTER);
		}

		@Override
		public String description() {
			return Messages.get(this, "desc", sand);
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("sand", sand);
			bundle.put("sand_max", sandMax);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			sand = bundle.getInt("sand");
			sandMax = bundle.getInt("sand_max");
		}
	}

	// *** the core, last act only ***

	public static class Core extends Part {

		{
			spriteClass = CoreSprite.class;
			HP = HT = 10000;
			properties.add(Property.LARGE);
		}

		@Override
		protected void onAdd() {
			boolean first = firstAdded;
			super.onAdd();
			if (first) HP = HT = 10000;
		}

		@Override
		protected boolean act() {
			spend(TICK);
			return true;
		}

		@Override
		public void damage( int dmg, Object src ) {
			if (!isAlive() || dmg <= 0 || !fromParty(src)) return;
			ThalVekhorr t = ThalVekhorr.find();
			if (t == null) return;
			dmg = ThalVekhorr.summonShare(src, AntiOP.cap(this, src, dmg));
			if (sprite != null){
				sprite.showStatus(CharSprite.NEGATIVE, Integer.toString(dmg));
				sprite.flash();
			}
			t.hurt(dmg);
		}
	}

	// *** echoes of the heroes: walk your steps from a few turns ago, swing when you swung ***

	public static class HeroEcho extends Mob {

		{
			spriteClass = EchoSprite.class;
			alignment = Alignment.ENEMY;
			state = HUNTING;
			EXP = 0;
			defenseSkill = 10;
			properties.add(Property.INORGANIC);
			immunities.add(ThalCall.Stasis.class);
		}

		public HeroClass cls = HeroClass.WARRIOR;
		public int armTier = 1;
		public int[] path = new int[0];
		public boolean[] swing = new boolean[0];
		public int step = 0;
		public int minDmg = 4, maxDmg = 10;

		public static HeroEcho of( Hero h, int[] path, boolean[] swing ){
			HeroEcho e = new HeroEcho();
			e.cls = h.heroClass;
			e.armTier = h.tier();
			e.path = path;
			e.swing = swing;
			e.HP = e.HT = Math.max(20, h.HT / 4);
			int lvl = h.lvl;
			e.minDmg = 4 + lvl / 3;
			e.maxDmg = 12 + lvl;
			e.defenseSkill = 6 + lvl;
			return e;
		}

		@Override
		protected boolean act() {
			if (step >= path.length){
				fade();
				return true;
			}
			int to = path[step];
			boolean sw = swing[step];
			step++;
			if (to != pos && Dungeon.level.passable[to] && Actor.findChar(to) == null){
				int from = pos;
				move(to);
				if (sprite != null) sprite.move(from, to);
			}
			if (sw){
				Char victim = null;
				for (int n : PathFinder.NEIGHBOURS8){
					Char c = Actor.findChar(pos + n);
					if (c != null && c.alignment == Alignment.ALLY && c.isAlive()){
						victim = c;
						break;
					}
				}
				if (victim != null){
					int dmg = Math.max(1, Random.NormalIntRange(minDmg, maxDmg) - victim.drRoll()/2);
					victim.damage(dmg, this);
					if (sprite != null && sprite.visible){
						//visual only, the hit already happened
						sprite.attack(victim.pos, new Callback() {
							@Override
							public void call() {
								if (sprite != null) sprite.idle();
							}
						});
					}
				}
			}
			spend(TICK);
			return true;
		}

		private void fade(){
			if (sprite != null){
				CellEmitter.get(pos).burst(Speck.factory(Speck.LIGHT), 4);
				sprite.killAndErase();
			}
			destroy();
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
		public void beckon( int cell ) {
		}

		@Override
		public float spawningWeight() {
			return 0;
		}

		@Override
		public void die( Object cause ) {
			super.die(cause);
		}

		@Override
		public String description() {
			return Messages.get(this, "desc");
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("cls", cls);
			bundle.put("arm", armTier);
			bundle.put("path", path);
			bundle.put("swing", swing);
			bundle.put("step", step);
			bundle.put("min", minDmg);
			bundle.put("max", maxDmg);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			cls = bundle.getEnum("cls", HeroClass.class);
			armTier = bundle.getInt("arm");
			path = bundle.getIntArray("path");
			swing = bundle.getBooleanArray("swing");
			step = bundle.getInt("step");
			minDmg = bundle.getInt("min");
			maxDmg = bundle.getInt("max");
		}
	}

	// *** the way out ***

	public static class ExitPortal extends SKProps.SKProp {

		{
			spriteClass = PortalSprite.class;
			HP = HT = 1000;
		}

		public int[] votes = new int[Party.MAX_HEROES];
		public boolean decided = false;

		@Override
		public boolean isInvulnerable( Class effect ) {
			return true;
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("votes", votes);
			bundle.put("decided", decided);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			votes = bundle.getIntArray("votes");
			decided = bundle.getBoolean("decided");
		}

		@Override
		public boolean interact( Char c ) {
			if (c instanceof Hero) ThalVekhorr.askVote((Hero) c);
			return true;
		}

		@Override
		public String description() {
			return Messages.get(this, ThalFinale.CONTINENT_READY ? "desc" : "desc_soon");
		}
	}

	// ===== sprites =====

	//6 frames x 2 rows (thumb left / thumb right): 0 shadow, 1 coming down, 2 resting, 3 slam, 4 fist, 5 going up
	public static class HandSprite extends MobSprite {

		public static final int W = 48, H = 48;
		private TextureFilm film;
		private Animation planted, slam, rise;

		public HandSprite(){
			super();
			texture(FinaleAssets.HAND);
			film = new TextureFilm(texture, W, H);
			setup(0);
		}

		private void setup( int row ){
			int o = row * 6;
			slam = new Animation(12, false);
			slam.frames(film, o + 1, o + 3, o + 2);
			planted = new Animation(2, true);
			planted.frames(film, o + 2);
			rise = new Animation(10, false);
			rise.frames(film, o + 5, o + 5);
			idle = planted;
			run = planted.clone();
			attack = slam;
			zap = planted.clone();
			die = rise;
			play(slam);
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (ch instanceof Hand) setup(((Hand) ch).left ? 0 : 1);
		}

		@Override
		public void onComplete( Animation anim ) {
			if (anim == slam){
				play(planted);
			} else if (anim == rise){
				killAndErase();
			} else {
				super.onComplete(anim);
			}
		}

		public void lift(){
			play(rise);
		}

		//frame's top-left on the top-left of the 3x3 block, the hit cell in the middle
		@Override
		public PointF worldToCamera( int cell ) {
			int w = Dungeon.level.width();
			return new PointF((cell % w) * 16 - 16, (cell / w) * 16 - 16);
		}

		@Override
		public PointF center() {
			return new PointF(x + 24, y + 26);
		}

		@Override
		public int blood() {
			return 0xFFC8A040;
		}
	}

	//13 frames: 0-7 beating, two per crack stage, 8-12 bursting
	public static class CoreSprite extends MobSprite {

		public static final int W = 48, H = 48;
		private TextureFilm film;
		private int stage = -1;

		public CoreSprite(){
			super();
			texture(FinaleAssets.CORE);
			film = new TextureFilm(texture, W, H);
			die = new Animation(10, false);
			die.frames(film, 8, 9, 10, 11, 12);
			stage(0);
		}

		private void stage( int st ){
			if (st == stage) return;
			stage = st;
			idle = new Animation(3, true);
			idle.frames(film, st * 2, st * 2 + 1);
			run = idle.clone();
			attack = idle.clone();
			zap = idle.clone();
			play(idle, true);
		}

		@Override
		public void update() {
			super.update();
			ThalVekhorr t = ThalVekhorr.find();
			if (t != null && t.HT > 0 && curAnim != die){
				//the last act is the last 20% of his hp
				float left = t.HP / (t.HT * 0.2f);
				stage(left > 0.75f ? 0 : left > 0.5f ? 1 : left > 0.25f ? 2 : 3);
			}
		}

		@Override
		public PointF worldToCamera( int cell ) {
			int w = Dungeon.level.width();
			return new PointF((cell % w) * 16 - 16, (cell / w) * 16 - 35);
		}

		@Override
		public PointF center() {
			return new PointF(x + 24, y + 21);
		}

		@Override
		public int blood() {
			return 0xFFDDF4FF;
		}
	}

	//grey, tinted per world. 0-3 spinning, 4-6 opening, 7-9 closing
	public static class PortalSprite extends MobSprite {

		public static final int W = 32, H = 48;
		private Animation spin, open, close;

		public PortalSprite(){
			super();
			texture(FinaleAssets.PORTAL);
			TextureFilm film = new TextureFilm(texture, W, H);
			spin = new Animation(8, true);
			spin.frames(film, 0, 1, 2, 3);
			open = new Animation(10, false);
			open.frames(film, 4, 5, 6);
			close = new Animation(10, false);
			close.frames(film, 7, 8, 9);
			idle = spin;
			run = spin.clone();
			attack = spin.clone();
			zap = spin.clone();
			die = close;
			play(open);
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			int w = ch instanceof Portal ? ((Portal) ch).world : 6;
			hardlight(worldColor(w));
		}

		public void pulse(){
			flash();
		}

		public void shut(){
			play(close);
		}

		@Override
		public void onComplete( Animation anim ) {
			if (anim == open){
				play(spin);
			} else if (anim == close && ch != null && !ch.isAlive()){
				killAndErase();
			} else {
				super.onComplete(anim);
			}
		}

		@Override
		public PointF worldToCamera( int cell ) {
			int w = Dungeon.level.width();
			return new PointF((cell % w) * 16 - 8, (cell / w) * 16 - 32);
		}

		@Override
		public PointF center() {
			return new PointF(x + 16, y + 26);
		}

		@Override
		public int blood() {
			return 0xFFDDF4FF;
		}
	}

	public static class HourglassSprite extends MobSprite {

		public static final int W = 16, H = 24;
		private TextureFilm film;
		private int shown = -1;

		public HourglassSprite(){
			super();
			texture(FinaleAssets.HOURGLASS);
			film = new TextureFilm(texture, W, H);
			idle = new Animation(1, true);
			idle.frames(film, 0);
			run = idle.clone();
			attack = idle.clone();
			zap = idle.clone();
			die = new Animation(10, false);
			die.frames(film, 4, 5);
			play(idle);
		}

		//1 full ... 0 empty
		public void sand( float left ){
			int f = left > 0.75f ? 0 : left > 0.5f ? 1 : left > 0.25f ? 2 : 3;
			if (f == shown) return;
			shown = f;
			idle = new Animation(1, true);
			idle.frames(film, f);
			play(idle, true);
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (ch instanceof Hourglass) sand(((Hourglass) ch).sand / (float) ((Hourglass) ch).sandMax);
		}


		@Override
		public int blood() {
			return 0xFFE8D8A0;
		}
	}

	//same frames as the hero sheets (like the rogue's mirror images), see-through and blue
	public static class EchoSprite extends MobSprite {

		private static final int FW = 12, FH = 15;

		public EchoSprite(){
			super();
			texture(HeroClass.WARRIOR.spritesheet());
			setup(1);
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (ch instanceof HeroEcho){
				texture(((HeroEcho) ch).cls.spritesheet());
				setup(((HeroEcho) ch).armTier);
			}
			hardlight(0x7FD8FF);
			alpha(0.7f);
		}

		private void setup( int tier ){
			TextureFilm film = new TextureFilm(HeroSprite.tiers(), tier, FW, FH);
			idle = new Animation(1, true);
			idle.frames(film, 0, 0, 0, 1, 0, 0, 1, 1);
			run = new Animation(20, true);
			run.frames(film, 2, 3, 4, 5, 6, 7);
			die = new Animation(20, false);
			die.frames(film, 0);
			attack = new Animation(15, false);
			attack.frames(film, 13, 14, 15, 0);
			zap = attack.clone();
			idle();
		}

		@Override
		public void bloodBurstA( PointF from, int damage ) {
		}
	}
}
