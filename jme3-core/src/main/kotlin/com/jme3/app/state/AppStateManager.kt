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
import com.jme3.util.SafeArrayList
import java.util.Arrays

/**
 * The `AppStateManager` holds a list of [AppState]s which
 * it will update and render.<br></br>
 * When an [AppState] is attached or detached, the
 * [AppState.stateAttached] and
 * [AppState.stateDetached] methods
 * will be called respectively.
 *
 *
 * The lifecycle for an attached AppState is as follows:
 *
 *  * stateAttached() : called when the state is attached on the thread on which
 * the state was attached.
 *  * initialize() : called ONCE on the render thread at the beginning of the next
 * AppStateManager.update().
 *  * stateDetached() : called when the state is detached on the thread on which
 * the state was detached.  This is not necessarily on the
 * render thread and it is not necessarily safe to modify
 * the scene graph, etc..
 *  * cleanup() : called ONCE on the render thread at the beginning of the next update
 * after the state has been detached or when the application is
 * terminating.
 *
 *
 * @author (kme) Ray Long
 * @author (jme) Kirill Vainer, Paul Speed
 */
class AppStateManager(
        // All of the above lists need to be thread safe but access will be
        // synchronized separately.... but always on the states list.  This
        // is to avoid deadlocking that may occur and the most common use case
        // is that they are all modified from the same thread anyway.

        /**
         * Returns the Application to which this AppStateManager belongs.
         */
        val application: Application) {

    /**
     * List holding the attached app states that are pending
     * initialization.  Once initialized they will be added to
     * the running app states.
     */
    private val initializing = SafeArrayList(AppState::class.java)

    /**
     * Holds the active states once they are initialized.
     */
    private val states = SafeArrayList(AppState::class.java)

    /**
     * List holding the detached app states that are pending
     * cleanup.
     */
    private val terminating = SafeArrayList(AppState::class.java)
    private val stateArray: Array<AppState>? = null

    protected fun getInitializing(): Array<AppState> {
        synchronized(states) {
            return initializing.array
        }
    }

    protected fun getTerminating(): Array<AppState> {
        synchronized(states) {
            return terminating.array
        }
    }

    protected fun getStates(): Array<AppState> {
        synchronized(states) {
            return states.array
        }
    }

    /**
     * Attach a state to the AppStateManager, the same state cannot be attached
     * twice.
     *
     * @param state The state to attach
     * @return True if the state was successfully attached, false if the state
     * was already attached.
     */
    fun attach(state: AppState): Boolean {
        synchronized(states) {
            return when {
                !states.contains(state) && !initializing.contains(state) -> {
                    state.stateAttached(this)
                    initializing.add(state)
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Attaches many state to the AppStateManager in a way that is guaranteed
     * that they will all get initialized before any of their updates are run.
     * The same state cannot be attached twice and will be ignored.
     *
     * @param states The states to attach
     */
    fun attachAll(vararg states: AppState) {
        attachAll(Arrays.asList(*states))
    }

    /**
     * Attaches many state to the AppStateManager in a way that is guaranteed
     * that they will all get initialized before any of their updates are run.
     * The same state cannot be attached twice and will be ignored.
     *
     * @param states The states to attach
     */
    fun attachAll(states: Iterable<AppState>) {
        synchronized(this.states) {
            for (state in states) {
                attach(state)
            }
        }
    }

    /**
     * Detaches the state from the AppStateManager.
     *
     * @param state The state to detach
     * @return True if the state was detached successfully, false
     * if the state was not attached in the first place.
     */
    fun detach(state: AppState): Boolean {
        synchronized(states) {
            return when {
                states.contains(state) -> {
                    state.stateDetached(this)
                    states.remove(state)
                    terminating.add(state)
                    true
                }
                initializing.contains(state) -> {
                    state.stateDetached(this)
                    initializing.remove(state)
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Check if a state is attached or not.
     *
     * @param state The state to check
     * @return True if the state is currently attached to this AppStateManager.
     *
     * @see AppStateManager.attach
     */
    fun hasState(state: AppState): Boolean {
        synchronized(states) {
            return states.contains(state) || initializing.contains(state)
        }
    }

    /**
     * Returns the first state that is an instance of subclass of the specified class.
     * @param <T>
     * @param stateClass
     * @return First attached state that is an instance of stateClass
    </T> */
    fun <T : AppState> getState(stateClass: Class<T>): T? {
        synchronized(states) {
            var array = getStates()
            for (state in array) {
                if (stateClass.isAssignableFrom(state.javaClass)) {
                    return state as T
                }
            }

            // This may be more trouble than its worth but I think
            // it's necessary for proper decoupling of states and provides
            // similar behavior to before where a state could be looked
            // up even if it wasn't initialized. -pspeed
            array = getInitializing()
            array.forEach { state ->
                if (stateClass.isAssignableFrom(state.javaClass)) {
                    return state as T
                }
            }
        }
        return null
    }

    protected fun initializePending() {
        val array = getInitializing()
        if (array.isEmpty())
            return

        synchronized(states) {
            // Move the states that will be initialized
            // into the active array.  In all but one case the
            // order doesn't matter but if we do this here then
            // a state can detach itself in initialize().  If we
            // did it after then it couldn't.
            val transfer = Arrays.asList(*array)
            states.addAll(transfer)
            initializing.removeAll(transfer)
        }
        array.forEach { state -> state.initialize(this, application) }
    }

    protected fun terminatePending() {
        val array = getTerminating()
        if (array.isEmpty())
            return

        array.forEach { state -> state.cleanup() }
        synchronized(states) {
            // Remove just the states that were terminated...
            // which might now be a subset of the total terminating
            // list.
            terminating.removeAll(Arrays.asList(*array))
        }
    }

    /**
     * Calls update for attached states, do not call directly.
     * @param tpf Time per frame.
     */
    fun update(tpf: Float) {

        // Cleanup any states pending
        terminatePending()

        // Initialize any states pending
        initializePending()

        // Update enabled states
        val array = getStates()
        array
                .filter { it.isEnabled }
                .forEach { it.update(tpf) }
    }

    /**
     * Calls render for all attached and initialized states, do not call directly.
     * @param rm The RenderManager
     */
    fun render(rm: RenderManager) {
        val array = getStates()
        array
                .filter { it.isEnabled }
                .forEach { it.render(rm) }
    }

    /**
     * Calls render for all attached and initialized states, do not call directly.
     */
    fun postRender() {
        val array = getStates()
        array
                .filter { it.isEnabled }
                .forEach { it.postRender() }
    }

    /**
     * Calls cleanup on attached states, do not call directly.
     */
    fun cleanup() {
        val array = getStates()
        array.forEach { state -> state.cleanup() }
    }
}
