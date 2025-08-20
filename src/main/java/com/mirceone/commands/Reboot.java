package com.mirceone.commands;

import com.mirceone.core.Command;
import com.mirceone.core.Exec;

import java.io.IOException;

public class Reboot implements Command {
    @Override public String name() { return "reboot"; }
    @Override public String description() { return "systemctl reboot"; }
    @Override public void run(String[] args) throws IOException, InterruptedException, Exec.CommandFailedException {
        Exec.run("systemctl", "reboot");
    }
}
