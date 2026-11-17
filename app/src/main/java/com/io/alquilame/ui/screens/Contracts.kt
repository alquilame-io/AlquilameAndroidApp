package com.io.alquilame.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.io.alquilame.R
import com.io.alquilame.data.Contract
import com.io.alquilame.data.ContractSummary
import com.io.alquilame.data.ExtraService
import com.io.alquilame.data.Payment
import com.io.alquilame.data.PaymentLink
import com.io.alquilame.ui.BottomBar
import com.io.alquilame.ui.Nav
import com.io.alquilame.ui.common.ActionError
import com.io.alquilame.ui.common.Actions
import com.io.alquilame.ui.common.AmountInput
import com.io.alquilame.ui.common.AppScreen
import com.io.alquilame.ui.common.ClientPickerDialog
import com.io.alquilame.ui.common.ConfirmDialog
import com.io.alquilame.ui.common.ContractStatusChip
import com.io.alquilame.ui.common.DateField
import com.io.alquilame.ui.common.Format
import com.io.alquilame.ui.common.IconLine
import com.io.alquilame.ui.common.Intents
import com.io.alquilame.ui.common.KeyValueRow
import com.io.alquilame.ui.common.LoadBox
import com.io.alquilame.ui.common.LocalBackend
import com.io.alquilame.ui.common.MessageView
import com.io.alquilame.ui.common.SearchField
import com.io.alquilame.ui.common.SectionTitle
import com.io.alquilame.ui.common.StatusChip
import com.io.alquilame.ui.common.TextInput
import com.io.alquilame.ui.common.money
import com.io.alquilame.ui.common.rememberActions
import com.io.alquilame.ui.common.rememberLoader
import com.io.alquilame.ui.theme.StatusColors
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

private val statuses = listOf("draft", "active", "completed", "cancelled")

@Composable
private fun statusLabel(status: String): String = stringResource(
    when (status) {
        "active" -> R.string.status_active
        "completed" -> R.string.status_completed
        "cancelled" -> R.string.status_cancelled
        else -> R.string.status_draft
    }
)

@Composable
fun ContractsScreen(nav: Nav) {
    val loader = rememberLoader("contracts") { call { contracts() } }
    var query by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf<String?>(null) }

    AppScreen(title = stringResource(R.string.tab_contracts), bottomBar = { BottomBar(nav, "contracts") }) { padding ->
        LoadBox(loader, Modifier.padding(padding)) { contracts ->
            val shown = remember(contracts, query, status) {
                val q = query.trim().lowercase()
                contracts.filter {
                    (status == null || it.status == status) &&
                        (q.isEmpty() || it.contractNumber.orEmpty().lowercase().contains(q) ||
                            it.client?.fullName.orEmpty().lowercase().contains(q))
                }
            }
            Column(Modifier.fillMaxSize()) {
                SearchField(
                    query,
                    { query = it },
                    stringResource(R.string.search_contracts),
                    Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        FilterChip(status == null, { status = null }, { Text(stringResource(R.string.all)) })
                    }
                    items(statuses) { value ->
                        FilterChip(
                            selected = status == value,
                            onClick = { status = if (status == value) null else value },
                            label = { Text(statusLabel(value)) },
                        )
                    }
                }
                if (shown.isEmpty()) {
                    MessageView(
                        Icons.Default.Description,
                        stringResource(if (contracts.isEmpty()) R.string.contracts_empty else R.string.no_results),
                    )
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(shown, key = { it.id }) { contract ->
                            ContractCard(contract) { nav.contract(contract.id) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContractCard(contract: ContractSummary, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    contract.contractNumber.orEmpty(),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                ContractStatusChip(contract.status)
            }
            IconLine(Icons.Default.Person, contract.client?.fullName ?: stringResource(R.string.no_client))
            IconLine(Icons.Default.CalendarMonth, Format.dateRange(contract.startDate, contract.endDate))
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(money(contract.grandTotal), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                val balance = Format.decimal(contract.remainingBalance)
                if (balance.signum() > 0) {
                    StatusChip(stringResource(R.string.balance_due, money(contract.remainingBalance)), StatusColors.warning)
                } else if (Format.decimal(contract.grandTotal).signum() > 0) {
                    StatusChip(stringResource(R.string.paid), StatusColors.success)
                }
            }
        }
    }
}

private enum class ContractDialog { Amounts, Service, Payment, Link, Client, Delete }

@Composable
fun ContractDetailScreen(nav: Nav, id: Int) {
    val context = LocalContext.current
    val tenant = LocalBackend.current.session.tenant
    val loader = rememberLoader("contract-$id") { call { contract(id) } }
    val actions = rememberActions()
    var dialog by rememberSaveable { mutableStateOf<ContractDialog?>(null) }
    var editingPayment by rememberSaveable { mutableStateOf<Int?>(null) }
    var menu by remember { mutableStateOf(false) }
    var statusMenu by remember { mutableStateOf(false) }
    val copied = stringResource(R.string.link_copied)

    fun pdf(path: String, prefix: String, share: Boolean) = actions.run {
        val number = loader.data?.contractNumber ?: id.toString()
        val file = download("api/mobile/v2/contracts/$id/$path", "${prefix}_$number.pdf")
        if (share) Intents.sharePdf(context, file) else Intents.openPdf(context, file)
    }

    AppScreen(
        title = loader.data?.contractNumber?.let { stringResource(R.string.contract_n, it) }
            ?: stringResource(R.string.contract),
        onBack = nav::back,
        actions = {
            if (loader.data != null) {
                IconButton(onClick = { pdf("pdf", "contrato", share = false) }, enabled = !actions.busy) {
                    Icon(Icons.Default.PictureAsPdf, stringResource(R.string.view_pdf))
                }
                IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, stringResource(R.string.more_options)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.share_pdf)) },
                        leadingIcon = { Icon(Icons.Default.Share, null) },
                        onClick = { menu = false; pdf("pdf", "contrato", share = true) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.service_order)) },
                        leadingIcon = { Icon(Icons.Default.Description, null) },
                        onClick = { menu = false; pdf("service-order", "orden", share = false) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete_contract)) },
                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                        onClick = { menu = false; dialog = ContractDialog.Delete },
                    )
                }
            }
        },
    ) { padding ->
        LoadBox(loader, Modifier.padding(padding)) { contract ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // ---- Status, client and dates
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box {
                            Row(
                                Modifier.clickable { statusMenu = true },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ContractStatusChip(contract.status)
                                Icon(Icons.Default.ArrowDropDown, stringResource(R.string.change_status))
                            }
                            DropdownMenu(expanded = statusMenu, onDismissRequest = { statusMenu = false }) {
                                statuses.forEach { status ->
                                    DropdownMenuItem(
                                        text = { Text(statusLabel(status)) },
                                        enabled = status != contract.status,
                                        onClick = {
                                            statusMenu = false
                                            actions.run {
                                                loader.set(
                                                    call { updateContractStatus(id, buildJsonObject { put("status", status) }) }
                                                )
                                            }
                                        },
                                    )
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconLine(
                                Icons.Default.Person,
                                contract.client?.fullName ?: stringResource(R.string.no_client),
                                Modifier.weight(1f),
                            )
                            TextButton(onClick = { dialog = ContractDialog.Client }) {
                                Text(stringResource(if (contract.client == null) R.string.assign else R.string.change))
                            }
                        }
                        IconLine(Icons.Default.CalendarMonth, Format.dateRange(contract.startDate, contract.endDate))
                        if (!contract.notes.isNullOrBlank()) {
                            Text(contract.notes, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // ---- Products
                SectionTitle(stringResource(R.string.products))
                contract.products.forEach { line ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${line.quantity} × ${line.name}", maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(
                                listOfNotNull(
                                    line.price?.let { money(it) },
                                    pluralStringResource(R.plurals.days, line.days, line.days),
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(money(line.subtotal))
                    }
                }
                if (contract.reservationGroupId != null || contract.reservationId != null) {
                    TextButton(onClick = {
                        if (contract.reservationGroupId != null) {
                            nav.group(contract.reservationGroupId)
                        } else if (contract.reservationId != null) {
                            nav.reservation(contract.reservationId)
                        }
                    }) { Text(stringResource(R.string.view_reservation)) }
                }

                // ---- Extra services
                SectionTitle(stringResource(R.string.extra_services)) {
                    TextButton(onClick = { dialog = ContractDialog.Service }) {
                        Icon(Icons.Default.Add, null)
                        Text(stringResource(R.string.add))
                    }
                }
                contract.extraServices.forEachIndexed { index, service ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(service.name, Modifier.weight(1f))
                        Text(money(service.price))
                        IconButton(
                            enabled = !actions.busy,
                            onClick = {
                                val remaining = contract.extraServices.filterIndexed { i, _ -> i != index }
                                actions.run { loader.set(call { updateContract(id, servicesBody(remaining)) }) }
                            },
                        ) { Icon(Icons.Default.RemoveCircleOutline, stringResource(R.string.remove)) }
                    }
                }

                // ---- Totals
                HorizontalDivider()
                KeyValueRow(stringResource(R.string.subtotal), money(contract.subtotal))
                if (Format.decimal(contract.discount).signum() > 0) {
                    KeyValueRow(stringResource(R.string.discount), "− ${money(contract.discount)}")
                }
                if (contract.includesTax) {
                    KeyValueRow(
                        stringResource(R.string.tax_n, Format.editable(contract.taxValue)),
                        money(contract.taxAmount),
                    )
                }
                KeyValueRow(stringResource(R.string.total), money(contract.grandTotal), strong = true)
                if (Format.decimal(contract.depositAmount).signum() > 0) {
                    KeyValueRow(stringResource(R.string.deposit), money(contract.depositAmount))
                }
                KeyValueRow(stringResource(R.string.paid), money(contract.totalPaid), valueColor = StatusColors.success)
                KeyValueRow(
                    stringResource(R.string.balance),
                    money(contract.remainingBalance),
                    strong = true,
                    valueColor = if (Format.decimal(contract.remainingBalance).signum() > 0) {
                        StatusColors.warning
                    } else {
                        StatusColors.success
                    },
                )
                OutlinedButton(onClick = { dialog = ContractDialog.Amounts }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.edit_amounts))
                }

                // ---- Payments
                SectionTitle(stringResource(R.string.payments)) {
                    TextButton(onClick = { editingPayment = null; dialog = ContractDialog.Payment }) {
                        Icon(Icons.Default.Add, null)
                        Text(stringResource(R.string.record_payment))
                    }
                }
                if (contract.payments.isEmpty()) {
                    Text(
                        stringResource(R.string.no_payments),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                contract.payments.forEach { payment ->
                    Row(
                        Modifier.clickable { editingPayment = payment.id; dialog = ContractDialog.Payment },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(money(payment.amount), fontWeight = FontWeight.SemiBold)
                            Text(
                                listOfNotNull(
                                    paymentMethodLabel(payment.paymentMethod),
                                    Format.mediumDate(payment.paymentDate),
                                    payment.referenceNumber?.takeIf { it.isNotBlank() },
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(
                            enabled = !actions.busy,
                            onClick = { actions.run { loader.set(call { deletePayment(payment.id) }) } },
                        ) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
                    }
                }

                // ---- Stripe payment links
                SectionTitle(stringResource(R.string.payment_links)) {
                    if (tenant?.stripeConnected == true) {
                        TextButton(onClick = { dialog = ContractDialog.Link }) {
                            Icon(Icons.Default.Add, null)
                            Text(stringResource(R.string.create_link))
                        }
                    }
                }
                if (tenant?.stripeConnected != true) {
                    Text(
                        stringResource(R.string.stripe_not_connected),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                contract.paymentLinks.forEach { link ->
                    PaymentLinkRow(
                        link = link,
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
            }

            when (dialog) {
                ContractDialog.Amounts -> AmountsDialog(contract, actions, { dialog = null }) { body ->
                    actions.run { loader.set(call { updateContract(id, body) }); dialog = null }
                }
                ContractDialog.Service -> ServiceDialog(actions, { dialog = null }) { service ->
                    actions.run {
                        loader.set(call { updateContract(id, servicesBody(contract.extraServices + service)) })
                        dialog = null
                    }
                }
                ContractDialog.Payment -> {
                    val payment = contract.payments.firstOrNull { it.id == editingPayment }
                    PaymentDialog(contract, payment, actions, { dialog = null }) { body ->
                        actions.run {
                            loader.set(call { if (payment == null) createPayment(id, body) else updatePayment(payment.id, body) })
                            dialog = null
                        }
                    }
                }
                ContractDialog.Link -> ContractLinkDialog(contract, actions, { dialog = null }) { amount ->
                    actions.run {
                        val updated = call {
                            createContractPaymentLink(id, buildJsonObject { put("amount", amount.toPlainString()) })
                        }
                        loader.set(updated)
                        dialog = null
                        updated.paymentLinks.maxByOrNull { it.id }?.let { Intents.shareText(context, it.url) }
                    }
                }
                ContractDialog.Client -> ClientPickerDialog(
                    onPick = { client ->
                        dialog = null
                        actions.run {
                            loader.set(call { updateContract(id, buildJsonObject { put("client_id", client.id) }) })
                        }
                    },
                    onDismiss = { dialog = null },
                )
                ContractDialog.Delete -> ConfirmDialog(
                    title = stringResource(R.string.delete_contract),
                    text = stringResource(R.string.delete_contract_text),
                    confirmLabel = stringResource(R.string.delete),
                    onConfirm = { actions.run { call { deleteContract(id) }; nav.back() } },
                    onDismiss = { dialog = null },
                )
                null -> Unit
            }
        }
    }
}

private fun servicesBody(services: List<ExtraService>) = buildJsonObject { putServices(services) }

fun JsonObjectBuilder.putServices(services: List<ExtraService>) {
    putJsonArray("extra_services") {
        services.forEach { service ->
            addJsonObject {
                put("name", service.name)
                put("price", service.price ?: "0")
                put("description", service.description)
            }
        }
    }
}

@Composable
fun paymentMethodLabel(method: String): String = stringResource(
    when (method) {
        "cash" -> R.string.method_cash
        "bank_transfer" -> R.string.method_transfer
        "check" -> R.string.method_check
        "card" -> R.string.method_card
        else -> R.string.method_other
    }
)

@Composable
fun PaymentLinkRow(link: PaymentLink, onCopy: () -> Unit, onShare: () -> Unit, onDeactivate: () -> Unit) {
    val active = link.status == "active" && link.paymentStatus == "unpaid"
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(money(link.amount), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                when {
                    link.paymentStatus == "paid" -> StatusChip(stringResource(R.string.paid), StatusColors.success)
                    link.paymentStatus == "refunded" ->
                        StatusChip(stringResource(R.string.refunded), MaterialTheme.colorScheme.outline)
                    link.status != "active" ->
                        StatusChip(stringResource(R.string.inactive), MaterialTheme.colorScheme.outline)
                    else -> StatusChip(stringResource(R.string.pending), StatusColors.warning)
                }
            }
            val detail = listOfNotNull(
                link.description?.takeIf { it.isNotBlank() },
                Format.mediumDate(link.paidAt ?: link.insertedAt).takeIf { it.isNotEmpty() },
            ).joinToString(" · ")
            if (detail.isNotEmpty()) {
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (active) {
            IconButton(onClick = onCopy) { Icon(Icons.Default.ContentCopy, stringResource(R.string.copy_link)) }
            IconButton(onClick = onShare) { Icon(Icons.Default.Share, stringResource(R.string.share)) }
            IconButton(onClick = onDeactivate) { Icon(Icons.Default.LinkOff, stringResource(R.string.deactivate)) }
        }
    }
}

@Composable
private fun AmountsDialog(contract: Contract, actions: Actions, onDismiss: () -> Unit, onSave: (kotlinx.serialization.json.JsonObject) -> Unit) {
    var start by remember { mutableStateOf(Format.date(contract.startDate)) }
    var end by remember { mutableStateOf(Format.date(contract.endDate)) }
    var deposit by remember { mutableStateOf(Format.editable(contract.depositAmount)) }
    var discount by remember { mutableStateOf(Format.editable(contract.discount)) }
    var tax by remember { mutableStateOf(contract.includesTax) }
    var taxValue by remember { mutableStateOf(Format.editable(contract.taxValue).ifEmpty { "16" }) }
    var notes by remember { mutableStateOf(contract.notes.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_amounts)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DateField(stringResource(R.string.date_from), start, { start = it })
                DateField(stringResource(R.string.date_to), end, { end = it })
                AmountInput(stringResource(R.string.deposit), deposit, { deposit = it })
                AmountInput(stringResource(R.string.discount), discount, { discount = it })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.add_tax), Modifier.weight(1f))
                    Switch(checked = tax, onCheckedChange = { tax = it })
                }
                if (tax) {
                    TextInput(
                        stringResource(R.string.tax_rate),
                        taxValue,
                        { new -> taxValue = new.filter { it.isDigit() || it == '.' } },
                        keyboard = KeyboardType.Decimal,
                        suffix = "%",
                    )
                }
                TextInput(stringResource(R.string.notes), notes, { notes = it }, singleLine = false)
                ActionError(actions)
            }
        },
        confirmButton = {
            TextButton(
                enabled = !actions.busy,
                onClick = {
                    onSave(
                        buildJsonObject {
                            start?.let { put("start_date", it.toString()) }
                            end?.let { put("end_date", it.toString()) }
                            put("deposit_amount", (Format.parseAmount(deposit) ?: BigDecimal.ZERO).toPlainString())
                            put("discount", (Format.parseAmount(discount) ?: BigDecimal.ZERO).toPlainString())
                            put("includes_tax", tax)
                            if (tax) put("tax_value", taxValue.ifBlank { "0" })
                            put("notes", notes.trim())
                        }
                    )
                },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun ServiceDialog(actions: Actions, onDismiss: () -> Unit, onSave: (ExtraService) -> Unit) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_service)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextInput(stringResource(R.string.service_name), name, { name = it })
                AmountInput(stringResource(R.string.price), price, { price = it })
                ActionError(actions)
            }
        },
        confirmButton = {
            TextButton(
                enabled = !actions.busy && name.isNotBlank() && Format.parseAmount(price) != null,
                onClick = { onSave(ExtraService(name.trim(), Format.parseAmount(price)?.toPlainString())) },
            ) { Text(stringResource(R.string.add)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

private val paymentMethods = listOf("cash", "bank_transfer", "card", "check", "other")

@Composable
private fun PaymentDialog(
    contract: Contract,
    payment: Payment?,
    actions: Actions,
    onDismiss: () -> Unit,
    onSave: (kotlinx.serialization.json.JsonObject) -> Unit,
) {
    val remaining = Format.decimal(contract.remainingBalance)
    var amount by remember {
        mutableStateOf(
            when {
                payment != null -> Format.editable(payment.amount)
                remaining.signum() > 0 -> Format.editable(contract.remainingBalance)
                else -> ""
            }
        )
    }
    var method by remember { mutableStateOf(payment?.paymentMethod ?: "cash") }
    var date by remember { mutableStateOf(Format.date(payment?.paymentDate) ?: LocalDate.now()) }
    var reference by remember { mutableStateOf(payment?.referenceNumber.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (payment == null) R.string.record_payment else R.string.edit_payment)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AmountInput(stringResource(R.string.amount), amount, { amount = it })
                paymentMethods.forEach { value ->
                    Row(
                        Modifier.fillMaxWidth().selectable(method == value, role = Role.RadioButton) { method = value },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = method == value, onClick = null, Modifier.padding(8.dp))
                        Text(paymentMethodLabel(value))
                    }
                }
                DateField(stringResource(R.string.date), date, { if (it != null) date = it })
                TextInput(stringResource(R.string.reference), reference, { reference = it })
                ActionError(actions)
            }
        },
        confirmButton = {
            TextButton(
                enabled = !actions.busy && (Format.parseAmount(amount)?.signum() ?: 0) > 0,
                onClick = {
                    onSave(
                        buildJsonObject {
                            put("amount", Format.parseAmount(amount)?.toPlainString())
                            put("payment_method", method)
                            put("payment_date", date.toString())
                            put("reference_number", reference.trim())
                        }
                    )
                },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Amount choice for a contract payment link: the deposit, the remaining balance or a custom amount. */
@Composable
private fun ContractLinkDialog(contract: Contract, actions: Actions, onDismiss: () -> Unit, onCreate: (BigDecimal) -> Unit) {
    val deposit = Format.decimal(contract.depositAmount)
    val remaining = Format.decimal(contract.remainingBalance)
    var choice by remember { mutableStateOf(if (remaining.signum() > 0) "remaining" else "custom") }
    var custom by remember { mutableStateOf("") }
    val amount = when (choice) {
        "deposit" -> deposit
        "remaining" -> remaining
        else -> Format.parseAmount(custom) ?: BigDecimal.ZERO
    }

    @Composable
    fun Option(value: String, label: String) {
        Row(
            Modifier.fillMaxWidth().selectable(choice == value, role = Role.RadioButton) { choice = value },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = choice == value, onClick = null, Modifier.padding(8.dp))
            Text(label)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_link)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (remaining.signum() > 0) {
                    Option("remaining", stringResource(R.string.link_remaining, money(contract.remainingBalance)))
                }
                if (deposit.signum() > 0) {
                    Option("deposit", stringResource(R.string.link_deposit, money(contract.depositAmount)))
                }
                Option("custom", stringResource(R.string.link_custom))
                if (choice == "custom") AmountInput(stringResource(R.string.amount), custom, { custom = it })
                ActionError(actions)
            }
        },
        confirmButton = {
            TextButton(enabled = !actions.busy && amount.signum() > 0, onClick = { onCreate(amount) }) {
                Text(stringResource(R.string.create_and_share))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
