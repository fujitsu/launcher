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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class ParserTest {
    private OptionSpec optionSpec = new OptionSpec() {
        {
            define("--iopt", IntOption.class);
            define("--sopt", StringOption.class);
            define("--bopt", BooleanOption.class);
        }
    };

    static Stream<ParserTestSpec> parserTestSpecProvider() {
        return Stream.of(
                new ParserTestSpec(
                        new String[] { "--iopt", "123", "--sopt", "abc", "--bopt", "true", "--foo", "bar" },
                        Arrays.asList(
                                new ExpectedOption("--iopt", 123, IntOption.class),
                                new ExpectedOption("--sopt", "abc", StringOption.class),
                                new ExpectedOption("--bopt", true, BooleanOption.class)),
                        Arrays.asList("--foo", "bar")),
                new ParserTestSpec(
                        new String[] { "--iopt=123", "--sopt=abc", "--bopt=true", "--foo=bar" },
                        Arrays.asList(
                                new ExpectedOption("--iopt", 123, IntOption.class),
                                new ExpectedOption("--sopt", "abc", StringOption.class),
                                new ExpectedOption("--bopt", true, BooleanOption.class)),
                        Arrays.asList("--foo=bar")),
                new ParserTestSpec(
                        new String[] { "--bopt", "false" },
                        Arrays.asList(
                                new ExpectedOption("--bopt", false, BooleanOption.class)),
                        Collections.emptyList()),
                new ParserTestSpec(
                        new String[] { "--bopt", "bar" },
                        Arrays.asList(
                                new ExpectedOption("--bopt", true, BooleanOption.class)),
                        Arrays.asList("bar")),
                new ParserTestSpec(
                        new String[] { "--bopt" },
                        Arrays.asList(
                                new ExpectedOption("--bopt", true, BooleanOption.class)),
                        Collections.emptyList()));
    }

    @ParameterizedTest
    @MethodSource("parserTestSpecProvider")
    public void testParser(ParserTestSpec testSpec) throws CommandLineException {
        Parser parser = new Parser(optionSpec);

        ParsingResult parsingResult = parser.parse(testSpec.getCommandLine());
        OptionMap optionMap = parsingResult.getOptionMap();
        ArgumentList argumentList = parsingResult.getArgumentList();

        assertEquals(testSpec.getExpectedOptions().size(), optionMap.getMap().size());

        for (ExpectedOption expectedOption : testSpec.getExpectedOptions()) {
            assertEquals(expectedOption.getExpected(),
                    optionMap.get(expectedOption.getOptionName(), expectedOption.getOptionType()).getValue());
        }

        assertEquals(testSpec.getExpectedArguments(), argumentList.getList());
    }

    @Test
    public void testMissingOperand() {
        Parser parser = new Parser(optionSpec);

        Exception exception = assertThrows(
                CommandLineException.class, () -> {
                    parser.parse(new String[] { "--sopt" });
                });

        assertEquals("Operand missing for option: --sopt", exception.getMessage());
    }

    public static class ParserTestSpec {
        private String[] commandLine;
        private List<ExpectedOption> expectedOptions;
        private List<String> expectedArguments;

        public ParserTestSpec(String[] commandLine, List<ExpectedOption> expectedOptions, List<String> expectedArguments) {
            this.commandLine = commandLine;
            this.expectedOptions = expectedOptions;
            this.expectedArguments = expectedArguments;
        }

        public String[] getCommandLine() {
            return commandLine;
        }

        public List<ExpectedOption> getExpectedOptions() {
            return expectedOptions;
        }

        public List<String> getExpectedArguments() {
            return expectedArguments;
        }
    }

    public static class ExpectedOption {
        private String optionName;
        private Object expected;
        private Class<? extends Option<?>> optionType;

        public ExpectedOption(String optionName, Object expected, Class<? extends Option<?>> optionType) {
            this.optionName = optionName;
            this.expected = expected;
            this.optionType = optionType;
        }

        public String getOptionName() {
            return optionName;
        }

        public Object getExpected() {
            return expected;
        }

        public Class<? extends Option<?>> getOptionType() {
            return optionType;
        }
    }
}
