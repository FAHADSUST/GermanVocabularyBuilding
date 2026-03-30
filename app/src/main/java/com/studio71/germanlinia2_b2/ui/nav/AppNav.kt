package com.studio71.germanlinia2_b2.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.studio71.germanlinia2_b2.data.repo.VocabularyRepository
import com.studio71.germanlinia2_b2.data.settings.SettingsStore
import com.studio71.germanlinia2_b2.ui.card.WordCardScreen
import com.studio71.germanlinia2_b2.ui.card.WordCardViewModel
import com.studio71.germanlinia2_b2.ui.list.VocabularyListScreen
import com.studio71.germanlinia2_b2.ui.list.VocabularyListViewModel
import com.studio71.germanlinia2_b2.ui.review.ReviewScreen
import com.studio71.germanlinia2_b2.ui.review.ReviewViewModel
import com.studio71.germanlinia2_b2.ui.settings.SettingsScreen
import com.studio71.germanlinia2_b2.ui.stats.StatsScreen
import com.studio71.germanlinia2_b2.ui.stats.StatsViewModel

object Routes {
    const val LIST = "list"
    const val CARD = "card/{wordId}"
    const val STATS = "stats"
    const val REVIEW = "review"
    const val SETTINGS = "settings"
    fun card(wordId: String) = "card/$wordId"
}

@Composable
fun AppNav(repository: VocabularyRepository, settings: SettingsStore) {
    val navController = rememberNavController()
    val appSettings by settings.state.collectAsStateWithLifecycle()

    NavHost(navController = navController, startDestination = Routes.LIST) {

        composable(Routes.LIST) {
            val vm: VocabularyListViewModel =
                viewModel(factory = VocabularyListViewModel.Factory(repository))
            VocabularyListScreen(
                viewModel = vm,
                onOpenCard = { wordId -> navController.navigate(Routes.card(wordId)) },
                onOpenStats = { navController.navigate(Routes.STATS) },
                onStartReview = { navController.navigate(Routes.REVIEW) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
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

        composable(Routes.REVIEW) {
            val vm: ReviewViewModel = viewModel(factory = ReviewViewModel.Factory(repository))
            ReviewScreen(
                viewModel = vm,
                autoSpeakOnReveal = appSettings.autoSpeakOnReveal,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(settings = settings, onBack = { navController.popBackStack() })
        }
    }
}

