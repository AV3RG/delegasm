/*
This file is part of Delegasm, licensed under the MIT License.

Copyright (c) 2025 Rohan Goyal

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package gg.rohan.delegasm.util;

import java.util.Objects;

/**
 * A generic pair class that holds two values of potentially different types.
 * This class is immutable and provides a convenient way to group two related values.
 *
 * @param <A> The type of the first value
 * @param <B> The type of the second value
 */
public interface Pair<A, B> {

    /**
     * Gets the first value of the pair.
     *
     * @return The first value
     */
    A getFirst();

    /**
     * Gets the second value of the pair.
     *
     * @return The second value
     */
    B getSecond();

    /**
     * Creates a new immutable pair with the given values.
     *
     * @param first The first value
     * @param second The second value
     * @param <A> The type of the first value
     * @param <B> The type of the second value
     * @return A new pair containing the given values
     */
    static <A, B> Pair<A, B> of(A first, B second) {
        return new Pair<A, B>() {
            @Override
            public A getFirst() {
                return first;
            }

            @Override
            public B getSecond() {
                return second;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Pair<?, ?> pair = (Pair<?, ?>) o;
                return Objects.equals(getFirst(), pair.getFirst()) &&
                       Objects.equals(getSecond(), pair.getSecond());
            }

            @Override
            public int hashCode() {
                return Objects.hash(getFirst(), getSecond());
            }

            @Override
            public String toString() {
                return "Pair{" +
                        "first=" + getFirst() +
                        ", second=" + getSecond() +
                        '}';
            }
        };
    }
}
