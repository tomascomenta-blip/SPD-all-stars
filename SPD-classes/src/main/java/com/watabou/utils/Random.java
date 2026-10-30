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

package com.watabou.utils;

import com.watabou.noosa.Game;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class Random {

	//we store a stack of random number generators, which may be seeded deliberately or randomly.
	//top of the stack is what is currently being used to generate new numbers.
	//the base generator is always created with no seed, and cannot be popped.
	private static ArrayDeque<java.util.Random> generators;
	static {
		resetGenerators();
	}

	public static synchronized void resetGenerators(){
		generators = new ArrayDeque<>();
		generators.push(new java.util.Random());
	}

	// *** Determinism support for cooperative play ***
	//When a cosmetic thread is set (the render thread during a cooperative run), random numbers requested
	//from that thread go to a separate generator, unless they are inside a gameplay section. This keeps
	//visual randomness (particles etc.) from consuming numbers of the shared gameplay sequence.
	private static volatile Thread cosmeticThread = null;
	private static int gameplayDepth = 0;
	private static final java.util.Random cosmetic = new java.util.Random();

	public static void setCosmeticThread( Thread thread ){
		cosmeticThread = thread;
		gameplayDepth = 0;
	}

	public static Thread cosmeticThread(){
		return cosmeticThread;
	}

	public static void beginGameplay(){
		if (cosmeticThread != null && Thread.currentThread() == cosmeticThread) gameplayDepth++;
	}

	public static void endGameplay(){
		if (cosmeticThread != null && Thread.currentThread() == cosmeticThread && gameplayDepth > 0) gameplayDepth--;
	}

	//runs a callback that continues game logic, see beginGameplay()
	public static void gameplay( Callback callback ){
		beginGameplay();
		try {
			callback.call();
		} finally {
			endGameplay();
		}
	}

	public static boolean inGameplay(){
		return cosmeticThread == null || Thread.currentThread() != cosmeticThread || gameplayDepth > 0;
	}

	//seeded generators pushed by visual code (e.g. tile variance) live in their own stack
	private static final ArrayDeque<java.util.Random> visualStack = new ArrayDeque<>();

	//visual sections force the cosmetic generator on any thread: interface code that runs during
	//game logic (e.g. indicators refreshed on the actor thread) must not consume shared numbers
	private static final ThreadLocal<int[]> visualDepth = new ThreadLocal<int[]>(){
		@Override
		protected int[] initialValue() {
			return new int[1];
		}
	};

	public static void beginVisual(){
		if (cosmeticThread != null) visualDepth.get()[0]++;
	}

	public static void endVisual(){
		if (cosmeticThread != null){
			int[] d = visualDepth.get();
			if (d[0] > 0) d[0]--;
		}
	}

	private static boolean visual(){
		if (cosmeticThread == null) return false;
		if (Thread.currentThread() == cosmeticThread && gameplayDepth == 0) return true;
		return visualDepth.get()[0] > 0;
	}

	private static java.util.Random top(){
		if (visual()) {
			if (visualHook != null) visualHook.onRenderThreadDraw();
			return visualStack.isEmpty() ? cosmetic : visualStack.peekFirst();
		}
		if (debugHook != null) debugHook.onRenderThreadDraw();
		return generators.peekFirst();
	}

	//testing aid: called whenever the render thread takes a number from the shared sequence
	public interface DebugHook {
		void onRenderThreadDraw();
	}
	public static volatile DebugHook debugHook = null;
	//testing aid: called when interface code takes a number (from the visual generator)
	public static volatile DebugHook visualHook = null;

	private static java.util.Random base(){
		if (visual()) return cosmetic;
		if (debugHook != null) debugHook.onRenderThreadDraw();
		return generators.peekLast();
	}

	//randomness for purely visual effects (particles, shakes...). It never touches the generator stack,
	//so effects can't change seeded results or the sequence shared by cooperative players.
	public static class Visual {
		private static final java.util.Random rng = new java.util.Random();
		public static synchronized float Float(){
			return rng.nextFloat();
		}
		public static float Float( float max ){
			return Float() * max;
		}
		public static float Float( float min, float max ){
			return min + Float(max - min);
		}
		public static synchronized int Int( int max ){
			return max <= 0 ? 0 : rng.nextInt(max);
		}
		public static int Int( int min, int max ){
			return min + Int(max - min);
		}
		public static int IntRange( int min, int max ){
			return min + Int(max - min + 1);
		}
	}

	//a generator whose state can be read and written, so it can be saved and sent to other players
	public static class StateRandom extends java.util.Random {
		private long state;
		public StateRandom( long state ){
			super(0);
			this.state = state & ((1L << 48) - 1);
		}
		@Override
		protected synchronized int next( int bits ){
			state = (state * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
			return (int)(state >>> (48 - bits));
		}
		public synchronized long state(){
			return state;
		}
	}

	//replaces the base generator with one that has a known state
	public static synchronized void setBaseState( long state ){
		generators.removeLast();
		generators.addLast( new StateRandom( state ) );
	}

	//-1 if the base generator is not a stateful one
	public static synchronized long baseState(){
		java.util.Random b = generators.peekLast();
		return b instanceof StateRandom ? ((StateRandom) b).state() : -1;
	}

	public static synchronized int stackSize(){
		return generators.size();
	}

	public static synchronized void pushGenerator(){
		if (visual())   visualStack.push( new java.util.Random() );
		else            generators.push( new java.util.Random() );
	}

	public static synchronized void pushGenerator( long seed ){
		if (visual())   visualStack.push( new java.util.Random( scrambleSeed(seed) ) );
		else            generators.push( new java.util.Random( scrambleSeed(seed) ) );
	}

	//scrambles a given seed, this helps eliminate patterns between the outputs of similar seeds
	//Algorithm used is MX3 by Jon Maiga (jonkagstrom.com), CC0 license.
	private static synchronized long scrambleSeed( long seed ){
		seed ^= seed >>> 32;
		seed *= 0xbea225f9eb34556dL;
		seed ^= seed >>> 29;
		seed *= 0xbea225f9eb34556dL;
		seed ^= seed >>> 32;
		seed *= 0xbea225f9eb34556dL;
		seed ^= seed >>> 29;
		return seed;
	}

	public static synchronized void popGenerator(){
		if (visual()){
			if (!visualStack.isEmpty()) visualStack.pop();
		} else if (generators.size() == 1){
			Game.reportException( new RuntimeException("tried to pop the last random number generator!"));
		} else {
			generators.pop();
		}
	}

	//returns a uniformly distributed float in the range [0, 1)
	public static synchronized float Float() {
		return Float(true);
	}

	public static synchronized float Float( boolean useGeneratorStack ) {
		if (useGeneratorStack)  return top().nextFloat();
		else                    return base().nextFloat();
	}

	//returns a uniformly distributed float in the range [0, max)
	public static float Float( float max ) {
		return Float() * max;
	}

	//returns a uniformly distributed float in the range [min, max)
	public static float Float( float min, float max ) {
		return min + Float(max - min);
	}
	
	//returns a triangularly distributed float in the range [min, max)
	public static float NormalFloat( float min, float max ) {
		return min + ((Float(max - min) + Float(max - min))/2f);
	}

	//returns a uniformly distributed int in the range [-2^31, 2^31)
	public static synchronized int Int() {
		return Int(true);
	}

	//returns a uniformly distributed int in the range [-2^31, 2^31)
	//can either use the current generator in the stack, or force the first generator (pure random)
	public static synchronized int Int( boolean useGeneratorStack ) {
		if (useGeneratorStack)  return top().nextInt();
		else                    return base().nextInt();
	}

	//returns a uniformly distributed int in the range [0, max)
	public static synchronized int Int( int max ) {
		return Int(max, true);
	}

	//returns a uniformly distributed int in the range [0, max)
	//can either use the current generator in the stack, or force the first generator (pure random)
	public static synchronized int Int( int max, boolean useGeneratorStack ) {
		if (max <= 0)                   return 0;
		else if (useGeneratorStack)     return top().nextInt(max);
		else                            return base().nextInt(max);
	}

	//returns a uniformly distributed int in the range [min, max)
	public static int Int( int min, int max ) {
		return min + Int(max - min);
	}

	//returns a uniformly distributed int in the range [min, max]
	public static int IntRange( int min, int max ) {
		return min + Int(max - min + 1);
	}

	//returns a triangularly distributed int in the range [min, max]
	//this makes results more likely as they get closer to the middle of the range
	public static int NormalIntRange( int min, int max ) {
		return min + (int)((Float() + Float()) * (max - min + 1) / 2f);
	}

	//returns an inverse triangularly distributed int in the range [min, max]
	//this makes results more likely as they get further from the middle of the range
	public static int InvNormalIntRange( int min, int max ){
		float roll1 = Float(), roll2 = Float();
		if (Math.abs(roll1-0.5f) >= Math.abs(roll2-0.5f)){
			return min + (int)(roll1*(max - min + 1));
		} else {
			return min + (int)(roll2*(max - min + 1));
		}
	}

	//returns a uniformly distributed long in the range [-2^63, 2^63)
	public static synchronized long Long() {
		return Long(true);
	}

	//returns a uniformly distributed long in the range [-2^63, 2^63)
	//can either use the current generator in the stack, or force the first generator (pure random)
	public static synchronized long Long( boolean useGeneratorStack ) {
		if (useGeneratorStack)  return top().nextLong();
		else                    return base().nextLong();
	}

	//returns a mostly uniformly distributed long in the range [0, max)
	public static long Long( long max ) {
		long result = Long();
		if (result < 0) result += Long.MAX_VALUE;
		//modulo isn't perfect, but as long as max is reasonably below 2^63 it's close enough
		return result % max;
	}

	//returns an index from chances, the probability of each index is the weight values in changes
	//negative values are treated as 0
	public static int chances( float[] chances ) {
		
		int length = chances.length;
		
		float sum = 0;
		for (int i=0; i < length; i++) {
			sum += Math.max(0, chances[i]);
		}

		if (sum <= 0){
			return -1;
		}
		
		float value = Float( sum );
		sum = 0;
		for (int i=0; i < length; i++) {
			sum += Math.max(0, chances[i]);
			if (value < sum) {
				return i;
			}
		}
		
		return -1;
	}
	
	@SuppressWarnings("unchecked")
	//returns a key element from chances, the probability of each key is the weight value it maps to
	public static <K> K chances( HashMap<K,Float> chances ) {
		
		int size = chances.size();

		Object[] values = chances.keySet().toArray();
		float[] probs = new float[size];
		float sum = 0;
		for (int i=0; i < size; i++) {
			probs[i] = chances.get( values[i] );
			sum += probs[i];
		}
		
		if (sum <= 0) {
			return null;
		}
		
		float value = Float( sum );
		
		sum = probs[0];
		for (int i=0; i < size; i++) {
			if (value < sum) {
				return (K)values[i];
			}
			sum += probs[i + 1];
		}
		
		return null;
	}
	
	public static int index( Collection<?> collection ) {
		return Int(collection.size());
	}

	@SafeVarargs
	public static<T> T oneOf(T... array ) {
		return array[Int(array.length)];
	}
	
	public static<T> T element( T[] array ) {
		return element( array, array.length );
	}
	
	public static<T> T element( T[] array, int max ) {
		return array[Int(max)];
	}
	
	@SuppressWarnings("unchecked")
	public static<T> T element( Collection<? extends T> collection ) {
		int size = collection.size();
		return size > 0 ?
			(T)collection.toArray()[Int( size )] :
			null;
	}

	public synchronized static<T> void shuffle( List<?extends T> list){
		Collections.shuffle(list, top());
	}

	public static void shuffle( int[] array ) {
		for (int i=0; i < array.length - 1; i++) {
			int j = Int( i, array.length );
			if (j != i) {
				int t = array[i];
				array[i] = array[j];
				array[j] = t;
			}
		}
	}

	public static<T> void shuffle( T[] array ) {
		for (int i=0; i < array.length - 1; i++) {
			int j = Int( i, array.length );
			if (j != i) {
				T t = array[i];
				array[i] = array[j];
				array[j] = t;
			}
		}
	}
	
	public static<U,V> void shuffle( U[] u, V[]v ) {
		for (int i=0; i < u.length - 1; i++) {
			int j = Int( i, u.length );
			if (j != i) {
				U ut = u[i];
				u[i] = u[j];
				u[j] = ut;
				
				V vt = v[i];
				v[i] = v[j];
				v[j] = vt;
			}
		}
	}
}
