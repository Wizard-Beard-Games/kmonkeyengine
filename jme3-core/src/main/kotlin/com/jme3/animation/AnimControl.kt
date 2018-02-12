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

import com.jme3.export.JmeExporter
import com.jme3.export.JmeImporter
import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.scene.Spatial
import com.jme3.scene.control.AbstractControl
import com.jme3.scene.control.Control
import com.jme3.util.TempVars
import com.jme3.util.clone.Cloner
import com.jme3.util.clone.JmeCloneable
import java.io.IOException
import java.util.*


/**
 * `AnimControl` is a Spatial control that allows manipulation
 * of skeletal animation.
 *
 * The control currently supports:
 * 1) Animation blending/transitions
 * 2) Multiple animation channels
 * 3) Multiple skins
 * 4) Animation event listeners
 * 5) Animated model cloning
 * 6) Animated model binary import/export
 * 7) Hardware skinning
 * 8) Attachments
 * 9) Add/remove skins
 *
 * Planned:
 * 1) Morph/Pose animation
 *
 * @author (kme) Ray Long
 * @author (jme) Kirill Vainer
 */
class AnimControl : AbstractControl, Cloneable, JmeCloneable {

    /**
     * Skeleton object must contain corresponding data for the targets' weight buffers.
     */
    /**
     * @return The skeleton of this `AnimControl`.
     */
    var skeleton: Skeleton? = null
        internal set
    /** only used for backward compatibility  */
    @Deprecated("")
    private var skeletonControl: SkeletonControl? = null
    /**
     * List of animations
     */
    internal var animationMap = HashMap<String, Animation>()
    /**
     * Animation channels
     */
    @Transient
    private var channels = ArrayList<AnimChannel>()
    /**
     * Animation event listeners
     */
    @Transient
    private var listeners = ArrayList<AnimEventListener>()

    /**
     * @return The number of channels that are controlled by this
     * `AnimControl`.
     *
     * @see AnimControl.createChannel
     */
    val numChannels: Int
        get() = channels.size

    /**
     * @return The names of all animations that this `AnimControl`
     * can play.
     */
    val animationNames: Collection<String>
        get() = animationMap.keys

    /**
     * Creates a new animation control for the given skeleton.
     * The method [AnimControl.setAnimations]
     * must be called after initialization in order for this class to be useful.
     *
     * @param skeleton The skeleton to animate
     */
    constructor(skeleton: Skeleton) {
        this.skeleton = skeleton
        reset()
    }

    /**
     * Serialization only. Do not use.
     */
    constructor() {}

    /**
     * Internal use only.
     */
    override fun cloneForSpatial(spatial: Spatial): Control {
        try {
//            val clone = super.clone() as AnimControl
            // TODO: determine proper supertype
            val clone = clone() as AnimControl
            clone.spatial = spatial
            clone.channels = ArrayList()
            clone.listeners = ArrayList()

            when {
                skeleton != null -> clone.skeleton = Skeleton(skeleton!!)
            }

            // animationMap is cloned, but only ClonableTracks will be cloned as they need a reference to a cloned spatial

            // animationMap is cloned, but only ClonableTracks will be cloned as they need a reference to a cloned spatial
            animationMap.forEach { (key, value) -> clone.animationMap[key] = value.cloneForSpatial(spatial) }

            return clone
        } catch (ex: CloneNotSupportedException) {
            throw AssertionError()
        }

    }

    override fun jmeClone(): Any {
        val clone = super.jmeClone() as AnimControl
        clone.channels = ArrayList()
        clone.listeners = ArrayList()

        return clone
    }

    override fun cloneFields(cloner: Cloner, original: Any) {
        super.cloneFields(cloner, original)

        this.skeleton = cloner.clone<Skeleton>(skeleton)

        // Note cloneForSpatial() never actually cloned the animation map... just its reference
        val newMap = HashMap<String, Animation>()

        // animationMap is cloned, but only ClonableTracks will be cloned as they need a reference to a cloned spatial
        animationMap.forEach { (key, value) -> newMap[key] = cloner.clone(value) }

        this.animationMap = newMap
    }

    /**
     * @param animations Set the animations that this `AnimControl`
     * will be capable of playing. The animations should be compatible
     * with the skeleton given in the constructor.
     */
    fun setAnimations(animations: HashMap<String, Animation>) {
        animationMap = animations
    }

    /**
     * Retrieve an animation from the list of animations.
     * @param name The name of the animation to retrieve.
     * @return The animation corresponding to the given name, or null, if no
     * such named animation exists.
     */
    fun getAnim(name: String): Animation {
        return animationMap[name]!!
    }

    /**
     * Adds an animation to be available for playing to this
     * `AnimControl`.
     * @param anim The animation to add.
     */
    fun addAnim(anim: Animation) {
        animationMap[anim.name!!] = anim
    }

    /**
     * Remove an animation so that it is no longer available for playing.
     * @param anim The animation to remove.
     */
    fun removeAnim(anim: Animation) {
        when {
            !animationMap.containsKey(anim.name) -> throw IllegalArgumentException("Given animation does not exist " + "in this AnimControl")
            else -> animationMap.remove(anim.name)
        }

    }

    /**
     * Create a new animation channel, by default assigned to all bones
     * in the skeleton.
     *
     * @return A new animation channel for this `AnimControl`.
     */
    fun createChannel(): AnimChannel {
        val channel = AnimChannel(this)
        channels.add(channel)
        return channel
    }

    /**
     * Return the animation channel at the given index.
     * @param index The index, starting at 0, to retrieve the `AnimChannel`.
     * @return The animation channel at the given index, or throws an exception
     * if the index is out of bounds.
     *
     * @throws IndexOutOfBoundsException If no channel exists at the given index.
     */
    fun getChannel(index: Int): AnimChannel {
        return channels[index]
    }

    /**
     * Clears all the channels that were created.
     *
     * @see AnimControl.createChannel
     */
    fun clearChannels() {
        channels.forEach { animChannel -> listeners.forEach { list -> list.onAnimCycleDone(this, animChannel, animChannel.animationName!!) } }
        channels.clear()
    }

    /**
     * Adds a new listener to receive animation related events.
     * @param listener The listener to add.
     */
    fun addListener(listener: AnimEventListener) {
        when {
            listeners.contains(listener) -> throw IllegalArgumentException("The given listener is already " + "registed at this AnimControl")
            else -> listeners.add(listener)
        }

    }

    /**
     * Removes the given listener from listening to events.
     * @param listener
     * @see AnimControl.addListener
     */
    fun removeListener(listener: AnimEventListener) {
        when {
            !listeners.remove(listener) -> throw IllegalArgumentException("The given listener is not " + "registed at this AnimControl")
            else -> {
            }
        }
    }

    /**
     * Clears all the listeners added to this `AnimControl`
     *
     * @see AnimControl.addListener
     */
    fun clearListeners() {
        listeners.clear()
    }

    internal fun notifyAnimChange(channel: AnimChannel, name: String) {
        listeners.indices.forEach { i -> listeners[i].onAnimChange(this, channel, name) }
    }

    internal fun notifyAnimCycleDone(channel: AnimChannel, name: String) {
        listeners.indices.forEach { i -> listeners[i].onAnimCycleDone(this, channel, name) }
    }

    /**
     * Internal use only.
     */
    override fun setSpatial(spatial: Spatial?) {
        when {
            spatial == null && skeletonControl != null -> this.spatial.removeControl(skeletonControl)
        }

        super.setSpatial(spatial)

                //Backward compatibility.

        //Backward compatibility.
        when {
            spatial != null && skeletonControl != null -> spatial.addControl(skeletonControl)
        }
    }

    internal fun reset() {
        when {
            skeleton != null -> skeleton!!.resetAndUpdate()
        }
    }

    /**
     * Returns the length of the given named animation.
     * @param name The name of the animation
     * @return The length of time, in seconds, of the named animation.
     */
    fun getAnimationLength(name: String): Float {
        val a = animationMap[name] ?: throw IllegalArgumentException("The animation " + name
                + " does not exist in this AnimControl")

        return a.length
    }

    /**
     * Internal use only.
     */
    override fun controlUpdate(tpf: Float) {
        when {
            skeleton != null -> skeleton!!.reset() // reset skeleton to bind pose
        }

        val vars = TempVars.get()
        for (i in channels.indices) {
            channels[i].update(tpf, vars)
        }
        vars.release()

        when {
            skeleton != null -> skeleton!!.updateWorldVectors()
        }
    }

    /**
     * Internal use only.
     */
    override fun controlRender(rm: RenderManager, vp: ViewPort) {}

    @Throws(IOException::class)
    override fun write(ex: JmeExporter) {
        super.write(ex)
        val oc = ex.getCapsule(this)
        oc.write(skeleton, "skeleton", null)
        oc.writeStringSavableMap(animationMap, "animations", null)
    }

    @Throws(IOException::class)
    override fun read(im: JmeImporter) {
        super.read(im)
        val `in` = im.getCapsule(this)
        skeleton = `in`.readSavable("skeleton", null) as Skeleton
        val loadedAnimationMap = `in`.readStringSavableMap("animations", null) as HashMap<String, Animation>
        animationMap = loadedAnimationMap

        when {
            im.formatVersion == 0 -> {
                // Changed for backward compatibility with j3o files generated
                // before the AnimControl/SkeletonControl split.

                // If we find a target mesh array the AnimControl creates the
                // SkeletonControl for old files and add it to the spatial.
                // When backward compatibility won't be needed anymore this can deleted
                val sav = `in`.readSavableArray("targets", null)
                when {
                    sav != null -> {
                        // NOTE: allow the targets to be gathered automatically
                        skeletonControl = SkeletonControl(skeleton)
                        spatial.addControl(skeletonControl)
                    }
                }
            }
        }
    }
}
