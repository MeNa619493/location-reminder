package com.udacity.project4.locationreminders.savereminder

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class SaveReminderViewModelTest {
    @get: Rule
    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var fakeLocalDataSource: FakeDataSource
    private lateinit var saveReminderViewModel: SaveReminderViewModel

    private val reminder1 = ReminderDataItem(title = "title",
        description = "desc",
        location = "loc",
        latitude = 48.123457,
        longitude = 120.253456)

    private val reminder2 = ReminderDTO(title = "title",
        description = "desc",
        location = "loc",
        latitude = 48.123457,
        longitude = 120.253456)

    private val reminderWithNoTitleNeitherLocation = ReminderDataItem(title = "",
        description = "desc",
        location = "",
        latitude = 48.123457,
        longitude = 120.253456)

    private val remindersList: ArrayList<ReminderDTO> = ArrayList()

    @Before
    fun setUp() {
        stopKoin()

        fakeLocalDataSource = FakeDataSource(remindersList)
        saveReminderViewModel = SaveReminderViewModel(
            ApplicationProvider.getApplicationContext(), fakeLocalDataSource)
        runBlocking{
            fakeLocalDataSource.deleteAllReminders()
        }
    }

    @Test
    fun saveReminder() {
        saveReminderViewModel.saveReminder(reminder1)
        Truth.assertThat(fakeLocalDataSource.remindersList?.first()?.title).isEqualTo(reminder2.title)
        Truth.assertThat(fakeLocalDataSource.remindersList?.first()?.description).isEqualTo(reminder2.description)
        Truth.assertThat(fakeLocalDataSource.remindersList?.first()?.location).isEqualTo(reminder2.location)
        Truth.assertThat(fakeLocalDataSource.remindersList?.first()?.longitude).isEqualTo(reminder2.longitude)
        Truth.assertThat(fakeLocalDataSource.remindersList?.first()?.latitude).isEqualTo(reminder2.latitude)
    }

    @Test
    fun saveReminder_whenSaving_showToast()= runBlockingTest {
        fakeLocalDataSource.deleteAllReminders()
        saveReminderViewModel.saveReminder(reminder1)
        MatcherAssert.assertThat(saveReminderViewModel.showToast.getOrAwaitValue(), CoreMatchers.`is`("Reminder Saved !"))
    }

    @Test
    fun saveReminder_whenNoTitle_showSnackBar() {
        saveReminderViewModel.validateAndSaveReminder(reminderWithNoTitleNeitherLocation)
        MatcherAssert.assertThat(saveReminderViewModel.showSnackBarInt.getOrAwaitValue(), CoreMatchers.notNullValue())
    }

    @Test
    fun saveReminder_whenNoLocation_showSnackBar() {
        saveReminderViewModel.validateAndSaveReminder(reminderWithNoTitleNeitherLocation)
        MatcherAssert.assertThat(saveReminderViewModel.showSnackBarInt.getOrAwaitValue(), CoreMatchers.notNullValue())
    }

    @Test
    fun saveReminder_whenSaving_showLoading() = runBlocking {
        mainCoroutineRule.pauseDispatcher()
        saveReminderViewModel.validateAndSaveReminder(reminder1)
        MatcherAssert.assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), CoreMatchers.`is`(true))

        mainCoroutineRule.resumeDispatcher()
        MatcherAssert.assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), CoreMatchers.`is`(false))
    }
}