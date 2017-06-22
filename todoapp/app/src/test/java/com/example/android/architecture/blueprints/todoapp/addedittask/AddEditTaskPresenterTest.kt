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

package com.example.android.architecture.blueprints.todoapp.addedittask

import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository
import com.example.android.architecture.blueprints.todoapp.util.schedulers.BaseSchedulerProvider
import com.example.android.architecture.blueprints.todoapp.util.schedulers.ImmediateSchedulerProvider

import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

import java.util.NoSuchElementException

import rx.Observable

import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.mockito.Matchers.any
import org.mockito.Matchers.eq
import org.mockito.Mockito
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

/**
 * Unit tests for the implementation of [AddEditTaskPresenter].
 */
class AddEditTaskPresenterTest {


  @Mock
  lateinit var mTasksRepository: TasksRepository

  @Mock
  lateinit var mAddEditTaskView: AddEditTaskContract.View

  private var mSchedulerProvider: BaseSchedulerProvider? = null

  private var mAddEditTaskPresenter: AddEditTaskPresenter? = null

  private fun <T> any(): T {
    Mockito.any<T>()
    return uninitialized()
  }
  private fun <T> uninitialized(): T = null as T

  @Before
  fun setupMocksAndView() {
    // Mockito has a very convenient way to inject mocks by using the @Mock annotation. To
    // inject the mocks in the test the initMocks method needs to be called.
    MockitoAnnotations.initMocks(this)

    mSchedulerProvider = ImmediateSchedulerProvider()

    // The presenter wont't update the view unless it's active.
    `when`(mAddEditTaskView!!.isActive).thenReturn(true)
  }

  @Test
  fun createPresenter_setsThePresenterToView() {
    // Get a reference to the class under test
    mAddEditTaskPresenter = AddEditTaskPresenter(
        null, mTasksRepository!!, mAddEditTaskView!!, true, mSchedulerProvider!!)

    // Then the presenter is set to the view
    verify(mAddEditTaskView).setPresenter(mAddEditTaskPresenter!!)
  }

  @Test
  fun saveNewTaskToRepository_showsSuccessMessageUi() {
    // Get a reference to the class under test
    mAddEditTaskPresenter = AddEditTaskPresenter(null, mTasksRepository!!, mAddEditTaskView!!, true, mSchedulerProvider!!)

    // When the presenter is asked to save a task
    mAddEditTaskPresenter!!.saveTask("New Task Title", "Some Task Description")

    // Then a task is saved in the repository and the view updated
    verify(mTasksRepository).saveTask(any()) // saved to the model
    verify(mAddEditTaskView).showTasksList() // shown in the UI
  }

  @Test
  fun saveTask_emptyTaskShowsErrorUi() {
    // Get a reference to the class under test
    mAddEditTaskPresenter = AddEditTaskPresenter(null, mTasksRepository!!, mAddEditTaskView!!, true, mSchedulerProvider!!)

    // When the presenter is asked to save an empty task
    mAddEditTaskPresenter!!.saveTask("", "")

    // Then an empty not error is shown in the UI
    verify(mAddEditTaskView).showEmptyTaskError()
  }

  @Test
  fun saveExistingTaskToRepository_showsSuccessMessageUi() {
    // Get a reference to the class under test
    mAddEditTaskPresenter = AddEditTaskPresenter(
        "1", mTasksRepository!!, mAddEditTaskView!!, true, mSchedulerProvider!!)

    // When the presenter is asked to save an existing task
    mAddEditTaskPresenter!!.saveTask("Existing Task Title", "Some Task Description")

    // Then a task is saved in the repository and the view updated
    verify(mTasksRepository).saveTask(any()) // saved to the model
    verify(mAddEditTaskView).showTasksList() // shown in the UI
  }

  @Test
  fun populateTask_callsRepoAndUpdatesViewOnSuccess() {
    val testTask = Task("TITLE", "DESCRIPTION")
    `when`(mTasksRepository!!.getTask(testTask.id)).thenReturn(Observable.just(testTask))

    // Get a reference to the class under test
    mAddEditTaskPresenter = AddEditTaskPresenter(testTask.id,
        mTasksRepository, mAddEditTaskView!!, true, mSchedulerProvider!!)

    // When the presenter is asked to populate an existing task
    mAddEditTaskPresenter!!.populateTask()

    // Then the task repository is queried and the view updated
    verify(mTasksRepository).getTask(eq(testTask.id))

    verify(mAddEditTaskView).setTitle(testTask.title!!)
    verify(mAddEditTaskView).setDescription(testTask.description!!)
    assertThat(mAddEditTaskPresenter!!.isDataMissing, `is`(false))
  }

  @Test
  fun populateTask_callsRepoAndUpdatesViewOnError() {
    val testTask = Task("TITLE", "DESCRIPTION")
    `when`(mTasksRepository!!.getTask(testTask.id)).thenReturn(
        Observable.error<Task>(NoSuchElementException()))

    // Get a reference to the class under test
    mAddEditTaskPresenter = AddEditTaskPresenter(testTask.id,
        mTasksRepository, mAddEditTaskView!!, true, mSchedulerProvider!!)

    // When the presenter is asked to populate an existing task
    mAddEditTaskPresenter!!.populateTask()

    // Then the task repository is queried and the view updated
    verify(mTasksRepository).getTask(eq(testTask.id))

    verify(mAddEditTaskView).showEmptyTaskError()
    verify(mAddEditTaskView, never()).setTitle(testTask.title!!)
    verify(mAddEditTaskView, never()).setDescription(testTask.description!!)
  }
}
