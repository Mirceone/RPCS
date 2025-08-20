package com.mirceone.commands;

import com.mirceone.core.Command;
import com.mirceone.core.Exec;

import java.io.IOException;

public class DevTest implements Command {
    @Override public String name() { return "test"; }
    @Override public String description() { return "Run a developer test (echo)"; }
    @Override public void run(String[] args) throws IOException, InterruptedException, Exec.CommandFailedException {
        Exec.run("echo", "dev Test working ;)");
    }
}
