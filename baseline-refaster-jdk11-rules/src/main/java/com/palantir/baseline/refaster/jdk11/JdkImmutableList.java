package com.palantir.baseline.refaster.jdk11;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.refaster.ImportPolicy;
import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import com.google.errorprone.refaster.annotation.Repeated;
import com.google.errorprone.refaster.annotation.UseImportPolicy;
import java.util.List;

public final class JdkImmutableList<T> {
    @BeforeTemplate
    List<T> guavaImmutableList(@Repeated T values) {
        return ImmutableList.of(values);
    }

    @AfterTemplate
    @UseImportPolicy(ImportPolicy.STATIC_IMPORT_ALWAYS)
    List<T> jdkImmutableList(@Repeated T values) {
        return List.of(values);
    }
}
