package com.tangem.tap.common.extensions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.transition.AutoTransition
import androidx.transition.Transition
import androidx.transition.TransitionManager

/**
* [REDACTED_AUTHOR]
 */
fun ViewGroup.inflate(viewToInflate: Int, rootView: ViewGroup?, parent: ViewGroup) {
    if (rootView == null) {
        val inflatedView = LayoutInflater.from(context).inflate(viewToInflate, rootView)
        parent.addView(inflatedView)
    }
}

fun ViewGroup.beginDelayedTransition(transition: Transition = AutoTransition()) {
    TransitionManager.beginDelayedTransition(this, transition)
}