/*
 *
 *  Copyright (c) 2016. Rick Hightower, Geoff Chandler
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    		http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.advantageous.reakt;

import org.junit.Test;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.Assert.*;


public class ExpectedTest {

    @Test
    public void testEmpty() {

        final Expected<Employee> empty = Expected.empty();
        emptyTest(empty);

    }

    @Test
    public void testEmptyFromNull() {

        final Expected<Employee> empty = Expected.ofNullable(null);
        emptyTest(empty);

    }

    @Test
    public void testNotEmptyFromNullable() {

        final Expected<Employee> rick = Expected.ofNullable(new Employee("Rick"));
        notEmptyTest(rick);

    }

    @Test
    public void testNotEmpty() {

        final Expected<Employee> rick = Expected.of(new Employee("Rick"));
        notEmptyTest(rick);

    }

    @Test
    public void testNotEmptyFromOptional() {

        final Expected<Employee> rick = Expected.ofOptional(Optional.of(new Employee("Rick")));
        notEmptyTest(rick);

    }


    private void emptyTest(Expected<Employee> empty) {
        final boolean[] flag = new boolean[1];

        assertTrue(empty.isEmpty());
        assertFalse(empty.isPresent());

        /* Test ifEmpty and ifPresent. */
        empty.ifEmpty(() -> flag[0] = true);
        assertTrue(flag[0]);
        empty.ifPresent(employee -> flag[0] = false);
        assertTrue(flag[0]);

        final Employee bob = empty.orElse(new Employee("bob"));
        assertNotNull(bob);


        try {
            empty.get();
            fail();
        } catch (NoSuchElementException nsee) {

        }
        assertFalse(empty.filter(employee -> employee.id.equals("Rick")).isPresent());
        assertFalse(empty.filter(employee -> employee.id.equals("Bob")).isPresent());


        final Expected<Sheep> sheepValue = empty.map(employee -> new Sheep(employee.id));
        assertTrue(sheepValue.isEmpty());
    }


    private void notEmptyTest(Expected<Employee> rick) {
        final boolean[] flag = new boolean[1];

        assertFalse(rick.isEmpty());
        assertTrue(rick.isPresent());

        flag[0] = false;
        /* Test ifEmpty and ifPresent. */
        rick.ifEmpty(() -> flag[0] = true);
        assertFalse(flag[0]);
        rick.ifPresent((Employee employee) -> flag[0] = true);
        assertTrue(flag[0]);


        final Employee notBob = rick.orElse(new Employee("bob"));
        assertEquals("Rick", notBob.id);

        final Sheep sheep = rick.map(employee -> new Sheep(employee.id)).get();
        assertEquals("Rick", sheep.id);


        assertTrue(rick.filter(employee -> employee.id.equals("Rick")).isPresent());
        assertFalse(rick.filter(employee -> employee.id.equals("Bob")).isPresent());

        /** Test equals. */
        assertTrue(rick.equalsValue(new Employee("Rick")));
        assertTrue(rick.equals(rick));
        assertTrue(rick.equals(Expected.of(new Employee("Rick"))));
        assertFalse(rick.equals(new Employee("Rick")));
        rick.hashCode();
        rick.toString();
    }

    static class Employee {
        private final String id;

        Employee(String id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Employee employee = (Employee) o;

            return id != null ? id.equals(employee.id) : employee.id == null;

        }

        @Override
        public int hashCode() {
            return id != null ? id.hashCode() : 0;
        }
    }


    static class Sheep {
        private final String id;

        Sheep(String id) {
            this.id = id;
        }
    }

}