package com.tangem.tap.common.extensions

import androidx.transition.Transition

/**
 * Created by Anton Zhilenkov on 17/10/2021.
 */
inline fun Transition.addListener(
    crossinline onStart: (animator: Transition) -> Unit = {},
    crossinline onEnd: (animator: Transition) -> Unit = {},
    crossinline onCancel: (animator: Transition) -> Unit = {},
    crossinline onPause: (animator: Transition) -> Unit = {},
    crossinline onRepeat: (animator: Transition) -> Unit = {},
): Transition.TransitionListener {
    val listener = object : Transition.TransitionListener {
        override fun onTransitionStart(transition: Transition) = onStart(transition)
        override fun onTransitionEnd(transition: Transition) = onEnd(transition)
        override fun onTransitionCancel(transition: Transition) = onCancel(transition)
        override fun onTransitionPause(transition: Transition) = onPause(transition)
        override fun onTransitionResume(transition: Transition) = onRepeat(transition)
    }
    addListener(listener)
    return listener
}
