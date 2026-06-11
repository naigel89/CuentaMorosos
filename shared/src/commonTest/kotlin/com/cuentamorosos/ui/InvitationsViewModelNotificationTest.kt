package com.cuentamorosos.ui

import com.cuentamorosos.data.repository.InvitationRepository
import com.cuentamorosos.model.EventInvitation
import com.cuentamorosos.model.InvitationStatus
import com.cuentamorosos.notifications.NotificationEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InvitationsViewModelNotificationTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun invitation(
        id: String,
        eventId: String = "evt-1",
        eventName: String = "Asado",
        email: String = "ana@test.com",
        status: String = InvitationStatus.PENDING,
    ) = EventInvitation(
        id = id,
        eventId = eventId,
        eventName = eventName,
        invitedByUid = "user-other",
        invitedByEmail = email,
        invitedEmail = "me@test.com",
        status = status,
    )

    @Test
    fun `onNewInvitation fires only once per invitation id`() = runTest {
        val invitations = listOf(invitation("inv-1"))
        val flow = MutableStateFlow<List<EventInvitation>>(invitations)
        val receivedEvents = mutableListOf<NotificationEvent.InvitationReceived>()

        val viewModel = InvitationsViewModel(
            invitationRepository = FakeInvitationRepository(flow),
            onNewInvitation = { receivedEvents.add(it) },
        )

        // Collect the flow to trigger side effects
        val collectJob = backgroundScope.launch {
            viewModel.pendingInvitations.collect {}
        }
        advanceUntilIdle()
        assertEquals(1, receivedEvents.size)

        // Re-emit same list → should NOT fire again
        flow.value = invitations
        advanceUntilIdle()
        assertEquals(1, receivedEvents.size)
        collectJob.cancel()
    }

    @Test
    fun `onNewInvitation does not fire for non-PENDING invitations`() = runTest {
        val invitations = listOf(
            invitation("inv-2", status = InvitationStatus.ACCEPTED),
        )
        val flow = MutableStateFlow<List<EventInvitation>>(invitations)
        val receivedEvents = mutableListOf<NotificationEvent.InvitationReceived>()

        val viewModel = InvitationsViewModel(
            invitationRepository = FakeInvitationRepository(flow),
            onNewInvitation = { receivedEvents.add(it) },
        )

        val collectJob = backgroundScope.launch {
            viewModel.pendingInvitations.collect {}
        }
        advanceUntilIdle()
        assertTrue(receivedEvents.isEmpty())
        collectJob.cancel()
    }

    @Test
    fun `onNewInvitation does not crash when callback is null`() = runTest {
        val invitations = listOf(invitation("inv-3"))
        val flow = MutableStateFlow<List<EventInvitation>>(invitations)

        val viewModel = InvitationsViewModel(
            invitationRepository = FakeInvitationRepository(flow),
            onNewInvitation = null,
        )

        val collectJob = backgroundScope.launch {
            viewModel.pendingInvitations.collect {}
        }
        advanceUntilIdle()
        // No crash = success
        collectJob.cancel()
    }

    @Test
    fun `onNewInvitation fires for each new invitation`() = runTest {
        val flow = MutableStateFlow<List<EventInvitation>>(emptyList())
        val receivedEvents = mutableListOf<NotificationEvent.InvitationReceived>()

        val viewModel = InvitationsViewModel(
            invitationRepository = FakeInvitationRepository(flow),
            onNewInvitation = { receivedEvents.add(it) },
        )

        val collectJob = backgroundScope.launch {
            viewModel.pendingInvitations.collect {}
        }
        advanceUntilIdle()
        assertEquals(0, receivedEvents.size)

        // Add two new invitations
        flow.value = listOf(invitation("inv-a"), invitation("inv-b"))
        advanceUntilIdle()
        assertEquals(2, receivedEvents.size)
        collectJob.cancel()
    }

    @Test
    fun `onNewInvitation passes correct event data`() = runTest {
        val inv = invitation(
            id = "inv-x",
            eventId = "evt-x",
            eventName = "Cena",
            email = "bob@test.com",
        )
        val flow = MutableStateFlow<List<EventInvitation>>(listOf(inv))
        val receivedEvents = mutableListOf<NotificationEvent.InvitationReceived>()

        val viewModel = InvitationsViewModel(
            invitationRepository = FakeInvitationRepository(flow),
            onNewInvitation = { receivedEvents.add(it) },
        )

        val collectJob = backgroundScope.launch {
            viewModel.pendingInvitations.collect {}
        }
        advanceUntilIdle()
        assertEquals(1, receivedEvents.size)
        val event = receivedEvents[0]
        assertEquals("inv-x", event.invitationId)
        assertEquals("evt-x", event.eventId)
        assertEquals("bob@test.com", event.inviterName)
        assertEquals("Cena", event.eventName)
        collectJob.cancel()
    }
}

/**
 * Minimal fake InvitationRepository for testing.
 */
private class FakeInvitationRepository(
    private val flow: MutableStateFlow<List<EventInvitation>>,
) : InvitationRepository {
    override fun observePendingInvitations(): Flow<List<EventInvitation>> = flow
    override suspend fun sendInvitation(invitation: EventInvitation) {}
    override suspend fun acceptInvitation(invitation: EventInvitation) {}
    override suspend fun rejectInvitation(invitationId: String) {}
}
