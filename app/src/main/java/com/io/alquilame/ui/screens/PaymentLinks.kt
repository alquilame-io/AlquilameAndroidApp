package com.io.alquilame.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.io.alquilame.R
import com.io.alquilame.ui.Nav
import com.io.alquilame.ui.common.ActionError
import com.io.alquilame.ui.common.AmountInput
import com.io.alquilame.ui.common.AppScreen
import com.io.alquilame.ui.common.Format
import com.io.alquilame.ui.common.Intents
import com.io.alquilame.ui.common.LoadBox
import com.io.alquilame.ui.common.MessageView
import com.io.alquilame.ui.common.TextInput
import com.io.alquilame.ui.common.rememberActions
import com.io.alquilame.ui.common.rememberLoader
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Stripe payment links: charge any amount by sharing a link, with or without a contract. */
@Composable
fun PaymentLinksScreen(nav: Nav) {
    val context = LocalContext.current
    val loader = rememberLoader("payment-links") { call { paymentLinks() } }
    val actions = rememberActions()
    var creating by rememberSaveable { mutableStateOf(false) }
    val copied = stringResource(R.string.link_copied)
    val settingsTitle = stringResource(R.string.payment_links)

    AppScreen(
        title = stringResource(R.string.payment_links),
        onBack = nav::back,
        floatingActionButton = {
            if (loader.data?.stripeConnected == true) {
                ExtendedFloatingActionButton(
                    onClick = { creating = true },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text(stringResource(R.string.new_link)) },
                )
            }
        },
    ) { padding ->
        LoadBox(loader, Modifier.padding(padding)) { links ->
            when {
                !links.stripeConnected -> MessageView(
                    icon = Icons.Default.Link,
                    text = stringResource(R.string.stripe_connect_explanation),
                    actionLabel = stringResource(R.string.connect_stripe),
                    onAction = { nav.web("/admin/settings/payment-settings", settingsTitle) },
                )
                links.items.isEmpty() -> MessageView(Icons.Default.Link, stringResource(R.string.links_empty))
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 96.dp),
                ) {
                    items(links.items, key = { it.id }) { link ->
                        Column(Modifier.padding(vertical = 8.dp)) {
                            PaymentLinkRow(
                                link = if (link.description.isNullOrBlank() && link.contractNumber != null) {
                                    link.copy(description = context.getString(R.string.contract_n, link.contractNumber))
                                } else {
                                    link
                                },
                                onCopy = { Intents.copy(context, link.url); actions.notify(copied) },
                                onShare = { Intents.shareText(context, link.url) },
                                onDeactivate = {
                                    actions.run {
                                        call { deactivatePaymentLink(link.id) }
                                        loader.reload(silent = true)
                                    }
                                },
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (creating) {
        var amount by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { creating = false },
            title = { Text(stringResource(R.string.new_link)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AmountInput(stringResource(R.string.amount), amount, { amount = it })
                    TextInput(stringResource(R.string.concept), description, { description = it })
                    ActionError(actions)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !actions.busy && (Format.parseAmount(amount)?.signum() ?: 0) > 0,
                    onClick = {
                        actions.run {
                            val link = call {
                                createPaymentLink(
                                    buildJsonObject {
                                        put("amount", Format.parseAmount(amount)?.toPlainString())
                                        put("description", description.trim())
                                    }
                                )
                            }
                            creating = false
                            loader.reload(silent = true)
                            Intents.shareText(context, link.url)
                        }
                    },
                ) { Text(stringResource(R.string.create_and_share)) }
            },
            dismissButton = { TextButton(onClick = { creating = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
