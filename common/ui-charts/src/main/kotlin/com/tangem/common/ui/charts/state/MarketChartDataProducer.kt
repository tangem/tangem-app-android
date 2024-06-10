package com.tangem.common.ui.charts.state

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.math.BigDecimal

/**
 * This class represents a transaction for updating the state and look of a Market Chart.
 *
 * @property look The updated look of the Market Chart.
 * @property state The updated state of the Market Chart.
 */
open class Transaction(
    private val currentState: MarketChartData,
    private val currentLook: MarketChartLook,
) {
    var look: MarketChartLook? = null
    var state: MarketChartData.NoData? = null

    fun updateLook(block: (prev: MarketChartLook) -> MarketChartLook) {
        look = block(currentLook)
    }

    fun updateState(block: (prev: MarketChartData) -> MarketChartData.NoData) {
        state = block(currentState)
    }
}

/**
 * This class represents a transaction for updating the state and look of a Market Chart.
 * It extends the Transaction class and allows update state by data.
 *
 * @property state The updated state of the Market Chart.
 * @property look The updated look of the Market Chart.
 */
class TransactionSuspend(
    private val currentState: MarketChartData,
    private val currentLook: MarketChartLook,
) {
    internal var nonSuspendTransaction: Transaction? = null
    var state: MarketChartData? = null
    var look: MarketChartLook?
        get() = nonSuspendTransaction?.look
        set(value) {
            if (nonSuspendTransaction == null) {
                nonSuspendTransaction = Transaction(currentState, currentLook)
            }
            nonSuspendTransaction?.look = value
        }

    fun updateLook(block: (prev: MarketChartLook) -> MarketChartLook) {
        look = block(currentLook)
    }

    internal fun updateState(block: (prev: MarketChartData) -> MarketChartData) {
        state = block(currentState)
    }

    internal fun updateData(block: (prev: MarketChartData.Data) -> MarketChartData.Data) {
        state = when (val currentState = currentState) {
            is MarketChartData.Data -> block(currentState)
            else -> currentState
        }
    }
}

@Stable
class MarketChartDataProducer private constructor(
    initialData: MarketChartData,
    initialLook: MarketChartLook,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    internal val startDrawingAnimation = MutableSharedFlow<Unit>()
    internal val dataState = MutableStateFlow(initialData)
    internal val lookState = MutableStateFlow(initialLook)
    internal val entries = MutableStateFlow<List<LineCartesianLayerModel.Entry>>(emptyList())

    internal var dsd: Pair<List<Float>, List<Float>>? by mutableStateOf(null) // TODO: remove

    internal val modelProducer = CartesianChartModelProducer.build(dispatcher = dispatcher)

    /**
     * This function runs a suspending transaction block to update the state and look of the Market Chart.
     */
    suspend fun runTransactionSuspend(block: TransactionSuspend.() -> Unit) =
        handleTransactionSuspend(transaction = TransactionSuspend(dataState.value, lookState.value).apply(block))

    /**
     * This function runs a non-suspending transaction block to update the state and look of the Market Chart.
     */
    fun runTransaction(block: Transaction.() -> Unit) =
        handleTransaction(transaction = Transaction(dataState.value, lookState.value).apply(block))

    private suspend fun handleTransactionSuspend(transaction: TransactionSuspend) {
        val nonSuspendTransaction = transaction.nonSuspendTransaction
        val data = transaction.state
        val oldData = dataState.value

        if (data != null) {
            dataState.value = data
        }

        if (data is MarketChartData.Data && (oldData !is MarketChartData.Data || oldData != data)) {
            if (lookState.value.animationOnDataChange) {
                startDrawingAnimation.emit(Unit)
            }
            withContext(dispatcher) {
                val minX = data.x.min()
                val minY = data.y.min()

                val normY = data.y.map { normalize(it, minY) }
                val normX = data.x.map { normalize(it, minX) }

                dsd = Pair(normX, normY)

                val entriesLocal = normX.mapIndexed { index, fl -> LineCartesianLayerModel.Entry(fl, normY[index]) }
                entries.value = entriesLocal

                modelProducer.runTransaction {
                    add(LineCartesianLayerModel.Partial(series = listOf(entriesLocal)))

                    updateExtras {
                        it[entriesKey] = entriesLocal
                        it[xKey] = data.x
                        it[yKey] = data.y
                    }
                }.await()
            }
        }

        nonSuspendTransaction?.let { handleTransaction(it) }
    }

    private fun handleTransaction(transaction: Transaction) {
        transaction.state?.let {
            dataState.value = it
        }
        transaction.look?.let {
            lookState.value = it
        }
    }

    companion object {
        private val entriesKey = ExtraStore.Key<List<LineCartesianLayerModel.Entry>>()
        private val xKey = ExtraStore.Key<List<BigDecimal>>()
        private val yKey = ExtraStore.Key<List<BigDecimal>>()

        private val initialData: MarketChartData = MarketChartData.NoData.Empty
        private val initialLook: MarketChartLook = MarketChartLook()

        /**
         * This function builds a MarketChartDataProducer with the given parameters.
         * It runs a suspending transaction block to initialize the data and look of the Market Chart.
         *
         * @param dispatcher The dispatcher to be used for data updates.
         * @param block The transaction block to be run.
         * @return A MarketChartDataProducer.
         */
        internal suspend fun buildSuspend(
            dispatcher: CoroutineDispatcher = Dispatchers.Default,
            block: TransactionSuspend.() -> Unit,
        ): MarketChartDataProducer {
            val transaction = TransactionSuspend(initialData, initialLook).apply(block)

            return MarketChartDataProducer(
                initialData = initialData,
                initialLook = initialLook,
                dispatcher = dispatcher,
            ).apply {
                handleTransactionSuspend(transaction)
            }
        }

        /**
         * This function builds a MarketChartDataProducer with the given parameters.
         * It runs a non-suspending transaction block to initialize the data and look of the Market Chart.
         *
         * @param dispatcher The dispatcher to be used for data updates.
         * @param block The transaction block to be run.
         * @return A MarketChartDataProducer.
         */
        internal fun build(
            dispatcher: CoroutineDispatcher = Dispatchers.Default,
            block: Transaction.() -> Unit,
        ): MarketChartDataProducer {
            val transaction = Transaction(initialData, initialLook).apply(block)

            return MarketChartDataProducer(
                initialData = transaction.state ?: initialData,
                initialLook = transaction.look ?: initialLook,
                dispatcher = dispatcher,
            )
        }
    }
}

private fun normalize(value: BigDecimal, min: BigDecimal, scale: Int = min.scale()): Float {
    val n = value - min
    return if (scale > 2) {
        n.movePointRight(scale - 2).toFloat()
    } else {
        n.toFloat()
    }
}
