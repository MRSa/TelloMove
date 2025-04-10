package jp.osdn.gokigen.tellomove.ui.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import jp.osdn.gokigen.tellomove.R
import jp.osdn.gokigen.tellomove.ui.model.MainViewModel


@Composable
fun MainScreen(navController: NavHostController, viewModel: MainViewModel)
{
    val scrollState = rememberScrollState()
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.weight(1.0f))
            ControlPadPanel(viewModel)
            Spacer(modifier = Modifier.padding((4.dp)))
            SetParameterLayout(viewModel)
            HorizontalDivider(thickness = 1.dp)
            BottomCommandPanel(navController, viewModel)
            HorizontalDivider(thickness = 1.dp)
        }
    }
}

@Composable
fun BottomCommandPanel(navController: NavHostController, viewModel: MainViewModel)
{
    Row()
    {
        IconButton(
            enabled = true,
            modifier = Modifier,
            onClick = {
                navController.navigate("PreferenceScreen")
            }
        ) {
            Icon(
                painter = painterResource(R.drawable.baseline_settings_24),
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = "transit to preference screen",
            )

        }
        Text(
            text = stringResource(id = R.string.label_switch_preference_screen),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .clickable {
                    navController.navigate("PreferenceScreen")
                }
        )
    }
}
