package com.humblesolutions.indsphinx.repository

interface ConfigRepository {
    suspend fun getFormFrequencyMonths(): Int
}
