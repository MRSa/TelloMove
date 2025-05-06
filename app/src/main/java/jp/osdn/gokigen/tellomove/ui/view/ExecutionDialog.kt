package jp.osdn.gokigen.tellomove.ui.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import jp.osdn.gokigen.tellomove.R
import jp.osdn.gokigen.tellomove.ui.model.FileListViewModel

@Composable
fun ExecutionDialog(listViewModel: FileListViewModel)
{
    val isExporting = listViewModel.processExecuting.observeAsState()
    if (isExporting.value == true)
    {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(stringResource(R.string.dialog_progress_executing)) },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            },
            confirmButton = { },
            dismissButton = null
        )
    }
}