package jp.osdn.gokigen.tellomove.ui.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
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
            Spacer(modifier = Modifier.weight(1.0f))
            ControlButtonsLayout(viewModel)
            Spacer(modifier = Modifier.weight(3.0f))
            HorizontalDivider(thickness = 1.dp)
            TopCommandPanel(navController, viewModel)
            HorizontalDivider(thickness = 1.dp)
        }
    }
}

@Composable
fun ControlButtonsLayout(viewModel: MainViewModel)
{
    val isConnected = viewModel.isTelloConnected.observeAsState()

    Column()
    {
        Row(
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        )
        {
            Text(
                text = stringResource(R.string.label_control),
                color = Color.Black, // MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.weight(1f)
            )
        }
        HorizontalDivider(thickness = 1.dp)
        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth()
        )
        {
            ControlPadButton(R.drawable.baseline_view_compact_24, false)
            ControlPadButton(R.drawable.baseline_expand_less_24, true)
            ControlPadButton(R.drawable.baseline_view_compact_24, false)
            ControlPadButton(R.drawable.baseline_view_compact_24, false)
            ControlPadButton(R.drawable.baseline_view_compact_24, false)
            ControlPadButton(R.drawable.baseline_upload_24, true)
            ControlPadButton(R.drawable.baseline_view_compact_24, false)
            ControlPadButton(R.drawable.baseline_flight_takeoff_24, true)
            Spacer(modifier = Modifier.weight(1.0f))
        }
        Spacer(modifier = Modifier.weight(1.0f))
        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth()
        )
        {
            ControlPadButton(R.drawable.baseline_keyboard_arrow_left_24, true)
            ControlPadButton(R.drawable.baseline_view_compact_24, false)
            ControlPadButton(R.drawable.baseline_keyboard_arrow_right_24, true)
            ControlPadButton(R.drawable.baseline_view_compact_24, false)
            ControlPadButton(R.drawable.baseline_undo_24, true)
            ControlPadButton(R.drawable.baseline_view_compact_24, false)
            ControlPadButton(R.drawable.baseline_redo_24, true)
            ControlPadButton(R.drawable.baseline_view_compact_24, false)
            Spacer(modifier = Modifier.weight(1.0f))
        }
        Spacer(modifier = Modifier.weight(1.0f))
        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth()
        )
        {
            ControlPadButton(R.drawable.baseline_view_compact_24, false)
            ControlPadButton(R.drawable.baseline_expand_more_24, true)
            ControlPadButton(R.drawable.baseline_view_compact_24, false)
            ControlPadButton(R.drawable.baseline_view_compact_24, false)
            ControlPadButton(R.drawable.baseline_view_compact_24, false)
            ControlPadButton(R.drawable.baseline_download_24, true)
            ControlPadButton(R.drawable.baseline_view_compact_24, false)
            ControlPadButton(R.drawable.baseline_flight_land_24, true)
            Spacer(modifier = Modifier.weight(1.0f))
        }
        HorizontalDivider(thickness = 1.dp)
        Row(
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        )
        {
            IconButton(
                enabled = (isConnected.value == false),
                onClick = { }
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_sensors_24),
                    contentDescription = ""
                )
            }
        }
        HorizontalDivider(thickness = 1.dp)
    }
}

@Composable
fun ControlPadButton(iconId: Int, isVisible: Boolean, command: String = "")
{
    IconButton(
        enabled = true,
        modifier = Modifier.alpha(if (isVisible) 1f else 0f),
        onClick = { }
    ) {
        Icon(
            painter = painterResource(iconId),
            contentDescription = ""
        )
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
