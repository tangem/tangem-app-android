package com.tangem.features.polymarket.impl.common.ui.chart

import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerDrawingModel
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerDrawingModel.PointInfo
import com.patrykandpatrick.vico.core.common.data.DefaultDrawingModelInterpolator
import com.patrykandpatrick.vico.core.common.data.DrawingModelInterpolator

/** Vico defaults new points to the plot bottom. Continue an appended tail from its previous endpoint instead. */
internal class ProbabilityDrawingModelInterpolator :
    DrawingModelInterpolator<PointInfo, LineCartesianLayerDrawingModel> {
    private val delegate = DefaultDrawingModelInterpolator<PointInfo, LineCartesianLayerDrawingModel>()

    override fun setModels(old: LineCartesianLayerDrawingModel?, new: LineCartesianLayerDrawingModel?) {
        val seeded = if (old != null && new != null) {
            LineCartesianLayerDrawingModel(
                pointInfo = new.mapIndexed { index, points ->
                    val previous = old.getOrNull(index).orEmpty()
                    val last = previous.maxByOrNull { it.key }
                    points.mapValues { (x, point) ->
                        previous[x] ?: if (last != null && x > last.key) last.value else point
                    }
                },
                zeroY = old.zeroY,
                opacity = old.opacity,
            )
        } else {
            old
        }
        delegate.setModels(seeded, new)
    }

    override suspend fun transform(fraction: Float): LineCartesianLayerDrawingModel? = delegate.transform(fraction)
}