package com.tangem.tangemtest.card_use_cases.actions

import android.os.Bundle
import android.view.View
import com.tangem.commands.SignResponse
import com.tangem.tangemtest.R
import com.tangem.tangemtest.card_use_cases.base.BaseCardActionFragment
import com.tangem.tangemtest.commons.postUI
import com.tangem.tasks.TaskEvent
import kotlinx.android.synthetic.main.fg_card_sign.*

/**
[REDACTED_AUTHOR]
 */
class SignActionFragment : BaseCardActionFragment() {

    override fun getLayoutId(): Int = R.layout.fg_card_sign

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cardId = cardContext.requireCard().cardId
        cardContext.cardManager.sign(createSampleHashes(), cardId) { taskEvent ->
            when (taskEvent) {
                is TaskEvent.Event -> handleDataEvent(taskEvent.data)
                is TaskEvent.Completion -> handleCompletionEvent(taskEvent)
            }
        }
    }

    private fun handleDataEvent(event: SignResponse) {
        postUI { tv_sign_result.text = toJson(event) }
    }

    private fun createSampleHashes(): Array<ByteArray> {
        val hash1 = ByteArray(32) { 1 }
        val hash2 = ByteArray(32) { 2 }
        return arrayOf(hash1, hash2)
    }
}