package jp.osdn.gokigen.tellomove.ui.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import jp.osdn.gokigen.tellomove.AppSingleton
import jp.osdn.gokigen.tellomove.R
import jp.osdn.gokigen.tellomove.communication.ICommandResult
import jp.osdn.gokigen.tellomove.communication.TelloConnectionCallback
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
    val statusMessage = viewModel.statusMessage.observeAsState()
    val isConnected = viewModel.isTelloConnected.observeAsState()
    val connectedStringId = if (isConnected.value == true) { R.string.label_connected } else { R.string.label_disconnected }
    val connectedIconId = if (isConnected.value == true) { R.drawable.baseline_import_export_24 } else { R.drawable.baseline_mobiledata_off_24 }
    val callback = TelloConnectionCallback(viewModel)

    Column()
    {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        )
        {
            IconButton(
                enabled = (isConnected.value == false),
                onClick = {
                    AppSingleton.starter.start()
                    AppSingleton.publisher.enqueueCommand("command", callback)
                }
            ) {
                Icon(
                    painter = painterResource(connectedIconId),
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = "Connection"
                )
            }
            Text(
                text = stringResource(connectedStringId),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .clickable {
                        AppSingleton.starter.start()
                        AppSingleton.publisher.enqueueCommand("command", callback)
                    }
            )
        }
        HorizontalDivider(thickness = 1.dp)
        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth()
        )
        {
            ControlPadButton(viewModel, R.drawable.baseline_view_compact_24, false)
            ControlPadButton(viewModel, R.drawable.baseline_expand_less_24, true)
            ControlPadButton(viewModel, R.drawable.baseline_view_compact_24, false)
            ControlPadButton(viewModel, R.drawable.baseline_view_compact_24, false)
            ControlPadButton(viewModel, R.drawable.baseline_view_compact_24, false)
            ControlPadButton(viewModel, R.drawable.baseline_upload_24, true)
            ControlPadButton(viewModel, R.drawable.baseline_view_compact_24, false)
            ControlPadButton(viewModel, R.drawable.baseline_flight_takeoff_24, true, "takeoff")
            Spacer(modifier = Modifier.weight(1.0f))
        }
        Spacer(modifier = Modifier.weight(1.0f))
        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth()
        )
        {
            ControlPadButton(viewModel, R.drawable.baseline_keyboard_arrow_left_24, true)
            ControlPadButton(viewModel, R.drawable.baseline_view_compact_24, false)
            ControlPadButton(viewModel, R.drawable.baseline_keyboard_arrow_right_24, true)
            ControlPadButton(viewModel, R.drawable.baseline_view_compact_24, false)
            ControlPadButton(viewModel, R.drawable.baseline_undo_24, true)
            ControlPadButton(viewModel, R.drawable.baseline_view_compact_24, false)
            ControlPadButton(viewModel, R.drawable.baseline_redo_24, true)
            ControlPadButton(viewModel, R.drawable.baseline_view_compact_24, false)
            Spacer(modifier = Modifier.weight(1.0f))
        }
        Spacer(modifier = Modifier.weight(1.0f))
        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth()
        )
        {
            ControlPadButton(viewModel, R.drawable.baseline_view_compact_24, false)
            ControlPadButton(viewModel, R.drawable.baseline_expand_more_24, true)
            ControlPadButton(viewModel, R.drawable.baseline_view_compact_24, false)
            ControlPadButton(viewModel, R.drawable.baseline_view_compact_24, false)
            ControlPadButton(viewModel, R.drawable.baseline_view_compact_24, false)
            ControlPadButton(viewModel, R.drawable.baseline_download_24, true)
            ControlPadButton(viewModel, R.drawable.baseline_view_compact_24, false)
            ControlPadButton(viewModel, R.drawable.baseline_flight_land_24, true, "land")
            Spacer(modifier = Modifier.weight(1.0f))
        }
        HorizontalDivider(thickness = 1.dp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        )
        {
            IconButton(
                enabled = (isConnected.value == true),
                onClick = { }
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_sensors_24),
                    contentDescription = "Get status Icon"
                )
            }
            Spacer(modifier = Modifier.padding((10.dp)))
            Text(
                text = stringResource(R.string.label_status),
                modifier = Modifier.padding(start = 6.dp),
            )
            Spacer(modifier = Modifier.padding((4.dp)))
            TextField(
                enabled = false,
                value = statusMessage.value ?: "",
                singleLine = true,
                onValueChange = { value -> viewModel.updateStatus(value) },
            )
        }
        HorizontalDivider(thickness = 1.dp)
    }
}

@Composable
fun ControlPadButton(viewModel: MainViewModel, iconId: Int, isVisible: Boolean, command: String = "", callback :ICommandResult? = null)
{
    val isConnected = viewModel.isTelloConnected.observeAsState()
    IconButton(
        enabled = (isConnected.value == true),
        modifier = Modifier.alpha(if (isVisible) 1f else 0f),
        onClick = { AppSingleton.publisher.enqueueCommand(command, callback) }
    ) {
        Icon(
            painter = painterResource(iconId),
            contentDescription = command
        )
    }
}

@Composable
fun TopCommandPanel(navController: NavHostController, viewModel: MainViewModel)
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
