package com.ongarand.gymtracker.data

object Seed {
    val MUSCLES = listOf("Dada", "Punggung", "Bahu", "Lengan", "Kaki", "Core", "Lainnya")
    val KINDS = listOf("Compound", "Isolasi")

    private data class S(val name: String, val muscle: String, val kind: String, val equipment: String)

    private val exercises = listOf(
        S("Bench Press", "Dada", "Compound", "Barbell"),
        S("Incline Dumbbell Press", "Dada", "Compound", "Dumbbell"),
        S("Chest Fly", "Dada", "Isolasi", "Dumbbell"),
        S("Push Up", "Dada", "Compound", "Bodyweight"),
        S("Dips", "Dada", "Compound", "Bodyweight"),
        S("Deadlift", "Punggung", "Compound", "Barbell"),
        S("Barbell Row", "Punggung", "Compound", "Barbell"),
        S("Lat Pulldown", "Punggung", "Compound", "Cable"),
        S("Pull Up", "Punggung", "Compound", "Bodyweight"),
        S("Seated Cable Row", "Punggung", "Compound", "Cable"),
        S("Face Pull", "Punggung", "Isolasi", "Cable"),
        S("Overhead Press", "Bahu", "Compound", "Barbell"),
        S("Lateral Raise", "Bahu", "Isolasi", "Dumbbell"),
        S("Rear Delt Fly", "Bahu", "Isolasi", "Dumbbell"),
        S("Barbell Curl", "Lengan", "Isolasi", "Barbell"),
        S("Hammer Curl", "Lengan", "Isolasi", "Dumbbell"),
        S("Triceps Pushdown", "Lengan", "Isolasi", "Cable"),
        S("Skull Crusher", "Lengan", "Isolasi", "Barbell"),
        S("Overhead Triceps Extension", "Lengan", "Isolasi", "Dumbbell"),
        S("Squat", "Kaki", "Compound", "Barbell"),
        S("Leg Press", "Kaki", "Compound", "Machine"),
        S("Romanian Deadlift", "Kaki", "Compound", "Barbell"),
        S("Leg Extension", "Kaki", "Isolasi", "Machine"),
        S("Leg Curl", "Kaki", "Isolasi", "Machine"),
        S("Lunge", "Kaki", "Compound", "Dumbbell"),
        S("Standing Calf Raise", "Kaki", "Isolasi", "Machine"),
        S("Plank", "Core", "Isolasi", "Bodyweight"),
        S("Hanging Leg Raise", "Core", "Isolasi", "Bodyweight"),
        S("Cable Crunch", "Core", "Isolasi", "Cable")
    )

    private val templates = listOf(
        Triple("Push", listOf("Bench Press", "Incline Dumbbell Press", "Overhead Press", "Lateral Raise", "Triceps Pushdown"), "1"),
        Triple("Pull", listOf("Deadlift", "Pull Up", "Barbell Row", "Face Pull", "Barbell Curl"), "3"),
        Triple("Legs", listOf("Squat", "Romanian Deadlift", "Leg Press", "Leg Curl", "Standing Calf Raise"), "5"),
        Triple("Upper", listOf("Bench Press", "Barbell Row", "Overhead Press", "Lat Pulldown", "Hammer Curl", "Triceps Pushdown"), ""),
        Triple("Lower", listOf("Squat", "Romanian Deadlift", "Lunge", "Leg Extension", "Standing Calf Raise"), ""),
        Triple("Full Body", listOf("Squat", "Bench Press", "Barbell Row", "Overhead Press", "Plank"), "")
    )

    suspend fun run(dao: GymDao) {
        exercises.forEach {
            dao.insertExercise(Exercise(name = it.name, muscle = it.muscle, kind = it.kind, equipment = it.equipment))
        }
        val byName = dao.exercisesOnce().associate { it.name to it.id }
        templates.forEach { (name, names, days) ->
            dao.upsertTemplate(
                Template(
                    name = name,
                    exerciseIds = names.mapNotNull { byName[it] }.joinToString(","),
                    days = days
                )
            )
        }
    }
}
