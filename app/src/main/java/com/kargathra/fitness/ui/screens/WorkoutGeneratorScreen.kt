package com.kargathra.fitness.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kargathra.fitness.data.model.*
import com.kargathra.fitness.data.sample.SampleData
import com.kargathra.fitness.ui.components.KCard
import com.kargathra.fitness.ui.components.SectionLabel
import com.kargathra.fitness.ui.components.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * AI workout generator. Collects goal, experience, days/week, session length and
 * target muscles, then calls the Claude API to produce a structured routine.
 * The result is previewed in-screen and the user can load it onto the Workout page.
 */
@Composable
fun WorkoutGeneratorScreen(
    anthropicApiKey: String,
    onBack: () -> Unit,
    onLoadRoutine: (Routine) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    // Form state
    var goal by remember { mutableStateOf(Goal.HYPERTROPHY) }
    var experience by remember { mutableStateOf(Experience.INTERMEDIATE) }
    var daysPerWeek by remember { mutableIntStateOf(4) }
    var sessionMinutes by remember { mutableIntStateOf(45) }
    val focusAreas = remember { mutableStateListOf<MuscleGroup>() }

    // Result state
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var generatedRoutine by remember { mutableStateOf<Routine?>(null) }

    val availableFocusAreas = MuscleGroup.entries.filter { it != MuscleGroup.CARDIO }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // ── Goal ──────────────────────────────────────────────────────────────
        SectionLabel("Your goal")
        KCard {
            Goal.entries.forEach { g ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = goal == g, onClick = { goal = g })
                    Spacer(Modifier.width(8.dp))
                    Text(g.display, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        // ── Experience ────────────────────────────────────────────────────────
        SectionLabel("Experience level")
        KCard {
            Experience.entries.forEach { e ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = experience == e, onClick = { experience = e })
                    Spacer(Modifier.width(8.dp))
                    Text(e.display, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        // ── Days / time ───────────────────────────────────────────────────────
        SectionLabel("Schedule")
        KCard {
            Text("Days per week: $daysPerWeek", style = MaterialTheme.typography.titleMedium)
            Slider(
                value = daysPerWeek.toFloat(),
                onValueChange = { daysPerWeek = it.toInt() },
                valueRange = 2f..6f,
                steps = 3
            )
            Spacer(Modifier.height(8.dp))
            Text("Session length: ~$sessionMinutes min", style = MaterialTheme.typography.titleMedium)
            Slider(
                value = sessionMinutes.toFloat(),
                onValueChange = { sessionMinutes = (it / 5).toInt() * 5 },
                valueRange = 20f..90f,
                steps = 13
            )
        }

        // ── Focus areas ───────────────────────────────────────────────────────
        SectionLabel("Target muscles (optional)")
        KCard {
            Text(
                "Leave blank for a balanced full-body session.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            availableFocusAreas.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { mg ->
                        val selected = mg in focusAreas
                        FilterChip(
                            selected = selected,
                            onClick = {
                                if (selected) focusAreas.remove(mg)
                                else focusAreas.add(mg)
                            },
                            label = { Text(mg.display) },
                            leadingIcon = if (selected) {
                                { Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Pad odd row
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        // ── Generate button ───────────────────────────────────────────────────
        errorMsg?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Button(
            onClick = {
                errorMsg = null
                isLoading = true
                generatedRoutine = null
                if (anthropicApiKey.isBlank()) {
                    errorMsg = "Add your Anthropic API key in Settings to use the generator."
                    isLoading = false
                    return@Button
                }
                scope.launch {
                    try {
                        val routine = callClaudeForRoutine(
                            anthropicApiKey = anthropicApiKey,
                            goal = goal,
                            experience = experience,
                            sessionMinutes = sessionMinutes,
                            focusAreas = focusAreas.toList()
                        )
                        generatedRoutine = routine
                    } catch (e: Exception) {
                        errorMsg = "Generation failed: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp).padding(end = 8.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Text(if (isLoading) "Building your workout…" else "Build my workout")
        }

        // ── Result preview ────────────────────────────────────────────────────
        AnimatedVisibility(visible = generatedRoutine != null) {
            generatedRoutine?.let { routine ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionLabel("Your workout")
                    KCard {
                        Text(routine.title, style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "${routine.items.size} exercises · ~${routine.estimatedMinutes} min",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            routine.focus.take(3).forEach { Tag(it.display) }
                        }
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(8.dp))
                        routine.items.forEach { item ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                Text(
                                    "• ${item.exercise.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "${item.sets} × ${item.repTarget.first}–${item.repTarget.last}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                onLoadRoutine(routine)
                                onBack()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Load onto Workout page")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back to Programs")
        }
    }
}

// ── Claude API call ──────────────────────────────────────────────────────────

private suspend fun callClaudeForRoutine(
    anthropicApiKey: String,
    goal: Goal,
    experience: Experience,
    sessionMinutes: Int,
    focusAreas: List<MuscleGroup>
): Routine = withContext(Dispatchers.IO) {

    val equipment = SampleData.ownedEquipment.joinToString(", ") { it.display }
    val focus = if (focusAreas.isEmpty()) "balanced full body"
                else focusAreas.joinToString(", ") { it.display }
    val exerciseNames = SampleData.allRoutines
        .flatMap { it.items }
        .map { it.exercise.name }
        .distinct()
        .joinToString(", ")

    val prompt = """
You are a personal trainer building a single gym session for one person.

User profile:
- Goal: ${goal.display}
- Experience: ${experience.display}
- Session length: ~$sessionMinutes minutes
- Target muscles: $focus
- Available equipment: $equipment

You MUST only use exercises from this exact list (use these exact names):
$exerciseNames

Respond with ONLY a valid JSON object — no markdown, no explanation, nothing else.
Schema:
{
  "title": "string",
  "estimatedMinutes": number,
  "focus": ["MuscleGroup display name", ...],
  "exercises": [
    {
      "name": "exact exercise name from the list above",
      "sets": number,
      "repsMin": number,
      "repsMax": number,
      "restSeconds": number
    }
  ]
}

Rules:
- Include 4–7 exercises appropriate for the session length and goal.
- For cardio exercises (Spin Bike Intervals, Incline Treadmill Walk) use repsMin=0, repsMax=0.
- Rest 90–150s for compound, 60–90s for isolation, goal-appropriate.
- Heavier loads / lower reps for Strength, moderate for Hypertrophy, higher reps for Fat loss.
""".trimIndent()

    val requestBody = JSONObject().apply {
        put("model", "claude-sonnet-4-6")
        put("max_tokens", 1000)
        put("messages", org.json.JSONArray().apply {
            put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            })
        })
    }

    val url = java.net.URL("https://api.anthropic.com/v1/messages")
    val conn = url.openConnection() as java.net.HttpURLConnection
    conn.requestMethod = "POST"
    conn.setRequestProperty("Content-Type", "application/json")
    conn.setRequestProperty("x-api-key", anthropicApiKey)
    conn.setRequestProperty("anthropic-version", "2023-06-01")
    conn.doOutput = true
    conn.connectTimeout = 15_000
    conn.readTimeout = 30_000
    conn.outputStream.use { it.write(requestBody.toString().toByteArray()) }

    val code = conn.responseCode
    if (code != 200) {
        val err = (conn.errorStream ?: conn.inputStream)?.bufferedReader()?.readText().orEmpty()
        // Surface the API's own error message where possible
        val detail = runCatching {
            JSONObject(err).getJSONObject("error").getString("message")
        }.getOrDefault(err.take(140))
        throw IllegalStateException("HTTP $code — $detail")
    }

    val responseText = conn.inputStream.bufferedReader().readText()
    val json = JSONObject(responseText)

    // Extract the text content block
    val content = json.getJSONArray("content").getJSONObject(0).getString("text").trim()
    parseRoutineJson(content)
}

private fun parseRoutineJson(json: String): Routine {
    val obj = JSONObject(json)
    val exerciseArray = obj.getJSONArray("exercises")

    // Build a name→Exercise lookup from the full library
    val library = SampleData.allRoutines.flatMap { it.items.map { i -> i.exercise } }
        .distinctBy { it.id }
        .associateBy { it.name.lowercase() }

    val items = mutableListOf<RoutineItem>()
    for (i in 0 until exerciseArray.length()) {
        val ex = exerciseArray.getJSONObject(i)
        val name = ex.getString("name")
        val exercise = library[name.lowercase()] ?: continue // skip unknown
        items.add(
            RoutineItem(
                exercise = exercise,
                sets = ex.getInt("sets"),
                repTarget = ex.getInt("repsMin")..ex.getInt("repsMax"),
                restSeconds = ex.getInt("restSeconds")
            )
        )
    }

    if (items.isEmpty()) throw IllegalStateException("No recognisable exercises in response")

    val focusArray = obj.getJSONArray("focus")
    val focusGroups = (0 until focusArray.length())
        .mapNotNull { idx ->
            MuscleGroup.entries.firstOrNull {
                it.display.equals(focusArray.getString(idx), ignoreCase = true)
            }
        }

    return Routine(
        id = "ai_${System.currentTimeMillis()}",
        title = obj.getString("title"),
        focus = focusGroups,
        items = items,
        estimatedMinutes = obj.getInt("estimatedMinutes"),
        isGenerated = true
    )
}
