package com.cuentamorosos.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.cuentamorosos.db.CuentaMorososDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class PendingOperationQueueTest {

    private lateinit var database: CuentaMorososDatabase
    private lateinit var queue: PendingOperationQueue

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CuentaMorososDatabase.Schema.create(driver)
        database = CuentaMorososDatabase(driver)

        val testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        queue = PendingOperationQueue(
            database = database,
            scope = kotlinx.coroutines.CoroutineScope(testDispatcher),
        )
    }

    @Test
    fun `enqueue creates row`() = runTest {
        queue.enqueue(
            id = "op-1",
            entityType = "event",
            entityId = "evt-1",
            operation = "save",
            payload = """{"name":"Test"}""",
        )

        assertEquals(1L, queue.getAllPending())
    }

    @Test
    fun `dequeue returns FIFO order`() = runTest {
        queue.enqueue("op-1", "event", "evt-1", "save", """{"a":1}""")
        // Small delay to ensure different createdAt timestamps
        Thread.sleep(10)
        queue.enqueue("op-2", "debt", "debt-1", "delete", """{"b":2}""")
        Thread.sleep(10)
        queue.enqueue("op-3", "expense", "exp-1", "save", """{"c":3}""")

        val results = queue.dequeue(10)

        assertEquals(3, results.size)
        assertEquals("op-1", results[0].id)
        assertEquals("op-2", results[1].id)
        assertEquals("op-3", results[2].id)
    }

    @Test
    fun `markComplete removes row`() = runTest {
        queue.enqueue("op-1", "event", "evt-1", "save", """{"x":1}""")
        queue.enqueue("op-2", "debt", "debt-1", "delete", """{"y":2}""")
        assertEquals(2L, queue.getAllPending())

        queue.markComplete("op-1")

        assertEquals(1L, queue.getAllPending())
        val remaining = queue.dequeue(10)
        assertEquals("op-2", remaining[0].id)
    }

    @Test
    fun `markFailed increments retryCount`() = runTest {
        queue.enqueue("op-1", "event", "evt-1", "save", """{"z":1}""")

        queue.markFailed("op-1")
        queue.markFailed("op-1")
        queue.markFailed("op-1")

        val results = queue.dequeue(10)
        assertEquals(1, results.size)
        assertEquals(3L, results[0].retryCount)
    }

    @Test
    fun `getAllPending returns count`() = runTest {
        assertEquals(0L, queue.getAllPending())

        queue.enqueue("op-1", "event", "evt-1", "save", """{"a":1}""")
        assertEquals(1L, queue.getAllPending())

        queue.enqueue("op-2", "debt", "debt-1", "delete", """{"b":2}""")
        queue.enqueue("op-3", "expense", "exp-1", "save", """{"c":3}""")
        assertEquals(3L, queue.getAllPending())

        queue.markComplete("op-2")
        assertEquals(2L, queue.getAllPending())
    }
}
