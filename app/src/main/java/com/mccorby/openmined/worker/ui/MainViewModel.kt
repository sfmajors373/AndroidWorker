package com.mccorby.openmined.worker.ui

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.util.Log
import com.mccorby.openmined.worker.domain.SyftCommand
import com.mccorby.openmined.worker.domain.SyftMessage
import com.mccorby.openmined.worker.domain.SyftOperand
import com.mccorby.openmined.worker.domain.SyftRepository
import com.mccorby.openmined.worker.domain.usecase.ConnectUseCase
import com.mccorby.openmined.worker.domain.usecase.ObserveMessagesUseCase
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class MainViewModel(
    private val observeMessagesUseCase: ObserveMessagesUseCase,
    private val connectUseCase: ConnectUseCase,
    private val syftRepository: SyftRepository
) : ViewModel() {

    val syftMessageState = MutableLiveData<SyftMessage>()
    val syftTensorState = MutableLiveData<SyftOperand.SyftTensor>()
    val viewState = MutableLiveData<String>()

    private val compositeDisposable = CompositeDisposable()

    fun initiateCommunication() {
        val connectDisposable = connectUseCase.execute()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe()
        compositeDisposable.add(connectDisposable)

        // TODO Ideally we should start listening to message when connectUseCase completes
        startListeningToMessages()
    }

    private fun startListeningToMessages() {
        val messageDisposable = observeMessagesUseCase.execute()
            .map { processNewMessage(it) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe()

        val statusDisposable = syftRepository.onStatusChange()
            .map { viewState.postValue(it) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe()

        compositeDisposable.addAll(messageDisposable, statusDisposable)
    }

    private fun processNewMessage(newSyftMessage: SyftMessage) {
        Log.d("MainActivity", "Received new SyftMessage at $newSyftMessage")
        when (newSyftMessage) {
            is SyftMessage.SetObject -> {
                syftTensorState.postValue(newSyftMessage.objectToSet as SyftOperand.SyftTensor)
            }
            is SyftMessage.ExecuteCommand -> {
                processCommand(newSyftMessage.command)
            }
            is SyftMessage.GetObject -> {
                viewState.postValue("Server requested tensor with id ${newSyftMessage.tensorPointerId}")
            }
            is SyftMessage.DeleteObject -> {
                viewState.postValue("Tensor with id ${newSyftMessage.objectToDelete} deleted")
            }
            else -> {
                syftMessageState.postValue(newSyftMessage)
            }
        }
    }

    private fun processCommand(command: SyftCommand) {
        when (command) {
            is SyftCommand.Add -> {
                syftTensorState.postValue(syftRepository.getObject(command.resultIds[0]))
            }
        }
    }

    public override fun onCleared() {
        compositeDisposable.clear()
        syftRepository.disconnect()
        super.onCleared()
    }
}
