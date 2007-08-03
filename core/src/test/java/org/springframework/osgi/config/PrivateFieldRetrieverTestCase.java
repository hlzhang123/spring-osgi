/*
 * Copyright 2002-2007 the original author or authors.
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
package org.springframework.osgi.config;

import java.lang.reflect.Field;

import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;
import org.springframework.util.ReflectionUtils.FieldFilter;

import junit.framework.TestCase;

/**
 * Base test class which permits retrieving values of private fields.
 * 
 * @author Costin Leau
 * 
 */
public abstract class PrivateFieldRetrieverTestCase extends TestCase {

	protected Object getPrivateProperty(final Object target, final String fieldName) {
		final Field foundField[] = new Field[1];

		ReflectionUtils.doWithFields(target.getClass(), new FieldCallback() {

			public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
				field.setAccessible(true);
				foundField[0] = field;
			}

		}, new FieldFilter() {

			public boolean matches(Field field) {
				return fieldName.equals(field.getName());
			}

		});

		try {
			return foundField[0].get(target);
		}
		catch (Exception ex) {
			// translate
			throw new RuntimeException(ex);
		}
	}
}