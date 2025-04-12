package jp.osdn.gokigen.tellomove.ui.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.osdn.gokigen.tellomove.R
import jp.osdn.gokigen.tellomove.ui.model.MainViewModel

@Composable
fun SetParameterLayout(viewModel: MainViewModel) {
    val moveDistance = viewModel.moveDistanceCm.observeAsState()
    val rotationDegree = viewModel.moveDegree.observeAsState()
    val moveSpeed = viewModel.moveSpeed.observeAsState()
    val status = viewModel.statusMessage.observeAsState()
    val statusMessage = status.value ?: ""
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (statusMessage.isNotBlank())
        {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = statusMessage,
                    modifier = Modifier.padding(start = 6.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.padding((4.dp)))
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.label_distance),
                modifier = Modifier
                    .padding(start = 6.dp)
                    .weight(1f), // テキストの幅に比重を与える
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.padding((4.dp)))
            TextField(
                enabled = true,
                value = moveDistance.value?.toString() ?: "",
                singleLine = true,
                onValueChange = { value -> try { viewModel.setDistance(value.toInt()) } catch (_: Exception) { viewModel.setDistance(0) } },
                modifier = Modifier.weight(1f), // TextFieldの幅に比重を与える
                textStyle = TextStyle(fontSize = 14.sp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.label_rotation_degree),
                modifier = Modifier
                    .padding(start = 6.dp)
                    .weight(1f), // テキストの幅に比重を与える
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.padding((4.dp)))
            TextField(
                enabled = true,
                value = rotationDegree.value.toString(),
                singleLine = true,
                onValueChange = { value -> try { viewModel.setDegree(value.toInt()) } catch (_: Exception) { viewModel.setDegree(0) }  },
                modifier = Modifier.weight(1f), // TextFieldの幅に比重を与える
                textStyle = TextStyle(fontSize = 14.sp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.label_speed),
                modifier = Modifier
                    .padding(start = 6.dp)
                    .weight(1f), // テキストの幅に比重を与える
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.padding((4.dp)))
            TextField(
                enabled = true,
                value = moveSpeed.value.toString(),
                singleLine = true,
                onValueChange = { value -> try { viewModel.setSpeed(value.toInt()) } catch (_: Exception) { viewModel.setSpeed(0) }  },
                modifier = Modifier.weight(1f), // TextFieldの幅に比重を与える
                textStyle = TextStyle(fontSize = 14.sp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }
}
