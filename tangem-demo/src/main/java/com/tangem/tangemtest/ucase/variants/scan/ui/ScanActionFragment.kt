package com.tangem.tangemtest.ucase.variants.scan.ui

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import com.tangem.tangemtest.R
import com.tangem.tangemtest.ucase.resources.ActionType
import com.tangem.tangemtest.ucase.ui.BaseCardActionFragment
import com.tangem.tangemtest.ucase.ui.response.BaseCardResponseFragment

/**
[REDACTED_AUTHOR]
 */
class ScanActionFragment : BaseCardActionFragment() {

    override fun getLayoutId(): Int = R.layout.fg_action_card_scan

    override fun getAction(): ActionType = ActionType.Scan

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        enableActionFab(true)
    }

    override fun listenReadResponse() {
        paramsVM.seReadResponse.observe(viewLifecycleOwner, Observer {
            navigateTo(
                    R.id.action_nav_card_action_to_response_screen,
                    bundleOf(Pair(BaseCardResponseFragment.response, it))
            )
        })
    }
}