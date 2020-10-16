/*
 * Copyright 2020 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.sample.dungeon

import com.squareup.sample.dungeon.DungeonAppWorkflow.Props
import com.squareup.sample.dungeon.DungeonAppWorkflow.State
import com.squareup.sample.dungeon.DungeonAppWorkflow.State.ChoosingBoard
import com.squareup.sample.dungeon.DungeonAppWorkflow.State.LoadingBoardList
import com.squareup.sample.dungeon.DungeonAppWorkflow.State.PlayingGame
import com.squareup.sample.dungeon.board.Board
import com.squareup.workflow1.Snapshot
import com.squareup.workflow1.StatefulWorkflow
import com.squareup.workflow1.action
import com.squareup.workflow1.renderChild
import com.squareup.workflow1.runningWorker
import com.squareup.workflow1.ui.WorkflowUiExperimentalApi
import com.squareup.workflow1.ui.modal.AlertContainerScreen

@OptIn(WorkflowUiExperimentalApi::class)
class DungeonAppWorkflow(
  private val gameSessionWorkflow: GameSessionWorkflow,
  private val boardLoader: BoardLoader
) : StatefulWorkflow<Props, State, Nothing, AlertContainerScreen<Any>>() {

  data class Props(val paused: Boolean = false)

  sealed class State {
    object LoadingBoardList : State()
    data class ChoosingBoard(val boards: List<Pair<String, Board>>) : State()
    data class PlayingGame(val boardPath: BoardPath) : State()
  }

  data class DisplayBoardsListScreen(
    val boards: List<Board>,
    val onBoardSelected: (index: Int) -> Unit
  )

  override fun initialState(
    props: Props,
    snapshot: Snapshot?
  ): State = LoadingBoardList

  override fun RenderContext.render(): AlertContainerScreen<Any> = when (val state = state) {

    LoadingBoardList -> {
      runningWorker(boardLoader.loadAvailableBoards()) { displayBoards(it) }
      AlertContainerScreen(state)
    }

    is ChoosingBoard -> {
      val screen = DisplayBoardsListScreen(
          boards = state.boards.map { it.second },
          onBoardSelected = { index -> actionSink.send(selectBoard(index)) }
      )
      AlertContainerScreen(screen)
    }

    is PlayingGame -> {
      val sessionProps = GameSessionWorkflow.Props(state.boardPath, props.paused)
      val gameScreen = renderChild(gameSessionWorkflow, sessionProps)
      gameScreen
    }
  }

  override fun snapshotState(state: State): Snapshot? = null

  private fun displayBoards(boards: Map<String, Board>) = action {
    state = ChoosingBoard(boards.toList())
  }

  private fun selectBoard(index: Int) = action {
    // No-op if we're not in the ChoosingBoard state.
    val boards = (state as? ChoosingBoard)?.boards ?: return@action
    state = PlayingGame(boards[index].first)
  }
}
