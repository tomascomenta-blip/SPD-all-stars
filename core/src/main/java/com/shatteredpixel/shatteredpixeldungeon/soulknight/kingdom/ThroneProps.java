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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.kingdom;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKBulletField;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forgotten.ForgottenFeatures;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.props.SKProps;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.watabou.noosa.Game;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

//throne hall props
//- columns: stop bullets. a charge/thrust into one stuns the knight (2 crashes and it collapses)
//- shield walls, low cover vs bullets, break after some hits
//- chandelier rope cleats: a hero cuts the rope and the chandelier drops on whoever's below
//- braziers, light up when the knight wakes
//- kingdom banner. he plants it at half hp, less dmg for him while it stands
public class ThroneProps {

	//nothing hurts it
	public static abstract class Solid extends SKProps.SKProp {
		{
			HP = HT = 1000;
		}

		@Override
		public boolean isInvulnerable( Class effect ) {
			return true;
		}

		@Override
		public void damage( int dmg, Object src ) {
		}
	}

	//dmg from the heroes' side? (their hits, bullets, allies)
	static boolean fromHeroes( Object src ){
		Char ch = null;
		if (src instanceof Char) ch = (Char) src;
		else if (src instanceof SKBulletField.Shot) ch = ((SKBulletField.Shot) src).owner;
		return ch != null && ch.alignment == Char.Alignment.ALLY;
	}

	// *** columns ***

	public static class Column extends Solid {

		{
			spriteClass = ColumnSprite.class;
		}

		public int cracks = 0;

		//knight crashed into it lol
		public void crash(){
			cracks++;
			if (Dungeon.level.heroFOV[pos]){
				Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 0.8f);
				CellEmitter.get(pos).burst(Speck.factory(Speck.ROCK), 6);
			}
			if (cracks >= 2){
				int at = pos;
				destroy();
				if (sprite != null) sprite.killAndErase();
				if (Dungeon.level instanceof KingdomBossLevel) ((KingdomBossLevel) Dungeon.level).rubble(at);
			}
		}

		@Override
		public String description() {
			return Messages.get(this, cracks > 0 ? "desc_cracked" : "desc");
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("cracks", cracks);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			cracks = bundle.getInt("cracks");
		}
	}

	public static class ColumnSprite extends Strip {
		public ColumnSprite(){
			super();
			perspectiveRaise = 0;
			strip(ThroneAssets.COLUMN, 16, 32, 1);
			still(0);
		}
		@Override
		public void update() {
			super.update();
			if (ch instanceof Column) still(Math.min(1, ((Column) ch).cracks));
		}
	}

	public static class ShieldWall extends SKProps.SKProp {

		{
			spriteClass = ShieldWallSprite.class;
			HP = HT = 18;
		}

		@Override
		public void damage( int dmg, Object src ) {
			if (dmg > 0 && Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.HIT_PARRY, 0.6f, 1.2f);
			super.damage(dmg, src);
		}

		@Override
		public String description() {
			return Messages.get(this, "desc");
		}
	}

	public static class ShieldWallSprite extends Strip {
		public ShieldWallSprite(){
			super();
			perspectiveRaise = 0;
			strip(ThroneAssets.SHIELD_WALL, 16, 16, 1);
			still(0);
		}
		@Override
		public void update() {
			super.update();
			if (ch != null && ch.isAlive()) still(ch.HP * 3 > ch.HT * 2 ? 0 : ch.HP * 3 > ch.HT ? 1 : 2);
		}
		@Override
		public int blood() {
			return 0xFF9A8F80;
		}
	}

	// *** chandelier rope ***

	public static class RopeCleat extends SKProps.SKProp {

		{
			spriteClass = CleatSprite.class;
			HP = HT = 1000;
		}

		public int chandelier = -1; //index of its chandelier in the hall
		public boolean cut = false;

		@Override
		public void damage( int dmg, Object src ) {
			//only heroes can cut it, the knight's own bullets don't
			if (cut || !fromHeroes(src)) return;
			cut = true;
			if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.HIT_SLASH, 1f, 1.3f);
			if (Dungeon.level instanceof KingdomBossLevel) ((KingdomBossLevel) Dungeon.level).dropChandelier(chandelier);
		}

		@Override
		public String description() {
			return Messages.get(this, cut ? "desc_cut" : "desc");
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("chandelier", chandelier);
			bundle.put("cut", cut);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			chandelier = bundle.getInt("chandelier");
			cut = bundle.getBoolean("cut");
		}
	}

	public static class CleatSprite extends Strip {
		public CleatSprite(){
			super();
			perspectiveRaise = 0;
			strip(ThroneAssets.CLEAT, 16, 16, 1);
			still(0);
		}
		@Override
		public void update() {
			super.update();
			if (ch instanceof RopeCleat) still(((RopeCleat) ch).cut ? 1 : 0);
		}
		//drawn on the side wall next to its cell, rope going up toward the hall
		@Override
		public PointF worldToCamera( int cell ) {
			PointF p = super.worldToCamera(cell);
			boolean rightWall = Dungeon.level.solid[cell + 1];
			flipHorizontal = rightWall;
			p.x += rightWall ? 7 : -7;
			return p;
		}
	}

	public static class Brazier extends Solid {

		{
			spriteClass = BrazierSprite.class;
		}

		public boolean lit(){
			DarkGrandKnight k = KingdomBossLevel.knight(Dungeon.level);
			return k == null || k.awake();
		}

		@Override
		protected boolean act() {
			if (lit() && Dungeon.level.heroFOV[pos] && sprite != null && Random.Int(3) == 0){
				sprite.emitter().burst(FlameParticle.FACTORY, 1);
			}
			spend(TICK);
			return true;
		}

		@Override
		public String description() {
			return Messages.get(this, "desc");
		}
	}

	public static class BrazierSprite extends Strip {
		private float t = 0;
		public BrazierSprite(){
			super();
			perspectiveRaise = 0;
			strip(ThroneAssets.BRAZIER, 16, 24, 1);
			still(0);
		}
		@Override
		public void update() {
			super.update();
			t += Game.elapsed;
			boolean lit = !(ch instanceof Brazier) || ((Brazier) ch).lit();
			still(lit ? 1 + ((int)(t * 8)) % 3 : 0);
		}
	}

	// *** kingdom banner ***

	public static class Banner extends SKProps.SKProp {

		{
			spriteClass = BannerSprite.class;
		}

		public Banner(){
			super();
			//tougher w/ more heroes. TODO maybe 25 per hero? needs a 4p test
			HP = HT = 30 + 20 * Math.max(1, Party.living().size());
		}

		@Override
		public void damage( int dmg, Object src ) {
			//the knight never hurts his own banner, ni por accidente
			if (!fromHeroes(src)) return;
			super.damage(dmg, src);
			if (isAlive() && sprite != null) sprite.flash();
		}

		@Override
		public void die( Object cause ) {
			super.die(cause);
			DarkGrandKnight k = KingdomBossLevel.knight(Dungeon.level);
			if (k != null) k.bannerFell();
		}

		@Override
		public String description() {
			return Messages.get(this, "desc");
		}
	}

	public static class BannerSprite extends MobSprite {
		public BannerSprite(){
			super();
			texture(ThroneAssets.BANNER);
			TextureFilm film = new TextureFilm(texture, 24, 40);
			idle = new Animation(6, true);
			idle.frames(film, 0, 1, 2, 3);
			run = idle.clone();
			attack = idle.clone();
			zap = idle.clone();
			die = new Animation(8, false);
			die.frames(film, 5, 6, 7);
			play(idle);
		}
		@Override
		public int blood() {
			return 0xFFB0303A;
		}
	}

	//one-strip sprite, the thing picks the frame
	public static class Strip extends ForgottenFeatures.StripSprite {
	}
}
