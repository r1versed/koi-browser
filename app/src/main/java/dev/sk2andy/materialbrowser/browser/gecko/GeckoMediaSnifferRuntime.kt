package dev.sk2andy.materialbrowser.browser.gecko

import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONObject
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtensionController

/** Built-in extension that reports video files requested by pages to the app. */
internal class GeckoMediaSnifferRuntime(
    private val controller: WebExtensionController,
    private val mainHandler: Handler = Handler(Looper.getMainLooper()),
) {
    private var port: WebExtension.Port? = null
    private var started = false

    fun start() {
        if (started) return
        started = true
        runCatching {
            controller.ensureBuiltIn(LOCATION, EXTENSION_ID).withHandler(mainHandler).accept(
                { installed ->
                    if (installed == null || installed.id != EXTENSION_ID) fail("Unexpected sniffer identity")
                    else installed.setMessageDelegate(MessageDelegate(), NATIVE_APP)
                },
                { fail("Sniffer installation failed") },
            )
        }.onFailure { fail("Sniffer could not start") }
    }

    private fun fail(reason: String) {
        Log.w(TAG, reason)
        started = false
    }

    private inner class MessageDelegate : WebExtension.MessageDelegate {
        override fun onConnect(connected: WebExtension.Port) {
            val sender = connected.sender
            if (connected.name != NATIVE_APP || sender.webExtension.id != EXTENSION_ID ||
                sender.environmentType != WebExtension.MessageSender.ENV_TYPE_EXTENSION ||
                sender.session != null
            ) {
                connected.disconnect()
                return
            }
            val previous = port
            port = connected
            connected.setDelegate(object : WebExtension.PortDelegate {
                override fun onPortMessage(message: Any, sourcePort: WebExtension.Port) {
                    if (sourcePort !== port) return
                    val value = message as? JSONObject ?: return
                    mainHandler.post { KoiMediaStore.handle(value) }
                }

                override fun onDisconnect(sourcePort: WebExtension.Port) {
                    if (sourcePort === port) port = null
                }
            })
            previous?.takeIf { it !== connected }?.let { runCatching { it.disconnect() } }
        }
    }

    companion object {
        const val EXTENSION_ID = "candy-media-sniffer@sk2andy.dev"
        private const val LOCATION = "resource://android/assets/candy_media_sniffer/"
        private const val NATIVE_APP = "candyMediaSniffer"
        private const val TAG = "KoiMedia"
    }
}
