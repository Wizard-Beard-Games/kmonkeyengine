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

import com.jme3.asset.AssetManager
import com.jme3.font.BitmapFont
import com.jme3.font.BitmapText
import com.jme3.renderer.RenderManager
import com.jme3.renderer.Statistics
import com.jme3.renderer.ViewPort
import com.jme3.renderer.queue.RenderQueue.Bucket
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import com.jme3.scene.control.Control
import com.jme3.util.clone.Cloner
import com.jme3.util.clone.JmeCloneable

/**
 * The `StatsView` provides a heads-up display (HUD) of various
 * statistics of rendering. The data is retrieved every frame from a
 * [com.jme3.renderer.Statistics] and then displayed on screen.<br></br>
 * <br></br>
 * Usage:<br></br>
 * To use the stats view, you need to retrieve the
 * [com.jme3.renderer.Statistics] from the
 * [com.jme3.renderer.Renderer] used by the application. Then, attach
 * the `StatsView` to the scene graph.<br></br>
 * `<br></br>
 * Statistics stats = renderer.getStatistics();<br></br>
 * StatsView statsView = new StatsView("MyStats", _assetManager, stats);<br></br>
 * rootNode.attachChild(statsView);<br></br>
` *
 */
open class StatsView(name: String, manager: AssetManager, private val statistics: Statistics) : Node(name), Control, JmeCloneable {

    private val statText: BitmapText

    private val statLabels: Array<String>
    private val statData: IntArray

    var isEnabled = true
        set(enabled) {
            field = enabled
            statistics.isEnabled = enabled
        }

    private val stringBuilder: StringBuilder

    val height: Float
        get() = statText.lineHeight * statLabels.size

    override fun update(tpf: Float) {

        if (!isEnabled)
            return

        statistics.getData(statData)
        stringBuilder.setLength(0)

        // Need to walk through it backwards, as the first label
        // should appear at the bottom, not the top.
        for (i in statLabels.indices.reversed()) {
            stringBuilder.append(statLabels[i]).append(" = ").append(statData[i]).append('\n')
        }
        statText.setText(stringBuilder)

        // Moved to ResetStatsState to make sure it is
        // done even if there is no StatsView or the StatsView
        // is disable.
        //statistics.clearFrame();
    }

    override fun cloneForSpatial(spatial: Spatial): Control {
        return spatial as Control
    }

    override fun jmeClone(): StatsView {
        throw UnsupportedOperationException("Not yet implemented.")
    }

    override fun cloneFields(cloner: Cloner, original: Any) {
        throw UnsupportedOperationException("Not yet implemented.")
    }

    override fun setSpatial(spatial: Spatial) {}

    override fun render(rm: RenderManager, vp: ViewPort) {}

    init {
        this.stringBuilder = StringBuilder()
        setQueueBucket(Bucket.Gui)
        setCullHint(Spatial.CullHint.Never)
        statistics.isEnabled = isEnabled
        statLabels = statistics.labels
        statData = IntArray(statLabels.size)
        val font = manager.loadFont("Interface/Fonts/Console.fnt")
        statText = BitmapText(font)
        statText.setLocalTranslation(0f, statText.lineHeight * statLabels.size, 0f)
        attachChild(statText)
        addControl(this)
    }

}
