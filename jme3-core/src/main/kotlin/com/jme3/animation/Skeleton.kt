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
import com.jme3.math.Matrix4f
import com.jme3.util.TempVars
import com.jme3.util.clone.JmeCloneable
import com.jme3.util.clone.Cloner
import java.io.IOException
import java.util.ArrayList

/**
 * `Skeleton` is a convenience class for managing a bone hierarchy.
 * Skeleton updates the world transforms to reflect the current local
 * animated matrixes.
 *
 * @author (kme) Ray Long
 * @author (jme) Kirill Vainer
 */
class Skeleton : Savable, JmeCloneable {

    /**
     * returns the array of all root bones of this skeleton
     * @return
     */
    var roots: Array<Bone>? = null
        private set
    private var boneList: Array<Bone>? = null

    /**
     * Contains the skinning matrices, multiplying it by a vertex affected by a bone
     * will cause it to go to the animated position.
     */
    @Transient
    private var skinningMatrixes: Array<Matrix4f>? = null

    /**
     * returns the number of bones of this skeleton
     * @return
     */
    val boneCount: Int
        get() = boneList!!.size

    /**
     * Creates a skeleton from a bone list.
     * The root bones are found automatically.
     *
     *
     * Note that using this constructor will cause the bones in the list
     * to have their bind pose recomputed based on their local transforms.
     *
     * @param boneList The list of bones to manage by this Skeleton
     */
    constructor(boneList: Array<Bone>) {
        this.boneList = boneList

        val rootBoneList = boneList.indices.reversed()
                .asSequence()
                .map { boneList[it] }
                .filter { it.parent == null }
                .toList()

        roots = rootBoneList.toTypedArray()

        createSkinningMatrices()

        roots!!.indices.reversed().forEach { i ->
            val rootBone = roots!![i]
            rootBone.update()
            rootBone.setBindingPose()
        }
    }

    /**
     * Special-purpose copy constructor.
     *
     *
     * Shallow copies bind pose data from the source skeleton, does not
     * copy any other data.
     *
     * @param source The source Skeleton to copy from
     */
    constructor(source: Skeleton) {
        val sourceList = source.boneList
//        boneList = arrayOfNulls(sourceList!!.size)
        boneList = Array(size = sourceList!!.size, init = { Bone() })
        sourceList.indices.forEach { i -> boneList!![i] = Bone(sourceList[i]) }

//        roots = arrayOfNulls(source.roots!!.size)
        roots = Array(size = source.roots!!.size, init = { Bone() })
        roots!!.indices.forEach { i -> roots!![i] = recreateBoneStructure(source.roots!![i]) }
        createSkinningMatrices()

        roots!!.indices.reversed().forEach { i -> roots!![i].update() }
    }

    /**
     * Serialization only. Do not use.
     */
    constructor() {}

    override fun jmeClone(): Any {
        return try {
            super.clone() as Skeleton
        } catch (ex: CloneNotSupportedException) {
            throw AssertionError()
        }

    }

    override fun cloneFields(cloner: Cloner, original: Any) {
        this.roots = cloner.clone<Array<Bone>>(roots)
        this.boneList = cloner.clone<Array<Bone>>(boneList)
        this.skinningMatrixes = cloner.clone<Array<Matrix4f>>(skinningMatrixes)
    }

    private fun createSkinningMatrices() {
//        skinningMatrixes = arrayOfNulls(boneList!!.size)
        skinningMatrixes = Array(size = boneList!!.size, init = { Matrix4f() })
        skinningMatrixes!!.indices.forEach { i -> skinningMatrixes!![i] = Matrix4f() }
    }

    private fun recreateBoneStructure(sourceRoot: Bone): Bone {
        val targetRoot = getBone(sourceRoot.name)
        val children = sourceRoot.children
        children.indices.forEach { i ->
            val sourceChild = children[i]
            // find my version of the child
            val targetChild = getBone(sourceChild.name)
            targetRoot!!.addChild(targetChild!!)
            recreateBoneStructure(sourceChild)
        }

        return targetRoot!!
    }

    /**
     * Updates world transforms for all bones in this skeleton.
     * Typically called after setting local animation transforms.
     */
    fun updateWorldVectors() {
        roots!!.indices.reversed().forEach { i -> roots!![i].update() }
    }

    /**
     * Saves the current skeleton state as its binding pose.
     */
    fun setBindingPose() {
        roots!!.indices.reversed().forEach { i -> roots!![i].setBindingPose() }
    }

    /**
     * Reset the skeleton to bind pose.
     */
    fun reset() {
        roots!!.indices.reversed().forEach { i -> roots!![i].reset() }
    }

    /**
     * Reset the skeleton to bind pose and updates the bones
     */
    fun resetAndUpdate() {
        roots!!.indices.reversed().forEach { i ->
            val rootBone = roots!![i]
            rootBone.reset()
            rootBone.update()
        }
    }

    /**
     * return a bone for the given index
     * @param index
     * @return
     */
    fun getBone(index: Int): Bone {
        return boneList!![index]
    }

    /**
     * returns the bone with the given name
     * @param name
     * @return
     */
    fun getBone(name: String?): Bone? {
        return boneList!!.indices
                .firstOrNull { boneList!![it].name == name }
                ?.let { boneList!![it] }
    }

    /**
     * returns the bone index of the given bone
     * @param bone
     * @return
     */
    fun getBoneIndex(bone: Bone): Int {

        return boneList!!.indices.firstOrNull { boneList!![it] == bone }
                ?: -1
    }

    /**
     * returns the bone index of the bone that has the given name
     * @param name
     * @return
     */
    fun getBoneIndex(name: String): Int {

        return boneList!!.indices.firstOrNull { boneList!![it].name == name }
                ?: -1
    }

    /**
     * Compute the skining matrices for each bone of the skeleton that would be used to transform vertices of associated meshes
     * @return
     */
    fun computeSkinningMatrices(): Array<Matrix4f> {
        val vars = TempVars.get()
        boneList!!.indices.forEach { i -> boneList!![i].getOffsetTransform(skinningMatrixes!![i], vars.quat1, vars.vect1, vars.vect2, vars.tempMat3) }
        vars.release()
        return skinningMatrixes!!
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("Skeleton - ").append(boneList!!.size).append(" bones, ").append(roots!!.size).append(" roots\n")
        roots!!.forEach { rootBone -> sb.append(rootBone.toString()) }
        return sb.toString()
    }

    @Throws(IOException::class)
    override fun read(im: JmeImporter) {
        val input = im.getCapsule(this)

        val boneRootsAsSav = input.readSavableArray("rootBones", null)
//        roots = arrayOfNulls(boneRootsAsSav.size)
        roots = Array(size = boneRootsAsSav.size, init = { Bone() })
        System.arraycopy(boneRootsAsSav, 0, roots!!, 0, boneRootsAsSav.size)

        val boneListAsSavable = input.readSavableArray("boneList", null)
//        boneList = arrayOfNulls(boneListAsSavable.size)
        boneList = Array(size = boneListAsSavable.size, init = { Bone() })
        System.arraycopy(boneListAsSavable, 0, boneList!!, 0, boneListAsSavable.size)

        createSkinningMatrices()

        roots!!.forEach { rootBone ->
            rootBone.reset()
            rootBone.update()
            rootBone.setBindingPose()
        }
    }

    @Throws(IOException::class)
    override fun write(ex: JmeExporter) {
        val output = ex.getCapsule(this)
        output.write(roots, "rootBones", null)
        output.write(boneList, "boneList", null)
    }
}
