package com.udacity.project4.locationreminders.reminderslist

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.hamcrest.core.IsNot
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class RemindersListViewModelTest {

    @get: Rule
    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var fakeLocalDataSource: FakeDataSource
    private lateinit var remindersListViewModel: RemindersListViewModel

    private val reminder1 = ReminderDTO(title = "title",
        description = "desc",
        location = "loc",
        latitude = 48.123457,
        longitude = 120.253456)

    private val reminder2 = ReminderDTO(title = "title",
        description = "desc",
        location = "loc",
        latitude = 55.645216,
        longitude = 125.124535)

    private val remindersList: ArrayList<ReminderDTO> = ArrayList()

    @Before
    fun setUp() {
        stopKoin()

        fakeLocalDataSource = FakeDataSource(remindersList)
        remindersListViewModel = RemindersListViewModel(
            ApplicationProvider.getApplicationContext(), fakeLocalDataSource)
    }

    @Test
    fun loadReminders_whenError_showSnackBar() = runBlockingTest {
        fakeLocalDataSource.setIsReturnError(true)
        remindersListViewModel.loadReminders()
        MatcherAssert.assertThat(remindersListViewModel.showSnackBar.getOrAwaitValue(), CoreMatchers.`is`("couldn't retrieve reminders"))
    }

    @Test
    fun loadReminders_whenEmptyArray_showNoData() = runBlockingTest {
        remindersListViewModel.loadReminders()
        MatcherAssert.assertThat(remindersListViewModel.showNoData.getOrAwaitValue(), CoreMatchers.`is`(true))
    }

    @Test
    fun loadReminders_whenDeleteAllData_showNoData() = runBlockingTest {
        fakeLocalDataSource.saveReminder(reminder1)
        fakeLocalDataSource.deleteAllReminders()
        remindersListViewModel.loadReminders()
        MatcherAssert.assertThat(remindersListViewModel.showNoData.getOrAwaitValue(), CoreMatchers.`is`(true))
    }

    @Test
    fun loadReminders_WhenRemindersAvailable_getReminders() = runBlocking {
        fakeLocalDataSource.saveReminder(reminder1)
        fakeLocalDataSource.saveReminder(reminder2)

        mainCoroutineRule.pauseDispatcher()
        remindersListViewModel.loadReminders()
        MatcherAssert.assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), CoreMatchers.`is`(true))
        mainCoroutineRule.resumeDispatcher()
        MatcherAssert.assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), CoreMatchers.`is`(false))
        MatcherAssert.assertThat(remindersListViewModel.showNoData.getOrAwaitValue(), CoreMatchers.`is`(false))
        Assert.assertThat(remindersListViewModel.remindersList.getOrAwaitValue(), (IsNot.not(emptyList())))
        Assert.assertThat(remindersListViewModel.remindersList.getOrAwaitValue().size, CoreMatchers.`is`(remindersList.size))
    }

}