/*
 * Copyright (c) 2009-2012 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.app

import com.jme3.app.state.AppStateManager
import com.jme3.asset.AssetManager
import com.jme3.audio.AudioRenderer
import com.jme3.audio.Listener
import com.jme3.input.InputManager
import com.jme3.profile.AppProfiler
import com.jme3.renderer.Camera
import com.jme3.renderer.RenderManager
import com.jme3.renderer.Renderer
import com.jme3.renderer.ViewPort
import com.jme3.system.*
import java.util.concurrent.Callable
import java.util.concurrent.Future

/**
 * The `Application` interface represents the minimum exposed
 * capabilities of a concrete jME3 application.
 */
interface Application {

    /**
     * Determine the application's behavior when unfocused.
     *
     * @return The lost focus behavior of the application.
     */
    /**
     * Change the application's behavior when unfocused.
     *
     * By default, the application will
     * [throttle the update loop][LostFocusBehavior.ThrottleOnLostFocus]
     * so as to not take 100% CPU usage when it is not in focus, e.g.
     * alt-tabbed, minimized, or obstructed by another window.
     *
     * @param lostFocusBehavior The new lost focus behavior to use.
     *
     * @see LostFocusBehavior
     */
    var lostFocusBehavior: LostFocusBehavior

    /**
     * Returns true if pause on lost focus is enabled, false otherwise.
     *
     * @return true if pause on lost focus is enabled
     *
     * @see .getLostFocusBehavior
     */
    /**
     * Enable or disable pause on lost focus.
     *
     *
     * By default, pause on lost focus is enabled.
     * If enabled, the application will stop updating
     * when it loses focus or becomes inactive (e.g. alt-tab).
     * For online or real-time applications, this might not be preferable,
     * so this feature should be set to disabled. For other applications,
     * it is best to keep it on so that CPU usage is not used when
     * not necessary.
     *
     * @param pauseOnLostFocus True to enable pause on lost focus, false
     * otherwise.
     *
     * @see .setLostFocusBehavior
     */
    var isPauseOnLostFocus: Boolean

    /**
     * Sets the Timer implementation that will be used for calculating
     * frame times.  By default, Application will use the Timer as returned
     * by the current JmeContext implementation.
     */
    var timer: Timer?

    /**
     * @return The [asset manager][AssetManager] for this application.
     */
    var _assetManager: AssetManager?

    /**
     * @return the [input manager][InputManager].
     */
    var inputManager: InputManager?

    /**
     * @return the [app state manager][AppStateManager]
     */
    var stateManager: AppStateManager?

    /**
     * @return the [render manager][RenderManager]
     */
    var renderManager: RenderManager?

    /**
     * @return The [renderer][Renderer] for the application
     */
    var renderer: Renderer?

    /**
     * @return The [audio renderer][AudioRenderer] for the application
     */
    var audioRenderer: AudioRenderer?

    /**
     * @return The [listener][Listener] object for audio
     */
    var listener: Listener?

    /**
     * @return The [display context][JmeContext] for the application
     */
    var context: JmeContext?

    /**
     * @return The main [camera][Camera] for the application
     */
    var camera: Camera?

    /**
     * Returns the current AppProfiler hook, or null if none is set.
     */
    /**
     * Sets an AppProfiler hook that will be called back for
     * specific steps within a single update frame.  Value defaults
     * to null.
     */
    var appProfiler: AppProfiler?

    /**
     * @return The GUI viewport. Which is used for the on screen
     * statistics and FPS.
     */
    var guiViewPort: ViewPort?

    var viewPort: ViewPort?

    /**
     * Set the display settings to define the display created.
     *
     *
     * Examples of display parameters include display pixel width and height,
     * color bit depth, z-buffer bits, anti-aliasing samples, and update frequency.
     * If this method is called while the application is already running, then
     * [.restart] must be called to apply the settings to the display.
     *
     * @param settings The settings to set.
     */
    var settings: AppSettings?
//    fun setSettings(settings: AppSettings?)

    /**
     * Starts the application.
     */
    fun start()

    /**
     * Starts the application.
     */
    fun start(waitFor: Boolean)

    /**
     * Restarts the context, applying any changed settings.
     *
     *
     * Changes to the [AppSettings] of this Application are not
     * applied immediately; calling this method forces the context
     * to restart, applying the new settings.
     */
    fun restart()

    /**
     * Requests the context to close, shutting down the main loop
     * and making necessary cleanup operations.
     *
     * Same as calling stop(false)
     *
     * @see .stop
     */
    fun stop()

    /**
     * Requests the context to close, shutting down the main loop
     * and making necessary cleanup operations.
     * After the application has stopped, it cannot be used anymore.
     */
    fun stop(waitFor: Boolean)

    /**
     * Enqueues a task/callable object to execute in the jME3
     * rendering thread.
     *
     *
     * Callables are executed right at the beginning of the main loop.
     * They are executed even if the application is currently paused
     * or out of focus.
     *
     * @param callable The callable to run in the main jME3 thread
     */
    fun <V> enqueue(callable: Callable<V>): Future<V>

    /**
     * Enqueues a runnable object to execute in the jME3
     * rendering thread.
     *
     *
     * Runnables are executed right at the beginning of the main loop.
     * They are executed even if the application is currently paused
     * or out of focus.
     *
     * @param runnable The runnable to run in the main jME3 thread
     */
    fun enqueue(runnable: Runnable)

    fun <T> enqueue(runnableWrapper: T) {}
    fun getAssetManager(): AssetManager?
}
