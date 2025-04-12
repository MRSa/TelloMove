package jp.osdn.gokigen.tellomove.ui.view

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import jp.osdn.gokigen.tellomove.R
import jp.osdn.gokigen.tellomove.ui.model.MainViewModel

@Composable
fun LiveViewPanel(viewModel: MainViewModel)
{

    val imageBitmap = viewModel.imageBitmap.observeAsState()
    if (imageBitmap.value != null)
    {
        Image(
            bitmap = imageBitmap.value!!.asImageBitmap(),
            contentDescription = "Live View"
        )
    }
    else
    {
        Image(
            painter = painterResource(R.drawable.tello),
            contentDescription = "Tello"
        )
    }
}
