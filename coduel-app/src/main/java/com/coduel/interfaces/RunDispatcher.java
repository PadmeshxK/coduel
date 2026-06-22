package com.coduel.interfaces;

import com.coduel.model.message.RunTask;

/**
 * Port: hands an ephemeral code-run off to the async execution pipeline. The transport is pluggable
 * (RabbitMQ today) — the Dto depends on this, not on the broker.
 */
public interface RunDispatcher {

    void dispatch(RunTask task);
}
