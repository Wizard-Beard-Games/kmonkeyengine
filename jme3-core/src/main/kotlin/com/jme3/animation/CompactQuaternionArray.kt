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
import com.jme3.math.Quaternion
import java.io.IOException

/**
 * Serialize and compress [Quaternion][] by indexing same values
 * It is converted to float[]
 * @author (kme) Ray Long
 * @author (jme) Lim, YongHoon
 */
class CompactQuaternionArray : CompactArray<Quaternion>, Savable {

    override val tupleSize: Int
        get() = 4

    override val elementClass: Class<Quaternion>
        get() = Quaternion::class.java

    /**
     * creates a compact Quaternion array
     */
    constructor() {}

    /**
     * creates a compact Quaternion array
     * @param dataArray the data array
     * @param index  the indices array
     */
    constructor(dataArray: FloatArray, index: IntArray) : super(dataArray, index) {}

    @Throws(IOException::class)
    override fun write(ex: JmeExporter) {
        serialize()
        val out = ex.getCapsule(this)
        out.write(array, "array", null)
        out.write(index, "index", null)
    }

    @Throws(IOException::class)
    override fun read(im: JmeImporter) {
        val `in` = im.getCapsule(this)
        array = `in`.readFloatArray("array", null)
        index = `in`.readIntArray("index", null)
    }

    override fun serialize(i: Int, store: Quaternion) {
        val j = i * tupleSize
        array!![j] = store.x
        array!![j + 1] = store.y
        array!![j + 2] = store.z
        array!![j + 3] = store.w
    }

    override fun deserialize(compactIndex: Int, store: Any?): Quaternion {
        val j = compactIndex * tupleSize
//        store.set(array!![j], array!![j + 1], array!![j + 2], array!![j + 3])
        (store as Quaternion).set(array!![j], array!![j + 1], array!![j + 2], array!![j + 3])
        return store
    }
}
