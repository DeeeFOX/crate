/*
 * Licensed to Crate under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.  Crate licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial
 * agreement.
 */

package io.crate.metadata;

import io.crate.metadata.functions.Signature;
import io.crate.types.DataType;

import java.util.List;
import java.util.function.Function;

public class FuncResolver implements Function<List<DataType>, FunctionImplementation> {

    private final Signature signature;
    private final Function<List<DataType>, FunctionImplementation> factory;

    public FuncResolver(Signature signature,
                        Function<List<DataType>, FunctionImplementation> factory) {
        this.signature = signature;
        this.factory = factory;
    }

    public Signature getSignature() {
        return signature;
    }

    @Override
    public FunctionImplementation apply(List<DataType> dataTypes) {
        return factory.apply(dataTypes);
    }

    @Override
    public String toString() {
        return "FuncResolver{" + "signature=" + signature + '}';
    }
}
