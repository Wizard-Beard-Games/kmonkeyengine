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
import com.jme3.math.Quaternion
import com.jme3.math.Transform
import com.jme3.math.Vector3f

/**
 * A convenience class to easily setup a spatial keyframed animation
 * you can add some keyFrames for a given time or a given keyFrameIndex, for translation rotation and scale.
 * The animationHelper will then generate an appropriate SpatialAnimation by interpolating values between the keyFrames.
 * <br></br><br></br>
 * Usage is : <br></br>
 * - Create the AnimationHelper<br></br>
 * - add some keyFrames<br></br>
 * - call the buildAnimation() method that will return a new Animation<br></br>
 * - add the generated Animation to any existing AnimationControl<br></br>
 * <br></br><br></br>
 * Note that the first keyFrame (index 0) is defaulted with the identity transforms.
 * If you want to change that you have to replace this keyFrame with any transform you want.
 *
 * @author Nehon
 */
class AnimationFactory
/**
 * Creates and AnimationHelper
 * @param duration the desired duration for the resulting animation
 * @param name the name of the resulting animation
 * @param fps the number of frames per second for this animation (default is 30)
 */
@JvmOverloads constructor(
        /**
         * Animation duration in seconds
         */
        protected var duration: Float,
        /**
         * Name of the animation
         */
        protected var name: String,
        /**
         * frames per seconds
         */
        protected var fps: Int = 30) {
    /**
     * total number of frames
     */
    protected var totalFrames: Int = 0
    /**
     * time per frame
     */
    protected var tpf: Float = 0.toFloat()
    /**
     * Time array for this animation
     */
    protected var times: FloatArray
    /**
     * Translation array for this animation
     */
    protected var translations: Array<Vector3f>
    /**
     * rotation array for this animation
     */
    protected var rotations: Array<Quaternion>
    /**
     * scales array for this animation
     */
    protected var scales: Array<Vector3f>
    /**
     * The map of keyFrames to compute the animation. The key is the index of the frame
     */
    protected var keyFramesTranslation: Array<Vector3f>
    protected var keyFramesScale: Array<Vector3f>
    protected var keyFramesRotation: Array<Rotation>

    /**
     * enum to determine the type of interpolation
     */
    private enum class Type {

        Translation, Rotation, Scale
    }

    /**
     * Inner Rotation type class to kep track on a rotation Euler angle
     */
    protected inner class Rotation {

        /**
         * The rotation Quaternion
         */
        internal var rotation = Quaternion()
        /**
         * This rotation expressed in Euler angles
         */
        internal var eulerAngles = Vector3f()
        /**
         * the index of the parent key frame is this keyFrame is a splitted rotation
         */
        internal var masterKeyFrame = -1

        init {
            rotation.loadIdentity()
        }

        internal fun set(rot: Quaternion) {
            rotation.set(rot)
            val a = FloatArray(3)
            rotation.toAngles(a)
            eulerAngles.set(a[0], a[1], a[2])
        }

        internal operator fun set(x: Float, y: Float, z: Float) {
            val a = floatArrayOf(x, y, z)
            rotation.fromAngles(a)
            eulerAngles.set(x, y, z)
        }
    }

    init {
        totalFrames = (fps * duration).toInt() + 1
        tpf = 1 / fps.toFloat()
        times = FloatArray(totalFrames)
//        translations = arrayOfNulls(totalFrames)
        translations = Array(size = totalFrames, init = { Vector3f() })
        rotations = Array(size = totalFrames, init = { Quaternion() })
        scales = arrayOfNulls(totalFrames)
        keyFramesTranslation = arrayOfNulls(totalFrames)
        keyFramesTranslation[0] = Vector3f()
        keyFramesScale = arrayOfNulls(totalFrames)
        keyFramesScale[0] = Vector3f(1f, 1f, 1f)
        keyFramesRotation = arrayOfNulls(totalFrames)
        keyFramesRotation[0] = Rotation()

    }

    /**
     * Adds a key frame for the given Transform at the given time
     * @param time the time at which the keyFrame must be inserted
     * @param transform the transforms to use for this keyFrame
     */
    fun addTimeTransform(time: Float, transform: Transform) {
        addKeyFrameTransform((time / tpf).toInt(), transform)
    }

    /**
     * Adds a key frame for the given Transform at the given keyFrame index
     * @param keyFrameIndex the index at which the keyFrame must be inserted
     * @param transform the transforms to use for this keyFrame
     */
    fun addKeyFrameTransform(keyFrameIndex: Int, transform: Transform) {
        addKeyFrameTranslation(keyFrameIndex, transform.translation)
        addKeyFrameScale(keyFrameIndex, transform.scale)
        addKeyFrameRotation(keyFrameIndex, transform.rotation)
    }

    /**
     * Adds a key frame for the given translation at the given time
     * @param time the time at which the keyFrame must be inserted
     * @param translation the translation to use for this keyFrame
     */
    fun addTimeTranslation(time: Float, translation: Vector3f) {
        addKeyFrameTranslation((time / tpf).toInt(), translation)
    }

    /**
     * Adds a key frame for the given translation at the given keyFrame index
     * @param keyFrameIndex the index at which the keyFrame must be inserted
     * @param translation the translation to use for this keyFrame
     */
    fun addKeyFrameTranslation(keyFrameIndex: Int, translation: Vector3f) {
        val t = getTranslationForFrame(keyFrameIndex)
        t.set(translation)
    }

    /**
     * Adds a key frame for the given rotation at the given time<br></br>
     * This can't be used if the interpolated angle is higher than PI (180°)<br></br>
     * Use [.addTimeRotationAngles]  instead that uses Euler angles rotations.<br></br>     *
     * @param time the time at which the keyFrame must be inserted
     * @param rotation the rotation Quaternion to use for this keyFrame
     * @see .addTimeRotationAngles
     */
    fun addTimeRotation(time: Float, rotation: Quaternion) {
        addKeyFrameRotation((time / tpf).toInt(), rotation)
    }

    /**
     * Adds a key frame for the given rotation at the given keyFrame index<br></br>
     * This can't be used if the interpolated angle is higher than PI (180°)<br></br>
     * Use [.addKeyFrameRotationAngles] instead that uses Euler angles rotations.
     * @param keyFrameIndex the index at which the keyFrame must be inserted
     * @param rotation the rotation Quaternion to use for this keyFrame
     * @see .addKeyFrameRotationAngles
     */
    fun addKeyFrameRotation(keyFrameIndex: Int, rotation: Quaternion) {
        val r = getRotationForFrame(keyFrameIndex)
        r.set(rotation)
    }

    /**
     * Adds a key frame for the given rotation at the given time.<br></br>
     * Rotation is expressed by Euler angles values in radians.<br></br>
     * Note that the generated rotation will be stored as a quaternion and interpolated using a spherical linear interpolation (slerp)<br></br>
     * Hence, this method may create intermediate keyFrames if the interpolation angle is higher than PI to ensure continuity in animation<br></br>
     *
     * @param time the time at which the keyFrame must be inserted
     * @param x the rotation around the x axis (aka yaw) in radians
     * @param y the rotation around the y axis (aka roll) in radians
     * @param z the rotation around the z axis (aka pitch) in radians
     */
    fun addTimeRotationAngles(time: Float, x: Float, y: Float, z: Float) {
        addKeyFrameRotationAngles((time / tpf).toInt(), x, y, z)
    }

    /**
     * Adds a key frame for the given rotation at the given key frame index.<br></br>
     * Rotation is expressed by Euler angles values in radians.<br></br>
     * Note that the generated rotation will be stored as a quaternion and interpolated using a spherical linear interpolation (slerp)<br></br>
     * Hence, this method may create intermediate keyFrames if the interpolation angle is higher than PI to ensure continuity in animation<br></br>
     *
     * @param keyFrameIndex the index at which the keyFrame must be inserted
     * @param x the rotation around the x axis (aka yaw) in radians
     * @param y the rotation around the y axis (aka roll) in radians
     * @param z the rotation around the z axis (aka pitch) in radians
     */
    fun addKeyFrameRotationAngles(keyFrameIndex: Int, x: Float, y: Float, z: Float) {
        val r = getRotationForFrame(keyFrameIndex)
        r[x, y] = z

        // if the delta of euler angles is higher than PI, we create intermediate keyframes
        // since we are using quaternions and slerp for rotation interpolation, we cannot interpolate over an angle higher than PI
        val prev = getPreviousKeyFrame(keyFrameIndex, keyFramesRotation)
        if (prev != -1) {
            //previous rotation keyframe
            val prevRot = keyFramesRotation[prev]
            //the maximum delta angle (x,y or z)
            var delta = Math.max(Math.abs(x - prevRot.eulerAngles.x), Math.abs(y - prevRot.eulerAngles.y))
            delta = Math.max(delta, Math.abs(z - prevRot.eulerAngles.z))
            //if delta > PI we have to create intermediates key frames
            if (delta >= FastMath.PI) {
                //frames delta
                val dF = keyFrameIndex - prev
                //angle per frame for x,y ,z
                val dXAngle = (x - prevRot.eulerAngles.x) / dF.toFloat()
                val dYAngle = (y - prevRot.eulerAngles.y) / dF.toFloat()
                val dZAngle = (z - prevRot.eulerAngles.z) / dF.toFloat()

                // the keyFrame step
                val keyStep = (dF.toFloat() / delta * EULER_STEP).toInt()
                // the current keyFrame
                var cursor = prev + keyStep
                while (cursor < keyFrameIndex) {
                    //for each step we create a new rotation by interpolating the angles
                    val dr = getRotationForFrame(cursor)
                    dr.masterKeyFrame = keyFrameIndex
                    dr[prevRot.eulerAngles.x + cursor * dXAngle, prevRot.eulerAngles.y + cursor * dYAngle] = prevRot.eulerAngles.z + cursor * dZAngle
                    cursor += keyStep
                }

            }
        }

    }

    /**
     * Adds a key frame for the given scale at the given time
     * @param time the time at which the keyFrame must be inserted
     * @param scale the scale to use for this keyFrame
     */
    fun addTimeScale(time: Float, scale: Vector3f) {
        addKeyFrameScale((time / tpf).toInt(), scale)
    }

    /**
     * Adds a key frame for the given scale at the given keyFrame index
     * @param keyFrameIndex the index at which the keyFrame must be inserted
     * @param scale the scale to use for this keyFrame
     */
    fun addKeyFrameScale(keyFrameIndex: Int, scale: Vector3f) {
        val s = getScaleForFrame(keyFrameIndex)
        s.set(scale)
    }

    /**
     * returns the translation for a given frame index
     * creates the translation if it doesn't exists
     * @param keyFrameIndex index
     * @return the translation
     */
    private fun getTranslationForFrame(keyFrameIndex: Int): Vector3f {
        if (keyFrameIndex < 0 || keyFrameIndex > totalFrames) {
            throw ArrayIndexOutOfBoundsException("keyFrameIndex must be between 0 and $totalFrames (received $keyFrameIndex)")
        }
        var v: Vector3f? = keyFramesTranslation[keyFrameIndex]
        if (v == null) {
            v = Vector3f()
            keyFramesTranslation[keyFrameIndex] = v
        }
        return v
    }

    /**
     * returns the scale for a given frame index
     * creates the scale if it doesn't exists
     * @param keyFrameIndex index
     * @return the scale
     */
    private fun getScaleForFrame(keyFrameIndex: Int): Vector3f {
        if (keyFrameIndex < 0 || keyFrameIndex > totalFrames) {
            throw ArrayIndexOutOfBoundsException("keyFrameIndex must be between 0 and $totalFrames (received $keyFrameIndex)")
        }
        var v: Vector3f? = keyFramesScale[keyFrameIndex]
        if (v == null) {
            v = Vector3f()
            keyFramesScale[keyFrameIndex] = v
        }
        return v
    }

    /**
     * returns the rotation for a given frame index
     * creates the rotation if it doesn't exists
     * @param keyFrameIndex index
     * @return the rotation
     */
    private fun getRotationForFrame(keyFrameIndex: Int): Rotation {
        if (keyFrameIndex < 0 || keyFrameIndex > totalFrames) {
            throw ArrayIndexOutOfBoundsException("keyFrameIndex must be between 0 and $totalFrames (received $keyFrameIndex)")
        }
        var v: Rotation? = keyFramesRotation[keyFrameIndex]
        if (v == null) {
            v = Rotation()
            keyFramesRotation[keyFrameIndex] = v
        }
        return v
    }

    /**
     * Creates an Animation based on the keyFrames previously added to the helper.
     * @return the generated animation
     */
    fun buildAnimation(): Animation {
        interpolateTime()
        interpolate(keyFramesTranslation, Type.Translation)
        interpolate(keyFramesRotation, Type.Rotation)
        interpolate(keyFramesScale, Type.Scale)

        val spatialTrack = SpatialTrack(times, translations, rotations, scales)

        //creating the animation
        val spatialAnimation = Animation(name, duration)
        spatialAnimation.setTracks(arrayOf<SpatialTrack>(spatialTrack))

        return spatialAnimation
    }

    /**
     * interpolates time values
     */
    private fun interpolateTime() {
        for (i in 0 until totalFrames) {
            times[i] = i * tpf
        }
    }

    /**
     * Interpolates over the key frames for the given keyFrame array and the given type of transform
     * @param keyFrames the keyFrames array
     * @param type the type of transforms
     */
    private fun interpolate(keyFrames: Array<Any>, type: Type) {
        var i = 0
        while (i < totalFrames) {
            //fetching the next keyFrame index transform in the array
            val key = getNextKeyFrame(i, keyFrames)
            if (key != -1) {
                //computing the frame span to interpolate over
                val span = key - i
                //interating over the frames
                for (j in i..key) {
                    // computing interpolation value
                    val `val` = (j - i).toFloat() / span.toFloat()
                    //interpolationg depending on the transform type
                    when (type) {
                        AnimationFactory.Type.Translation -> translations[j] = FastMath.interpolateLinear(`val`, keyFrames[i] as Vector3f, keyFrames[key] as Vector3f)
                        AnimationFactory.Type.Rotation -> {
                            val rot = Quaternion()
                            rotations[j] = rot.slerp((keyFrames[i] as Rotation).rotation, (keyFrames[key] as Rotation).rotation, `val`)
                        }
                        AnimationFactory.Type.Scale -> scales[j] = FastMath.interpolateLinear(`val`, keyFrames[i] as Vector3f, keyFrames[key] as Vector3f)
                    }
                }
                //jumping to the next keyFrame
                i = key
            } else {
                //No more key frame, filling the array witht he last transform computed.
                for (j in i until totalFrames) {

                    when (type) {
                        AnimationFactory.Type.Translation -> translations[j] = (keyFrames[i] as Vector3f).clone()
                        AnimationFactory.Type.Rotation -> rotations[j] = (keyFrames[i] as Rotation).rotation.clone()
                        AnimationFactory.Type.Scale -> scales[j] = (keyFrames[i] as Vector3f).clone()
                    }
                }
                //we're done
                i = totalFrames
            }
        }
    }

    /**
     * Get the index of the next keyFrame that as a transform
     * @param index the start index
     * @param keyFrames the keyFrames array
     * @return the index of the next keyFrame
     */
    private fun getNextKeyFrame(index: Int, keyFrames: Array<Any>): Int {
        for (i in index + 1 until totalFrames) {
            if (keyFrames[i] != null) {
                return i
            }
        }
        return -1
    }

    /**
     * Get the index of the previous keyFrame that as a transform
     * @param index the start index
     * @param keyFrames the keyFrames array
     * @return the index of the previous keyFrame
     */
    private fun getPreviousKeyFrame(index: Int, keyFrames: Array<Any>): Int {
        for (i in index - 1 downTo 0) {
            if (keyFrames[i] != null) {
                return i
            }
        }
        return -1
    }

    companion object {

        /**
         * step for splitting rotation that have a n angle above PI/2
         */
        private val EULER_STEP = FastMath.QUARTER_PI * 3
    }
}
/**
 * Creates and AnimationHelper
 * @param duration the desired duration for the resulting animation
 * @param name the name of the resulting animation
 */
