package com.tangem.tap.common.text

import android.widget.TextView
import com.tangem.tap.common.extensions.isEven

/**
[REDACTED_AUTHOR]
 */
enum class TruncateType {
    START, MIDDLE, END
}

interface Truncate {
    fun apply(tv: TextView, text: String, with: String): String

    companion object {
        fun create(type: TruncateType): Truncate {
            return when (type) {
              TruncateType.START -> TruncateStart()
              TruncateType.MIDDLE -> TruncateMiddle()
              TruncateType.END -> TruncateEnd()
            }
        }
    }
}

abstract class BaseTruncate : Truncate {
    protected var hasBeenTruncated = false

    override fun apply(tv: TextView, text: String, with: String): String {
        val roughLength = getRoughFitLength(tv, text, with)
        val fittedText = preciseFitting(tv, roughTruncate(text, roughLength), with)
        return if (hasBeenTruncated) attachWith(fittedText, with) else fittedText
    }

    protected fun getRoughFitLength(tv: TextView, text: String, with: String): Int {
        val existingSpace = tv.measuredWidth - (tv.paddingStart + tv.paddingEnd)
        val textWillTakeSpace = tv.paint.measureText(text)
        val overSizeRatio: Float = textWillTakeSpace / existingSpace

        val maxLengthOfText = text.length / overSizeRatio
        if (text.length <= maxLengthOfText) return text.length

        return maxLengthOfText.toInt()
    }

    protected fun preciseFitting(tv: TextView, text: String, with: String): String {
        if (!hasBeenTruncated) return text

        val spaceForText = tv.measuredWidth - (tv.paddingStart + tv.paddingEnd)

        var fittedText = text
        while (tv.paint.measureText(fittedText + with) > spaceForText) {
            fittedText = preciseTruncate(fittedText)
        }
        return fittedText
    }

    protected abstract fun roughTruncate(text: String, residualLength: Int): String
    protected abstract fun preciseTruncate(text: String): String
    protected abstract fun attachWith(text: String, with: String): String
}

class TruncateStart : BaseTruncate() {
    override fun roughTruncate(text: String, residualLength: Int): String {
        if (text.length <= residualLength) return text

        hasBeenTruncated = true
        return text.substring(residualLength, text.length)
    }

    override fun preciseTruncate(text: String): String = text.substring(1, text.length)

    override fun attachWith(text: String, with: String): String = with + text
}

class TruncateMiddle : BaseTruncate() {

    override fun roughTruncate(text: String, residualLength: Int): String {
        if (text.length <= residualLength) return text

        hasBeenTruncated = true
        val halfOfResidualLength = residualLength / 2
        val leftSide = text.substring(0, halfOfResidualLength)
        val rightSide = text.substring(text.length - halfOfResidualLength, text.length)

        return leftSide + rightSide
    }

    override fun preciseTruncate(text: String): String {
        val middlePosition = text.length / 2
        return if (text.length.isEven()) {
            val leftSide = text.substring(0, middlePosition - 1)
            val rightSide = text.substring(middlePosition, text.length)
            leftSide + rightSide
        } else {
            val leftSide = text.substring(0, middlePosition)
            val rightSide = text.substring(middlePosition + 1, text.length)
            leftSide + rightSide
        }
    }

    override fun attachWith(text: String, with: String): String {
        val cuttingPosition = text.length / 2
        val leftSide = text.substring(0, cuttingPosition)
        val rightSide = text.substring(cuttingPosition, text.length)
        return leftSide + with + rightSide
    }
}

class TruncateEnd : BaseTruncate() {
    override fun roughTruncate(text: String, residualLength: Int): String {
        if (text.length <= residualLength) return text

        hasBeenTruncated = true
        return text.substring(0, residualLength)
    }

    override fun preciseTruncate(text: String): String = text.substring(0, text.length - 1)

    override fun attachWith(text: String, with: String): String = text + with
}

fun TextView.truncateWith(text: String, type: TruncateType, with: String = "..."): String {
    val truncate = Truncate.create(type)
    return truncate.apply(this, text, with)
}

fun TextView.truncateStartWith(text: String, with: String = "..."): String =
        this.truncateWith(text, TruncateType.START, with)

fun TextView.truncateMiddleWith(text: String, with: String = "..."): String =
        this.truncateWith(text, TruncateType.MIDDLE, with)

fun TextView.truncateEndWith(text: String, with: String = "..."): String =
        this.truncateWith(text, TruncateType.END, with)