package com.io.alquilame.ui.common

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/** Hand-offs to other apps: sharing, dialling, WhatsApp, PDF viewers. */
object Intents {
    private fun Context.start(intent: Intent): Boolean = try {
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    fun shareText(context: Context, text: String) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        context.start(Intent.createChooser(send, null))
    }

    fun copy(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Alquilame", text))
    }

    fun openUrl(context: Context, url: String) = context.start(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    fun dial(context: Context, phone: String) =
        context.start(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.filter { it.isDigit() || it == '+' }}")))

    fun email(context: Context, address: String) =
        context.start(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$address")))

    fun whatsApp(context: Context, phone: String, text: String = "") {
        val digits = phone.filter { it.isDigit() }
        openUrl(context, "https://wa.me/$digits" + if (text.isNotEmpty()) "?text=${Uri.encode(text)}" else "")
    }

    private fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.files", file)

    /** Opens a downloaded PDF in a viewer, or falls back to the share sheet. */
    fun openPdf(context: Context, file: File) {
        val uri = uriFor(context, file)
        val view = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/pdf")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (!context.start(view)) sharePdf(context, file)
    }

    fun sharePdf(context: Context, file: File) {
        val uri = uriFor(context, file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("application/pdf")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.start(Intent.createChooser(send, null).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
    }
}
