package com.anchor.migration.javaastssot.cli;

import com.anchor.migration.javaastssot.profile.ExportProfile;
import com.anchor.migration.javaastssot.profile.ProfileRegistry;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(name = "profiles", description = "List available stack profiles")
public final class ProfilesCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        for (String id : ProfileRegistry.knownProfileIds()) {
            ExportProfile profile = ProfileRegistry.require(id);
            System.out.printf("%s%n  schema: %s%n", id, profile.schemaResource());
        }
        return 0;
    }
}
