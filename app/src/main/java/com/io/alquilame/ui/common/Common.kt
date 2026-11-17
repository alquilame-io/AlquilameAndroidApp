package com.io.alquilame.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.io.alquilame.R
import com.io.alquilame.data.ApiException
import com.io.alquilame.data.Backend
import com.io.alquilame.ui.theme.StatusColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

val LocalBackend = staticCompositionLocalOf<Backend> { error("Backend not provided") }
val LocalSnackbar = staticCompositionLocalOf { SnackbarHostState() }

/** Formats an API amount in the signed-in tenant's currency. */
@Composable
fun money(amount: String?): String =
    Format.money(amount, LocalBackend.current.session.tenant?.currency ?: "MXN")

// ---------------------------------------------------------------------------------------------
// Loading data
// ---------------------------------------------------------------------------------------------

sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Failed(val message: String) : Load<Nothing>
    data class Ready<T>(val data: T) : Load<T>
}

/**
 * Holds the result of one fetch across configuration changes. The screen
 * that owns it decides when to [reload]; a silent reload keeps showing the
 * current data while the new one arrives.
 */
class Loader<T>(private val backend: Backend, private val fetch: suspend Backend.() -> T) : ViewModel() {
    var state: Load<T> by mutableStateOf(Load.Loading)
        private set
    var refreshing by mutableStateOf(false)
        private set
    private var job: Job? = null
    private var loadedOnce = false

    val data: T? get() = (state as? Load.Ready<T>)?.data

    fun reload(silent: Boolean = false) {
        job?.cancel()
        if (!silent || state !is Load.Ready) state = Load.Loading
        job = viewModelScope.launch {
            try {
                state = Load.Ready(backend.fetch())
                loadedOnce = true
            } catch (e: ApiException) {
                // A failed background refresh keeps the data already on screen.
                if (state !is Load.Ready) state = Load.Failed(e.message ?: "")
            } finally {
                refreshing = false
            }
        }
    }

    fun refresh() {
        refreshing = true
        reload(silent = true)
    }

    /** Replaces the data with a fresher copy returned by a write call. */
    fun set(value: T) {
        state = Load.Ready(value)
    }

    internal fun onResume() = reload(silent = loadedOnce)
}

/**
 * Creates (or finds) the [Loader] for [key] and (re)loads it every time the
 * screen comes back to the foreground, so lists reflect edits made elsewhere.
 */
@Composable
fun <T> rememberLoader(key: String, fetch: suspend Backend.() -> T): Loader<T> {
    val backend = LocalBackend.current
    val loader: Loader<T> = viewModel(key = key) { Loader(backend, fetch) }
    LifecycleResumeEffect(loader) {
        loader.onResume()
        onPauseOrDispose { }
    }
    return loader
}

/** Loading / error / content switch with pull-to-refresh. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> LoadBox(loader: Loader<T>, modifier: Modifier = Modifier, content: @Composable (T) -> Unit) {
    when (val state = loader.state) {
        Load.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is Load.Failed -> MessageView(
            icon = Icons.Default.CloudOff,
            text = state.message,
            modifier = modifier,
            actionLabel = stringResource(R.string.retry),
            onAction = { loader.reload() },
        )
        is Load.Ready -> PullToRefreshBox(
            isRefreshing = loader.refreshing,
            onRefresh = { loader.refresh() },
            modifier = modifier.fillMaxSize(),
        ) { content(state.data) }
    }
}

/**
 * Runs write actions for a screen: tracks the busy flag and shows failures
 * in the snackbar.
 */
class Actions(
    private val scope: CoroutineScope,
    private val backend: Backend,
    private val snackbar: SnackbarHostState,
) {
    var busy by mutableStateOf(false)
        private set

    /** Message of the last failed action, for dialogs that cover the snackbar. */
    var error: String? by mutableStateOf(null)
        private set

    fun run(block: suspend Backend.() -> Unit) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try {
                backend.block()
            } catch (e: ApiException) {
                error = e.message
                busy = false
                snackbar.showSnackbar(e.message ?: "")
            } finally {
                busy = false
            }
        }
    }

    fun clearError() {
        error = null
    }

    fun notify(message: String) {
        scope.launch { snackbar.showSnackbar(message) }
    }
}

/** Inline error line for dialogs; starts clean every time the dialog opens. */
@Composable
fun ActionError(actions: Actions) {
    DisposableEffect(actions) {
        actions.clearError()
        onDispose { }
    }
    actions.error?.let {
        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun rememberActions(): Actions {
    val scope = rememberCoroutineScope()
    val backend = LocalBackend.current
    val snackbar = LocalSnackbar.current
    return remember { Actions(scope, backend, snackbar) }
}

// ---------------------------------------------------------------------------------------------
// Screen scaffolding
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen(
    title: String,
    onBack: (() -> Unit)? = null,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                        }
                    }
                },
                actions = actions,
            )
        },
        floatingActionButton = floatingActionButton,
        bottomBar = bottomBar,
        snackbarHost = { SnackbarHost(LocalSnackbar.current) },
        content = content,
    )
}

/** Scrollable form body with a pinned primary button that stays above the keyboard. */
@Composable
fun FormBody(
    padding: PaddingValues,
    saveLabel: String,
    onSave: () -> Unit,
    saving: Boolean,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.padding(padding).fillMaxSize().imePadding()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
        Surface(tonalElevation = 3.dp) {
            Button(
                onClick = onSave,
                enabled = enabled && !saving,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).height(52.dp),
            ) {
                if (saving) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(saveLabel)
                }
            }
        }
    }
}

@Composable
fun MessageView(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(12.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

@Composable
fun StatusChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 3.dp),
        color = color,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
    )
}

/** Localised label and colour of a contract status. */
@Composable
fun ContractStatusChip(status: String, modifier: Modifier = Modifier) {
    val (label, color) = when (status) {
        "active" -> stringResource(R.string.status_active) to StatusColors.info
        "completed" -> stringResource(R.string.status_completed) to StatusColors.success
        "cancelled" -> stringResource(R.string.status_cancelled) to MaterialTheme.colorScheme.error
        else -> stringResource(R.string.status_draft) to MaterialTheme.colorScheme.outline
    }
    StatusChip(label, color, modifier)
}

@Composable
fun KeyValueRow(label: String, value: String, strong: Boolean = false, valueColor: Color = Color.Unspecified) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            label,
            Modifier.weight(1f),
            style = if (strong) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = if (strong) Color.Unspecified else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = if (strong) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (strong) FontWeight.Bold else FontWeight.Normal,
            color = valueColor,
        )
    }
}

@Composable
fun IconLine(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun ProductImage(url: String?, size: Dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    if (url.isNullOrBlank()) {
        Box(
            modifier.size(size).clip(shape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Inventory2, null, tint = MaterialTheme.colorScheme.outline)
        }
    } else {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = modifier.size(size).clip(shape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
fun QuantityStepper(value: Int, onChange: (Int) -> Unit, min: Int = 0, max: Int = Int.MAX_VALUE) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(
            onClick = { onChange((value - 1).coerceAtLeast(min)) },
            enabled = value > min,
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
        ) { Icon(Icons.Default.Remove, stringResource(R.string.less)) }
        Text(
            value.toString(),
            Modifier.width(40.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
        )
        FilledTonalIconButton(
            onClick = { onChange((value + 1).coerceAtMost(max)) },
            enabled = value < max,
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
        ) { Icon(Icons.Default.Add, stringResource(R.string.more)) }
    }
}

@Composable
fun SearchField(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) { Icon(Icons.Default.Clear, stringResource(R.string.clear)) }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    )
}

@Composable
fun TextInput(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboard: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    singleLine: Boolean = true,
    prefix: String? = null,
    suffix: String? = null,
    enabled: Boolean = true,
    supporting: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        enabled = enabled,
        prefix = prefix?.let { { Text(it) } },
        suffix = suffix?.let { { Text(it) } },
        supportingText = supporting?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboard,
            capitalization = if (keyboard == KeyboardType.Text) capitalization else KeyboardCapitalization.None,
            imeAction = if (singleLine) ImeAction.Next else ImeAction.Default,
        ),
    )
}

@Composable
fun AmountInput(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    TextInput(
        label = label,
        value = value,
        onChange = { new -> onChange(new.filter { it.isDigit() || it == '.' || it == ',' }) },
        modifier = modifier,
        keyboard = KeyboardType.Decimal,
        prefix = "$",
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = { onDismiss(); onConfirm() }) {
                Text(confirmLabel, color = if (destructive) MaterialTheme.colorScheme.error else Color.Unspecified)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Tappable list row used for navigation targets and pickers. */
@Composable
fun NavRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing?.invoke()
    }
}
