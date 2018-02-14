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
package com.jme3.app

import com.jme3.app.state.AppState
import com.jme3.app.state.AppStateManager
import com.jme3.asset.AssetManager
import com.jme3.audio.AudioContext
import com.jme3.audio.AudioRenderer
import com.jme3.audio.Listener
import com.jme3.input.*
import com.jme3.math.Vector3f
import com.jme3.profile.AppProfiler
import com.jme3.profile.AppStep
import com.jme3.renderer.Camera
import com.jme3.renderer.RenderManager
import com.jme3.renderer.Renderer
import com.jme3.renderer.ViewPort
import com.jme3.system.*
import com.jme3.system.JmeContext.Type
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Future
import java.util.logging.Level
import java.util.logging.Logger

/**
 * The `LegacyApplication` class represents an instance of a
 * real-time 3D rendering jME application.
 *
 * An `LegacyApplication` provides all the tools that are commonly used in jME3
 * applications.
 *
 * jME3 applications *SHOULD NOT EXTEND* this class but extend [com.jme3.app.SimpleApplication] instead.
 *
 */
class LegacyApplication
/**
 * Create a new instance of `LegacyApplication`, preinitialized
 * with the specified set of app states.
 */
@JvmOverloads constructor(vararg initialStates: AppState = arrayOf()) : Application, SystemListener {

    override var _assetManager: AssetManager? = null
        set(value) {
            if (field != null) {
                throw IllegalStateException("Can only set asset manager before initialization.")
            }
            field = value
        }

    /**
     * @return The [audio renderer][AudioRenderer] for the application
     */
    override var audioRenderer: AudioRenderer? = null

    /**
     * @return The [renderer][Renderer] for the application
     */
    override var renderer: Renderer? = null

    /**
     * @return the [render manager][RenderManager]
     */
    override var renderManager: RenderManager? = null

    override var viewPort: ViewPort? = null

    /**
     * @return The GUI viewport. Which is used for the on screen
     * statistics and FPS.
     */
    override var guiViewPort: ViewPort? = null

    /**
     * @return The [display context][JmeContext] for the application
     */
    override var context: JmeContext? = null

    override var settings: AppSettings? = null
        set(settings) {
            field = settings
            if (context != null && settings!!.useInput() != inputEnabled) {
                // may need to create or destroy input based
                // on settings change
                inputEnabled = !inputEnabled
                if (inputEnabled) {
                    initInput()
                } else {
                    destroyInput()
                }
            } else {
                inputEnabled = settings!!.useInput()
            }
        }

    override var timer: Timer? = NanoTimer()
        set(value) {
            field = value

            timer?.reset()

            if (renderManager != null) {
                renderManager!!.setTimer(timer)
            }
        }

    /**
     * @return The [camera][Camera] for the application
     */
    override var camera: Camera
        protected set(value: Camera) {
            super.camera = value
        }
    /**
     * @return The [listener][Listener] object for audio
     */
    override var listener: Listener
        protected set(value: Listener) {
            super.listener = value
        }

    protected var inputEnabled = true
    /**
     * Determine the application's behavior when unfocused.
     *
     * @return The lost focus behavior of the application.
     */
    /**
     * Change the application's behavior when unfocused.
     *
     * By default, the application will
     * [throttle the update loop][LostFocusBehavior.ThrottleOnLostFocus]
     * so as to not take 100% CPU usage when it is not in focus, e.g.
     * alt-tabbed, minimized, or obstructed by another window.
     *
     * @param lostFocusBehavior The new lost focus behavior to use.
     *
     * @see LostFocusBehavior
     */
    override var lostFocusBehavior = LostFocusBehavior.ThrottleOnLostFocus
    protected var speed = 1f
    protected var paused = false
    protected var mouseInput: MouseInput? = null
    protected var keyInput: KeyInput? = null
    protected var joyInput: JoyInput? = null
    protected var touchInput: TouchInput? = null
    /**
     * @return the [input manager][InputManager].
     */
    override var inputManager: InputManager? = null
        protected set(value: InputManager?) {
            super.inputManager = value
        }
    /**
     * @return the [app state manager][AppStateManager]
     */
    override var stateManager: AppStateManager
        protected set(value: AppStateManager) {
            super.stateManager = value
        }

    protected var prof: AppProfiler? = null

    private val taskQueue = ConcurrentLinkedQueue<AppTask<*>>()

    /**
     * Returns true if pause on lost focus is enabled, false otherwise.
     *
     * @return true if pause on lost focus is enabled
     *
     * @see .getLostFocusBehavior
     */
    /**
     * Enable or disable pause on lost focus.
     *
     *
     * By default, pause on lost focus is enabled.
     * If enabled, the application will stop updating
     * when it loses focus or becomes inactive (e.g. alt-tab).
     * For online or real-time applications, this might not be preferable,
     * so this feature should be set to disabled. For other applications,
     * it is best to keep it on so that CPU usage is not used when
     * not necessary.
     *
     * @param pauseOnLostFocus True to enable pause on lost focus, false
     * otherwise.
     *
     * @see .setLostFocusBehavior
     */
    override var isPauseOnLostFocus: Boolean
        get() = lostFocusBehavior == LostFocusBehavior.PauseOnLostFocus
        set(pauseOnLostFocus) = if (pauseOnLostFocus) {
            lostFocusBehavior = LostFocusBehavior.PauseOnLostFocus
        } else {
            lostFocusBehavior = LostFocusBehavior.Disabled
        }

    /**
     * Returns the current AppProfiler hook, or null if none is set.
     */
    /**
     * Sets an AppProfiler hook that will be called back for
     * specific steps within a single update frame.  Value defaults
     * to null.
     */
    override var appProfiler: AppProfiler?
        get() = prof
        set(prof) {
            this.prof = prof
            if (renderManager != null) {
                renderManager!!.setAppProfiler(prof)
            }
        }

    init {
        initStateManager()

        if (initialStates != null) {
            for (a in initialStates) {
                if (a != null) {
                    stateManager.attach(a)
                }
            }
        }
    }

//    @Deprecated("")
//    fun set_assetManager(_assetManager: AssetManager) {
//        if (this._assetManager != null)
//            throw IllegalStateException("Can only set asset manager" + " before initialization.")
//
//        this._assetManager = _assetManager
//    }

    private fun initAssetManager() {
        var assetCfgUrl: URL? = null

        if (settings != null) {
            val assetCfg = settings!!.getString("AssetConfigURL")
            if (assetCfg != null) {
                try {
                    assetCfgUrl = URL(assetCfg)
                } catch (ex: MalformedURLException) {
                }

                if (assetCfgUrl == null) {
                    assetCfgUrl = LegacyApplication::class.java.classLoader.getResource(assetCfg)
                    if (assetCfgUrl == null) {
                        logger.log(Level.SEVERE, "Unable to access AssetConfigURL in asset config:{0}", assetCfg)
                        return
                    }
                }
            }
        }
        if (assetCfgUrl == null) {
            assetCfgUrl = JmeSystem.getPlatformAssetConfigURL()
        }
        if (_assetManager == null) {
            _assetManager = JmeSystem.newAssetManager(assetCfgUrl)
        }
    }

    /**
     * Set the display settings to define the display created.
     *
     *
     * Examples of display parameters include display pixel width and height,
     * color bit depth, z-buffer bits, anti-aliasing samples, and update frequency.
     * If this method is called while the application is already running, then
     * [.restart] must be called to apply the settings to the display.
     *
     * @param settings The settings to set.
     */
//    override fun setSettings(settings: AppSettings?) {
//        this.settings = settings
//        if (context != null && settings!!.useInput() != inputEnabled) {
//            // may need to create or destroy input based
//            // on settings change
//            inputEnabled = !inputEnabled
//            if (inputEnabled) {
//                initInput()
//            } else {
//                destroyInput()
//            }
//        } else {
//            inputEnabled = settings!!.useInput()
//        }
//    }

    /**
     * Sets the Timer implementation that will be used for calculating
     * frame times.  By default, Application will use the Timer as returned
     * by the current JmeContext implementation.
     */
//    override fun setTimer(timer: Timer) {
//        this.timer = timer
//
//        timer?.reset()
//
//        if (renderManager != null) {
//            renderManager!!.setTimer(timer)
//        }
//    }

    private fun initDisplay() {
        // aquire important objects
        // from the context
        settings = context!!.settings

        // Only reset the timer if a user has not already provided one
        if (timer == null) {
            timer = context!!.timer
        }

        renderer = context!!.renderer
    }

    private fun initAudio() {
        if (settings!!.audioRenderer != null && context!!.type != Type.Headless) {
            audioRenderer = JmeSystem.newAudioRenderer(settings)
            audioRenderer!!.initialize()
            AudioContext.setAudioRenderer(audioRenderer)

            listener = Listener()
            audioRenderer!!.setListener(listener)
        }
    }

    /**
     * Creates the camera to use for rendering. Default values are perspective
     * projection with 45° field of view, with near and far values 1 and 1000
     * units respectively.
     */
    private fun initCamera() {
        camera = Camera(settings!!.width, settings!!.height)

        camera.setFrustumPerspective(45f, camera.width.toFloat() / camera.height, 1f, 1000f)
        camera.location = Vector3f(0f, 0f, 10f)
        camera.lookAt(Vector3f(0f, 0f, 0f), Vector3f.UNIT_Y)

        renderManager = RenderManager(renderer)
        //Remy - 09/14/2010 setted the timer in the renderManager
        renderManager!!.setTimer(timer)

        if (prof != null) {
            renderManager!!.setAppProfiler(prof)
        }

        viewPort = renderManager!!.createMainView("Default", camera)
        viewPort.setClearFlags(true, true, true)

        // Create a new cam for the gui
        val guiCam = Camera(settings!!.width, settings!!.height)
        guiViewPort = renderManager!!.createPostView("Gui Default", guiCam)
        guiViewPort.setClearFlags(false, false, false)
    }

    /**
     * Initializes mouse and keyboard input. Also
     * initializes joystick input if joysticks are enabled in the
     * AppSettings.
     */
    private fun initInput() {
        mouseInput = context!!.mouseInput
        if (mouseInput != null)
            mouseInput!!.initialize()

        keyInput = context!!.keyInput
        if (keyInput != null)
            keyInput!!.initialize()

        touchInput = context!!.touchInput
        if (touchInput != null)
            touchInput!!.initialize()

        if (!settings!!.getBoolean("DisableJoysticks")) {
            joyInput = context!!.joyInput
            if (joyInput != null)
                joyInput!!.initialize()
        }

        inputManager = InputManager(mouseInput, keyInput, joyInput, touchInput)
    }

    private fun initStateManager() {
        stateManager = AppStateManager(this)

        // Always register a ResetStatsState to make sure
        // that the stats are cleared every frame
        stateManager.attach(ResetStatsState())
    }

    /**
     * @return The [asset manager][AssetManager] for this application.
     */
//    fun get_assetManager(): AssetManager? {
//        return _assetManager
//    }

    /**
     * Starts the application in [display][Type.Display] mode.
     *
     * @see .start
     */
    override fun start() {
        start(JmeContext.Type.Display, false)
    }

    /**
     * Starts the application in [display][Type.Display] mode.
     *
     * @see .start
     */
    override fun start(waitFor: Boolean) {
        start(JmeContext.Type.Display, waitFor)
    }

    /**
     * Starts the application.
     * Creating a rendering context and executing
     * the main loop in a separate thread.
     */
    @JvmOverloads
    fun start(contextType: JmeContext.Type, waitFor: Boolean = false) {
        if (context != null && context!!.isCreated) {
            logger.warning("start() called when application already created!")
            return
        }

        if (settings == null) {
            settings = AppSettings(true)
        }

        logger.log(Level.FINE, "Starting application: {0}", javaClass.name)
        context = JmeSystem.newContext(settings, contextType)
        context!!.setSystemListener(this)
        context!!.create(waitFor)
    }

    /**
     * Initializes the application's canvas for use.
     *
     *
     * After calling this method, cast the [context][.getContext] to
     * [JmeCanvasContext],
     * then acquire the canvas with [JmeCanvasContext.getCanvas]
     * and attach it to an AWT/Swing Frame.
     * The rendering thread will start when the canvas becomes visible on
     * screen, however if you wish to start the context immediately you
     * may call [.startCanvas] to force the rendering thread
     * to start.
     *
     * @see JmeCanvasContext
     *
     * @see Type.Canvas
     */
    fun createCanvas() {
        if (context != null && context!!.isCreated) {
            logger.warning("createCanvas() called when application already created!")
            return
        }

        if (settings == null) {
            settings = AppSettings(true)
        }

        logger.log(Level.FINE, "Starting application: {0}", javaClass.name)
        context = JmeSystem.newContext(settings, JmeContext.Type.Canvas)
        context!!.setSystemListener(this)
    }

    /**
     * Starts the rendering thread after createCanvas() has been called.
     *
     *
     * Calling this method is optional, the canvas will start automatically
     * when it becomes visible.
     *
     * @param waitFor If true, the current thread will block until the
     * rendering thread is running
     */
    @JvmOverloads
    fun startCanvas(waitFor: Boolean = false) {
        context!!.create(waitFor)
    }

    /**
     * Internal use only.
     */
    override fun reshape(w: Int, h: Int) {
        if (renderManager != null) {
            renderManager!!.notifyReshape(w, h)
        }
    }

    /**
     * Restarts the context, applying any changed settings.
     *
     *
     * Changes to the [AppSettings] of this Application are not
     * applied immediately; calling this method forces the context
     * to restart, applying the new settings.
     */
    override fun restart() {
        context!!.settings = settings
        context!!.restart()
    }

    /**
     * Requests the context to close, shutting down the main loop
     * and making necessary cleanup operations.
     *
     * Same as calling stop(false)
     *
     * @see .stop
     */
    override fun stop() {
        stop(false)
    }

    /**
     * Requests the context to close, shutting down the main loop
     * and making necessary cleanup operations.
     * After the application has stopped, it cannot be used anymore.
     */
    override fun stop(waitFor: Boolean) {
        logger.log(Level.FINE, "Closing application: {0}", javaClass.name)
        context!!.destroy(waitFor)
    }

    /**
     * Do not call manually.
     * Callback from ContextListener.
     *
     *
     * Initializes the `Application`, by creating a display and
     * default camera. If display settings are not specified, a default
     * 640x480 display is created. Default values are used for the camera;
     * perspective projection with 45° field of view, with near
     * and far values 1 and 1000 units respectively.
     */
    override fun initialize() {
        if (_assetManager == null) {
            initAssetManager()
        }

        initDisplay()
        initCamera()

        if (inputEnabled) {
            initInput()
        }
        initAudio()

        // update timer so that the next delta is not too large
        //        timer.update();
        timer!!.reset()

        // user code here..
    }

    /**
     * Internal use only.
     */
    override fun handleError(errMsg: String, t: Throwable?) {
        // Print error to log.
        logger.log(Level.SEVERE, errMsg, t)
        // Display error message on screen if not in headless mode
        if (context!!.type != JmeContext.Type.Headless) {
            if (t != null) {
                JmeSystem.showErrorDialog(errMsg + "\n" + t.javaClass.simpleName +
                        if (t.message != null) ": " + t.message else "")
            } else {
                JmeSystem.showErrorDialog(errMsg)
            }
        }

        stop() // stop the application
    }

    /**
     * Internal use only.
     */
    override fun gainFocus() {
        if (lostFocusBehavior != LostFocusBehavior.Disabled) {
            if (lostFocusBehavior == LostFocusBehavior.PauseOnLostFocus) {
                paused = false
            }
            context!!.setAutoFlushFrames(true)
            if (inputManager != null) {
                inputManager!!.reset()
            }
        }
    }

    /**
     * Internal use only.
     */
    override fun loseFocus() {
        if (lostFocusBehavior != LostFocusBehavior.Disabled) {
            if (lostFocusBehavior == LostFocusBehavior.PauseOnLostFocus) {
                paused = true
            }
            context!!.setAutoFlushFrames(false)
        }
    }

    /**
     * Internal use only.
     */
    override fun requestClose(esc: Boolean) {
        context!!.destroy(false)
    }

    /**
     * Enqueues a task/callable object to execute in the jME3
     * rendering thread.
     *
     *
     * Callables are executed right at the beginning of the main loop.
     * They are executed even if the application is currently paused
     * or out of focus.
     *
     * @param callable The callable to run in the main jME3 thread
     */
    override fun <V> enqueue(callable: Callable<V>): Future<V> {
        val task = AppTask(callable)
        taskQueue.add(task)
        return task
    }

    /**
     * Enqueues a runnable object to execute in the jME3
     * rendering thread.
     *
     *
     * Runnables are executed right at the beginning of the main loop.
     * They are executed even if the application is currently paused
     * or out of focus.
     *
     * @param runnable The runnable to run in the main jME3 thread
     */
    override fun enqueue(runnable: Runnable) {
        enqueue<Any>(RunnableWrapper(runnable))
    }

    /**
     * Runs tasks enqueued via [.enqueue]
     */
    protected fun runQueuedTasks() {
        var task: AppTask<*>
        while ((task = taskQueue.poll()) != null) {
            if (!task.isCancelled) {
                task.invoke()
            }
        }
    }

    /**
     * Do not call manually.
     * Callback from ContextListener.
     */
    override fun update() {
        // Make sure the audio renderer is available to callables
        AudioContext.setAudioRenderer(audioRenderer)

        if (prof != null) prof!!.appStep(AppStep.QueuedTasks)
        runQueuedTasks()

        if (speed == 0f || paused)
            return

        timer!!.update()

        if (inputEnabled) {
            if (prof != null) prof!!.appStep(AppStep.ProcessInput)
            inputManager!!.update(timer!!.timePerFrame)
        }

        if (audioRenderer != null) {
            if (prof != null) prof!!.appStep(AppStep.ProcessAudio)
            audioRenderer!!.update(timer!!.timePerFrame)
        }

        // user code here..
    }

    protected fun destroyInput() {
        if (mouseInput != null)
            mouseInput!!.destroy()

        if (keyInput != null)
            keyInput!!.destroy()

        if (joyInput != null)
            joyInput!!.destroy()

        if (touchInput != null)
            touchInput!!.destroy()

        inputManager = null
    }

    /**
     * Do not call manually.
     * Callback from ContextListener.
     */
    override fun destroy() {
        stateManager.cleanup()

        destroyInput()
        if (audioRenderer != null)
            audioRenderer!!.cleanup()

        timer!!.reset()
    }

    private inner class RunnableWrapper(private val runnable: Runnable) : Callable<*> {

        override fun call(): Any? {
            runnable.run()
            return null
        }

    }

    companion object {

        private val logger = Logger.getLogger(LegacyApplication::class.java.name)
    }

}
/**
 * Create a new instance of `LegacyApplication`.
 */
/**
 * Starts the application.
 * Creating a rendering context and executing
 * the main loop in a separate thread.
 */
/**
 * Starts the rendering thread after createCanvas() has been called.
 *
 *
 * Same as calling startCanvas(false)
 *
 * @see .startCanvas
 */
