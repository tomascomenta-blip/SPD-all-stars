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

package com.shatteredpixel.shatteredpixeldungeon.coop.net;

//A raw connection to the other player. Frames may be lost when the connection drops,
//the Channel on top of it takes care of ordering and resending.
public interface Link {

	interface Listener {
		void onFrame( String frame );
		//connected = whether frames can currently be sent, info = text for the interface
		void onLinkState( boolean connected, String info );
	}

	void setListener( Listener listener );

	void start();

	//returns false if the frame could not be handed to the connection
	boolean send( String frame );

	boolean connected();

	void close();

}
