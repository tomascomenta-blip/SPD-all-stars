/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtifactRecharge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CounterBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.EnhancedRings;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LostInventory;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PhysicalEmpower;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Recharging;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RevealedArea;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ScrollEmpower;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.WandEmpower;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.Ratmogrify;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.DivineSense;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.RecallInscription;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.champions.ChampionHooks;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Flare;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.SpellSprite;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.Runestone;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfIntuition;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ShardOfOblivion;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Gloves;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.soulknight.finale.reward.Specialization;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.GameMath;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

import java.util.LinkedHashSet;

public enum Talent {

	//Warrior T1
	HEARTY_MEAL(0), VETERANS_INTUITION(1), PROVOKED_ANGER(2), IRON_WILL(3),
	//Warrior T2
	IRON_STOMACH(4), LIQUID_WILLPOWER(5), RUNIC_TRANSFERENCE(6), LETHAL_MOMENTUM(7), IMPROVISED_PROJECTILES(8),
	//Warrior T3
	HOLD_FAST(9, 3), STRONGMAN(10, 3),
	//Berserker T3
	ENDLESS_RAGE(11, 3), DEATHLESS_FURY(12, 3), ENRAGED_CATALYST(13, 3),
	//Gladiator T3
	CLEAVE(14, 3), LETHAL_DEFENSE(15, 3), ENHANCED_COMBO(16, 3),
	//Heroic Leap T4
	BODY_SLAM(17, 4), IMPACT_WAVE(18, 4), DOUBLE_JUMP(19, 4),
	//Shockwave T4
	EXPANDING_WAVE(20, 4), STRIKING_WAVE(21, 4), SHOCK_FORCE(22, 4),
	//Endure T4
	SUSTAINED_RETRIBUTION(23, 4), SHRUG_IT_OFF(24, 4), EVEN_THE_ODDS(25, 4),

	//Mage T1
	EMPOWERING_MEAL(32), SCHOLARS_INTUITION(33), LINGERING_MAGIC(34), BACKUP_BARRIER(35),
	//Mage T2
	ENERGIZING_MEAL(36), INSCRIBED_POWER(37), WAND_PRESERVATION(38), ARCANE_VISION(39), SHIELD_BATTERY(40),
	//Mage T3
	DESPERATE_POWER(41, 3), ALLY_WARP(42, 3),
	//Battlemage T3
	EMPOWERED_STRIKE(43, 3), MYSTICAL_CHARGE(44, 3), EXCESS_CHARGE(45, 3),
	//Warlock T3
	SOUL_EATER(46, 3), SOUL_SIPHON(47, 3), NECROMANCERS_MINIONS(48, 3),
	//Elemental Blast T4
	BLAST_RADIUS(49, 4), ELEMENTAL_POWER(50, 4), REACTIVE_BARRIER(51, 4),
	//Wild Magic T4
	WILD_POWER(52, 4), FIRE_EVERYTHING(53, 4), CONSERVED_MAGIC(54, 4),
	//Warp Beacon T4
	TELEFRAG(55, 4), REMOTE_BEACON(56, 4), LONGRANGE_WARP(57, 4),

	//Rogue T1
	CACHED_RATIONS(64), THIEFS_INTUITION(65), SUCKER_PUNCH(66), PROTECTIVE_SHADOWS(67),
	//Rogue T2
	MYSTICAL_MEAL(68), INSCRIBED_STEALTH(69), WIDE_SEARCH(70), SILENT_STEPS(71), ROGUES_FORESIGHT(72),
	//Rogue T3
	ENHANCED_RINGS(73, 3), LIGHT_CLOAK(74, 3),
	//Assassin T3
	ENHANCED_LETHALITY(75, 3), ASSASSINS_REACH(76, 3), BOUNTY_HUNTER(77, 3),
	//Freerunner T3
	EVASIVE_ARMOR(78, 3), PROJECTILE_MOMENTUM(79, 3), SPEEDY_STEALTH(80, 3),
	//Smoke Bomb T4
	HASTY_RETREAT(81, 4), BODY_REPLACEMENT(82, 4), SHADOW_STEP(83, 4),
	//Death Mark T4
	FEAR_THE_REAPER(84, 4), DEATHLY_DURABILITY(85, 4), DOUBLE_MARK(86, 4),
	//Shadow Clone T4
	SHADOW_BLADE(87, 4), CLONED_ARMOR(88, 4), PERFECT_COPY(89, 4),

	//Huntress T1
	NATURES_BOUNTY(96), SURVIVALISTS_INTUITION(97), FOLLOWUP_STRIKE(98), NATURES_AID(99),
	//Huntress T2
	INVIGORATING_MEAL(100), LIQUID_NATURE(101), REJUVENATING_STEPS(102), HEIGHTENED_SENSES(103), DURABLE_PROJECTILES(104),
	//Huntress T3
	POINT_BLANK(105, 3), SEER_SHOT(106, 3),
	//Sniper T3
	FARSIGHT(107, 3), SHARED_ENCHANTMENT(108, 3), SHARED_UPGRADES(109, 3),
	//Warden T3
	DURABLE_TIPS(110, 3), BARKSKIN(111, 3), SHIELDING_DEW(112, 3),
	//Spectral Blades T4
	FAN_OF_BLADES(113, 4), PROJECTING_BLADES(114, 4), SPIRIT_BLADES(115, 4),
	//Natures Power T4
	GROWING_POWER(116, 4), NATURES_WRATH(117, 4), WILD_MOMENTUM(118, 4),
	//Spirit Hawk T4
	EAGLE_EYE(119, 4), GO_FOR_THE_EYES(120, 4), SWIFT_SPIRIT(121, 4),

	//Duelist T1
	STRENGTHENING_MEAL(128), ADVENTURERS_INTUITION(129), PATIENT_STRIKE(130), AGGRESSIVE_BARRIER(131),
	//Duelist T2
	FOCUSED_MEAL(132), LIQUID_AGILITY(133), WEAPON_RECHARGING(134), LETHAL_HASTE(135), SWIFT_EQUIP(136),
	//Duelist T3
	PRECISE_ASSAULT(137, 3), DEADLY_FOLLOWUP(138, 3),
	//Champion T3
	VARIED_CHARGE(139, 3), TWIN_UPGRADES(140, 3), COMBINED_LETHALITY(141, 3),
	//Monk T3
	UNENCUMBERED_SPIRIT(142, 3), MONASTIC_VIGOR(143, 3), COMBINED_ENERGY(144, 3),
	//Challenge T4
	CLOSE_THE_GAP(145, 4), INVIGORATING_VICTORY(146, 4), ELIMINATION_MATCH(147, 4),
	//Elemental Strike T4
	ELEMENTAL_REACH(148, 4), STRIKING_FORCE(149, 4), DIRECTED_POWER(150, 4),
	//Feint T4
	FEIGNED_RETREAT(151, 4), EXPOSE_WEAKNESS(152, 4), COUNTER_ABILITY(153, 4),

	//Cleric T1
	SATIATED_SPELLS(160), HOLY_INTUITION(161), SEARING_LIGHT(162), SHIELD_OF_LIGHT(163),
	//Cleric T2
	ENLIGHTENING_MEAL(164), RECALL_INSCRIPTION(165), SUNRAY(166), DIVINE_SENSE(167), BLESS(168),
	//Cleric T3
	CLEANSE(169, 3), LIGHT_READING(170, 3),
	//Priest T3
	HOLY_LANCE(171, 3), HALLOWED_GROUND(172, 3), MNEMONIC_PRAYER(173, 3),
	//Paladin T3
	LAY_ON_HANDS(174, 3), AURA_OF_PROTECTION(175, 3), WALL_OF_LIGHT(176, 3),
	//Ascended Form T4
	DIVINE_INTERVENTION(177, 4), JUDGEMENT(178, 4), FLASH(179, 4),
	//Trinity T4
	BODY_FORM(180, 4), MIND_FORM(181, 4), SPIRIT_FORM(182, 4),
	//Power of Many T4
	BEAMING_RAY(183, 4), LIFE_LINK(184, 4), STASIS(185, 4),

	//Executioner T1
	EXE_MEAL(256), EXE_INTUITION(257), EXE_DEEP_WOUNDS(258), EXE_THICK_SKIN(259),
	//Executioner T2
	EXE_IRON_RESOLVE(260), EXE_LONG_REACH(261), EXE_CRUEL_BLOW(262), EXE_BLOODLUST(263), EXE_INTIMIDATE(264),
	//Executioner T3
	EXE_SHARP_GUILLOTINE(265, 3), EXE_RELENTLESS(266, 3),
	//Conqueror T3
	CQ_WARLORD(267, 3), CQ_SPIN(268, 3), CQ_BLOODFEAST(269, 3),
	//Headsman T3
	HS_SENTENCE(270, 3), HS_REPEAT(271, 3), HS_BLOODY_AXE(272, 3),

	//Scout T1
	SCO_MEAL(288), SCO_INTUITION(289), SCO_POTENT_VENOM(290), SCO_BLINDING_EYE(291),
	//Scout T2
	SCO_FLEET_FOOT(292), SCO_QUICK_CAMO(293), SCO_MORE_SHROOMS(294), SCO_LIQUID_CAMO(295), SCO_KEEN_SENSES(296),
	//Scout T3
	SCO_BIG_SHROOM(297, 3), SCO_AMBUSH(298, 3),
	//Trapper T3
	TR_BOUNCE(299, 3), TR_PARALYTIC(300, 3), TR_LONG_THROW(301, 3),
	//Skirmisher T3
	SK_CORROSIVE(302, 3), SK_SHADOW_STEP(303, 3), SK_RICOCHET(304, 3),

	//Pyromancer T1
	PYR_MEAL(320), PYR_INTUITION(321), PYR_SPARK(322), PYR_WARMTH(323),
	//Pyromancer T2
	PYR_FIRE_DRINKER(324), PYR_WIDE_FLAMES(325), PYR_BEAR_HUG(326), PYR_EARLY_SPARK(327), PYR_EMBER_STEP(328),
	//Pyromancer T3
	PYR_COMBUSTION(329, 3), PYR_SCORCHED(330, 3),
	//Tamer T3
	TM_BIG_BEAR(331, 3), TM_BOND(332, 3), TM_RESUMMON(333, 3),
	//Infernalist T3
	IN_PYROCLASM(334, 3), IN_FLASHFIRE(335, 3), IN_HEARTFIRE(336, 3),

	//Jailer T1
	JAI_MEAL(352), JAI_INTUITION(353), JAI_LONG_CHAIN(354), JAI_HUNGRY_SOULS(355),
	//Jailer T2
	JAI_SOUL_MEND(356), JAI_HEAVY_LANTERN(357), JAI_FLAY_FORCE(358), JAI_LIQUID_SOULS(359), JAI_WARDENS_GAZE(360),
	//Jailer T3
	JAI_DEATHS_GRIP(361, 3), JAI_PRISON(362, 3),
	//Soulreaper T3
	SR_SOUL_ARMOR(363, 3), SR_REAP(364, 3), SR_SOUL_FLAME(365, 3),
	//Lanternkeeper T3
	LK_LIGHT_WARD(366, 3), LK_ESCAPE(367, 3), LK_BOX_LANTERN(368, 3),

	//Brawler T1
	BRA_MEAL(384), BRA_INTUITION(385), BRA_HEAVY_HANDS(386), BRA_SECOND_WIND(387),
	//Brawler T2
	BRA_LIQUID_GRIT(388), BRA_ONE_TWO(389), BRA_STUBBORN(390), BRA_FOOTWORK(391), BRA_TAUNT(392),
	//Brawler T3
	BRA_BIG_SHOW(393, 3), BRA_GRIT_SHIELD(394, 3),
	//Pitboss T3
	PB_THICK_HIDE(395, 3), PB_IRON_JAW(396, 3), PB_HAYMAKER(397, 3),
	//Showman T3
	SM_CROWD(398, 3), SM_ENCORE(399, 3), SM_FLASHY(400, 3),

	//Gravedigger T1
	GRA_MEAL(416), GRA_INTUITION(417), GRA_HEAVY_SHOVEL(418), GRA_DEEP_GRAVES(419),
	//Gravedigger T2
	GRA_BOTTLED_MIST(420), GRA_LONG_PROCESSION(421), GRA_CLINGING_MIST(422), GRA_HARD_BONES(423), GRA_GRIM_HARVEST(424),
	//Gravedigger T3
	GRA_MAIDENS_WRATH(425, 3), GRA_FLOCK(426, 3),
	//Mistlord T3
	ML_MIST_BURST(427, 3), ML_ENDLESS_NIGHT(428, 3), ML_DARK_COMMUNION(429, 3),
	//Exhumer T3
	EX_GRAVE_STRENGTH(430, 3), EX_DIG_DEEP(431, 3), EX_SOUL_SHOVEL(432, 3),

	//Guardian T1
	GUA_MEAL(448), GUA_INTUITION(449), GUA_SAND_SKIN(450), GUA_HUNGRY_STAFF(451),
	//Guardian T2
	GUA_LIQUID_SAND(452), GUA_LONG_WITHER(453), GUA_HOT_SPIRIT(454), GUA_LIFE_DRAIN(455), GUA_COLLECTOR(456),
	//Guardian T3
	GUA_ETERNAL(457, 3), GUA_DUSTSTORM(458, 3),
	//Curator T3
	CU_ARCHIVE(459, 3), CU_QUICK_SIPHON(460, 3), CU_SPIRIT_TAX(461, 3),
	//Colossus T3
	CO_GIANT(462, 3), CO_UNBROKEN(463, 3), CO_WITHERING_STEPS(464, 3),

	//Bard T1
	BAR_MEAL(480), BAR_INTUITION(481), BAR_LOUD_VOICE(482), BAR_APPLAUSE(483),
	//Bard T2
	BAR_LIQUID_MUSIC(484), BAR_DEEP_SLEEP(485), BAR_SOOTHING(486), BAR_RALLY(487), BAR_TEMPO(488),
	//Bard T3
	BAR_VIRTUOSO(489, 3), BAR_DUET(490, 3),
	//Troubadour T3
	TB_ANTHEM(491, 3), TB_LIFE_SONG(492, 3), TB_HARMONY(493, 3),
	//Minstrel T3
	MI_DISSONANCE(494, 3), MI_RUDE_AWAKENING(495, 3), MI_CRESCENDO(496, 3),

	//botanico: armor abilities of the Scout, Pyromancer and Gravedigger (row 16 of talent_icons.png)
	SCO_DEEP_VENOM(512, 4), SCO_BLIND_STORM(513, 4), SCO_ENDLESS_QUIVER(514, 4),
	PYR_BLAZING_CORE(515, 4), PYR_AFTERGLOW(516, 4), PYR_BLAST_WAVE(517, 4),
	GRA_HEAVY_STONE(518, 4), GRA_LONG_MOURNING(519, 4), GRA_FRESH_GRAVES(520, 4),
	//botanico: Botanist. T4 = Bloom
	BOT_DEEP_BLOOM(521, 4), BOT_LUSH_GARDEN(522, 4), BOT_HEALING_POLLEN(523, 4),
	//Botanist T1
	BOT_MEAL(524), BOT_INTUITION(525), BOT_THORNY_SPROUT(526), BOT_FRESH_AIR(527),
	//Botanist T2
	BOT_LIQUID_SAP(528), BOT_STRONG_ROOTS(529), BOT_DEEP_ROOTS(530), BOT_PRUNING(531), BOT_SEED_BAG(532),
	//Botanist T3
	BOT_OVERGROWTH(533, 3), BOT_ANCIENT_BARK(534, 3),
	//Gardener of war T3
	WG_HARD_WOOD(535, 3), WG_TWIN_SPROUTS(536, 3), WG_THORN_BARRAGE(537, 3),
	//Druid T3
	DR_LIFE_BLOOM(538, 3), DR_PURIFYING_SAP(539, 3), DR_SPRING_RAIN(540, 3),

	//Forger T1
	FOR_MEAL(433), FOR_INTUITION(434), FOR_THICK_PLATES(435), FOR_HEAVY_HAMMER(436),
	//Forger T2
	FOR_LIQUID_FORGE(437), FOR_LONG_RIFT(438), FOR_WIDE_BELLOWS(439), FOR_LONG_CHARGE(440), FOR_TEMPER(441),
	//Forger T3
	FOR_RAM_HORNS(442, 3), FOR_FIRE_PILLARS(443, 3),
	//Master Smith T3
	MS_FINE_TEMPER(444, 3), MS_QUICK_ANVIL(445, 3), MS_MOLTEN_GIFT(446, 3),
	//Living Mountain T3
	LM_STONE_SKIN(447, 3), LM_QUAKE(465, 3), LM_GRANITE_HEART(466, 3),
	//Eruption of the Forge T4 (armor ability of the Forger)
	FER_WIDE_CRATER(467, 4), FER_DENSE_MAGMA(468, 4), FER_SEISMIC(469, 4),
	//Sand Tomb T4 (armor ability of the Guardian)
	GUA_DEEP_SANDS(470, 4), GUA_THIRSTY_TOMB(471, 4), GUA_AGES(472, 4),
	//Grand Overture T4 (armor ability of the Bard)
	BAR_ENCORE(473, 4), BAR_ECHO(474, 4), BAR_OVATION(475, 4),

	//Ironlord T1
	IRL_MEAL(544), IRL_INTUITION(545), IRL_HEAVY_MACE(546), IRL_DARK_HIDE(547),
	//Ironlord T2
	IRL_LIQUID_IRON(548), IRL_LONG_REACH(549), IRL_BRUTE_FORCE(550), IRL_GRAVE_WARD(551), IRL_RESONANCE(552),
	//Ironlord T3
	IRL_DEEPENING_DARK(553, 3), IRL_UNDYING_WILL(554, 3),
	//Tyrant T3
	TY_CRUSHING_AURA(555, 3), TY_OVERLORD(556, 3), TY_IRON_FIST(557, 3),
	//Revenant T3
	RV_BULWARK(558, 3), RV_SOUL_FEAST(559, 3), RV_ENDLESS_NIGHT(560, 3),
	//Iron Eclipse T4
	IE_LONG_NIGHT(561, 4), IE_SMOTHERING_DARK(562, 4), IE_IRON_HUNGER(563, 4),
	//Blood Harvest T4 (Executioner)
	HV_DEEP_CUT(564, 4), HV_CRIMSON_FEAST(565, 4), HV_RED_MARK(566, 4),
	//Spectral Shackles T4 (Jailer)
	SH_LONG_CHAINS(567, 4), SH_HEAVY_LINKS(568, 4), SH_CAPTIVE_SOULS(569, 4),
	//Ovation T4 (Brawler)
	OV_ROAR(570, 4), OV_IRON_GRIT(571, 4), OV_BLOOD_SAND(572, 4),

	//alt worlds: Automaton T1
	AUT_COOLANT(576), AUT_DIAGNOSTICS(577), AUT_PLATING(578), AUT_CAPACITOR(579),
	//Automaton T2
	AUT_SELF_REPAIR(580), AUT_SCANNER(581), AUT_OVERCLOCK(582), AUT_GROUNDING(583), AUT_SALVAGE(584),
	//Automaton T3
	AUT_HIGH_VOLTAGE(585, 3), AUT_FAILSAFE(586, 3),
	//Swarm Engineer T3
	SE_HIVE(587, 3), SE_ARMORED_DRONES(588, 3), SE_FOCUS_FIRE(589, 3),
	//Tesla Coil T3
	TC_ARC(590, 3), TC_SHOCK(591, 3), TC_FEEDBACK(592, 3),
	//Sentry Turret T4
	ST_LONG_SERVICE(593, 4), ST_HEAVY_CALIBER(594, 4), ST_TWIN_BARRELS(595, 4),

	//alt worlds: Beastmaster T1
	BM_SHARED_MEAL(596), BM_TRACKER(597), BM_THICK_FUR(598), BM_SHARP_FANGS(599),
	//Beastmaster T2
	BM_BONE_THROW(600), BM_HOWL(601), BM_QUICK_RECOVERY(602), BM_THORNS(603), BM_NATURES_BOND(604),
	//Beastmaster T3
	BM_ELEMENTAL_FANGS(605, 3), BM_BIG_BEAR(606, 3),
	//Pack Alpha T3
	PA_THIRD_WOLF(607, 3), PA_PACK_TACTICS(608, 3), PA_ALPHA_ROAR(609, 3),
	//Forest Warden T3
	FW_IRONBARK(610, 3), FW_ROOTS(611, 3), FW_BEAR_HUG(612, 3),
	//Pack Roar T4
	PR_THUNDEROUS(613, 4), PR_RALLY(614, 4), PR_FEROCITY(615, 4),

	//alt worlds: Spiritist T1
	SP_SOUL_FEAST(616), SP_DEATH_SENSE(617), SP_GRAVE_CHILL(618), SP_SOUL_HARVEST(619),
	//Spiritist T2
	SP_LINGERING_DREAD(620), SP_GHOSTLY_GRASP(621), SP_OMEN_WEIGHT(622), SP_RESTLESS_DEAD(623), SP_DARK_PACT(624),
	//Spiritist T3
	SP_BONE_LEGION(625, 3), SP_DREAD_HOWL(626, 3),
	//Spectre Lord T3
	SL_WAILING(627, 3), SL_ETHEREAL(628, 3), SL_SPECTRAL_HIDE(629, 3),
	//Plague Herald T3
	PH_VIRULENCE(630, 3), PH_SPREAD(631, 3), PH_BONE_HARVEST(632, 3),
	//Soul Veil T4
	SV_LONG_VEIL(633, 4), SV_PHANTOMS(634, 4), SV_SOUL_MEND(635, 4),

	//alt worlds: Gunslinger T1
	GS_GUN_OIL(636), GS_APPRAISAL(637), GS_STEADY_AIM(638), GS_THICK_COAT(639),
	//Gunslinger T2
	GS_FAST_RELOAD(640), GS_DEADEYE(641), GS_BIG_BOUNTY(642), GS_CARPET_BOMB(643), GS_HOT_STREAK(644),
	//Gunslinger T3
	GS_EXECUTION(645, 3), GS_RICOCHET(646, 3),
	//Quickdraw T3
	QD_HAIR_TRIGGER(647, 3), QD_SPEEDLOADER(648, 3), QD_DUELIST(649, 3),
	//Bounty Hunter T3
	BH_MARKED(650, 3), BH_LONG_HUNT(651, 3), BH_COLLECTOR(652, 3),

	//T5, the paths of the heart of Thal-Vekhorr (any class)
	//yog hunter
	YH_BANE(688, 3), YH_SCENT(689, 3), YH_HIDE(690, 3), YH_TROPHY(691, 3),
	//lightbearer
	LB_BEACON(692, 3), LB_VESSEL(693, 3), LB_DAWN(694, 3), LB_RADIANCE(695, 3),
	//time wanderer
	TW_STOMACH(696, 3), TW_LONG_PAUSE(697, 3), TW_CLOCKWORK(698, 3), TW_QUICKSTEP(699, 3),
	//Lead Rain T4
	LR_WIDE(653, 4), LR_HEAVY(654, 4), LR_STUNNING(655, 4),

	//universal T4
	HEROIC_ENERGY(26, 4), //See icon() and title() for special logic for this one
	//Ratmogrify T4
	RATSISTANCE(215, 4), RATLOMACY(216, 4), RATFORCEMENTS(217, 4);

	public static class ImprovisedProjectileCooldown extends FlavourBuff{
		public int icon() { return BuffIndicator.TIME; }
		public void tintIcon(Image icon) { icon.hardlight(0.15f, 0.2f, 0.5f); }
		public float iconFadePercent() { return Math.max(0, visualcooldown() / 50); }
	};
	public static class LethalMomentumTracker extends FlavourBuff{};
	public static class StrikingWaveTracker extends FlavourBuff{};
	public static class WandPreservationCounter extends CounterBuff{{revivePersists = true;}};
	public static class EmpoweredStrikeTracker extends FlavourBuff{
		//blast wave on-hit doesn't resolve instantly, so we delay detaching for it
		public boolean delayedDetach = false;
	};
	public static class ProtectiveShadowsTracker extends Buff {
		float barrierInc = 0.5f;

		@Override
		public boolean act() {
			//barrier every 2/1 turns, to a max of 3/5
			if (((Hero)target).hasTalent(Talent.PROTECTIVE_SHADOWS) && target.invisible > 0){
				Barrier barrier = Buff.affect(target, Barrier.class);
				if (barrier.shielding() < 1 + 2*((Hero)target).pointsInTalent(Talent.PROTECTIVE_SHADOWS)) {
					barrierInc += 0.5f * ((Hero) target).pointsInTalent(Talent.PROTECTIVE_SHADOWS);
				}
				if (barrierInc >= 1){
					barrierInc = 0;
					barrier.incShield(1);
				} else {
					barrier.incShield(0); //resets barrier decay
				}
			} else {
				detach();
			}
			spend( TICK );
			return true;
		}

		private static final String BARRIER_INC = "barrier_inc";
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put( BARRIER_INC, barrierInc);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			barrierInc = bundle.getFloat( BARRIER_INC );
		}
	}
	public static class BountyHunterTracker extends FlavourBuff{};
	public static class RejuvenatingStepsCooldown extends FlavourBuff{
		public int icon() { return BuffIndicator.TIME; }
		public void tintIcon(Image icon) { icon.hardlight(0f, 0.35f, 0.15f); }
		public float iconFadePercent() { return GameMath.gate(0, visualcooldown() / (15 - 5*Dungeon.hero.pointsInTalent(REJUVENATING_STEPS)), 1); }
	};
	public static class RejuvenatingStepsFurrow extends CounterBuff{{revivePersists = true;}};
	public static class SeerShotCooldown extends FlavourBuff{
		public int icon() { return target.buff(RevealedArea.class) != null ? BuffIndicator.NONE : BuffIndicator.TIME; }
		public void tintIcon(Image icon) { icon.hardlight(0.7f, 0.4f, 0.7f); }
		public float iconFadePercent() { return Math.max(0, visualcooldown() / 20); }
	};
	public static class SpiritBladesTracker extends FlavourBuff{};
	public static class PatientStrikeTracker extends Buff {
		public int pos;
		{ type = Buff.buffType.POSITIVE; }
		public int icon() { return BuffIndicator.TIME; }
		public void tintIcon(Image icon) { icon.hardlight(0.5f, 0f, 1f); }
		@Override
		public boolean act() {
			if (pos != target.pos) {
				detach();
			} else {
				spend(TICK);
			}
			return true;
		}
		private static final String POS = "pos";
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(POS, pos);
		}
		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			pos = bundle.getInt(POS);
		}
	};
	public static class AggressiveBarrierCooldown extends FlavourBuff{
		public int icon() { return BuffIndicator.TIME; }
		public void tintIcon(Image icon) { icon.hardlight(0.35f, 0f, 0.7f); }
		public float iconFadePercent() { return Math.max(0, visualcooldown() / 50); }
	};
	public static class LiquidAgilEVATracker extends FlavourBuff{
		{
			//detaches after hero acts, not after mobs act
			actPriority = HERO_PRIO+1;
		}
	};
	public static class LiquidAgilACCTracker extends FlavourBuff{
		public int uses;

		{ type = buffType.POSITIVE; }
		public int icon() { return BuffIndicator.INVERT_MARK; }
		public void tintIcon(Image icon) { icon.hardlight(0.5f, 0f, 1f); }
		public float iconFadePercent() { return Math.max(0, 1f - (visualcooldown() / 5)); }

		private static final String USES = "uses";
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(USES, uses);
		}
		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			uses = bundle.getInt(USES);
		}
	};
	public static class LethalHasteCooldown extends FlavourBuff{
		public int icon() { return BuffIndicator.TIME; }
		public void tintIcon(Image icon) { icon.hardlight(0.35f, 0f, 0.7f); }
		public float iconFadePercent() { return Math.max(0, visualcooldown() / 100); }
	};
	public static class SwiftEquipCooldown extends FlavourBuff{
		public boolean secondUse;
		public boolean hasSecondUse(){
			return secondUse;
		}

		public int icon() { return BuffIndicator.TIME; }
		public void tintIcon(Image icon) {
			if (hasSecondUse()) icon.hardlight(0.85f, 0f, 1.0f);
			else                icon.hardlight(0.35f, 0f, 0.7f);
		}
		public float iconFadePercent() { return GameMath.gate(0, visualcooldown() / 20f, 1); }

		private static final String SECOND_USE = "second_use";
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(SECOND_USE, secondUse);
		}
		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			secondUse = bundle.getBoolean(SECOND_USE);
		}
	};
	public static class DeadlyFollowupTracker extends FlavourBuff{
		public int object;
		{ type = Buff.buffType.POSITIVE; }
		public int icon() { return BuffIndicator.INVERT_MARK; }
		public void tintIcon(Image icon) { icon.hardlight(0.5f, 0f, 1f); }
		public float iconFadePercent() { return Math.max(0, 1f - (visualcooldown() / 5)); }
		private static final String OBJECT    = "object";
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(OBJECT, object);
		}
		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			object = bundle.getInt(OBJECT);
		}
	}
	public static class PreciseAssaultTracker extends FlavourBuff{
		{ type = buffType.POSITIVE; }
		public int icon() { return BuffIndicator.INVERT_MARK; }
		public void tintIcon(Image icon) { icon.hardlight(1f, 1f, 0.0f); }
		public float iconFadePercent() { return Math.max(0, 1f - (visualcooldown() / 5)); }
	};
	public static class VariedChargeTracker extends Buff{
		public Class weapon;

		private static final String WEAPON    = "weapon";
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(WEAPON, weapon);
		}
		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			weapon = bundle.getClass(WEAPON);
		}
	}
	public static class CombinedLethalityAbilityTracker extends FlavourBuff{
		public MeleeWeapon weapon;
	};
	public static class CombinedEnergyAbilityTracker extends FlavourBuff{
		public boolean monkAbilused = false;
		public boolean wepAbilUsed = false;

		private static final String MONK_ABIL_USED  = "monk_abil_used";
		private static final String WEP_ABIL_USED   = "wep_abil_used";
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(MONK_ABIL_USED, monkAbilused);
			bundle.put(WEP_ABIL_USED, wepAbilUsed);
		}
		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			monkAbilused = bundle.getBoolean(MONK_ABIL_USED);
			wepAbilUsed = bundle.getBoolean(WEP_ABIL_USED);
		}
	}
	public static class CounterAbilityTacker extends FlavourBuff{}
	public static class SatiatedSpellsTracker extends Buff{
		@Override
		public int icon() {
			return BuffIndicator.SPELL_FOOD;
		}
	}
	//used for metamorphed searing light
	public static class SearingLightCooldown extends FlavourBuff{
		@Override
		public int icon() {
			return BuffIndicator.TIME;
		}
		public void tintIcon(Image icon) { icon.hardlight(0f, 0f, 1f); }
		public float iconFadePercent() { return Math.max(0, visualcooldown() / 20); }
	}

	int icon;
	int maxPoints;

	// tiers 1/2/3/4 start at levels 2/7/13/21, tier 5 (past the usual level cap) at 31
	public static int[] tierLevelThresholds = new int[]{0, 2, 7, 13, 21, 31, 41};

	Talent( int icon ){
		this(icon, 2);
	}

	Talent( int icon, int maxPoints ){
		this.icon = icon;
		this.maxPoints = maxPoints;
	}

	public int icon(){
		if (this == HEROIC_ENERGY){
			if (Ratmogrify.useRatroicEnergy){
				return 218;
			}
			HeroClass cls = Dungeon.hero != null ? Dungeon.hero.heroClass : GamesInProgress.selectedClass;
			switch (cls){
				case WARRIOR: default:
					return 26;
				case MAGE:
					return 58;
				case ROGUE:
					return 90;
				case HUNTRESS:
					return 122;
				case DUELIST:
					return 154;
				case CLERIC:
					return 186;
			}
		} else {
			return icon;
		}
	}

	public int maxPoints(){
		return maxPoints;
	}

	public String title(){
		if (this == HEROIC_ENERGY && Ratmogrify.useRatroicEnergy){
			return Messages.get(this, name() + ".rat_title");
		}
		return Messages.get(this, name() + ".title");
	}

	public final String desc(){
		return desc(false);
	}

	public String desc(boolean metamorphed){
		if (metamorphed){
			String metaDesc = Messages.get(this, name() + ".meta_desc");
			if (!metaDesc.equals(Messages.NO_TEXT_FOUND)){
				return Messages.get(this, name() + ".desc") + "\n\n" + metaDesc;
			}
		}
		return Messages.get(this, name() + ".desc");
	}

	public static void onTalentUpgraded( Hero hero, Talent talent ){
		//for metamorphosis
		if (talent == IRON_WILL && hero.heroClass != HeroClass.WARRIOR){
			Buff.affect(hero, BrokenSeal.WarriorShield.class);
		}

		if (talent == VETERANS_INTUITION && hero.pointsInTalent(VETERANS_INTUITION) == 2){
			if (hero.belongings.armor() != null && !ShardOfOblivion.passiveIDDisabled())  {
				hero.belongings.armor.identify();
			}
		}
		if (talent == THIEFS_INTUITION && hero.pointsInTalent(THIEFS_INTUITION) == 2){
			if (hero.belongings.ring instanceof Ring && !ShardOfOblivion.passiveIDDisabled()) {
				hero.belongings.ring.identify();
			}
			if (hero.belongings.misc instanceof Ring && !ShardOfOblivion.passiveIDDisabled()) {
				hero.belongings.misc.identify();
			}
			for (Item item : Dungeon.hero.belongings){
				if (item instanceof Ring){
					((Ring) item).setKnown();
				}
			}
		}
		if (talent == THIEFS_INTUITION && hero.pointsInTalent(THIEFS_INTUITION) == 1){
			if (hero.belongings.ring instanceof Ring) hero.belongings.ring.setKnown();
			if (hero.belongings.misc instanceof Ring) ((Ring) hero.belongings.misc).setKnown();
		}
		if (talent == ADVENTURERS_INTUITION && hero.pointsInTalent(ADVENTURERS_INTUITION) == 2){
			if (hero.belongings.weapon() != null && !ShardOfOblivion.passiveIDDisabled()){
				hero.belongings.weapon().identify();
			}
			if (hero.belongings.secondWep() != null && !ShardOfOblivion.passiveIDDisabled()){
				hero.belongings.secondWep().identify();
			}
		}

		if (talent == PROTECTIVE_SHADOWS && hero.invisible > 0){
			Buff.affect(hero, Talent.ProtectiveShadowsTracker.class);
		}

		if (talent == LIGHT_CLOAK && hero.heroClass == HeroClass.ROGUE){
			for (Item item : Dungeon.hero.belongings.backpack){
				if (item instanceof CloakOfShadows){
					if (!hero.belongings.lostInventory() || item.keptThroughLostInventory()) {
						((CloakOfShadows) item).activate(Dungeon.hero);
					}
				}
			}
		}

		if (talent == HEIGHTENED_SENSES || talent == FARSIGHT || talent == DIVINE_SENSE || talent == SCO_KEEN_SENSES
				|| talent == AUT_SCANNER || talent == BM_TRACKER){
			Dungeon.observe();
		}

		if (talent == TWIN_UPGRADES || talent == DESPERATE_POWER
				|| talent == STRONGMAN || talent == DURABLE_PROJECTILES){
			Item.updateQuickslot();
		}

		if (talent == UNENCUMBERED_SPIRIT && hero.pointsInTalent(talent) == 3){
			Item toGive = new ClothArmor().identify();
			if (!toGive.collect()){
				Dungeon.level.drop(toGive, hero.pos).sprite.drop();
			}
			toGive = new Gloves().identify();
			if (!toGive.collect()){
				Dungeon.level.drop(toGive, hero.pos).sprite.drop();
			}
		}

		if (talent == LIGHT_READING && hero.heroClass == HeroClass.CLERIC){
			for (Item item : Dungeon.hero.belongings.backpack){
				if (item instanceof HolyTome){
					if (!hero.belongings.lostInventory() || item.keptThroughLostInventory()) {
						((HolyTome) item).activate(Dungeon.hero);
					}
				}
			}
		}

		//if we happen to have spirit form applied with a ring of might
		if (talent == SPIRIT_FORM){
			Dungeon.hero.updateHT(false);
		}
	}

	public static class CachedRationsDropped extends CounterBuff{{revivePersists = true;}};
	public static class NatureBerriesDropped extends CounterBuff{{revivePersists = true;}};

	public static void onFoodEaten( Hero hero, float foodVal, Item foodSource ){
		if (hero.hasTalent(HEARTY_MEAL)){
			//4/6 HP healed, when hero is below 33% health (with a little rounding up)
			if (hero.HP/(float)hero.HT < 0.334f) {
				int healing = 2 + 2 * hero.pointsInTalent(HEARTY_MEAL);
				hero.HP = Math.min(hero.HP + healing, hero.HT);
				hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healing), FloatingText.HEALING);

			}
		}
		if (hero.hasTalent(IRON_STOMACH)){
			if (hero.cooldown() > 0) {
				Buff.affect(hero, WarriorFoodImmunity.class, hero.cooldown());
			}
		}
		if (hero.hasTalent(EMPOWERING_MEAL)){
			//2/3 bonus wand damage for next 3 zaps
			Buff.affect( hero, WandEmpower.class).set(1 + hero.pointsInTalent(EMPOWERING_MEAL), 3);
			ScrollOfRecharging.charge( hero );
		}
		int wandChargeTurns = 0;
		if (hero.hasTalent(ENERGIZING_MEAL)){
			//5/8 turns of recharging
			wandChargeTurns += 2 + 3*hero.pointsInTalent(ENERGIZING_MEAL);
		}
		int artifactChargeTurns = 0;
		if (hero.hasTalent(MYSTICAL_MEAL)){
			//3/5 turns of recharging
			artifactChargeTurns += 1 + 2*hero.pointsInTalent(MYSTICAL_MEAL);
		}
		if (hero.hasTalent(INVIGORATING_MEAL)){
			//effectively 1/2 turns of haste
			Buff.prolong( hero, Haste.class, 0.67f+hero.pointsInTalent(INVIGORATING_MEAL));
		}
		if (hero.hasTalent(STRENGTHENING_MEAL)){
			//3 bonus physical damage for next 2/3 attacks
			Buff.affect( hero, PhysicalEmpower.class).set(3, 1 + hero.pointsInTalent(STRENGTHENING_MEAL));
		}
		if (hero.hasTalent(FOCUSED_MEAL)){
			if (hero.heroClass == HeroClass.DUELIST){
				//0.67/1 charge for the duelist
				Buff.affect( hero, MeleeWeapon.Charger.class ).gainCharge((hero.pointsInTalent(FOCUSED_MEAL)+1)/3f);
				ScrollOfRecharging.charge( hero );
			} else {
				// lvl/3 / lvl/2 bonus dmg on next hit for other classes
				Buff.affect( hero, PhysicalEmpower.class).set(Math.round(hero.lvl / (4f - hero.pointsInTalent(FOCUSED_MEAL))), 1);
			}
		}
		if (hero.hasTalent(SATIATED_SPELLS)){
			if (hero.heroClass == HeroClass.CLERIC) {
				Buff.affect(hero, SatiatedSpellsTracker.class);
			} else {
				//3/5 shielding, delayed up to 10 turns
				int amount = 1 + 2*hero.pointsInTalent(SATIATED_SPELLS);
				Barrier b = Buff.affect(hero, Barrier.class);
				if (b.shielding() <= amount){
					b.setShield(amount);
					b.delay(Math.max(10-b.cooldown(), 0));
				}
			}
		}
		if (hero.hasTalent(ENLIGHTENING_MEAL)){
			if (hero.heroClass == HeroClass.CLERIC) {
				HolyTome tome = hero.belongings.getItem(HolyTome.class);
				if (tome != null) {
					// 2/3 of a charge at +1, 1 full charge at +2
					tome.directCharge( (1+hero.pointsInTalent(ENLIGHTENING_MEAL))/3f );
					ScrollOfRecharging.charge(hero);
				}
			} else {
				//2/3 turns of recharging, both kinds
				wandChargeTurns += 1 + hero.pointsInTalent(ENLIGHTENING_MEAL);
				artifactChargeTurns += 1 + hero.pointsInTalent(ENLIGHTENING_MEAL);
			}
		}

		ChampionHooks.onFoodEaten(hero);

		//we process these at the end as they can stack together from some talents
		if (wandChargeTurns > 0){
			Buff.prolong( hero, Recharging.class, wandChargeTurns );
			ScrollOfRecharging.charge( hero );
			SpellSprite.show(hero, SpellSprite.CHARGE);
		}
		if (artifactChargeTurns > 0){
			ArtifactRecharge buff = Buff.affect( hero, ArtifactRecharge.class);
			if (buff.left() < artifactChargeTurns){
				buff.set(artifactChargeTurns).ignoreHornOfPlenty = foodSource instanceof HornOfPlenty;
			}
			ScrollOfRecharging.charge( hero );
			SpellSprite.show(hero, SpellSprite.CHARGE, 0, 1, 1);
		}
	}

	public static class WarriorFoodImmunity extends FlavourBuff{
		{ actPriority = HERO_PRIO+1; }
	}

	public static float itemIDSpeedFactor( Hero hero, Item item ){
		float factor = 1f;

		// Affected by both Warrior(1.75x/2.5x) and Duelist(2.5x/inst.) talents
		if (item instanceof MeleeWeapon){
			factor *= 1f + 1.5f*hero.pointsInTalent(ADVENTURERS_INTUITION); //instant at +2 (see onItemEquipped)
			factor *= 1f + 0.75f*hero.pointsInTalent(VETERANS_INTUITION);
		}
		// Affected by both Warrior(2.5x/inst.) and Duelist(1.75x/2.5x) talents
		if (item instanceof Armor){
			factor *= 1f + 0.75f*hero.pointsInTalent(ADVENTURERS_INTUITION);
			factor *= 1f + hero.pointsInTalent(VETERANS_INTUITION); //instant at +2 (see onItemEquipped)
		}
		// 3x/instant for Mage (see Wand.wandUsed())
		if (item instanceof Wand){
			factor *= 1f + 2.0f*hero.pointsInTalent(SCHOLARS_INTUITION);
		}
		// 3x/instant speed with Huntress talent (see MissileWeapon.proc)
		if (item instanceof MissileWeapon){
			factor *= 1f + 2.0f*hero.pointsInTalent(SURVIVALISTS_INTUITION);
		}
		// 2x/instant for Rogue (see onItemEqupped), also id's type on equip/on pickup
		if (item instanceof Ring){
			factor *= 1f + hero.pointsInTalent(THIEFS_INTUITION);
		}
		factor *= ChampionHooks.itemIDSpeedFactor(hero, item);
		return factor;
	}

	public static void onPotionUsed( Hero hero, int cell, float factor ){
		ChampionHooks.onPotionUsed(hero, factor);
		if (hero.hasTalent(LIQUID_WILLPOWER)){
			// 6.5/10% of max HP
			int shieldToGive = Math.round( factor * hero.HT * (0.030f + 0.035f*hero.pointsInTalent(LIQUID_WILLPOWER)));
			hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shieldToGive), FloatingText.SHIELDING);
			Buff.affect(hero, Barrier.class).setShield(shieldToGive);
		}
		if (hero.hasTalent(LIQUID_NATURE)){
			ArrayList<Integer> grassCells = new ArrayList<>();
			for (int i : PathFinder.NEIGHBOURS9){
				grassCells.add(cell+i);
			}
			Random.shuffle(grassCells);
			for (int grassCell : grassCells){
				Char ch = Actor.findChar(grassCell);
				if (ch != null && ch.alignment == Char.Alignment.ENEMY){
					//1/2 turns of roots
					Buff.affect(ch, Roots.class, factor * hero.pointsInTalent(LIQUID_NATURE));
				}
				if (Dungeon.level.map[grassCell] == Terrain.EMPTY ||
						Dungeon.level.map[grassCell] == Terrain.EMBERS ||
						Dungeon.level.map[grassCell] == Terrain.EMPTY_DECO){
					Level.set(grassCell, Terrain.GRASS);
					GameScene.updateMap(grassCell);
				}
				CellEmitter.get(grassCell).burst(LeafParticle.LEVEL_SPECIFIC, 4);
			}
			// 4/6 cells total
			int totalGrassCells = (int) (factor * (2 + 2 * hero.pointsInTalent(LIQUID_NATURE)));
			while (grassCells.size() > totalGrassCells){
				grassCells.remove(0);
			}
			for (int grassCell : grassCells){
				int t = Dungeon.level.map[grassCell];
				if ((t == Terrain.EMPTY || t == Terrain.EMPTY_DECO || t == Terrain.EMBERS
						|| t == Terrain.GRASS || t == Terrain.FURROWED_GRASS)
						&& Dungeon.level.plants.get(grassCell) == null){
					Level.set(grassCell, Terrain.HIGH_GRASS);
					GameScene.updateMap(grassCell);
				}
			}
			Dungeon.observe();
		}
		if (hero.hasTalent(LIQUID_AGILITY)){
			Buff.prolong(hero, LiquidAgilEVATracker.class, hero.cooldown() + Math.max(0, factor-1));
			if (factor >= 0.5f){
				Buff.prolong(hero, LiquidAgilACCTracker.class, 5f).uses = Math.round(factor);
			}
		}
	}

	public static void onScrollUsed( Hero hero, int pos, float factor, Class<?extends Item> cls ){
		if (hero.hasTalent(INSCRIBED_POWER)){
			// 2/3 empowered wand zaps
			Buff.affect(hero, ScrollEmpower.class).reset((int) (factor * (1 + hero.pointsInTalent(INSCRIBED_POWER))));
		}
		if (hero.hasTalent(INSCRIBED_STEALTH)){
			// 3/5 turns of stealth
			Buff.affect(hero, Invisibility.class, factor * (1 + 2*hero.pointsInTalent(INSCRIBED_STEALTH)));
			Sample.INSTANCE.play( Assets.Sounds.MELD );
		}
		if (hero.hasTalent(RECALL_INSCRIPTION) && Scroll.class.isAssignableFrom(cls) && cls != ScrollOfUpgrade.class){
			if (hero.heroClass == HeroClass.CLERIC){
				Buff.prolong(hero, RecallInscription.UsedItemTracker.class, hero.pointsInTalent(RECALL_INSCRIPTION) == 2 ? 300 : 10).item = cls;
			} else {
				// 10/15%
				if (Random.Int(20) < 1 + hero.pointsInTalent(RECALL_INSCRIPTION)){
					Reflection.newInstance(cls).collect();
					GLog.p(Messages.get(Talent.class, RECALL_INSCRIPTION.name() + ".refunded"));
				}
			}
		}
	}

	public static void onRunestoneUsed( Hero hero, int pos, Class<?extends Item> cls ){
		if (hero.hasTalent(RECALL_INSCRIPTION) && Runestone.class.isAssignableFrom(cls)){
			if (hero.heroClass == HeroClass.CLERIC){
				Buff.prolong(hero, RecallInscription.UsedItemTracker.class, hero.pointsInTalent(RECALL_INSCRIPTION) == 2 ? 300 : 10).item = cls;
			} else {

				//don't trigger on 1st intuition use
				if (cls.equals(StoneOfIntuition.class) && hero.buff(StoneOfIntuition.IntuitionUseTracker.class) != null){
					return;
				}
				// 10/15%
				if (Random.Int(20) < 1 + hero.pointsInTalent(RECALL_INSCRIPTION)){
					Reflection.newInstance(cls).collect();
					GLog.p(Messages.get(Talent.class, RECALL_INSCRIPTION.name() + ".refunded"));
				}
			}
		}
	}

	public static void onArtifactUsed( Hero hero ){
		if (hero.hasTalent(ENHANCED_RINGS)){
			Buff.prolong(hero, EnhancedRings.class, 3f*hero.pointsInTalent(ENHANCED_RINGS));
		}

		if (Dungeon.hero.heroClass != HeroClass.CLERIC
				&& Dungeon.hero.hasTalent(Talent.DIVINE_SENSE)){
			Buff.prolong(Dungeon.hero, DivineSense.DivineSenseTracker.class, Dungeon.hero.cooldown()+1);
		}

		// 10/20/30%
		if (Dungeon.hero.heroClass != HeroClass.CLERIC
				&& Dungeon.hero.hasTalent(Talent.CLEANSE)
				&& Random.Int(10) < Dungeon.hero.pointsInTalent(Talent.CLEANSE)){
			boolean removed = false;
			for (Buff b : Dungeon.hero.buffs()) {
				if (b.type == Buff.buffType.NEGATIVE
						&& !(b instanceof LostInventory)) {
					b.detach();
					removed = true;
				}
			}
			if (removed && Dungeon.hero.sprite != null) {
				new Flare( 6, 32 ).color(0xFF4CD2, true).show( Dungeon.hero.sprite, 2f );
			}
		}
	}

	public static void onItemEquipped( Hero hero, Item item ){
		boolean identify = false;
		if (hero.pointsInTalent(VETERANS_INTUITION) == 2 && item instanceof Armor){
			identify = true;
		}
		if (hero.hasTalent(THIEFS_INTUITION) && item instanceof Ring){
			if (hero.pointsInTalent(THIEFS_INTUITION) == 2){
				identify = true;
			}
			((Ring) item).setKnown();
		}
		if (hero.pointsInTalent(ADVENTURERS_INTUITION) == 2 && item instanceof Weapon){
			identify = true;
		}

		if (identify) {
			if (ShardOfOblivion.passiveIDDisabled()) {
				if (item instanceof Weapon){
					((Weapon) item).setIDReady();
				} else if (item instanceof Armor){
					((Armor) item).setIDReady();
				} else if (item instanceof Ring){
					((Ring) item).setIDReady();
				}
			} else {
				item.identify();
			}
		}
	}

	public static void onItemCollected( Hero hero, Item item ){
		if (hero.pointsInTalent(THIEFS_INTUITION) == 2){
			if (item instanceof Ring) ((Ring) item).setKnown();
		}
	}

	public static int onAttackProc( Hero hero, Char enemy, int dmg ){

		if (hero.hasTalent(Talent.PROVOKED_ANGER)
			&& hero.buff(ProvokedAngerTracker.class) != null){
			dmg += 1 + 2*hero.pointsInTalent(Talent.PROVOKED_ANGER);
			hero.buff(ProvokedAngerTracker.class).detach();
		}

		if (hero.hasTalent(Talent.LINGERING_MAGIC)
				&& hero.buff(LingeringMagicTracker.class) != null){
			dmg += Random.IntRange(hero.pointsInTalent(Talent.LINGERING_MAGIC) , 2);
			hero.buff(LingeringMagicTracker.class).detach();
		}

		if (hero.hasTalent(Talent.SUCKER_PUNCH)
				&& enemy instanceof Mob && ((Mob) enemy).surprisedBy(hero)
				&& enemy.buff(SuckerPunchTracker.class) == null){
			dmg += Random.IntRange(hero.pointsInTalent(Talent.SUCKER_PUNCH) , 2);
			Buff.affect(enemy, SuckerPunchTracker.class);
		}

		if (hero.hasTalent(Talent.FOLLOWUP_STRIKE) && enemy.isAlive() && enemy.alignment == Char.Alignment.ENEMY) {
			if (hero.belongings.attackingWeapon() instanceof MissileWeapon) {
				Buff.prolong(hero, FollowupStrikeTracker.class, 5f).object = enemy.id();
			} else if (hero.buff(FollowupStrikeTracker.class) != null
					&& hero.buff(FollowupStrikeTracker.class).object == enemy.id()){
				dmg += 1 + hero.pointsInTalent(FOLLOWUP_STRIKE);
				hero.buff(FollowupStrikeTracker.class).detach();
			}
		}

		if (hero.buff(Talent.SpiritBladesTracker.class) != null
				&& Random.Int(10) < 3*hero.pointsInTalent(Talent.SPIRIT_BLADES)){
			SpiritBow bow = hero.belongings.getItem(SpiritBow.class);
			if (bow != null) dmg = bow.proc( hero, enemy, dmg );
			hero.buff(Talent.SpiritBladesTracker.class).detach();
		}

		if (hero.hasTalent(PATIENT_STRIKE)){
			if (hero.buff(PatientStrikeTracker.class) != null
					&& !(hero.belongings.attackingWeapon() instanceof MissileWeapon)){
				hero.buff(PatientStrikeTracker.class).detach();
				dmg += Random.IntRange(hero.pointsInTalent(Talent.PATIENT_STRIKE), 2);
			}
		}

		if (hero.hasTalent(DEADLY_FOLLOWUP) && enemy.alignment == Char.Alignment.ENEMY) {
			if (hero.belongings.attackingWeapon() instanceof MissileWeapon) {
				if (!(hero.belongings.attackingWeapon() instanceof SpiritBow.SpiritArrow)) {
					Buff.prolong(hero, DeadlyFollowupTracker.class, 5f).object = enemy.id();
				}
			} else if (hero.buff(DeadlyFollowupTracker.class) != null
					&& hero.buff(DeadlyFollowupTracker.class).object == enemy.id()){
				dmg = Math.round(dmg * (1.0f + .1f*hero.pointsInTalent(DEADLY_FOLLOWUP)));
			}
		}

		dmg = ChampionHooks.onAttackProc(hero, enemy, dmg);

		return dmg;
	}

	public static class ProvokedAngerTracker extends FlavourBuff{
		{ type = Buff.buffType.POSITIVE; }
		public int icon() { return BuffIndicator.WEAPON; }
		public void tintIcon(Image icon) { icon.hardlight(1.43f, 1.43f, 1.43f); }
		public float iconFadePercent() { return Math.max(0, 1f - (visualcooldown() / 5)); }
	}
	public static class LingeringMagicTracker extends FlavourBuff{
		{ type = Buff.buffType.POSITIVE; }
		public int icon() { return BuffIndicator.WEAPON; }
		public void tintIcon(Image icon) { icon.hardlight(1.43f, 1.43f, 0f); }
		public float iconFadePercent() { return Math.max(0, 1f - (visualcooldown() / 5)); }
	}
	public static class SuckerPunchTracker extends Buff{};
	public static class FollowupStrikeTracker extends FlavourBuff{
		public int object;
		{ type = Buff.buffType.POSITIVE; }
		public int icon() { return BuffIndicator.INVERT_MARK; }
		public void tintIcon(Image icon) { icon.hardlight(0f, 0.75f, 1f); }
		public float iconFadePercent() { return Math.max(0, 1f - (visualcooldown() / 5)); }
		private static final String OBJECT    = "object";
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(OBJECT, object);
		}
		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			object = bundle.getInt(OBJECT);
		}
	};

	public static final int MAX_TALENT_TIERS = 5;

	public static void initClassTalents( Hero hero ){
		initClassTalents( hero.heroClass, hero.talents, hero.metamorphedTalents );
	}

	public static void initClassTalents( HeroClass cls, ArrayList<LinkedHashMap<Talent, Integer>> talents){
		initClassTalents( cls, talents, new LinkedHashMap<>());
	}

	public static void initClassTalents( HeroClass cls, ArrayList<LinkedHashMap<Talent, Integer>> talents, LinkedHashMap<Talent, Talent> replacements ){
		while (talents.size() < MAX_TALENT_TIERS){
			talents.add(new LinkedHashMap<>());
		}

		ArrayList<Talent> tierTalents = new ArrayList<>();

		//tier 1
		switch (cls){
			case WARRIOR: default:
				Collections.addAll(tierTalents, HEARTY_MEAL, VETERANS_INTUITION, PROVOKED_ANGER, IRON_WILL);
				break;
			case MAGE:
				Collections.addAll(tierTalents, EMPOWERING_MEAL, SCHOLARS_INTUITION, LINGERING_MAGIC, BACKUP_BARRIER);
				break;
			case ROGUE:
				Collections.addAll(tierTalents, CACHED_RATIONS, THIEFS_INTUITION, SUCKER_PUNCH, PROTECTIVE_SHADOWS);
				break;
			case HUNTRESS:
				Collections.addAll(tierTalents, NATURES_BOUNTY, SURVIVALISTS_INTUITION, FOLLOWUP_STRIKE, NATURES_AID);
				break;
			case DUELIST:
				Collections.addAll(tierTalents, STRENGTHENING_MEAL, ADVENTURERS_INTUITION, PATIENT_STRIKE, AGGRESSIVE_BARRIER);
				break;
			case CLERIC:
				Collections.addAll(tierTalents, SATIATED_SPELLS, HOLY_INTUITION, SEARING_LIGHT, SHIELD_OF_LIGHT);
				break;
			case EXECUTIONER:
				Collections.addAll(tierTalents, EXE_MEAL, EXE_INTUITION, EXE_DEEP_WOUNDS, EXE_THICK_SKIN);
				break;
			case SCOUT:
				Collections.addAll(tierTalents, SCO_MEAL, SCO_INTUITION, SCO_POTENT_VENOM, SCO_BLINDING_EYE);
				break;
			case PYROMANCER:
				Collections.addAll(tierTalents, PYR_MEAL, PYR_INTUITION, PYR_SPARK, PYR_WARMTH);
				break;
			case JAILER:
				Collections.addAll(tierTalents, JAI_MEAL, JAI_INTUITION, JAI_LONG_CHAIN, JAI_HUNGRY_SOULS);
				break;
			case BRAWLER:
				Collections.addAll(tierTalents, BRA_MEAL, BRA_INTUITION, BRA_HEAVY_HANDS, BRA_SECOND_WIND);
				break;
			case GRAVEDIGGER:
				Collections.addAll(tierTalents, GRA_MEAL, GRA_INTUITION, GRA_HEAVY_SHOVEL, GRA_DEEP_GRAVES);
				break;
			case GUARDIAN:
				Collections.addAll(tierTalents, GUA_MEAL, GUA_INTUITION, GUA_SAND_SKIN, GUA_HUNGRY_STAFF);
				break;
			case BARD:
				Collections.addAll(tierTalents, BAR_MEAL, BAR_INTUITION, BAR_LOUD_VOICE, BAR_APPLAUSE);
				break;
			//botanico
			case BOTANIST:
				Collections.addAll(tierTalents, BOT_MEAL, BOT_INTUITION, BOT_THORNY_SPROUT, BOT_FRESH_AIR);
				break;
			//ornn
			case FORGER:
				Collections.addAll(tierTalents, FOR_MEAL, FOR_INTUITION, FOR_THICK_PLATES, FOR_HEAVY_HAMMER);
				break;
			//mordekaiser
			case IRONLORD:
				Collections.addAll(tierTalents, IRL_MEAL, IRL_INTUITION, IRL_HEAVY_MACE, IRL_DARK_HIDE);
				break;
			//alt worlds
			case AUTOMATON:
				Collections.addAll(tierTalents, AUT_COOLANT, AUT_DIAGNOSTICS, AUT_PLATING, AUT_CAPACITOR);
				break;
			case BEASTMASTER:
				Collections.addAll(tierTalents, BM_SHARED_MEAL, BM_TRACKER, BM_THICK_FUR, BM_SHARP_FANGS);
				break;
			case SPIRITIST:
				Collections.addAll(tierTalents, SP_SOUL_FEAST, SP_DEATH_SENSE, SP_GRAVE_CHILL, SP_SOUL_HARVEST);
				break;
			case GUNSLINGER:
				Collections.addAll(tierTalents, GS_GUN_OIL, GS_APPRAISAL, GS_STEADY_AIM, GS_THICK_COAT);
				break;
		}
		for (Talent talent : tierTalents){
			if (replacements.containsKey(talent)){
				talent = replacements.get(talent);
			}
			talents.get(0).put(talent, 0);
		}
		tierTalents.clear();

		//tier 2
		switch (cls){
			case WARRIOR: default:
				Collections.addAll(tierTalents, IRON_STOMACH, LIQUID_WILLPOWER, RUNIC_TRANSFERENCE, LETHAL_MOMENTUM, IMPROVISED_PROJECTILES);
				break;
			case MAGE:
				Collections.addAll(tierTalents, ENERGIZING_MEAL, INSCRIBED_POWER, WAND_PRESERVATION, ARCANE_VISION, SHIELD_BATTERY);
				break;
			case ROGUE:
				Collections.addAll(tierTalents, MYSTICAL_MEAL, INSCRIBED_STEALTH, WIDE_SEARCH, SILENT_STEPS, ROGUES_FORESIGHT);
				break;
			case HUNTRESS:
				Collections.addAll(tierTalents, INVIGORATING_MEAL, LIQUID_NATURE, REJUVENATING_STEPS, HEIGHTENED_SENSES, DURABLE_PROJECTILES);
				break;
			case DUELIST:
				Collections.addAll(tierTalents, FOCUSED_MEAL, LIQUID_AGILITY, WEAPON_RECHARGING, LETHAL_HASTE, SWIFT_EQUIP);
				break;
			case CLERIC:
				Collections.addAll(tierTalents, ENLIGHTENING_MEAL, RECALL_INSCRIPTION, SUNRAY, DIVINE_SENSE, BLESS);
				break;
			case EXECUTIONER:
				Collections.addAll(tierTalents, EXE_IRON_RESOLVE, EXE_LONG_REACH, EXE_CRUEL_BLOW, EXE_BLOODLUST, EXE_INTIMIDATE);
				break;
			case SCOUT:
				Collections.addAll(tierTalents, SCO_FLEET_FOOT, SCO_QUICK_CAMO, SCO_MORE_SHROOMS, SCO_LIQUID_CAMO, SCO_KEEN_SENSES);
				break;
			case PYROMANCER:
				Collections.addAll(tierTalents, PYR_FIRE_DRINKER, PYR_WIDE_FLAMES, PYR_BEAR_HUG, PYR_EARLY_SPARK, PYR_EMBER_STEP);
				break;
			case JAILER:
				Collections.addAll(tierTalents, JAI_SOUL_MEND, JAI_HEAVY_LANTERN, JAI_FLAY_FORCE, JAI_LIQUID_SOULS, JAI_WARDENS_GAZE);
				break;
			case BRAWLER:
				Collections.addAll(tierTalents, BRA_LIQUID_GRIT, BRA_ONE_TWO, BRA_STUBBORN, BRA_FOOTWORK, BRA_TAUNT);
				break;
			case GRAVEDIGGER:
				Collections.addAll(tierTalents, GRA_BOTTLED_MIST, GRA_LONG_PROCESSION, GRA_CLINGING_MIST, GRA_HARD_BONES, GRA_GRIM_HARVEST);
				break;
			case GUARDIAN:
				Collections.addAll(tierTalents, GUA_LIQUID_SAND, GUA_LONG_WITHER, GUA_HOT_SPIRIT, GUA_LIFE_DRAIN, GUA_COLLECTOR);
				break;
			case BARD:
				Collections.addAll(tierTalents, BAR_LIQUID_MUSIC, BAR_DEEP_SLEEP, BAR_SOOTHING, BAR_RALLY, BAR_TEMPO);
				break;
			//botanico
			case BOTANIST:
				Collections.addAll(tierTalents, BOT_LIQUID_SAP, BOT_STRONG_ROOTS, BOT_DEEP_ROOTS, BOT_PRUNING, BOT_SEED_BAG);
				break;
			//ornn
			case FORGER:
				Collections.addAll(tierTalents, FOR_LIQUID_FORGE, FOR_LONG_RIFT, FOR_WIDE_BELLOWS, FOR_LONG_CHARGE, FOR_TEMPER);
				break;
			//mordekaiser
			case IRONLORD:
				Collections.addAll(tierTalents, IRL_LIQUID_IRON, IRL_LONG_REACH, IRL_BRUTE_FORCE, IRL_GRAVE_WARD, IRL_RESONANCE);
				break;
			//alt worlds
			case AUTOMATON:
				Collections.addAll(tierTalents, AUT_SELF_REPAIR, AUT_SCANNER, AUT_OVERCLOCK, AUT_GROUNDING, AUT_SALVAGE);
				break;
			case BEASTMASTER:
				Collections.addAll(tierTalents, BM_BONE_THROW, BM_HOWL, BM_QUICK_RECOVERY, BM_THORNS, BM_NATURES_BOND);
				break;
			case SPIRITIST:
				Collections.addAll(tierTalents, SP_LINGERING_DREAD, SP_GHOSTLY_GRASP, SP_OMEN_WEIGHT, SP_RESTLESS_DEAD, SP_DARK_PACT);
				break;
			case GUNSLINGER:
				Collections.addAll(tierTalents, GS_FAST_RELOAD, GS_DEADEYE, GS_BIG_BOUNTY, GS_CARPET_BOMB, GS_HOT_STREAK);
				break;
		}
		for (Talent talent : tierTalents){
			if (replacements.containsKey(talent)){
				talent = replacements.get(talent);
			}
			talents.get(1).put(talent, 0);
		}
		tierTalents.clear();

		//tier 3
		switch (cls){
			case WARRIOR: default:
				Collections.addAll(tierTalents, HOLD_FAST, STRONGMAN);
				break;
			case MAGE:
				Collections.addAll(tierTalents, DESPERATE_POWER, ALLY_WARP);
				break;
			case ROGUE:
				Collections.addAll(tierTalents, ENHANCED_RINGS, LIGHT_CLOAK);
				break;
			case HUNTRESS:
				Collections.addAll(tierTalents, POINT_BLANK, SEER_SHOT);
				break;
			case DUELIST:
				Collections.addAll(tierTalents, PRECISE_ASSAULT, DEADLY_FOLLOWUP);
				break;
			case CLERIC:
				Collections.addAll(tierTalents, CLEANSE, LIGHT_READING);
				break;
			case EXECUTIONER:
				Collections.addAll(tierTalents, EXE_SHARP_GUILLOTINE, EXE_RELENTLESS);
				break;
			case SCOUT:
				Collections.addAll(tierTalents, SCO_BIG_SHROOM, SCO_AMBUSH);
				break;
			case PYROMANCER:
				Collections.addAll(tierTalents, PYR_COMBUSTION, PYR_SCORCHED);
				break;
			case JAILER:
				Collections.addAll(tierTalents, JAI_DEATHS_GRIP, JAI_PRISON);
				break;
			case BRAWLER:
				Collections.addAll(tierTalents, BRA_BIG_SHOW, BRA_GRIT_SHIELD);
				break;
			case GRAVEDIGGER:
				Collections.addAll(tierTalents, GRA_MAIDENS_WRATH, GRA_FLOCK);
				break;
			case GUARDIAN:
				Collections.addAll(tierTalents, GUA_ETERNAL, GUA_DUSTSTORM);
				break;
			case BARD:
				Collections.addAll(tierTalents, BAR_VIRTUOSO, BAR_DUET);
				break;
			//botanico
			case BOTANIST:
				Collections.addAll(tierTalents, BOT_OVERGROWTH, BOT_ANCIENT_BARK);
				break;
			//ornn
			case FORGER:
				Collections.addAll(tierTalents, FOR_RAM_HORNS, FOR_FIRE_PILLARS);
				break;
			//mordekaiser
			case IRONLORD:
				Collections.addAll(tierTalents, IRL_DEEPENING_DARK, IRL_UNDYING_WILL);
				break;
			//alt worlds
			case AUTOMATON:
				Collections.addAll(tierTalents, AUT_HIGH_VOLTAGE, AUT_FAILSAFE);
				break;
			case BEASTMASTER:
				Collections.addAll(tierTalents, BM_ELEMENTAL_FANGS, BM_BIG_BEAR);
				break;
			case SPIRITIST:
				Collections.addAll(tierTalents, SP_BONE_LEGION, SP_DREAD_HOWL);
				break;
			case GUNSLINGER:
				Collections.addAll(tierTalents, GS_EXECUTION, GS_RICOCHET);
				break;
		}
		for (Talent talent : tierTalents){
			if (replacements.containsKey(talent)){
				talent = replacements.get(talent);
			}
			talents.get(2).put(talent, 0);
		}
		tierTalents.clear();

		//tier4
		//TBD
	}

	public static void initSubclassTalents( Hero hero ){
		initSubclassTalents( hero.subClass, hero.talents );
	}

	public static void initSubclassTalents( HeroSubClass cls, ArrayList<LinkedHashMap<Talent, Integer>> talents ){
		if (cls == HeroSubClass.NONE) return;

		while (talents.size() < MAX_TALENT_TIERS){
			talents.add(new LinkedHashMap<>());
		}

		ArrayList<Talent> tierTalents = new ArrayList<>();

		//tier 3
		switch (cls){
			case BERSERKER: default:
				Collections.addAll(tierTalents, ENDLESS_RAGE, DEATHLESS_FURY, ENRAGED_CATALYST);
				break;
			case GLADIATOR:
				Collections.addAll(tierTalents, CLEAVE, LETHAL_DEFENSE, ENHANCED_COMBO);
				break;
			case BATTLEMAGE:
				Collections.addAll(tierTalents, EMPOWERED_STRIKE, MYSTICAL_CHARGE, EXCESS_CHARGE);
				break;
			case WARLOCK:
				Collections.addAll(tierTalents, SOUL_EATER, SOUL_SIPHON, NECROMANCERS_MINIONS);
				break;
			case ASSASSIN:
				Collections.addAll(tierTalents, ENHANCED_LETHALITY, ASSASSINS_REACH, BOUNTY_HUNTER);
				break;
			case FREERUNNER:
				Collections.addAll(tierTalents, EVASIVE_ARMOR, PROJECTILE_MOMENTUM, SPEEDY_STEALTH);
				break;
			case SNIPER:
				Collections.addAll(tierTalents, FARSIGHT, SHARED_ENCHANTMENT, SHARED_UPGRADES);
				break;
			case WARDEN:
				Collections.addAll(tierTalents, DURABLE_TIPS, BARKSKIN, SHIELDING_DEW);
				break;
			case CHAMPION:
				Collections.addAll(tierTalents, VARIED_CHARGE, TWIN_UPGRADES, COMBINED_LETHALITY);
				break;
			case MONK:
				Collections.addAll(tierTalents, UNENCUMBERED_SPIRIT, MONASTIC_VIGOR, COMBINED_ENERGY);
				break;
			case PRIEST:
				Collections.addAll(tierTalents, HOLY_LANCE, HALLOWED_GROUND, MNEMONIC_PRAYER);
				break;
			case PALADIN:
				Collections.addAll(tierTalents, LAY_ON_HANDS, AURA_OF_PROTECTION, WALL_OF_LIGHT);
				break;
			case CONQUEROR:
				Collections.addAll(tierTalents, CQ_WARLORD, CQ_SPIN, CQ_BLOODFEAST);
				break;
			case HEADSMAN:
				Collections.addAll(tierTalents, HS_SENTENCE, HS_REPEAT, HS_BLOODY_AXE);
				break;
			case TRAPPER:
				Collections.addAll(tierTalents, TR_BOUNCE, TR_PARALYTIC, TR_LONG_THROW);
				break;
			case SKIRMISHER:
				Collections.addAll(tierTalents, SK_CORROSIVE, SK_SHADOW_STEP, SK_RICOCHET);
				break;
			case TAMER:
				Collections.addAll(tierTalents, TM_BIG_BEAR, TM_BOND, TM_RESUMMON);
				break;
			case INFERNALIST:
				Collections.addAll(tierTalents, IN_PYROCLASM, IN_FLASHFIRE, IN_HEARTFIRE);
				break;
			case SOULREAPER:
				Collections.addAll(tierTalents, SR_SOUL_ARMOR, SR_REAP, SR_SOUL_FLAME);
				break;
			case LANTERNKEEPER:
				Collections.addAll(tierTalents, LK_LIGHT_WARD, LK_ESCAPE, LK_BOX_LANTERN);
				break;
			case PITBOSS:
				Collections.addAll(tierTalents, PB_THICK_HIDE, PB_IRON_JAW, PB_HAYMAKER);
				break;
			case SHOWMAN:
				Collections.addAll(tierTalents, SM_CROWD, SM_ENCORE, SM_FLASHY);
				break;
			case MISTLORD:
				Collections.addAll(tierTalents, ML_MIST_BURST, ML_ENDLESS_NIGHT, ML_DARK_COMMUNION);
				break;
			case EXHUMER:
				Collections.addAll(tierTalents, EX_GRAVE_STRENGTH, EX_DIG_DEEP, EX_SOUL_SHOVEL);
				break;
			case CURATOR:
				Collections.addAll(tierTalents, CU_ARCHIVE, CU_QUICK_SIPHON, CU_SPIRIT_TAX);
				break;
			case COLOSSUS:
				Collections.addAll(tierTalents, CO_GIANT, CO_UNBROKEN, CO_WITHERING_STEPS);
				break;
			case TROUBADOUR:
				Collections.addAll(tierTalents, TB_ANTHEM, TB_LIFE_SONG, TB_HARMONY);
				break;
			case MINSTREL:
				Collections.addAll(tierTalents, MI_DISSONANCE, MI_RUDE_AWAKENING, MI_CRESCENDO);
				break;
			//botanico
			case WARGARDENER:
				Collections.addAll(tierTalents, WG_HARD_WOOD, WG_TWIN_SPROUTS, WG_THORN_BARRAGE);
				break;
			case DRUID:
				Collections.addAll(tierTalents, DR_LIFE_BLOOM, DR_PURIFYING_SAP, DR_SPRING_RAIN);
				break;
			//ornn
			case MASTERSMITH:
				Collections.addAll(tierTalents, MS_FINE_TEMPER, MS_QUICK_ANVIL, MS_MOLTEN_GIFT);
				break;
			case LIVINGMOUNTAIN:
				Collections.addAll(tierTalents, LM_STONE_SKIN, LM_QUAKE, LM_GRANITE_HEART);
				break;
			//mordekaiser
			case TYRANT:
				Collections.addAll(tierTalents, TY_CRUSHING_AURA, TY_OVERLORD, TY_IRON_FIST);
				break;
			case REVENANT:
				Collections.addAll(tierTalents, RV_BULWARK, RV_SOUL_FEAST, RV_ENDLESS_NIGHT);
				break;
			//alt worlds
			case SWARM_ENGINEER:
				Collections.addAll(tierTalents, SE_HIVE, SE_ARMORED_DRONES, SE_FOCUS_FIRE);
				break;
			case TESLA_COIL:
				Collections.addAll(tierTalents, TC_ARC, TC_SHOCK, TC_FEEDBACK);
				break;
			case PACK_ALPHA:
				Collections.addAll(tierTalents, PA_THIRD_WOLF, PA_PACK_TACTICS, PA_ALPHA_ROAR);
				break;
			case FOREST_WARDEN:
				Collections.addAll(tierTalents, FW_IRONBARK, FW_ROOTS, FW_BEAR_HUG);
				break;
			case SPECTRE_LORD:
				Collections.addAll(tierTalents, SL_WAILING, SL_ETHEREAL, SL_SPECTRAL_HIDE);
				break;
			case PLAGUE_HERALD:
				Collections.addAll(tierTalents, PH_VIRULENCE, PH_SPREAD, PH_BONE_HARVEST);
				break;
			case QUICKDRAW:
				Collections.addAll(tierTalents, QD_HAIR_TRIGGER, QD_SPEEDLOADER, QD_DUELIST);
				break;
			case BOUNTY_HUNTER:
				Collections.addAll(tierTalents, BH_MARKED, BH_LONG_HUNT, BH_COLLECTOR);
				break;
		}
		for (Talent talent : tierTalents){
			talents.get(2).put(talent, 0);
		}
		tierTalents.clear();

	}

	public static void initArmorTalents( Hero hero ){
		initArmorTalents( hero.armorAbility, hero.talents);
	}

	public static void initArmorTalents(ArmorAbility abil, ArrayList<LinkedHashMap<Talent, Integer>> talents ){
		if (abil == null) return;

		while (talents.size() < MAX_TALENT_TIERS){
			talents.add(new LinkedHashMap<>());
		}

		for (Talent t : abil.talents()){
			talents.get(3).put(t, 0);
		}
	}

	public static void initSpecTalents( Hero hero ){
		initSpecTalents( hero.specialization, hero.talents );
	}

	public static void initSpecTalents( Specialization spec, ArrayList<LinkedHashMap<Talent, Integer>> talents ){
		if (spec == null) return;

		while (talents.size() < MAX_TALENT_TIERS){
			talents.add(new LinkedHashMap<>());
		}

		for (Talent t : spec.talents()){
			talents.get(4).put(t, 0);
		}
	}

	private static final String TALENT_TIER = "talents_tier_";

	public static void storeTalentsInBundle( Bundle bundle, Hero hero ){
		for (int i = 0; i < MAX_TALENT_TIERS; i++){
			LinkedHashMap<Talent, Integer> tier = hero.talents.get(i);
			Bundle tierBundle = new Bundle();

			for (Talent talent : tier.keySet()){
				if (tier.get(talent) > 0){
					tierBundle.put(talent.name(), tier.get(talent));
				}
				if (tierBundle.contains(talent.name())){
					tier.put(talent, Math.min(tierBundle.getInt(talent.name()), talent.maxPoints()));
				}
			}
			bundle.put(TALENT_TIER+(i+1), tierBundle);
		}

		Bundle replacementsBundle = new Bundle();
		for (Talent t : hero.metamorphedTalents.keySet()){
			replacementsBundle.put(t.name(), hero.metamorphedTalents.get(t));
		}
		bundle.put("replacements", replacementsBundle);
	}

	private static final HashSet<String> removedTalents = new LinkedHashSet<>();
	static{
		//nothing atm
	}

	private static final HashMap<String, String> renamedTalents = new LinkedHashMap<>();
	static{
		//nothing atm
	}

	public static void restoreTalentsFromBundle( Bundle bundle, Hero hero ){
		if (bundle.contains("replacements")){
			Bundle replacements = bundle.getBundle("replacements");
			for (String key : replacements.getKeys()){
				String value = replacements.getString(key);
				if (renamedTalents.containsKey(key)) key = renamedTalents.get(key);
				if (renamedTalents.containsKey(value)) value = renamedTalents.get(value);
				if (!removedTalents.contains(key) && !removedTalents.contains(value)){
					try {
						hero.metamorphedTalents.put(Talent.valueOf(key), Talent.valueOf(value));
					} catch (Exception e) {
						ShatteredPixelDungeon.reportException(e);
					}
				}
			}
		}

		if (hero.heroClass != null)     initClassTalents(hero);
		if (hero.subClass != null)      initSubclassTalents(hero);
		if (hero.armorAbility != null)  initArmorTalents(hero);
		if (hero.specialization != null) initSpecTalents(hero);

		for (int i = 0; i < MAX_TALENT_TIERS; i++){
			LinkedHashMap<Talent, Integer> tier = hero.talents.get(i);
			Bundle tierBundle = bundle.contains(TALENT_TIER+(i+1)) ? bundle.getBundle(TALENT_TIER+(i+1)) : null;

			if (tierBundle != null){
				for (String tName : tierBundle.getKeys()){
					int points = tierBundle.getInt(tName);
					if (renamedTalents.containsKey(tName)) tName = renamedTalents.get(tName);
					if (!removedTalents.contains(tName)) {
						try {
							Talent talent = Talent.valueOf(tName);
							if (tier.containsKey(talent)) {
								tier.put(talent, Math.min(points, talent.maxPoints()));
							}
						} catch (Exception e) {
							ShatteredPixelDungeon.reportException(e);
						}
					}
				}
			}
		}
	}

}
