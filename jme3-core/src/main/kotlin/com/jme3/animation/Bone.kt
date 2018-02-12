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
import com.jme3.material.MatParamOverride
import com.jme3.math.*
import com.jme3.scene.*
import com.jme3.shader.VarType
import com.jme3.util.SafeArrayList
import com.jme3.util.TempVars
import com.jme3.util.clone.Cloner
import com.jme3.util.clone.JmeCloneable

import java.io.IOException
import java.util.ArrayList

/**
 * `Bone` describes a bone in the bone-weight skeletal animation
 * system. A bone contains a name and an index, as well as relevant
 * transformation data.
 *
 * A bone has 3 sets of transforms :
 * 1. The bind transforms, that are the transforms of the bone when the skeleton
 * is in its rest pose (also called bind pose or T pose in the literature).
 * The bind transforms are expressed in Local space meaning relatively to the
 * parent bone.
 *
 * 2. The Local transforms, that are the transforms of the bone once animation
 * or user transforms has been applied to the bind pose. The local transforms are
 * expressed in Local space meaning relatively to the parent bone.
 *
 * 3. The Model transforms, that are the transforms of the bone relatives to the
 * rootBone of the skeleton. Those transforms are what is needed to apply skinning
 * to the mesh the skeleton controls.
 * Note that there can be several rootBones in a skeleton. The one considered for
 * these transforms is the one that is an ancestor of this bone.
 *
 * @author (kme) Ray Long
 * @author (jme) Kirill Vainer
 * @author (jme) Rémy Bouquet
 */
class Bone : Savable, JmeCloneable {
    /**
     * Returns the name of the bone, set in the constructor.
     *
     * @return The name of the bone, set in the constructor.
     */
    var name: String? = null
        private set
    /**
     * Returns parent bone of this bone, or null if it is a root bone.
     * @return The parent bone of this bone, or null if it is a root bone.
     */
    var parent: Bone? = null
        private set
    /**
     * Returns all the children bones of this bone.
     *
     * @return All the children bones of this bone.
     */
    var children = ArrayList<Bone>()
        private set
    /**
     * If enabled, user can control bone transform with setUserTransforms.
     * Animation transforms are not applied to this bone when enabled.
     */
    private var userControl = false
    /**
     * The attachment node.
     */
    private var attachNode: Node? = null
    /**
     * A geometry animated by this node, used when updating the attachments node.
     */
    private var targetGeometry: Geometry? = null
    /**
     * Bind transform is the local bind transform of this bone. (local space)
     */

    @get:Deprecated("use {@link #getBindPosition()}")
    var worldBindPosition: Vector3f? = null
        private set

    @get:Deprecated("use {@link #getBindRotation() }")
    var worldBindRotation: Quaternion? = null
        private set

    @get:Deprecated("use {@link #getBindScale() }")
    var worldBindScale: Vector3f? = null
        private set

    /**
     * The inverse bind transforms of this bone expressed in model space
     */

    @get:Deprecated("use {@link #getModelBindInversePosition()}")
    var worldBindInversePosition: Vector3f? = null
        private set

    @get:Deprecated("use {@link #getModelBindInverseRotation()}")
    var worldBindInverseRotation: Quaternion? = null
        private set

    @get:Deprecated("use {@link #getModelBindInverseScale()}")
    var worldBindInverseScale: Vector3f? = null
        private set

    /**
     * The local animated or user transform combined with the local bind transform
     */
    /**
     * Returns the local position of the bone, relative to the parent bone.
     *
     * @return The local position of the bone, relative to the parent bone.
     */
    var localPosition = Vector3f()
        private set
    private var localRot = Quaternion()
    private var localScale = Vector3f(1.0f, 1.0f, 1.0f)
    /**
     * The model transforms of this bone
     */
    /**
     * Returns the position of the bone in model space.
     *
     * @return The position of the bone in model space.
     */
    var modelSpacePosition = Vector3f()
        private set
    /**
     * Returns the rotation of the bone in model space.
     *
     * @return The rotation of the bone in model space.
     */
    var modelSpaceRotation = Quaternion()
        private set
    /**
     * Returns the scale of the bone in model space.
     *
     * @return The scale of the bone in model space.
     */
    var modelSpaceScale = Vector3f()
        private set

    // Used for getCombinedTransform
    private var tmpTransform: Transform? = null

    /**
     * Used to handle blending from one animation to another.
     * See [.blendAnimTransforms]
     * on how this variable is used.
     */
    @Transient
    private var currentWeightSum = -1f

    /**
     * Returns the local rotation of the bone, relative to the parent bone.
     *
     * @return The local rotation of the bone, relative to the parent bone.
     */
    /**
     * Sets the rotation of the bone in object space.
     * Warning: you need to call [.setUserControl] with true to be able to do that operation
     * @param rot
     */
    var localRotation: Quaternion
        get() = localRot
        set(rot) {
            when {
                !userControl -> throw IllegalStateException("User control must be on bone to allow user transforms")
                else -> this.localRot.set(rot)
            }
        }

    val modelBindInverseTransform: Transform
        get() {
            val t = Transform()
            t.translation = worldBindInversePosition
            t.rotation = worldBindInverseRotation
            when {
                worldBindInverseScale != null -> t.scale = worldBindInverseScale
            }
            return t
        }

    val bindInverseTransform: Transform
        get() {
            val t = Transform()
            t.translation = worldBindPosition
            t.rotation = worldBindRotation
            when {
                worldBindScale != null -> t.scale = worldBindScale
            }
            return t.invert()
        }

    /**
     * Creates a new bone with the given name.
     *
     * @param name Name to give to this bone
     */
    constructor(name: String?) {
        if (name == null)
            throw IllegalArgumentException("Name cannot be null")

        this.name = name

        worldBindPosition = Vector3f()
        worldBindRotation = Quaternion()
        worldBindScale = Vector3f(1f, 1f, 1f)

        worldBindInversePosition = Vector3f()
        worldBindInverseRotation = Quaternion()
        worldBindInverseScale = Vector3f()
    }

    /**
     * Special-purpose copy constructor.
     *
     *
     * Only copies the name, user control state and bind pose transforms from the original.
     *
     *
     * The rest of the data is *NOT* copied, as it will be
     * generated automatically when the bone is animated.
     *
     * @param source The bone from which to copy the data.
     */
    internal constructor(source: Bone) {
        this.name = source.name

        userControl = source.userControl

        worldBindPosition = source.worldBindPosition!!.clone()
        worldBindRotation = source.worldBindRotation!!.clone()
        worldBindScale = source.worldBindScale!!.clone()

        worldBindInversePosition = source.worldBindInversePosition!!.clone()
        worldBindInverseRotation = source.worldBindInverseRotation!!.clone()
        worldBindInverseScale = source.worldBindInverseScale!!.clone()

        // parent and children will be assigned manually..
    }

    /**
     * Serialization only. Do not use.
     */
    constructor() {}

    override fun jmeClone(): Any {
        return try {
            super.clone() as Bone
        } catch (ex: CloneNotSupportedException) {
            throw AssertionError()
        }

    }

    override fun cloneFields(cloner: Cloner, original: Any) {

        this.parent = cloner.clone<Bone>(parent)
        this.children = cloner.clone(children)

        this.attachNode = cloner.clone<Node>(attachNode)
        this.targetGeometry = cloner.clone<Geometry>(targetGeometry)

        this.worldBindPosition = cloner.clone<Vector3f>(worldBindPosition)
        this.worldBindRotation = cloner.clone<Quaternion>(worldBindRotation)
        this.worldBindScale = cloner.clone<Vector3f>(worldBindScale)

        this.worldBindInversePosition = cloner.clone<Vector3f>(worldBindInversePosition)
        this.worldBindInverseRotation = cloner.clone<Quaternion>(worldBindInverseRotation)
        this.worldBindInverseScale = cloner.clone<Vector3f>(worldBindInverseScale)

        this.localPosition = cloner.clone(localPosition)
        this.localRot = cloner.clone(localRot)
        this.localScale = cloner.clone(localScale)

        this.modelSpacePosition = cloner.clone(modelSpacePosition)
        this.modelSpaceRotation = cloner.clone(modelSpaceRotation)
        this.modelSpaceScale = cloner.clone(modelSpaceScale)

        this.tmpTransform = cloner.clone<Transform>(tmpTransform)
    }

    /**
     * Returns the local scale of the bone, relative to the parent bone.
     *
     * @return The local scale of the bone, relative to the parent bone.
     */
    fun getLocalScale(): Vector3f {
        return localScale
    }

    /**
     * Returns the inverse Bind position of this bone expressed in model space.
     *
     *
     * The inverse bind pose transform of the bone in model space is its "default"
     * transform with no animation applied.
     *
     * @return the inverse bind position of this bone expressed in model space.
     */
    fun getModelBindInversePosition(): Vector3f? {
        return worldBindInversePosition
    }

    /**
     * Returns the inverse bind rotation of this bone expressed in model space.
     *
     *
     * The inverse bind pose transform of the bone in model space is its "default"
     * transform with no animation applied.
     *
     * @return the inverse bind rotation of this bone expressed in model space.
     */
    fun getModelBindInverseRotation(): Quaternion? {
        return worldBindInverseRotation
    }

    /**
     * Returns the inverse world bind pose scale.
     *
     *
     * The inverse bind pose transform of the bone in model space is its "default"
     * transform with no animation applied.
     *
     * @return the inverse world bind pose scale.
     */
    fun getModelBindInverseScale(): Vector3f? {
        return worldBindInverseScale
    }

    /**
     * Returns the bind position expressed in local space (relative to the parent bone).
     *
     *
     * The bind pose transform of the bone in local space is its "default"
     * transform with no animation applied.
     *
     * @return the bind position in local space.
     */
    fun getBindPosition(): Vector3f? {
        return worldBindPosition
    }

    /**
     * Returns the bind rotation expressed in local space (relative to the parent bone).
     *
     *
     * The bind pose transform of the bone in local space is its "default"
     * transform with no animation applied.
     *
     * @return the bind rotation in local space.
     */
    fun getBindRotation(): Quaternion? {
        return worldBindRotation
    }

    /**
     * Returns the  bind scale expressed in local space (relative to the parent bone).
     *
     *
     * The bind pose transform of the bone in local space is its "default"
     * transform with no animation applied.
     *
     * @return the bind scale in local space.
     */
    fun getBindScale(): Vector3f? {
        return worldBindScale
    }

    /**
     * If enabled, user can control bone transform with setUserTransforms.
     * Animation transforms are not applied to this bone when enabled.
     */
    fun setUserControl(enable: Boolean) {
        userControl = enable
    }

    /**
     * Add a new child to this bone. Shouldn't be used by user code.
     * Can corrupt skeleton.
     *
     * @param bone The bone to add
     */
    fun addChild(bone: Bone) {
        children.add(bone)
        bone.parent = this
    }


    @Deprecated("use {@link #updateModelTransforms() }")
    fun updateWorldVectors() {
        updateModelTransforms()
    }


    /**
     * Updates the model transforms for this bone, and, possibly the attach node
     * if not null.
     *
     *
     * The model transform of this bone is computed by combining the parent's
     * model transform with this bones' local transform.
     */
    fun updateModelTransforms() {
        //rotation

        //scale
        //For scale parent scale is not taken into account!
        // worldScale.set(localScale);

        //translation
        //scale and rotation of parent affect bone position
        when {
            currentWeightSum == 1f -> currentWeightSum = -1f
            currentWeightSum != -1f -> {
                // Apply the weight to the local transform
                when (currentWeightSum) {
                    0f -> {
                        localRot.set(worldBindRotation)
                        localPosition.set(worldBindPosition)
                        localScale.set(worldBindScale)
                    }
                    else -> {
                        val invWeightSum = 1f - currentWeightSum
                        localRot.nlerp(worldBindRotation, invWeightSum)
                        localPosition.interpolateLocal(worldBindPosition, invWeightSum)
                        localScale.interpolateLocal(worldBindScale, invWeightSum)
                    }
                }

                // Future invocations of transform blend will start over.
                currentWeightSum = -1f
            }
        }

        when {
            parent != null -> {
                //rotation
                parent!!.modelSpaceRotation.mult(localRot, modelSpaceRotation)

                //scale
                //For scale parent scale is not taken into account!
                // worldScale.set(localScale);
                parent!!.modelSpaceScale.mult(localScale, modelSpaceScale)

                //translation
                //scale and rotation of parent affect bone position
                parent!!.modelSpaceRotation.mult(localPosition, modelSpacePosition)
                modelSpacePosition.multLocal(parent!!.modelSpaceScale)
                modelSpacePosition.addLocal(parent!!.modelSpacePosition)
            }
            else -> {
                modelSpaceRotation.set(localRot)
                modelSpacePosition.set(localPosition)
                modelSpaceScale.set(localScale)
            }
        }

        when {
            attachNode != null -> updateAttachNode()
        }
    }

    /**
     * Update the local transform of the attachments node.
     */
    private fun updateAttachNode() {
        val attachParent = attachNode!!.parent
        when {
            attachParent == null || targetGeometry == null || targetGeometry!!.parent === attachParent && targetGeometry!!.localTransform.isIdentity -> {
                /*
             * The animated meshes are in the same coordinate system as the
             * attachments node: no further transforms are needed.
             */
                attachNode!!.localTranslation = modelSpacePosition
                attachNode!!.localRotation = modelSpaceRotation
                attachNode!!.localScale = modelSpaceScale

            }
            else -> {
                var loopSpatial: Spatial? = targetGeometry
                val combined = Transform(modelSpacePosition, modelSpaceRotation, modelSpaceScale)
                /*
             * Climb the scene graph applying local transforms until the
             * attachments node's parent is reached.
             */
                while (loopSpatial !== attachParent && loopSpatial != null) {
                    val localTransform = loopSpatial.localTransform
                    combined.combineWithParent(localTransform)
                    loopSpatial = loopSpatial.parent
                }
                attachNode!!.localTransform = combined
            }
        }
    }

    /**
     * Updates world transforms for this bone and its children.
     */
    fun update() {
        this.updateModelTransforms()

        children.indices.reversed().forEach { i -> children[i].update() }
    }

    /**
     * Saves the current bone state as its binding pose, including its children.
     */
    internal fun setBindingPose() {
        worldBindPosition!!.set(localPosition)
        worldBindRotation!!.set(localRot)
        worldBindScale!!.set(localScale)

        when (worldBindInversePosition) {
            null -> {
                worldBindInversePosition = Vector3f()
                worldBindInverseRotation = Quaternion()
                worldBindInverseScale = Vector3f()
            }

        // Save inverse derived position/scale/orientation, used for calculate offset transform later
        }

        // Save inverse derived position/scale/orientation, used for calculate offset transform later
        worldBindInversePosition!!.set(modelSpacePosition)
        worldBindInversePosition!!.negateLocal()

        worldBindInverseRotation!!.set(modelSpaceRotation)
        worldBindInverseRotation!!.inverseLocal()

        worldBindInverseScale!!.set(Vector3f.UNIT_XYZ)
        worldBindInverseScale!!.divideLocal(modelSpaceScale)

        children.forEach { b -> b.setBindingPose() }
    }

    /**
     * Reset the bone and its children to bind pose.
     */
    internal fun reset() {
        when {
            !userControl -> {
                localPosition.set(worldBindPosition)
                localRot.set(worldBindRotation)
                localScale.set(worldBindScale)
            }
        }

        children.indices.reversed().forEach { i -> children[i].reset() }
    }

    /**
     * Stores the skinning transform in the specified Matrix4f.
     * The skinning transform applies the animation of the bone to a vertex.
     *
     * This assumes that the world transforms for the entire bone hierarchy
     * have already been computed, otherwise this method will return undefined
     * results.
     *
     * @param outTransform
     */
    internal fun getOffsetTransform(outTransform: Matrix4f, tmp1: Quaternion, tmp2: Vector3f, tmp3: Vector3f, tmp4: Matrix3f) {
        // Computing scale
        val scale = modelSpaceScale.mult(worldBindInverseScale, tmp3)

        // Computing rotation
        val rotate = modelSpaceRotation.mult(worldBindInverseRotation, tmp1)

        // Computing translation
        // Translation depend on rotation and scale
        val translate = modelSpacePosition.add(rotate.mult(scale!!.mult(worldBindInversePosition, tmp2), tmp2), tmp2)

        // Populating the matrix
        outTransform.setTransform(translate, scale, rotate.toRotationMatrix(tmp4))
    }

    /**
     *
     * Sets the transforms of this bone in local space (relative to the parent bone)
     *
     * @param translation the translation in local space
     * @param rotation the rotation in local space
     * @param scale the scale in local space
     */
    fun setUserTransforms(translation: Vector3f, rotation: Quaternion, scale: Vector3f) {
        when {
            !userControl -> throw IllegalStateException("You must call setUserControl(true) in order to setUserTransform to work")
            else -> {
                localPosition.set(worldBindPosition)
                localRot.set(worldBindRotation)
                localScale.set(worldBindScale)

                localPosition.addLocal(translation)
                localRot.multLocal(rotation)
                localScale.multLocal(scale)
            }
        }

    }

    /**
     *
     * @param translation -
     * @param rotation -
     */
    @Deprecated("use {@link #setUserTransformsInModelSpace(com.jme3.math.Vector3f, com.jme3.math.Quaternion) }")
    fun setUserTransformsWorld(translation: Vector3f, rotation: Quaternion) {

    }

    /**
     * Sets the transforms of this bone in model space (relative to the root bone)
     *
     * Must update all bones in skeleton for this to work.
     * @param translation translation in model space
     * @param rotation rotation in model space
     */
    fun setUserTransformsInModelSpace(translation: Vector3f, rotation: Quaternion) {
        when {
            !userControl -> throw IllegalStateException("You must call setUserControl(true) in order to setUserTransformsInModelSpace to work")

        // TODO: add scale here ???

        //if there is an attached Node we need to set its local transforms too.
            else -> {
                modelSpacePosition.set(translation)
                modelSpaceRotation.set(rotation)

                //if there is an attached Node we need to set its local transforms too.
                when {
                    attachNode != null -> {
                        attachNode!!.localTranslation = translation
                        attachNode!!.localRotation = rotation
                    }
                }
            }
        }

    }

    /**
     * Returns the local transform of this bone combined with the given position and rotation
     * @param position a position
     * @param rotation a rotation
     */
    fun getCombinedTransform(position: Vector3f, rotation: Quaternion): Transform {
        when (tmpTransform) {
            null -> tmpTransform = Transform()
        }
        rotation.mult(localPosition, tmpTransform!!.translation).addLocal(position)
        tmpTransform!!.setRotation(rotation).rotation.multLocal(localRot)
        return tmpTransform!!
    }

    /**
     * Access the attachments node of this bone. If this bone doesn't already
     * have an attachments node, create one. Models and effects attached to the
     * attachments node will follow this bone's motions.
     *
     * @param boneIndex this bone's index in its skeleton (0)
     * @param targets a list of geometries animated by this bone's skeleton (not
     * null, unaffected)
     */
    internal fun getAttachmentsNode(boneIndex: Int, targets: SafeArrayList<Geometry>): Node {
        targetGeometry = null
        /*
         * Search for a geometry animated by this particular bone.
         */
        for (geometry in targets) {
            val mesh = geometry.mesh
            if (mesh != null && mesh.isAnimatedByBone(boneIndex)) {
                targetGeometry = geometry
                break
            }
        }

        when (attachNode) {
            null -> {
                attachNode = Node(name!! + "_attachnode")
                attachNode!!.setUserData("AttachedBone", this)
                //We don't want the node to have a numBone set by a parent node so we force it to null
                attachNode!!.addMatParamOverride(MatParamOverride(VarType.Int, "NumberOfBones", null))
            }
        }

        return attachNode!!
    }

    /**
     * Used internally after model cloning.
     * @param attachNode
     */
    internal fun setAttachmentsNode(attachNode: Node) {
        this.attachNode = attachNode
    }

    /**
     * Sets the local animation transform of this bone.
     * Bone is assumed to be in bind pose when this is called.
     */
    internal fun setAnimTransforms(translation: Vector3f, rotation: Quaternion, scale: Vector3f?) {
        when {
            userControl -> return

        //        localPos.addLocal(translation);
        //        localRot.multLocal(rotation);
        //localRot = localRot.mult(rotation);
            else -> {
                localPosition.set(worldBindPosition).addLocal(translation)
                localRot.set(worldBindRotation).multLocal(rotation)

                when {
                    scale != null -> localScale.set(worldBindScale).multLocal(scale)
                }
            }
        }

    }

    /**
     * Blends the given animation transform onto the bone's local transform.
     *
     *
     * Subsequent calls of this method stack up, with the final transformation
     * of the bone computed at [.updateModelTransforms] which resets
     * the stack.
     *
     *
     * E.g. a single transform blend with weight = 0.5 followed by an
     * updateModelTransforms() call will result in final transform = transform * 0.5.
     * Two transform blends with weight = 0.5 each will result in the two
     * transforms blended together (nlerp) with blend = 0.5.
     *
     * @param translation The translation to blend in
     * @param rotation The rotation to blend in
     * @param scale The scale to blend in
     * @param weight The weight of the transform to apply. Set to 1.0 to prevent
     * any other transform from being applied until updateModelTransforms().
     */
    internal fun blendAnimTransforms(translation: Vector3f, rotation: Quaternion, scale: Vector3f?, weight: Float) {// Ensures no new weights will be blended in the future.

        // Ensures no new weights will be blended in the future.
// The weight is already set.
        // Blend in the new transform.
// Set the transform fully
        // Set the weight. It will be applied in updateModelTransforms().
        // Set the weight. It will be applied in updateModelTransforms().
// More than 2 transforms are being blended
        // Do not apply this transform at all.
        when {
            userControl -> return
        // Ensures no new weights will be blended in the future.

        // Ensures no new weights will be blended in the future.
// The weight is already set.
        // Blend in the new transform.
// Set the transform fully
        // Set the weight. It will be applied in updateModelTransforms().
        // Set the weight. It will be applied in updateModelTransforms().

        // More than 2 transforms are being blended
            else -> when (weight) {
                0f -> // Do not apply this transform at all.
                    return
                else -> when (currentWeightSum) {
                    1f -> return  // More than 2 transforms are being blended
                    -1f, 0f -> {
                        // Set the transform fully
                        localPosition.set(worldBindPosition).addLocal(translation)
                        localRot.set(worldBindRotation).multLocal(rotation)
                        when {
                            scale != null -> localScale.set(worldBindScale).multLocal(scale)
                        }
                        // Set the weight. It will be applied in updateModelTransforms().
                        // Set the weight. It will be applied in updateModelTransforms().
                        currentWeightSum = weight
                    }
                    else -> {
                        // The weight is already set.
                        // Blend in the new transform.
                        val vars = TempVars.get()

                        val tmpV = vars.vect1
                        val tmpV2 = vars.vect2
                        val tmpQ = vars.quat1

                        tmpV.set(worldBindPosition).addLocal(translation)
                        localPosition.interpolateLocal(tmpV, weight)

                        tmpQ.set(worldBindRotation).multLocal(rotation)
                        localRot.nlerp(tmpQ, weight)

                        when {
                            scale != null -> {
                                tmpV2.set(worldBindScale).multLocal(scale)
                                localScale.interpolateLocal(tmpV2, weight)
                            }

                        // Ensures no new weights will be blended in the future.
                        }

                        // Ensures no new weights will be blended in the future.
                        currentWeightSum = 1f

                        vars.release()
                    }
                }
            }
        }

    }

    /**
     * Sets local bind transform for bone.
     * Call setBindingPose() after all of the skeleton bones' bind transforms are set to save them.
     */
    fun setBindTransforms(translation: Vector3f, rotation: Quaternion, scale: Vector3f?) {
        worldBindPosition!!.set(translation)
        worldBindRotation!!.set(rotation)
        //ogre.xml can have null scale values breaking this if the check is removed
        when {
            scale != null -> worldBindScale!!.set(scale)
        }

        localPosition.set(translation)
        localRot.set(rotation)
        when {
            scale != null -> localScale.set(scale)
        }
    }

    private fun toString(depth: Int): String {
        val sb = StringBuilder()
        (0 until depth).forEach { _ -> sb.append('-') }

        sb.append(name).append(" bone\n")
        children.forEach { child -> sb.append(child.toString(depth + 1)) }
        return sb.toString()
    }

    override fun toString(): String {
        return this.toString(0)
    }

    @Throws(IOException::class)
    override fun read(im: JmeImporter) {
        val input = im.getCapsule(this)

        name = input.readString("name", null)
        val ver = input.getSavableVersion(Bone::class.java)
        when {
            ver < 2 -> {
                worldBindPosition = input.readSavable("initialPos", null) as Vector3f
                worldBindRotation = input.readSavable("initialRot", null) as Quaternion
                worldBindScale = input.readSavable("initialScale", Vector3f(1.0f, 1.0f, 1.0f)) as Vector3f
            }
            else -> {
                worldBindPosition = input.readSavable("bindPos", null) as Vector3f
                worldBindRotation = input.readSavable("bindRot", null) as Quaternion
                worldBindScale = input.readSavable("bindScale", Vector3f(1.0f, 1.0f, 1.0f)) as Vector3f
            }
        }

        attachNode = input.readSavable("attachNode", null) as Node
        targetGeometry = input.readSavable("targetGeometry", null) as Geometry

        localPosition.set(worldBindPosition)
        localRot.set(worldBindRotation)
        localScale.set(worldBindScale)

        val childList = input.readSavableArrayList("children", null)
        childList.indices.reversed().forEach { i -> this.addChild(childList[i] as Bone) }

        // NOTE: Parent skeleton will call update() then setBindingPose()
        // after Skeleton has been de-serialized.
        // Therefore, worldBindInversePos and worldBindInverseRot
        // will be reconstructed based on that information.
    }

    @Throws(IOException::class)
    override fun write(ex: JmeExporter) {
        val output = ex.getCapsule(this)

        output.write(name, "name", null)
        output.write(attachNode, "attachNode", null)
        output.write(targetGeometry, "targetGeometry", null)
        output.write(worldBindPosition, "bindPos", null)
        output.write(worldBindRotation, "bindRot", null)
        output.write(worldBindScale, "bindScale", Vector3f(1.0f, 1.0f, 1.0f))
        output.writeSavableArrayList(children, "children", null)
    }

    /**
     * Sets the position of the bone in object space.
     * Warning: you need to call [.setUserControl] with true to be able to do that operation
     * @param pos
     */
    fun setLocalTranslation(pos: Vector3f) {
        when {
            !userControl -> throw IllegalStateException("User control must be on bone to allow user transforms")
            else -> this.localPosition.set(pos)
        }
    }

    /**
     * Sets the scale of the bone in object space.
     * Warning: you need to call [.setUserControl] with true to be able to do that operation
     * @param scale the scale to apply
     */
    fun setLocalScale(scale: Vector3f) {
        when {
            !userControl -> throw IllegalStateException("User control must be on bone to allow user transforms")
            else -> this.localScale.set(scale)
        }
    }

    /**
     * returns true if this bone can be directly manipulated by the user.
     * @see .setUserControl
     * @return
     */
    fun hasUserControl(): Boolean {
        return userControl
    }

    companion object {

        // Version #2: Changed naming of transforms as they were misleading
        val SAVABLE_VERSION = 2
    }
}
