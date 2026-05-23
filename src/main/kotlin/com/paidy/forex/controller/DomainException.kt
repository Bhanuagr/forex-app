package com.paidy.forex.controller

import com.paidy.forex.domain.DomainError

class DomainException(val error: DomainError) : RuntimeException()