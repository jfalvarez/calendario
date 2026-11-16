package com.jfaf.calendario.data.repository

import com.jfaf.calendario.domain.repository.IdGenerator

class UuidIdGenerator : IdGenerator {
    override fun newId(): String = com.jfaf.calendario.data.repository.newId()
}
