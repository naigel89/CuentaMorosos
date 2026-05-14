package com.cuentamorosos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cuentamorosos.ui.DashboardViewModel
import com.cuentamorosos.ui.EventDetailViewModel
import com.cuentamorosos.ui.EventsViewModel
import com.cuentamorosos.ui.InvitationsViewModel
import com.cuentamorosos.ui.ProfilesViewModel

/**
 * ViewModelProvider.Factory that creates all ViewModels using the RepositoryProvider.
 */
class AppViewModelFactory(
    private val repositoryProvider: RepositoryProvider
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(EventsViewModel::class.java) -> {
                EventsViewModel(
                    eventRepository = repositoryProvider.eventRepository,
                    debtRepository = repositoryProvider.debtRepository
                ) as T
            }
            modelClass.isAssignableFrom(EventDetailViewModel::class.java) -> {
                EventDetailViewModel(
                    debtRepository = repositoryProvider.debtRepository,
                    expenseRepository = repositoryProvider.expenseRepository
                ) as T
            }
            modelClass.isAssignableFrom(ProfilesViewModel::class.java) -> {
                ProfilesViewModel(
                    profileRepository = repositoryProvider.profileRepository,
                    debtRepository = repositoryProvider.debtRepository
                ) as T
            }
            modelClass.isAssignableFrom(InvitationsViewModel::class.java) -> {
                InvitationsViewModel(
                    invitationRepository = repositoryProvider.invitationRepository
                ) as T
            }
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> {
                DashboardViewModel(
                    eventRepository = repositoryProvider.eventRepository,
                    debtRepository = repositoryProvider.debtRepository,
                    expenseRepository = repositoryProvider.expenseRepository,
                    profileRepository = repositoryProvider.profileRepository,
                    currentUserUid = "", // TODO: pass actual UID from MainActivity
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
