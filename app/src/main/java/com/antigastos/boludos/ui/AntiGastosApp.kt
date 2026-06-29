package com.antigastos.boludos.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.data.local.entity.SettingsEntity
import com.antigastos.boludos.ui.achievements.AchievementUnlockOverlay
import com.antigastos.boludos.ui.achievements.AchievementsScreen
import com.antigastos.boludos.ui.achievements.LegendaryDropOverlay
import com.antigastos.boludos.ui.bolsillo.BolsilloScreen
import com.antigastos.boludos.ui.bolsillo.BolsilloViewModel
import com.antigastos.boludos.ui.categories.CategoriesScreen
import com.antigastos.boludos.ui.categories.CategoriesViewModel
import com.antigastos.boludos.ui.crush.CrushResolveScreen
import com.antigastos.boludos.ui.donate.DonateScreen
import com.antigastos.boludos.ui.editor.ExpenseEditorScreen
import com.antigastos.boludos.ui.editor.ExpenseEditorViewModel
import com.antigastos.boludos.ui.goals.GoalsScreen
import com.antigastos.boludos.ui.goals.GoalsViewModel
import com.antigastos.boludos.ui.home.HomeScreen
import com.antigastos.boludos.ui.home.HomeViewModel
import com.antigastos.boludos.ui.list.ExpenseListScreen
import com.antigastos.boludos.ui.list.ExpenseListViewModel
import com.antigastos.boludos.ui.loteria.LoteriaScreen
import com.antigastos.boludos.ui.navigation.Routes
import com.antigastos.boludos.ui.pact.PactScreen
import com.antigastos.boludos.ui.pact.PactViewModel
import com.antigastos.boludos.ui.personas.PersonaChatScreen
import com.antigastos.boludos.ui.personas.PersonaChatViewModel
import com.antigastos.boludos.ui.personas.PersonaListScreen
import com.antigastos.boludos.ui.quiz.PersonaQuizScreen
import com.antigastos.boludos.ui.recap.RecapScreen
import com.antigastos.boludos.ui.ruleta.RuletaScreen
import com.antigastos.boludos.ui.settings.PrivacyPolicyScreen
import com.antigastos.boludos.ui.settings.SettingsScreen
import com.antigastos.boludos.ui.settings.SettingsViewModel
import com.antigastos.boludos.ui.stats.StatsScreen
import com.antigastos.boludos.ui.stats.StatsViewModel
import com.antigastos.boludos.ui.subscriptions.SubscriptionsScreen
import com.antigastos.boludos.ui.subscriptions.SubscriptionsViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Shell de la app: TopBar + FAB + bottom nav + NavHost. Toda la
 * navegación principal vive acá; las pantallas se inyectan vía
 * `composable(Routes.X) { ... }`.
 *
 * Antes vivía dentro de `MainActivity.kt` y eso hacía muy pesado
 * abrir el archivo. Ahora `MainActivity` queda solo con lifecycle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AntiGastosApp(
    mainViewModel: MainViewModel,
    displayName: String,
) {
    val app = LocalContext.current.applicationContext as AntiGastosApplication
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val pendingRoute by mainViewModel.pendingRoute.collectAsState()
    LaunchedEffect(pendingRoute) {
        val route = pendingRoute ?: return@LaunchedEffect
        navController.navigate(route) {
            launchSingleTop = true
        }
        mainViewModel.consumePendingRoute()
    }

    val isMainRoute = currentRoute in Routes.mainBarRoutes ||
        currentRoute == Routes.GOALS ||
        currentRoute == Routes.STATS ||
        currentRoute == Routes.ACHIEVEMENTS ||
        currentRoute == Routes.DONATE
    // El "+" de cargar gasto vive en cualquier pantalla principal donde
    // tenga sentido cargar guita: Home, lista, bolsillo, ranking, trofeos
    // y stats. En la ruleta y el chat con personajes lo escondemos para
    // no tapar la animación / el teclado.
    val showFab = currentRoute in setOf(
        Routes.HOME,
        Routes.LIST,
        Routes.BOLSILLO,
        Routes.STATS,
        Routes.GOALS,
        Routes.ACHIEVEMENTS,
    )

    val today = remember { LocalDate.now() }
    val locale = remember { Locale("es", "AR") }
    val dateText = remember(today, locale) {
        val raw = today.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", locale))
        raw.replaceFirstChar { it.titlecase(locale) }
    }
    val sectionTitle = sectionTitleFor(currentRoute, displayName)

    val settings by app.settingsRepository.flow.collectAsState(initial = SettingsEntity())
    val headAchievement by mainViewModel.currentAchievement.collectAsState()
    val pendingExtras by mainViewModel.pendingExtras.collectAsState()
    val legendary by mainViewModel.legendaryDropVisible.collectAsState()
    val toastsHost = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        mainViewModel.toasts.collect { toastsHost.showSnackbar(it) }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                if (isMainRoute) {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    sectionTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    dateText,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { navController.navigate(Routes.ACHIEVEMENTS) }) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = "Trofeos")
                            }
                            IconButton(onClick = { navController.navigate(Routes.STATS) }) {
                                Icon(Icons.Default.BarChart, contentDescription = "Estadísticas")
                            }
                            IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                                Icon(Icons.Default.Settings, contentDescription = "Ajustes")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            titleContentColor = MaterialTheme.colorScheme.onPrimary,
                            actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
            },
            floatingActionButton = {
                if (showFab) {
                    ExtendedFloatingActionButton(
                        onClick = { navController.navigate(Routes.ADD) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("Agregar gasto") },
                        modifier = Modifier.testTag("fab_add_expense"),
                    )
                }
            },
            bottomBar = {
                if (isMainRoute) {
                    BottomNavigationBar(navController = navController)
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier
                    .padding(padding)
                    .imePadding(),
            ) {
                composable(Routes.HOME) {
                    val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(app, mainViewModel))
                    HomeScreen(
                        viewModel = vm,
                        mainViewModel = mainViewModel,
                        onOpenRecap = { navController.navigate(Routes.RECAP) },
                        onQuickAdd = { navController.navigate(Routes.ADD) },
                        onOpenAchievements = { navController.navigate(Routes.ACHIEVEMENTS) },
                        onOpenRuleta = { navController.navigate(Routes.RULETA) },
                        onOpenQuiz = { navController.navigate(Routes.PERSONA_QUIZ) },
                        onOpenLoteria = { navController.navigate(Routes.LOTERIA) },
                        onOpenSubscriptions = { navController.navigate(Routes.SUBSCRIPTIONS) },
                    )
                }
                composable(Routes.LIST) {
                    val vm: ExpenseListViewModel = viewModel(factory = ExpenseListViewModel.factory(app, mainViewModel))
                    ExpenseListScreen(
                        viewModel = vm,
                        mainViewModel = mainViewModel,
                        onEdit = { id -> navController.navigate(Routes.edit(id)) },
                        onAdd = { navController.navigate(Routes.ADD) },
                    )
                }
                composable(Routes.STATS) {
                    val vm: StatsViewModel = viewModel(
                        factory = StatsViewModel.factory(app, mainViewModel),
                    )
                    StatsScreen(
                        viewModel = vm,
                        mainViewModel = mainViewModel,
                        onAdd = { navController.navigate(Routes.ADD) },
                    )
                }
                composable(Routes.GOALS) {
                    val vm: GoalsViewModel = viewModel(factory = GoalsViewModel.factory(app, mainViewModel))
                    GoalsScreen(viewModel = vm, mainViewModel = mainViewModel)
                }
                composable(Routes.BOLSILLO) {
                    val vm: BolsilloViewModel = viewModel(factory = BolsilloViewModel.factory(app, mainViewModel))
                    BolsilloScreen(
                        viewModel = vm,
                        mainViewModel = mainViewModel,
                        onAdd = { navController.navigate(Routes.ADD) },
                    )
                }
                composable(Routes.SETTINGS) {
                    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(app))
                    SettingsScreen(
                        viewModel = vm,
                        onDonate = { navController.navigate(Routes.DONATE) },
                        onOpenPrivacy = { navController.navigate(Routes.PRIVACY_POLICY) },
                        onOpenLoteria = { navController.navigate(Routes.LOTERIA) },
                        onOpenQuiz = { navController.navigate(Routes.PERSONA_QUIZ) },
                        onOpenRecap = { navController.navigate(Routes.RECAP) },
                        onOpenPact = { navController.navigate(Routes.PACT) },
                        onOpenCategories = { navController.navigate(Routes.CATEGORIES) },
                    )
                }
                composable(Routes.PRIVACY_POLICY) {
                    PrivacyPolicyScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.SUBSCRIPTIONS) {
                    val vm: SubscriptionsViewModel = viewModel(factory = SubscriptionsViewModel.factory(app))
                    val prefill by mainViewModel.subscriptionPrefill.collectAsState()
                    SubscriptionsScreen(
                        viewModel = vm,
                        prefill = prefill,
                        onConsumePrefill = { mainViewModel.consumeSubscriptionPrefill() },
                    )
                }
                composable(Routes.ACHIEVEMENTS) {
                    AchievementsScreen(app = app)
                }
                composable(Routes.DONATE) {
                    val donateVm: com.antigastos.boludos.ui.donate.DonateViewModel =
                        viewModel(factory = com.antigastos.boludos.ui.donate.DonateViewModel.factory(app))
                    val devSettings by app.settingsRepository.flow.collectAsState(
                        initial = com.antigastos.boludos.data.local.entity.SettingsEntity(),
                    )
                    com.antigastos.boludos.ui.donate.DonateScreen(
                        viewModel = donateVm,
                        showDeveloperEarnings = devSettings.developerMode,
                    )
                }
                composable(Routes.PACT) {
                    val vm: PactViewModel = viewModel(factory = PactViewModel.factory(app))
                    PactScreen(viewModel = vm)
                }
                composable(Routes.CATEGORIES) {
                    val vm: CategoriesViewModel = viewModel(factory = CategoriesViewModel.factory(app))
                    CategoriesScreen(viewModel = vm)
                }
                composable(Routes.RULETA) {
                    RuletaScreen()
                }
                composable(Routes.RECAP) {
                    RecapScreen(
                        mainViewModel = mainViewModel,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.PERSONA_QUIZ) {
                    PersonaQuizScreen()
                }
                composable(Routes.LOTERIA) {
                    LoteriaScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.PERSONAS) {
                    PersonaListScreen(
                        onOpen = { key -> navController.navigate(Routes.personaChat(key)) },
                    )
                }
                composable(
                    route = Routes.PERSONA_CHAT,
                    arguments = listOf(navArgument("key") { type = NavType.StringType }),
                ) { entry ->
                    val key = entry.arguments?.getString("key") ?: "termo"
                    val vm: PersonaChatViewModel = viewModel(
                        key = "personaChat_$key",
                        factory = PersonaChatViewModel.factory(app, key),
                    )
                    PersonaChatScreen(
                        viewModel = vm,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.ADD) {
                    val vm: ExpenseEditorViewModel = viewModel(
                        key = "add",
                        factory = ExpenseEditorViewModel.factory(app, null, mainViewModel),
                    )
                    ExpenseEditorScreen(
                        viewModel = vm,
                        title = "Nuevo gasto",
                        onBack = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() },
                    )
                }
                composable(
                    route = Routes.EDIT,
                    arguments = listOf(navArgument("expenseId") { type = NavType.LongType }),
                ) {
                    val id = it.arguments!!.getLong("expenseId")
                    val vm: ExpenseEditorViewModel = viewModel(
                        key = "edit_$id",
                        factory = ExpenseEditorViewModel.factory(app, id, mainViewModel),
                    )
                    ExpenseEditorScreen(
                        viewModel = vm,
                        title = "Editar gasto",
                        onBack = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() },
                    )
                }
                composable(
                    route = Routes.CRUSH_RESOLVE,
                    arguments = listOf(navArgument("crushId") { type = NavType.LongType }),
                ) { entry ->
                    val crushId = entry.arguments!!.getLong("crushId")
                    CrushResolveScreen(
                        crushId = crushId,
                        app = app,
                        mainViewModel = mainViewModel,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
            headAchievement?.let { meta ->
                AchievementUnlockOverlay(
                    meta = meta,
                    pendingExtras = pendingExtras,
                    soundEnabled = settings.soundEnabled,
                    onDismiss = { mainViewModel.dismissCurrentAchievement() },
                )
            }
            if (legendary) {
                LegendaryDropOverlay(
                    soundEnabled = settings.soundEnabled,
                    onDismiss = { mainViewModel.consumeLegendaryDrop() },
                )
            }
            SnackbarHost(
                hostState = toastsHost,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
            )
        }
    }
}

/**
 * Mapea cada `Route` al título visible que mostramos en la TopBar.
 */
private fun sectionTitleFor(route: String?, displayName: String): String = when (route) {
    Routes.HOME -> "Hola, $displayName"
    Routes.LIST -> "Historial"
    Routes.STATS -> "Estadísticas"
    Routes.GOALS -> "Metas del mes"
    Routes.BOLSILLO -> "Bolsillo"
    Routes.RULETA -> "Ruleta"
    Routes.SETTINGS -> "Ajustes"
    Routes.PRIVACY_POLICY -> "Privacidad"
    Routes.SUBSCRIPTIONS -> "Suscripciones"
    Routes.ACHIEVEMENTS -> "Trofeos"
    Routes.PACT -> "Modo pacto"
    Routes.CATEGORIES -> "Categorías"
    Routes.PERSONAS -> "Chatealos"
    Routes.DONATE -> "Doname un cafecito"
    Routes.RECAP -> "Recap del mes"
    Routes.PERSONA_QUIZ -> "Quiz de personaje"
    Routes.LOTERIA -> "Regalo del día"
    "crush/{crushId}" -> "Antojo 24 h"
    else -> "Anti-gastos boludos"
}

@Composable
private fun BottomNavigationBar(navController: NavController) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        val items = listOf(
            Triple(Routes.HOME, "Inicio", Icons.Default.Home),
            Triple(Routes.LIST, "Lista", Icons.AutoMirrored.Filled.FormatListBulleted),
            Triple(Routes.BOLSILLO, "Bolsillo", Icons.Default.AccountBalanceWallet),
            Triple(Routes.PERSONAS, "Chat", Icons.AutoMirrored.Filled.Chat),
            Triple(Routes.RULETA, "Ruleta", Icons.Default.Casino),
        )

        items.forEach { (route, label, icon) ->
            NavigationBarItem(
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
                selected = currentDestination?.hierarchy?.any { it.route == route } == true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                ),
                onClick = {
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
    }
}
