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
package com.example.android.architecture.blueprints.todoapp.tasks

import com.example.android.architecture.blueprints.todoapp.argumentCaptor
import com.example.android.architecture.blueprints.todoapp.capture
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksDataSource
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository
import com.example.android.architecture.blueprints.todoapp.anyMockito
import com.example.android.architecture.blueprints.todoapp.data.source.DataSourceException
import com.example.android.architecture.blueprints.todoapp.data.source.Result
import com.example.android.architecture.blueprints.todoapp.util.runBlockingSilent
import kotlinx.coroutines.experimental.Unconfined
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

/**
 * Unit tests for the implementation of [TasksPresenter]
 */
class TasksPresenterTest {

    @Mock private lateinit var tasksRepository: TasksRepository

    @Mock private lateinit var tasksView: TasksContract.View

    private lateinit var tasksPresenter: TasksPresenter

    private lateinit var tasks: List<Task>

    @Before
    fun setupTasksPresenter() {
        // Mockito has a very convenient way to inject mocks by using the @Mock annotation. To
        // inject the mocks in the test the initMocks method needs to be called.
        MockitoAnnotations.initMocks(this)

        // Get a reference to the class under test
        tasksPresenter = TasksPresenter(tasksRepository, tasksView, coroutineScope = Unconfined)

        // The presenter won't update the view unless it's active.
        `when`(tasksView.isActive).thenReturn(true)

        // We start the tasks to 3, with one active and two completed
        tasks = mutableListOf(Task("Title1", "Description1"),
                Task("Title2", "Description2").apply { isCompleted = true },
                Task("Title3", "Description3").apply { isCompleted = true })
    }

    @Test
    fun createPresenter_setsThePresenterToView() {
        // Get a reference to the class under test
        tasksPresenter = TasksPresenter(tasksRepository, tasksView, coroutineScope = Unconfined)

        // Then the presenter is set to the view
        verify(tasksView).presenter = tasksPresenter
    }

    @Test
    fun loadAllTasksFromRepositoryAndLoadIntoView() = runBlockingSilent {
        setTasksAvailable(tasksRepository, tasks)

        with(tasksPresenter) {
            // Given an initialized TasksPresenter with initialized tasks
            // When loading of Tasks is requested
            currentFiltering = TasksFilterType.ALL_TASKS
            loadTasks(true)
        }

        verify(tasksRepository).getTasks()

        // Then progress indicator is shown
        val inOrder = inOrder(tasksView)
        inOrder.verify(tasksView).setLoadingIndicator(true)
        // Then progress indicator is hidden and all tasks are shown in UI
        inOrder.verify(tasksView).setLoadingIndicator(false)
        val showTasksArgumentCaptor = argumentCaptor<List<Task>>()
        verify(tasksView).showTasks(capture(showTasksArgumentCaptor))
        assertTrue(showTasksArgumentCaptor.value.size == 3)
    }

    @Test
    fun loadActiveTasksFromRepositoryAndLoadIntoView() = runBlockingSilent {
        setTasksAvailable(tasksRepository, tasks)

        with(tasksPresenter) {
            // Given an initialized TasksPresenter with initialized tasks
            // When loading of Tasks is requested
            currentFiltering = TasksFilterType.ACTIVE_TASKS
            loadTasks(true)
        }

        // Then progress indicator is hidden and active tasks are shown in UI
        verify(tasksView).setLoadingIndicator(false)
        val showTasksArgumentCaptor = argumentCaptor<List<Task>>()
        verify(tasksView).showTasks(capture(showTasksArgumentCaptor))
        assertTrue(showTasksArgumentCaptor.value.size == 1)
    }

    @Test
    fun loadCompletedTasksFromRepositoryAndLoadIntoView() = runBlockingSilent {
        setTasksAvailable(tasksRepository, tasks)

        with(tasksPresenter) {
            // Given an initialized TasksPresenter with initialized tasks
            // When loading of Tasks is requested
            currentFiltering = TasksFilterType.COMPLETED_TASKS
            loadTasks(true)
        }

        // Then progress indicator is hidden and completed tasks are shown in UI
        verify(tasksView).setLoadingIndicator(false)
        val showTasksArgumentCaptor = argumentCaptor<List<Task>>()
        verify(tasksView).showTasks(capture(showTasksArgumentCaptor))
        assertTrue(showTasksArgumentCaptor.value.size == 2)
    }

    @Test
    fun clickOnFab_ShowsAddTaskUi() {
        // When adding a new task
        tasksPresenter.addNewTask()

        // Then add task UI is shown
        verify(tasksView).showAddTask()
    }

    @Test
    fun clickOnTask_ShowsDetailUi() {
        // Given a stubbed active task
        val requestedTask = Task("Details Requested", "For this task")

        // When open task details is requested
        tasksPresenter.openTaskDetails(requestedTask)

        // Then task detail UI is shown
        verify(tasksView).showTaskDetailsUi(anyMockito())
    }

    @Test
    fun completeTask_ShowsTaskMarkedComplete() = runBlockingSilent {
        // Given a stubbed task
        val task = Task("Details Requested", "For this task")

        // When task is marked as complete
        tasksPresenter.completeTask(task)

        // Then repository is called and task marked complete UI is shown
        verify(tasksRepository).completeTask(task)
        verify(tasksView).showTaskMarkedComplete()
    }

    @Test
    fun activateTask_ShowsTaskMarkedActive() = runBlockingSilent {
        // Given a stubbed completed task
        val task = Task("Details Requested", "For this task").apply { isCompleted = true }
        with(tasksPresenter) {
            loadTasks(true)

            // When task is marked as activated
            activateTask(task)
        }

        // Then repository is called and task marked active UI is shown
        verify(tasksRepository).activateTask(task)
        verify(tasksView).showTaskMarkedActive()
    }

    @Test
    fun unavailableTasks_ShowsError() = runBlockingSilent {
        setTasksNotAvailable(tasksRepository)

        with(tasksPresenter) {
            // When tasks are loaded
            currentFiltering = TasksFilterType.ALL_TASKS
            loadTasks(true)
        }

        // Then an error message is shown
        verify(tasksView).showLoadingTasksError()
    }

    private suspend fun setTasksAvailable(dataSource: TasksDataSource, tasks: List<Task>) {
        `when`(dataSource.getTasks()).thenReturn(Result.Success(tasks))
    }

    private suspend fun setTasksNotAvailable(dataSource: TasksDataSource) {
        `when`(dataSource.getTasks()).thenReturn(Result.Error(DataSourceException()))
    }
}
