package sk.vava.mhd.ui.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import sk.vava.mhd.RepositoryInterface
import sk.vava.mhd.ui.map.MapViewModel

val appModules = module {
    single<RepositoryInterface> { RepositoryInterface.create() }
    viewModel { MapViewModel(get()) }
}