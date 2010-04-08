/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gautam.lp.optimization.linear;

import gautam.lp.optimization.OptimizationException;

/**
 * This class represents exceptions thrown by optimizers when no solution
 * fulfills the constraints.
 * @version $Revision: 1.1 $ $Date: 2009/12/12 00:08:03 $
 * @since 2.0
 */
public class NoFeasibleSolutionException extends OptimizationException {

    /** Serializable version identifier. */
    private static final long serialVersionUID = -3044253632189082760L;

    /** 
     * Simple constructor using a default message.
     */
    public NoFeasibleSolutionException() {
        super("no feasible solution");
    }

}
