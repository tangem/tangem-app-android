package com.tangem.tap.common.extensions

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewParent
import androidx.core.view.forEach
import androidx.transition.AutoTransition
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.tangem.tap.common.GlobalLayoutStateHandler
import timber.log.Timber

/**
[REDACTED_AUTHOR]
 */
fun ViewGroup.inflate(viewToInflate: Int, rootView: ViewGroup?, parent: ViewGroup) {
    if (rootView == null) {
        val inflatedView = LayoutInflater.from(context).inflate(viewToInflate, rootView)
        parent.addView(inflatedView)
    }
}

fun ViewParent?.beginDelayedTransition(transition: Transition = AutoTransition()) {
    if (this == null) Timber.e("Can't invoke beginDelayedTransition, because parent is NULL")
    (this as? ViewGroup)?.beginDelayedTransition(transition)
}

fun ViewGroup.beginDelayedTransition(transition: Transition = AutoTransition()) {
    TransitionManager.beginDelayedTransition(this, transition)
}

fun ChipGroup.fitChipsByGroupWidth() {
    val layoutStateHandler = GlobalLayoutStateHandler(this)
    layoutStateHandler.onStateChanged = stateHandler@{
        if (it.childCount < 2) {
            layoutStateHandler.detach()
            return@stateHandler
        }

        val spacingBetweenViews = it.chipSpacingHorizontal * (it.childCount - 1)
        val width = (it.width - spacingBetweenViews) / it.childCount
        it.forEach { chip -> (chip as? Chip)?.width = width }
        layoutStateHandler.detach()
    }
}