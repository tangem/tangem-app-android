package com.tangem.tangemtest.ucase.variants.scan.ui

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import com.tangem.tangemtest.R
import com.tangem.tangemtest.ucase.domain.paramsManager.ItemsManager
import com.tangem.tangemtest.ucase.domain.paramsManager.managers.ScanItemsManager
import com.tangem.tangemtest.ucase.ui.BaseCardActionFragment
import com.tangem.tangemtest.ucase.variants.responses.ui.ResponseFragment

/**
[REDACTED_AUTHOR]
 */
class ScanActionFragment : BaseCardActionFragment() {

    override val itemsManager: ItemsManager by lazy { ScanItemsManager() }

    override fun getLayoutId(): Int = R.layout.fg_action_card_scan

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        enableActionFab(true)
    }

    override fun listenReadResponse() {
        actionVM.seReadResponse.observe(viewLifecycleOwner, Observer {
            navigateTo(
                    R.id.action_nav_card_action_to_response_screen,
                    bundleOf(Pair(ResponseFragment.response, it))
            )
        })
    }
}