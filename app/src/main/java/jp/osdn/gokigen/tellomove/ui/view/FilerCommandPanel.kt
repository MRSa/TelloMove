package jp.osdn.gokigen.tellomove.ui.view

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import jp.osdn.gokigen.tellomove.R
import jp.osdn.gokigen.tellomove.file.IFileOperationNotify
import jp.osdn.gokigen.tellomove.ui.model.FileListViewModel

@Composable
fun FilerCommandPanel(navController: NavHostController, listViewModel: FileListViewModel)
{
    var deleteAllFileConfirm by remember { mutableStateOf(false) }
    var deleteSingleFileConfirm by remember { mutableStateOf(false) }
    var exportSingleFileConfirm by remember { mutableStateOf(false) }
    var finishExportNotify by remember { mutableStateOf(false) }
    val selectedFileName = listViewModel.selectedFileName.observeAsState()

    val density = LocalDensity.current
    Spacer(Modifier.size(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(id = R.drawable.baseline_arrow_back_24),
            contentDescription = "Back",
            modifier = Modifier.clickable( onClick = {
                Log.v("BACK", "CURRENT SCREEN: ${navController.currentBackStackEntry?.destination?.route}")
                if (navController.currentBackStackEntry?.destination?.route == "RecordedFileListScreen") {
                    navController.popBackStack()
                }
            })
        )
        Text(text = stringResource(R.string.label_return_to_main_screen),
            fontSize = with(density) { 18.dp.toSp() },
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable( onClick = {
                Log.v("BACK", "CURRENT SCREEN: ${navController.currentBackStackEntry?.destination?.route}")
                if (navController.currentBackStackEntry?.destination?.route == "RecordedFileListScreen")
                {
                    navController.popBackStack()
                }
            })
        )
        IconButton(
            enabled = true,
            onClick = { listViewModel.updateFileNameList() }
        ) {
            Icon(
                modifier = Modifier.alpha(if (false) 1f else 0f),
                painter = painterResource(R.drawable.baseline_refresh_24),
                //tint = MaterialTheme.colorScheme.primary,
                contentDescription = "Refresh"
            )
        }
        IconButton(
            enabled = false,
            onClick = {  }
        ) {
            Icon(
                modifier = Modifier.alpha(if (false) 1f else 0f),
                painter = painterResource(R.drawable.baseline_view_compact_24),
                //tint = MaterialTheme.colorScheme.primary,
                contentDescription = "Spacer"
            )
        }
        IconButton(
            enabled = true,
            onClick = { deleteAllFileConfirm = true }
        ) {
            Icon(
                painter = painterResource(R.drawable.baseline_delete_sweep_24),
                contentDescription = "delete all"
            )
        }
        IconButton(
            enabled = false,
            onClick = {  }
        ) {
            Icon(
                modifier = Modifier.alpha(if (false) 1f else 0f),
                painter = painterResource(R.drawable.baseline_view_compact_24),
                //tint = MaterialTheme.colorScheme.primary,
                contentDescription = "Spacer"
            )
        }
        IconButton(
            enabled = true,
            onClick = { deleteSingleFileConfirm = true }
        ) {
            Icon(
                painter = painterResource(R.drawable.baseline_delete_24),
                contentDescription = "delete"
            )
        }
        IconButton(
            enabled = false,
            onClick = {  }
        ) {
            Icon(
                modifier = Modifier.alpha(if (false) 1f else 0f),
                painter = painterResource(R.drawable.baseline_view_compact_24),
                //tint = MaterialTheme.colorScheme.primary,
                contentDescription = "Spacer"
            )
        }
        IconButton(
            enabled = true,
            onClick = { exportSingleFileConfirm = true }
        ) {
            Icon(
                painter = painterResource(R.drawable.baseline_save_alt_24),
                contentDescription = "delete"
            )
        }
    }

    if (deleteAllFileConfirm)
    {
        // val context = LocalContext.current
        AlertDialog(
            onDismissRequest = { deleteAllFileConfirm = false },
            title = { Text(text = stringResource(R.string.dialog_title_delete_all_confirm)) },
            text = { Text(text = stringResource(R.string.dialog_description_delete_all_confirm)) },
            confirmButton = {
                Button(
                    onClick = {
                        deleteAllFileConfirm = false
                        listViewModel.deleteAllFiles()
                        listViewModel.updateFileNameList()
                    }
                ) {
                    Text(text = stringResource(R.string.dialog_button_ok))
                }
            },
            dismissButton = {
                Button(onClick = { deleteAllFileConfirm = false }) {
                    Text(text = stringResource(R.string.dialog_button_cancel))
                }
            }
        )
    }

    if (deleteSingleFileConfirm)
    {
        val targetFileName = selectedFileName.value?: ""
        if (targetFileName.isNotEmpty())
        {
            AlertDialog(
                onDismissRequest = { deleteSingleFileConfirm = false },
                title = { Text(text = stringResource(R.string.dialog_title_delete_single_confirm)) },
                text = { Text(text = "${stringResource(R.string.dialog_description_delete_single_confirm)} $targetFileName") },
                confirmButton = {
                    Button(
                        onClick = {
                            deleteSingleFileConfirm = false
                            listViewModel.deleteFileName(targetFileName)
                            listViewModel.updateFileNameList()
                        }
                    ) {
                        Text(text = stringResource(R.string.dialog_button_ok))
                    }
                },
                dismissButton = {
                    Button(onClick = { deleteSingleFileConfirm = false }) {
                        Text(text = stringResource(R.string.dialog_button_cancel))
                    }
                }
            )
        }
        else
        {
            deleteSingleFileConfirm = false
        }
    }

    if (exportSingleFileConfirm)
    {
        val targetFileName = selectedFileName.value?: ""
        if (targetFileName.isNotEmpty())
        {
            AlertDialog(
                onDismissRequest = { exportSingleFileConfirm = false },
                title = { Text(text = stringResource(R.string.dialog_title_export_single_confirm)) },
                text = { Text(text = "${stringResource(R.string.dialog_description_export_single_confirm)} $targetFileName") },
                confirmButton = {
                    Button(
                        onClick = {
                            exportSingleFileConfirm = false
                            listViewModel.exportMovieFile(targetFileName, object: IFileOperationNotify {
                                override fun onCompletedExport(result: Boolean, fileName: String) {
                                    finishExportNotify = true
                                }
                            })
                        }
                    ) {
                        Text(text = stringResource(R.string.dialog_button_ok))
                    }
                },
                dismissButton = {
                    Button(onClick = { exportSingleFileConfirm = false }) {
                        Text(text = stringResource(R.string.dialog_button_cancel))
                    }
                }
            )
        }
        else
        {
            exportSingleFileConfirm = false
        }
    }

    if (finishExportNotify)
    {
        // ----- エクスポートの実行終了通知
        AlertDialog(
            onDismissRequest = { finishExportNotify = false },
            title = { Text(text = stringResource(R.string.dialog_title_finish_export)) },
            text = { Text(text = stringResource(R.string.dialog_message_finish_export)) },
            confirmButton = {
                Button(
                    onClick = {
                        finishExportNotify = false
                    }
                ) {
                    Text(text = stringResource(R.string.dialog_button_ok))
                }
            }
        )
    }
}
