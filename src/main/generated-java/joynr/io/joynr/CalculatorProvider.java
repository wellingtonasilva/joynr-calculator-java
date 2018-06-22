/*
 *
 * Copyright (C) 2011 - 2018 BMW Car IT GmbH
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

// #####################################################
//#######################################################
//###                                                 ###
//##    WARNING: This file is generated. DO NOT EDIT   ##
//##             All changes will be lost!             ##
//###                                                 ###
//#######################################################
// #####################################################
package joynr.io.joynr;

import io.joynr.provider.Promise;
import io.joynr.provider.AbstractDeferred;

import io.joynr.provider.JoynrInterface;
import io.joynr.JoynrVersion;


@JoynrInterface(provides = Calculator.class, provider = CalculatorProvider.class, name = "io/joynr/Calculator")
@JoynrVersion(major = 0, minor = 0)
public interface CalculatorProvider {


	/**
	 * calcularSoma
	 * @param a the parameter a
	 * @param b the parameter b
	 * @return promise for asynchronous handling
	 */
	public Promise<CalcularSomaDeferred> calcularSoma(
			Integer a,
			Integer b
	);

	public class CalcularSomaDeferred extends AbstractDeferred {
		public synchronized boolean resolve(Integer result) {
			return super.resolve(result);
		}
	}
}
