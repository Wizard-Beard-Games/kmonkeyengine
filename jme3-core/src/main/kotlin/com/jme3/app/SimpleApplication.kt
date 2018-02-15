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

import com.jme3.app.state.AppState
import com.jme3.audio.AudioListenerState
import com.jme3.font.BitmapFont
import com.jme3.font.BitmapText
import com.jme3.input.FlyByCamera
import com.jme3.input.KeyInput
import com.jme3.input.controls.ActionListener
import com.jme3.input.controls.KeyTrigger
import com.jme3.profile.AppStep
import com.jme3.renderer.RenderManager
import com.jme3.renderer.queue.RenderQueue.Bucket
import com.jme3.scene.Node
import com.jme3.scene.Spatial.CullHint
import com.jme3.system.AppSettings
import com.jme3.system.JmeContext.Type
import com.jme3.system.JmeSystem

/**
 * `SimpleApplication` is the base class for all jME3 Applications.
 * `SimpleApplication` will display a statistics view
 * using the [com.jme3.app.StatsAppState] AppState. It will display
 * the current frames-per-second value on-screen in addition to the statistics.
 * Several keys have special functionality in `SimpleApplication`:<br></br>
 *
 * <table>
 * <tr><td>Esc</td><td>- Close the application</td></tr>
 * <tr><td>C</td><td>- Display the camera position and rotation in the console.</td></tr>
 * <tr><td>M</td><td>- Display memory usage in the console.</td></tr>
</table> *
 *
 * A [com.jme3.app.FlyCamAppState] is by default attached as well and can
 * be removed by calling `stateManager.detach( stateManager.getState(FlyCamAppState.class) );`
 *
 * @author (kme) Ray Long
 * @author (jme)
 */
abstract class SimpleApplication(vararg initialStates: AppState) : LegacyApplication(*initialStates) {

    /**
     * Retrieves rootNode
     * @return rootNode Node object
     */
    var rootNode = Node("Root Node")
        protected set
    /**
     * Retrieves guiNode
     * @return guiNode Node object
     */
    var guiNode = Node("Gui Node")
        protected set
    protected var fpsText: BitmapText? = null
    var guiFont: BitmapFont? = null
    /**
     * Retrieves flyCam
     * @return flyCam Camera object
     */
    var flyByCamera: FlyByCamera? = null
        protected set
    /**
     * Toggles settings window to display at start-up
     * @param showSettings Sets true/false
     */
    var isShowSettings = true
    private val actionListener = AppActionListener()

    private inner class AppActionListener : ActionListener {

        override fun onAction(name: String, value: Boolean, tpf: Float) {
            when {
                !value -> return
                else -> when (name) {
                    INPUT_MAPPING_EXIT -> stop()
                    else -> when (name) {
                        INPUT_MAPPING_HIDE_STATS -> when {
                            stateManager!!.getState(StatsAppState::class.java) != null -> stateManager!!.getState(StatsAppState::class.java)!!.toggleStats()
                        }
                    }
                }
            }

        }
    }

    constructor() : this(StatsAppState(), FlyCamAppState(), AudioListenerState(), DebugKeysAppState()) {}

    override fun start() {
        // set some default settings in-case
        // settings dialog is not shown
        var loadSettings = false
        //re-setting settings they can have been merged from the registry.
        //re-setting settings they can have been merged from the registry.
        when (settings) {
            null -> {
                settings = AppSettings(true)
                loadSettings = true
            }

        // show settings dialog
        }

        // show settings dialog
        when {
            isShowSettings -> if (!JmeSystem.showSettingsDialog(settings, loadSettings)) {
                return
            }
        }
        //re-setting settings they can have been merged from the registry.
        //re-setting settings they can have been merged from the registry.
        settings = settings
        super.start()
    }

    /**
     * Creates the font that will be set to the guiFont field
     * and subsequently set as the font for the stats text.
     */
    protected fun loadGuiFont(): BitmapFont {
        return _assetManager!!.loadFont("Interface/Fonts/Default.fnt")
    }

    override fun initialize() {
        super.initialize()

        // Several things rely on having this
        guiFont = loadGuiFont()

        guiNode.queueBucket = Bucket.Gui
        guiNode.cullHint = CullHint.Never
        viewPort!!.attachScene(rootNode)
        guiViewPort!!.attachScene(guiNode)

        // Some of the tests rely on having access to fpsText
        // for quick display.  Maybe a different way would be better.

        // call user code

        // call user code
        when {
            inputManager != null -> {

                // We have to special-case the FlyCamAppState because too
                // many SimpleApplication subclasses expect it to exist in
                // simpleInit().  But at least it only gets initialized if
                // the app state is added.
                when {
                    stateManager!!.getState(FlyCamAppState::class.java) != null -> {
                        flyByCamera = FlyByCamera(this.camera)
                        flyByCamera?.moveSpeed = 1f // odd to set this here but it did it before
                        //                stateManager!!.getState(FlyCamAppState::class.java)!!.setCamera(flyByCamera)
                        stateManager!!.getState(FlyCamAppState::class.java)!!.camera = flyByCamera
                    }
                }

                when {
                    context!!.getType() == Type.Display -> inputManager!!.addMapping(INPUT_MAPPING_EXIT, KeyTrigger(KeyInput.KEY_ESCAPE))
                }

                when {
                    stateManager!!.getState(StatsAppState::class.java) != null -> {
                        inputManager!!.addMapping(INPUT_MAPPING_HIDE_STATS, KeyTrigger(KeyInput.KEY_F5))
                        inputManager!!.addListener(actionListener, INPUT_MAPPING_HIDE_STATS)
                    }
                }

                inputManager!!.addListener(actionListener, INPUT_MAPPING_EXIT)
            }
        }

        when {
            stateManager!!.getState(StatsAppState::class.java) != null -> {
                // Some of the tests rely on having access to fpsText
                // for quick display.  Maybe a different way would be better.
                stateManager!!.getState(StatsAppState::class.java)!!.setFont(guiFont!!)
                fpsText = stateManager!!.getState(StatsAppState::class.java)!!.fpsText
            }

        // call user code
        }

        // call user code
        simpleInitApp()
    }

    override fun update() {// render states

        // render states
// simple update and root node

        // simple update and root node
// update states
        // render states

        // render states
        // update states
        // render states

        // render states
        when {
            prof != null -> prof!!.appStep(AppStep.BeginFrame)
        // makes sure to execute AppTasks// render states

        // render states
        // simple update and root node

        // simple update and root node
        }

        super.update() // makes sure to execute AppTasks// render states

        // render states
        // simple update and root node

        // simple update and root node
        when {
            speed == 0f || paused -> return

        // update states
        // render states

        // render states
            else -> {
                val tpf = timer!!.getTimePerFrame() * speed

                // update states
                // render states

                // render states
                when {
                    prof != null -> prof!!.appStep(AppStep.StateManagerUpdate)

                // simple update and root node
                }
                stateManager!!.update(tpf)

                // simple update and root node
                simpleUpdate(tpf)

                when {
                    prof != null -> prof!!.appStep(AppStep.SpatialUpdate)

                // render states
                }
                rootNode.updateLogicalState(tpf)
                guiNode.updateLogicalState(tpf)

                rootNode.updateGeometricState()
                guiNode.updateGeometricState()

                // render states
                when {
                    prof != null -> prof!!.appStep(AppStep.StateManagerRender)
                }
                stateManager!!.render(renderManager!!)

                when {
                    prof != null -> prof!!.appStep(AppStep.RenderFrame)
                }
                renderManager!!.render(tpf, context!!.isRenderable())
                simpleRender(renderManager!!)
                stateManager!!.postRender()

                when {
                    prof != null -> prof!!.appStep(AppStep.EndFrame)
                }
            }
        }

    }

    fun setDisplayFps(show: Boolean) {
        when {
            stateManager!!.getState(StatsAppState::class.java) != null -> stateManager!!.getState(StatsAppState::class.java)!!.setDisplayFps(show)
        }
    }

    fun setDisplayStatView(show: Boolean) {
        when {
            stateManager!!.getState(StatsAppState::class.java) != null -> stateManager!!.getState(StatsAppState::class.java)!!.setDisplayStatView(show)
        }
    }

    abstract fun simpleInitApp()

    fun simpleUpdate(tpf: Float) {}

    fun simpleRender(rm: RenderManager) {}

    companion object {

        val INPUT_MAPPING_EXIT = "SIMPLEAPP_Exit"
        val INPUT_MAPPING_CAMERA_POS = DebugKeysAppState.INPUT_MAPPING_CAMERA_POS
        val INPUT_MAPPING_MEMORY = DebugKeysAppState.INPUT_MAPPING_MEMORY
        val INPUT_MAPPING_HIDE_STATS = "SIMPLEAPP_HideStats"
    }
}
