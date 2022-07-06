package com.tangem.tap.features.send.ui.stateSubscribers

import com.tangem.tap.features.BaseStoreFragment
import java.lang.ref.WeakReference
import org.rekotlin.StateType
import org.rekotlin.StoreSubscriber

/**
 * Created by Anton Zhilenkov on 31/08/2020.
 */
abstract class FragmentStateSubscriber<S : StateType>(fragment: BaseStoreFragment) : StoreSubscriber<S> {
    private val weakFragment: WeakReference<BaseStoreFragment> = WeakReference(fragment)

    abstract fun updateWithNewState(fg: BaseStoreFragment, state: S)

    override fun newState(state: S) {
        val fg = weakFragment.get() ?: return

        updateWithNewState(fg, state)
    }
}
