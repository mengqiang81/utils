需要依赖
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>5.1</version>
</dependency>

如果使用同步 appender，又没有使用 EagleEyeSupportThreadPoolExecutor 线程池的话，可以使用 EagleEyeProvider 用于记录 trace_id

如果使用了 EagleEyeSupportThreadPoolExecutor 线程池的话，可以使用异步 appender，只需要使用 AppNameProvider 记录 app_name 就可以了

logstash-logback-encoder 的使用见[这里](https://github.com/logstash/logstash-logback-encoder)

logback 的推荐配置见 test/resources/logback.xml
