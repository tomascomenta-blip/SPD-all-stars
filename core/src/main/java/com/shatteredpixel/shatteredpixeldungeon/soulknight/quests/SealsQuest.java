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
import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Eye;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.RipperDemon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Succubus;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.coop.CoopLog;
import com.shatteredpixel.shatteredpixeldungeon.coop.Party;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.PlateArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatsword;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKMobSprite;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.SKWorlds;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.forgotten.ForgottenFeatures;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.props.SKProps;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndQuest;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/*
 * demonic seals, demon halls 21-23.
 * the soul of a paladin who went down before the heroes waits by the stairs on 21. three seals feed
 * Yog-Dzewa's fists, one on each of the next floors. hit them till they break, and every broken seal
 * lets out a wave of demons. each one broken within 150 turns on its floor = bonus
 * all three: Yog calls one fist less. not all: one more. no pressure
 */
public class SealsQuest {

	public static class State extends QuestState {
		public int broken = 0;
		public int placed = 0;
		public int quick = 0;           //seals broken in time
		public float arrived = -1;      //when the heroes got to the current seal's floor

		public State(){
			super("quest_seals");
		}

		@Override
		public void reset() {
			super.reset();
			broken = placed = quick = 0;
			arrived = -1;
		}

		@Override
		protected void storeMore( Bundle b ) {
			b.put("broken", broken);
			b.put("placed", placed);
			b.put("quick", quick);
			b.put("arrived", arrived);
		}

		@Override
		protected void restoreMore( Bundle b ) {
			broken = b.getInt("broken");
			placed = b.getInt("placed");
			quick = b.getInt("quick");
			arrived = b.getFloat("arrived");
		}

		@Override
		public boolean punish() {
			return given && broken < 3;
		}
	}

	public static final State Q = SKQuests.SEALS;

	//quest switch (on, it's finished)
	public static boolean READY = true;

	public static void place( RegularLevel level, ArrayList<Room> rooms, Room entrance ){
		if (!READY || SKWorlds.isSK(Dungeon.depth) || DevFlags.noQuests()) return;
		if (Dungeon.depth == 21 && Q.depth == -1){
			int c = HuntQuest.freeCell(level, entrance, -1);
			if (c == -1) return;
			Q.depth = 21;
			HuntQuest.add(level, new LostSoul(), c);
		}
		if (Dungeon.depth >= 21 && Dungeon.depth <= 23 && Q.placed < Dungeon.depth - 20){
			ArrayList<Room> far = new ArrayList<>();
			for (Room r : rooms) if (r instanceof StandardRoom && r != entrance && r.width() >= 5 && r.height() >= 5) far.add(r);
			if (far.isEmpty()) return;
			final int from = level.entrance();
			Collections.sort(far, new Comparator<Room>() {
				@Override
				public int compare( Room a, Room b ){
					return level.distance(from, level.pointToCell(b.center())) - level.distance(from, level.pointToCell(a.center()));
				}
			});
			int c = HuntQuest.freeCell(level, far.get(0), -1);
			if (c == -1) return;
			HuntQuest.add(level, new DemonSeal(), c);
			Q.placed = Dungeon.depth - 20;
			CoopLog.line("seal placed depth=" + Dungeon.depth + " at " + c);
		}
	}

	//true = this phase's fist shouldn't come
	public static boolean skipFist( int phase ){
		if (Q.broken < 3 || phase != 2) return false;
		CoopLog.line("seals: yog calls one fist less");
		return true;
	}

	public static boolean extraFist( int phase ){
		if (!Q.punish() || phase != 2) return false;
		CoopLog.line("seals: yog calls one fist more");
		return true;
	}

	public static void yogFight(){
		if (Q.broken >= 3) GLog.p(Messages.get(SealsQuest.class, "fist_less"));
		else if (Q.given) GLog.n(Messages.get(SealsQuest.class, "fist_more"));
	}

	static ArrayList<Hero> heroes(){
		ArrayList<Hero> l = new ArrayList<>();
		if (Party.active()) l.addAll(Party.living());
		else if (Dungeon.hero != null && Dungeon.hero.isAlive()) l.add(Dungeon.hero);
		return l;
	}

	static void sealBroken( int at, boolean quick ){
		Q.broken++;
		if (quick) Q.quick++;
		Q.slain = Q.broken >= 3;
		Q.bonus = Q.quick >= 3;
		GLog.p(Messages.get(SealsQuest.class, "broken", Q.broken));
		MiniBossHud.banner(Messages.get(SealsQuest.class, "banner", Q.broken));
		PixelScene.shake(4, 0.8f);
		Sample.INSTANCE.play(Assets.Sounds.BLAST, 1f, 0.6f);
		//demon wave, here we go
		int n = 2 + Q.broken;
		boolean wave = false;
		for (int i = 0; i < n; i++){
			int cell = -1;
			for (int tries = 0; tries < 30 && cell == -1; tries++){
				int c = at + Random.IntRange(-4, 4) + Random.IntRange(-4, 4) * Dungeon.level.width();
				if (c > 0 && c < Dungeon.level.length() && Dungeon.level.passable[c] && Actor.findChar(c) == null
						&& Dungeon.level.distance(c, at) >= 2) cell = c;
			}
			if (cell == -1) continue;
			Mob m;
			switch (Random.Int(3)){
				case 0: default: m = new RipperDemon(); break;
				case 1: m = new Succubus(); break;
				case 2: m = new Eye(); break;
			}
			m.pos = cell;
			GameScene.add(m);
			m.state = m.HUNTING;
			Buff.affect(m, Guardian.class);
			wave = true;
			CellEmitter.get(cell).burst(ShadowParticle.UP, 8);
		}
		if (wave && READY){
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					Music.INSTANCE.play(Assets.Music.HALLS_TENSE, true);
				}
			});
		}
		//paladin soul shows up to say thanks after the last one. qué majo
		if (Q.broken >= 3){
			for (Hero h : heroes()){
				int c = -1;
				for (int k : PathFinder.NEIGHBOURS8){
					if (Dungeon.level.passable[h.pos + k] && Actor.findChar(h.pos + k) == null){
						c = h.pos + k;
						break;
					}
				}
				if (c != -1){
					LostSoul s = new LostSoul();
					s.pos = c;
					s.came = true;
					GameScene.add(s);
					s.yell(Messages.get(LostSoul.class, "came"));
				}
				break;
			}
		}
		CoopLog.line("seal broken " + Q.broken + " quick " + Q.quick);
	}

	//demon from a broken seal's wave. fight music keeps going till the last one dies
	public static class Guardian extends Buff {

		@Override
		public void detach() {
			super.detach();
			if (!READY || Dungeon.level == null) return;
			for (Mob m : Dungeon.level.mobs){
				if (m != target && m.isAlive() && m.alignment == Char.Alignment.ENEMY && m.buff(Guardian.class) != null) return;
			}
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					if (Dungeon.level != null) Dungeon.level.playLevelMusic();
				}
			});
		}
	}

	// *** paladin soul ***

	public static class LostSoul extends NPC {

		{
			spriteClass = LostSoulSprite.class;
			properties.add(Property.IMMOVABLE);
			flying = true;
		}

		public boolean came = false;

		@Override
		protected boolean act() {
			spend(TICK);
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
			final Hero h = (Hero) c;
			if (sprite != null) sprite.turnTo(pos, h.pos);
			if (!Q.given){
				Q.given = true;
				Q.arrived = Actor.now();
				CoopLog.line("seals given");
				Game.runOnRenderThread(new Callback() {
					@Override
					public void call() {
						GameScene.show(new WndQuest(LostSoul.this, Messages.get(LostSoul.class, "intro")));
					}
				});
				GLog.p(Messages.get(LostSoul.class, "intro_hint"));
				return true;
			}
			if (Q.broken < 3){
				yell(Messages.get(LostSoul.class, "hint", 3 - Q.broken));
				return true;
			}
			if (!Q.done){
				Q.done = true;
				Sample.INSTANCE.play(Assets.Sounds.MASTERY);
			}
			if (Q.claimedBy(h)){
				yell(Messages.get(LostSoul.class, "after"));
				return true;
			}
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					reward(h);
				}
			});
			return true;
		}

		private void reward( final Hero h ){
			GameScene.show(new WndOptions(
					Messages.titleCase(name()),
					Messages.get(LostSoul.class, Q.bonus ? "reward_bonus" : "reward"),
					Messages.get(PaladinBlade.class, "name"),
					Messages.get(BlessedPlate.class, "name"),
					Messages.get(PaladinRelic.class, "name")){
				@Override
				protected void onSelect( int index ) {
					if (Q.claimedBy(h)) return;
					Item item;
					switch (index){
						case 0: default: item = new PaladinBlade(); break;
						case 1: item = new BlessedPlate(); break;
						case 2: item = new PaladinRelic(); break;
					}
					if (item instanceof Artifact){ if (Q.bonus) item.upgrade(2); }
					else if (item.isUpgradable()) item.upgrade(Q.bonus ? 2 : 1);
					item.identify();
					Q.claim(h);
					if (!item.collect(h.belongings.backpack)) Dungeon.level.drop(item, h.pos).sprite.drop();
					GLog.p(Messages.get(LostSoul.class, "got", item.name()));
					CoopLog.line("seals reward " + item.getClass().getSimpleName() + " for hero " + ChampionHooks.indexOf(h));
				}
				@Override
				public void onBackPressed() {
					//pick one pls
				}
			});
		}

		@Override
		public String description() {
			return Messages.get(this, "desc");
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("came", came);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			came = bundle.getBoolean("came");
		}
	}

	public static class LostSoulSprite extends SKMobSprite {
		public LostSoulSprite(){
			super();
			setup(QuestAssets.LOST_SOUL, 12, 15);
		}
	}

	// *** seal, breaks with hits ***

	public static class DemonSeal extends SKProps.SKProp {

		{
			spriteClass = DemonSealSprite.class;
			alignment = Alignment.ENEMY;
		}

		public DemonSeal(){
			super();
			HP = HT = 60 + 20 * heroes().size();
		}

		public int age = 0; //turns on this floor while the quest is on

		@Override
		protected boolean act() {
			if (Q.given) age++;
			if (sprite != null && Dungeon.level.heroFOV[pos] && (int) now() % 3 == 0) CellEmitter.get(pos).burst(ShadowParticle.UP, 2);
			spend(TICK);
			return true;
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("age", age);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			age = bundle.getInt("age");
		}

		@Override
		public void damage( int dmg, Object src ) {
			if (!Q.given){
				if (sprite != null && src instanceof Hero) sprite.showStatus(0xA060FF, Messages.get(DemonSeal.class, "unbroken"));
				return;
			}
			super.damage(dmg, src);
		}

		@Override
		public void die( Object cause ) {
			int at = pos;
			super.die(cause);
			sealBroken(at, age <= 150);
		}

		@Override
		public String description() {
			return Messages.get(this, Q.given ? "desc" : "desc_unknown");
		}
	}

	public static class DemonSealSprite extends ForgottenFeatures.StripSprite {
		public DemonSealSprite(){
			super();
			strip(QuestAssets.DEMON_SEAL, 16, 24, 4);
		}
	}

	//seal counter, under the boss bar
	public static class SealsHud extends Group {

		private final RenderedTextBlock text;
		private final ColorBlock back;
		private String shown = "";

		//made on every seals floor, shows up once the soul has given the quest
		public static boolean wanted(){
			return READY && Q.broken < 3 && Dungeon.depth >= 21 && Dungeon.depth <= 24 && !SKWorlds.isSK(Dungeon.depth);
		}

		public SealsHud( float top ){
			back = new ColorBlock(10, 10, 0xAA100418);
			add(back);
			text = PixelScene.renderTextBlock(7);
			text.hardlight(0xD0A0FF);
			add(text);
			back.y = top;
		}

		@Override
		public void update() {
			super.update();
			boolean show = Q.given && Q.broken < 3;
			back.visible = text.visible = show;
			if (!show) return;
			String t = Messages.get(SealsQuest.class, "hud", Q.broken);
			if (!t.equals(shown)){
				shown = t;
				text.text(t);
				float uiw = PixelScene.uiCamera.width;
				back.size(text.width() + 8, text.height() + 4);
				back.x = (uiw - back.width()) / 2f;
				text.setPos(back.x + 4, back.y + 2);
				PixelScene.align(text);
			}
		}
	}

	//paladin's sword. burns demons and undead (x1.5 dmg)
	public static class PaladinBlade extends Greatsword {

		{
			image = ItemSpriteSheet.PALADIN_BLADE;
		}

		@Override
		public int proc( Char attacker, Char defender, int damage ) {
			if (defender.properties().contains(Char.Property.DEMONIC) || defender.properties().contains(Char.Property.UNDEAD)){
				damage = damage * 3 / 2;
				if (defender.sprite != null) defender.sprite.emitter().burst(Speck.factory(Speck.LIGHT), 3);
			}
			return super.proc(attacker, defender, damage);
		}
	}

	//blessed plate: demons hit it 20% softer
	public static class BlessedPlate extends PlateArmor {

		{
			image = ItemSpriteSheet.BLESSED_PLATE;
		}

		@Override
		public int proc( Char attacker, Char defender, int damage ) {
			int d = super.proc(attacker, defender, damage);
			if (attacker != null && attacker.properties().contains(Char.Property.DEMONIC)) d = d * 4 / 5;
			return d;
		}
	}

	//paladin relic. raise it: heals the bearer + hurts every demon in sight
	public static class PaladinRelic extends Artifact {

		public static final String AC_RAISE = "RAISE";

		{
			image = ItemSpriteSheet.PALADIN_RELIC;
			levelCap = 10;
			charge = 0;
			chargeCap = 100;
			defaultAction = AC_RAISE;
			unique = true;
		}

		@Override
		public ArrayList<String> actions( Hero hero ) {
			ArrayList<String> actions = super.actions(hero);
			if (isEquipped(hero) && !cursed) actions.add(AC_RAISE);
			return actions;
		}

		@Override
		public void execute( Hero hero, String action ) {
			super.execute(hero, action);
			if (!action.equals(AC_RAISE)) return;
			if (!isEquipped(hero)){
				GLog.i(Messages.get(Artifact.class, "need_to_equip"));
				return;
			}
			if (charge < chargeCap){
				GLog.i(Messages.get(this, "no_charge"));
				return;
			}
			charge = 0;
			hero.HP = Math.min(hero.HT, hero.HP + 20 + 4 * level());
			hero.sprite.emitter().burst(Speck.factory(Speck.HEALING), 4);
			for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
				if (m.alignment == Char.Alignment.ENEMY && Dungeon.level.heroFOV[m.pos]
						&& (m.properties().contains(Char.Property.DEMONIC) || m.properties().contains(Char.Property.UNDEAD))){
					m.damage(Random.NormalIntRange(10 + 2 * level(), 20 + 3 * level()), this);
					if (m.sprite != null) m.sprite.emitter().burst(Speck.factory(Speck.LIGHT), 4);
				}
			}
			Sample.INSTANCE.play(Assets.Sounds.MASTERY, 0.8f, 1.2f);
			GLog.p(Messages.get(this, "raised"));
			exp++;
			if (exp >= level() + 2 && level() < levelCap){
				exp = 0;
				upgrade();
				GLog.p(Messages.get(this, "levelup"));
			}
			updateQuickslot();
			hero.spendAndNext(1f);
		}

		@Override
		protected ArtifactBuff passiveBuff() {
			return new Prayer();
		}

		@Override
		public void charge( Hero target, float amount ) {
			if (charge < chargeCap){
				charge = Math.min(chargeCap, charge + Math.round(3 * amount));
				updateQuickslot();
			}
		}

		@Override
		public String desc() {
			String desc = Messages.get(this, "desc");
			if (isEquipped(Dungeon.hero)) desc += "\n\n" + Messages.get(this, charge >= chargeCap ? "desc_ready" : "desc_charging");
			return desc;
		}

		public class Prayer extends ArtifactBuff {
			@Override
			public boolean act() {
				if (charge < chargeCap && !cursed && target instanceof Hero && ((Hero) target).visibleEnemies() > 0){
					partialCharge += 1.4f + 0.15f * level();
					while (partialCharge >= 1 && charge < chargeCap){
						partialCharge--;
						charge++;
					}
					if (charge >= chargeCap) GLog.p(Messages.get(PaladinRelic.class, "ready"));
					updateQuickslot();
				}
				spend(TICK);
				return true;
			}
		}
	}

	static boolean onFloor( Level level ){
		for (Mob m : level.mobs) if (m instanceof DemonSeal) return true;
		return false;
	}
}
