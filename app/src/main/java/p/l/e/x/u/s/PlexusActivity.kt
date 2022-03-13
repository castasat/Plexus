package p.l.e.x.u.s

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build.*
import android.os.Bundle
import android.view.View.*
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import p.l.e.x.u.s.PlexusApp.Companion.log
import p.l.e.x.u.s.PlexusViewModel.Companion.CODE

class PlexusActivity : AppCompatActivity() {
    private var allPermissionsGranted: Boolean = true
    private val viewModel: PlexusViewModel by viewModels()

    private val advertiseButton: Button by lazy { findViewById(R.id.advertiseButton) }
    private val discoverButton: Button by lazy { findViewById(R.id.discoverButton) }
    private val sendButton: Button by lazy { findViewById(R.id.sendButton) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_layout)
        observeLiveData()
        viewModel.init()
        setListeners()
    }

    private fun setListeners() {
        with(viewModel) {
            advertiseButton.setOnClickListener { advertise() }
            discoverButton.setOnClickListener { discover() }
        }
    }

    private fun observeLiveData() {
        observeRequestRuntimePermissionLiveData()
        observeShowSendButtonLiveData()
        observeAlertDialogLiveData()
        observeShowToastLiveData()
    }

    private fun observeShowToastLiveData() {
        viewModel.showToastLiveData
            .observe(this) { toastMessage: String ->
                Toast
                    .makeText(this, toastMessage, Toast.LENGTH_LONG)
                    .show()
            }
    }

    @SuppressLint("SetTextI18n")
    private fun observeShowSendButtonLiveData() {
        viewModel.showSendButtonLiveData
            .observe(this) { endpointId: String ->
                if (endpointId.isNotBlank()) {
                    with(sendButton) {
                        visibility = VISIBLE
                        text = "Send\n to $endpointId"
                        setOnClickListener { viewModel.send(endpointId) }
                    }
                }
            }
    }

    private fun observeAlertDialogLiveData() {
        viewModel.showAlertDialogLiveData
            .observe(this) { (positiveButtonListener, negativeButtonListener, info) ->
                AlertDialog
                    .Builder(this)
                    .setTitle("Do you accept connection to ${info.endpointName}?")
                    .setMessage("Confirm the code matches on both devices: ${info.authenticationDigits}")
                    .setPositiveButton("Accept", positiveButtonListener)
                    .setNegativeButton(android.R.string.cancel, negativeButtonListener)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show()
            }
    }

    private fun observeRequestRuntimePermissionLiveData() {
        viewModel.requestRuntimePermissionLiveData
            .observe(this) { permissionsToRequestAtRuntime: Array<String> ->
                if (VERSION.SDK_INT >= VERSION_CODES.M) {
                    for (permission in permissionsToRequestAtRuntime) {
                        if (checkSelfPermission(permission) != PERMISSION_GRANTED) {
                            requestPermissions(permissionsToRequestAtRuntime, CODE)
                        }
                    }
                } else {
                    log(
                        "PlexusActivity.observeRequestRuntimePermissionLiveData(): " +
                                "permission granted or API < 23 (M, Android 6)"
                    )
                }
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == CODE) {
            // находим индекс в массиве permissions и проверяем значение
            // из массива grantResults с этим индексом
            for (i in permissions.indices) {
                log("PlexusActivity.onRequestPermissionsResult(): permission = ${permissions[i]}")
                when (permissions[i]) {
                    ACCESS_FINE_LOCATION,
                    BLUETOOTH_ADVERTISE,
                    BLUETOOTH_CONNECT,
                    BLUETOOTH_SCAN,
                    READ_EXTERNAL_STORAGE -> allPermissionsGranted =
                        if (grantResults[i] == PERMISSION_GRANTED) {
                            log("PlexusActivity.onRequestPermissionsResult(): permission granted")
                            allPermissionsGranted
                        } else {
                            log("PlexusActivity.onRequestPermissionsResult(): permission denied")
                            false
                        }
                    else -> log("PlexusActivity.onRequestPermissionsResult(): unknown permission")
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}