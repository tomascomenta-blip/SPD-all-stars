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

package com.shatteredpixel.shatteredpixeldungeon.coop;

import com.watabou.noosa.Camera;
import com.shatteredpixel.shatteredpixeldungeon.DevFlags;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTextInput;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.ui.Component;

//little panel up top: partners' hp + who we're waiting for (usually the guy making coffee)
public class CoopHud extends Component {

	static void leaveToMenu(){
		afk = false;
		CoopSession.end();
		ShatteredPixelDungeon.switchScene(TitleScene.class);
	}

	private ColorBlock bg;
	private RenderedTextBlock text;
	private RedButton chat;
	private RedButton afkBtn;
	private RedButton markBtn;

	//AFK: while it is on, the local hero rests every turn and nothing interrupts it (windows, cell
	//selections, monsters). Interface state of this device; the rests are sent as ordinary orders.
	//If the game goes to the background the host plays the hero meanwhile (CoopSession.appPaused).
	//testing: -Dspd.test.afk=1 starts with AFK on
	public static boolean afk = DevFlags.afk();
	private float timer = 0;
	private String last = null;

	@Override
	protected void createChildren() {
		bg = new ColorBlock(1, 1, 0x99000000);
		add(bg);
		text = PixelScene.renderTextBlock(6);
		add(text);
		chat = new RedButton(Messages.get(CoopHud.class, "chat"), 6){
			@Override
			protected void onClick() {
				GameScene.showUntracked(new WndTextInput(Messages.get(CoopHud.class, "chat_title"), null, "", 60, false,
						Messages.get(CoopScene.class, "ok"), Messages.get(CoopScene.class, "cancel")){
					@Override
					public void onSelect(boolean positive, String message) {
						CoopSession s = CoopSession.current;
						if (positive && s != null && !message.trim().isEmpty()){
							s.say(message.trim());
						}
					}
				});
			}
		};
		add(chat);
		afkBtn = new RedButton(Messages.get(CoopHud.class, "afk"), 6){
			@Override
			protected void onClick() {
				afk = !afk;
				textColor(afk ? 0x66FF66 : 0xFFFFFF);
				CoopSession s = CoopSession.current;
				if (s != null){
					s.say(Messages.get(CoopHud.class, afk ? "afk_on" : "afk_off"));
				}
			}
		};
		afkBtn.textColor(afk ? 0x66FF66 : 0xFFFFFF);
		add(afkBtn);
		markBtn = new RedButton(Messages.get(CoopHud.class, "mark"), 6){
			@Override
			protected void onClick() {
				CoopPings.askKind(-1);
			}
		};
		add(markBtn);
	}

	@Override
	protected void layout() {
		text.setPos(x + 3, y + 2);
		bg.x = x;
		bg.y = y;
		bg.size(Math.max(text.width() + 6, 30), text.height() + 4);
		width = bg.width();
		chat.setRect(x + width - 30, bg.y + bg.height() + 1, 30, 14);
		afkBtn.setRect(chat.left() - 25, chat.top(), 24, 14);
		markBtn.setRect(afkBtn.left() - 33, chat.top(), 32, 14);
		height = bg.height() + 15;
	}

	//the partner the camera follows while our hero is down
	private Hero following = null;

	@Override
	public void update() {
		super.update();
		timer -= Game.elapsed;
		if (timer > 0) return;
		timer = 0.2f;

		//AFK: rest whenever it is our turn, whatever happens (even with a window open)
		Hero self = Dungeon.local();
		if (afk && self != null && self.isAlive() && !self.coopDown && CoopSession.localInputAllowed()){
			if (GameScene.isSelectingCell()) GameScene.cancelCellSelector();
			CoopOrders.named("rest");
		}

		final CoopSession session = CoopSession.current;
		//guests: the host left the run
		if (session != null && session.hostLeft){
			session.hostLeft = false;
			String[] names = session.roomNames();
			String who = names.length > 0 ? names[0] : "?";
			GameScene.showUntracked(new WndOptions(Messages.get(CoopScene.class, "title"),
					Messages.get(this, "peer_left_guest", who),
					Messages.get(this, "to_menu")){
				@Override
				protected void onSelect(int index) {
					leaveToMenu();
				}
			});
		}
		//players who left or came back
		for (String n : CoopSession.takeNotices()){
			GLog.w(n);
		}

		//when our hero has fallen, the camera follows a partner that still stands
		Hero me = Dungeon.local();
		if (me != null && me.coopDown){
			if (following == null || following.coopDown || !following.isAlive()){
				following = null;
				for (Hero h : Party.living()){
					if (h != me && h.sprite != null){
						Camera.main.panFollow(h.sprite, 5f);
						following = h;
						break;
					}
				}
			}
		} else {
			following = null;
		}

		//one line for each other hero
		StringBuilder sb = new StringBuilder();
		for (Hero h : Party.heroes()){
			if (h == Dungeon.local()) continue;
			if (sb.length() > 0) sb.append("\n");
			sb.append(Party.name(h)).append(": ");
			if (h.coopDown || !h.isAlive()) sb.append("-");
			else sb.append(h.HP).append("/").append(h.HT);
			if (CoopSession.heroAway(Party.indexOf(h))) sb.append(" ").append(Messages.get(this, "away"));
		}
		String extra = null;
		if (CoopSession.peerSilent() && Party.anyAlive()){
			extra = Messages.get(this, "disconnected");
		} else if (me != null && me.coopDown){
			extra = Messages.get(this, "down");
		} else {
			String waiting = CoopSession.waitingForName();
			if (waiting != null) extra = Messages.get(this, "waiting", waiting);
		}
		if (extra != null) sb.append("\n").append(extra);

		String s = sb.toString();
		if (!s.equals(last)){
			last = s;
			text.text(s);
			layout();
			//anchored to the right edge of the screen
			x = PixelScene.uiCamera.width - width - 1;
			layout();
		}
	}
}
