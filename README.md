# Spring Boot Interceptors Guide

This guide explains how to use **interceptors** in Spring Boot to perform actions before or after a request is processed by a controller method. We will cover how to create custom interceptors, configure them, and use them to monitor request handling times, for example.

---

## 1. Introduction to Interceptors

In Spring Boot, interceptors provide a way to perform operations before and after a request is handled by a controller. The most common use cases for interceptors include logging, timing, authentication, and modifying request or response objects.

Interceptors in Spring Boot implement the `HandlerInterceptor` interface and are configured in the `WebMvcConfigurer` implementation.

### Key Points:
- Interceptors can be used to pre-process requests and post-process responses.
- They can be applied globally or to specific URLs using `addInterceptors()` method.
- You can intercept requests to inspect or modify the `HttpServletRequest` and `HttpServletResponse`.

## 2. Creating a Custom Interceptor

Below is an example of a custom interceptor that measures the time it takes to process a request.

```java
@Component("timeInterceptor")
public class LoadingTimeInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(LoadingTimeInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HandlerMethod method = (HandlerMethod) handler;
        String methodName = method.getMethod().getName();
        long startTime = System.currentTimeMillis();
        request.setAttribute("start", startTime);

        // Simulating a delay to demonstrate logging
        Random random = new Random();
        Thread.sleep(random.nextInt(500));

        logger.info("LoadingTimeInterceptor: preHandle() entered method " + methodName + "...");

        // Creating custom error response
        Map<String, String> resp = new HashMap<>();
        resp.put("error", "Could not load " + methodName);
        resp.put("date", new Date().toString());

        // Convert response map to JSON
        ObjectMapper mapper = new ObjectMapper();
        String respString = mapper.writeValueAsString(resp);

        response.setContentType("application/json");
        response.setStatus(401);
        response.getWriter().write(respString);
        return false; // Stops the request flow here, response is sent to client
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable ModelAndView modelAndView) throws Exception {
        HandlerMethod method = (HandlerMethod) handler;
        String methodName = method.getMethod().getName();
        long timeTaken = System.currentTimeMillis() - (long) request.getAttribute("start");

        logger.info("LoadingTimeInterceptor: postHandle() exiting method " + methodName + "...");
        logger.info("LoadingTimeInterceptor: Time taken for method " + methodName + " is " + timeTaken + " ms");
    }
}
```
### Detailed Breakdown:

1. **`preHandle` Method**:
   - This method is executed **before** the request is passed to the controller.
   - We first **log** the name of the method being executed (`methodName`).
   - We then **simulate a delay** to demonstrate how logging works while processing the request.
   - The **start time** of the request is saved as an attribute in the `HttpServletRequest` object using `request.setAttribute("start", startTime)`.
   - A **custom error response** is generated (in this case, it’s a simulated error), which includes:
     - The error message (`Could not load {methodName}`).
     - The **current date** when the error occurs.
   - The error response is **converted to JSON** using `ObjectMapper` and sent back to the client with a **401 Unauthorized status**.
   - **Returning `false`** prevents the request from continuing to the controller. Instead, the response is sent directly to the client, effectively **stopping further request processing**.

2. **`postHandle` Method**:
   - This method is executed **after** the controller method has executed but before the view is rendered (if any).
   - It calculates the **time taken** to process the request by subtracting the `start` time from the current time.
   - The **time taken** is logged using `logger.info`, giving insight into the request's processing duration.
   - The `postHandle` method can be used for additional logging or operations that need to happen after the request has been processed by the controller.

### Key Points:

- **`preHandle`**:
   - This method is called **before** the controller method is executed.
   - It allows you to manipulate or log data **before** request handling.
   - If `preHandle` returns `false`, it prevents the controller method from being called and **directly sends a response** to the client.
   
- **`postHandle`**:
   - This method is called **after** the controller method has executed.
   - It allows you to log data or modify the response **after** the controller method has been invoked but before the view is rendered.
   
- **Simulating a Delay**:
   - In this example, we use `Thread.sleep(random.nextInt(500))` to **simulate a delay**. This can be helpful when testing the performance of your application or logging request processing time.
   
- **Error Response**:
   - In the `preHandle` method, we send a **custom error response** to the client. This demonstrates how interceptors can be used for **early error handling** and how to provide structured error messages in the response.

- **Return `false` in `preHandle`**:
   - By returning `false` in `preHandle`, the **request flow is stopped** and a custom response is sent directly to the client. This is useful when you want to halt further request processing (e.g., for error handling or validation).

This detailed explanation helps you understand the flow of request handling in Spring Boot interceptors and how they can be used for logging, monitoring, and error handling.


## 3. Configuring the Interceptor

Now that we have created the interceptor, we need to configure it so it gets applied to specific URL patterns.
```java
@Configuration
public class AppConfig implements WebMvcConfigurer {

    @Autowired
    @Qualifier("timeInterceptor")
    private HandlerInterceptor timeInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Apply the interceptor to specific paths
        registry.addInterceptor(timeInterceptor).addPathPatterns("/app/bar", "/app/baz");
    }
}
``` 
### Key Points:
- The `addInterceptors()` method is used to register the interceptor.
- `addPathPatterns()` specifies which URLs the interceptor will apply to (/app/bar, /app/baz).
- The `timeInterceptor` is injected and used to monitor requests for the specified paths.

## 4. Using the Interceptor in a Controller

We can now define some controller methods that will trigger the interceptor.

```java
@RestController
@RequestMapping("/app")
public class AppController {

    @GetMapping("/foo")
    public Map<String, String> foo() {
        return Collections.singletonMap("message", "handler foo in the controller");
    }

    @GetMapping("/bar")
    public Map<String, String> bar() {
        return Collections.singletonMap("message", "handler bar in the controller");
    }

    @GetMapping("/baz")
    public Map<String, String> baz() {
        return Collections.singletonMap("message", "handler baz in the controller");
    }
}
```
### Key Points:
- The `/foo`, `/bar`, and `/baz` endpoints are defined.
- The `timeInterceptor` will only be applied to `/app/bar` and `/app/baz` (as per the configuration).
- `/foo` will not trigger the interceptor, as it was not included in the `addPathPatterns()` method.


## 5. Testing the Interceptor

When you access the `/app/bar` or `/app/baz` endpoints, the following actions occur:

1. **Pre-handle**: The interceptor measures the request processing time.
   - Logs entry into the method.
   - Simulates a delay and returns a custom JSON error response.

2. **Post-handle**: After the request is processed, it logs the time taken to process the request.

#### Example Output:

- For `/app/bar` or `/app/baz`, the response would be a 401 error with a custom message like this:

```json
{
  "error": "Could not load bar",
  "date": "2025-02-10T10:00:00"
}
```

The logs will also show the method entry and processing time:

```java
LoadingTimeInterceptor: preHandle() entered method bar...
LoadingTimeInterceptor: postHandle() exiting method bar...
LoadingTimeInterceptor: Time taken for method bar is 123 ms
```

## 6. Key Considerations

### 6.1 **Order of Interceptors**

You can control the order of interceptors by setting a lower `order()` value, which ensures that interceptors with lower numbers are executed first. This is useful when you need certain interceptors (like authentication) to run before others (like logging).

### Example:
```java
registry.addInterceptor(timeInterceptor).order(1); // Lower order = higher priority
```

### Key Points:
Interceptors with lower `order` values are executed first.
Order matters when you need to control the execution flow (e.g., authentication before logging).

### 6.2 **Global vs Specific Interceptors**

Interceptors can be applied globally or to specific paths:

- **Global Interceptors**: Apply to all requests.
  
  ```java
  registry.addInterceptor(timeInterceptor).addPathPatterns("/**");
  ```
- **Specific Path Interceptors**: Apply only to certain paths.
```java
registry.addInterceptor(timeInterceptor).addPathPatterns("/app/bar", "/app/baz");
```

You can also exclude specific paths with `excludePathPatterns()`.

### Example:
```java
registry.addInterceptor(timeInterceptor)
        .addPathPatterns("/**")
        .excludePathPatterns("/app/ignore");
```
### Key Points:
- Global interceptors affect all paths.
- Specific interceptors apply to selected paths, improving efficiency.
- Use excludePathPatterns() to avoid intercepting certain paths.

### 6.3 **Stopping Request Processing with `return false`**

Returning `false` in `preHandle` prevents further request processing, allowing you to send an immediate response (e.g., in case of authentication failure).

### Example:

```java
@Override
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    if (userNotAuthenticated(request)) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("Unauthorized access");
        return false; // Stop further processing
    }
    return true;
}
```
### Key Points:
return false stops the request flow, sending the response immediately.
Useful for early exits, such as authentication checks.

### 6.4 **Exception Handling in Interceptors**

You can handle exceptions directly within interceptors, centralizing error handling and ensuring consistent responses for failures.

### Example:

```java
@Override
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    try {
        // Some logic that might throw an exception
    } catch (Exception e) {
        logger.error("Error occurred during request processing: " + e.getMessage());
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.getWriter().write("Internal Server Error");
        return false; // Stop request flow
    }
    return true;
}
```
### Key Points:
- Centralized exception handling in interceptors.
- Allows for consistent error responses across all requests.

### 6.5 **Performance Considerations**

Keep interceptors lightweight. Avoid heavy or blocking operations in `preHandle` to prevent delays in request processing. For tasks like database queries or external API calls, consider using asynchronous processing.

### Key Points:
- Interceptors should not block request processing with heavy or long-running tasks.
- Use asynchronous processing for tasks that may take time (e.g., external API calls).
- Always test the performance impact of interceptors on response times.

### 6.6 **Logging and Monitoring**

Interceptors are ideal for logging requests and responses. Logging request details helps in tracing issues and monitoring application behavior.

### Example:

```java
@Override
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    logger.info("Request method: " + request.getMethod() + ", URL: " + request.getRequestURI());
    return true;
}
```

### Key Points:
- Use interceptors to log request and response information for debugging and monitoring.
- Helps in tracking issues and understanding application usage patterns.
- Consider structured logging (JSON or key-value pairs) for better integration with logging tools.

### **Conclusion**

Interceptors in Spring Boot are useful for handling cross-cutting concerns like logging, error handling, and performance monitoring. Make sure to manage their order, scope, and performance to ensure they don't become a bottleneck.

### Key Considerations:
- Control the execution order of interceptors.
- Apply interceptors globally or to specific paths.
- Use `return false` to stop the request flow when needed.
- Handle exceptions centrally to ensure consistent error responses.
- Keep interceptors lightweight to prevent performance degradation.

---

## Summary

This guide covers:
- How to create custom interceptors in Spring Boot.
- How to apply interceptors to specific URL patterns.
- How to monitor and log request processing times using interceptors.
- How to stop request processing and return custom error responses.

Interceptors are a powerful feature in Spring Boot that allow you to manage request processing in a modular way, centralizing logging, error handling, and other pre/post-processing actions.
