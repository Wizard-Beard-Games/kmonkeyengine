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
 * `AbstractAppState` implements some common methods
 * that make creation of AppStates easier.
 * @author (kme) Ray Long
 * @author (jme) Kirill Vainer
 * @see com.jme3.app.state.BaseAppState
 */
class AbstractAppState : AppState {

    /**
     * `initialized` is set to true when the method
     * [AbstractAppState.initialize]
     * is called. When [AbstractAppState.cleanup] is called, `initialized`
     * is set back to false.
     */
    protected var initialized = false
    private var enabled = true

    override fun initialize(stateManager: AppStateManager, app: Application) {
        initialized = true
    }

    override fun isInitialized(): Boolean {
        return initialized
    }

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    override fun isEnabled(): Boolean {
        return enabled
    }

    override fun stateAttached(stateManager: AppStateManager) {}

    override fun stateDetached(stateManager: AppStateManager) {}

    override fun update(tpf: Float) {}

    override fun render(rm: RenderManager) {}

    override fun postRender() {}

    override fun cleanup() {
        initialized = false
    }

}
