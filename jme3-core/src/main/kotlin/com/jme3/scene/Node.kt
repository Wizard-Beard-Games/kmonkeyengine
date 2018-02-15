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
package com.jme3.scene

import com.jme3.bounding.BoundingVolume
import com.jme3.collision.Collidable
import com.jme3.collision.CollisionResults
import com.jme3.export.JmeExporter
import com.jme3.export.JmeImporter
import com.jme3.export.Savable
import com.jme3.material.Material
import com.jme3.util.SafeArrayList
import com.jme3.util.TempVars
import com.jme3.util.clone.Cloner
import java.io.IOException
import java.util.ArrayList
import java.util.Queue
import java.util.logging.Level
import java.util.logging.Logger


/**
 * `Node` defines an internal node of a scene graph. The internal
 * node maintains a collection of children and handles merging said children
 * into a single bound to allow for very fast culling of multiple nodes. Node
 * allows for any number of children to be attached.
 *
 * @author Mark Powell
 * @author Gregg Patton
 * @author Joshua Slack
 */
open class Node
/**
 * Constructor instantiates a new `Node` with a default empty
 * list for containing children.
 *
 * @param name the name of the scene element. This is required for
 * identification and comparison purposes.
 */
@JvmOverloads constructor(name: String? = null) : Spatial(name) {

    /**
     * This node's children.
     */
    protected var children: SafeArrayList<Spatial>? = SafeArrayList(Spatial::class.java)

    /**
     * If this node is a root, this list will contain the current
     * set of children (and children of children) that require
     * updateLogicalState() to be called as indicated by their
     * requiresUpdate() method.
     */
    private var updateList: SafeArrayList<Spatial>? = null
    /**
     * False if the update list requires rebuilding.  This is Node.class
     * specific and therefore not included as part of the Spatial update flags.
     * A flag is used instead of nulling the updateList to avoid reallocating
     * a whole list every time the scene graph changes.
     */
    private var updateListValid = false

    /**
     *
     * `getQuantity` returns the number of children this node
     * maintains.
     *
     * @return the number of children this node maintains.
     */
    val quantity: Int
        get() = children!!.size

    init {
        // For backwards compatibility, only clear the "requires
        // update" flag if we are not a subclass of Node.
        // This prevents subclass from silently failing to receive
        // updates when they upgrade.
        setRequiresUpdates(Node::class.java != javaClass)
    }

    override fun setTransformRefresh() {
        super.setTransformRefresh()
        for (child in children!!.array) {
            if (child.refreshFlags and Spatial.RF_TRANSFORM != 0)
                continue

            child.setTransformRefresh()
        }
    }

    override fun setLightListRefresh() {
        super.setLightListRefresh()
        for (child in children!!.array) {
            if (child.refreshFlags and Spatial.RF_LIGHTLIST != 0)
                continue

            child.setLightListRefresh()
        }
    }

    override fun setMatParamOverrideRefresh() {
        super.setMatParamOverrideRefresh()
        for (child in children!!.array) {
            if (child.refreshFlags and Spatial.RF_MATPARAM_OVERRIDE != 0) {
                continue
            }

            child.setMatParamOverrideRefresh()
        }
    }

    override fun updateWorldBound() {
        super.updateWorldBound()
        // for a node, the world bound is a combination of all its children
        // bounds
        var resultBound: BoundingVolume? = null
        for (child in children!!.array) {
            // child bound is assumed to be updated
            assert(child.refreshFlags and Spatial.RF_BOUND == 0)
            if (resultBound != null) {
                // merge current world bound with child world bound
                resultBound.mergeLocal(child.getWorldBound())
            } else {
                // set world bound to first non-null child world bound
                if (child.getWorldBound() != null) {
                    resultBound = child.getWorldBound().clone(this.worldBound)
                }
            }
        }
        this.worldBound = resultBound
    }

    override fun setParent(parent: Node?) {
        if (this.parent == null && parent != null) {
            // We were a root before and now we aren't... make sure if
            // we had an updateList then we clear it completely to
            // avoid holding the dead array.
            updateList = null
            updateListValid = false
        }
        super.setParent(parent)
    }

    private fun addUpdateChildren(results: SafeArrayList<Spatial>) {
        for (child in children!!.array) {
            if (child.requiresUpdates()) {
                results.add(child)
            }
            (child as? Node)?.addUpdateChildren(results)
        }
    }

    /**
     * Called to invalidate the root node's update list.  This is
     * called whenever a spatial is attached/detached as well as
     * when a control is added/removed from a Spatial in a way
     * that would change state.
     */
    internal fun invalidateUpdateList() {
        updateListValid = false
        if (parent != null) {
            parent.invalidateUpdateList()
        }
    }

    private fun getUpdateList(): SafeArrayList<Spatial>? {
        if (updateListValid) {
            return updateList
        }
        if (updateList == null) {
            updateList = SafeArrayList(Spatial::class.java)
        } else {
            updateList!!.clear()
        }

        // Build the list
        addUpdateChildren(updateList!!)
        updateListValid = true
        return updateList
    }

    override fun updateLogicalState(tpf: Float) {
        super.updateLogicalState(tpf)

        // Only perform updates on children if we are the
        // root and then only peform updates on children we
        // know to require updates.
        // So if this isn't the root, abort.
        if (parent != null) {
            return
        }

        for (s in getUpdateList()!!.array) {
            s.updateLogicalState(tpf)
        }
    }

    override fun updateGeometricState() {
        if (refreshFlags == 0) {
            // This branch has no geometric state that requires updates.
            return
        }
        if (refreshFlags and Spatial.RF_LIGHTLIST != 0) {
            updateWorldLightList()
        }
        if (refreshFlags and Spatial.RF_TRANSFORM != 0) {
            // combine with parent transforms- same for all spatial
            // subclasses.
            updateWorldTransforms()
        }
        if (refreshFlags and Spatial.RF_MATPARAM_OVERRIDE != 0) {
            updateMatParamOverrides()
        }

        refreshFlags = refreshFlags and Spatial.RF_CHILD_LIGHTLIST.inv()
        if (!children!!.isEmpty()) {
            // the important part- make sure child geometric state is refreshed
            // first before updating own world bound. This saves
            // a round-trip later on.
            // NOTE 9/19/09
            // Although it does save a round trip,
            for (child in children!!.array) {
                child.updateGeometricState()
            }
        }

        if (refreshFlags and Spatial.RF_BOUND != 0) {
            updateWorldBound()
        }

        assert(refreshFlags == 0)
    }

    /**
     * `getTriangleCount` returns the number of triangles contained
     * in all sub-branches of this node that contain geometry.
     *
     * @return the triangle count of this branch.
     */
    override fun getTriangleCount(): Int {
        var count = 0
        if (children != null) {
            for (i in children!!.indices) {
                count += children!![i].triangleCount
            }
        }

        return count
    }

    /**
     * `getVertexCount` returns the number of vertices contained
     * in all sub-branches of this node that contain geometry.
     *
     * @return the vertex count of this branch.
     */
    override fun getVertexCount(): Int {
        var count = 0
        if (children != null) {
            for (i in children!!.indices) {
                count += children!![i].vertexCount
            }
        }

        return count
    }

    /**
     * `attachChild` attaches a child to this node. This node
     * becomes the child's parent. The current number of children maintained is
     * returned.
     * <br></br>
     * If the child already had a parent it is detached from that former parent.
     *
     * @param child
     * the child to attach to this node.
     * @return the number of children maintained by this node.
     * @throws IllegalArgumentException if child is null.
     */
    fun attachChild(child: Spatial): Int {
        return attachChildAt(child, children!!.size)
    }

    /**
     *
     * `attachChildAt` attaches a child to this node at an index. This node
     * becomes the child's parent. The current number of children maintained is
     * returned.
     * <br></br>
     * If the child already had a parent it is detached from that former parent.
     *
     * @param child
     * the child to attach to this node.
     * @return the number of children maintained by this node.
     * @throws NullPointerException if child is null.
     */
    fun attachChildAt(child: Spatial?, index: Int): Int {
        if (child == null)
            throw NullPointerException()

        if (child.getParent() != this && child !== this) {
            if (child.getParent() != null) {
                child.getParent().detachChild(child)
            }
            child.setParent(this)
            children!!.add(index, child)
            // XXX: Not entirely correct? Forces bound update up the
            // tree stemming from the attached child. Also forces
            // transform update down the tree-
            child.setTransformRefresh()
            child.setLightListRefresh()
            child.setMatParamOverrideRefresh()
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Child ({0}) attached to this node ({1})",
                        arrayOf<Any>(child.getName(), getName()))
            }
            invalidateUpdateList()
        }
        return children!!.size
    }

    /**
     * `detachChild` removes a given child from the node's list.
     * This child will no longer be maintained.
     *
     * @param child
     * the child to remove.
     * @return the index the child was at. -1 if the child was not in the list.
     */
    fun detachChild(child: Spatial?): Int {
        if (child == null)
            throw NullPointerException()

        if (child.getParent() == this) {
            val index = children!!.indexOf(child)
            if (index != -1) {
                detachChildAt(index)
            }
            return index
        }

        return -1
    }

    /**
     * `detachChild` removes a given child from the node's list.
     * This child will no longe be maintained. Only the first child with a
     * matching name is removed.
     *
     * @param childName
     * the child to remove.
     * @return the index the child was at. -1 if the child was not in the list.
     */
    fun detachChildNamed(childName: String?): Int {
        if (childName == null)
            throw NullPointerException()

        var x = 0
        val max = children!!.size
        while (x < max) {
            val child = children!![x]
            if (childName == child.getName()) {
                detachChildAt(x)
                return x
            }
            x++
        }
        return -1
    }

    /**
     *
     * `detachChildAt` removes a child at a given index. That child
     * is returned for saving purposes.
     *
     * @param index
     * the index of the child to be removed.
     * @return the child at the supplied index.
     */
    fun detachChildAt(index: Int): Spatial? {
        val child = children!!.removeAt(index)
        if (child != null) {
            child.setParent(null)
            logger.log(Level.FINE, "{0}: Child removed.", this.toString())

            // since a child with a bound was detached;
            // our own bound will probably change.
            setBoundRefresh()

            // our world transform no longer influences the child.
            // XXX: Not necessary? Since child will have transform updated
            // when attached anyway.
            child.setTransformRefresh()
            // lights are also inherited from parent
            child.setLightListRefresh()
            child.setMatParamOverrideRefresh()

            invalidateUpdateList()
        }
        return child
    }

    /**
     *
     * `detachAllChildren` removes all children attached to this
     * node.
     */
    fun detachAllChildren() {
        // Note: this could be a bit more efficient if it delegated
        // to a private method that avoided setBoundRefresh(), etc.
        // for every child and instead did one in here at the end.
        for (i in children!!.indices.reversed()) {
            detachChildAt(i)
        }
        logger.log(Level.FINE, "{0}: All children removed.", this.toString())
    }

    /**
     * `getChildIndex` returns the index of the given spatial
     * in this node's list of children.
     * @param sp
     * The spatial to look up
     * @return
     * The index of the spatial in the node's children, or -1
     * if the spatial is not attached to this node
     */
    fun getChildIndex(sp: Spatial): Int {
        return children!!.indexOf(sp)
    }

    /**
     * More efficient than e.g detaching and attaching as no updates are needed.
     *
     * @param index1 The index of the first child to swap
     * @param index2 The index of the second child to swap
     */
    fun swapChildren(index1: Int, index2: Int) {
        val c2 = children!![index2]
        val c1 = children!!.removeAt(index1)
        children!!.add(index1, c2)
        children!!.removeAt(index2)
        children!!.add(index2, c1)
    }

    /**
     *
     * `getChild` returns a child at a given index.
     *
     * @param i
     * the index to retrieve the child from.
     * @return the child at a specified index.
     */
    fun getChild(i: Int): Spatial {
        return children!![i]
    }

    /**
     * `getChild` returns the first child found with exactly the
     * given name (case sensitive.) This method does a depth first recursive
     * search of all descendants of this node, it will return the first spatial
     * found with a matching name.
     *
     * @param name
     * the name of the child to retrieve. If null, we'll return null.
     * @return the child if found, or null.
     */
    fun getChild(name: String?): Spatial? {
        if (name == null)
            return null

        for (child in children!!.array) {
            if (name == child.getName()) {
                return child
            } else if (child is Node) {
                val out = child.getChild(name)
                if (out != null) {
                    return out
                }
            }
        }
        return null
    }

    /**
     * determines if the provided Spatial is contained in the children list of
     * this node.
     *
     * @param spat
     * the child object to look for.
     * @return true if the object is contained, false otherwise.
     */
    fun hasChild(spat: Spatial): Boolean {
        if (children!!.contains(spat))
            return true

        for (child in children!!.array) {
            if (child is Node && child.hasChild(spat))
                return true
        }

        return false
    }

    /**
     * Returns all children to this node. Note that modifying that given
     * list is not allowed.
     *
     * @return a list containing all children to this node
     */
    fun getChildren(): List<Spatial>? {
        return children
    }

    override fun setMaterial(mat: Material) {
        for (i in children!!.indices) {
            children!![i].setMaterial(mat)
        }
    }

    override fun setLodLevel(lod: Int) {
        super.setLodLevel(lod)
        for (child in children!!.array) {
            child.setLodLevel(lod)
        }
    }

    override fun collideWith(other: Collidable, results: CollisionResults): Int {
        var total = 0
        // optimization: try collideWith BoundingVolume to avoid possibly redundant tests on children
        // number 4 in condition is somewhat arbitrary. When there is only one child, the boundingVolume test is redundant at all.
        // The idea is when there are few children, it can be too expensive to test boundingVolume first.
        /*
        I'm removing this change until some issues can be addressed and I really
        think it needs to be implemented a better way anyway.
        First, it causes issues for anyone doing collideWith() with BoundingVolumes
        and expecting it to trickle down to the children.  For example, children
        with BoundingSphere bounding volumes and collideWith(BoundingSphere).  Doing
        a collision check at the parent level then has to do a BoundingSphere to BoundingBox
        collision which isn't resolved.  (Having to come up with a collision point in that
        case is tricky and the first sign that this is the wrong approach.)
        Second, the rippling changes this caused to 'optimize' collideWith() for this
        special use-case are another sign that this approach was a bit dodgy.  The whole
        idea of calculating a full collision just to see if the two shapes collide at all
        is very wasteful.
        A proper implementation should support a simpler boolean check that doesn't do
        all of that calculation.  For example, if 'other' is also a BoundingVolume (ie: 99.9%
        of all non-Ray cases) then a direct BV to BV intersects() test can be done.  So much
        faster.  And if 'other' _is_ a Ray then the BV.intersects(Ray) call can be done.
        I don't have time to do it right now but I'll at least un-break a bunch of peoples'
        code until it can be 'optimized' properly.  Hopefully it's not too late to back out
        the other dodgy ripples this caused.  -pspeed (hindsight-expert ;))
        Note: the code itself is relatively simple to implement but I don't have time to
        a) test it, and b) see if '> 4' is still a decent check for it.  Could be it's fast
        enough to do all the time for > 1.
        if (children.size() > 4)
        {
          BoundingVolume bv = this.getWorldBound();
          if (bv==null) return 0;

          // collideWith without CollisionResults parameter used to avoid allocation when possible
          if (bv.collideWith(other) == 0) return 0;
        }
        */
        for (child in children!!.array) {
            total += child.collideWith(other, results)
        }
        return total
    }


    /**
     * Returns flat list of Spatials implementing the specified class AND
     * with name matching the specified pattern.
     *  <P>
     * Note that we are *matching* the pattern, therefore the pattern
     * must match the entire pattern (i.e. it behaves as if it is sandwiched
     * between "^" and "$").
     * You can set regex modes, like case insensitivity, by using the (?X)
     * or (?X:Y) constructs.
    </P> *  <P>
     * By design, it is always safe to code loops like:<CODE><PRE>
     * for (Spatial spatial : node.descendantMatches(AClass.class, "regex"))
    </PRE></CODE> *
    </P> *  <P>
     * "Descendants" does not include self, per the definition of the word.
     * To test for descendants AND self, you must do a
     * `node.matches(aClass, aRegex)` +
     * `node.descendantMatches(aClass, aRegex)`.
    </P> * <P>
     *
     * @param spatialSubclass Subclass which matching Spatials must implement.
     * Null causes all Spatials to qualify.
     * @param nameRegex  Regular expression to match Spatial name against.
     * Null causes all Names to qualify.
     * @return Non-null, but possibly 0-element, list of matching Spatials (also Instances extending Spatials).
     *
     * @see java.util.regex.Pattern
     *
     * @see Spatial.matches
    </P> */
    fun <T : Spatial> descendantMatches(
            spatialSubclass: Class<T>?, nameRegex: String?): List<T> {
        val newList = ArrayList<T>()
        if (quantity < 1) return newList
        for (child in getChildren()!!) {
            if (child.matches(spatialSubclass, nameRegex))
                newList.add(child as T)
            if (child is Node)
                newList.addAll(child.descendantMatches(
                        spatialSubclass, nameRegex))
        }
        return newList
    }

    /**
     * Convenience wrapper.
     *
     * @see .descendantMatches
     */
    fun <T : Spatial> descendantMatches(
            spatialSubclass: Class<T>): List<T> {
        return descendantMatches(spatialSubclass, null)
    }

    /**
     * Convenience wrapper.
     *
     * @see .descendantMatches
     */
    fun <T : Spatial> descendantMatches(nameRegex: String): List<T> {
        return descendantMatches(null, nameRegex)
    }

    override fun clone(cloneMaterials: Boolean): Node {
        val nodeClone = super.clone(cloneMaterials) as Node
        //        nodeClone.children = new ArrayList<Spatial>();
        //        for (Spatial child : children){
        //            Spatial childClone = child.clone();
        //            childClone.parent = nodeClone;
        //            nodeClone.children.add(childClone);
        //        }

        // Reset the fields of the clone that should be in a 'new' state.
        nodeClone.updateList = null
        nodeClone.updateListValid = false // safe because parent is nulled out in super.clone()
        return nodeClone
    }

    override fun deepClone(): Spatial {
        val nodeClone = super.deepClone() as Node

        // Reset the fields of the clone that should be in a 'new' state.
        nodeClone.updateList = null
        nodeClone.updateListValid = false // safe because parent is nulled out in super.clone()

        return nodeClone
    }

    fun oldDeepClone(): Spatial {
        val nodeClone = super.clone() as Node
        nodeClone.children = SafeArrayList(Spatial::class.java)
        for (child in children!!) {
            val childClone = child.deepClone()
            childClone.parent = nodeClone
            nodeClone.children!!.add(childClone)
        }
        return nodeClone
    }

    /**
     * Called internally by com.jme3.util.clone.Cloner.  Do not call directly.
     */
    override fun cloneFields(cloner: Cloner, original: Any) {
        super.cloneFields(cloner, original)

        this.children = cloner.clone<SafeArrayList<Spatial>>(children)

        // Only the outer cloning thing knows whether this should be nulled
        // or not... after all, we might be cloning a root node in which case
        // cloning this list is fine.
        this.updateList = cloner.clone<SafeArrayList<Spatial>>(updateList)
    }

    @Throws(IOException::class)
    override fun write(e: JmeExporter) {
        super.write(e)
        e.getCapsule(this).writeSavableArrayList(ArrayList(children!!), "children", null)
    }

    @Throws(IOException::class)
    override fun read(e: JmeImporter) {
        // XXX: Load children before loading itself!!
        // This prevents empty children list if controls query
        // it in Control.setSpatial().
        children = SafeArrayList(Spatial::class.java,
                e.getCapsule(this).readSavableArrayList("children", null))

        // go through children and set parent to this node
        if (children != null) {
            for (child in children!!.array) {
                child.parent = this
            }
        }
        super.read(e)
    }

    override fun setModelBound(modelBound: BoundingVolume?) {
        if (children != null) {
            for (child in children!!.array) {
                child.setModelBound(modelBound?.clone(null))
            }
        }
    }

    override fun updateModelBound() {
        if (children != null) {
            for (child in children!!.array) {
                child.updateModelBound()
            }
        }
    }

    override fun depthFirstTraversal(visitor: SceneGraphVisitor, mode: Spatial.DFSMode) {
        if (mode == Spatial.DFSMode.POST_ORDER) {
            for (child in children!!.array) {
                child.depthFirstTraversal(visitor)
            }
            visitor.visit(this)
        } else { //pre order
            visitor.visit(this)
            for (child in children!!.array) {
                child.depthFirstTraversal(visitor)
            }
        }
    }

    override fun breadthFirstTraversal(visitor: SceneGraphVisitor, queue: Queue<Spatial>) {
        queue.addAll(children!!)
    }

    companion object {

        private val logger = Logger.getLogger(Node::class.java.name)
    }
}
/**
 * Serialization only. Do not use.
 */
