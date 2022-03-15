package p.l.e.x.u.s.ui.activity

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build.*
import android.os.Bundle
import android.view.View.*
import android.widget.Button
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import p.l.e.x.u.s.viewmodel.PlexusViewModel
import p.l.e.x.u.s.application.PlexusApp.Companion.log
import p.l.e.x.u.s.viewmodel.PlexusViewModel.Companion.CODE
import p.l.e.x.u.s.R

class PlexusActivity : AppCompatActivity() {
    private var allPermissionsGranted: Boolean = true

    private var shouldAdvertise: Boolean? = null
    private var shouldDiscover: Boolean? = null

    private val viewModel: PlexusViewModel by viewModels()

    private val advertiseButton: Button by lazy { findViewById(R.id.advertiseButton) }
    private val discoverButton: Button by lazy { findViewById(R.id.discoverButton) }
    private val sendButton: Button by lazy { findViewById(R.id.sendButton) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_layout)
        observeLiveData()
        initViewModel()
        setListeners()
    }

    private fun observeLiveData() {
        observeRequestRuntimePermissionLiveData()
        observeShowSendButtonLiveData()
        observeAlertDialogLiveData()
        observeShowToastLiveData()
    }

    private fun initViewModel() = viewModel.init()

    private fun setListeners() {
        advertiseButton.setOnClickListener {
            when (shouldAdvertise) {
                null, true -> {
                    viewModel.startAdvertising()
                    advertiseButton.text = getString(R.string.stopAdvertising)
                    shouldAdvertise = false
                }
                false -> {
                    viewModel.stopAdvertising()
                    advertiseButton.text = getString(R.string.startAdvertising)
                    shouldAdvertise = true
                }
            }
        }
        discoverButton.setOnClickListener {
            when (shouldDiscover) {
                null, true -> {
                    viewModel.startDiscovering()
                    discoverButton.text = getString(R.string.stopDiscovering)
                    shouldDiscover = false
                }
                false -> {
                    viewModel.stopDiscovering()
                    discoverButton.text = getString(R.string.startDiscovering)
                    shouldDiscover = true
                }
            }
        }
    }

    private fun observeShowToastLiveData() {
        viewModel.showToastLiveData
            .observe(this) { toastMessage: String ->
                Toast
                    .makeText(this, toastMessage, LENGTH_SHORT)
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
                        text = "Send \nto $endpointId"
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
                    .setTitle("Connect to ${info.endpointName}")
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