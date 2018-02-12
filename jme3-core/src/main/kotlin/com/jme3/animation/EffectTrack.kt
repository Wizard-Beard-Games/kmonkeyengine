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

import com.jme3.effect.ParticleEmitter
import com.jme3.export.JmeExporter
import com.jme3.export.JmeImporter
import com.jme3.renderer.RenderManager
import com.jme3.renderer.ViewPort
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import com.jme3.scene.Spatial.CullHint
import com.jme3.scene.control.AbstractControl
import com.jme3.scene.control.Control
import com.jme3.util.TempVars
import com.jme3.util.clone.Cloner
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger

/**
 * EffectTrack is a track to add to an existing animation, to emit particles
 * during animations for example: exhaust, dust raised by footsteps, shock
 * waves, lightning, etc...
 *
 * usage is
 * <pre>
 * AnimControl control model.getControl(AnimControl.class);
 * EffectTrack track = new EffectTrack(existingEmmitter, control.getAnim("TheAnim").getLength());
 * control.getAnim("TheAnim").addTrack(track);
</pre> *
 *
 * if the emitter emits 0 particles per second, emitAllPArticles will be
 * called on it at time 0 + startOffset. if it has more it will start
 * emitting normally at time 0 + startOffset.
 *
 *
 * @author (kme) Ray Long
 * @author (jme) Nehon
 */
class EffectTrack : ClonableTrack {
    private var emitter: ParticleEmitter? = null
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
    private var particlesPerSeconds = 0f
    private var length = 0f
    private var emitted = false
    private var initialized = false
    //control responsible for disable and cull the emitter once all particles are gone
    private val killParticles = KillParticleControl()

    class KillParticleControl : AbstractControl() {

        internal lateinit var emitter: ParticleEmitter
        internal var stopRequested = false
        internal var remove = false

        override fun setSpatial(spatial: Spatial?) {
            super.setSpatial(spatial)
            when {
                spatial != null -> when (spatial) {
                    is ParticleEmitter -> emitter = spatial
                    else -> throw IllegalArgumentException("KillParticleEmitter can only ba attached to ParticleEmitter")
                }
            }


        }

        override fun controlUpdate(tpf: Float) {
            when {
                remove -> {
                    emitter.removeControl(this)
                    return
                }
                else -> when {
                    emitter.numVisibleParticles == 0 -> {
                        emitter.cullHint = CullHint.Always
                        emitter.isEnabled = false
                        emitter.removeControl(this)
                        stopRequested = false
                    }
                }
            }
        }

        override fun jmeClone(): Any {
            val c = KillParticleControl()
            //this control should be removed as it shouldn't have been persisted in the first place
            //In the quest to find the less hackish solution to achieve this,
            //making it remove itself from the spatial in the first update loop when loaded was the less bad.
            c.remove = true
            c.spatial = spatial
            return c
        }

        override fun controlRender(rm: RenderManager, vp: ViewPort) {}

        override fun cloneForSpatial(spatial: Spatial): Control {

            val c = KillParticleControl()
            //this control should be removed as it shouldn't have been persisted in the first place
            //In the quest to find the less hackish solution to achieve this,
            //making it remove itself from the spatial in the first update loop when loaded was the less bad.
            c.remove = true
            c.setSpatial(spatial)
            return c

        }
    }

    //Anim listener that stops the Emmitter when the animation is finished or changed.
    private inner class OnEndListener : AnimEventListener {

        override fun onAnimCycleDone(control: AnimControl, channel: AnimChannel, animName: String) {
            stop()
        }

        override fun onAnimChange(control: AnimControl, channel: AnimChannel, animName: String) {}
    }

    /**
     * default constructor only for serialization
     */
    constructor() {}

    /**
     * Creates and EffectTrack
     *
     * @param emitter the emitter of the track
     * @param length the length of the track (usually the length of the
     * animation you want to add the track to)
     */
    constructor(emitter: ParticleEmitter, length: Float) {
        this.emitter = emitter
        //saving particles per second value
        this.particlesPerSeconds = emitter.particlesPerSec
        //setting the emmitter to not emmit.
        this.emitter!!.particlesPerSec = 0f
        this.length = length
        //Marking the emitter with a reference to this track for further use in deserialization.
        setUserData(this)

    }

    /**
     * Creates and EffectTrack
     *
     * @param emitter the emitter of the track
     * @param length the length of the track (usually the length of the
     * animation you want to add the track to)
     * @param startOffset the time in second when the emitter will be triggered
     * after the animation starts (default is 0)
     */
    constructor(emitter: ParticleEmitter, length: Float, startOffset: Float) : this(emitter, length) {
        this.startOffset = startOffset
    }

    /**
     * Internal use only
     *
     * @see Track.setTime
     */
    override fun setTime(time: Float, weight: Float, control: AnimControl, channel: AnimChannel, vars: TempVars) {//else reset its former particlePerSec value to let it emmit.

        //if the emitter has 0 particles per seconds emmit all particles in one shot
        when {
            time >= length -> return
        //first time adding the Animation listener to stop the track at the end of the animation//else reset its former particlePerSec value to let it emmit.
        //if the emitter has 0 particles per seconds emmit all particles in one shot
        //checking fo time to trigger the effect
            else -> {
                when {
                    !initialized -> {
                        control.addListener(OnEndListener())
                        initialized = true
                    }
                }
                //checking fo time to trigger the effect
                when {
                    !emitted && time >= startOffset -> {
                        emitted = true
                        emitter!!.cullHint = CullHint.Dynamic
                        emitter!!.isEnabled = true
                        //if the emitter has 0 particles per seconds emmit all particles in one shot
                        when (particlesPerSeconds) {
                            0f -> {
                                emitter!!.emitAllParticles()
                                when {
                                    !killParticles.stopRequested -> {
                                        emitter!!.addControl(killParticles)
                                        killParticles.stopRequested = true
                                    }
                                }
                            }
                            else -> //else reset its former particlePerSec value to let it emmit.
                                emitter!!.particlesPerSec = particlesPerSeconds
                        }
                    }
                }
            }
        }
    }

    //stops the emmiter to emit.
    private fun stop() {
        emitter!!.particlesPerSec = 0f
        emitted = false
        when {
            !killParticles.stopRequested -> {
                emitter!!.addControl(killParticles)
                killParticles.stopRequested = true
            }
        }

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
//    override fun clone(): Track {
    override fun clone(): EffectTrack {
        return EffectTrack(emitter!!, length, startOffset)
    }

    /**
     * This method clone the Track and search for the cloned counterpart of the
     * original emitter in the given cloned spatial. The spatial is assumed to
     * be the Spatial holding the AnimControl controlling the animation using
     * this Track.
     *
     * @param spatial the Spatial holding the AnimControl
     * @return the cloned Track with proper reference
     */
    override fun cloneForSpatial(spatial: Spatial): Track {
        val effectTrack = EffectTrack()
        effectTrack.particlesPerSeconds = this.particlesPerSeconds
        effectTrack.length = this.length
        effectTrack.startOffset = this.startOffset

        //searching for the newly cloned ParticleEmitter
        effectTrack.emitter = findEmitter(spatial)
        when {
            effectTrack.emitter == null -> {
                logger.log(Level.WARNING, "{0} was not found in {1} or is not bound to this track", arrayOf<Any>(emitter!!.name, spatial.name))
                effectTrack.emitter = emitter
            }
        //setting user data on the new emmitter and marking it with a reference to the cloned Track.
        }

        removeUserData(this)
        //setting user data on the new emmitter and marking it with a reference to the cloned Track.
        setUserData(effectTrack)
        effectTrack.emitter!!.particlesPerSec = 0f
        return effectTrack
    }

    override fun jmeClone(): Any {
        try {
//            return super.clone()
            return clone()
        } catch (e: CloneNotSupportedException) {
            throw RuntimeException("Error cloning", e)
        }

    }


    override fun cloneFields(cloner: Cloner, original: Any) {
        this.emitter = cloner.clone<ParticleEmitter>(emitter)
    }

    /**
     * recursive function responsible for finding the newly cloned Emitter
     *
     * @param spat
     * @return
     */
    private fun findEmitter(spat: Spatial): ParticleEmitter? {
        when (spat) {
            is ParticleEmitter -> {
                //spat is a PArticleEmitter
                //getting the UserData TrackInfo so check if it should be attached to this Track
                val t = spat.getUserData<Any>("TrackInfo") as TrackInfo
                return if (t != null && t.tracks.contains(this)) {
                    spat
                } else null

            }
            is Node -> spat.children.forEach { child ->
                val em = findEmitter(child)
                when {
                    em != null -> return em
                    else -> {
                    }
                }
            }
        }
        return null
    }

    override fun cleanUp() {
        val t = emitter!!.getUserData<Any>("TrackInfo") as TrackInfo
        t.tracks.remove(this)
        when {
            t.tracks.isEmpty() -> emitter!!.setUserData("TrackInfo", null)
        }
    }

    /**
     *
     * @return the emitter used by this track
     */
    fun getEmitter(): ParticleEmitter? {
        return emitter
    }

    /**
     * Sets the Emitter to use in this track
     *
     * @param emitter
     */
    fun setEmitter(emitter: ParticleEmitter) {
        when {
            this.emitter != null -> {
                val data = emitter.getUserData<Any>("TrackInfo") as TrackInfo
                data.tracks.remove(this)
            }
        }
        this.emitter = emitter
        //saving particles per second value
        this.particlesPerSeconds = emitter.particlesPerSec
        //setting the emmitter to not emmit.
        this.emitter!!.particlesPerSec = 0f
        setUserData(this)
    }

    private fun setUserData(effectTrack: EffectTrack) {
        //fetching the UserData TrackInfo.
        var data = effectTrack.emitter!!.getUserData<Any>("TrackInfo") as TrackInfo?

        //if it does not exist, we create it and attach it to the emitter.
        when (data) {
            null -> {
                data = TrackInfo()
                effectTrack.emitter!!.setUserData("TrackInfo", data)
            }
        }

        //adding the given Track to the TrackInfo.
        data?.addTrack(effectTrack)
    }

    private fun removeUserData(effectTrack: EffectTrack) {
        //fetching the UserData TrackInfo.
        val data = effectTrack.emitter!!.getUserData<Any>("TrackInfo") as TrackInfo ?: return

        //if it does not exist, we create it and attach it to the emitter.

        //removing the given Track to the TrackInfo.
        data.tracks.remove(effectTrack)


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
        //reset the particle emission rate on the emitter before saving.
        emitter!!.particlesPerSec = particlesPerSeconds
        out.write(emitter, "emitter", null)
        out.write(particlesPerSeconds, "particlesPerSeconds", 0f)
        out.write(length, "length", 0f)
        out.write(startOffset, "startOffset", 0f)
        //Setting emission rate to 0 so that this track can go on being used.
        emitter!!.particlesPerSec = 0f
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
        this.particlesPerSeconds = `in`.readFloat("particlesPerSeconds", 0f)
        //reading the emitter even if the track will then reference its cloned counter part if it's loaded with the assetManager.
        //This also avoid null pointer exception if the model is not loaded via the AssetManager.
        emitter = `in`.readSavable("emitter", null) as ParticleEmitter
        emitter!!.particlesPerSec = 0f
        //if the emitter was saved with a KillParticleControl we remove it.
        //        Control c = emitter.getControl(KillParticleControl.class);
        //        if(c!=null){
        //            emitter.removeControl(c);
        //        }
        //emitter.removeControl(KillParticleControl.class);
        length = `in`.readFloat("length", length)
        startOffset = `in`.readFloat("startOffset", 0f)
    }

    companion object {

        private val logger = Logger.getLogger(EffectTrack::class.java.name)
    }
}
