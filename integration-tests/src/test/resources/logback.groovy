package com.kalixia.grapi

import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender

import static ch.qos.logback.classic.Level.DEBUG
import static ch.qos.logback.classic.Level.INFO
import static ch.qos.logback.classic.Level.WARN

appender("STDOUT", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%d{HH:mm:ss.SSS} [%X{CLIENT_ADDR}/%X{REQUEST_ID}] [%X{USER}] [%thread] %-5level %logger{36} - %msg%n"
    }
    withJansi = true
}

logger "com.kalixia.grapi", DEBUG
logger "org.apache.shiro", INFO
logger "io.netty", INFO
logger "io.reactivex.netty", DEBUG
logger "org.hibernate.validator", WARN

root(WARN, ["STDOUT"])