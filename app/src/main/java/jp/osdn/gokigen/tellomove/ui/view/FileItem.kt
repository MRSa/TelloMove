package jp.osdn.gokigen.tellomove.ui.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.osdn.gokigen.tellomove.R
import jp.osdn.gokigen.tellomove.ui.model.FileListViewModel

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
