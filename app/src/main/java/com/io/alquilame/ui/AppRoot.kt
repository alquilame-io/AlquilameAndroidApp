package com.io.alquilame.ui

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.io.alquilame.R
import com.io.alquilame.data.AgendaItem
import com.io.alquilame.ui.common.LocalBackend
import com.io.alquilame.ui.common.LocalSnackbar
import com.io.alquilame.ui.screens.CalendarScreen
import com.io.alquilame.ui.screens.ClientDetailScreen
import com.io.alquilame.ui.screens.ClientEditScreen
import com.io.alquilame.ui.screens.ClientsScreen
import com.io.alquilame.ui.screens.ContractDetailScreen
import com.io.alquilame.ui.screens.ContractsScreen
import com.io.alquilame.ui.screens.GroupDetailScreen
import com.io.alquilame.ui.screens.HomeScreen
import com.io.alquilame.ui.screens.InventoryScreen
import com.io.alquilame.ui.screens.LoginScreen
import com.io.alquilame.ui.screens.MerchandiseEditScreen
import com.io.alquilame.ui.screens.MoreScreen
import com.io.alquilame.ui.screens.PaymentLinksScreen
import com.io.alquilame.ui.screens.QuoteScreen
import com.io.alquilame.ui.screens.QuotesScreen
import com.io.alquilame.ui.screens.ReportsScreen
import com.io.alquilame.ui.screens.ReservationDetailScreen
import com.io.alquilame.ui.screens.ReservationEditorScreen
import com.io.alquilame.ui.screens.WebAdminScreen

/** Typed wrapper around the nav controller so screens never build route strings. */
class Nav(private val controller: NavHostController) {
    fun back() {
        controller.popBackStack()
    }

    private fun go(route: String) = controller.navigate(route) { launchSingleTop = true }

    /** Navigates to [route] replacing the current screen (e.g. after creating a record). */
    private fun swap(route: String) {
        val current = controller.currentDestination?.id
        controller.navigate(route) { if (current != null) popUpTo(current) { inclusive = true } }
    }

    fun tab(route: String) = controller.navigate(route) {
        popUpTo(controller.graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    fun agendaItem(item: AgendaItem) = when {
        item.groupId != null -> group(item.groupId)
        item.reservationId != null -> reservation(item.reservationId)
        else -> Unit
    }

    fun newReservation(date: String? = null, clientId: Int? = null, clientName: String? = null) = go(
        "reservation/new?date=${date.orEmpty()}&clientId=${clientId ?: -1}&clientName=${Uri.encode(clientName.orEmpty())}"
    )

    fun reservation(id: Int, replace: Boolean = false) = "reservation/$id".let { if (replace) swap(it) else go(it) }
    fun group(id: Int, replace: Boolean = false) = "group/$id".let { if (replace) swap(it) else go(it) }
    fun newMerchandise() = go("merchandise/-1")
    fun merchandise(id: Int) = go("merchandise/$id")
    fun clients() = go("clients")
    fun client(id: Int, replace: Boolean = false) = "client/$id".let { if (replace) swap(it) else go(it) }
    fun newClient() = go("client/-1/edit")
    fun editClient(id: Int) = go("client/$id/edit")
    fun contract(id: Int, replace: Boolean = false) = "contract/$id".let { if (replace) swap(it) else go(it) }
    fun quotes() = go("quotes")
    fun newQuote() = go("quote/-1")
    fun quote(id: Int) = go("quote/$id")
    fun paymentLinks() = go("payment-links")
    fun reports() = go("reports")
    fun web(path: String, title: String) = go("web?path=${Uri.encode(path)}&title=${Uri.encode(title)}")
}

private data class Tab(val route: String, val label: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", R.string.tab_agenda, Icons.Default.Home),
    Tab("calendar", R.string.tab_calendar, Icons.Default.CalendarMonth),
    Tab("inventory", R.string.tab_inventory, Icons.Default.Inventory2),
    Tab("contracts", R.string.tab_contracts, Icons.Default.Description),
    Tab("more", R.string.tab_more, Icons.Default.Menu),
)

@Composable
fun BottomBar(nav: Nav, current: String) {
    NavigationBar {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = tab.route == current,
                onClick = { if (tab.route != current) nav.tab(tab.route) },
                icon = { Icon(tab.icon, null) },
                label = { Text(stringResource(tab.label), maxLines = 1) },
            )
        }
    }
}

@Composable
fun AppRoot() {
    val backend = LocalBackend.current
    val snackbar = remember { SnackbarHostState() }
    CompositionLocalProvider(LocalSnackbar provides snackbar) {
        if (backend.session.tenant == null) {
            LoginScreen()
        } else {
            // Refresh plan, currency and Stripe status once per launch.
            LaunchedEffect(Unit) {
                runCatching { backend.call { me() } }.onSuccess { backend.session.updateTenant(it) }
            }
            MainNav()
        }
    }
}

@Composable
private fun MainNav() {
    val controller = rememberNavController()
    val nav = remember(controller) { Nav(controller) }
    val id = listOf(navArgument("id") { type = NavType.IntType })

    NavHost(controller, startDestination = "home") {
        composable("home") { HomeScreen(nav) }
        composable("calendar") { CalendarScreen(nav) }
        composable("inventory") { InventoryScreen(nav) }
        composable("contracts") { ContractsScreen(nav) }
        composable("more") { MoreScreen(nav) }

        composable(
            "reservation/new?date={date}&clientId={clientId}&clientName={clientName}",
            arguments = listOf(
                navArgument("date") { defaultValue = "" },
                navArgument("clientId") { type = NavType.IntType; defaultValue = -1 },
                navArgument("clientName") { defaultValue = "" },
            ),
        ) { entry ->
            val args = entry.arguments
            ReservationEditorScreen(
                nav = nav,
                initialDate = args?.getString("date").orEmpty(),
                clientId = args?.getInt("clientId")?.takeIf { it > 0 },
                clientName = args?.getString("clientName").orEmpty(),
            )
        }
        composable("reservation/{id}", id) { ReservationDetailScreen(nav, it.arguments!!.getInt("id")) }
        composable("group/{id}", id) { GroupDetailScreen(nav, it.arguments!!.getInt("id")) }
        composable("merchandise/{id}", id) {
            MerchandiseEditScreen(nav, it.arguments!!.getInt("id").takeIf { id -> id > 0 })
        }
        composable("clients") { ClientsScreen(nav) }
        composable("client/{id}", id) { ClientDetailScreen(nav, it.arguments!!.getInt("id")) }
        composable("client/{id}/edit", id) {
            ClientEditScreen(nav, it.arguments!!.getInt("id").takeIf { id -> id > 0 })
        }
        composable("contract/{id}", id) { ContractDetailScreen(nav, it.arguments!!.getInt("id")) }
        composable("quotes") { QuotesScreen(nav) }
        composable("quote/{id}", id) { QuoteScreen(nav, it.arguments!!.getInt("id").takeIf { id -> id > 0 }) }
        composable("payment-links") { PaymentLinksScreen(nav) }
        composable("reports") { ReportsScreen(nav) }
        composable(
            "web?path={path}&title={title}",
            arguments = listOf(navArgument("path") { defaultValue = "" }, navArgument("title") { defaultValue = "" }),
        ) {
            WebAdminScreen(nav, it.arguments?.getString("path").orEmpty(), it.arguments?.getString("title").orEmpty())
        }
    }
}
