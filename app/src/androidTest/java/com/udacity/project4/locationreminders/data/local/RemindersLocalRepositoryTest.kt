package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var localDataSource: RemindersLocalRepository
    private lateinit var database: RemindersDatabase

    @Before
    fun setup() {
        // Using an in-memory database for testing, because it doesn't survive killing the process.
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        )
        .allowMainThreadQueries()
        .build()

        localDataSource =
            RemindersLocalRepository(
                database.reminderDao(),
                Dispatchers.Main
            )
    }

    @After
    fun cleanUp() {
        database.close()
    }

    @Test
    fun saveReminder_retrievesRemindersById_returnSuccess() = runBlocking {
        // GIVEN - A new reminder saved in the database.
        val reminder = ReminderDTO(
            title = "title",
            description = "desc",
            location = "loc",
            latitude = 48.123457,
            longitude = 120.253456
        )
        localDataSource.saveReminder(reminder)

        // WHEN  - Reminder retrieved by ID.
        val result = localDataSource.getReminder(reminder.id)

        // THEN - Same reminder is returned.
        assertThat(result is Result.Success, `is`(true))
        result as Result.Success
        assertThat(result.data.title, `is`("title"))
        assertThat(result.data.description, `is`("desc"))
        assertThat(result.data.location, `is`("loc"))
        assertThat(result.data.latitude, `is`(48.123457))
        assertThat(result.data.longitude, `is`(120.253456))
    }

    @Test
    fun saveReminder_retrievesReminderById_returnNotFound() = runBlocking {
        // GIVEN - A new reminder saved in the database.
        val reminder = ReminderDTO(
            title = "title",
            description = "desc",
            location = "loc",
            latitude = 48.123457,
            longitude = 120.253456
        )
        localDataSource.saveReminder(reminder)
        localDataSource.deleteAllReminders()

        // WHEN  - Reminder retrieved by ID.
        val result = localDataSource.getReminder(reminder.id)

        // THEN - Same reminder is returned.
        assertThat(result is Result.Error, `is`(true))
        result as Result.Error
        assertThat(result.message, `is`("Reminder not found!"))
    }

    @Test
    fun saveReminders_retrievesReminders_returnSuccess() = runBlocking {
        // GIVEN - A new reminders saved in the database.
        val reminder1 = ReminderDTO(
            title = "title",
            description = "desc",
            location = "loc",
            latitude = 48.123457,
            longitude = 120.253456)
        val reminder2 = ReminderDTO(title = "title",
            description = "desc",
            location = "loc",
            latitude = 48.123457,
            longitude = 120.253456)
        localDataSource.saveReminder(reminder1)
        localDataSource.saveReminder(reminder2)

        // WHEN  - Reminders retrieved by ID.
        val result = localDataSource.getReminders()

        // THEN - Same reminders is returned.
        assertThat(result is Result.Success, `is`(true))
        result as Result.Success
        assertThat(result.data.size, `is`(2))
    }

    @Test
    fun saveReminders_retrievesReminders_returnEmptyList() = runBlocking {
        // GIVEN - A new reminders saved in the database.
        val reminder1 = ReminderDTO(
            title = "title",
            description = "desc",
            location = "loc",
            latitude = 48.123457,
            longitude = 120.253456)
        val reminder2 = ReminderDTO(title = "title",
            description = "desc",
            location = "loc",
            latitude = 48.123457,
            longitude = 120.253456)
        localDataSource.saveReminder(reminder1)
        localDataSource.saveReminder(reminder2)
        localDataSource.deleteAllReminders()

        // WHEN  - Reminders retrieved by ID.
        val result = localDataSource.getReminders()

        // THEN - Same reminders is returned.
        assertThat(result is Result.Success, `is`(true))
        result as Result.Success
        assertThat(result.data, `is`(emptyList()))
    }

    @Test
    fun deleteAllReminders_retrievesReminders_returnNull() = runBlocking {
        // GIVEN - A new task saved in the database.
        val reminder1 = ReminderDTO(
            title = "title",
            description = "desc",
            location = "loc",
            latitude = 48.123457,
            longitude = 120.253456)
        val reminder2 = ReminderDTO(title = "title",
            description = "desc",
            location = "loc",
            latitude = 48.123457,
            longitude = 120.253456)
        localDataSource.saveReminder(reminder1)
        localDataSource.saveReminder(reminder2)
        localDataSource.deleteAllReminders()

        // WHEN  - Task retrieved by ID.
        val result = localDataSource.getReminders()

        // THEN - Same task is returned.
        assertThat(result is Result.Success, `is`(true))
        result as Result.Success
        assertThat(result.data.firstOrNull(), `is`(nullValue()))
    }
}