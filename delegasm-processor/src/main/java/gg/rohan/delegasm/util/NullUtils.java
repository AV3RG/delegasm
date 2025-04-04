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

/**
 * Utility class for handling null-related operations.
 * This class provides methods for common null-related checks and operations.
 */
public final class NullUtils {

    private NullUtils() {
        // Private constructor to prevent instantiation
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Performs an exclusive OR (XOR) operation on two objects' null states.
     * Returns true if exactly one of the objects is null, false otherwise.
     *
     * @param a The first object to check
     * @param b The second object to check
     * @return true if exactly one of the objects is null, false otherwise
     */
    public static boolean xor(final Object a, final Object b) {
        return (a == null) != (b == null);
    }

    /**
     * Checks if exactly one of the given objects is null.
     * This is a more explicit version of xor() that makes the intent clearer.
     *
     * @param a The first object to check
     * @param b The second object to check
     * @return true if exactly one of the objects is null, false otherwise
     */
    public static boolean exactlyOneNull(final Object a, final Object b) {
        return xor(a, b);
    }
}
