package com.cuentamorosos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import kotlin.reflect.KClass
import com.cuentamorosos.ui.AccountViewModel
import com.cuentamorosos.ui.DashboardViewModel
import com.cuentamorosos.ui.EventDetailViewModel
import com.cuentamorosos.ui.EventsViewModel
import com.cuentamorosos.ui.InvitationsViewModel
import com.cuentamorosos.ui.ProfilesViewModel

/**
 * ViewModelProvider.Factory that creates all ViewModels using the RepositoryProvider.
 */
class AppViewModelFactory(
    private val repositoryProvider: RepositoryProvider,
    private val currentProfileId: String = "",
    private val notificationCallbacks: NotificationCallbacks = NotificationCallbacks(),
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
        return when {
            modelClass == EventsViewModel::class -> {
                EventsViewModel(
                    eventRepository = repositoryProvider.eventRepository,
                    debtRepository = repositoryProvider.debtRepository,
                    expenseRepository = repositoryProvider.expenseRepository,
                    currentProfileId = currentProfileId,
                ) as T
            }
            modelClass == EventDetailViewModel::class -> {
                EventDetailViewModel(
                    eventRepository = repositoryProvider.eventRepository,
                    debtRepository = repositoryProvider.debtRepository,
                    expenseRepository = repositoryProvider.expenseRepository,
                    invitationRepository = repositoryProvider.invitationRepository,
                    currentProfileId = currentProfileId,
                ) as T
            }
            modelClass == ProfilesViewModel::class -> {
                ProfilesViewModel(
                    profileRepository = repositoryProvider.profileRepository,
                    debtRepository = repositoryProvider.debtRepository
                ) as T
            }
            modelClass == InvitationsViewModel::class -> {
                InvitationsViewModel(
                    invitationRepository = repositoryProvider.invitationRepository,
                    onNewInvitation = notificationCallbacks.onInvitationReceived,
                    onInvitationAccepted = notificationCallbacks.onInvitationAccepted,
                ) as T
            }
            modelClass == DashboardViewModel::class -> {
                DashboardViewModel(
                    eventRepository = repositoryProvider.eventRepository,
                    debtRepository = repositoryProvider.debtRepository,
                    expenseRepository = repositoryProvider.expenseRepository,
                    profileRepository = repositoryProvider.profileRepository,
                    invitationRepository = repositoryProvider.invitationRepository,
                    currentUserUid = currentProfileId,
                    onCalculationCompleted = notificationCallbacks.onCalculationCompleted,
                ) as T
            }
            modelClass == AccountViewModel::class -> {
                AccountViewModel(
                    profileRepository = repositoryProvider.profileRepository,
                    currentProfileId = currentProfileId,
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.simpleName}")
        }
    }
}
