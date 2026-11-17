package com.io.alquilame.ui.screens

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Web
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.io.alquilame.BuildConfig
import com.io.alquilame.R
import com.io.alquilame.data.ApiException
import com.io.alquilame.ui.BottomBar
import com.io.alquilame.ui.Nav
import com.io.alquilame.ui.common.AppScreen
import com.io.alquilame.ui.common.ConfirmDialog
import com.io.alquilame.ui.common.Intents
import com.io.alquilame.ui.common.LocalBackend
import com.io.alquilame.ui.common.MessageView
import com.io.alquilame.ui.common.NavRow
import com.io.alquilame.ui.common.SectionTitle
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/** Everything that does not fit in the bottom bar, plus the desktop-oriented sections of the web admin. */
@Composable
fun MoreScreen(nav: Nav) {
    val context = LocalContext.current
    val session = LocalBackend.current.session
    val tenant = session.tenant
    var confirmLogout by rememberSaveable { mutableStateOf(false) }

    AppScreen(title = stringResource(R.string.tab_more), bottomBar = { BottomBar(nav, "more") }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(tenant?.name.orEmpty(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    listOfNotNull(tenant?.email, tenant?.plan?.let { stringResource(R.string.plan_n, it) })
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            NavRow(Icons.Default.People, stringResource(R.string.clients), onClick = { nav.clients() })
            NavRow(Icons.Default.RequestQuote, stringResource(R.string.quotes), onClick = { nav.quotes() })
            NavRow(Icons.Default.Link, stringResource(R.string.payment_links), onClick = { nav.paymentLinks() })
            NavRow(Icons.Default.BarChart, stringResource(R.string.reports), onClick = { nav.reports() })
            HorizontalDivider()

            SectionTitle(stringResource(R.string.web_admin), Modifier.padding(horizontal = 16.dp))
            val webSections = listOf(
                Triple(Icons.Default.Web, R.string.web_my_site, "/admin/my-site"),
                Triple(Icons.Default.GridOn, R.string.web_floor_plans, "/admin/floor-plans"),
                Triple(Icons.Default.Settings, R.string.web_settings, "/admin/settings"),
                Triple(Icons.Default.WorkspacePremium, R.string.web_billing, "/admin/settings/billing"),
            )
            webSections.forEach { (icon, label, path) ->
                val title = stringResource(label)
                NavRow(icon, title, onClick = { nav.web(path, title) })
            }
            tenant?.storefrontUrl?.takeIf { it.startsWith("http") }?.let { url ->
                NavRow(
                    Icons.Default.Storefront,
                    stringResource(R.string.open_storefront),
                    subtitle = url.substringAfter("://"),
                    onClick = { Intents.openUrl(context, url) },
                )
            }
            HorizontalDivider()
            NavRow(Icons.AutoMirrored.Filled.Logout, stringResource(R.string.logout), onClick = { confirmLogout = true })
            Text(
                stringResource(R.string.version_n, BuildConfig.VERSION_NAME),
                Modifier.fillMaxWidth().padding(16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (confirmLogout) {
        ConfirmDialog(
            title = stringResource(R.string.logout),
            text = stringResource(R.string.logout_text),
            confirmLabel = stringResource(R.string.logout),
            onConfirm = {
                CookieManager.getInstance().removeAllCookies(null)
                session.clear()
            },
            onDismiss = { confirmLogout = false },
        )
    }
}

/**
 * Sections of the web admin that are built for large screens (site builder,
 * floor plans, settings). A one-time nonce signs the embedded browser in,
 * then it is sent to [path].
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebAdminScreen(nav: Nav, path: String, title: String) {
    val backend = LocalBackend.current
    var authUrl by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableStateOf(0) }
    var progress by remember { mutableStateOf(0) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var fileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        fileCallback?.onReceiveValue(uris.toTypedArray())
        fileCallback = null
    }

    LaunchedEffect(attempt) {
        error = null
        try {
            authUrl = backend.call {
                webLogin(
                    buildJsonObject {
                        putJsonObject("device_info") {
                            put("os", "Android")
                            put("version", Build.VERSION.RELEASE)
                            put("model", Build.MODEL)
                            put("app_version", BuildConfig.VERSION_NAME)
                        }
                    }
                )
            }.authUrl
        } catch (e: ApiException) {
            error = e.message
        }
    }

    BackHandler { if (webView?.canGoBack() == true) webView?.goBack() else nav.back() }

    AppScreen(title = title, onBack = nav::back) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            val url = authUrl
            when {
                error != null -> MessageView(
                    Icons.Default.CloudOff,
                    error.orEmpty(),
                    actionLabel = stringResource(R.string.retry),
                    onAction = { attempt++ },
                )
                url != null -> AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            CookieManager.getInstance().setAcceptCookie(true)
                            var redirected = false
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView, finished: String) {
                                    // The nonce login lands on the dashboard; continue to the requested section.
                                    val uri = Uri.parse(finished)
                                    if (!redirected && uri.path.orEmpty().startsWith("/admin")) {
                                        redirected = true
                                        if (uri.path != path) view.loadUrl("${uri.scheme}://${uri.authority}$path")
                                    }
                                }
                            }
                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView, newProgress: Int) {
                                    progress = newProgress
                                }

                                override fun onShowFileChooser(
                                    view: WebView,
                                    callback: ValueCallback<Array<Uri>>,
                                    params: FileChooserParams,
                                ): Boolean {
                                    fileCallback?.onReceiveValue(null)
                                    fileCallback = callback
                                    filePicker.launch(params.acceptTypes.firstOrNull { it.isNotBlank() } ?: "*/*")
                                    return true
                                }
                            }
                            loadUrl(url)
                            webView = this
                        }
                    },
                    onRelease = { it.destroy() },
                )
            }
            if (error == null && progress < 100) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
    }
}
