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

package gautam.lp.linear;

/**
 * Default implementation of the {@link RealMatrixChangingVisitor} interface.
 * <p>
 * This class is a convenience to create custom visitors without defining all
 * methods. This class provides default implementations that do nothing.
 * </p>
 * 
 * @version $Revision: 1.1 $ $Date: 2009/12/12 00:08:04 $
 * @since 2.0
 */
public class DefaultRealMatrixChangingVisitor implements RealMatrixChangingVisitor {

    /** {@inheritDoc} */
    public void start(int rows, int columns,
                      int startRow, int endRow, int startColumn, int endColumn) {
    }

    /** {@inheritDoc} */
    public double visit(int row, int column, double value)
        throws MatrixVisitorException {
        return value;
    }

    /** {@inheritDoc} */
    public double end() {
        return 0;
    }

}
