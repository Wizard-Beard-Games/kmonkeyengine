/*
 * Copyright (c) 2009-2018 jMonkeyEngine
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

import java.lang.reflect.Array
import java.util.HashMap

/**
 * Object is indexed and stored in primitive float[]
 * @author (kme) Ray Long
 * @author (jme) Lim, YongHoon
 * @param <T>
</T> */
abstract class CompactArray<T> {

    private val indexPool = HashMap<T, Int>()
    protected var index: IntArray? = null
    protected var array: FloatArray? = null
    private var invalid: Boolean = false

    /**
     * return a float array of serialized data
     * @return
     */
    val serializedData: FloatArray?
        get() {
            serialize()
            return array
        }

    /**
     * @return compacted array's primitive size
     */
    protected val serializedSize: Int
        get() = Array.getLength(serializedData)

    /**
     * @return uncompressed object size
     */
    val totalObjectSize: Int
        get() {
            assert(serializedSize % tupleSize == 0)
            return if (index != null) index!!.size else serializedSize / tupleSize
        }

    /**
     * @return compressed object size
     */
    val compactObjectSize: Int
        get() {
            assert(serializedSize % tupleSize == 0)
            return serializedSize / tupleSize
        }

    /**
     * serialized size of one object element
     */
    protected abstract val tupleSize: Int

    protected abstract val elementClass: Class<T>

    /**
     * Creates a compact array
     */
    constructor() {}

    /**
     * create array using serialized data
     * @param compressedArray
     * @param index
     */
    constructor(compressedArray: FloatArray, index: IntArray) {
        this.array = compressedArray
        this.index = index
    }

    /**
     * Add objects.
     * They are serialized automatically when get() method is called.
     * @param objArray
     */
    fun add(vararg objArray: T) {//index = Arrays.copyOf(index, base+objArray.length);
        //index = Arrays.copyOf(index, base+objArray.length);
        when {
            objArray.isEmpty() -> return
            else -> {
                invalid = true
                var base = 0
                when (index) {
                    null -> index = IntArray(objArray.size)
                    else -> when {
                        indexPool.isEmpty() -> throw RuntimeException("Internal is already fixed")
                    //index = Arrays.copyOf(index, base+objArray.length);
                        else -> {
                            base = index!!.size

                            val tmp = IntArray(base + objArray.size)
                            System.arraycopy(index!!, 0, tmp, 0, index!!.size)
                            index = tmp
                            //index = Arrays.copyOf(index, base+objArray.length);
                        }
                    }
                }
                objArray.indices.forEach { j ->
                    val obj = objArray[j]
                    when (obj) {
                        null -> index!![base + j] = -1
                        else -> {
                            var i: Int? = indexPool[obj]
                            when (i) {
                                null -> {
                                    i = indexPool.size
                                    indexPool[obj] = i
                                }
                            }
                            index!![base + j] = i!!
                        }
                    }
                }
            }
        }
    }

    /**
     * release objects.
     * add() method call is not allowed anymore.
     */
    fun freeze() {
        serialize()
        indexPool.clear()
    }

    /**
     * @param index
     * @param value
     */
    operator fun set(index: Int, value: T) {
        val j = getCompactIndex(index)
        serialize(j, value)
    }

    /**
     * returns the object for the given index
     * @param index the index
     * @param store an object to store the result
     * @return
     */
    operator fun get(index: Int, store: T): T {
        serialize()
        val j = getCompactIndex(index)
        return deserialize(j, store)
    }

    /**
     * serialize this compact array
     */
    fun serialize() {
        when {
            invalid -> {
                val newSize = indexPool.size * tupleSize
                when {
                    array == null || Array.getLength(array) < newSize -> {
                        array = ensureCapacity(array, newSize)
                        indexPool.forEach { (obj, i) -> serialize(i, obj) }
                    }
                }
                invalid = false
            }
        }
    }

    /**
     * Ensure the capacity for the given array and the given size
     * @param arr the array
     * @param size the size
     * @return
     */
    protected fun ensureCapacity(arr: FloatArray?, size: Int): FloatArray {
        return when {
            arr == null -> FloatArray(size)
            arr.size >= size -> arr
            else -> {
                val tmp = FloatArray(size)
                System.arraycopy(arr, 0, tmp, 0, arr.size)
                tmp
                //return Arrays.copyOf(arr, size);
            }
        }
    }

    /**
     * Return an array of indices for the given objects
     * @param objArray
     * @return
     */
    fun getIndex(vararg objArray: T): IntArray {
        val index = IntArray(objArray.size)
        index.indices.forEach { i ->
            val obj = objArray[i]
            index[i] = if (obj != null) indexPool[obj]!! else -1
        }
        return index
    }

    /**
     * returns the corresponding index in the compact array
     * @param objIndex
     * @return object index in the compacted object array
     */
    fun getCompactIndex(objIndex: Int): Int {
        return if (index != null) index!![objIndex] else objIndex
    }

    /**
     * decompress and return object array
     * @return decompress and return object array
     */
     fun <T> toObjectArray(): kotlin.Array<T>? {
        try {
            val compactArr = Array.newInstance(elementClass, serializedSize / tupleSize) as kotlin.Array<T>
            compactArr.indices.forEach { i ->
                compactArr[i] = elementClass.newInstance() as T
                deserialize(i, compactArr[i])
            }

            val objArr = Array.newInstance(elementClass, totalObjectSize) as kotlin.Array<T>
            objArr.indices.forEach { i ->
                val compactIndex = getCompactIndex(i)
                objArr[i] = compactArr[compactIndex]
            }
            return objArr
        } catch (e: Exception) {
            return null
        }

    }

    // TODO: is this what we want?  It doesn't appear correct - let's review after more of the engine is converted
//    protected abstract fun <T> deserialize(compactIndex: Int, store: T)

    /**
     * serialize object
     * @param compactIndex compacted object index
     * @param store
     */
    protected abstract fun serialize(compactIndex: Int, store: T)

    /**
     * deserialize object
     * @param compactIndex compacted object index
     * @param store
     */
//    protected abstract fun deserialize(compactIndex: Int, store: T): T
    protected abstract fun deserialize(compactIndex: Int, store: Any?): T
}
