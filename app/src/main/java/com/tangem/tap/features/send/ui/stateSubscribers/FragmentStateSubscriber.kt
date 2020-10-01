package com.tangem.tap.features.send.ui.stateSubscribers

import com.tangem.tap.features.send.BaseStoreFragment
import org.rekotlin.StateType
import org.rekotlin.StoreSubscriber
import java.lang.ref.WeakReference

/**
* [REDACTED_AUTHOR]
 */
abstract class FragmentStateSubscriber<S : StateType>(fragment: BaseStoreFragment) : StoreSubscriber<S> {
    private val weakFragment: WeakReference<BaseStoreFragment> = WeakReference(fragment)

    abstract fun updateWithNewState(fg: BaseStoreFragment, state: S)

    override fun newState(state: S) {
        val fg = weakFragment.get() ?: return

        updateWithNewState(fg, state)
    }
}