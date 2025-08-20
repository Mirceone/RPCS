package com.mirceone.core;

import java.io.IOException;
import java.util.List;

public interface Command {
    String name();                       // e.g., "suspend"
    String description();                // short help line
    default List<String> aliases() { return List.of(); }
    void run(String[] args) throws IOException, InterruptedException, Exec.CommandFailedException;
}
