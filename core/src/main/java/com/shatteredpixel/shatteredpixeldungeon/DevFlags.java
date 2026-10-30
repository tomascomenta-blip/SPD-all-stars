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

package com.shatteredpixel.shatteredpixeldungeon;

//test switches, desktop only, e.g.
//  java -Dspd.test.god=1 -Dspd.test.start=WARRIOR -Dspd.test.startdepth=20 -jar desktop.jar
//nothing sets these in a real game so you just get the normal values.
//bots & test scripts (CoopAuto, SoloTest, SKTest...) read their own, and spd.coop.* lives w/ the network code.
//(yes there are a LOT of these. they all earned their place, mostly)
public class DevFlags {

	private static boolean on( String name ){
		return "1".equals(System.getProperty("spd.test." + name));
	}

	private static int number( String name, int normal ){
		return Integer.getInteger("spd.test." + name, normal);
	}

	private static String text( String name ){
		return System.getProperty("spd.test." + name);
	}

	// *** heroes and runs ***

	//god=1: the heroes take no damage and don't freeze, so the bots reach the bosses alive
	public static boolean god(){ return on("god"); }

	//start=CLASS: a solo run with that hero class starts at once (null in a normal game)
	public static String start(){ return text("start"); }

	//startdepth=N: that solo run starts at floor N
	public static int startDepth(){ return number("startdepth", 0); }

	//extraheroes=N: the hero selection repeats the last classes N times, to test its layout
	public static int extraHeroes(){ return number("extraheroes", 0); }

	//afk=1: a cooperative run starts with AFK on
	public static boolean afk(){ return on("afk"); }

	//antiop=N: a new run (solo, or the host's) caps every hit on a monster at N, whatever the options say
	public static int antiOp(){ return number("antiop", 0); }

	// *** the alt worlds ***

	//skworld=1: every finished world, =all also the unfinished ones, =0 none (null in a normal game)
	public static String skWorld(){ return text("skworld"); }

	//skevents=1: an ambush room and a trap chest on every floor of a world
	public static boolean skEvents(){ return on("skevents"); }

	//skchampion=N: one of each N monsters of a world is a champion
	public static int skChampion( int normal ){ return number("skchampion", normal); }

	//skforest=1, skkingdom=1, skstarship=1, skundersea=1: every floor of that world gets all its
	//features, and sooner those that come on a timer
	public static boolean skForest(){ return on("skforest"); }
	public static boolean skKingdom(){ return on("skkingdom"); }
	public static boolean skStarship(){ return on("skstarship"); }
	public static boolean skUndersea(){ return on("skundersea"); }

	//lairnests=T: the nests in the lair of the Varkolyn lord break at turn T
	public static int lairNests(){ return number("lairnests", -1); }

	//mindcontrol=T: the second hero (or the only one) is put under mind control at turn T
	public static int mindControl(){ return number("mindcontrol", -1); }

	// *** bosses ***

	//kraken=1: on floor 20 the sea takes the party to the Kraken at once
	public static boolean kraken(){ return on("kraken"); }

	//krakenweak=1: the tentacles of the Kraken have 1 health
	public static boolean krakenWeak(){ return on("krakenweak"); }

	//krakenstart=N: the fight with the Kraken starts at its N-th assault
	public static int krakenStart(){ return number("krakenstart", 1); }

	//knightweak=1: the dark grand knight has little health
	//thal=1: walking out of floor 1 with the amulet always brings Thal-Vekhorr, even without the five worlds
	public static boolean thal(){ return on("thal"); }

	//continent=N: a test run starts on that layer of the dark continent, already past Thal-Vekhorr
	//(1 the surface of the ash plains, 2 the tunnels under it, 3 its abyss)
	public static int continent(){ return number("continent", 0); }
	//biome=flesh|city|frost|bones|isles: that run starts in that biome instead of the ash plains
	public static String biome(){ return text("biome"); }
	//landwalk=W,N,E...: the hero (a bot in co-op) walks out through those passes, one after the
	//other, each from wherever it arrived (LandWalk)
	public static String landWalk(){ return text("landwalk"); }
	//landshots=<dir>;x,y,zoom;...;map: the open world lit from above, a picture per spot (LandShots)
	public static String landShots(){ return text("landshots"); }
	//landhop=1: solo, the hero walks down the skull, back up, down the chasm and back up (LandHop)
	public static boolean landHop(){ return on("landhop"); }
	//landdump=1: the layout of each new surface goes to the log as text (SurfaceBuilder)
	public static boolean landDump(){ return on("landdump"); }
	//worldmap=1: the map of the open world opens by itself on every arrival
	public static boolean worldMap(){ return on("worldmap"); }
	//shrine=1: the besieged shrines wake up on their own and count as guarded
	public static boolean shrineTest(){ return on("shrine"); }

	//thalnow=1: arriving on floor 1 is enough, the victory cracks right away
	public static boolean thalNow(){ return on("thalnow"); }

	//thalweak=1: a tenth of his hp. thalact=N: the fight starts at act N
	public static boolean thalWeak(){ return on("thalweak"); }
	public static int thalAct(){ return number("thalact", 1); }

	public static boolean knightWeak(){ return on("knightweak"); }

	//knightact=N: the fight with the dark grand knight starts at its N-th act
	public static int knightAct(){ return number("knightact", 1); }
	//priestweak=1, priestact=N, priestbuff=X: the high priest of the forest, the same way
	public static boolean priestWeak(){ return on("priestweak"); }
	public static int priestAct(){ return number("priestact", 1); }
	public static String priestBuff(){ return text("priestbuff"); }
	//varkweak=1, varkphase=N: the varkolyn queen, the same way
	public static boolean varkWeak(){ return on("varkweak"); }
	public static int varkPhase(){ return number("varkphase", 1); }

	//thalrewards=1: every hero gets the loot of Thal-Vekhorr as the run starts
	public static boolean thalRewards(){ return on("thalrewards"); }
	//thalexp=N: and then the experience for N more levels (past 30 if the loot let them)
	public static int thalExp(){ return number("thalexp", 0); }
	//thalclock=1: the melted clock of that loot comes fully wound
	public static boolean thalClock(){ return on("thalclock"); }
	//thalwalk=1: a solo run with that loot plays it by itself (heart, chest, clock, then a few demons)
	public static boolean thalWalk(){ return on("thalwalk"); }
	//echo=N (1-5): on every floor the echo of that world's boss comes out 3-4 cells from the party,
	//=all: the five, each one when the last one breaks
	public static String echo(){ return text("echo"); }
	//echokit=N: with echo on, every hero gets a greatsword and plate armor +N (the gear of the last fight)
	public static int echoKit(){ return number("echokit", 0); }

	// *** the dark continent ***
	//cmob=eye,larva...|all: on every floor those monsters of the continent come out 3-6 cells from
	//the party, one kind at a time (eye*3 = three eyes), and the floor's own monsters go away
	public static String cmob(){ return text("cmob"); }
	//cmobkit=N: with cmob on, every hero gets a greatsword and plate armor +N
	public static int cmobKit(){ return number("cmobkit", 0); }
	//pyre=1: a new run (solo or the host's) starts in the arena of the walking pyre, at the end of the ash plains
	public static boolean pyre(){ return on("pyre"); }
	//pyreweak=1: the walking pyre has a tenth of its hp. pyrephase=N: its fight starts at phase N
	public static boolean pyreWeak(){ return on("pyreweak"); }
	public static int pyrePhase(){ return number("pyrephase", 1); }
	//pyrekit=N: when its fight starts, every hero below tier 5 gear gets a greatsword and plate armor +N
	public static int pyreKit(){ return number("pyrekit", 0); }
	//colpreview=<dir>: a solo run on an empty plain with the drawings of <dir>/preview.txt laid out (ColossusPreview)
	public static String colPreview(){ return text("colpreview"); }
	//bigtest=1: with colpreview, two plain colossi come for the hero (BigTest)
	public static boolean bigTest(){ return on("bigtest"); }
	//fauna=world|world1|world2|rex,pyre...: with colpreview, the beasts of the ash plains (FaunaTest): a whole layer
	//filled the way a real floor is (0 surface, 1 depths, 2 abyss), or those kinds placed around the hero.
	//flesh:... and city:... (flesh:world, flesh:heart,elk, city:king) are the beasts of those zones
	public static String fauna(){ return text("fauna"); }
	//faunabot=1: the hero of that test plays by itself (dodges the marks, hits back); =2 also walks at them
	public static int faunaBot(){ return number("faunabot", 0); }
	//faunalit=1: the hero of that test carries a torch all the time
	public static boolean faunaLit(){ return on("faunalit"); }
	//faunashow=1: every beast of that test is drawn even out of sight (for screenshots)
	public static boolean faunaShow(){ return on("faunashow"); }
	//faunaweak=1: and they start with a few hit points (deaths, the essence of the pyre)
	public static boolean faunaWeak(){ return on("faunaweak"); }
	//fauna=frost|bones|isles (+ layer digit) or their kinds by name (wolf,shepherd,yog...) do the same for the
	//moor, the graveyard and the isles (WildTest). blindworm=1: the blind worm comes under any floor right
	//away, even a test one, and blindwormnoise=N makes a racket N cells off the hero every few turns to call it
	public static boolean blindWorm(){ return on("blindworm"); }
	public static int blindWormNoise(){ return number("blindwormnoise", 0); }
	//yogphase=N: yog's true form starts at phase N (with the hit points to match)
	public static int yogPhase(){ return number("yogphase", 1); }
	//awake=<biome>[layer]: a run starts on the dark continent already awake (the way home of the true
	//ending), with seven drops of essence: ash|flesh|city|frost|bones|isles, a 2 or 3 after it for the layers below
	public static String awake(){ return text("awake"); }
	//essence=N: a test run on the dark continent starts with N drops of sacred essence
	public static int essence(){ return number("essence", 0); }
	//awakekit=N: with awake or essence, every hero gets level 30, a greatsword and plate armor +N
	public static int awakeKit(){ return number("awakekit", 0); }
	//awakewalk=1: on the awake continent the hero (every bot in co-op) walks home on its own: up, through
	//the passes towards the ash plains and into the portal
	public static boolean awakeWalk(){ return on("awakewalk"); }
	//faunadown=1: and the big ones start knocked down for a long while (the frames of lying down, the weak spot)
	public static boolean faunaDown(){ return on("faunadown"); }
	//hunt=1: with colpreview, the hero goes through the hunt by itself (HuntTest): parts, taming, climbing, knocking down, the forge
	public static boolean hunt(){ return on("hunt"); }
	//huntparts=1: a part of each big beast beyond the ash plains in every bag, =2 laid round the first hero
	public static int huntParts(){ return number("huntparts", 0); }
	//tametest=1: with colpreview and fauna=..., the bots tame the beast there, watch it and hunt the other (BiomeHuntTest)
	public static boolean tameTest(){ return on("tametest"); }
	//tamed=1: with colpreview and fauna=..., the first beast there that can be tamed starts out as the party's
	public static boolean startTamed(){ return on("tamed"); }
	//duel=1: with colpreview and fauna=..., the goblin priest of the forest comes out on that plain too, by
	//the first colossus there and strong enough to stand up to it (priestbuff=1 for his brute); =2 the hero
	//only watches, hidden, and the turns go by on their own
	public static int duel(){ return number("duel", 0); }
	//duelboss=priest|tharog: who comes out for that duel (the goblin priest by default)
	public static String duelBoss(){ return text("duelboss"); }
	//ally=turtle|rex|bison...: on every floor the party gets to, that beast of the dark continent comes along tamed
	public static String ally(){ return text("ally"); }
	//partdrop=1: every big one that leaves a part always leaves it
	public static boolean partDrop(){ return on("partdrop"); }
	//surv=1: every hero gets a bag of survival things (kit, colossus meat and bones, torches, corrupt food,
	//3 essence) and the bot goes through them: fire, cooking, recipes, sacred torch, eating, poultice, bait
	public static boolean survTest(){ return on("surv"); }
	//survmeat=1: a colossus is dropped by the party and falls three turns later, to see what it leaves
	public static boolean survMeat(){ return on("survmeat"); }
	//tether=1: the second hero (or a lone one, then the way in counts as the party) walks off alone
	//until something comes for it, then goes back
	public static boolean tetherTest(){ return on("tether"); }
	//cathedral=2: a run starts on the streets of the devoured city by a fallen furnace cathedral, doors open; =1 walking into it
	public static int cathedral(){ return number("cathedral", 0); }
	//cathedralbot=1: the hero (every bot in co-op) goes in and breaks the heart, =2 in and out by the portal, =3 a walk round the inside
	public static int cathedralBot(){ return number("cathedralbot", 0); }
	//cathedralweak=1: the heart of the furnace cathedral starts with a tenth of its life
	public static boolean cathedralWeak(){ return on("cathedralweak"); }
	//event=steps|tide|storm|migration (or several, with commas): that world event starts a few turns into the continent
	//(event=natural: they come on their own clock); alone, the hero waits and dodges by itself
	public static String event(){ return text("event"); }

	// *** side quests ***
	//noquests=1: none of the new side quests shows up (the balance bot measures plain floors)
	public static boolean noQuests(){ return on("noquests"); }
	//hunt=1, caiman=1, worm=1, golem=1: that quest shows up on the first floor it can
	public static boolean forceQuest( String name ){ return on(name); }
	//questset=hunt:win,caiman:fail,seals:2...: a new run starts as if those quests were already played
	public static String questSet(){ return text("questset"); }
	//androidboot=N: the boot of the android starts at N %
	public static int androidBoot(){ return number("androidboot", 0); }
	//labtime=N: turns to seal the bulkheads of the flooded lab
	public static int labTime(){ return number("labtime", 160); }
	//tribe=1: the bot takes the sacred flame and is put next to the altar, =2 walks it down 21-24 itself,
	//=3 only measures the way of the flame on each floor and takes the stairs at once (=4 without the new braziers)
	public static int tribe(){ return number("tribe", 0); }
}
