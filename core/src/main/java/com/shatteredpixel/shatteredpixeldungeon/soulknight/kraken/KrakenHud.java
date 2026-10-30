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

package com.shatteredpixel.shatteredpixeldungeon.soulknight.kraken;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.utils.Callback;
import com.watabou.utils.GameMath;

import java.util.ArrayList;
import java.util.Random;

//kraken HUD: hp bar (800 in 4 chunks), ship bar, distance to the whirlpool, last assault countdown,
//big center banners, and the storm overlay
public class KrakenHud extends Group {

	private static KrakenHud instance;
	private static Storm storm;
	private static final ArrayList<String> queued = new ArrayList<>();

	private final Image krakenFrame, shipFrame, route, routeShip;
	private final ColorBlock krakenFill, krakenBack, tentFill, shipFill, shipBack;
	private final RenderedTextBlock shipText, routeText, countText;
	private RenderedTextBlock banner;
	private ColorBlock bannerBack;
	private float bannerT = 99;
	private float s = 1f;
	private float x0, y0;
	private float t = 0;

	public KrakenHud( float top ){
		instance = this;
		float uiw = PixelScene.uiCamera.width;
		s = Math.min(1f, (uiw - 8) / 208f);
		x0 = (uiw - 208 * s) / 2f;
		y0 = top;

		krakenBack = new ColorBlock(152 * s, 10 * s, 0xFF1A0610);
		krakenBack.x = x0 + 46 * s;
		krakenBack.y = y0 + 16 * s;
		add(krakenBack);
		krakenFill = new ColorBlock(152 * s, 10 * s, 0xFFC0305A);
		krakenFill.x = krakenBack.x;
		krakenFill.y = krakenBack.y;
		add(krakenFill);
		tentFill = new ColorBlock(152 * s, 2 * s, 0xFF7FE0FF);
		tentFill.x = krakenBack.x;
		tentFill.y = krakenBack.y + 8 * s;
		add(tentFill);
		krakenFrame = new Image(KrakenAssets.BAR_KRAKEN);
		krakenFrame.scale.set(s);
		krakenFrame.x = x0;
		krakenFrame.y = y0;
		add(krakenFrame);

		float sy = y0 + 40 * s;
		shipBack = new ColorBlock(166 * s, 8 * s, 0xFF201008);
		shipBack.x = x0 + 32 * s;
		shipBack.y = sy + 9 * s;
		add(shipBack);
		shipFill = new ColorBlock(166 * s, 8 * s, 0xFF6FBF5A);
		shipFill.x = shipBack.x;
		shipFill.y = shipBack.y;
		add(shipFill);
		shipFrame = new Image(KrakenAssets.BAR_SHIP);
		shipFrame.scale.set(s);
		shipFrame.x = x0;
		shipFrame.y = sy;
		add(shipFrame);

		float ry = sy + 27 * s;
		route = new Image(KrakenAssets.BAR_ROUTE);
		route.scale.set(s);
		route.x = x0;
		route.y = ry;
		add(route);
		routeShip = new Image(KrakenAssets.ROUTE_SHIP);
		routeShip.scale.set(s);
		routeShip.y = ry + 3 * s;
		add(routeShip);

		shipText = PixelScene.renderTextBlock(6);
		add(shipText);
		routeText = PixelScene.renderTextBlock(6);
		add(routeText);
		countText = PixelScene.renderTextBlock(9);
		countText.hardlight(0xFF4040);
		add(countText);

		for (String q : queued) show(q);
		queued.clear();
	}

	@Override
	public void destroy() {
		super.destroy();
		if (instance == this) instance = null;
	}

	public static Storm storm(){
		storm = new Storm();
		return storm;
	}

	//center banner, every device
	public static void banner( final String text ){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				if (instance != null) instance.show(text);
				else queued.add(text);
			}
		});
	}

	private void show( String text ){
		if (banner != null){
			banner.killAndErase();
			bannerBack.killAndErase();
		}
		float uiw = PixelScene.uiCamera.width, uih = PixelScene.uiCamera.height;
		banner = PixelScene.renderTextBlock(text, 9);
		banner.maxWidth((int) Math.min(uiw - 20, 220));
		banner.hardlight(0xFFE08A);
		bannerBack = new ColorBlock(banner.width() + 16, banner.height() + 10, 0xCC060A12);
		bannerBack.x = (uiw - bannerBack.width()) / 2f;
		bannerBack.y = uih * 0.30f;
		banner.setPos(bannerBack.x + 8, bannerBack.y + 5);
		PixelScene.align(banner);
		add(bannerBack);
		add(banner);
		bannerT = 0;
	}

	public static void darken(){
		if (storm != null) storm.sinking = true;
	}

	public static void calm(){
		if (storm != null) storm.calming = true;
	}

	@Override
	public void update() {
		super.update();
		float e = Game.elapsed;
		t += e;
		KrakenLevel L = Dungeon.level instanceof KrakenLevel ? (KrakenLevel) Dungeon.level : null;
		if (L == null) return;
		//hide bars once it's over, ya está
		boolean show = L.stage != KrakenLevel.WON && L.stage != KrakenLevel.SUNK;
		krakenFrame.visible = krakenFill.visible = krakenBack.visible = show;
		shipFrame.visible = shipFill.visible = shipBack.visible = shipText.visible = show;
		route.visible = routeShip.visible = show;
		tentFill.visible = false;

		Kraken k = Kraken.find();
		int hp = k == null ? (L.stage == KrakenLevel.WON ? 0 : Kraken.MAX_HP) : k.HP;
		krakenFill.size(152 * s * hp / (float) Kraken.MAX_HP, 10 * s);
		GiantTentacle gt = k == null ? null : k.activeTentacle();
		tentFill.visible = gt != null;
		if (gt != null) tentFill.size(152 * s * gt.HP / (float) gt.HT, 2 * s);
		boolean vuln = k != null && k.vulnerable();
		krakenFill.color(vuln && ((int)(t * 4)) % 2 == 0 ? 0xFFFFD040 : 0xFFC0305A);

		float sp = L.shipHP / (float) Math.max(1, L.shipHT);
		shipFill.size(166 * s * sp, 8 * s);
		shipFill.color(sp > 0.5f ? 0xFF6FBF5A : sp > 0.25f ? 0xFFE0B040 : (((int)(t * 4)) % 2 == 0 ? 0xFFFF3030 : 0xFF901010));
		shipText.text(Messages.get(KrakenHud.class, "ship", L.shipHP, L.shipHT, L.holes.size()));
		shipText.setPos(x0 + 34 * s, shipFrame.y + 26 * s - 1);
		PixelScene.align(shipText);

		float d = 1f;
		if (L.stage == KrakenLevel.SAILING) d = L.distanceMax <= 0 ? 0 : L.distance / (float) L.distanceMax;
		else if (L.stage == KrakenLevel.ARRIVED || L.stage == KrakenLevel.WON) d = 0;
		float rx0 = x0 + 4 * s, rx1 = x0 + 178 * s;
		routeShip.x = rx1 - (rx1 - rx0) * d;
		routeShip.y = route.y + 3 * s + (float)Math.sin(t * 3) * s;
		String r;
		switch (L.stage){
			case KrakenLevel.TENTACLE: r = Messages.get(KrakenHud.class, "held"); break;
			case KrakenLevel.SAILING: r = Messages.get(KrakenHud.class, "sailing", L.distance) + (Kraken.helmManned() ? " " + Messages.get(KrakenHud.class, "helm") : ""); break;
			case KrakenLevel.ARRIVED: r = Messages.get(KrakenHud.class, "arrived"); break;
			case KrakenLevel.PUSHED: r = Messages.get(KrakenHud.class, "pushed"); break;
			case KrakenLevel.WON: r = Messages.get(KrakenHud.class, "won"); break;
			default: r = "";
		}
		routeText.text(r);
		routeText.setPos(x0 + 4 * s, route.y + 16 * s);
		PixelScene.align(routeText);

		if (L.countdown > 0 && L.stage == KrakenLevel.TENTACLE){
			countText.visible = true;
			countText.text(Messages.get(KrakenHud.class, "countdown", L.countdown));
			countText.setPos((PixelScene.uiCamera.width - countText.width()) / 2f, routeText.top() + routeText.height() + 3);
			PixelScene.align(countText);
			countText.alpha(L.countdown <= 10 ? 0.5f + 0.5f * (float)Math.abs(Math.sin(t * 6)) : 1f);
		} else {
			countText.visible = false;
		}

		if (banner != null){
			bannerT += e;
			float a = bannerT < 0.3f ? bannerT / 0.3f : GameMath.gate(0, (4.5f - bannerT) / 0.8f, 1);
			banner.alpha(a);
			bannerBack.alpha(a * 0.8f);
			if (bannerT > 4.5f){
				banner.killAndErase();
				bannerBack.killAndErase();
				banner = null;
				bannerBack = null;
			}
		}
	}

	//rain + dark tint, then calm (or darker if you sank)
	public static class Storm extends Group {

		private final ColorBlock tint;
		private final Group rain = new Group();
		private final Random rnd = new Random();
		boolean calming = false, sinking = false;
		private float calmT = 0, sinkT = 0;

		public Storm(){
			float w = PixelScene.uiCamera.width, h = PixelScene.uiCamera.height;
			tint = new ColorBlock(w, h, 0xFF0A1830);
			tint.alpha(0.22f);
			add(tint);
			add(rain);
			for (int i = 0; i < 140; i++){
				ColorBlock d = new ColorBlock(1, 5 + rnd.nextInt(5), 0xFFBFD8F0);
				d.alpha(0.35f + rnd.nextFloat() * 0.3f);
				d.x = rnd.nextFloat() * w;
				d.y = rnd.nextFloat() * h;
				d.angle = 18;
				rain.add(d);
			}
			KrakenLevel L = Dungeon.level instanceof KrakenLevel ? (KrakenLevel) Dungeon.level : null;
			if (L != null && L.stage == KrakenLevel.WON){
				calming = true;
				calmT = 10;
			}
		}

		@Override
		public void update() {
			super.update();
			float e = Game.elapsed;
			float w = PixelScene.uiCamera.width, h = PixelScene.uiCamera.height;
			KrakenLevel L = Dungeon.level instanceof KrakenLevel ? (KrakenLevel) Dungeon.level : null;
			int a = L == null ? 1 : L.assault;
			float intensity = 0.7f + 0.1f * a;
			if (calming){
				calmT += e;
				intensity *= GameMath.gate(0, 1 - calmT / 4f, 1);
				//sun :)
				tint.color(0xFFFFD890);
				tint.alpha(0.12f * GameMath.gate(0, calmT / 4f, 1) * GameMath.gate(0, (12 - calmT) / 4f, 1));
			} else if (sinking){
				sinkT += e;
				tint.color(0xFF000000);
				tint.alpha(GameMath.gate(0.22f, sinkT / 3f, 0.9f));
			} else {
				tint.alpha(0.16f + 0.04f * a);
			}
			for (Gizmo g : rain.membersList()){
				if (!(g instanceof ColorBlock)) continue;
				ColorBlock d = (ColorBlock) g;
				d.visible = rnd.nextFloat() < intensity + 0.2f && intensity > 0.02f;
				d.y += (260 + 40 * a) * e;
				d.x -= (80 + 15 * a) * e;
				if (d.y > h){
					d.y -= h + 10;
					d.x = rnd.nextFloat() * (w + 60);
				}
				if (d.x < -10) d.x += w + 20;
			}
		}
	}
}
