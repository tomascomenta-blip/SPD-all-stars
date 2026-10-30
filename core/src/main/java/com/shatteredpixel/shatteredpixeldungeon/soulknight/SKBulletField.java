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

package com.shatteredpixel.shatteredpixeldungeon.soulknight;

import com.shatteredpixel.shatteredpixeldungeon.coop.CoopRules;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AscensionChallenge;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.automaton.AutomatonHooks;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.props.SKProps;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/*
 bullets for the SK mobs. visible, move a few cells per turn so you can actually dodge them.
 it's a Blob so it gets saved with the floor like a gas (yes, bullets are a gas. don't ask)
 positions are ints, 16 units per cell. NO floats here, both coop devices have to get the exact same numbers
*/
public class SKBulletField extends Blob {

	public static final int UNIT = 16;

	//bullet types
	public static final int STRAIGHT = 0; //boring
	public static final int SPLIT    = 1; //bursts into 'children' after 'timer' turns
	public static final int SEEDER   = 2; //drops a turret every 'timer' turns
	public static final int ORBIT    = 3; //circles the owner
	public static final int TRAIL    = 4; //leaves still bullets behind for 'period' turns
	public static final int HOMING   = 5; //turns max 'spread' deg/turn toward the closest foe
	public static final int SPINNER  = 6; //sprinkler. 'children' bullets per turn, rotates 'spread' deg

	{
		actPriority = BLOB_PRIO;
	}

	public static class Bullet implements Bundlable {
		public int id;
		public int image;
		public int behaviour = STRAIGHT;
		public int ownerId = -1;
		public boolean hostile = true; //true = hits heroes+allies, false = hits mobs. ojo, easy to flip by accident
		public int px, py;             //units, not cells!
		public int vx, vy;
		public int life = 12;          //turns left
		public int minDmg = 1, maxDmg = 4;
		public int timer;
		public int period;             //SEEDER: turns between turrets
		public int children = 3;
		public int childImage = -1;
		public int childSpeed = UNIT*2;
		public int spread = 45;        //SPLIT: deg between children, 360 = full circle
		public int angle;              //ORBIT: 0-7 around the owner
		public int radius = 1;         //ORBIT: distance to the owner
		public int markImage = -1;     //SEEDER: how its turrets look
		public int bounces = 0;        //how many walls before it dies
		public boolean burst = false;  //pops into 'children' on wall hit
		public boolean fake = false;   //illusion bullets. look scary, do nothing

		public int cell(){
			return cellOf(px, py);
		}

		private static final int FIELDS = 20; //old saves = 20 numbers, new = 22. keep reading both or old runs explode

		@Override
		public void storeInBundle( Bundle b ) {
			b.put("fake", fake);
			int[] v = {id, image, behaviour, ownerId, px, py, vx, vy, life, minDmg, maxDmg, timer, period,
					children, childImage, childSpeed, spread, angle, radius, markImage, bounces, burst ? 1 : 0};
			b.put("data", v);
			b.put("hostile", hostile);
		}

		@Override
		public void restoreFromBundle( Bundle b ) {
			fake = b.getBoolean("fake");
			int[] v = b.getIntArray("data");
			if (v == null || v.length < FIELDS) { life = 0; return; }
			id = v[0]; image = v[1]; behaviour = v[2]; ownerId = v[3]; px = v[4]; py = v[5];
			vx = v[6]; vy = v[7]; life = v[8]; minDmg = v[9]; maxDmg = v[10]; timer = v[11];
			period = v[12]; children = v[13]; childImage = v[14]; childSpeed = v[15]; spread = v[16];
			angle = v[17]; radius = v[18]; markImage = v[19];
			if (v.length >= 22){
				bounces = v[20];
				burst = v[21] != 0;
			}
			hostile = b.getBoolean("hostile");
		}
	}

	//SEEDER's turret. 2 bullets sideways every other turn
	public static class Turret implements Bundlable {
		public int id;
		public int cell;
		public int ax, ay;  //units/turn
		public int life = 6;
		public int timer = 1;
		public int ownerId = -1;
		public int image;
		public int bulletImage;
		public int minDmg = 1, maxDmg = 3;

		@Override
		public void storeInBundle( Bundle b ) {
			b.put("data", new int[]{id, cell, ax, ay, life, timer, ownerId, image, minDmg, maxDmg, bulletImage});
		}

		@Override
		public void restoreFromBundle( Bundle b ) {
			int[] v = b.getIntArray("data");
			if (v == null || v.length < 11) { life = 0; return; }
			id = v[0]; cell = v[1]; ax = v[2]; ay = v[3]; life = v[4]; timer = v[5];
			ownerId = v[6]; image = v[7]; minDmg = v[8]; maxDmg = v[9]; bulletImage = v[10];
		}
	}

	public ArrayList<Bullet> bullets = new ArrayList<>();
	public ArrayList<Turret> turrets = new ArrayList<>();
	private int nextId = 1;

	//death msg
	public static class Shot implements Hero.Doom {
		public final Char owner;
		public Shot( Char owner ){
			this.owner = owner;
		}
		@Override
		public void onDeath() {
			Dungeon.fail(owner != null ? owner : new SKBulletField());
			GLog.n(owner != null
					? Messages.get(SKBulletField.class, "kill_by", owner.name())
					: Messages.get(SKBulletField.class, "kill"));
		}
	}

	public static SKBulletField get(){
		Level level = Dungeon.level;
		SKBulletField field = (SKBulletField) level.blobs.get(SKBulletField.class);
		if (field == null){
			field = new SKBulletField();
			//if a mob/buff spawned it, don't give it a free move this turn
			if (Actor.curActorPriority() < field.actPriority) field.spend(1f);
			field.seed(level, 0, 0);
			level.blobs.put(SKBulletField.class, field);
			GameScene.add(field);
		}
		return field;
	}

	public static int cellOf( int px, int py ){
		//floor div by hand bc Math.floorDiv needs android 7. yes really
		int x = px >= 0 ? px / UNIT : -((-px + UNIT - 1) / UNIT);
		int y = py >= 0 ? py / UNIT : -((-py + UNIT - 1) / UNIT);
		if (x < 0 || y < 0 || x >= Dungeon.level.width() || y >= Dungeon.level.height()) return -1;
		return x + y*Dungeon.level.width();
	}

	public static int centerX( int cell ){
		return (cell % Dungeon.level.width())*UNIT + UNIT/2;
	}

	public static int centerY( int cell ){
		return (cell / Dungeon.level.width())*UNIT + UNIT/2;
	}

	//degrees: 0 = right, 90 = DOWN (screen coords). speed in cells/turn
	public synchronized Bullet fire( Char owner, int from, float degrees, float cellsPerTurn, int image, int minDmg, int maxDmg ){
		Bullet b = new Bullet();
		b.id = nextId++;
		b.image = image;
		b.ownerId = owner != null ? owner.id() : -1;
		b.hostile = owner == null || owner.alignment == Char.Alignment.ENEMY;
		b.px = centerX(from);
		b.py = centerY(from);
		double rad = StrictMath.toRadians(degrees);
		b.vx = (int)StrictMath.round(StrictMath.cos(rad) * cellsPerTurn * UNIT);
		b.vy = (int)StrictMath.round(StrictMath.sin(rad) * cellsPerTurn * UNIT);
		//amulet curse boosts enemy bullets like it does warlock bolts
		float boost = owner != null ? AscensionChallenge.statModifier(owner) : 1f;
		b.minDmg = Math.round(minDmg * boost);
		b.maxDmg = Math.round(maxDmg * boost);
		bullets.add(b);
		return b;
	}

	public static float angleTo( int from, int to ){
		int w = Dungeon.level.width();
		int dx = to % w - from % w;
		int dy = to / w - from / w;
		if (dx == 0 && dy == 0) return 0;
		return (float)StrictMath.toDegrees(StrictMath.atan2(dy, dx));
	}

	public synchronized Bullet orbit( Char owner, int angle, int radius, int life, int image, int minDmg, int maxDmg ){
		Bullet b = new Bullet();
		b.id = nextId++;
		b.image = image;
		b.behaviour = ORBIT;
		b.ownerId = owner.id();
		b.hostile = owner.alignment == Char.Alignment.ENEMY;
		b.angle = angle;
		b.radius = radius;
		b.life = life;
		b.minDmg = minDmg;
		b.maxDmg = maxDmg;
		int c = orbitCell(owner.pos, angle, radius);
		b.px = centerX(c == -1 ? owner.pos : c);
		b.py = centerY(c == -1 ? owner.pos : c);
		bullets.add(b);
		return b;
	}

	//owner died, se lleva sus orbs/turrets
	public synchronized void clearOwner( Char owner ){
		for (Bullet b : bullets){
			if (b.ownerId == owner.id() && b.behaviour == ORBIT) b.life = 0;
		}
		for (Turret t : turrets){
			if (t.ownerId == owner.id()) t.life = 0;
		}
	}

	//EMP & co. wipes that side's bullets near center (+ enemy turrets), returns the count
	public synchronized int destroyNear( int center, int radius, boolean hostile ){
		int n = 0;
		for (Bullet b : bullets){
			int c = b.cell();
			if (b.life > 0 && b.hostile == hostile && c != -1 && Dungeon.level.distance(center, c) <= radius){
				b.life = 0;
				n++;
				if (Dungeon.level.heroFOV[c]) CellEmitter.center(c).burst(Speck.factory(Speck.STEAM), 2);
			}
		}
		if (hostile){
			for (Turret t : turrets){
				if (t.life > 0 && Dungeon.level.distance(center, t.cell) <= radius){
					t.life = 0;
					n++;
					if (Dungeon.level.heroFOV[t.cell]) CellEmitter.center(t.cell).burst(Speck.factory(Speck.STEAM), 4);
				}
			}
		}
		return n;
	}

	public static int destroyAround( int center, int radius ){
		SKBulletField field = (SKBulletField) Dungeon.level.blobs.get(SKBulletField.class);
		return field == null ? 0 : field.destroyNear(center, radius, true);
	}

	public boolean isEmpty(){
		return bullets.isEmpty() && turrets.isEmpty();
	}

	//game thread edits the lists while the render thread draws them -> both lock the field.
	//got a ConcurrentModificationException every few runs before this
	@Override
	public synchronized boolean act() {
		spend(TICK);
		if (isEmpty()) return true;

		ArrayList<Bullet> born = new ArrayList<>();

		for (Turret t : turrets){
			if (t.life <= 0) continue;
			t.life--;
			if (--t.timer <= 0){
				t.timer = 2;
				for (int sign = -1; sign <= 1; sign += 2){
					Bullet b = new Bullet();
					b.id = nextId++;
					b.image = t.bulletImage;
					b.ownerId = t.ownerId;
					b.hostile = true;
					b.px = centerX(t.cell);
					b.py = centerY(t.cell);
					b.vx = t.ax*sign;
					b.vy = t.ay*sign;
					b.life = 8;
					b.minDmg = t.minDmg;
					b.maxDmg = t.maxDmg;
					born.add(b);
				}
			}
		}

		for (Bullet b : bullets){
			if (b.life <= 0) continue;
			b.life--;
			if (b.behaviour == ORBIT){
				moveOrbit(b);
				continue;
			}
			if (b.behaviour == HOMING) steer(b);
			int before = b.cell();
			move(b, born);
			if (b.behaviour == TRAIL && before != -1 && before != b.cell() && !Dungeon.level.solid[before]){
				Bullet t = still(b, before);
				born.add(t);
			}
			if (b.life <= 0) continue;
			if (b.behaviour == SPINNER){
				spin(b, born);
			} else if (b.behaviour == SPLIT && --b.timer <= 0){
				split(b, born);
				b.life = 0;
			} else if (b.behaviour == SEEDER && --b.timer <= 0){
				b.timer = b.period;
				plant(b);
			}
		}

		//newborns move next turn but they can still hit whoever's standing on them
		for (Bullet b : born){
			hitAt(b, b.cell());
		}
		bullets.addAll(born);

		for (Iterator<Bullet> it = bullets.iterator(); it.hasNext(); ){
			if (it.next().life <= 0) it.remove();
		}
		for (Iterator<Turret> it = turrets.iterator(); it.hasNext(); ){
			if (it.next().life <= 0) it.remove();
		}
		return true;
	}

	//TRAIL leftovers
	private Bullet still( Bullet b, int cell ){
		Bullet t = new Bullet();
		t.id = nextId++;
		t.image = b.childImage >= 0 ? b.childImage : b.image;
		t.ownerId = b.ownerId;
		t.hostile = b.hostile;
		t.px = centerX(cell);
		t.py = centerY(cell);
		t.life = Math.max(1, b.period);
		t.minDmg = Math.max(1, b.minDmg*2/3);
		t.maxDmg = Math.max(1, b.maxDmg*2/3);
		return t;
	}

	private void steer( Bullet b ){
		int c = b.cell();
		if (c == -1) return;
		Char best = null;
		int bestD = Integer.MAX_VALUE;
		for (Char ch : Actor.chars()){
			if (!ch.isAlive() || ch.id() == b.ownerId) continue;
			if (b.hostile ? ch.alignment != Char.Alignment.ALLY : ch.alignment != Char.Alignment.ENEMY) continue;
			int d = Dungeon.level.distance(c, ch.pos);
			if (d <= 8 && (d < bestD || (d == bestD && ch.pos < best.pos))){
				best = ch;
				bestD = d;
			}
		}
		if (best == null) return;
		double speed = StrictMath.sqrt((double)b.vx*b.vx + (double)b.vy*b.vy);
		if (speed == 0) return;
		double cur = StrictMath.toDegrees(StrictMath.atan2(b.vy, b.vx));
		double want = StrictMath.toDegrees(StrictMath.atan2(centerY(best.pos) - b.py, centerX(best.pos) - b.px));
		double diff = want - cur;
		while (diff > 180) diff -= 360;
		while (diff < -180) diff += 360;
		double turn = Math.max(-b.spread, Math.min(b.spread, diff));
		double rad = StrictMath.toRadians(cur + turn);
		b.vx = (int)StrictMath.round(StrictMath.cos(rad)*speed);
		b.vy = (int)StrictMath.round(StrictMath.sin(rad)*speed);
	}

	private void spin( Bullet b, ArrayList<Bullet> born ){
		int c = b.cell();
		if (c == -1) return;
		for (int i = 0; i < b.children; i++){
			double rad = StrictMath.toRadians(b.angle + i*360.0/b.children);
			Bullet k = new Bullet();
			k.id = nextId++;
			k.image = b.childImage >= 0 ? b.childImage : b.image;
			k.ownerId = b.ownerId;
			k.hostile = b.hostile;
			k.px = centerX(c);
			k.py = centerY(c);
			k.vx = (int)StrictMath.round(StrictMath.cos(rad)*b.childSpeed);
			k.vy = (int)StrictMath.round(StrictMath.sin(rad)*b.childSpeed);
			k.life = 6;
			k.minDmg = Math.max(1, b.minDmg*2/3);
			k.maxDmg = Math.max(1, b.maxDmg*2/3);
			born.add(k);
		}
		b.angle = (b.angle + b.spread) % 360;
	}

	//wall: bounce, burst, or just die
	private void wall( Bullet b, int last, int nextCell, ArrayList<Bullet> born ){
		if (b.bounces > 0 && nextCell != -1){
			int w = Dungeon.level.width();
			int lx = last % w, ly = last / w;
			int nx = nextCell % w, ny = nextCell / w;
			boolean blockX = nx != lx && Dungeon.level.solid[nx + ly*w];
			boolean blockY = ny != ly && Dungeon.level.solid[lx + ny*w];
			if (blockX || !blockY) b.vx = -b.vx;
			if (blockY || !blockX) b.vy = -b.vy;
			b.bounces--;
			return;
		}
		b.life = 0;
		if (b.burst && last != -1){
			Bullet ring = new Bullet();
			ring.children = b.children;
			ring.spread = 360;
			ring.childImage = b.childImage;
			ring.childSpeed = b.childSpeed;
			ring.image = b.image;
			ring.ownerId = b.ownerId;
			ring.hostile = b.hostile;
			ring.px = centerX(last);
			ring.py = centerY(last);
			ring.vx = b.vx;
			ring.vy = b.vy;
			ring.minDmg = b.minDmg;
			ring.maxDmg = b.maxDmg;
			split(ring, born);
		}
	}

	private void move( Bullet b ){
		move(b, null);
	}

	private void move( Bullet b, ArrayList<Bullet> born ){
		int steps = Math.max(1, (Math.max(Math.abs(b.vx), Math.abs(b.vy)) + UNIT/4 - 1) / (UNIT/4));
		int last = b.cell();
		//stepped onto it? you get hit first, then it moves. no free dodges
		if (hitAt(b, last)) return;
		for (int i = 1; i <= steps; i++){
			int nx = b.px + b.vx*i/steps;
			int ny = b.py + b.vy*i/steps;
			int c = cellOf(nx, ny);
			if (c != last){
				if (c == -1 || Dungeon.level.solid[c]){
					b.px = b.px + b.vx*(i-1)/steps;
					b.py = b.py + b.vy*(i-1)/steps;
					if (last != -1 && Dungeon.level.heroFOV[last]) {
						CellEmitter.center(last).burst(Speck.factory(Speck.DUST), 2);
					}
					if (born != null) wall(b, last, c, born);
					else b.life = 0;
					return;
				}
				last = c;
				//mow the grass (forest). purely cosmetic but it looks cool
				if (Dungeon.level.map[c] == Terrain.HIGH_GRASS){
					Level.set(c, Terrain.GRASS);
					GameScene.updateMap(c);
				}
				if (hitAt(b, c)){
					b.px = nx;
					b.py = ny;
					return;
				}
			}
		}
		b.px += b.vx;
		b.py += b.vy;
	}

	private void moveOrbit( Bullet b ){
		Actor a = Actor.findById(b.ownerId);
		if (!(a instanceof Char) || !((Char) a).isAlive()){
			b.life = 0;
			return;
		}
		Char owner = (Char) a;
		b.angle = (b.angle + 1) % 8;
		int c = orbitCell(owner.pos, b.angle, b.radius);
		if (c == -1 || Dungeon.level.solid[c]){
			//tucked behind the owner this turn
			b.px = centerX(owner.pos);
			b.py = centerY(owner.pos);
			return;
		}
		b.px = centerX(c);
		b.py = centerY(c);
		//orbs don't get used up, por eso ignoramos lo que devuelve
		hitAt(b, c);
	}

	private static final int[][] ORBIT_OFS = {{1,0},{1,1},{0,1},{-1,1},{-1,0},{-1,-1},{0,-1},{1,-1}};

	public static int orbitCell( int center, int angle, int radius ){
		int w = Dungeon.level.width();
		int x = center % w + ORBIT_OFS[angle][0]*radius;
		int y = center / w + ORBIT_OFS[angle][1]*radius;
		if (x < 0 || y < 0 || x >= w || y >= Dungeon.level.height()) return -1;
		return x + y*w;
	}

	private void split( Bullet b, ArrayList<Bullet> born ){
		int c = b.cell();
		if (c == -1) return;
		float heading = (float)StrictMath.toDegrees(StrictMath.atan2(b.vy, b.vx));
		if (Dungeon.level.heroFOV[c]) CellEmitter.center(c).burst(Speck.factory(Speck.STAR), 3);
		for (int i = 0; i < b.children; i++){
			float deg;
			if (b.spread >= 360) deg = heading + i*360f/b.children;
			else deg = heading + (i - (b.children-1)/2f)*b.spread;
			Bullet k = new Bullet();
			k.id = nextId++;
			k.image = b.childImage >= 0 ? b.childImage : b.image;
			k.ownerId = b.ownerId;
			k.hostile = b.hostile;
			k.px = centerX(c);
			k.py = centerY(c);
			double rad = StrictMath.toRadians(deg);
			k.vx = (int)StrictMath.round(StrictMath.cos(rad)*b.childSpeed);
			k.vy = (int)StrictMath.round(StrictMath.sin(rad)*b.childSpeed);
			k.life = 6;
			k.minDmg = Math.max(1, b.minDmg*2/3);
			k.maxDmg = Math.max(1, b.maxDmg*2/3);
			born.add(k);
		}
	}

	private void plant( Bullet b ){
		int c = b.cell();
		if (c == -1 || Dungeon.level.solid[c] || !Dungeon.level.passable[c]) return;
		for (Turret t : turrets){
			if (t.cell == c) return;
		}
		double len = StrictMath.sqrt((double)b.vx*b.vx + (double)b.vy*b.vy);
		if (len == 0) return;
		Turret t = new Turret();
		t.id = nextId++;
		t.cell = c;
		//perpendicular to the seed, 1 cell/turn
		t.ax = (int)StrictMath.round(-b.vy/len*UNIT);
		t.ay = (int)StrictMath.round(b.vx/len*UNIT);
		t.ownerId = b.ownerId;
		t.image = b.markImage >= 0 ? b.markImage : b.image;
		t.bulletImage = b.childImage >= 0 ? b.childImage : b.image;
		t.minDmg = Math.max(1, b.minDmg*2/3);
		t.maxDmg = Math.max(1, b.maxDmg*2/3);
		turrets.add(t);
	}

	//friendly fire: a hero's bullet also hits the other heroes (homing still only looks for enemies)
	private static boolean friendHit( Bullet b, Char ch ){
		Actor owner = Actor.findById(b.ownerId);
		return owner instanceof Char && CoopRules.friendlyTarget((Char) owner, ch);
	}

	//true = hit someone (and it's spent, except orbs)
	private boolean hitAt( Bullet b, int cell ){
		if (cell == -1) return false;
		Char ch = Actor.findChar(cell);
		if (ch == null || ch.id() == b.ownerId || !ch.isAlive()) return false;
		boolean prop = ch instanceof SKProps.SKProp;
		if (!prop && (b.hostile ? ch.alignment != Char.Alignment.ALLY
				: ch.alignment != Char.Alignment.ENEMY && !friendHit(b, ch))) return false;

		if (b.behaviour != ORBIT) b.life = 0;
		if (b.fake){
			if (Dungeon.level.heroFOV[cell]) CellEmitter.get(cell).burst(Speck.factory(Speck.WOOL), 2);
			return true;
		}
		if (ch.isInvulnerable(SKBulletField.class)) return true;

		Actor a = Actor.findById(b.ownerId);
		Char owner = a instanceof Char ? (Char) a : null;
		int dmg = Random.NormalIntRange(b.minDmg, b.maxDmg);
		dmg = Math.max(1, dmg - ch.drRoll()/2);
		//automaton plating halves bullets
		dmg = AutomatonHooks.bulletDamage(ch, dmg);
		if (Dungeon.level.heroFOV[cell]) {
			CellEmitter.center(cell).burst(Speck.factory(Speck.STAR), 2);
			Sample.INSTANCE.play(Assets.Sounds.HIT, 0.8f, 1.2f);
		}
		ch.damage(dmg, new Shot(owner));
		return true;
	}

	@Override
	public synchronized void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("bullets", bullets);
		bundle.put("turrets", turrets);
		bundle.put("next_id", nextId);
	}

	@Override
	public synchronized void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		bullets.clear();
		turrets.clear();
		for (Bundlable b : bundle.getCollection("bullets")){
			if (b instanceof Bullet && ((Bullet) b).life > 0) bullets.add((Bullet) b);
		}
		for (Bundlable b : bundle.getCollection("turrets")){
			if (b instanceof Turret && ((Turret) b).life > 0) turrets.add((Turret) b);
		}
		nextId = Math.max(1, bundle.getInt("next_id"));
	}

	@Override
	public String tileDesc() {
		return null;
	}

	//-- drawing --

	@Override
	public void use( BlobEmitter emitter ) {
		super.use(emitter);
		emitter.autoKill = false;
		emitter.add(new Layer(this));
	}

	//bullets+turrets, slides them between cells
	public static class Layer extends Group {

		private final SKBulletField field;
		private final HashMap<Integer, Shown> shown = new HashMap<>();

		private static class Shown {
			ItemSprite sprite;
			float fromX, fromY, toX, toY, t = 1f;
			boolean seen;
		}

		public Layer( SKBulletField field ){
			this.field = field;
		}

		@Override
		public void update() {
			super.update();
			for (Shown s : shown.values()) s.seen = false;

			ArrayList<Bullet> bullets;
			ArrayList<Turret> turrets;
			synchronized (field){
				bullets = new ArrayList<>(field.bullets);
				turrets = new ArrayList<>(field.turrets);
			}
			for (Bullet b : bullets){
				if (b.life <= 0) continue;
				show(b.id, b.image, b.px, b.py, b.vx, b.vy, b.cell());
			}
			for (Turret t : turrets){
				if (t.life <= 0) continue;
				show(-t.id, t.image, centerX(t.cell), centerY(t.cell), 0, 0, t.cell);
			}

			for (Iterator<Shown> it = shown.values().iterator(); it.hasNext(); ){
				Shown s = it.next();
				if (!s.seen){
					s.sprite.killAndErase();
					it.remove();
				}
			}
		}

		private void show( int id, int image, int px, int py, int vx, int vy, int cell ){
			float wx = px * (DungeonTilemap.SIZE / (float)UNIT);
			float wy = py * (DungeonTilemap.SIZE / (float)UNIT);
			Shown s = shown.get(id);
			if (s == null){
				s = new Shown();
				s.sprite = new ItemSprite(image, null);
				s.sprite.originToCenter();
				s.fromX = s.toX = wx;
				s.fromY = s.toY = wy;
				if (vx != 0 || vy != 0){
					s.sprite.angle = 135 - (float)(Math.atan2(vx, vy) / Math.PI * 180);
				}
				shown.put(id, s);
				add(s.sprite);
			} else if (s.toX != wx || s.toY != wy){
				float t = Math.min(1f, s.t);
				s.fromX = s.fromX + (s.toX - s.fromX)*t;
				s.fromY = s.fromY + (s.toY - s.fromY)*t;
				s.toX = wx;
				s.toY = wy;
				s.t = 0f;
			}
			s.seen = true;
			s.t = Math.min(1f, s.t + Game.elapsed/0.15f);
			float x = s.fromX + (s.toX - s.fromX)*s.t;
			float y = s.fromY + (s.toY - s.fromY)*s.t;
			s.sprite.x = x - s.sprite.width/2f;
			s.sprite.y = y - s.sprite.height/2f;
			s.sprite.visible = cell >= 0 && cell < Dungeon.level.heroFOV.length && Dungeon.level.heroFOV[cell];
		}
	}
}
