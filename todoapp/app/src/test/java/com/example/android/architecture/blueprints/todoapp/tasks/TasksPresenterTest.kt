/*
 * Copyright 2016, The Android Open Source Project
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

import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository
import com.example.android.architecture.blueprints.todoapp.util.schedulers.BaseSchedulerProvider
import com.example.android.architecture.blueprints.todoapp.util.schedulers.ImmediateSchedulerProvider
import com.google.common.collect.Lists

import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

import rx.Observable

import org.mockito.Matchers.any
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

/**
 * Unit tests for the implementation of [TasksPresenter]
 */
class TasksPresenterTest {

  @Mock
  private val mTasksRepository: TasksRepository? = null

  @Mock
  private val mTasksView: TasksContract.View? = null

  private var mSchedulerProvider: BaseSchedulerProvider? = null

  private var mTasksPresenter: TasksPresenter? = null

  @Before
  fun setupTasksPresenter() {
    // Mockito has a very convenient way to inject mocks by using the @Mock annotation. To
    // inject the mocks in the test the initMocks method needs to be called.
    MockitoAnnotations.initMocks(this)

    // Make the sure that all schedulers are immediate.
    mSchedulerProvider = ImmediateSchedulerProvider()

    // Get a reference to the class under test
    mTasksPresenter = TasksPresenter(mTasksRepository!!, mTasksView!!, mSchedulerProvider!!)

    // The presenter won't update the view unless it's active.
    `when`(mTasksView.isActive).thenReturn(true)

    // We subscribe the tasks to 3, with one active and two completed
    TASKS = Lists.newArrayList(Task("Title1", "Description1"),
        Task("Title2", "Description2", true), Task("Title3", "Description3", true))
  }

  @Test
  fun createPresenter_setsThePresenterToView() {
    // Get a reference to the class under test
    mTasksPresenter = TasksPresenter(mTasksRepository!!, mTasksView!!, mSchedulerProvider!!)

    // Then the presenter is set to the view
      verify(mTasksView).setPresenter(mTasksPresenter!!)
  }

  @Test
  fun loadAllTasksFromRepositoryAndLoadIntoView() {
    // Given an initialized TasksPresenter with initialized tasks
    `when`(mTasksRepository!!.tasks).thenReturn(Observable.just<List<Task>>(TASKS))
    // When loading of Tasks is requested
    mTasksPresenter!!.filtering = TasksFilterType.ALL_TASKS
    mTasksPresenter!!.loadTasks(true)

    // Then progress indicator is shown
    verify(mTasksView)?.setLoadingIndicator(true)
    // Then progress indicator is hidden and all tasks are shown in UI
    verify(mTasksView)?.setLoadingIndicator(false)
  }

  @Test
  fun loadActiveTasksFromRepositoryAndLoadIntoView() {
    // Given an initialized TasksPresenter with initialized tasks
    `when`(mTasksRepository!!.tasks).thenReturn(Observable.just<List<Task>>(TASKS))
    // When loading of Tasks is requested
    mTasksPresenter!!.filtering = TasksFilterType.ACTIVE_TASKS
    mTasksPresenter!!.loadTasks(true)

    // Then progress indicator is hidden and active tasks are shown in UI
    verify(mTasksView)?.setLoadingIndicator(false)
  }

  @Test
  fun loadCompletedTasksFromRepositoryAndLoadIntoView() {
    // Given an initialized TasksPresenter with initialized tasks
    `when`(mTasksRepository!!.tasks).thenReturn(Observable.just<List<Task>>(TASKS))
    // When loading of Tasks is requested
    mTasksPresenter!!.filtering = TasksFilterType.COMPLETED_TASKS
    mTasksPresenter!!.loadTasks(true)

    // Then progress indicator is hidden and completed tasks are shown in UI
    verify(mTasksView)?.setLoadingIndicator(false)
  }

  @Test
  fun clickOnFab_ShowsAddTaskUi() {
    // When adding a new task
    mTasksPresenter!!.addNewTask()

    // Then add task UI is shown
    verify(mTasksView)?.showAddTask()
  }

  @Test
  fun clickOnTask_ShowsDetailUi() {
    // Given a stubbed active task
    val requestedTask = Task("Details Requested", "For this task")

    // When open task details is requested
    mTasksPresenter!!.openTaskDetails(requestedTask)

    // Then task detail UI is shown
    verify(mTasksView)?.showTaskDetailsUi(any(String::class.java))
  }

  @Test
  fun completeTask_ShowsTaskMarkedComplete() {
    // Given a stubbed task
    val task = Task("Details Requested", "For this task")
    // And no tasks available in the repository
    `when`(mTasksRepository!!.tasks).thenReturn(Observable.empty<List<Task>>())

    // When task is marked as complete
    mTasksPresenter!!.completeTask(task)

    // Then repository is called and task marked complete UI is shown
    verify(mTasksRepository).completeTask(task)
    verify(mTasksView)?.showTaskMarkedComplete()
  }

  @Test
  fun activateTask_ShowsTaskMarkedActive() {
    // Given a stubbed completed task
    val task = Task("Details Requested", "For this task", true)
    // And no tasks available in the repository
    `when`(mTasksRepository!!.tasks).thenReturn(Observable.empty<List<Task>>())
    mTasksPresenter!!.loadTasks(true)

    // When task is marked as activated
    mTasksPresenter!!.activateTask(task)

    // Then repository is called and task marked active UI is shown
    verify(mTasksRepository).activateTask(task)
    verify(mTasksView)?.showTaskMarkedActive()
  }

  @Test
  fun errorLoadingTasks_ShowsError() {
    // Given that no tasks are available in the repository
    `when`(mTasksRepository!!.tasks).thenReturn(Observable.error<List<Task>>(Exception()))

    // When tasks are loaded
    mTasksPresenter!!.filtering = TasksFilterType.ALL_TASKS
    mTasksPresenter!!.loadTasks(true)

    // Then an error message is shown
    verify(mTasksView)?.showLoadingTasksError()
  }

  companion object {

    private var TASKS: List<Task>? = null
  }
}
