package com.squareup.sample.dungeon

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.squareup.workflow1.ui.WorkflowLayout
import com.squareup.workflow1.ui.WorkflowUiExperimentalApi
import com.squareup.workflow1.ui.container.asRoot
import kotlinx.coroutines.flow.map

class DungeonActivity : AppCompatActivity() {

  @OptIn(WorkflowUiExperimentalApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Ignore config changes for now.
    val component = Component(this)
    val model: TimeMachineModel by viewModels { component.timeMachineModelFactory }

    val contentView = WorkflowLayout(this).apply {
      take(model.renderings.map { it.asRoot(component.viewRegistry) })
    }
    setContentView(contentView)
  }
}
