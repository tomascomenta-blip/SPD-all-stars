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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.hunt;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.continent.ash.fauna.AshColossus;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Texture;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.PointF;

//hunting the big ones: knocking them down, climbing them, taming one, the forge and eating their
//parts. what the pieces share: the party's one tamed beast and a few sheets
public class Hunt {

	public static final String PARTS      = "sprites/colosos/items_mutar.png";
	public static final String FORGE      = "sprites/colosos/forja_coloso.png";
	public static final String GRIP_FRAME = "sprites/colosos/agarre_marco.png";
	public static final String GRIP_FILL  = "sprites/colosos/agarre_relleno.png";
	public static final String GRIP_HAND  = "sprites/colosos/agarre_mano.png";
	public static final String TAMED_SUN  = "sprites/colosos/domado_mini.png";

	//the whole party gets one: its id while it lives, -1 otherwise
	public static int tamed = -1;

	public static void reset(){
		tamed = -1;
	}

	public static void store( Bundle b ){
		b.put("tamed", tamed);
	}

	public static void restore( Bundle b ){
		tamed = b.contains("tamed") ? b.getInt("tamed") : -1;
	}

	public static AshColossus tamedHere(){
		if (tamed == -1) return null;
		Actor a = Actor.findById(tamed);
		return a instanceof AshColossus && ((AshColossus) a).isAlive() ? (AshColossus) a : null;
	}

	//in the shadow of the tamed one: the lesser things leave you be and the dark doesn't grow
	public static boolean sheltered( Char ch ){
		AshColossus t = tamedHere();
		return t != null && ch != t && t.reach(ch.pos) <= 2;
	}

	//the tamed one lets go of whatever it scared off (it's running anyway) unless it's right there, and
	//of any fight once the party is well away from it
	public static boolean letsGo( AshColossus tamer, Char e ){
		if (e == null || !tamer.tamed) return false;
		Terror t = e.buff(Terror.class);
		if (t != null && t.object == tamer.id() && tamer.reach(e.pos) > 1) return true;
		int near = Integer.MAX_VALUE;
		for (Hero h : Party.living()) near = Math.min(near, tamer.reach(h.pos));
		return near != Integer.MAX_VALUE && near > tamer.half() + 8;
	}

	//nothing smaller than the tamed one comes near it
	public static boolean scares( AshColossus tamer, Mob m ){
		if (m == tamer || m.alignment != Char.Alignment.ENEMY || !m.isAlive()) return false;
		if (m instanceof AshColossus && ((AshColossus) m).size >= tamer.size) return false;
		return tamer.reach(m.pos) <= tamer.size / 2 + 4;
	}

	// *** the weak spot, drawn over the big ones ***

	private static final String GLOW = "hunt_weak_glow";

	//a ring of light a few pixels across
	public static Object glow(){
		if (TextureCache.contains(GLOW)) return GLOW;
		int s = 11;
		SmartTexture tx = TextureCache.create(GLOW, s, s);
		tx.filter(Texture.NEAREST, Texture.NEAREST);
		float c = (s - 1) / 2f;
		for (int y = 0; y < s; y++){
			for (int x = 0; x < s; x++){
				float d = (float) Math.sqrt((x - c) * (x - c) + (y - c) * (y - c));
				int a;
				if (d <= 1.2f) a = 0xFF;
				else if (d >= 3.5f && d <= 4.6f) a = 0xE0;
				else if (d < 3.5f) a = 0x50;
				else a = 0;
				int rgb = d <= 1.2f ? 0xFFFFF0 : 0xFFD860;
				tx.bitmap.drawPixel(x, y, (rgb << 8) | a);
			}
		}
		return GLOW;
	}

	//the mark that pulses over the weak spot of a big one, kept at its pixel by the sprite
	public static class WeakGlow extends Image {

		private float t;

		public WeakGlow(){
			super(glow());
			hardlight(1f, 0.85f, 0.4f);
		}

		public void at( PointF p ){
			x = p.x - width / 2f;
			y = p.y - height / 2f;
		}

		@Override
		public void update() {
			super.update();
			t += Game.elapsed;
			float k = 0.55f + 0.45f * (float) Math.sin(t * 5f);
			alpha(k);
			scale.set(0.8f + 0.25f * k);
		}
	}
}
