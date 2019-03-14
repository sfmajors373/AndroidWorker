package com.mccorby.openmined.worker

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.mccorby.openmined.worker.datasource.SyftWebSocketDataSource
import com.mccorby.openmined.worker.domain.SyftMessage
import com.mccorby.openmined.worker.domain.SyftRepository
import com.mccorby.openmined.worker.domain.SyftTensor
import com.mccorby.openmined.worker.framework.DL4JOperations
import com.mccorby.openmined.worker.framework.toINDArray
import com.mccorby.openmined.worker.ui.MainViewModelFactory
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btn_initiate.setOnClickListener { viewModel.initiateCommunication() }
        btn_sendMessage.setOnClickListener { viewModel.sendMessage() }

        injectDependencies()
    }

    // TODO Inject using Kodein or another DI framework
    private fun injectDependencies() {
        val webSocketUrl = "http://10.0.2.2:5000"
//        val webSocketUrl = "http://192.168.0.33:5000"
        val syftDataSource = SyftWebSocketDataSource(webSocketUrl)
        val syftRepository = SyftRepository(syftDataSource)

        viewModel = ViewModelProviders.of(
            this,
            MainViewModelFactory(syftRepository)
        ).get(MainViewModel::class.java)

        viewModel.syftMessageState.observe(this, Observer<SyftMessage> {
            log_area.append(it.toString() + "\n")

        })
        viewModel.syftTensorState.observe(this, Observer<SyftTensor> {
            log_area.append(it.toString() + "\n")
            log_area.append(it!!.toINDArray().toString())
        })
        viewModel.viewState.observe(this, Observer {
            log_area.append(it + "\n")
        })
    }
}