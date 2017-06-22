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

package com.example.android.architecture.blueprints.todoapp.data

import com.google.common.base.Strings
import java.util.*

/**
 * Immutable model class for a Task.
 *
 * Use this constructor to specify a completed Task if the Task already has an id (copy of
 * another Task).

 * @param title       title of the task
 * *
 * @param description description of the task
 * *
 * @param id          id of the task
 * *
 * @param isCompleted   true if the task is completed, false if it's active
 */
data class Task(val title: String?, val description: String?,
                val id: String = UUID.randomUUID().toString(), val isCompleted: Boolean = false ){


  val titleForList: String?
    get() {
      if (!Strings.isNullOrEmpty(title)) {
        return title
      } else {
        return description
      }
    }

  val isEmpty: Boolean
    get() = Strings.isNullOrEmpty(title) && Strings.isNullOrEmpty(description)

  val isActive: Boolean
    get() = !isCompleted

}

