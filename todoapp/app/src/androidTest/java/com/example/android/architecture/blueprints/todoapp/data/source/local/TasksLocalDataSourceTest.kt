/*
 * Copyright 2017, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.architecture.blueprints.todoapp.data.source.local

import androidx.room.Room
import androidx.test.InstrumentationRegistry
import androidx.test.filters.LargeTest
import androidx.test.runner.AndroidJUnit4
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.Result
import com.example.android.architecture.blueprints.todoapp.data.source.TasksDataSource
import com.example.android.architecture.blueprints.todoapp.util.runBlockingSilent
import com.example.android.architecture.blueprints.todoapp.utils.SingleExecutors
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.core.Is.`is`
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test for the [TasksDataSource].
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class TasksLocalDataSourceTest {

    private val TITLE = "title"
    private val TITLE2 = "title2"
    private val TITLE3 = "title3"

    private lateinit var localDataSource: TasksLocalDataSource
    private lateinit var database: ToDoDatabase

    @Before
    fun setup() {
        // using an in-memory database for testing, since it doesn't survive killing the process
        database = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getContext(),
                ToDoDatabase::class.java)
                .build()

        // Make sure that we're not keeping a reference to the wrong instance.
        TasksLocalDataSource.clearInstance()
        localDataSource = TasksLocalDataSource.getInstance(SingleExecutors(), database.taskDao())
    }

    @After
    fun cleanUp() {
        database.close()
        TasksLocalDataSource.clearInstance()
    }

    @Test
    fun testPreConditions() {
        assertNotNull(localDataSource)
    }

    @Test
    fun saveTask_retrievesTask() = runBlockingSilent {
        // Given a new task
        val newTask = Task(TITLE)

        with(localDataSource) {
            // When saved into the persistent repository
            saveTask(newTask)

            // Then the task can be retrieved from the persistent repository
            val result = getTask(newTask.id)
            assertThat(result, instanceOf(Result.Success::class.java))
            if (result is Result.Success) {
                assertThat(result.data, `is`(newTask))
            }
        }
    }

    @Test
    fun completeTask_retrievedTaskIsComplete() = runBlockingSilent {
        // Given a new task in the persistent repository
        val newTask = Task(TITLE)
        localDataSource.saveTask(newTask)

        // When completed in the persistent repository
        localDataSource.completeTask(newTask)

        // Then the task can be retrieved from the persistent repository and is complete
        val result = localDataSource.getTask(newTask.id)
        assertThat(result, instanceOf(Result.Success::class.java))
        if (result is Result.Success) {
            assertThat(result.data, `is`(newTask))
            assertThat(result.data.isCompleted, `is`(true))
        }

    }

    @Test
    fun activateTask_retrievedTaskIsActive() = runBlockingSilent {
        // Given a new completed task in the persistent repository
        val newTask = Task(TITLE)
        with(localDataSource) {
            saveTask(newTask)
            completeTask(newTask)

            // When activated in the persistent repository
            activateTask(newTask)

            // Then the task can be retrieved from the persistent repository and is active
            val task = getTask(newTask.id)
            assertNotEquals(null, task)
        }

        assertThat(newTask.isCompleted, `is`(false))
    }

    @Test
    fun clearCompletedTask_taskNotRetrievable() = runBlockingSilent {
        // Given 2 new completed tasks and 1 active task in the persistent repository
        val newTask1 = Task(TITLE)
        val newTask2 = Task(TITLE2)
        val newTask3 = Task(TITLE3)

        with(localDataSource) {
            saveTask(newTask1)
            completeTask(newTask1)
            saveTask(newTask2)
            completeTask(newTask2)
            saveTask(newTask3)
            // When completed tasks are cleared in the repository
            clearCompletedTasks()

            // Then the completed tasks cannot be retrieved and the active one can
            val result1 = getTask(newTask1.id)
            assertThat(result1, instanceOf(Result.Error::class.java))

            val result2 = getTask(newTask2.id)
            assertThat(result2, instanceOf(Result.Error::class.java))

            val result3 = getTask(newTask3.id)
            assertThat(result3, instanceOf(Result.Success::class.java))
            if (result3 is Result.Success) {
                assertThat(result3.data, `is`(newTask3))
            }
        }
    }

    @Test
    fun deleteAllTasks_emptyListOfRetrievedTask() = runBlockingSilent {
        with(localDataSource) {
            // Given a new task in the persistent repository and a mocked callback
            saveTask(Task(TITLE))

            // When all tasks are deleted
            deleteAllTasks()

            // Then the retrieved tasks is an empty list
            val result = getTasks()
            assertThat(result, instanceOf(Result.Error::class.java))
        }
    }

    @Test
    fun getTasks_retrieveSavedTasks() = runBlockingSilent {
        with(localDataSource) {
            // Given 2 new tasks in the persistent repository
            val newTask1 = Task(TITLE)
            saveTask(newTask1)
            val newTask2 = Task(TITLE)
            saveTask(newTask2)

            // Then the tasks can be retrieved from the persistent repository
            val result = getTasks()
            assertThat(result, CoreMatchers.instanceOf(Result.Success::class.java))
            if (result is Result.Success) {
                assertNotNull(result.data)
                assertTrue(result.data.size >= 2)

                var newTask1IdFound = false
                var newTask2IdFound = false
                for (task in result.data) {
                    if (task.id == newTask1.id) {
                        newTask1IdFound = true
                    }
                    if (task.id == newTask2.id) {
                        newTask2IdFound = true
                    }
                }
                assertTrue(newTask1IdFound)
                assertTrue(newTask2IdFound)
            }
        }
    }
}
