package com.sychev.assistantapp.presentation.utils

import androidx.constraintlayout.motion.widget.MotionLayout
import kotlinx.coroutines.*

/**
 * Wait for the transition to complete so that the given [transitionId] is fully displayed.
 *
 * @param transitionId The transition set to await the completion of
 * @param timeout Timeout for the transition to take place. Defaults to 5 seconds.
 */

suspend fun MotionLayout.awaitTransitionComplete(transitionId: Int, timeout: Long = 5000L) {
    var listener: MotionLayout.TransitionListener? = null
    try {
        withTimeout(timeout) {
            suspendCancellableCoroutine<Unit> { continuation ->
                listener = object : SimpleTransitionListener() {
                    override fun onTransitionCompleted(
                        motionLayout: MotionLayout,
                        currentId: Int
                    ) {
                        apply(::removeTransitionListener)
                        continuation.resume(Unit, Throwable::printStackTrace)
                    }
                }
                continuation.invokeOnCancellation {
                    listener?.apply(this@awaitTransitionComplete::removeTransitionListener)
                }
                addTransitionListener(listener)
            }
        }
    } catch (ex: TimeoutCancellationException) {
        listener?.apply(this::removeTransitionListener)
    }
}

open class SimpleTransitionListener : MotionLayout.TransitionListener {
    override fun onTransitionStarted(motionLayout: MotionLayout, startId: Int, endId: Int) {}

    override fun onTransitionChange(
        motionLayout: MotionLayout,
        startId: Int,
        endId: Int,
        progress: Float
    ) {
        // No-op
    }

    override fun onTransitionCompleted(motionLayout: MotionLayout, currentId: Int) {
        // No-op
    }

    override fun onTransitionTrigger(
        motionLayout: MotionLayout,
        triggerId: Int,
        endId: Boolean,
        progress: Float
    ) {
        // No-op
    }
}