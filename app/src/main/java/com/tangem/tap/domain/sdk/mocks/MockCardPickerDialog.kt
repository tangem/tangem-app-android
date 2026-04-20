package com.tangem.tap.domain.sdk.mocks

import androidx.appcompat.app.AlertDialog
import com.tangem.wallet.R
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

internal suspend fun showMockCardPicker(activity: AppCompatActivity): MockContent? = withContext(Dispatchers.Main) {
    suspendCancellableCoroutine { continuation ->
        val mocks = MockProvider.availableMocks
        val names = mocks.map { it.first }.toTypedArray()

        val dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.mock_card_picker_title)
            .setItems(names) { _, which ->
                if (continuation.isActive) {
                    continuation.resume(mocks[which].second)
                }
            }
            .setOnCancelListener {
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
            .create()

        continuation.invokeOnCancellation { activity.runOnUiThread { dialog.dismiss() } }
        dialog.show()
    }
}