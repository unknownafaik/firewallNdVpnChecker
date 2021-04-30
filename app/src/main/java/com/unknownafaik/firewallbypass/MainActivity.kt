package com.unknownafaik.firewallbypass

import android.app.DownloadManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.viewModelScope
import com.google.android.material.snackbar.Snackbar
import com.unknownafaik.firewallbypass.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var viewModel: RequestResponseViewModel

    lateinit var views: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }

        views = ActivityMainBinding.inflate(layoutInflater)
        setContentView(views.root)

        viewModel.viewModel.observe(
            this
        ) { update ->

            update?.let {
                when (update) {
                    is RequestResponseViewModel.Status.InProgress -> {

                        val total = update.ids.size
                        val completed = update.completed.size

                        val status = "in progress $completed in $total completed"

                        views.test.text = status

                        if (completed > 1) {
                            views.test.isEnabled = true
                            views.showResponse.isEnabled = true
                        }
                    }
                    is RequestResponseViewModel.Status.Completed -> {
                        views.test.text = "test again"
                        views.test.isEnabled = true
                        views.showResponse.isEnabled = true
                    }
                    is RequestResponseViewModel.Status.ReadyToTest -> {
                        views.test.text = "perform test"
                        views.test.isEnabled = true
                        views.showResponse.isEnabled = false
                    }
                }
            }
        }

        views.test.setOnClickListener {

            viewModel.viewModelScope.launch(Dispatchers.IO) {
                try {
                    viewModel.makeNewRequest()
                } catch (e: SecurityException) {
                    println(e.localizedMessage)

                    withContext(Dispatchers.Main) {
                        Snackbar.make(
                            views.root,
                            "INTERNET permission removed 🤨 \n" +
                                    "Mission passed respect +",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        views.showResponse.setOnClickListener {

            AlertDialog.Builder(this)
                .setTitle("How to check result")
                .setMessage(
                    "few web page with info about your private and public ip has been downloaded in your \"Download \" folder " +
                            "\n \n" +
                            "You can see what info is leaked in most case it also include your private ip NOT JUST VPN ONE"
                )
                .setPositiveButton(
                    "okay open file manager"
                ) { _, _ ->
                    startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
                }
                .show()
        }

    }

}