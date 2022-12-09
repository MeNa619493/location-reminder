package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase

    @Before
    fun initDb() {
        // Using an in-memory database so that the information stored here disappears when the
        // process is killed.
        database = Room.inMemoryDatabaseBuilder(
            getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun insertReminderAndGetById() = runBlockingTest {
        // GIVEN - Insert a task.
        val reminder = ReminderDTO(
            title = "title",
            description = "desc",
            location = "loc",
            latitude = 48.123457,
            longitude = 120.253456)
        database.reminderDao().saveReminder(reminder)

        // WHEN - Get the task by id from the database.
        val loaded = database.reminderDao().getReminderById(reminder.id)

        // THEN - The loaded data contains the expected values.
        assertThat<ReminderDTO>(loaded as ReminderDTO, notNullValue())
        assertThat(loaded.id, `is`(reminder.id))
        assertThat(loaded.title, `is`(reminder.title))
        assertThat(loaded.description, `is`(reminder.description))
        assertThat(loaded.location, `is`(reminder.location))
        assertThat(loaded.latitude, `is`(reminder.latitude))
        assertThat(loaded.longitude, `is`(reminder.longitude))
    }

    @Test
    fun insertReminderAndGetAllReminders() = runBlockingTest {
        // GIVEN - Insert the tasks.
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
        database.reminderDao().saveReminder(reminder1)
        database.reminderDao().saveReminder(reminder2)

        // WHEN - Get all tasks from the database.
        val loaded = database.reminderDao().getReminders()

        // THEN - The loaded data contains the expected values.
        assertThat<List<ReminderDTO>>(loaded , notNullValue())
        assertThat(loaded.size, `is`(2))
    }

    @Test
    fun insertAndDeleteDatabase() = runBlockingTest {
        // GIVEN - Insert a task.
        val reminder = ReminderDTO(
            title = "title",
            description = "desc",
            location = "loc",
            latitude = 48.123457,
            longitude = 120.253456)
        database.reminderDao().saveReminder(reminder)
        database.reminderDao().deleteAllReminders()

        // WHEN - Get the tasks from the database.
        val loaded = database.reminderDao().getReminders()

        // THEN - The loaded data contains the expected values.
        assertThat(loaded.isEmpty(), `is`(true))
    }
}