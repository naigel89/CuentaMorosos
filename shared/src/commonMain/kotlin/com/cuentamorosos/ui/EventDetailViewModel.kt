package com.cuentamorosos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuentamorosos.data.repository.DebtRepository
import com.cuentamorosos.data.repository.ExpenseRepository
import com.cuentamorosos.model.EventDebtItem
import com.cuentamorosos.model.EventExpenseItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EventDetailViewModel(
    private val debtRepository: DebtRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val _eventId = MutableStateFlow<String?>(null)
    val eventId: StateFlow<String?> = _eventId.asStateFlow()

    val debts: Flow<List<EventDebtItem>> = _eventId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else debtRepository.observeDebts(id)
    }

    val expenses: Flow<List<EventExpenseItem>> = _eventId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else expenseRepository.observeExpenses(id)
    }

    fun setEventId(id: String?) {
        _eventId.value = id
    }

    fun saveDebt(debt: EventDebtItem) {
        viewModelScope.launch {
            debtRepository.saveDebt(debt)
        }
    }

    fun deleteDebt(eventId: String, debtId: String) {
        viewModelScope.launch {
            debtRepository.deleteDebt(eventId, debtId)
        }
    }

    fun saveExpense(expense: EventExpenseItem) {
        viewModelScope.launch {
            expenseRepository.saveExpense(expense)
        }
    }

    fun deleteExpense(eventId: String, expenseId: String) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(eventId, expenseId)
        }
    }
}
