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

import java.util.concurrent.*
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Level
import java.util.logging.Logger

/**
 * `AppTask` is used in `AppTaskQueue` to manage tasks that have
 * yet to be accomplished. The AppTask system is used to execute tasks either
 * in the OpenGL/Render thread, or outside of it.
 *
 * @author (kme) Ray Long
 * @author (jme) Matthew D. Hicks, lazloh
 */
class AppTask<V>
/**
 * Create an `AppTask` that will execute the given
 * [Callable].
 *
 * @param callable The callable to be executed
 */
(val callable: Callable<V>) : Future<V> {

    private var result: V? = null
    private var exception: ExecutionException? = null
    private var cancelled: Boolean = false
    private var finished: Boolean = false
    private val stateLock = ReentrantLock()
    private val finishedCondition = stateLock.newCondition()

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        stateLock.lock()
        try {
            if (result != null) {
                return false
            }
            cancelled = true

            finishedCondition.signalAll()

            return true
        } finally {
            stateLock.unlock()
        }
    }

    @Throws(InterruptedException::class, ExecutionException::class)
    override fun get(): V? {
        stateLock.lock()
        try {
            while (!isDone) {
                finishedCondition.await()
            }
            if (exception != null) {
                throw exception!!
            }
            return result
        } finally {
            stateLock.unlock()
        }
    }

    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    override fun get(timeout: Long, unit: TimeUnit): V {
        stateLock.lock()
        try {
            if (!isDone) {
                finishedCondition.await(timeout, unit)
            }
            if (exception != null) {
                throw exception!!
            }
            if (result == null) {
                throw TimeoutException("Object not returned in time allocated.")
            }
            return result!!
        } finally {
            stateLock.unlock()
        }
    }

    override fun isCancelled(): Boolean {
        stateLock.lock()
        try {
            return cancelled
        } finally {
            stateLock.unlock()
        }
    }

    override fun isDone(): Boolean {
        stateLock.lock()
        try {
            return finished || cancelled || exception != null
        } finally {
            stateLock.unlock()
        }
    }

    operator fun invoke() {
        try {
            val tmpResult = callable.call()

            stateLock.lock()
            try {
                result = tmpResult
                finished = true

                finishedCondition.signalAll()
            } finally {
                stateLock.unlock()
            }
        } catch (e: Exception) {
            logger.logp(Level.SEVERE, this.javaClass.toString(), "invoke()", "Exception", e)

            stateLock.lock()
            try {
                exception = ExecutionException(e)

                finishedCondition.signalAll()
            } finally {
                stateLock.unlock()
            }
        }

    }

    companion object {
        private val logger = Logger.getLogger(AppTask<*>::javaClass.name)
    }

}