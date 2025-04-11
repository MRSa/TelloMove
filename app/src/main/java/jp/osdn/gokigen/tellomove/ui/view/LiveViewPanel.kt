package jp.osdn.gokigen.tellomove.ui.view

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import jp.osdn.gokigen.tellomove.R
import jp.osdn.gokigen.tellomove.ui.model.MainViewModel

@Composable
fun LiveViewPanel(viewModel: MainViewModel)
{
    Image(
        painter = painterResource(id = R.drawable.tello),
        contentDescription = "Tello"
    )
}
