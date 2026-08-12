package com.mounir.barcodestock

import android.app.Application
import com.mounir.barcodestock.notify.ExpiryWorker

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        ExpiryWorker.createChannel(this)
        ExpiryWorker.schedule(this)
    }
}
