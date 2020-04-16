package org.covidwatch.android.ui

import android.Manifest.permission.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.work.*
import org.covidwatch.android.CovidWatchApplication
import org.covidwatch.android.R
import org.covidwatch.android.data.BluetoothViewModel
import org.covidwatch.android.data.firestore.SignedReportsDownloadWorker

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var vm: BluetoothViewModel

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        vm = ViewModelProvider(this).get(BluetoothViewModel::class.java)
        // TODO #15: COMMENTING OUT BECAUSE USING EMULATOR
        // BRING BACK AFTER MERGING UX FIRST RUN
        // REMOVE adding example CEN
//        initBluetoothAdapter()
        Log.i("test", "did we make it here?")
        addDummyCEN()

        val navController = findNavController(R.id.nav_host_fragment)
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    public override fun onResume() {
        super.onResume()
        (application as? CovidWatchApplication)?.refreshOneTime()
    }

    public fun addDummyCEN() {
//        val cen = BluetoothManagerImpl.DefaultCenGenerator().generate()
//        Log.i("test", "how about here?")
//        Log.i("CEN BOI", cen.data.toString())
//        CovidWatchDatabase.databaseWriteExecutor.execute {
//            val dao: ContactEventDAO = CovidWatchDatabase.getInstance(this).tempraryContactNumberDAO()
//            val tempraryContactNumber = ContactEvent(cen.data.toString())
//            val isCurrentUserSick = this.getSharedPreferences(
//                this.getString(R.string.preference_file_key),
//                Context.MODE_PRIVATE
//            ).getBoolean(this.getString(R.string.preference_is_current_user_sick), false)
//            tempraryContactNumber.wasPotentiallyInfectious = isCurrentUserSick
//            dao.insert(tempraryContactNumber)
//        }
    }

    private fun setContactEventLogging(enabled: Boolean) {

        val application = this?.applicationContext ?: return
        val sharedPref = application.getSharedPreferences(
            application.getString(R.string.preference_file_key), Context.MODE_PRIVATE
        ) ?: return
        with(sharedPref.edit()) {
            putBoolean(
                application.getString(org.covidwatch.android.R.string.preference_is_temporary_contact_number_logging_enabled),
                enabled
            )
            commit()
        }
    }

    /**
     * Initializes the BluetoothAdapter. Manifest file is already setup to allow bluetooth access.
     * The user will be asked to enable bluetooth if it is turned off
     */
    private fun initBluetoothAdapter() {
        bluetoothAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 1)
        }
    }

    /**
     * Initializes the Location Manager used to obtain coarse bluetooth/wifi location
     * and fine GPS location, logged on a temporary contact number.
     *
     * TODO add GPS initialization here, for now we just ask for location permissions
     */
    private val LOCATION_REQUEST_CODE = 1
    fun initLocationManager() {
        runOnUiThread {
            val permissionStatus = ContextCompat.checkSelfPermission(
                this,
                ACCESS_FINE_LOCATION
            )
            when (permissionStatus){
                PackageManager.PERMISSION_GRANTED -> vm.permissionRequestResultLiveData.value = true
                PackageManager.PERMISSION_DENIED -> {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this,ACCESS_FINE_LOCATION)){
                        Toast.makeText(this,
                            getString(R.string.ble_location_permission),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    //TODO: If a user selects "Don't ask again" the function below does not show the system dialogue.  This prevents the user from ever progressing. Maybe this is an OK UX?
                    val permissions = arrayOf(ACCESS_COARSE_LOCATION,ACCESS_FINE_LOCATION,ACCESS_BACKGROUND_LOCATION)
                    ActivityCompat.requestPermissions(this, permissions, LOCATION_REQUEST_CODE)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode){
            LOCATION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    setContactEventLogging(true)
                    vm.permissionRequestResultLiveData.value = true
                }
            }
            else -> {}//Ignore all other requests
        }
    }
}
