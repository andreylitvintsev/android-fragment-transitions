package com.github.andreylitvintsev.transitionbetweenfragments

import android.animation.Animator
import android.view.View
import androidx.annotation.IdRes
import androidx.fragment.app.FragmentManager

private enum class CommandType {
    TRANSACTION, ANIMATION
}

private class CommandDescriptor(val commandType: CommandType, var nextCommand: CommandDescriptor?, val command: () -> Unit)

class FragmentComposer(
    private val fragmentManager: FragmentManager
) {
    private val commands = arrayListOf<CommandDescriptor>() // TODO: можно избавиться
    private var currentCommandIndex = -1

    private var currentFragment: BaseFragment? = null

    fun add(@IdRes containerViewId: Int, baseFragment: BaseFragment): FragmentComposer {
        commands.add(CommandDescriptor(CommandType.TRANSACTION, null) {
            currentCommandIndex++

            currentFragment = baseFragment
            fragmentManager.beginTransaction().add(containerViewId, baseFragment).commit()

            if (hasNextCommand()) {
                if (checkNextCommandForType(CommandType.ANIMATION)) {
                    baseFragment.setOnResumeListener {
                        nextCommandDescriptor()?.command?.invoke()
                    }
                } else {
                    nextCommandDescriptor()?.command?.invoke()
                }
            }

            // TODO: нужен механизм выполнения команд со слушателями, запускаем следующую комманду после ее выполнения

            // TODO: при транзакциях мы выполняем моментально следующую, но только в случае если следующая комманда не является анимацией.
            // TODO: В таком случае мы дожидаемся отрисовки интерфейса.
            // TODO: Если текущая комманда это анимация, то мы выполняем следующую только после окончания выполнения этой анимации
        })
        if (commands.size > 1) {
            commands[commands.size - 2].nextCommand = commands[commands.size - 1]
        }
        return this@FragmentComposer
    }

    private fun nextCommandDescriptor(): CommandDescriptor? = commands.getOrNull(currentCommandIndex + 1)

    private fun hasNextCommand(): Boolean = (commands.size - 1) != currentCommandIndex

    private fun checkNextCommandForType(commandType: CommandType): Boolean {
        return (hasNextCommand()) && commands[currentCommandIndex + 1].commandType == commandType
    }

    fun remove(baseFragment: BaseFragment): FragmentComposer {
        commands.add(CommandDescriptor(CommandType.TRANSACTION, null) {
            currentFragment = baseFragment
            fragmentManager.beginTransaction().remove(baseFragment).commit()
        })
        if (commands.size > 1) {
            commands[commands.size - 2].nextCommand = commands[commands.size - 1]
        }
        return this@FragmentComposer
    }

    fun animate(animationCreating: (view: View, baseFragment: BaseFragment) -> Animator): FragmentComposer {
        commands.add(CommandDescriptor(CommandType.ANIMATION, null) {
            animationCreating.invoke()
        })
    }

    fun letsGo() {
        var currentCommand: CommandDescriptor? = commands[0]


    }

}
