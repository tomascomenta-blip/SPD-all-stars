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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.kraken.Kraken;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.kraken.KrakenCall;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopRewards;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BlastParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKAssets;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKBoss;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKBulletField;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.kingdom.KingdomMobs;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.LinkedHashSet;

/* abyssal submarine, boss of the Undersea
   sonar pulses that mark whoever they touch, homing torpedoes, sea mines on marked cells (more around
   the marked heroes), fast shots w/ a trail. enraged: opens the rear door to let the crew out and rams
   along a marked line. takes half dmg while the crew it let out is alive */
public class AbyssalSubmariner extends SKBoss {

	{
		spriteClass = Sprite.class;
		HP = HT = Dungeon.isChallenged(Challenges.STRONGER_BOSSES) ? 400 : 330;
		defenseSkill = 20;
		baseSpeed = 0.75f;
		EXP = 40;
		properties.add(Property.INORGANIC);
	}

	private int[] crew = new int[0];

	private boolean crewAlive(){
		for (int id : crew){
			Actor a = Actor.findById(id);
			if (a instanceof Char && ((Char) a).isAlive()) return true;
		}
		return false;
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (dmg > 0 && crewAlive()){
			dmg = Math.max(1, dmg/2);
			if (sprite != null && Dungeon.level.heroFOV[pos]) sprite.showStatus(CharSprite.NEUTRAL, Messages.get(AbyssalSubmariner.class, "covered"));
		}
		super.damage(dmg, src);
	}

	@Override
	protected boolean act() {
		if (sprite != null){
			if (crewAlive()) sprite.add(CharSprite.State.SHIELDED);
			else sprite.remove(CharSprite.State.SHIELDED);
		}
		return super.act();
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("crew", crew);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		crew = bundle.contains("crew") ? bundle.getIntArray("crew") : new int[0];
	}

	private static final int SONAR = 0;
	private static final int MINES = 1;
	private static final int RAM   = 2;

	@Override public int damageRoll() { return Random.NormalIntRange(10, 20); }
	@Override public int attackSkill( Char target ) { return 28; }
	@Override public int drRoll() { return super.drRoll() + Random.NormalIntRange(0, 8); }

	@Override protected int scoreIndex() { return 3; }
	@Override protected int score() { return 4000; }

	@Override
	protected boolean special( Char enemy ) {
		if (!fieldOfView[enemy.pos]) return false;
		int spell = pickSpell(enraged ? 6 : 4);
		boolean visible = Dungeon.level.heroFOV[pos];
		if (sprite != null){
			sprite.turnTo(pos, enemy.pos);
			if (sprite instanceof SKMobSprite) ((SKMobSprite) sprite).cast();
		}
		switch (spell){
			case 0: default: {
				//sonar ring at 2-3 cells
				LinkedHashSet<Integer> ring = new LinkedHashSet<>();
				for (int c : SKCombat.openArea(pos, 3)){
					if (Dungeon.level.distance(pos, c) >= 2) ring.add(c);
				}
				announce(SONAR, ring, enemy.pos);
				if (visible) GLog.w(Messages.get(this, "sonar"));
				return true;
			}
			case 1: {
				float aim = SKBulletField.angleTo(pos, enemy.pos);
				int n = enraged ? 6 : 4;
				for (int i = 0; i < n; i++){
					SKBulletField.Bullet t = shoot(aim - 50 + i*100f/(n-1), 1f + speedBonus(), ItemSpriteSheet.SHOT_TORPEDO, 6, 12);
					t.behaviour = SKBulletField.HOMING;
					t.spread = 30;
					t.life = 10;
				}
				if (visible) GLog.w(Messages.get(this, "torpedoes"));
				break;
			}
			case 2: {
				//mines: some random, more around the marked heroes
				LinkedHashSet<Integer> cells = new LinkedHashSet<>();
				ArrayList<Integer> room = SKCombat.area(pos, 6);
				int random = enraged ? 10 : 6;
				for (int i = 0; i < random && !room.isEmpty(); i++){
					cells.add(room.get(Random.Int(room.size())));
				}
				for (Hero h : Party.living()){
					if (h.buff(SonarMark.class) != null){
						for (int c : SKCombat.area(h.pos, 1)) if (Random.Int(2) == 0) cells.add(c);
						cells.add(h.pos);
					}
				}
				announce(MINES, cells, enemy.pos);
				if (visible) GLog.w(Messages.get(this, "mines"));
				return true;
			}
			case 3: {
				for (int i = 0; i < 5; i++){
					SKBulletField.Bullet b = shoot(Random.Int(360), 2f + speedBonus(), ItemSpriteSheet.SHOT_BLADE, 4, 9);
					b.behaviour = SKBulletField.TRAIL;
					b.period = 2;
					b.childImage = ItemSpriteSheet.SHOT_BUBBLE;
					b.life = 7;
				}
				if (visible) GLog.w(Messages.get(this, "trail"));
				break;
			}
			case 4: {
				//crew comes out (and covers it while alive). kill the crew first
				int made = 0;
				ArrayList<Integer> ids = new ArrayList<>();
				for (int id : crew){
					Actor a = Actor.findById(id);
					if (a instanceof Char && ((Char) a).isAlive()) ids.add(id);
				}
				for (int c : SKCombat.area(pos, 2)){
					if (made >= 2) break;
					if (Actor.findChar(c) != null || !Dungeon.level.passable[c]) continue;
					Mob m = Random.Int(2) == 0 ? new UnderseaMobs.InfectedCrew() : new UnderseaMobs.KrakenMinion();
					m.pos = c;
					m.state = m.HUNTING;
					GameScene.add(m);
					ids.add(m.id());
					made++;
				}
				crew = new int[ids.size()];
				for (int i = 0; i < crew.length; i++) crew[i] = ids.get(i);
				if (made > 0 && visible) GLog.w(Messages.get(this, "covered_by_crew"));
				if (visible) GLog.w(Messages.get(this, "crew"));
				break;
			}
			case 5: {
				ArrayList<Integer> line = line(enemy.pos, 7);
				if (Dungeon.level.distance(pos, enemy.pos) >= 2 && line.contains(enemy.pos)){
					announce(RAM, line, enemy.pos);
					if (visible) GLog.w(Messages.get(this, "ram"));
					return true;
				}
				ring(12, enemy.pos, 1f, ItemSpriteSheet.SHOT_BUBBLE, 5, 10);
				break;
			}
		}
		if (visible) Sample.INSTANCE.play(Assets.Sounds.WATER, 1f, 0.7f);
		spend(TICK);
		return true;
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		switch (kind){
			case SONAR:
				if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.CHARGEUP, 1f, 0.5f);
				for (Char ch : SKCombat.foesIn(this, cells)){
					ch.damage(Math.max(1, Random.NormalIntRange(4, 9) - ch.drRoll()/2), this);
					if (ch.isAlive()){
						Buff.prolong(ch, Weakness.class, 5f);
						Buff.prolong(ch, SonarMark.class, 10f);
					}
				}
				break;
			case MINES: {
				boolean any = false;
				for (int c : cells){
					if (Dungeon.level.heroFOV[c]){
						any = true;
						CellEmitter.center(c).burst(BlastParticle.FACTORY, 4);
					}
				}
				if (any){
					Sample.INSTANCE.play(Assets.Sounds.BLAST);
					PixelScene.shake(2, 0.5f);
				}
				hitFoes(cells, 8, 14);
				break;
			}
			case RAM:
				KingdomMobs.charge(this, cells, 2f);
				break;
		}
	}

	//plot twist: the sub was only holding the Kraken back. its rewards go to the Kraken. when it sinks every hero
	//gets 2 healing potions straight into the pack, and a few turns later the sea takes the party to the real
	//fight (KrakenCall)
	@Override
	protected boolean dropsRewards() {
		return false;
	}

	@Override
	public void die( Object cause ) {
		super.die(cause);
		for (Hero h : Kraken.partyHeroes()){
			PotionOfHealing p =
					new PotionOfHealing();
			p.quantity(2);
			p.identify();
			Kraken.give(h, p);
		}
		GLog.p(Messages.get(AbyssalSubmariner.class, "potions"));
		Hero leader = Party.active() ? Party.heroes().get(0) : Dungeon.hero;
		if (leader != null && !Dungeon.levelHasBeenGenerated(20, 2)){
			Buff.affect(leader,
					KrakenCall.class);
		}
	}

	@Override
	protected ArrayList<Item> loot() {
		ArrayList<Item> items = new ArrayList<>();
		//Dwarf King's loot: the crown, one per hero that needs it
		items.addAll(CoopRewards.crowns());
		items.add(new AbyssSonar().random());
		return items;
	}

	//sonar-marked, the mines go for this one
	public static class SonarMark extends FlavourBuff {

		{
			type = buffType.NEGATIVE;
			announced = true;
		}

		@Override
		public int icon() {
			return BuffIndicator.MARK;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(1f, 0.2f, 0.2f);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", dispTurns());
		}
	}

	public static class Sprite extends SKMobSprite {
		public Sprite(){
			super();
			setup(SKAssets.SUBMARINER, 30, 22);
		}
		@Override
		public int blood() {
			return 0xFF666677;
		}
	}
}
