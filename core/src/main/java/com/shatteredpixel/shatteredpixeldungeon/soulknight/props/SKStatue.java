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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.props;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKWorlds;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

//statue on the first floor of each new world. pay gold -> blessing until you leave that world
//(more hp / heal when mobs die nearby / faster recharge). one per hero per world, no stacking, i checked
public class SKStatue extends NPC {

	{
		spriteClass = StatueSprite.class;
		properties.add(Property.IMMOVABLE);
	}

	public static final int KNIGHT = 0, PRIESTESS = 1, WIZARD = 2;

	public static int cost(){
		return 40 * SKWorlds.region(Dungeon.depth);
	}

	@Override
	protected boolean act() {
		spend(TICK*5);
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
		final Hero hero = (Hero) c;
		sprite.turnTo(pos, hero.pos);
		final int world = SKWorlds.region(Dungeon.depth);
		Blessing old = hero.buff(Blessing.class);
		if (old != null && old.world == world){
			GLog.i(Messages.get(SKStatue.class, "already"));
			return true;
		}
		final int cost = cost();
		//window on the render thread (coop tracks whose window it is)
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				showOffer(hero, world, cost);
			}
		});
		return true;
	}

	private void showOffer( final Hero hero, final int world, final int cost ){
		GameScene.show(new WndOptions(
				Messages.get(SKStatue.class, "name"),
				Messages.get(SKStatue.class, "offer", cost),
				Messages.get(SKStatue.class, "knight"),
				Messages.get(SKStatue.class, "priestess"),
				Messages.get(SKStatue.class, "wizard"),
				Messages.get(SKStatue.class, "leave")){
			@Override
			protected void onSelect( int index ) {
				if (index < 0 || index > WIZARD) return;
				if (Dungeon.gold < cost){
					GLog.n(Messages.get(SKStatue.class, "no_gold", cost));
					return;
				}
				Dungeon.gold -= cost;
				Blessing b = hero.buff(Blessing.class);
				if (b != null) b.detach();
				b = Buff.affect(hero, Blessing.class);
				b.set(world, index);
				if (sprite != null){
					((SKMobSprite) sprite).cast();
					sprite.emitter().burst(Speck.factory(Speck.STAR), 10);
				}
				hero.sprite.emitter().burst(Speck.factory(Speck.UP), 6);
				Sample.INSTANCE.play(Assets.Sounds.MASTERY, 0.7f, 1.1f);
				GLog.p(Messages.get(SKStatue.class, "blessed_" + index));
				CoopLog.line("statue blessing " + index + " world=" + world
						+ " hero HT=" + hero.HT + " gold=" + Dungeon.gold);
			}
		});
	}

	public static class Blessing extends Buff {

		{
			type = buffType.POSITIVE;
			revivePersists = true;
		}

		public int world;
		public int kind;

		void set( int world, int kind ){
			this.world = world;
			this.kind = kind;
			if (kind == KNIGHT && target instanceof Hero) ((Hero) target).updateHT(true);
		}

		@Override
		public boolean act() {
			spend(TICK);
			//se va al salir del mundo
			if (!SKWorlds.isSK(Dungeon.depth) || SKWorlds.region(Dungeon.depth) != world){
				detach();
			}
			return true;
		}

		@Override
		public void detach() {
			super.detach();
			if (kind == KNIGHT && target instanceof Hero) ((Hero) target).updateHT(false);
		}

		@Override
		public int icon() {
			return BuffIndicator.BLESS;
		}

		@Override
		public void tintIcon( Image icon ) {
			switch (world){
				case SKWorlds.FOREST:   icon.hardlight(0x6CC26F); break;
				case SKWorlds.KINGDOM:  icon.hardlight(0xF07A84); break;
				case SKWorlds.STARSHIP: icon.hardlight(0xC591EC); break;
				default:                icon.hardlight(0x4FC7D6); break;
			}
		}

		@Override
		public String name() {
			return Messages.get(SKStatue.class, "blessing_" + kind);
		}

		@Override
		public String desc() {
			return Messages.get(SKStatue.class, "blessing_desc_" + kind);
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("world", world);
			bundle.put("kind", kind);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			world = bundle.getInt("world");
			kind = bundle.getInt("kind");
		}
	}

	private static Blessing blessing( Char ch, int kind ){
		Blessing b = ch == null ? null : ch.buff(Blessing.class);
		return b != null && b.kind == kind ? b : null;
	}

	//knight: +20% hp (Hero.updateHT)
	public static int htBonus( Hero hero, int ht ){
		return blessing(hero, KNIGHT) != null ? Math.round(ht * 0.2f) : 0;
	}

	//wizard: wands+guns 1/3 faster (Wand.Charger)
	public static float chargeMultiplier( Char target ){
		return blessing(target, WIZARD) != null ? 1.3f : 1f;
	}

	//priestess: heal when a mob dies near you
	public static void onMobDeath( Mob m ){
		for (Hero h : Party.living()){
			if (blessing(h, PRIESTESS) == null || Dungeon.level.distance(h.pos, m.pos) > 5) continue;
			int heal = Math.min(h.HT - h.HP, 2 + SKWorlds.region(Dungeon.depth));
			if (heal <= 0) continue;
			h.HP += heal;
			if (h.sprite != null){
				h.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
			}
		}
	}

	//levelgen: 1st floor of each world, near the stairs
	public static void place( Level level, ArrayList<Room> rooms ){
		if (Dungeon.depth % 5 != 1) return;
		int entrance = level.entrance();
		if (entrance < 0) return;
		int best = -1;
		for (int c = 0; c < level.length(); c++){
			int d = level.distance(c, entrance);
			if (d < 2 || d > 4 || !fits(level, c)) continue;
			if (best == -1 || d < level.distance(best, entrance) || (d == level.distance(best, entrance) && c < best)) best = c;
		}
		if (best == -1) return;
		SKStatue s = new SKStatue();
		s.pos = best;
		level.mobs.add(s);
	}

	private static boolean fits( Level level, int c ){
		if (!level.passable[c] || level.pit[c] || level.water[c]) return false;
		if (level.heaps.get(c) != null || level.traps.get(c) != null || level.plants.get(c) != null) return false;
		int t = level.map[c];
		if (t != Terrain.EMPTY && t != Terrain.EMPTY_SP && t != Terrain.EMPTY_DECO && t != Terrain.GRASS) return false;
		for (int n : PathFinder.NEIGHBOURS4){
			if (!level.passable[c + n]) return false;
		}
		for (int n : PathFinder.NEIGHBOURS8){
			int k = level.map[c + n];
			if (k == Terrain.DOOR || k == Terrain.OPEN_DOOR || k == Terrain.ENTRANCE || k == Terrain.EXIT) return false;
		}
		for (Mob m : level.mobs){
			if (level.distance(m.pos, c) <= 1) return false;
		}
		return true;
	}

	public static class StatueSprite extends SKMobSprite {
		public StatueSprite(){
			super();
			setup(SKAssets.WORLD_STATUE, 16, 24);
			int world = SKWorlds.region(Dungeon.depth);
			if (world >= 1 && world <= 4) row(world - 1);
		}
	}
}
