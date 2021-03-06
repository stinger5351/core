/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.switchyard.internal;

import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.switchyard.ServiceDomain;
import org.switchyard.spi.ServiceRegistry;
import org.switchyard.spi.EndpointProvider;

public class ServiceDomains {
    public static final String ROOT_DOMAIN = "org.switchyard.domains.root";
    public static final String ENDPOINT_PROVIDER_CLASS_NAME     
        = "org.switchyard.endpoint.provider.class.name";
    public static final String REGISTRY_CLASS_NAME
        = "org.switchyard.registry.class.name";

    private static ConcurrentHashMap<String, ServiceDomain> _domains =
        new ConcurrentHashMap<String, ServiceDomain>();

    private static ServiceRegistry _registry = null;
    private static EndpointProvider _endpointProvider = null;

    /**
     * Returns an instance of the ServiceRegistry.
     * @param registryClass class name of the serviceregistry
     * @return ServiceRegistry
     */
    private static ServiceRegistry getRegistry(final String registryClass) {
        ServiceLoader<ServiceRegistry> registryServices
            = ServiceLoader.load(ServiceRegistry.class);
        for (ServiceRegistry registry : registryServices) {
            if (registryClass.equals(registry.getClass().getName())) {
                return registry;
            }
        }
        return null;
    }
    
    /**
     * Returns an instance of the EndpointProvider.
     * @param providerClass class name of the endpointprovider implementation
     * @return EndpointProvider
     */
    private static EndpointProvider 
        getEndpointProvider(final String providerClass) {
        ServiceLoader<EndpointProvider> providerServices
            = ServiceLoader.load(EndpointProvider.class);
        for (EndpointProvider provider : providerServices) {
            if (providerClass.equals(provider.getClass().getName())) {
                return provider;
            }
        }
        return null;
    }

    /**
     * Initialize the endpointProvider and the registry.
     */
    public synchronized static void init() {
        String registryClassName = System.getProperty(REGISTRY_CLASS_NAME,
                DefaultServiceRegistry.class.getName());
        String endpointProviderClassName = System.getProperty(ENDPOINT_PROVIDER_CLASS_NAME,
                DefaultEndpointProvider.class.getName());

        try {
            _registry = getRegistry(registryClassName);
            _endpointProvider = getEndpointProvider(endpointProviderClassName);
        } catch (NullPointerException npe) {
            throw new RuntimeException(npe);
        }    	
    }
    
    /**
     * Checks to see if the registry and endpointProvider have been 
     * initialized.
     * @return initialization status of the registry/endpointProvider
     */
    public static boolean isInitialized() {
        return (_registry != null) && (_endpointProvider != null);
    }
    
    /**
     * Return the root domain.
     * @return root domain
     */
    public synchronized static ServiceDomain getDomain() {    	
        if (!_domains.containsKey(ROOT_DOMAIN)) {
            createDomain(ROOT_DOMAIN);
        }
        
        return getDomain(ROOT_DOMAIN);
    }
    
    /**
     * Create a domain with the name passed in.
     * @param name name of the domain
     * @return ServiceDomain
     */
    public synchronized static ServiceDomain createDomain(final String name) {
        if (!isInitialized()) {
            init();
        }

        if (_domains.containsKey(name)) {
            throw new RuntimeException("Domain already exists: " + name);
        }

        ServiceDomain domain = new DomainImpl(name, _registry, _endpointProvider);
        _domains.put(name, domain);
        return domain;
    }
    
    /**
     * Get a particular domain.
     * @param domainName domain name
     * @return ServiceDomain
     */
    public static ServiceDomain getDomain(final String domainName) {
        return _domains.get(domainName);
    }

    /**
     * Get the names of the domains that exist.
     * @return domain names
     */
    public static Set<String> getDomainNames() {
        return _domains.keySet();
    }
}
