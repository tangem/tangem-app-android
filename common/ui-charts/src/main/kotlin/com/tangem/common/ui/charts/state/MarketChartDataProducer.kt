package com.tangem.common.ui.charts.state

import androidx.compose.runtime.Stable
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
 * @property chartLook The updated look of the Market Chart.
 * @property chartData The updated state of the Market Chart.
 */
class Transaction(
    private val currentData: MarketChartData,
    private val currentLook: MarketChartLook,
) {
    var chartLook: MarketChartLook? = null
    var chartData: MarketChartData.NoData? = null

    fun updateLook(block: (prev: MarketChartLook) -> MarketChartLook) {
        chartLook = block(currentLook)
    }

    fun updateState(block: (prev: MarketChartData) -> MarketChartData.NoData) {
        chartData = block(currentData)
    }
}

/**
 * This class represents a transaction for updating the state and look of a Market Chart.
 * It extends the Transaction class and allows update state by data.
 *
 * @property chartData The updated state of the Market Chart.
 * @property chartLook The updated look of the Market Chart.
 */
class TransactionSuspend(
    private val currentData: MarketChartData,
    private val currentLook: MarketChartLook,
) {
    internal var nonSuspendTransaction: Transaction? = null
    var chartData: MarketChartData? = null
    var chartLook: MarketChartLook?
        get() = nonSuspendTransaction?.chartLook
        set(value) {
            if (nonSuspendTransaction == null) {
                nonSuspendTransaction = Transaction(currentData, currentLook)
            }
            nonSuspendTransaction?.chartLook = value
        }

    fun updateLook(block: (prev: MarketChartLook) -> MarketChartLook) {
        chartLook = block(currentLook)
    }

    internal fun updateState(block: (prev: MarketChartData) -> MarketChartData) {
        chartData = block(currentData)
    }

    internal fun updateData(block: (prev: MarketChartData.Data) -> MarketChartData.Data) {
        chartData = when (val currentState = currentData) {
            is MarketChartData.Data -> block(currentState)
            else -> currentState
        }
    }
}

@Stable
class MarketChartDataProducer private constructor(
    initialData: MarketChartData,
    initialLook: MarketChartLook,
    val pointsValuesConverter: PointValuesConverter = DefaultPointValuesConverter,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    internal val startDrawingAnimation = MutableSharedFlow<Unit>()
    internal val dataState = MutableStateFlow(initialData)
    internal val lookState = MutableStateFlow(initialLook)
    internal val entries = MutableStateFlow<List<LineCartesianLayerModel.Entry>>(emptyList())

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
        val chartData = transaction.chartData
        val oldData = dataState.value

        if (chartData != null) {
            dataState.value = chartData
        }

        if (chartData is MarketChartData.Data && (oldData !is MarketChartData.Data || oldData != chartData)) {
            if (lookState.value.animationOnDataChange) {
                startDrawingAnimation.emit(Unit)
            }
            withContext(dispatcher) {
                val rawData = pointsValuesConverter.convert(chartData)

                val entriesLocal =
                    rawData.x.mapIndexed { index, fl -> LineCartesianLayerModel.Entry(fl, rawData.y[index]) }

                entries.value = entriesLocal

                modelProducer.runTransaction {
                    add(LineCartesianLayerModel.Partial(series = listOf(entriesLocal)))

                    updateExtras {
                        it[entriesKey] = entriesLocal
                        it[xKey] = chartData.x
                        it[yKey] = chartData.y
                    }
                }.await()
            }
        }

        nonSuspendTransaction?.let { handleTransaction(it) }
    }

    private fun handleTransaction(transaction: Transaction) {
        transaction.chartData?.let {
            dataState.value = it
        }
        transaction.chartLook?.let {
            lookState.value = it
        }
    }

    companion object {
        internal val entriesKey = ExtraStore.Key<List<LineCartesianLayerModel.Entry>>()
        internal val xKey = ExtraStore.Key<List<BigDecimal>>()
        internal val yKey = ExtraStore.Key<List<BigDecimal>>()

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
        suspend fun buildSuspend(
            pointsValuesConverter: PointValuesConverter = DefaultPointValuesConverter,
            dispatcher: CoroutineDispatcher = Dispatchers.Default,
            block: TransactionSuspend.() -> Unit,
        ): MarketChartDataProducer {
            val transaction = TransactionSuspend(initialData, initialLook).apply(block)

            return MarketChartDataProducer(
                initialData = initialData,
                initialLook = initialLook,
                dispatcher = dispatcher,
                pointsValuesConverter = pointsValuesConverter,
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
        fun build(
            pointsValuesConverter: PointValuesConverter = DefaultPointValuesConverter,
            dispatcher: CoroutineDispatcher = Dispatchers.Default,
            block: Transaction.() -> Unit,
        ): MarketChartDataProducer {
            val transaction = Transaction(initialData, initialLook).apply(block)

            return MarketChartDataProducer(
                initialData = transaction.chartData ?: initialData,
                initialLook = transaction.chartLook ?: initialLook,
                dispatcher = dispatcher,
                pointsValuesConverter = pointsValuesConverter,
            )
        }
    }
}
