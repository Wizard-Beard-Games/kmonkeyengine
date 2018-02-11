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

import com.jme3.math.FastMath
import com.jme3.util.TempVars
import java.util.BitSet

/**
 * `AnimChannel` provides controls, such as play, pause,
 * fast forward, etc, for an animation. The animation
 * channel may influence the entire model or specific bones of the model's
 * skeleton. A single model may have multiple animation channels influencing
 * various parts of its body. For example, a character model may have an
 * animation channel for its feet, and another for its torso, and
 * the animations for each channel are controlled independently.
 *
 * @author (kme) Ray Long
 * @author (jme) Kirill Vainer
 */
class AnimChannel {

    /**
     * Returns the parent control of this AnimChannel.
     *
     * @return the parent control of this AnimChannel.
     * @see AnimControl
     */
    lateinit var control: AnimControl

    internal var affectedBones: BitSet? = null
        private set

    private var animation: Animation? = null
    private var blendFrom: Animation? = null
    private var time: Float = 0.toFloat()
    private var speed: Float = 0.toFloat()
    private var timeBlendFrom: Float = 0.toFloat()
    private var blendTime: Float = 0.toFloat()
    private var speedBlendFrom: Float = 0.toFloat()
    private var notified = false

    /**
     * @return The loop mode currently set for the animation. The loop mode
     * determines what will happen to the animation once it finishes
     * playing.
     *
     * For more information, see the LoopMode enum class.
     * @see LoopMode
     *
     * @see AnimChannel.setLoopMode
     */
    /**
     * @param loopMode Set the loop mode for the channel. The loop mode
     * determines what will happen to the animation once it finishes
     * playing.
     *
     * For more information, see the LoopMode enum class.
     * @see LoopMode
     */
    var loopMode: LoopMode? = null
    private var loopModeBlendFrom: LoopMode? = null

    private var blendAmount = 1f
    private var blendRate = 0f

    /**
     * @return The name of the currently playing animation, or null if
     * none is assigned.
     *
     * @see AnimChannel.setAnim
     */
    val animationName: String?
        get() = if (animation != null) animation!!.name else null

    /**
     * @return The length of the currently playing animation, or zero
     * if no animation is playing.
     *
     * @see AnimChannel.getTime
     */
    val animMaxTime: Float
        get() = if (animation != null) animation!!.length else 0f

    constructor() {

    }

    constructor(control: AnimControl) {
        this.control = control
    }

    /**
     * @return The speed that is assigned to the animation channel. The speed
     * is a scale value starting from 0.0, at 1.0 the animation will play
     * at its default speed.
     *
     * @see AnimChannel.setSpeed
     */
    fun getSpeed(): Float {
        return speed
    }

    /**
     * @param speed Set the speed of the animation channel. The speed
     * is a scale value starting from 0.0, at 1.0 the animation will play
     * at its default speed.
     */
    fun setSpeed(speed: Float) {
        this.speed = speed
        if (blendTime > 0) {
            this.speedBlendFrom = speed
            blendTime = Math.min(blendTime, animation!!.length / speed)
            blendRate = 1 / blendTime
        }
    }

    /**
     * @return The time of the currently playing animation. The time
     * starts at 0 and continues on until getAnimMaxTime().
     *
     * @see AnimChannel.setTime
     */
    fun getTime(): Float {
        return time
    }

    /**
     * @param time Set the time of the currently playing animation, the time
     * is clamped from 0 to [.getAnimMaxTime].
     */
    fun setTime(time: Float) {
        this.time = FastMath.clamp(time, 0f, animMaxTime)
    }

    /**
     * Set the current animation that is played by this AnimChannel.
     *
     *
     * This resets the time to zero, and optionally blends the animation
     * over `blendTime` seconds with the currently playing animation.
     * Notice that this method will reset the control's speed to 1.0.
     *
     * @param name The name of the animation to play
     * @param blendTime The blend time over which to blend the new animation
     * with the old one. If zero, then no blending will occur and the new
     * animation will be applied instantly.
     */
    @JvmOverloads
    fun setAnim(name: String?, blendTime: Float = DEFAULT_BLEND_TIME) {
        var blendTime = blendTime
        if (name == null)
            throw IllegalArgumentException("name cannot be null")

        if (blendTime < 0f)
            throw IllegalArgumentException("blendTime cannot be less than zero")

        val anim = control.animationMap[name] ?: throw IllegalArgumentException("Cannot find animation named: '$name'")

        control.notifyAnimChange(this, name)

        if (animation != null && blendTime > 0f) {
            this.blendTime = blendTime
            // activate blending
            blendTime = Math.min(blendTime, anim.length / speed)
            blendFrom = animation
            timeBlendFrom = time
            speedBlendFrom = speed
            loopModeBlendFrom = loopMode
            blendAmount = 0f
            blendRate = 1f / blendTime
        } else {
            blendFrom = null
        }

        animation = anim
        time = 0f
        speed = 1f
        loopMode = LoopMode.Loop
        notified = false
    }

    /**
     * Add all the bones of the model's skeleton to be
     * influenced by this animation channel.
     */
    fun addAllBones() {
        affectedBones = null
    }

    /**
     * Add a single bone to be influenced by this animation channel.
     */
    fun addBone(name: String) {
        addBone(control.skeleton!!.getBone(name))
    }

    /**
     * Add a single bone to be influenced by this animation channel.
     */
    fun addBone(bone: Bone?) {
        val boneIndex = control.skeleton!!.getBoneIndex(bone)
        if (affectedBones == null) {
            affectedBones = BitSet(control.skeleton!!.boneCount)
        }
        affectedBones!!.set(boneIndex)
    }

    /**
     * Add bones to be influenced by this animation channel starting from the
     * given bone name and going toward the root bone.
     */
    fun addToRootBone(name: String) {
        addToRootBone(control.skeleton!!.getBone(name))
    }

    /**
     * Add bones to be influenced by this animation channel starting from the
     * given bone and going toward the root bone.
     */
    fun addToRootBone(bone: Bone?) {
        var bone = bone
        addBone(bone)
        while (bone!!.parent != null) {
            bone = bone.parent
            addBone(bone)
        }
    }

    /**
     * Add bones to be influenced by this animation channel, starting
     * from the given named bone and going toward its children.
     */
    fun addFromRootBone(name: String) {
        addFromRootBone(control.skeleton!!.getBone(name))
    }

    /**
     * Add bones to be influenced by this animation channel, starting
     * from the given bone and going toward its children.
     */
    fun addFromRootBone(bone: Bone?) {
        addBone(bone)
        if (bone!!.children == null)
            return
        for (childBone in bone.children) {
            addBone(childBone)
            addFromRootBone(childBone)
        }
    }

    fun reset(rewind: Boolean) {
        if (rewind) {
            setTime(0f)
            if (control.skeleton != null) {
                control.skeleton!!.resetAndUpdate()
            } else {
                val vars = TempVars.get()
                update(0f, vars)
                vars.release()
            }
        }
        animation = null
        notified = false
    }

    internal fun update(tpf: Float, vars: TempVars) {
        if (animation == null)
            return

        if (blendFrom != null && blendAmount != 1.0f) {
            // The blendFrom anim is set, the actual animation
            // playing will be set
            //            blendFrom.setTime(timeBlendFrom, 1f, control, this, vars);
            blendFrom!!.setTime(timeBlendFrom, 1f - blendAmount, control, this, vars)

            timeBlendFrom += tpf * speedBlendFrom
            timeBlendFrom = AnimationUtils.clampWrapTime(timeBlendFrom,
                    blendFrom!!.length,
                    loopModeBlendFrom!!)
            if (timeBlendFrom < 0) {
                timeBlendFrom = -timeBlendFrom
                speedBlendFrom = -speedBlendFrom
            }

            blendAmount += tpf * blendRate
            if (blendAmount > 1f) {
                blendAmount = 1f
                blendFrom = null
            }
        }

        animation!!.setTime(time, blendAmount, control, this, vars)
        time += tpf * speed
        if (animation!!.length > 0) {
            if (!notified && (time >= animation!!.length || time < 0)) {
                if (loopMode == LoopMode.DontLoop) {
                    // Note that this flag has to be set before calling the notify
                    // since the notify may start a new animation and then unset
                    // the flag.
                    notified = true
                }
                control.notifyAnimCycleDone(this, animation!!.name!!)
            }
        }
        time = AnimationUtils.clampWrapTime(time, animation!!.length, loopMode!!)
        if (time < 0) {
            // Negative time indicates that speed should be inverted
            // (for cycle loop mode only)
            time = -time
            speed = -speed
        }
    }

    companion object {

        private val DEFAULT_BLEND_TIME = 0.15f
    }
}
/**
 * Set the current animation that is played by this AnimChannel.
 *
 *
 * See [.setAnim].
 * The blendTime argument by default is 150 milliseconds.
 *
 * @param name The name of the animation to play
 */
