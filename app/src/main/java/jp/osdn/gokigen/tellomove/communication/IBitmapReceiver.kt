package jp.osdn.gokigen.tellomove.communication

import android.graphics.Bitmap

interface IBitmapReceiver
{
    fun updateBitmapImage(bitmap: Bitmap)
}
