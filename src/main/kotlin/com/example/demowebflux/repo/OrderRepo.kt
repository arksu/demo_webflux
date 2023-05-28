package com.example.demowebflux.repo

import com.example.jooq.Tables.ORDER
import com.example.jooq.tables.pojos.Order
import com.example.jooq.tables.records.OrderRecord
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class OrderRepo : AbstractCrudRepo<UUID, Order, OrderRecord>(
    ORDER, ORDER.ID, ::Order
)