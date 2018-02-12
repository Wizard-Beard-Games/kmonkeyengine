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

import com.jme3.app.state.AbstractAppState
import com.jme3.app.state.AppStateManager
import com.jme3.input.CameraInput
import com.jme3.input.InputManager
import com.jme3.input.MouseInput
import com.jme3.input.controls.ActionListener
import com.jme3.input.controls.AnalogListener
import com.jme3.input.controls.MouseAxisTrigger
import com.jme3.input.controls.MouseButtonTrigger
import com.jme3.input.controls.Trigger
import com.jme3.math.FastMath
import com.jme3.math.Vector3f
import com.jme3.scene.CameraNode
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import com.jme3.scene.control.CameraControl
import com.jme3.util.TempVars

/**
 * This class is a camera controller that allow the camera to follow a target
 * Spatial.
 *
 * @author (kme) Ray Long
 * @author (jme) Nehon
 */
open class ChaseCameraAppState : AbstractAppState(), ActionListener, AnalogListener {

    protected var spatial: Spatial? = null
    protected lateinit var target: Node
    protected var camNode: CameraNode
    protected var inputManager: InputManager? = null
    protected var invertYaxis = false
    protected var invertXaxis = false
    protected var hideCursorOnRotate = true
    protected var canRotate: Boolean = false
    private var _dragToRotate = true

    /**
     * Returns the rotation speed when the mouse is moved.
     *
     * @return the rotation speed when the mouse is moved.
     */
    /**
     * Sets the rotate amount when user moves his mouse, the lower the value,
     * the slower the camera will rotate. default is 1.
     *
     * @param rotationSpeed Rotation speed on mouse movement, default is 1.
     */
    var rotationSpeed = 1.0f
    /**
     * returns the zoom speed
     *
     * @return
     */
    /**
     * Sets the zoom speed, the lower the value, the slower the camera will zoom
     * in and out. default is 2.
     *
     * @param zoomSpeed
     */
    var zoomSpeed = 2.0f
    //protected boolean zoomin;
    private var _minDistance = 1.0f
    private var _maxDistance = 40.0f
    protected var distance = 20f
    private var _maxVerticalRotation = 1.4f
    protected var verticalRotation = 0f
    private var _minVerticalRotation = 0f
    protected var horizontalRotation = 0f
    //protected float distanceLerpFactor = 0;
    protected var upVector = Vector3f()
    protected var leftVector = Vector3f()
    private var _zoomOutTrigger = arrayOf<Trigger>(MouseAxisTrigger(MouseInput.AXIS_WHEEL, true))
    private var _zoomInTrigger = arrayOf<Trigger>(MouseAxisTrigger(MouseInput.AXIS_WHEEL, false))
    private var _toggleRotateTrigger = arrayOf<Trigger>(MouseButtonTrigger(MouseInput.BUTTON_LEFT), MouseButtonTrigger(MouseInput.BUTTON_RIGHT))

    /**
     * @return If drag to rotate feature is enabled.
     *
     * @see FlyByCamera.setDragToRotate
     */
    /**
     * @param dragToRotate When true, the user must hold the mouse button and
     * drag over the screen to rotate the camera, and the cursor is visible
     * until dragged. Otherwise, the cursor is invisible at all times and
     * holding the mouse button is not needed to rotate the camera. This feature
     * is disabled by default.
     */
    var isDragToRotate: Boolean
        get() = _dragToRotate
        set(dragToRotate) {
            this._dragToRotate = dragToRotate
            this.canRotate = !dragToRotate
            if (inputManager != null) {
                inputManager!!.isCursorVisible = dragToRotate
            }
        }

    //
    //    protected boolean rotating = false;
    //    protected float rotation = 0;
    //    protected float targetRotation = rotation;
    init {
        camNode = CameraNode("ChaseCameraNode", CameraControl())
    }

    override fun initialize(stateManager: AppStateManager, app: Application) {
        super.initialize(stateManager, app)
        this.inputManager = app.inputManager
        target = Node("ChaseCamTarget")
        camNode.camera = app.camera
        camNode.controlDir = CameraControl.ControlDirection.SpatialToCamera
        target.attachChild(camNode)
        camNode.setLocalTranslation(0f, 0f, distance)
        upVector = app.camera.up.clone()
        leftVector = app.camera.left.clone()
        registerWithInput()
        rotateCamera()
    }

    /**
     * Registers inputs with the input manager
     *
     */
    fun registerWithInput() {

        val inputs = arrayOf(CameraInput.CHASECAM_TOGGLEROTATE, CameraInput.CHASECAM_DOWN, CameraInput.CHASECAM_UP, CameraInput.CHASECAM_MOVELEFT, CameraInput.CHASECAM_MOVERIGHT, CameraInput.CHASECAM_ZOOMIN, CameraInput.CHASECAM_ZOOMOUT)
        initVerticalAxisInputs()
        initZoomInput()
        initHorizontalAxisInput()
        initTogleRotateInput()

        inputManager!!.addListener(this, *inputs)
        inputManager!!.isCursorVisible = _dragToRotate
    }

    override fun onAction(name: String, keyPressed: Boolean, tpf: Float) {
        when {
            isEnabled() -> when {
                _dragToRotate -> when {
                    name == CameraInput.CHASECAM_TOGGLEROTATE && isEnabled() -> when {
                        keyPressed -> {
                            canRotate = true
                            if (hideCursorOnRotate) {
                                inputManager!!.isCursorVisible = false
                            }
                        }
                        else -> {
                            canRotate = false
                            if (hideCursorOnRotate) {
                                inputManager!!.isCursorVisible = true
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onAnalog(name: String, value: Float, tpf: Float) {
        if (isEnabled()) {
            if (canRotate) {
                when (name) {
                    CameraInput.CHASECAM_MOVELEFT -> {
                        horizontalRotation -= value * rotationSpeed
                        rotateCamera()
                    }
                    CameraInput.CHASECAM_MOVERIGHT -> {
                        horizontalRotation += value * rotationSpeed
                        rotateCamera()
                    }
                    CameraInput.CHASECAM_UP -> {
                        verticalRotation += value * rotationSpeed
                        rotateCamera()
                    }
                    CameraInput.CHASECAM_DOWN -> {
                        verticalRotation -= value * rotationSpeed
                        rotateCamera()
                    }
                }
            }
            if (name == CameraInput.CHASECAM_ZOOMIN) {
                zoomCamera(-value * zoomSpeed)
            } else if (name == CameraInput.CHASECAM_ZOOMOUT) {
                zoomCamera(+value * zoomSpeed)
            }
        }
    }

    /**
     * rotate the camera around the target
     */
    protected fun rotateCamera() {
        verticalRotation = FastMath.clamp(verticalRotation, _minVerticalRotation, _maxVerticalRotation)
        val vars = TempVars.get()
        val rot = vars.quat1
        val rot2 = vars.quat2
        rot.fromAngleNormalAxis(verticalRotation, leftVector)
        rot2.fromAngleNormalAxis(horizontalRotation, upVector)
        rot2.multLocal(rot)
        target.localRotation = rot2
        vars.release()
    }

    /**
     * move the camera toward or away the target
     */
    protected fun zoomCamera(value: Float) {
        distance = FastMath.clamp(distance + value, _minDistance, _maxDistance)
        camNode.localTranslation = Vector3f(0f, 0f, distance)
    }

    fun setTarget(targetSpatial: Spatial) {
        spatial = targetSpatial
    }

    override fun update(tpf: Float) {
        if (spatial == null) {
            throw IllegalArgumentException("The spatial to follow is null, please use the setTarget method")
        }
        target.localTranslation = spatial!!.worldTranslation
        camNode.lookAt(target.worldTranslation, upVector)

        target.updateLogicalState(tpf)
        target.updateGeometricState()
    }

    /**
     * Sets custom triggers for toggling the rotation of the cam default are
     * new MouseButtonTrigger(MouseInput.BUTTON_LEFT) left mouse button new
     * MouseButtonTrigger(MouseInput.BUTTON_RIGHT) right mouse button
     *
     * @param triggers
     */
    fun setToggleRotationTrigger(vararg triggers: Trigger) {
        _toggleRotateTrigger = triggers as Array<Trigger>
        if (inputManager != null) {
            inputManager!!.deleteMapping(CameraInput.CHASECAM_TOGGLEROTATE)
            initTogleRotateInput()
            inputManager!!.addListener(this, CameraInput.CHASECAM_TOGGLEROTATE)
        }
    }

    /**
     * Sets custom triggers for zooming in the cam default is new
     * MouseAxisTrigger(MouseInput.AXIS_WHEEL, true) mouse wheel up
     *
     * @param triggers
     */
    fun setZoomInTrigger(vararg triggers: Trigger) {
        _zoomInTrigger = triggers as Array<Trigger>
        if (inputManager != null) {
            inputManager!!.deleteMapping(CameraInput.CHASECAM_ZOOMIN)
            inputManager!!.addMapping(CameraInput.CHASECAM_ZOOMIN, *_zoomInTrigger)
            inputManager!!.addListener(this, CameraInput.CHASECAM_ZOOMIN)
        }
    }

    /**
     * Sets custom triggers for zooming out the cam default is new
     * MouseAxisTrigger(MouseInput.AXIS_WHEEL, false) mouse wheel down
     *
     * @param triggers
     */
    fun setZoomOutTrigger(vararg triggers: Trigger) {
        _zoomOutTrigger = triggers as Array<Trigger>
        if (inputManager != null) {
            inputManager!!.deleteMapping(CameraInput.CHASECAM_ZOOMOUT)
            inputManager!!.addMapping(CameraInput.CHASECAM_ZOOMOUT, *_zoomOutTrigger)
            inputManager!!.addListener(this, CameraInput.CHASECAM_ZOOMOUT)
        }
    }

    /**
     * Returns the max zoom distance of the camera (default is 40)
     *
     * @return _maxDistance
     */
    fun getMaxDistance(): Float {
        return _maxDistance
    }

    /**
     * Sets the max zoom distance of the camera (default is 40)
     *
     * @param maxDistance
     */
    fun setMaxDistance(maxDistance: Float) {
        this._maxDistance = maxDistance
        if (initialized) {
            zoomCamera(distance)
        }
    }

    /**
     * Returns the min zoom distance of the camera (default is 1)
     *
     * @return _minDistance
     */
    fun getMinDistance(): Float {
        return _minDistance
    }

    /**
     * Sets the min zoom distance of the camera (default is 1)
     *
     * @param minDistance
     */
    fun setMinDistance(minDistance: Float) {
        this._minDistance = minDistance
        if (initialized) {
            zoomCamera(distance)
        }
    }

    /**
     * @return The maximal vertical rotation angle in radian of the camera
     * around the target
     */
    fun getMaxVerticalRotation(): Float {
        return _maxVerticalRotation
    }

    /**
     * Sets the maximal vertical rotation angle in radian of the camera around
     * the target. Default is Pi/2;
     *
     * @param maxVerticalRotation
     */
    fun setMaxVerticalRotation(maxVerticalRotation: Float) {
        this._maxVerticalRotation = maxVerticalRotation
        if (initialized) {
            rotateCamera()
        }
    }

    /**
     *
     * @return The minimal vertical rotation angle in radian of the camera
     * around the target
     */
    fun getMinVerticalRotation(): Float {
        return _minVerticalRotation
    }

    /**
     * Sets the minimal vertical rotation angle in radian of the camera around
     * the target default is 0;
     *
     * @param minHeight
     */
    fun setMinVerticalRotation(minHeight: Float) {
        this._minVerticalRotation = minHeight
        if (initialized) {
            rotateCamera()
        }
    }

    /**
     * Sets the default distance at start of application
     *
     * @param defaultDistance
     */
    fun setDefaultDistance(defaultDistance: Float) {
        distance = defaultDistance
    }

    /**
     * sets the default horizontal rotation in radian of the camera at start of
     * the application
     *
     * @param angleInRad
     */
    fun setDefaultHorizontalRotation(angleInRad: Float) {
        horizontalRotation = angleInRad
    }

    /**
     * sets the default vertical rotation in radian of the camera at start of
     * the application
     *
     * @param angleInRad
     */
    fun setDefaultVerticalRotation(angleInRad: Float) {
        verticalRotation = angleInRad
    }

    /**
     * invert the vertical axis movement of the mouse
     *
     * @param invertYaxis
     */
    fun setInvertVerticalAxis(invertYaxis: Boolean) {
        this.invertYaxis = invertYaxis
        if (inputManager != null) {
            inputManager!!.deleteMapping(CameraInput.CHASECAM_DOWN)
            inputManager!!.deleteMapping(CameraInput.CHASECAM_UP)
            initVerticalAxisInputs()
            inputManager!!.addListener(this, CameraInput.CHASECAM_DOWN, CameraInput.CHASECAM_UP)
        }
    }

    /**
     * invert the Horizontal axis movement of the mouse
     *
     * @param invertXaxis
     */
    fun setInvertHorizontalAxis(invertXaxis: Boolean) {
        this.invertXaxis = invertXaxis
        if (inputManager != null) {
            inputManager!!.deleteMapping(CameraInput.CHASECAM_MOVELEFT)
            inputManager!!.deleteMapping(CameraInput.CHASECAM_MOVERIGHT)
            initHorizontalAxisInput()
            inputManager!!.addListener(this, CameraInput.CHASECAM_MOVELEFT, CameraInput.CHASECAM_MOVERIGHT)
        }
    }

    private fun initVerticalAxisInputs() {
        if (!invertYaxis) {
            inputManager!!.addMapping(CameraInput.CHASECAM_DOWN, MouseAxisTrigger(MouseInput.AXIS_Y, true))
            inputManager!!.addMapping(CameraInput.CHASECAM_UP, MouseAxisTrigger(MouseInput.AXIS_Y, false))
        } else {
            inputManager!!.addMapping(CameraInput.CHASECAM_DOWN, MouseAxisTrigger(MouseInput.AXIS_Y, false))
            inputManager!!.addMapping(CameraInput.CHASECAM_UP, MouseAxisTrigger(MouseInput.AXIS_Y, true))
        }
    }

    private fun initHorizontalAxisInput() {
        if (!invertXaxis) {
            inputManager!!.addMapping(CameraInput.CHASECAM_MOVELEFT, MouseAxisTrigger(MouseInput.AXIS_X, true))
            inputManager!!.addMapping(CameraInput.CHASECAM_MOVERIGHT, MouseAxisTrigger(MouseInput.AXIS_X, false))
        } else {
            inputManager!!.addMapping(CameraInput.CHASECAM_MOVELEFT, MouseAxisTrigger(MouseInput.AXIS_X, false))
            inputManager!!.addMapping(CameraInput.CHASECAM_MOVERIGHT, MouseAxisTrigger(MouseInput.AXIS_X, true))
        }
    }

    private fun initZoomInput() {
        inputManager!!.addMapping(CameraInput.CHASECAM_ZOOMIN, *_zoomInTrigger)
        inputManager!!.addMapping(CameraInput.CHASECAM_ZOOMOUT, *_zoomOutTrigger)
    }

    private fun initTogleRotateInput() {
        inputManager!!.addMapping(CameraInput.CHASECAM_TOGGLEROTATE, *_toggleRotateTrigger)
    }
}
