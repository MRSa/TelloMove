package jp.osdn.gokigen.tellomove.ui.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import jp.osdn.gokigen.tellomove.R
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

        val listState = rememberLazyListState()
        val fileNameList = listViewModel.fileNameList.observeAsState()
        val fileList = fileNameList.value ?: ArrayList()
        if (fileList.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            )
            {
                FilerCommandPanel(navController, listViewModel)
                Spacer(Modifier.size(padding))
                HorizontalDivider(thickness = 1.dp)
                Text(
                    text = stringResource(id = R.string.file_empty),
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
        else
        {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top,
                contentPadding = PaddingValues(bottom = 80.dp), // 末尾に 80dpの余白を設ける
                state = listState
            ) {
                item {
                    FilerCommandPanel(navController, listViewModel)
                    //Text(stringResource(R.string.file_list))
                }
                this.items(fileList.size) { index ->
                    key(index) {
                        FileItem(listViewModel, fileList[index])
                    }
                    HorizontalDivider(thickness = 1.dp)
                }
            }
        }

/*
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
*/
    }
}
