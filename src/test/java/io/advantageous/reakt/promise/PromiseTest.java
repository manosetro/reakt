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

package io.advantageous.reakt.promise;

import io.advantageous.reakt.Callback;
import io.advantageous.reakt.Expected;
import io.advantageous.reakt.Result;
import io.advantageous.reakt.promise.impl.BasePromise;
import io.advantageous.reakt.promise.impl.BlockingPromise;
import io.advantageous.reakt.reactor.Reactor;
import org.junit.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.Assert.*;

public class PromiseTest {

    @Test
    public void test() throws Exception {

        TestService testService = new TestService();
        Employee[] employee = new Employee[1];
        Expected[] value = new Expected[1];

        Promise<Employee> promise = Promises.<Employee>promise().then(e -> employee[0] = e)
                .thenExpect(employeeValue -> value[0] = employeeValue);


        testSuccessWithPromise(testService, employee, value, promise);
    }

    @Test
    public void testSafe() throws Exception {

        TestService testService = new TestService();
        Employee[] employee = new Employee[1];
        Expected[] value = new Expected[1];

        Promise<Employee> promise = Promises.<Employee>promise().thenSafe(e -> employee[0] = e)
                .thenSafeExpect(employeeValue -> value[0] = employeeValue);


        testSuccessWithPromise(testService, employee, value, promise);
    }

    @Test
    public void testSafeFail() throws Exception {

        TestService testService = new TestService();
        AtomicReference<Throwable> error = new AtomicReference<>();

        Promise<Employee> promise = Promises.<Employee>promise().thenSafe(new Consumer<Employee>() {
            @Override
            public void accept(Employee employee) {
                throw new IllegalStateException("BOOM.. handler failed");
            }
        }).catchError(error::set);

        testService.simple(promise);
        assertNotNull(error.get());

    }

    @Test
    public void testSafeFinal() throws Exception {

        TestService testService = new TestService();
        Employee[] employee = new Employee[1];
        Expected[] value = new Expected[1];

        Promise<Employee> promise = Promises.<Employee>promise().thenSafe(e -> employee[0] = e)
                .thenSafeExpect(employeeValue -> value[0] = employeeValue).freeze();


        testSuccessWithPromise(testService, employee, value, promise);
    }


    @Test
    public void promiseSafeMethods() throws Exception {
        Promise<String> promise = new Promise<String>() {
            @Override
            public Promise<String> then(Consumer<String> consumer) {
                return null;
            }

            @Override
            public Promise<String> whenComplete(Consumer<Promise<String>> doneListener) {
                return null;
            }

            @Override
            public Promise<String> thenExpect(Consumer<Expected<String>> consumer) {
                return null;
            }

            @Override
            public <U> Promise<U> thenMap(Function<? super String, ? extends U> mapper) {
                return null;
            }

            @Override
            public Promise<String> catchError(Consumer<Throwable> consumer) {
                return null;
            }

            @Override
            public boolean success() {
                return false;
            }

            @Override
            public boolean complete() {
                return false;
            }

            @Override
            public boolean failure() {
                return false;
            }

            @Override
            public Throwable cause() {
                return null;
            }

            @Override
            public Expected<String> expect() {
                return null;
            }

            @Override
            public String get() {
                return null;
            }

            @Override
            public String orElse(String other) {
                return null;
            }

            @Override
            public void onResult(Result<String> result) {

            }

            @Override
            public Promise<String> invokeWithReactor(Reactor reactor) {
                return null;
            }

            @Override
            public Promise<String> invokeWithReactor(Reactor reactor, Duration timeout) {
                return null;
            }
        };

        assertFalse(promise.supportsSafe());

        try {
            promise.thenSafe(s -> {
            });
            fail();
        }catch (Exception ex) {

        }


        try {
            promise.thenSafeExpect(s -> {
            });
            fail();
        }catch (Exception ex) {

        }
    }

    @Test
    public void testSafeHandlerThrows() throws Exception {

        TestService testService = new TestService();
        Employee[] employee = new Employee[1];
        Expected[] value = new Expected[1];

        boolean[] error = new boolean[1];

        Promise<Employee> promise = Promises.<Employee>promise()
                .thenSafe(e -> {
                    employee[0] = e;
                    throw new IllegalStateException("handler blew chunks");
                })
                .thenSafeExpect(employeeValue -> value[0] = employeeValue)
                .catchError(throwable -> error[0] = true);


        testErrorWithPromise(testService, employee, error, promise);
    }


    @Test
    public void testSafeHandlerFinalThrows() throws Exception {

        TestService testService = new TestService();
        Employee[] employee = new Employee[1];
        Expected[] value = new Expected[1];

        boolean[] error = new boolean[1];

        Promise<Employee> promise = Promises.<Employee>promise()
                .thenSafe(e -> {
                    employee[0] = e;
                    throw new IllegalStateException("handler blew chunks");
                })
                .thenSafeExpect(employeeValue -> value[0] = employeeValue)
                .catchError(throwable -> error[0] = true).freeze();


        testErrorWithPromise(testService, employee, error, promise);
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testNotSupportedInvokableInvoke() throws Exception {

        Promise promise = Promises.promise();
        promise.invoke();
    }


    @Test
    public void testNotSupportedInvokableIsInvokable() throws Exception {
        Promise promise = Promises.promise();
        assertFalse(promise.isInvokable());
    }

    @Test
    public void testAnyBlocking() throws Exception {

        TestService testService = new TestService();

        Promise<Employee> promise1 = Promises.promise();
        Promise<Employee> promise2 = Promises.promise();

        final Promise<Void> promise = Promises.anyBlocking(Arrays.asList(promise1, promise2));

        assertFalse(promise.complete());

        testService.async(promise2);

        assertTrue(promise.success());

    }

    @Test
    public void testAllBlocking() throws Exception {

        TestService testService = new TestService();

        Promise<Employee> promise1 = Promises.promise();
        Promise<Employee> promise2 = Promises.promise();

        final Promise<Void> promise = Promises.allBlocking(Arrays.asList(promise1, promise2));

        assertFalse(promise.complete());

        testService.async(promise1);

        assertFalse(promise.complete());

        testService.async(promise2);

        assertTrue(promise.success());

    }


    @Test
    public void testAll() throws Exception {

        /** Test service. */
        TestService testService = new TestService();

        /* Promise that expects an employee. */
        Promise<Employee> promise1 = Promises.promise();
        Promise<Employee> promise2 = Promises.promise();


        /* Promise that returns when all employees are returned. */
        final Promise<Void> promise = Promises.all(promise1, promise2);


        promise.then(nil -> System.out.println("DONE!"));

        assertFalse("Not done yet", promise.complete());

        /** Call service. */
        testService.simple(promise1);

        /** Still not done because only one service has been called. */
        assertFalse(promise.complete());

        /** Ok now second service is called. */
        testService.simple(promise2);

        /** Wait some time. */
        //...

        assertTrue(promise.complete());
        assertTrue(promise.success());

    }

    @Test
    public void testAny() throws Exception {

        /** Test service. */
        TestService testService = new TestService();

        /* Promise that expects an employee. */
        Promise<Employee> promise1 = Promises.promise();
        Promise<Employee> promise2 = Promises.promise();


        /* Promise that returns when all employees are returned. */
        final Promise<Void> promise = Promises.any(promise1, promise2);


        promise.then(nil -> System.out.println("DONE!"));

        assertFalse("Not done yet", promise.complete());

        /** Call service. */
        testService.simple(promise2);


        /** Wait some time. */
        //...

        assertTrue(promise.complete());
        assertTrue(promise.success());

    }


    @Test
    public void testAllReplay() throws Exception {

        TestService testService = new TestService();

        Promise<Employee> promise1 = Promises.promise();
        Promise<Employee> promise2 = Promises.promise();

        final ReplayPromise<Void> promise = Promises.allReplay(Duration.ofMillis(1000),
                Arrays.asList(promise1, promise2));

        assertFalse(promise.complete());

        testService.async(promise1);

        assertFalse(promise.complete());

        testService.async(promise2);


        for (int index = 0; index < 10; index++) {
            promise.checkTimeout(System.currentTimeMillis());
            if (promise.complete()) break;
            Thread.sleep(10);

        }


        assertTrue(promise.complete());
        assertTrue(promise.success());

    }


    @Test
    public void testAllReplayFailFast() throws Exception {

        TestService testService = new TestService();

        Promise<Employee> promise1 = Promises.promise();
        Promise<Employee> promise2 = Promises.promise();

        final ReplayPromise<Void> promise = Promises.allReplay(Duration.ofMillis(1000),
                Arrays.asList(promise1, promise2));

        assertFalse(promise.complete());

        testService.async(promise1);

        assertFalse(promise.complete());

        testService.asyncError(promise2);


        for (int index = 0; index < 10; index++) {
            promise.checkTimeout(System.currentTimeMillis());
            if (promise.complete()) break;
            Thread.sleep(10);

        }


        assertTrue(promise.complete());
        assertTrue(promise.failure());

    }

    @Test
    public void testAnyReplay() throws Exception {

        TestService testService = new TestService();

        Promise<Employee> promise1 = Promises.promise();
        Promise<Employee> promise2 = Promises.promise();

        final ReplayPromise<Void> promise = Promises.anyReplay(Duration.ofMillis(1000),
                Arrays.asList(promise1, promise2));

        assertFalse(promise.complete());

        testService.async(promise2);


        for (int index = 0; index < 10; index++) {
            promise.checkTimeout(System.currentTimeMillis());
            if (promise.complete()) break;
            Thread.sleep(10);

        }


        assertTrue(promise.complete());
        assertTrue(promise.success());

    }


    @Test
    public void testAnyReplayFailFast() throws Exception {

        TestService testService = new TestService();

        Promise<Employee> promise1 = Promises.promise();
        Promise<Employee> promise2 = Promises.promise();

        final ReplayPromise<Void> promise = Promises.anyReplay(Duration.ofMillis(1000),
                promise1, promise2);

        assertFalse(promise.complete());

        testService.asyncError(promise2);


        for (int index = 0; index < 10; index++) {
            promise.checkTimeout(System.currentTimeMillis());
            if (promise.complete()) break;
            Thread.sleep(10);

        }


        assertTrue(promise.complete());
        assertTrue(promise.failure());

    }


    @Test
    public void testFreeze() throws Exception {

        TestService testService = new TestService();
        Employee[] employee = new Employee[1];
        Expected[] value = new Expected[1];
        Promise<Employee> promise = Promises.<Employee>promise().then(e -> employee[0] = e)
                .thenExpect(employeeValue -> value[0] = employeeValue).freeze();


        testSuccessWithPromise(testService, employee, value, promise);
    }

    private void testSuccessWithPromise(TestService testService, Employee[] employee, Expected[] value, Promise<Employee> promise) {


        testService.simple(promise);

        assertNotNull(promise.get());
        assertNotNull(promise.expect());
        assertNotNull(value[0]);
        assertTrue(promise.complete());
        assertFalse(promise.failure());
        assertTrue(promise.success());
        assertNotNull(employee[0]);
    }


    @Test
    public void testAsyncWithBlockingPromise() throws Exception {

        TestService testService = new TestService();
        Employee[] employee = new Employee[1];
        Expected[] value = new Expected[1];

        /* Note this is only for legacy integration and testing. */
        Promise<Employee> promise = Promises.blockingPromise();

        promise.then(e -> employee[0] = e);
        promise.thenExpect(employeeValue -> value[0] = employeeValue);


        testService.async(promise);

        assertNotNull(promise.get());
        assertNotNull(promise.expect());
        assertTrue(promise.complete());
        assertFalse(promise.failure());
        assertTrue(promise.success());
        assertNull(promise.cause());
        assertNotNull(employee[0]);

        assertNotNull(value[0]);

    }

    @Test
    public void testAsyncWithBlockingPromiseThenMap() throws Exception {

        TestService testService = new TestService();
        Sheep[] employee = new Sheep[1];
        Expected[] value = new Expected[1];

        /* Note this is only for legacy integration and testing. */

        Promise<Employee> employeePromise = Promises.<Employee>blockingPromise();

        Promise<Sheep> sheepPromise = employeePromise
                .thenMap(employee1 -> new Sheep(employee1.id));

        sheepPromise.then(e -> employee[0] = e);
        sheepPromise.thenExpect(employeeValue -> value[0] = employeeValue);


        testService.async(employeePromise);

        assertNotNull(sheepPromise.get());
        assertNotNull(sheepPromise.expect());
        assertTrue(sheepPromise.complete());
        assertFalse(sheepPromise.failure());
        assertTrue(sheepPromise.success());
        assertNull(sheepPromise.cause());
        assertNotNull(employee[0]);

        assertNotNull(value[0]);

    }

    @Test
    public void testPromiseThenMap() throws Exception {

        TestService testService = new TestService();
        Sheep[] employee = new Sheep[1];
        Expected[] value = new Expected[1];

        /* Note this is only for legacy integration and testing. */

        Promise<Employee> employeePromise = Promises.<Employee>promise();

        Promise<Sheep> sheepPromise = employeePromise
                .thenMap(employee1 -> new Sheep(employee1.id));

        sheepPromise.then(e -> employee[0] = e);
        sheepPromise.thenExpect(employeeValue -> value[0] = employeeValue);


        testService.simple(employeePromise);

        assertNotNull(employeePromise.get());
        assertNotNull(employeePromise.expect());
        assertTrue(employeePromise.complete());
        assertFalse(employeePromise.failure());
        assertTrue(employeePromise.success());
        assertNull(employeePromise.cause());
        assertNotNull(employee[0]);
        assertNotNull(value[0]);
    }

    @Test
    public void testAsyncWithBlockingPromiseWithDuration() throws Exception {

        TestService testService = new TestService();
        Employee[] employee = new Employee[1];
        Expected[] value = new Expected[1];

        AtomicBoolean completedCalled = new AtomicBoolean();

        /* Note this is only for legacy integration and testing. */
        Promise<Employee> promise = Promises.blockingPromise(Duration.ofMillis(1000));

        promise.then(e -> employee[0] = e);
        promise.thenExpect(employeeValue -> value[0] = employeeValue)
                .whenComplete((p) -> completedCalled.set(true));


        testService.async(promise);

        assertNotNull(promise.get());

        assertTrue(completedCalled.get());
        assertNotNull(promise.expect());
        assertTrue(promise.complete());
        assertFalse(promise.failure());
        assertTrue(promise.success());
        assertNull(promise.cause());
        assertNotNull(employee[0]);

        assertNotNull(value[0]);

    }

    @Test
    public void testAsyncWithReplayPromise() throws Exception {


        ReplayPromise<Employee> promise = Promises.replayPromise(Duration.ofMinutes(10));

        validateReplay(promise);

    }

    @Test
    public void testAsyncWithReplayPromise2() throws Exception {


        ReplayPromise<Employee> promise = Promises.replayPromise(Duration.ofMinutes(10), System.currentTimeMillis());

        validateReplay(promise);

    }

    private void validateReplay(ReplayPromise<Employee> promise) throws InterruptedException {
        TestService testService = new TestService();
        AtomicReference<Employee> employee = new AtomicReference<>();
        AtomicReference<Expected> ref = new AtomicReference<>();
        AtomicBoolean afterCalled = new AtomicBoolean();


        promise.then(employee::set);
        promise.thenExpect(ref::set);
        promise.afterResultProcessed(replayPromise -> {

            promise.replay();
            afterCalled.set(true);
        });


        testService.async(promise);

        for (int index = 0; index < 100; index++) {
            if (promise.checkTimeout(System.currentTimeMillis())) {
                break;
            }
            Thread.sleep(1);

        }

        assertNotNull(promise.get());
        assertNotNull(promise.expect());
        assertTrue(promise.complete());
        assertFalse(promise.failure());
        assertTrue(promise.success());
        assertNull(promise.cause());
        assertNotNull(employee.get());
        assertNotNull(ref.get());
        assertTrue(afterCalled.get());
    }

    @Test
    public void testAsyncHandleTimeout() throws Exception {

        TestService testService = new TestService();
        AtomicReference<Employee> employee = new AtomicReference<>();
        AtomicReference<Expected> ref = new AtomicReference<>();
        AtomicBoolean afterCalled = new AtomicBoolean();
        AtomicBoolean timeoutCalled = new AtomicBoolean();

        ReplayPromise<Employee> promise = Promises.replayPromise(Duration.ofMillis(1));

        promise.then(employee::set);
        promise.thenExpect(ref::set);
        promise.afterResultProcessed(replayPromise -> afterCalled.set(true));
        promise.onTimeout(() -> timeoutCalled.set(true));


        testService.asyncTimeout(promise);

        for (int index = 0; index < 100; index++) {
            if (promise.checkTimeout(System.currentTimeMillis())) {
                break;
            }
            Thread.sleep(10);

        }

        try {
            assertNotNull(promise.get());
            fail();
        } catch (Exception ex) {

        }

        try {
            assertNotNull(promise.expect());
            fail();
        } catch (Exception ex) {

        }

        assertTrue(promise.complete());

        assertTrue(promise.failure());
        assertFalse(promise.success());
        assertNotNull(promise.cause());
        assertNull(employee.get());
        assertNull(ref.get());
        assertTrue(timeoutCalled.get());

        promise.checkTimeout(System.currentTimeMillis());

    }

    @Test
    public void testErrorFreeze() throws Exception {

        TestService testService = new TestService();
        Employee[] employee = new Employee[1];
        boolean[] error = new boolean[1];

        final Promise<Employee> promise = Promises.<Employee>promise()
                .then(e -> employee[0] = e)
                .catchError(throwable -> error[0] = true).freeze();

        testErrorWithPromise(testService, employee, error, promise);
    }

    @Test
    public void testError() throws Exception {

        TestService testService = new TestService();
        Employee[] employee = new Employee[1];
        boolean[] error = new boolean[1];

        final Promise<Employee> promise = Promises.<Employee>promise()
                .then(e -> employee[0] = e)
                .catchError(throwable -> error[0] = true);


        testErrorWithPromise(testService, employee, error, promise);
    }

    private void testErrorWithPromise(TestService testService, Employee[] employee, boolean[] error, Promise<Employee> promise) {
        testService.error(promise);


        try {
            assertNull(promise.get());
            fail();
        } catch (Exception ex) {

        }

        try {
            assertNull(promise.expect());
            fail();
        } catch (Exception ex) {

        }


        assertNull(employee[0]);
        assertTrue(error[0]);
        assertTrue(promise.complete());
        assertTrue(promise.failure());
        assertFalse(promise.success());


        final Employee richard = promise.orElse(new Employee("richard"));

        assertNotNull(richard);
    }

    @Test
    public void testPrematureAccess() throws Exception {

        Employee[] employee = new Employee[1];
        boolean[] error = new boolean[1];

        Promise<Employee> promise = Promises.promise();
        promise
                .then(e -> employee[0] = e)
                .catchError(throwable -> error[0] = true);

        testPrematureAccessWithPromise(promise);

    }

    @Test
    public void testFreezeImmutability() throws Exception {

        Employee[] employee = new Employee[1];
        boolean[] error = new boolean[1];

        Promise<Employee> promise = Promises.<Employee>promise().freeze();

        try {
            promise.then(e -> employee[0] = e);
            fail();
        } catch (UnsupportedOperationException oe) {

        }

        try {
            promise.thenMap((Function<Employee, Sheep>) employee1 -> null);
            fail();
        } catch (UnsupportedOperationException oe) {

        }

        try {
            promise.whenComplete(null);
            fail();
        } catch (UnsupportedOperationException oe) {

        }


        try {
            promise.thenExpect(e -> {
            });
            fail();
        } catch (UnsupportedOperationException oe) {

        }

        try {
            promise.catchError(throwable -> error[0] = true);
            fail();
        } catch (UnsupportedOperationException oe) {

        }

    }

    @Test
    public void testPrematureAccessWithFreeze() throws Exception {

        Employee[] employee = new Employee[1];
        boolean[] error = new boolean[1];

        Promise<Employee> promise = Promises.<Employee>promise()
                .then(e -> employee[0] = e)
                .catchError(throwable -> error[0] = true).freeze();

        testPrematureAccessWithPromise(promise);

    }

    private void testPrematureAccessWithPromise(Promise<Employee> promise) {
        try {
            promise.get();
            fail();
        } catch (NoSuchElementException ex) {

        }

        try {
            promise.expect();
            fail();
        } catch (NoSuchElementException ex) {

        }


        try {
            promise.cause();
            fail();
        } catch (NoSuchElementException ex) {

        }


        try {
            promise.failure();
            fail();
        } catch (NoSuchElementException ex) {

        }


        try {
            promise.success();
            fail();
        } catch (NoSuchElementException ex) {

        }
    }

    @Test
    public void utilityMethods() {
        Promise promise;

        promise = Promises.promiseNotify();
        assertTrue(promise instanceof BasePromise);
        promise = Promises.promise(Employee.class);
        assertTrue(promise instanceof BasePromise);
        promise = Promises.promiseString();
        assertTrue(promise instanceof BasePromise);
        promise = Promises.promiseBoolean();
        assertTrue(promise instanceof BasePromise);
        promise = Promises.promiseLong();
        assertTrue(promise instanceof BasePromise);
        promise = Promises.promiseInt();
        assertTrue(promise instanceof BasePromise);
        promise = Promises.promiseFloat();
        assertTrue(promise instanceof BasePromise);
        promise = Promises.promiseDouble();
        assertTrue(promise instanceof BasePromise);
        promise = Promises.promiseList(Employee.class);
        assertTrue(promise instanceof BasePromise);
        promise = Promises.promiseSet(Employee.class);
        assertTrue(promise instanceof BasePromise);
        promise = Promises.promiseCollection(Employee.class);
        assertTrue(promise instanceof BasePromise);
        promise = Promises.promiseMap(String.class, Employee.class);
        assertTrue(promise instanceof BasePromise);


        promise = Promises.blockingPromiseNotify();
        assertTrue(promise instanceof BlockingPromise);
        promise = Promises.blockingPromise(Employee.class);
        assertTrue(promise instanceof BlockingPromise);
        promise = Promises.blockingPromiseString();
        assertTrue(promise instanceof BlockingPromise);
        promise = Promises.blockingPromiseBoolean();
        assertTrue(promise instanceof BlockingPromise);
        promise = Promises.blockingPromiseLong();
        assertTrue(promise instanceof BlockingPromise);
        promise = Promises.blockingPromiseInt();
        assertTrue(promise instanceof BlockingPromise);
        promise = Promises.blockingPromiseFloat();
        assertTrue(promise instanceof BlockingPromise);
        promise = Promises.blockingPromiseDouble();
        assertTrue(promise instanceof BlockingPromise);
        promise = Promises.blockingPromiseList(Employee.class);
        assertTrue(promise instanceof BlockingPromise);
        promise = Promises.blockingPromiseSet(Employee.class);
        assertTrue(promise instanceof BlockingPromise);
        promise = Promises.blockingPromiseCollection(Employee.class);
        assertTrue(promise instanceof BlockingPromise);
        promise = Promises.blockingPromiseMap(String.class, Employee.class);
        assertTrue(promise instanceof BlockingPromise);


        Duration duration = Duration.ZERO;


        promise = Promises.blockingPromiseNotify(duration);
        assertTrue(promise instanceof BlockingPromise);
        promise = Promises.blockingPromise(Employee.class, duration);
        assertTrue(promise instanceof BlockingPromise);
        promise = Promises.blockingPromiseString(duration);
        assertTrue(promise instanceof BlockingPromise);
        promise = Promises.blockingPromiseBoolean(duration);
        assertTrue(promise instanceof BlockingPromise);
        promise = Promises.blockingPromiseLong(duration);
        assertTrue(promise instanceof BlockingPromise);
        promise = Promises.blockingPromiseInt(duration);
        assertTrue(promise instanceof BlockingPromise);
        promise = Promises.blockingPromiseFloat(duration);
        assertTrue(promise instanceof BlockingPromise);
        promise = Promises.blockingPromiseDouble(duration);
        assertTrue(promise instanceof BlockingPromise);
        promise = Promises.blockingPromiseList(Employee.class, duration);
        assertTrue(promise instanceof BlockingPromise);
        promise = Promises.blockingPromiseSet(Employee.class, duration);
        assertTrue(promise instanceof BlockingPromise);
        promise = Promises.blockingPromiseCollection(Employee.class, duration);
        assertTrue(promise instanceof BlockingPromise);
        promise = Promises.blockingPromiseMap(String.class, Employee.class, duration);
        assertTrue(promise instanceof BlockingPromise);

        long time = 0;


        promise = Promises.replayPromiseNotify(duration, time);
        assertTrue(promise instanceof ReplayPromise);
        promise = Promises.replayPromise(Employee.class, duration, time);
        assertTrue(promise instanceof ReplayPromise);
        promise = Promises.replayPromiseString(duration, time);
        assertTrue(promise instanceof ReplayPromise);
        promise = Promises.replayPromiseBoolean(duration, time);
        assertTrue(promise instanceof ReplayPromise);
        promise = Promises.replayPromiseLong(duration, time);
        assertTrue(promise instanceof ReplayPromise);
        promise = Promises.replayPromiseInt(duration, time);
        assertTrue(promise instanceof ReplayPromise);
        promise = Promises.replayPromiseFloat(duration, time);
        assertTrue(promise instanceof ReplayPromise);
        promise = Promises.replayPromiseDouble(duration, time);
        assertTrue(promise instanceof ReplayPromise);
        promise = Promises.replayPromiseList(Employee.class, duration, time);
        assertTrue(promise instanceof ReplayPromise);
        promise = Promises.replayPromiseSet(Employee.class, duration, time);
        assertTrue(promise instanceof ReplayPromise);
        promise = Promises.replayPromiseCollection(Employee.class, duration, time);
        assertTrue(promise instanceof ReplayPromise);
        promise = Promises.replayPromiseMap(String.class, Employee.class, duration, time);
        assertTrue(promise instanceof ReplayPromise);

    }

    public static class Sheep {

        private final String name;

        public Sheep(String name) {
            this.name = name;
        }
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

    public static class TestService {

        public void simple(Callback<Employee> callback) {
            callback.reply(new Employee("Rick"));
        }


        public void async(final Callback<Employee> callback) {

            new Thread(() -> {
                callback.reply(new Employee("Rick"));
            }).start();
        }


        public void asyncTimeout(final Callback<Employee> callback) {

            new Thread(() -> {
                try {
                    Thread.sleep(10_000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                callback.reply(new Employee("Rick"));
            }).start();
        }

        public void asyncError(final Callback<Employee> callback) {
            new Thread(() -> {
                callback.reject("Rick");
            }).start();
        }


        public void error(Callback<Employee> callback) {
            callback.reject("Error");
        }

        public void exception(Callback<Employee> callback) {
            callback.reject(new IllegalStateException("Error"));
        }
    }
}