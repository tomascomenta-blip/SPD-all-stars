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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKCombat;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forgotten.ForgottenBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forgotten.ForgottenVisuals;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//yeti king. his roar knocks icicles off the cave ceiling (marked cells around the heroes, fall 2 turns later)
//and they hurt anyone under them, him included: lure him under one while he chases you and he takes
//a big hit + gets dazed. his own avalanches never touch him (he's the king, ok)
// I   claws, throws ice blocks (big bullets), roars icicles down
// II  60%: avalanches (a whole marked band of the cave) + blizzard, can't see him from more than 2 cells away
// III 30%: everything more often, 2 avalanches at once
public class YetiKing extends MiniBoss {

	{
		spriteClass = YetiKingSprite.class;
		HP = HT = 420;
		defenseSkill = 22;
		EXP = 50;
		maxLvl = 30;
		lootChance = 0;
		immunities.add(Chill.class);
		immunities.add(Frost.class);
	}

	private static final int AVALANCHE = 1;
	public int icicleHits = 0;
	private int blizzard = 0;
	private int[] icicles = new int[0];  //cells where the last roar's icicles are about to fall
	private int icicleFall = 0;          //turns till they fall

	@Override public String barAsset() { return QuestAssets.BAR_YETI; }
	@Override protected String music() { return Assets.Music.HALLS_TENSE; }

	@Override public int damageRoll() { return Random.NormalIntRange(28, 40 + 2 * phase); }
	@Override public int attackSkill( Char target ) { return 45; }
	@Override public int drRoll() { return super.drRoll() + Random.NormalIntRange(4, 12); }

	@Override
	protected int specialCooldown() {
		return phase >= 3 ? 2 : 3;
	}

	@Override
	public String status() {
		String p = Messages.get(YetiKing.class, "status_" + Math.min(3, phase));
		if (blizzard > 0) p += "   " + Messages.get(YetiKing.class, "status_blizzard");
		return p;
	}

	@Override
	public int barColor() {
		return blizzard > 0 ? 0xFFC0D8FF : 0xFF80B0E0;
	}

	//in Tharog's glaciation the boss music keeps going
	@Override
	public void playMusic() {
		if (!(Dungeon.level instanceof ForgottenBossLevel)) super.playMusic();
	}

	@Override
	protected void onStart() {
		yell(Messages.get(YetiKing.class, "notice"));
		Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 1f, 0.5f);
		PixelScene.shake(4, 0.8f);
	}

	@Override
	protected boolean act() {
		if (icicleFall > 0 && --icicleFall == 0) dropIcicles();
		if (blizzard > 0){
			blizzard--;
			//in the blizzard he's just a shape past 2 cells. spooky
			boolean close = false;
			for (Hero h : YetiQuest.heroes()) if (Dungeon.level.distance(h.pos, pos) <= 2) close = true;
			invisible = close ? 0 : 1;
			if (blizzard == 0) invisible = 0;
		}
		return super.act();
	}

	@Override
	public void damage( int dmg, Object src ) {
		super.damage(dmg, src);
		if (!isAlive()){
			icicleFall = 0;
			return;
		}
		if (phase == 1 && HP * 5 <= HT * 3){
			newPhase(2);
			blizzard = 10;
		} else if (phase == 2 && HP * 10 <= HT * 3){
			newPhase(3);
			blizzard = 14;
		}
	}

	@Override
	protected boolean special( Char enemy ) {
		int d = Dungeon.level.distance(pos, enemy.pos);
		int r = Random.Int(phase >= 2 ? 3 : 2);
		if (r == 0 && icicleFall == 0){
			//roar: icicles all around the heroes, never right on him (you have to lure him under)
			ArrayList<Integer> cells = new ArrayList<>();
			for (int tries = 0; tries < 60 && cells.size() < 6 + 2 * phase; tries++){
				int c = enemy.pos + Random.IntRange(-3, 3) + Random.IntRange(-3, 3) * Dungeon.level.width();
				if (c < 0 || c >= Dungeon.level.length() || Dungeon.level.solid[c] || c == pos || cells.contains(c)) continue;
				cells.add(c);
			}
			//cells next to the heroes (where he'll step to reach them) get more
			for (int n : PathFinder.NEIGHBOURS8){
				int c = enemy.pos + n;
				if (cells.size() < 8 + 2 * phase && c != pos && !Dungeon.level.solid[c] && !cells.contains(c) && Random.Int(3) == 0) cells.add(c);
			}
			float delay = SKCombat.announceDelay(2f);
			SKCombat.mark(cells, delay);
			icicles = new int[cells.size()];
			for (int i = 0; i < icicles.length; i++) icicles[i] = cells.get(i);
			icicleFall = (int) Math.ceil(delay);
			if (sprite instanceof SKMobSprite) ((SKMobSprite) sprite).cast();
			yell(Messages.get(YetiKing.class, "roar"));
			GLog.w(Messages.get(YetiKing.class, "icicles"));
			spend(TICK);
			return true;
		}
		if (r == 2 && phase >= 2){
			//avalanche: the band of rows (or cols) where the enemy stands
			ArrayList<Integer> cells = new ArrayList<>();
			int w = Dungeon.level.width();
			boolean rows = Random.Int(2) == 0;
			int ex = enemy.pos % w, ey = enemy.pos / w;
			for (int k = -1; k <= 1; k++){
				for (int s = -6; s <= 6; s++){
					int c = rows ? (ex + s) + (ey + k) * w : (ex + k) + (ey + s) * w;
					if (c < 0 || c >= Dungeon.level.length() || Dungeon.level.solid[c]) continue;
					cells.add(c);
				}
			}
			announce(AVALANCHE, cells, enemy.pos);
			GLog.w(Messages.get(YetiKing.class, "avalanche"));
			return true;
		}
		if (d >= 2){
			arc(phase >= 3 ? 5 : 3, 36, enemy.pos, 1.5f, ItemSpriteSheet.SHOT_BIG_ORB, 16, 26);
			if (sprite instanceof SKMobSprite) ((SKMobSprite) sprite).cast();
			Sample.INSTANCE.play(Assets.Sounds.SHATTER, 0.8f, 0.8f);
			spend(TICK);
			return true;
		}
		return false;
	}

	//roar icicles come down. on heroes still under them, and on him if he walked in
	private void dropIcicles(){
		ArrayList<Integer> cells = new ArrayList<>();
		for (int c : icicles) cells.add(c);
		icicles = new int[0];
		Sample.INSTANCE.play(Assets.Sounds.SHATTER);
		for (int c : cells){
			if (Dungeon.level.heroFOV[c]){
				ForgottenVisuals.icicle(c);
				CellEmitter.get(c).burst(Speck.factory(Speck.ROCK), 3);
			}
		}
		for (Char ch : SKCombat.foesIn(this, cells)){
			ch.damage(Math.max(1, Random.NormalIntRange(22, 32) - ch.drRoll() / 2), this);
		}
		if (cells.contains(pos)) hitByIcicle();
	}

	@Override
	protected void release( int kind, ArrayList<Integer> cells, int aim ) {
		Sample.INSTANCE.play(Assets.Sounds.SHATTER);
		PixelScene.shake(4, 0.7f);
		for (int c : cells) if (Dungeon.level.heroFOV[c] && c % 3 == 0) CellEmitter.get(c).burst(Speck.factory(Speck.ROCK), 2);
		for (Char ch : SKCombat.foesIn(this, cells)){
			ch.damage(Math.max(1, Random.NormalIntRange(28, 40) - ch.drRoll() / 2), this);
			//chill, NEVER frost. frost breaks potions and nobody wants that
			if (ch.isAlive()) Buff.prolong(ch, Chill.class, 4f);
		}
	}

	private void hitByIcicle(){
		icicleHits++;
		damage(Math.max(10, HT / 10), this);
		if (!isAlive()) return;
		Buff.prolong(this, Paralysis.class, 2f);
		if (sprite != null) sprite.showStatus(0xC0E0FF, Messages.get(YetiKing.class, "hit"));
		GLog.p(Messages.get(YetiKing.class, "hit_log", icicleHits));
		CoopLog.line("yeti king hit by his own icicles " + icicleHits);
	}

	@Override
	public void die( Object cause ) {
		int at = pos;
		super.die(cause);
		invisible = 0;
		YetiQuest.Q.slain = true;
		if (!(Dungeon.level instanceof ForgottenBossLevel)){
			YetiQuest.Q.bonus = icicleHits >= 2;
			GLog.p(Messages.get(YetiKing.class, icicleHits >= 2 ? "slain_bonus" : "slain"));
		}
		Dungeon.level.drop(new Gold(Random.IntRange(200, 320)), at).sprite.drop();
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("icicle_hits", icicleHits);
		bundle.put("blizzard", blizzard);
		bundle.put("icicles", icicles);
		bundle.put("icicle_fall", icicleFall);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		icicleHits = bundle.getInt("icicle_hits");
		blizzard = bundle.getInt("blizzard");
		if (bundle.contains("icicles")) icicles = bundle.getIntArray("icicles");
		icicleFall = bundle.getInt("icicle_fall");
	}

	public static class YetiKingSprite extends SKMobSprite {
		public YetiKingSprite(){
			super();
			setup(QuestAssets.YETI_KING, 26, 26);
		}
	}
}
