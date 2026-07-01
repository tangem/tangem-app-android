@file:Suppress("all")

package com.tangem.core.ui.res.generated.icons

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Auto-generated from design tokens. Do not edit manually.
 */

private var _ic_contact_book_24_filled: ImageVector? = null

val Icons.ic_contact_book_24_filled: ImageVector
    get() {
        if (_ic_contact_book_24_filled != null) return _ic_contact_book_24_filled!!
        _ic_contact_book_24_filled = ImageVector.Builder(
            name = "ic_contact_book_24_filled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M17.2129 3.01367C18.5081 3.14732 19.4998 4.25745 19.5 5.58301V18.417C19.4996 19.8307 18.3715 20.9999 16.9502 21H7.0498C5.62854 20.9999 4.50043 19.8307 4.5 18.417V17.8945H3C2.44797 17.8945 2.00041 17.4465 2 16.8945C2 16.3422 2.44772 15.8945 3 15.8945H4.5V12.9844H3C2.44781 12.9844 2.00015 12.5365 2 11.9844C2 11.4321 2.44772 10.9844 3 10.9844H4.5V8.0752H3C2.44781 8.0752 2.00015 7.62735 2 7.0752C2 6.52291 2.44772 6.0752 3 6.0752H4.5V5.58301C4.50017 4.16912 5.62838 2.99913 7.0498 2.99902H16.9502L17.2129 3.01367ZM10.6104 12.8867C9.49218 12.8899 8.57322 13.7392 8.45996 14.8281L8.44824 15.0488V15.3252C8.44831 15.6136 8.56268 15.8908 8.7666 16.0947C8.97055 16.2985 9.24782 16.4131 9.53613 16.4131H14.4648C15.0652 16.4128 15.5516 15.9256 15.5518 15.3252V15.0488C15.5486 13.9309 14.6999 13.012 13.6113 12.8984L13.3896 12.8867H10.6104ZM12 7.58789C10.8399 7.58798 9.8995 8.52839 9.89941 9.68848C9.8996 10.8485 10.84 11.789 12 11.7891C13.16 11.789 14.1004 10.8485 14.1006 9.68848C14.1005 8.52839 13.1601 7.58798 12 7.58789Z"),
            )
        }.build()
        return _ic_contact_book_24_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcContactBook24FilledPreview() {
    Icon(
        imageVector = Icons.ic_contact_book_24_filled,
        contentDescription = null,
    )
}