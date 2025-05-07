package jp.osdn.gokigen.tellomove.ui.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.osdn.gokigen.tellomove.R
import jp.osdn.gokigen.tellomove.ui.model.FileListViewModel

@Composable
fun MovieFileList(listViewModel: FileListViewModel)
{
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
                Text(stringResource(R.string.file_list))
            }
            this.items(fileList.size) { index ->
                key(index) {
                    FileItem(listViewModel, fileList[index])
                }
                HorizontalDivider(thickness = 1.dp)
            }
        }
    }
}

@Composable
fun FileItem(listViewModel: FileListViewModel, fileName: String)
{
    val selectedFileName = listViewModel.selectedFileName.observeAsState()
    val currentSelectedFileName = selectedFileName.value ?: ""
    val currentSelect = (currentSelectedFileName == fileName)
    Row()
    {
        Icon(
            modifier =  Modifier.clickable(onClick = {
                val select = if (currentSelect) { "" } else { fileName }
                listViewModel.selectedFileName(select)
            }),
            painter = if (currentSelect)
            {
                painterResource(R.drawable.baseline_check_box_24)
            }
            else
            {
                painterResource(R.drawable.baseline_check_box_outline_blank_24)
            },
            tint = if (currentSelect)
            {
                // 選択中
                if(isSystemInDarkTheme()) { Color.LightGray } else { Color.DarkGray }
            } else {
                // 未選択
                if(isSystemInDarkTheme()) { Color.DarkGray } else { Color.LightGray }
            },
            contentDescription = "selection",
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .clickable(onClick = {
                    val select = if (currentSelect) { "" } else { fileName }
                    listViewModel.selectedFileName(select)
                })
        ) {
            Text(
                fontSize = 18.sp,
                text = fileName,
                color = if(isSystemInDarkTheme()) { Color.LightGray } else { Color.DarkGray },
                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)
            )
            //var secondText = " "
            Text(
                fontSize = 16.sp,
                //text = secondText,
                text = "",
                color = if(isSystemInDarkTheme()) { Color.LightGray } else { Color.DarkGray },
                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)
            )
        }
    }
}