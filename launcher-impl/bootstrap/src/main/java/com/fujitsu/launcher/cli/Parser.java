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

import java.util.ArrayList;
import java.util.List;

public class Parser {

    private OptionSpec optionSpec;

    public Parser(OptionSpec optionSpec) {
        this.optionSpec = optionSpec;
    }

    public ParsingResult parse(String[] args) throws CommandLineException {
        return parse(tokenize(args));
    }

    public ParsingResult parse(List<Token> tokens) throws CommandLineException {
        OptionMap optionMap = new OptionMap();
        ArgumentList argumentList = new ArgumentList();

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);

            if (token instanceof OptionNameToken) {
                Token nextToken = i + 1 < tokens.size() ? tokens.get(i + 1) : null;

                if (nextToken != null && nextToken instanceof OptionValueToken) {
                    String optionName = token.get();
                    String optionValue = nextToken.get();
                    Class<? extends Option<?>> optionType = optionSpec.getType(optionName);
                    if (optionType == StringOption.class) {
                        optionMap.put(new StringOption(optionName, optionValue));
                    } else if (optionType == IntOption.class) {
                        int intValue = Integer.parseInt(optionValue);
                        optionMap.put(new IntOption(optionName, intValue));
                    } else if (optionType == BooleanOption.class) {
                        boolean boolValue = Boolean.parseBoolean(optionValue);
                        optionMap.put(new BooleanOption(optionName, boolValue));
                    } else {
                        throw new CommandLineException(
                                "Unsupported type '" + optionType + "' for option '" + optionName + "'");
                    }
                    i++;
                } else {
                    String optionName = token.get();
                    Class<? extends Option<?>> optionType = optionSpec.getType(optionName);

                    if (optionType == BooleanOption.class) {
                        optionMap.put(new BooleanOption(optionName, true));
                    } else {
                        throw new CommandLineException("Operand missing for option: " + optionName);
                    }
                }
            } else if (token instanceof ArgumentToken) {
                argumentList.add(token.get());
            } else {
                throw new CommandLineException("Unexpected token: " + token);
            }
        }

        return new ParsingResult(optionMap, argumentList);
    }

    public List<Token> tokenize(String[] args) {
        List<Token> tokens = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if (arg.startsWith("--")) {
                int equalIndex = arg.indexOf("=");

                String optionName;
                String optionValue;

                if (equalIndex != -1) {
                    optionName = arg.substring(0, equalIndex);
                    optionValue = arg.substring(equalIndex + 1);
                } else {
                    optionName = arg;
                    optionValue = null;
                }

                Class<?> optionType = optionSpec.getType(optionName);
                if (optionType != null) {
                    if (optionValue != null) {
                        tokens.add(new OptionNameToken(optionName));
                        tokens.add(new OptionValueToken(optionValue));
                    } else {
                        if (i + 1 < args.length) {
                            String nextArg = args[i + 1];
                            if (optionType == BooleanOption.class &&
                                    !nextArg.equalsIgnoreCase("true") &&
                                    !nextArg.equalsIgnoreCase("false")) {
                                // if the option is boolean and the next token is not a boolean literal,
                                // we should not consume the next token
                                tokens.add(new OptionNameToken(optionName));
                            } else {
                                tokens.add(new OptionNameToken(optionName));
                                tokens.add(new OptionValueToken(nextArg));
                                i++;
                            }
                        } else {
                            tokens.add(new OptionNameToken(optionName));
                        }
                    }
                } else {
                    // we regard unknown tokens as arguments even if it starts with "--"
                    tokens.add(new ArgumentToken(arg));
                }
            } else {
                tokens.add(new ArgumentToken(arg));
            }
        }
        return tokens;
    }
}
