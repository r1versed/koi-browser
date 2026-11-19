package dev.sk2andy.materialbrowser.ui

import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.gecko.KoiDetectedMedia
import dev.sk2andy.materialbrowser.browser.gecko.KoiMediaStore

private const val KOI_BROWSER_USER_AGENT =
    "Mozilla/5.0 (Android 14; Mobile; rv:128.0) Gecko/128.0 Firefox/128.0"

@Composable
internal fun BoxScope.KoiMediaOverlay(
    pageUrl: String?,
    hidden: Boolean,
    incognito: Boolean,
) {
    val context = LocalContext.current
    val items = KoiMediaStore.itemsFor(pageUrl)
    var open by remember { mutableStateOf(false) }
    if (!hidden && !incognito && items.isNotEmpty()) {
        Surface(
            onClick = { open = true },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .safeDrawingPadding()
                .padding(end = 16.dp, bottom = 96.dp)
                .zIndex(24f),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_reader_download),
                    contentDescription = "Videos found on this page",
                    modifier = Modifier.size(22.dp),
                )
                Text(items.size.toString(), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
    if (open && items.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text("Videos on this page") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    items.forEach { item ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    open = false
                                    if (item.isStream) copyLink(context, item) else saveMedia(context, item)
                                }
                                .padding(vertical = 10.dp),
                        ) {
                            Text(
                                KoiMediaStore.fileNameFor(item),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            val size = if (item.sizeBytes >= 0) {
                                Formatter.formatShortFileSize(context, item.sizeBytes)
                            } else {
                                "size unknown"
                            }
                            Text(
                                if (item.isStream) "Stream, tap to copy link" else KoiMediaStore.labelFor(item) + ", " + size + ", tap to save",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { open = false }) { Text("Close") } },
        )
    }
}

private fun saveMedia(context: Context, item: KoiDetectedMedia) {
    val name = KoiMediaStore.fileNameFor(item)
    runCatching {
        val request = DownloadManager.Request(Uri.parse(item.url))
            .setTitle(name)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Koi/" + name)
            .addRequestHeader("User-Agent", KOI_BROWSER_USER_AGENT)
        if (item.pageUrl.isNotBlank()) request.addRequestHeader("Referer", item.pageUrl)
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
    }.onSuccess {
        Toast.makeText(context, "Saving " + name + " to Downloads/Koi", Toast.LENGTH_LONG).show()
    }.onFailure {
        Toast.makeText(context, "Could not start the download", Toast.LENGTH_LONG).show()
    }
}

private fun copyLink(context: Context, item: KoiDetectedMedia) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Video stream", item.url))
    Toast.makeText(context, "Stream link copied. Saving streams is not supported yet.", Toast.LENGTH_LONG).show()
}
