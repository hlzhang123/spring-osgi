/*
 * Copyright 2006-2009 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.osgi.blueprint.reflect;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.LocalComponentMetadata;
import org.osgi.service.blueprint.reflect.NullValue;
import org.osgi.service.blueprint.reflect.Value;
import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanNameReference;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.ManagedProperties;
import org.springframework.beans.factory.support.ManagedSet;
import org.springframework.osgi.blueprint.config.internal.temporary.TempManagedList;
import org.springframework.osgi.blueprint.config.internal.temporary.TempManagedMap;
import org.springframework.osgi.blueprint.config.internal.temporary.TempManagedSet;

/**
 * Adapter between Spring {@link BeanMetadataElement} and OSGi's Blueprint
 * {@link Value}.
 * 
 * @see BeanMetadataElementFactory
 * @author Costin Leau
 */
class ValueFactory {

	/**
	 * Creates the equivalent value for the given Spring metadata. Since
	 * Spring's metadata is a superset of the Blueprint spec, not all Spring
	 * types are supported.
	 * 
	 * @param metadata
	 * @return
	 */
	static Value buildValue(Object metadata) {

		if (metadata instanceof BeanMetadataElement) {

			// reference
			if (metadata instanceof RuntimeBeanReference) {
				RuntimeBeanReference reference = (RuntimeBeanReference) metadata;
				return new SimpleReferenceValue(reference.getBeanName());
			}
			// reference name
			if (metadata instanceof RuntimeBeanNameReference) {
				RuntimeBeanNameReference reference = (RuntimeBeanNameReference) metadata;
				return new SimpleReferenceNameValue(reference.getBeanName());
			}
			// typed String
			if (metadata instanceof TypedStringValue) {
				// check if it's a <null/>
				TypedStringValue typedString = (TypedStringValue) metadata;
				return (typedString.getValue() == null ? NullValue.NULL : new SimpleTypedStringValue(typedString));
			}

			// bean definition
			if (metadata instanceof BeanDefinition) {
				ComponentMetadata componentMetadata = MetadataFactory.buildComponentMetadataFor(null,
					(BeanDefinition) metadata);
				return new SimpleComponentValue((LocalComponentMetadata) componentMetadata);
			}

			// bean definition holder (used for inner beans/components)
			if (metadata instanceof BeanDefinitionHolder) {
				BeanDefinitionHolder holder = (BeanDefinitionHolder) metadata;

				// we ignore the name even though one was specified
				ComponentMetadata componentMetadata = MetadataFactory.buildComponentMetadataFor(null,
					holder.getBeanDefinition());
				return new SimpleComponentValue((LocalComponentMetadata) componentMetadata);
			}

			// managedXXX...
			if (metadata instanceof ManagedList) {
				ManagedList list = (ManagedList) metadata;

				Value[] values = new Value[list.size()];
				// convert list objects
				for (int i = 0; i < values.length; i++) {
					Object element = list.get(i);
					values[i] = ValueFactory.buildValue(element);
				}
				String defaultType = null;
				if (list instanceof TempManagedList) {
					defaultType = ((TempManagedList) list).getDefaultTypeClassName();
				}
				return new SimpleListValue(values, defaultType);
			}

			if (metadata instanceof ManagedSet) {
				ManagedSet set = (ManagedSet) metadata;

				Value[] values = new Value[set.size()];
				int i = 0;
				// convert set objects
				for (Object element : set) {
					values[i++] = ValueFactory.buildValue(element);
				}
				String defaultType = null;
				if (set instanceof TempManagedSet) {
					defaultType = ((TempManagedSet) set).getDefaultTypeClassName();
				}

				return new SimpleSetValue(values, defaultType);
			}

			if (metadata instanceof ManagedMap) {
				ManagedMap map = (ManagedMap) metadata;

				Value[] keys = new Value[map.size()];
				Value[] values = new Value[map.size()];
				int i = 0;
				// convert map objects
				Set<Map.Entry> entrySet = (Set<Map.Entry>) map.entrySet();
				for (Iterator<Map.Entry> iterator = entrySet.iterator(); iterator.hasNext();) {
					Map.Entry next = iterator.next();
					keys[i] = ValueFactory.buildValue(next.getKey());
					values[i] = ValueFactory.buildValue(next.getValue());
					i++;
				}

				String defaultKeyType = null, defaultValueType = null;
				if (map instanceof TempManagedMap) {
					defaultKeyType = ((TempManagedMap) map).getKeyDefaultTypeClassName();
					defaultValueType = ((TempManagedMap) map).getValueDefaultTypeClassName();
				}

				return new SimpleMapValue(keys, defaultKeyType, values, defaultValueType);
			}

			if (metadata instanceof ManagedProperties) {
				ManagedProperties properties = (ManagedProperties) metadata;
				Properties props = new Properties();
				// convert properties items
				Set<Map.Entry<Object, Object>> entrySet = (Set<Map.Entry<Object, Object>>) properties.entrySet();

				for (Iterator<Map.Entry<Object, Object>> iterator = entrySet.iterator(); iterator.hasNext();) {
					Map.Entry<Object, Object> next = iterator.next();
					Object key = ValueFactory.buildValue(next.getKey());
					Object value = ValueFactory.buildValue(next.getValue());
					props.put(key, value);
				}

				return new SimplePropertiesValue(props);
			}

			throw new IllegalArgumentException("Unsupported metadata type " + metadata.getClass());
		}

		throw new UnsupportedOperationException("Cannot handle non metadata elements " + metadata + "| class "
				+ metadata.getClass());
	}
}