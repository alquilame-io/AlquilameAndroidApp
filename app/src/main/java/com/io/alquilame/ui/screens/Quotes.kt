package com.io.alquilame.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.io.alquilame.R
import com.io.alquilame.data.Backend
import com.io.alquilame.data.ClientBrief
import com.io.alquilame.data.ExtraService
import com.io.alquilame.data.Merchandise
import com.io.alquilame.data.Quote
import com.io.alquilame.data.QuoteSummary
import com.io.alquilame.ui.Nav
import com.io.alquilame.ui.common.ActionError
import com.io.alquilame.ui.common.AmountInput
import com.io.alquilame.ui.common.AppScreen
import com.io.alquilame.ui.common.ClientField
import com.io.alquilame.ui.common.ConfirmDialog
import com.io.alquilame.ui.common.DateField
import com.io.alquilame.ui.common.FormBody
import com.io.alquilame.ui.common.Format
import com.io.alquilame.ui.common.IconLine
import com.io.alquilame.ui.common.Intents
import com.io.alquilame.ui.common.KeyValueRow
import com.io.alquilame.ui.common.LoadBox
import com.io.alquilame.ui.common.LocalBackend
import com.io.alquilame.ui.common.MessageView
import com.io.alquilame.ui.common.ProductImage
import com.io.alquilame.ui.common.ProductPickerDialog
import com.io.alquilame.ui.common.QuantityStepper
import com.io.alquilame.ui.common.SectionTitle
import com.io.alquilame.ui.common.TextInput
import com.io.alquilame.ui.common.TimeField
import com.io.alquilame.ui.common.money
import com.io.alquilame.ui.common.rememberActions
import com.io.alquilame.ui.common.rememberLoader
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

@Composable
fun QuotesScreen(nav: Nav) {
    val loader = rememberLoader("quotes") { call { quotes() } }

    AppScreen(
        title = stringResource(R.string.quotes),
        onBack = nav::back,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { nav.newQuote() },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text(stringResource(R.string.new_quote)) },
            )
        },
    ) { padding ->
        LoadBox(loader, Modifier.padding(padding)) { quotes ->
            if (quotes.isEmpty()) {
                MessageView(Icons.Default.RequestQuote, stringResource(R.string.quotes_empty))
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(quotes, key = { it.id }) { quote -> QuoteCard(quote) { nav.quote(quote.id) } }
                }
            }
        }
    }
}

@Composable
private fun QuoteCard(quote: QuoteSummary, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    quote.quoteNumber.orEmpty(),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(money(quote.total), style = MaterialTheme.typography.titleMedium)
            }
            IconLine(Icons.Default.Person, quote.client?.fullName ?: stringResource(R.string.no_client))
            val dates = Format.dateRange(quote.startDate, quote.endDate)
            if (dates.isNotEmpty()) IconLine(Icons.Default.CalendarMonth, dates)
            Text(
                pluralStringResource(R.plurals.products, quote.itemsCount, quote.itemsCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

data class QuoteLine(val productId: Int, val name: String, val thumbUrl: String?, val price: BigDecimal, val quantity: Int)

/** Editable copy of a quote; a ViewModel so it survives rotation. */
class QuoteDraft : ViewModel() {
    var loaded by mutableStateOf(false)
    var number by mutableStateOf<String?>(null)
    var client: ClientBrief? by mutableStateOf(null)
    var startDate: LocalDate? by mutableStateOf(null)
    var endDate: LocalDate? by mutableStateOf(null)
    var discount by mutableStateOf("")
    var tax by mutableStateOf(false)
    var taxRate by mutableStateOf("16")
    val lines = mutableStateListOf<QuoteLine>()
    val services = mutableStateListOf<ExtraService>()

    fun load(quote: Quote) {
        number = quote.quoteNumber
        client = quote.client
        startDate = Format.date(quote.startDate)
        endDate = Format.date(quote.endDate)
        discount = Format.editable(quote.discount)
        tax = quote.includesTax
        taxRate = Format.editable(quote.taxValue).ifEmpty { "16" }
        lines.clear()
        lines.addAll(
            quote.items.mapNotNull { item ->
                item.merchandise?.let {
                    QuoteLine(it.id, it.name, it.thumbUrl, Format.decimal(item.price), item.quantity)
                }
            }
        )
        services.clear()
        services.addAll(quote.extraServices)
        loaded = true
    }

    val discountAmount: BigDecimal get() = Format.parseAmount(discount) ?: BigDecimal.ZERO

    val subtotal: BigDecimal
        get() = lines.fold(BigDecimal.ZERO) { acc, line -> acc + line.price * line.quantity.toBigDecimal() } +
            services.fold(BigDecimal.ZERO) { acc, service -> acc + Format.decimal(service.price) } - discountAmount

    val taxAmount: BigDecimal
        get() = if (tax) {
            (subtotal * (taxRate.toBigDecimalOrNull() ?: BigDecimal.ZERO))
                .divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

    fun body(): JsonObject = buildJsonObject {
        put("client_id", client?.id)
        // The API needs both dates or none; a single day is a one-day range.
        put("start_date", startDate?.toString())
        put("end_date", (endDate?.takeIf { end -> startDate?.let { !end.isBefore(it) } ?: false } ?: startDate)?.toString())
        put("discount", discountAmount.toPlainString())
        put("includes_tax", tax)
        if (tax) put("tax_value", taxRate.ifBlank { "0" })
        putServices(services)
        putJsonArray("items") {
            lines.forEach { line ->
                addJsonObject {
                    put("merchandise_id", line.productId)
                    put("quantity", line.quantity)
                    put("price", line.price.toPlainString())
                }
            }
        }
    }
}

private enum class QuoteDialog { Products, Service, Convert, Delete }

/** Creates or edits a quote; an existing quote can be shared as PDF or booked as a contract. */
@Composable
fun QuoteScreen(nav: Nav, id: Int?) {
    val context = LocalContext.current
    val currency = LocalBackend.current.session.tenant?.currency ?: "MXN"
    val draft: QuoteDraft = viewModel()
    val existing = if (id != null) rememberLoader("quote-$id") { call { quote(id) } } else null
    val actions = rememberActions()
    var dialog by rememberSaveable { mutableStateOf<QuoteDialog?>(null) }
    var menu by remember { mutableStateOf(false) }
    val saved = stringResource(R.string.saved)

    if (id == null && !draft.loaded) draft.loaded = true
    val quote = existing?.data
    LaunchedEffect(quote) { if (quote != null && !draft.loaded) draft.load(quote) }

    /** Persists the draft; returns the quote id. */
    suspend fun Backend.persist(): Int =
        if (id == null) call { createQuote(draft.body()) }.id else call { updateQuote(id, draft.body()) }.id

    fun save() = actions.run {
        val quoteId = persist()
        if (id == null) {
            nav.back()
            nav.quote(quoteId)
        } else {
            actions.notify(saved)
        }
    }

    fun pdf(share: Boolean) = actions.run {
        val quoteId = persist()
        val file = download("api/mobile/v2/quotes/$quoteId/pdf", "cotizacion_${draft.number ?: quoteId}.pdf")
        if (share) Intents.sharePdf(context, file) else Intents.openPdf(context, file)
    }

    AppScreen(
        title = draft.number?.let { stringResource(R.string.quote_n, it) } ?: stringResource(R.string.new_quote),
        onBack = nav::back,
        actions = {
            if (id != null && draft.loaded) {
                IconButton(onClick = { pdf(share = false) }, enabled = !actions.busy) {
                    Icon(Icons.Default.PictureAsPdf, stringResource(R.string.view_pdf))
                }
                IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, stringResource(R.string.more_options)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.share_pdf)) },
                        leadingIcon = { Icon(Icons.Default.Share, null) },
                        onClick = { menu = false; pdf(share = true) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.convert_to_contract)) },
                        leadingIcon = { Icon(Icons.Default.Description, null) },
                        onClick = { menu = false; dialog = QuoteDialog.Convert },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete_quote)) },
                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                        onClick = { menu = false; dialog = QuoteDialog.Delete },
                    )
                }
            }
        },
    ) { padding ->
        if (existing != null && !draft.loaded) {
            LoadBox(existing, Modifier.padding(padding)) { }
            return@AppScreen
        }
        FormBody(
            padding = padding,
            saveLabel = stringResource(R.string.save_quote),
            onSave = ::save,
            saving = actions.busy,
            enabled = draft.lines.isNotEmpty() || draft.services.isNotEmpty(),
        ) {
            ClientField(draft.client, { draft.client = it })
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DateField(
                    stringResource(R.string.date_from),
                    draft.startDate,
                    { draft.startDate = it; if (it == null) draft.endDate = null },
                    Modifier.weight(1f),
                    clearable = true,
                )
                DateField(
                    stringResource(R.string.date_to),
                    draft.endDate,
                    { draft.endDate = it },
                    Modifier.weight(1f),
                    clearable = true,
                    enabled = draft.startDate != null,
                )
            }

            SectionTitle(stringResource(R.string.products))
            draft.lines.forEachIndexed { index, line ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProductImage(line.thumbUrl, 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(line.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(
                            Format.money(line.price * line.quantity.toBigDecimal(), currency),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    QuantityStepper(line.quantity, { quantity ->
                        if (quantity <= 0) draft.lines.removeAt(index) else draft.lines[index] = line.copy(quantity = quantity)
                    })
                }
            }
            OutlinedButton(onClick = { dialog = QuoteDialog.Products }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(if (draft.lines.isEmpty()) R.string.add_products else R.string.edit_products))
            }

            SectionTitle(stringResource(R.string.extra_services)) {
                TextButton(onClick = { dialog = QuoteDialog.Service }) {
                    Icon(Icons.Default.Add, null)
                    Text(stringResource(R.string.add))
                }
            }
            draft.services.forEachIndexed { index, service ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(service.name, Modifier.weight(1f))
                    Text(money(service.price))
                    IconButton(onClick = { draft.services.removeAt(index) }) {
                        Icon(Icons.Default.RemoveCircleOutline, stringResource(R.string.remove))
                    }
                }
            }

            HorizontalDivider()
            AmountInput(stringResource(R.string.discount), draft.discount, { draft.discount = it })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.add_tax), Modifier.weight(1f))
                Switch(checked = draft.tax, onCheckedChange = { draft.tax = it })
            }
            if (draft.tax) {
                TextInput(
                    stringResource(R.string.tax_rate),
                    draft.taxRate,
                    { new -> draft.taxRate = new.filter { it.isDigit() || it == '.' } },
                    keyboard = KeyboardType.Decimal,
                    suffix = "%",
                )
            }
            KeyValueRow(stringResource(R.string.subtotal), Format.money(draft.subtotal, currency))
            if (draft.tax) {
                KeyValueRow(stringResource(R.string.tax_n, draft.taxRate), Format.money(draft.taxAmount, currency))
            }
            KeyValueRow(
                stringResource(R.string.total),
                Format.money(draft.subtotal + draft.taxAmount, currency),
                strong = true,
            )
        }
    }

    when (dialog) {
        QuoteDialog.Products -> ProductPickerDialog(
            date = draft.startDate,
            startTime = null,
            endTime = null,
            initial = draft.lines.associate { it.productId to it.quantity },
            enforceStock = false,
            onDone = { picked ->
                val prices = draft.lines.associate { it.productId to it.price }
                draft.lines.clear()
                draft.lines.addAll(picked.map { it.toLine(prices[it.product.id]) })
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        QuoteDialog.Service -> ServiceDialog(actions, { dialog = null }) { service ->
            draft.services.add(service)
            dialog = null
        }
        QuoteDialog.Convert -> ConvertDialog(
            draft = draft,
            busy = actions.busy,
            error = { ActionError(actions) },
            onDismiss = { dialog = null },
        ) { start, end, startTime, endTime ->
            actions.run {
                val quoteId = persist()
                val contract = call {
                    convertQuote(
                        quoteId,
                        buildJsonObject {
                            put("start_date", start.toString())
                            put("end_date", (end ?: start).toString())
                            put("start_time", startTime?.let(Format::hour))
                            put("end_time", endTime?.let(Format::hour))
                        },
                    )
                }
                dialog = null
                nav.contract(contract.id, replace = true)
            }
        }
        QuoteDialog.Delete -> ConfirmDialog(
            title = stringResource(R.string.delete_quote),
            text = stringResource(R.string.delete_quote_text),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = { if (id != null) actions.run { call { deleteQuote(id) }; nav.back() } },
            onDismiss = { dialog = null },
        )
        null -> Unit
    }
}

private fun com.io.alquilame.ui.common.PickedProduct.toLine(knownPrice: BigDecimal?): QuoteLine {
    val item: Merchandise = product
    return QuoteLine(item.id, item.name, item.thumbUrl, knownPrice ?: Format.decimal(item.price), quantity)
}

/** Dates (and optional hours) to reserve when a quote becomes a contract. */
@Composable
private fun ConvertDialog(
    draft: QuoteDraft,
    busy: Boolean,
    error: @Composable () -> Unit,
    onDismiss: () -> Unit,
    onConvert: (LocalDate, LocalDate?, LocalTime?, LocalTime?) -> Unit,
) {
    var start by remember { mutableStateOf(draft.startDate) }
    var end by remember { mutableStateOf(draft.endDate) }
    var startTime by remember { mutableStateOf<LocalTime?>(null) }
    var endTime by remember { mutableStateOf<LocalTime?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.convert_to_contract)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.convert_explanation), style = MaterialTheme.typography.bodyMedium)
                DateField(stringResource(R.string.date_from), start, { start = it })
                DateField(stringResource(R.string.date_to_optional), end, { end = it }, clearable = true)
                TimeField(stringResource(R.string.time_from), startTime, { startTime = it })
                TimeField(stringResource(R.string.time_to), endTime, { endTime = it })
                error()
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy && start != null && draft.lines.isNotEmpty(),
                onClick = { start?.let { onConvert(it, end?.takeIf { e -> !e.isBefore(it) }, startTime, endTime) } },
            ) { Text(stringResource(R.string.create_contract)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
