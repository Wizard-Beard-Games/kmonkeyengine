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
package com.jme3.util

import java.util.*

/**
 *
 * Provides a list with similar modification semantics to java.util.concurrent's
 * CopyOnWriteArrayList except that it is not concurrent and also provides
 * direct access to the current array.  This List allows modification of the
 * contents while iterating as any iterators will be looking at a snapshot of
 * the list at the time they were created.  Similarly, access the raw internal
 * array is only presenting a snap shot and so can be safely iterated while
 * the list is changing.
 *
 *
 * All modifications, including set() operations will cause a copy of the
 * data to be created that replaces the old version.  Because this list is
 * not designed for threading concurrency it further optimizes the "many modifications"
 * case by buffering them as a normal ArrayList until the next time the contents
 * are accessed.
 *
 *
 * Normal list modification performance should be equal to ArrayList in a
 * many situations and always better than CopyOnWriteArrayList.  Optimum usage
 * is when modifications are done infrequently or in batches... as is often the
 * case in a scene graph.  Read operations perform superior to all other methods
 * as the array can be accessed directly.
 *
 *
 * Important caveats over normal java.util.Lists:
 *
 *  * Even though this class supports modifying the list, the subList() method
 * returns a read-only list.  This technically breaks the List contract.
 *  * The ListIterators returned by this class only support the remove()
 * modification method.  add() and set() are not supported on the iterator.
 * Even after ListIterator.remove() or Iterator.remove() is called, this change
 * is not reflected in the iterator instance as it is still refering to its
 * original snapshot.
 *
 *
 * @author (kme) Ray Long
 * @version   $Revision$
 * @author    (jme) Paul Speed
 */
open class SafeArrayList<E> : List<E>, Cloneable {

    // Implementing List directly to avoid accidentally acquiring
    // incorrect or non-optimal behavior from AbstractList.  For
    // example, the default iterator() method will not work for
    // this list.

    // Note: given the particular use-cases this was intended,
    //       it would make sense to nerf the public mutators and
    //       make this publicly act like a read-only list.
    //       SafeArrayList-specific methods could then be exposed
    //       for the classes like Node and Spatial to use to manage
    //       the list.  This was the callers couldn't remove a child
    //       without it being detached properly, for example.

    private var elementType: Class<E>? = null
    private var buffer: List<E>? = null
    private var backingArray: Array<E>? = null
    override var size = 0

    /**
     * Returns a current snapshot of this List's backing array that
     * is guaranteed not to change through further List manipulation.
     * Changes to this array may or may not be reflected in the list and
     * should be avoided.
     */
    // Only keep the array or the buffer but never both at
    // the same time.  1) it saves space, 2) it keeps the rest
    // of the code safer.
    val array: Array<E>
        get() {
            if (backingArray != null)
                return backingArray!!

            if (buffer == null) {
                backingArray = createArray(0)
            } else {
                backingArray = buffer!!.toTypedArray()
                buffer = null
            }
            return backingArray!!
        }

    constructor(elementType: Class<E>) {
        this.elementType = elementType
    }

    constructor(elementType: Class<E>, capacity: Int) {
        this.elementType = elementType
        this.buffer = ArrayList(capacity)
    }

    constructor(elementType: Class<E>, collection: Collection<E>) {
        this.elementType = elementType
        this.buffer = ArrayList(collection)
        this.size = buffer!!.size
    }

    public override fun clone(): SafeArrayList<E> {
        try {
            val clone = super.clone() as SafeArrayList<E>

            // Clone whichever backing store is currently active
            if (backingArray != null) {
                clone.backingArray = backingArray!!.clone()
            }
            if (buffer != null) {
                clone.buffer = (buffer as ArrayList<E>).clone() as List<E>
            }

            return clone
        } catch (e: CloneNotSupportedException) {
            throw AssertionError()
        }

    }

    protected fun <T> createArray(type: Class<T>?, size: Int): Array<T> {
        return java.lang.reflect.Array.newInstance(type, size) as Array<T>
    }

    protected fun createArray(size: Int): Array<E> {
        return createArray(elementType, size)
    }

    protected fun getBuffer(): MutableList<E> {
        if (buffer != null)
            return buffer

        if (backingArray == null) {
            buffer = ArrayList()
        } else {
            // Only keep the array or the buffer but never both at
            // the same time.  1) it saves space, 2) it keeps the rest
            // of the code safer.
            buffer = ArrayList(Arrays.asList(*backingArray!!))
            backingArray = null
        }
        return buffer
    }

    override fun size(): Int {
        return size
    }

    override fun isEmpty(): Boolean {
        return size == 0
    }

    override operator fun contains(o: Any): Boolean {
        return indexOf(o) >= 0
    }

    override fun iterator(): Iterator<E> {
        return listIterator()
    }

    override fun toArray(): Array<Any> {
        return array
    }

    override fun <T> toArray(a: Array<T>): Array<T> {

        val array = array
        if (a.size < array.size) {
            return Arrays.copyOf<Any, E>(array, array.size, a.javaClass) as Array<T>
        }

        System.arraycopy(array, 0, a, 0, array.size)

        if (a.size > array.size) {
            a[array.size] = null
        }

        return a
    }

    override fun add(e: E): Boolean {
        val result = getBuffer().add(e)
        size = getBuffer().size
        return result
    }

    override fun remove(o: Any): Boolean {
        val result = getBuffer().remove(o)
        size = getBuffer().size
        return result
    }

    override fun containsAll(c: Collection<*>): Boolean {
        return Arrays.asList(*array).containsAll(c)
    }

    override fun addAll(c: Collection<E>): Boolean {
        val result = getBuffer().addAll(c)
        size = getBuffer().size
        return result
    }

    override fun addAll(index: Int, c: Collection<E>): Boolean {
        val result = getBuffer().addAll(index, c)
        size = getBuffer().size
        return result
    }

    override fun removeAll(c: Collection<*>): Boolean {
        val result = getBuffer().removeAll(c)
        size = getBuffer().size
        return result
    }

    override fun retainAll(c: Collection<*>): Boolean {
        val result = getBuffer().retainAll(c)
        size = getBuffer().size
        return result
    }

    override fun clear() {
        getBuffer().clear()
        size = 0
    }

    override fun equals(o: Any?): Boolean {

        if (o === this) {
            return true
        } else if (o is SafeArrayList<*>) {

            val targetArray = o.array
            val array = array

            return Arrays.equals(targetArray, array)
        } else if (o !is List<*>) {//covers null too
            return false
        }

        val other = o as List<*>?
        val i1 = iterator()
        val i2 = other!!.iterator()
        while (i1.hasNext() && i2.hasNext()) {
            val o1 = i1.next()
            val o2 = i2.next()
            if (o1 === o2)
                continue
            if (o1 == null || o1 != o2)
                return false
        }
        return !(i1.hasNext() || i2.hasNext())
    }

    override fun hashCode(): Int {
        // Exactly the hash code described in the List interface, basically
        val array = array
        var result = 1
        for (e in array) {
            result = 31 * result + (e?.hashCode() ?: 0)
        }
        return result
    }

    override fun get(index: Int): E {
        if (backingArray != null)
            return backingArray!![index]
        if (buffer != null)
            return buffer!![index]
        throw IndexOutOfBoundsException("Index:$index, Size:0")
    }

    operator fun set(index: Int, element: E): E {
        return getBuffer().set(index, element)
    }

    fun add(index: Int, element: E) {
        getBuffer().add(index, element)
        size = getBuffer().size
    }

    fun remove(index: Int): E {
        val result = getBuffer().removeAt(index)
        size = getBuffer().size
        return result
    }

    fun indexOf(o: Any): Int {
        val array = array
        for (i in array.indices) {
            val element = array[i]
            if (element === o) {
                return i
            }
            if (element != null && element == o) {
                return i
            }
        }
        return -1
    }

    fun lastIndexOf(o: Any): Int {
        val array = array
        for (i in array.indices.reversed()) {
            val element = array[i]
            if (element === o) {
                return i
            }
            if (element != null && element == o) {
                return i
            }
        }
        return -1
    }

    override fun listIterator(): ListIterator<E> {
        return ArrayIterator(array, 0)
    }

    override fun listIterator(index: Int): ListIterator<E> {
        return ArrayIterator(array, index)
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<E> {

        // So far JME doesn't use subList that I can see so I'm nerfing it.
        val raw = Arrays.asList(*array).subList(fromIndex, toIndex)
        return Collections.unmodifiableList(raw)
    }

    override fun toString(): String {

        val array = array
        if (array.size == 0) {
            return "[]"
        }

        val sb = StringBuilder()
        sb.append('[')
        for (i in array.indices) {
            if (i > 0)
                sb.append(", ")
            val e = array[i]
            sb.append(if (e === this) "(this Collection)" else e)
        }
        sb.append(']')
        return sb.toString()
    }

    protected inner class ArrayIterator<E> protected constructor(private val array: Array<E>, private var next: Int) : ListIterator<E> {
        private var lastReturned: Int = 0

        init {
            this.lastReturned = -1
        }

        override fun hasNext(): Boolean {
            return next != array.size
        }

        override fun next(): E {
            if (!hasNext())
                throw NoSuchElementException()
            lastReturned = next++
            return array[lastReturned]
        }

        override fun hasPrevious(): Boolean {
            return next != 0
        }

        override fun previous(): E {
            if (!hasPrevious())
                throw NoSuchElementException()
            lastReturned = --next
            return array[lastReturned]
        }

        override fun nextIndex(): Int {
            return next
        }

        override fun previousIndex(): Int {
            return next - 1
        }

        override fun remove() {
            // This operation is not so easy to do but we will fake it.
            // The issue is that the backing list could be completely
            // different than the one this iterator is a snapshot of.
            // We'll just remove(element) which in most cases will be
            // correct.  If the list had earlier .equals() equivalent
            // elements then we'll remove one of those instead.  Either
            // way, none of those changes are reflected in this iterator.
            this@SafeArrayList.remove(array[lastReturned])
        }

        override fun set(e: E) {
            throw UnsupportedOperationException()
        }

        override fun add(e: E) {
            throw UnsupportedOperationException()
        }
    }
}
