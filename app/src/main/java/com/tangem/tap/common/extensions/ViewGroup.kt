package com.tangem.tap.common.extensions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.transition.AutoTransition
import androidx.transition.Transition
import androidx.transition.TransitionManager

/**
[REDACTED_AUTHOR]
 */
fun ViewGroup.inflate(viewToInflate: Int, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(context).inflate(viewToInflate, this, attachToRoot)
}

fun ViewGroup.beginDelayedTransition(transition: Transition = AutoTransition()) {
    TransitionManager.beginDelayedTransition(this, transition)
}