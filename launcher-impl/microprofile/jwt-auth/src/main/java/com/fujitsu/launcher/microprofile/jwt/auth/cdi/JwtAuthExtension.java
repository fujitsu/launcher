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
package com.fujitsu.launcher.microprofile.jwt.auth.cdi;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessBeanAttributes;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.Claims;
import org.glassfish.hk2.utilities.reflection.ParameterizedTypeImpl;
import org.glassfish.internal.api.Globals;

import com.fujitsu.launcher.microprofile.jwt.auth.JwtAuthService;

import io.smallrye.jwt.auth.cdi.ClaimValueProducer;
import io.smallrye.jwt.auth.cdi.CommonJwtProducer;
import io.smallrye.jwt.auth.cdi.JWTCallerPrincipalFactoryProducer;
import io.smallrye.jwt.auth.cdi.JsonValueProducer;
import io.smallrye.jwt.auth.cdi.PrincipalProducer;
import io.smallrye.jwt.auth.cdi.RawClaimTypeProducer;
import io.smallrye.jwt.auth.jaxrs.JWTAuthenticationFilter;
import io.smallrye.jwt.auth.principal.DefaultJWTParser;

public class JwtAuthExtension implements Extension {

    private static final Type[] OPTIONAL_RAW_CLAIM_TYPES = new Type[] {
            new ParameterizedTypeImpl(Optional.class, String.class),
            new ParameterizedTypeImpl(Optional.class, new ParameterizedTypeImpl(Set.class, String.class)),
            new ParameterizedTypeImpl(Optional.class, Long.class),
            new ParameterizedTypeImpl(Optional.class, Boolean.class),
    };

    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd, BeanManager bm) {
        if (Globals.getDefaultHabitat().getService(JwtAuthService.class).isJwtAuthEnabled()) {
            bbd.addAnnotatedType(bm.createAnnotatedType(ClaimValueProducer.class), ClaimValueProducer.class.getName());
            bbd.addAnnotatedType(bm.createAnnotatedType(CommonJwtProducer.class), CommonJwtProducer.class.getName());
            bbd.addAnnotatedType(bm.createAnnotatedType(JsonValueProducer.class), JsonValueProducer.class.getName());
            bbd.addAnnotatedType(bm.createAnnotatedType(VerboseJwtAuthContextInfoProvider.class), VerboseJwtAuthContextInfoProvider.class.getName());
            bbd.addAnnotatedType(bm.createAnnotatedType(JWTAuthenticationFilter.class), JWTAuthenticationFilter.class.getName());
            bbd.addAnnotatedType(bm.createAnnotatedType(PrincipalProducer.class), PrincipalProducer.class.getName());
            bbd.addAnnotatedType(bm.createAnnotatedType(RawClaimTypeProducer.class), RawClaimTypeProducer.class.getName());
            bbd.addAnnotatedType(bm.createAnnotatedType(DefaultJWTParser.class), DefaultJWTParser.class.getName());
            bbd.addAnnotatedType(bm.createAnnotatedType(JWTCallerPrincipalFactoryProducer.class), JWTCallerPrincipalFactoryProducer.class.getName());
        }
    }

    public void processBeanAttributes(@Observes ProcessBeanAttributes<?> pba, BeanManager bm) {
        Claim claim = pba.getAnnotated().getAnnotation(Claim.class);
        if (claim != null && isClaimUnknown(claim)) {
            if (pba.getBeanAttributes().getTypes().contains(Optional.class)) {
                // let RawClaimTypeProducer#getOptionalValue handle each optional type
                pba.configureBeanAttributes().addTypes(new HashSet<>(Arrays.asList(OPTIONAL_RAW_CLAIM_TYPES)));
            }
        }
    }

    private boolean isClaimUnknown(Claim claim) {
        return (claim.value() == null || claim.value().isEmpty()) && claim.standard() == Claims.UNKNOWN;
    }
}
