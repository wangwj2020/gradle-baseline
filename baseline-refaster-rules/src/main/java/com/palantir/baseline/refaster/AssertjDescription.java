/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.baseline.refaster;

import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import com.google.errorprone.refaster.annotation.Repeated;
import org.assertj.core.api.Descriptable;

/**
 * Prefer assertj succinct {@link Descriptable#as} over {@link Descriptable#describedAs}.
 * {@link Descriptable#describedAs} documentation mentions that this method is an alias to
 * {@link Descriptable#as} for groovy where <code>as</code> is a reserved word.
 */
class AssertjDescription {

    @BeforeTemplate
    <T extends Descriptable<T>> T before(T input, String description, @Repeated Object descriptionArgs) {
        return input.describedAs(description, descriptionArgs);
    }

    @AfterTemplate
    <T extends Descriptable<T>> T after(T input, String description, @Repeated Object descriptionArgs) {
        return input.as(description, descriptionArgs);
    }
}
