/*
 * Copyright 2015-2023 the original author or authors.
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
package de.odrotbohm.restbucks;

import java.util.regex.Pattern;

import javax.money.MonetaryAmount;
import javax.money.format.MonetaryFormats;

import org.javamoney.moneta.Money;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.rest.webmvc.json.JsonSchema.JsonSchemaProperty;
import org.springframework.data.rest.webmvc.json.JsonSchemaPropertyCustomizer;
import org.springframework.lang.Nullable;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.deser.ValueInstantiator;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Configures custom serialization and deserialization of {@link Money} instances
 *
 * @author Oliver Gierke
 */
@Configuration(proxyBeanMethods = false)
class JacksonCustomizations {

	@Bean
	JacksonModule moneyModule() {
		return new MoneyModule();
	}

	@SuppressWarnings("serial")
	static class MoneyModule extends SimpleModule {

		public MoneyModule() {

			addSerializer(MonetaryAmount.class, new MonetaryAmountSerializer());
			addValueInstantiator(MonetaryAmount.class, new MoneyInstantiator());
		}

		/**
		 * A dedicated serializer to render {@link MonetaryAmount} instances as formatted {@link String}. Also implements
		 * {@link JsonSchemaPropertyCustomizer} to expose the different rendering to the schema exposed by Spring Data REST.
		 *
		 * @author Oliver Gierke
		 */
		static class MonetaryAmountSerializer extends StdSerializer<MonetaryAmount>
				implements JsonSchemaPropertyCustomizer {

			private static final Pattern MONEY_PATTERN;

			static {

				StringBuilder builder = new StringBuilder();

				builder.append("(?=.)^"); // Start
				builder.append("[A-Z]{3}?"); // 3-digit currency code
				builder.append("\\s"); // single whitespace character
				builder.append("(([1-9][0-9]{0,2}(,[0-9]{3})*)|[0-9]+)?"); // digits with optional grouping by "," every 3)
				builder.append("(\\.[0-9]{1,2})?$"); // End with a dot and two digits

				MONEY_PATTERN = Pattern.compile(builder.toString());
			}

			public MonetaryAmountSerializer() {
				super(MonetaryAmount.class);
			}

			/*
			 * (non-Javadoc)
			 * @see tools.jackson.databind.ser.std.StdSerializer#serialize(java.lang.Object, tools.jackson.core.JsonGenerator, tools.jackson.databind.SerializationContext)
			 */
			@Override
			public void serialize(@Nullable MonetaryAmount value, JsonGenerator jgen, SerializationContext provider) {

				if (value != null) {
					jgen.writeString(MonetaryFormats.getAmountFormat(LocaleContextHolder.getLocale()).format(value));
				} else {
					jgen.writeNull();
				}
			}

			/*
			 * (non-Javadoc)
			 * @see org.springframework.data.rest.webmvc.json.JsonSchemaPropertyCustomizer#customize(org.springframework.data.rest.webmvc.json.JsonSchema.JsonSchemaProperty, org.springframework.data.core.TypeInformation)
			 */
			@Override
			public JsonSchemaProperty customize(JsonSchemaProperty property, TypeInformation<?> type) {
				return property.withType(String.class).withPattern(MONEY_PATTERN);
			}
		}

		static class MoneyInstantiator extends ValueInstantiator.Base {

			MoneyInstantiator() {
				super(MonetaryAmount.class);
			}

			/*
			 * (non-Javadoc)
			 * @see tools.jackson.databind.deser.ValueInstantiator#canCreateFromString()
			 */
			@Override
			public boolean canCreateFromString() {
				return true;
			}

			/*
			 * (non-Javadoc)
			 * @see tools.jackson.databind.deser.ValueInstantiator#createFromString(tools.jackson.databind.DeserializationContext, java.lang.String)
			 */
			@Override
			public Object createFromString(DeserializationContext context, String value) {
				return Money.parse(value, MonetaryFormats.getAmountFormat(LocaleContextHolder.getLocale()));
			}
		}
	}
}
