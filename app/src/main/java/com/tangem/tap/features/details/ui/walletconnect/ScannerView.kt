package com.tangem.tap.features.details.ui.walletconnect

import android.content.Context
import android.hardware.Camera
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.widget.Toast
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import com.tangem.tap.common.extensions.toast
import me.dm7.barcodescanner.core.BarcodeScannerView
import me.dm7.barcodescanner.core.DisplayUtils

@Deprecated("delete this when zxing fix https://github.com/dm77/barcodescanner/issues/294 issue")
class ScannerView : BarcodeScannerView {

    private var mMultiFormatReader: MultiFormatReader? = null

    private var mFormats: List<BarcodeFormat>? = null
    private var mResultHandler: ResultHandler? = null

    constructor(context: Context?) : super(context) {
        initMultiFormatReader()
    }


    constructor(context: Context?, attributeSet: AttributeSet?) : super(context, attributeSet) {
        initMultiFormatReader()
    }

    fun setFormats(formats: List<BarcodeFormat>?) {
        mFormats = formats
        initMultiFormatReader()
    }

    fun setResultHandler(resultHandler: ResultHandler?) {
        mResultHandler = resultHandler
    }

    fun getFormats(): Collection<BarcodeFormat?>? {
        return if (mFormats == null) ALL_FORMATS else mFormats
    }

    private fun initMultiFormatReader() {
        var hints: MutableMap<DecodeHintType, Any?> = mutableMapOf()
        hints[DecodeHintType.POSSIBLE_FORMATS] = getFormats()
        mMultiFormatReader = MultiFormatReader()
        mMultiFormatReader!!.setHints(hints)
    }

    override fun onPreviewFrame(data: ByteArray?, camera: Camera) {
        var data = data
        if (mResultHandler != null) {
            try {
                val parameters = camera.parameters
                val size = parameters.previewSize
                var width = size.width
                var height = size.height
                if (DisplayUtils.getScreenOrientation(this.context) == 1) {
                    val rotationCount = this.rotationCount
                    if (rotationCount == 1 || rotationCount == 3) {
                        val tmp = width
                        width = height
                        height = tmp
                    }
                    data = getRotatedData(data, camera)
                }
                var rawResult: Result? = null
                val source = buildLuminanceSource(data, width, height)
                if (source != null) {
                    var bitmap = BinaryBitmap(HybridBinarizer(source))
                    try {
                        rawResult = mMultiFormatReader!!.decodeWithState(bitmap)
                    } catch (var29: ReaderException) {
                    } catch (var30: NullPointerException) {
                    } catch (var31: ArrayIndexOutOfBoundsException) {
                    } finally {
                        mMultiFormatReader!!.reset()
                    }
                    if (rawResult == null) {
                        val invertedSource = source.invert()
                        bitmap = BinaryBitmap(HybridBinarizer(invertedSource))
                        try {
                            rawResult = mMultiFormatReader!!.decodeWithState(bitmap)
                        } catch (var27: NotFoundException) {
                        } finally {
                            mMultiFormatReader!!.reset()
                        }
                    }
                }
                if (rawResult != null) {
                    val handler = Handler(Looper.getMainLooper())
                    handler.post {
                        val tmpResultHandler: ResultHandler = this.mResultHandler!!
                        this.mResultHandler = null
                        this.stopCameraPreview()
                        if (tmpResultHandler != null) {
                            tmpResultHandler.handleResult(rawResult)
                        }
                    }
                } else {
                    camera.setOneShotPreviewCallback(this)
                }
            } catch (var33: RuntimeException) {
                Log.e("ZXingScannerView", var33.toString(), var33)
                toast("WRONG QR",Toast.LENGTH_LONG)
            }
        }
    }

    fun resumeCameraPreview(resultHandler: ResultHandler?) {
        mResultHandler = resultHandler
        super.resumeCameraPreview()
    }

    fun buildLuminanceSource(data: ByteArray?, width: Int, height: Int): PlanarYUVLuminanceSource? {
        val rect = getFramingRectInPreview(width, height)
        return if (rect == null) {
            null
        } else {
            var source: PlanarYUVLuminanceSource? = null
            try {
                source =
                    PlanarYUVLuminanceSource(
                        data,
                        width,
                        height,
                        rect.left,
                        rect.top,
                        rect.width(),
                        rect.height(),
                        false,
                    )
            } catch (var7: Exception) {
            }
            source
        }
    }

    companion object
    {
        var ALL_FORMATS: MutableList<BarcodeFormat?> = mutableListOf(
        BarcodeFormat.AZTEC,
        BarcodeFormat.CODABAR,
        BarcodeFormat.CODE_39,
        BarcodeFormat.CODE_93,
        BarcodeFormat.CODE_128,
        BarcodeFormat.DATA_MATRIX,
        BarcodeFormat.EAN_8,
        BarcodeFormat.EAN_13,
        BarcodeFormat.ITF,
        BarcodeFormat.MAXICODE,
        BarcodeFormat.PDF_417,
        BarcodeFormat.QR_CODE,
        BarcodeFormat.RSS_14,
        BarcodeFormat.RSS_EXPANDED,
        BarcodeFormat.UPC_A,
        BarcodeFormat.UPC_E,
        BarcodeFormat.UPC_EAN_EXTENSION)
    }

    interface ResultHandler {
        fun handleResult(var1: Result?)
    }
}
