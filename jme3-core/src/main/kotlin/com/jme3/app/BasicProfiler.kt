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

import com.jme3.profile.*
import com.jme3.renderer.ViewPort
import com.jme3.renderer.queue.RenderQueue.Bucket
import com.jme3.scene.Mesh
import com.jme3.scene.VertexBuffer.Type
import com.jme3.util.BufferUtils
import java.nio.FloatBuffer


/**
 * An AppProfiler implementation that collects two
 * per-frame application-wide timings for update versus
 * render and uses it to create a bar chart style Mesh.
 * The number of frames displayed and the update interval
 * can be specified.  The chart Mesh is in 'milliseconds'
 * and can be scaled up or down as required.
 *
 *
 * Each column of the chart represents a single frames
 * timing.  Yellow represents the time it takes to
 * perform all non-rendering activities (running enqueued
 * tasks, stateManager.update, control.update(), etc) while
 * the cyan portion represents the rendering time.
 *
 *
 * When the end of the chart is reached, the current
 * frame cycles back around to the beginning.
 *
 * @author (kme) Ray Long
 * @author (jme) Paul Speed
 */
class BasicProfiler @JvmOverloads constructor(size: Int = 1280) : AppProfiler {

    /**
     * Sets the number of frames to display and track.  By default
     * this is 1280.
     */
    var frameCount: Int = 0
        set(size) {
            when (size) {
                this.frameCount -> return
                else -> {
                    field = size
                    this.frames = LongArray(size * 2)

                    createMesh()

                    when {
                        frameIndex >= size -> frameIndex = 0
                    }
                }
            }

        }
    private var frameIndex = 0
    private var frames: LongArray? = null
    private var startTime: Long = 0
    private var renderTime: Long = 0
    private var previousFrame: Long = 0
    /**
     * Sets the number of nanoseconds to wait before updating the
     * mesh.  By default this is once a millisecond, ie: 1000000 nanoseconds.
     */
    var updateInterval = 1000000L // once a millisecond
    private var lastUpdate: Long = 0

    /**
     * Returns the mesh that contains the bar chart of tracked frame
     * timings.
     */
    var mesh: Mesh? = null
        private set

    init {
        frameCount = size
    }

    protected fun createMesh() {
        // For each index we add 4 colors, one for each line
        // endpoint for two layers.
        when (mesh) {
            null -> {
                mesh = Mesh()
                mesh!!.mode = Mesh.Mode.Lines
            }
        }

        mesh!!.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(frameCount * 4 * 3))

        val cb = BufferUtils.createFloatBuffer(frameCount * 4 * 4)
        (0 until frameCount).forEach { i ->
            // For each index we add 4 colors, one for each line
            // endpoint for two layers.
            cb.put(0.5f).put(0.5f).put(0f).put(1f)
            cb.put(1f).put(1f).put(0f).put(1f)
            cb.put(0f).put(0.5f).put(0.5f).put(1f)
            cb.put(0f).put(1f).put(1f).put(1f)
        }
        mesh!!.setBuffer(Type.Color, 4, cb)
    }

    protected fun updateMesh() {
        val pb = mesh!!.getBuffer(Type.Position).data as FloatBuffer
        pb.rewind()
        val scale = 1 / 1000000f // scaled to ms as pixels
        (0 until frameCount).forEach { i ->
            val t1 = frames!![i * 2] * scale
            val t2 = frames!![i * 2 + 1] * scale

            pb.put(i.toFloat()).put(0f).put(0f)
            pb.put(i.toFloat()).put(t1).put(0f)
            pb.put(i.toFloat()).put(t1).put(0f)
            pb.put(i.toFloat()).put(t2).put(0f)
        }
        mesh!!.setBuffer(Type.Position, 3, pb)
    }

    override fun appStep(step: AppStep) {

        when (step) {
            AppStep.BeginFrame -> startTime = System.nanoTime()
            AppStep.RenderFrame -> {
                renderTime = System.nanoTime()
                frames!![frameIndex * 2] = renderTime - startTime
            }
            AppStep.EndFrame -> {
                val time = System.nanoTime()
                frames!![frameIndex * 2 + 1] = time - renderTime
                previousFrame = startTime
                frameIndex++
                when {
                    frameIndex >= frameCount -> frameIndex = 0
                }
                when {
                    startTime - lastUpdate > updateInterval -> {
                        updateMesh()
                        lastUpdate = startTime
                    }
                }
            }
            AppStep.QueuedTasks -> TODO()
            AppStep.ProcessInput -> TODO()
            AppStep.ProcessAudio -> TODO()
            AppStep.StateManagerUpdate -> TODO()
            AppStep.SpatialUpdate -> TODO()
            AppStep.StateManagerRender -> TODO()
            AppStep.RenderPreviewViewPorts -> TODO()
            AppStep.RenderMainViewPorts -> TODO()
            AppStep.RenderPostViewPorts -> TODO()
        }
    }

    override fun vpStep(step: VpStep, vp: ViewPort, bucket: Bucket) {}

    override fun spStep(step: SpStep, vararg additionalInfo: String) {

    }

}


