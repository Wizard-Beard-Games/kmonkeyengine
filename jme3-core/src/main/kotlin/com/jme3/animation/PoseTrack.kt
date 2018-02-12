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
package com.jme3.animation

import com.jme3.export.*
import com.jme3.scene.Mesh
import com.jme3.scene.VertexBuffer
import com.jme3.scene.VertexBuffer.Type
import com.jme3.util.TempVars
import java.io.IOException
import java.nio.FloatBuffer

/**
 * A single track of pose animation associated with a certain mesh.
 *
 * @author (kme) Ray Long
 * @author (jme)
 */
@Deprecated("")
class PoseTrack : Track {

    private var targetMeshIndex: Int = 0
    private var frames: Array<PoseFrame>? = null
    private var times: FloatArray? = null

    class PoseFrame : Savable, Cloneable {

        internal var poses: Array<Pose>? = null
        internal lateinit var weights: FloatArray

        constructor(poses: Array<Pose>, weights: FloatArray) {
            this.poses = poses
            this.weights = weights
        }

        /**
         * Serialization-only. Do not use.
         */
        constructor() {}

        /**
         * This method creates a clone of the current object.
         * @return a clone of the current object
         */
        public override fun clone(): PoseFrame {
            try {
                val result = super.clone() as PoseFrame
                result.weights = this.weights.clone()
                when {
                    this.poses != null -> {
        //                    result.poses = arrayOfNulls(this.poses!!.size)
                        result.poses = Array(size = this.poses!!.size, init = { Pose() })
                        this.poses!!.indices.forEach { i -> result.poses!![i] = this.poses!![i].clone() }
                    }
                }
                return result
            } catch (e: CloneNotSupportedException) {
                throw AssertionError()
            }

        }

        @Throws(IOException::class)
        override fun write(e: JmeExporter) {
            val out = e.getCapsule(this)
            out.write(poses, "poses", null)
            out.write(weights, "weights", null)
        }

        @Throws(IOException::class)
        override fun read(i: JmeImporter) {
            val `in` = i.getCapsule(this)
            weights = `in`.readFloatArray("weights", null)

            val readSavableArray = `in`.readSavableArray("poses", null)
            when {
                readSavableArray != null -> {
    //                poses = arrayOfNulls(readSavableArray.size)
                    poses = Array(size = readSavableArray.size, init = { Pose() })
                    System.arraycopy(readSavableArray, 0, poses!!, 0, readSavableArray.size)
                }
            }
        }
    }

    constructor(targetMeshIndex: Int, times: FloatArray, frames: Array<PoseFrame>) {
        this.targetMeshIndex = targetMeshIndex
        this.times = times
        this.frames = frames
    }

    /**
     * Serialization-only. Do not use.
     */
    constructor() {}

    private fun applyFrame(target: Mesh, frameIndex: Int, weight: Float) {
        val frame = frames!![frameIndex]
        val pb = target.getBuffer(Type.Position)
        frame.poses!!.indices.forEach { i ->
            val pose = frame.poses!![i]
            val poseWeight = frame.weights[i] * weight

            pose.apply(poseWeight, pb.data as FloatBuffer)
        }

        // force to re-upload data to gpu
        pb.updateData(pb.data)
    }

    override fun setTime(time: Float, weight: Float, control: AnimControl, channel: AnimChannel, vars: TempVars) {
        // TODO: When MeshControl is created, it will gather targets
        // list automatically which is then retrieved here.

        /*
        Mesh target = targets[targetMeshIndex];
        if (time < times[0]) {
            applyFrame(target, 0, weight);
        } else if (time > times[times.length - 1]) {
            applyFrame(target, times.length - 1, weight);
        } else {
            int startFrame = 0;
            for (int i = 0; i < times.length; i++) {
                if (times[i] < time) {
                    startFrame = i;
                }
            }

            int endFrame = startFrame + 1;
            float blend = (time - times[startFrame]) / (times[endFrame] - times[startFrame]);
            applyFrame(target, startFrame, blend * weight);
            applyFrame(target, endFrame, (1f - blend) * weight);
        }
        */
    }

    /**
     * @return the length of the track
     */
    override fun getLength(): Float {
        return if (times == null) 0f else times!![times!!.size - 1] - times!![0]
    }

    override fun getKeyFrameTimes(): FloatArray? {
        return times
    }

    /**
     * This method creates a clone of the current object.
     * @return a clone of the current object
     */
    override fun clone(): PoseTrack = try {
//            val result = super.clone() as PoseTrack
        val result = clone()
        result.times = this.times!!.clone()
        when {
            this.frames != null -> {
                //                result.frames = arrayOfNulls(this.frames!!.size)
                result.frames = Array(size = this.frames!!.size, init = { PoseFrame() })
                this.frames!!.indices.forEach { i -> result.frames!![i] = this.frames!![i].clone() }
            }
        }
        result
    } catch (e: CloneNotSupportedException) {
        throw AssertionError()
    }

    @Throws(IOException::class)
    override fun write(e: JmeExporter) {
        val out = e.getCapsule(this)
        out.write(targetMeshIndex, "meshIndex", 0)
        out.write(frames, "frames", null)
        out.write(times, "times", null)
    }

    @Throws(IOException::class)
    override fun read(i: JmeImporter) {
        val `in` = i.getCapsule(this)
        targetMeshIndex = `in`.readInt("meshIndex", 0)
        times = `in`.readFloatArray("times", null)

        val readSavableArray = `in`.readSavableArray("frames", null)
        when {
            readSavableArray != null -> {
//            frames = arrayOfNulls(readSavableArray.size)
                frames = Array(size = readSavableArray.size, init = { PoseFrame() })
                System.arraycopy(readSavableArray, 0, frames!!, 0, readSavableArray.size)
            }
        }
    }
}
