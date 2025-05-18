package jp.osdn.gokigen.tellomove.ui.view

import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import jp.osdn.gokigen.tellomove.R
import jp.osdn.gokigen.tellomove.ui.model.PreferenceViewModel
import kotlinx.coroutines.launch

@Composable
fun PreferenceScreen(navController: NavHostController, prefsModel: PreferenceViewModel)
{
    val padding = 6.dp

    MaterialTheme {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        )
        {
            //Spacer(Modifier.size(padding))
            HorizontalDivider(thickness = 1.dp)
            ReturnToMainScreen(navController)
            Spacer(Modifier.size(padding))
            HorizontalDivider(thickness = 1.dp)
            ShowWifiSettings()
            Spacer(Modifier.size(padding))
            HorizontalDivider(thickness = 1.dp)
            SwitchSetWatchdog(prefsModel)
            Spacer(Modifier.size(padding))
            SwitchSetSpeakCommand(prefsModel)
            Spacer(Modifier.size(padding))
            HorizontalDivider(thickness = 1.dp)
            OpenRecordedFileList(navController)
            Spacer(Modifier.size(padding))
            HorizontalDivider(thickness = 1.dp)
            ShowAboutGokigen()
            Spacer(Modifier.size(padding))
            HorizontalDivider(thickness = 1.dp)
            ShowGokigenPrivacyPolicy()
            Spacer(Modifier.size(padding))
            HorizontalDivider(thickness = 1.dp)
        }
    }
}

@Composable
fun ReturnToMainScreen(navController: NavHostController)
{
    val density = LocalDensity.current
    Spacer(Modifier.size(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(id = R.drawable.baseline_arrow_back_24),
            contentDescription = "Back",
            modifier = Modifier.clickable( onClick = {
                Log.v("BACK", "CURRENT SCREEN: ${navController.currentBackStackEntry?.destination?.route}")
                if (navController.currentBackStackEntry?.destination?.route == "PreferenceScreen") {
                    navController.popBackStack()
                }
            })
        )
        Text(text = stringResource(R.string.label_return_to_main_screen),
            fontSize = with(density) { 18.dp.toSp() },
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable( onClick = {
                Log.v("BACK", "CURRENT SCREEN: ${navController.currentBackStackEntry?.destination?.route}")
                if (navController.currentBackStackEntry?.destination?.route == "PreferenceScreen")
                {
                    navController.popBackStack()
                }
            })
        )
    }
}

@Composable
fun ShowWifiSettings()
{
    val context = LocalContext.current
    val density = LocalDensity.current
    Row(modifier = Modifier.padding(all = 8.dp)) {
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            painter = painterResource(id = R.drawable.baseline_wifi_24),
            tint = MaterialTheme.colorScheme.primary,
            contentDescription = "Wifi",
            modifier = Modifier.clickable( onClick = {
                context.startActivity(Intent(Intent(Settings.ACTION_WIFI_SETTINGS)))
            })
        )
        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = stringResource(R.string.pref_show_wifi_settings),
            color = MaterialTheme.colorScheme.primary,
            fontSize = with(density) { 18.dp.toSp() },
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .clickable {
                    context.startActivity(Intent(Intent(Settings.ACTION_WIFI_SETTINGS)))
                }
        )
    }
}

@Composable
fun SwitchSetWatchdog(prefsModel: PreferenceViewModel)
{
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val checkProductId = prefsModel.useWatchdog.observeAsState()
    Row (
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Switch(
            checked = checkProductId.value?: false,
            onCheckedChange = {
                scope.launch {
                    prefsModel.setUseWatchDog(!(checkProductId.value?: false))
                }
            })
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = stringResource(R.string.label_switch_watchdog),
            fontSize = with(density) { 18.dp.toSp() },
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable( onClick = {
                scope.launch { prefsModel.setUseWatchDog(!(checkProductId.value?: false)) }
            })
        )
    }
    Text(text = stringResource(R.string.description_switch_watchdog),
        color = MaterialTheme.colorScheme.secondary,
        fontSize = with(density) { 14.dp.toSp() },)
}

@Composable
fun SwitchSetSpeakCommand(prefsModel: PreferenceViewModel)
{
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val speakCommand = prefsModel.speakCommands.observeAsState()
    Row (
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Switch(
            checked = speakCommand.value?: false,
            onCheckedChange = {
                scope.launch {
                    prefsModel.setSpeakCommand(!(speakCommand.value?: false))
                }
            })
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = stringResource(R.string.pref_speak_command),
            fontSize = with(density) { 18.dp.toSp() },
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable( onClick = {
                scope.launch { prefsModel.setSpeakCommand(!(speakCommand.value?: false)) }
            })
        )
    }
    Text(text = stringResource(R.string.pref_detail_speak_command),
        color = MaterialTheme.colorScheme.secondary,
        fontSize = with(density) { 14.dp.toSp() },)
}

@Composable
fun OpenRecordedFileList(navController: NavHostController)
{
    val density = LocalDensity.current
    Row(modifier = Modifier.padding(all = 8.dp)) {
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = stringResource(R.string.pref_recorded_file_list),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = { navController.navigate("RecordedFileListScreen") }),
                fontSize = with(density) { 18.dp.toSp() }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.pref_detail_recorded_file_list),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.clickable(onClick = { navController.navigate("RecordedFileListScreen") }),
                fontSize = with(density) { 14.dp.toSp() }
            )
        }
    }
}

@Composable
fun ShowAboutGokigen()
{
    val density = LocalDensity.current
    val uriHandler = LocalUriHandler.current
    val openUri = stringResource(R.string.pref_instruction_manual_url)
    Row(modifier = Modifier.padding(all = 8.dp)) {
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = stringResource(R.string.pref_instruction_manual),
                color = MaterialTheme.colorScheme.primary,
                fontSize = with(density) { 18.dp.toSp() }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = openUri,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.clickable(onClick = { uriHandler.openUri(openUri) }),
                fontSize = with(density) { 14.dp.toSp() }
            )
        }
    }
}

@Composable
fun ShowGokigenPrivacyPolicy()
{
    val density = LocalDensity.current
    val uriHandler = LocalUriHandler.current
    val openUri = stringResource(R.string.pref_privacy_policy_url)
    Row(modifier = Modifier.padding(all = 8.dp)) {
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = stringResource(R.string.pref_privacy_policy),
                color = MaterialTheme.colorScheme.primary,
                fontSize = with(density) { 18.dp.toSp() }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = openUri,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.clickable(onClick = { uriHandler.openUri(openUri) }),
                fontSize = with(density) { 14.dp.toSp() }
            )
        }
    }
}
