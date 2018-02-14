package com.jme3.app

import com.jme3.profile.*
import com.jme3.renderer.Renderer
import com.jme3.renderer.ViewPort
import com.jme3.renderer.queue.RenderQueue

import java.util.*

/**
 * @author (kme) Ray Long
 * Created by Nehon on 25/01/2017.
 */
class DetailedProfiler : AppProfiler {
    private var data: MutableMap<String, StatLine>? = null
    private var pool: MutableMap<String, StatLine>? = null
    private val startFrame: Long = 0
    private var prevPath: String? = null
    private var frameEnded = false
    private var renderer: Renderer? = null
    private var ongoingGpuProfiling = false


    private var curAppPath: String? = null
    private var curVpPath: String? = null
    private var curSpPath: String? = null
    private var lastVpStep: VpStep? = null

    private val path = StringBuilder(256)
    private val vpPath = StringBuilder(256)

    private val idsPool = ArrayDeque<Int>(100)

    internal lateinit var frameTime: StatLine

    //new LinkedHashMap<>(data);
    val stats: Map<String, StatLine>?
        get() = when {
            data != null -> data
            else -> null
        }

    val averageFrameTime: Double
        get() = frameTime.averageCpu

    private val unusedTaskId: Int
        get() {
            when {
                idsPool.isEmpty() -> poolTaskIds(renderer)
            }

            return idsPool.pop()
        }


    override fun appStep(step: AppStep) {
        curAppPath = step.name

        when (step) {
            AppStep.BeginFrame -> {
                when (data) {
                    null -> {
                        data = LinkedHashMap()
                        pool = HashMap()
                        frameTime = StatLine(currentFrame)
                    }
                }
                when {
                    frameTime.isActive -> {
                        frameTime.valueCpu = System.nanoTime() - frameTime.valueCpu
                        frameTime.closeFrame()

                    }
                }
                frameTime.setNewFrameValueCpu(System.nanoTime())

                frameEnded = false
                data!!.values.forEach { statLine ->
                    val i = statLine.taskIds.iterator()
                    while (i.hasNext()) {
                        val id = i.next()
                        when {
                            renderer!!.isTaskResultAvailable(id) -> {
                                val `val` = renderer!!.getProfilingTime(id)
                                statLine.setValueGpu(`val`)
                                i.remove()
                                idsPool.push(id)
                            }
                        }
                    }
                }
                data!!.clear()
            }
        }

        when {
            data != null -> {
                val path = getPath(step.name)
                when (step) {
                    AppStep.EndFrame -> when {
                        frameEnded -> return
                        else -> {
                            addStep(path, System.nanoTime())
                            val end = data!![path]
                            end?.valueCpu = System.nanoTime() - startFrame
                            frameEnded = true
                        }
                    }
                    else -> addStep(path, System.nanoTime())
                }
            }
        }
        when (step) {
            AppStep.EndFrame -> closeFrame()
        }
    }

    private fun closeFrame() {
        //close frame
        when {
            data != null -> {

                prevPath = null

                data!!.values.forEach { statLine -> statLine.closeFrame() }
                currentFrame++
            }
        }
    }

    override fun vpStep(step: VpStep, vp: ViewPort, bucket: RenderQueue.Bucket?) {

        when {
            data != null -> {
                vpPath.setLength(0)
                vpPath.append(vp.name).append("/").append(if (bucket == null) step.name else bucket!!.name + " Bucket")
                path.setLength(0)
                when {
                    (lastVpStep == VpStep.PostQueue || lastVpStep == VpStep.PostFrame) && bucket != null -> {
                        path.append(curAppPath).append("/").append(curVpPath).append(curSpPath).append("/").append(vpPath)
                        curVpPath = vpPath.toString()
                    }
                    else -> when {
                        bucket != null -> path.append(curAppPath).append("/").append(curVpPath).append("/").append(bucket.name + " Bucket")
                        else -> {
                            path.append(curAppPath).append("/").append(vpPath)
                            curVpPath = vpPath.toString()
                        }
                    }
                }
                lastVpStep = step

                addStep(path.toString(), System.nanoTime())
            }
        }
    }

    override fun spStep(step: SpStep, vararg additionalInfo: String) {

        when {
            data != null -> {
                curSpPath = getPath("", *additionalInfo)
                path.setLength(0)
                path.append(curAppPath).append("/").append(curVpPath).append(curSpPath)
                addStep(path.toString(), System.nanoTime())
            }
        }

    }


    private fun addStep(path: String, value: Long) {
        when {
            ongoingGpuProfiling && renderer != null -> {
                renderer!!.stopProfiling()
                ongoingGpuProfiling = false
            }
        }

        when {
            prevPath != null -> {
                val prevLine = data!![prevPath!!]
                if (prevLine != null) {
                    prevLine.valueCpu = value - prevLine.valueCpu
                }
            }
        }

        var line: StatLine? = pool!![path]
        when (line) {
            null -> {
                line = StatLine(currentFrame)
                pool!![path] = line
            }
        }
        data!![path] = line!!
        line!!.setNewFrameValueCpu(value)
        when {
            renderer != null -> {
                val id = unusedTaskId
                line.taskIds.add(id)
                renderer!!.startProfiling(id)
            }
        }
        ongoingGpuProfiling = true
        prevPath = path

    }

    private fun getPath(step: String, vararg subPath: String): String {
        val path = StringBuilder(step)
        for (s in subPath) {
            path.append("/").append(s)
        }
        return path.toString()
    }

    fun setRenderer(renderer: Renderer) {
        this.renderer = renderer
        poolTaskIds(renderer)
    }

    private fun poolTaskIds(renderer: Renderer?) {
        val ids = renderer!!.generateProfilingTasks(100)
        ids.forEach { id -> idsPool.push(id) }
    }

    class StatLine(currentFrame: Int) {
        private val cpuTimes = LongArray(MAX_FRAMES)
        private val gpuTimes = LongArray(MAX_FRAMES)
        private var startCursor = 0
        private var cpuCursor = 0
        private var gpuCursor = 0
        private var cpuSum: Long = 0
        private var gpuSum: Long = 0
        internal var valueCpu: Long = 0
        private var nbFramesCpu: Int = 0
        private var nbFramesGpu: Int = 0
        internal var taskIds: MutableList<Int> = ArrayList()

        val isActive: Boolean
            get() = cpuCursor >= currentFrame % MAX_FRAMES - 1

        val averageCpu: Double
            get() {
                return when (nbFramesCpu) {
                    0 -> 0.0
                    else -> cpuSum.toDouble() / Math.min(nbFramesCpu, MAX_FRAMES).toDouble()
                }
            }

        val averageGpu: Double
            get() {
                return when (nbFramesGpu) {
                    0 -> 0.0
                    else -> gpuSum.toDouble() / Math.min(nbFramesGpu, MAX_FRAMES).toDouble()
                }

            }


        init {
            startCursor = currentFrame % MAX_FRAMES
            cpuCursor = startCursor
            gpuCursor = startCursor
        }

        internal fun setNewFrameValueCpu(value: Long) {
            val newCursor = currentFrame % MAX_FRAMES
            when (nbFramesCpu) {
                0 -> startCursor = newCursor
            }
            cpuCursor = newCursor
            valueCpu = value
        }

        internal fun closeFrame() {
            when {
                isActive -> {
                    cpuSum -= cpuTimes[cpuCursor]
                    cpuTimes[cpuCursor] = valueCpu
                    cpuSum += valueCpu
                    nbFramesCpu++
                }
                else -> nbFramesCpu = 0
            }
        }

        fun setValueGpu(value: Long) {
            gpuSum -= gpuTimes[gpuCursor]
            gpuTimes[gpuCursor] = value
            gpuSum += value
            nbFramesGpu++
            gpuCursor = (gpuCursor + 1) % MAX_FRAMES
        }
    }

    companion object {

        private val MAX_FRAMES = 100
        private var currentFrame = 0
    }

}
