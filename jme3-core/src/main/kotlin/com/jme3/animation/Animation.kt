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
import com.jme3.scene.Spatial
import com.jme3.util.SafeArrayList
import com.jme3.util.TempVars
import com.jme3.util.clone.Cloner
import com.jme3.util.clone.JmeCloneable
import java.io.IOException

/**
 * The animation class updates the animation target with the tracks of a given type.
 *
 * @author (kme) Ray Long
 * @author (jme) Kirill Vainer, Marcin Roguski (Kaelthas)
 */
class Animation : Savable, Cloneable, JmeCloneable {

    /**
     * The name of the animation.
     */
    /**
     * The name of the bone animation
     * @return name of the bone animation
     */
    /**
     * Sets the name of the animation
     *
     * @param name
     */
    var name: String? = null
    /**
     * The length of the animation.
     */
    /**
     * Returns the length in seconds of this animation
     *
     * @return the length in seconds of this animation
     */
    /**
     * Set the length of the animation
     *
     * @param length
     */
    var length: Float = 0.toFloat()
    /**
     * The tracks of the animation.
     */
    private var tracks: SafeArrayList<Track>? = SafeArrayList(Track::class.java)

    /**
     * Serialization-only. Do not use.
     */
    constructor() {}

    /**
     * Creates a new `Animation` with the given name and length.
     *
     * @param name The name of the animation.
     * @param length Length in seconds of the animation.
     */
    constructor(name: String, length: Float) {
        this.name = name
        this.length = length
    }

    /**
     * This method sets the current time of the animation.
     * This method behaves differently for every known track type.
     * Override this method if you have your own type of track.
     *
     * @param time the time of the animation
     * @param blendAmount the blend amount factor
     * @param control the animation control
     * @param channel the animation channel
     */
    internal fun setTime(time: Float, blendAmount: Float, control: AnimControl, channel: AnimChannel, vars: TempVars) {
        when (tracks) {
            null -> return
            else -> tracks!!.forEach { track -> track.setTime(time, blendAmount, control, channel, vars) }
        }

    }

    /**
     * Set the [Track]s to be used by this animation.
     *
     * @param tracksArray The tracks to set.
     */
    fun setTracks(tracksArray: Array<Track>) {
        tracksArray.forEach { track -> tracks!!.add(track) }
    }

    /**
     * Adds a track to this animation
     * @param track the track to add
     */
    fun addTrack(track: Track) {
        tracks!!.add(track)
    }

    /**
     * removes a track from this animation
     * @param track the track to remove
     */
    fun removeTrack(track: Track) {
        tracks!!.remove(track)
        (track as? ClonableTrack)?.cleanUp()
    }

    /**
     * Returns the tracks set in [.setTracks].
     *
     * @return the tracks set previously
     */
    fun getTracks(): Array<Track> {
        return tracks!!.array
    }

    /**
     * This method creates a clone of the current object.
     * @return a clone of the current object
     */
    public override fun clone(): Animation = try {
//            val result = super.clone() as Animation
        // TODO: fix call to super::clone
        val result = clone() as Animation
        result.tracks = SafeArrayList(Track::class.java)
        tracks!!.forEach { track -> result.tracks!!.add(track.clone()) }
        result
    } catch (e: CloneNotSupportedException) {
        throw AssertionError()
    }

    /**
     *
     * @param spat
     * @return
     */
    fun cloneForSpatial(spat: Spatial): Animation = try {
//            val result = super.clone() as Animation
        // TODO: fix the call to super::clone
        val result = clone() as Animation
        result.tracks = SafeArrayList(Track::class.java)
        tracks!!.forEach { track ->
            when (track) {
                is ClonableTrack -> result.tracks!!.add(track.cloneForSpatial(spat))
                else -> result.tracks!!.add(track)
            }
        }
        result
    } catch (e: CloneNotSupportedException) {
        throw AssertionError()
    }

    override fun jmeClone(): Any {
        return try {
//            return super.clone()
            // TODO: fix call to super::clone
            clone()
        } catch (e: CloneNotSupportedException) {
            throw RuntimeException("Error cloning", e)
        }

    }

    override fun cloneFields(cloner: Cloner, original: Any) {

        // There is some logic here that I'm copying but I'm not sure if
        // it's a mistake or not.  If a track is not a CloneableTrack then it
        // isn't cloned at all... even though they all implement clone() methods. -pspeed
        val newTracks = SafeArrayList<Track>(Track::class.java)
        tracks!!.forEach { track ->
            when (track) {
                is JmeCloneable -> newTracks.add(cloner.clone(track))
                else -> // this is the part that seems fishy
                    newTracks.add(track)
            }
        }
        this.tracks = newTracks
    }

    override fun toString(): String {
        return javaClass.getSimpleName() + "[name=" + name + ", length=" + length + ']'.toString()
    }

    @Throws(IOException::class)
    override fun write(ex: JmeExporter) {
        val out = ex.getCapsule(this)
        out.write(name, "name", null)
        out.write(length, "length", 0f)
        out.write(tracks!!.array, "tracks", null)
    }

    @Throws(IOException::class)
    override fun read(im: JmeImporter) {
        val `in` = im.getCapsule(this)
        name = `in`.readString("name", null)
        length = `in`.readFloat("length", 0f)

        val arr = `in`.readSavableArray("tracks", null)
        when {
            arr != null -> {
                // NOTE: Backward compat only .. Some animations have no
                // tracks set at all even though it makes no sense.
                // Since there's a null check in setTime(),
                // its only appropriate that the check is made here as well.
                tracks = SafeArrayList(Track::class.java)
                arr.forEach { savable -> tracks!!.add(savable as Track) }
            }
        }
    }
}
