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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.Beam;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.levels.CityBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//great golem. each lit rune = 1/6 less dmg taken + a bit of regen per turn. standing on a rune puts it out
// I   slams, ground pound (marked ring, 2 cells)
// II  60%: beams from the lit runes (marked lines), relights a rune every so often (yes on purpose)
// III 30%: faster, throws boulders
public class GreatGolem extends MiniBoss {

	{
		spriteClass = GreatGolemSprite.class;
		HP = HT = 260;
		defenseSkill = 12;
		EXP = 40;
		maxLvl = 24;
		lootChance = 0;
		properties.add(Property.INORGANIC);
		properties.add(Property.LARGE);
	}

	private static final int POUND = 0, BEAM = 1;
	public boolean guard = false;    //the one guarding the dwarf king's throne
	private int rekindle = 15;
	private int beamRune = -1;

	@Override public String barAsset() { return QuestAssets.BAR_GOLEM; }
	@Override protected String music() { return Assets.Music.CITY_TENSE; }

	@Override public int damageRoll() { return Random.NormalIntRange(18, 28 + 2 * phase); }
	@Override public int attackSkill( Char target ) { return 30; }
	@Override public int drRoll() { return super.drRoll() + Random.NormalIntRange(4, 12); }

	@Override
	public float speed() {
		return super.speed() * (phase >= 3 ? 1.1f : 0.8f);
	}

	@Override
	public float attackDelay() {
		return super.attackDelay() * 1.2f;
	}

	private int lit(){
		return guard ? 0 : GolemQuest.Q.litCount();
	}

	@Override
	public String status() {
		String p = Messages.get(GreatGolem.class, "status_" + Math.min(3, phase));
		if (guard) return p;
		return p + "   " + Messages.get(GreatGolem.class, "status_runes", lit());
	}

	@Override
	public int barColor() {
		return lit() > 0 ? 0xFF6AA0FF : 0xFFB0805A;
	}

	@Override
	public String subtitle() {
		return Messages.get(GreatGolem.class, guard ? "subtitle_guard" : "subtitle");
	}

	@Override
	protected void onStart() {
		yell(Messages.get(GreatGolem.class, guard ? "notice_guard" : "notice"));
		Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 0.5f);
		PixelScene.shake(3, 0.6f);
	}

	//at the throne the king's music keeps going, the golem doesn't get its own song. pobre
	@Override
	public void playMusic() {
		if (!guard) super.playMusic();
	}

	@Override
	protected boolean act() {
		//runes can be put out while it still sleeps too
		if (!guard){
			ArrayList<Hero> list = new ArrayList<>();
			if (Party.active()) list.addAll(Party.living());
			else if (Dungeon.hero != null && Dungeon.hero.isAlive()) list.add(Dungeon.hero);
			GolemQuest.stepCheck(list);
		}
		if (started && !guard){
			if (lit() > 0 && HP < HT && Random.Int(2) == 0) HP = Math.min(HT, HP + lit());
			if (phase >= 2 && --rekindle <= 0){
				rekindle = 15;
				for (int i = 0; i < GolemQuest.Q.lit.length; i++){
					if (!GolemQuest.Q.lit[i] && Actor.findChar(GolemQuest.Q.runes[i]) == null){
						GolemQuest.Q.lit[i] = true;
						GLog.w(Messages.get(GreatGolem.class, "rekindle"));
						break;
					}
				}
			}
		}
		return super.act();
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (lit() > 0) dmg = Math.max(1, dmg * (6 - lit()) / 6);
		super.damage(dmg, src);
		if (!isAlive()) return;
		if (phase == 1 && HP * 5 <= HT * 3) newPhase(2);
		else if (phase == 2 && HP * 10 <= HT * 3) newPhase(3);
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = Dungeon.level.distance(pos, enemy.pos);
		if (phase >= 2 && !guard && lit() > 0 && Random.Int(2) == 0){
			for (int i = 0; i < GolemQuest.Q.runes.length; i++){
				if (!GolemQuest.Q.lit[i]) continue;
				int r = GolemQuest.Q.runes[i];
				Ballistica b = new Ballistica(r, enemy.pos, Ballistica.STOP_SOLID);
				ArrayList<Integer> cells = new ArrayList<>(b.subPath(0, Math.min(b.dist, 9)));
				if (!cells.contains(enemy.pos)) continue;
				beamRune = r;
				announce(BEAM, cells, enemy.pos);
				return true;
			}
		}
		if (d <= 2){
			ArrayList<Integer> ring = new ArrayList<>();
			int w = Dungeon.level.width();
			for (int dy = -2; dy <= 2; dy++){
				for (int dx = -2; dx <= 2; dx++){
					int c = pos + dx + dy * w;
					if ((dx != 0 || dy != 0) && c >= 0 && c < Dungeon.level.length() && !Dungeon.level.solid[c]) ring.add(c);
				}
			}
			announce(POUND, ring, enemy.pos);
			return true;
		}
		if (phase >= 3 && d >= 3){
			arc(3, 30, enemy.pos, 1.5f, ItemSpriteSheet.SHOT_BIG_ORB, 14, 22);
			if (sprite instanceof SKMobSprite) ((SKMobSprite) sprite).cast();
			Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.7f, 1.2f);
			spend(TICK);
			return true;
		}
		return false;
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		if (kind == BEAM){
			if (beamRune != -1 && sprite != null && sprite.parent != null && !cells.isEmpty()){
				sprite.parent.add(new Beam.LightRay(DungeonTilemap.raisedTileCenterToWorld(beamRune),
						DungeonTilemap.raisedTileCenterToWorld(cells.get(cells.size() - 1))));
			}
			Sample.INSTANCE.play(Assets.Sounds.RAY);
			for (Char ch : SKCombat.foesIn(this, cells)){
				ch.damage(Math.max(1, Random.NormalIntRange(14, 24) - ch.drRoll() / 2), this);
			}
			beamRune = -1;
			return;
		}
		PixelScene.shake(3, 0.5f);
		Sample.INSTANCE.play(Assets.Sounds.ROCKS);
		for (int c : cells) if (Dungeon.level.heroFOV[c] && c % 3 == 0) CellEmitter.get(c).burst(Speck.factory(Speck.ROCK), 2);
		for (Char ch : SKCombat.foesIn(this, cells)){
			ch.damage(Math.max(1, Random.NormalIntRange(14, 24) - ch.drRoll() / 2), this);
			if (ch.isAlive() && !ch.properties().contains(Property.IMMOVABLE)){
				Ballistica traj = new Ballistica(ch.pos, ch.pos + (ch.pos - pos), Ballistica.MAGIC_BOLT);
				WandOfBlastWave.throwChar(ch, traj, 1, false, true, this);
			}
		}
	}

	@Override
	public void die( Object cause ) {
		int at = pos;
		super.die(cause);
		GolemQuest.Q.slain = true;
		if (!guard && !(Dungeon.level instanceof CityBossLevel)){
			GolemQuest.Q.bonus = GolemQuest.Q.litCount() == 0;
			Dungeon.level.drop(new GolemQuest.GolemCore(), at).sprite.drop();
			GLog.p(Messages.get(GreatGolem.class, GolemQuest.Q.bonus ? "slain_bonus" : "slain"));
		}
		CoopLog.line("great golem slain, runes lit " + GolemQuest.Q.litCount());
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("guard", guard);
		bundle.put("rekindle", rekindle);
		bundle.put("beam_rune", beamRune);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		guard = bundle.getBoolean("guard");
		rekindle = bundle.getInt("rekindle");
		beamRune = bundle.getInt("beam_rune");
	}

	public static class GreatGolemSprite extends SKMobSprite {
		public GreatGolemSprite(){
			super();
			setup(QuestAssets.GREAT_GOLEM, 26, 28);
		}
	}
}
