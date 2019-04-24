package com.mccorby.openmined.worker

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.util.Log
import com.mccorby.openmined.worker.domain.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainViewModel(
    private val syftRepository: SyftRepository,
    private val mlFramework: Operations
) : ViewModel() {

    val syftMessageState = MutableLiveData<SyftMessage>()
    val syftTensorState = MutableLiveData<SyftOperand.SyftTensor>()
    val viewState = MutableLiveData<String>()

    private val compositeDisposable = CompositeDisposable()

    fun initiateCommunication() {
        GlobalScope.launch {
            Log.d("MAinActivity", "Starting datasource")
            syftRepository.connect()
            startListeningToMessages()
        }
    }

    private fun startListeningToMessages() {
        val messageDisposable = syftRepository.onNewMessage()
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
                syftRepository.setObject(newSyftMessage.objectToSet as SyftOperand.SyftTensor)
                syftTensorState.postValue(newSyftMessage.objectToSet)
                syftRepository.sendMessage(SyftMessage.OperationAck)
            }
            is SyftMessage.ExecuteCommand -> {
                syftTensorState.postValue(createCommandEvent(newSyftMessage))
            }
            is SyftMessage.GetObject -> {
                val tensor = syftRepository.getObject(newSyftMessage.tensorPointerId)
                viewState.postValue("Server requested tensor with id ${newSyftMessage.tensorPointerId}")
                // TODO copy should not be necessary. Here set just to make it work. This is a value that should have been already set before
                syftRepository.sendMessage(SyftMessage.RespondToObjectRequest(tensor.copy(id = newSyftMessage.tensorPointerId)))
            }
            is SyftMessage.DeleteObject -> {
                syftRepository.removeObject(newSyftMessage.objectToDelete)
                viewState.postValue("Tensor with id ${newSyftMessage.objectToDelete} deleted")
                syftRepository.sendMessage(SyftMessage.OperationAck)
            }
            else -> {
                syftMessageState.postValue(newSyftMessage)
            }
        }
    }

    private fun createCommandEvent(syftMessage: SyftMessage.ExecuteCommand): SyftOperand.SyftTensor {
        // TODO This should be done by a domain component
        return when (syftMessage.command) {
            is SyftCommand.Add -> {
                when (syftMessage.command.tensors[0]) {
                    is SyftOperand.SyftTensor -> {
                        mlFramework.add(
                            syftMessage.command.tensors[0] as SyftOperand.SyftTensor,
                            syftMessage.command.tensors[1] as SyftOperand.SyftTensor
                        )
                    }
                    is SyftOperand.SyftTensorPointer -> {
                        val result = mlFramework.add(
                            syftRepository.getObject(syftMessage.command.tensors[0].id),
                            syftRepository.getObject(syftMessage.command.tensors[1].id)
                        )
                        // Add only expects now a single return id
                        val resultId = syftMessage.command.resultIds[0]
                        syftRepository.setObject(resultId, result)
                        syftRepository.sendMessage(SyftMessage.OperationAck)
                        result
                    }
                }
            }
            else -> {
                TODO("${syftMessage.command} not yet implemented")
            }
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
