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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.osdn.gokigen.tellomove.AppSingleton
import jp.osdn.gokigen.tellomove.R
import jp.osdn.gokigen.tellomove.communication.ICommandResult
import jp.osdn.gokigen.tellomove.communication.TelloCommandCallback
import jp.osdn.gokigen.tellomove.communication.TelloConnectionCallback
import jp.osdn.gokigen.tellomove.ui.model.MainViewModel

@Composable
fun ControlPadPanel(viewModel: MainViewModel)
{
    val informationMessage = viewModel.informationMessage.observeAsState()
    val isConnected = viewModel.isTelloConnected.observeAsState()
    val isVideoOn = viewModel.isVideoStreamOn.observeAsState()
    val batteryPercentage = viewModel.batteryPercent.observeAsState()
    val connectedStringId = if (isConnected.value == true) { R.string.label_connected } else { R.string.label_disconnected }
    val connectedIconId = if (isConnected.value == true) { R.drawable.baseline_import_export_24 } else { R.drawable.baseline_mobiledata_off_24 }
    val battery = batteryPercentage.value ?: -1
    val batteryIconId = if (battery > 90) {
        R.drawable.baseline_battery_full_24
    } else if (battery > 80) {
        R.drawable.baseline_battery_6_bar_24
    } else if (battery > 70) {
        R.drawable.baseline_battery_5_bar_24
    } else if (battery > 60) {
        R.drawable.baseline_battery_4_bar_24
    } else if (battery > 50) {
        R.drawable.baseline_battery_3_bar_24
    } else if (battery > 40) {
        R.drawable.baseline_battery_2_bar_24
    } else if (battery > 30) {
        R.drawable.baseline_battery_1_bar_24
    } else if (battery > 20) {
        R.drawable.baseline_battery_0_bar_24
    } else {
        R.drawable.baseline_battery_alert_24
    }
    val videoStreamId = if (isVideoOn.value == true) { R.drawable.baseline_videocam_24 } else { R.drawable.baseline_videocam_off_24 }

    val connectionCallback = TelloConnectionCallback(viewModel)
    val commandCallback = TelloCommandCallback(viewModel)

    Column()
    {
        HorizontalDivider(thickness = 1.dp)
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
            IconButton(
                enabled = false,
                onClick = {
                    AppSingleton.starter.start()
                    AppSingleton.publisher.enqueueCommand("command", connectionCallback)
                }
            ) {
                Icon(
                    modifier = Modifier.alpha(if (false) 1f else 0f),
                    painter = painterResource(R.drawable.baseline_videogame_asset_24),
                    //tint = MaterialTheme.colorScheme.primary,
                    contentDescription = "Connection"
                )
            }
            IconButton(
                enabled = false,
                onClick = {
                    AppSingleton.starter.start()
                    AppSingleton.publisher.enqueueCommand("command", connectionCallback)
                }
            ) {
                Icon(
                    modifier = Modifier.alpha(if (false) 1f else 0f),
                    painter = painterResource(R.drawable.baseline_videogame_asset_24),
                    //tint = MaterialTheme.colorScheme.primary,
                    contentDescription = "Connection"
                )
            }
            IconButton(
                enabled = (isConnected.value == true),
                onClick = {
                    val command = if (isVideoOn.value == true) {
                        "streamoff"
                    } else {
                        "streamon"
                    }
                    AppSingleton.starter.start()
                    AppSingleton.publisher.enqueueCommand(command, commandCallback)
                }
            ) {
                Icon(
                    painter = painterResource(videoStreamId),
                    contentDescription = "video stream"
                )
            }
            IconButton(
                enabled = false,
                onClick = { }
            ) {
                Icon(
                    painter = painterResource(batteryIconId),
                    contentDescription = "Battery"
                )
            }
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
            ControlPadButton(viewModel, R.drawable.baseline_undo_24, true, "ccw ${viewModel.moveDegree.value}", commandCallback)
            ControlPadButton(viewModel, R.drawable.baseline_view_compact_24, false)
            ControlPadButton(viewModel, R.drawable.baseline_redo_24, true, "cw ${viewModel.moveDegree.value}", commandCallback)
            ControlPadButton(viewModel, R.drawable.baseline_view_compact_24, false)
            ControlPadButton(viewModel, R.drawable.baseline_keyboard_arrow_left_24, true, "left ${viewModel.moveDistanceCm.value}", commandCallback)
            ControlPadButton(viewModel, R.drawable.baseline_view_compact_24, false)
            ControlPadButton(viewModel, R.drawable.baseline_keyboard_arrow_right_24, true, "right ${viewModel.moveDistanceCm.value}", commandCallback)
            ControlPadButton(viewModel, R.drawable.baseline_indeterminate_check_box_24, true, "stop", commandCallback)
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
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.padding((4.dp)))
            Text(
                text = informationMessage.value ?: "",
                modifier = Modifier.padding(start = 6.dp),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp
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
