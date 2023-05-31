package com.example.demowebflux.scheduler

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class TronScheduler {

    @Scheduled(fixedDelay = 5000)
    fun update() {
    }
}