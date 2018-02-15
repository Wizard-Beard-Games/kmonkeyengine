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

import com.jme3.app.state.AbstractAppState
import com.jme3.app.state.AppStateManager
import com.jme3.font.BitmapFont
import com.jme3.font.BitmapText
import com.jme3.material.Material
import com.jme3.material.RenderState.BlendMode
import com.jme3.math.ColorRGBA
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.Spatial.CullHint
import com.jme3.scene.shape.Quad


/**
 * Displays stats in SimpleApplication's GUI node or
 * using the node and font parameters provided.
 *
 * @author    Paul Speed
 */
class StatsAppState : AbstractAppState {

    private var app: Application? = null
    var statsView: StatsView? = null
        protected set
    protected var showSettings = true
    private var showFps = true
    private var showStats = true
    var isDarkenBehind = true
        set(darkenBehind) {
            field = darkenBehind
            setEnabled(isEnabled())
        }

    protected var guiNode: Node? = null
    var secondCounter = 0.0f
        protected set
    protected var frameCounter = 0
    var fpsText: BitmapText? = null
        protected set
    protected var guiFont: BitmapFont? = null
    protected var darkenFps: Geometry? = null
    protected var darkenStats: Geometry? = null

    constructor() {}

    constructor(guiNode: Node, guiFont: BitmapFont) {
        this.guiNode = guiNode
        this.guiFont = guiFont
    }

    /**
     * Called by SimpleApplication to provide an early font
     * so that the fpsText can be created before init.  This
     * is because several applications expect to directly access
     * fpsText... unfortunately.
     */
    fun setFont(guiFont: BitmapFont) {
        this.guiFont = guiFont
        this.fpsText = BitmapText(guiFont, false)
    }

    fun toggleStats() {
        setDisplayFps(!showFps)
        setDisplayStatView(!showStats)
    }

    fun setDisplayFps(show: Boolean) {
        showFps = show
        when {
            fpsText != null -> {
                fpsText!!.cullHint = if (show) CullHint.Never else CullHint.Always
                when {
                    darkenFps != null -> darkenFps!!.cullHint = when {
                        showFps && isDarkenBehind -> CullHint.Never
                        else -> CullHint.Always
                    }
                }

            }
        }
    }

    fun setDisplayStatView(show: Boolean) {
        showStats = show
        when {
            statsView != null -> {
                statsView!!.isEnabled = show
                statsView!!.cullHint = when {
                    show -> CullHint.Never
                    else -> CullHint.Always
                }
                when {
                    darkenStats != null -> darkenStats!!.cullHint = when {
                        showStats && isDarkenBehind -> CullHint.Never
                        else -> CullHint.Always
                    }
                }
            }
        }
    }

    override fun initialize(stateManager: AppStateManager, app: Application?) {
        super.initialize(stateManager, app)
        this.app = app

        when (app) {
            is SimpleApplication -> {
                val simpleApp = app as SimpleApplication?
                when (guiNode) {
                    null -> guiNode = simpleApp!!.guiNode
                }
                when (guiFont) {
                    null -> guiFont = simpleApp!!.guiFont
                }
            }
        }

        when (guiNode) {
            null -> throw RuntimeException("No guiNode specific and cannot be automatically determined.")
            else -> {
                when (guiFont) {
                    null -> guiFont = app!!.getAssetManager()?.loadFont("Interface/Fonts/Default.fnt")
                }

                loadFpsText()
                loadStatsView()
                loadDarken()
            }
        }

    }

    /**
     * Attaches FPS statistics to guiNode and displays it on the screen.
     *
     */
    fun loadFpsText() {
        when (fpsText) {
            null -> fpsText = BitmapText(guiFont, false)
        }

        fpsText!!.setLocalTranslation(0f, fpsText!!.lineHeight, 0f)
        fpsText!!.text = "Frames per second"
        fpsText!!.cullHint = if (showFps) CullHint.Never else CullHint.Always
        guiNode!!.attachChild(fpsText)

    }

    /**
     * Attaches Statistics View to guiNode and displays it on the screen
     * above FPS statistics line.
     *
     */
    fun loadStatsView() {
        statsView = StatsView("Statistics View",
                app!!.getAssetManager(),
                app!!.renderer!!.statistics)
        // move it up so it appears above fps text
        statsView!!.setLocalTranslation(0f, fpsText!!.lineHeight, 0f)
        statsView!!.isEnabled = showStats
        statsView!!.cullHint = if (showStats) CullHint.Never else CullHint.Always
        guiNode!!.attachChild(statsView)
    }

    fun loadDarken() {
        val mat = Material(app!!.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md")
        mat.setColor("Color", ColorRGBA(0f, 0f, 0f, 0.5f))
        mat.additionalRenderState.blendMode = BlendMode.Alpha

        darkenFps = Geometry("StatsDarken", Quad(200f, fpsText!!.lineHeight))
        darkenFps!!.material = mat
        darkenFps!!.setLocalTranslation(0f, 0f, -1f)
        darkenFps!!.cullHint = if (showFps && isDarkenBehind) CullHint.Never else CullHint.Always
        guiNode!!.attachChild(darkenFps)

        darkenStats = Geometry("StatsDarken", Quad(200f, statsView!!.height))
        darkenStats!!.material = mat
        darkenStats!!.setLocalTranslation(0f, fpsText!!.height, -1f)
        darkenStats!!.cullHint = if (showStats && isDarkenBehind) CullHint.Never else CullHint.Always
        guiNode!!.attachChild(darkenStats)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)

        when {
            enabled -> {
                fpsText!!.cullHint = if (showFps) CullHint.Never else CullHint.Always
                darkenFps!!.cullHint = if (showFps && isDarkenBehind) CullHint.Never else CullHint.Always
                statsView!!.isEnabled = showStats
                statsView!!.cullHint = if (showStats) CullHint.Never else CullHint.Always
                darkenStats!!.cullHint = if (showStats && isDarkenBehind) CullHint.Never else CullHint.Always
            }
            else -> {
                fpsText!!.cullHint = CullHint.Always
                darkenFps!!.cullHint = CullHint.Always
                statsView!!.isEnabled = false
                statsView!!.cullHint = CullHint.Always
                darkenStats!!.cullHint = CullHint.Always
            }
        }
    }

    override fun update(tpf: Float) {
        when {
            showFps -> {
                secondCounter += app!!.timer!!.timePerFrame
                frameCounter++
                when {
                    secondCounter >= 1.0f -> {
                        val fps = (frameCounter / secondCounter).toInt()
                        fpsText!!.text = "Frames per second: " + fps
                        secondCounter = 0.0f
                        frameCounter = 0
                    }
                }
            }
        }
    }

    override fun cleanup() {
        super.cleanup()

        guiNode!!.detachChild(statsView)
        guiNode!!.detachChild(fpsText)
        guiNode!!.detachChild(darkenFps)
        guiNode!!.detachChild(darkenStats)
    }


}
