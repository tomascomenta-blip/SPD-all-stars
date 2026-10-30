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

import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.coop.net.LanDiscovery;
import com.shatteredpixel.shatteredpixeldungeon.coop.net.Rooms;
import com.shatteredpixel.shatteredpixeldungeon.journal.Journal;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.noosa.Image;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.ExitButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.TitleBackground;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.IconTitle;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTextInput;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.utils.Callback;
import com.watabou.utils.GameSettings;
import com.watabou.utils.RectF;

import java.util.ArrayList;
import java.util.LinkedHashMap;

//co-op menu: host (wifi or internet) or join. 2-8 players
public class CoopScene extends PixelScene {

	private static final int WIDTH = 150;
	private static final int BTN_H = 18;
	private static final int GAP = 3;
	//room code or address, and up to MAX_HEROES players
	private static final int STATUS_H = 84 + 9*(Party.MAX_HEROES - 4);

	private enum Stage { MENU, HOST, JOIN_LAN, GUEST }
	private static Stage stage = Stage.MENU;

	private Group ui;
	private RenderedTextBlock status;
	private RedButton btnStart;
	private RedButton btnResume;
	private RedButton btnKick;
	//host: runs saved on this device, counted when the room is built
	private int savedRuns = 0;
	//guest: the password prompt is up
	private boolean askingPassword = false;
	private float left, top;
	//the centred column, the bottom and the width of the usable screen
	private float centerLeft, bottom, screenW;
	private float refresh = 0;

	//games found on the local network: address -> host name
	private static final LinkedHashMap<String, String> found = new LinkedHashMap<>();
	private int shownFound = -1;
	private static volatile String busy = null;

	@Override
	public void create() {
		super.create();

		Badges.loadGlobal();
		Journal.loadGlobal();
		uiCamera.visible = false;

		int w = Camera.main.width;
		int h = Camera.main.height;
		RectF insets = getCommonInsets();

		add(new TitleBackground(w, h));

		w -= insets.left + insets.right;
		h -= insets.top + insets.bottom;

		ExitButton btnExit = new ExitButton(){
			@Override
			protected void onClick() {
				onBackPressed();
			}
		};
		btnExit.setPos(insets.left + w - btnExit.width(), insets.top);
		add(btnExit);

		IconTitle title = new IconTitle(Icons.get(Icons.TALENT), Messages.get(this, "title"));
		title.setSize(200, 0);
		title.setPos(insets.left + (w - title.reqWidth()) / 2f, insets.top + (20 - title.height()) / 2f);
		align(title);
		add(title);

		centerLeft = left = insets.left + (w - WIDTH) / 2f;
		top = title.bottom() + 6;
		bottom = insets.top + h;
		screenW = w;

		CoopSession s = CoopSession.current;
		if (s == null){
			stage = Stage.MENU;
		} else if (stage == Stage.MENU){
			stage = s.isHost() ? Stage.HOST : Stage.GUEST;
		}

		rebuild();
		fadeIn();

		CoopAuto.onLobby(this);
	}

	static String myName(){
		return GameSettings.getString("coop_name", Messages.get(CoopScene.class, "default_name"), 16);
	}

	static HeroClass myClass(){
		String c = GameSettings.getString("coop_class", HeroClass.WARRIOR.name());
		try {
			//co-op: every class can be played, unlocked or not
			return HeroClass.valueOf(c);
		} catch (IllegalArgumentException e){
			//fall through
		}
		return HeroClass.WARRIOR;
	}

	private void rebuild(){
		if (ui != null){
			ui.destroy();
			remove(ui);
		}
		ui = new Group();
		add(ui);

		float y = top;
		left = centerLeft;
		btnStart = null;
		btnResume = null;
		btnKick = null;

		switch (stage){
			case MENU:
				y = addButton(Messages.get(this, "name", myName()), y, new Runnable() {
					@Override
					public void run() {
						askName();
					}
				});
				y = addButton(Messages.get(this, "hero", Messages.titleCase(myClass().title())), y, new Runnable() {
					@Override
					public void run() {
						askClass();
					}
				});
				y += GAP*2;
				y = addButton(Messages.get(this, "host_lan"), y, new Runnable() {
					@Override
					public void run() {
						hostLan();
					}
				});
				y = addButton(Messages.get(this, "host_online"), y, new Runnable() {
					@Override
					public void run() {
						hostOnline();
					}
				});
				y += GAP*2;
				y = addButton(Messages.get(this, "join_lan"), y, new Runnable() {
					@Override
					public void run() {
						joinLanList();
					}
				});
				y = addButton(Messages.get(this, "join_online"), y, new Runnable() {
					@Override
					public void run() {
						askCode();
					}
				});
				y += GAP*2;
				y = addButton(Messages.get(this, "saved", CoopSaves.all().size()), y, new Runnable() {
					@Override
					public void run() {
						addToFront(new WndCoopGames(WndCoopGames.Mode.VIEW, new WndCoopGames.Listener() {
							@Override
							public void picked( int slot ) {
								rebuild();
							}
						}));
					}
				});
				break;

			case HOST:
				int saved = savedRuns = CoopSaves.all().size();
				y = addStatus(y, 5);
				btnStart = button(Messages.get(this, "start"), new Runnable() {
					@Override
					public void run() {
						startNew();
					}
				});
				btnStart.setRect(left, y, WIDTH, BTN_H);
				ui.add(btnStart);
				y += BTN_H + GAP;
				btnResume = button(Messages.get(this, "resume_list", saved), new Runnable() {
					@Override
					public void run() {
						addToFront(new WndCoopGames(WndCoopGames.Mode.CONTINUE, new WndCoopGames.Listener() {
							@Override
							public void picked( int slot ) {
								if (slot >= 0) startGame(slot, true);
								else rebuild();
							}
						}));
					}
				});
				btnResume.setRect(left, y, WIDTH, BTN_H);
				ui.add(btnResume);
				y += BTN_H + GAP;
				//difficulty, worlds, challenges, seed, friendly fire, password
				y = addButton(Messages.get(this, "options"), y, new Runnable() {
					@Override
					public void run() {
						addToFront(new WndCoopOptions(null));
					}
				});
				btnKick = button(Messages.get(this, "kick"), new Runnable() {
					@Override
					public void run() {
						askKick();
					}
				});
				btnKick.setRect(left, y, WIDTH, BTN_H);
				ui.add(btnKick);
				y += BTN_H + GAP;
				y = addButton(Messages.get(this, "cancel"), y, new Runnable() {
					@Override
					public void run() {
						cancelSession();
					}
				});
				break;

			case JOIN_LAN:
				RenderedTextBlock info = renderTextBlock(Messages.get(this, "searching"), 7);
				info.maxWidth(WIDTH);
				info.setPos(left, y);
				ui.add(info);
				y = info.bottom() + GAP*2;
				synchronized (found){
					shownFound = found.size();
					for (final String address : found.keySet()){
						final String name = found.get(address);
						y = addButton(Messages.get(this, "found", name, address), y, new Runnable() {
							@Override
							public void run() {
								joinLan(address);
							}
						});
					}
				}
				y += GAP;
				y = addButton(Messages.get(this, "join_ip"), y, new Runnable() {
					@Override
					public void run() {
						askAddress();
					}
				});
				y = addButton(Messages.get(this, "cancel"), y, new Runnable() {
					@Override
					public void run() {
						LanDiscovery.stopListening();
						stage = Stage.MENU;
						rebuild();
					}
				});
				break;

			case GUEST:
				y = addStatus(y, 1);
				y += GAP;
				y = addButton(Messages.get(this, "cancel"), y, new Runnable() {
					@Override
					public void run() {
						cancelSession();
					}
				});
				break;
		}
		refreshStatus();
	}

	//the room's status with its buttons below it, or in a column to its right when they don't fit
	//under it (8 players on a phone held sideways)
	private float addStatus( float y, int buttons ){
		boolean side = y + STATUS_H + buttons*(BTN_H + GAP) + GAP > bottom && screenW >= 2*WIDTH + 4*GAP;
		if (side) left = centerLeft - WIDTH/2f - GAP;
		status = renderTextBlock(7);
		status.maxWidth(WIDTH);
		status.setPos(left, y);
		ui.add(status);
		if (side){
			left = centerLeft + WIDTH/2f + GAP;
			return y;
		}
		return y + STATUS_H;
	}

	private float addButton( String label, float y, final Runnable action ){
		RedButton b = button(label, action);
		b.setRect(left, y, WIDTH, BTN_H);
		ui.add(b);
		return y + BTN_H + GAP;
	}

	private RedButton button( String label, final Runnable action ){
		return new RedButton(label, 8){
			@Override
			protected void onClick() {
				action.run();
			}
		};
	}

	@Override
	public void update() {
		super.update();
		refresh -= Game.elapsed;
		if (refresh <= 0){
			refresh = 0.25f;
			CoopSession.lobbyTick();
			refreshStatus();
			CoopSession s = CoopSession.current;
			if (s != null && !s.isHost() && s.passwordWanted && !askingPassword){
				askingPassword = true;
				askPassword();
			}
			if (stage == Stage.JOIN_LAN){
				synchronized (found){
					if (found.size() != shownFound) rebuild();
				}
			}
		}
		CoopAuto.onLobbyUpdate(this);
	}

	private void refreshStatus(){
		CoopSession s = CoopSession.current;
		if (status != null){
			String text;
			if (busy != null){
				text = busy;
			} else if (s == null){
				text = "";
			} else if (s.problem != null){
				text = s.problem;
			} else if (s.isHost()){
				text = s.net == CoopSession.Net.ONLINE
						? Messages.get(this, "code", s.code)
						: Messages.get(this, "your_ip", addresses());
				text += "\n\n" + players(s);
				if (s.guestCount() == 0) text += "\n" + Messages.get(this, "waiting_partner");
			} else if (!s.connectedToHost()){
				text = Messages.get(this, "connecting");
			} else if (!s.hostPresent()){
				text = Messages.get(this, "connected");
			} else {
				String[] names = s.roomNames();
				text = Messages.get(this, "joined", names.length > 0 ? names[0] : "?") + "\n\n" + players(s);
			}
			status.text(text);
		}
		if (btnStart != null){
			btnStart.enable(s != null && s.guestCount() > 0);
		}
		if (btnKick != null){
			btnKick.enable(s != null && s.guestCount() > 0);
		}
		if (btnResume != null){
			btnResume.enable(s != null && s.guestCount() > 0 && savedRuns > 0);
		}
	}

	//the players in the room, the host first
	private String players( CoopSession s ){
		String[] names = s.roomNames();
		StringBuilder sb = new StringBuilder();
		int n = 0;
		for (int i = 0; i < names.length; i++){
			if (names[i] == null || names[i].isEmpty()) continue;
			n++;
			HeroClass c = s.roomClass(i);
			sb.append("\n").append(Messages.get(this, "seat", n, names[i],
					c == null ? "?" : Messages.titleCase(c.title())));
			if (i == 0) sb.append(" - ").append(Messages.get(this, "host_tag"));
		}
		return Messages.get(this, "players", n, Party.MAX_HEROES) + sb;
	}

	private static String addresses(){
		ArrayList<String> list = Rooms.localAddresses();
		if (list.isEmpty()) return "?";
		StringBuilder sb = new StringBuilder();
		for (String a : list){
			if (sb.length() > 0) sb.append(", ");
			sb.append(a);
		}
		return sb.toString();
	}

	// *** actions ***

	private void askName(){
		addToFront(new WndTextInput(Messages.get(this, "name_title"), null, myName(), 16, false,
				Messages.get(this, "ok"), Messages.get(this, "cancel")){
			@Override
			public void onSelect(boolean positive, String text) {
				if (positive && !text.trim().isEmpty()){
					GameSettings.put("coop_name", text.trim());
					rebuild();
				}
			}
		});
	}

	void askClass(){
		final ArrayList<HeroClass> classes = new ArrayList<>();
		for (HeroClass c : HeroClass.values()){
			classes.add(c);
		}
		//a grid with the hero's look: every class of the game, it has to fit on a phone screen
		Window w = new Window();
		final Window wnd = w;
		RenderedTextBlock t = PixelScene.renderTextBlock(Messages.get(this, "hero_title"), 9);
		t.hardlight(Window.TITLE_COLOR);
		int maxH = (int)PixelScene.uiCamera.height - 16;
		int cols = 2;
		int rowH = 22;
		int rows = (classes.size() + cols - 1) / cols;
		float top = t.height() + 6;
		while (top + rows*rowH > maxH && rowH > 18) rowH--;
		if (top + rows*rowH > maxH){
			cols = 3;
			rows = (classes.size() + cols - 1) / cols;
			while (top + rows*rowH > maxH && rowH > 17) rowH--;
		}
		int width = cols == 2 ? 150 : Math.min(186, (int)PixelScene.uiCamera.width - 8);
		int textSize = cols == 2 ? 8 : 6;
		t.setPos((width - t.width()) / 2f, 2);
		w.add(t);
		int colW = (width - 2*(cols-1)) / cols;
		for (int i = 0; i < classes.size(); i++){
			final HeroClass c = classes.get(i);
			RedButton b = new RedButton(Messages.titleCase(c.title()), textSize){
				@Override
				protected void onClick() {
					GameSettings.put("coop_class", c.name());
					wnd.hide();
					rebuild();
				}
			};
			b.icon(new Image(c.spritesheet(), 0, 90, 12, 15));
			if (c == myClass()) b.textColor(Window.TITLE_COLOR);
			b.setRect((i % cols) * (colW + 2), top + (i / cols) * rowH, colW, rowH - 2);
			w.add(b);
		}
		w.resize(width, (int)(top + rows * rowH));
		addToFront(w);
	}

	void hostLan(){
		CoopSession.hostLan(myName(), myClass());
		stage = Stage.HOST;
		rebuild();
	}

	void hostOnline(){
		busy = Messages.get(this, "looking_server");
		stage = Stage.HOST;
		rebuild();
		new Thread(new Runnable() {
			@Override
			public void run() {
				final int broker = Rooms.probeBroker();
				Game.runOnRenderThread(new Callback() {
					@Override
					public void call() {
						busy = null;
						if (broker < 0){
							stage = Stage.MENU;
							rebuild();
							addToFront(new WndOptions(Messages.get(CoopScene.class, "title"),
									Messages.get(CoopScene.class, "no_server"), Messages.get(CoopScene.class, "ok")));
						} else {
							CoopSession.hostOnline(broker, myName(), myClass());
							rebuild();
						}
					}
				});
			}
		}).start();
	}

	private void joinLanList(){
		synchronized (found){
			found.clear();
		}
		LanDiscovery.startListening(new LanDiscovery.Listener() {
			@Override
			public void found(String address, int port, String hostName) {
				synchronized (found){
					//a beacon from 127.0.0.1 and from a real address can be the same game, keep both
					found.put(address, hostName);
				}
			}
		});
		stage = Stage.JOIN_LAN;
		rebuild();
	}

	void joinLan( String address ){
		LanDiscovery.stopListening();
		CoopSession.joinLan(address, myName(), myClass());
		stage = Stage.GUEST;
		rebuild();
	}

	private void askAddress(){
		addToFront(new WndTextInput(Messages.get(this, "ip_title"), null, "", 40, false,
				Messages.get(this, "ok"), Messages.get(this, "cancel")){
			@Override
			public void onSelect(boolean positive, String text) {
				if (positive && !text.trim().isEmpty()){
					joinLan(text.trim());
				}
			}
		});
	}

	private void askCode(){
		addToFront(new WndTextInput(Messages.get(this, "code_title"), Messages.get(this, "code_body"), "",
				Rooms.CODE_LENGTH + 2, false, Messages.get(this, "ok"), Messages.get(this, "cancel")){
			@Override
			public void onSelect(boolean positive, String text) {
				if (positive){
					if (Rooms.valid(text)){
						joinOnline(text);
					} else {
						addToFront(new WndOptions(Messages.get(CoopScene.class, "title"),
								Messages.get(CoopScene.class, "bad_code"), Messages.get(CoopScene.class, "ok")));
					}
				}
			}
		});
	}

	void joinOnline( String code ){
		CoopSession.joinOnline(code, myName(), myClass());
		stage = Stage.GUEST;
		rebuild();
	}

	//a new run goes into the first free slot; with the 8 taken, the host picks one to replace
	private void startNew(){
		int slot = CoopSaves.firstEmpty();
		if (slot >= 0){
			startGame(slot, false);
			return;
		}
		addToFront(new WndCoopGames(WndCoopGames.Mode.REPLACE, new WndCoopGames.Listener() {
			@Override
			public void picked( int slot ) {
				if (slot >= 0) startGame(slot, false);
			}
		}));
	}

	void startGame( int slot, boolean resume ){
		CoopSession s = CoopSession.current;
		if (s == null || s.guestCount() == 0) return;
		s.hostStart(slot, resume);
	}

	//test runs (CoopAuto): the last saved run, or a new one in a free slot (or over the first one)
	void startGame( boolean resume ){
		ArrayList<CoopSaves.Info> saves = CoopSaves.all();
		if (resume && !saves.isEmpty()){
			startGame(saves.get(0).slot, true);
			return;
		}
		int slot = CoopSaves.firstEmpty();
		startGame(slot < 0 ? CoopSaves.FIRST_SLOT : slot, false);
	}

	private void askKick(){
		final CoopSession s = CoopSession.current;
		if (s == null) return;
		String[] names = s.roomNames();
		final ArrayList<Integer> seats = new ArrayList<>();
		ArrayList<String> labels = new ArrayList<>();
		for (int i = 1; i < names.length; i++){
			if (names[i] == null || names[i].isEmpty()) continue;
			seats.add(i);
			labels.add(names[i]);
		}
		if (seats.isEmpty()) return;
		labels.add(Messages.get(this, "cancel"));
		addToFront(new WndOptions(Messages.get(this, "kick_title"), Messages.get(this, "kick_body"),
				labels.toArray(new String[0])){
			@Override
			protected void onSelect( int index ) {
				if (index >= 0 && index < seats.size()) s.kick(seats.get(index));
			}
		});
	}

	//guest: the room has a password (or the one typed was wrong)
	private void askPassword(){
		addToFront(new WndTextInput(Messages.get(this, "password_title"), Messages.get(this, "password_body"),
				"", 20, false, Messages.get(this, "ok"), Messages.get(this, "cancel")){
			@Override
			public void onSelect( boolean positive, String text ) {
				askingPassword = false;
				CoopSession s = CoopSession.current;
				if (s == null) return;
				if (positive) s.retryWithPassword(text);
				else cancelSession();
			}
		});
	}

	private void cancelSession(){
		CoopSession.end();
		LanDiscovery.stopListening();
		stage = Stage.MENU;
		rebuild();
	}

	@Override
	protected void onBackPressed() {
		CoopSession.end();
		LanDiscovery.stopListening();
		stage = Stage.MENU;
		ShatteredPixelDungeon.switchNoFade(TitleScene.class);
	}
}
