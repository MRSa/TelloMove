package jp.osdn.gokigen.tellomove.ui.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            HorizontalDivider(thickness = 1.dp)
            TopCommandPanel(navController, viewModel)
            HorizontalDivider(thickness = 1.dp)
        }
    }
}

@Composable
fun TopCommandPanel(navController: NavHostController, viewModel: MainViewModel)
{
    val isRunning = viewModel.isTelloRunning.observeAsState()
    val buttonEnable = (isRunning.value != true)
    Row()
    {
        IconButton(
            enabled = buttonEnable,
            modifier = Modifier,
            onClick = {
                navController.navigate("PreferenceScreen")
            }
        ) {
            Icon(
                painter = painterResource(R.drawable.baseline_settings_24),
                contentDescription = "transit to preference screen")
        }
        //Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(id = R.string.label_switch_preference_screen),
            fontSize = 22.sp,
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
