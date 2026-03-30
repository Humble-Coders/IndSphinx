package com.humblesolutions.indsphinx.repository

import com.humblesolutions.indsphinx.model.Complaint

interface ComplaintRepository {
    suspend fun submitComplaint(complaint: Complaint): String
}
