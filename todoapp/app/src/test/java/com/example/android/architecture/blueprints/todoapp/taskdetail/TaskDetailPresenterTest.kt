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

package com.example.android.architecture.blueprints.todoapp.taskdetail

import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository
import com.example.android.architecture.blueprints.todoapp.util.schedulers.BaseSchedulerProvider
import com.example.android.architecture.blueprints.todoapp.util.schedulers.ImmediateSchedulerProvider

import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

import rx.Observable

import org.mockito.Matchers.eq
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

/**
 * Unit tests for the implementation of [TaskDetailPresenter]
 */
class TaskDetailPresenterTest {

  @Mock
  private val mTasksRepository: TasksRepository? = null

  @Mock
  private val mTaskDetailView: TaskDetailContract.View? = null

  private var mSchedulerProvider: BaseSchedulerProvider? = null

  private var mTaskDetailPresenter: TaskDetailPresenter? = null

  @Before
  fun setup() {
    // Mockito has a very convenient way to inject mocks by using the @Mock annotation. To
    // inject the mocks in the test the initMocks method needs to be called.
    MockitoAnnotations.initMocks(this)

    // Make the sure that all schedulers are immediate.
    mSchedulerProvider = ImmediateSchedulerProvider()

    // The presenter won't update the view unless it's active.
    `when`(mTaskDetailView!!.isActive).thenReturn(true)
  }

  @Test
  fun createPresenter_setsThePresenterToView() {
    // Get a reference to the class under test
    mTaskDetailPresenter = TaskDetailPresenter(
        ACTIVE_TASK.id, mTasksRepository!!, mTaskDetailView!!, mSchedulerProvider!!)

    // Then the presenter is set to the view
    verify(mTaskDetailView).setPresenter(mTaskDetailPresenter!!)
  }

  @Test
  fun getActiveTaskFromRepositoryAndLoadIntoView() {
    // When tasks presenter is asked to open a task
    mTaskDetailPresenter = TaskDetailPresenter(
        ACTIVE_TASK.id, mTasksRepository!!, mTaskDetailView!!, mSchedulerProvider!!)
    setTaskAvailable(ACTIVE_TASK)
    mTaskDetailPresenter!!.subscribe()

    // Then task is loaded from model, callback is captured and progress indicator is shown
    verify(mTasksRepository).getTask(eq(ACTIVE_TASK.id))
    verify(mTaskDetailView).setLoadingIndicator(true)

    // Then progress indicator is hidden and title, description and completion status are shown
    // in UI
    verify(mTaskDetailView).setLoadingIndicator(false)
    verify(mTaskDetailView).showTitle(TITLE_TEST)
    verify(mTaskDetailView).showDescription(DESCRIPTION_TEST)
    verify(mTaskDetailView).showCompletionStatus(false)
  }

  @Test
  fun getCompletedTaskFromRepositoryAndLoadIntoView() {
    mTaskDetailPresenter = TaskDetailPresenter(
        COMPLETED_TASK.id, mTasksRepository!!, mTaskDetailView!!, mSchedulerProvider!!)
    setTaskAvailable(COMPLETED_TASK)
    mTaskDetailPresenter!!.subscribe()

    // Then task is loaded from model, callback is captured and progress indicator is shown
    verify(mTasksRepository).getTask(
        eq(COMPLETED_TASK.id))
    verify(mTaskDetailView).setLoadingIndicator(true)

    // Then progress indicator is hidden and title, description and completion status are shown
    // in UI
    verify(mTaskDetailView).setLoadingIndicator(false)
    verify(mTaskDetailView).showTitle(TITLE_TEST)
    verify(mTaskDetailView).showDescription(DESCRIPTION_TEST)
    verify(mTaskDetailView).showCompletionStatus(true)
  }

  @Test
  fun getUnknownTaskFromRepositoryAndLoadIntoView() {
    // When loading of a task is requested with an invalid task ID.
    mTaskDetailPresenter = TaskDetailPresenter(
        INVALID_TASK_ID, mTasksRepository!!, mTaskDetailView!!, mSchedulerProvider!!)
    mTaskDetailPresenter!!.subscribe()
    verify(mTaskDetailView).showMissingTask()
  }

  @Test
  fun deleteTask() {
    // Given an initialized TaskDetailPresenter with stubbed task
    val task = Task(TITLE_TEST, DESCRIPTION_TEST)

    // When the deletion of a task is requested
    mTaskDetailPresenter = TaskDetailPresenter(
        task.id, mTasksRepository!!, mTaskDetailView!!, mSchedulerProvider!!)
    mTaskDetailPresenter!!.deleteTask()

    // Then the repository and the view are notified
    verify(mTasksRepository).deleteTask(task.id)
    verify(mTaskDetailView).showTaskDeleted()
  }

  @Test
  fun completeTask() {
    // Given an initialized presenter with an active task
    val task = Task(TITLE_TEST, DESCRIPTION_TEST)
    mTaskDetailPresenter = TaskDetailPresenter(
        task.id, mTasksRepository!!, mTaskDetailView!!, mSchedulerProvider!!)
    setTaskAvailable(task)
    mTaskDetailPresenter!!.subscribe()

    // When the presenter is asked to complete the task
    mTaskDetailPresenter!!.completeTask()

    // Then a request is sent to the task repository and the UI is updated
    verify(mTasksRepository).completeTask(task.id)
    verify(mTaskDetailView).showTaskMarkedComplete()
  }

  @Test
  fun activateTask() {
    // Given an initialized presenter with a completed task
    val task = Task(TITLE_TEST, DESCRIPTION_TEST, true)
    mTaskDetailPresenter = TaskDetailPresenter(
        task.id, mTasksRepository!!, mTaskDetailView!!, mSchedulerProvider!!)
    setTaskAvailable(task)
    mTaskDetailPresenter!!.subscribe()

    // When the presenter is asked to activate the task
    mTaskDetailPresenter!!.activateTask()

    // Then a request is sent to the task repository and the UI is updated
    verify(mTasksRepository).activateTask(task.id)
    verify(mTaskDetailView).showTaskMarkedActive()
  }

  @Test
  fun activeTaskIsShownWhenEditing() {
    // When the edit of an ACTIVE_TASK is requested
    mTaskDetailPresenter = TaskDetailPresenter(
        ACTIVE_TASK.id, mTasksRepository!!, mTaskDetailView!!, mSchedulerProvider!!)
    mTaskDetailPresenter!!.editTask()

    // Then the view is notified
    verify(mTaskDetailView).showEditTask(ACTIVE_TASK.id)
  }

  @Test
  fun invalidTaskIsNotShownWhenEditing() {
    // When the edit of an invalid task id is requested
    mTaskDetailPresenter = TaskDetailPresenter(
        INVALID_TASK_ID, mTasksRepository!!, mTaskDetailView!!, mSchedulerProvider!!)
    mTaskDetailPresenter!!.editTask()

    // Then the edit mode is never started
    verify(mTaskDetailView, never()).showEditTask(INVALID_TASK_ID)
    // instead, the error is shown.
    verify(mTaskDetailView).showMissingTask()
  }

  private fun setTaskAvailable(task: Task) {
    `when`(mTasksRepository!!.getTask(eq(task.id))).thenReturn(Observable.just(task))
  }

  companion object {

    val TITLE_TEST = "title"

    val DESCRIPTION_TEST = "description"

    val INVALID_TASK_ID = ""

    val ACTIVE_TASK = Task(TITLE_TEST, DESCRIPTION_TEST)

    val COMPLETED_TASK = Task(TITLE_TEST, DESCRIPTION_TEST, true)
  }

}
