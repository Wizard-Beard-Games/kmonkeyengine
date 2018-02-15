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

package com.jme3.app

import com.jme3.app.state.BaseAppState
import com.jme3.input.InputManager
import com.jme3.input.KeyInput
import com.jme3.input.controls.ActionListener
import com.jme3.input.controls.KeyTrigger
import com.jme3.material.Material
import com.jme3.material.RenderState.BlendMode
import com.jme3.scene.Geometry
import com.jme3.scene.Mesh
import com.jme3.scene.Node
import com.jme3.scene.VertexBuffer.Type


/**
 * Provides a basic profiling visualization that shows
 * per-frame application-wide timings for update and
 * rendering.
 *
 * @author (kme) Ray Long
 * @author (jme) Paul Speed
 */
class BasicProfilerState @JvmOverloads constructor(enabled: Boolean = false) : BaseAppState() {

    val profiler: BasicProfiler
    private var graph: Geometry? = null
    private var background: Geometry? = null
    /**
     * Sets the vertical scale of the visualization where
     * each unit is a millisecond.  Defaults to 2, ie: a
     * single millisecond stretches two pixels high.
     * @param scale the scale
     */
    var graphScale = 2f
        set(scale) {
            when (scale) {
                this.graphScale -> return
                else -> {
                    field = scale
                    when {
                        graph != null -> graph!!.setLocalScale(1f, scale, 1f)
                    }
                }
            }
        }

    private val keyListener = ProfilerKeyListener()

    /**
     * Sets the number frames displayed and tracked.
     * @param count the number of frames
     */
    var frameCount: Int
        get() = profiler.frameCount
        set(count) {
            if (profiler.frameCount == count) {
                return
            }
            profiler.frameCount = count
            refreshBackground()
        }

    init {
//        isEnabled = enabled
        setEnabled(enabled)
        this.profiler = BasicProfiler()
    }

    fun toggleProfiler() {
//        isEnabled = !isEnabled
        setEnabled(!isEnabled())
    }

    protected fun refreshBackground() {
        val mesh = background!!.mesh

        val size = profiler.frameCount
        val frameTime = 1000f / 60
        mesh.setBuffer(Type.Position, 3, floatArrayOf(

                // first quad
                0f, 0f, 0f, size.toFloat(), 0f, 0f, size.toFloat(), frameTime, 0f, 0f, frameTime, 0f,

                // second quad
                0f, frameTime, 0f, size.toFloat(), frameTime, 0f, size.toFloat(), frameTime * 2, 0f, 0f, frameTime * 2, 0f,

                // A lower dark border just to frame the
                // 'update' stats against bright backgrounds
                0f, -2f, 0f, size.toFloat(), -2f, 0f, size.toFloat(), 0f, 0f, 0f, 0f, 0f))

        mesh.setBuffer(Type.Color, 4, floatArrayOf(
                // first quad, within normal frame limits
                0f, 1f, 0f, 0.25f, 0f, 1f, 0f, 0.25f, 0f, 0.25f, 0f, 0.25f, 0f, 0.25f, 0f, 0.25f,

                // Second quad, dropped frames
                0.25f, 0f, 0f, 0.25f, 0.25f, 0f, 0f, 0.25f, 1f, 0f, 0f, 0.25f, 1f, 0f, 0f, 0.25f,

                0f, 0f, 0f, 0.5f, 0f, 0f, 0f, 0.5f, 0f, 0f, 0f, 0.5f, 0f, 0f, 0f, 0.5f))

        mesh.setBuffer(Type.Index, 3, shortArrayOf(0, 1, 2, 0, 2, 3, 4, 5, 6, 4, 6, 7, 8, 9, 10, 8, 10, 11))
    }

    override fun initialize(app: Application) {

        graph = Geometry("profiler", profiler.mesh)

        var mat = Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md")
        mat.setBoolean("VertexColor", true)
        graph!!.material = mat
        graph!!.setLocalTranslation(0f, 300f, 0f)
        graph!!.setLocalScale(1f, graphScale, 1f)

        val mesh = Mesh()
        background = Geometry("profiler.background", mesh)
        mat = Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md")
        mat.setBoolean("VertexColor", true)
        mat.additionalRenderState.blendMode = BlendMode.Alpha
        background!!.material = mat
        background!!.setLocalTranslation(0f, 300f, -1f)
        background!!.setLocalScale(1f, graphScale, 1f)

        refreshBackground()

        val inputManager = app.inputManager
        when {
            inputManager != null -> {
                inputManager.addMapping(INPUT_MAPPING_PROFILER_TOGGLE, KeyTrigger(KeyInput.KEY_F6))
                inputManager.addListener(keyListener, INPUT_MAPPING_PROFILER_TOGGLE)
            }
        }
    }

    override fun cleanup(app: Application?) {
        val inputManager = app?.inputManager
        when {
            inputManager?.hasMapping(INPUT_MAPPING_PROFILER_TOGGLE)!! -> inputManager.deleteMapping(INPUT_MAPPING_PROFILER_TOGGLE)
        }
        inputManager?.removeListener(keyListener)
    }

    override fun onEnable() {

        // Set the number of visible frames to the current width of the screen
        frameCount = application!!.camera!!.width

        application!!.appProfiler = profiler
        val gui = (application as SimpleApplication).guiNode
        gui.attachChild(graph!!)
        gui.attachChild(background!!)
    }

    override fun onDisable() {
        application!!.appProfiler = null
        graph!!.removeFromParent()
        background!!.removeFromParent()
    }

    private inner class ProfilerKeyListener : ActionListener {

        override fun onAction(name: String, value: Boolean, tpf: Float) {
            when {
                !value -> return
                else -> toggleProfiler()
            }
        }
    }

    companion object {

        val INPUT_MAPPING_PROFILER_TOGGLE = "BasicProfilerState_Toggle"
    }
}

