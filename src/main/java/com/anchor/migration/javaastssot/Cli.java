package com.anchor.migration.javaastssot;

import com.anchor.migration.javaastssot.cli.CrosswalkCommand;
import com.anchor.migration.javaastssot.cli.ExportCommand;
import com.anchor.migration.javaastssot.cli.InfoCommand;
import com.anchor.migration.javaastssot.cli.ProfilesCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(
        name = "java-ast-ssot",
        mixinStandardHelpOptions = true,
        subcommands = {ExportCommand.class, CrosswalkCommand.class, InfoCommand.class, ProfilesCommand.class})
public final class Cli implements Callable<Integer> {

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    public static void main(String[] args) {
        int exit = new CommandLine(new Cli()).execute(args);
        System.exit(exit);
    }
}
