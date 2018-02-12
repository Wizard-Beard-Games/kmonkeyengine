/*
 * Copyright (c) 2014 jMonkeyEngine
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
import java.util.logging.Level
import java.util.logging.Logger


/**
 * A base app state implementation the provides more built-in
 * management convenience than AbstractAppState, including methods
 * for enable/disable/initialize state management.
 * The abstract onEnable() and onDisable() methods are called
 * appropriately during initialize(), terminate(), or setEnabled()
 * depending on the mutual state of "initialized" and "enabled".
 *
 *
 * initialize() and terminate() can be used by subclasses to
 * manage resources that should exist the entire time that the
 * app state is attached.  This is useful for resources that might
 * be expensive to create or load.
 *
 *
 * onEnable()/onDisable() can be used for managing things that
 * should only exist while the state is enabled.  Prime examples
 * would be scene graph attachment or input listener attachment.
 *
 *
 * The base class logic is such that onDisable() will always be called
 * before cleanup() if the state is enabled.  Likewise, enable()
 * will always be called after initialize() if the state is enable().
 * onEnable()/onDisable() are also called appropriate when setEnabled()
 * is called that changes the enabled state AND if the state is attached.
 * In other words, onEnable()/onDisable() are only ever called on an already
 * attached state.
 *
 *
 * It is technically safe to do all initialization and cleanup in
 * the onEnable()/onDisable() methods.  Choosing to use initialize()
 * and cleanup() for this is a matter of performance specifics for the
 * implementor.
 *
 * @Author (kme) Ray Long
 * @author (jme) Paul Speed
 */
abstract class BaseAppState : AppState {

    var application: Application? = null
        private set
//    final override var isInitialized: Boolean = false
//        private set(value: Boolean) {
////            super.isInitialized = value
//            isInitialized = value
//        }

    private var initialized = false

    override fun isInitialized(): Boolean {
        return initialized
    }

    private fun setInitialized(value: Boolean) {
        this.initialized = value
    }

//    override var isEnabled = true
//        set(enabled) {
//            if (this.isEnabled == enabled)
//                return
//            field = enabled
//            if (!isInitialized)
//                return
//            if (enabled) {
//                log.log(Level.FINEST, "onEnable():{0}", this)
//                onEnable()
//            } else {
//                log.log(Level.FINEST, "onDisable():{0}", this)
//                onDisable()
//            }
//        }


    override fun isEnabled(): Boolean {
        return this.enabled
    }

    private var enabled: Boolean = true

    override fun setEnabled(enabled: Boolean) {
        when (enabled) {
            this.isEnabled() -> return
            else -> {
                this.enabled = enabled
                when {
                    !isInitialized() -> return
                    else -> when {
                        enabled -> {
                            log.log(Level.FINEST, "onEnable():{0}", this)
                            onEnable()
                        }
                        else -> {
                            log.log(Level.FINEST, "onDisable():{0}", this)
                            onDisable()
                        }
                    }
                }
            }
        }
    }

    val stateManager: AppStateManager
        get() = application!!.stateManager

    /**
     * Called during initialization once the app state is
     * attached and before onEnable() is called.
     * @param app the application
     */
    protected abstract fun initialize(app: Application)

    /**
     * Called after the app state is detached or during
     * application shutdown if the state is still attached.
     * onDisable() is called before this cleanup() method if
     * the state is enabled at the time of cleanup.
     * @param app the application
     */
    protected abstract fun cleanup(app: Application?)

    /**
     * Called when the state is fully enabled, ie: is attached
     * and isEnabled() is true or when the setEnabled() status
     * changes after the state is attached.
     */
    protected abstract fun onEnable()

    /**
     * Called when the state was previously enabled but is
     * now disabled either because setEnabled(false) was called
     * or the state is being cleaned up.
     */
    protected abstract fun onDisable()

    /**
     * Do not call directly: Called by the state manager to initialize this
     * state post-attachment.
     * This implementation calls initialize(app) and then onEnable() if the
     * state is enabled.
     */
    override fun initialize(stateManager: AppStateManager, app: Application) {
        log.log(Level.FINEST, "initialize():{0}", this)

        this.application = app
//        isInitialized = true
        setInitialized(true)
        initialize(app)
        when {
            isEnabled() -> {
                log.log(Level.FINEST, "onEnable():{0}", this)
                onEnable()
            }
        }
    }

    fun <T : AppState> getState(type: Class<T>): T? {
        return stateManager.getState(type)
    }

    override fun stateAttached(stateManager: AppStateManager) {}

    override fun stateDetached(stateManager: AppStateManager) {}

    override fun update(tpf: Float) {}

    override fun render(rm: RenderManager) {}

    override fun postRender() {}

    /**
     * Do not call directly: Called by the state manager to terminate this
     * state post-detachment or during state manager termination.
     * This implementation calls onDisable() if the state is enabled and
     * then cleanup(app).
     */
    override fun cleanup() {
        log.log(Level.FINEST, "cleanup():{0}", this)

        when {
            isEnabled() -> {
                log.log(Level.FINEST, "onDisable():{0}", this)
                onDisable()
            }
//        isInitialized = false
        }
        cleanup(application)
//        isInitialized = false
        setInitialized(false)
    }

    companion object {

        internal val log = Logger.getLogger(BaseAppState::class.java.name)
    }
}
