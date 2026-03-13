package com.studio71.germanlinia2_b2.ui.nav

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.studio71.germanlinia2_b2.data.repo.VocabularyRepository
import com.studio71.germanlinia2_b2.ui.card.WordCardScreen
import com.studio71.germanlinia2_b2.ui.card.WordCardViewModel
import com.studio71.germanlinia2_b2.ui.list.VocabularyListScreen
import com.studio71.germanlinia2_b2.ui.list.VocabularyListViewModel
import com.studio71.germanlinia2_b2.ui.stats.StatsScreen
import com.studio71.germanlinia2_b2.ui.stats.StatsViewModel

object Routes {
    const val LIST = "list"
    const val CARD = "card/{wordId}"
    const val STATS = "stats"
    fun card(wordId: String) = "card/$wordId"
}

@Composable
fun AppNav(repository: VocabularyRepository) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.LIST) {

        composable(Routes.LIST) {
            val vm: VocabularyListViewModel =
                viewModel(factory = VocabularyListViewModel.Factory(repository))
            VocabularyListScreen(
                viewModel = vm,
                onOpenCard = { wordId -> navController.navigate(Routes.card(wordId)) },
                onOpenStats = { navController.navigate(Routes.STATS) }
            )
        }

        composable(
            route = Routes.CARD,
            arguments = listOf(navArgument("wordId") { type = NavType.StringType })
        ) { backStackEntry ->
            val wordId = backStackEntry.arguments?.getString("wordId").orEmpty()
            val vm: WordCardViewModel = viewModel(
                key = wordId,
                factory = WordCardViewModel.Factory(repository, wordId)
            )
            WordCardScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }

        composable(Routes.STATS) {
            val vm: StatsViewModel = viewModel(factory = StatsViewModel.Factory(repository))
            StatsScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
    }
}

