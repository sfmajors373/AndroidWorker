package com.mccorby.openmined.worker

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.util.Log
import com.mccorby.openmined.worker.datasource.SyftWebSocketDataSource
import com.mccorby.openmined.worker.domain.SyftCommand
import com.mccorby.openmined.worker.domain.SyftMessage
import com.mccorby.openmined.worker.domain.SyftRepository
import com.mccorby.openmined.worker.domain.SyftTensor
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainViewModel(private val syftRepository: SyftRepository) : ViewModel() {

    val syftMessageState = MutableLiveData<SyftMessage>()
    val syftTensorState = MutableLiveData<SyftTensor>()
    val viewState = MutableLiveData<String>()

    private val compositeDisposable = CompositeDisposable()

    fun initiateCommunication() {
        GlobalScope.launch {
            Log.d("MAinActivity", "Starting datasource")
            syftRepository.connect()
            startListeningToMessages()
        }
    }

    fun sendMessage() {
        GlobalScope.launch {
            syftRepository.sendMessage(SyftMessage.ExecuteCommand(SyftCommand.Result(null, desc = "This would be a result")))
            viewState.postValue("Message sent")
        }
    }

    private fun startListeningToMessages() {
        val disposable = syftRepository.onNewMessage()
            .map { processNewMessage(it) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe()
        compositeDisposable.add(disposable)
    }

    private fun processNewMessage(newSyftMessage: SyftMessage) {
        Log.d("MainActivity", "Received new SyftMessage at $newSyftMessage")
        when (newSyftMessage) {
            is SyftMessage.SetObject -> { syftTensorState.postValue(newSyftMessage.objectToSet) }
            else -> { syftMessageState.postValue(newSyftMessage) }
        }
    }


    public override fun onCleared() {
        compositeDisposable.clear()
        GlobalScope.launch {
            syftRepository.disconnect()
        }
        super.onCleared()
    }
}
