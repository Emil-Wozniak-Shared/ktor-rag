package pl.model

import io.ktor.server.plugins.requestvalidation.*
import kotlin.reflect.full.memberProperties

interface Validated {
    private val reasons: MutableList<ValidationProblem>
        get() = mutableListOf()

    fun min(field: String, value: Number): MinimumValidationRule = this::class.memberProperties
        .find { it.name == field }
        ?.let { MinimumValidationRule(field,it.getter.call(this) as Number, value) }
        ?: error("Field $field not found")

    fun max(field: String, value: Number) {
        this::class.memberProperties
            .find { it.name == field }
            ?.let { MaximumValidationRule(field,it.getter.call(this) as Number, value) }
            ?: error("Field $field not found")
    }
    fun len(field: String, value: Int) {
        this::class.memberProperties
            .find { it.name == field }
            ?.let { LenValidationRule(field,it.getter.call(this) as String, value) }
            ?.let {
                if (!it.check()) {
                    reasons.add(ValidationProblem(it.field, it.msg()))
                }
            }
            ?: error("Field $field not found")
    }

    fun validate(): ValidationResult = run {
        val messages = messages()
        if (messages.isNotEmpty()) ValidationResult.Invalid(messages)
        else ValidationResult.Valid
    }

    private fun messages() = reasons.map {
        "<${it.name}> ${it.reason}"
    }
}

abstract class ValidationRule<T, R>(open val field: String, value: T, open val expected: R) {
    abstract fun check(): Boolean
    abstract fun msg(): String
}

class MinimumValidationRule(
    override val field: String,
    val value: Number,
    override val expected: Number
) : ValidationRule<Number, Number>(field, value, expected) {
    override fun check(): Boolean = when (value) {
        is Short -> value > expected.toShort()
        is Int -> value > expected.toInt()
        is Double -> value > expected.toDouble()
        is Float -> value > expected.toFloat()
        else -> false
    }

    override fun msg(): String = "Must be greater then $expected"
}

class LenValidationRule(
    override val field: String,
    val value: String,
    override val expected: Int
) : ValidationRule<String, Int>(field, value, expected) {
    override fun check(): Boolean = value.length >= expected


    override fun msg(): String = "Must have at least $expected characters"
}

class MaximumValidationRule(
    override val field: String,
    val value: Number,
    override val expected: Number
) : ValidationRule<Number, Number>(field, value, expected) {
    override fun check(): Boolean = when (value) {
        is Short -> value < expected.toShort()
        is Int -> value < expected.toInt()
        is Double -> value < expected.toDouble()
        is Float -> value < expected.toFloat()
        else -> false
    }

    override fun msg(): String = "Must be less then $expected"
}

class ValidationProblem(
    val name: String,
    val reason: String
)

abstract class Validator<T>(private val subject: T) {
    protected val reasons: MutableList<ValidationProblem> = mutableListOf()

    protected abstract fun check()

    fun validate(): ValidationResult = run {
        check()
        val messages = reasons.map {
            "<${it.name}> ${it.reason}"
        }
        if (messages.isNotEmpty()) ValidationResult.Invalid(messages)
        else ValidationResult.Valid
    }

    protected class ValidationProblem(
        val name: String,
        val reason: String
    )
}