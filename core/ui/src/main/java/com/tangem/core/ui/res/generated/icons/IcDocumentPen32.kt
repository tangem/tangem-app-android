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

private var _ic_document_pen_32: ImageVector? = null

val Icons.ic_document_pen_32: ImageVector
    get() {
        if (_ic_document_pen_32 != null) return _ic_document_pen_32!!
        _ic_document_pen_32 = ImageVector.Builder(
            name = "ic_document_pen_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M13.333 19.834C14.1614 19.834 14.833 20.5056 14.833 21.334C14.8326 22.162 14.1612 22.834 13.333 22.834H9.33301C8.50502 22.8338 7.83345 22.1619 7.83301 21.334C7.83301 20.5057 8.50475 19.8342 9.33301 19.834H13.333Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M13.333 14.5C14.1614 14.5 14.833 15.1716 14.833 16C14.8328 16.8283 14.1613 17.5 13.333 17.5H9.33301C8.50486 17.4998 7.83318 16.8282 7.83301 16C7.83301 15.1717 8.50475 14.5002 9.33301 14.5H13.333Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M18.667 9.16699C19.4952 9.16726 20.167 9.83873 20.167 10.667C20.1666 11.4949 19.4949 12.1667 18.667 12.167H9.33398C8.50583 12.167 7.83442 11.495 7.83398 10.667C7.83398 9.83857 8.50556 9.16699 9.33398 9.16699H18.667Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M21.333 2.5C23.6342 2.5 25.5 4.36581 25.5 6.66699V9.17969C26.496 9.25483 27.4707 9.67232 28.2324 10.4336C29.869 12.0713 29.9201 14.6944 28.3857 16.3936L28.2324 16.5557L25.5 19.2881V25.333C25.5 27.6342 23.6342 29.5 21.333 29.5H6.66699C4.36581 29.5 2.5 27.6342 2.5 25.333V6.66699C2.5 4.36581 4.36581 2.5 6.66699 2.5H21.333ZM6.66699 5.5C6.02266 5.5 5.5 6.02266 5.5 6.66699V25.333C5.5 25.9773 6.02266 26.5 6.66699 26.5H21.333C21.9773 26.5 22.5 25.9773 22.5 25.333V22.248C22.0089 22.6236 21.4068 22.8339 20.7812 22.834H17.333C16.505 22.8338 15.8334 22.162 15.833 21.334V17.8857C15.833 17.1341 16.1321 16.4129 16.6631 15.8818L22.1113 10.4346C22.2352 10.3108 22.3657 10.1955 22.5 10.0898V6.66699C22.5 6.02266 21.9773 5.5 21.333 5.5H6.66699ZM26.1104 12.5547C25.5913 12.0373 24.7511 12.0377 24.2324 12.5557L18.833 17.9551V19.834H20.7119L26.1104 14.4346C26.6291 13.9155 26.6291 13.0738 26.1104 12.5547Z"),
            )
        }.build()
        return _ic_document_pen_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcDocumentPen32Preview() {
    Icon(
        imageVector = Icons.ic_document_pen_32,
        contentDescription = null,
    )
}