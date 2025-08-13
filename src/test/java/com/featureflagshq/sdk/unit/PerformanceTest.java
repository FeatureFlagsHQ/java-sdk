package com.featureflagshq.sdk.unit;

import com.featureflagshq.sdk.FeatureFlagsHQSDK;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.RepeatedTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Performance and Stress Tests")
public class PerformanceTest {

    private FeatureFlagsHQSDK sdk;
    private static final String TEST_CLIENT_ID = "test-client-id";
    private static final String TEST_CLIENT_SECRET = "test-client-secret";
    private static final String TEST_USER_ID = "test-user-123";

    @BeforeEach
    void setUp() {
        sdk = new FeatureFlagsHQSDK.Builder()
                .clientId(TEST_CLIENT_ID)
                .clientSecret(TEST_CLIENT_SECRET)
                .environment("test")
                .offlineMode(true)
                .enableMetrics(true) // Enable metrics for performance testing
                .build();
    }

    @AfterEach
    void tearDown() {
        if (sdk != null) {
            sdk.close();
        }
    }

    @Nested
    @DisplayName("Latency Tests")
    class LatencyTests {

        @Test
        @DisplayName("Should achieve low latency for single flag evaluation")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void shouldAchieveLowLatencyForSingleFlagEvaluation() {
            // Warm up
            for (int i = 0; i < 100; i++) {
                sdk.getBool(TEST_USER_ID, "warmup_flag", false);
            }

            // Measure latency
            long totalTime = 0;
            int iterations = 1000;

            for (int i = 0; i < iterations; i++) {
                long start = System.nanoTime();
                sdk.getBool(TEST_USER_ID, "perf_flag_" + (i % 10), false);
                long end = System.nanoTime();
                totalTime += (end - start);
            }

            double avgLatencyMs = (totalTime / iterations) / 1_000_000.0;
            System.out.println("Average flag evaluation latency: " + avgLatencyMs + " ms");

            // Should be under 5ms on average
            assertTrue(avgLatencyMs < 5.0, "Average latency should be under 5ms, was: " + avgLatencyMs + "ms");
        }

        @Test
        @DisplayName("Should maintain consistent latency under load")
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        void shouldMaintainConsistentLatencyUnderLoad() {
            List<Long> latencies = new ArrayList<>();
            int iterations = 5000;

            for (int i = 0; i < iterations; i++) {
                long start = System.nanoTime();
                sdk.getBool("user_" + (i % 100), "load_flag_" + (i % 50), false);
                long end = System.nanoTime();
                latencies.add(end - start);
            }

            // Calculate statistics
            latencies.sort(Long::compareTo);
            double p50 = latencies.get(latencies.size() / 2) / 1_000_000.0;
            double p95 = latencies.get((int) (latencies.size() * 0.95)) / 1_000_000.0;
            double p99 = latencies.get((int) (latencies.size() * 0.99)) / 1_000_000.0;

            System.out.printf("Latency percentiles - P50: %.2fms, P95: %.2fms, P99: %.2fms%n", p50, p95, p99);

            assertTrue(p50 < 5.0, "P50 latency should be under 5ms");
            assertTrue(p95 < 15.0, "P95 latency should be under 15ms");
            assertTrue(p99 < 50.0, "P99 latency should be under 50ms");
        }

        @Test
        @DisplayName("Should handle latency with complex segments")
        void shouldHandleLatencyWithComplexSegments() {
            Map<String, Object> complexSegments = new HashMap<>();
            for (int i = 0; i < 50; i++) {
                complexSegments.put("key_" + i, "value_" + i);
            }

            long totalTime = 0;
            int iterations = 1000;

            for (int i = 0; i < iterations; i++) {
                long start = System.nanoTime();
                sdk.getBool(TEST_USER_ID, "complex_flag", false, complexSegments);
                long end = System.nanoTime();
                totalTime += (end - start);
            }

            double avgLatencyMs = (totalTime / iterations) / 1_000_000.0;
            System.out.println("Complex segments latency: " + avgLatencyMs + " ms");

            assertTrue(avgLatencyMs < 10.0, "Complex segment latency should be under 10ms");
        }
    }

    @Nested
    @DisplayName("Throughput Tests")
    class ThroughputTests {

        @Test
        @DisplayName("Should achieve high throughput for flag evaluations")
        @Timeout(value = 30, unit = TimeUnit.SECONDS)
        void shouldAchieveHighThroughputForFlagEvaluations() {
            final int duration = 10; // seconds
            final AtomicInteger operations = new AtomicInteger(0);
            final AtomicLong errors = new AtomicLong(0);

            long startTime = System.currentTimeMillis();
            long endTime = startTime + (duration * 1000);

            // Run operations for specified duration
            while (System.currentTimeMillis() < endTime) {
                try {
                    sdk.getBool("throughput_user_" + (operations.get() % 1000), 
                               "throughput_flag_" + (operations.get() % 100), false);
                    operations.incrementAndGet();
                } catch (Exception e) {
                    errors.incrementAndGet();
                }
            }

            long actualDuration = System.currentTimeMillis() - startTime;
            double throughput = (operations.get() * 1000.0) / actualDuration;

            System.out.printf("Throughput: %.2f operations/second (%d operations in %d ms)%n", 
                             throughput, operations.get(), actualDuration);

            assertTrue(throughput > 1000, "Should achieve over 1000 ops/sec, achieved: " + throughput);
            assertEquals(0, errors.get(), "Should have no errors during throughput test");
        }

        @Test
        @DisplayName("Should maintain throughput with different flag types")
        void shouldMaintainThroughputWithDifferentFlagTypes() {
            final int iterations = 10000;
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < iterations; i++) {
                String userId = "mixed_user_" + (i % 500);
                int flagType = i % 5;
                
                switch (flagType) {
                    case 0:
                        sdk.getBool(userId, "bool_flag_" + (i % 20), false);
                        break;
                    case 1:
                        sdk.getString(userId, "string_flag_" + (i % 20), "default");
                        break;
                    case 2:
                        sdk.getInt(userId, "int_flag_" + (i % 20), 0);
                        break;
                    case 3:
                        sdk.getFloat(userId, "float_flag_" + (i % 20), 0.0);
                        break;
                    case 4:
                        sdk.getJson(userId, "json_flag_" + (i % 20), new HashMap<>());
                        break;
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            double throughput = (iterations * 1000.0) / duration;

            System.out.printf("Mixed flag types throughput: %.2f operations/second%n", throughput);
            assertTrue(throughput > 800, "Mixed flag throughput should be over 800 ops/sec");
        }
    }

    @Nested
    @DisplayName("Concurrency Tests")
    class ConcurrencyTests {

        @Test
        @DisplayName("Should handle high concurrency without performance degradation")
        @Timeout(value = 30, unit = TimeUnit.SECONDS)
        void shouldHandleHighConcurrencyWithoutPerformanceDegradation() throws InterruptedException {
            final int threadCount = 50;
            final int operationsPerThread = 1000;
            final AtomicInteger totalOperations = new AtomicInteger(0);
            final AtomicLong totalTime = new AtomicLong(0);
            final CountDownLatch latch = new CountDownLatch(threadCount);

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            long testStartTime = System.currentTimeMillis();

            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                executor.submit(() -> {
                    try {
                        long threadStartTime = System.nanoTime();
                        
                        for (int i = 0; i < operationsPerThread; i++) {
                            String userId = "concurrent_user_" + threadId + "_" + i;
                            String flagName = "concurrent_flag_" + (i % 10);
                            sdk.getBool(userId, flagName, false);
                            totalOperations.incrementAndGet();
                        }
                        
                        long threadEndTime = System.nanoTime();
                        totalTime.addAndGet(threadEndTime - threadStartTime);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertTrue(latch.await(25, TimeUnit.SECONDS), "All threads should complete within 25 seconds");
            executor.shutdown();

            long testDuration = System.currentTimeMillis() - testStartTime;
            double throughput = (totalOperations.get() * 1000.0) / testDuration;
            double avgLatencyMs = (totalTime.get() / totalOperations.get()) / 1_000_000.0;

            System.out.printf("Concurrent test - Throughput: %.2f ops/sec, Avg Latency: %.2fms%n", 
                             throughput, avgLatencyMs);

            assertEquals(threadCount * operationsPerThread, totalOperations.get());
            assertTrue(throughput > 1000, "Concurrent throughput should be over 1000 ops/sec");
            assertTrue(avgLatencyMs < 10.0, "Concurrent average latency should be under 10ms");
        }

        @Test
        @DisplayName("Should scale linearly with thread count")
        void shouldScaleLinearlyWithThreadCount() throws InterruptedException {
            int[] threadCounts = {1, 2, 4, 8, 16};
            double[] throughputs = new double[threadCounts.length];

            for (int t = 0; t < threadCounts.length; t++) {
                int threadCount = threadCounts[t];
                int operationsPerThread = 500;
                
                CountDownLatch latch = new CountDownLatch(threadCount);
                ExecutorService executor = Executors.newFixedThreadPool(threadCount);
                
                long startTime = System.currentTimeMillis();
                
                for (int i = 0; i < threadCount; i++) {
                    final int threadId = i;
                    executor.submit(() -> {
                        try {
                            for (int j = 0; j < operationsPerThread; j++) {
                                sdk.getBool("scale_user_" + threadId + "_" + j, "scale_flag", false);
                            }
                        } finally {
                            latch.countDown();
                        }
                    });
                }
                
                latch.await();
                executor.shutdown();
                
                long duration = System.currentTimeMillis() - startTime;
                throughputs[t] = (threadCount * operationsPerThread * 1000.0) / duration;
                
                System.out.printf("Threads: %d, Throughput: %.2f ops/sec%n", threadCount, throughputs[t]);
            }

            // Check that throughput generally increases with more threads
            assertTrue(throughputs[1] > throughputs[0] * 0.8, "Should scale with thread count");
            assertTrue(throughputs[2] > throughputs[1] * 0.8, "Should continue scaling");
        }
    }

    @Nested
    @DisplayName("Memory Performance Tests")
    class MemoryPerformanceTests {

        @Test
        @DisplayName("Should maintain stable memory usage under load")
        void shouldMaintainStableMemoryUsageUnderLoad() {
            Runtime runtime = Runtime.getRuntime();
            
            // Force garbage collection and get baseline
            System.gc();
            long baselineMemory = runtime.totalMemory() - runtime.freeMemory();
            
            // Generate load
            for (int i = 0; i < 50000; i++) {
                String userId = "memory_user_" + (i % 1000); // Reuse user IDs
                String flagName = "memory_flag_" + (i % 100); // Reuse flag names
                sdk.getBool(userId, flagName, false);
            }
            
            // Force garbage collection and measure
            System.gc();
            long finalMemory = runtime.totalMemory() - runtime.freeMemory();
            long memoryIncrease = finalMemory - baselineMemory;
            
            System.out.printf("Memory usage - Baseline: %d bytes, Final: %d bytes, Increase: %d bytes%n",
                             baselineMemory, finalMemory, memoryIncrease);
            
            // Memory increase should be reasonable (less than 10MB)
            assertTrue(memoryIncrease < 10 * 1024 * 1024, 
                      "Memory increase should be under 10MB, was: " + (memoryIncrease / 1024) + " KB");
        }

        @Test
        @DisplayName("Should handle memory pressure gracefully")
        void shouldHandleMemoryPressureGracefully() {
            // Try to create memory pressure
            List<String> memoryConsumer = new ArrayList<>();
            
            try {
                // Consume some memory
                for (int i = 0; i < 100000; i++) {
                    memoryConsumer.add("memory_string_" + i + "_" + "x".repeat(100));
                }
                
                // SDK should still work under memory pressure
                long startTime = System.currentTimeMillis();
                for (int i = 0; i < 1000; i++) {
                    sdk.getBool("pressure_user_" + i, "pressure_flag", false);
                }
                long duration = System.currentTimeMillis() - startTime;
                
                assertTrue(duration < 5000, "Should handle memory pressure without significant slowdown");
                
            } finally {
                memoryConsumer.clear(); // Release memory
                System.gc();
            }
        }
    }

    @Nested
    @DisplayName("Stress Tests")
    class StressTests {

        @Test
        @DisplayName("Should survive extended stress test")
        @Timeout(value = 60, unit = TimeUnit.SECONDS)
        void shouldSurviveExtendedStressTest() {
            final AtomicInteger operations = new AtomicInteger(0);
            final AtomicInteger errors = new AtomicInteger(0);
            final int duration = 30; // seconds
            
            long startTime = System.currentTimeMillis();
            long endTime = startTime + (duration * 1000);
            
            // Create multiple stress patterns
            ExecutorService executor = Executors.newFixedThreadPool(10);
            
            // Pattern 1: High frequency single user
            executor.submit(() -> {
                while (System.currentTimeMillis() < endTime) {
                    try {
                        sdk.getBool("stress_single_user", "stress_flag_1", false);
                        operations.incrementAndGet();
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    }
                }
            });
            
            // Pattern 2: Many different users
            executor.submit(() -> {
                int userId = 0;
                while (System.currentTimeMillis() < endTime) {
                    try {
                        sdk.getBool("stress_user_" + (userId++), "stress_flag_2", false);
                        operations.incrementAndGet();
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    }
                }
            });
            
            // Pattern 3: Complex segments
            executor.submit(() -> {
                while (System.currentTimeMillis() < endTime) {
                    try {
                        Map<String, Object> segments = new HashMap<>();
                        segments.put("stress_key", System.currentTimeMillis());
                        sdk.getBool("stress_segment_user", "stress_flag_3", false, segments);
                        operations.incrementAndGet();
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    }
                }
            });
            
            // Wait for completion
            try {
                Thread.sleep(duration * 1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            executor.shutdown();
            
            long actualDuration = System.currentTimeMillis() - startTime;
            double throughput = (operations.get() * 1000.0) / actualDuration;
            
            System.out.printf("Stress test - Operations: %d, Errors: %d, Throughput: %.2f ops/sec%n",
                             operations.get(), errors.get(), throughput);
            
            assertTrue(operations.get() > 1000, "Should complete significant operations");
            assertTrue(errors.get() == 0, "Should have no errors during stress test");
            
            // SDK should still be healthy
            Map<String, Object> health = sdk.getHealthCheck();
            assertEquals("healthy", health.get("status"));
        }

        @RepeatedTest(3)
        @DisplayName("Should be consistent across repeated stress tests")
        void shouldBeConsistentAcrossRepeatedStressTests() {
            final int iterations = 5000;
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < iterations; i++) {
                sdk.getBool("repeated_user_" + (i % 100), "repeated_flag_" + (i % 10), false);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            double throughput = (iterations * 1000.0) / duration;
            
            System.out.printf("Repeated stress test throughput: %.2f ops/sec%n", throughput);
            
            assertTrue(throughput > 500, "Repeated tests should maintain performance");
            
            Map<String, Object> stats = sdk.getStats();
            assertTrue((Integer) stats.get("total_user_accesses") >= iterations);
        }
    }

    @Nested
    @DisplayName("Resource Cleanup Tests")
    class ResourceCleanupTests {

        @Test
        @DisplayName("Should clean up resources efficiently")
        void shouldCleanUpResourcesEfficiently() {
            List<FeatureFlagsHQSDK> sdks = new ArrayList<>();
            
            // Create multiple SDKs
            for (int i = 0; i < 10; i++) {
                FeatureFlagsHQSDK tempSDK = new FeatureFlagsHQSDK.Builder()
                        .clientId(TEST_CLIENT_ID)
                        .clientSecret(TEST_CLIENT_SECRET)
                        .offlineMode(true)
                        .build();
                sdks.add(tempSDK);
                
                // Use each SDK
                tempSDK.getBool("cleanup_user_" + i, "cleanup_flag", false);
            }
            
            // Measure cleanup time
            long startCleanup = System.currentTimeMillis();
            for (FeatureFlagsHQSDK tempSDK : sdks) {
                tempSDK.close();
            }
            long cleanupDuration = System.currentTimeMillis() - startCleanup;
            
            System.out.printf("Cleanup duration for 10 SDKs: %d ms%n", cleanupDuration);
            
            assertTrue(cleanupDuration < 5000, "Cleanup should complete within 5 seconds");
        }

        @Test
        @DisplayName("Should handle rapid create/destroy cycles")
        void shouldHandleRapidCreateDestroyCycles() {
            final int cycles = 100;
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < cycles; i++) {
                try (FeatureFlagsHQSDK tempSDK = new FeatureFlagsHQSDK.Builder()
                        .clientId(TEST_CLIENT_ID)
                        .clientSecret(TEST_CLIENT_SECRET)
                        .offlineMode(true)
                        .enableMetrics(false)
                        .build()) {
                    
                    tempSDK.getBool("cycle_user_" + i, "cycle_flag", false);
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            double cyclesPerSecond = (cycles * 1000.0) / duration;
            
            System.out.printf("Create/destroy cycles: %.2f per second%n", cyclesPerSecond);
            
            assertTrue(cyclesPerSecond > 5, "Should handle at least 5 create/destroy cycles per second");
        }
    }
}