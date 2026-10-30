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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.props.SKProps;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

//forest ritual totems, one per floor 1-4, each one carries its floor's power to the high priest.
//any still standing when you reach the clearing -> he drinks it and turns into the brute w/ 2x hp (see GoblinPriest)
public class RitualTotems {

	public static final int FLOORS = 4;

	public static int placed = 0;
	public static int broken = 0;

	public static void reset(){
		placed = 0;
		broken = 0;
	}

	public static void storeInBundle( Bundle bundle ){
		Bundle b = new Bundle();
		b.put("placed", placed);
		b.put("broken", broken);
		bundle.put("forest_ritual", b);
	}

	public static void restoreFromBundle( Bundle bundle ){
		if (!bundle.contains("forest_ritual")){
			reset();
			return;
		}
		Bundle b = bundle.getBundle("forest_ritual");
		placed = b.getInt("placed");
		broken = b.getInt("broken");
	}

	public static int standing(){
		return Math.max(0, placed - broken);
	}

	//true if the priest comes out as the brute
	public static boolean empowered(){
		String t = DevFlags.priestBuff();
		if (t != null) return "1".equals(t);
		return standing() > 0;
	}

	//one totem per floor, room farthest from the entrance
	public static void place( Level level, ArrayList<Room> rooms ){
		if (Dungeon.depth < 1 || Dungeon.depth > FLOORS) return;
		Room entrance = null;
		for (Room r : rooms) if (r.isEntrance()) entrance = r;
		ArrayList<Room> options = new ArrayList<>();
		for (Room r : rooms){
			if (r instanceof StandardRoom && !r.isEntrance() && !r.isExit() && r.width() >= 5 && r.height() >= 5){
				options.add(r);
			}
		}
		Random.shuffle(options);
		if (entrance != null){
			final Room from = entrance;
			options.sort((a, b) -> Float.compare(
					distance(from, b), distance(from, a)));
		}
		int cell = -1;
		for (Room r : options){
			cell = SKProps.nearCenter(level, r);
			if (cell != -1) break;
		}
		if (cell == -1){
			//fallback: any open cell far from the entrance, 200 tries y ya
			for (int tries = 0; tries < 200 && cell == -1; tries++){
				int c = Random.Int(level.length());
				if (level.passable[c] && !level.solid[c] && level.map[c] != Terrain.DOOR && level.findMob(c) == null
						&& level.distance(c, level.entrance()) > 6) cell = c;
			}
		}
		if (cell == -1) return;
		FloorTotem t = new FloorTotem();
		t.pos = cell;
		level.mobs.add(t);
		placed++;
		CoopLog.line("ritual totem placed depth=" + Dungeon.depth + " cell=" + cell + " placed=" + placed);
	}

	private static float distance( Room a, Room b ){
		float dx = a.center().x - b.center().x, dy = a.center().y - b.center().y;
		return dx * dx + dy * dy;
	}

	//a floor's totem. doesn't fight, but nearby goblins come running when it gets hit
	public static class FloorTotem extends SKProps.SKProp {

		{
			spriteClass = TotemSprite.class;
		}

		private boolean announced = false;
		private boolean called = false;

		public FloorTotem(){
			super();
			HP = HT = 16 + 3 * Dungeon.scalingDepth();
		}

		@Override
		protected boolean act() {
			if (!announced){
				announced = true;
				GLog.w(Messages.get(RitualTotems.class, "sensed"));
			}
			spend(TICK * 5);
			return true;
		}

		@Override
		public void damage( int dmg, Object src ) {
			if (dmg > 0 && !called){
				called = true;
				Hero near = Party.nearestLiving(pos);
				if (near != null){
					for (Mob m : Dungeon.level.mobs){
						if ((ForestFeatures.isGoblin(m) || m instanceof Boar) && m.state != m.HUNTING
								&& Dungeon.level.distance(m.pos, pos) <= 8){
							m.beckon(near.pos);
						}
					}
				}
				if (Dungeon.level.heroFOV[pos]) GLog.w(Messages.get(RitualTotems.class, "struck"));
			}
			super.damage(dmg, src);
		}

		@Override
		public void die( Object cause ) {
			super.die(cause);
			broken++;
			CoopLog.line("ritual totem broken depth=" + Dungeon.depth + " broken=" + broken + "/" + placed);
			if (Dungeon.level.heroFOV[pos]){
				CellEmitter.get(pos).burst(ShadowParticle.UP, 10);
				CellEmitter.get(pos).burst(FlameParticle.FACTORY, 6);
			}
			Sample.INSTANCE.play(Assets.Sounds.SHATTER, 1f, 0.7f);
			GLog.p(Messages.get(RitualTotems.class, "broken", broken, FLOORS));
			if (broken >= placed && placed >= FLOORS) GLog.p(Messages.get(RitualTotems.class, "all_broken"));
		}

		@Override
		public boolean add( Buff buff ) {
			return false;
		}

		@Override
		public String description() {
			return Messages.get(RitualTotems.class, "desc");
		}

		@Override
		public String name() {
			return Messages.get(RitualTotems.class, "name");
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("announced", announced);
			bundle.put("called", called);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			announced = bundle.getBoolean("announced");
			called = bundle.getBoolean("called");
		}
	}

	//ritual_totem.png 20x32, rows of 8 (0-3 pulse, 4 hit, 5 cracked, 6 breaking, 7 rubble)
	public static class TotemSprite extends MobSprite {

		private int row = -1;
		private boolean cracked = false;
		private TextureFilm film;
		//the shake when it's hit. NOT 'attack': that one makes the totem attack when it ends, and only
		//on the devices that saw the hit -> the old desync of the priest fight
		private Animation flinch;

		public TotemSprite(){
			super();
			texture(PriestAssets.TOTEM);
			film = new TextureFilm(texture, 20, 32);
			setRow(PriestAssets.TOTEM_FLOOR);
		}

		protected int wantedRow(){
			return PriestAssets.TOTEM_FLOOR;
		}

		protected void setRow( int r ){
			row = r;
			int o = r * 8;
			idle = new Animation(4, true);
			if (cracked) idle.frames(film, o + 5);
			else idle.frames(film, o, o + 1, o + 2, o + 3, o + 2, o + 1);
			run = idle.clone();
			attack = new Animation(10, false);
			attack.frames(film, o + 4, o + 4, cracked ? o + 5 : o);
			zap = attack.clone();
			flinch = attack.clone();
			die = new Animation(8, false);
			die.frames(film, o + 6, o + 7);
			play(idle, true);
		}

		@Override
		public void update() {
			super.update();
			if (ch == null || !ch.isAlive()) return;
			boolean c = ch.HP * 2 < ch.HT;
			int r = wantedRow();
			if ((c != cracked || r != row) && (curAnim == idle || curAnim == null)){
				cracked = c;
				setRow(r);
			}
		}

		@Override
		public void bloodBurstA( PointF from, int damage ) {
			super.bloodBurstA(from, damage);
			play(flinch);
		}

		@Override
		public void onComplete( Animation anim ) {
			super.onComplete(anim);
			if (anim == attack || anim == flinch) play(idle);
		}

		@Override
		public int blood() {
			return 0xFF7A4A2A;
		}
	}
}
