package com.paidy.forex.domain

sealed class Either<out L, out R> {
    data class Left<out L>(val value: L) : Either<L, Nothing>()
    data class Right<out R>(val value: R) : Either<Nothing, R>()

    fun isLeft(): Boolean = this is Left
    fun isRight(): Boolean = this is Right

    fun <T> fold(onLeft: (L) -> T, onRight: (R) -> T): T = when (this) {
        is Left -> onLeft(value)
        is Right -> onRight(value)
    }

    fun <T> map(transform: (R) -> T): Either<L, T> = when (this) {
        is Left -> Left(value)
        is Right -> Right(transform(value))
    }

    fun <T> flatMap(transform: (R) -> Either<@UnsafeVariance L, T>): Either<L, T> = when (this) {
        is Left -> Left(value)
        is Right -> transform(value)
    }

    fun getOrNull(): R? = if (this is Right) value else null
}