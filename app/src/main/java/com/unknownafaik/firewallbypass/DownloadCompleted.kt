package com.unknownafaik.firewallbypass

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.lang.Exception
import javax.inject.Inject

@AndroidEntryPoint
class DownloadCompleted : BroadcastReceiver() {

    @Inject
    lateinit var viewModel: RequestResponseViewModel

    override fun onReceive(context: Context?, intent: Intent?) {

       val extra = intent?.extras

        extra?.let { bundle ->

            try {
                val long = bundle.getLong(DownloadManager.EXTRA_DOWNLOAD_ID)
                viewModel.onCompleted(long)
            }catch (e : Exception){
                e.fillInStackTrace()
            }
        }
    }
}