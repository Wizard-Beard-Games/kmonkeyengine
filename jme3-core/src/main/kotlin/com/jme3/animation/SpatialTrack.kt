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

import com.jme3.export.InputCapsule
import com.jme3.export.JmeExporter
import com.jme3.export.JmeImporter
import com.jme3.export.OutputCapsule
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Spatial
import com.jme3.util.TempVars
import com.jme3.util.clone.Cloner
import com.jme3.util.clone.JmeCloneable

import java.io.IOException
import java.util.Arrays

/**
 * This class represents the track for spatial animation.
 *
 * @author (kme) Ray Long
 * @author (jme) Marcin Roguski (Kaelthas)
 */
class SpatialTrack : Track, JmeCloneable {

    /**
     * Translations of the track.
     */
    private var translations: CompactVector3Array? = null

    /**
     * Rotations of the track.
     */
    private var rotations: CompactQuaternionArray? = null

    /**
     * Scales of the track.
     */
    private var scales: CompactVector3Array? = null

    /**
     * The spatial to which this track applies.
     * Note that this is optional, if no spatial is defined, the AnimControl's Spatial will be used.
     */
    var trackSpatial: Spatial? = null

    /**
     * The times of the animations frames.
     */
    /**
     * @return the arrays of time for this track
     */
    var times: FloatArray? = null
        private set

    constructor() {}

    /**
     * Creates a spatial track for the given track data.
     *
     * @param times
     * a float array with the time of each frame
     * @param translations
     * the translation of the bone for each frame
     * @param rotations
     * the rotation of the bone for each frame
     * @param scales
     * the scale of the bone for each frame
     */
    constructor(times: FloatArray, translations: Array<Vector3f>,
                rotations: Array<Quaternion>, scales: Array<Vector3f>) {
        setKeyframes(times, translations, rotations, scales)
    }

    /**
     *
     * Modify the spatial which this track modifies.
     *
     * @param time
     * the current time of the animation
     */
    override fun setTime(time: Float, weight: Float, control: AnimControl, channel: AnimChannel, vars: TempVars) {
        var spatial = trackSpatial
        // use lastFrame so we never overflow the array
        when (spatial) {
            null -> spatial = control.spatial
        }

        val tempV = vars.vect1
        val tempS = vars.vect2
        val tempQ = vars.quat1
        val tempV2 = vars.vect3
        val tempS2 = vars.vect4
        val tempQ2 = vars.quat2

        val lastFrame = times!!.size - 1
        when {
            time < 0 || lastFrame == 0 -> {
                if (rotations != null)
                    rotations!![0, tempQ]
                if (translations != null)
                    translations!![0, tempV]
                if (scales != null) {
                    scales!![0, tempS]
                }
            }
            time >= times!![lastFrame] -> {
                if (rotations != null)
                    rotations!![lastFrame, tempQ]
                if (translations != null)
                    translations!![lastFrame, tempV]
                if (scales != null) {
                    scales!![lastFrame, tempS]
                }
            }
            else -> {
                var startFrame = 0
                var endFrame = 1
                // use lastFrame so we never overflow the array
                var i = 0
                while (i < lastFrame && times!![i] < time) {
                    startFrame = i
                    endFrame = i + 1
                    ++i
                }

                val blend = (time - times!![startFrame]) / (times!![endFrame] - times!![startFrame])

                when {
                    rotations != null -> rotations!![startFrame, tempQ]
                }
                when {
                    translations != null -> translations!![startFrame, tempV]
                }
                when {
                    scales != null -> scales!![startFrame, tempS]
                }
                when {
                    rotations != null -> rotations!![endFrame, tempQ2]
                }
                when {
                    translations != null -> translations!![endFrame, tempV2]
                }
                when {
                    scales != null -> scales!![endFrame, tempS2]
                }
                tempQ.nlerp(tempQ2, blend)
                tempV.interpolateLocal(tempV2, blend)
                tempS.interpolateLocal(tempS2, blend)
            }
        }

        when {
            translations != null -> spatial!!.localTranslation = tempV
        }
        when {
            rotations != null -> spatial!!.localRotation = tempQ
        }
        when {
            scales != null -> spatial!!.localScale = tempS
        }
    }

    /**
     * Set the translations, rotations and scales for this track.
     *
     * @param times
     * a float array with the time of each frame
     * @param translations
     * the translation of the bone for each frame
     * @param rotations
     * the rotation of the bone for each frame
     * @param scales
     * the scale of the bone for each frame
     */
    fun setKeyframes(times: FloatArray, translations: Array<Vector3f>?,
                     rotations: Array<Quaternion>?, scales: Array<Vector3f>?) {
        when {
            times.isEmpty() -> throw RuntimeException("BoneTrack with no keyframes!")
            else -> {
                this.times = times
                when {
                    translations != null -> {
                        assert(times.size == translations.size)
                        this.translations = CompactVector3Array()
                        this.translations!!.add(*translations)
                        this.translations!!.freeze()
                    }
                }
                when {
                    rotations != null -> {
                        assert(times.size == rotations.size)
                        this.rotations = CompactQuaternionArray()
                        this.rotations!!.add(*rotations)
                        this.rotations!!.freeze()
                    }
                }
                when {
                    scales != null -> {
                        assert(times.size == scales.size)
                        this.scales = CompactVector3Array()
                        this.scales!!.add(*scales)
                        this.scales!!.freeze()
                    }
                }
            }
        }

    }

    /**
     * @return the array of rotations of this track
     */
    fun getRotations(): Array<Quaternion>? {
        return if (rotations == null) null else rotations!!.toObjectArray()
    }

    /**
     * @return the array of scales for this track
     */
    fun getScales(): Array<Vector3f>? {
        return if (scales == null) null else scales!!.toObjectArray()
    }

    /**
     * @return the array of translations of this track
     */
    fun getTranslations(): Array<Vector3f>? {
        return if (translations == null) null else translations!!.toObjectArray()
    }

    /**
     * @return the length of the track
     */
    override fun getLength(): Float {
        return if (times == null) 0f else times!![times!!.size - 1] - times!![0]
    }

    override fun clone(): Track {
        return jmeClone() as Track
    }

    override fun getKeyFrameTimes(): FloatArray? {
        return times
    }

    override fun jmeClone(): Any {
        val tablesLength = times!!.size

        val timesCopy = this.times!!.clone()
        val translationsCopy = if (this.getTranslations() == null) null else Arrays.copyOf(this.getTranslations()!!, tablesLength)
        val rotationsCopy = if (this.getRotations() == null) null else Arrays.copyOf(this.getRotations()!!, tablesLength)
        val scalesCopy = if (this.getScales() == null) null else Arrays.copyOf(this.getScales()!!, tablesLength)

        //need to use the constructor here because of the final fields used in this class
        return SpatialTrack(timesCopy, translationsCopy!!, rotationsCopy!!, scalesCopy!!)
    }

    override fun cloneFields(cloner: Cloner, original: Any) {
        this.trackSpatial = cloner.clone<Spatial>((original as SpatialTrack).trackSpatial)
    }

    @Throws(IOException::class)
    override fun write(ex: JmeExporter) {
        val oc = ex.getCapsule(this)
        oc.write(translations, "translations", null)
        oc.write(rotations, "rotations", null)
        oc.write(times, "times", null)
        oc.write(scales, "scales", null)
        oc.write(trackSpatial, "trackSpatial", null)
    }

    @Throws(IOException::class)
    override fun read(im: JmeImporter) {
        val ic = im.getCapsule(this)
        translations = ic.readSavable("translations", null) as CompactVector3Array
        rotations = ic.readSavable("rotations", null) as CompactQuaternionArray
        times = ic.readFloatArray("times", null)
        scales = ic.readSavable("scales", null) as CompactVector3Array
        trackSpatial = ic.readSavable("trackSpatial", null) as Spatial
    }
}
