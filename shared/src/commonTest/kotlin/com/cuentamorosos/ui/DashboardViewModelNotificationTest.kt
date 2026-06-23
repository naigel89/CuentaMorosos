package com.cuentamorosos.ui

import com.cuentamorosos.data.repository.DebtRepository
import com.cuentamorosos.data.repository.EventRepository
import com.cuentamorosos.data.repository.ExpenseRepository
import com.cuentamorosos.data.repository.InvitationRepository
import com.cuentamorosos.data.repository.ProfileRepository
import com.cuentamorosos.model.EventDebtItem
import com.cuentamorosos.model.EventExpenseItem
import com.cuentamorosos.model.EventInvitation
import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.EventState
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.SettlementTransfer
import com.cuentamorosos.notifications.NotificationEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
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

class DashboardViewModelNotificationTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun event(
        id: String,
        name: String = "Cena",
        state: EventState = EventState.CALCULATED,
    ) = EventItem(
        id = id,
        name = name,
        dateMillis = 0L,
        ownerId = "user-1",
        state = state,
    )

    private fun debt(
        eventId: String,
        profileId: String,
        amount: Double,
        paid: Boolean = false,
    ) = EventDebtItem(
        eventId = eventId,
        profileId = profileId,
        amountEuros = amount,
        paid = paid,
    )

    @Test
    fun `onCalculationCompleted fires when CALCULATED and amountOwed greater than 0`() = runTest {
        val events = listOf(event("evt-1"))
        val debts = listOf(debt("evt-1", "user-1", 25.0))

        val receivedEvents = mutableListOf<NotificationEvent.CalculationCompleted>()
        val viewModel = DashboardViewModel(
            eventRepository = FakeEventRepository(MutableStateFlow(events)),
            debtRepository = FakeDebtRepository(MutableStateFlow(debts)),
            expenseRepository = FakeExpenseRepository(MutableStateFlow(emptyList())),
            profileRepository = FakeProfileRepository(MutableStateFlow(emptyList())),
            invitationRepository = FakeDashboardInvitationRepository(observeCalculationCompletedFlow = emptyFlow()),
            currentUserUid = "user-1",
            onCalculationCompleted = { receivedEvents.add(it) },
        )

        advanceUntilIdle()
        assertEquals(1, receivedEvents.size)
        assertEquals(25.0, receivedEvents[0].amountOwed)
        assertEquals("evt-1", receivedEvents[0].eventId)
        assertEquals("Cena", receivedEvents[0].eventName)
    }

    @Test
    fun `onCalculationCompleted does NOT fire when amountOwed is 0`() = runTest {
        val events = listOf(event("evt-1"))
        val debts = listOf(debt("evt-1", "other-user", 25.0))

        val receivedEvents = mutableListOf<NotificationEvent.CalculationCompleted>()
        val viewModel = DashboardViewModel(
            eventRepository = FakeEventRepository(MutableStateFlow(events)),
            debtRepository = FakeDebtRepository(MutableStateFlow(debts)),
            expenseRepository = FakeExpenseRepository(MutableStateFlow(emptyList())),
            profileRepository = FakeProfileRepository(MutableStateFlow(emptyList())),
            invitationRepository = FakeDashboardInvitationRepository(observeCalculationCompletedFlow = emptyFlow()),
            currentUserUid = "user-1",
            onCalculationCompleted = { receivedEvents.add(it) },
        )

        advanceUntilIdle()
        assertTrue(receivedEvents.isEmpty())
    }

    @Test
    fun `dedup suppresses duplicate calculation notification on combine re-emission`() = runTest {
        val eventsFlow = MutableStateFlow(listOf(event("evt-1")))
        val debtsFlow = MutableStateFlow(listOf(debt("evt-1", "user-1", 10.0)))

        val receivedEvents = mutableListOf<NotificationEvent.CalculationCompleted>()
        val viewModel = DashboardViewModel(
            eventRepository = FakeEventRepository(eventsFlow),
            debtRepository = FakeDebtRepository(debtsFlow),
            expenseRepository = FakeExpenseRepository(MutableStateFlow(emptyList())),
            profileRepository = FakeProfileRepository(MutableStateFlow(emptyList())),
            invitationRepository = FakeDashboardInvitationRepository(observeCalculationCompletedFlow = emptyFlow()),
            currentUserUid = "user-1",
            onCalculationCompleted = { receivedEvents.add(it) },
        )

        advanceUntilIdle()
        assertEquals(1, receivedEvents.size)
        assertEquals("evt-1", receivedEvents[0].eventId)

        // Emit a new list with an additional calculated event — evt-1 should NOT fire again
        eventsFlow.value = listOf(event("evt-1"), event("evt-2"))
        debtsFlow.value = listOf(debt("evt-1", "user-1", 10.0), debt("evt-2", "user-1", 5.0))
        advanceUntilIdle()
        // Only evt-2 fires; evt-1 was already notified (in-memory dedup)
        assertEquals(2, receivedEvents.size)
        assertEquals("evt-2", receivedEvents[1].eventId)
    }

    @Test
    fun `onCalculationCompleted does NOT fire for non-CALCULATED events`() = runTest {
        val events = listOf(event("evt-1", state = EventState.OPEN))
        val debts = listOf(debt("evt-1", "user-1", 25.0))

        val receivedEvents = mutableListOf<NotificationEvent.CalculationCompleted>()
        val viewModel = DashboardViewModel(
            eventRepository = FakeEventRepository(MutableStateFlow(events)),
            debtRepository = FakeDebtRepository(MutableStateFlow(debts)),
            expenseRepository = FakeExpenseRepository(MutableStateFlow(emptyList())),
            profileRepository = FakeProfileRepository(MutableStateFlow(emptyList())),
            invitationRepository = FakeDashboardInvitationRepository(observeCalculationCompletedFlow = emptyFlow()),
            currentUserUid = "user-1",
            onCalculationCompleted = { receivedEvents.add(it) },
        )

        advanceUntilIdle()
        assertTrue(receivedEvents.isEmpty())
    }

    @Test
    fun `onCalculationCompleted does not crash when callback is null`() = runTest {
        val events = listOf(event("evt-1"))
        val debts = listOf(debt("evt-1", "user-1", 25.0))

        val viewModel = DashboardViewModel(
            eventRepository = FakeEventRepository(MutableStateFlow(events)),
            debtRepository = FakeDebtRepository(MutableStateFlow(debts)),
            expenseRepository = FakeExpenseRepository(MutableStateFlow(emptyList())),
            profileRepository = FakeProfileRepository(MutableStateFlow(emptyList())),
            invitationRepository = FakeDashboardInvitationRepository(observeCalculationCompletedFlow = emptyFlow()),
            currentUserUid = "user-1",
            onCalculationCompleted = null,
        )

        advanceUntilIdle()
        // No crash = success
    }

    @Test
    fun `onCalculationCompleted only counts unpaid debts for current user`() = runTest {
        val events = listOf(event("evt-1"))
        val debts = listOf(
            debt("evt-1", "user-1", 10.0, paid = false),
            debt("evt-1", "user-1", 5.0, paid = true),  // paid → excluded
            debt("evt-1", "other", 20.0, paid = false),  // other user → excluded
        )

        val receivedEvents = mutableListOf<NotificationEvent.CalculationCompleted>()
        @Suppress("UNUSED_VARIABLE")
        val viewModel = DashboardViewModel(
            eventRepository = FakeEventRepository(MutableStateFlow(events)),
            debtRepository = FakeDebtRepository(MutableStateFlow(debts)),
            expenseRepository = FakeExpenseRepository(MutableStateFlow(emptyList())),
            profileRepository = FakeProfileRepository(MutableStateFlow(emptyList())),
            invitationRepository = FakeDashboardInvitationRepository(observeCalculationCompletedFlow = emptyFlow()),
            currentUserUid = "user-1",
            onCalculationCompleted = { receivedEvents.add(it) },
        )

        advanceUntilIdle()
        assertEquals(1, receivedEvents.size)
        assertEquals(10.0, receivedEvents[0].amountOwed)
    }

    @Test
    fun `observeCalculationCompleted emission fires onCalculationCompleted callback`() = runTest {
        val calculationCompletedFlow = MutableStateFlow(
            NotificationEvent.CalculationCompleted(eventId = "", eventName = "", amountOwed = 0.0)
        )

        val receivedEvents = mutableListOf<NotificationEvent.CalculationCompleted>()
        val viewModel = DashboardViewModel(
            eventRepository = FakeEventRepository(MutableStateFlow(emptyList())),
            debtRepository = FakeDebtRepository(MutableStateFlow(emptyList())),
            expenseRepository = FakeExpenseRepository(MutableStateFlow(emptyList())),
            profileRepository = FakeProfileRepository(MutableStateFlow(emptyList())),
            invitationRepository = FakeDashboardInvitationRepository(observeCalculationCompletedFlow = calculationCompletedFlow),
            currentUserUid = "user-1",
            onCalculationCompleted = { receivedEvents.add(it) },
        )

        advanceUntilIdle()

        calculationCompletedFlow.value = NotificationEvent.CalculationCompleted(
            eventId = "evt-x",
            eventName = "Cena",
            amountOwed = 42.5,
        )
        advanceUntilIdle()

        assertEquals(1, receivedEvents.size)
        assertEquals("evt-x", receivedEvents[0].eventId)
        assertEquals("Cena", receivedEvents[0].eventName)
        assertEquals(42.5, receivedEvents[0].amountOwed)
    }
}

// ── Fake Repositories ─────────────────────────────────────────────────────────

private class FakeDashboardInvitationRepository(
    private val observeCalculationCompletedFlow: Flow<NotificationEvent.CalculationCompleted>,
    private val _sentNotifications: MutableList<Triple<String, String, Double>> = mutableListOf(),
) : InvitationRepository {
    val sentNotifications: List<Triple<String, String, Double>> get() = _sentNotifications

    override fun observePendingInvitations(): Flow<List<EventInvitation>> = emptyFlow()
    override suspend fun sendInvitation(invitation: EventInvitation) {}
    override suspend fun acceptInvitation(invitation: EventInvitation, inviteeName: String) {}
    override suspend fun rejectInvitation(invitationId: String) {}
    override fun observeInvitationAccepted(): Flow<NotificationEvent.InvitationAccepted> = emptyFlow()
    override suspend fun sendCalculationNotification(eventId: String, eventName: String, participantUid: String, amountOwed: Double) {
        _sentNotifications.add(Triple(eventId, eventName, amountOwed))
    }
    override fun observeCalculationCompleted(): Flow<NotificationEvent.CalculationCompleted> = observeCalculationCompletedFlow
}

private class FakeEventRepository(
    private val flow: MutableStateFlow<List<EventItem>>,
) : EventRepository {
    override fun observeEvents(): Flow<List<EventItem>> = flow
    override fun observeEvent(eventId: String): Flow<EventItem?> = MutableStateFlow(null)
    override suspend fun fetchEvents(): List<EventItem> = flow.value
    override suspend fun saveEvent(event: EventItem) {}
    override suspend fun deleteEvent(eventId: String) {}
    override suspend fun removeMember(eventId: String, memberUid: String) {}
    override suspend fun findUidByEmail(email: String): String? = null
    override suspend fun replaceMemberId(oldId: String, newId: String) {}
}

private class FakeDebtRepository(
    private val flow: MutableStateFlow<List<EventDebtItem>>,
) : DebtRepository {
    override fun observeDebts(eventId: String): Flow<List<EventDebtItem>> = flow
    override fun observeAllDebts(): Flow<List<EventDebtItem>> = flow
    override suspend fun saveDebt(debt: EventDebtItem) {}
    override suspend fun deleteDebt(eventId: String, debtId: String) {}
    override suspend fun deleteAllDebtsForEvent(eventId: String) {}
    override suspend fun deleteDebtsForProfile(profileId: String) {}
    override suspend fun replaceProfileId(oldId: String, newId: String) {}
    override suspend fun fetchDebtsForEvent(eventId: String): List<EventDebtItem> = flow.value
    override suspend fun fetchAllDebts(): List<EventDebtItem> = flow.value
    override suspend fun applyCalculation(eventId: String, modeId: String, transfers: List<SettlementTransfer>, paidTransferIndices: List<Int>) {}
}

private class FakeExpenseRepository(
    private val flow: MutableStateFlow<List<EventExpenseItem>>,
) : ExpenseRepository {
    override fun observeExpenses(eventId: String): Flow<List<EventExpenseItem>> = flow
    override fun observeAllExpenses(): Flow<List<EventExpenseItem>> = flow
    override suspend fun saveExpense(expense: EventExpenseItem) {}
    override suspend fun deleteExpense(eventId: String, expenseId: String) {}
    override suspend fun replaceProfileId(oldId: String, newId: String) {}
    override suspend fun fetchExpensesForEvent(eventId: String): List<EventExpenseItem> = flow.value
    override suspend fun fetchAllExpenses(): List<EventExpenseItem> = flow.value
    override suspend fun deleteAllExpensesForEvent(eventId: String) {}
}

private class FakeProfileRepository(
    private val flow: MutableStateFlow<List<ProfileItem>>,
) : ProfileRepository {
    override fun observeProfiles(): Flow<List<ProfileItem>> = flow
    override suspend fun saveProfile(profile: ProfileItem) {}
    override suspend fun deleteProfile(profileId: String) {}
    override suspend fun linkGhostProfile(userEmail: String, userUid: String) {}
    override suspend fun updateProfilePhoto(photoUrl: String): Result<String> = Result.success("")
    override suspend fun updateUsername(username: String): Result<Unit> = Result.success(Unit)
    override suspend fun updateDisplayName(displayName: String): Result<Unit> = Result.success(Unit)
    override suspend fun setCustomName(profileId: String, customName: String): Result<Unit> = Result.success(Unit)
    override suspend fun isUsernameAvailable(username: String): Boolean = true
    override suspend fun updatePassword(currentPassword: String, newPassword: String): Result<Unit> = Result.success(Unit)
    override suspend fun deleteProfilePhoto(): Result<Unit> = Result.success(Unit)
    override suspend fun searchByUsername(prefix: String): List<ProfileItem> = emptyList()
}
