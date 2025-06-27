package com.tangem.common.ui.userwallet.converter

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import com.tangem.core.ui.components.artwork.ArtworkUM
import com.tangem.domain.models.ArtworkModel
import com.tangem.utils.converter.Converter
import javax.inject.Inject

class ArtworkUMConverter @Inject constructor() : Converter<ArtworkModel, ArtworkUM> {

    override fun convert(value: ArtworkModel): ArtworkUM {
        val bimap = try {
            value.verifiedArtwork?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        } catch (ignore: Exception) {
            null
        }
        return ArtworkUM(
            verifiedArtwork = bimap?.asImageBitmap(),
            defaultUrl = value.defaultUrl,
        )
    }
}