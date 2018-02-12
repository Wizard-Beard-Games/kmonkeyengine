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

import com.jme3.export.*
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.util.TempVars
import java.io.IOException


/**
 * Contains a list of transforms and times for each keyframe.
 *
 * @author (kme) Ray Long
 * @author (jme) Kirill Vainer
 */
class BoneTrack : Track {

    /**
     * Bone index in the skeleton which this track effects.
     */
    /**
     * @return the bone index of this bone track.
     */
    var targetBoneIndex: Int = 0
        private set

    /**
     * Transforms and times for track.
     */
    private var translations: CompactVector3Array? = null
    private var rotations: CompactQuaternionArray? = null
    private var scales: CompactVector3Array? = null
    /**
     * returns the arrays of time for this track
     * @return
     */
    var times: FloatArray? = null
        private set

    /**
     * Serialization-only. Do not use.
     */
    constructor() {}

    /**
     * Creates a bone track for the given bone index
     * @param targetBoneIndex the bone index
     * @param times a float array with the time of each frame
     * @param translations the translation of the bone for each frame
     * @param rotations the rotation of the bone for each frame
     */
    constructor(targetBoneIndex: Int, times: FloatArray, translations: Array<Vector3f>, rotations: Array<Quaternion>) {
        this.targetBoneIndex = targetBoneIndex
        this.setKeyframes(times, translations, rotations)
    }

    /**
     * Creates a bone track for the given bone index
     * @param targetBoneIndex the bone index
     * @param times a float array with the time of each frame
     * @param translations the translation of the bone for each frame
     * @param rotations the rotation of the bone for each frame
     * @param scales the scale of the bone for each frame
     */
    constructor(targetBoneIndex: Int, times: FloatArray, translations: Array<Vector3f>, rotations: Array<Quaternion>, scales: Array<Vector3f>) {
        this.targetBoneIndex = targetBoneIndex
        this.setKeyframes(times, translations, rotations, scales)
    }

    /**
     * Creates a bone track for the given bone index
     * @param targetBoneIndex the bone's index
     */
    constructor(targetBoneIndex: Int) {
        this.targetBoneIndex = targetBoneIndex
    }

    /**
     * return the array of rotations of this track
     * @return
     */
    fun getRotations(): Array<Quaternion>? {
        return rotations!!.toObjectArray()
    }

    /**
     * returns the array of scales for this track
     * @return
     */
    fun getScales(): Array<Vector3f>? {
        return if (scales == null) null else scales!!.toObjectArray()
    }

    /**
     * returns the array of translations of this track
     * @return
     */
    fun getTranslations(): Array<Vector3f>? {
        return translations!!.toObjectArray()
    }

    /**
     * Set the translations and rotations for this bone track
     *
     * @param times the time of each frame, measured from the start of the track
     * (not null, length&gt;0)
     * @param translations the translation of the bone for each frame (not null,
     * same length as times)
     * @param rotations the rotation of the bone for each frame (not null, same
     * length as times)
     */
    fun setKeyframes(times: FloatArray, translations: Array<Vector3f>?, rotations: Array<Quaternion>?) {
        when {
            times.isEmpty() -> throw RuntimeException("BoneTrack with no keyframes!")
            else -> {
                assert(translations != null)
                assert(times.size == translations!!.size)
                assert(rotations != null)
                assert(times.size == rotations!!.size)

                this.times = times
                this.translations = CompactVector3Array()
                this.translations!!.add(*translations)
                this.translations!!.freeze()
                this.rotations = CompactQuaternionArray()
                this.rotations!!.add(*rotations)
                this.rotations!!.freeze()
            }
        }

    }

    /**
     * Set the translations, rotations and scales for this bone track
     *
     * @param times the time of each frame, measured from the start of the track
     * (not null, length&gt;0)
     * @param translations the translation of the bone for each frame (not null,
     * same length as times)
     * @param rotations the rotation of the bone for each frame (not null, same
     * length as times)
     * @param scales the scale of the bone for each frame (ignored if null)
     */
    fun setKeyframes(times: FloatArray, translations: Array<Vector3f>, rotations: Array<Quaternion>, scales: Array<Vector3f>?) {
        this.setKeyframes(times, translations, rotations)
        when {
            scales != null -> {
                assert(times.size == scales.size)
                this.scales = CompactVector3Array()
                this.scales!!.add(*scales)
                this.scales!!.freeze()
            }
        }
    }

    /**
     *
     * Modify the bone which this track modifies in the skeleton to contain
     * the correct animation transforms for a given time.
     * The transforms can be interpolated in some method from the keyframes.
     *
     * @param time the current time of the animation
     * @param weight the weight of the animation
     * @param control
     * @param channel
     * @param vars
     */
    override fun setTime(time: Float, weight: Float, control: AnimControl, channel: AnimChannel, vars: TempVars) {
        val affectedBones = channel.affectedBones
        // use lastFrame so we never overflow the array

        //        if (weight != 1f) {
        //        } else {
        //            target.setAnimTransforms(tempV, tempQ, scales != null ? tempS : null);
        //        }
        when {
            affectedBones != null && !affectedBones.get(targetBoneIndex) -> return
            else -> {
                val target = control.skeleton!!.getBone(targetBoneIndex)

                val tempV = vars.vect1
                val tempS = vars.vect2
                val tempQ = vars.quat1
                val tempV2 = vars.vect3
                val tempS2 = vars.vect4
                val tempQ2 = vars.quat2

                val lastFrame = times!!.size - 1
                when {
                    time < 0 || lastFrame == 0 -> {
                        rotations!!.get(0, tempQ)
                        translations!!.get(0, tempV)
                        if (scales != null) {
                            scales!!.get(0, tempS)
                        }
                    }
                    time >= times!![lastFrame] -> {
                        rotations!!.get(lastFrame, tempQ)
                        translations!!.get(lastFrame, tempV)
                        if (scales != null) {
                            scales!!.get(lastFrame, tempS)
                        }
                    }
                    else -> {
                        var startFrame = 0
                        var endFrame = 1
                        // use lastFrame so we never overflow the array
                        var i: Int = 0
                        while (i < lastFrame && times!![i] < time) {
                            startFrame = i
                            endFrame = i + 1
                            i++
                        }

                        val blend = (time - times!![startFrame]) / (times!![endFrame] - times!![startFrame])

                        rotations!!.get(startFrame, tempQ)
                        translations!!.get(startFrame, tempV)
                        when {
                            scales != null -> scales!!.get(startFrame, tempS)
                        }
                        rotations!!.get(endFrame, tempQ2)
                        translations!!.get(endFrame, tempV2)
                        when {
                            scales != null -> scales!!.get(endFrame, tempS2)
                        }
                        tempQ.nlerp(tempQ2, blend)
                        tempV.interpolateLocal(tempV2, blend)
                        tempS.interpolateLocal(tempS2, blend)
                    }
                }

                //        if (weight != 1f) {
                target.blendAnimTransforms(tempV, tempQ, if (scales != null) tempS else null, weight)
                //        } else {
                //            target.setAnimTransforms(tempV, tempQ, scales != null ? tempS : null);
                //        }
            }
        }

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
    override fun clone(): BoneTrack {
        val tablesLength = times!!.size

        val times = this.times!!.clone()
        val sourceTranslations = this.getTranslations()
        val sourceRotations = this.getRotations()
        val sourceScales = this.getScales()

        val translations = arrayOfNulls<Vector3f>(tablesLength)
        val rotations = arrayOfNulls<Quaternion>(tablesLength)
        val scales = arrayOfNulls<Vector3f>(tablesLength)
        (0 until tablesLength).forEach { i ->
            translations[i] = sourceTranslations!![i].clone()
            rotations[i] = sourceRotations!![i].clone()
            scales[i] = if (sourceScales != null) sourceScales[i].clone() else Vector3f(1.0f, 1.0f, 1.0f)
        }

        // Need to use the constructor here because of the final fields used in this class
        return BoneTrack(targetBoneIndex, times, translations as Array<Vector3f>, rotations as Array<Quaternion>, scales as Array<Vector3f>)
    }

    @Throws(IOException::class)
    override fun write(ex: JmeExporter) {
        val oc = ex.getCapsule(this)
        oc.write(targetBoneIndex, "boneIndex", 0)
        oc.write(translations, "translations", null)
        oc.write(rotations, "rotations", null)
        oc.write(times, "times", null)
        oc.write(scales, "scales", null)
    }

    @Throws(IOException::class)
    override fun read(im: JmeImporter) {
        val ic = im.getCapsule(this)
        targetBoneIndex = ic.readInt("boneIndex", 0)

        translations = ic.readSavable("translations", null) as CompactVector3Array
        rotations = ic.readSavable("rotations", null) as CompactQuaternionArray
        times = ic.readFloatArray("times", null)
        scales = ic.readSavable("scales", null) as CompactVector3Array

        //Backward compatibility for old j3o files generated before revision 6807
        if (im.formatVersion == 0) {
            when (translations) {
                null -> {
                    val sav = ic.readSavableArray("translations", null)
                    when {
                        sav != null -> {
                            translations = CompactVector3Array()
                            val transCopy = Array(sav.size, init = { Vector3f() })
                            System.arraycopy(sav, 0, transCopy, 0, sav.size)
                            translations!!.add(*transCopy)
                            translations!!.freeze()
                        }
                    }
                }
            }
            when (rotations) {
                null -> {
                    val sav = ic.readSavableArray("rotations", null)
                    when {
                        sav != null -> {
                            rotations = CompactQuaternionArray()
                            val rotCopy = Array(sav.size, init = { Quaternion() })
                            System.arraycopy(sav, 0, rotCopy, 0, sav.size)
                            rotations!!.add(*rotCopy)
                            rotations!!.freeze()
                        }
                    }
                }
            }
        }
    }

    fun setTime(time: Float, weight: Float, control: AnimControl, channel: AnimChannel) {
        throw UnsupportedOperationException("Not supported yet.")
    }
}
