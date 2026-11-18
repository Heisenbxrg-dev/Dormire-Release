package com.music.spotui.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.myspoti.engine.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

@Composable
fun CopyAppLogButton() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    TextButton(enabled = !busy, onClick = {
        busy = true
        scope.launch {
            try {
                val report = withContext(Dispatchers.IO) {
                    val file = File.createTempFile("dormire-logcat-", ".txt", context.cacheDir)
                    try {
                        val process = ProcessBuilder("logcat", "-d", "-v", "threadtime", "--pid=${android.os.Process.myPid()}", "-t", "1500")
                            .redirectErrorStream(true).redirectOutput(file).start()
                        if (!process.waitFor(5, TimeUnit.SECONDS)) {
                            process.destroyForcibly()
                            throw java.io.IOException("logcat timeout")
                        }
                        if (process.exitValue() != 0) throw java.io.IOException("logcat unavailable")
                        val raw = file.inputStream().use { input ->
                            val bytes = java.io.ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            while (bytes.size() < 250_000) {
                                val count = input.read(buffer, 0, minOf(buffer.size, 250_000 - bytes.size()))
                                if (count < 0) break
                                bytes.write(buffer, 0, count)
                            }
                            bytes.toString("UTF-8")
                        }
                        "Dormire ${app.myspoti.engine.BuildConfig.VERSION_NAME}\nAndroid SDK ${android.os.Build.VERSION.SDK_INT}\n\n" +
                            com.music.spotui.data.BetaLogs.sanitize(raw)
                                .replace(Regex("(?im)(cookie|set-cookie|authorization|x-csrf-token)\\s*[:=]\\s*[^\\r\\n]+"), "$1: <hidden>")
                    } finally { file.delete() }
                }
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(ClipData.newPlainText("Dormire logcat", report))
                android.widget.Toast.makeText(context, context.getString(R.string.app_log_copied), android.widget.Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                android.widget.Toast.makeText(context, context.getString(R.string.app_log_failed), android.widget.Toast.LENGTH_LONG).show()
            } finally { busy = false }
        }
    }) { Text(stringResource(if (busy) R.string.app_log_collecting else R.string.app_log_copy)) }
}
