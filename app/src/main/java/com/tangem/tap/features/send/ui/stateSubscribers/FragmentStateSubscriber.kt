package com.tangem.tap.features.send.ui.stateSubscribers

import androidx.fragment.app.Fragment
import org.rekotlin.StateType
import org.rekotlin.StoreSubscriber
import java.lang.ref.WeakReference

/**
[REDACTED_AUTHOR]
 */
abstract class FragmentStateSubscriber<S : StateType>(fragment: Fragment) : StoreSubscriber<S> {
    private val weakFragment: WeakReference<Fragment> = WeakReference(fragment)

    abstract fun updateWithNewState(fg: Fragment, state: S)

    override fun newState(state: S) {
        val fg = weakFragment.get() ?: return

        updateWithNewState(fg, state)
    }
}