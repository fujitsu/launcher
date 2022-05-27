/*
 * Copyright (c) 2021 Fujitsu Limited and/or its affiliates. All rights
 * reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package com.fujitsu.launcher.cli;

public class ParsingResult {

    private OptionMap optionMap;
    private ArgumentList argumentList;

    public ParsingResult(OptionMap optionMap, ArgumentList argumentList) {
        this.optionMap = optionMap;
        this.argumentList = argumentList;
    }

    public OptionMap getOptionMap() {
        return optionMap;
    }

    public void setOptionMap(OptionMap optionMap) {
        this.optionMap = optionMap;
    }

    public ArgumentList getArgumentList() {
        return argumentList;
    }

    public void setArgumentList(ArgumentList argumentList) {
        this.argumentList = argumentList;
    }

    @Override
    public String toString() {
        return "ParseResult[" + optionMap + ", " + argumentList + "]";
    }
}
