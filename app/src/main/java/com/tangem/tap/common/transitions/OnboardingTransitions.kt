package com.tangem.tap.common.transitions

import androidx.transition.*

/**
[REDACTED_AUTHOR]
 */
class FrontCardEnterTransition : TransitionSet() {
    init {
        ordering = ORDERING_TOGETHER;
        addTransition(Fade())
        addTransition(ChangeImageTransform()) // изменения внутри ImageView
        addTransition(ChangeTransform()) // изменение размеров, углов накона
        addTransition(ChangeBounds()) // изменение положения
    }
}

class FrontCardExitTransition : TransitionSet() {
    init {
        ordering = ORDERING_TOGETHER;
        addTransition(Fade())
        addTransition(ChangeImageTransform())
        addTransition(ChangeTransform())
        addTransition(ChangeBounds())
    }
}

class HomeToOnboardingTransition : TransitionSet() {
    init {
        ordering = ORDERING_TOGETHER;
        addTransition(Fade())
        addTransition(ChangeTransform())
        addTransition(ChangeBounds())
    }
}

class InternalNoteLayoutTransition : TransitionSet() {
    init {
        ordering = ORDERING_TOGETHER;
        addTransition(ChangeTransform())
        addTransition(ChangeBounds())
        addTransition(Fade())
    }
}