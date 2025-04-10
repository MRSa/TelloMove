package jp.osdn.gokigen.tellomove.ui.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import jp.osdn.gokigen.tellomove.AppSingleton
import jp.osdn.gokigen.tellomove.R
import jp.osdn.gokigen.tellomove.communication.ICommandResult
import jp.osdn.gokigen.tellomove.communication.TelloCommandCallback
import jp.osdn.gokigen.tellomove.communication.TelloConnectionCallback
import jp.osdn.gokigen.tellomove.ui.model.MainViewModel

@Composable
fun ControlPadPanel(viewModel: MainViewModel)
{
    val statusMessage = viewModel.statusMessage.observeAsState()
    val isConnected = viewModel.isTelloConnected.observeAsState()
    val connectedStringId = if (isConnected.value == true) { R.string.label_connected } else { R.string.label_disconnected }
    val connectedIconId = if (isConnected.value == true) { R.drawable.baseline_import_export_24 } else { R.drawable.baseline_mobiledata_off_24 }
    val connectionCallback = TelloConnectionCallback(viewModel)
    val commandCallback = TelloCommandCallback(viewModel)

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
                    AppSingleton.publisher.enqueueCommand("command", connectionCallback)
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
                        AppSingleton.publisher.enqueueCommand("command", connectionCallback)
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
            ControlPadButton(viewModel, R.drawable.baseline_expand_less_24, true, "forward ${viewModel.moveDistanceCm.value}", commandCallback)
            ControlPadButton(viewModel, R.drawable.baseline_view_compact_24, false)
            ControlPadButton(viewModel, R.drawable.baseline_view_compact_24, false)
            ControlPadButton(viewModel, R.drawable.baseline_view_compact_24, false)
            ControlPadButton(viewModel, R.drawable.baseline_upload_24, true, "up ${viewModel.moveDistanceCm.value}", commandCallback)
            ControlPadButton(viewModel, R.drawable.baseline_view_compact_24, false)
            ControlPadButton(viewModel, R.drawable.baseline_flight_takeoff_24, true, "takeoff", commandCallback)
            Spacer(modifier = Modifier.weight(1.0f))
        }
        Spacer(modifier = Modifier.weight(1.0f))
        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth()
        )
        {
            ControlPadButton(viewModel, R.drawable.baseline_keyboard_arrow_left_24, true, "left ${viewModel.moveDistanceCm.value}", commandCallback)
            ControlPadButton(viewModel, R.drawable.baseline_view_compact_24, false)
            ControlPadButton(viewModel, R.drawable.baseline_keyboard_arrow_right_24, true, "right ${viewModel.moveDistanceCm.value}", commandCallback)
            ControlPadButton(viewModel, R.drawable.baseline_view_compact_24, false)
            ControlPadButton(viewModel, R.drawable.baseline_undo_24, true, "ccw ${viewModel.moveDegree.value}", commandCallback)
            ControlPadButton(viewModel, R.drawable.baseline_view_compact_24, false)
            ControlPadButton(viewModel, R.drawable.baseline_redo_24, true, "cw ${viewModel.moveDegree.value}", commandCallback)
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
            ControlPadButton(viewModel, R.drawable.baseline_expand_more_24, true, "back ${viewModel.moveDistanceCm.value}", commandCallback)
            ControlPadButton(viewModel, R.drawable.baseline_view_compact_24, false)
            ControlPadButton(viewModel, R.drawable.baseline_view_compact_24, false)
            ControlPadButton(viewModel, R.drawable.baseline_view_compact_24, false)
            ControlPadButton(viewModel, R.drawable.baseline_download_24, true, "down ${viewModel.moveDistanceCm.value}", commandCallback)
            ControlPadButton(viewModel, R.drawable.baseline_view_compact_24, false)
            ControlPadButton(viewModel, R.drawable.baseline_flight_land_24, true, "land", commandCallback)
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
fun ControlPadButton(viewModel: MainViewModel, iconId: Int, isVisible: Boolean, command: String = "", callback : ICommandResult? = null)
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