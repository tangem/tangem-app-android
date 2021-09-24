package com.tangem.tap.common.transitions

import androidx.transition.ChangeBounds
import androidx.transition.ChangeImageTransform
import androidx.transition.TransitionSet

/**
[REDACTED_AUTHOR]
 */
class FrontCardEnterTransition : TransitionSet() {
    init {
        ordering = ORDERING_TOGETHER;
        addTransition(ChangeImageTransform())
        addTransition(ChangeBounds())
//        addTransition(Fade())
//        addTransition(Fade(Visibility.MODE_IN))
//        addTransition(ChangeTransform())
    }
}

class FrontCardExitTransition : TransitionSet() {
    init {
        ordering = ORDERING_TOGETHER;
        addTransition(ChangeImageTransform())
        addTransition(ChangeBounds())
    }
}