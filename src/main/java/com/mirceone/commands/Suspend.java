package com.mirceone.commands;

import com.mirceone.core.Command;
import com.mirceone.core.Exec;

import java.io.IOException;

public final class Suspend implements Command {
    @Override public String name() { return "suspend"; }
    @Override public String description() { return "systemctl suspend"; }
    @Override public void run(String[] args) throws IOException, InterruptedException, Exec.CommandFailedException {
        Exec.run("systemctl", "suspend");
    }
}
