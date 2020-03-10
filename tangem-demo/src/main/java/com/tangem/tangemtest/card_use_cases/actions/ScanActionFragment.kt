package com.tangem.tangemtest.card_use_cases.actions

import android.os.Bundle
import android.view.View
import com.google.gson.Gson
import com.tangem.tangemtest.R
import com.tangem.tangemtest.card_use_cases.base.BaseCardActionFragment
import com.tangem.tangemtest.commons.postUI
import com.tangem.tasks.ScanEvent
import com.tangem.tasks.TaskEvent
import kotlinx.android.synthetic.main.fg_card_scan.*

/**
[REDACTED_AUTHOR]
 */
class ScanActionFragment : BaseCardActionFragment() {

    override fun getLayoutId(): Int = R.layout.fg_card_scan

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cardContext.reset()
        cardContext.cardManager.scanCard { taskEvent ->
            when (taskEvent) {
                is TaskEvent.Event -> handleDataEvent(taskEvent.data)
                is TaskEvent.Completion -> handleCompletionEvent(taskEvent)
            }
        }
    }

    private fun handleDataEvent(event: ScanEvent) {
        when (event) {
            is ScanEvent.OnReadEvent -> {
                cardContext.card = event.card
                postUI { tv_card_cid.text = Gson().toJson(cardContext.card) }
            }
            is ScanEvent.OnVerifyEvent -> cardContext.isVerified = true
        }
    }
}