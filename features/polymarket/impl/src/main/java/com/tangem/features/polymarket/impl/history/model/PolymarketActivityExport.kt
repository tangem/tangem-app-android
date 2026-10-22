package com.tangem.features.polymarket.impl.history.model

import android.content.Context
import com.tangem.core.res.R
import com.tangem.core.res.getStringSafe
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketActivityFilter
import com.tangem.domain.polymarket.usecase.GetAllPolymarketActivityUseCase
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import javax.inject.Inject
import java.io.File
import java.util.UUID

internal class PolymarketActivityExport @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getAllActivity: GetAllPolymarketActivityUseCase,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, filter: PolymarketActivityFilter): File =
        withContext(dispatchers.io) {
            val records = getAllActivity(userWalletId, filter).getOrNull() ?: error("Activity export failed")
            val csv = PolymarketActivityCsv.make(records, headers())
            ensureActive()
            val directory = File(context.cacheDir, "prediction-exports/${UUID.randomUUID()}")
            check(directory.mkdirs())
            File(directory, "predictions-history.csv").apply { writeText(csv, Charsets.UTF_8) }
        }

    private fun headers(): List<String> = with(context.resources) {
        listOf(
            getStringSafe(R.string.prediction_history_csv_date),
            getStringSafe(R.string.common_type),
            getStringSafe(R.string.prediction_history_csv_result),
            getStringSafe(R.string.common_amount),
            getStringSafe(R.string.prediction_history_csv_market),
            getStringSafe(R.string.prediction_history_csv_event),
            getStringSafe(R.string.prediction_positions_outcome),
            getStringSafe(R.string.prediction_positions_quantity),
            getStringSafe(R.string.prediction_history_csv_average_price),
            getStringSafe(R.string.prediction_history_csv_polymarket_fee),
            getStringSafe(R.string.prediction_history_csv_tangem_fee),
            getStringSafe(R.string.prediction_history_csv_notional),
            getStringSafe(R.string.prediction_history_csv_transaction_hash),
        )
    }
}