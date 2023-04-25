package com.example.presentation.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletionChunk
import com.example.domain.usecase.SendChatUseCase
import com.example.presentation.state.GptState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(BetaOpenAI::class)
@HiltViewModel
class ChatGPTViewModel @Inject constructor(private val sendChatUseCase: SendChatUseCase) :
    ViewModel() {
    private val _gptSate = MutableStateFlow<GptState>(GptState.Loading)
    val gptState = _gptSate

    val input = mutableStateOf("")
    val chatResult = MutableStateFlow("")

    val onSend: (String) -> Unit = {
        input.value = ""
        sendChat(it)
    }
    val inputChange: (String) -> Unit = {
        input.value = it
    }

    private fun sendChat(chat: String) = viewModelScope.launch {
        sendChatUseCase.invoke(chat).catch {
            chatResult.value += it.message
            gptState.value = GptState.Error(it)
        }.collect {
            chatResult.value += it.choices[0].delta?.content ?: ""
            if (it.choices[0].finishReason == "stop") {
                gptState.value = GptState.End(it)
                return@collect
            }
        }
    }

    fun chatGenerationEnd() {
        chatResult.value += "\n \n \n"
    }
}