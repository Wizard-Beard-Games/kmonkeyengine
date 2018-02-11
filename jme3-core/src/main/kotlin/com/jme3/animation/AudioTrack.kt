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

import com.jme3.audio.AudioNode
import com.jme3.export.JmeExporter
import com.jme3.export.JmeImporter
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import com.jme3.util.TempVars
import com.jme3.util.clone.Cloner
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger

/**
 * AudioTrack is a track to add to an existing animation, to play a sound during
 * an animations for example : gun shot, foot step, shout, etc...
 *
 * usage is
 * <pre>
 * AnimControl control model.getControl(AnimControl.class);
 * AudioTrack track = new AudioTrack(existionAudioNode, control.getAnim("TheAnim").getLength());
 * control.getAnim("TheAnim").addTrack(track);
</pre> *
 *
 * This is mostly intended for short sounds, playInstance will be called on the
 * AudioNode at time 0 + startOffset.
 *
 *
 * @author (kme) Ray Long
 * @author (jme) Nehon
 */
class AudioTrack : ClonableTrack {
    private var audio: AudioNode? = null
    /**
     *
     * @return the start offset of the track
     */
    /**
     * set the start offset of the track
     *
     * @param startOffset
     */
    var startOffset = 0f
    private var length = 0f
    private var initialized = false
    private var started = false
    private var played = false

    //Animation listener to stop the sound when the animation ends or is changed
    private inner class OnEndListener : AnimEventListener {

        override fun onAnimCycleDone(control: AnimControl, channel: AnimChannel, animName: String) {
            stop()
        }

        override fun onAnimChange(control: AnimControl, channel: AnimChannel, animName: String) {}
    }

    /**
     * default constructor for serialization only
     */
    constructor() {}

    /**
     * Creates an AudioTrack
     *
     * @param audio the AudioNode
     * @param length the length of the track (usually the length of the
     * animation you want to add the track to)
     */
    constructor(audio: AudioNode, length: Float) {
        this.audio = audio
        this.length = length
        setUserData(this)
    }

    /**
     * Creates an AudioTrack
     *
     * @param audio the AudioNode
     * @param length the length of the track (usually the length of the
     * animation you want to add the track to)
     * @param startOffset the time in second when the sound will be played after
     * the animation starts (default is 0)
     */
    constructor(audio: AudioNode, length: Float, startOffset: Float) : this(audio, length) {
        this.startOffset = startOffset
    }

    /**
     * Internal use only
     *
     * @see Track.setTime
     */
    override fun setTime(time: Float, weight: Float, control: AnimControl, channel: AnimChannel, vars: TempVars) {

        if (time >= length) {
            return
        }
        if (!initialized) {
            control.addListener(OnEndListener())
            initialized = true
        }
        if (!started && time >= startOffset) {
            started = true
            audio!!.playInstance()
        }
    }

    //stops the sound
    private fun stop() {
        audio!!.stop()
        started = false
    }

    /**
     * Return the length of the track
     *
     * @return length of the track
     */
    override fun getLength(): Float {
        return length
    }

    override fun getKeyFrameTimes(): FloatArray {
        return floatArrayOf(startOffset)
    }

    /**
     * Clone this track
     *
     * @return
     */
    override fun clone(): Track {
        return AudioTrack(audio!!, length, startOffset)
    }

    /**
     * This method clone the Track and search for the cloned counterpart of the
     * original audio node in the given cloned spatial. The spatial is assumed
     * to be the Spatial holding the AnimControl controlling the animation using
     * this Track.
     *
     * @param spatial the Spatial holding the AnimControl
     * @return the cloned Track with proper reference
     */
    override fun cloneForSpatial(spatial: Spatial): Track {
        val audioTrack = AudioTrack()
        audioTrack.length = this.length
        audioTrack.startOffset = this.startOffset

        //searching for the newly cloned AudioNode
        audioTrack.audio = findAudio(spatial)
        if (audioTrack.audio == null) {
            logger.log(Level.WARNING, "{0} was not found in {1} or is not bound to this track", arrayOf<Any>(audio!!.name, spatial.name))
            audioTrack.audio = audio
        }

        //setting user data on the new AudioNode and marking it with a reference to the cloned Track.
        setUserData(audioTrack)

        return audioTrack
    }

    override fun jmeClone(): Any {
        try {
//            return super.clone()
            // TODO: determine proper supertype
            return clone()
        } catch (e: CloneNotSupportedException) {
            throw RuntimeException("Error cloning", e)
        }

    }


    override fun cloneFields(cloner: Cloner, original: Any) {
        // Duplicating the old cloned state from cloneForSpatial()
        this.initialized = false
        this.started = false
        this.played = false
        this.audio = cloner.clone<AudioNode>(audio)
    }


    /**
     * recursive function responsible for finding the newly cloned AudioNode
     *
     * @param spat
     * @return
     */
    private fun findAudio(spat: Spatial): AudioNode? {
        if (spat is AudioNode) {
            //spat is an AudioNode
            //getting the UserData TrackInfo so check if it should be attached to this Track
            val t = spat.getUserData<Any>("TrackInfo") as TrackInfo
            return if (t != null && t.getTracks().contains(this)) {
                spat
            } else null

        } else if (spat is Node) {
            for (child in spat.children) {
                val em = findAudio(child)
                if (em != null) {
                    return em
                }
            }
        }
        return null
    }

    private fun setUserData(audioTrack: AudioTrack) {
        //fetching the UserData TrackInfo.
        var data = audioTrack.audio!!.getUserData<Any>("TrackInfo") as TrackInfo

        //if it does not exist, we create it and attach it to the AudioNode.
        if (data == null) {
            data = TrackInfo()
            audioTrack.audio!!.setUserData("TrackInfo", data)
        }

        //adding the given Track to the TrackInfo.
        data.addTrack(audioTrack)
    }

    override fun cleanUp() {
        val t = audio!!.getUserData<Any>("TrackInfo") as TrackInfo
        t.getTracks().remove(this)
        if (!t.getTracks().isEmpty()) {
            audio!!.setUserData("TrackInfo", null)
        }
    }

    /**
     *
     * @return the audio node used by this track
     */
    fun getAudio(): AudioNode? {
        return audio
    }

    /**
     * sets the audio node to be used for this track
     *
     * @param audio
     */
    fun setAudio(audio: AudioNode) {
        if (this.audio != null) {
            val data = audio.getUserData<Any>("TrackInfo") as TrackInfo
            data.getTracks().remove(this)
        }
        this.audio = audio
        setUserData(this)
    }

    /**
     * Internal use only serialization
     *
     * @param ex exporter
     * @throws IOException exception
     */
    @Throws(IOException::class)
    override fun write(ex: JmeExporter) {
        val out = ex.getCapsule(this)
        out.write(audio, "audio", null)
        out.write(length, "length", 0f)
        out.write(startOffset, "startOffset", 0f)
    }

    /**
     * Internal use only serialization
     *
     * @param im importer
     * @throws IOException Exception
     */
    @Throws(IOException::class)
    override fun read(im: JmeImporter) {
        val `in` = im.getCapsule(this)
        audio = `in`.readSavable("audio", null) as AudioNode
        length = `in`.readFloat("length", length)
        startOffset = `in`.readFloat("startOffset", 0f)
    }

    companion object {

        private val logger = Logger.getLogger(AudioTrack::class.java.name)
    }
}
