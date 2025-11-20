package _gis.company_search.logging;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;

@Component
public class AsyncLogger {
    private final ExecutorService loggingExecutor;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public AsyncLogger(@Qualifier("loggingExecutorService") ExecutorService loggingExecutor) {
        this.loggingExecutor = loggingExecutor;
    }

    public void info(String message) {
        loggingExecutor.submit(() -> {
            String timestamp = LocalDateTime.now().format(formatter);
            String threadName = Thread.currentThread().getName();
            System.out.println(String.format("[%s] [%s] [INFO] %s", timestamp, threadName, message));
        });
    }

    public void error(String message, Throwable throwable) {
        loggingExecutor.submit(() -> {
            String timestamp = LocalDateTime.now().format(formatter);
            String threadName = Thread.currentThread().getName();
            System.err.println(String.format("[%s] [%s] [ERROR] %s", timestamp, threadName, message));
            if (throwable != null) {
                throwable.printStackTrace(System.err);
            }
        });
    }

    public void warn(String message) {
        loggingExecutor.submit(() -> {
            String timestamp = LocalDateTime.now().format(formatter);
            String threadName = Thread.currentThread().getName();
            System.out.println(String.format("[%s] [%s] [WARN] %s", timestamp, threadName, message));
        });
    }

    public void debug(String message) {
        loggingExecutor.submit(() -> {
            String timestamp = LocalDateTime.now().format(formatter);
            String threadName = Thread.currentThread().getName();
            System.out.println(String.format("[%s] [%s] [DEBUG] %s", timestamp, threadName, message));
        });
    }
}
