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
package com.jme3.app.state

import com.jme3.app.Application
import com.jme3.renderer.RenderManager

/**
 * AppState represents continously executing code inside the main loop.
 *
 * An `AppState` can track when it is attached to the
 * [AppStateManager] or when it is detached.
 *
 * <br></br>`AppState`s are initialized in the render thread, upon a call to
 * [AppState.initialize]
 * and are de-initialized upon a call to [AppState.cleanup].
 * Implementations should return the correct value with a call to
 * [AppState.isInitialized] as specified above.<br></br>
 *
 *
 *  * If a detached AppState is attached then `initialize()` will be called
 * on the following render pass.
 *
 *  * If an attached AppState is detached then `cleanup()` will be called
 * on the following render pass.
 *
 *  * If you attach an already-attached `AppState` then the second attach
 * is a no-op and will return false.
 *
 *  * If you both attach and detach an `AppState` within one frame then
 * neither `initialize()` or `cleanup()` will be called,
 * although if either is called both will be.
 *
 *  * If you both detach and then re-attach an `AppState` within one frame
 * then on the next update pass its `cleanup()` and `initialize()`
 * methods will be called in that order.
 *
 *
 * @author (kme) Ray Long
 * @author (jme) Kirill Vainer
 */
interface AppState {

    /**
     * @return True if `initialize()` was called on the state,
     * false otherwise.
     */
    val isInitialized: Boolean

    /**
     * @return True if the `AppState` is enabled, false otherwise.
     *
     * @see AppState.setEnabled
     */
    /**
     * Enable or disable the functionality of the `AppState`.
     * The effect of this call depends on implementation. An
     * `AppState` starts as being enabled by default.
     * A disabled `AppState`s does not get calls to
     * [.update], [.render], or
     * [.postRender] from its [AppStateManager].
     *
     * @param active activate the AppState or not.
     */
    var isEnabled: Boolean

    /**
     * Called by [AppStateManager] when transitioning this `AppState`
     * from *initializing* to *running*.<br></br>
     * This will happen on the next iteration through the update loop after
     * [AppStateManager.attach] was called.
     *
     *
     * `AppStateManager` will call this only from the update loop
     * inside the rendering thread. This means is it safe to modify the scene
     * graph from this method.
     *
     * @param stateManager The state manager
     * @param app The application
     */
    fun initialize(stateManager: AppStateManager, app: Application)

    /**
     * Called by [AppStateManager.attach]
     * when transitioning this
     * `AppState` from *detached* to *initializing*.
     *
     *
     * There is no assumption about the thread from which this function is
     * called, therefore it is **unsafe** to modify the scene graph
     * from this method. Please use
     * [.initialize]
     * instead.
     *
     * @param stateManager State manager to which the state was attached to.
     */
    fun stateAttached(stateManager: AppStateManager)

    /**
     * Called by [AppStateManager.detach]
     * when transitioning this
     * `AppState` from *running* to *terminating*.
     *
     *
     * There is no assumption about the thread from which this function is
     * called, therefore it is **unsafe** to modify the scene graph
     * from this method. Please use
     * [.cleanup]
     * instead.
     *
     * @param stateManager The state manager from which the state was detached from.
     */
    fun stateDetached(stateManager: AppStateManager)

    /**
     * Called to update the `AppState`. This method will be called
     * every render pass if the `AppState` is both attached and enabled.
     *
     * @param tpf Time since the last call to update(), in seconds.
     */
    fun update(tpf: Float)

    /**
     * Render the state. This method will be called
     * every render pass if the `AppState` is both attached and enabled.
     *
     * @param rm RenderManager
     */
    fun render(rm: RenderManager)

    /**
     * Called after all rendering commands are flushed. This method will be called
     * every render pass if the `AppState` is both attached and enabled.
     */
    fun postRender()

    /**
     * Called by [AppStateManager] when transitioning this
     * `AppState` from *terminating* to *detached*. This
     * method is called the following render pass after the `AppState` has
     * been detached and is always called once and only once for each time
     * `initialize()` is called. Either when the `AppState`
     * is detached or when the application terminates (if it terminates normally).
     */
    fun cleanup()

}
