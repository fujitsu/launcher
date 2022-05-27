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

import java.util.function.Consumer;

import com.fujitsu.launcher.LauncherConfig;
import com.fujitsu.launcher.Operation;

public class LauncherCommandLine {
    public static final String CONFIG_FILE_OPTION = "--config-file";
    public static final String CONTEXT_ROOT_OPTION = "--contextroot";
    public static final String DEPLOY_OPTION = "--deploy";
    public static final String EXECUTE_OPTION = "--execute";
    public static final String FORCE_OPTION = "--force";
    public static final String GENERATE_OPTION = "--generate";
    public static final String HTTP_LISTENER_OPTION = "--http-listener";
    public static final String HTTPS_LISTENER_OPTION = "--https-listener";
    public static final String LIBRARIES_OPTION = "--libraries";
    public static final String PRECOMPILEJSP_OPTION = "--precompilejsp";

    private static final String[] FORBIDDEN_OPTIONS_FOR_EXECUTE = new String[] {
            CONTEXT_ROOT_OPTION,
            FORCE_OPTION,
            GENERATE_OPTION,
            LIBRARIES_OPTION,
            PRECOMPILEJSP_OPTION,
    };

    public LauncherConfig parseCommandLine(String[] args) throws CommandLineException {
        OptionSpec optionSpec = new OptionSpec();
        initOptionMap(optionSpec);

        Parser parser = new Parser(optionSpec);
        ParsingResult result = parser.parse(args);
        OptionMap optionMap = result.getOptionMap();
        ArgumentList argumentList = result.getArgumentList();
        Operation operation = detectOperation(optionMap);

        validateCommandLine(operation, optionMap, argumentList);

        return createConfigFromParsingResult(operation, optionMap, argumentList);
    }

    private void initOptionMap(OptionSpec optionSpec) {
        optionSpec.define(CONFIG_FILE_OPTION, StringOption.class);
        optionSpec.define(CONTEXT_ROOT_OPTION, StringOption.class);
        optionSpec.define(DEPLOY_OPTION, StringOption.class);
        optionSpec.define(EXECUTE_OPTION, StringOption.class);
        optionSpec.define(FORCE_OPTION, BooleanOption.class);
        optionSpec.define(GENERATE_OPTION, StringOption.class);
        optionSpec.define(HTTP_LISTENER_OPTION, IntOption.class);
        optionSpec.define(HTTPS_LISTENER_OPTION, IntOption.class);
        optionSpec.define(LIBRARIES_OPTION, StringOption.class);
        optionSpec.define(PRECOMPILEJSP_OPTION, BooleanOption.class);
    }

    private static LauncherConfig createConfigFromParsingResult(Operation operation, OptionMap optionMap, ArgumentList argumentList) {
        LauncherConfig config = new LauncherConfig();

        config.setOperation(operation);

        setConfigIfPresent(config::setConfigFile, optionMap.get(CONFIG_FILE_OPTION, StringOption.class));
        setConfigIfPresent(config::setHttpListener, optionMap.get(HTTP_LISTENER_OPTION, StringOption.class));
        setConfigIfPresent(config::setHttpsListener, optionMap.get(HTTPS_LISTENER_OPTION, StringOption.class));
        setConfigIfPresent(config::setContextRoot, optionMap.get(CONTEXT_ROOT_OPTION, StringOption.class));
        setConfigIfPresent(config::setLibraries, optionMap.get(LIBRARIES_OPTION, StringOption.class));
        setConfigIfPresent(config::setPrecompilejsp, optionMap.get(PRECOMPILEJSP_OPTION, StringOption.class));
        setConfigIfPresent(config::setDeploy, optionMap.get(DEPLOY_OPTION, StringOption.class));
        setConfigIfPresent(config::setGenerate, optionMap.get(GENERATE_OPTION, StringOption.class));
        setConfigIfPresent(config::setForce, optionMap.get(FORCE_OPTION, StringOption.class));
        setConfigIfPresent(config::setExecute, optionMap.get(EXECUTE_OPTION, StringOption.class));

        config.setSubcommandArguments(argumentList.getList());

        return config;
    }

    @SuppressWarnings("unchecked")
    private static <T> void setConfigIfPresent(Consumer<T> setter, Option<?> option) {
        if (option != null) {
            setter.accept((T) option.getValue());
        }
    }

    private static Operation detectOperation(OptionMap optionMap) throws CommandLineException {
        boolean deployOptionIsSet = optionMap.get(DEPLOY_OPTION) != null;
        boolean executeOptionIsSet = optionMap.get(EXECUTE_OPTION) != null;
        boolean generateOptionIsSet = optionMap.get(GENERATE_OPTION) != null;

        if (deployOptionIsSet && executeOptionIsSet) {
            throw new CommandLineException("Options '--deploy' and '--execute' are conflicting.");
        } else if (deployOptionIsSet && !executeOptionIsSet) {
            if (generateOptionIsSet) {
                return Operation.GENERATE;
            } else {
                return Operation.DEPLOY;
            }
        } else if (!deployOptionIsSet && executeOptionIsSet) {
            return Operation.EXECUTE;
        } else {
            throw new CommandLineException("Neither option '--deploy' nor '--execute' is specified.");
        }
    }

    private static void validateCommandLine(Operation operation, OptionMap optionMap, ArgumentList argumentList) throws CommandLineException {
        // test emptiness of argument list
        if (operation == Operation.DEPLOY || operation == Operation.GENERATE) {
            if (!argumentList.getList().isEmpty()) {
                throw new CommandLineException("Unexpected argument: " + argumentList.getList().get(0));
            }
        }

        // test forbidden options
        if (operation == Operation.DEPLOY) {
            if (optionMap.get(FORCE_OPTION) != null) {
                throw new CommandLineException("Option '--force' must be used with option '--generate'.");
            }
        } else if (operation == Operation.EXECUTE) {
            for (String forbidden : FORBIDDEN_OPTIONS_FOR_EXECUTE) {
                if (optionMap.get(forbidden) != null) {
                    throw new CommandLineException("Option '" + forbidden + "' cannot be used with option '--execute'.");
                }
            }
        }
    }
}
