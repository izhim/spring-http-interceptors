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

---

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

Key Points:

    The preHandle method is called before the request is processed by the controller method.

    The postHandle method is called after the request is processed by the controller but before the view is rendered.

    In preHandle, we simulate a delay and log the method name, then send a custom error response with a 401 status code.

    postHandle logs the time it took to process the request.

3. Configuring the Interceptor

Now that we have created the interceptor, we need to configure it so it gets applied to specific URL patterns.

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

Key Points:

    The addInterceptors() method is used to register the interceptor.

    addPathPatterns() specifies which URLs the interceptor will apply to (/app/bar, /app/baz).

    The timeInterceptor is injected and used to monitor requests for the specified paths.

4. Using the Interceptor in a Controller

We can now define some controller methods that will trigger the interceptor.

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

Key Points:

    The /foo, /bar, and /baz endpoints are defined.

    The timeInterceptor will only be applied to /app/bar and /app/baz (as per the configuration).

    /foo will not trigger the interceptor, as it was not included in the addPathPatterns() method.

5. Testing the Interceptor

When you access the /app/bar or /app/baz endpoints, the following actions occur:

    Pre-handle: The interceptor measures the request processing time.

        Logs entry into the method.

        Simulates a delay and returns a custom JSON error response.

    Post-handle: After the request is processed, it logs the time taken to process the request.

Example Output:

    For /app/bar or /app/baz, the response would be a 401 error with a custom message like this:

{
  "error": "Could not load bar",
  "date": "2025-02-10T10:00:00"
}

    The logs will also show the method entry and processing time:

LoadingTimeInterceptor: preHandle() entered method bar...
LoadingTimeInterceptor: postHandle() exiting method bar...
LoadingTimeInterceptor: Time taken for method bar is 123 ms

6. Key Considerations

    Order of Interceptors: You can set the order in which interceptors are applied by using registry.addInterceptor(timeInterceptor).order(1).

    Global vs Specific Interceptors: If you want the interceptor to apply to all endpoints, use addPathPatterns("/**").

    Return true or false: If preHandle returns true, the request continues to the controller method. If it returns false, the request processing is stopped, and the response is directly returned.

    Exception Handling: You can also use interceptors for logging or modifying the response of exceptions that occur in the controller.

Summary

This guide covers:

    How to create custom interceptors in Spring Boot.

    How to apply interceptors to specific URL patterns.

    How to monitor and log request processing times using interceptors.

    How to stop request processing and return custom error responses.

Interceptors are a powerful feature in Spring Boot that allow you to manage request processing in a modular way, centralizing logging, error handling, and other pre/post-processing actions.