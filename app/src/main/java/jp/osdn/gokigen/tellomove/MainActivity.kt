package jp.osdn.gokigen.tellomove

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
//import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import jp.osdn.gokigen.tellomove.ui.model.MainViewModel
import jp.osdn.gokigen.tellomove.ui.model.PreferenceViewModel
import jp.osdn.gokigen.tellomove.ui.view.ViewRootComponent

class MainActivity : AppCompatActivity()
{
    private lateinit var rootComponent : ViewRootComponent
    private lateinit var myMainViewModel : MainViewModel
    private lateinit var myPreferenceViewModel : PreferenceViewModel

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        try
        {
            ///////// SHOW SPLASH SCREEN /////////
            //installSplashScreen()

            enableEdgeToEdge()

            ///////// INITIALIZATION /////////
            myPreferenceViewModel = ViewModelProvider(this)[PreferenceViewModel::class.java]
            myPreferenceViewModel.initializeViewModel(this)

            myMainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
            myMainViewModel.initializeViewModel(this)

            ///////// SET ROOT VIEW /////////
            rootComponent = ViewRootComponent(applicationContext)
            rootComponent.prepare(myMainViewModel, myPreferenceViewModel)
            setContent {
                rootComponent.Content()
            }
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
        Log.v(TAG, "...MainActivity...")

        supportActionBar?.hide()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        try
        {
            ///////// SET PERMISSIONS /////////
            if (!allPermissionsGranted())
            {
                val requestPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {

                    ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
                    if(!allPermissionsGranted())
                    {
                        // Abort launch application because required permissions was rejected.
                        Toast.makeText(this, getString(R.string.permission_not_granted), Toast.LENGTH_SHORT).show()
                        Log.v(TAG, "----- APPLICATION LAUNCH ABORTED -----")
                        finish()
                    }
                }
                requestPermission.launch(REQUIRED_PERMISSIONS)
            }
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    private fun allPermissionsGranted() : Boolean
    {
        var result = true
        for (param in REQUIRED_PERMISSIONS)
        {
            if (ContextCompat.checkSelfPermission(
                    baseContext,
                    param
                ) != PackageManager.PERMISSION_GRANTED
            )
            {
                // Permission Denied...
                if ((param == Manifest.permission.ACCESS_MEDIA_LOCATION)&&(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q))
                {
                    //　この場合は権限付与の判断を除外 (デバイスが (10) よりも古く、ACCESS_MEDIA_LOCATION がない場合）
                }
                else
                {
                    // ----- 権限が得られなかった場合...
                    Log.v(TAG, " Permission: $param : ${Build.VERSION.SDK_INT}")
                    result = false
                }
            }
        }
        return (result)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean
    {
        try
        {
            if (event.action == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_CAMERA))
            {
                Log.v(TAG, "onKeyDown() $keyCode")
            }
        }
        catch (e: java.lang.Exception)
        {
            e.printStackTrace()
        }
        return super.onKeyDown(keyCode, event)
    }

    companion object
    {
        private val TAG = MainActivity::class.java.simpleName

        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_MULTICAST_STATE,
        )
    }
}
