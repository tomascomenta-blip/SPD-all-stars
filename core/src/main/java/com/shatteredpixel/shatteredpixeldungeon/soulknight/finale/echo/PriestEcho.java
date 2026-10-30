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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.finale.echo;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BlastParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKBulletField;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKWorlds;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.finale.ThalVekhorr;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forest.PriestAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forgotten.ForgottenAssets;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.LinkedHashSet;

//echo of the goblin high priest. keeps away, bursting stars, meteors on marked cells, and the giant star:
//floats for a few turns over a 5x5 square around one hero. hit him hard enough and it breaks, otherwise it lands
public class PriestEcho extends EchoBoss {

	{
		spriteClass = Sprite.class;
		HP = HT = 1500;
		defenseSkill = 28;
	}

	private static final int METEORS = 0;
	private static final int STARS = 1;

	public static final int STAR_TURNS = 4;

	public int starLeft = 0;
	public int starCenter = -1;
	public int starResult = 0;  //1 landed, 2 broken. only for the drawing
	private int[] starCells = new int[0];
	private int starHurt = 0, starNeed = 0;
	private float starUntil = -1;
	private int starCooldown = 7;
	public int dazed = 0;
	private int lastSpell = -1;

	@Override
	public int world() {
		return SKWorlds.FOREST;
	}

	@Override public int damageRoll() { return Random.NormalIntRange(26, 40); }
	@Override public int attackSkill( Char target ) { return 42; }
	@Override public int drRoll() { return super.drRoll() + Random.NormalIntRange(0, 10); }

	@Override
	protected int specialCooldown() {
		return HP * 2 < HT ? Random.IntRange(2, 3) : 3;
	}

	//backs off now and then, not every turn. a melee hero could never catch him otherwise
	private int retreats = 1;
	private int retreatTimer = 0;

	@Override
	protected boolean keepsDistance() {
		return starLeft == 0 && dazed == 0 && retreats > 0;
	}

	@Override
	protected boolean getCloser( int target ) {
		return starLeft == 0 && dazed == 0 && super.getCloser(target);
	}

	@Override
	protected boolean getFurther( int target ) {
		if (starLeft > 0 || dazed > 0 || !super.getFurther(target)) return false;
		retreats--;
		return true;
	}

	@Override
	protected boolean act() {
		if (emerging() || !isAlive()) return super.act();
		if (dazed > 0){
			dazed--;
			spend(TICK);
			return true;
		}
		if (starLeft > 0){
			if (--starLeft == 0) starFalls();
			spend(TICK);
			return true;
		}
		if (starCooldown > 0) starCooldown--;
		if (++retreatTimer >= 5){
			retreatTimer = 0;
			retreats = Math.min(1, retreats + 1);
		}
		return super.act();
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (!isAlive() || emerging()) return;
		if (dmg > 0 && dazed > 0) dmg = Math.round(dmg * 1.4f);
		int before = HP;
		super.damage(dmg, src);
		if (isAlive() && starLeft > 0 && HP < before){
			starHurt += before - HP;
			if (starHurt >= starNeed) starBroken();
		}
	}

	@Override
	protected boolean special( Char enemy ) {
		if (!fieldOfView[enemy.pos]) return false;
		if (starCooldown <= 0 && Dungeon.level.distance(pos, enemy.pos) <= 8){
			callStar(enemy);
			spend(TICK);
			return true;
		}
		int spell = lastSpell == METEORS ? STARS : (lastSpell == STARS ? METEORS : Random.Int(2));
		lastSpell = spell;
		if (sprite != null) sprite.turnTo(pos, enemy.pos);
		if (sprite instanceof Sprite) ((Sprite) sprite).cast();
		if (spell == METEORS) return announceMeteors(enemy);
		castStars(enemy);
		spend(TICK);
		return true;
	}

	private void castStars( Char enemy ){
		SKBulletField field = SKBulletField.get();
		int n = HP * 2 < HT ? 8 : 6;
		float start = SKBulletField.angleTo(pos, enemy.pos) + 180f / n;
		for (int i = 0; i < n; i++){
			SKBulletField.Bullet b = field.fire(this, pos, start + i * 360f / n, 1f, ItemSpriteSheet.SHOT_STAR,
					ThalVekhorr.echoHit(22), ThalVekhorr.echoHit(32));
			b.behaviour = SKBulletField.SPLIT;
			b.timer = 3;
			b.life = 4;
			b.children = 3;
			b.spread = 40;
			b.childImage = ItemSpriteSheet.SHOT_BULLET;
			b.childSpeed = SKBulletField.UNIT * 3 / 2;
		}
		if (Dungeon.level.heroFOV[pos]){
			Sample.INSTANCE.play(Assets.Sounds.ZAP, 1f, 0.8f);
			GLog.w(Messages.get(this, "stars"));
		}
	}

	//one on each hero he sees, a couple more near his target
	private boolean announceMeteors( Char enemy ){
		ArrayList<Integer> centers = new ArrayList<>();
		for (Hero h : Party.living()){
			if (fieldOfView[h.pos]) centers.add(h.pos);
		}
		if (centers.isEmpty()) centers.add(enemy.pos);
		int wanted = centers.size() + 1;
		ArrayList<Integer> near = SKCombat.area(enemy.pos, 3);
		for (int tries = 0; centers.size() < wanted && tries < 30 && !near.isEmpty(); tries++){
			int c = near.get(Random.Int(near.size()));
			if (!centers.contains(c)) centers.add(c);
		}
		LinkedHashSet<Integer> cells = new LinkedHashSet<>();
		for (int c : centers) cells.addAll(SKCombat.openArea(c, 1));
		meteorCenters = new int[centers.size()];
		for (int i = 0; i < meteorCenters.length; i++) meteorCenters[i] = centers.get(i);
		announce(METEORS, cells, enemy.pos);
		if (Dungeon.level.heroFOV[pos]) GLog.w(Messages.get(this, "meteors"));
		return true;
	}

	private int[] meteorCenters = new int[0];

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		if (kind != METEORS) return;
		for (int c : meteorCenters){
			EchoFx.fall(c, ForgottenAssets.METEOR, 24, 48, 0, 1f, 140, 0.3f, BlastParticle.FACTORY, 6);
		}
		meteorCenters = new int[0];
		boolean seen = false;
		for (int c : cells){
			if (Dungeon.level.heroFOV[c]){
				seen = true;
				CellEmitter.get(c).burst(FlameParticle.FACTORY, 3);
			}
		}
		if (seen){
			Sample.INSTANCE.play(Assets.Sounds.BLAST);
			EchoFx.shake(2, 0.5f);
		}
		hitFoes(cells, 32, 44);
	}

	// *** the giant star ***

	private void callStar( Char enemy ){
		starCenter = enemy.pos;
		ArrayList<Integer> cells = SKCombat.area(starCenter, 2);
		starCells = new int[cells.size()];
		for (int i = 0; i < starCells.length; i++) starCells[i] = cells.get(i);
		starLeft = STAR_TURNS;
		starResult = 0;
		starHurt = 0;
		starNeed = Math.max(30, HT * 6 / 100);
		pendingKind = -1;
		float delay = Math.max(STAR_TURNS, SKCombat.announceDelay(TICK));
		starUntil = Actor.now() + delay;
		SKCombat.mark(cells, delay);
		EchoFx.later(new Runnable() { public void run() { GameScene.effect(new GiantStar(PriestEcho.this)); } });
		yell(Messages.get(this, "star_call"));
		GLog.w(Messages.get(this, "star_hint", starNeed));
		Sample.INSTANCE.play(Assets.Sounds.CHARGEUP, 1f, 0.6f);
		CoopLog.line("echo priest star t=" + (int) Actor.now() + " need=" + starNeed);
	}

	private void starBroken(){
		starLeft = 0;
		starResult = 2;
		starCooldown = 16;
		dazed = 3;
		EchoFx.unmark(starCells, starUntil);
		starCells = new int[0];
		if (sprite != null) sprite.showStatus(CharSprite.POSITIVE, Messages.get(this, "dazed"));
		GLog.p(Messages.get(this, "star_broken"));
		Sample.INSTANCE.play(Assets.Sounds.SHATTER, 1f, 0.8f);
		EchoFx.flash(0x50FFFFFF);
	}

	private void starFalls(){
		starResult = 1;
		starCooldown = 16;
		ArrayList<Integer> cells = new ArrayList<>();
		for (int c : starCells) cells.add(c);
		starCells = new int[0];
		for (int c : cells){
			if (Dungeon.level.heroFOV[c]) CellEmitter.center(c).burst(BlastParticle.FACTORY, 2);
		}
		Sample.INSTANCE.play(Assets.Sounds.BLAST, 1f, 0.5f);
		EchoFx.shake(6, 0.9f);
		EchoFx.flash(0x80FFF0C0);
		hitFoes(cells, 44, 60);
		GLog.n(Messages.get(this, "star_fell"));
	}

	@Override
	public void die( Object cause ) {
		starLeft = 0;
		starResult = 2;
		super.die(cause);
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("star_left", starLeft);
		bundle.put("star_center", starCenter);
		bundle.put("star_cells", starCells);
		bundle.put("star_hurt", starHurt);
		bundle.put("star_need", starNeed);
		bundle.put("star_until", starUntil);
		bundle.put("star_cd", starCooldown);
		bundle.put("dazed", dazed);
		bundle.put("retreats", retreats);
		bundle.put("retreat_timer", retreatTimer);
		bundle.put("last_spell", lastSpell);
		bundle.put("meteors", meteorCenters);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		starLeft = bundle.getInt("star_left");
		starCenter = bundle.getInt("star_center");
		starCells = bundle.getIntArray("star_cells");
		starHurt = bundle.getInt("star_hurt");
		starNeed = bundle.getInt("star_need");
		starUntil = bundle.getFloat("star_until");
		starCooldown = bundle.getInt("star_cd");
		dazed = bundle.getInt("dazed");
		retreats = bundle.getInt("retreats");
		retreatTimer = bundle.getInt("retreat_timer");
		lastSpell = bundle.getInt("last_spell");
		meteorCenters = bundle.getIntArray("meteors");
		if (starCells == null) starCells = new int[0];
		if (meteorCenters == null) meteorCenters = new int[0];
		if (starLeft > 0){
			EchoFx.later(new Runnable() { public void run() { GameScene.effect(new GiantStar(PriestEcho.this)); } });
		}
	}

	//comes down over the square while he calls it, then lands or bursts. giant_star.png: 0-2 falling
	//(centre at 30,30), 3-5 the impact (centre at 24,24), like the real one
	public static class GiantStar extends Image {

		private final PriestEcho echo;
		private final PointF at;
		private float t = 0, end = -1;
		private int result = 0;

		public GiantStar( PriestEcho echo ){
			super(PriestAssets.STAR);
			this.echo = echo;
			at = DungeonTilemap.tileCenterToWorld(echo.starCenter);
			frame(0, 0, 48, 48);
			origin.set(30, 30);
		}

		@Override
		public void update() {
			super.update();
			t += Game.elapsed;
			if (result == 0 && (echo.starLeft <= 0 || !echo.isAlive())){
				result = echo.starResult == 0 ? 2 : echo.starResult;
				end = 0;
				if (result == 2){
					CellEmitter.center(echo.starCenter).burst(Speck.factory(Speck.STAR), 10);
					CellEmitter.center(echo.starCenter).burst(EchoFx.SAND, 16);
				}
			}
			if (result == 0){
				float left = echo.starLeft / (float) STAR_TURNS;
				float height = (1.5f + 4.5f * left) * DungeonTilemap.SIZE;
				frame(((int) (t * 6) % 3) * 48, 0, 48, 48);
				scale.set(0.8f + 0.9f * (1 - left) + 0.05f * (float) Math.sin(t * 8));
				x = at.x - 30;
				y = at.y - 30 - height + (float) Math.sin(t * 2) * 2;
				return;
			}
			end += Game.elapsed;
			if (result == 1){
				frame((3 + Math.min(2, (int) (end * 8))) * 48, 0, 48, 48);
				origin.set(24, 24);
				scale.set(2f + end * 2f);
				x = at.x - 24;
				y = at.y - 24;
				am = Math.max(0, Math.min(1, 1 - (end - 0.3f) / 0.5f));
			} else {
				am = Math.max(0, 1 - end / 0.4f);
			}
			if (end > 0.8f) killAndErase();
		}
	}

	/* same frames as the real priest: sk_goblin_priest.png (20x20) and priest_rite.png for floating and dazed */
	public static class Sprite extends EchoSprite {

		private final TextureFilm thin, rite;
		private Animation cast, floatAnim, dazedAnim;
		private int look = 0;  //0 normal, 1 floating, 2 dazed

		public Sprite(){
			super();
			texture(PriestAssets.RITE);
			rite = new TextureFilm(texture, 20, 20);
			texture(SKAssets.GOBLIN_PRIEST);
			thin = new TextureFilm(texture, 20, 20);
			thinLook();
			echo(1f);
			play(idle);
		}

		private void thinLook(){
			texture(SKAssets.GOBLIN_PRIEST);
			idle = anim(thin, 2, true, 0, 0, 0, 1, 0, 0, 1, 1);
			run = anim(thin, 12, true, 4, 5, 6, 7);
			attack = anim(thin, 12, false, 2, 3, 0);
			zap = attack.clone();
			cast = attack.clone();
			die = anim(thin, 12, false, 8, 9, 10);
		}

		private void riteLook( boolean dazed ){
			texture(PriestAssets.RITE);
			floatAnim = anim(rite, 3, true, 3, 4);
			dazedAnim = anim(rite, 4, true, 5, 6);
			idle = dazed ? dazedAnim : floatAnim;
		}

		private int wanted(){
			if (!(ch instanceof PriestEcho) || !ch.isAlive()) return look;
			PriestEcho p = (PriestEcho) ch;
			return p.starLeft > 0 ? 1 : (p.dazed > 0 ? 2 : 0);
		}

		public void cast(){
			if (look == 0) play(cast);
		}

		@Override
		public void update() {
			int w = wanted();
			if (w != look && !isMoving && (curAnim == null || curAnim.looped)){
				look = w;
				if (w == 0) thinLook();
				else riteLook(w == 2);
				play(idle, true);
			}
			super.update();
			float r = (6 + (look == 1 ? 4 + 1.5f * (float) Math.sin(t * 3) : 0)) / 16f;
			if (r != perspectiveRaise){
				perspectiveRaise = r;
				if (!isMoving && ch != null && !appearing()) place(ch.pos);
			}
		}

		@Override
		public void die() {
			if (look != 0){
				look = 0;
				thinLook();
			}
			super.die();
		}

		@Override
		public int blood() {
			return 0xFF7FE8FF;
		}
	}
}
