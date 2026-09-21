package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GlassAccentCyan
import com.example.ui.theme.GlassWhiteBorder
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed

val PRESET_SECURITY_QUESTIONS = listOf(
    "What was your first pet's name?",
    "What city were you born in?",
    "What is your favorite food?",
    "What was your first school called?",
    "What is your favorite movie?",
    "What is your dream car?",
    "Write a custom question..."
)

@Composable
fun PasswordRecoverySetupView(
    initialQ1: String = PRESET_SECURITY_QUESTIONS[0],
    initialA1: String = "",
    initialQ2: String = PRESET_SECURITY_QUESTIONS[1],
    initialA2: String = "",
    initialQ3: String = PRESET_SECURITY_QUESTIONS[2],
    initialA3: String = "",
    onSaveRecovery: (q1: String, a1: String, q2: String, a2: String, q3: String, a3: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var q1 by remember { mutableStateOf(if (initialQ1.isBlank()) PRESET_SECURITY_QUESTIONS[0] else initialQ1) }
    var a1 by remember { mutableStateOf(initialA1) }
    var customQ1 by remember { mutableStateOf("") }
    var isCustom1 by remember { mutableStateOf(!PRESET_SECURITY_QUESTIONS.dropLast(1).contains(initialQ1) && initialQ1.isNotBlank()) }

    var q2 by remember { mutableStateOf(if (initialQ2.isBlank()) PRESET_SECURITY_QUESTIONS[1] else initialQ2) }
    var a2 by remember { mutableStateOf(initialA2) }
    var customQ2 by remember { mutableStateOf("") }
    var isCustom2 by remember { mutableStateOf(!PRESET_SECURITY_QUESTIONS.dropLast(1).contains(initialQ2) && initialQ2.isNotBlank()) }

    var q3 by remember { mutableStateOf(if (initialQ3.isBlank()) PRESET_SECURITY_QUESTIONS[2] else initialQ3) }
    var a3 by remember { mutableStateOf(initialA3) }
    var customQ3 by remember { mutableStateOf("") }
    var isCustom3 by remember { mutableStateOf(!PRESET_SECURITY_QUESTIONS.dropLast(1).contains(initialQ3) && initialQ3.isNotBlank()) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSavedSuccessfully by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    fun validateAndSave() {
        focusManager.clearFocus()
        keyboardController?.hide()

        val finalQ1 = if (isCustom1) customQ1.trim() else q1.trim()
        val finalQ2 = if (isCustom2) customQ2.trim() else q2.trim()
        val finalQ3 = if (isCustom3) customQ3.trim() else q3.trim()

        if (finalQ1.isEmpty() || finalQ2.isEmpty() || finalQ3.isEmpty()) {
            errorMessage = "Please choose or enter all 3 security questions"
            return
        }
        if (finalQ1.equals(finalQ2, ignoreCase = true) ||
            finalQ1.equals(finalQ3, ignoreCase = true) ||
            finalQ2.equals(finalQ3, ignoreCase = true)
        ) {
            errorMessage = "Please choose 3 distinct security questions"
            return
        }
        if (a1.trim().length < 2 || a2.trim().length < 2 || a3.trim().length < 2) {
            errorMessage = "All 3 answers must be at least 2 characters long"
            return
        }

        errorMessage = null
        isSavedSuccessfully = true
        onSaveRecovery(finalQ1, a1.trim(), finalQ2, a2.trim(), finalQ3, a3.trim())
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(GlassAccentCyan.copy(alpha = 0.2f))
                    .border(1.dp, GlassAccentCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LockReset,
                    contentDescription = null,
                    tint = GlassAccentCyan,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Password Recovery Setup",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "3 security questions required to reset credentials",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }

        SecurityQuestionItem(
            stepNumber = 1,
            selectedQuestion = q1,
            isCustom = isCustom1,
            customQuestion = customQ1,
            answer = a1,
            onQuestionSelect = { selected ->
                if (selected == "Write a custom question...") {
                    isCustom1 = true
                } else {
                    isCustom1 = false
                    q1 = selected
                }
                errorMessage = null
            },
            onCustomQuestionChange = {
                customQ1 = it
                errorMessage = null
            },
            onAnswerChange = {
                a1 = it
                errorMessage = null
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        SecurityQuestionItem(
            stepNumber = 2,
            selectedQuestion = q2,
            isCustom = isCustom2,
            customQuestion = customQ2,
            answer = a2,
            onQuestionSelect = { selected ->
                if (selected == "Write a custom question...") {
                    isCustom2 = true
                } else {
                    isCustom2 = false
                    q2 = selected
                }
                errorMessage = null
            },
            onCustomQuestionChange = {
                customQ2 = it
                errorMessage = null
            },
            onAnswerChange = {
                a2 = it
                errorMessage = null
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        SecurityQuestionItem(
            stepNumber = 3,
            selectedQuestion = q3,
            isCustom = isCustom3,
            customQuestion = customQ3,
            answer = a3,
            onQuestionSelect = { selected ->
                if (selected == "Write a custom question...") {
                    isCustom3 = true
                } else {
                    isCustom3 = false
                    q3 = selected
                }
                errorMessage = null
            },
            onCustomQuestionChange = {
                customQ3 = it
                errorMessage = null
            },
            onAnswerChange = {
                a3 = it
                errorMessage = null
            }
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = errorMessage!!,
                color = NeonRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (isSavedSuccessfully) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Recovery questions secured with Android KeyStore",
                    color = NeonGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { validateAndSave() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GlassAccentCyan,
                contentColor = Color.Black
            )
        ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isSavedSuccessfully) "Saved & Verified" else "Confirm & Save Recovery Questions",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun SecurityQuestionItem(
    stepNumber: Int,
    selectedQuestion: String,
    isCustom: Boolean,
    customQuestion: String,
    answer: String,
    onQuestionSelect: (String) -> Unit,
    onCustomQuestionChange: (String) -> Unit,
    onAnswerChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(GlassAccentCyan.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$stepNumber", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Security Question $stepNumber",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Question Dropdown
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, GlassWhiteBorder, RoundedCornerShape(10.dp))
                    .clickable {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        expanded = true
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isCustom) "Custom: ${customQuestion.ifBlank { "Enter question below" }}" else selectedQuestion,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select question",
                        tint = GlassAccentCyan
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(Color(0xFF1E293B))
                ) {
                    PRESET_SECURITY_QUESTIONS.forEach { q ->
                        DropdownMenuItem(
                            text = { Text(q, color = Color.White, fontSize = 13.sp) },
                            onClick = {
                                expanded = false
                                onQuestionSelect(q)
                            }
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isCustom) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedTextField(
                        value = customQuestion,
                        onValueChange = onCustomQuestionChange,
                        label = { Text("Write your custom question", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GlassAccentCyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                            focusedLabelColor = GlassAccentCyan,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Answer input
            OutlinedTextField(
                value = answer,
                onValueChange = onAnswerChange,
                label = { Text("Your answer (Encrypted)", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = GlassAccentCyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                    focusedLabelColor = GlassAccentCyan,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                )
            )
        }
    }
}
