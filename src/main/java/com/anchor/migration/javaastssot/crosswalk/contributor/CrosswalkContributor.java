package com.anchor.migration.javaastssot.crosswalk.contributor;

import com.anchor.migration.javaastssot.crosswalk.CrosswalkContext;
import com.anchor.migration.javaastssot.crosswalk.model.CrosswalkContribution;

public interface CrosswalkContributor {
    String profileId();

    CrosswalkContribution contribute(CrosswalkContext context) throws Exception;
}
