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
package com.jme3.app.state

import com.jme3.app.Application
import com.jme3.input.InputManager
import com.jme3.input.KeyInput
import com.jme3.input.controls.ActionListener
import com.jme3.input.controls.KeyTrigger
import com.jme3.post.SceneProcessor
import com.jme3.profile.AppProfiler
import com.jme3.renderer.Camera
import com.jme3.renderer.RenderManager
import com.jme3.renderer.Renderer
import com.jme3.renderer.ViewPort
import com.jme3.renderer.queue.RenderQueue
import com.jme3.system.JmeSystem
import com.jme3.texture.FrameBuffer
import com.jme3.texture.Image
import com.jme3.util.BufferUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.logging.Level
import java.util.logging.Logger

class ScreenshotAppState : AbstractAppState, ActionListener, SceneProcessor {
    private var filePath: String? = null
    private var capture = false
    private var numbered = true
    private var renderer: Renderer? = null
    private var rm: RenderManager? = null
    private var outBuf: ByteBuffer? = null
    private var shotName: String? = null
    private var shotIndex: Long = 0
    private var width: Int = 0
    private var height: Int = 0
    private var prof: AppProfiler? = null

    /**
     * Using the default constructor, the screenshot files will be written sequentially to the system
     * default storage folder.
     */

    /**
     * This constructor allows you to specify the output file path of the screenshot.
     * Include the seperator at the end of the path.
     * Use an emptry string to use the application folder. Use NULL to use the system
     * default storage folder.
     * @param filePath The screenshot file path to use. Include the seperator at the end of the path.
     */
    @JvmOverloads constructor(filePath: String? = null) {
        this.filePath = filePath
    }

    /**
     * This constructor allows you to specify the output file path of the screenshot.
     * Include the seperator at the end of the path.
     * Use an emptry string to use the application folder. Use NULL to use the system
     * default storage folder.
     * @param filePath The screenshot file path to use. Include the seperator at the end of the path.
     * @param fileName The name of the file to save the screeshot as.
     */
    constructor(filePath: String, fileName: String) {
        this.filePath = filePath
        this.shotName = fileName
    }

    /**
     * This constructor allows you to specify the output file path of the screenshot and
     * a base index for the shot index.
     * Include the seperator at the end of the path.
     * Use an emptry string to use the application folder. Use NULL to use the system
     * default storage folder.
     * @param filePath The screenshot file path to use. Include the seperator at the end of the path.
     * @param shotIndex The base index for screen shots.  The first screen shot will have
     * shotIndex + 1 appended, the next shotIndex + 2, and so on.
     */
    constructor(filePath: String, shotIndex: Long) {
        this.filePath = filePath
        this.shotIndex = shotIndex
    }

    /**
     * This constructor allows you to specify the output file path of the screenshot and
     * a base index for the shot index.
     * Include the seperator at the end of the path.
     * Use an emptry string to use the application folder. Use NULL to use the system
     * default storage folder.
     * @param filePath The screenshot file path to use. Include the seperator at the end of the path.
     * @param fileName The name of the file to save the screeshot as.
     * @param shotIndex The base index for screen shots.  The first screen shot will have
     * shotIndex + 1 appended, the next shotIndex + 2, and so on.
     */
    constructor(filePath: String, fileName: String, shotIndex: Long) {
        this.filePath = filePath
        this.shotName = fileName
        this.shotIndex = shotIndex
    }

    /**
     * Set the file path to store the screenshot.
     * Include the seperator at the end of the path.
     * Use an emptry string to use the application folder. Use NULL to use the system
     * default storage folder.
     * @param filePath File path to use to store the screenshot. Include the seperator at the end of the path.
     */
    fun setFilePath(filePath: String) {
        this.filePath = filePath
    }

    /**
     * Set the file name of the screenshot.
     * @param fileName File name to save the screenshot as.
     */
    fun setFileName(fileName: String) {
        this.shotName = fileName
    }

    /**
     * Sets the base index that will used for subsequent screen shots.
     */
    fun setShotIndex(index: Long) {
        this.shotIndex = index
    }

    /**
     * Sets if the filename should be appended with a number representing the
     * current sequence.
     * @param numberedWanted If numbering is wanted.
     */
    fun setIsNumbered(numberedWanted: Boolean) {
        this.numbered = numberedWanted
    }

    override fun initialize(stateManager: AppStateManager, app: Application) {
        when {
            !super.isInitialized() -> {
                val inputManager = app.inputManager
                inputManager.addMapping("ScreenShot", KeyTrigger(KeyInput.KEY_SYSRQ))
                inputManager.addListener(this, "ScreenShot")

                val vps = app.renderManager.postViews
                val last = vps[vps.size - 1]
                last.addProcessor(this)

                when (shotName) {
                    null -> shotName = app.javaClass.simpleName
                }
            }
        }

        super.initialize(stateManager, app)
    }

    override fun onAction(name: String, value: Boolean, tpf: Float) {
        when {
            value -> capture = true
        }
    }

    fun takeScreenshot() {
        capture = true
    }

    override fun initialize(rm: RenderManager, vp: ViewPort) {
        renderer = rm.renderer
        this.rm = rm
        reshape(vp, vp.camera.width, vp.camera.height)
    }

    override fun isInitialized(): Boolean {
        return super.isInitialized() && renderer != null
    }

    override fun reshape(vp: ViewPort, w: Int, h: Int) {
        outBuf = BufferUtils.createByteBuffer(w * h * 4)
        width = w
        height = h
    }

    override fun preFrame(tpf: Float) {}

    override fun postQueue(rq: RenderQueue) {}

    override fun postFrame(out: FrameBuffer) {
        if (capture) {
            capture = false

            val curCamera = rm!!.currentCamera
            val viewX = (curCamera.viewPortLeft * curCamera.width).toInt()
            val viewY = (curCamera.viewPortBottom * curCamera.height).toInt()
            val viewWidth = ((curCamera.viewPortRight - curCamera.viewPortLeft) * curCamera.width).toInt()
            val viewHeight = ((curCamera.viewPortTop - curCamera.viewPortBottom) * curCamera.height).toInt()

            renderer!!.setViewPort(0, 0, width, height)
            renderer!!.readFrameBuffer(out, outBuf)
            renderer!!.setViewPort(viewX, viewY, viewWidth, viewHeight)

            val file: File
            val filename: String? = when {
                numbered -> {
                    shotIndex++
                    shotName!! + shotIndex
                }
                else -> shotName
            }

            file = when (filePath) {
                null -> File(JmeSystem.getStorageFolder().toString() + File.separator + filename + ".png").absoluteFile
                else -> File(filePath + filename + ".png").absoluteFile
            }
            logger.log(Level.FINE, "Saving ScreenShot to: {0}", file.absolutePath)

            try {
                writeImageFile(file)
            } catch (ex: IOException) {
                logger.log(Level.SEVERE, "Error while saving screenshot", ex)
            }

        }
    }

    override fun setProfiler(profiler: AppProfiler) {
        this.prof = profiler
    }

    /**
     * Called by postFrame() once the screen has been captured to outBuf.
     */
    @Throws(IOException::class)
    protected fun writeImageFile(file: File) {
        val outStream = FileOutputStream(file)
        outStream.use { outStream ->
            JmeSystem.writeImageFile(outStream, "png", outBuf, width, height)
        }
    }

    companion object {

        private val logger = Logger.getLogger(ScreenshotAppState::class.java.name)
    }
}

