package com.tangem.tap.domain.sdk.mocks

import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.tangem.tap.domain.sdk.mocks.content.CobrandMockContent
import com.tangem.wallet.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

private const val BATCH_ID_MAX_LENGTH = 8
private const val MIN_CARD_COUNT = 2
private const val MAX_CARD_COUNT = 3
private const val FIELD_PADDING_DP = 16
private val BATCH_ID_REGEX = Regex("[0-9A-F]{4}|[0-9A-F]{8}")

internal suspend fun showCobrandConfigDialog(activity: AppCompatActivity): CobrandMockContent? =
    withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val density = activity.resources.displayMetrics.density
            val paddingPx = (FIELD_PADDING_DP * density).toInt()

            val batchInput = EditText(activity).apply {
                hint = activity.getString(R.string.mock_cobrand_batch_hint)
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                filters = arrayOf(InputFilter.LengthFilter(BATCH_ID_MAX_LENGTH), InputFilter.AllCaps())
            }
            val countInput = EditText(activity).apply {
                hint = activity.getString(R.string.mock_cobrand_card_count_hint)
                inputType = InputType.TYPE_CLASS_NUMBER
                filters = arrayOf(InputFilter.LengthFilter(1))
            }
            val container = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(paddingPx, paddingPx, paddingPx, 0)
                val lp = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                addView(batchInput, lp)
                addView(countInput, lp)
            }

            val dialog = AlertDialog.Builder(activity)
                .setTitle(R.string.mock_cobrand_dialog_title)
                .setView(container)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    if (continuation.isActive) continuation.resume(null)
                }
                .setOnCancelListener {
                    if (continuation.isActive) continuation.resume(null)
                }
                .create()

            dialog.setOnShowListener {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val batch = batchInput.text.toString().trim()
                    val count = countInput.text.toString().toIntOrNull()
                    batchInput.error = null
                    countInput.error = null
                    when {
                        !batch.matches(BATCH_ID_REGEX) -> {
                            batchInput.error = activity.getString(R.string.mock_cobrand_batch_error)
                        }
                        count == null || count !in MIN_CARD_COUNT..MAX_CARD_COUNT -> {
                            countInput.error = activity.getString(R.string.mock_cobrand_card_count_error)
                        }
                        else -> {
                            dialog.dismiss()
                            if (continuation.isActive) {
                                continuation.resume(CobrandMockContent(batch, count))
                            }
                        }
                    }
                }
            }

            continuation.invokeOnCancellation { activity.runOnUiThread { dialog.dismiss() } }
            dialog.show()
        }
    }