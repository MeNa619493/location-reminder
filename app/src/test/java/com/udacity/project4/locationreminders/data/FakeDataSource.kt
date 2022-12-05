package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(var remindersList: MutableList<ReminderDTO>?)
    : ReminderDataSource {

    private var isReturnError: Boolean = false

    fun setIsReturnError(isReturnError: Boolean) {
        this.isReturnError = isReturnError
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        return if (isReturnError) {
            Result.Error("couldn't retrieve reminders")
        } else {
            if (remindersList?.isEmpty() == true) {
                Result.Success(ArrayList())
            } else {
                val reminders = remindersList?.let { ArrayList(it) }
                Result.Success(reminders!!)
            }
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        remindersList?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        return if (isReturnError || remindersList?.isEmpty() == true) {
            Result.Error("couldn't retrieve reminders")
        } else {
            val reminder = remindersList?.find { it.id == id }
            if (reminder != null) {
                Result.Success(reminder)
            } else {
                Result.Error("No such reminder with that id")
            }
        }
    }

    override suspend fun deleteAllReminders() {
        remindersList?.clear()
    }
}