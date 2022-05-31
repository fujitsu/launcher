/*
 * Copyright (c) 2019-2022 Fujitsu Limited and/or its affiliates. All rights
 * reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.glassfish.web.deployment.annotation.handlers;

import java.util.logging.Level;

import org.eclipse.microprofile.auth.LoginConfig;
import org.glassfish.apf.AnnotationHandlerFor;
import org.glassfish.apf.AnnotationInfo;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.apf.HandlerProcessingResult;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.annotation.context.WebBundleContext;
import com.sun.enterprise.deployment.annotation.context.WebComponentContext;

@Service
@AnnotationHandlerFor(LoginConfig.class)
public class LoginConfigHandler extends AbstractWebHandler {

    public static final String MP_JWT_AUTHENTICATION = "MP-JWT";

    private boolean annotationPresent = false;
    private String authMethod;
    private String realmName;

    @Override
    protected HandlerProcessingResult processAnnotation(AnnotationInfo ainfo, WebComponentContext[] webCompContexts)
            throws AnnotationProcessorException {
        return processAnnotation(ainfo, webCompContexts[0].getDescriptor().getWebBundleDescriptor());
    }

    @Override
    protected HandlerProcessingResult processAnnotation(AnnotationInfo ainfo, WebBundleContext webBundleContext)
            throws AnnotationProcessorException {
        return processAnnotation(ainfo, webBundleContext.getDescriptor());
    }

    private HandlerProcessingResult processAnnotation(AnnotationInfo ainfo, WebBundleDescriptor webBundleDesc)
            throws AnnotationProcessorException {
        Class<?> annotated = (Class<?>) ainfo.getAnnotatedElement();
        LoginConfig annotation = ainfo.getAnnotatedElement().getAnnotation(LoginConfig.class);

        if (!jakarta.ws.rs.core.Application.class.isAssignableFrom(annotated)) {
            log(Level.SEVERE, ainfo,
                    localStrings.getLocalString(
                    "web.deployment.annotation.handlers.needtoimpl",
                    "The Class {0} having annotation {1} need to implement the interface {2}.",
                    new Object[] { annotated.getName(), LoginConfig.class.getName(), jakarta.ws.rs.core.Application.class.getName() }));
            return getDefaultFailedResult();
        }

        if (!MP_JWT_AUTHENTICATION.equals(annotation.authMethod())) {
            log(Level.SEVERE, ainfo, String.format("Unsupported authentication method '%s' is specified in %s at %s.",
                    annotation.authMethod(), LoginConfig.class.getName(), annotated.getName()));
            return getDefaultFailedResult();
        }

        annotationPresent = true;
        authMethod = annotation.authMethod();
        realmName = annotation.realmName();

        // TODO support MP-JWT login-config in dd and adopt the code below:
        // LoginConfiguration loginConfig= new LoginConfigurationImpl();
        // loginConfig.setAuthenticationMethod(annotation.authMethod());
        // loginConfig.setRealmName(annotation.realmName());
        // webBundleDesc.setLoginConfiguration(loginConfig);

        return getDefaultProcessedResult();
    }

    public boolean isAnnotationPresent() {
        return annotationPresent;
    }

    public String getAuthMethod() {
        return authMethod;
    }

    public String getRealmName() {
        return realmName;
    }
}
