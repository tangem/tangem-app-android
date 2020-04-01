package com.tangem.tangemtest.ucase.variants.scan.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.lifecycle.Observer
import com.tangem.tangemtest.R
import com.tangem.tangemtest.ucase.resources.ActionType
import com.tangem.tangemtest.ucase.ui.BaseCardActionFragment
import ru.dev.gbixahue.eu4d.lib.android._android.views.show
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log

/**
[REDACTED_AUTHOR]
 */
class ScanActionFragment : BaseCardActionFragment() {

    override fun getLayoutId(): Int = R.layout.fg_action_card_scan

    override fun getAction(): ActionType = ActionType.Scan

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        showActionFab(true)
    }

    override fun listenResponse() {
        val tvStub = mainView.findViewById<TextView>(R.id.tv_screen_stub)
        val tvResponse by lazy { mainView.findViewById<TextView>(R.id.tv_action_response_json) }
        paramsVM.ldReadResponse.observe(viewLifecycleOwner, Observer {
            Log.d(this, "action response: ${if (it.length > 50) it.substring(0..50) else it}")
            tvStub.show(false)
            tvResponse.text = it
        })
    }
}