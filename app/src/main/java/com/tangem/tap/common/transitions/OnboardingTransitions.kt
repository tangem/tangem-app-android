package com.tangem.tap.common.transitions

import androidx.transition.ChangeBounds
import androidx.transition.ChangeTransform
import androidx.transition.Fade
import androidx.transition.TransitionSet

class HomeToOnboardingTransition : TransitionSet() {
    init {
        ordering = ORDERING_TOGETHER
        addTransition(Fade())
        addTransition(ChangeTransform())
        addTransition(ChangeBounds())
    }
}

class InternalNoteLayoutTransition : TransitionSet() {
    init {
        ordering = ORDERING_TOGETHER
        addTransition(ChangeTransform())
        addTransition(ChangeBounds())
        addTransition(Fade())
    }
}
