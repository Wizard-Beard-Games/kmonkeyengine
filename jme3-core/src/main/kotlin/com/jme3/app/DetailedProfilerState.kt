package com.jme3.app

import com.jme3.app.state.AppStateManager
import com.jme3.app.state.BaseAppState
import com.jme3.font.BitmapFont
import com.jme3.font.BitmapText
import com.jme3.input.*
import com.jme3.input.controls.*
import com.jme3.material.Material
import com.jme3.material.RenderState
import com.jme3.math.*
import com.jme3.profile.AppStep
import com.jme3.scene.*
import com.jme3.scene.shape.Quad

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

/**
 * @author (kme) Ray Long
 * Created by (jme) Nehon on 25/01/2017.
 */
open class DetailedProfilerState : BaseAppState() {

    private val prof = DetailedProfiler()

    private var time = 0f
    private var font: BitmapFont? = null
    private var bigFont: BitmapFont? = null
    val uiNode = Node("Stats ui")
    private val lines = HashMap<String, StatLineView>()
    private var totalTimeCpu: Double = 0.toDouble()
    private var totalTimeGpu: Double = 0.toDouble()
    private var maxLevel = 0

    private var frameTimeValue: BitmapText? = null
    private var frameCpuTimeValue: BitmapText? = null
    private var frameGpuTimeValue: BitmapText? = null
    private var hideInsignificantField: BitmapText? = null

    private var selectedField: BitmapText? = null
    private var selectedValueCpu = 0.0
    private var selectedValueGpu = 0.0
    private var hideInsignificant = false

    private var rootLine: StatLineView? = null
    private var height = 0
    private val df = DecimalFormat("##0.00", DecimalFormatSymbols(Locale.US))

    private val dimmedWhite = ColorRGBA.White.mult(0.7f)
    private val dimmedGreen = ColorRGBA.Green.mult(0.7f)
    private val dimmedOrange = ColorRGBA.Orange.mult(0.7f)
    private val dimmedRed = ColorRGBA.Red.mult(0.7f)

    private val inputListener = ProfilerInputListener()

    override fun initialize(app: Application) {
        val mat = Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md")
        mat.setColor("Color", ColorRGBA(0f, 0f, 0f, 0.5f))
        mat.additionalRenderState.blendMode = RenderState.BlendMode.Alpha
        val darkenStats = Geometry("StatsDarken", Quad(PANEL_WIDTH.toFloat(), app.camera?.height!!.toFloat()))
        darkenStats.material = mat
        darkenStats.setLocalTranslation(0f, (-app.camera?.height!!).toFloat(), -1f)

        uiNode.attachChild(darkenStats)
        uiNode.setLocalTranslation((app.camera?.width!! - PANEL_WIDTH).toFloat(), app.camera?.height!!.toFloat(), 0f)
        font = app.getAssetManager()?.loadFont("Interface/Fonts/Console.fnt")
        bigFont = app.getAssetManager()?.loadFont("Interface/Fonts/Default.fnt")
        prof.setRenderer(app.renderer!!)
        rootLine = StatLineView("Frame")
        rootLine!!.attachTo(uiNode)

        val frameLabel = BitmapText(bigFont)
        frameLabel.text = "Total Frame Time: "
        uiNode.attachChild(frameLabel)
        frameLabel.localTranslation = Vector3f(PANEL_WIDTH / 2 - bigFont!!.getLineWidth(frameLabel.text), (-PADDING).toFloat(), 0f)

        val cpuLabel = BitmapText(bigFont)
        cpuLabel.text = "CPU"
        uiNode.attachChild(cpuLabel)
        cpuLabel.setLocalTranslation(PANEL_WIDTH / 4 - bigFont!!.getLineWidth(cpuLabel.text) / 2, (-PADDING - 30).toFloat(), 0f)

        val gpuLabel = BitmapText(bigFont)
        gpuLabel.text = "GPU"
        uiNode.attachChild(gpuLabel)
        gpuLabel.setLocalTranslation(3 * PANEL_WIDTH / 4 - bigFont!!.getLineWidth(gpuLabel.text) / 2, (-PADDING - 30).toFloat(), 0f)

        frameTimeValue = BitmapText(bigFont)
        frameCpuTimeValue = BitmapText(bigFont)
        frameGpuTimeValue = BitmapText(bigFont)

        selectedField = BitmapText(font)
        selectedField!!.text = "Selected: "
        selectedField!!.setLocalTranslation((PANEL_WIDTH / 2).toFloat(), (-PADDING - 75).toFloat(), 0f)
        selectedField!!.color = ColorRGBA.Yellow


        uiNode.attachChild(frameTimeValue!!)
        uiNode.attachChild(frameCpuTimeValue!!)
        uiNode.attachChild(frameGpuTimeValue!!)
        uiNode.attachChild(selectedField!!)

        hideInsignificantField = BitmapText(font)
        hideInsignificantField!!.text = "O " + INSIGNIFICANT
        hideInsignificantField!!.setLocalTranslation(PADDING.toFloat(), (-PADDING - 75).toFloat(), 0f)
        uiNode.attachChild(hideInsignificantField!!)

        val inputManager = app.inputManager
        inputManager?.addMapping(TOGGLE_KEY, KeyTrigger(KeyInput.KEY_F6))
        inputManager?.addMapping(CLICK_KEY, MouseButtonTrigger(MouseInput.BUTTON_LEFT))
        inputManager?.addListener(inputListener, TOGGLE_KEY, CLICK_KEY)
    }

    override fun cleanup(app: Application?) {
        uiNode.detachAllChildren()
        val manager = application!!.inputManager
        manager?.deleteMapping(TOGGLE_KEY)
        manager?.deleteMapping(CLICK_KEY)
        manager?.removeListener(inputListener)
    }

    override fun update(tpf: Float) {
        time += tpf
    }

    private fun displayData(data: Map<String, DetailedProfiler.StatLine>?) {
        when {
            data == null || data.isEmpty() -> return
            else -> {
                lines.values.forEach { statLine ->
                    statLine.reset()
                    statLine.removeFromParent()
                }
                rootLine!!.reset()
                maxLevel = 0
                data.keys.forEach { path ->
                    when (path) {
                        "EndFrame" -> return@forEach
                    }
                    maxLevel = Math.max(maxLevel, path.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size)
                    var line = getStatLineView(path)
                    val statLine = data[path]
                    line.updateValues(statLine!!.averageCpu, statLine.averageGpu)
                    var parent = getParent(path)
                    while (parent != null) {
                        val parentView = getStatLineView(parent)
                        parentView.updateValues(statLine.averageCpu, statLine.averageGpu)
                        parentView.children.add(line)
                        line.attachTo(uiNode)
                        line = parentView
                        parent = getParent(parent)
                    }
                    rootLine!!.children.add(line)
                    line.attachTo(uiNode)
                    rootLine!!.updateValues(statLine.averageCpu, statLine.averageGpu)
                }

                totalTimeCpu = rootLine!!.cpuValue
                totalTimeGpu = rootLine!!.gpuValue + data["EndFrame"]!!.averageGpu

                layout()

            }
        }

    }

    private fun layout() {
        height = 0
        selectedValueCpu = 0.0
        selectedValueGpu = 0.0
        rootLine!!.layout(0)

        frameTimeValue!!.text = df.format(getMsFromNs(prof.averageFrameTime)) + "ms"
        frameTimeValue!!.setLocalTranslation((PANEL_WIDTH / 2).toFloat(), (-PADDING).toFloat(), 0f)
        setColor(frameTimeValue!!, prof.averageFrameTime, totalTimeCpu, false, false)

        frameCpuTimeValue!!.text = df.format(getMsFromNs(totalTimeCpu)) + "ms"
        frameCpuTimeValue!!.localTranslation = Vector3f(PANEL_WIDTH / 4 - bigFont!!.getLineWidth(frameCpuTimeValue!!.text) / 2, (-PADDING - 50).toFloat(), 0f)
        setColor(frameCpuTimeValue!!, totalTimeCpu, totalTimeCpu, false, false)

        frameGpuTimeValue!!.text = df.format(getMsFromNs(totalTimeGpu)) + "ms"
        frameGpuTimeValue!!.localTranslation = Vector3f(3 * PANEL_WIDTH / 4 - bigFont!!.getLineWidth(frameGpuTimeValue!!.text) / 2, (-PADDING - 50).toFloat(), 0f)
        setColor(frameGpuTimeValue!!, totalTimeGpu, totalTimeGpu, false, false)

        selectedField!!.text = "Selected: " + df.format(getMsFromNs(selectedValueCpu)) + "ms / " + df.format(getMsFromNs(selectedValueGpu)) + "ms"

        selectedField!!.setLocalTranslation(3 * PANEL_WIDTH / 4 - font!!.getLineWidth(selectedField!!.text) / 2, (-PADDING - 75).toFloat(), 0f)
    }

    private fun getStatLineView(path: String): StatLineView {
        var line: StatLineView? = lines[path]

        when (line) {
            null -> {
                line = StatLineView(getLeaf(path))
                lines[path] = line
                line.attachTo(uiNode)
            }
        }
        return line!!
    }

    private fun getLeaf(path: String): String {
        val idx = path.lastIndexOf("/")
        return when {
            idx >= 0 -> path.substring(idx + 1)
            else -> path
        }
    }

    private fun getParent(path: String): String? {
        val idx = path.lastIndexOf("/")
        return when {
            idx >= 0 -> path.substring(0, idx)
            else -> null
        }
    }


    override fun postRender() {
        when {
            time > REFRESH_TIME -> {
                prof.appStep(AppStep.EndFrame)
                val data = prof.stats
                displayData(data)
                time = 0f
            }
        }
    }

    private fun getMsFromNs(time: Double): Double {
        return time / 1000000.0
    }

    override fun onEnable() {
        application!!.appProfiler = prof
        (application as SimpleApplication).guiNode.attachChild(uiNode)
    }

    override fun onDisable() {
        application!!.appProfiler = null
        uiNode.removeFromParent()
    }

    fun setColor(t: BitmapText, value: Double, totalTime: Double, isParent: Boolean, expended: Boolean): Boolean {

        val dimmed = isParent && expended
        var insignificant = false

        when {
            value > 1000000000.0 / 30.0 -> t.color = if (dimmed) dimmedRed else ColorRGBA.Red
            value > 1000000000.0 / 60.0 -> t.color = if (dimmed) dimmedOrange else ColorRGBA.Orange
            value > totalTime / 3 -> t.color = if (dimmed) dimmedGreen else ColorRGBA.Green
            value < 30000 -> {
                t.color = ColorRGBA.DarkGray
                insignificant = true
            }
            else -> t.color = if (dimmed) dimmedWhite else ColorRGBA.White
        }
        return insignificant
    }

    private fun handleClick(pos: Vector2f) {

        val lp = hideInsignificantField!!.worldTranslation
        val width = font!!.getLineWidth(hideInsignificantField!!.text)
        when {
            pos.x > lp.x && pos.x < lp.x + width
                    && pos.y < lp.y && pos.y > lp.y - LINE_HEIGHT -> {
                hideInsignificant = !hideInsignificant
                hideInsignificantField!!.text = (when {
                    hideInsignificant -> "X "
                    else -> "O "
                }) + INSIGNIFICANT
                when {
                    !hideInsignificant -> rootLine!!.setExpended(true)
                }
            }
        }

        rootLine!!.onClick(pos)
        lines.values.forEach { statLineView -> statLineView.onClick(pos) }
        layout()
    }

    private inner class StatLineView(internal var text: String) {
        internal var label: BitmapText = BitmapText(font)
        internal var cpuText: BitmapText
        internal var gpuText: BitmapText
        internal var checkBox: BitmapText = BitmapText(font)
        internal var cpuValue: Double = 0.toDouble()
        internal var gpuValue: Double = 0.toDouble()
        private var expended = true
        private var visible = true
        private var selected = false

        internal var children: MutableSet<StatLineView> = LinkedHashSet()

        init {
            this.checkBox.text = "O"
            this.label.text = "- " + text
            this.cpuText = BitmapText(font)
            this.gpuText = BitmapText(font)
        }

        fun onClick(pos: Vector2f) {

            when {
                !visible -> return
                else -> {
                    val lp = label.worldTranslation
                    val cp = checkBox.worldTranslation
                    when {
                        pos.x > cp.x
                                && pos.y < lp.y && pos.y > lp.y - LINE_HEIGHT -> {

                            val width = font!!.getLineWidth(checkBox.text)
                            when {
                                pos.x >= cp.x && pos.x <= cp.x + width -> {
                                    selected = !selected
                                    when {
                                        selected -> checkBox.text = "X"
                                        else -> checkBox.text = "O"
                                    }
                                }
                                else -> setExpended(!expended)
                            }
                        }
                    }
                }
            }

        }

        fun setExpended(expended: Boolean) {
            this.expended = expended
            when {
                expended -> label.text = "- " + text
                else -> label.text = "+ " + text
            }
            children.forEach { child -> child.setVisible(expended) }
        }

        fun layout(indent: Int) {

            var insignificant: Boolean = setColor(cpuText, cpuValue, totalTimeCpu, !children.isEmpty(), expended)
            cpuText.text = df.format(getMsFromNs(cpuValue)) + "ms /"
            gpuText.text = " " + df.format(getMsFromNs(gpuValue)) + "ms"
            insignificant = insignificant and setColor(gpuText, gpuValue, totalTimeGpu, !children.isEmpty(), expended)

            when {
                insignificant && hideInsignificant -> setVisible(false)
            }

            when {
                !visible -> return
                else -> {
                    when {
                        selected -> {
                            label.color = ColorRGBA.Yellow
                            selectedValueCpu += cpuValue
                            selectedValueGpu += gpuValue
                        }
                        else -> label.color = ColorRGBA.White
                    }

                    val y = -(height * LINE_HEIGHT + HEADER_HEIGHT)

                    label.setLocalTranslation((PADDING + indent * PADDING).toFloat(), y.toFloat(), 0f)
                    val gpuPos = PANEL_WIDTH.toFloat() - font!!.getLineWidth(gpuText.text) - (PADDING * (maxLevel - indent + 1)).toFloat()
                    cpuText.setLocalTranslation(gpuPos - font!!.getLineWidth(cpuText.text), y.toFloat(), 0f)
                    gpuText.setLocalTranslation(gpuPos, y.toFloat(), 0f)

                    checkBox.setLocalTranslation(3f, y.toFloat(), 0f)
                    height++
                    children.forEach { child -> child.layout(indent + 1) }
                }
            }

        }

        fun updateValues(cpu: Double, gpu: Double) {
            cpuValue += cpu
            gpuValue += gpu
        }

        fun attachTo(node: Node) {
            node.attachChild(label)
            node.attachChild(cpuText)
            node.attachChild(gpuText)
            node.attachChild(checkBox)
        }

        fun removeFromParent() {
            label.removeFromParent()
            cpuText.removeFromParent()
            gpuText.removeFromParent()
            checkBox.removeFromParent()
        }

        fun reset() {
            children.clear()
            cpuValue = 0.0
            gpuValue = 0.0
        }

        fun setVisible(visible: Boolean) {
            this.visible = visible
            label.cullHint = if (visible) Spatial.CullHint.Dynamic else Spatial.CullHint.Always
            cpuText.cullHint = if (visible) Spatial.CullHint.Dynamic else Spatial.CullHint.Always
            gpuText.cullHint = if (visible) Spatial.CullHint.Dynamic else Spatial.CullHint.Always
            checkBox.cullHint = if (visible) Spatial.CullHint.Dynamic else Spatial.CullHint.Always


            children.forEach { child -> child.setVisible(visible && expended) }

        }


        override fun toString(): String {
            return label.text + " - " + df.format(getMsFromNs(cpuValue)) + "ms / " + df.format(getMsFromNs(gpuValue)) + "ms"
        }
    }

    private inner class ProfilerInputListener : ActionListener {
        override fun onAction(name: String, isPressed: Boolean, tpf: Float) {
            when {
                name == TOGGLE_KEY && isPressed -> setEnabled(!isEnabled())
            }
            when {
                isEnabled() && name == CLICK_KEY && isPressed -> handleClick(application!!.inputManager!!.cursorPosition)
            }
        }
    }

    companion object {

        private val PANEL_WIDTH = 400
        private val PADDING = 10
        private val LINE_HEIGHT = 12
        private val HEADER_HEIGHT = 100
        private val REFRESH_TIME = 1.0f
        private val TOGGLE_KEY = "Toggle_Detailed_Profiler"
        private val CLICK_KEY = "Click_Detailed_Profiler"
        private val INSIGNIFICANT = "Hide insignificant stat"
    }
}

