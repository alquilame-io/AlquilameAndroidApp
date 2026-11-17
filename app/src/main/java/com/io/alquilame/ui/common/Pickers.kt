package com.io.alquilame.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.io.alquilame.R
import com.io.alquilame.data.ClientBrief
import com.io.alquilame.data.Merchandise
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Read-only text field that opens a picker when tapped. */
@Composable
private fun PickerField(
    label: String,
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val interactions = remember { MutableInteractionSource() }
    LaunchedEffect(interactions, enabled) {
        interactions.interactions.collect { if (it is PressInteraction.Release && enabled) onClick() }
    }
    OutlinedTextField(
        value = text,
        onValueChange = {},
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        readOnly = true,
        enabled = enabled,
        singleLine = true,
        leadingIcon = { Icon(icon, null) },
        trailingIcon = if (onClear != null && text.isNotEmpty() && enabled) {
            { IconButton(onClick = onClear) { Icon(Icons.Default.Clear, stringResource(R.string.clear)) } }
        } else {
            null
        },
        interactionSource = interactions,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    value: LocalDate?,
    onChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    clearable: Boolean = false,
    enabled: Boolean = true,
) {
    var open by remember { mutableStateOf(false) }
    PickerField(
        label = label,
        text = Format.mediumDate(value),
        icon = Icons.Default.CalendarMonth,
        onClick = { open = true },
        modifier = modifier,
        onClear = if (clearable) ({ onChange(null) }) else null,
        enabled = enabled,
    )
    if (open) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (value ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    open = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(state) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeField(label: String, value: LocalTime?, onChange: (LocalTime?) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    PickerField(
        label = label,
        text = Format.hour(value),
        icon = Icons.Default.Schedule,
        onClick = { open = true },
        modifier = modifier,
        onClear = { onChange(null) },
    )
    if (open) {
        val state = rememberTimePickerState(
            initialHour = value?.hour ?: 10,
            initialMinute = value?.minute ?: 0,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    onChange(LocalTime.of(state.hour, state.minute))
                    open = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.cancel)) } },
            text = { TimePicker(state) },
        )
    }
}

/** Client selector field; opens [ClientPickerDialog]. */
@Composable
fun ClientField(value: ClientBrief?, onChange: (ClientBrief?) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    PickerField(
        label = stringResource(R.string.client),
        text = value?.fullName ?: "",
        icon = Icons.Default.People,
        onClick = { open = true },
        modifier = modifier,
        onClear = { onChange(null) },
    )
    if (open) {
        ClientPickerDialog(onPick = { onChange(it); open = false }, onDismiss = { open = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullScreenDialog(
    title: String,
    onDismiss: () -> Unit,
    actions: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            modifier = Modifier.fillMaxSize().imePadding(),
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, stringResource(R.string.close)) }
                    },
                    actions = { actions() },
                )
            },
            content = content,
        )
    }
}

/** Searchable client list with an inline "new client" form for walk-ins. */
@Composable
fun ClientPickerDialog(onPick: (ClientBrief) -> Unit, onDismiss: () -> Unit) {
    val loader = rememberLoader("client-picker") { call { clients() } }
    val actions = rememberActions()
    var query by rememberSaveable { mutableStateOf("") }
    var creating by rememberSaveable { mutableStateOf(false) }

    FullScreenDialog(
        title = stringResource(if (creating) R.string.new_client else R.string.select_client),
        onDismiss = { if (creating) creating = false else onDismiss() },
    ) { padding ->
        if (creating) {
            var firstName by rememberSaveable { mutableStateOf("") }
            var lastName by rememberSaveable { mutableStateOf("") }
            var phone by rememberSaveable { mutableStateOf("") }
            Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextInput(stringResource(R.string.first_name), firstName, { firstName = it })
                TextInput(stringResource(R.string.last_name), lastName, { lastName = it })
                TextInput(stringResource(R.string.phone), phone, { phone = it }, keyboard = KeyboardType.Phone)
                ActionError(actions)
                Button(
                    onClick = {
                        actions.run {
                            val client = call {
                                createClient(
                                    buildJsonObject {
                                        put("first_name", firstName.trim())
                                        put("last_name", lastName.trim())
                                        put("phone", phone.trim())
                                    }
                                )
                            }
                            onPick(client.brief())
                        }
                    },
                    enabled = firstName.isNotBlank() && lastName.isNotBlank() && !actions.busy,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text(stringResource(R.string.save)) }
            }
        } else {
            Column(Modifier.padding(padding)) {
                SearchField(
                    query,
                    { query = it },
                    stringResource(R.string.search_clients),
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                NavRow(Icons.Default.PersonAdd, stringResource(R.string.new_client), onClick = { creating = true })
                HorizontalDivider()
                LoadBox(loader) { clients ->
                    val shown = remember(clients, query) {
                        val q = query.trim().lowercase()
                        clients.filter {
                            q.isEmpty() || it.fullName.lowercase().contains(q) || it.phone.orEmpty().contains(q)
                        }
                    }
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(shown, key = { it.id }) { client ->
                            Column(
                                Modifier.fillMaxWidth().clickable { onPick(client.brief()) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(client.fullName, style = MaterialTheme.typography.bodyLarge)
                                val detail = listOfNotNull(client.phone, client.email).filter { it.isNotBlank() }
                                if (detail.isNotEmpty()) {
                                    Text(
                                        detail.joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A product chosen in [ProductPickerDialog] with its quantity. */
data class PickedProduct(val product: Merchandise, val quantity: Int)

/**
 * Catalog with the units still free on [date]. Steppers are capped at the
 * available stock, so an overbooking is caught before the form is sent.
 *
 * @param initial quantities already chosen, by product id
 * @param enforceStock false for quotes, which may exceed the current stock
 */
@Composable
fun ProductPickerDialog(
    date: LocalDate?,
    startTime: LocalTime?,
    endTime: LocalTime?,
    initial: Map<Int, Int>,
    onDone: (List<PickedProduct>) -> Unit,
    onDismiss: () -> Unit,
    excludeGroupId: Int? = null,
    excludeReservationId: Int? = null,
    enforceStock: Boolean = true,
    singleChoice: Boolean = false,
) {
    val key = "stock-$date-$startTime-$endTime-$excludeGroupId-$excludeReservationId"
    val loader = rememberLoader(key) {
        call {
            stock(
                date?.toString(),
                startTime?.let(Format::hour),
                endTime?.let(Format::hour),
                excludeReservationId,
                excludeGroupId,
            )
        }
    }
    val quantities = remember { mutableStateMapOf<Int, Int>().apply { putAll(initial) } }
    var query by rememberSaveable { mutableStateOf("") }

    fun result(products: List<Merchandise>) =
        products.mapNotNull { p -> quantities[p.id]?.takeIf { it > 0 }?.let { PickedProduct(p, it) } }

    FullScreenDialog(
        title = stringResource(R.string.select_products),
        onDismiss = onDismiss,
        actions = {
            if (!singleChoice) {
                TextButton(onClick = { onDone(result(loader.data?.items.orEmpty())) }, enabled = loader.data != null) {
                    Text(stringResource(R.string.done))
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            SearchField(
                query,
                { query = it },
                stringResource(R.string.search_products),
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (date != null && enforceStock) {
                Text(
                    stringResource(R.string.availability_for, Format.mediumDate(date)),
                    Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LoadBox(loader) { stock ->
                val shown = remember(stock, query) {
                    val q = query.trim().lowercase()
                    stock.items.filter {
                        q.isEmpty() || it.name.lowercase().contains(q) ||
                            it.category.orEmpty().lowercase().contains(q) || it.code.orEmpty().lowercase().contains(q)
                    }
                }
                if (shown.isEmpty()) {
                    MessageView(Icons.Default.Inventory2, stringResource(R.string.no_products))
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(shown, key = { it.id }) { product ->
                            val available = product.available ?: product.quantity
                            val max = if (enforceStock) available else Int.MAX_VALUE
                            val soldOut = enforceStock && available <= 0
                            Row(
                                Modifier.fillMaxWidth()
                                    .clickable(enabled = singleChoice && !soldOut) {
                                        onDone(listOf(PickedProduct(product, 1)))
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ProductImage(product.thumbUrl, 48.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        product.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        if (soldOut) {
                                            stringResource(R.string.sold_out)
                                        } else {
                                            stringResource(R.string.n_available, available) +
                                                (product.price?.let { " · ${money(it)}" } ?: "")
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (soldOut) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                }
                                if (!singleChoice) {
                                    QuantityStepper(
                                        value = quantities[product.id] ?: 0,
                                        onChange = { quantities[product.id] = it },
                                        max = max.coerceAtLeast(quantities[product.id] ?: 0),
                                    )
                                }
                            }
                        }
                        item { Box(Modifier.height(88.dp)) }
                    }
                }
            }
        }
        val count = quantities.values.count { it > 0 }
        if (!singleChoice && count > 0) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.BottomCenter) {
                Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { onDone(result(loader.data?.items.orEmpty())) },
                        modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
                    ) { Text(stringResource(R.string.add_n_products, count)) }
                }
            }
        }
    }
}
