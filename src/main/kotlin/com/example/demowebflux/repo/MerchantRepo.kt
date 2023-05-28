package com.example.demowebflux.repo

import com.example.jooq.Tables.MERCHANT
import com.example.jooq.tables.pojos.Merchant
import com.example.jooq.tables.records.MerchantRecord
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class MerchantRepo : AbstractCrudRepo<UUID, Merchant, MerchantRecord>(
    MERCHANT, MERCHANT.ID, ::Merchant
)