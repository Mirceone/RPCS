package com.mirceone.commands;

import com.mirceone.core.Command;
import com.mirceone.core.Exec;

import java.io.IOException;

public final class Poweroff  implements Command {
    @Override public String name() { return "poweroff"; }
    @Override public String description() { return "systemctl reboot"; }
    @Override public void run(String[] args) throws IOException, InterruptedException, Exec.CommandFailedException {
        Exec.run("systemctl", "poweroff");
    }
}
