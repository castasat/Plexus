package p.l.e.x.u.s.ui.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import p.l.e.x.u.s.R
import p.l.e.x.u.s.application.PlexusApp.Companion.log
import p.l.e.x.u.s.viewmodel.PlexusViewModel

class ConnectionFragment : Fragment() {
    private val viewModel: PlexusViewModel by activityViewModels()

    private var advertiseButton: Button? = null
    private var discoverButton: Button? = null
    private var sendButton: Button? = null
    private var messageEditText: EditText? = null
    private var advertiseProgressBar: ProgressBar? = null
    private var discoverProgressBar: ProgressBar? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = layoutInflater.inflate(R.layout.fragment_connection, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        findViewsByIds(view)
        observeLiveData()
        setListeners()
        super.onViewCreated(view, savedInstanceState)
    }

    private fun findViewsByIds(view: View) = with(view) {
        advertiseButton = findViewById(R.id.advertiseButton)
        discoverButton = findViewById(R.id.discoverButton)
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)
        advertiseProgressBar = findViewById(R.id.advertiseProgress)
        discoverProgressBar = findViewById(R.id.discoverProgress)
    }

    private fun observeLiveData() {
        observeConnectionAlertDialogLiveData()
        observeCoarseLocationAlertDialogLiveData()
        observeShowToastLiveData()
        observeIsAdvertisingLiveData()
        observeIsDiscoveringLiveData()
        observeShowChatLiveData()
    }

    private fun setListeners() {
        advertiseButton?.setOnClickListener {
            advertiseProgressBar?.visibility = VISIBLE
            viewModel.startAdvertising()
        }
        discoverButton?.setOnClickListener {
            discoverProgressBar?.visibility = VISIBLE
            viewModel.startDiscovering()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun observeShowChatLiveData() {
        viewModel.showChatLiveData
            .observe(viewLifecycleOwner) { endpointId: String ->
                log("ConnectionFragment.observeShowChatLivData(): endpointId = $endpointId")
                if (endpointId.isNotBlank()) {
                    sendButton?.visibility = VISIBLE
                    log(
                        "ConnectionFragment.observeShowChatLivData(): " +
                                "text = ${messageEditText?.text}"
                    )
                    messageEditText?.visibility = VISIBLE
                    sendButton?.setOnClickListener {
                        viewModel.sendBytes(endpointId, messageEditText?.text.toString())
                        messageEditText?.text?.clear()
                    }

                }
            }
    }

    private fun observeConnectionAlertDialogLiveData() {
        viewModel.connectionAlertDialogLiveData
            .observe(viewLifecycleOwner) { (positiveButtonListener, negativeButtonListener, info) ->
                AlertDialog
                    .Builder(requireActivity())
                    .setTitle("Connect to ${info.endpointName}")
                    .setMessage("Confirm the code matches on both devices: ${info.authenticationDigits}")
                    .setPositiveButton("Accept", positiveButtonListener)
                    .setNegativeButton(android.R.string.cancel, negativeButtonListener)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show()
            }
    }

    private fun observeCoarseLocationAlertDialogLiveData() {
        viewModel.coarseLocationAlertDialogLiveData
            .observe(viewLifecycleOwner) { message: String ->
                AlertDialog
                    .Builder(requireActivity())
                    .setTitle("Missing permission access location")
                    .setMessage(message)
                    .setNeutralButton(android.R.string.ok) { dialog, _ ->
                        // hide progress bar
                        discoverProgressBar?.visibility = GONE
                        dialog.dismiss()
                    }
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show()
            }
    }

    private fun observeShowToastLiveData() = viewModel.showToastLiveData
        .observe(viewLifecycleOwner) { toastMessage: String ->
            log("ConnectionFragment.observeShowToastLiveData(): toastMessage = $toastMessage")
            Toast.makeText(requireActivity(), toastMessage, LENGTH_SHORT).show()
        }

    private fun observeIsAdvertisingLiveData() {
        viewModel.isAdvertisingLiveData
            .observe(viewLifecycleOwner) { isAdvertising: Boolean ->
                advertiseButton?.apply {
                    when (isAdvertising) {
                        true -> {
                            text = getString(R.string.stopAdvertising)
                            advertiseProgressBar?.visibility = GONE
                            setOnClickListener { viewModel.stopAdvertising() }
                        }
                        false -> {
                            text = getString(R.string.startAdvertising)
                            setOnClickListener {
                                advertiseProgressBar?.visibility = VISIBLE
                                viewModel.startAdvertising()
                            }
                        }
                    }
                }
            }
    }

    private fun observeIsDiscoveringLiveData() {
        viewModel.isDiscoveringLiveData
            .observe(viewLifecycleOwner) { isDiscovering: Boolean ->
                discoverButton?.apply {
                    when (isDiscovering) {
                        true -> {
                            text = getString(R.string.stopDiscovering)
                            discoverProgressBar?.visibility = GONE
                            setOnClickListener { viewModel.stopDiscovering() }
                        }
                        false -> {
                            text = getString(R.string.startDiscovering)
                            setOnClickListener {
                                discoverProgressBar?.visibility = VISIBLE
                                viewModel.startDiscovering()
                            }
                        }
                    }
                }
            }
    }
}