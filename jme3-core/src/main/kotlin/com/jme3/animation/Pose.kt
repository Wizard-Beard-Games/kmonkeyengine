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
import com.jme3.math.Vector3f
import com.jme3.util.BufferUtils
import java.io.IOException
import java.nio.FloatBuffer

/**
 * A pose is a list of offsets that say where a mesh vertices should be for this pose.
 *
 * @author (kme) Ray Long
 * @author (jme)
 */
class Pose : Savable, Cloneable {

    private var name: String? = null
    var targetMeshIndex: Int = 0
        private set

    private var offsets: Array<Vector3f>? = null
    private var indices: IntArray? = null

    @Transient
    private val tempVec = Vector3f()
    @Transient
    private val tempVec2 = Vector3f()

    constructor(name: String, targetMeshIndex: Int, offsets: Array<Vector3f>, indices: IntArray) {
        this.name = name
        this.targetMeshIndex = targetMeshIndex
        this.offsets = offsets
        this.indices = indices
    }

    /**
     * Serialization-only. Do not use.
     */
    constructor() {}


    /**
     * Applies the offsets of this pose to the vertex buffer given by the blend factor.
     *
     * @param blend Blend factor, 0 = no change to vertex buffer, 1 = apply full offsets
     * @param vertbuf Vertex buffer to apply this pose to
     */
    fun apply(blend: Float, vertbuf: FloatBuffer) {
        for (i in indices!!.indices) {
            val offset = offsets!![i]
            val vertIndex = indices!![i]

            tempVec.set(offset).multLocal(blend)

            // acquire vertex
            BufferUtils.populateFromBuffer(tempVec2, vertbuf, vertIndex)

            // add offset multiplied by factor
            tempVec2.addLocal(tempVec)

            // write modified vertex
            BufferUtils.setInBuffer(tempVec2, vertbuf, vertIndex)
        }
    }

    /**
     * This method creates a clone of the current object.
     * @return a clone of the current object
     */
    public override fun clone(): Pose {
        try {
            val result = super.clone() as Pose
            result.indices = this.indices!!.clone()
            if (this.offsets != null) {
//                result.offsets = arrayOfNulls(this.offsets!!.size)
                result.offsets = Array(size = this.offsets!!.size, init = { Vector3f() })
                for (i in this.offsets!!.indices) {
                    result.offsets!![i] = this.offsets!![i].clone()
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
        out.write(name, "name", "")
        out.write(targetMeshIndex, "meshIndex", -1)
        out.write(offsets, "offsets", null)
        out.write(indices, "indices", null)
    }

    @Throws(IOException::class)
    override fun read(i: JmeImporter) {
        val `in` = i.getCapsule(this)
        name = `in`.readString("name", "")
        targetMeshIndex = `in`.readInt("meshIndex", -1)
        indices = `in`.readIntArray("indices", null)

        val readSavableArray = `in`.readSavableArray("offsets", null)
        if (readSavableArray != null) {
//            offsets = arrayOfNulls(readSavableArray.size)
            offsets = Array(size = readSavableArray.size, init = { Vector3f() })
            System.arraycopy(readSavableArray, 0, offsets!!, 0, readSavableArray.size)
        }
    }
}
