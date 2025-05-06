package jp.osdn.gokigen.tellomove.ui.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import jp.osdn.gokigen.tellomove.ui.model.FileListViewModel


@Composable
fun RecordedFileListScreen(navController: NavHostController, listViewModel: FileListViewModel)
{
    // 画面遷移時にデータを取得
    rememberNavController()
    LaunchedEffect(key1 = Unit) {
        listViewModel.updateFileNameList()
    }

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
            FilerCommandPanel(navController, listViewModel)
            Spacer(Modifier.size(padding))
            HorizontalDivider(thickness = 1.dp)
            MovieFileList(listViewModel)
            ExecutionDialog(listViewModel)
        }
    }
}
