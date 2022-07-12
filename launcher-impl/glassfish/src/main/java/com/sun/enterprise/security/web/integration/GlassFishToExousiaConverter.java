/*
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
 * Copyright (c) 2022 Fujitsu Limited.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package com.sun.enterprise.security.web.integration;

import static jakarta.servlet.annotation.ServletSecurity.TransportGuarantee.CONFIDENTIAL;
import static jakarta.servlet.annotation.ServletSecurity.TransportGuarantee.NONE;
import static java.util.Collections.list;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.glassfish.exousia.constraints.SecurityConstraint;
import org.glassfish.exousia.constraints.WebResourceCollection;
import org.glassfish.exousia.mapping.SecurityRoleRef;

import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.WebComponentDescriptor;
import com.sun.enterprise.deployment.web.SecurityRoleReference;
import com.sun.enterprise.deployment.web.UserDataConstraint;

import jakarta.servlet.annotation.ServletSecurity.TransportGuarantee;

/**
 * This class converts from GlassFish security types to Exousia security types.
 *
 * @author arjan
 *
 */
public class GlassFishToExousiaConverter {


    /**
     * Get the security constraints from the WebBundleDescriptor.
     *
     * @param webBundleDescriptor the WebBundleDescriptor.
     * @return the security constraints.
     */
    public static List<SecurityConstraint> getConstraintsFromBundle(WebBundleDescriptor webBundleDescriptor) {
        List<SecurityConstraint> constraints = new ArrayList<>();

        for (com.sun.enterprise.deployment.web.SecurityConstraint glassFishSecurityConstraint : list(webBundleDescriptor.getSecurityConstraints())) {

            List<WebResourceCollection> webResourceCollections = new ArrayList<>();
            for (com.sun.enterprise.deployment.web.WebResourceCollection glassFishCollection : glassFishSecurityConstraint.getWebResourceCollections()) {
                webResourceCollections.add(new WebResourceCollection(
                    glassFishCollection.getUrlPatterns(),
                    glassFishCollection.getHttpMethods(),
                    glassFishCollection.getHttpMethodOmissions()));
            }

            constraints.add(new SecurityConstraint(
                webResourceCollections,
                securityRoles(glassFishSecurityConstraint),
                transportGuarantee(glassFishSecurityConstraint)));

        }

        return constraints;
    }

    static Set<String> securityRoles(com.sun.enterprise.deployment.web.SecurityConstraint glassFishSecurityConstraint) {
        if (glassFishSecurityConstraint.getAuthorizationConstraint() == null) {
            return null;
        }

        return
            list(glassFishSecurityConstraint.getAuthorizationConstraint().getSecurityRoles())
                .stream()
                .map(role -> role.getName())
                .collect(toSet());
    }


    static TransportGuarantee transportGuarantee(com.sun.enterprise.deployment.web.SecurityConstraint glassFishSecurityConstraint) {
        UserDataConstraint userDataConstraint = glassFishSecurityConstraint.getUserDataConstraint();
        if (userDataConstraint == null) {
            return NONE;
        }
        String transportGuarantee = userDataConstraint.getTransportGuarantee(); 
        if ("confidential".equalsIgnoreCase(transportGuarantee) || "integral".equalsIgnoreCase(transportGuarantee)) {
            return CONFIDENTIAL;
        } else {
            return NONE;
        }
    }

    /**
     * Get the security role refs from the WebBundleDescriptor.
     *
     * @param servletNames the servlet names.
     * @param webBundleDescriptor the WebBundleDescriptor.
     * @return the security role refs.
     */
    public static Map<String, List<SecurityRoleRef>> getSecurityRoleRefsFromBundle(WebBundleDescriptor webBundleDescriptor) {
        Map<String, List<SecurityRoleRef>> exousiaRoleRefsPerServlet = new HashMap<>();

        for (WebComponentDescriptor webComponent : webBundleDescriptor.getWebComponentDescriptors()) {

           List<SecurityRoleRef> exousiaSecurityRoleRefs = new ArrayList<>();

           for (SecurityRoleReference glassFishSecurityRoleRef : webComponent.getSecurityRoleReferenceSet()) {
               exousiaSecurityRoleRefs.add(new SecurityRoleRef(
                   glassFishSecurityRoleRef.getRoleName(),
                   glassFishSecurityRoleRef.getSecurityRoleLink().getName()));
           }

           exousiaRoleRefsPerServlet.put(webComponent.getCanonicalName(), exousiaSecurityRoleRefs);
        }

        return exousiaRoleRefsPerServlet;
    }

}