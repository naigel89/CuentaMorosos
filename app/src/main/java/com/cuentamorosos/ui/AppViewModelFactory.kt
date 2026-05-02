package com.cuentamorosos.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cuentamorosos.data.NotificationScheduler
import com.cuentamorosos.data.repository.RepositoryProvider

class AppViewModelFactory(private val appContext: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(EventsViewModel::class.java) ->
                EventsViewModel(RepositoryProvider.eventRepository, RepositoryProvider.debtRepository) as T
            modelClass.isAssignableFrom(EventDetailViewModel::class.java) ->
                EventDetailViewModel(RepositoryProvider.debtRepository, RepositoryProvider.expenseRepository) as T
            modelClass.isAssignableFrom(ProfilesViewModel::class.java) ->
                ProfilesViewModel(RepositoryProvider.profileRepository, RepositoryProvider.debtRepository) as T
            modelClass.isAssignableFrom(InvitationsViewModel::class.java) ->
                InvitationsViewModel(
                    invitationRepository = RepositoryProvider.invitationRepository,
                    onNewInvitation = { eventName, invitedByEmail ->
                        NotificationScheduler.postInvitationNotification(appContext, eventName, invitedByEmail)
                    }
                ) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
