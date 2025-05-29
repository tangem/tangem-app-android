package com.tangem.feature.tester.presentation.testpush.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.tangem.core.ui.R
import com.tangem.core.ui.components.OutlineTextField
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.components.buttons.common.TangemButtonSize
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.tester.presentation.testpush.entity.TestPushClickIntents
import com.tangem.feature.tester.presentation.testpush.entity.TestPushUM

private const val SWIPE_THRESHOLD = 0.25f

@Composable
internal fun TestPushScreen(testPushUM: TestPushUM, testPushClickIntents: TestPushClickIntents) {
    Scaffold(
        topBar = {
            AppBarWithBackButton(onBackClick = testPushClickIntents::onBackClick)
        },
        bottomBar = {
            SendLocalPushButton(testPushUM)
        },
        containerColor = TangemTheme.colors.background.secondary,
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(it)
                .fillMaxSize(),
        ) {
            tokenData(fcmToken = testPushUM.fcmToken)
            contentData(testPushUM = testPushUM, testPushClickIntents = testPushClickIntents)
            extraData(testPushUM = testPushUM, testPushClickIntents = testPushClickIntents)
        }
    }
}

private fun LazyListScope.tokenData(fcmToken: String) {
    item("token_key") {
        val clipboard = LocalClipboardManager.current
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(TangemTheme.colors.background.action)
                .padding(16.dp)
                .animateItem(),
        ) {
            Text(
                text = "FCM Token",
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.primary1,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(TangemTheme.colors.background.tertiary)
                    .clickable {
                        clipboard.setText(AnnotatedString(fcmToken))
                    }
                    .padding(8.dp),
            ) {
                Text(
                    text = fcmToken,
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.primary1,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .weight(1f),
                )
                Icon(
                    painter = rememberVectorPainter(
                        ImageVector.vectorResource(R.drawable.ic_copy_24),
                    ),
                    tint = TangemTheme.colors.icon.secondary,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

private fun LazyListScope.contentData(testPushUM: TestPushUM, testPushClickIntents: TestPushClickIntents) {
    item("content_key") {
        Column(
            modifier = Modifier
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(TangemTheme.colors.background.action)
                .padding(16.dp)
                .animateItem(),
        ) {
            Text(
                text = "Content",
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.primary1,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlineTextField(
                label = "Title",
                value = testPushUM.title,
                onValueChange = testPushClickIntents::onTitleChange,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlineTextField(
                label = "Message",
                value = testPushUM.message,
                onValueChange = testPushClickIntents::onMessageChange,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun LazyListScope.extraData(testPushUM: TestPushUM, testPushClickIntents: TestPushClickIntents) {
    item("data_key") {
        Text(
            text = "Data",
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.primary1,
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topEnd = 16.dp, topStart = 16.dp))
                .background(TangemTheme.colors.background.action)
                .padding(16.dp)
                .animateItem(),
        )
    }

    itemsIndexed(
        items = testPushUM.data,
        key = { index, _ -> index },
        contentType = { _, item -> item::class },
    ) { index, (key, value) ->
        EnterPushData(
            key = key,
            value = value,
            index = index,
            testPushClickIntents = testPushClickIntents,
            modifier = Modifier
                .background(TangemTheme.colors.background.action)
                .padding(horizontal = 16.dp)
                .animateItem(),
        )
    }

    item("data_add_key") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                .background(TangemTheme.colors.background.action)
                .padding(top = 8.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
                .animateItem(),
        ) {
            PrimaryButton(
                text = "Add param",
                onClick = testPushClickIntents::onDataAdd,
                size = TangemButtonSize.Action,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}

@Composable
private fun EnterPushData(
    key: TextFieldValue,
    value: TextFieldValue,
    index: Int,
    testPushClickIntents: TestPushClickIntents,
    modifier: Modifier = Modifier,
) {
    val swipeToDelete = rememberSwipeToDismissBoxState(
        positionalThreshold = { it * SWIPE_THRESHOLD },
    )

    SwipeToDismissBox(
        modifier = modifier,
        state = swipeToDelete,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(1.dp)
                    .background(Color.Red),
            ) {
                Text(
                    text = "Remove",
                    style = TangemTheme.typography.caption1,
                    color = TangemTheme.colors.text.constantWhite,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(8.dp),
                )
            }
        },
    ) {
        Column(
            modifier = Modifier.background(TangemTheme.colors.background.action),
        ) {
            if (index != 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            OutlineTextField(
                label = "Key",
                value = key,
                onValueChange = {
                    testPushClickIntents.onDataKeyChange(index, it)
                },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlineTextField(
                label = "Value",
                value = value,
                onValueChange = {
                    testPushClickIntents.onDataValueChange(index, it)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    when (swipeToDelete.currentValue) {
        SwipeToDismissBoxValue.EndToStart -> {
            LaunchedEffect(swipeToDelete) {
                testPushClickIntents.onDataRemove(index)
                swipeToDelete.snapTo(SwipeToDismissBoxValue.Settled)
            }
        }
        SwipeToDismissBoxValue.StartToEnd -> {}
        SwipeToDismissBoxValue.Settled -> {}
    }
}

@Composable
private fun SendLocalPushButton(testPushUM: TestPushUM) {
    val context = LocalContext.current

    PrimaryButton(
        text = "Send test push",
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        onClick = {
            val channelId = "Test Tangem Channel"

            val intent = Intent(context, Class.forName("com.tangem.tap.MainActivity")).apply {
                testPushUM.data.forEach { (key, value) ->
                    putExtra(key.text, value.text)
                }
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val pendingIntent = PendingIntent.getActivity(
                /* context = */ context,
                /* requestCode = */ 1,
                /* intent = */ intent,
                /* flags = */ PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
            )

            val notificationBuilder =
                NotificationCompat.Builder(context.applicationContext, channelId)
                    .setSmallIcon(com.tangem.feature.tester.impl.R.drawable.ic_tangem_24)
                    .setContentTitle(testPushUM.title.text)
                    .setContentText(testPushUM.message.text)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)

            val notificationManager =
                context.applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationChannel = NotificationChannel(
                    channelId,
                    ContextCompat.getString(
                        context.applicationContext,
                        R.string.common_tangem_wallet,
                    ),
                    NotificationManager.IMPORTANCE_HIGH,
                )
                notificationManager.createNotificationChannel(notificationChannel)
            }

            // Generating unique notification id
            val uniqueId = (System.currentTimeMillis() % Integer.MAX_VALUE).toInt()

            notificationManager.notify(
                /* id = */ uniqueId,
                /* notification = */ notificationBuilder.build(),
            )
        },
    )
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TestPushScreen_Preview(@PreviewParameter(TestPushScreenPreviewProvider::class) params: TestPushUM) {
    TangemThemePreview {
        TestPushScreen(
            testPushUM = params,
            testPushClickIntents = object : TestPushClickIntents {
                override fun onTitleChange(value: TextFieldValue) {}
                override fun onMessageChange(value: TextFieldValue) {}
                override fun onDataKeyChange(index: Int, value: TextFieldValue) {}
                override fun onDataValueChange(index: Int, value: TextFieldValue) {}
                override fun onDataAdd() {}
                override fun onDataRemove(index: Int) {}
                override fun onBackClick() {}
            },
        )
    }
}

private class TestPushScreenPreviewProvider : PreviewParameterProvider<TestPushUM> {
    override val values: Sequence<TestPushUM>
        get() = sequenceOf(
            TestPushUM(
                fcmToken = "qwertyuiopoiuytrewqwertyuiopoiuytrewqertyuiop",
                title = TextFieldValue("Test push"),
                message = TextFieldValue("Test push"),
                data = listOf(
                    TextFieldValue("deeplink") to TextFieldValue("tangem://main"),
                    TextFieldValue("") to TextFieldValue(""),
                ),
            ),
        )
}
// endregion