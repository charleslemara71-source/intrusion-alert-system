package com.ias.engine;

import java.util.Optional;
import com.ias.model.Alert;
import com.ias.model.LogEvent;

public interface Rule {

    String name();

    // Look at one event. Return an alert if it is suspicious, or nothing if it is fine.
    Optional<Alert> evaluate(LogEvent event);
}