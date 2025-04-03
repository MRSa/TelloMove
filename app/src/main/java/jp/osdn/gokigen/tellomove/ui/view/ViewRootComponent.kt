package jp.osdn.gokigen.tellomove.ui.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import jp.osdn.gokigen.tellomove.ui.model.MainViewModel
import jp.osdn.gokigen.tellomove.ui.model.PreferenceViewModel
import jp.osdn.gokigen.tellomove.ui.theme.TelloMoveTheme

class ViewRootComponent @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : AbstractComposeView(context, attrs, defStyleAttr)
{
    private lateinit var myViewModel : MainViewModel
    private lateinit var myPreferenceViewModel : PreferenceViewModel

    fun prepare(
        viewModel: MainViewModel,
        prefsModel: PreferenceViewModel
    )
    {
        myViewModel = viewModel
        myPreferenceViewModel = prefsModel
        Log.v(TAG, " ...prepare...")
    }

    @Composable
    override fun Content()
    {
        val navController = rememberNavController()

        TelloMoveTheme {
            NavigationMain(navController, myViewModel, myPreferenceViewModel)
        }
        Log.v(TAG, " ...NavigationRootComponent...")
    }

    companion object
    {
        private val TAG = ViewRootComponent::class.java.simpleName
    }
}

@Composable
fun NavigationMain(navController: NavHostController,
                   mainViewModel: MainViewModel,
                   preferenceViewModel: PreferenceViewModel
)
{
    TelloMoveTheme {
        NavHost(
            modifier = Modifier.systemBarsPadding(),
            navController = navController,
            startDestination = "MainScreen"
        ) {
            composable("MainScreen") { MainScreen(navController = navController, viewModel = mainViewModel) }
            composable("PreferenceScreen") { PreferenceScreen(navController = navController, prefsModel = preferenceViewModel) }
        }
    }
}
