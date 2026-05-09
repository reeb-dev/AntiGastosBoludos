package com.antigastos.boludos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.antigastos.boludos.ui.MainViewModel
import com.antigastos.boludos.ui.editor.ExpenseEditorScreen
import com.antigastos.boludos.ui.editor.ExpenseEditorViewModel
import com.antigastos.boludos.ui.goals.GoalsScreen
import com.antigastos.boludos.ui.goals.GoalsViewModel
import com.antigastos.boludos.ui.home.HomeScreen
import com.antigastos.boludos.ui.home.HomeViewModel
import com.antigastos.boludos.ui.list.ExpenseListScreen
import com.antigastos.boludos.ui.list.ExpenseListViewModel
import com.antigastos.boludos.ui.navigation.Routes
import com.antigastos.boludos.ui.stats.StatsScreen
import com.antigastos.boludos.ui.stats.StatsViewModel
import com.antigastos.boludos.ui.theme.AntiGastosTheme

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AntiGastosTheme {
                AntiGastosApp(mainViewModel = mainViewModel)
            }
        }
    }
}

@Composable
fun AntiGastosApp(mainViewModel: MainViewModel) {
    val app = LocalContext.current.applicationContext as AntiGastosApplication
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute != Routes.ADD && currentRoute?.startsWith("edit/") != true
    val showFab = currentRoute == Routes.HOME

    Scaffold(
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(
                    onClick = { navController.navigate(Routes.ADD) },
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar gasto")
                }
            }
        },
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(navController = navController)
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.HOME) {
                val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(app, mainViewModel))
                HomeScreen(
                    viewModel = vm,
                    mainViewModel = mainViewModel,
                    onOpenList = {
                        navController.navigate(Routes.LIST) {
                            launchSingleTop = true
                        }
                    },
                    onOpenStats = {
                        navController.navigate(Routes.STATS) {
                            launchSingleTop = true
                        }
                    },
                    onOpenGoals = {
                        navController.navigate(Routes.GOALS) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.LIST) {
                val vm: ExpenseListViewModel = viewModel(factory = ExpenseListViewModel.factory(app, mainViewModel))
                ExpenseListScreen(
                    viewModel = vm,
                    mainViewModel = mainViewModel,
                    onEdit = { id -> navController.navigate(Routes.edit(id)) },
                )
            }
            composable(Routes.STATS) {
                val vm: StatsViewModel = viewModel(factory = StatsViewModel.factory(app, mainViewModel))
                StatsScreen(viewModel = vm, mainViewModel = mainViewModel)
            }
            composable(Routes.GOALS) {
                val vm: GoalsViewModel = viewModel(factory = GoalsViewModel.factory(app, mainViewModel))
                GoalsScreen(viewModel = vm, mainViewModel = mainViewModel)
            }
            composable(Routes.ADD) {
                val vm: ExpenseEditorViewModel = viewModel(
                    key = "add",
                    factory = ExpenseEditorViewModel.factory(app, null),
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
                    factory = ExpenseEditorViewModel.factory(app, id),
                )
                ExpenseEditorScreen(
                    viewModel = vm,
                    title = "Editar gasto",
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(navController: NavController) {
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        val items = listOf(
            Triple(Routes.HOME, "Inicio", Icons.Default.Home),
            Triple(Routes.LIST, "Lista", Icons.Default.FormatListBulleted),
            Triple(Routes.STATS, "Stats", Icons.Default.BarChart),
            Triple(Routes.GOALS, "Metas", Icons.Default.Flag),
        )

        items.forEach { (route, label, icon) ->
            NavigationBarItem(
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
                selected = currentDestination?.hierarchy?.any { it.route == route } == true,
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
