package com.mccorby.openmined.worker.ui

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.mccorby.openmined.worker.MainViewModel
import com.mccorby.openmined.worker.domain.Operations
import com.mccorby.openmined.worker.domain.SyftRepository

class MainViewModelFactory(
    private val syftRepository: SyftRepository,
    private val mlFramework: Operations
) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(syftRepository, mlFramework) as T
    }
}